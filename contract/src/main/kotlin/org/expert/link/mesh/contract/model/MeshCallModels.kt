package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Тип звонка по медиа. */
@Serializable
enum class MeshCallType {
    AUDIO,
    VIDEO,
}

/** Контекст звонка: 1:1 или группа. */
@Serializable
enum class MeshCallScope {
    DIRECT,
    GROUP,
}

/** Состояние звонка (state machine). */
@Serializable
enum class MeshCallState {
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
typealias MeshCallStatus = MeshCallState

/** Состояние участника звонка. */
@Serializable
enum class MeshCallParticipantState {
    INVITED,
    RINGING,
    JOINING,
    CONNECTED,
    RECONNECTING,
    LEFT,
    DECLINED,
    FAILED,
}

/** Участник звонка. */
@Serializable
data class MeshCallParticipant(
    val peerId: String,
    val displayName: String,
    val state: MeshCallParticipantState,
    val muted: Boolean = false,
    val videoEnabled: Boolean = true,
    val joinedAt: Instant? = null,
    val updatedAt: Instant,
)

/** Снимок качества медиа для диагностики. */
@Serializable
data class MeshMediaQualitySnapshot(
    val rttMs: Int,
    val packetLossPercent: Double,
    val jitterMs: Int,
    val bitrateKbps: Int,
    val capturedAt: Instant,
)

/** Приглашение в звонок. */
@Serializable
data class MeshCallInvitation(
    val callId: String,
    val roomId: String,
    val conversationId: String? = null,
    val initiatorPeerId: String,
    val targetPeerIds: Set<String>,
    val callType: MeshCallType,
    val callScope: MeshCallScope,
    val offer: String,
    val createdAt: Instant,
)

/** Комната звонка. */
@Serializable
data class MeshCallRoom(
    val roomId: String,
    val conversationId: String? = null,
    val scope: MeshCallScope,
    val title: String? = null,
    val createdByPeerId: String,
    val participantPeerIds: Set<String>,
    val activeCallId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Специализированная модель групповой комнаты звонка. */
@Serializable
data class MeshGroupCallRoom(
    val roomId: String,
    val conversationId: String? = null,
    val title: String,
    val ownerPeerId: String,
    val participantPeerIds: Set<String>,
    val activeCallId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Тип события звонка. */
@Serializable
enum class MeshCallEventType {
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
data class MeshCallEvent(
    val eventId: String,
    val callId: String,
    val roomId: String,
    val eventType: MeshCallEventType,
    val actorPeerId: String,
    val subjectPeerId: String? = null,
    val state: MeshCallState? = null,
    val participantState: MeshCallParticipantState? = null,
    val note: String? = null,
    val payload: Map<String, String> = emptyMap(),
    val createdAt: Instant,
)

/** Публичное состояние звонка. */
@Serializable
data class MeshCallSession(
    val callId: String,
    val roomId: String = callId,
    val conversationId: String? = null,
    val initiatorPeerId: String,
    val recipientPeerId: String,
    val callType: MeshCallType = MeshCallType.AUDIO,
    val callScope: MeshCallScope = MeshCallScope.DIRECT,
    val targetPeerIds: Set<String> = setOf(recipientPeerId),
    val status: MeshCallState,
    val participants: List<MeshCallParticipant> = emptyList(),
    val invitation: MeshCallInvitation? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSignalAt: Instant? = null,
    val qualitySnapshot: MeshMediaQualitySnapshot? = null,
    val reconnectAttempts: Int = 0,
    val metadata: Map<String, String> = emptyMap(),
)

/** Тип сигнального события звонка. */
@Serializable
enum class MeshCallSignalType {
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

/** Результат отправки сигнала звонка. */
@Serializable
data class MeshCallSignal(
    val callId: String,
    val roomId: String? = null,
    val signalType: MeshCallSignalType,
    val senderPeerId: String,
    val recipientPeerId: String,
    val callType: MeshCallType? = null,
    val callScope: MeshCallScope? = null,
    val participantState: MeshCallParticipantState? = null,
    val muted: Boolean? = null,
    val videoEnabled: Boolean? = null,
    val correlationId: String? = null,
    val payload: String,
    val createdAt: Instant,
)
