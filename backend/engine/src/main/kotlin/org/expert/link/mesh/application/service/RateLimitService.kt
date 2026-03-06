package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.security.RateLimitRule
import java.util.concurrent.ConcurrentHashMap

/** Ограничитель частоты входящих пакетов по скользящему окну. */
class RateLimitService(
    private val defaultRule: RateLimitRule,
) {
    private val buckets = ConcurrentHashMap<String, MutableList<Long>>()

    /**
     * Checks whether the event is allowed under the configured policy.
     */
    fun allow(peerId: String?, packetType: PacketType? = null, rule: RateLimitRule = defaultRule): Boolean {
        val key = buildString {
            append(rule.scope.name)
            append(':')
            append(peerId ?: "global")
            append(':')
            append(packetType?.name ?: "any")
        }
        val nowMs = now().toEpochMilliseconds()
        val windowStart = nowMs - (rule.windowSeconds * 1_000L)
        val bucket = buckets.computeIfAbsent(key) { mutableListOf() }
        synchronized(bucket) {
            bucket.removeIf { it < windowStart }
            if (bucket.size >= rule.burstSize || bucket.size >= rule.maxRequests) {
                return false
            }
            bucket += nowMs
            return true
        }
    }
}
