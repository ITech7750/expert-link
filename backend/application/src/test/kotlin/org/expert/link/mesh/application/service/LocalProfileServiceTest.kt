package org.expert.link.mesh.application.service

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.application.factory.KeyMaterialFactory
import org.expert.link.mesh.infrastructure.adapter.BasicCryptoAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryLocalProfileRepositoryAdapter
import org.junit.jupiter.api.Test

class LocalProfileServiceTest {
    @Test
    fun `should create local profile once and refresh mutable fields from config`() = runTest {
        val repository = InMemoryLocalProfileRepositoryAdapter()
        val service = LocalProfileService(repository, KeyMaterialFactory(BasicCryptoAdapter()))

        val created = service.getOrCreate(displayName = "node-a", capabilities = setOf("chat", "file"))
        val reused = service.getOrCreate(displayName = "node-b", capabilities = setOf("call"))

        assertThat(reused.peerId).isEqualTo(created.peerId)
        assertThat(reused.displayName).isEqualTo("node-b")
        assertThat(reused.capabilities).containsExactly("call")
        assertThat(repository.get()?.peerId).isEqualTo(created.peerId)
    }

    @Test
    fun `should expose local profile as public peer identity`() = runTest {
        val repository = InMemoryLocalProfileRepositoryAdapter()
        val service = LocalProfileService(repository, KeyMaterialFactory(BasicCryptoAdapter()))
        val profile = service.getOrCreate(displayName = "node-a", capabilities = setOf("chat"))

        val peerIdentity = service.asPeerIdentity(profile)

        assertThat(peerIdentity.peerId).isEqualTo(profile.peerId)
        assertThat(peerIdentity.displayName).isEqualTo(profile.displayName)
        assertThat(peerIdentity.publicKey).isEqualTo(profile.publicKey)
        assertThat(peerIdentity.capabilities).containsExactly("chat")
    }
}
