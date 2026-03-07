package org.expert.link.mesh.application.service

import kotlinx.datetime.Clock
import org.expert.link.mesh.application.factory.KeyMaterialFactory
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.port.repository.LocalProfileRepositoryPort

/** Сервис загрузки или создания локального профиля. */
class LocalProfileService(
    private val localProfileRepositoryPort: LocalProfileRepositoryPort,
    private val keyMaterialFactory: KeyMaterialFactory,
) {
    /**
     * Returns the existing local profile or creates a new one if absent.
     */
    suspend fun getOrCreate(displayName: String, capabilities: Set<String>): LocalProfile {
        val current = localProfileRepositoryPort.get()
        if (current == null) {
            return localProfileRepositoryPort.save(
                keyMaterialFactory.createLocalProfile(displayName, capabilities),
            )
        }
        if (current.displayName == displayName && current.capabilities == capabilities) {
            return current
        }
        return localProfileRepositoryPort.save(
            current.copy(
                displayName = displayName,
                capabilities = capabilities,
                updatedAt = Clock.System.now(),
            ),
        )
    }

    /**
     * Returns the stored local profile or throws when the node is not initialized.
     */
    suspend fun require(): LocalProfile = requireNotNull(localProfileRepositoryPort.get()) {
        "Local profile is not initialized"
    }

    /**
     * Converts a local profile into its public identity representation.
     */
    fun asPeerIdentity(profile: LocalProfile): PeerIdentity = PeerIdentity(
        peerId = profile.peerId,
        displayName = profile.displayName,
        publicKey = profile.publicKey,
        capabilities = profile.capabilities,
    )
}
