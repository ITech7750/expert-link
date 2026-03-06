package org.expert.link.mesh.infrastructure.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.call.CallSession
import org.expert.link.mesh.domain.model.call.CallStatus
import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.port.external.EventLogRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.FileTransferRepositoryPort
import org.expert.link.mesh.domain.model.diagnostics.EventLogEntry

/** Репозиторий передач файлов в памяти. */
class InMemoryFileTransferRepositoryAdapter : FileTransferRepositoryPort {
    private val mutex = Mutex()
    private val sessions = linkedMapOf<String, FileTransferSession>()

    override suspend fun save(session: FileTransferSession): FileTransferSession = mutex.withLock {
        sessions[session.transferId] = session
        session
    }

    override suspend fun findByTransferId(transferId: String): FileTransferSession? = mutex.withLock { sessions[transferId] }

    override suspend fun list(): List<FileTransferSession> = mutex.withLock { sessions.values.toList() }

    override suspend fun updateStatus(transferId: String, status: FileTransferStatus): FileTransferSession? = mutex.withLock {
        sessions[transferId]?.let {
            val updated = it.copy(status = status, updatedAt = now())
            sessions[transferId] = updated
            updated
        }
    }
}

/** Репозиторий звонков в памяти. */
class InMemoryCallSessionRepositoryAdapter : CallSessionRepositoryPort {
    private val mutex = Mutex()
    private val sessions = linkedMapOf<String, CallSession>()

    override suspend fun save(session: CallSession): CallSession = mutex.withLock {
        sessions[session.callId] = session
        session
    }

    override suspend fun findByCallId(callId: String): CallSession? = mutex.withLock { sessions[callId] }

    override suspend fun list(): List<CallSession> = mutex.withLock { sessions.values.toList() }

    override suspend fun updateStatus(callId: String, status: CallStatus): CallSession? = mutex.withLock {
        sessions[callId]?.let {
            val updated = it.copy(status = status, updatedAt = now())
            sessions[callId] = updated
            updated
        }
    }
}

/** Репозиторий журнала событий в памяти. */
class InMemoryEventLogRepositoryAdapter : EventLogRepositoryPort {
    private val mutex = Mutex()
    private val entries = ArrayDeque<EventLogEntry>()

    override suspend fun append(entry: EventLogEntry): EventLogEntry = mutex.withLock {
        entries.addLast(entry)
        while (entries.size > 5_000) {
            entries.removeFirst()
        }
        entry
    }

    override suspend fun listRecent(limit: Int): List<EventLogEntry> = mutex.withLock {
        entries.takeLast(limit)
    }
}
