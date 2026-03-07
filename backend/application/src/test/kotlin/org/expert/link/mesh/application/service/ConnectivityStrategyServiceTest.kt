package org.expert.link.mesh.application.service

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.RouteEntry
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.model.network.TransportDeliveryResult
import org.expert.link.mesh.domain.model.relay.PeerLookupResult
import org.expert.link.mesh.domain.model.relay.PeerRegistration
import org.expert.link.mesh.domain.model.relay.RelayRouteCandidate
import org.expert.link.mesh.domain.port.external.RelayGatewayPort
import org.expert.link.mesh.domain.port.external.RendezvousRegistryPort
import org.expert.link.mesh.infrastructure.repository.InMemoryEndpointCacheAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryRouteRepositoryAdapter
import org.junit.jupiter.api.Test
import kotlinx.datetime.Instant

class ConnectivityStrategyServiceTest {
    @Test
    fun `should prefer direct route from route repository`() = runTest {
        val routeRepository = InMemoryRouteRepositoryAdapter()
        val endpoint = PeerEndpoint(host = "127.0.0.1", port = 8080, announcedAt = Instant.parse("2026-01-01T00:00:00Z"))
        routeRepository.save(
            RouteEntry(
                targetPeerId = "peer-b",
                nextHopPeerId = "peer-b",
                endpoint = endpoint,
                routeMode = RouteMode.LOCAL_DIRECT,
                hopCount = 1,
                expiresAt = Instant.parse("2030-01-01T00:00:00Z"),
                learnedAt = Instant.parse("2026-01-01T00:00:00Z"),
                direct = true,
            ),
        )
        val service = ConnectivityStrategyService(routeRepository, InMemoryEndpointCacheAdapter(), null, null)

        val decision = service.chooseBest("peer-b")

        assertThat(decision.routeMode).isEqualTo(RouteMode.LOCAL_DIRECT)
        assertThat(decision.endpoint).isEqualTo(endpoint)
    }

    @Test
    fun `should prefer rendezvous relay over cached direct route when force relay lookup is enabled`() = runTest {
        val routeRepository = InMemoryRouteRepositoryAdapter()
        val directEndpoint = PeerEndpoint(host = "127.0.0.1", port = 8080, announcedAt = Instant.parse("2026-01-01T00:00:00Z"))
        routeRepository.save(
            RouteEntry(
                targetPeerId = "peer-b",
                nextHopPeerId = "peer-b",
                endpoint = directEndpoint,
                routeMode = RouteMode.LOCAL_DIRECT,
                hopCount = 1,
                expiresAt = Instant.parse("2030-01-01T00:00:00Z"),
                learnedAt = Instant.parse("2026-01-01T00:00:00Z"),
                direct = true,
            ),
        )
        val rendezvousRegistry = object : RendezvousRegistryPort {
            override suspend fun registerNode(registration: PeerRegistration) = Unit

            override suspend fun heartbeat(peerId: String) = Unit

            override suspend fun findPeer(peerId: String): PeerLookupResult {
                return PeerLookupResult(
                    peerId = peerId,
                    relayCandidates = listOf(
                        RelayRouteCandidate(
                            targetPeerId = peerId,
                            relayPeerId = "relay-node",
                            endpoint = PeerEndpoint(host = "relay.local", port = 9090, announcedAt = Instant.parse("2026-01-01T00:00:00Z")),
                            expiresAt = Instant.parse("2030-01-01T00:00:00Z"),
                        ),
                    ),
                    foundAt = Instant.parse("2026-01-01T00:00:00Z"),
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
        val service = ConnectivityStrategyService(
            routeRepository,
            InMemoryEndpointCacheAdapter(),
            rendezvousRegistry,
            relayGateway,
            forceRelayLookup = true,
        )

        val decision = service.chooseBest("peer-b")

        assertThat(decision.routeMode).isEqualTo(RouteMode.RENDEZVOUS_RELAY)
        assertThat(decision.useRelayGateway).isTrue()
    }
}
