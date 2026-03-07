package org.expert.link.mesh.application.mapper

import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.expert.link.mesh.domain.entity.CallSessionEntity
import org.expert.link.mesh.domain.entity.FileTransferEntity
import org.expert.link.mesh.domain.model.call.CallSession
import org.expert.link.mesh.domain.model.call.CallStatus
import org.expert.link.mesh.domain.model.call.MediaQualitySnapshot
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.model.filetransfer.TransferDirection

/** Маппер передачи файла. */
object FileTransferEntityMapper {
    /**
     * Converts a transfer session into a storage entity.
     */
    fun toEntity(session: FileTransferSession): FileTransferEntity = FileTransferEntity(
        transferId = session.transferId,
        conversationId = session.conversationId,
        descriptorJson = Json.encodeToString(FileDescriptor.serializer(), session.descriptor),
        senderPeerId = session.senderPeerId,
        recipientPeerId = session.recipientPeerId,
        direction = session.direction.name,
        status = session.status.name,
        chunkSizeBytes = session.chunkSizeBytes,
        totalChunks = session.totalChunks,
        acknowledgedChunks = session.acknowledgedChunks.toList(),
        receivedChunks = session.receivedChunks.toList(),
        localPath = session.localPath,
        createdAt = session.createdAt.toString(),
        updatedAt = session.updatedAt.toString(),
    )

    /**
     * Converts a storage entity back into a transfer session.
     */
    fun fromEntity(entity: FileTransferEntity): FileTransferSession = FileTransferSession(
        transferId = entity.transferId,
        conversationId = entity.conversationId,
        descriptor = Json.decodeFromString(FileDescriptor.serializer(), entity.descriptorJson),
        senderPeerId = entity.senderPeerId,
        recipientPeerId = entity.recipientPeerId,
        direction = TransferDirection.valueOf(entity.direction),
        status = FileTransferStatus.valueOf(entity.status),
        chunkSizeBytes = entity.chunkSizeBytes,
        totalChunks = entity.totalChunks,
        acknowledgedChunks = entity.acknowledgedChunks.toSet(),
        receivedChunks = entity.receivedChunks.toSet(),
        localPath = entity.localPath,
        createdAt = Instant.parse(entity.createdAt),
        updatedAt = Instant.parse(entity.updatedAt),
    )
}

/** Маппер звонка. */
object CallSessionEntityMapper {
    /**
     * Converts a call session into a storage entity.
     */
    fun toEntity(session: CallSession): CallSessionEntity = CallSessionEntity(
        callId = session.callId,
        conversationId = session.conversationId,
        initiatorPeerId = session.initiatorPeerId,
        recipientPeerId = session.recipientPeerId,
        status = session.status.name,
        createdAt = session.createdAt.toString(),
        updatedAt = session.updatedAt.toString(),
        lastSignalAt = session.lastSignalAt?.toString(),
        qualitySnapshotJson = session.qualitySnapshot?.let { Json.encodeToString(MediaQualitySnapshot.serializer(), it) },
    )

    /**
     * Converts a storage entity back into a call session.
     */
    fun fromEntity(entity: CallSessionEntity): CallSession = CallSession(
        callId = entity.callId,
        conversationId = entity.conversationId,
        initiatorPeerId = entity.initiatorPeerId,
        recipientPeerId = entity.recipientPeerId,
        status = CallStatus.valueOf(entity.status),
        createdAt = Instant.parse(entity.createdAt),
        updatedAt = Instant.parse(entity.updatedAt),
        lastSignalAt = entity.lastSignalAt?.let(Instant::parse),
        qualitySnapshot = entity.qualitySnapshotJson?.let { Json.decodeFromString(MediaQualitySnapshot.serializer(), it) },
    )
}
