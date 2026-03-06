package org.expert.link.mesh.domain.port.repository

import org.expert.link.mesh.domain.model.call.CallSession
import org.expert.link.mesh.domain.model.call.CallStatus
import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus

/** Порт хранения передачи файла. */
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

/** Порт работы с чанками и файловым источником. */
interface FileChunkStoragePort {
    /** Читает chunk файла. */
    suspend fun readChunk(path: String, chunkIndex: Int, chunkSizeBytes: Int): ByteArray

    /** Сохраняет входящий chunk. */
    suspend fun writeChunk(transferId: String, chunkIndex: Int, data: ByteArray)

    /** Возвращает `true`, если chunk уже сохранён. */
    suspend fun hasChunk(transferId: String, chunkIndex: Int): Boolean

    /** Возвращает индексы missing chunks. */
    suspend fun listMissingChunks(transferId: String, totalChunks: Int): Set<Int>

    /** Собирает файл и возвращает итоговый путь. */
    suspend fun assembleFile(transferId: String, targetPath: String, totalChunks: Int): String
}

/** Порт хранения звонка. */
interface CallSessionRepositoryPort {
    /** Сохраняет call session. */
    suspend fun save(session: CallSession): CallSession

    /** Ищет call session по id. */
    suspend fun findByCallId(callId: String): CallSession?

    /** Возвращает все call session. */
    suspend fun list(): List<CallSession>

    /** Обновляет статус звонка. */
    suspend fun updateStatus(callId: String, status: CallStatus): CallSession?
}
