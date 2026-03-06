package org.expert.link.mesh.application.service

import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.infrastructure.crypto.BasicCryptoAdapter
import org.junit.jupiter.api.Test
import java.io.File

class FileHashServiceTest {
    @Test
    fun `should compute and verify file hash`() {
        val file = File.createTempFile("hash-service", ".txt")
        file.writeText("hello secure mesh")
        val service = FileHashService(BasicCryptoAdapter())

        val hash = service.computeSha256(file.absolutePath)

        assertThat(service.verify(file.absolutePath, hash)).isTrue()
    }
}
