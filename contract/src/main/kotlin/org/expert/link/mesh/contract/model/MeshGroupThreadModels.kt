package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Краткая сводка чата для списка. */
@Serializable
data class MeshChatSummary(
    val chatId: String,
    val chatType: MeshChatType,
    val title: String,
    val lastMessageId: String? = null,
    val lastMessagePreview: String? = null,
    val unreadCount: Int = 0,
    val participantCount: Int = 0,
    val updatedAt: Instant,
)

/** Публичная модель группового чата. */
@Serializable
data class MeshGroupChat(
    val chatId: String,
    val title: String,
    val description: String? = null,
    val createdByPeerId: String,
    val members: List<MeshChatMember>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastMessageId: String? = null,
    val pinned: Boolean = false,
    val archived: Boolean = false,
)

/** Тип системного события группы. */
@Serializable
enum class MeshGroupEventType {
    GROUP_CREATED,
    GROUP_RENAMED,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    THREAD_CREATED,
}

/** Системное событие группового чата. */
@Serializable
data class MeshGroupEvent(
    val eventId: String,
    val chatId: String,
    val eventType: MeshGroupEventType,
    val actorPeerId: String,
    val subjectPeerId: String? = null,
    val text: String,
    val attributes: Map<String, String> = emptyMap(),
    val createdAt: Instant,
)

/** Метаданные треда. */
@Serializable
data class MeshThread(
    val threadId: String,
    val chatId: String,
    val rootMessageId: String,
    val rootSenderPeerId: String,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val replyCount: Int,
    val lastReplyMessageId: String? = null,
    val participantPeerIds: Set<String> = emptySet(),
)

/** Сообщение треда. */
@Serializable
data class MeshThreadMessage(
    val threadId: String,
    val chatId: String,
    val rootMessageId: String,
    val messageId: String,
    val senderPeerId: String,
    val body: String,
    val parentMessageId: String? = null,
    val replyToMessageId: String? = null,
    val deliveryStatus: MeshMessageDeliveryStatus,
    val createdAt: Instant,
    val deliveredAt: Instant? = null,
    val failedAt: Instant? = null,
)

