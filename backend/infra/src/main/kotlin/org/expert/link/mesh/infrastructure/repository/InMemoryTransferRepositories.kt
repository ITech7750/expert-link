package org.expert.link.mesh.infrastructure.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.call.CallEvent
import org.expert.link.mesh.domain.model.call.CallParticipant
import org.expert.link.mesh.domain.model.call.CallRoom
import org.expert.link.mesh.domain.model.call.CallSession
import org.expert.link.mesh.domain.model.call.CallState
import org.expert.link.mesh.domain.model.diagnostics.EventLogEntry
import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.port.external.EventLogRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallParticipantRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallRoomRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.FileTransferRepositoryPort

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

/** Репозиторий call-сессий в памяти. */
class InMemoryCallSessionRepositoryAdapter : CallSessionRepositoryPort {
    private val mutex = Mutex()
    private val sessions = linkedMapOf<String, CallSession>()

    override suspend fun save(session: CallSession): CallSession = mutex.withLock {
        sessions[session.callId] = session
        session
    }

    override suspend fun findByCallId(callId: String): CallSession? = mutex.withLock { sessions[callId] }

    override suspend fun list(): List<CallSession> = mutex.withLock { sessions.values.toList() }

    override suspend fun listByRoomId(roomId: String): List<CallSession> = mutex.withLock {
        sessions.values.filter { it.roomId == roomId }
    }

    override suspend fun updateStatus(callId: String, status: CallState): CallSession? = mutex.withLock {
        sessions[callId]?.let {
            val updated = it.copy(status = status, updatedAt = now())
            sessions[callId] = updated
            updated
        }
    }
}

/** Репозиторий call-комнат в памяти. */
class InMemoryCallRoomRepositoryAdapter : CallRoomRepositoryPort {
    private val mutex = Mutex()
    private val rooms = linkedMapOf<String, CallRoom>()

    override suspend fun save(room: CallRoom): CallRoom = mutex.withLock {
        rooms[room.roomId] = room
        room
    }

    override suspend fun findByRoomId(roomId: String): CallRoom? = mutex.withLock { rooms[roomId] }

    override suspend fun list(): List<CallRoom> = mutex.withLock { rooms.values.toList() }
}

/** Репозиторий участников звонков в памяти. */
class InMemoryCallParticipantRepositoryAdapter : CallParticipantRepositoryPort {
    private val mutex = Mutex()
    private val byCall = linkedMapOf<String, LinkedHashMap<String, CallParticipant>>()

    override suspend fun replace(callId: String, participants: List<CallParticipant>) {
        mutex.withLock {
            val map = linkedMapOf<String, CallParticipant>()
            participants.forEach { map[it.peerId] = it }
            byCall[callId] = map
        }
    }

    override suspend fun upsert(callId: String, participant: CallParticipant) {
        mutex.withLock {
            val map = byCall.getOrPut(callId) { linkedMapOf() }
            map[participant.peerId] = participant
        }
    }

    override suspend fun listByCallId(callId: String): List<CallParticipant> = mutex.withLock {
        byCall[callId]?.values?.toList() ?: emptyList()
    }
}

/** Репозиторий событий звонков в памяти. */
class InMemoryCallEventRepositoryAdapter : CallEventRepositoryPort {
    private val mutex = Mutex()
    private val byCall = linkedMapOf<String, ArrayDeque<CallEvent>>()
    private val maxEvents = 2_000

    override suspend fun append(event: CallEvent): CallEvent = mutex.withLock {
        val queue = byCall.getOrPut(event.callId) { ArrayDeque() }
        queue.addLast(event)
        while (queue.size > maxEvents) {
            queue.removeFirst()
        }
        event
    }

    override suspend fun listByCallId(callId: String, limit: Int): List<CallEvent> = mutex.withLock {
        val items = byCall[callId]?.toList() ?: emptyList()
        if (limit >= items.size) items else items.takeLast(limit)
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
        val items = entries.toList()
        if (limit >= items.size) items else items.subList(items.size - limit, items.size)
    }
}
