package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Источник найденного endpoint. */
@Serializable
enum class MeshEndpointSource {
    DISCOVERY_MULTICAST,
    DISCOVERY_BROADCAST,
    MANUAL_HINT,
    ROUTE_LEARNING,
    RENDEZVOUS,
}

/** Режим доставки пакета. */
@Serializable
enum class MeshRouteMode {
    LOCAL_DIRECT,
    RELAY_FLOOD,
    OVERLAY_DIRECT,
    RENDEZVOUS_DIRECT_CANDIDATE,
    RENDEZVOUS_RELAY,
}

/** Узел, найденный рядом через discovery или ручной hint. */
@Serializable
data class MeshNearbyPeer(
    val peerId: String,
    val endpoint: MeshPeerEndpoint,
    val source: MeshEndpointSource,
    val discoveredAt: Instant,
    val qualityScore: Int,
    val capabilities: Set<String> = emptySet(),
)

/** Известный маршрут до узла. */
@Serializable
data class MeshRouteInfo(
    val targetPeerId: String,
    val nextHopPeerId: String,
    val endpoint: MeshPeerEndpoint? = null,
    val routeMode: MeshRouteMode,
    val hopCount: Int,
    val expiresAt: Instant,
    val learnedAt: Instant,
    val direct: Boolean,
)

/** Один hop в плане доставки. */
@Serializable
data class MeshRouteHop(
    val peerId: String,
    val endpoint: MeshPeerEndpoint,
    val routeMode: MeshRouteMode,
)

/** Текущий план доставки до целевого узла. */
@Serializable
data class MeshRoutingPlan(
    val targetPeerId: String,
    val routeMode: MeshRouteMode,
    val hops: List<MeshRouteHop> = emptyList(),
    val useRelayGateway: Boolean = false,
)

/** Доступность relay/rendezvous режима для узла. */
@Serializable
data class MeshRelayStatus(
    val enabled: Boolean,
    val forceRelayLookup: Boolean,
    val relayEligible: Boolean,
)

/** Роль узла в локальной сетевой топологии. */
@Serializable
enum class MeshHostRole {
    HOST,
    MEMBER,
    CANDIDATE,
    UNKNOWN,
}

/** Режим связности узла. */
@Serializable
enum class MeshConnectivityMode {
    LOCAL_MESH,
    HOST_ROUTED,
    RELAY_PROXY,
    DEGRADED,
}

/** Состояние relay/proxy режима. */
@Serializable
enum class MeshRelayMode {
    DISABLED,
    STANDBY,
    ACTIVE_FALLBACK,
    FORCED,
}

/** Состояние здоровья маршрута. */
@Serializable
enum class MeshRouteHealthState {
    HEALTHY,
    DEGRADED,
    STALE,
    FAILED,
}

/** Кандидат на роль хоста. */
@Serializable
data class MeshHostCandidate(
    val peerId: String,
    val endpoint: MeshPeerEndpoint? = null,
    val score: Int,
    val reachable: Boolean,
    val roleHint: MeshHostRole = MeshHostRole.CANDIDATE,
    val lastSeenAt: Instant,
)

/** Текущая стратегия связности для peer. */
@Serializable
data class MeshConnectivityStrategy(
    val targetPeerId: String,
    val routeMode: MeshRouteMode,
    val connectivityMode: MeshConnectivityMode,
    val relayMode: MeshRelayMode,
    val useRelayGateway: Boolean,
    val reason: String,
    val decidedAt: Instant,
)

/** Снимок здоровья маршрута до peer. */
@Serializable
data class MeshRouteHealth(
    val targetPeerId: String,
    val nextHopPeerId: String? = null,
    val routeMode: MeshRouteMode,
    val state: MeshRouteHealthState,
    val failureCount: Int = 0,
    val lastUpdatedAt: Instant,
    val expiresAt: Instant? = null,
    val detail: String? = null,
)

/** Тип события топологии. */
@Serializable
enum class MeshTopologyEventType {
    STARTED,
    HOST_ELECTED,
    HOST_LOST,
    HOST_SWITCHED,
    ROUTE_HEALTH_CHANGED,
    CONNECTIVITY_MODE_CHANGED,
    RELAY_MODE_CHANGED,
    TOPOLOGY_REBUILT,
    CONTINUITY_DEGRADED,
    CONTINUITY_RECOVERED,
}

/** Событие изменения топологии. */
@Serializable
data class MeshTopologyEvent(
    val eventId: String,
    val eventType: MeshTopologyEventType,
    val message: String,
    val peerId: String? = null,
    val targetPeerId: String? = null,
    val detail: Map<String, String> = emptyMap(),
    val occurredAt: Instant,
)

/** Состояние роли узла и выбранного хоста. */
@Serializable
data class MeshNetworkRoleState(
    val localPeerId: String,
    val localRole: MeshHostRole,
    val currentHostPeerId: String? = null,
    val hostReachable: Boolean,
    val failoverInProgress: Boolean,
    val updatedAt: Instant,
)

/** Полный снимок topology/connectivity состояния узла. */
@Serializable
data class MeshTopologyState(
    val localPeerId: String,
    val connectivityMode: MeshConnectivityMode,
    val relayMode: MeshRelayMode,
    val networkRoleState: MeshNetworkRoleState,
    val hostCandidates: List<MeshHostCandidate> = emptyList(),
    val routeHealth: List<MeshRouteHealth> = emptyList(),
    val activeStrategies: List<MeshConnectivityStrategy> = emptyList(),
    val pendingAckCount: Int = 0,
    val queuedPacketCount: Int = 0,
    val activeFileTransfers: Int = 0,
    val activeCallSessions: Int = 0,
    val continuityDegraded: Boolean = false,
    val recentEvents: List<MeshTopologyEvent> = emptyList(),
    val refreshedAt: Instant,
)
