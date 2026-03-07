package org.expert.link.mesh.application.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.security.RateLimitRule

/** Ограничитель частоты входящих пакетов по скользящему окну. */
class RateLimitService(
    private val defaultRule: RateLimitRule,
) {
    private val mutex = Mutex()
    private val buckets = linkedMapOf<String, MutableList<Long>>()

    /**
     * Checks whether the event is allowed under the configured policy.
     */
    suspend fun allow(peerId: String?, packetType: PacketType? = null, rule: RateLimitRule = defaultRule): Boolean {
        val key = buildString {
            append(rule.scope.name)
            append(':')
            append(peerId ?: "global")
            append(':')
            append(packetType?.name ?: "any")
        }
        val nowMs = now().toEpochMilliseconds()
        val windowStart = nowMs - (rule.windowSeconds * 1_000L)
        return mutex.withLock {
            val bucket = buckets.getOrPut(key) { mutableListOf() }
            bucket.removeIf { it < windowStart }
            if (bucket.size >= rule.burstSize || bucket.size >= rule.maxRequests) {
                return@withLock false
            }
            bucket += nowMs
            true
        }
    }
}
