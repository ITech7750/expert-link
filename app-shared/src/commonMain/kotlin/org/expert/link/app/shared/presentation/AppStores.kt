package org.expert.link.app.shared.presentation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.expert.link.app.shared.navigation.AppNavigator
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.mesh.contract.api.MeshCallSignalCommand
import org.expert.link.mesh.contract.api.MeshChatCommand
import org.expert.link.mesh.contract.api.MeshFileTransferCommand
import org.expert.link.mesh.contract.api.MeshHangupCallCommand
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.api.MeshStartCallCommand
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.model.MeshBlockedPeer
import org.expert.link.mesh.contract.model.MeshCallSession
import org.expert.link.mesh.contract.model.MeshCallSignalType
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshConversation
import org.expert.link.mesh.contract.model.MeshEventLogEntry
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshLocalProfile
import org.expert.link.mesh.contract.model.MeshMetricSnapshot
import org.expert.link.mesh.contract.model.MeshMessageReceipt
import org.expert.link.mesh.contract.model.MeshNearbyPeer
import org.expert.link.mesh.contract.model.MeshPairingSession
import org.expert.link.mesh.contract.model.MeshPairedPeer
import org.expert.link.mesh.contract.model.MeshPeerEndpoint
import org.expert.link.mesh.contract.model.MeshRelayStatus
import org.expert.link.mesh.contract.model.MeshRouteInfo
import org.expert.link.mesh.contract.model.MeshRoutingPlan
import org.expert.link.mesh.contract.model.MeshTrustState

/** Статус runtime узла в UI. */
enum class NodeRuntimeStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR,
}

data class NodeSessionState(
    val platformName: String,
    val transportHint: String,
    val status: NodeRuntimeStatus = NodeRuntimeStatus.STOPPED,
    val config: MeshNodeConfig,
    val profile: MeshLocalProfile? = null,
    val endpoint: MeshPeerEndpoint? = null,
    val errorMessage: String? = null,
)

class NodeSessionController(
    private val services: AppPlatformServices,
) {
    private val mutex = Mutex()
    private var node: MeshNode? = null
    private val _state = MutableStateFlow(
        NodeSessionState(
            platformName = services.platformName,
            transportHint = services.transportHint,
            config = services.defaultConfig(),
        ),
    )
    val state: StateFlow<NodeSessionState> = _state.asStateFlow()

    suspend fun start(config: MeshNodeConfig = state.value.config) {
        _state.update { it.copy(status = NodeRuntimeStatus.STARTING, config = config, errorMessage = null) }
        runCatching {
            services.launchNode(config)
        }.onSuccess { createdNode ->
            mutex.withLock { node = createdNode }
            _state.update {
                it.copy(
                    status = NodeRuntimeStatus.RUNNING,
                    config = config,
                    profile = createdNode.profile,
                    endpoint = createdNode.endpoint,
                    errorMessage = null,
                )
            }
        }.onFailure { error ->
            mutex.withLock { node = null }
            _state.update {
                it.copy(
                    status = NodeRuntimeStatus.ERROR,
                    config = config,
                    errorMessage = error.message ?: "Не удалось запустить узел",
                )
            }
        }
    }

    private suspend fun shutdownNode() {
        val current = mutex.withLock {
            val existing = node
            node = null
            existing
        }
        current?.stop()
    }

    suspend fun stopNode() {
        _state.update { it.copy(status = NodeRuntimeStatus.STARTING, errorMessage = null) }
        runCatching { shutdownNode() }
        _state.update {
            it.copy(
                status = NodeRuntimeStatus.STOPPED,
                profile = null,
                endpoint = null,
                errorMessage = null,
            )
        }
    }

    suspend fun restart(config: MeshNodeConfig = state.value.config) {
        stopNode()
        start(config)
    }

    fun updateConfig(config: MeshNodeConfig) {
        _state.update { it.copy(config = config) }
    }

    suspend fun <T> withNode(block: suspend (MeshNode) -> T): Result<T> {
        val current = mutex.withLock { node }
        return if (current == null) {
            Result.failure(IllegalStateException("Узел не запущен"))
        } else {
            runCatching { block(current) }
        }
    }
}

data class HomeState(
    val loading: Boolean = false,
    val error: String? = null,
    val metrics: MeshMetricSnapshot? = null,
    val relayStatus: MeshRelayStatus? = null,
    val nearbyCount: Int = 0,
    val pairingCount: Int = 0,
    val peersCount: Int = 0,
    val conversationsCount: Int = 0,
    val transfersCount: Int = 0,
    val callsCount: Int = 0,
)

class HomeStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    suspend fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        val result = session.withNode { node ->
            HomeState(
                metrics = node.metrics(),
                relayStatus = node.relayStatus(),
                nearbyCount = node.nearbyPeers().size,
                pairingCount = node.pairingSessions().size,
                peersCount = node.peers().size,
                conversationsCount = node.conversations().size,
                transfersCount = node.fileTransfers().size,
                callsCount = node.callSessions().size,
            )
        }
        _state.value = result.getOrElse { HomeState(error = it.message ?: "Не удалось загрузить сводку") }
    }
}

data class PairingState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val invite: String = "",
    val importInvite: String = "",
    val sessions: List<MeshPairingSession> = emptyList(),
    val peers: List<MeshPairedPeer> = emptyList(),
)

class PairingStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(PairingState())
    val state: StateFlow<PairingState> = _state.asStateFlow()

    fun updateInviteInput(value: String) {
        _state.update { it.copy(importInvite = value) }
    }

    suspend fun refresh() {
        val result = session.withNode { node ->
            PairingState(
                invite = state.value.invite,
                importInvite = state.value.importInvite,
                sessions = node.pairingSessions(),
                peers = node.peers(),
            )
        }
        _state.value = result.getOrElse {
            state.value.copy(error = it.message ?: "Не удалось обновить данные")
        }
    }

    suspend fun createInvite() {
        _state.update { it.copy(loading = true, error = null, message = null) }
        val result = session.withNode { it.createPairingInvite() }
        _state.update {
            result.fold(
                onSuccess = { invite -> it.copy(loading = false, invite = invite, message = "Приглашение готово") },
                onFailure = { error -> it.copy(loading = false, error = error.message ?: "Не удалось создать приглашение") },
            )
        }
    }

    suspend fun pair() {
        val invite = state.value.importInvite.trim()
        if (invite.isEmpty()) {
            _state.update { it.copy(error = "Вставьте приглашение") }
            return
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        val result = session.withNode { node ->
            val sessionResult = node.pairWithInvite(invite)
            Triple(sessionResult, node.peers(), node.pairingSessions())
        }
        _state.update {
            result.fold(
                onSuccess = { (pairingSession, peers, sessions) ->
                    val trustedPeerAdded = peers.any { peer ->
                        peer.trustState == MeshTrustState.TRUSTED &&
                            (pairingSession.remotePeerId == null || peer.identity.peerId == pairingSession.remotePeerId)
                    }
                    it.copy(
                        loading = false,
                        message = if (trustedPeerAdded) "Сопряжение завершено" else "Запрос отправлен",
                        peers = peers,
                        sessions = sessions,
                        importInvite = "",
                    )
                },
                onFailure = { error -> it.copy(loading = false, error = error.message ?: "Не удалось выполнить сопряжение") },
            )
        }
    }
}

data class NearbyState(
    val loading: Boolean = false,
    val error: String? = null,
    val queryPeerId: String = "",
    val nearby: List<MeshNearbyPeer> = emptyList(),
    val routes: List<MeshRouteInfo> = emptyList(),
    val routingPlan: MeshRoutingPlan? = null,
    val trustedPeerIds: Set<String> = emptySet(),
    val trustedPeerNames: Map<String, String> = emptyMap(),
)

class NearbyStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(NearbyState())
    val state: StateFlow<NearbyState> = _state.asStateFlow()

    fun updateQueryPeerId(value: String) {
        _state.update { it.copy(queryPeerId = value) }
    }

    fun selectPeer(peerId: String) {
        _state.update { it.copy(queryPeerId = peerId) }
    }

    suspend fun refresh(selectedPeerId: String? = state.value.queryPeerId.takeIf { it.isNotBlank() }) {
        val result = session.withNode { node ->
            val peers = node.peers()
            NearbyState(
                queryPeerId = state.value.queryPeerId,
                nearby = node.nearbyPeers(),
                routes = node.routes(),
                routingPlan = selectedPeerId?.takeIf { it.isNotBlank() }?.let { node.routingPlan(it) },
                trustedPeerIds = peers.map { it.identity.peerId }.toSet(),
                trustedPeerNames = peers.associate { it.identity.peerId to it.identity.displayName },
            )
        }
        _state.value = result.getOrElse { state.value.copy(error = it.message ?: "Не удалось загрузить узлы") }
    }

    suspend fun announcePresence() {
        session.withNode { it.announcePresence() }
        refresh()
    }

    suspend fun discoverPeer() {
        val peerId = state.value.queryPeerId.trim()
        if (peerId.isEmpty()) {
            _state.update { it.copy(error = "Введите peerId") }
            return
        }
        session.withNode { it.discoverPeer(peerId) }
        refresh(peerId)
    }
}

data class ContactsState(
    val loading: Boolean = false,
    val error: String? = null,
    val peers: List<MeshPairedPeer> = emptyList(),
    val blockedPeers: List<MeshBlockedPeer> = emptyList(),
)

class ContactsStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(ContactsState())
    val state: StateFlow<ContactsState> = _state.asStateFlow()

    suspend fun refresh() {
        val result = session.withNode { node ->
            ContactsState(
                peers = node.peers(),
                blockedPeers = node.blockedPeers(),
            )
        }
        _state.value = result.getOrElse { state.value.copy(error = it.message ?: "Не удалось загрузить контакты") }
    }

    suspend fun block(peerId: String) {
        session.withNode { it.blockPeer(peerId, "Заблокировано из клиента") }
        refresh()
    }

    suspend fun unblock(peerId: String) {
        session.withNode { it.unblockPeer(peerId) }
        refresh()
    }
}

data class ChatsState(
    val loading: Boolean = false,
    val error: String? = null,
    val newPeerId: String = "",
    val localPeerId: String? = null,
    val peers: List<MeshPairedPeer> = emptyList(),
    val conversations: List<MeshConversation> = emptyList(),
)

class ChatsStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(ChatsState())
    val state: StateFlow<ChatsState> = _state.asStateFlow()

    fun updateNewPeerId(value: String) {
        _state.update { it.copy(newPeerId = value) }
    }

    fun prefillPeerId(peerId: String) {
        _state.update { it.copy(newPeerId = peerId) }
    }

    suspend fun refresh() {
        val result = session.withNode { node ->
            ChatsState(
                newPeerId = state.value.newPeerId,
                localPeerId = node.profile.peerId,
                peers = node.peers(),
                conversations = node.conversations(),
            )
        }
        _state.value = result.getOrElse { state.value.copy(error = it.message ?: "Не удалось загрузить чаты") }
    }

    suspend fun openConversation(peerId: String = state.value.newPeerId.trim()): Result<MeshConversation> {
        if (peerId.isBlank()) {
            return Result.failure(IllegalArgumentException("Введите peerId"))
        }
        val result = session.withNode { it.openConversation(peerId) }
        refresh()
        return result
    }
}

data class ChatState(
    val loading: Boolean = false,
    val error: String? = null,
    val conversationId: String = "",
    val peerId: String = "",
    val draft: String = "",
    val messages: List<MeshChatMessage> = emptyList(),
    val receipts: List<MeshMessageReceipt> = emptyList(),
    val route: MeshRoutingPlan? = null,
)

class ChatStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun bind(conversationId: String, peerId: String) {
        _state.update { it.copy(conversationId = conversationId, peerId = peerId) }
    }

    fun updateDraft(value: String) {
        _state.update { it.copy(draft = value) }
    }

    suspend fun refresh() {
        val current = state.value
        if (current.conversationId.isBlank()) return
        val result = session.withNode { node ->
            ChatState(
                conversationId = current.conversationId,
                peerId = current.peerId,
                draft = current.draft,
                messages = node.messages(current.conversationId),
                receipts = node.messageReceipts(50).filter { it.conversationId == current.conversationId },
                route = current.peerId.takeIf { it.isNotBlank() }?.let { node.routingPlan(it) },
            )
        }
        _state.value = result.getOrElse { current.copy(error = it.message ?: "Не удалось загрузить диалог") }
    }

    suspend fun send() {
        val current = state.value
        val body = current.draft.trim()
        if (body.isEmpty()) {
            _state.update { it.copy(error = "Введите сообщение") }
            return
        }
        val result = session.withNode {
            it.sendChat(MeshChatCommand(targetPeerId = current.peerId, body = body, conversationId = current.conversationId))
        }
        _state.update {
            result.fold(
                onSuccess = { current.copy(draft = "", error = null) },
                onFailure = { current.copy(error = it.message ?: "Не удалось отправить сообщение") },
            )
        }
        refresh()
    }

    suspend fun resend(message: MeshChatMessage) {
        _state.update { it.copy(draft = message.body) }
        send()
    }
}

data class TransfersState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val targetPeerId: String = "",
    val path: String = "",
    val conversationId: String = "",
    val transfers: List<MeshFileTransferSession> = emptyList(),
)

class TransfersStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(TransfersState())
    val state: StateFlow<TransfersState> = _state.asStateFlow()

    fun updateTargetPeerId(value: String) { _state.update { it.copy(targetPeerId = value) } }
    fun updatePath(value: String) { _state.update { it.copy(path = value) } }
    fun updateConversationId(value: String) { _state.update { it.copy(conversationId = value) } }

    fun prefill(targetPeerId: String, conversationId: String? = null) {
        _state.update {
            it.copy(
                targetPeerId = targetPeerId,
                conversationId = conversationId ?: it.conversationId,
                message = null,
                error = null,
            )
        }
    }

    suspend fun refresh() {
        val result = session.withNode { node ->
            state.value.copy(transfers = node.fileTransfers(), error = null)
        }
        _state.value = result.getOrElse { state.value.copy(error = it.message ?: "Не удалось загрузить передачи") }
    }

    suspend fun send() {
        val current = state.value
        if (current.targetPeerId.isBlank() || current.path.isBlank()) {
            _state.update { it.copy(error = "Укажите peerId и путь") }
            return
        }
        val result = session.withNode {
            it.sendFile(
                MeshFileTransferCommand(
                    targetPeerId = current.targetPeerId.trim(),
                    path = current.path.trim(),
                    conversationId = current.conversationId.trim().ifBlank { null },
                ),
            )
        }
        _state.update {
            result.fold(
                onSuccess = { _ -> current.copy(message = "Передача запущена", error = null, path = "") },
                onFailure = { error -> it.copy(error = error.message ?: "Не удалось отправить файл") },
            )
        }
        refresh()
    }

    suspend fun resume(transferId: String) {
        session.withNode { it.resumeFileTransfer(transferId) }
        refresh()
    }

    suspend fun cancel(transferId: String) {
        session.withNode { it.cancelFileTransfer(transferId) }
        refresh()
    }
}

data class CallsState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val targetPeerId: String = "",
    val offer: String = "demo-offer",
    val signalPayload: String = "ok",
    val sessions: List<MeshCallSession> = emptyList(),
)

class CallsStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(CallsState())
    val state: StateFlow<CallsState> = _state.asStateFlow()

    fun updateTargetPeerId(value: String) { _state.update { it.copy(targetPeerId = value) } }
    fun updateOffer(value: String) { _state.update { it.copy(offer = value) } }
    fun updateSignalPayload(value: String) { _state.update { it.copy(signalPayload = value) } }

    fun prefill(targetPeerId: String) {
        _state.update { it.copy(targetPeerId = targetPeerId, error = null, message = null) }
    }

    suspend fun refresh() {
        val result = session.withNode { node ->
            state.value.copy(sessions = node.callSessions(), error = null)
        }
        _state.value = result.getOrElse { state.value.copy(error = it.message ?: "Не удалось загрузить звонки") }
    }

    suspend fun startCall() {
        val current = state.value
        if (current.targetPeerId.isBlank()) {
            _state.update { it.copy(error = "Введите peerId") }
            return
        }
        val result = session.withNode {
            it.startCall(MeshStartCallCommand(targetPeerId = current.targetPeerId.trim(), offer = current.offer))
        }
        _state.update {
            result.fold(
                onSuccess = { _ -> current.copy(message = "Вызов отправлен", error = null) },
                onFailure = { error -> it.copy(error = error.message ?: "Не удалось начать звонок") },
            )
        }
        refresh()
    }

    suspend fun accept(call: MeshCallSession) {
        session.withNode {
            it.sendCallSignal(
                MeshCallSignalCommand(
                    callId = call.callId,
                    recipientPeerId = call.initiatorPeerId,
                    signalType = MeshCallSignalType.ACCEPTED,
                    payload = state.value.signalPayload,
                ),
            )
        }
        refresh()
    }

    suspend fun reject(call: MeshCallSession) {
        session.withNode {
            it.sendCallSignal(
                MeshCallSignalCommand(
                    callId = call.callId,
                    recipientPeerId = call.initiatorPeerId,
                    signalType = MeshCallSignalType.REJECTED,
                    payload = "отклонено",
                ),
            )
        }
        refresh()
    }

    suspend fun sendQuality(call: MeshCallSession) {
        val recipient = if (call.recipientPeerId == session.state.value.profile?.peerId) call.initiatorPeerId else call.recipientPeerId
        session.withNode {
            it.sendCallSignal(
                MeshCallSignalCommand(
                    callId = call.callId,
                    recipientPeerId = recipient,
                    signalType = MeshCallSignalType.QUALITY,
                    payload = state.value.signalPayload,
                ),
            )
        }
        refresh()
    }

    suspend fun hangup(call: MeshCallSession) {
        val recipient = if (call.recipientPeerId == session.state.value.profile?.peerId) call.initiatorPeerId else call.recipientPeerId
        session.withNode {
            it.hangupCall(MeshHangupCallCommand(callId = call.callId, recipientPeerId = recipient, reason = "Завершено"))
        }
        refresh()
    }
}

data class DiagnosticsState(
    val loading: Boolean = false,
    val error: String? = null,
    val events: List<MeshEventLogEntry> = emptyList(),
    val metrics: MeshMetricSnapshot? = null,
    val routes: List<MeshRouteInfo> = emptyList(),
    val nearby: List<MeshNearbyPeer> = emptyList(),
    val relayStatus: MeshRelayStatus? = null,
)

class DiagnosticsStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = _state.asStateFlow()

    suspend fun refresh() {
        val result = session.withNode { node ->
            DiagnosticsState(
                events = node.recentEvents(24),
                metrics = node.metrics(),
                routes = node.routes(),
                nearby = node.nearbyPeers(),
                relayStatus = node.relayStatus(),
            )
        }
        _state.value = result.getOrElse { state.value.copy(error = it.message ?: "Не удалось загрузить диагностику") }
    }
}

data class ProfileState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val profile: MeshLocalProfile? = null,
    val endpoint: MeshPeerEndpoint? = null,
    val invite: String = "",
)

class ProfileStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    suspend fun refresh() {
        val current = session.state.value
        _state.update {
            it.copy(
                profile = current.profile,
                endpoint = current.endpoint,
                message = null,
                error = if (current.status == NodeRuntimeStatus.ERROR) current.errorMessage else null,
            )
        }
    }

    suspend fun createInvite() {
        val result = session.withNode { it.createPairingInvite() }
        _state.update {
            result.fold(
                onSuccess = { invite -> it.copy(invite = invite, message = "Приглашение готово", error = null) },
                onFailure = { error -> it.copy(error = error.message ?: "Не удалось создать приглашение") },
            )
        }
    }
}

data class SettingsState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val config: MeshNodeConfig,
)

class SettingsStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(SettingsState(config = session.state.value.config))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun syncFromSession() {
        _state.update { it.copy(config = session.state.value.config, error = null) }
    }

    fun updateConfig(transform: (MeshNodeConfig) -> MeshNodeConfig) {
        _state.update { current -> current.copy(config = transform(current.config)) }
    }

    suspend fun applyAndRestart() {
        _state.update { it.copy(loading = true, error = null, message = null) }
        val config = state.value.config
        session.updateConfig(config)
        session.restart(config)
        _state.update {
            it.copy(
                loading = false,
                message = if (session.state.value.status == NodeRuntimeStatus.RUNNING) "Узел обновлён" else null,
                error = session.state.value.errorMessage,
            )
        }
    }

    suspend fun stopNode() {
        session.stopNode()
        syncFromSession()
        _state.update { it.copy(message = "Узел остановлен") }
    }
}

/** Корневой контейнер presentation-слоя. */
class ExpertLinkAppStore(
    services: AppPlatformServices,
) {
    val navigator = AppNavigator()
    val session = NodeSessionController(services)
    val home = HomeStore(session)
    val pairing = PairingStore(session)
    val nearby = NearbyStore(session)
    val contacts = ContactsStore(session)
    val chats = ChatsStore(session)
    val chat = ChatStore(session)
    val transfers = TransfersStore(session)
    val calls = CallsStore(session)
    val diagnostics = DiagnosticsStore(session)
    val profile = ProfileStore(session)
    val settings = SettingsStore(session)
}
