package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.factory.PacketEnvelopeFactory
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.application.support.plusSeconds
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.filetransfer.FileChunk
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.model.filetransfer.TransferDirection
import org.expert.link.mesh.domain.model.network.FileAccept
import org.expert.link.mesh.domain.model.network.FileAck
import org.expert.link.mesh.domain.model.network.FileChunkPayload
import org.expert.link.mesh.domain.model.network.FileComplete
import org.expert.link.mesh.domain.model.network.FileOffer
import org.expert.link.mesh.domain.model.network.FileResumeRequestPayload
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.port.external.CryptoPort
import org.expert.link.mesh.domain.port.repository.FileChunkStoragePort
import org.expert.link.mesh.domain.port.repository.FileTransferRepositoryPort
import org.expert.link.mesh.domain.support.decodeBase64
import org.expert.link.mesh.domain.support.encodeBase64
import kotlin.math.ceil

/**
 * Сервис передачи файлов по чанкам.
 *
 * Отвечает за offer/accept, отправку чанков, resume и проверку хеша.
 */
class FileTransferService(
    private val cryptoPort: CryptoPort,
    private val localProfileService: LocalProfileService,
    private val peerTrustVerificationService: PeerTrustVerificationService,
    private val messageEncryptionService: MessageEncryptionService,
    private val packetEnvelopeFactory: PacketEnvelopeFactory,
    private val packetSignatureService: PacketSignatureService,
    private val deliveryTrackingService: DeliveryTrackingService,
    private val fileTransferRepositoryPort: FileTransferRepositoryPort,
    private val fileChunkStoragePort: FileChunkStoragePort,
    private val fileResumeService: FileResumeService,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
    private val downloadDirectory: String,
    private val defaultChunkSizeBytes: Int,
) {
    /** Запускает исходящую передачу файла. */
    suspend fun offerFile(targetPeerId: String, sourcePath: String, conversationId: String? = null): FileTransferSession {
        val localProfile = localProfileService.require()
        val trustedPeer = requireNotNull(peerTrustVerificationService.requireTrusted(targetPeerId)) {
            "Peer $targetPeerId is not trusted"
        }
        val file = fileChunkStoragePort.describe(sourcePath)
        val descriptor = FileDescriptor(
            fileId = newId("file"),
            fileName = file.fileName,
            sizeBytes = file.sizeBytes,
            sha256 = fileChunkStoragePort.computeSha256(sourcePath),
            contentType = file.contentType,
        )
        val totalChunks = ceil(file.sizeBytes.toDouble() / defaultChunkSizeBytes.toDouble()).toInt().coerceAtLeast(1)
        val session = FileTransferSession(
            transferId = newId("transfer"),
            conversationId = conversationId,
            descriptor = descriptor,
            senderPeerId = localProfile.peerId,
            recipientPeerId = targetPeerId,
            direction = TransferDirection.OUTGOING,
            status = FileTransferStatus.OFFERED,
            chunkSizeBytes = defaultChunkSizeBytes,
            totalChunks = totalChunks,
            localPath = sourcePath,
            createdAt = now(),
            updatedAt = now(),
        )
        fileTransferRepositoryPort.save(session)
        val offer = FileOffer(
            transferId = session.transferId,
            descriptor = descriptor,
            chunkSizeBytes = defaultChunkSizeBytes,
            totalChunks = totalChunks,
            senderPeerId = localProfile.peerId,
            recipientPeerId = targetPeerId,
            createdAt = now(),
        )
        val encrypted = messageEncryptionService.encryptPayload(trustedPeer.peerIdentity.publicKey, offer)
        val unsigned = packetEnvelopeFactory.create(
            packetType = PacketType.FILE_OFFER,
            sourcePeerId = localProfile.peerId,
            targetPeerId = targetPeerId,
            encryptedPayload = encrypted,
            routeMode = RouteMode.LOCAL_DIRECT,
            ttl = 5,
            requiresAck = false,
            conversationId = conversationId,
        )
        val signed = packetSignatureService.signEnvelope(localProfile.privateKey, unsigned)
        deliveryTrackingService.send(signed)
        eventLogService.log(
            category = EventCategory.FILE_TRANSFER,
            level = EventLevel.INFO,
            message = "Sent file offer",
            peerId = targetPeerId,
            packetId = signed.packetId,
            attributes = mapOf("transferId" to session.transferId),
        )
        return session
    }

    /**
     * Accepts an inbound file offer and notifies the sender.
     */
    suspend fun handleFileOffer(offer: FileOffer): FileTransferSession {
        val localProfile = localProfileService.require()
        val session = FileTransferSession(
            transferId = offer.transferId,
            conversationId = null,
            descriptor = offer.descriptor,
            senderPeerId = offer.senderPeerId,
            recipientPeerId = localProfile.peerId,
            direction = TransferDirection.INCOMING,
            status = FileTransferStatus.ACCEPTED,
            chunkSizeBytes = offer.chunkSizeBytes,
            totalChunks = offer.totalChunks,
            localPath = fileChunkStoragePort.resolveTargetPath(
                baseDirectory = downloadDirectory,
                transferId = offer.transferId,
                fileName = offer.descriptor.fileName,
            ),
            createdAt = now(),
            updatedAt = now(),
        )
        fileTransferRepositoryPort.save(session)
        sendControlPacket(
            targetPeerId = offer.senderPeerId,
            targetPublicKey = requireNotNull(peerTrustVerificationService.requireTrusted(offer.senderPeerId)).peerIdentity.publicKey,
            packetType = PacketType.FILE_ACCEPT,
            payload = FileAccept(transferId = offer.transferId, acceptedAt = now()),
            conversationId = session.conversationId,
        )
        nodeMetricsService.increment("file.offer.received")
        return session
    }

    /**
     * Handles confirmation that the receiver accepted the file transfer.
     */
    suspend fun handleFileAccept(accept: FileAccept): FileTransferSession? {
        val session = fileTransferRepositoryPort.findByTransferId(accept.transferId) ?: return null
        if (session.status == FileTransferStatus.CANCELLED) {
            return session
        }
        val updated = fileTransferRepositoryPort.save(session.copy(status = FileTransferStatus.ACCEPTED, updatedAt = now()))
        sendNextChunk(updated)
        return updated
    }

    /**
     * Persists an inbound chunk, acknowledges it and completes the file if all chunks are available.
     */
    suspend fun handleFileChunk(payload: FileChunkPayload): FileTransferSession? {
        val session = fileTransferRepositoryPort.findByTransferId(payload.chunk.transferId) ?: return null
        val data = decodeBase64(payload.chunk.dataBase64)
        fileChunkStoragePort.writeChunk(payload.chunk.transferId, payload.chunk.chunkIndex, data)
        val updated = fileTransferRepositoryPort.save(
            session.copy(
                status = FileTransferStatus.IN_PROGRESS,
                receivedChunks = session.receivedChunks + payload.chunk.chunkIndex,
                updatedAt = now(),
            ),
        )
        sendControlPacket(
            targetPeerId = updated.senderPeerId,
            targetPublicKey = requireNotNull(peerTrustVerificationService.requireTrusted(updated.senderPeerId)).peerIdentity.publicKey,
            packetType = PacketType.FILE_ACK,
            payload = FileAck(updated.transferId, payload.chunk.chunkIndex, now()),
            conversationId = updated.conversationId,
        )
        if (updated.receivedChunks.size == updated.totalChunks) {
            val assembledPath = fileChunkStoragePort.assembleFile(updated.transferId, requireNotNull(updated.localPath), updated.totalChunks)
            val verified = fileChunkStoragePort.computeSha256(assembledPath) == updated.descriptor.sha256
            val finalStatus = if (verified) FileTransferStatus.COMPLETED else FileTransferStatus.FAILED
            val finalSession = fileTransferRepositoryPort.save(updated.copy(status = finalStatus, updatedAt = now()))
            if (verified) {
                sendControlPacket(
                    targetPeerId = finalSession.senderPeerId,
                    targetPublicKey = requireNotNull(peerTrustVerificationService.requireTrusted(finalSession.senderPeerId)).peerIdentity.publicKey,
                    packetType = PacketType.FILE_COMPLETE,
                    payload = FileComplete(finalSession.transferId, now(), finalSession.descriptor.sha256),
                    conversationId = finalSession.conversationId,
                )
            }
            return finalSession
        }
        return updated
    }

    /**
     * Handles a chunk acknowledgement and sends the next chunk if necessary.
     */
    suspend fun handleFileAck(ack: FileAck): FileTransferSession? {
        val session = fileTransferRepositoryPort.findByTransferId(ack.transferId) ?: return null
        if (session.status == FileTransferStatus.CANCELLED) {
            return session
        }
        val updated = fileTransferRepositoryPort.save(
            session.copy(
                status = FileTransferStatus.IN_PROGRESS,
                acknowledgedChunks = session.acknowledgedChunks + ack.chunkIndex,
                updatedAt = now(),
            ),
        )
        sendNextChunk(updated)
        return updated
    }

    /**
     * Sends requested missing chunks again.
     */
    suspend fun handleResumeRequest(payload: FileResumeRequestPayload): FileTransferSession? {
        val session = fileTransferRepositoryPort.findByTransferId(payload.request.transferId) ?: return null
        if (session.status == FileTransferStatus.CANCELLED) {
            return session
        }
        payload.request.missingChunkIndices.sorted().forEach { sendChunk(session, it) }
        return session
    }

    /**
     * Marks the sender-side session as completed after receiver verification.
     */
    suspend fun handleFileComplete(payload: FileComplete): FileTransferSession? {
        val session = fileTransferRepositoryPort.findByTransferId(payload.transferId) ?: return null
        return fileTransferRepositoryPort.save(session.copy(status = FileTransferStatus.COMPLETED, updatedAt = now()))
    }

    /**
     * Requests retransmission of missing chunks for an inbound transfer.
     */
    suspend fun requestResume(transferId: String): FileTransferSession? {
        val session = fileTransferRepositoryPort.findByTransferId(transferId) ?: return null
        val missing = fileResumeService.missingChunks(session)
        if (missing.isEmpty()) {
            return session
        }
        sendControlPacket(
            targetPeerId = session.senderPeerId,
            targetPublicKey = requireNotNull(peerTrustVerificationService.requireTrusted(session.senderPeerId)).peerIdentity.publicKey,
            packetType = PacketType.FILE_RESUME_REQUEST,
            payload = FileResumeRequestPayload(org.expert.link.mesh.domain.model.filetransfer.FileResumeRequest(session.transferId, missing, now())),
            conversationId = session.conversationId,
        )
        return session
    }

    /** Отменяет передачу файла на локальном узле. */
    suspend fun cancelTransfer(transferId: String): FileTransferSession? {
        val session = fileTransferRepositoryPort.updateStatus(transferId, FileTransferStatus.CANCELLED) ?: return null
        eventLogService.log(
            category = EventCategory.FILE_TRANSFER,
            level = EventLevel.WARN,
            message = "Cancelled file transfer",
            peerId = if (session.direction == TransferDirection.OUTGOING) session.recipientPeerId else session.senderPeerId,
            attributes = mapOf("transferId" to transferId),
        )
        nodeMetricsService.increment("file.transfer.cancelled")
        return session
    }

    private suspend fun sendNextChunk(session: FileTransferSession) {
        if (session.status == FileTransferStatus.CANCELLED) {
            return
        }
        val nextChunk = fileResumeService.nextOutboundChunk(session) ?: return
        sendChunk(session, nextChunk)
    }

    private suspend fun sendChunk(session: FileTransferSession, chunkIndex: Int) {
        if (session.status == FileTransferStatus.CANCELLED) {
            return
        }
        val localProfile = localProfileService.require()
        val trustedPeer = requireNotNull(peerTrustVerificationService.requireTrusted(session.recipientPeerId)) {
            "Peer ${session.recipientPeerId} is not trusted"
        }
        val data = fileChunkStoragePort.readChunk(requireNotNull(session.localPath), chunkIndex, session.chunkSizeBytes)
        val chunk = FileChunk(
            transferId = session.transferId,
            chunkIndex = chunkIndex,
            totalChunks = session.totalChunks,
            dataBase64 = encodeBase64(data),
            sha256 = cryptoPort.sha256Hex(data),
            createdAt = now(),
        )
        val encrypted = messageEncryptionService.encryptPayload(trustedPeer.peerIdentity.publicKey, FileChunkPayload(chunk))
        val unsigned = packetEnvelopeFactory.create(
            packetType = PacketType.FILE_CHUNK,
            sourcePeerId = localProfile.peerId,
            targetPeerId = session.recipientPeerId,
            encryptedPayload = encrypted,
            routeMode = RouteMode.LOCAL_DIRECT,
            ttl = 5,
            requiresAck = false,
            conversationId = session.conversationId,
        )
        val signed = packetSignatureService.signEnvelope(localProfile.privateKey, unsigned)
        deliveryTrackingService.send(signed)
        eventLogService.log(
            category = EventCategory.FILE_TRANSFER,
            level = EventLevel.INFO,
            message = "Sent file chunk",
            peerId = session.recipientPeerId,
            packetId = signed.packetId,
            attributes = mapOf(
                "transferId" to session.transferId,
                "chunkIndex" to chunkIndex.toString(),
            ),
        )
    }

    private suspend fun sendControlPacket(
        targetPeerId: String,
        targetPublicKey: String,
        packetType: PacketType,
        payload: org.expert.link.mesh.domain.model.network.PacketPayload,
        conversationId: String? = null,
    ) {
        val localProfile = localProfileService.require()
        val encrypted = messageEncryptionService.encryptPayload(targetPublicKey, payload)
        val unsigned = packetEnvelopeFactory.create(
            packetType = packetType,
            sourcePeerId = localProfile.peerId,
            targetPeerId = targetPeerId,
            encryptedPayload = encrypted,
            routeMode = RouteMode.LOCAL_DIRECT,
            ttl = 5,
            requiresAck = false,
            conversationId = conversationId,
        )
        val signed = packetSignatureService.signEnvelope(localProfile.privateKey, unsigned)
        deliveryTrackingService.send(signed)
    }
}
