package org.expert.link.mesh.application.mapper

import kotlinx.datetime.Instant
import org.expert.link.mesh.domain.entity.ConversationEntity
import org.expert.link.mesh.domain.entity.MessageEntity
import org.expert.link.mesh.domain.model.messaging.ChatMessage
import org.expert.link.mesh.domain.model.messaging.Conversation
import org.expert.link.mesh.domain.model.messaging.MessageDeliveryStatus

/** Маппер диалога. */
object ConversationEntityMapper {
    /**
     * Converts a conversation into a storage entity.
     */
    fun toEntity(conversation: Conversation): ConversationEntity = ConversationEntity(
        conversationId = conversation.conversationId,
        participantPeerIds = conversation.participantPeerIds.toList(),
        createdAt = conversation.createdAt.toString(),
        updatedAt = conversation.updatedAt.toString(),
        lastMessageId = conversation.lastMessageId,
    )

    /**
     * Converts a storage entity back into a conversation.
     */
    fun fromEntity(entity: ConversationEntity): Conversation = Conversation(
        conversationId = entity.conversationId,
        participantPeerIds = entity.participantPeerIds.toSet(),
        createdAt = Instant.parse(entity.createdAt),
        updatedAt = Instant.parse(entity.updatedAt),
        lastMessageId = entity.lastMessageId,
    )
}

/** Маппер сообщения. */
object MessageEntityMapper {
    /**
     * Converts a chat message into a storage entity.
     */
    fun toEntity(message: ChatMessage): MessageEntity = MessageEntity(
        messageId = message.messageId,
        conversationId = message.conversationId,
        senderPeerId = message.senderPeerId,
        recipientPeerId = message.recipientPeerId,
        body = message.body,
        deliveryStatus = message.deliveryStatus.name,
        createdAt = message.createdAt.toString(),
        deliveredAt = message.deliveredAt?.toString(),
        failedAt = message.failedAt?.toString(),
    )

    /**
     * Converts a storage entity back into a chat message.
     */
    fun fromEntity(entity: MessageEntity): ChatMessage = ChatMessage(
        messageId = entity.messageId,
        conversationId = entity.conversationId,
        senderPeerId = entity.senderPeerId,
        recipientPeerId = entity.recipientPeerId,
        body = entity.body,
        deliveryStatus = MessageDeliveryStatus.valueOf(entity.deliveryStatus),
        createdAt = Instant.parse(entity.createdAt),
        deliveredAt = entity.deliveredAt?.let(Instant::parse),
        failedAt = entity.failedAt?.let(Instant::parse),
    )
}
