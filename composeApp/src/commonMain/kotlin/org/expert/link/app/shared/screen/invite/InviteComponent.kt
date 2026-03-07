package org.expert.link.app.shared.screen.invite

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.expert.link.app.shared.navigation.BaseComponent
import org.expert.link.app.shared.navigation.Config
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.platform.PlatformCapabilities
import org.expert.link.app.shared.platform.QrCodeMatrix
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.presentation.NodeSessionController
import org.expert.link.mesh.contract.model.MeshChatType
import org.expert.link.mesh.contract.model.MeshTrustState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class InviteTab {
    CREATE,
    SCAN,
}

data class InviteState(
    val runtimeStatus: NodeRuntimeStatus = NodeRuntimeStatus.STOPPED,
    val selectedTab: InviteTab = InviteTab.CREATE,
    val invite: String = "",
    val scanInput: String = "",
    val isCreating: Boolean = false,
    val isPairing: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class InviteComponent(
    context: ComponentContext,
    onNavigate: (Config) -> Unit,
    onBack: () -> Unit,
) : BaseComponent(context, onNavigate, onBack), KoinComponent {
    private val session: NodeSessionController by inject()
    private val services: AppPlatformServices by inject()

    private val _state = MutableStateFlow(InviteState())
    val state: StateFlow<InviteState> = _state.asStateFlow()

    val capabilities: PlatformCapabilities
        get() = services.capabilities

    init {
        componentScope.launch {
            session.state.collectLatest { sessionState ->
                _state.update {
                    it.copy(
                        runtimeStatus = sessionState.status,
                        error = if (sessionState.status == NodeRuntimeStatus.ERROR) {
                            sessionState.errorMessage ?: it.error
                        } else {
                            it.error
                        },
                    )
                }
            }
        }
    }

    fun goBack() {
        onBack()
    }

    fun selectTab(tab: InviteTab) {
        _state.update { it.copy(selectedTab = tab, error = null, message = null) }
    }

    fun updateScanInput(value: String) {
        _state.update { it.copy(scanInput = value) }
    }

    fun applyScannedInvite(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        _state.update {
            it.copy(
                selectedTab = InviteTab.SCAN,
                scanInput = trimmed,
                message = "Код считан",
                error = null,
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun createInvite() {
        componentScope.launch {
            _state.update { it.copy(isCreating = true, error = null, message = null) }
            val result = session.withNode { node -> node.createPairingInvite() }
            _state.update { current ->
                result.fold(
                    onSuccess = { invite ->
                        current.copy(
                            invite = invite,
                            isCreating = false,
                            message = "Приглашение готово",
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isCreating = false,
                            error = error.message ?: "Не удалось создать приглашение",
                        )
                    },
                )
            }
        }
    }

    fun scanQr() {
        componentScope.launch {
            val scanned = services.scanQr().getOrElse { error ->
                _state.update { it.copy(error = error.message ?: "Не удалось открыть сканер") }
                return@launch
            } ?: return@launch
            applyScannedInvite(scanned)
        }
    }

    fun buildQrCode(): QrCodeMatrix? = state.value.invite.takeIf { it.isNotBlank() }?.let(services::buildQrCode)

    fun copyInvite() {
        val invite = state.value.invite
        if (invite.isBlank()) return
        componentScope.launch {
            val result = services.copyText("Expert Link Invite", invite)
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(message = "Приглашение скопировано", error = null) },
                    onFailure = { error -> current.copy(error = error.message ?: "Не удалось скопировать приглашение") },
                )
            }
        }
    }

    fun pair() {
        val invite = state.value.scanInput.trim()
        if (invite.isEmpty()) {
            _state.update { it.copy(error = "Вставьте приглашение или отсканируйте QR") }
            return
        }
        componentScope.launch {
            _state.update { it.copy(isPairing = true, error = null, message = null) }
            val result = session.withNode { node ->
                val pairingSession = node.pairWithInvite(invite)
                val targetPeerId = pairingSession.remotePeerId
                var pairedPeer = targetPeerId?.let { peerId ->
                    node.peers().firstOrNull {
                        it.identity.peerId == peerId && it.trustState == MeshTrustState.TRUSTED
                    }
                }
                var conversation = targetPeerId?.let { peerId ->
                    node.conversations().firstOrNull { existing ->
                        existing.chatType == MeshChatType.DIRECT &&
                            existing.participantPeerIds.contains(peerId)
                    }
                }
                for (attempt in 0 until 40) {
                    if (pairedPeer != null && conversation != null) break
                    if (attempt > 0) {
                        delay(200)
                    }
                    val peers = node.peers()
                    if (pairedPeer == null && targetPeerId != null) {
                        pairedPeer = peers.firstOrNull {
                            it.identity.peerId == targetPeerId && it.trustState == MeshTrustState.TRUSTED
                        }
                    }
                    if (pairedPeer != null && conversation == null) {
                        val peerId = pairedPeer!!.identity.peerId
                        conversation = node.conversations().firstOrNull { existing ->
                            existing.chatType == MeshChatType.DIRECT &&
                                existing.participantPeerIds.contains(peerId)
                        } ?: runCatching {
                            node.createDirectChat(peerId)
                        }.getOrElse {
                            runCatching { node.openConversation(peerId) }.getOrNull()
                        }
                    }
                }
                if (pairedPeer != null && conversation == null) {
                    val peerId = pairedPeer!!.identity.peerId
                    conversation = runCatching { node.createDirectChat(peerId) }
                        .getOrElse { runCatching { node.openConversation(peerId) }.getOrNull() }
                }
                PairingResult(
                    paired = pairedPeer != null || conversation != null,
                    peerId = pairedPeer?.identity?.peerId,
                    title = pairedPeer?.identity?.displayName
                        ?: conversation?.title?.takeIf { it.isNotBlank() },
                    conversationId = conversation?.conversationId,
                )
            }
            val pairing = result.getOrElse { error ->
                _state.update {
                    it.copy(
                        isPairing = false,
                        error = error.message ?: "Не удалось добавить собеседника",
                    )
                }
                return@launch
            }

            _state.update {
                it.copy(
                    isPairing = false,
                    scanInput = "",
                    message = if (pairing.paired) "Собеседник добавлен" else "Запрос отправлен",
                )
            }

            if (!pairing.paired) {
                onBack()
                return@launch
            }

            val chatConfig = if (pairing.conversationId != null) {
                Config.Chat(
                    conversationId = pairing.conversationId,
                    peerId = pairing.peerId,
                    title = pairing.title?.ifBlank { "Новый чат" } ?: "Новый чат",
                )
            } else {
                null
            }

            onBack()
            if (chatConfig != null) {
                delay(50)
                onNavigate(chatConfig)
            }
        }
    }

    private data class PairingResult(
        val paired: Boolean,
        val peerId: String?,
        val title: String?,
        val conversationId: String?,
    )
}
