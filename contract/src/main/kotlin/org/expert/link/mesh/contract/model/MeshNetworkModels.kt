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
