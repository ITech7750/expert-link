package org.expert.link.mesh.domain.model.filetransfer

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Метаданные файла для передачи. */
@Serializable
data class FileDescriptor(
    val fileId: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val contentType: String? = null,
)

/** Направление передачи относительно локального узла. */
@Serializable
enum class TransferDirection {
    OUTGOING,
    INCOMING,
}

/** Состояния передачи файла. */
@Serializable
enum class FileTransferStatus {
    OFFERED,
    ACCEPTED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
}

/** Состояние передачи файла по чанкам. */
@Serializable
data class FileTransferSession(
    val transferId: String,
    val conversationId: String? = null,
    val descriptor: FileDescriptor,
    val senderPeerId: String,
    val recipientPeerId: String,
    val direction: TransferDirection,
    val status: FileTransferStatus,
    val chunkSizeBytes: Int,
    val totalChunks: Int,
    val acknowledgedChunks: Set<Int> = emptySet(),
    val receivedChunks: Set<Int> = emptySet(),
    val localPath: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Один зашифрованный чанк файла. */
@Serializable
data class FileChunk(
    val transferId: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val dataBase64: String,
    val sha256: String,
    val createdAt: Instant,
)

/** Квитанция после сохранения чанка. */
@Serializable
data class FileChunkReceipt(
    val transferId: String,
    val chunkIndex: Int,
    val receivedAt: Instant,
)

/** Запрос на повтор недостающих чанков. */
@Serializable
data class FileResumeRequest(
    val transferId: String,
    val missingChunkIndices: Set<Int>,
    val requestedAt: Instant,
)
