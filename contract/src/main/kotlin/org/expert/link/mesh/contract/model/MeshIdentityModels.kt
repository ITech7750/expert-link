package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Публичный сетевой endpoint. */
@Serializable
data class MeshPeerEndpoint(
    val scheme: String = "http",
    val host: String,
    val port: Int,
    val path: String = "/api/v1/packets",
    val announcedPeerId: String? = null,
    val announcedAt: Instant,
    val expiresAt: Instant? = null,
)

/** Публичная идентичность узла. */
@Serializable
data class MeshPeerIdentity(
    val peerId: String,
    val displayName: String,
    val publicKey: String,
    val capabilities: Set<String> = emptySet(),
)

/** Публичный профиль локального узла. */
@Serializable
data class MeshLocalProfile(
    val peerId: String,
    val displayName: String,
    val publicKey: String,
    val capabilities: Set<String> = emptySet(),
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Состояние доверия узлу. */
@Serializable
enum class MeshTrustState {
    INVITED,
    PENDING,
    TRUSTED,
    BLOCKED,
    REVOKED,
}

/** Публичная запись доверенного узла. */
@Serializable
data class MeshPairedPeer(
    val identity: MeshPeerIdentity,
    val trustState: MeshTrustState,
    val pairedAt: Instant,
    val lastSeenAt: Instant? = null,
    val endpointHint: MeshPeerEndpoint? = null,
    val metadata: Map<String, String> = emptyMap(),
)

/** Роль сессии сопряжения. */
@Serializable
enum class MeshPairingRole {
    INVITER,
    JOINER,
}

/** Публичное состояние сессии сопряжения. */
@Serializable
data class MeshPairingSession(
    val sessionId: String,
    val localPeerId: String,
    val remotePeerId: String? = null,
    val role: MeshPairingRole,
    val createdAt: Instant,
    val expiresAt: Instant,
    val trustState: MeshTrustState,
    val used: Boolean = false,
    val endpointHint: MeshPeerEndpoint? = null,
    val remotePublicKey: String? = null,
    val remoteDisplayName: String? = null,
)

/** Публичная запись списка блокировок. */
@Serializable
data class MeshBlockedPeer(
    val peerId: String,
    val reason: String,
    val blockedAt: Instant,
    val expiresAt: Instant? = null,
)
