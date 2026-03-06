package org.expert.link.mesh.domain.model.network

import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PacketEnvelopeTest {
    @Test
    fun `should decrement ttl and increment hop count when relayed`() {
        val envelope = PacketEnvelope(
            packetId = "packet-1",
            packetType = PacketType.CHAT_MESSAGE,
            sourcePeerId = "source",
            targetPeerId = "target",
            ttl = 5,
            hopCount = 0,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            requiresAck = true,
            routeMode = RouteMode.RELAY_FLOOD,
            payloadNonce = "nonce",
            encryptedPayload = "payload",
            metadataSignature = "signature",
        )

        val relayed = envelope.decrementTtl("relay")

        assertThat(relayed.ttl).isEqualTo(4)
        assertThat(relayed.hopCount).isEqualTo(1)
        assertThat(relayed.previousHopPeerId).isEqualTo("relay")
    }
}
