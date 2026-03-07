package org.expert.link.mesh.application.service

import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RetryPolicyServiceTest {
    @Test
    fun `should calculate exponential style retry schedule`() {
        val service = RetryPolicyService()
        val start = Instant.parse("2026-01-01T00:00:00Z")

        assertThat(service.nextRetryAt(0, start)).isEqualTo(Instant.parse("2026-01-01T00:00:01Z"))
        assertThat(service.nextRetryAt(1, start)).isEqualTo(Instant.parse("2026-01-01T00:00:02Z"))
        assertThat(service.nextRetryAt(2, start)).isEqualTo(Instant.parse("2026-01-01T00:00:05Z"))
        assertThat(service.nextRetryAt(3, start)).isEqualTo(Instant.parse("2026-01-01T00:00:10Z"))
        assertThat(service.nextRetryAt(4, start)).isNull()
    }
}
