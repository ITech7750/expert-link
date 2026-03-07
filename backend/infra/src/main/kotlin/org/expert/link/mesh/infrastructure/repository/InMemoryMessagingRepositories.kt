package org.expert.link.mesh.infrastructure.repository

import kotlinx.datetime.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.messaging.ChatMessage
import org.expert.link.mesh.domain.model.messaging.Conversation
import org.expert.link.mesh.domain.model.messaging.MessageDeliveryStatus
import org.expert.link.mesh.domain.model.messaging.ThreadSummary
import org.expert.link.mesh.domain.model.network.PendingAckRecord
import org.expert.link.mesh.domain.port.repository.ConversationRepositoryPort
import org.expert.link.mesh.domain.port.repository.MessageRepositoryPort
import org.expert.link.mesh.domain.port.repository.OutgoingQueuePort
import org.expert.link.mesh.domain.port.repository.PendingAckRepositoryPort

/** Репозиторий диалогов в памяти. */
class InMemoryConversationRepositoryAdapter : ConversationRepositoryPort {
    private val mutex = Mutex()
    private val conversations = linkedMapOf<String, Conversation>()

    override suspend fun save(conversation: Conversation): Conversation = mutex.withLock {
        conversations[conversation.conversationId] = conversation
        conversation
    }

    override suspend fun findByConversationId(conversationId: String): Conversation? = mutex.withLock { conversations[conversationId] }

    override suspend fun findByParticipants(participantPeerIds: Set<String>): Conversation? = mutex.withLock {
        conversations.values.firstOrNull { it.participantPeerIds == participantPeerIds }
    }

    override suspend fun list(): List<Conversation> = mutex.withLock { conversations.values.toList() }
}

/** Репозиторий сообщений в памяти. */
class InMemoryMessageRepositoryAdapter : MessageRepositoryPort {
    private val mutex = Mutex()
    private val messages = linkedMapOf<String, ChatMessage>()

    override suspend fun save(message: ChatMessage): ChatMessage = mutex.withLock {
        messages[message.messageId] = message
        message
    }

    override suspend fun findByMessageId(messageId: String): ChatMessage? = mutex.withLock { messages[messageId] }

    override suspend fun listByConversation(conversationId: String): List<ChatMessage> = mutex.withLock {
        messages.values.filter { it.conversationId == conversationId }
    }

    override suspend fun listByThread(conversationId: String, rootMessageId: String): List<ChatMessage> = mutex.withLock {
        messages.values.filter {
            it.conversationId == conversationId && it.threadRootMessageId == rootMessageId
        }
    }

    override suspend fun getThreadSummary(conversationId: String, rootMessageId: String): ThreadSummary? = mutex.withLock {
        val threadMessages = messages.values.filter {
            it.conversationId == conversationId && it.threadRootMessageId == rootMessageId
        }
        if (threadMessages.isEmpty()) {
            null
        } else {
            ThreadSummary(
                threadId = "thread-$rootMessageId",
                chatId = conversationId,
                rootMessageId = rootMessageId,
                replyCount = threadMessages.size,
                lastReplyAt = threadMessages.maxByOrNull { it.createdAt }?.createdAt,
                participantPeerIds = threadMessages.map { it.senderPeerId }.toSet(),
            )
        }
    }

    override suspend fun updateStatus(messageId: String, status: MessageDeliveryStatus): ChatMessage? = mutex.withLock {
        messages[messageId]?.let { current ->
            val updated = current.copy(
                deliveryStatus = status,
                deliveredAt = if (status == MessageDeliveryStatus.DELIVERED) now() else current.deliveredAt,
                failedAt = if (status == MessageDeliveryStatus.FAILED) now() else current.failedAt,
            )
            messages[messageId] = updated
            updated
        }
    }

    override suspend fun updateThreadReplyCount(messageId: String, replyCount: Int): ChatMessage? = mutex.withLock {
        messages[messageId]?.let { current ->
            val updated = current.copy(threadReplyCount = replyCount)
            messages[messageId] = updated
            updated
        }
    }
}

/** Очередь исходящих пакетов в памяти. */
class InMemoryOutgoingQueueAdapter : OutgoingQueuePort {
    private val mutex = Mutex()
    private val queue = ArrayDeque<PendingAckRecord>()

    override suspend fun enqueue(record: PendingAckRecord) {
        mutex.withLock { queue.addLast(record) }
    }

    override suspend fun dequeue(): PendingAckRecord? = mutex.withLock {
        if (queue.isEmpty()) null else queue.removeFirst()
    }

    override suspend fun list(): List<PendingAckRecord> = mutex.withLock { queue.toList() }
}

/** Репозиторий ожидаемых ACK в памяти. */
class InMemoryPendingAckRepositoryAdapter : PendingAckRepositoryPort {
    private val mutex = Mutex()
    private val pending = linkedMapOf<String, PendingAckRecord>()

    override suspend fun save(record: PendingAckRecord): PendingAckRecord = mutex.withLock {
        pending[record.packetId] = record
        record
    }

    override suspend fun findByPacketId(packetId: String): PendingAckRecord? = mutex.withLock { pending[packetId] }

    override suspend fun listExpired(now: Instant): List<PendingAckRecord> = mutex.withLock {
        pending.values.filter { it.nextAttemptAt <= now }
    }

    override suspend fun remove(packetId: String) {
        mutex.withLock { pending.remove(packetId) }
    }

    override suspend fun list(): List<PendingAckRecord> = mutex.withLock { pending.values.toList() }
}
