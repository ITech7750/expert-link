package org.expert.link.mesh.domain.model.call

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Тип SDP-описания. */
@Serializable
enum class SdpType {
    OFFER,
    ANSWER,
}

/** Состояние соединения media-сессии. */
@Serializable
enum class MediaConnectionState {
    NEW,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED,
    CLOSED,
}

/** Направление локальной камеры. */
@Serializable
enum class CameraFacing {
    FRONT,
    BACK,
    UNKNOWN,
}

/** SDP-описание WebRTC сессии. */
@Serializable
data class SessionDescription(
    val type: SdpType,
    val sdp: String,
)

/** ICE-кандидат WebRTC сессии. */
@Serializable
data class IceCandidate(
    val sdpMid: String?,
    val sdpMLineIndex: Int,
    val candidate: String,
)

/** Media-состояние участника звонка. */
@Serializable
data class PeerMediaState(
    val peerId: String,
    val audioEnabled: Boolean,
    val videoEnabled: Boolean,
    val hasAudioTrack: Boolean,
    val hasVideoTrack: Boolean,
    val connectionState: MediaConnectionState,
)

/** Снимок media-состояния звонка для UI и диагностики. */
@Serializable
data class CallMediaState(
    val callId: String,
    val localPeerId: String,
    val localAudioEnabled: Boolean,
    val localVideoEnabled: Boolean,
    val cameraFacing: CameraFacing,
    val connectionState: MediaConnectionState,
    val peers: List<PeerMediaState>,
    val updatedAt: Instant,
    val errorMessage: String? = null,
)

/** Снимок media-метрик WebRTC сессии. */
@Serializable
data class CallMediaStats(
    val callId: String,
    val rttMs: Int,
    val packetLossPercent: Double,
    val jitterMs: Int,
    val outboundBitrateKbps: Int,
    val inboundBitrateKbps: Int,
    val capturedAt: Instant,
)

/** Исходящее сигнальное событие от media-engine. */
@Serializable
data class WebRtcSignalEvent(
    val callId: String,
    val signalType: CallSignalType,
    val description: SessionDescription? = null,
    val iceCandidate: IceCandidate? = null,
    val createdAt: Instant,
)

/** Конфигурация WebRTC-сессии для конкретного звонка. */
@Serializable
data class WebRtcSessionConfig(
    val callId: String,
    val localPeerId: String,
    val remotePeerIds: Set<String>,
    val callType: CallType,
    val callScope: CallScope,
)
