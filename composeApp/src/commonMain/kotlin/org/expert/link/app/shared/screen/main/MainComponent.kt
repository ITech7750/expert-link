package org.expert.link.app.shared.screen.main

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.expert.link.app.shared.presentation.HybridStore
import org.expert.link.app.shared.presentation.NearbyStore
import org.expert.link.app.shared.presentation.NodeSessionController
import org.expert.link.app.shared.presentation.PairingStore
import org.expert.link.app.shared.presentation.ProfileStore
import org.expert.link.app.shared.presentation.SettingsStore
import org.expert.link.app.shared.presentation.TransfersStore
import org.expert.link.app.shared.screen.inventory.InventoryComponent
import org.expert.link.app.shared.screen.profile.ProfileComponent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class MainSection(val title: String) {
    HOME("Главная"),
    ORGANIZATIONS("Организации"),
    EQUIPMENT("Оборудование"),
    SESSIONS("Учёт"),
    REPORTS("Отчёты"),
    CHATS("Чаты"),
    TRANSFERS("Передачи"),
    DIAGNOSTICS("Диагностика"),
    PROFILE("Профиль"),
    SETTINGS("Настройки"),
}

data class MainState(
    val selectedSection: MainSection = MainSection.HOME,
)

class MainComponent(
    context: ComponentContext,
    onNavigate: (Config) -> Unit,
    onBack: () -> Unit,
) : BaseComponent(context, onNavigate, onBack), KoinComponent {
    private val session: NodeSessionController by inject()
    private val services: AppPlatformServices by inject()

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    val sessionState = session.state
    val homeStore = HomeStore(session)
    val chatsStore = ChatsStore(session)
    val contactsStore = ContactsStore(session)
    val nearbyStore = NearbyStore(session)
    val pairingStore = PairingStore(session)
    val transfersStore = TransfersStore(session)
    val callsStore = CallsStore(session)
    val diagnosticsStore = DiagnosticsStore(session)
    val profileStore = ProfileStore(session)
    val hybridStore = HybridStore(session)
    val settingsStore = SettingsStore(session)
    val inventoryComponent = InventoryComponent(context, onNavigate, onBack)
    val profileComponent = ProfileComponent(context, onNavigate, onBack)

    val platformServices: AppPlatformServices
        get() = services

    fun selectSection(section: MainSection) {
        _state.update { it.copy(selectedSection = section) }
        when (section) {
            MainSection.ORGANIZATIONS -> inventoryComponent.selectSection(
                org.expert.link.app.shared.presentation.InventorySection.ORGANIZATIONS
            )
            MainSection.EQUIPMENT -> inventoryComponent.selectSection(
                org.expert.link.app.shared.presentation.InventorySection.ITEMS
            )
            MainSection.SESSIONS -> inventoryComponent.selectSection(
                org.expert.link.app.shared.presentation.InventorySection.SESSIONS
            )
            MainSection.REPORTS -> inventoryComponent.selectSection(
                org.expert.link.app.shared.presentation.InventorySection.REPORTS
            )
            else -> Unit
        }
    }

    fun openInvite() {
        onNavigate(Config.Invite)
    }

    fun openScanner() {
        onNavigate(Config.InventoryScanner)
    }

    fun openChatList() {
        onNavigate(Config.ChatList)
    }

    fun openItem(itemId: String) {
        onNavigate(Config.InventoryItem(itemId))
    }

    fun openSession(sessionId: String) {
        onNavigate(Config.InventorySession(sessionId))
    }

    fun openConversation(conversationId: String, peerId: String? = null, titleHint: String? = null) {
        componentScope.launch {
            resolveChatConfig(conversationId, peerId, titleHint)
                .onSuccess { onNavigate(it) }
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
                    ?: conversation.title.takeIf { it.isNotBlank() }
                    ?: "Чат"
                Config.Chat(conversation.conversationId, peerId, title)
            }
            result.onSuccess { onNavigate(it) }
        }
    }

    private suspend fun resolveChatConfig(
        conversationId: String,
        fallbackPeerId: String? = null,
        fallbackTitle: String? = null,
    ): Result<Config.Chat> {
        return session.withNode { node ->
            val localPeerId = node.profile.peerId
            val conversation = node.conversations().firstOrNull { it.conversationId == conversationId }
            val peerId = conversation?.participantPeerIds?.firstOrNull { it != localPeerId } ?: fallbackPeerId
            val title = when {
                conversation?.title?.isNotBlank() == true -> conversation.title
                !fallbackTitle.isNullOrBlank() -> fallbackTitle
                !peerId.isNullOrBlank() -> node.peers()
                    .firstOrNull { it.identity.peerId == peerId }
                    ?.identity
                    ?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: peerId
                else -> "Чат"
            }
            Config.Chat(conversationId, peerId, title)
        }
    }
}
