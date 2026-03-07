package org.expert.link.mesh.domain.model.messaging

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Краткая сводка чата для списка диалогов. */
@Serializable
data class ChatSummary(
    val chatId: String,
    val chatType: ChatType,
    val title: String,
    val lastMessageId: String? = null,
    val lastMessagePreview: String? = null,
    val unreadCount: Int = 0,
    val participantCount: Int = 0,
    val updatedAt: Instant,
)

/** Нормализованная модель direct-чата. */
@Serializable
data class DirectChat(
    val chatId: String,
    val localPeerId: String,
    val remotePeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastMessageId: String? = null,
)

/** Нормализованная модель группового чата. */
@Serializable
data class GroupChat(
    val chatId: String,
    val title: String,
    val description: String? = null,
    val createdByPeerId: String,
    val members: List<ChatMember>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastMessageId: String? = null,
    val pinned: Boolean = false,
    val archived: Boolean = false,
)

/** Тип системного события группы. */
@Serializable
enum class GroupEventType {
    GROUP_CREATED,
    GROUP_RENAMED,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    THREAD_CREATED,
}

/** Системное событие группового чата. */
@Serializable
data class GroupChatEvent(
    val eventId: String,
    val chatId: String,
    val eventType: GroupEventType,
    val actorPeerId: String,
    val subjectPeerId: String? = null,
    val text: String,
    val attributes: Map<String, String> = emptyMap(),
    val createdAt: Instant,
)

/** Метаданные треда внутри чата. */
@Serializable
data class ChatThread(
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

/** Сообщение, входящее в конкретный тред. */
@Serializable
data class ThreadMessage(
    val threadId: String,
    val chatId: String,
    val rootMessageId: String,
    val messageId: String,
    val senderPeerId: String,
    val body: String,
    val parentMessageId: String? = null,
    val replyToMessageId: String? = null,
    val deliveryStatus: MessageDeliveryStatus,
    val createdAt: Instant,
    val deliveredAt: Instant? = null,
    val failedAt: Instant? = null,
)

