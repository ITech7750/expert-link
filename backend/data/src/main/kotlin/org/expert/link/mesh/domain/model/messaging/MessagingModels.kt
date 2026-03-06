package org.expert.link.mesh.domain.model.messaging

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Статус доставки локального сообщения. */
@Serializable
enum class MessageDeliveryStatus {
    NEW,
    QUEUED,
    SENT,
    ACK_PENDING,
    DELIVERED,
    FAILED,
}

/** Диалог между двумя узлами. */
@Serializable
data class Conversation(
    val conversationId: String,
    val participantPeerIds: Set<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastMessageId: String? = null,
)

/** Неизменяемое чат-сообщение. */
@Serializable
data class ChatMessage(
    val messageId: String,
    val conversationId: String,
    val senderPeerId: String,
    val recipientPeerId: String,
    val body: String,
    val deliveryStatus: MessageDeliveryStatus,
    val createdAt: Instant,
    val deliveredAt: Instant? = null,
    val failedAt: Instant? = null,
)

/** Входная модель для отправки сообщения. */
@Serializable
data class OutgoingMessage(
    val targetPeerId: String,
    val conversationId: String? = null,
    val body: String,
    val requestedAt: Instant,
)

/** Квитанция после доставки или локальной обработки. */
@Serializable
data class MessageReceipt(
    val messageId: String,
    val packetId: String,
    val conversationId: String,
    val receivedAt: Instant,
    val deliveryStatus: MessageDeliveryStatus,
)
