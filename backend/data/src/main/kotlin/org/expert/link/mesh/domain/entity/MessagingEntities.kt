package org.expert.link.mesh.domain.entity

import kotlinx.serialization.Serializable

/** Сущность хранения диалога. */
@Serializable
data class ConversationEntity(
    val conversationId: String,
    val chatType: String,
    val title: String,
    val description: String? = null,
    val createdByPeerId: String,
    val participantPeerIds: List<String>,
    val membersJson: String,
    val createdAt: String,
    val updatedAt: String,
    val lastMessageId: String? = null,
    val unreadCount: Int = 0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
)

/** Сущность хранения сообщения. */
@Serializable
data class MessageEntity(
    val messageId: String,
    val conversationId: String,
    val senderPeerId: String,
    val recipientPeerId: String,
    val body: String,
    val messageType: String,
    val threadRootMessageId: String? = null,
    val parentMessageId: String? = null,
    val replyToMessageId: String? = null,
    val threadReplyCount: Int = 0,
    val deliveryStatus: String,
    val createdAt: String,
    val deliveredAt: String? = null,
    val failedAt: String? = null,
)
