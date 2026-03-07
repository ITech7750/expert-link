package org.expert.link.mesh.application.service

import org.expert.link.mesh.domain.model.security.BlockedPeer
import org.expert.link.mesh.domain.port.repository.BlockListRepositoryPort

/** Сервис работы с block list. */
class BlockListService(
    private val blockListRepositoryPort: BlockListRepositoryPort,
) {
    /**
     * Returns `true` if the peer is currently blocked.
     */
    suspend fun isBlocked(peerId: String): Boolean = blockListRepositoryPort.findByPeerId(peerId) != null

    /**
     * Stores a blocked peer entry.
     */
    suspend fun block(blockedPeer: BlockedPeer): BlockedPeer = blockListRepositoryPort.save(blockedPeer)

    /**
     * Removes a blocked peer entry.
     */
    suspend fun unblock(peerId: String) = blockListRepositoryPort.remove(peerId)
}
