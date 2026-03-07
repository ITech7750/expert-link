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
import org.expert.link.mesh.contract.api.MeshEndCallCommand
import org.expert.link.mesh.contract.api.MeshFileTransferCommand
import org.expert.link.mesh.contract.api.MeshRejectCallCommand
import org.expert.link.mesh.contract.api.MeshSendMessageCommand
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class ChatState(
    val runtimeStatus: NodeRuntimeStatus = NodeRuntimeStatus.STOPPED,
    val conversationId: String,
    val peerId: String? = null,
    val title: String = "Чат",
    val members: Map<String, String> = emptyMap(),
    val localPeerId: String? = null,
    val messages: List<MeshChatMessage> = emptyList(),
    val transfers: List<MeshFileTransferSession> = emptyList(),
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
        get() = !peerId.isNullOrBlank()
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

    private val _state = MutableStateFlow(
        ChatState(
            conversationId = conversationId,
            peerId = initialPeerId,
            title = initialTitle,
        ),
    )
    val state: StateFlow<ChatState> = _state.asStateFlow()

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
                val peerId = state.value.peerId
                if (!peerId.isNullOrBlank()) {
                    node.sendChat(
                        MeshChatCommand(
                            targetPeerId = peerId,
                            body = body,
                            conversationId = conversationId,
                        ),
                    )
                } else {
                    node.sendMessage(
                        MeshSendMessageCommand(
                            chatId = conversationId,
                            body = body,
                        ),
                    )
                }
            }
            _state.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
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
        val peerId = state.value.peerId
        if (peerId.isNullOrBlank()) {
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
                node.sendFile(
                    MeshFileTransferCommand(
                        targetPeerId = peerId,
                        path = path,
                        conversationId = conversationId,
                    ),
                )
            }
            _state.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
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
        val peerId = state.value.peerId
        if (peerId.isNullOrBlank()) {
            _state.update { it.copy(error = "Видеозвонок доступен только в личном чате") }
            return
        }
        componentScope.launch {
            _state.update { it.copy(isStartingCall = true, error = null, message = null) }
            val result = session.withNode { node ->
                node.startVideoCall(
                    MeshStartCallCommand(
                        targetPeerId = peerId,
                        conversationId = conversationId,
                    ),
                )
            }
            _state.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
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
            val result = session.withNode { node -> node.cancelFileTransfer(transferId) }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(message = "Передача отменена", error = null) },
                    onFailure = { error -> current.copy(error = error.message ?: "Не удалось отменить передачу") },
                )
            }
            refresh()
        }
    }

    private suspend fun refresh() {
        if (session.state.value.status != NodeRuntimeStatus.RUNNING) {
            return
        }
        val current = state.value
        val result = session.withNode { node ->
            val conversation = node.conversations().firstOrNull { it.conversationId == conversationId }
            val resolvedPeerId = current.peerId ?: conversation?.directPeerId(current.localPeerId)
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
            current.copy(
                title = conversation?.resolveTitle(current.localPeerId) ?: current.title,
                peerId = resolvedPeerId,
                members = conversation?.members?.associate { it.peerId to it.displayName } ?: emptyMap(),
                messages = node.messages(conversationId).sortedBy { it.createdAt },
                transfers = transfers,
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
                    error = error.message ?: "Не удалось загрузить чат",
                )
            }
        }
    }

    private fun MeshConversation.resolveTitle(localPeerId: String?): String {
        return if (chatType == MeshChatType.DIRECT) {
            val peerId = directPeerId(localPeerId)
            members.firstOrNull { it.peerId == peerId }?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: title
        } else {
            title
        }
    }

    private fun MeshConversation.directPeerId(localPeerId: String?): String? {
        if (chatType != MeshChatType.DIRECT) return null
        return participantPeerIds.firstOrNull { it != localPeerId }
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
}
