package org.expert.link.mesh.application.service

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.application.support.plusSeconds
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.PendingAckRecord
import org.expert.link.mesh.domain.model.network.RouteEntry
import org.expert.link.mesh.domain.model.network.RouteHealthState
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.model.network.TransportDeliveryResult
import org.expert.link.mesh.domain.model.relay.PeerLookupResult
import org.expert.link.mesh.domain.model.relay.PeerRegistration
import org.expert.link.mesh.domain.model.relay.RelayRouteCandidate
import org.expert.link.mesh.domain.port.external.RelayGatewayPort
import org.expert.link.mesh.domain.port.external.RendezvousRegistryPort
import org.expert.link.mesh.infrastructure.repository.InMemoryCallSessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryEndpointCacheAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryFileTransferRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOutgoingQueueAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryPendingAckRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryRouteRepositoryAdapter
import org.junit.jupiter.api.Test

class TopologyStateServiceTest {
    @Test
    fun `should switch host role when preferred host is lost`() = runTest {
        val routeRepository = InMemoryRouteRepositoryAdapter()
        val connectivity = ConnectivityStrategyService(
            routeRepositoryPort = routeRepository,
            endpointCachePort = InMemoryEndpointCacheAdapter(),
            rendezvousRegistryPort = null,
            relayGatewayPort = null,
        )
        val topology = TopologyStateService(
            routeRepositoryPort = routeRepository,
            endpointCachePort = InMemoryEndpointCacheAdapter(),
            pendingAckRepositoryPort = InMemoryPendingAckRepositoryAdapter(),
            outgoingQueuePort = InMemoryOutgoingQueueAdapter(),
            fileTransferRepositoryPort = InMemoryFileTransferRepositoryAdapter(),
            callSessionRepositoryPort = InMemoryCallSessionRepositoryAdapter(),
            connectivityStrategyService = connectivity,
        )
        topology.initialize(
            localPeerId = "local-a",
            endpoint = PeerEndpoint(host = "127.0.0.1", port = 19001, announcedAt = now()),
            relayMode = org.expert.link.mesh.domain.model.network.RelayMode.DISABLED,
        )

        topology.onPeerDiscovered(
            peerId = "host-z",
            endpoint = PeerEndpoint(host = "127.0.0.1", port = 19002, announcedAt = now()),
            qualityScore = 2_000,
        )
        val beforeLoss = topology.observeHostRole()
        topology.onPeerLost("host-z", "test-host-loss")
        val afterLoss = topology.forceRefresh().networkRoleState

        assertThat(beforeLoss.currentHostPeerId).isEqualTo("host-z")
        assertThat(beforeLoss.localRole.name).isEqualTo("MEMBER")
        assertThat(afterLoss.currentHostPeerId).isEqualTo("local-a")
        assertThat(afterLoss.localRole.name).isEqualTo("HOST")
    }

    @Test
    fun `should mark route failed and reflect continuity degradation`() = runTest {
        val routeRepository = InMemoryRouteRepositoryAdapter()
        val endpointCache = InMemoryEndpointCacheAdapter()
        val pendingAckRepository = InMemoryPendingAckRepositoryAdapter()
        val outgoingQueue = InMemoryOutgoingQueueAdapter()
        val connectivity = ConnectivityStrategyService(
            routeRepositoryPort = routeRepository,
            endpointCachePort = endpointCache,
            rendezvousRegistryPort = null,
            relayGatewayPort = null,
        )
        val topology = TopologyStateService(
            routeRepositoryPort = routeRepository,
            endpointCachePort = endpointCache,
            pendingAckRepositoryPort = pendingAckRepository,
            outgoingQueuePort = outgoingQueue,
            fileTransferRepositoryPort = InMemoryFileTransferRepositoryAdapter(),
            callSessionRepositoryPort = InMemoryCallSessionRepositoryAdapter(),
            connectivityStrategyService = connectivity,
        )
        topology.initialize(
            localPeerId = "local-b",
            endpoint = PeerEndpoint(host = "127.0.0.1", port = 19011, announcedAt = now()),
            relayMode = org.expert.link.mesh.domain.model.network.RelayMode.DISABLED,
        )
        val route = RouteEntry(
            targetPeerId = "peer-b",
            nextHopPeerId = "peer-b",
            endpoint = PeerEndpoint(host = "127.0.0.1", port = 19012, announcedAt = now()),
            routeMode = RouteMode.LOCAL_DIRECT,
            hopCount = 1,
            expiresAt = now().plusSeconds(60),
            learnedAt = now(),
            direct = true,
        )
        routeRepository.save(route)
        topology.onRouteLearned(route)
        val healthy = topology.inspectRouteHealth().first { it.targetPeerId == "peer-b" }

        pendingAckRepository.save(
            PendingAckRecord(
                packetId = "packet-1",
                messageId = "message-1",
                conversationId = "conversation-1",
                targetPeerId = "peer-b",
                routeMode = RouteMode.LOCAL_DIRECT,
                attempt = 0,
                nextAttemptAt = now().plusSeconds(1),
                envelope = testEnvelope(targetPeerId = "peer-b"),
                createdAt = now(),
            ),
        )
        topology.onRouteInvalidated("peer-b", "retry-exhausted")
        routeRepository.remove("peer-b")
        val degraded = topology.forceRefresh()
        val failed = degraded.routeHealth.first { it.targetPeerId == "peer-b" }

        assertThat(healthy.state).isEqualTo(RouteHealthState.HEALTHY)
        assertThat(failed.state).isEqualTo(RouteHealthState.FAILED)
        assertThat(degraded.pendingAckCount).isEqualTo(1)
        assertThat(degraded.continuityDegraded).isTrue()
    }

    @Test
    fun `should expose relay connectivity strategy and relay mode fallback`() = runTest {
        val routeRepository = InMemoryRouteRepositoryAdapter()
        val endpointCache = InMemoryEndpointCacheAdapter()
        val rendezvous = object : RendezvousRegistryPort {
            override suspend fun registerNode(registration: PeerRegistration) = Unit
            override suspend fun heartbeat(peerId: String) = Unit
            override suspend fun findPeer(peerId: String): PeerLookupResult {
                return PeerLookupResult(
                    peerId = peerId,
                    directEndpoints = emptyList(),
                    relayCandidates = listOf(
                        RelayRouteCandidate(
                            targetPeerId = peerId,
                            relayPeerId = "relay-1",
                            endpoint = PeerEndpoint(host = "relay.local", port = 19444, announcedAt = now()),
                            expiresAt = now().plusSeconds(60),
                        ),
                    ),
                    foundAt = now(),
                    relayOnly = true,
                )
            }

            override suspend fun unregisterNode(peerId: String) = Unit
        }
        val relayGateway = object : RelayGatewayPort {
            override suspend fun relayPacket(targetPeerId: String, envelope: PacketEnvelope): TransportDeliveryResult {
                return TransportDeliveryResult(success = true)
            }
        }
        val connectivity = ConnectivityStrategyService(
            routeRepositoryPort = routeRepository,
            endpointCachePort = endpointCache,
            rendezvousRegistryPort = rendezvous,
            relayGatewayPort = relayGateway,
            forceRelayLookup = false,
        )
        val topology = TopologyStateService(
            routeRepositoryPort = routeRepository,
            endpointCachePort = endpointCache,
            pendingAckRepositoryPort = InMemoryPendingAckRepositoryAdapter(),
            outgoingQueuePort = InMemoryOutgoingQueueAdapter(),
            fileTransferRepositoryPort = InMemoryFileTransferRepositoryAdapter(),
            callSessionRepositoryPort = InMemoryCallSessionRepositoryAdapter(),
            connectivityStrategyService = connectivity,
        )
        topology.initialize(
            localPeerId = "local-r",
            endpoint = PeerEndpoint(host = "127.0.0.1", port = 19101, announcedAt = now()),
            relayMode = org.expert.link.mesh.domain.model.network.RelayMode.STANDBY,
        )

        val strategy = topology.observeConnectivityStrategy("remote-r")
        topology.onDeliveryResult(
            targetPeerId = "remote-r",
            routeMode = strategy.routeMode,
            success = true,
            viaRelayGateway = true,
        )
        val snapshot = topology.forceRefresh()

        assertThat(strategy.routeMode).isEqualTo(RouteMode.RENDEZVOUS_RELAY)
        assertThat(strategy.connectivityMode.name).isEqualTo("RELAY_PROXY")
        assertThat(snapshot.relayMode.name).isEqualTo("ACTIVE_FALLBACK")
    }

    private fun testEnvelope(targetPeerId: String): PacketEnvelope = PacketEnvelope(
        packetId = "packet-test",
        messageId = "message-test",
        conversationId = "conversation-test",
        packetType = org.expert.link.mesh.domain.model.network.PacketType.CHAT_MESSAGE,
        sourcePeerId = "local",
        targetPeerId = targetPeerId,
        ttl = 4,
        hopCount = 0,
        createdAt = now(),
        requiresAck = true,
        routeMode = RouteMode.LOCAL_DIRECT,
        payloadNonce = "nonce",
        encryptedPayload = "cipher",
        metadataSignature = "signature",
    )
}
