package org.expert.link.mesh.infrastructure.adapter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import org.expert.link.mesh.domain.model.call.CallMediaState
import org.expert.link.mesh.domain.model.call.CallMediaStats
import org.expert.link.mesh.domain.model.call.CameraFacing
import org.expert.link.mesh.domain.model.call.IceCandidate
import org.expert.link.mesh.domain.model.call.MediaConnectionState
import org.expert.link.mesh.domain.model.call.PeerMediaState
import org.expert.link.mesh.domain.model.call.SdpType
import org.expert.link.mesh.domain.model.call.SessionDescription
import org.expert.link.mesh.domain.model.call.WebRtcSessionConfig
import org.expert.link.mesh.domain.model.call.WebRtcSignalEvent
import org.expert.link.mesh.domain.port.external.MediaEnginePort
import org.expert.link.mesh.domain.port.external.WebRtcSessionPort

/**
 * Заглушка media-engine для JVM окружений без реального WebRTC backend.
 *
 * Адаптер честно сообщает `isSupported=false` и не эмулирует медиа-трафик.
 */
class NoopMediaEngineAdapter : MediaEnginePort {
    private val mutex = Mutex()
    private val sessions = linkedMapOf<String, NoopWebRtcSession>()

    override val isSupported: Boolean = false

    override suspend fun openSession(config: WebRtcSessionConfig): WebRtcSessionPort = mutex.withLock {
        sessions.getOrPut(config.callId) { NoopWebRtcSession(config) }
    }

    override suspend fun findSession(callId: String): WebRtcSessionPort? = mutex.withLock { sessions[callId] }

    override suspend fun listSessions(): List<WebRtcSessionPort> = mutex.withLock { sessions.values.toList() }

    override suspend fun closeSession(callId: String) {
        mutex.withLock {
            sessions.remove(callId)?.close()
        }
    }

    private class NoopWebRtcSession(
        config: WebRtcSessionConfig,
    ) : WebRtcSessionPort {
        override val callId: String = config.callId
        override val localPeerId: String = config.localPeerId
        override val remotePeerIds: Set<String> = config.remotePeerIds

        private val signalFlow = MutableSharedFlow<WebRtcSignalEvent>(extraBufferCapacity = 16)
        private val statsFlow = MutableSharedFlow<CallMediaStats>(extraBufferCapacity = 16)
        private val stateFlow = MutableStateFlow(
            CallMediaState(
                callId = config.callId,
                localPeerId = config.localPeerId,
                localAudioEnabled = true,
                localVideoEnabled = config.callType.name == "VIDEO",
                cameraFacing = CameraFacing.UNKNOWN,
                connectionState = MediaConnectionState.FAILED,
                peers = config.remotePeerIds.map { peerId ->
                    PeerMediaState(
                        peerId = peerId,
                        audioEnabled = false,
                        videoEnabled = false,
                        hasAudioTrack = false,
                        hasVideoTrack = false,
                        connectionState = MediaConnectionState.NEW,
                    )
                },
                updatedAt = Clock.System.now(),
                errorMessage = "WebRTC media engine не подключён для этой платформы",
            ),
        )

        override suspend fun createOffer(): SessionDescription {
            throw UnsupportedOperationException("WebRTC media engine недоступен")
        }

        override suspend fun createAnswer(): SessionDescription {
            throw UnsupportedOperationException("WebRTC media engine недоступен")
        }

        override suspend fun setRemoteDescription(description: SessionDescription) {
            throw UnsupportedOperationException("WebRTC media engine недоступен")
        }

        override suspend fun addIceCandidate(candidate: IceCandidate) {
            throw UnsupportedOperationException("WebRTC media engine недоступен")
        }

        override suspend fun currentState(): CallMediaState = stateFlow.value

        override suspend fun currentStats(): CallMediaStats? = null

        override fun signalEvents(): Flow<WebRtcSignalEvent> = signalFlow.asSharedFlow()

        override fun stateUpdates(): Flow<CallMediaState> = stateFlow.asStateFlow()

        override fun statsUpdates(): Flow<CallMediaStats> = statsFlow.asSharedFlow()

        override suspend fun setMicrophoneEnabled(enabled: Boolean) {
            stateFlow.value = stateFlow.value.copy(
                localAudioEnabled = enabled,
                updatedAt = Clock.System.now(),
            )
        }

        override suspend fun isMicrophoneEnabled(): Boolean = stateFlow.value.localAudioEnabled

        override suspend fun setCameraEnabled(enabled: Boolean) {
            stateFlow.value = stateFlow.value.copy(
                localVideoEnabled = enabled,
                updatedAt = Clock.System.now(),
            )
        }

        override suspend fun isCameraEnabled(): Boolean = stateFlow.value.localVideoEnabled

        override suspend fun switchCamera() {
            stateFlow.value = stateFlow.value.copy(
                cameraFacing = when (stateFlow.value.cameraFacing) {
                    CameraFacing.FRONT -> CameraFacing.BACK
                    CameraFacing.BACK -> CameraFacing.FRONT
                    CameraFacing.UNKNOWN -> CameraFacing.FRONT
                },
                updatedAt = Clock.System.now(),
            )
        }

        override suspend fun cameraFacing(): CameraFacing = stateFlow.value.cameraFacing

        override suspend fun attachLocalRenderer(rendererId: String) = Unit

        override suspend fun attachRemoteRenderer(peerId: String, rendererId: String) = Unit

        override suspend fun detachRenderer(rendererId: String) = Unit

        override suspend fun close() {
            stateFlow.value = stateFlow.value.copy(
                connectionState = MediaConnectionState.CLOSED,
                updatedAt = Clock.System.now(),
            )
        }
    }
}
