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
        evictStaleRegistrations()
        val registration = registrations[peerId] ?: return null
        val relayCandidates = registrations.values
            .asSequence()
            .filter { it.peerId != peerId && it.relayEligible }
            .mapNotNull { relay ->
                relay.endpoints.firstOrNull()?.let { endpoint ->
                    RelayRouteCandidate(
                        targetPeerId = peerId,
                        relayPeerId = relay.peerId,
                        endpoint = endpoint,
                        expiresAt = relay.lastHeartbeatAt,
                    )
                }
            }
            .toList()
        return if (forceRelayLookup) {
            PeerLookupResult(
                peerId = peerId,
                relayCandidates = relayCandidates.ifEmpty {
                    registration.endpoints.firstOrNull()?.let {
                        listOf(
                            RelayRouteCandidate(
                                targetPeerId = peerId,
                                relayPeerId = registration.peerId,
                                endpoint = it,
                                expiresAt = registration.lastHeartbeatAt,
                            ),
                        )
                    }.orEmpty()
                },
                foundAt = kotlinx.datetime.Clock.System.now(),
                relayOnly = true,
            )
        } else {
            PeerLookupResult(
                peerId = peerId,
                directEndpoints = registration.endpoints,
                relayCandidates = relayCandidates,
                foundAt = kotlinx.datetime.Clock.System.now(),
                relayOnly = registration.endpoints.isEmpty(),
            )
        }
    }

    override suspend fun unregisterNode(peerId: String) {
        registrations.remove(peerId)
    }

    override suspend fun relayPacket(targetPeerId: String, envelope: PacketEnvelope): TransportDeliveryResult {
        evictStaleRegistrations()
        val registration = registrations[targetPeerId]
            ?: return TransportDeliveryResult(success = false, errorMessage = "Target is not registered in rendezvous registry")
        val endpoint = registration.endpoints.firstOrNull()
            ?: return TransportDeliveryResult(success = false, errorMessage = "Target has no reachable endpoint")
        logger.info { "Relaying packet ${envelope.packetId} to $targetPeerId via stub rendezvous client" }
        return packetTransportPort.sendPacket(endpoint, envelope.copy(routeMode = RouteMode.RENDEZVOUS_RELAY))
    }

    companion object StubRegistry {
        private val registrations = ConcurrentHashMap<String, PeerRegistration>()
        private const val HEARTBEAT_TTL_MILLIS: Long = 45_000

        private fun evictStaleRegistrations() {
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            registrations.entries.removeIf { (_, registration) ->
                now - registration.lastHeartbeatAt.toEpochMilliseconds() > HEARTBEAT_TTL_MILLIS
            }
        }
    }
}
