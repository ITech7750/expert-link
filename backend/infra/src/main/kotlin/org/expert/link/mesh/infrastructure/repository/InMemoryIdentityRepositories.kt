package org.expert.link.mesh.infrastructure.repository

import kotlinx.datetime.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.identity.PairedPeer
import org.expert.link.mesh.domain.model.identity.PairingSession
import org.expert.link.mesh.domain.model.identity.TrustState
import org.expert.link.mesh.domain.model.security.BlockedPeer
import org.expert.link.mesh.domain.port.repository.BlockListRepositoryPort
import org.expert.link.mesh.domain.port.repository.LocalProfileRepositoryPort
import org.expert.link.mesh.domain.port.repository.PairingSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.PeerRepositoryPort

/** Репозиторий локального профиля в памяти. */
class InMemoryLocalProfileRepositoryAdapter : LocalProfileRepositoryPort {
    private val mutex = Mutex()
    private var profile: LocalProfile? = null

    override suspend fun get(): LocalProfile? = mutex.withLock { profile }

    override suspend fun save(profile: LocalProfile): LocalProfile = mutex.withLock {
        this.profile = profile
        profile
    }
}

/** Репозиторий paired peer в памяти. */
class InMemoryPeerRepositoryAdapter : PeerRepositoryPort {
    private val mutex = Mutex()
    private val peers = linkedMapOf<String, PairedPeer>()

    override suspend fun save(peer: PairedPeer): PairedPeer = mutex.withLock {
        peers[peer.peerIdentity.peerId] = peer
        peer
    }

    override suspend fun findByPeerId(peerId: String): PairedPeer? = mutex.withLock { peers[peerId] }

    override suspend fun list(): List<PairedPeer> = mutex.withLock { peers.values.toList() }

    override suspend fun updateTrustState(peerId: String, trustState: TrustState): PairedPeer? = mutex.withLock {
        peers[peerId]?.let {
            val updated = it.copy(trustState = trustState)
            peers[peerId] = updated
            updated
        }
    }
}

/** Репозиторий pairing-сессий в памяти. */
class InMemoryPairingSessionRepositoryAdapter : PairingSessionRepositoryPort {
    private val mutex = Mutex()
    private val sessions = linkedMapOf<String, PairingSession>()

    override suspend fun save(session: PairingSession): PairingSession = mutex.withLock {
        sessions[session.sessionId] = session
        session
    }

    override suspend fun findBySessionId(sessionId: String): PairingSession? = mutex.withLock { sessions[sessionId] }

    override suspend fun findActiveByInviteSecret(inviteSecret: String): PairingSession? = mutex.withLock {
        sessions.values.lastOrNull { it.inviteSecret == inviteSecret && !it.used }
    }

    override suspend fun markUsed(sessionId: String): PairingSession? = mutex.withLock {
        sessions[sessionId]?.let {
            val updated = it.copy(used = true)
            sessions[sessionId] = updated
            updated
        }
    }

    override suspend fun listActive(): List<PairingSession> = mutex.withLock { sessions.values.filterNot { it.used } }

    override suspend fun removeExpired(now: Instant): Int = mutex.withLock {
        val before = sessions.size
        sessions.entries.removeIf { (_, value) -> value.expiresAt <= now }
        before - sessions.size
    }
}

/** Репозиторий block list в памяти. */
class InMemoryBlockListRepositoryAdapter : BlockListRepositoryPort {
    private val mutex = Mutex()
    private val blocked = linkedMapOf<String, BlockedPeer>()

    override suspend fun save(blockedPeer: BlockedPeer): BlockedPeer = mutex.withLock {
        blocked[blockedPeer.peerId] = blockedPeer
        blockedPeer
    }

    override suspend fun remove(peerId: String) {
        mutex.withLock { blocked.remove(peerId) }
    }

    override suspend fun findByPeerId(peerId: String): BlockedPeer? = mutex.withLock { blocked[peerId] }

    override suspend fun list(): List<BlockedPeer> = mutex.withLock { blocked.values.toList() }
}
