package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Состояние звонка. */
@Serializable
enum class MeshCallStatus {
    NEW,
    INVITED,
    RINGING,
    ACTIVE,
    ENDED,
    REJECTED,
    FAILED,
}

/** Тип сигнального события. */
@Serializable
enum class MeshCallSignalType {
    SDP_OFFER,
    SDP_ANSWER,
    ICE_CANDIDATE,
    RINGING,
    ACCEPTED,
    REJECTED,
    QUALITY,
}

/** Снимок качества медиа для диагностики. */
@Serializable
data class MeshMediaQualitySnapshot(
    val rttMs: Int,
    val packetLossPercent: Double,
    val jitterMs: Int,
    val bitrateKbps: Int,
    val capturedAt: Instant,
)

/** Публичное состояние звонка. */
@Serializable
data class MeshCallSession(
    val callId: String,
    val conversationId: String? = null,
    val initiatorPeerId: String,
    val recipientPeerId: String,
    val status: MeshCallStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSignalAt: Instant? = null,
    val qualitySnapshot: MeshMediaQualitySnapshot? = null,
)

/** Результат отправки сигнала звонка. */
@Serializable
data class MeshCallSignal(
    val callId: String,
    val signalType: MeshCallSignalType,
    val senderPeerId: String,
    val recipientPeerId: String,
    val payload: String,
    val createdAt: Instant,
)
