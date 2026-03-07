package org.expert.link.mesh.domain.model.messaging

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Тип чата. */
@Serializable
enum class ChatType {
    DIRECT,
    GROUP,
}

/** Роль участника чата. */
@Serializable
enum class ChatMemberRole {
    OWNER,
    ADMIN,
    MEMBER,
}

/** Участник чата. */
@Serializable
data class ChatMember(
    val peerId: String,
    val displayName: String,
    val role: ChatMemberRole,
    val joinedAt: Instant,
)

/** Тип сообщения. */
@Serializable
enum class MessageType {
    TEXT,
    SYSTEM,
    FILE,
    CALL_EVENT,
}

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
    val chatType: ChatType,
    val title: String,
    val description: String? = null,
    val createdByPeerId: String,
    val participantPeerIds: Set<String>,
    val members: List<ChatMember> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastMessageId: String? = null,
    val unreadCount: Int = 0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
)

/** Неизменяемое чат-сообщение. */
@Serializable
data class ChatMessage(
    val messageId: String,
    val conversationId: String,
    val senderPeerId: String,
    val recipientPeerId: String,
    val body: String,
    val messageType: MessageType = MessageType.TEXT,
    val threadRootMessageId: String? = null,
    val parentMessageId: String? = null,
    val replyToMessageId: String? = null,
    val threadReplyCount: Int = 0,
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

/** Сводка треда внутри чата. */
@Serializable
data class ThreadSummary(
    val threadId: String,
    val chatId: String,
    val rootMessageId: String,
    val replyCount: Int,
    val lastReplyAt: Instant? = null,
    val participantPeerIds: Set<String> = emptySet(),
)
