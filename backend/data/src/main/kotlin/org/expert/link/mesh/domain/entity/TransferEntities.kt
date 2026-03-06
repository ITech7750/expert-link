package org.expert.link.mesh.domain.entity

import kotlinx.serialization.Serializable

/** Сущность хранения передачи файла. */
@Serializable
data class FileTransferEntity(
    val transferId: String,
    val conversationId: String? = null,
    val descriptorJson: String,
    val senderPeerId: String,
    val recipientPeerId: String,
    val direction: String,
    val status: String,
    val chunkSizeBytes: Int,
    val totalChunks: Int,
    val acknowledgedChunks: List<Int> = emptyList(),
    val receivedChunks: List<Int> = emptyList(),
    val localPath: String? = null,
    val createdAt: String,
    val updatedAt: String,
)
