package org.expert.link.mesh.application.factory

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.model.security.EncryptedPayload
import org.junit.jupiter.api.Test

class PacketEnvelopeFactoryTest {
    @Test
    fun `should create envelope with generated packet id and default hop metadata`() = runTest {
        val factory = PacketEnvelopeFactory()

        val envelope = factory.create(
            packetType = PacketType.CHAT_MESSAGE,
            sourcePeerId = "peer-a",
            targetPeerId = "peer-b",
            encryptedPayload = EncryptedPayload(
                payloadNonce = "nonce-1",
                encryptedPayload = "ciphertext",
            ),
            routeMode = RouteMode.LOCAL_DIRECT,
            ttl = 5,
            requiresAck = true,
            messageId = "message-1",
            conversationId = "conversation-1",
        )

        assertThat(envelope.packetId).startsWith("packet-")
        assertThat(envelope.messageId).isEqualTo("message-1")
        assertThat(envelope.conversationId).isEqualTo("conversation-1")
        assertThat(envelope.packetType).isEqualTo(PacketType.CHAT_MESSAGE)
        assertThat(envelope.sourcePeerId).isEqualTo("peer-a")
        assertThat(envelope.targetPeerId).isEqualTo("peer-b")
        assertThat(envelope.previousHopPeerId).isNull()
        assertThat(envelope.ttl).isEqualTo(5)
        assertThat(envelope.hopCount).isZero()
        assertThat(envelope.requiresAck).isTrue()
        assertThat(envelope.routeMode).isEqualTo(RouteMode.LOCAL_DIRECT)
        assertThat(envelope.payloadNonce).isEqualTo("nonce-1")
        assertThat(envelope.encryptedPayload).isEqualTo("ciphertext")
        assertThat(envelope.metadataSignature).isEmpty()
    }
}
