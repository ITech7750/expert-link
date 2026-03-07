package org.expert.link.app.shared.screen.chat

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.expert.link.app.shared.navigation.BaseComponent
import org.expert.link.app.shared.navigation.Config
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.presentation.NodeSessionController
import org.expert.link.mesh.contract.api.MeshAcceptCallCommand
import org.expert.link.mesh.contract.api.MeshChatCommand
import org.expert.link.mesh.contract.api.MeshCreateThreadCommand
import org.expert.link.mesh.contract.api.MeshEndCallCommand
import org.expert.link.mesh.contract.api.MeshFileTransferCommand
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.api.MeshRejectCallCommand
import org.expert.link.mesh.contract.api.MeshSendMessageCommand
import org.expert.link.mesh.contract.api.MeshSendThreadMessageCommand
import org.expert.link.mesh.contract.api.MeshStartCallCommand
import org.expert.link.mesh.contract.api.MeshToggleCameraCommand
import org.expert.link.mesh.contract.api.MeshToggleMicrophoneCommand
import org.expert.link.mesh.contract.model.MeshCallMediaState
import org.expert.link.mesh.contract.model.MeshCallSession
import org.expert.link.mesh.contract.model.MeshCallState
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshChatType
import org.expert.link.mesh.contract.model.MeshConversation
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshFileTransferStatus
import org.expert.link.mesh.contract.model.MeshPairedPeer
import org.expert.link.mesh.contract.model.MeshThreadMessage
import org.expert.link.mesh.contract.model.MeshThreadSummary
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class ChatState(
    val runtimeStatus: NodeRuntimeStatus = NodeRuntimeStatus.STOPPED,
    val conversationId: String,
    val peerId: String? = null,
    val isDirectChat: Boolean = peerId != null,
    val title: String = "Чат",
    val members: Map<String, String> = emptyMap(),
    val localPeerId: String? = null,
    val messages: List<MeshChatMessage> = emptyList(),
    val threadSummaries: List<MeshThreadSummary> = emptyList(),
    val selectedThreadRootMessageId: String? = null,
    val selectedThreadSummary: MeshThreadSummary? = null,
    val selectedThreadMessages: List<MeshThreadMessage> = emptyList(),
    val threadDraft: String = "",
    val isThreadLoading: Boolean = false,
    val transfers: List<MeshFileTransferSession> = emptyList(),
    val dismissedTransferIds: Set<String> = emptySet(),
    val activeCall: MeshCallSession? = null,
    val mediaState: MeshCallMediaState? = null,
    val draft: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val isSendingFile: Boolean = false,
    val isStartingCall: Boolean = false,
    val error: String? = null,
    val message: String? = null,
) {
    val canCallOrSendFile: Boolean
        get() = isDirectChat || !peerId.isNullOrBlank()

    val overlayTransfer: MeshFileTransferSession?
        get() = transfers
            .filterNot { it.transferId in dismissedTransferIds || it.status == MeshFileTransferStatus.CANCELLED }
            .maxByOrNull { it.updatedAt }
}

class ChatComponent(
    context: ComponentContext,
    onNavigate: (Config) -> Unit,
    onBack: () -> Unit,
    private val conversationId: String,
    private val initialPeerId: String?,
    private val initialTitle: String,
) : BaseComponent(context, onNavigate, onBack), KoinComponent {
    private val session: NodeSessionController by inject()
    private val services: AppPlatformServices by inject()
    private val uiStateStore: ChatUiStateStore by inject()

    private val _state = MutableStateFlow(
        ChatState(
            conversationId = conversationId,
            peerId = initialPeerId,
            title = initialTitle,
        ),
    )
    val state: StateFlow<ChatState> = _state.asStateFlow()
    val capabilities
        get() = services.capabilities

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
                        localPeerId = sessionState.profile?.peerId,
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
                delay(1_500)
            }
        }
    }

    fun updateDraft(value: String) {
        _state.update { it.copy(draft = value) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun dismissTransfer(transferId: String) {
        uiStateStore.dismissTransfer(conversationId, transferId)
        _state.update { it.copy(dismissedTransferIds = it.dismissedTransferIds + transferId) }
    }

    fun openThread(rootMessageId: String) {
        _state.update {
            it.copy(
                selectedThreadRootMessageId = rootMessageId,
                isThreadLoading = true,
                error = null,
            )
        }
        componentScope.launch {
            refresh()
        }
    }

    fun closeThread() {
        _state.update {
            it.copy(
                selectedThreadRootMessageId = null,
                selectedThreadSummary = null,
                selectedThreadMessages = emptyList(),
                threadDraft = "",
                isThreadLoading = false,
            )
        }
    }

    fun updateThreadDraft(value: String) {
        _state.update { it.copy(threadDraft = value) }
    }

    fun goBack() {
        onBack()
    }

    fun sendMessage() {
        val body = state.value.draft.trim()
        if (body.isEmpty()) {
            _state.update { it.copy(error = "Введите сообщение") }
            return
        }
        componentScope.launch {
            _state.update { it.copy(isSending = true, error = null, message = null) }
            val result = session.withNode { node ->
                val peerId = resolveDirectPeerId(node)
                if (!peerId.isNullOrBlank()) {
                    val message = node.sendChat(
                        MeshChatCommand(
                            targetPeerId = peerId,
                            body = body,
                            conversationId = conversationId,
                        ),
                    )
                    SendMessageResult(peerId = peerId, message = message)
                } else {
                    SendMessageResult(
                        peerId = null,
                        message = node.sendMessage(
                        MeshSendMessageCommand(
                            chatId = conversationId,
                            body = body,
                        ),
                        ),
                    )
                }
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { sent ->
                        current.copy(
                            peerId = sent.peerId ?: current.peerId,
                            draft = "",
                            isSending = false,
                            error = null,
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isSending = false,
                            error = error.message ?: "Не удалось отправить сообщение",
                        )
                    },
                )
            }
            refresh()
        }
    }

    fun sendFile() {
        if (!state.value.canCallOrSendFile) {
            _state.update { it.copy(error = "Отправка файлов доступна только в личном чате") }
            return
        }
        componentScope.launch {
            _state.update { it.copy(isSendingFile = true, error = null, message = null) }
            val path = services.pickFile().getOrElse { error ->
                _state.update {
                    it.copy(
                        isSendingFile = false,
                        error = error.message ?: "Не удалось открыть выбор файла",
                    )
                }
                return@launch
            }
            if (path.isNullOrBlank()) {
                _state.update { it.copy(isSendingFile = false) }
                return@launch
            }
            val result = session.withNode { node ->
                val resolvedPeerId = resolveDirectPeerId(node)
                    ?: error("Не удалось определить собеседника для отправки файла")
                node.sendFile(
                    MeshFileTransferCommand(
                        targetPeerId = resolvedPeerId,
                        path = path,
                        conversationId = conversationId,
                    ),
                ) to resolvedPeerId
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { (_, resolvedPeerId) ->
                        current.copy(
                            peerId = resolvedPeerId,
                            isSendingFile = false,
                            message = "Файл отправляется",
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isSendingFile = false,
                            error = error.message ?: "Не удалось отправить файл",
                        )
                    },
                )
            }
            refresh()
        }
    }

    fun startVideoCall() {
        if (!state.value.canCallOrSendFile) {
            _state.update { it.copy(error = "Видеозвонок доступен только в личном чате") }
            return
        }
        componentScope.launch {
            _state.update { it.copy(isStartingCall = true, error = null, message = null) }
            val result = session.withNode { node ->
                val resolvedPeerId = resolveDirectPeerId(node)
                    ?: error("Не удалось определить собеседника для звонка")
                node.startVideoCall(
                    MeshStartCallCommand(
                        targetPeerId = resolvedPeerId,
                        conversationId = conversationId,
                    ),
                ) to resolvedPeerId
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { (_, resolvedPeerId) ->
                        current.copy(
                            peerId = resolvedPeerId,
                            isStartingCall = false,
                            message = "Видеозвонок запущен",
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isStartingCall = false,
                            error = error.message ?: "Не удалось начать видеозвонок",
                        )
                    },
                )
            }
            refresh()
        }
    }

    fun acceptCall() {
        val call = state.value.activeCall ?: return
        val localPeerId = state.value.localPeerId ?: return
        componentScope.launch {
            _state.update { it.copy(error = null, message = null) }
            val result = session.withNode { node ->
                node.acceptCall(
                    MeshAcceptCallCommand(
                        callId = call.callId,
                        recipientPeerId = replyPeer(call, localPeerId),
                        answer = null,
                    ),
                )
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(message = "Звонок принят") },
                    onFailure = { error -> current.copy(error = error.message ?: "Не удалось принять звонок") },
                )
            }
            refresh()
        }
    }

    fun rejectCall() {
        val call = state.value.activeCall ?: return
        val localPeerId = state.value.localPeerId ?: return
        componentScope.launch {
            _state.update { it.copy(error = null, message = null) }
            val result = session.withNode { node ->
                node.rejectCall(
                    MeshRejectCallCommand(
                        callId = call.callId,
                        recipientPeerId = replyPeer(call, localPeerId),
                        reason = "rejected-from-chat",
                    ),
                )
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(message = "Звонок отклонён") },
                    onFailure = { error -> current.copy(error = error.message ?: "Не удалось отклонить звонок") },
                )
            }
            refresh()
        }
    }

    fun hangupCall() {
        val call = state.value.activeCall ?: return
        componentScope.launch {
            _state.update { it.copy(error = null, message = null) }
            val result = session.withNode { node ->
                node.endCall(
                    MeshEndCallCommand(
                        callId = call.callId,
                        reason = "hangup-from-chat",
                    ),
                )
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(message = "Звонок завершён") },
                    onFailure = { error -> current.copy(error = error.message ?: "Не удалось завершить звонок") },
                )
            }
            refresh()
        }
    }

    fun toggleMicrophone() {
        val call = state.value.activeCall ?: return
        val enabled = !(state.value.mediaState?.localAudioEnabled ?: true)
        componentScope.launch {
            val result = session.withNode { node ->
                node.toggleMicrophone(
                    MeshToggleMicrophoneCommand(
                        callId = call.callId,
                        enabled = enabled,
                    ),
                )
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { mediaState -> current.copy(mediaState = mediaState, error = null) },
                    onFailure = { error -> current.copy(error = error.message ?: "Не удалось переключить микрофон") },
                )
            }
            refresh()
        }
    }

    fun toggleCamera() {
        val call = state.value.activeCall ?: return
        val enabled = !(state.value.mediaState?.localVideoEnabled ?: true)
        componentScope.launch {
            val result = session.withNode { node ->
                node.toggleCamera(
                    MeshToggleCameraCommand(
                        callId = call.callId,
                        enabled = enabled,
                    ),
                )
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { mediaState -> current.copy(mediaState = mediaState, error = null) },
                    onFailure = { error -> current.copy(error = error.message ?: "Не удалось переключить камеру") },
                )
            }
            refresh()
        }
    }

    fun resumeTransfer(transferId: String) {
        componentScope.launch {
            val result = session.withNode { node -> node.resumeFileTransfer(transferId) }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(message = "Передача продолжена", error = null) },
                    onFailure = { error -> current.copy(error = error.message ?: "Не удалось продолжить передачу") },
                )
            }
            refresh()
        }
    }

    fun cancelTransfer(transferId: String) {
        componentScope.launch {
            uiStateStore.dismissTransfer(conversationId, transferId)
            _state.update {
                it.copy(
                    dismissedTransferIds = it.dismissedTransferIds + transferId,
                    error = null,
                    message = null,
                )
            }
            val result = session.withNode { node -> node.cancelFileTransfer(transferId) }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(message = "Передача отменена", error = null) },
                    onFailure = { error ->
                        uiStateStore.restoreTransfer(conversationId, transferId)
                        current.copy(
                            dismissedTransferIds = current.dismissedTransferIds - transferId,
                            error = error.message ?: "Не удалось отменить передачу",
                        )
                    },
                )
            }
            refresh()
        }
    }

    fun sendThreadReply() {
        val rootMessageId = state.value.selectedThreadRootMessageId ?: return
        val body = state.value.threadDraft.trim()
        if (body.isBlank()) {
            _state.update { it.copy(error = "Введите сообщение для треда") }
            return
        }
        componentScope.launch {
            _state.update { it.copy(isThreadLoading = true, error = null, message = null) }
            val result = session.withNode { node ->
                if (node.thread(conversationId, rootMessageId) == null) {
                    node.createThread(
                        MeshCreateThreadCommand(
                            chatId = conversationId,
                            rootMessageId = rootMessageId,
                        ),
                    )
                }
                runCatching {
                    node.sendThreadReply(
                        MeshSendThreadMessageCommand(
                            chatId = conversationId,
                            rootMessageId = rootMessageId,
                            body = body,
                        ),
                    )
                }.getOrElse {
                    node.sendThreadMessage(
                        MeshSendThreadMessageCommand(
                            chatId = conversationId,
                            rootMessageId = rootMessageId,
                            body = body,
                        ),
                    )
                    null
                }
            }
            _state.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
                            threadDraft = "",
                            isThreadLoading = false,
                            message = "Ответ отправлен",
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isThreadLoading = false,
                            error = error.message ?: "Не удалось отправить ответ",
                        )
                    },
                )
            }
            refresh()
        }
    }

    fun shareTransfer(transferId: String) {
        componentScope.launch {
            val transfer = state.value.transfers.firstOrNull { it.transferId == transferId }
            val path = transfer?.localPath
            if (path.isNullOrBlank()) {
                _state.update { it.copy(error = "Файл ещё недоступен") }
                return@launch
            }
            val result = services.shareFile(
                label = transfer.descriptor.fileName,
                path = path,
            )
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(message = "Окно отправки открыто", error = null) },
                    onFailure = { error -> current.copy(error = error.message ?: "Не удалось поделиться файлом") },
                )
            }
        }
    }

    fun saveTransferToDownloads(transferId: String) {
        componentScope.launch {
            val transfer = state.value.transfers.firstOrNull { it.transferId == transferId }
            val path = transfer?.localPath
            if (path.isNullOrBlank()) {
                _state.update { it.copy(error = "Файл ещё недоступен") }
                return@launch
            }
            val result = services.saveFileToDownloads(
                path = path,
                fileName = transfer.descriptor.fileName,
            )
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(message = "Файл сохранён", error = null) },
                    onFailure = { error -> current.copy(error = error.message ?: "Не удалось сохранить файл") },
                )
            }
        }
    }

    private suspend fun refresh() {
        if (session.state.value.status != NodeRuntimeStatus.RUNNING) {
            return
        }
        val current = state.value
        val result = session.withNode { node ->
            val conversation = node.conversations().firstOrNull { it.conversationId == conversationId }
            val localPeerId = node.profile.peerId
            val localDisplayName = node.profile.displayName
            val peers = node.peers().associateBy { it.identity.peerId }
            val resolvedPeerId = resolveDirectPeerId(
                node = node,
                current = current.copy(localPeerId = localPeerId),
                conversation = conversation,
            )
            val callSessions = node.callSessions()
            val transfers = node.fileTransfers()
                .filter { transfer ->
                    transfer.conversationId == conversationId ||
                        (!resolvedPeerId.isNullOrBlank() && setOf(transfer.senderPeerId, transfer.recipientPeerId).contains(resolvedPeerId))
                }
                .sortedBy { it.createdAt }
            val call = callSessions
                .filter { session -> session.belongsToConversation(conversationId, resolvedPeerId) }
                .sortedByDescending { it.updatedAt }
                .firstOrNull { it.status !in terminalCallStates }
            val dismissedTransferIds = uiStateStore.retainTransfers(
                conversationId = conversationId,
                activeTransferIds = transfers.mapTo(linkedSetOf()) { it.transferId },
            ) +
                transfers.filter { it.status == MeshFileTransferStatus.CANCELLED }.map { it.transferId }
            val selectedThread = current.selectedThreadRootMessageId?.let { rootMessageId ->
                loadThreadSnapshot(
                    node = node,
                    rootMessageId = rootMessageId,
                )
            }
            current.copy(
                isDirectChat = conversation?.chatType == MeshChatType.DIRECT || current.isDirectChat,
                title = conversation?.resolveTitle(
                    localPeerId = localPeerId,
                    localDisplayName = localDisplayName,
                    peers = peers,
                ) ?: current.title,
                peerId = resolvedPeerId,
                members = conversation?.members
                    ?.associate { member ->
                        member.peerId to resolveDisplayName(
                            peerId = member.peerId,
                            currentName = member.displayName,
                            localPeerId = localPeerId,
                            localDisplayName = localDisplayName,
                            peers = peers,
                        )
                    }
                    .orEmpty(),
                messages = node.messages(conversationId).sortedBy { it.createdAt },
                threadSummaries = node.threadUpdates(conversationId).sortedByDescending { it.lastReplyAt ?: conversation?.updatedAt },
                selectedThreadSummary = selectedThread?.summary,
                selectedThreadMessages = selectedThread?.messages ?: emptyList(),
                isThreadLoading = false,
                transfers = transfers,
                dismissedTransferIds = dismissedTransferIds,
                activeCall = call,
                mediaState = call?.let { node.observeMediaState(it.callId) },
                isLoading = false,
                error = null,
            )
        }
        _state.update { old ->
            result.getOrElse { error ->
                old.copy(
                    isLoading = false,
                    isThreadLoading = false,
                    error = error.message ?: "Не удалось загрузить чат",
                )
            }
        }
    }

    private suspend fun loadThreadSnapshot(
        node: MeshNode,
        rootMessageId: String,
    ): ThreadSnapshot {
        val summary = node.threadSummary(conversationId, rootMessageId)
        val detailedMessages = node.threadMessagesDetailed(conversationId, rootMessageId)
        val messages = if (detailedMessages.isNotEmpty()) {
            detailedMessages
        } else {
            val threadId = summary?.threadId ?: "thread-$rootMessageId"
            node.threadMessages(conversationId, rootMessageId).map { message ->
                MeshThreadMessage(
                    threadId = threadId,
                    chatId = message.conversationId,
                    rootMessageId = rootMessageId,
                    messageId = message.messageId,
                    senderPeerId = message.senderPeerId,
                    body = message.body,
                    parentMessageId = message.parentMessageId,
                    replyToMessageId = message.replyToMessageId,
                    deliveryStatus = message.deliveryStatus,
                    createdAt = message.createdAt,
                    deliveredAt = message.deliveredAt,
                    failedAt = message.failedAt,
                )
            }
        }
        return ThreadSnapshot(
            summary = summary,
            messages = messages,
        )
    }

    private fun MeshConversation.resolveTitle(
        localPeerId: String?,
        localDisplayName: String,
        peers: Map<String, MeshPairedPeer>,
    ): String {
        return if (chatType == MeshChatType.DIRECT) {
            val peerId = directPeerId(localPeerId)
            resolveDisplayName(
                peerId = peerId,
                currentName = members.firstOrNull { it.peerId == peerId }?.displayName,
                localPeerId = localPeerId,
                localDisplayName = localDisplayName,
                peers = peers,
            ).takeIf { it.isNotBlank() }
                ?: title
        } else {
            title
        }
    }

    private fun resolveDisplayName(
        peerId: String?,
        currentName: String?,
        localPeerId: String?,
        localDisplayName: String,
        peers: Map<String, MeshPairedPeer>,
    ): String {
        if (peerId == null) {
            return currentName.orEmpty()
        }
        if (peerId == localPeerId) {
            return localDisplayName
        }
        val trustedName = peers[peerId]?.identity?.displayName
        return when {
            !trustedName.isNullOrBlank() -> trustedName
            !currentName.isNullOrBlank() && currentName != peerId -> currentName
            else -> peerId
        }
    }

    private fun MeshConversation.directPeerId(localPeerId: String?): String? {
        if (chatType != MeshChatType.DIRECT) return null
        if (localPeerId == null) return null
        return participantPeerIds.firstOrNull { it != localPeerId }
    }

    private suspend fun resolveDirectPeerId(
        node: MeshNode,
        current: ChatState = state.value,
        conversation: MeshConversation? = null,
    ): String? {
        val localPeerId = node.profile.peerId
        val fallbackPeerId = current.peerId?.takeIf { it.isNotBlank() && it != localPeerId }
        val directConversation = conversation
            ?: node.conversations().firstOrNull { it.conversationId == conversationId }
        if (directConversation?.chatType != MeshChatType.DIRECT) {
            return fallbackPeerId
        }
        return directConversation.directPeerId(localPeerId) ?: fallbackPeerId
    }

    private fun MeshCallSession.belongsToConversation(conversationId: String, peerId: String?): Boolean {
        if (this.conversationId == conversationId) {
            return true
        }
        return !peerId.isNullOrBlank() && (
            initiatorPeerId == peerId ||
                recipientPeerId == peerId ||
                targetPeerIds.contains(peerId)
            )
    }

    private fun replyPeer(call: MeshCallSession, localPeerId: String): String {
        return if (call.initiatorPeerId == localPeerId) {
            call.targetPeerIds.firstOrNull { it != localPeerId } ?: call.recipientPeerId
        } else {
            call.initiatorPeerId
        }
    }

    private companion object {
        val terminalCallStates = setOf(
            MeshCallState.ENDED,
            MeshCallState.REJECTED,
            MeshCallState.FAILED,
            MeshCallState.MISSED,
            MeshCallState.LEFT,
        )
    }

    private data class SendMessageResult(
        val peerId: String?,
        val message: MeshChatMessage,
    )

    private data class ThreadSnapshot(
        val summary: MeshThreadSummary?,
        val messages: List<MeshThreadMessage>,
    )
}
