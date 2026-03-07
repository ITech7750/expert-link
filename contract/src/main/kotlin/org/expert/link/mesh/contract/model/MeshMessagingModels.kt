package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Тип чата. */
@Serializable
enum class MeshChatType {
    DIRECT,
    GROUP,
}

/** Роль участника чата. */
@Serializable
enum class MeshChatMemberRole {
    OWNER,
    ADMIN,
    MEMBER,
}

/** Участник чата. */
@Serializable
data class MeshChatMember(
    val peerId: String,
    val displayName: String,
    val role: MeshChatMemberRole,
    val joinedAt: Instant,
)

/** Тип сообщения. */
@Serializable
enum class MeshMessageType {
    TEXT,
    SYSTEM,
    FILE,
    CALL_EVENT,
}

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
    val chatType: MeshChatType,
    val title: String,
    val description: String? = null,
    val createdByPeerId: String,
    val participantPeerIds: Set<String>,
    val members: List<MeshChatMember> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastMessageId: String? = null,
    val unreadCount: Int = 0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
)

/** Публичное представление чат-сообщения. */
@Serializable
data class MeshChatMessage(
    val messageId: String,
    val conversationId: String,
    val senderPeerId: String,
    val recipientPeerId: String,
    val body: String,
    val messageType: MeshMessageType = MeshMessageType.TEXT,
    val threadRootMessageId: String? = null,
    val parentMessageId: String? = null,
    val replyToMessageId: String? = null,
    val threadReplyCount: Int = 0,
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

/** Сводка треда. */
@Serializable
data class MeshThreadSummary(
    val threadId: String,
    val chatId: String,
    val rootMessageId: String,
    val replyCount: Int,
    val lastReplyAt: Instant? = null,
    val participantPeerIds: Set<String> = emptySet(),
)
