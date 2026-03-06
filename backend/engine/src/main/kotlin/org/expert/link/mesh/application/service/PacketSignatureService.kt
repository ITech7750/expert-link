package org.expert.link.mesh.application.service

import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.port.external.CryptoPort

/** Сервис подписи и проверки метаданных пакета. */
class PacketSignatureService(
    private val cryptoPort: CryptoPort,
) {
    /**
     * Signs the envelope metadata with the supplied private key.
     */
    suspend fun signEnvelope(privateKey: String, envelope: PacketEnvelope): PacketEnvelope {
        val signature = cryptoPort.sign(privateKey, envelope.signaturePayload().toByteArray(Charsets.UTF_8))
        return envelope.copy(metadataSignature = signature)
    }

    /**
     * Verifies the envelope signature with the supplied public key.
     */
    suspend fun verifyEnvelope(publicKey: String, envelope: PacketEnvelope): Boolean {
        if (envelope.metadataSignature.isBlank()) {
            return false
        }
        return cryptoPort.verify(publicKey, envelope.signaturePayload().toByteArray(Charsets.UTF_8), envelope.metadataSignature)
    }
}
