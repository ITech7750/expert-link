package org.expert.link.mesh.domain.entity

import kotlinx.serialization.Serializable

/** Сущность хранения локального профиля. */
@Serializable
data class LocalProfileEntity(
    val peerId: String,
    val displayName: String,
    val publicKey: String,
    val privateKey: String,
    val keyAlias: String,
    val capabilities: List<String>,
    val createdAt: String,
    val updatedAt: String,
)

/** Сущность хранения доверенного узла. */
@Serializable
data class PairedPeerEntity(
    val peerId: String,
    val displayName: String,
    val publicKey: String,
    val trustState: String,
    val pairedAt: String,
    val lastSeenAt: String? = null,
    val endpointUrl: String? = null,
    val capabilities: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
)

/** Сущность хранения записи списка блокировок. */
@Serializable
data class BlockedPeerEntity(
    val peerId: String,
    val reason: String,
    val blockedAt: String,
    val expiresAt: String? = null,
)
