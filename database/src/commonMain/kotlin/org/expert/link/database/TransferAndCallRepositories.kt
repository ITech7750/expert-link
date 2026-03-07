package org.expert.link.database

import kotlinx.datetime.Clock
import org.expert.link.mesh.domain.model.call.CallEvent
import org.expert.link.mesh.domain.model.call.CallParticipant
import org.expert.link.mesh.domain.model.call.CallRoom
import org.expert.link.mesh.domain.model.call.CallSession
import org.expert.link.mesh.domain.model.call.CallState
import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.port.repository.CallEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallParticipantRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallRoomRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.FileTransferRepositoryPort

internal class RoomFileTransferRepository(
    private val transferDao: TransferDao,
) : FileTransferRepositoryPort {
    override suspend fun save(session: FileTransferSession): FileTransferSession {
        transferDao.upsertTransfer(
            FileTransferRecord(
                transferId = session.transferId,
                conversationId = session.conversationId,
                recipientPeerId = session.recipientPeerId,
                status = session.status.name,
                updatedAt = session.updatedAt.toString(),
                payloadJson = encodePayload(session),
            ),
        )
        return session
    }

    override suspend fun findByTransferId(transferId: String): FileTransferSession? =
        transferDao.findTransfer(transferId)?.let { decodePayload(it.payloadJson) }

    override suspend fun list(): List<FileTransferSession> =
        transferDao.listTransfers().map { decodePayload(it.payloadJson) }

    override suspend fun updateStatus(transferId: String, status: FileTransferStatus): FileTransferSession? {
        val current = findByTransferId(transferId) ?: return null
        val updated = current.copy(status = status, updatedAt = Clock.System.now())
        save(updated)
        return updated
    }
}

internal class RoomCallSessionRepository(
    private val callDao: CallDao,
) : CallSessionRepositoryPort {
    override suspend fun save(session: CallSession): CallSession {
        callDao.upsertCallSession(
            CallSessionRecord(
                callId = session.callId,
                roomId = session.roomId,
                status = session.status.name,
                updatedAt = session.updatedAt.toString(),
                payloadJson = encodePayload(session),
            ),
        )
        return session
    }

    override suspend fun findByCallId(callId: String): CallSession? =
        callDao.findCallSession(callId)?.let { decodePayload(it.payloadJson) }

    override suspend fun list(): List<CallSession> =
        callDao.listCallSessions().map { decodePayload(it.payloadJson) }

    override suspend fun listByRoomId(roomId: String): List<CallSession> =
        callDao.listCallSessionsByRoom(roomId).map { decodePayload(it.payloadJson) }

    override suspend fun updateStatus(callId: String, status: CallState): CallSession? {
        val current = findByCallId(callId) ?: return null
        val updated = current.copy(status = status, updatedAt = Clock.System.now())
        save(updated)
        return updated
    }
}

internal class RoomCallRoomRepository(
    private val callDao: CallDao,
) : CallRoomRepositoryPort {
    override suspend fun save(room: CallRoom): CallRoom {
        callDao.upsertCallRoom(
            CallRoomRecord(
                roomId = room.roomId,
                activeCallId = room.activeCallId,
                updatedAt = room.updatedAt.toString(),
                payloadJson = encodePayload(room),
            ),
        )
        return room
    }

    override suspend fun findByRoomId(roomId: String): CallRoom? =
        callDao.findCallRoom(roomId)?.let { decodePayload(it.payloadJson) }

    override suspend fun list(): List<CallRoom> =
        callDao.listCallRooms().map { decodePayload(it.payloadJson) }
}

internal class RoomCallParticipantRepository(
    private val callDao: CallDao,
) : CallParticipantRepositoryPort {
    override suspend fun replace(callId: String, participants: List<CallParticipant>) {
        callDao.deleteCallParticipants(callId)
        if (participants.isNotEmpty()) {
            val roomId = callDao.findCallSession(callId)?.roomId ?: callId
            callDao.upsertCallParticipants(
                participants.map { participant ->
                    CallParticipantRecord(
                        callId = callId,
                        roomId = roomId,
                        peerId = participant.peerId,
                        updatedAt = participant.updatedAt.toString(),
                        payloadJson = encodePayload(participant),
                    )
                },
            )
        }
    }

    override suspend fun upsert(callId: String, participant: CallParticipant) {
        val currentRoomId = callDao.findCallSession(callId)?.roomId ?: callId
        callDao.upsertCallParticipant(
            CallParticipantRecord(
                callId = callId,
                roomId = currentRoomId,
                peerId = participant.peerId,
                updatedAt = participant.updatedAt.toString(),
                payloadJson = encodePayload(participant),
            ),
        )
    }

    override suspend fun listByCallId(callId: String): List<CallParticipant> =
        callDao.listCallParticipants(callId).map { decodePayload(it.payloadJson) }
}

internal class RoomCallEventRepository(
    private val callDao: CallDao,
) : CallEventRepositoryPort {
    override suspend fun append(event: CallEvent): CallEvent {
        callDao.upsertCallEvent(
            CallEventRecord(
                eventId = event.eventId,
                callId = event.callId,
                roomId = event.roomId,
                createdAt = event.createdAt.toString(),
                payloadJson = encodePayload(event),
            ),
        )
        return event
    }

    override suspend fun listByCallId(callId: String, limit: Int): List<CallEvent> =
        callDao.listCallEvents(callId, limit)
            .asReversed()
            .map { decodePayload(it.payloadJson) }
}
