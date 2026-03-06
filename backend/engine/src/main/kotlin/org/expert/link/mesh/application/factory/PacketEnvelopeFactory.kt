package org.expert.link.mesh.application.factory

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.model.security.EncryptedPayload

/** Фабрика пакетов до подписания метаданных. */
class PacketEnvelopeFactory {
    /** Создаёт новый packet envelope. */
    fun create(
        packetType: PacketType,
        sourcePeerId: String,
        targetPeerId: String,
        encryptedPayload: EncryptedPayload,
        routeMode: RouteMode,
        ttl: Int,
        requiresAck: Boolean,
        messageId: String? = null,
        conversationId: String? = null,
    ): PacketEnvelope = PacketEnvelope(
        packetId = newId("packet"),
        messageId = messageId,
        conversationId = conversationId,
        packetType = packetType,
        sourcePeerId = sourcePeerId,
        targetPeerId = targetPeerId,
        previousHopPeerId = null,
        ttl = ttl,
        hopCount = 0,
        createdAt = now(),
        requiresAck = requiresAck,
        routeMode = routeMode,
        payloadNonce = encryptedPayload.payloadNonce,
        encryptedPayload = encryptedPayload.encryptedPayload,
        metadataSignature = "",
    )
}
