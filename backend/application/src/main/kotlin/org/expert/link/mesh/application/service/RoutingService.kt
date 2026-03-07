package org.expert.link.mesh.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.application.support.plusSeconds
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.network.EndpointSource
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.PeerEndpointCandidate
import org.expert.link.mesh.domain.model.network.RouteEntry
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.port.repository.EndpointCachePort
import org.expert.link.mesh.domain.port.repository.RouteRepositoryPort

/** Конкретный hop для исходящей доставки. */
data class RouteHop(
    val peerId: String,
    val endpoint: PeerEndpoint,
    val routeMode: RouteMode,
)

/** План маршрутизации исходящего пакета. */
data class RoutingPlan(
    val routeMode: RouteMode,
    val hops: List<RouteHop> = emptyList(),
    val useRelayGateway: Boolean = false,
)

/**
 * Сервис маршрутизации mesh-сети.
 *
 * Выбирает прямой маршрут, flood или relay gateway и обновляет кэш маршрутов.
 */
class RoutingService(
    private val routeRepositoryPort: RouteRepositoryPort,
    private val endpointCachePort: EndpointCachePort,
    private val connectivityStrategyService: ConnectivityStrategyService,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
    private val forceRelayLookup: Boolean = false,
) {
    private val logger = KotlinLogging.logger {}

    /** Строит план доставки для целевого peer. */
    suspend fun resolvePlan(targetPeerId: String, excludePeerId: String? = null): RoutingPlan {
        purgeExpired()

        if (!forceRelayLookup) {
            routeRepositoryPort.findByTargetPeerId(targetPeerId)?.let { route ->
                val endpoint = route.endpoint
                if (endpoint != null && route.nextHopPeerId != excludePeerId) {
                    return RoutingPlan(
                        routeMode = route.routeMode,
                        hops = listOf(RouteHop(route.nextHopPeerId, endpoint, route.routeMode)),
                    )
                }
            }

            endpointCachePort.findByPeerId(targetPeerId)
                .filter { it.peerId != excludePeerId }
                .maxByOrNull { it.qualityScore }
                ?.let { candidate ->
                    learnDirectEndpoint(targetPeerId, candidate.endpoint, candidate.capabilities)
                    return RoutingPlan(
                        routeMode = RouteMode.LOCAL_DIRECT,
                        hops = listOf(RouteHop(targetPeerId, candidate.endpoint, RouteMode.LOCAL_DIRECT)),
                    )
                }
        }

        val connectivityDecision = connectivityStrategyService.chooseBest(targetPeerId)
        if (connectivityDecision.endpoint != null) {
            val hop = RouteHop(targetPeerId, connectivityDecision.endpoint, connectivityDecision.routeMode)
            return RoutingPlan(routeMode = connectivityDecision.routeMode, hops = listOf(hop))
        }
        if (connectivityDecision.useRelayGateway) {
            return RoutingPlan(routeMode = RouteMode.RENDEZVOUS_RELAY, useRelayGateway = true)
        }

        val neighbors = endpointCachePort.listNeighbors()
            .filter { it.peerId != excludePeerId }
            .sortedByDescending { it.qualityScore }
            .map { RouteHop(it.peerId, it.endpoint, RouteMode.RELAY_FLOOD) }
        return RoutingPlan(routeMode = RouteMode.RELAY_FLOOD, hops = neighbors)
    }

    /**
     * Learns a direct endpoint and stores the corresponding route entry.
     */
    suspend fun learnDirectEndpoint(peerId: String, endpoint: PeerEndpoint, capabilities: Set<String> = emptySet()) {
        val currentTime = now()
        endpointCachePort.put(
            PeerEndpointCandidate(
                peerId = peerId,
                endpoint = endpoint,
                source = EndpointSource.ROUTE_LEARNING,
                discoveredAt = currentTime,
                capabilities = capabilities,
            ),
        )
        routeRepositoryPort.save(
            RouteEntry(
                targetPeerId = peerId,
                nextHopPeerId = peerId,
                endpoint = endpoint,
                routeMode = RouteMode.LOCAL_DIRECT,
                hopCount = 1,
                expiresAt = currentTime.plusSeconds(60),
                learnedAt = currentTime,
                direct = true,
            ),
        )
        nodeMetricsService.increment("route.learned")
    }

    /**
     * Learns a reverse route towards the original sender from observed packet traffic.
     */
    suspend fun learnObservedRoute(
        sourcePeerId: String,
        previousHopPeerId: String?,
        previousHopEndpoint: PeerEndpoint?,
        observedHopCount: Int,
    ) {
        val nextHopPeerId = previousHopPeerId ?: sourcePeerId
        val routeMode = if (nextHopPeerId == sourcePeerId) RouteMode.LOCAL_DIRECT else RouteMode.RELAY_FLOOD
        val currentTime = now()
        routeRepositoryPort.save(
            RouteEntry(
                targetPeerId = sourcePeerId,
                nextHopPeerId = nextHopPeerId,
                endpoint = previousHopEndpoint,
                routeMode = routeMode,
                hopCount = observedHopCount + 1,
                expiresAt = currentTime.plusSeconds(60),
                learnedAt = currentTime,
                direct = nextHopPeerId == sourcePeerId,
            ),
        )
        nodeMetricsService.increment("route.observed")
        logger.debug { "Learned route to $sourcePeerId via $nextHopPeerId" }
    }

    /**
     * Removes a route after repeated failures.
     */
    suspend fun invalidateRoute(targetPeerId: String) {
        routeRepositoryPort.remove(targetPeerId)
        nodeMetricsService.increment("route.invalidated")
        eventLogService.log(
            category = EventCategory.ROUTING,
            level = EventLevel.WARN,
            message = "Route invalidated",
            peerId = targetPeerId,
        )
    }

    /**
     * Evicts stale routes and endpoint candidates.
     */
    suspend fun purgeExpired() {
        val currentTime = now()
        val removedRoutes = routeRepositoryPort.evictExpired(currentTime)
        val removedEndpoints = endpointCachePort.evictExpired(currentTime)
        if (removedRoutes > 0 || removedEndpoints > 0) {
            eventLogService.log(
                category = EventCategory.ROUTING,
                level = EventLevel.INFO,
                message = "Expired stale routing state",
                attributes = mapOf(
                    "routes" to removedRoutes.toString(),
                    "endpoints" to removedEndpoints.toString(),
                ),
            )
        }
    }
}
