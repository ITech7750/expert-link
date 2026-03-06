package org.expert.link.mesh.domain.model.relay

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.expert.link.mesh.domain.model.network.PeerEndpoint

/** Данные регистрации узла во внешнем реестре. */
@Serializable
data class PeerRegistration(
    val peerId: String,
    val publicKey: String,
    val displayName: String,
    val endpoints: List<PeerEndpoint>,
    val lastHeartbeatAt: Instant,
    val capabilities: Set<String> = emptySet(),
    val relayEligible: Boolean = false,
)

/** Кандидат маршрута через relay-узел. */
@Serializable
data class RelayRouteCandidate(
    val targetPeerId: String,
    val relayPeerId: String,
    val endpoint: PeerEndpoint? = null,
    val estimatedLatencyMs: Int? = null,
    val expiresAt: Instant,
)

/** Результат поиска узла во внешнем реестре. */
@Serializable
data class PeerLookupResult(
    val peerId: String,
    val directEndpoints: List<PeerEndpoint> = emptyList(),
    val relayCandidates: List<RelayRouteCandidate> = emptyList(),
    val foundAt: Instant,
    val relayOnly: Boolean = false,
)
