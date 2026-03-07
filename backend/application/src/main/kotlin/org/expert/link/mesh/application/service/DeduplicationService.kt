package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.application.support.plusSeconds
import org.expert.link.mesh.domain.port.repository.DedupCachePort

/** Сервис dedup-проверки пакетов. */
class DeduplicationService(
    private val dedupCachePort: DedupCachePort,
    private val retentionSeconds: Long = 300,
) {
    /**
     * Returns `true` only when the packet identifier was not observed recently.
     */
    suspend fun isNew(packetId: String): Boolean = dedupCachePort.markSeenIfNew(packetId, now().plusSeconds(retentionSeconds))

    /**
     * Evicts expired deduplication markers.
     */
    suspend fun evictExpired(): Int = dedupCachePort.evictExpired(now())
}
