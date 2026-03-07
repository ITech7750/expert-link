package org.expert.link.app.shared.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.screen.components.MessageBubble
import org.expert.link.app.shared.screen.components.initials
import org.expert.link.app.shared.ui.components.CallVideoSurface
import org.expert.link.mesh.contract.model.MeshCallSession
import org.expert.link.mesh.contract.model.MeshCallState
import org.expert.link.mesh.contract.model.MeshCallType
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshFileTransferStatus
import org.expert.link.mesh.contract.model.MeshThreadMessage
import org.expert.link.mesh.contract.model.MeshThreadSummary
import org.expert.link.mesh.contract.model.MeshTransferDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(component: ChatComponent) {
    val state by component.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val selectedThreadMessage = remember(state.selectedThreadRootMessageId, state.messages) {
        state.messages.firstOrNull { it.messageId == state.selectedThreadRootMessageId }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    if (state.selectedThreadRootMessageId != null) {
        ThreadSheet(
            title = state.title,
            rootMessage = selectedThreadMessage,
            threadSummary = state.selectedThreadSummary,
            messages = state.selectedThreadMessages,
            members = state.members,
            localPeerId = state.localPeerId,
            draft = state.threadDraft,
            isLoading = state.isThreadLoading,
            onDraftChange = component::updateThreadDraft,
            onSend = component::sendThreadReply,
            onDismiss = component::closeThread,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initials(state.title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = state.title,
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = component::goBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = component::sendFile,
                        enabled = state.canCallOrSendFile && !state.isSendingFile,
                    ) {
                        if (state.isSendingFile) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.AttachFile, contentDescription = "Файл")
                        }
                    }
                    IconButton(
                        onClick = component::startVideoCall,
                        enabled = state.canCallOrSendFile && !state.isStartingCall,
                    ) {
                        if (state.isStartingCall) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Videocam, contentDescription = "Видеозвонок")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(paddingValues),
        ) {
            if (state.runtimeStatus != NodeRuntimeStatus.RUNNING) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text = if (state.runtimeStatus == NodeRuntimeStatus.STARTING) "Подключение..." else "Чат недоступен",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            state.activeCall?.let { call ->
                CallCard(
                    call = call,
                    localVideoEnabled = state.mediaState?.localVideoEnabled ?: true,
                    localAudioEnabled = state.mediaState?.localAudioEnabled ?: true,
                    remotePeerId = state.mediaState?.peers?.firstOrNull()?.peerId,
                    onAccept = component::acceptCall,
                    onReject = component::rejectCall,
                    onHangup = component::hangupCall,
                    onToggleMicrophone = component::toggleMicrophone,
                    onToggleCamera = component::toggleCamera,
                )
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = if (state.overlayTransfer != null) 168.dp else 16.dp,
                            end = 16.dp,
                            bottom = 16.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.threadSummaries.isNotEmpty()) {
                            item {
                                ThreadSummaryRow(
                                    summaries = state.threadSummaries,
                                    messages = state.messages.associateBy { it.messageId },
                                    onOpenThread = component::openThread,
                                )
                            }
                        }
                        items(state.messages, key = { it.messageId }) { message ->
                            val senderName = if (message.senderPeerId == state.localPeerId) {
                                "Вы"
                            } else {
                                state.members[message.senderPeerId]
                                    ?.takeIf { it.isNotBlank() }
                                    ?: message.senderPeerId
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                MessageBubble(
                                    message = message,
                                    isFromCurrentUser = message.senderPeerId == state.localPeerId,
                                    senderName = senderName,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (message.senderPeerId == state.localPeerId) {
                                        Arrangement.End
                                    } else {
                                        Arrangement.Start
                                    },
                                ) {
                                    TextButton(onClick = { component.openThread(message.messageId) }) {
                                        Text(
                                            if (message.threadReplyCount > 0) {
                                                "Тред ${message.threadReplyCount}"
                                            } else {
                                                "Тред"
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        if (state.messages.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(24.dp),
                                ) {
                                    Text(
                                        text = "Сообщений нет",
                                        modifier = Modifier.padding(18.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    state.overlayTransfer?.let { transfer ->
                        TransferOverlayCard(
                            transfer = transfer,
                            canShare = component.capabilities.canShareFiles,
                            canSave = component.capabilities.canSaveFiles,
                            onResume = { component.resumeTransfer(transfer.transferId) },
                            onCancel = { component.cancelTransfer(transfer.transferId) },
                            onDismiss = { component.dismissTransfer(transfer.transferId) },
                            onShare = { component.shareTransfer(transfer.transferId) },
                            onSave = { component.saveTransferToDownloads(transfer.transferId) },
                        )
                    }
                }
            }

            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .then(
                            if (state.selectedThreadRootMessageId == null) {
                                Modifier.imePadding()
                            } else {
                                Modifier
                            },
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = component::updateDraft,
                        modifier = Modifier.weight(1f),
                        label = { Text("Сообщение") },
                        minLines = 1,
                        maxLines = 5,
                        shape = RoundedCornerShape(22.dp),
                    )
                    FilledIconButton(
                        onClick = component::sendMessage,
                        enabled = state.draft.isNotBlank() && !state.isSending,
                        modifier = Modifier.size(56.dp),
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallCard(
    call: MeshCallSession,
    localVideoEnabled: Boolean,
    localAudioEnabled: Boolean,
    remotePeerId: String?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onHangup: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onToggleCamera: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = callStatusLabel(call.status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (call.callType == MeshCallType.VIDEO && call.status !in incomingCallStates) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CallVideoSurface(
                        callId = call.callId,
                        peerId = remotePeerId,
                        local = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    )
                    CallVideoSurface(
                        callId = call.callId,
                        peerId = null,
                        local = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (call.status in incomingCallStates) {
                    Button(onClick = onAccept) {
                        Text("Принять")
                    }
                    OutlinedButton(onClick = onReject) {
                        Text("Отклонить")
                    }
                } else if (call.status !in finishedCallStates) {
                    OutlinedButton(onClick = onToggleMicrophone) {
                        Icon(
                            imageVector = if (localAudioEnabled) Icons.Outlined.Mic else Icons.Outlined.MicOff,
                            contentDescription = null,
                        )
                    }
                    if (call.callType == MeshCallType.VIDEO) {
                        OutlinedButton(onClick = onToggleCamera) {
                            Icon(
                                imageVector = if (localVideoEnabled) Icons.Outlined.Videocam else Icons.Outlined.VideocamOff,
                                contentDescription = null,
                            )
                        }
                    }
                    OutlinedButton(onClick = onHangup) {
                        Icon(Icons.Outlined.CallEnd, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferCard(
    transfer: MeshFileTransferSession,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    val completedChunks = when (transfer.direction) {
        MeshTransferDirection.OUTGOING -> transfer.acknowledgedChunks.size
        MeshTransferDirection.INCOMING -> transfer.receivedChunks.size
    }
    val progress = if (transfer.totalChunks == 0) 0f else completedChunks.toFloat() / transfer.totalChunks.toFloat()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = transfer.descriptor.fileName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = transferStatusLabel(transfer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (transfer.status in resumableTransferStates) {
                    TextButton(onClick = onResume) {
                        Text("Повтор")
                    }
                }
                if (transfer.status !in finalTransferStates) {
                    TextButton(onClick = onCancel) {
                        Text("Отмена")
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.TransferOverlayCard(
    transfer: MeshFileTransferSession,
    canShare: Boolean,
    canSave: Boolean,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    val completedChunks = when (transfer.direction) {
        MeshTransferDirection.OUTGOING -> transfer.acknowledgedChunks.size
        MeshTransferDirection.INCOMING -> transfer.receivedChunks.size
    }
    val progress = if (transfer.totalChunks == 0) 0f else completedChunks.toFloat() / transfer.totalChunks.toFloat()
    val isCompleted = transfer.status == MeshFileTransferStatus.COMPLETED
    val canOpenActions = isCompleted && !transfer.localPath.isNullOrBlank()
    val canDismiss = transfer.status in finalTransferStates

    Surface(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = transfer.descriptor.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = transferStatusLabel(transfer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (canDismiss) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Закрыть")
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (transfer.status in resumableTransferStates) {
                        TextButton(onClick = onResume) {
                            Text("Повтор")
                        }
                    }
                    if (transfer.status !in finalTransferStates) {
                        TextButton(onClick = onCancel) {
                            Text("Отмена")
                        }
                    }
                }

                if (canOpenActions) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (canSave) {
                            IconButton(onClick = onSave) {
                                Icon(Icons.Outlined.FileDownload, contentDescription = "Сохранить")
                            }
                        }
                        if (canShare) {
                            IconButton(onClick = onShare) {
                                Icon(Icons.Outlined.Share, contentDescription = "Поделиться")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadSummaryRow(
    summaries: List<MeshThreadSummary>,
    messages: Map<String, org.expert.link.mesh.contract.model.MeshChatMessage>,
    onOpenThread: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Треды",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(summaries, key = { it.rootMessageId }) { summary ->
                    Surface(
                        onClick = { onOpenThread(summary.rootMessageId) },
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 220.dp)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = messages[summary.rootMessageId]?.body ?: "Сообщение",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${summary.replyCount} ответов",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadSheet(
    title: String,
    rootMessage: org.expert.link.mesh.contract.model.MeshChatMessage?,
    threadSummary: MeshThreadSummary?,
    messages: List<MeshThreadMessage>,
    members: Map<String, String>,
    localPeerId: String?,
    draft: String,
    isLoading: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            val listMaxHeight = (maxHeight * 0.42f).coerceAtLeast(160.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Тред: $title",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        rootMessage?.let { message ->
                            Text(
                                text = message.body,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        threadSummary?.let {
                            Text(
                                text = "${it.replyCount} ответов",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = listMaxHeight),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (messages.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(22.dp),
                                ) {
                                    Text(
                                        text = "Пока нет ответов",
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            items(messages, key = { it.messageId }) { message ->
                                MessageBubble(
                                    message = org.expert.link.mesh.contract.model.MeshChatMessage(
                                        messageId = message.messageId,
                                        conversationId = message.chatId,
                                        senderPeerId = message.senderPeerId,
                                        recipientPeerId = message.chatId,
                                        body = message.body,
                                        threadRootMessageId = message.rootMessageId,
                                        parentMessageId = message.parentMessageId,
                                        replyToMessageId = message.replyToMessageId,
                                        deliveryStatus = message.deliveryStatus,
                                        createdAt = message.createdAt,
                                        deliveredAt = message.deliveredAt,
                                        failedAt = message.failedAt,
                                    ),
                                    isFromCurrentUser = message.senderPeerId == localPeerId,
                                    senderName = if (message.senderPeerId == localPeerId) {
                                        "Вы"
                                    } else {
                                        members[message.senderPeerId] ?: message.senderPeerId
                                    },
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = onDraftChange,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp),
                            label = { Text("Ответ") },
                            minLines = 1,
                            maxLines = 3,
                            shape = RoundedCornerShape(22.dp),
                        )
                        FilledIconButton(
                            onClick = onSend,
                            enabled = draft.isNotBlank() && !isLoading,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun transferStatusLabel(transfer: MeshFileTransferSession): String {
    val done = when (transfer.direction) {
        MeshTransferDirection.OUTGOING -> transfer.acknowledgedChunks.size
        MeshTransferDirection.INCOMING -> transfer.receivedChunks.size
    }
    val prefix = when (transfer.status) {
        MeshFileTransferStatus.OFFERED -> "Ожидание"
        MeshFileTransferStatus.ACCEPTED -> "Подготовка"
        MeshFileTransferStatus.IN_PROGRESS -> "Передача"
        MeshFileTransferStatus.PAUSED -> "Пауза"
        MeshFileTransferStatus.COMPLETED -> "Готово"
        MeshFileTransferStatus.FAILED -> "Ошибка"
        MeshFileTransferStatus.CANCELLED -> "Отменено"
    }
    return "$prefix • $done/${transfer.totalChunks}"
}

private fun callStatusLabel(status: MeshCallState): String {
    return when (status) {
        MeshCallState.NEW -> "Новый звонок"
        MeshCallState.INVITED,
        MeshCallState.OUTGOING,
        MeshCallState.RINGING -> "Звоним..."
        MeshCallState.INCOMING -> "Входящий звонок"
        MeshCallState.ACCEPTED,
        MeshCallState.CONNECTING -> "Подключение..."
        MeshCallState.ACTIVE,
        MeshCallState.CONNECTED -> "Звонок"
        MeshCallState.RECONNECTING -> "Переподключение..."
        MeshCallState.ENDED -> "Звонок завершён"
        MeshCallState.REJECTED -> "Звонок отклонён"
        MeshCallState.FAILED -> "Ошибка звонка"
        MeshCallState.MISSED -> "Пропущен"
        MeshCallState.LEFT -> "Вы вышли"
    }
}

private val incomingCallStates = setOf(
    MeshCallState.INCOMING,
    MeshCallState.RINGING,
    MeshCallState.INVITED,
)

private val finishedCallStates = setOf(
    MeshCallState.ENDED,
    MeshCallState.REJECTED,
    MeshCallState.FAILED,
    MeshCallState.MISSED,
    MeshCallState.LEFT,
)

private val resumableTransferStates = setOf(
    MeshFileTransferStatus.PAUSED,
    MeshFileTransferStatus.FAILED,
)

private val finalTransferStates = setOf(
    MeshFileTransferStatus.COMPLETED,
    MeshFileTransferStatus.CANCELLED,
)
