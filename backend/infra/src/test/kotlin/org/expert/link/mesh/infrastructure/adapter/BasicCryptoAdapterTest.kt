package org.expert.link.mesh.infrastructure.adapter

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BasicCryptoAdapterTest {
    @Test
    fun `should generate stable peer id from public key`() = runTest {
        val crypto = BasicCryptoAdapter()
        val keyMaterial = crypto.generateKeyMaterial("node-a")

        val first = crypto.derivePeerId(keyMaterial.publicKey)
        val second = crypto.derivePeerId(keyMaterial.publicKey)

        assertThat(first).isEqualTo(second)
        assertThat(first).hasSize(64)
    }
}
