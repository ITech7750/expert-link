package org.expert.link.mesh.domain.entity

import kotlinx.serialization.Serializable

/** Сущность хранения call session. */
@Serializable
data class CallSessionEntity(
    val callId: String,
    val roomId: String,
    val conversationId: String? = null,
    val initiatorPeerId: String,
    val recipientPeerId: String,
    val callType: String,
    val callScope: String,
    val targetPeerIdsJson: String,
    val status: String,
    val participantsJson: String,
    val invitationJson: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val lastSignalAt: String? = null,
    val qualitySnapshotJson: String? = null,
    val reconnectAttempts: Int = 0,
    val metadataJson: String,
)

/** Сущность хранения комнаты звонка. */
@Serializable
data class CallRoomEntity(
    val roomId: String,
    val conversationId: String? = null,
    val scope: String,
    val title: String? = null,
    val createdByPeerId: String,
    val participantPeerIdsJson: String,
    val activeCallId: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

/** Сущность хранения состояния участника звонка. */
@Serializable
data class CallParticipantEntity(
    val callId: String,
    val roomId: String,
    val peerId: String,
    val displayName: String,
    val state: String,
    val muted: Boolean,
    val videoEnabled: Boolean,
    val joinedAt: String? = null,
    val updatedAt: String,
)

/** Сущность хранения события звонка. */
@Serializable
data class CallEventEntity(
    val eventId: String,
    val callId: String,
    val roomId: String,
    val eventType: String,
    val actorPeerId: String,
    val subjectPeerId: String? = null,
    val state: String? = null,
    val participantState: String? = null,
    val note: String? = null,
    val payloadJson: String,
    val createdAt: String,
)
