package org.expert.link.database

import kotlinx.datetime.Instant
import org.expert.link.mesh.domain.model.messaging.ChatMessage
import org.expert.link.mesh.domain.model.messaging.Conversation
import org.expert.link.mesh.domain.model.messaging.MessageDeliveryStatus
import org.expert.link.mesh.domain.model.messaging.ThreadSummary
import org.expert.link.mesh.domain.port.repository.ConversationRepositoryPort
import org.expert.link.mesh.domain.port.repository.MessageRepositoryPort

internal class RoomConversationRepository(
    private val messagingDao: MessagingDao,
) : ConversationRepositoryPort {
    override suspend fun save(conversation: Conversation): Conversation {
        messagingDao.upsertConversation(
            ConversationRecord(
                conversationId = conversation.conversationId,
                chatType = conversation.chatType.name,
                title = conversation.title,
                participantKey = participantKey(conversation.participantPeerIds),
                updatedAt = conversation.updatedAt.toString(),
                lastMessageId = conversation.lastMessageId,
                unreadCount = conversation.unreadCount,
                payloadJson = encodePayload(conversation),
            ),
        )
        return conversation
    }

    override suspend fun findByConversationId(conversationId: String): Conversation? =
        messagingDao.findConversation(conversationId)?.let { decodePayload(it.payloadJson) }

    override suspend fun findByParticipants(participantPeerIds: Set<String>): Conversation? =
        messagingDao.findConversationByParticipantKey(participantKey(participantPeerIds))
            ?.let { decodePayload(it.payloadJson) }

    override suspend fun list(): List<Conversation> =
        messagingDao.listConversations().map { decodePayload(it.payloadJson) }
}

internal class RoomMessageRepository(
    private val messagingDao: MessagingDao,
) : MessageRepositoryPort {
    override suspend fun save(message: ChatMessage): ChatMessage {
        messagingDao.upsertMessage(
            MessageRecord(
                messageId = message.messageId,
                conversationId = message.conversationId,
                threadRootMessageId = message.threadRootMessageId,
                senderPeerId = message.senderPeerId,
                recipientPeerId = message.recipientPeerId,
                deliveryStatus = message.deliveryStatus.name,
                createdAt = message.createdAt.toString(),
                payloadJson = encodePayload(message),
            ),
        )
        return message
    }

    override suspend fun findByMessageId(messageId: String): ChatMessage? =
        messagingDao.findMessage(messageId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByConversation(conversationId: String): List<ChatMessage> =
        messagingDao.listMessagesByConversation(conversationId).map { decodePayload(it.payloadJson) }

    override suspend fun listByThread(conversationId: String, rootMessageId: String): List<ChatMessage> =
        messagingDao.listMessagesByThread(conversationId, rootMessageId).map { decodePayload(it.payloadJson) }

    override suspend fun getThreadSummary(conversationId: String, rootMessageId: String): ThreadSummary? {
        val threadMessages = listByThread(conversationId, rootMessageId)
        if (threadMessages.isEmpty()) return null
        return ThreadSummary(
            threadId = "thread-$rootMessageId",
            chatId = conversationId,
            rootMessageId = rootMessageId,
            replyCount = threadMessages.size,
            lastReplyAt = threadMessages.maxByOrNull { it.createdAt }?.createdAt,
            participantPeerIds = threadMessages.map { it.senderPeerId }.toSet(),
        )
    }

    override suspend fun updateStatus(messageId: String, status: MessageDeliveryStatus): ChatMessage? {
        val current = findByMessageId(messageId) ?: return null
        val now = kotlinx.datetime.Clock.System.now()
        val updated = current.copy(
            deliveryStatus = status,
            deliveredAt = if (status == MessageDeliveryStatus.DELIVERED) now else current.deliveredAt,
            failedAt = if (status == MessageDeliveryStatus.FAILED) now else current.failedAt,
        )
        save(updated)
        return updated
    }

    override suspend fun updateThreadReplyCount(messageId: String, replyCount: Int): ChatMessage? {
        val current = findByMessageId(messageId) ?: return null
        val updated = current.copy(threadReplyCount = replyCount)
        save(updated)
        return updated
    }
}
