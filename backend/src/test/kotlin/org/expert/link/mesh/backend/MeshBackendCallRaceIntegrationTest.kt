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
import org.expert.link.mesh.contract.api.MeshWebRtcSession
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.model.MeshCallMediaState
import org.expert.link.mesh.contract.model.MeshCallSignalType
import org.expert.link.mesh.contract.model.MeshCallType
import org.expert.link.mesh.contract.model.MeshCameraFacing
import org.expert.link.mesh.contract.model.MeshIceCandidate
import org.expert.link.mesh.contract.model.MeshMediaConnectionState
import org.expert.link.mesh.contract.model.MeshMediaStats
import org.expert.link.mesh.contract.model.MeshPeerMediaState
import org.expert.link.mesh.contract.model.MeshSdpType
import org.expert.link.mesh.contract.model.MeshSessionDescription
import org.expert.link.mesh.contract.model.MeshWebRtcSignalEvent
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.backend.internal.toDomain
import org.expert.link.mesh.infrastructure.adapter.InMemoryPacketTransportAdapter
import org.junit.jupiter.api.Test

class MeshBackendCallRaceIntegrationTest {
    @Test
    fun `should retry dropped call signal carrying answer`() = runBlocking {
        val aliceMedia = AnswerAwareMediaEngine(owner = "alice")
        val bobMedia = AnswerAwareMediaEngine(owner = "bob")
        val alice = MeshBackend.launch(config("race-retry-a", 20101), mediaEngine = aliceMedia)
        val bob = MeshBackend.launch(config("race-retry-b", 20102), mediaEngine = bobMedia)
        try {
            connectBidirectional(alice, bob)
            pair(alice, bob)

            val outbound = alice.startVideoCall(
                MeshStartCallCommand(
                    targetPeerId = bob.profile.peerId,
                    offer = null,
                ),
            )
            val incoming = waitForIncomingCall(bob, outbound.callId)
            assertThat(incoming).isNotNull()

            InMemoryPacketTransportAdapter.dropNext(alice.endpoint.toDomain(), PacketType.CALL_SIGNAL)

            bob.acceptCall(
                MeshAcceptCallCommand(
                    callId = outbound.callId,
                    recipientPeerId = alice.profile.peerId,
                    answer = null,
                ),
            )

            repeat(25) {
                if (aliceMedia.answerAppliedCount(outbound.callId) > 0) return@repeat
                delay(200)
            }

            assertThat(aliceMedia.answerAppliedCount(outbound.callId)).isGreaterThan(0)
        } finally {
            runCatching { alice.stop() }
            runCatching { bob.stop() }
        }
    }

    @Test
    fun `should keep incoming call actionable after remote offer arrives before local accept`() = runBlocking {
        val aliceMedia = DelayedOfferStrictMediaEngine(owner = "alice", offerDelayMillis = 0)
        val bobMedia = DelayedOfferStrictMediaEngine(owner = "bob", offerDelayMillis = 0)
        val alice = MeshBackend.launch(config("race-ui-a", 20071), mediaEngine = aliceMedia)
        val bob = MeshBackend.launch(config("race-ui-b", 20072), mediaEngine = bobMedia)
        try {
            connectBidirectional(alice, bob)
            pair(alice, bob)

            val outbound = alice.startVideoCall(
                MeshStartCallCommand(
                    targetPeerId = bob.profile.peerId,
                    offer = null,
                ),
            )

            repeat(20) {
                val incoming = bob.observeIncomingCalls().firstOrNull { it.callId == outbound.callId }
                if (incoming != null) {
                    assertThat(incoming.status.name).isIn("INCOMING", "RINGING")
                    return@runBlocking
                }
                delay(100)
            }

            val sessions = bob.callSessions().filter { it.callId == outbound.callId }
            assertThat(sessions).isNotEmpty()
            assertThat(sessions.first().status.name).isIn("INCOMING", "RINGING")
        } finally {
            runCatching { alice.stop() }
            runCatching { bob.stop() }
        }
    }

    @Test
    fun `should still send answer when accept happens before remote offer arrives`() = runBlocking {
        val aliceMedia = DelayedOfferStrictMediaEngine(owner = "alice", offerDelayMillis = 450)
        val bobMedia = DelayedOfferStrictMediaEngine(owner = "bob", offerDelayMillis = 0)
        val alice = MeshBackend.launch(config("race-call-a", 20081), mediaEngine = aliceMedia)
        val bob = MeshBackend.launch(config("race-call-b", 20082), mediaEngine = bobMedia)
        try {
            connectBidirectional(alice, bob)
            pair(alice, bob)

            val outbound = alice.startVideoCall(
                MeshStartCallCommand(
                    targetPeerId = bob.profile.peerId,
                    offer = null,
                ),
            )
            val incoming = waitForIncomingCall(bob, outbound.callId)
            assertThat(incoming).isNotNull()

            bob.acceptCall(
                MeshAcceptCallCommand(
                    callId = outbound.callId,
                    recipientPeerId = alice.profile.peerId,
                    answer = null,
                ),
            )

            repeat(15) {
                if (bobMedia.answerCount(outbound.callId) > 0) return@repeat
                delay(150)
            }

            assertThat(bobMedia.answerCount(outbound.callId)).isGreaterThanOrEqualTo(1)
        } finally {
            runCatching { alice.stop() }
            runCatching { bob.stop() }
        }
    }

    @Test
    fun `should buffer ice candidates until remote description is applied`() = runBlocking {
        val aliceMedia = EarlyIceMediaEngine(owner = "alice", offerDelayMillis = 400)
        val bobMedia = EarlyIceMediaEngine(owner = "bob", offerDelayMillis = 0)
        val alice = MeshBackend.launch(config("race-ice-a", 20091), mediaEngine = aliceMedia)
        val bob = MeshBackend.launch(config("race-ice-b", 20092), mediaEngine = bobMedia)
        try {
            connectBidirectional(alice, bob)
            pair(alice, bob)

            val outbound = alice.startVideoCall(
                MeshStartCallCommand(
                    targetPeerId = bob.profile.peerId,
                    offer = null,
                ),
            )
            val incoming = waitForIncomingCall(bob, outbound.callId)
            assertThat(incoming).isNotNull()

            bob.acceptCall(
                MeshAcceptCallCommand(
                    callId = outbound.callId,
                    recipientPeerId = alice.profile.peerId,
                    answer = null,
                ),
            )

            repeat(20) {
                if (bobMedia.appliedIceCount(outbound.callId) > 0) return@repeat
                delay(150)
            }

            assertThat(bobMedia.appliedIceCount(outbound.callId)).isGreaterThan(0)
        } finally {
            runCatching { alice.stop() }
            runCatching { bob.stop() }
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

    private suspend fun waitForIncomingCall(node: MeshNode, callId: String): org.expert.link.mesh.contract.model.MeshCallSession? {
        repeat(20) {
            val incoming = node.observeIncomingCalls().firstOrNull { it.callId == callId }
            if (incoming != null) {
                return incoming
            }
            delay(100)
        }
        return null
    }

    private fun config(name: String, port: Int): MeshNodeConfig = MeshNodeConfig(
        displayName = name,
        bindHost = "127.0.0.1",
        httpPort = port,
        discoveryPort = port + 1_000,
        multicastGroup = "239.32.32.32",
        featureFlags = MeshFeatureFlags(
            discoveryEnabled = false,
            relayEnabled = false,
            inMemoryTransport = true,
            inMemoryDiscovery = true,
        ),
    )
}

private class DelayedOfferStrictMediaEngine(
    private val owner: String,
    private val offerDelayMillis: Long,
) : MeshMediaEngine {
    private val sessions = linkedMapOf<String, DelayedOfferStrictWebRtcSession>()

    override val isSupported: Boolean = true

    override suspend fun openSession(config: MeshMediaSessionConfig): MeshWebRtcSession {
        return sessions.getOrPut(config.callId) {
            DelayedOfferStrictWebRtcSession(
                owner = owner,
                config = config,
                offerDelayMillis = offerDelayMillis,
            )
        }
    }

    override suspend fun findSession(callId: String): MeshWebRtcSession? = sessions[callId]

    override suspend fun listSessions(): List<MeshWebRtcSession> = sessions.values.toList()

    override suspend fun closeSession(callId: String) {
        sessions.remove(callId)?.close()
    }

    fun answerCount(callId: String): Int = sessions[callId]?.answerCount ?: 0
}

private class EarlyIceMediaEngine(
    private val owner: String,
    private val offerDelayMillis: Long,
) : MeshMediaEngine {
    private val sessions = linkedMapOf<String, EarlyIceWebRtcSession>()

    override val isSupported: Boolean = true

    override suspend fun openSession(config: MeshMediaSessionConfig): MeshWebRtcSession {
        return sessions.getOrPut(config.callId) {
            EarlyIceWebRtcSession(
                owner = owner,
                config = config,
                offerDelayMillis = offerDelayMillis,
            )
        }
    }

    override suspend fun findSession(callId: String): MeshWebRtcSession? = sessions[callId]

    override suspend fun listSessions(): List<MeshWebRtcSession> = sessions.values.toList()

    override suspend fun closeSession(callId: String) {
        sessions.remove(callId)?.close()
    }

    fun appliedIceCount(callId: String): Int = sessions[callId]?.appliedIceCount ?: 0
}

private class AnswerAwareMediaEngine(
    private val owner: String,
) : MeshMediaEngine {
    private val sessions = linkedMapOf<String, AnswerAwareWebRtcSession>()

    override val isSupported: Boolean = true

    override suspend fun openSession(config: MeshMediaSessionConfig): MeshWebRtcSession {
        return sessions.getOrPut(config.callId) {
            AnswerAwareWebRtcSession(owner = owner, config = config)
        }
    }

    override suspend fun findSession(callId: String): MeshWebRtcSession? = sessions[callId]

    override suspend fun listSessions(): List<MeshWebRtcSession> = sessions.values.toList()

    override suspend fun closeSession(callId: String) {
        sessions.remove(callId)?.close()
    }

    fun answerAppliedCount(callId: String): Int = sessions[callId]?.answerAppliedCount ?: 0
}

private class DelayedOfferStrictWebRtcSession(
    private val owner: String,
    config: MeshMediaSessionConfig,
    private val offerDelayMillis: Long,
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
            localVideoEnabled = config.callType == MeshCallType.VIDEO,
            cameraFacing = MeshCameraFacing.FRONT,
            connectionState = MeshMediaConnectionState.CONNECTED,
            peers = remotePeerIds.map { peerId ->
                MeshPeerMediaState(
                    peerId = peerId,
                    audioEnabled = true,
                    videoEnabled = config.callType == MeshCallType.VIDEO,
                    hasAudioTrack = true,
                    hasVideoTrack = config.callType == MeshCallType.VIDEO,
                    connectionState = MeshMediaConnectionState.CONNECTED,
                )
            },
            updatedAt = Clock.System.now(),
            errorMessage = null,
        ),
    )

    private var hasRemoteDescription: Boolean = false
    var answerCount: Int = 0
        private set

    override suspend fun createOffer(): MeshSessionDescription {
        if (offerDelayMillis > 0) {
            delay(offerDelayMillis)
        }
        return MeshSessionDescription(MeshSdpType.OFFER, "offer-$owner-$callId")
    }

    override suspend fun createAnswer(): MeshSessionDescription {
        check(hasRemoteDescription) { "Remote description is required before answer creation" }
        answerCount += 1
        return MeshSessionDescription(MeshSdpType.ANSWER, "answer-$owner-$callId")
    }

    override suspend fun setRemoteDescription(description: MeshSessionDescription) {
        hasRemoteDescription = true
    }

    override suspend fun addIceCandidate(candidate: MeshIceCandidate) = Unit

    override suspend fun setMicrophoneEnabled(enabled: Boolean) = Unit

    override suspend fun isMicrophoneEnabled(): Boolean = true

    override suspend fun setCameraEnabled(enabled: Boolean) = Unit

    override suspend fun isCameraEnabled(): Boolean = true

    override suspend fun switchCamera() = Unit

    override suspend fun attachLocalRenderer(rendererId: String) = Unit

    override suspend fun attachRemoteRenderer(peerId: String, rendererId: String) = Unit

    override suspend fun detachRenderer(rendererId: String) = Unit

    override suspend fun currentState(): MeshCallMediaState = stateFlow.value

    override suspend fun currentStats(): MeshMediaStats {
        return MeshMediaStats(
            callId = callId,
            rttMs = 20,
            packetLossPercent = 0.0,
            jitterMs = 2,
            outboundBitrateKbps = 256,
            inboundBitrateKbps = 256,
            capturedAt = Clock.System.now(),
        )
    }

    override fun signalEvents(): Flow<MeshWebRtcSignalEvent> = signalFlow.asSharedFlow()

    override fun stateUpdates(): Flow<MeshCallMediaState> = stateFlow.asStateFlow()

    override fun statsUpdates(): Flow<MeshMediaStats> = statsFlow.asSharedFlow()

    override suspend fun close() = Unit
}

private class AnswerAwareWebRtcSession(
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
            localVideoEnabled = true,
            cameraFacing = MeshCameraFacing.FRONT,
            connectionState = MeshMediaConnectionState.NEW,
            peers = remotePeerIds.map { peerId ->
                MeshPeerMediaState(
                    peerId = peerId,
                    audioEnabled = false,
                    videoEnabled = false,
                    hasAudioTrack = false,
                    hasVideoTrack = false,
                    connectionState = MeshMediaConnectionState.NEW,
                )
            },
            updatedAt = Clock.System.now(),
            errorMessage = null,
        ),
    )

    private var hasRemoteOffer = false
    var answerAppliedCount: Int = 0
        private set

    override suspend fun createOffer(): MeshSessionDescription {
        return MeshSessionDescription(MeshSdpType.OFFER, "offer-$owner-$callId")
    }

    override suspend fun createAnswer(): MeshSessionDescription {
        check(hasRemoteOffer) { "Remote offer is required before answer creation" }
        return MeshSessionDescription(MeshSdpType.ANSWER, "answer-$owner-$callId")
    }

    override suspend fun setRemoteDescription(description: MeshSessionDescription) {
        when (description.type) {
            MeshSdpType.OFFER -> hasRemoteOffer = true
            MeshSdpType.ANSWER -> answerAppliedCount += 1
        }
        stateFlow.value = stateFlow.value.copy(
            connectionState = MeshMediaConnectionState.CONNECTING,
            updatedAt = Clock.System.now(),
        )
    }

    override suspend fun addIceCandidate(candidate: MeshIceCandidate) = Unit

    override suspend fun setMicrophoneEnabled(enabled: Boolean) = Unit

    override suspend fun isMicrophoneEnabled(): Boolean = true

    override suspend fun setCameraEnabled(enabled: Boolean) = Unit

    override suspend fun isCameraEnabled(): Boolean = true

    override suspend fun switchCamera() = Unit

    override suspend fun attachLocalRenderer(rendererId: String) = Unit

    override suspend fun attachRemoteRenderer(peerId: String, rendererId: String) = Unit

    override suspend fun detachRenderer(rendererId: String) = Unit

    override suspend fun currentState(): MeshCallMediaState = stateFlow.value

    override suspend fun currentStats(): MeshMediaStats {
        return MeshMediaStats(
            callId = callId,
            rttMs = 10,
            packetLossPercent = 0.0,
            jitterMs = 1,
            outboundBitrateKbps = 128,
            inboundBitrateKbps = 128,
            capturedAt = Clock.System.now(),
        )
    }

    override fun signalEvents(): Flow<MeshWebRtcSignalEvent> = signalFlow.asSharedFlow()

    override fun stateUpdates(): Flow<MeshCallMediaState> = stateFlow.asStateFlow()

    override fun statsUpdates(): Flow<MeshMediaStats> = statsFlow.asSharedFlow()

    override suspend fun close() = Unit
}

private class EarlyIceWebRtcSession(
    private val owner: String,
    config: MeshMediaSessionConfig,
    private val offerDelayMillis: Long,
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
            localVideoEnabled = config.callType == MeshCallType.VIDEO,
            cameraFacing = MeshCameraFacing.FRONT,
            connectionState = MeshMediaConnectionState.CONNECTING,
            peers = remotePeerIds.map { peerId ->
                MeshPeerMediaState(
                    peerId = peerId,
                    audioEnabled = true,
                    videoEnabled = config.callType == MeshCallType.VIDEO,
                    hasAudioTrack = true,
                    hasVideoTrack = config.callType == MeshCallType.VIDEO,
                    connectionState = MeshMediaConnectionState.CONNECTING,
                )
            },
            updatedAt = Clock.System.now(),
            errorMessage = null,
        ),
    )

    private var hasRemoteDescription: Boolean = false
    var appliedIceCount: Int = 0
        private set

    override suspend fun createOffer(): MeshSessionDescription {
        signalFlow.tryEmit(
            MeshWebRtcSignalEvent(
                callId = callId,
                signalType = MeshCallSignalType.ICE_CANDIDATE,
                iceCandidate = MeshIceCandidate(
                    sdpMid = "0",
                    sdpMLineIndex = 0,
                    candidate = "candidate-$owner-$callId",
                ),
                createdAt = Clock.System.now(),
            ),
        )
        if (offerDelayMillis > 0) {
            delay(offerDelayMillis)
        }
        return MeshSessionDescription(MeshSdpType.OFFER, "offer-$owner-$callId")
    }

    override suspend fun createAnswer(): MeshSessionDescription {
        check(hasRemoteDescription) { "Remote description is required before answer creation" }
        return MeshSessionDescription(MeshSdpType.ANSWER, "answer-$owner-$callId")
    }

    override suspend fun setRemoteDescription(description: MeshSessionDescription) {
        hasRemoteDescription = true
        stateFlow.value = stateFlow.value.copy(
            connectionState = MeshMediaConnectionState.CONNECTING,
            updatedAt = Clock.System.now(),
        )
    }

    override suspend fun addIceCandidate(candidate: MeshIceCandidate) {
        check(hasRemoteDescription) { "ICE candidate requires remote description" }
        appliedIceCount += 1
        stateFlow.value = stateFlow.value.copy(
            connectionState = MeshMediaConnectionState.CONNECTED,
            updatedAt = Clock.System.now(),
        )
    }

    override suspend fun setMicrophoneEnabled(enabled: Boolean) = Unit

    override suspend fun isMicrophoneEnabled(): Boolean = true

    override suspend fun setCameraEnabled(enabled: Boolean) = Unit

    override suspend fun isCameraEnabled(): Boolean = true

    override suspend fun switchCamera() = Unit

    override suspend fun attachLocalRenderer(rendererId: String) = Unit

    override suspend fun attachRemoteRenderer(peerId: String, rendererId: String) = Unit

    override suspend fun detachRenderer(rendererId: String) = Unit

    override suspend fun currentState(): MeshCallMediaState = stateFlow.value

    override suspend fun currentStats(): MeshMediaStats {
        return MeshMediaStats(
            callId = callId,
            rttMs = 20,
            packetLossPercent = 0.0,
            jitterMs = 2,
            outboundBitrateKbps = 256,
            inboundBitrateKbps = 256,
            capturedAt = Clock.System.now(),
        )
    }

    override fun signalEvents(): Flow<MeshWebRtcSignalEvent> = signalFlow.asSharedFlow()

    override fun stateUpdates(): Flow<MeshCallMediaState> = stateFlow.asStateFlow()

    override fun statsUpdates(): Flow<MeshMediaStats> = statsFlow.asSharedFlow()

    override suspend fun close() = Unit
}
