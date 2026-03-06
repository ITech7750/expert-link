package org.expert.link.mesh.domain.model.identity

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PairingInviteTest {
    @Test
    fun `should encode and decode invite without losing fields`() {
        val invite = PairingInvite(
            protocolVersion = 1,
            peerId = "peer-a",
            displayName = "Alice",
            publicKey = "public-key",
            inviteSecret = "secret",
            expiresAt = Instant.parse("2030-01-01T00:00:00Z"),
            capabilities = setOf("chat", "file"),
        )

        val decoded = PairingInvite.fromEncodedString(invite.toEncodedString())

        assertThat(decoded).isEqualTo(invite)
    }

    @Test
    fun `should report invite expiration correctly`() {
        val invite = PairingInvite(
            protocolVersion = 1,
            peerId = "peer-a",
            displayName = "Alice",
            publicKey = "public-key",
            inviteSecret = "secret",
            expiresAt = Instant.parse("2000-01-01T00:00:00Z"),
        )

        assertThat(invite.isExpired(Clock.System.now())).isTrue()
    }
}
