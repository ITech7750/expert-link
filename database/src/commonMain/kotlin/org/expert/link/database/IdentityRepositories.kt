package org.expert.link.database

import kotlinx.datetime.Clock
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.identity.PairedPeer
import org.expert.link.mesh.domain.model.identity.PairingSession
import org.expert.link.mesh.domain.model.identity.TrustState
import org.expert.link.mesh.domain.model.security.BlockedPeer
import org.expert.link.mesh.domain.port.repository.BlockListRepositoryPort
import org.expert.link.mesh.domain.port.repository.LocalProfileRepositoryPort
import org.expert.link.mesh.domain.port.repository.PairingSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.PeerRepositoryPort

internal class RoomLocalProfileRepository(
    private val identityDao: IdentityDao,
) : LocalProfileRepositoryPort {
    override suspend fun get(): LocalProfile? =
        identityDao.getLocalProfile()?.let { decodePayload(it.payloadJson) }

    override suspend fun save(profile: LocalProfile): LocalProfile {
        identityDao.upsertLocalProfile(
            LocalProfileRecord(
                peerId = profile.peerId,
                displayName = profile.displayName,
                updatedAt = profile.updatedAt.toString(),
                payloadJson = encodePayload(profile),
            ),
        )
        return profile
    }
}

internal class RoomPeerRepository(
    private val identityDao: IdentityDao,
) : PeerRepositoryPort {
    override suspend fun save(peer: PairedPeer): PairedPeer {
        identityDao.upsertPeer(
            PairedPeerRecord(
                peerId = peer.peerIdentity.peerId,
                displayName = peer.peerIdentity.displayName,
                trustState = peer.trustState.name,
                pairedAt = peer.pairedAt.toString(),
                lastSeenAt = peer.lastSeenAt?.toString(),
                payloadJson = encodePayload(peer),
            ),
        )
        return peer
    }

    override suspend fun findByPeerId(peerId: String): PairedPeer? =
        identityDao.findPeer(peerId)?.let { decodePayload(it.payloadJson) }

    override suspend fun list(): List<PairedPeer> =
        identityDao.listPeers().map { decodePayload(it.payloadJson) }

    override suspend fun updateTrustState(peerId: String, trustState: TrustState): PairedPeer? {
        val current = findByPeerId(peerId) ?: return null
        val updated = current.copy(trustState = trustState)
        save(updated)
        return updated
    }
}

internal class RoomPairingSessionRepository(
    private val identityDao: IdentityDao,
) : PairingSessionRepositoryPort {
    override suspend fun save(session: PairingSession): PairingSession {
        identityDao.upsertPairingSession(
            PairingSessionRecord(
                sessionId = session.sessionId,
                inviteSecret = session.inviteSecret,
                remotePeerId = session.remotePeerId,
                used = session.used,
                createdAt = session.createdAt.toString(),
                expiresAt = session.expiresAt.toString(),
                payloadJson = encodePayload(session),
            ),
        )
        return session
    }

    override suspend fun findBySessionId(sessionId: String): PairingSession? =
        identityDao.findPairingSession(sessionId)?.let { decodePayload(it.payloadJson) }

    override suspend fun findActiveByInviteSecret(inviteSecret: String): PairingSession? =
        identityDao.findActivePairingSession(inviteSecret, Clock.System.now().toString())
            ?.let { decodePayload(it.payloadJson) }

    override suspend fun markUsed(sessionId: String): PairingSession? {
        val current = findBySessionId(sessionId) ?: return null
        val updated = current.copy(used = true)
        save(updated)
        return updated
    }

    override suspend fun listActive(): List<PairingSession> =
        identityDao.listActivePairingSessions(Clock.System.now().toString())
            .map { decodePayload(it.payloadJson) }

    override suspend fun removeExpired(now: kotlinx.datetime.Instant): Int =
        identityDao.removeExpiredPairingSessions(now.toString())
}

internal class RoomBlockListRepository(
    private val identityDao: IdentityDao,
) : BlockListRepositoryPort {
    override suspend fun save(blockedPeer: BlockedPeer): BlockedPeer {
        identityDao.upsertBlockedPeer(
            BlockedPeerRecord(
                peerId = blockedPeer.peerId,
                blockedAt = blockedPeer.blockedAt.toString(),
                expiresAt = blockedPeer.expiresAt?.toString(),
                payloadJson = encodePayload(blockedPeer),
            ),
        )
        return blockedPeer
    }

    override suspend fun remove(peerId: String) {
        identityDao.removeBlockedPeer(peerId)
    }

    override suspend fun findByPeerId(peerId: String): BlockedPeer? =
        identityDao.findBlockedPeer(peerId)?.let { decodePayload(it.payloadJson) }

    override suspend fun list(): List<BlockedPeer> =
        identityDao.listBlockedPeers().map { decodePayload(it.payloadJson) }
}
