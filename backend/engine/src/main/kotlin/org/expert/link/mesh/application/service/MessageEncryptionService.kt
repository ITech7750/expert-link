package org.expert.link.mesh.application.service

import org.expert.link.mesh.domain.model.network.PacketPayload
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.security.EncryptedPayload
import org.expert.link.mesh.domain.port.external.CryptoPort

/** Сервис сериализации и шифрования payload. */
class MessageEncryptionService(
    private val cryptoPort: CryptoPort,
    private val packetSerializationService: PacketSerializationService,
) {
    /**
     * Serializes and encrypts a payload for the supplied public key.
     */
    suspend fun encryptPayload(targetPublicKey: String, payload: PacketPayload): EncryptedPayload {
        val serialized = packetSerializationService.serializePayload(payload)
        return cryptoPort.encrypt(targetPublicKey, serialized)
    }

    /**
     * Decrypts and deserializes a packet payload using the supplied private key.
     */
    suspend fun decryptPayload(privateKey: String, packetType: PacketType, encryptedPayload: EncryptedPayload): PacketPayload {
        val serialized = cryptoPort.decrypt(privateKey, encryptedPayload)
        return packetSerializationService.deserializePayload(packetType, serialized)
    }
}
