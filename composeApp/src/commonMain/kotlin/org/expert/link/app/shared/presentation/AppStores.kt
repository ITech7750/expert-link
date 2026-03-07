package org.expert.link.app.shared.presentation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import org.expert.link.app.shared.navigation.AppNavigator
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.mesh.contract.api.MeshCallSignalCommand
import org.expert.link.mesh.contract.api.MeshAcceptCallCommand
import org.expert.link.mesh.contract.api.MeshChatMemberCommand
import org.expert.link.mesh.contract.api.MeshCreateGroupChatCommand
import org.expert.link.mesh.contract.api.MeshCreateThreadCommand
import org.expert.link.mesh.contract.api.MeshEndCallCommand
import org.expert.link.mesh.contract.api.MeshFileTransferCommand
import org.expert.link.mesh.contract.api.MeshHangupCallCommand
import org.expert.link.mesh.contract.api.MeshJoinCallCommand
import org.expert.link.mesh.contract.api.MeshLeaveCallCommand
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.api.MeshRejectCallCommand
import org.expert.link.mesh.contract.api.MeshSendMessageCommand
import org.expert.link.mesh.contract.api.MeshSendThreadMessageCommand
import org.expert.link.mesh.contract.api.MeshStartCallCommand
import org.expert.link.mesh.contract.api.MeshStartGroupCallCommand
import org.expert.link.mesh.contract.api.MeshToggleCameraCommand
import org.expert.link.mesh.contract.api.MeshToggleMicrophoneCommand
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.model.MeshBlockedPeer
import org.expert.link.mesh.contract.model.MeshCallEvent
import org.expert.link.mesh.contract.model.MeshCallMediaState
import org.expert.link.mesh.contract.model.MeshCallParticipant
import org.expert.link.mesh.contract.model.MeshCallSession
import org.expert.link.mesh.contract.model.MeshCallSignalType
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshChatSummary
import org.expert.link.mesh.contract.model.MeshConversation
import org.expert.link.mesh.contract.model.MeshEventLogEntry
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshGroupChat
import org.expert.link.mesh.contract.model.MeshGroupEvent
import org.expert.link.mesh.contract.model.MeshLocalProfile
import org.expert.link.mesh.contract.model.MeshMetricSnapshot
import org.expert.link.mesh.contract.model.MeshMediaStats
import org.expert.link.mesh.contract.model.MeshMessageReceipt
import org.expert.link.mesh.contract.model.MeshNearbyPeer
import org.expert.link.mesh.contract.model.MeshPairingSession
import org.expert.link.mesh.contract.model.MeshPairedPeer
import org.expert.link.mesh.contract.model.MeshPeerEndpoint
import org.expert.link.mesh.contract.model.MeshConnectivityStrategy
import org.expert.link.mesh.contract.model.MeshNetworkRoleState
import org.expert.link.mesh.contract.model.MeshRelayStatus
import org.expert.link.mesh.contract.model.MeshRelayMode
import org.expert.link.mesh.contract.model.MeshRouteInfo
import org.expert.link.mesh.contract.model.MeshRouteHealth
import org.expert.link.mesh.contract.model.MeshRoutingPlan
import org.expert.link.mesh.contract.model.MeshThread
import org.expert.link.mesh.contract.model.MeshThreadMessage
import org.expert.link.mesh.contract.model.MeshThreadSummary
import org.expert.link.mesh.contract.model.MeshTopologyState
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
    val topology: MeshTopologyState? = null,
    val relayMode: MeshRelayMode? = null,
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
                relayMode = node.relayModeState(),
                topology = node.observeTopologyState(),
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
    val message: String? = null,
    val queryPeerId: String = "",
    val manualPeerId: String = "",
    val manualHost: String = "",
    val manualPort: String = "",
    val manualPath: String = "/api/v1/packets",
    val nearby: List<MeshNearbyPeer> = emptyList(),
    val routes: List<MeshRouteInfo> = emptyList(),
    val routeHealth: List<MeshRouteHealth> = emptyList(),
    val hostRole: MeshNetworkRoleState? = null,
    val connectivityStrategy: MeshConnectivityStrategy? = null,
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

    fun updateManualPeerId(value: String) {
        _state.update { it.copy(manualPeerId = value) }
    }

    fun updateManualHost(value: String) {
        _state.update { it.copy(manualHost = value) }
    }

    fun updateManualPort(value: String) {
        _state.update { it.copy(manualPort = value) }
    }

    fun updateManualPath(value: String) {
        _state.update { it.copy(manualPath = value) }
    }

    fun selectPeer(peerId: String) {
        _state.update { it.copy(queryPeerId = peerId) }
    }

    suspend fun refresh(selectedPeerId: String? = state.value.queryPeerId.takeIf { it.isNotBlank() }) {
        val result = session.withNode { node ->
            val peers = node.peers()
            NearbyState(
                message = state.value.message,
                queryPeerId = state.value.queryPeerId,
                manualPeerId = state.value.manualPeerId,
                manualHost = state.value.manualHost,
                manualPort = state.value.manualPort,
                manualPath = state.value.manualPath,
                nearby = node.nearbyPeers(),
                routes = node.routes(),
                routeHealth = node.inspectRouteHealth(),
                hostRole = node.observeHostRole(),
                connectivityStrategy = selectedPeerId?.takeIf { it.isNotBlank() }?.let { node.observeConnectivityStrategy(it) },
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

    suspend fun forceTopologyRefresh() {
        val result = session.withNode { it.forceTopologyRefresh() }
        _state.update {
            result.fold(
                onSuccess = { _ -> it.copy(message = "Сеть пересобрана", error = null) },
                onFailure = { error -> it.copy(error = error.message ?: "Не удалось пересобрать сеть", message = null) },
            )
        }
        refresh()
    }

    suspend fun discoverPeer() {
        val peerId = state.value.queryPeerId.trim()
        if (peerId.isEmpty()) {
            _state.update { it.copy(error = "Введите ID узла") }
            return
        }
        session.withNode { it.discoverPeer(peerId) }
        refresh(peerId)
    }

    suspend fun rememberPeerEndpoint() {
        val current = state.value
        val peerId = current.manualPeerId.trim()
        val host = current.manualHost.trim()
        val port = current.manualPort.trim().toIntOrNull()
        if (peerId.isBlank() || host.isBlank() || port == null) {
            _state.update { it.copy(error = "Укажите ID узла, адрес и порт") }
            return
        }
        val result = session.withNode { node ->
            node.rememberPeerEndpoint(
                peerId = peerId,
                endpoint = MeshPeerEndpoint(
                    scheme = "http",
                    host = host,
                    port = port,
                    path = current.manualPath.trim().ifBlank { "/api/v1/packets" },
                    announcedAt = Clock.System.now(),
                ),
            )
        }
        _state.update {
            result.fold(
                onSuccess = { _ -> it.copy(message = "Узел добавлен вручную", error = null, queryPeerId = peerId) },
                onFailure = { error -> it.copy(error = error.message ?: "Не удалось добавить узел", message = null) },
            )
        }
        refresh(peerId)
    }

    suspend fun forgetPeerEndpoint() {
        val peerId = state.value.manualPeerId.trim()
        if (peerId.isBlank()) {
            _state.update { it.copy(error = "Введите ID узла для удаления") }
            return
        }
        val result = session.withNode { it.forgetPeerEndpoint(peerId) }
        _state.update {
            result.fold(
                onSuccess = { _ -> it.copy(message = "Узел удалён из кэша", error = null, queryPeerId = peerId) },
                onFailure = { error -> it.copy(error = error.message ?: "Не удалось удалить узел", message = null) },
            )
        }
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
    val newGroupTitle: String = "",
    val newGroupDescription: String = "",
    val newGroupMembersInput: String = "",
    val localPeerId: String? = null,
    val peers: List<MeshPairedPeer> = emptyList(),
    val conversations: List<MeshConversation> = emptyList(),
    val groupChats: List<MeshGroupChat> = emptyList(),
    val chatSummaries: List<MeshChatSummary> = emptyList(),
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

    fun updateNewGroupTitle(value: String) {
        _state.update { it.copy(newGroupTitle = value) }
    }

    fun updateNewGroupDescription(value: String) {
        _state.update { it.copy(newGroupDescription = value) }
    }

    fun updateNewGroupMembersInput(value: String) {
        _state.update { it.copy(newGroupMembersInput = value) }
    }

    suspend fun refresh() {
        val result = session.withNode { node ->
            ChatsState(
                newPeerId = state.value.newPeerId,
                newGroupTitle = state.value.newGroupTitle,
                newGroupDescription = state.value.newGroupDescription,
                newGroupMembersInput = state.value.newGroupMembersInput,
                localPeerId = node.profile.peerId,
                peers = node.peers(),
                conversations = node.conversations(),
                groupChats = node.groupChats(),
                chatSummaries = node.chatSummaries(),
            )
        }
        _state.value = result.getOrElse { state.value.copy(error = it.message ?: "Не удалось загрузить чаты") }
    }

    suspend fun openConversation(peerId: String = state.value.newPeerId.trim()): Result<MeshConversation> {
        if (peerId.isBlank()) {
            return Result.failure(IllegalArgumentException("Введите ID узла"))
        }
        val result = session.withNode { node ->
            val created = runCatching { node.createDirectChat(peerId) }
                .getOrElse { node.openConversation(peerId) }
            runCatching { node.openConversation(peerId) }.getOrElse { created }
        }
        refresh()
        return result
    }

    suspend fun createGroupChat(): Result<MeshConversation> {
        val title = state.value.newGroupTitle.trim()
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("Введите название группы"))
        }
        val participantIds = state.value.newGroupMembersInput
            .split(',', ';', '\n', ' ')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        val result = session.withNode { node ->
            node.createGroupChat(
                MeshCreateGroupChatCommand(
                    title = title,
                    description = state.value.newGroupDescription.trim().ifBlank { null },
                    participantPeerIds = participantIds,
                ),
            )
        }
        if (result.isSuccess) {
            _state.update { it.copy(newGroupTitle = "", newGroupDescription = "", newGroupMembersInput = "") }
        }
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
    val threadSummaries: List<MeshThreadSummary> = emptyList(),
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
                threadSummaries = node.threadUpdates(current.conversationId),
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
        val result = session.withNode { node ->
            if (current.peerId.isNotBlank()) {
                node.sendChat(
                    org.expert.link.mesh.contract.api.MeshChatCommand(
                        targetPeerId = current.peerId,
                        body = body,
                        conversationId = current.conversationId,
                    ),
                )
            } else {
                node.sendMessage(MeshSendMessageCommand(chatId = current.conversationId, body = body))
            }
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

    suspend fun createThread(rootMessageId: String): Result<MeshThread> {
        return session.withNode { node ->
            node.createThread(MeshCreateThreadCommand(chatId = state.value.conversationId, rootMessageId = rootMessageId))
        }
    }
}

data class GroupState(
    val loading: Boolean = false,
    val error: String? = null,
    val chatId: String = "",
    val titleDraft: String = "",
    val participantDraft: String = "",
    val group: MeshGroupChat? = null,
    val events: List<MeshGroupEvent> = emptyList(),
    val messages: List<MeshChatMessage> = emptyList(),
)

class GroupStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(GroupState())
    val state: StateFlow<GroupState> = _state.asStateFlow()

    fun bind(chatId: String) {
        _state.update { it.copy(chatId = chatId) }
    }

    fun updateTitleDraft(value: String) {
        _state.update { it.copy(titleDraft = value) }
    }

    fun updateParticipantDraft(value: String) {
        _state.update { it.copy(participantDraft = value) }
    }

    suspend fun refresh() {
        val chatId = state.value.chatId
        if (chatId.isBlank()) return
        val result = session.withNode { node ->
            val group = node.groupChats().firstOrNull { it.chatId == chatId }
            GroupState(
                chatId = chatId,
                titleDraft = state.value.titleDraft,
                participantDraft = state.value.participantDraft,
                group = group,
                events = node.groupEvents(chatId, 50),
                messages = node.groupMessages(chatId),
            )
        }
        _state.value = result.getOrElse { state.value.copy(error = it.message ?: "Не удалось загрузить группу") }
    }

    suspend fun rename() {
        val title = state.value.titleDraft.trim()
        if (title.isBlank()) {
            _state.update { it.copy(error = "Введите новое название") }
            return
        }
        val chatId = state.value.chatId
        session.withNode { it.renameChat(chatId, title) }
        _state.update { it.copy(titleDraft = "") }
        refresh()
    }

    suspend fun addParticipant() {
        val peerId = state.value.participantDraft.trim()
        if (peerId.isBlank()) {
            _state.update { it.copy(error = "Введите ID участника") }
            return
        }
        session.withNode {
            it.addParticipants(state.value.chatId, listOf(MeshChatMemberCommand(peerId, peerId)))
        }
        _state.update { it.copy(participantDraft = "") }
        refresh()
    }

    suspend fun removeParticipant(peerId: String) {
        session.withNode { it.removeParticipant(state.value.chatId, peerId) }
        refresh()
    }
}

data class ThreadState(
    val loading: Boolean = false,
    val error: String? = null,
    val chatId: String = "",
    val rootMessageId: String = "",
    val draft: String = "",
    val thread: MeshThread? = null,
    val summary: MeshThreadSummary? = null,
    val messages: List<MeshThreadMessage> = emptyList(),
)

class ThreadStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(ThreadState())
    val state: StateFlow<ThreadState> = _state.asStateFlow()

    fun bind(chatId: String, rootMessageId: String) {
        _state.update { it.copy(chatId = chatId, rootMessageId = rootMessageId) }
    }

    fun updateDraft(value: String) {
        _state.update { it.copy(draft = value) }
    }

    suspend fun refresh() {
        val current = state.value
        if (current.chatId.isBlank() || current.rootMessageId.isBlank()) return
        val result = session.withNode { node ->
            val thread = node.thread(current.chatId, current.rootMessageId)
            val summary = node.threadSummary(current.chatId, current.rootMessageId)
            val detailedMessages = node.threadMessagesDetailed(current.chatId, current.rootMessageId)
            val fallbackMessages = node.threadMessages(current.chatId, current.rootMessageId)
            ThreadState(
                chatId = current.chatId,
                rootMessageId = current.rootMessageId,
                draft = current.draft,
                thread = thread,
                summary = summary,
                messages = if (detailedMessages.isNotEmpty()) {
                    detailedMessages
                } else {
                    val resolvedThreadId = thread?.threadId ?: summary?.threadId ?: "thread-${current.rootMessageId}"
                    fallbackMessages.map {
                        MeshThreadMessage(
                            threadId = resolvedThreadId,
                            messageId = it.messageId,
                            chatId = it.conversationId,
                            rootMessageId = current.rootMessageId,
                            senderPeerId = it.senderPeerId,
                            body = it.body,
                            parentMessageId = it.parentMessageId,
                            replyToMessageId = it.replyToMessageId,
                            createdAt = it.createdAt,
                            deliveryStatus = it.deliveryStatus,
                            deliveredAt = it.deliveredAt,
                            failedAt = it.failedAt,
                        )
                    }
                },
            )
        }
        _state.value = result.getOrElse { current.copy(error = it.message ?: "Не удалось загрузить тред") }
    }

    suspend fun sendReply() {
        val current = state.value
        val body = current.draft.trim()
        if (body.isBlank()) {
            _state.update { it.copy(error = "Введите сообщение") }
            return
        }
        session.withNode { node ->
            if (node.thread(current.chatId, current.rootMessageId) == null) {
                node.createThread(MeshCreateThreadCommand(current.chatId, current.rootMessageId))
            }
            runCatching {
                node.sendThreadReply(
                    MeshSendThreadMessageCommand(
                        chatId = current.chatId,
                        rootMessageId = current.rootMessageId,
                        body = body,
                    ),
                )
            }.getOrElse {
                node.sendThreadMessage(
                    MeshSendThreadMessageCommand(
                        chatId = current.chatId,
                        rootMessageId = current.rootMessageId,
                        body = body,
                    ),
                )
                null
            }
        }
        _state.update { it.copy(draft = "") }
        refresh()
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
            _state.update { it.copy(error = "Укажите ID получателя и путь") }
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
    val groupTargets: String = "",
    val roomTitle: String = "",
    val offer: String = "demo-offer",
    val signalPayload: String = "",
    val sessions: List<MeshCallSession> = emptyList(),
    val activeCalls: List<MeshCallSession> = emptyList(),
    val incomingCalls: List<MeshCallSession> = emptyList(),
    val participantsByCall: Map<String, List<MeshCallParticipant>> = emptyMap(),
    val eventsByCall: Map<String, List<MeshCallEvent>> = emptyMap(),
    val mediaByCall: Map<String, MeshCallMediaState> = emptyMap(),
    val mediaStatsByCall: Map<String, MeshMediaStats> = emptyMap(),
)

class CallsStore(private val session: NodeSessionController) {
    private val _state = MutableStateFlow(CallsState())
    val state: StateFlow<CallsState> = _state.asStateFlow()

    fun updateTargetPeerId(value: String) { _state.update { it.copy(targetPeerId = value) } }
    fun updateGroupTargets(value: String) { _state.update { it.copy(groupTargets = value) } }
    fun updateRoomTitle(value: String) { _state.update { it.copy(roomTitle = value) } }
    fun updateOffer(value: String) { _state.update { it.copy(offer = value) } }
    fun updateSignalPayload(value: String) { _state.update { it.copy(signalPayload = value) } }

    fun prefill(targetPeerId: String) {
        _state.update { it.copy(targetPeerId = targetPeerId, error = null, message = null) }
    }

    suspend fun refresh() {
        val current = state.value
        val result = session.withNode { node ->
            val sessions = node.callSessions()
            val active = node.observeActiveCall()
            val incoming = node.observeIncomingCalls()
            val participantsByCall = sessions.associate { session ->
                session.callId to node.observeCallParticipants(session.callId)
            }
            val eventsByCall = sessions.associate { session ->
                session.callId to node.observeCallEvents(session.callId, 16)
            }
            val mediaByCall = sessions.mapNotNull { session ->
                node.observeMediaState(session.callId)?.let { media -> session.callId to media }
            }.toMap()
            val mediaStatsByCall = sessions.mapNotNull { session ->
                node.observeMediaStats(session.callId)?.let { stats -> session.callId to stats }
            }.toMap()
            current.copy(
                sessions = sessions,
                activeCalls = active,
                incomingCalls = incoming,
                participantsByCall = participantsByCall,
                eventsByCall = eventsByCall,
                mediaByCall = mediaByCall,
                mediaStatsByCall = mediaStatsByCall,
                error = null,
            )
        }
        _state.value = result.getOrElse { state.value.copy(error = it.message ?: "Не удалось загрузить звонки") }
    }

    suspend fun startCall() {
        val current = state.value
        if (current.targetPeerId.isBlank()) {
            _state.update { it.copy(error = "Введите ID контакта") }
            return
        }
        val result = session.withNode {
            it.startAudioCall(MeshStartCallCommand(targetPeerId = current.targetPeerId.trim(), offer = current.offer))
        }
        _state.update {
            result.fold(
                onSuccess = { _ -> current.copy(message = "Аудиозвонок отправлен", error = null) },
                onFailure = { error -> it.copy(error = error.message ?: "Не удалось начать звонок") },
            )
        }
        refresh()
    }

    suspend fun startVideoCall() {
        val current = state.value
        if (current.targetPeerId.isBlank()) {
            _state.update { it.copy(error = "Введите ID контакта") }
            return
        }
        val result = session.withNode {
            it.startVideoCall(MeshStartCallCommand(targetPeerId = current.targetPeerId.trim(), offer = current.offer))
        }
        _state.update {
            result.fold(
                onSuccess = { _ -> current.copy(message = "Видеозвонок отправлен", error = null) },
                onFailure = { error -> it.copy(error = error.message ?: "Не удалось начать видеозвонок") },
            )
        }
        refresh()
    }

    @Suppress("DEPRECATION")
    suspend fun startCallLegacy() {
        val current = state.value
        if (current.targetPeerId.isBlank()) {
            _state.update { it.copy(error = "Введите ID контакта") }
            return
        }
        val result = session.withNode {
            it.startCall(MeshStartCallCommand(targetPeerId = current.targetPeerId.trim(), offer = current.offer))
        }
        _state.update {
            result.fold(
                onSuccess = { _ -> current.copy(message = "Базовый вызов отправлен", error = null) },
                onFailure = { error -> it.copy(error = error.message ?: "Не удалось начать базовый вызов") },
            )
        }
        refresh()
    }

    suspend fun startGroupAudioCall() {
        val current = state.value
        val targets = current.groupTargets.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (targets.isEmpty()) {
            _state.update { it.copy(error = "Введите ID участников через запятую") }
            return
        }
        val result = session.withNode {
            it.startGroupAudioCall(
                MeshStartGroupCallCommand(
                    targetPeerIds = targets,
                    offer = current.offer,
                    conversationId = null,
                    roomTitle = current.roomTitle.trim().ifBlank { null },
                ),
            )
        }
        _state.update {
            result.fold(
                onSuccess = { _ -> current.copy(message = "Групповой аудиозвонок создан", error = null) },
                onFailure = { error -> it.copy(error = error.message ?: "Не удалось создать групповой звонок") },
            )
        }
        refresh()
    }

    suspend fun startGroupVideoCall() {
        val current = state.value
        val targets = current.groupTargets.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (targets.isEmpty()) {
            _state.update { it.copy(error = "Введите ID участников через запятую") }
            return
        }
        val result = session.withNode {
            it.startGroupVideoCall(
                MeshStartGroupCallCommand(
                    targetPeerIds = targets,
                    offer = current.offer,
                    conversationId = null,
                    roomTitle = current.roomTitle.trim().ifBlank { null },
                ),
            )
        }
        _state.update {
            result.fold(
                onSuccess = { _ -> current.copy(message = "Групповой видеозвонок создан", error = null) },
                onFailure = { error -> it.copy(error = error.message ?: "Не удалось создать групповой видеозвонок") },
            )
        }
        refresh()
    }

    suspend fun accept(call: MeshCallSession) {
        session.withNode {
            it.acceptCall(
                MeshAcceptCallCommand(
                    callId = call.callId,
                    recipientPeerId = replyPeer(call, it.profile.peerId),
                    answer = null,
                ),
            )
        }
        refresh()
    }

    suspend fun reject(call: MeshCallSession) {
        session.withNode {
            it.rejectCall(
                MeshRejectCallCommand(
                    callId = call.callId,
                    recipientPeerId = replyPeer(call, it.profile.peerId),
                    reason = "отклонено",
                ),
            )
        }
        refresh()
    }

    suspend fun join(call: MeshCallSession) {
        session.withNode {
            it.joinCall(
                MeshJoinCallCommand(
                    callId = call.callId,
                    recipientPeerId = replyPeer(call, it.profile.peerId),
                    answer = null,
                ),
            )
        }
        refresh()
    }

    suspend fun leave(call: MeshCallSession) {
        session.withNode {
            it.leaveCall(
                MeshLeaveCallCommand(
                    callId = call.callId,
                    recipientPeerId = replyPeer(call, it.profile.peerId),
                    reason = "left-from-ui",
                ),
            )
        }
        refresh()
    }

    @Suppress("DEPRECATION")
    suspend fun sendQuality(call: MeshCallSession) {
        val recipient = if (call.recipientPeerId == session.state.value.profile?.peerId) call.initiatorPeerId else call.recipientPeerId
        val payload = state.value.signalPayload.ifBlank { "ok" }
        session.withNode {
            it.sendCallSignal(
                MeshCallSignalCommand(
                    callId = call.callId,
                    recipientPeerId = recipient,
                    signalType = MeshCallSignalType.QUALITY,
                    payload = payload,
                ),
            )
        }
        refresh()
    }

    suspend fun hangup(call: MeshCallSession) {
        session.withNode {
            runCatching {
                it.endCall(MeshEndCallCommand(callId = call.callId, reason = "Завершено"))
            }.getOrElse { _ ->
                @Suppress("DEPRECATION")
                it.hangupCall(
                    MeshHangupCallCommand(
                        callId = call.callId,
                        recipientPeerId = replyPeer(call, it.profile.peerId),
                        reason = "Завершено",
                    ),
                )
            }
        }
        refresh()
    }

    suspend fun toggleMicrophone(call: MeshCallSession) {
        val current = state.value.mediaByCall[call.callId]
        val next = !(current?.localAudioEnabled ?: true)
        session.withNode {
            it.toggleMicrophone(
                MeshToggleMicrophoneCommand(
                    callId = call.callId,
                    enabled = next,
                ),
            )
        }
        refresh()
    }

    suspend fun toggleCamera(call: MeshCallSession) {
        val current = state.value.mediaByCall[call.callId]
        val next = !(current?.localVideoEnabled ?: true)
        session.withNode {
            it.toggleCamera(
                MeshToggleCameraCommand(
                    callId = call.callId,
                    enabled = next,
                ),
            )
        }
        refresh()
    }

    suspend fun switchCamera(call: MeshCallSession) {
        session.withNode { it.switchCamera(call.callId) }
        refresh()
    }

    @Suppress("unused", "DEPRECATION")
    suspend fun hangupLegacy(call: MeshCallSession) {
        val recipient = if (call.recipientPeerId == session.state.value.profile?.peerId) call.initiatorPeerId else call.recipientPeerId
        session.withNode {
            it.hangupCall(MeshHangupCallCommand(callId = call.callId, recipientPeerId = recipient, reason = "Завершено"))
        }
        refresh()
    }

    private fun replyPeer(call: MeshCallSession, localPeerId: String): String {
        return if (call.initiatorPeerId == localPeerId) {
            call.targetPeerIds.firstOrNull { it != localPeerId } ?: call.recipientPeerId
        } else {
            call.initiatorPeerId
        }
    }
}

data class DiagnosticsState(
    val loading: Boolean = false,
    val error: String? = null,
    val events: List<MeshEventLogEntry> = emptyList(),
    val metrics: MeshMetricSnapshot? = null,
    val routes: List<MeshRouteInfo> = emptyList(),
    val routeHealth: List<MeshRouteHealth> = emptyList(),
    val nearby: List<MeshNearbyPeer> = emptyList(),
    val relayStatus: MeshRelayStatus? = null,
    val relayMode: MeshRelayMode? = null,
    val topology: MeshTopologyState? = null,
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
                routeHealth = node.inspectRouteHealth(),
                nearby = node.nearbyPeers(),
                relayStatus = node.relayStatus(),
                relayMode = node.relayModeState(),
                topology = node.observeTopologyState(),
            )
        }
        _state.value = result.getOrElse { state.value.copy(error = it.message ?: "Не удалось загрузить диагностику") }
    }

    suspend fun forceTopologyRefresh() {
        val result = session.withNode { node -> node.forceTopologyRefresh() }
        _state.update {
            result.fold(
                onSuccess = { _ -> it.copy(error = null) },
                onFailure = { error -> it.copy(error = error.message ?: "Не удалось обновить топологию") },
            )
        }
        refresh()
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
    val group = GroupStore(session)
    val thread = ThreadStore(session)
    val transfers = TransfersStore(session)
    val calls = CallsStore(session)
    val diagnostics = DiagnosticsStore(session)
    val profile = ProfileStore(session)
    val settings = SettingsStore(session)
}
