package org.expert.link.mesh.application.service

import java.util.concurrent.ConcurrentHashMap
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.network.ConnectivityMode
import org.expert.link.mesh.domain.model.network.ConnectivityStrategy
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.RelayMode
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
    val connectivityMode: ConnectivityMode,
    val relayMode: RelayMode,
    val reason: String,
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
    private val strategySnapshots = ConcurrentHashMap<String, ConnectivityStrategy>()

    /** Возвращает лучшую доступную стратегию доставки. */
    suspend fun chooseBest(targetPeerId: String): ConnectivityDecision {
        if (forceRelayLookup) {
            resolveViaRendezvous(targetPeerId)?.let { return remember(targetPeerId, it) }
        }

        routeRepositoryPort.findByTargetPeerId(targetPeerId)?.let { route ->
            if (route.endpoint != null) {
                return remember(
                    targetPeerId = targetPeerId,
                    decision = ConnectivityDecision(
                        routeMode = route.routeMode,
                        endpoint = route.endpoint,
                        connectivityMode = route.routeMode.toConnectivityMode(),
                        relayMode = currentRelayMode().takeIf { route.routeMode == RouteMode.RENDEZVOUS_RELAY } ?: RelayMode.DISABLED,
                        reason = "cached-route",
                    ),
                )
            }
        }

        endpointCachePort.findByPeerId(targetPeerId)
            .maxByOrNull { it.qualityScore }
            ?.let { candidate ->
                return remember(
                    targetPeerId = targetPeerId,
                    decision = ConnectivityDecision(
                        routeMode = RouteMode.LOCAL_DIRECT,
                        endpoint = candidate.endpoint,
                        connectivityMode = ConnectivityMode.LOCAL_MESH,
                        relayMode = RelayMode.DISABLED,
                        reason = "endpoint-cache",
                    ),
                )
            }

        resolveViaRendezvous(targetPeerId)?.let { return remember(targetPeerId, it) }

        return remember(
            targetPeerId = targetPeerId,
            decision = ConnectivityDecision(
                routeMode = RouteMode.RELAY_FLOOD,
                connectivityMode = ConnectivityMode.DEGRADED,
                relayMode = currentRelayMode(),
                reason = "flood-fallback",
            ),
        )
    }

    /** Возвращает последнюю выбранную стратегию для peer. */
    suspend fun observeStrategy(targetPeerId: String): ConnectivityStrategy {
        strategySnapshots[targetPeerId]?.let { return it }
        chooseBest(targetPeerId)
        return strategySnapshots[targetPeerId]
            ?: ConnectivityStrategy(
                targetPeerId = targetPeerId,
                routeMode = RouteMode.RELAY_FLOOD,
                connectivityMode = ConnectivityMode.DEGRADED,
                relayMode = currentRelayMode(),
                useRelayGateway = false,
                reason = "strategy-unavailable",
                decidedAt = now(),
            )
    }

    /** Возвращает снимок всех известных стратегий связности. */
    fun activeStrategies(): List<ConnectivityStrategy> = strategySnapshots.values.sortedBy { it.targetPeerId }

    /** Возвращает текущее состояние relay-mode. */
    fun currentRelayMode(): RelayMode {
        return when {
            forceRelayLookup -> RelayMode.FORCED
            relayGatewayPort != null || rendezvousRegistryPort != null -> RelayMode.STANDBY
            else -> RelayMode.DISABLED
        }
    }

    private suspend fun resolveViaRendezvous(targetPeerId: String): ConnectivityDecision? {
        val lookupResult = rendezvousRegistryPort?.findPeer(targetPeerId) ?: return null
        lookupResult.directEndpoints.firstOrNull()?.let { endpoint ->
            return ConnectivityDecision(
                routeMode = RouteMode.RENDEZVOUS_DIRECT_CANDIDATE,
                endpoint = endpoint,
                connectivityMode = ConnectivityMode.RELAY_PROXY,
                relayMode = currentRelayMode(),
                reason = "rendezvous-direct-candidate",
            )
        }
        if (relayGatewayPort != null) {
            lookupResult.relayCandidates.firstOrNull()?.let { candidate ->
                return ConnectivityDecision(
                    routeMode = RouteMode.RENDEZVOUS_RELAY,
                    relayRouteCandidate = candidate,
                    useRelayGateway = true,
                    connectivityMode = ConnectivityMode.RELAY_PROXY,
                    relayMode = RelayMode.ACTIVE_FALLBACK,
                    reason = "rendezvous-relay",
                )
            }
        }
        return null
    }

    private fun remember(targetPeerId: String, decision: ConnectivityDecision): ConnectivityDecision {
        strategySnapshots[targetPeerId] = ConnectivityStrategy(
            targetPeerId = targetPeerId,
            routeMode = decision.routeMode,
            connectivityMode = decision.connectivityMode,
            relayMode = decision.relayMode,
            useRelayGateway = decision.useRelayGateway,
            reason = decision.reason,
            decidedAt = now(),
        )
        return decision
    }

    private fun RouteMode.toConnectivityMode(): ConnectivityMode {
        return when (this) {
            RouteMode.LOCAL_DIRECT -> ConnectivityMode.LOCAL_MESH
            RouteMode.RELAY_FLOOD,
            RouteMode.OVERLAY_DIRECT,
            -> ConnectivityMode.HOST_ROUTED
            RouteMode.RENDEZVOUS_DIRECT_CANDIDATE,
            RouteMode.RENDEZVOUS_RELAY,
            -> ConnectivityMode.RELAY_PROXY
        }
    }
}
