package org.expert.link.mesh.backend.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.expert.link.mesh.contract.api.MeshMediaEngine
import org.expert.link.mesh.contract.api.MeshMediaSessionConfig
import org.expert.link.mesh.contract.api.MeshWebRtcSession
import org.expert.link.mesh.domain.model.call.CallMediaState
import org.expert.link.mesh.domain.model.call.CallMediaStats
import org.expert.link.mesh.domain.model.call.IceCandidate
import org.expert.link.mesh.domain.model.call.SessionDescription
import org.expert.link.mesh.domain.model.call.WebRtcSessionConfig
import org.expert.link.mesh.domain.model.call.WebRtcSignalEvent
import org.expert.link.mesh.domain.port.external.MediaEnginePort
import org.expert.link.mesh.domain.port.external.WebRtcSessionPort

internal fun MeshMediaEngine.toDomainPort(): MediaEnginePort = ContractMediaEngineAdapter(this)

/** Адаптер contract-level media-engine к domain media-портам runtime. */
private class ContractMediaEngineAdapter(
    private val delegate: MeshMediaEngine,
) : MediaEnginePort {
    override val isSupported: Boolean
        get() = delegate.isSupported

    override suspend fun openSession(config: WebRtcSessionConfig): WebRtcSessionPort {
        return ContractWebRtcSessionAdapter(
            delegate.openSession(
                MeshMediaSessionConfig(
                    callId = config.callId,
                    localPeerId = config.localPeerId,
                    remotePeerIds = config.remotePeerIds,
                    callType = config.callType.toContract(),
                    callScope = config.callScope.toContract(),
                ),
            ),
        )
    }

    override suspend fun findSession(callId: String): WebRtcSessionPort? {
        return delegate.findSession(callId)?.let(::ContractWebRtcSessionAdapter)
    }

    override suspend fun listSessions(): List<WebRtcSessionPort> {
        return delegate.listSessions().map(::ContractWebRtcSessionAdapter)
    }

    override suspend fun closeSession(callId: String) {
        delegate.closeSession(callId)
    }
}

/** Адаптер contract-level WebRTC сессии к domain media-порту. */
private class ContractWebRtcSessionAdapter(
    private val delegate: MeshWebRtcSession,
) : WebRtcSessionPort {
    override val callId: String
        get() = delegate.callId

    override val localPeerId: String
        get() = delegate.localPeerId

    override val remotePeerIds: Set<String>
        get() = delegate.remotePeerIds

    override suspend fun createOffer(): SessionDescription = delegate.createOffer().toDomain()

    override suspend fun createAnswer(): SessionDescription = delegate.createAnswer().toDomain()

    override suspend fun setRemoteDescription(description: SessionDescription) {
        delegate.setRemoteDescription(description.toContract())
    }

    override suspend fun addIceCandidate(candidate: IceCandidate) {
        delegate.addIceCandidate(candidate.toContract())
    }

    override suspend fun currentState(): CallMediaState = delegate.currentState().toDomain()

    override suspend fun currentStats(): CallMediaStats? = delegate.currentStats()?.toDomain()

    override fun signalEvents(): Flow<WebRtcSignalEvent> = delegate.signalEvents().map { it.toDomain() }

    override fun stateUpdates(): Flow<CallMediaState> = delegate.stateUpdates().map { it.toDomain() }

    override fun statsUpdates(): Flow<CallMediaStats> = delegate.statsUpdates().map { it.toDomain() }

    override suspend fun setMicrophoneEnabled(enabled: Boolean) {
        delegate.setMicrophoneEnabled(enabled)
    }

    override suspend fun isMicrophoneEnabled(): Boolean = delegate.isMicrophoneEnabled()

    override suspend fun setCameraEnabled(enabled: Boolean) {
        delegate.setCameraEnabled(enabled)
    }

    override suspend fun isCameraEnabled(): Boolean = delegate.isCameraEnabled()

    override suspend fun switchCamera() {
        delegate.switchCamera()
    }

    override suspend fun cameraFacing() = delegate.currentState().cameraFacing.toDomain()

    override suspend fun attachLocalRenderer(rendererId: String) {
        delegate.attachLocalRenderer(rendererId)
    }

    override suspend fun attachRemoteRenderer(peerId: String, rendererId: String) {
        delegate.attachRemoteRenderer(peerId, rendererId)
    }

    override suspend fun detachRenderer(rendererId: String) {
        delegate.detachRenderer(rendererId)
    }

    override suspend fun close() {
        delegate.close()
    }
}
