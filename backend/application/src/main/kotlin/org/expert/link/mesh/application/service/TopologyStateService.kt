package org.expert.link.mesh.application.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.call.CallState
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.model.network.ConnectivityMode
import org.expert.link.mesh.domain.model.network.ConnectivityStrategy
import org.expert.link.mesh.domain.model.network.HostCandidate
import org.expert.link.mesh.domain.model.network.HostRole
import org.expert.link.mesh.domain.model.network.NetworkRoleState
import org.expert.link.mesh.domain.model.network.NetworkTopologyState
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.RelayMode
import org.expert.link.mesh.domain.model.network.RouteEntry
import org.expert.link.mesh.domain.model.network.RouteHealth
import org.expert.link.mesh.domain.model.network.RouteHealthState
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.model.network.TopologyEvent
import org.expert.link.mesh.domain.model.network.TopologyEventType
import org.expert.link.mesh.domain.port.repository.CallSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.EndpointCachePort
import org.expert.link.mesh.domain.port.repository.FileTransferRepositoryPort
import org.expert.link.mesh.domain.port.repository.OutgoingQueuePort
import org.expert.link.mesh.domain.port.repository.PendingAckRepositoryPort
import org.expert.link.mesh.domain.port.repository.RouteRepositoryPort

/**
 * Сервис наблюдения и перестройки topology/connectivity состояния узла.
 *
 * Сервис держит единый runtime-снимок:
 * - роли локального узла и выбранного хоста;
 * - здоровья маршрутов и активных стратегий доставки;
 * - состояния continuity для активных chat/file/call сценариев.
 */
class TopologyStateService(
    private val routeRepositoryPort: RouteRepositoryPort,
    private val endpointCachePort: EndpointCachePort,
    private val pendingAckRepositoryPort: PendingAckRepositoryPort,
    private val outgoingQueuePort: OutgoingQueuePort,
    private val fileTransferRepositoryPort: FileTransferRepositoryPort,
    private val callSessionRepositoryPort: CallSessionRepositoryPort,
    private val connectivityStrategyService: ConnectivityStrategyService,
) {
    private val mutex = Mutex()
    private val hostCandidates = linkedMapOf<String, HostCandidate>()
    private val routeHealthByTarget = linkedMapOf<String, RouteHealth>()
    private val routeFailures = linkedMapOf<String, Int>()
    private val recentEvents = ArrayDeque<TopologyEvent>()

    private var localPeerId: String = ""
    private var localEndpoint: PeerEndpoint? = null
    private var relayMode: RelayMode = RelayMode.DISABLED
    private var failoverInProgress: Boolean = false
    private var roleState: NetworkRoleState = NetworkRoleState(
        localPeerId = "",
        localRole = HostRole.UNKNOWN,
        currentHostPeerId = null,
        hostReachable = false,
        failoverInProgress = false,
        updatedAt = now(),
    )
    private var lastSnapshot: NetworkTopologyState = NetworkTopologyState(
        localPeerId = "",
        connectivityMode = ConnectivityMode.DEGRADED,
        relayMode = RelayMode.DISABLED,
        networkRoleState = roleState,
        refreshedAt = now(),
    )

    /** Инициализирует topology state при старте runtime. */
    suspend fun initialize(localPeerId: String, endpoint: PeerEndpoint, relayMode: RelayMode) {
        mutex.withLock {
            this.localPeerId = localPeerId
            this.localEndpoint = endpoint
            this.relayMode = relayMode
            hostCandidates[localPeerId] = HostCandidate(
                peerId = localPeerId,
                endpoint = endpoint,
                score = 700,
                reachable = true,
                roleHint = HostRole.CANDIDATE,
                lastSeenAt = now(),
            )
            electHostLocked("startup")
            addEventLocked(
                eventType = TopologyEventType.STARTED,
                message = "Topology initialized",
                peerId = localPeerId,
            )
        }
        refresh()
    }

    /** Обновляет данные кандидата-хоста после discovery или route learning. */
    suspend fun onPeerDiscovered(peerId: String, endpoint: PeerEndpoint?, qualityScore: Int = 100) {
        if (peerId.isBlank()) return
        mutex.withLock {
            if (peerId == localPeerId) return@withLock
            val previous = hostCandidates[peerId]
            val candidate = HostCandidate(
                peerId = peerId,
                endpoint = endpoint ?: previous?.endpoint,
                score = maxOf(previous?.score ?: 0, qualityScore),
                reachable = true,
                roleHint = previous?.roleHint ?: HostRole.CANDIDATE,
                lastSeenAt = now(),
            )
            hostCandidates[peerId] = candidate
            electHostLocked("peer-discovered")
        }
    }

    /** Помечает узел как потерянный и запускает failover-перевыбор хоста. */
    suspend fun onPeerLost(peerId: String, reason: String) {
        if (peerId.isBlank()) return
        mutex.withLock {
            val candidate = hostCandidates[peerId]
            if (candidate != null) {
                hostCandidates[peerId] = candidate.copy(reachable = false, lastSeenAt = now())
            }
            routeRepositoryPort.remove(peerId)
            val failures = (routeFailures[peerId] ?: 0) + 1
            routeFailures[peerId] = failures
            routeHealthByTarget[peerId] = RouteHealth(
                targetPeerId = peerId,
                routeMode = RouteMode.RELAY_FLOOD,
                state = RouteHealthState.FAILED,
                failureCount = failures,
                lastUpdatedAt = now(),
                detail = reason,
            )
            if (roleState.currentHostPeerId == peerId) {
                failoverInProgress = true
                addEventLocked(
                    eventType = TopologyEventType.HOST_LOST,
                    message = "Host became unreachable",
                    peerId = peerId,
                    detail = mapOf("reason" to reason),
                )
            }
            electHostLocked("peer-lost")
        }
    }

    /** Обновляет health snapshot после обучения маршрута. */
    suspend fun onRouteLearned(routeEntry: RouteEntry) {
        mutex.withLock {
            val previousFailures = routeFailures[routeEntry.targetPeerId] ?: 0
            routeHealthByTarget[routeEntry.targetPeerId] = RouteHealth(
                targetPeerId = routeEntry.targetPeerId,
                nextHopPeerId = routeEntry.nextHopPeerId,
                routeMode = routeEntry.routeMode,
                state = if (previousFailures == 0) RouteHealthState.HEALTHY else RouteHealthState.DEGRADED,
                failureCount = previousFailures,
                lastUpdatedAt = now(),
                expiresAt = routeEntry.expiresAt,
                detail = "route-learned",
            )
            routeFailures.remove(routeEntry.targetPeerId)
        }
    }

    /** Помечает маршрут как невалидный. */
    suspend fun onRouteInvalidated(targetPeerId: String, reason: String) {
        if (targetPeerId.isBlank()) return
        mutex.withLock {
            val failures = (routeFailures[targetPeerId] ?: 0) + 1
            routeFailures[targetPeerId] = failures
            routeHealthByTarget[targetPeerId] = RouteHealth(
                targetPeerId = targetPeerId,
                routeMode = RouteMode.RELAY_FLOOD,
                state = RouteHealthState.FAILED,
                failureCount = failures,
                lastUpdatedAt = now(),
                detail = reason,
            )
            addEventLocked(
                eventType = TopologyEventType.ROUTE_HEALTH_CHANGED,
                message = "Route marked failed",
                targetPeerId = targetPeerId,
                detail = mapOf("reason" to reason, "failures" to failures.toString()),
            )
        }
    }

    /** Обновляет health маршрута по результату доставки. */
    suspend fun onDeliveryResult(targetPeerId: String, routeMode: RouteMode, success: Boolean, viaRelayGateway: Boolean) {
        if (targetPeerId.isBlank()) return
        mutex.withLock {
            val previous = routeHealthByTarget[targetPeerId]
            val failures = if (success) 0 else (routeFailures[targetPeerId] ?: 0) + 1
            routeFailures[targetPeerId] = failures
            val healthState = when {
                success -> RouteHealthState.HEALTHY
                failures >= 3 -> RouteHealthState.FAILED
                else -> RouteHealthState.DEGRADED
            }
            routeHealthByTarget[targetPeerId] = RouteHealth(
                targetPeerId = targetPeerId,
                nextHopPeerId = previous?.nextHopPeerId,
                routeMode = if (viaRelayGateway) RouteMode.RENDEZVOUS_RELAY else routeMode,
                state = healthState,
                failureCount = failures,
                lastUpdatedAt = now(),
                expiresAt = previous?.expiresAt,
                detail = if (success) "delivered" else "delivery-failed",
            )
            if (viaRelayGateway) {
                relayMode = if (relayMode == RelayMode.FORCED) RelayMode.FORCED else RelayMode.ACTIVE_FALLBACK
            }
        }
    }

    /** Возвращает текущую роль узла в топологии. */
    suspend fun observeHostRole(): NetworkRoleState {
        refresh()
        return mutex.withLock { roleState }
    }

    /** Возвращает последний topology snapshot. */
    suspend fun observeTopologyState(): NetworkTopologyState {
        refresh()
        return mutex.withLock { lastSnapshot }
    }

    /** Принудительно пересобирает topology snapshot. */
    suspend fun forceRefresh(): NetworkTopologyState {
        refresh(force = true)
        return mutex.withLock { lastSnapshot }
    }

    /** Возвращает снимок health маршрутов. */
    suspend fun inspectRouteHealth(): List<RouteHealth> {
        refresh()
        return mutex.withLock { routeHealthByTarget.values.sortedBy { it.targetPeerId } }
    }

    /** Возвращает выбранную стратегию связности к peer. */
    suspend fun observeConnectivityStrategy(targetPeerId: String): ConnectivityStrategy {
        return connectivityStrategyService.observeStrategy(targetPeerId)
    }

    /** Возвращает текущий relay mode. */
    suspend fun relayModeState(): RelayMode {
        refresh()
        return mutex.withLock { relayMode }
    }

    /** Возвращает последний topology snapshot без пересчёта. */
    suspend fun currentSnapshot(): NetworkTopologyState = mutex.withLock { lastSnapshot }

    /** Пересчитывает topology/connectivity snapshot. */
    suspend fun refresh(force: Boolean = false) {
        val currentTime = now()
        val routes = routeRepositoryPort.list()
        val neighbors = endpointCachePort.listNeighbors()
        val pendingAck = pendingAckRepositoryPort.list()
        val queue = outgoingQueuePort.list()
        val fileTransfers = fileTransferRepositoryPort.list()
        val callSessions = callSessionRepositoryPort.list()
        val strategies = connectivityStrategyService.activeStrategies()
        val currentRelayMode = connectivityStrategyService.currentRelayMode()

        mutex.withLock {
            if (force) {
                addEventLocked(
                    eventType = TopologyEventType.TOPOLOGY_REBUILT,
                    message = "Topology refresh requested",
                    peerId = localPeerId,
                )
            }
            relayMode = when {
                relayMode == RelayMode.FORCED -> RelayMode.FORCED
                currentRelayMode == RelayMode.FORCED -> RelayMode.FORCED
                strategies.any { it.useRelayGateway || it.routeMode == RouteMode.RENDEZVOUS_RELAY } -> RelayMode.ACTIVE_FALLBACK
                else -> currentRelayMode
            }

            neighbors.forEach { neighbor ->
                val existing = hostCandidates[neighbor.peerId]
                hostCandidates[neighbor.peerId] = HostCandidate(
                    peerId = neighbor.peerId,
                    endpoint = neighbor.endpoint,
                    score = maxOf(existing?.score ?: 0, neighbor.qualityScore),
                    reachable = true,
                    roleHint = existing?.roleHint ?: HostRole.CANDIDATE,
                    lastSeenAt = currentTime,
                )
            }

            routes.forEach { route ->
                val failureCount = routeFailures[route.targetPeerId] ?: 0
                routeHealthByTarget[route.targetPeerId] = RouteHealth(
                    targetPeerId = route.targetPeerId,
                    nextHopPeerId = route.nextHopPeerId,
                    routeMode = route.routeMode,
                    state = when {
                        route.expiresAt <= currentTime -> RouteHealthState.STALE
                        failureCount >= 3 -> RouteHealthState.FAILED
                        failureCount > 0 -> RouteHealthState.DEGRADED
                        else -> RouteHealthState.HEALTHY
                    },
                    failureCount = failureCount,
                    lastUpdatedAt = currentTime,
                    expiresAt = route.expiresAt,
                    detail = "route-scan",
                )
            }

            val existingTargets = routes.map { it.targetPeerId }.toSet()
            routeHealthByTarget.entries.removeIf { (targetPeerId, routeHealth) ->
                routeHealth.state == RouteHealthState.STALE && targetPeerId !in existingTargets
            }

            electHostLocked("refresh")

            val activeTransfers = fileTransfers.count {
                it.status != FileTransferStatus.COMPLETED &&
                    it.status != FileTransferStatus.CANCELLED &&
                    it.status != FileTransferStatus.FAILED
            }
            val activeCalls = callSessions.count {
                it.status != CallState.ENDED &&
                    it.status != CallState.REJECTED &&
                    it.status != CallState.FAILED &&
                    it.status != CallState.MISSED &&
                    it.status != CallState.LEFT
            }
            val continuityDegraded = pendingAck.isNotEmpty() ||
                queue.isNotEmpty() ||
                activeTransfers > 0 ||
                activeCalls > 0 ||
                routeHealthByTarget.values.any { it.state == RouteHealthState.FAILED } ||
                failoverInProgress

            val previousConnectivityMode = lastSnapshot.connectivityMode
            val previousRelayMode = lastSnapshot.relayMode
            val previousContinuityDegraded = lastSnapshot.continuityDegraded

            val connectivityMode = resolveConnectivityModeLocked(strategies)
            val hostReachable = roleState.currentHostPeerId == localPeerId ||
                hostCandidates[roleState.currentHostPeerId]?.reachable == true
            val resolvedRoleState = roleState.copy(
                hostReachable = hostReachable,
                failoverInProgress = failoverInProgress,
                updatedAt = currentTime,
            )
            roleState = resolvedRoleState
            if (failoverInProgress && hostReachable) {
                failoverInProgress = false
                addEventLocked(
                    eventType = TopologyEventType.CONTINUITY_RECOVERED,
                    message = "Host failover completed",
                    peerId = roleState.currentHostPeerId,
                )
            }

            if (previousConnectivityMode != connectivityMode) {
                addEventLocked(
                    eventType = TopologyEventType.CONNECTIVITY_MODE_CHANGED,
                    message = "Connectivity mode changed",
                    peerId = resolvedRoleState.currentHostPeerId,
                    detail = mapOf(
                        "from" to previousConnectivityMode.name,
                        "to" to connectivityMode.name,
                    ),
                )
            }
            if (previousRelayMode != relayMode) {
                addEventLocked(
                    eventType = TopologyEventType.RELAY_MODE_CHANGED,
                    message = "Relay mode changed",
                    detail = mapOf(
                        "from" to previousRelayMode.name,
                        "to" to relayMode.name,
                    ),
                )
            }
            if (!previousContinuityDegraded && continuityDegraded) {
                addEventLocked(
                    eventType = TopologyEventType.CONTINUITY_DEGRADED,
                    message = "Continuity degraded",
                )
            }
            if (previousContinuityDegraded && !continuityDegraded) {
                addEventLocked(
                    eventType = TopologyEventType.CONTINUITY_RECOVERED,
                    message = "Continuity recovered",
                )
            }

            lastSnapshot = NetworkTopologyState(
                localPeerId = localPeerId,
                connectivityMode = connectivityMode,
                relayMode = relayMode,
                networkRoleState = resolvedRoleState,
                hostCandidates = hostCandidates.values.sortedByDescending { it.score },
                routeHealth = routeHealthByTarget.values.sortedBy { it.targetPeerId },
                activeStrategies = strategies,
                pendingAckCount = pendingAck.size,
                queuedPacketCount = queue.size,
                activeFileTransfers = activeTransfers,
                activeCallSessions = activeCalls,
                continuityDegraded = continuityDegraded,
                recentEvents = recentEvents.toList(),
                refreshedAt = currentTime,
            )
        }
    }

    private fun resolveConnectivityModeLocked(strategies: List<ConnectivityStrategy>): ConnectivityMode {
        if (relayMode == RelayMode.ACTIVE_FALLBACK || relayMode == RelayMode.FORCED) {
            return ConnectivityMode.RELAY_PROXY
        }
        if (routeHealthByTarget.isEmpty()) {
            return ConnectivityMode.LOCAL_MESH
        }
        if (routeHealthByTarget.values.all { it.state == RouteHealthState.FAILED || it.state == RouteHealthState.STALE }) {
            return ConnectivityMode.DEGRADED
        }
        if (strategies.any { it.connectivityMode == ConnectivityMode.HOST_ROUTED }) {
            return ConnectivityMode.HOST_ROUTED
        }
        if (roleState.currentHostPeerId != null && roleState.currentHostPeerId != localPeerId) {
            return ConnectivityMode.HOST_ROUTED
        }
        return ConnectivityMode.LOCAL_MESH
    }

    private fun electHostLocked(reason: String) {
        if (localPeerId.isBlank()) return
        val previousHost = roleState.currentHostPeerId
        val selectedHost = hostCandidates.values
            .filter { it.reachable }
            .maxWithOrNull(compareBy<HostCandidate> { it.score }.thenByDescending { it.peerId == localPeerId }.thenBy { it.peerId })
            ?.peerId
            ?: localPeerId

        val localRole = when {
            selectedHost == localPeerId -> HostRole.HOST
            hostCandidates[localPeerId]?.reachable == true -> HostRole.MEMBER
            else -> HostRole.UNKNOWN
        }
        roleState = NetworkRoleState(
            localPeerId = localPeerId,
            localRole = localRole,
            currentHostPeerId = selectedHost,
            hostReachable = selectedHost == localPeerId || hostCandidates[selectedHost]?.reachable == true,
            failoverInProgress = failoverInProgress,
            updatedAt = now(),
        )

        if (previousHost == null) {
            addEventLocked(
                eventType = TopologyEventType.HOST_ELECTED,
                message = "Host selected",
                peerId = selectedHost,
                detail = mapOf("reason" to reason),
            )
        } else if (previousHost != null && previousHost != selectedHost) {
            failoverInProgress = true
            addEventLocked(
                eventType = TopologyEventType.HOST_SWITCHED,
                message = "Host switched",
                peerId = selectedHost,
                detail = mapOf(
                    "previousHost" to previousHost,
                    "reason" to reason,
                ),
            )
        }
    }

    private fun addEventLocked(
        eventType: TopologyEventType,
        message: String,
        peerId: String? = null,
        targetPeerId: String? = null,
        detail: Map<String, String> = emptyMap(),
    ) {
        recentEvents.addLast(
            TopologyEvent(
                eventId = newId("topology"),
                eventType = eventType,
                message = message,
                peerId = peerId,
                targetPeerId = targetPeerId,
                detail = detail,
                occurredAt = now(),
            ),
        )
        while (recentEvents.size > 48) {
            recentEvents.removeFirst()
        }
    }
}
