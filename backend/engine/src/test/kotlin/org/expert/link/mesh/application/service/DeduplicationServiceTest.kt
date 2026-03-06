package org.expert.link.mesh.application.service

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.infrastructure.cache.InMemoryDedupCacheAdapter
import org.junit.jupiter.api.Test

class DeduplicationServiceTest {
    @Test
    fun `should accept packet id only once`() = runTest {
        val service = DeduplicationService(InMemoryDedupCacheAdapter())

        assertThat(service.isNew("packet-1")).isTrue()
        assertThat(service.isNew("packet-1")).isFalse()
    }
}
