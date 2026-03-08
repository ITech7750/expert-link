package org.expert.link.app.shared.screen.chatlist

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.expert.link.app.shared.navigation.BaseComponent
import org.expert.link.app.shared.navigation.Config
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.presentation.NodeSessionController
import org.expert.link.mesh.contract.model.MeshCallType
import org.expert.link.mesh.contract.model.MeshChatSummary
import org.expert.link.mesh.contract.model.MeshChatType
import org.expert.link.mesh.contract.model.MeshConversation
import org.expert.link.mesh.contract.model.MeshPairedPeer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class ChatListItem(
    val conversationId: String,
    val peerId: String?,
    val title: String,
    val preview: String,
    val updatedAt: Instant,
    val unreadCount: Int,
    val isGroup: Boolean,
    val secondaryText: String,
)

data class IncomingChatCall(
    val conversationId: String,
    val peerId: String?,
    val title: String,
    val isVideo: Boolean,
)

data class ChatListState(
    val runtimeStatus: NodeRuntimeStatus = NodeRuntimeStatus.STOPPED,
    val searchQuery: String = "",
    val localDisplayName: String = "",
    val items: List<ChatListItem> = emptyList(),
    val incomingCall: IncomingChatCall? = null,
    val isRefreshing: Boolean = false,
    val error: String? = null,
) {
    val filteredItems: List<ChatListItem>
        get() = if (searchQuery.isBlank()) {
            items
        } else {
            items.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.preview.contains(searchQuery, ignoreCase = true) ||
                    it.secondaryText.contains(searchQuery, ignoreCase = true)
            }
        }
}

class ChatListComponent(
    context: ComponentContext,
    onNavigate: (Config) -> Unit,
    onBack: () -> Unit,
) : BaseComponent(context, onNavigate, onBack), KoinComponent {
    private val session: NodeSessionController by inject()

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init {
        observeSession()
        startPolling()
    }

    private fun observeSession() {
        componentScope.launch {
            session.state.collectLatest { sessionState ->
                _state.update {
                    it.copy(
                        runtimeStatus = sessionState.status,
                        localDisplayName = sessionState.profile?.displayName ?: sessionState.config.displayName,
                        incomingCall = if (sessionState.status == NodeRuntimeStatus.RUNNING) it.incomingCall else null,
                        error = if (sessionState.status == NodeRuntimeStatus.ERROR) {
                            sessionState.errorMessage ?: it.error
                        } else {
                            it.error
                        },
                    )
                }
                if (sessionState.status == NodeRuntimeStatus.RUNNING) {
                    refresh()
                }
            }
        }
    }

    private fun startPolling() {
        componentScope.launch {
            while (isActive) {
                if (session.state.value.status == NodeRuntimeStatus.RUNNING) {
                    refresh()
                }
                delay(2_000)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun openInvite() {
        onNavigate(Config.Invite)
    }

    fun openProfile() {
        onNavigate(Config.Profile)
    }

    fun openIncomingCall() {
        val incoming = state.value.incomingCall ?: return
        onNavigate(
            Config.Chat(
                conversationId = incoming.conversationId,
                peerId = incoming.peerId,
                title = incoming.title,
            ),
        )
    }

    fun openChat(item: ChatListItem) {
        onNavigate(
            Config.Chat(
                conversationId = item.conversationId,
                peerId = item.peerId,
                title = item.title,
            ),
        )
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun refreshNow() {
        componentScope.launch { refresh() }
    }

    private suspend fun refresh() {
        if (session.state.value.status != NodeRuntimeStatus.RUNNING) {
            return
        }
        _state.update { it.copy(isRefreshing = true, error = null) }
        val result = session.withNode { node ->
            val localPeerId = node.profile.peerId
            val peers = node.peers().associateBy { it.identity.peerId }
            val summaries = node.chatSummaries().associateBy { it.chatId }
            val conversations = node.conversations()
            val items = conversations
                .map { conversation ->
                    conversation.toItem(
                        localPeerId = localPeerId,
                        peers = peers,
                        summaries = summaries,
                    )
                }
                .groupBy { item ->
                    if (item.peerId != null) {
                        "direct:${item.peerId}"
                    } else {
                        "chat:${item.conversationId}"
                    }
                }
                .map { (_, groupedItems) ->
                    groupedItems.maxByOrNull { it.updatedAt }!!
                }
                .sortedByDescending { it.updatedAt }
            val incoming = node.observeIncomingCalls()
                .sortedByDescending { it.updatedAt }
                .firstOrNull()
                ?.let { call ->
                    val conversationId = call.conversationId
                        ?: conversations.firstOrNull { conversation ->
                            conversation.chatType == MeshChatType.DIRECT &&
                                conversation.participantPeerIds.contains(localPeerId) &&
                                conversation.participantPeerIds.contains(call.initiatorPeerId)
                        }?.conversationId
                    val peerId = call.initiatorPeerId.takeIf { it != localPeerId } ?: call.recipientPeerId
                    conversationId?.let {
                        IncomingChatCall(
                            conversationId = it,
                            peerId = peerId,
                            title = peers[peerId]?.identity?.displayName?.ifBlank { null }
                                ?: items.firstOrNull { item -> item.conversationId == conversationId }?.title
                                ?: peerId,
                            isVideo = call.callType == MeshCallType.VIDEO,
                        )
                    }
                }
            currentState().copy(
                items = items,
                incomingCall = incoming,
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { refreshed ->
                    current.copy(
                        items = refreshed.items,
                        incomingCall = refreshed.incomingCall,
                        isRefreshing = false,
                        error = null,
                    )
                },
                onFailure = { error ->
                    current.copy(
                        isRefreshing = false,
                        error = error.message ?: "Не удалось загрузить список чатов",
                    )
                },
            )
        }
    }

    private fun MeshConversation.toItem(
        localPeerId: String,
        peers: Map<String, MeshPairedPeer>,
        summaries: Map<String, MeshChatSummary>,
    ): ChatListItem {
        val summary = summaries[conversationId]
        val peerId = if (chatType == MeshChatType.DIRECT) {
            participantPeerIds.firstOrNull { it != localPeerId }
        } else {
            null
        }
        val peer = peerId?.let(peers::get)
        val resolvedTitle = when {
            chatType == MeshChatType.GROUP -> summary?.title ?: title
            !peer?.identity?.displayName.isNullOrBlank() -> peer?.identity?.displayName.orEmpty()
            else -> title
        }
        val secondaryText = when {
            chatType == MeshChatType.GROUP -> "${members.size.coerceAtLeast(summary?.participantCount ?: 0)} участников"
            peer != null -> peer.identity.peerId
            else -> peerId.orEmpty()
        }
        return ChatListItem(
            conversationId = conversationId,
            peerId = peerId,
            title = resolvedTitle,
            preview = summary?.lastMessagePreview ?: if (lastMessageId == null) "Нет сообщений" else "Сообщение",
            updatedAt = summary?.updatedAt ?: updatedAt,
            unreadCount = summary?.unreadCount ?: unreadCount,
            isGroup = chatType == MeshChatType.GROUP,
            secondaryText = secondaryText,
        )
    }

    private fun currentState(): ChatListState = state.value
}
