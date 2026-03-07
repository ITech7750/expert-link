package org.expert.link.mesh.infrastructure.repository

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class FileSystemChunkStorageAdapterTest {
    @Test
    fun `should inspect file and compute sha256`() = runTest {
        val workingDirectory = createTempDirectory("chunk-storage").toFile()
        val sourceFile = File(workingDirectory, "payload.txt").apply {
            writeText("hello secure mesh")
        }
        val storage = FileSystemChunkStorageAdapter(workingDirectory.absolutePath)

        val descriptor = storage.describe(sourceFile.absolutePath)
        val hash = storage.computeSha256(sourceFile.absolutePath)

        assertThat(descriptor.handle).isEqualTo(sourceFile.absolutePath)
        assertThat(descriptor.fileName).isEqualTo("payload.txt")
        assertThat(descriptor.sizeBytes).isEqualTo(sourceFile.length())
        assertThat(hash).hasSize(64)
    }
}
