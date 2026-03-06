package org.expert.link.mesh.infrastructure.client

import io.github.oshai.kotlinlogging.KotlinLogging
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.model.network.TransportDeliveryResult
import org.expert.link.mesh.domain.model.relay.PeerLookupResult
import org.expert.link.mesh.domain.model.relay.PeerRegistration
import org.expert.link.mesh.domain.model.relay.RelayRouteCandidate
import org.expert.link.mesh.domain.port.external.PacketTransportPort
import org.expert.link.mesh.domain.port.external.RelayGatewayPort
import org.expert.link.mesh.domain.port.external.RendezvousRegistryPort
import java.util.concurrent.ConcurrentHashMap

/**
 * Заглушка клиента rendezvous/relay-сервиса.
 *
 * В текущей JVM-сборке хранит регистрации в памяти и переиспользует локальный транспорт
 * для имитации внешнего relay.
 */
class RendezvousRelayClient(
    private val packetTransportPort: PacketTransportPort,
    private val forceRelayLookup: Boolean = false,
) : RendezvousRegistryPort, RelayGatewayPort {
    private val logger = KotlinLogging.logger {}

    override suspend fun registerNode(registration: PeerRegistration) {
        registrations[registration.peerId] = registration
    }

    override suspend fun heartbeat(peerId: String) {
        registrations.computeIfPresent(peerId) { _, registration ->
            registration.copy(lastHeartbeatAt = kotlinx.datetime.Clock.System.now())
        }
    }

    override suspend fun findPeer(peerId: String): PeerLookupResult? {
        val registration = registrations[peerId] ?: return null
        return if (forceRelayLookup) {
            PeerLookupResult(
                peerId = peerId,
                relayCandidates = listOf(
                    RelayRouteCandidate(
                        targetPeerId = peerId,
                        relayPeerId = registration.peerId,
                        endpoint = registration.endpoints.firstOrNull(),
                        expiresAt = registration.lastHeartbeatAt,
                    ),
                ),
                foundAt = kotlinx.datetime.Clock.System.now(),
                relayOnly = true,
            )
        } else {
            PeerLookupResult(
                peerId = peerId,
                directEndpoints = registration.endpoints,
                relayCandidates = if (registration.relayEligible) {
                    registration.endpoints.firstOrNull()?.let {
                        listOf(RelayRouteCandidate(peerId, registration.peerId, it, expiresAt = registration.lastHeartbeatAt))
                    }.orEmpty()
                } else {
                    emptyList()
                },
                foundAt = kotlinx.datetime.Clock.System.now(),
                relayOnly = false,
            )
        }
    }

    override suspend fun unregisterNode(peerId: String) {
        registrations.remove(peerId)
    }

    override suspend fun relayPacket(targetPeerId: String, envelope: PacketEnvelope): TransportDeliveryResult {
        val registration = registrations[targetPeerId]
            ?: return TransportDeliveryResult(success = false, errorMessage = "Target is not registered in rendezvous registry")
        val endpoint = registration.endpoints.firstOrNull()
            ?: return TransportDeliveryResult(success = false, errorMessage = "Target has no reachable endpoint")
        logger.info { "Relaying packet ${envelope.packetId} to $targetPeerId via stub rendezvous client" }
        return packetTransportPort.sendPacket(endpoint, envelope.copy(routeMode = RouteMode.RENDEZVOUS_RELAY))
    }

    companion object StubRegistry {
        private val registrations = ConcurrentHashMap<String, PeerRegistration>()
    }
}
