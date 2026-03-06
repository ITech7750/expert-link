package org.expert.link.mesh.domain.entity

import kotlinx.serialization.Serializable

/** Сущность хранения диалога. */
@Serializable
data class ConversationEntity(
    val conversationId: String,
    val participantPeerIds: List<String>,
    val createdAt: String,
    val updatedAt: String,
    val lastMessageId: String? = null,
)

/** Сущность хранения сообщения. */
@Serializable
data class MessageEntity(
    val messageId: String,
    val conversationId: String,
    val senderPeerId: String,
    val recipientPeerId: String,
    val body: String,
    val deliveryStatus: String,
    val createdAt: String,
    val deliveredAt: String? = null,
    val failedAt: String? = null,
)
