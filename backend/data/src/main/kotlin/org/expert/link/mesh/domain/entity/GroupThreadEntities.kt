package org.expert.link.mesh.domain.entity

import kotlinx.serialization.Serializable

/** Сущность хранения группового чата. */
@Serializable
data class GroupChatEntity(
    val chatId: String,
    val title: String,
    val description: String? = null,
    val createdByPeerId: String,
    val membersJson: String,
    val createdAt: String,
    val updatedAt: String,
    val lastMessageId: String? = null,
    val pinned: Boolean = false,
    val archived: Boolean = false,
)

/** Сущность хранения участника группового чата. */
@Serializable
data class ChatMemberEntity(
    val chatId: String,
    val peerId: String,
    val displayName: String,
    val role: String,
    val joinedAt: String,
)

/** Сущность хранения метаданных треда. */
@Serializable
data class ThreadEntity(
    val threadId: String,
    val chatId: String,
    val rootMessageId: String,
    val rootSenderPeerId: String,
    val createdByPeerId: String,
    val createdAt: String,
    val updatedAt: String,
    val replyCount: Int,
    val lastReplyMessageId: String? = null,
    val participantPeerIds: List<String>,
)

/** Сущность хранения сообщения треда. */
@Serializable
data class ThreadMessageEntity(
    val threadId: String,
    val chatId: String,
    val rootMessageId: String,
    val messageId: String,
    val senderPeerId: String,
    val body: String,
    val parentMessageId: String? = null,
    val replyToMessageId: String? = null,
    val deliveryStatus: String,
    val createdAt: String,
    val deliveredAt: String? = null,
    val failedAt: String? = null,
)

/** Сущность хранения системного события группы. */
@Serializable
data class GroupEventEntity(
    val eventId: String,
    val chatId: String,
    val eventType: String,
    val actorPeerId: String,
    val subjectPeerId: String? = null,
    val text: String,
    val attributesJson: String,
    val createdAt: String,
)

