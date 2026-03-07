package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.messaging.ChatThread
import org.expert.link.mesh.domain.model.messaging.ChatType
import org.expert.link.mesh.domain.model.messaging.GroupChatEvent
import org.expert.link.mesh.domain.model.messaging.GroupEventType
import org.expert.link.mesh.domain.model.messaging.ThreadMessage
import org.expert.link.mesh.domain.model.messaging.ThreadSummary
import org.expert.link.mesh.domain.port.repository.ConversationRepositoryPort
import org.expert.link.mesh.domain.port.repository.GroupEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.MessageRepositoryPort
import org.expert.link.mesh.domain.port.repository.ThreadMessageRepositoryPort
import org.expert.link.mesh.domain.port.repository.ThreadRepositoryPort

/**
 * Application-сервис тредов внутри чата.
 *
 * Отвечает за lifecycle треда: создание root-ветки, отправка reply и чтение истории.
 */
class ThreadService(
    private val localProfileService: LocalProfileService,
    private val conversationRepositoryPort: ConversationRepositoryPort,
    private val messageRepositoryPort: MessageRepositoryPort,
    private val threadRepositoryPort: ThreadRepositoryPort,
    private val threadMessageRepositoryPort: ThreadMessageRepositoryPort,
    private val groupEventRepositoryPort: GroupEventRepositoryPort,
    private val chatMessagingService: ChatMessagingService,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
) {
    /** Создаёт тред для root-сообщения или возвращает существующий. */
    suspend fun createThread(chatId: String, rootMessageId: String): ChatThread {
        threadRepositoryPort.findByRootMessage(chatId, rootMessageId)?.let { return it }
        val rootMessage = requireNotNull(chatMessagingService.message(rootMessageId)) {
            "Root message $rootMessageId not found"
        }
        val conversation = requireNotNull(conversationRepositoryPort.findByConversationId(chatId)) {
            "Conversation $chatId not found"
        }
        require(rootMessage.conversationId == chatId) { "Root message does not belong to chat $chatId" }

        val localPeerId = localProfileService.require().peerId
        val thread = ChatThread(
            threadId = newId("thread"),
            chatId = chatId,
            rootMessageId = rootMessageId,
            rootSenderPeerId = rootMessage.senderPeerId,
            createdByPeerId = localPeerId,
            createdAt = now(),
            updatedAt = now(),
            replyCount = 0,
            participantPeerIds = setOf(rootMessage.senderPeerId, localPeerId),
        )
        val saved = threadRepositoryPort.save(thread)
        if (conversation.chatType == ChatType.GROUP) {
            groupEventRepositoryPort.append(
                GroupChatEvent(
                    eventId = newId("group-event"),
                    chatId = chatId,
                    eventType = GroupEventType.THREAD_CREATED,
                    actorPeerId = localPeerId,
                    text = "Создан тред для сообщения",
                    attributes = mapOf("rootMessageId" to rootMessageId, "threadId" to saved.threadId),
                    createdAt = now(),
                ),
            )
        }
        eventLogService.log(
            category = EventCategory.MESSAGING,
            level = EventLevel.INFO,
            message = "Thread created",
            peerId = rootMessage.senderPeerId,
            attributes = mapOf("chatId" to chatId, "threadId" to saved.threadId),
        )
        nodeMetricsService.increment("thread.created")
        return saved
    }

    /** Возвращает тред по root-сообщению. */
    suspend fun thread(chatId: String, rootMessageId: String): ChatThread? {
        threadRepositoryPort.findByRootMessage(chatId, rootMessageId)?.let { return it }
        val summary = chatMessagingService.threadSummary(chatId, rootMessageId) ?: return null
        val root = chatMessagingService.message(rootMessageId)
        val createdAt = root?.createdAt ?: summary.lastReplyAt ?: now()
        val updatedAt = summary.lastReplyAt ?: createdAt
        val ownerPeerId = root?.senderPeerId
            ?: summary.participantPeerIds.firstOrNull()
            ?: localProfileService.require().peerId
        return ChatThread(
            threadId = summary.threadId,
            chatId = chatId,
            rootMessageId = rootMessageId,
            rootSenderPeerId = root?.senderPeerId ?: ownerPeerId,
            createdByPeerId = ownerPeerId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            replyCount = summary.replyCount,
            lastReplyMessageId = null,
            participantPeerIds = summary.participantPeerIds,
        )
    }

    /** Возвращает все треды чата. */
    suspend fun threads(chatId: String): List<ChatThread> {
        return threadRepositoryPort.listByChatId(chatId).sortedByDescending { it.updatedAt }
    }

    /** Возвращает агрегированные сводки тредов чата. */
    suspend fun threadSummaries(chatId: String): List<ThreadSummary> {
        val repositorySummaries = threads(chatId).map {
            ThreadSummary(
                threadId = it.threadId,
                chatId = it.chatId,
                rootMessageId = it.rootMessageId,
                replyCount = it.replyCount,
                lastReplyAt = it.updatedAt,
                participantPeerIds = it.participantPeerIds,
            )
        }
        val inboundSummaries = messageRepositoryPort.listByConversation(chatId)
            .filter { it.threadRootMessageId != null }
            .groupBy { it.threadRootMessageId!! }
            .map { (rootMessageId, messages) ->
                ThreadSummary(
                    threadId = "thread-$rootMessageId",
                    chatId = chatId,
                    rootMessageId = rootMessageId,
                    replyCount = messages.size,
                    lastReplyAt = messages.maxByOrNull { it.createdAt }?.createdAt,
                    participantPeerIds = messages.map { it.senderPeerId }.toSet(),
                )
            }
        return (repositorySummaries + inboundSummaries)
            .groupBy { it.rootMessageId }
            .values
            .map { sameRootSummaries ->
                sameRootSummaries.reduce { acc, next ->
                    val accLastReplyAt = acc.lastReplyAt
                    val nextLastReplyAt = next.lastReplyAt
                    val bestLastReplyAt = when {
                        accLastReplyAt == null -> nextLastReplyAt
                        nextLastReplyAt == null -> accLastReplyAt
                        accLastReplyAt >= nextLastReplyAt -> accLastReplyAt
                        else -> nextLastReplyAt
                    }
                    val bestThreadId = if (acc.threadId.startsWith("thread-")) next.threadId else acc.threadId
                    ThreadSummary(
                        threadId = if (bestThreadId.isBlank()) acc.threadId else bestThreadId,
                        chatId = chatId,
                        rootMessageId = acc.rootMessageId,
                        replyCount = maxOf(acc.replyCount, next.replyCount),
                        lastReplyAt = bestLastReplyAt,
                        participantPeerIds = acc.participantPeerIds + next.participantPeerIds,
                    )
                }
            }
            .sortedWith { left, right ->
                val leftAt = left.lastReplyAt
                val rightAt = right.lastReplyAt
                when {
                    leftAt == null && rightAt == null -> 0
                    leftAt == null -> 1
                    rightAt == null -> -1
                    else -> rightAt.compareTo(leftAt)
                }
            }
    }

    /** Отправляет reply в тред и обновляет его метаданные. */
    suspend fun sendThreadMessage(
        chatId: String,
        rootMessageId: String,
        body: String,
        parentMessageId: String? = null,
    ): ThreadMessage {
        val thread = thread(chatId, rootMessageId) ?: createThread(chatId, rootMessageId)
        val message = chatMessagingService.sendThreadMessage(chatId, rootMessageId, body, parentMessageId)
        val threadMessage = ThreadMessage(
            threadId = thread.threadId,
            chatId = chatId,
            rootMessageId = rootMessageId,
            messageId = message.messageId,
            senderPeerId = message.senderPeerId,
            body = message.body,
            parentMessageId = parentMessageId,
            replyToMessageId = rootMessageId,
            deliveryStatus = message.deliveryStatus,
            createdAt = message.createdAt,
            deliveredAt = message.deliveredAt,
            failedAt = message.failedAt,
        )
        threadMessageRepositoryPort.save(threadMessage)
        val updatedMessages = threadMessageRepositoryPort.listByThreadId(thread.threadId)
        val updatedThread = thread.copy(
            updatedAt = now(),
            replyCount = updatedMessages.size,
            lastReplyMessageId = message.messageId,
            participantPeerIds = thread.participantPeerIds + message.senderPeerId,
        )
        threadRepositoryPort.save(updatedThread)
        nodeMetricsService.increment("thread.reply.sent")
        return threadMessage
    }

    /** Возвращает историю сообщений треда. */
    suspend fun threadMessages(chatId: String, rootMessageId: String): List<ThreadMessage> {
        val thread = thread(chatId, rootMessageId)
        val fromRepository = if (thread != null) {
            threadMessageRepositoryPort.listByThreadId(thread.threadId)
        } else {
            emptyList()
        }
        val fromChatService = chatMessagingService.threadMessages(chatId, rootMessageId)
            .map {
                ThreadMessage(
                    threadId = "thread-$rootMessageId",
                    chatId = chatId,
                    rootMessageId = rootMessageId,
                    messageId = it.messageId,
                    senderPeerId = it.senderPeerId,
                    body = it.body,
                    parentMessageId = it.parentMessageId,
                    replyToMessageId = it.replyToMessageId,
                    deliveryStatus = it.deliveryStatus,
                    createdAt = it.createdAt,
                    deliveredAt = it.deliveredAt,
                    failedAt = it.failedAt,
                )
            }
        return (fromRepository + fromChatService)
            .associateBy { it.messageId }
            .values
            .sortedBy { it.createdAt }
    }
}
