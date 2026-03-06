package org.expert.link.mesh.application.service

import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.model.relay.RelayRouteCandidate
import org.expert.link.mesh.domain.port.external.RelayGatewayPort
import org.expert.link.mesh.domain.port.external.RendezvousRegistryPort
import org.expert.link.mesh.domain.port.repository.EndpointCachePort
import org.expert.link.mesh.domain.port.repository.RouteRepositoryPort

/** Результат выбора стратегии доставки. */
data class ConnectivityDecision(
    val routeMode: RouteMode,
    val endpoint: PeerEndpoint? = null,
    val relayRouteCandidate: RelayRouteCandidate? = null,
    val useRelayGateway: Boolean = false,
)

/**
 * Выбирает лучшую стратегию доставки для узла.
 *
 * Порядок выбора: local direct, rendezvous direct, rendezvous relay, затем flood.
 */
class ConnectivityStrategyService(
    private val routeRepositoryPort: RouteRepositoryPort,
    private val endpointCachePort: EndpointCachePort,
    private val rendezvousRegistryPort: RendezvousRegistryPort?,
    private val relayGatewayPort: RelayGatewayPort?,
    private val forceRelayLookup: Boolean = false,
) {
    /** Возвращает лучшую доступную стратегию доставки. */
    suspend fun chooseBest(targetPeerId: String): ConnectivityDecision {
        if (forceRelayLookup) {
            resolveViaRendezvous(targetPeerId)?.let { return it }
        }

        routeRepositoryPort.findByTargetPeerId(targetPeerId)?.let { route ->
            if (route.endpoint != null) {
                return ConnectivityDecision(routeMode = route.routeMode, endpoint = route.endpoint)
            }
        }

        endpointCachePort.findByPeerId(targetPeerId)
            .maxByOrNull { it.qualityScore }
            ?.let { candidate ->
                return ConnectivityDecision(routeMode = RouteMode.LOCAL_DIRECT, endpoint = candidate.endpoint)
            }

        resolveViaRendezvous(targetPeerId)?.let { return it }

        return ConnectivityDecision(routeMode = RouteMode.RELAY_FLOOD)
    }

    private suspend fun resolveViaRendezvous(targetPeerId: String): ConnectivityDecision? {
        val lookupResult = rendezvousRegistryPort?.findPeer(targetPeerId) ?: return null
        lookupResult.directEndpoints.firstOrNull()?.let { endpoint ->
            return ConnectivityDecision(
                routeMode = RouteMode.RENDEZVOUS_DIRECT_CANDIDATE,
                endpoint = endpoint,
            )
        }
        if (relayGatewayPort != null) {
            lookupResult.relayCandidates.firstOrNull()?.let { candidate ->
                return ConnectivityDecision(
                    routeMode = RouteMode.RENDEZVOUS_RELAY,
                    relayRouteCandidate = candidate,
                    useRelayGateway = true,
                )
            }
        }
        return null
    }
}
