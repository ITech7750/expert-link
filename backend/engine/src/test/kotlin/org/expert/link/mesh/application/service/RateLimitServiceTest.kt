package org.expert.link.mesh.application.service

import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.security.RateLimitRule
import org.expert.link.mesh.domain.model.security.RateLimitScope
import org.junit.jupiter.api.Test

class RateLimitServiceTest {
    @Test
    fun `should block requests after configured burst size`() {
        val service = RateLimitService(RateLimitRule("default", RateLimitScope.PEER, maxRequests = 2, windowSeconds = 60, burstSize = 2))

        assertThat(service.allow("peer-a", PacketType.CHAT_MESSAGE)).isTrue()
        assertThat(service.allow("peer-a", PacketType.CHAT_MESSAGE)).isTrue()
        assertThat(service.allow("peer-a", PacketType.CHAT_MESSAGE)).isFalse()
    }
}
