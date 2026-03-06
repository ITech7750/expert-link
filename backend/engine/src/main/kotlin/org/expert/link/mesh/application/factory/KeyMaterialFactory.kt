package org.expert.link.mesh.application.factory

import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.security.CryptoMaterialRef
import org.expert.link.mesh.domain.model.security.CryptoStorageType
import org.expert.link.mesh.domain.model.security.KeyMaterial
import org.expert.link.mesh.domain.port.external.CryptoPort

/** Фабрика локального профиля из ключевого материала. */
class KeyMaterialFactory(
    private val cryptoPort: CryptoPort,
) {
    /** Создаёт новый локальный профиль. */
    suspend fun createLocalProfile(displayName: String, capabilities: Set<String>): LocalProfile {
        val keyAlias = "local-profile-${displayName.lowercase().replace(' ', '-') }"
        val keyMaterial = cryptoPort.generateKeyMaterial(keyAlias)
        return createFromKeyMaterial(displayName, capabilities, keyMaterial)
    }

    /** Собирает профиль из готовых ключей. */
    fun createFromKeyMaterial(displayName: String, capabilities: Set<String>, keyMaterial: KeyMaterial): LocalProfile {
        val peerId = cryptoPort.derivePeerId(keyMaterial.publicKey)
        val createdAt = now()
        return LocalProfile(
            peerId = peerId,
            displayName = displayName,
            publicKey = keyMaterial.publicKey,
            privateKey = keyMaterial.privateKey,
            keyMaterialRef = CryptoMaterialRef(
                alias = keyMaterial.keyId,
                storageType = CryptoStorageType.IN_MEMORY,
                createdAt = createdAt,
            ),
            capabilities = capabilities,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }
}
