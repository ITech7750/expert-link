package org.expert.link.mesh.application.mapper

import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.expert.link.mesh.domain.entity.ConversationEntity
import org.expert.link.mesh.domain.entity.MessageEntity
import org.expert.link.mesh.domain.model.messaging.ChatMember
import org.expert.link.mesh.domain.model.messaging.ChatMessage
import org.expert.link.mesh.domain.model.messaging.ChatType
import org.expert.link.mesh.domain.model.messaging.Conversation
import org.expert.link.mesh.domain.model.messaging.MessageDeliveryStatus
import org.expert.link.mesh.domain.model.messaging.MessageType

/** Маппер диалога. */
object ConversationEntityMapper {
    /**
     * Converts a conversation into a storage entity.
     */
    fun toEntity(conversation: Conversation): ConversationEntity = ConversationEntity(
        conversationId = conversation.conversationId,
        chatType = conversation.chatType.name,
        title = conversation.title,
        description = conversation.description,
        createdByPeerId = conversation.createdByPeerId,
        participantPeerIds = conversation.participantPeerIds.toList(),
        membersJson = Json.encodeToString(ListSerializer(ChatMember.serializer()), conversation.members),
        createdAt = conversation.createdAt.toString(),
        updatedAt = conversation.updatedAt.toString(),
        lastMessageId = conversation.lastMessageId,
        unreadCount = conversation.unreadCount,
        pinned = conversation.pinned,
        archived = conversation.archived,
    )

    /**
     * Converts a storage entity back into a conversation.
     */
    fun fromEntity(entity: ConversationEntity): Conversation = Conversation(
        conversationId = entity.conversationId,
        chatType = ChatType.valueOf(entity.chatType),
        title = entity.title,
        description = entity.description,
        createdByPeerId = entity.createdByPeerId,
        participantPeerIds = entity.participantPeerIds.toSet(),
        members = Json.decodeFromString(ListSerializer(ChatMember.serializer()), entity.membersJson),
        createdAt = Instant.parse(entity.createdAt),
        updatedAt = Instant.parse(entity.updatedAt),
        lastMessageId = entity.lastMessageId,
        unreadCount = entity.unreadCount,
        pinned = entity.pinned,
        archived = entity.archived,
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
        messageType = message.messageType.name,
        threadRootMessageId = message.threadRootMessageId,
        parentMessageId = message.parentMessageId,
        replyToMessageId = message.replyToMessageId,
        threadReplyCount = message.threadReplyCount,
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
        messageType = MessageType.valueOf(entity.messageType),
        threadRootMessageId = entity.threadRootMessageId,
        parentMessageId = entity.parentMessageId,
        replyToMessageId = entity.replyToMessageId,
        threadReplyCount = entity.threadReplyCount,
        deliveryStatus = MessageDeliveryStatus.valueOf(entity.deliveryStatus),
        createdAt = Instant.parse(entity.createdAt),
        deliveredAt = entity.deliveredAt?.let(Instant::parse),
        failedAt = entity.failedAt?.let(Instant::parse),
    )
}
