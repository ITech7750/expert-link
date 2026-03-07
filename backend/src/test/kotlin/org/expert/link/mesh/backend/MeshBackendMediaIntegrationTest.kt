package org.expert.link.mesh.backend

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.contract.api.MeshAcceptCallCommand
import org.expert.link.mesh.contract.api.MeshMediaEngine
import org.expert.link.mesh.contract.api.MeshMediaSessionConfig
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.api.MeshStartCallCommand
import org.expert.link.mesh.contract.api.MeshToggleCameraCommand
import org.expert.link.mesh.contract.api.MeshToggleMicrophoneCommand
import org.expert.link.mesh.contract.api.MeshWebRtcSession
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.model.MeshCallMediaState
import org.expert.link.mesh.contract.model.MeshCallSignalType
import org.expert.link.mesh.contract.model.MeshCameraFacing
import org.expert.link.mesh.contract.model.MeshIceCandidate
import org.expert.link.mesh.contract.model.MeshMediaConnectionState
import org.expert.link.mesh.contract.model.MeshMediaStats
import org.expert.link.mesh.contract.model.MeshPeerMediaState
import org.expert.link.mesh.contract.model.MeshSdpType
import org.expert.link.mesh.contract.model.MeshSessionDescription
import org.expert.link.mesh.contract.model.MeshWebRtcSignalEvent
import org.junit.jupiter.api.Test

class MeshBackendMediaIntegrationTest {
    @Test
    fun `should wire media engine with call lifecycle and media controls`() {
        runBlocking {
            val aliceMedia = FakeMeshMediaEngine("alice")
            val bobMedia = FakeMeshMediaEngine("bob")
            val alice = MeshBackend.launch(config("media-a", 19981), mediaEngine = aliceMedia)
            val bob = MeshBackend.launch(config("media-b", 19982), mediaEngine = bobMedia)
            try {
                connectBidirectional(alice, bob)
                pair(alice, bob)

                val outbound = alice.startAudioCall(
                    MeshStartCallCommand(
                        targetPeerId = bob.profile.peerId,
                        offer = null,
                    ),
                )
                val incoming = waitForAnyCallSnapshot(bob, outbound.callId)
                assertThat(incoming).isNotNull()

                bob.acceptCall(
                    MeshAcceptCallCommand(
                        callId = outbound.callId,
                        recipientPeerId = alice.profile.peerId,
                        answer = null,
                    ),
                )
                delay(700)

                val mediaState = alice.observeMediaState(outbound.callId)
                val mediaStats = alice.observeMediaStats(outbound.callId)
                val micOff = alice.toggleMicrophone(MeshToggleMicrophoneCommand(outbound.callId, enabled = false))
                val camOff = alice.toggleCamera(MeshToggleCameraCommand(outbound.callId, enabled = false))
                val switched = alice.switchCamera(outbound.callId)

                assertThat(aliceMedia.offerCount(outbound.callId)).isGreaterThanOrEqualTo(1)
                assertThat(bobMedia.answerCount(outbound.callId)).isGreaterThanOrEqualTo(1)
                assertThat(mediaState).isNotNull()
                assertThat(mediaState?.connectionState).isEqualTo(MeshMediaConnectionState.CONNECTED)
                assertThat(mediaStats).isNotNull()
                assertThat(micOff?.localAudioEnabled).isFalse()
                assertThat(camOff?.localVideoEnabled).isFalse()
                assertThat(switched?.cameraFacing).isNotEqualTo(MeshCameraFacing.UNKNOWN)
            } finally {
                runCatching { alice.stop() }
                runCatching { bob.stop() }
            }
        }
    }

    private suspend fun pair(left: MeshNode, right: MeshNode) {
        val invite = right.createPairingInvite()
        left.pairWithInvite(invite)
        delay(500)
    }

    private suspend fun connectBidirectional(left: MeshNode, right: MeshNode) {
        left.rememberPeerEndpoint(right.profile.peerId, right.endpoint)
        right.rememberPeerEndpoint(left.profile.peerId, left.endpoint)
    }

    private suspend fun waitForAnyCallSnapshot(node: MeshNode, callId: String): org.expert.link.mesh.contract.model.MeshCallSession? {
        repeat(20) {
            val incoming = node.observeIncomingCalls().firstOrNull { it.callId == callId }
            if (incoming != null) {
                return incoming
            }
            val anySession = node.callSessions().firstOrNull { it.callId == callId }
            if (anySession != null) {
                return anySession
            }
            delay(150)
        }
        return null
    }

    private fun config(name: String, port: Int): MeshNodeConfig = MeshNodeConfig(
        displayName = name,
        bindHost = "127.0.0.1",
        httpPort = port,
        discoveryPort = port + 1_000,
        multicastGroup = "239.31.31.31",
        featureFlags = MeshFeatureFlags(
            discoveryEnabled = false,
            relayEnabled = false,
            inMemoryTransport = true,
            inMemoryDiscovery = true,
        ),
    )
}

private class FakeMeshMediaEngine(
    private val owner: String,
) : MeshMediaEngine {
    private val sessions = linkedMapOf<String, FakeMeshWebRtcSession>()

    override val isSupported: Boolean = true

    override suspend fun openSession(config: MeshMediaSessionConfig): MeshWebRtcSession {
        return sessions.getOrPut(config.callId) { FakeMeshWebRtcSession(owner, config) }
    }

    override suspend fun findSession(callId: String): MeshWebRtcSession? = sessions[callId]

    override suspend fun listSessions(): List<MeshWebRtcSession> = sessions.values.toList()

    override suspend fun closeSession(callId: String) {
        sessions.remove(callId)?.close()
    }

    fun offerCount(callId: String): Int = sessions[callId]?.offerCount ?: 0

    fun answerCount(callId: String): Int = sessions[callId]?.answerCount ?: 0
}

private class FakeMeshWebRtcSession(
    private val owner: String,
    config: MeshMediaSessionConfig,
) : MeshWebRtcSession {
    override val callId: String = config.callId
    override val localPeerId: String = config.localPeerId
    override val remotePeerIds: Set<String> = config.remotePeerIds

    private val signalFlow = MutableSharedFlow<MeshWebRtcSignalEvent>(extraBufferCapacity = 16)
    private val statsFlow = MutableSharedFlow<MeshMediaStats>(extraBufferCapacity = 4)
    private val stateFlow = MutableStateFlow(
        MeshCallMediaState(
            callId = callId,
            localPeerId = localPeerId,
            localAudioEnabled = true,
            localVideoEnabled = config.callType.name == "VIDEO",
            cameraFacing = MeshCameraFacing.FRONT,
            connectionState = MeshMediaConnectionState.CONNECTED,
            peers = remotePeerIds.map { peerId ->
                MeshPeerMediaState(
                    peerId = peerId,
                    audioEnabled = true,
                    videoEnabled = true,
                    hasAudioTrack = true,
                    hasVideoTrack = config.callType.name == "VIDEO",
                    connectionState = MeshMediaConnectionState.CONNECTED,
                )
            },
            updatedAt = Clock.System.now(),
            errorMessage = null,
        ),
    )

    var offerCount: Int = 0
        private set
    var answerCount: Int = 0
        private set

    override suspend fun createOffer(): MeshSessionDescription {
        offerCount += 1
        return MeshSessionDescription(MeshSdpType.OFFER, "offer-$owner-$callId")
    }

    override suspend fun createAnswer(): MeshSessionDescription {
        answerCount += 1
        return MeshSessionDescription(MeshSdpType.ANSWER, "answer-$owner-$callId")
    }

    override suspend fun setRemoteDescription(description: MeshSessionDescription) = Unit

    override suspend fun addIceCandidate(candidate: MeshIceCandidate) = Unit

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
            cameraFacing = if (stateFlow.value.cameraFacing == MeshCameraFacing.FRONT) MeshCameraFacing.BACK else MeshCameraFacing.FRONT,
            updatedAt = Clock.System.now(),
        )
    }

    override suspend fun attachLocalRenderer(rendererId: String) = Unit

    override suspend fun attachRemoteRenderer(peerId: String, rendererId: String) = Unit

    override suspend fun detachRenderer(rendererId: String) = Unit

    override suspend fun currentState(): MeshCallMediaState = stateFlow.value

    override suspend fun currentStats(): MeshMediaStats {
        return MeshMediaStats(
            callId = callId,
            rttMs = 24,
            packetLossPercent = 0.2,
            jitterMs = 3,
            outboundBitrateKbps = 512,
            inboundBitrateKbps = 480,
            capturedAt = Clock.System.now(),
        )
    }

    override fun signalEvents(): Flow<MeshWebRtcSignalEvent> = signalFlow.asSharedFlow()

    override fun stateUpdates(): Flow<MeshCallMediaState> = stateFlow.asStateFlow()

    override fun statsUpdates(): Flow<MeshMediaStats> = statsFlow.asSharedFlow()

    override suspend fun close() = Unit
}
