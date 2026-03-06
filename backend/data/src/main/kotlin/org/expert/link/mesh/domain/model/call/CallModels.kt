package org.expert.link.mesh.domain.model.call

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Состояния звонка. */
@Serializable
enum class CallStatus {
    NEW,
    INVITED,
    RINGING,
    ACTIVE,
    ENDED,
    REJECTED,
    FAILED,
}

/** Типы сигнальных событий звонка. */
@Serializable
enum class CallSignalType {
    SDP_OFFER,
    SDP_ANSWER,
    ICE_CANDIDATE,
    RINGING,
    ACCEPTED,
    REJECTED,
    QUALITY,
}

/** Снимок качества медиа. */
@Serializable
data class MediaQualitySnapshot(
    val rttMs: Int,
    val packetLossPercent: Double,
    val jitterMs: Int,
    val bitrateKbps: Int,
    val capturedAt: Instant,
)

/** Активная или завершённая сессия звонка. */
@Serializable
data class CallSession(
    val callId: String,
    val conversationId: String? = null,
    val initiatorPeerId: String,
    val recipientPeerId: String,
    val status: CallStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSignalAt: Instant? = null,
    val qualitySnapshot: MediaQualitySnapshot? = null,
)

/** Сигнальные данные звонка. */
@Serializable
data class CallSignal(
    val callId: String,
    val signalType: CallSignalType,
    val senderPeerId: String,
    val recipientPeerId: String,
    val payload: String,
    val createdAt: Instant,
)
