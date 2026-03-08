package org.expert.link.app.shared.screen.profile

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.expert.link.app.shared.navigation.BaseComponent
import org.expert.link.app.shared.navigation.Config
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.presentation.CallsStore
import org.expert.link.app.shared.presentation.ChatsStore
import org.expert.link.app.shared.presentation.ContactsStore
import org.expert.link.app.shared.presentation.DiagnosticsStore
import org.expert.link.app.shared.presentation.HomeStore
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.presentation.NodeSessionController
import org.expert.link.app.shared.presentation.NodeSessionState
import org.expert.link.app.shared.presentation.NearbyStore
import org.expert.link.app.shared.presentation.SettingsStore
import org.expert.link.app.shared.presentation.TransfersStore
import org.expert.link.mesh.contract.model.MeshPeerEndpoint
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class ProfileTab(val title: String) {
    OVERVIEW("Обзор"),
    PROFILE("Профиль"),
    CHATS("Чаты"),
    NETWORK("Сеть"),
    CONTACTS("Контакты"),
    METRICS("Метрики"),
    SETTINGS("Настройки"),
}

data class ProfileState(
    val runtimeStatus: NodeRuntimeStatus = NodeRuntimeStatus.STOPPED,
    val displayName: String = "",
    val draftName: String = "",
    val peerId: String = "",
    val endpoint: MeshPeerEndpoint? = null,
    val selectedTab: ProfileTab = ProfileTab.PROFILE,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class ProfileComponent(
    context: ComponentContext,
    onNavigate: (Config) -> Unit,
    onBack: () -> Unit,
) : BaseComponent(context, onNavigate, onBack), KoinComponent {
    private val session: NodeSessionController by inject()
    private val platformServices: AppPlatformServices by inject()

    val sessionState: StateFlow<NodeSessionState> = session.state
    val services: AppPlatformServices
        get() = platformServices

    val homeStore = HomeStore(session)
    val chatsStore = ChatsStore(session)
    val nearbyStore = NearbyStore(session)
    val contactsStore = ContactsStore(session)
    val transfersStore = TransfersStore(session)
    val callsStore = CallsStore(session)
    val diagnosticsStore = DiagnosticsStore(session)
    val settingsStore = SettingsStore(session)

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private var lastSyncedName: String = ""

    init {
        componentScope.launch {
            session.state.collectLatest { sessionState ->
                val syncedName = sessionState.profile?.displayName ?: sessionState.config.displayName
                _state.update { current ->
                    current.copy(
                        runtimeStatus = sessionState.status,
                        displayName = syncedName,
                        draftName = if (current.draftName.isBlank() || current.draftName == lastSyncedName) {
                            syncedName
                        } else {
                            current.draftName
                        },
                        peerId = sessionState.profile?.peerId.orEmpty(),
                        endpoint = sessionState.endpoint,
                        error = if (sessionState.status == NodeRuntimeStatus.ERROR) {
                            sessionState.errorMessage ?: current.error
                        } else {
                            current.error
                        },
                    )
                }
                if (state.value.selectedTab == ProfileTab.SETTINGS) {
                    settingsStore.syncFromSession()
                }
                lastSyncedName = syncedName
            }
        }
    }

    fun goBack() {
        onBack()
    }

    fun updateName(value: String) {
        _state.update { it.copy(draftName = value) }
    }

    fun selectTab(tab: ProfileTab) {
        _state.update { it.copy(selectedTab = tab, error = null) }
        componentScope.launch {
            refreshTab(tab)
        }
    }

    fun openInvite() {
        onNavigate(Config.Invite)
    }

    fun showMessage(message: String) {
        _state.update { it.copy(message = null) }
        _state.update { it.copy(message = message) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun save() {
        val draft = state.value.draftName.trim()
        if (draft.isEmpty()) {
            _state.update { it.copy(error = "Имя не может быть пустым") }
            return
        }
        componentScope.launch {
            _state.update { it.copy(isSaving = true, error = null, message = null) }
            val config = session.state.value.config.copy(displayName = draft)
            session.updateConfig(config)
            session.restart(config)
            val sessionState = session.state.value
            _state.update {
                it.copy(
                    isSaving = false,
                    message = if (sessionState.status == NodeRuntimeStatus.RUNNING) {
                        "Имя сохранено"
                    } else {
                        null
                    },
                    error = if (sessionState.status == NodeRuntimeStatus.ERROR) {
                        sessionState.errorMessage ?: "Не удалось обновить профиль"
                    } else {
                        null
                    },
                )
            }
        }
    }

    fun openChatForPeer(peerId: String) {
        componentScope.launch {
            val result = session.withNode { node ->
                val conversation = runCatching { node.createDirectChat(peerId) }
                    .getOrElse { node.openConversation(peerId) }
                val title = node.peers()
                    .firstOrNull { it.identity.peerId == peerId }
                    ?.identity
                    ?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: conversation.title
                        .takeIf { it.isNotBlank() }
                    ?: "Чат"
                Config.Chat(conversation.conversationId, peerId, title)
            }
            result.onSuccess { config ->
                onNavigate(config)
            }.onFailure { error ->
                    _state.update { it.copy(error = error.message ?: "Не удалось открыть чат") }
                }
        }
    }

    fun openConversation(conversationId: String, peerId: String? = null, titleHint: String? = null) {
        componentScope.launch {
            onNavigate(resolveChatConfig(conversationId, peerId, titleHint))
        }
    }

    fun openContactsTab() {
        selectTab(ProfileTab.CONTACTS)
    }

    private suspend fun refreshTab(tab: ProfileTab) {
        when (tab) {
            ProfileTab.OVERVIEW -> homeStore.refresh()
            ProfileTab.PROFILE -> Unit
            ProfileTab.CHATS -> chatsStore.refresh()
            ProfileTab.NETWORK -> nearbyStore.refresh()
            ProfileTab.CONTACTS -> contactsStore.refresh()
            ProfileTab.METRICS -> diagnosticsStore.refresh()
            ProfileTab.SETTINGS -> settingsStore.syncFromSession()
        }
    }

    private suspend fun resolveChatConfig(
        conversationId: String,
        fallbackPeerId: String? = null,
        fallbackTitle: String? = null,
    ): Config.Chat {
        return session.withNode { node ->
            val localPeerId = node.profile.peerId
            val conversation = node.conversations().firstOrNull { it.conversationId == conversationId }
            val resolvedPeerId = fallbackPeerId ?: conversation?.participantPeerIds?.firstOrNull { it != localPeerId }
            val resolvedTitle = when {
                !fallbackTitle.isNullOrBlank() -> fallbackTitle
                !resolvedPeerId.isNullOrBlank() -> node.peers()
                    .firstOrNull { it.identity.peerId == resolvedPeerId }
                    ?.identity
                    ?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: conversation?.title
                    ?: resolvedPeerId
                else -> conversation?.title?.takeIf { it.isNotBlank() } ?: "Чат"
            }
            Config.Chat(
                conversationId = conversationId,
                peerId = resolvedPeerId,
                title = resolvedTitle,
            )
        }.getOrElse {
            Config.Chat(
                conversationId = conversationId,
                peerId = fallbackPeerId,
                title = fallbackTitle ?: "Чат",
            )
        }
    }
}
