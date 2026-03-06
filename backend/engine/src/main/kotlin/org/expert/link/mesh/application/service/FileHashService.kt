package org.expert.link.mesh.application.service

import org.expert.link.mesh.domain.port.external.CryptoPort
import java.io.File
import java.security.MessageDigest

/** Сервис вычисления и проверки хеша файла. */
class FileHashService(
    private val cryptoPort: CryptoPort,
) {
    /**
     * Computes the SHA-256 digest of a file.
     */
    fun computeSha256(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        File(path).inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }

    /**
     * Computes the hash of an in-memory byte array.
     */
    fun computeSha256(bytes: ByteArray): String = cryptoPort.sha256Hex(bytes)

    /**
     * Verifies that a file matches the expected SHA-256 digest.
     */
    fun verify(path: String, expectedSha256: String): Boolean = computeSha256(path) == expectedSha256
}
