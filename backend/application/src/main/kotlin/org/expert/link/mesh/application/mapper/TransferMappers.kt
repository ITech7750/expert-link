package org.expert.link.mesh.application.mapper

import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.expert.link.mesh.domain.entity.CallEventEntity
import org.expert.link.mesh.domain.entity.CallParticipantEntity
import org.expert.link.mesh.domain.entity.CallRoomEntity
import org.expert.link.mesh.domain.entity.CallSessionEntity
import org.expert.link.mesh.domain.entity.FileTransferEntity
import org.expert.link.mesh.domain.model.call.CallEvent
import org.expert.link.mesh.domain.model.call.CallEventType
import org.expert.link.mesh.domain.model.call.CallInvitation
import org.expert.link.mesh.domain.model.call.CallParticipant
import org.expert.link.mesh.domain.model.call.CallParticipantState
import org.expert.link.mesh.domain.model.call.CallRoom
import org.expert.link.mesh.domain.model.call.CallScope
import org.expert.link.mesh.domain.model.call.CallSession
import org.expert.link.mesh.domain.model.call.CallState
import org.expert.link.mesh.domain.model.call.CallType
import org.expert.link.mesh.domain.model.call.MediaQualitySnapshot
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.model.filetransfer.TransferDirection

private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

/** Маппер передачи файла. */
object FileTransferEntityMapper {
    /** Конвертирует file transfer session в storage entity. */
    fun toEntity(session: FileTransferSession): FileTransferEntity = FileTransferEntity(
        transferId = session.transferId,
        conversationId = session.conversationId,
        descriptorJson = json.encodeToString(FileDescriptor.serializer(), session.descriptor),
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

    /** Конвертирует storage entity в file transfer session. */
    fun fromEntity(entity: FileTransferEntity): FileTransferSession = FileTransferSession(
        transferId = entity.transferId,
        conversationId = entity.conversationId,
        descriptor = json.decodeFromString(FileDescriptor.serializer(), entity.descriptorJson),
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

/** Маппер call session. */
object CallSessionEntityMapper {
    /** Конвертирует call session в storage entity. */
    fun toEntity(session: CallSession): CallSessionEntity = CallSessionEntity(
        callId = session.callId,
        roomId = session.roomId,
        conversationId = session.conversationId,
        initiatorPeerId = session.initiatorPeerId,
        recipientPeerId = session.recipientPeerId,
        callType = session.callType.name,
        callScope = session.callScope.name,
        targetPeerIdsJson = json.encodeToString(SetSerializer(String.serializer()), session.targetPeerIds),
        status = session.status.name,
        participantsJson = json.encodeToString(ListSerializer(CallParticipant.serializer()), session.participants),
        invitationJson = session.invitation?.let { json.encodeToString(CallInvitation.serializer(), it) },
        createdAt = session.createdAt.toString(),
        updatedAt = session.updatedAt.toString(),
        lastSignalAt = session.lastSignalAt?.toString(),
        qualitySnapshotJson = session.qualitySnapshot?.let { json.encodeToString(MediaQualitySnapshot.serializer(), it) },
        reconnectAttempts = session.reconnectAttempts,
        metadataJson = json.encodeToString(MapSerializer(String.serializer(), String.serializer()), session.metadata),
    )

    /** Конвертирует storage entity в call session. */
    fun fromEntity(entity: CallSessionEntity): CallSession = CallSession(
        callId = entity.callId,
        roomId = entity.roomId,
        conversationId = entity.conversationId,
        initiatorPeerId = entity.initiatorPeerId,
        recipientPeerId = entity.recipientPeerId,
        callType = CallType.valueOf(entity.callType),
        callScope = CallScope.valueOf(entity.callScope),
        targetPeerIds = json.decodeFromString(SetSerializer(String.serializer()), entity.targetPeerIdsJson),
        status = CallState.valueOf(entity.status),
        participants = json.decodeFromString(ListSerializer(CallParticipant.serializer()), entity.participantsJson),
        invitation = entity.invitationJson?.let { json.decodeFromString(CallInvitation.serializer(), it) },
        createdAt = Instant.parse(entity.createdAt),
        updatedAt = Instant.parse(entity.updatedAt),
        lastSignalAt = entity.lastSignalAt?.let(Instant::parse),
        qualitySnapshot = entity.qualitySnapshotJson?.let { json.decodeFromString(MediaQualitySnapshot.serializer(), it) },
        reconnectAttempts = entity.reconnectAttempts,
        metadata = json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), entity.metadataJson),
    )
}

/** Маппер call room. */
object CallRoomEntityMapper {
    /** Конвертирует call room в storage entity. */
    fun toEntity(room: CallRoom): CallRoomEntity = CallRoomEntity(
        roomId = room.roomId,
        conversationId = room.conversationId,
        scope = room.scope.name,
        title = room.title,
        createdByPeerId = room.createdByPeerId,
        participantPeerIdsJson = json.encodeToString(SetSerializer(String.serializer()), room.participantPeerIds),
        activeCallId = room.activeCallId,
        createdAt = room.createdAt.toString(),
        updatedAt = room.updatedAt.toString(),
    )

    /** Конвертирует storage entity в call room. */
    fun fromEntity(entity: CallRoomEntity): CallRoom = CallRoom(
        roomId = entity.roomId,
        conversationId = entity.conversationId,
        scope = CallScope.valueOf(entity.scope),
        title = entity.title,
        createdByPeerId = entity.createdByPeerId,
        participantPeerIds = json.decodeFromString(SetSerializer(String.serializer()), entity.participantPeerIdsJson),
        activeCallId = entity.activeCallId,
        createdAt = Instant.parse(entity.createdAt),
        updatedAt = Instant.parse(entity.updatedAt),
    )
}

/** Маппер состояния участника звонка. */
object CallParticipantEntityMapper {
    /** Конвертирует участника звонка в storage entity. */
    fun toEntity(callId: String, roomId: String, participant: CallParticipant): CallParticipantEntity = CallParticipantEntity(
        callId = callId,
        roomId = roomId,
        peerId = participant.peerId,
        displayName = participant.displayName,
        state = participant.state.name,
        muted = participant.muted,
        videoEnabled = participant.videoEnabled,
        joinedAt = participant.joinedAt?.toString(),
        updatedAt = participant.updatedAt.toString(),
    )

    /** Конвертирует storage entity в участника звонка. */
    fun fromEntity(entity: CallParticipantEntity): CallParticipant = CallParticipant(
        peerId = entity.peerId,
        displayName = entity.displayName,
        state = CallParticipantState.valueOf(entity.state),
        muted = entity.muted,
        videoEnabled = entity.videoEnabled,
        joinedAt = entity.joinedAt?.let(Instant::parse),
        updatedAt = Instant.parse(entity.updatedAt),
    )
}

/** Маппер событий звонка. */
object CallEventEntityMapper {
    /** Конвертирует событие звонка в storage entity. */
    fun toEntity(event: CallEvent): CallEventEntity = CallEventEntity(
        eventId = event.eventId,
        callId = event.callId,
        roomId = event.roomId,
        eventType = event.eventType.name,
        actorPeerId = event.actorPeerId,
        subjectPeerId = event.subjectPeerId,
        state = event.state?.name,
        participantState = event.participantState?.name,
        note = event.note,
        payloadJson = json.encodeToString(MapSerializer(String.serializer(), String.serializer()), event.payload),
        createdAt = event.createdAt.toString(),
    )

    /** Конвертирует storage entity в событие звонка. */
    fun fromEntity(entity: CallEventEntity): CallEvent = CallEvent(
        eventId = entity.eventId,
        callId = entity.callId,
        roomId = entity.roomId,
        eventType = CallEventType.valueOf(entity.eventType),
        actorPeerId = entity.actorPeerId,
        subjectPeerId = entity.subjectPeerId,
        state = entity.state?.let(CallState::valueOf),
        participantState = entity.participantState?.let(CallParticipantState::valueOf),
        note = entity.note,
        payload = json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), entity.payloadJson),
        createdAt = Instant.parse(entity.createdAt),
    )
}
