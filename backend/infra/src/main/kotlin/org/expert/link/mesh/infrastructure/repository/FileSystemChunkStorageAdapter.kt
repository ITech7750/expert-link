package org.expert.link.mesh.infrastructure.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.expert.link.mesh.domain.port.repository.FileChunkStoragePort
import java.io.File

/** Адаптер хранения чанков в файловой системе. */
class FileSystemChunkStorageAdapter(
    private val baseDirectory: File,
) : FileChunkStoragePort {
    init {
        baseDirectory.mkdirs()
    }

    override suspend fun readChunk(path: String, chunkIndex: Int, chunkSizeBytes: Int): ByteArray = withContext(Dispatchers.IO) {
        val file = File(path)
        file.inputStream().use { input ->
            input.skip(chunkIndex.toLong() * chunkSizeBytes.toLong())
            val data = ByteArray(chunkSizeBytes)
            val read = input.read(data)
            if (read <= 0) {
                ByteArray(0)
            } else if (read == data.size) {
                data
            } else {
                data.copyOf(read)
            }
        }
    }

    override suspend fun writeChunk(transferId: String, chunkIndex: Int, data: ByteArray) = withContext(Dispatchers.IO) {
        transferDirectory(transferId).resolve(chunkFileName(chunkIndex)).writeBytes(data)
    }

    override suspend fun hasChunk(transferId: String, chunkIndex: Int): Boolean = withContext(Dispatchers.IO) {
        transferDirectory(transferId).resolve(chunkFileName(chunkIndex)).exists()
    }

    override suspend fun listMissingChunks(transferId: String, totalChunks: Int): Set<Int> = withContext(Dispatchers.IO) {
        (0 until totalChunks).filterNot { transferDirectory(transferId).resolve(chunkFileName(it)).exists() }.toSet()
    }

    override suspend fun assembleFile(transferId: String, targetPath: String, totalChunks: Int): String = withContext(Dispatchers.IO) {
        val target = File(targetPath)
        target.parentFile?.mkdirs()
        target.outputStream().use { output ->
            for (index in 0 until totalChunks) {
                output.write(transferDirectory(transferId).resolve(chunkFileName(index)).readBytes())
            }
        }
        target.absolutePath
    }

    private fun transferDirectory(transferId: String): File = baseDirectory.resolve(transferId).also(File::mkdirs)

    private fun chunkFileName(chunkIndex: Int): String = chunkIndex.toString().padStart(8, '0') + ".chunk"
}
