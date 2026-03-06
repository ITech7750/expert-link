package org.expert.link.mesh.application.service

import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession

/** Сервис расчёта недостающих чанков при resume. */
class FileResumeService {
    /**
     * Returns chunk indices still missing on the receiving side.
     */
    fun missingChunks(session: FileTransferSession): Set<Int> = (0 until session.totalChunks).toSet() - session.receivedChunks

    /**
     * Returns the next outbound chunk index that has not been acknowledged yet.
     */
    fun nextOutboundChunk(session: FileTransferSession): Int? = (0 until session.totalChunks).firstOrNull { it !in session.acknowledgedChunks }
}
