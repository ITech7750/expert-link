package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Публичный дескриптор файла. */
@Serializable
data class MeshFileDescriptor(
    val fileId: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val contentType: String? = null,
)

/** Направление передачи относительно локального узла. */
@Serializable
enum class MeshTransferDirection {
    OUTGOING,
    INCOMING,
}

/** Состояние передачи файла. */
@Serializable
enum class MeshFileTransferStatus {
    OFFERED,
    ACCEPTED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
}

/** Публичное состояние передачи файла. */
@Serializable
data class MeshFileTransferSession(
    val transferId: String,
    val conversationId: String? = null,
    val descriptor: MeshFileDescriptor,
    val senderPeerId: String,
    val recipientPeerId: String,
    val direction: MeshTransferDirection,
    val status: MeshFileTransferStatus,
    val chunkSizeBytes: Int,
    val totalChunks: Int,
    val acknowledgedChunks: Set<Int> = emptySet(),
    val receivedChunks: Set<Int> = emptySet(),
    val localPath: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
