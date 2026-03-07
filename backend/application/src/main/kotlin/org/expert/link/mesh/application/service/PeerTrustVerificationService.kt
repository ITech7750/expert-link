package org.expert.link.mesh.application.service

import org.expert.link.mesh.domain.model.identity.PairedPeer
import org.expert.link.mesh.domain.model.identity.TrustState
import org.expert.link.mesh.domain.port.repository.PeerRepositoryPort

/** Сервис проверки доверия к peer. */
class PeerTrustVerificationService(
    private val peerRepositoryPort: PeerRepositoryPort,
) {
    /**
     * Returns the paired peer only when it is trusted.
     */
    suspend fun requireTrusted(peerId: String): PairedPeer? {
        val peer = peerRepositoryPort.findByPeerId(peerId) ?: return null
        return peer.takeIf { it.trustState == TrustState.TRUSTED }
    }

    /**
     * Returns `true` when the peer is trusted.
     */
    suspend fun isTrusted(peerId: String): Boolean = requireTrusted(peerId) != null
}
