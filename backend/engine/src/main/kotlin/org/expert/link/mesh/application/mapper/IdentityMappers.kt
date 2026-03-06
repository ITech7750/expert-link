package org.expert.link.mesh.application.mapper

import kotlinx.datetime.Instant
import org.expert.link.mesh.domain.entity.BlockedPeerEntity
import org.expert.link.mesh.domain.entity.LocalProfileEntity
import org.expert.link.mesh.domain.entity.PairedPeerEntity
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.identity.PairedPeer
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.identity.TrustState
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.security.BlockedPeer
import org.expert.link.mesh.domain.model.security.CryptoMaterialRef
import org.expert.link.mesh.domain.model.security.CryptoStorageType
import java.net.URI

/** Маппер локального профиля. */
object LocalProfileEntityMapper {
    /**
     * Converts a domain profile into a storage entity.
     */
    fun toEntity(profile: LocalProfile): LocalProfileEntity = LocalProfileEntity(
        peerId = profile.peerId,
        displayName = profile.displayName,
        publicKey = profile.publicKey,
        privateKey = profile.privateKey,
        keyAlias = profile.keyMaterialRef.alias,
        capabilities = profile.capabilities.toList(),
        createdAt = profile.createdAt.toString(),
        updatedAt = profile.updatedAt.toString(),
    )

    /**
     * Converts a storage entity back into a domain profile.
     */
    fun fromEntity(entity: LocalProfileEntity): LocalProfile = LocalProfile(
        peerId = entity.peerId,
        displayName = entity.displayName,
        publicKey = entity.publicKey,
        privateKey = entity.privateKey,
        keyMaterialRef = CryptoMaterialRef(
            alias = entity.keyAlias,
            storageType = CryptoStorageType.IN_MEMORY,
            createdAt = Instant.parse(entity.createdAt),
        ),
        capabilities = entity.capabilities.toSet(),
        createdAt = Instant.parse(entity.createdAt),
        updatedAt = Instant.parse(entity.updatedAt),
    )
}

/** Маппер paired peer. */
object PeerEntityMapper {
    /**
     * Converts a domain peer into a storage entity.
     */
    fun toEntity(peer: PairedPeer): PairedPeerEntity = PairedPeerEntity(
        peerId = peer.peerIdentity.peerId,
        displayName = peer.peerIdentity.displayName,
        publicKey = peer.peerIdentity.publicKey,
        trustState = peer.trustState.name,
        pairedAt = peer.pairedAt.toString(),
        lastSeenAt = peer.lastSeenAt?.toString(),
        endpointUrl = peer.endpointHint?.asUrl(),
        capabilities = peer.peerIdentity.capabilities.toList(),
        metadata = peer.metadata,
    )

    /**
     * Converts a storage entity back into a domain peer.
     */
    fun fromEntity(entity: PairedPeerEntity): PairedPeer = PairedPeer(
        peerIdentity = PeerIdentity(
            peerId = entity.peerId,
            displayName = entity.displayName,
            publicKey = entity.publicKey,
            capabilities = entity.capabilities.toSet(),
        ),
        trustState = TrustState.valueOf(entity.trustState),
        pairedAt = Instant.parse(entity.pairedAt),
        lastSeenAt = entity.lastSeenAt?.let(Instant::parse),
        endpointHint = entity.endpointUrl?.let(::parseEndpoint),
        metadata = entity.metadata,
    )

    private fun parseEndpoint(url: String): PeerEndpoint {
        val uri = URI(url)
        return PeerEndpoint(
            scheme = uri.scheme ?: "http",
            host = uri.host,
            port = if (uri.port > 0) uri.port else 80,
            path = uri.path.ifBlank { "/api/v1/packets" },
            announcedAt = Instant.parse("1970-01-01T00:00:00Z"),
            expiresAt = null,
        )
    }
}

/** Маппер block list. */
object BlockedPeerEntityMapper {
    /**
     * Converts a blocked peer into a storage entity.
     */
    fun toEntity(blockedPeer: BlockedPeer): BlockedPeerEntity = BlockedPeerEntity(
        peerId = blockedPeer.peerId,
        reason = blockedPeer.reason,
        blockedAt = blockedPeer.blockedAt.toString(),
        expiresAt = blockedPeer.expiresAt?.toString(),
    )

    /**
     * Converts a storage entity back into a blocked peer.
     */
    fun fromEntity(entity: BlockedPeerEntity): BlockedPeer = BlockedPeer(
        peerId = entity.peerId,
        reason = entity.reason,
        blockedAt = Instant.parse(entity.blockedAt),
        expiresAt = entity.expiresAt?.let(Instant::parse),
    )
}
