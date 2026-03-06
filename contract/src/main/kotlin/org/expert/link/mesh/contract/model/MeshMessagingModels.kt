package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Статус доставки чат-сообщения. */
@Serializable
enum class MeshMessageDeliveryStatus {
    NEW,
    QUEUED,
    SENT,
    ACK_PENDING,
    DELIVERED,
    FAILED,
}

/** Диалог один-к-одному. */
@Serializable
data class MeshConversation(
    val conversationId: String,
    val participantPeerIds: Set<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastMessageId: String? = null,
)

/** Публичное представление чат-сообщения. */
@Serializable
data class MeshChatMessage(
    val messageId: String,
    val conversationId: String,
    val senderPeerId: String,
    val recipientPeerId: String,
    val body: String,
    val deliveryStatus: MeshMessageDeliveryStatus,
    val createdAt: Instant,
    val deliveredAt: Instant? = null,
    val failedAt: Instant? = null,
)

/** Квитанция после обработки ACK. */
@Serializable
data class MeshMessageReceipt(
    val messageId: String,
    val packetId: String,
    val conversationId: String,
    val receivedAt: Instant,
    val deliveryStatus: MeshMessageDeliveryStatus,
)
