package org.expert.link.mesh.application.mapper

import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.expert.link.mesh.domain.entity.GroupChatEntity
import org.expert.link.mesh.domain.entity.GroupEventEntity
import org.expert.link.mesh.domain.entity.ThreadEntity
import org.expert.link.mesh.domain.entity.ThreadMessageEntity
import org.expert.link.mesh.domain.model.messaging.ChatMember
import org.expert.link.mesh.domain.model.messaging.ChatThread
import org.expert.link.mesh.domain.model.messaging.GroupChat
import org.expert.link.mesh.domain.model.messaging.GroupChatEvent
import org.expert.link.mesh.domain.model.messaging.GroupEventType
import org.expert.link.mesh.domain.model.messaging.ThreadMessage
import org.expert.link.mesh.domain.model.messaging.MessageDeliveryStatus

/** Маппер группового чата. */
object GroupChatEntityMapper {
    /** Преобразует доменную модель в сущность хранения. */
    fun toEntity(groupChat: GroupChat): GroupChatEntity = GroupChatEntity(
        chatId = groupChat.chatId,
        title = groupChat.title,
        description = groupChat.description,
        createdByPeerId = groupChat.createdByPeerId,
        membersJson = Json.encodeToString(ListSerializer(ChatMember.serializer()), groupChat.members),
        createdAt = groupChat.createdAt.toString(),
        updatedAt = groupChat.updatedAt.toString(),
        lastMessageId = groupChat.lastMessageId,
        pinned = groupChat.pinned,
        archived = groupChat.archived,
    )

    /** Преобразует сущность хранения в доменную модель. */
    fun fromEntity(entity: GroupChatEntity): GroupChat = GroupChat(
        chatId = entity.chatId,
        title = entity.title,
        description = entity.description,
        createdByPeerId = entity.createdByPeerId,
        members = Json.decodeFromString(ListSerializer(ChatMember.serializer()), entity.membersJson),
        createdAt = Instant.parse(entity.createdAt),
        updatedAt = Instant.parse(entity.updatedAt),
        lastMessageId = entity.lastMessageId,
        pinned = entity.pinned,
        archived = entity.archived,
    )
}

/** Маппер треда. */
object ThreadEntityMapper {
    /** Преобразует доменную модель треда в сущность хранения. */
    fun toEntity(thread: ChatThread): ThreadEntity = ThreadEntity(
        threadId = thread.threadId,
        chatId = thread.chatId,
        rootMessageId = thread.rootMessageId,
        rootSenderPeerId = thread.rootSenderPeerId,
        createdByPeerId = thread.createdByPeerId,
        createdAt = thread.createdAt.toString(),
        updatedAt = thread.updatedAt.toString(),
        replyCount = thread.replyCount,
        lastReplyMessageId = thread.lastReplyMessageId,
        participantPeerIds = thread.participantPeerIds.toList(),
    )

    /** Преобразует сущность хранения в доменную модель треда. */
    fun fromEntity(entity: ThreadEntity): ChatThread = ChatThread(
        threadId = entity.threadId,
        chatId = entity.chatId,
        rootMessageId = entity.rootMessageId,
        rootSenderPeerId = entity.rootSenderPeerId,
        createdByPeerId = entity.createdByPeerId,
        createdAt = Instant.parse(entity.createdAt),
        updatedAt = Instant.parse(entity.updatedAt),
        replyCount = entity.replyCount,
        lastReplyMessageId = entity.lastReplyMessageId,
        participantPeerIds = entity.participantPeerIds.toSet(),
    )
}

/** Маппер сообщения треда. */
object ThreadMessageEntityMapper {
    /** Преобразует сообщение треда в сущность хранения. */
    fun toEntity(message: ThreadMessage): ThreadMessageEntity = ThreadMessageEntity(
        threadId = message.threadId,
        chatId = message.chatId,
        rootMessageId = message.rootMessageId,
        messageId = message.messageId,
        senderPeerId = message.senderPeerId,
        body = message.body,
        parentMessageId = message.parentMessageId,
        replyToMessageId = message.replyToMessageId,
        deliveryStatus = message.deliveryStatus.name,
        createdAt = message.createdAt.toString(),
        deliveredAt = message.deliveredAt?.toString(),
        failedAt = message.failedAt?.toString(),
    )

    /** Преобразует сущность хранения в сообщение треда. */
    fun fromEntity(entity: ThreadMessageEntity): ThreadMessage = ThreadMessage(
        threadId = entity.threadId,
        chatId = entity.chatId,
        rootMessageId = entity.rootMessageId,
        messageId = entity.messageId,
        senderPeerId = entity.senderPeerId,
        body = entity.body,
        parentMessageId = entity.parentMessageId,
        replyToMessageId = entity.replyToMessageId,
        deliveryStatus = MessageDeliveryStatus.valueOf(entity.deliveryStatus),
        createdAt = Instant.parse(entity.createdAt),
        deliveredAt = entity.deliveredAt?.let(Instant::parse),
        failedAt = entity.failedAt?.let(Instant::parse),
    )
}

/** Маппер системного события группы. */
object GroupEventEntityMapper {
    private val attributesSerializer = MapSerializer(String.serializer(), String.serializer())

    /** Преобразует событие в сущность хранения. */
    fun toEntity(event: GroupChatEvent): GroupEventEntity = GroupEventEntity(
        eventId = event.eventId,
        chatId = event.chatId,
        eventType = event.eventType.name,
        actorPeerId = event.actorPeerId,
        subjectPeerId = event.subjectPeerId,
        text = event.text,
        attributesJson = Json.encodeToString(attributesSerializer, event.attributes),
        createdAt = event.createdAt.toString(),
    )

    /** Преобразует сущность хранения в событие группы. */
    fun fromEntity(entity: GroupEventEntity): GroupChatEvent = GroupChatEvent(
        eventId = entity.eventId,
        chatId = entity.chatId,
        eventType = GroupEventType.valueOf(entity.eventType),
        actorPeerId = entity.actorPeerId,
        subjectPeerId = entity.subjectPeerId,
        text = entity.text,
        attributes = Json.decodeFromString(attributesSerializer, entity.attributesJson),
        createdAt = Instant.parse(entity.createdAt),
    )
}

