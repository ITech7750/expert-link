package org.expert.link.mesh.backend

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.contract.api.MeshAcceptCallCommand
import org.expert.link.mesh.contract.api.MeshEndCallCommand
import org.expert.link.mesh.contract.api.MeshJoinCallCommand
import org.expert.link.mesh.contract.api.MeshLeaveCallCommand
import org.expert.link.mesh.contract.api.MeshStartCallCommand
import org.expert.link.mesh.contract.api.MeshStartGroupCallCommand
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.junit.jupiter.api.Test

class MeshBackendCallIntegrationTest {
    @Test
    fun `should complete direct audio call lifecycle via public contract`() {
        runBlocking {
            val alice = MeshBackend.launch(config("call-a", 19781))
            val bob = MeshBackend.launch(config("call-b", 19782))
            try {
                connectBidirectional(alice, bob)
                pair(alice, bob)

                val outbound = alice.startAudioCall(
                    MeshStartCallCommand(
                        targetPeerId = bob.profile.peerId,
                        offer = "v=0 audio-offer",
                    ),
                )
                delay(500)
                val incoming = bob.observeIncomingCalls().firstOrNull { it.callId == outbound.callId }
                assertThat(incoming).isNotNull()

                val accepted = bob.acceptCall(
                    MeshAcceptCallCommand(
                        callId = outbound.callId,
                        recipientPeerId = alice.profile.peerId,
                        answer = "v=0 audio-answer",
                    ),
                )
                delay(500)

                val active = alice.observeActiveCall().firstOrNull { it.callId == outbound.callId }
                val participants = alice.observeCallParticipants(outbound.callId)
                val events = alice.observeCallEvents(outbound.callId, 64)
                val ended = alice.endCall(MeshEndCallCommand(outbound.callId, "test-complete"))
                delay(350)

                assertThat(accepted.callId).isEqualTo(outbound.callId)
                assertThat(active).isNotNull()
                assertThat(participants.map { it.peerId }).contains(alice.profile.peerId, bob.profile.peerId)
                assertThat(events.map { it.eventType.name }).contains("INVITED")
                assertThat(ended?.status?.name).isEqualTo("ENDED")
            } finally {
                runCatching { alice.stop() }
                runCatching { bob.stop() }
            }
        }
    }

    @Test
    fun `should complete group video call lifecycle with join and leave`() {
        runBlocking {
            val alice = MeshBackend.launch(config("group-call-a", 19881))
            val bob = MeshBackend.launch(config("group-call-b", 19882))
            val carol = MeshBackend.launch(config("group-call-c", 19883))
            try {
                connectBidirectional(alice, bob)
                connectBidirectional(alice, carol)
                connectBidirectional(bob, carol)
                pair(alice, bob)
                pair(alice, carol)
                pair(bob, carol)

                val group = alice.startGroupVideoCall(
                    MeshStartGroupCallCommand(
                        targetPeerIds = setOf(bob.profile.peerId, carol.profile.peerId),
                        offer = "v=0 group-video-offer",
                        roomTitle = "Тест-команда",
                    ),
                )
                delay(500)
                val joinBob = bob.joinCall(
                    MeshJoinCallCommand(
                        callId = group.callId,
                        recipientPeerId = alice.profile.peerId,
                        answer = "join-bob",
                    ),
                )
                val joinCarol = carol.joinCall(
                    MeshJoinCallCommand(
                        callId = group.callId,
                        recipientPeerId = alice.profile.peerId,
                        answer = "join-carol",
                    ),
                )
                delay(500)
                val participantsAfterJoin = alice.observeCallParticipants(group.callId)

                val leaveCarol = carol.leaveCall(
                    MeshLeaveCallCommand(
                        callId = group.callId,
                        recipientPeerId = alice.profile.peerId,
                        reason = "left-test",
                    ),
                )
                delay(350)
                val events = alice.observeCallEvents(group.callId, 128)
                val ended = alice.endCall(MeshEndCallCommand(group.callId, "group-complete"))

                assertThat(joinBob.signalType.name).isEqualTo("JOIN")
                assertThat(joinCarol.signalType.name).isEqualTo("JOIN")
                assertThat(leaveCarol.signalType.name).isEqualTo("LEAVE")
                assertThat(participantsAfterJoin.size).isGreaterThanOrEqualTo(3)
                assertThat(events.map { it.eventType.name }).contains("INVITED", "JOINED")
                assertThat(ended?.status?.name).isEqualTo("ENDED")
            } finally {
                runCatching { alice.stop() }
                runCatching { bob.stop() }
                runCatching { carol.stop() }
            }
        }
    }

    private suspend fun pair(left: org.expert.link.mesh.contract.api.MeshNode, right: org.expert.link.mesh.contract.api.MeshNode) {
        val invite = right.createPairingInvite()
        left.pairWithInvite(invite)
        delay(500)
    }

    private suspend fun connectBidirectional(left: org.expert.link.mesh.contract.api.MeshNode, right: org.expert.link.mesh.contract.api.MeshNode) {
        left.rememberPeerEndpoint(right.profile.peerId, right.endpoint)
        right.rememberPeerEndpoint(left.profile.peerId, left.endpoint)
    }

    private fun config(name: String, port: Int): MeshNodeConfig = MeshNodeConfig(
        displayName = name,
        bindHost = "127.0.0.1",
        httpPort = port,
        discoveryPort = port + 1_000,
        multicastGroup = "239.30.30.30",
        featureFlags = MeshFeatureFlags(
            discoveryEnabled = false,
            relayEnabled = false,
            inMemoryTransport = true,
            inMemoryDiscovery = true,
        ),
    )
}
