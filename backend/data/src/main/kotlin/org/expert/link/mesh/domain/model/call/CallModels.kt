package org.expert.link.mesh.domain.model.call

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Тип звонка по медиа. */
@Serializable
enum class CallType {
    AUDIO,
    VIDEO,
}

/** Контекст звонка: 1:1 или группа. */
@Serializable
enum class CallScope {
    DIRECT,
    GROUP,
}

/** Состояния звонка (state machine). */
@Serializable
enum class CallState {
    NEW,
    INVITED,
    OUTGOING,
    INCOMING,
    RINGING,
    ACCEPTED,
    CONNECTING,
    ACTIVE,
    CONNECTED,
    RECONNECTING,
    ENDED,
    REJECTED,
    FAILED,
    MISSED,
    LEFT,
}

/** Совместимость со старым именем статуса. */
typealias CallStatus = CallState

/** Состояние участника звонка. */
@Serializable
enum class CallParticipantState {
    INVITED,
    RINGING,
    JOINING,
    CONNECTED,
    RECONNECTING,
    LEFT,
    DECLINED,
    FAILED,
}

/** Участник сессии звонка. */
@Serializable
data class CallParticipant(
    val peerId: String,
    val displayName: String,
    val state: CallParticipantState,
    val muted: Boolean = false,
    val videoEnabled: Boolean = true,
    val joinedAt: Instant? = null,
    val updatedAt: Instant,
)

/** Снимок качества медиа. */
@Serializable
data class MediaQualitySnapshot(
    val rttMs: Int,
    val packetLossPercent: Double,
    val jitterMs: Int,
    val bitrateKbps: Int,
    val capturedAt: Instant,
)

/** Приглашение к звонку. */
@Serializable
data class CallInvitation(
    val callId: String,
    val roomId: String,
    val conversationId: String? = null,
    val initiatorPeerId: String,
    val targetPeerIds: Set<String>,
    val callType: CallType,
    val callScope: CallScope,
    val offer: String,
    val createdAt: Instant,
)

/** Комната звонка. */
@Serializable
data class CallRoom(
    val roomId: String,
    val conversationId: String? = null,
    val scope: CallScope,
    val title: String? = null,
    val createdByPeerId: String,
    val participantPeerIds: Set<String>,
    val activeCallId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Специализированная модель групповой комнаты звонка. */
@Serializable
data class GroupCallRoom(
    val roomId: String,
    val conversationId: String? = null,
    val title: String,
    val ownerPeerId: String,
    val participantPeerIds: Set<String>,
    val activeCallId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Типы событий звонка. */
@Serializable
enum class CallEventType {
    INVITED,
    ACCEPTED,
    REJECTED,
    JOINED,
    LEFT,
    STATE_CHANGED,
    PARTICIPANT_UPDATED,
    RECONNECTING,
    RECONNECTED,
    ENDED,
    FAILED,
    QUALITY_UPDATED,
}

/** Событие жизненного цикла звонка. */
@Serializable
data class CallEvent(
    val eventId: String,
    val callId: String,
    val roomId: String,
    val eventType: CallEventType,
    val actorPeerId: String,
    val subjectPeerId: String? = null,
    val state: CallState? = null,
    val participantState: CallParticipantState? = null,
    val note: String? = null,
    val payload: Map<String, String> = emptyMap(),
    val createdAt: Instant,
)

/** Активная или завершённая сессия звонка. */
@Serializable
data class CallSession(
    val callId: String,
    val roomId: String = callId,
    val conversationId: String? = null,
    val initiatorPeerId: String,
    val recipientPeerId: String,
    val callType: CallType = CallType.AUDIO,
    val callScope: CallScope = CallScope.DIRECT,
    val targetPeerIds: Set<String> = setOf(recipientPeerId),
    val status: CallStatus,
    val participants: List<CallParticipant> = emptyList(),
    val invitation: CallInvitation? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSignalAt: Instant? = null,
    val qualitySnapshot: MediaQualitySnapshot? = null,
    val reconnectAttempts: Int = 0,
    val metadata: Map<String, String> = emptyMap(),
)

/** Типы сигнальных сообщений звонка. */
@Serializable
enum class CallSignalType {
    INVITE,
    ACCEPT,
    REJECT,
    JOIN,
    LEAVE,
    HANGUP,
    SDP_OFFER,
    SDP_ANSWER,
    ICE_CANDIDATE,
    RINGING,
    ACCEPTED,
    REJECTED,
    QUALITY,
    MUTE_CHANGED,
    VIDEO_CHANGED,
    PARTICIPANT_STATE,
    RECONNECTING,
    RECONNECTED,
}

/** Сигнальные данные звонка. */
@Serializable
data class CallSignal(
    val callId: String,
    val roomId: String? = null,
    val signalType: CallSignalType,
    val senderPeerId: String,
    val recipientPeerId: String,
    val callType: CallType? = null,
    val callScope: CallScope? = null,
    val participantState: CallParticipantState? = null,
    val muted: Boolean? = null,
    val videoEnabled: Boolean? = null,
    val correlationId: String? = null,
    val payload: String,
    val createdAt: Instant,
)
