package org.expert.link.mesh.application.service

import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.model.filetransfer.TransferDirection
import org.junit.jupiter.api.Test

class FileResumeServiceTest {
    @Test
    fun `should identify missing and next outbound chunks`() {
        val session = FileTransferSession(
            transferId = "transfer-1",
            descriptor = FileDescriptor("file-1", "demo.txt", 12, "hash"),
            senderPeerId = "a",
            recipientPeerId = "b",
            direction = TransferDirection.OUTGOING,
            status = FileTransferStatus.IN_PROGRESS,
            chunkSizeBytes = 4,
            totalChunks = 3,
            acknowledgedChunks = setOf(0),
            receivedChunks = setOf(0, 2),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val service = FileResumeService()

        assertThat(service.missingChunks(session)).containsExactly(1)
        assertThat(service.nextOutboundChunk(session)).isEqualTo(1)
    }
}
