package org.expert.link.mesh.domain.port.repository

import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.model.storage.LocalFileResource

/** Порт постоянного хранения сессий передачи файлов. */
interface FileTransferRepositoryPort {
    /** Сохраняет file transfer session. */
    suspend fun save(session: FileTransferSession): FileTransferSession

    /** Ищет file transfer session по id. */
    suspend fun findByTransferId(transferId: String): FileTransferSession?

    /** Возвращает все file transfer session. */
    suspend fun list(): List<FileTransferSession>

    /** Обновляет статус передачи. */
    suspend fun updateStatus(transferId: String, status: FileTransferStatus): FileTransferSession?
}

/** Порт работы с файловым источником и чанковым staging-хранилищем.
 *
 * Общий слой считает строковый `path` платформенным file handle.
 * JVM, Android и iOS могут интерпретировать его по-своему.
 */
interface FileChunkStoragePort {
    /** Описывает локальный файл по платформенному handle. */
    suspend fun describe(path: String): LocalFileResource

    /** Читает chunk файла. */
    suspend fun readChunk(path: String, chunkIndex: Int, chunkSizeBytes: Int): ByteArray

    /** Вычисляет SHA-256 для файла по платформенному handle. */
    suspend fun computeSha256(path: String): String

    /** Сохраняет входящий chunk. */
    suspend fun writeChunk(transferId: String, chunkIndex: Int, data: ByteArray)

    /** Возвращает `true`, если chunk уже сохранён. */
    suspend fun hasChunk(transferId: String, chunkIndex: Int): Boolean

    /** Возвращает индексы missing chunks. */
    suspend fun listMissingChunks(transferId: String, totalChunks: Int): Set<Int>

    /** Строит platform-specific handle для собранного входящего файла. */
    fun resolveTargetPath(baseDirectory: String, transferId: String, fileName: String): String

    /** Собирает файл и возвращает итоговый путь. */
    suspend fun assembleFile(transferId: String, targetPath: String, totalChunks: Int): String
}
