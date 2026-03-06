package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.plusSeconds
import org.expert.link.mesh.application.support.now
import kotlinx.datetime.Instant

/** Политика повторов для пакетов с ACK. */
class RetryPolicyService {
    private val retryDelaysSeconds = listOf(1L, 2L, 5L, 10L)

    /**
     * Returns the next retry instant after a packet has already completed the supplied number of retries.
     *
     * @param attempt retry count already performed.
     * @param from base instant used to calculate the next deadline.
     * @return next retry timestamp or `null` when the packet should be marked as failed.
     */
    fun nextRetryAt(attempt: Int, from: Instant = now()): Instant? = retryDelaysSeconds.getOrNull(attempt)?.let(from::plusSeconds)

    /**
     * Returns `true` when another retry is still allowed.
     */
    fun canRetry(attempt: Int): Boolean = attempt < retryDelaysSeconds.size
}
