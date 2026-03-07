package org.expert.link.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "local_profile")
data class LocalProfileRecord(
    @PrimaryKey val singletonId: Int = 0,
    val peerId: String,
    val displayName: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "paired_peer",
    indices = [
        Index("displayName"),
        Index("trustState"),
        Index("pairedAt"),
    ],
)
data class PairedPeerRecord(
    @PrimaryKey val peerId: String,
    val displayName: String,
    val trustState: String,
    val pairedAt: String,
    val lastSeenAt: String?,
    val payloadJson: String,
)

@Entity(
    tableName = "pairing_session",
    indices = [
        Index("inviteSecret"),
        Index("remotePeerId"),
        Index("createdAt"),
        Index("expiresAt"),
    ],
)
data class PairingSessionRecord(
    @PrimaryKey val sessionId: String,
    val inviteSecret: String,
    val remotePeerId: String?,
    val used: Boolean,
    val createdAt: String,
    val expiresAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "blocked_peer",
    indices = [
        Index("blockedAt"),
        Index("expiresAt"),
    ],
)
data class BlockedPeerRecord(
    @PrimaryKey val peerId: String,
    val blockedAt: String,
    val expiresAt: String?,
    val payloadJson: String,
)

@Entity(
    tableName = "conversation",
    indices = [
        Index("participantKey"),
        Index("updatedAt"),
        Index("lastMessageId"),
    ],
)
data class ConversationRecord(
    @PrimaryKey val conversationId: String,
    val chatType: String,
    val title: String,
    val participantKey: String,
    val updatedAt: String,
    val lastMessageId: String?,
    val unreadCount: Int,
    val payloadJson: String,
)

@Entity(
    tableName = "message",
    indices = [
        Index("conversationId"),
        Index("threadRootMessageId"),
        Index("createdAt"),
        Index("deliveryStatus"),
    ],
)
data class MessageRecord(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val threadRootMessageId: String?,
    val senderPeerId: String,
    val recipientPeerId: String,
    val deliveryStatus: String,
    val createdAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "file_transfer",
    indices = [
        Index("conversationId"),
        Index("recipientPeerId"),
        Index("status"),
        Index("updatedAt"),
    ],
)
data class FileTransferRecord(
    @PrimaryKey val transferId: String,
    val conversationId: String?,
    val recipientPeerId: String,
    val status: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "call_session",
    indices = [
        Index("roomId"),
        Index("status"),
        Index("updatedAt"),
    ],
)
data class CallSessionRecord(
    @PrimaryKey val callId: String,
    val roomId: String,
    val status: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "call_room",
    indices = [
        Index("activeCallId"),
        Index("updatedAt"),
    ],
)
data class CallRoomRecord(
    @PrimaryKey val roomId: String,
    val activeCallId: String?,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "call_participant",
    primaryKeys = ["callId", "peerId"],
    indices = [
        Index("roomId"),
        Index("updatedAt"),
    ],
)
data class CallParticipantRecord(
    val callId: String,
    val roomId: String,
    val peerId: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "call_event",
    indices = [
        Index("callId"),
        Index("roomId"),
        Index("createdAt"),
    ],
)
data class CallEventRecord(
    @PrimaryKey val eventId: String,
    val callId: String,
    val roomId: String,
    val createdAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "group_chat",
    indices = [
        Index("updatedAt"),
        Index("lastMessageId"),
    ],
)
data class GroupChatRecord(
    @PrimaryKey val chatId: String,
    val title: String,
    val updatedAt: String,
    val lastMessageId: String?,
    val payloadJson: String,
)

@Entity(
    tableName = "chat_member",
    primaryKeys = ["chatId", "peerId"],
    indices = [
        Index("joinedAt"),
    ],
)
data class ChatMemberRecord(
    val chatId: String,
    val peerId: String,
    val joinedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "thread_metadata",
    indices = [
        Index("chatId"),
        Index("rootMessageId"),
        Index("updatedAt"),
    ],
)
data class ThreadRecord(
    @PrimaryKey val threadId: String,
    val chatId: String,
    val rootMessageId: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "thread_message",
    indices = [
        Index("threadId"),
        Index("chatId"),
        Index("rootMessageId"),
        Index("createdAt"),
    ],
)
data class ThreadMessageRecord(
    @PrimaryKey val messageId: String,
    val threadId: String,
    val chatId: String,
    val rootMessageId: String,
    val createdAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "group_event",
    indices = [
        Index("chatId"),
        Index("createdAt"),
    ],
)
data class GroupEventRecord(
    @PrimaryKey val eventId: String,
    val chatId: String,
    val createdAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "event_log",
    indices = [
        Index("category"),
        Index("createdAt"),
    ],
)
data class EventLogRecord(
    @PrimaryKey val eventId: String,
    val category: String,
    val createdAt: String,
    val payloadJson: String,
)
