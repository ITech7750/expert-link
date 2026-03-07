package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.factory.PacketEnvelopeFactory
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.call.CallEvent
import org.expert.link.mesh.domain.model.call.CallEventType
import org.expert.link.mesh.domain.model.call.CallInvitation
import org.expert.link.mesh.domain.model.call.CallParticipant
import org.expert.link.mesh.domain.model.call.CallParticipantState
import org.expert.link.mesh.domain.model.call.CallRoom
import org.expert.link.mesh.domain.model.call.CallScope
import org.expert.link.mesh.domain.model.call.CallSession
import org.expert.link.mesh.domain.model.call.CallSignal
import org.expert.link.mesh.domain.model.call.CallSignalType
import org.expert.link.mesh.domain.model.call.CallState
import org.expert.link.mesh.domain.model.call.CallType
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.network.CallHangup
import org.expert.link.mesh.domain.model.network.CallInvite
import org.expert.link.mesh.domain.model.network.CallSignalPayload
import org.expert.link.mesh.domain.model.network.PacketPayload
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.port.repository.CallEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallParticipantRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallRoomRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallSessionRepositoryPort

/**
 * Сервис call signaling v2.
 *
 * Отвечает за lifecycle звонка, состояние участников и событийную историю.
 * Транспорт сигналов остаётся через packet-plane, а media-подсистема подключается отдельно.
 */
class CallSignalingService(
    private val localProfileService: LocalProfileService,
    private val peerTrustVerificationService: PeerTrustVerificationService,
    private val messageEncryptionService: MessageEncryptionService,
    private val packetEnvelopeFactory: PacketEnvelopeFactory,
    private val packetSignatureService: PacketSignatureService,
    private val deliveryTrackingService: DeliveryTrackingService,
    private val callSessionRepositoryPort: CallSessionRepositoryPort,
    private val callRoomRepositoryPort: CallRoomRepositoryPort,
    private val callParticipantRepositoryPort: CallParticipantRepositoryPort,
    private val callEventRepositoryPort: CallEventRepositoryPort,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
) {
    private fun trace(message: String) {
        println("ExpertLinkCall/CallSignalingService $message")
    }

    /** Запускает исходящий 1:1 звонок. */
    suspend fun startDirectCall(
        recipientPeerId: String,
        conversationId: String? = null,
        offer: String,
        callType: CallType,
    ): CallSession {
        trace("startDirectCall peer=$recipientPeerId conversation=$conversationId type=$callType offerLength=${offer.length}")
        val local = localProfileService.require()
        val trustedPeer = requireNotNull(peerTrustVerificationService.requireTrusted(recipientPeerId)) {
            "Peer $recipientPeerId is not trusted"
        }
        val startedAt = now()
        val callId = newId("call")
        val roomId = callId
        val invitation = CallInvitation(
            callId = callId,
            roomId = roomId,
            conversationId = conversationId,
            initiatorPeerId = local.peerId,
            targetPeerIds = setOf(recipientPeerId),
            callType = callType,
            callScope = CallScope.DIRECT,
            offer = offer,
            createdAt = startedAt,
        )
        val participants = listOf(
            CallParticipant(
                peerId = local.peerId,
                displayName = local.displayName,
                state = CallParticipantState.CONNECTED,
                muted = false,
                videoEnabled = true,
                joinedAt = startedAt,
                updatedAt = startedAt,
            ),
            CallParticipant(
                peerId = recipientPeerId,
                displayName = trustedPeer.peerIdentity.displayName,
                state = CallParticipantState.INVITED,
                muted = false,
                videoEnabled = true,
                joinedAt = null,
                updatedAt = startedAt,
            ),
        )
        val session = CallSession(
            callId = callId,
            roomId = roomId,
            conversationId = conversationId,
            initiatorPeerId = local.peerId,
            recipientPeerId = recipientPeerId,
            callType = callType,
            callScope = CallScope.DIRECT,
            targetPeerIds = setOf(recipientPeerId),
            status = CallState.OUTGOING,
            participants = participants,
            invitation = invitation,
            createdAt = startedAt,
            updatedAt = startedAt,
            lastSignalAt = startedAt,
        )
        persistSession(session)
        persistRoom(
            CallRoom(
                roomId = roomId,
                conversationId = conversationId,
                scope = CallScope.DIRECT,
                title = null,
                createdByPeerId = local.peerId,
                participantPeerIds = setOf(local.peerId, recipientPeerId),
                activeCallId = callId,
                createdAt = startedAt,
                updatedAt = startedAt,
            ),
        )
        appendEvent(
            callId = callId,
            roomId = roomId,
            eventType = CallEventType.INVITED,
            actorPeerId = local.peerId,
            subjectPeerId = recipientPeerId,
            state = CallState.OUTGOING,
            note = "Outbound direct ${callType.name.lowercase()} call started",
        )
        val payload = CallInvite(
            callId = callId,
            roomId = roomId,
            conversationId = conversationId,
            senderPeerId = local.peerId,
            recipientPeerId = recipientPeerId,
            targetPeerIds = setOf(recipientPeerId),
            callType = callType,
            callScope = CallScope.DIRECT,
            offer = offer,
            createdAt = startedAt,
        )
        sendPacket(
            targetPeerId = recipientPeerId,
            targetPublicKey = trustedPeer.peerIdentity.publicKey,
            packetType = PacketType.CALL_INVITE,
            payload = payload,
            conversationId = conversationId,
        )
        trace("startDirectCall sent invite call=$callId recipient=$recipientPeerId")
        nodeMetricsService.increment("call.outbound.direct")
        return session
    }

    /** Запускает исходящий групповой звонок. */
    suspend fun startGroupCall(
        targetPeerIds: Set<String>,
        conversationId: String? = null,
        offer: String,
        callType: CallType,
        roomTitle: String? = null,
    ): CallSession {
        require(targetPeerIds.isNotEmpty()) { "Group call requires at least one target peer" }
        val local = localProfileService.require()
        val startedAt = now()
        val callId = newId("call")
        val roomId = newId("room")
        val trustedPeers = targetPeerIds.map { target ->
            target to requireNotNull(peerTrustVerificationService.requireTrusted(target)) { "Peer $target is not trusted" }
        }.toMap()
        val participants = buildList {
            add(
                CallParticipant(
                    peerId = local.peerId,
                    displayName = local.displayName,
                    state = CallParticipantState.CONNECTED,
                    muted = false,
                    videoEnabled = true,
                    joinedAt = startedAt,
                    updatedAt = startedAt,
                ),
            )
            targetPeerIds.forEach { target ->
                add(
                    CallParticipant(
                        peerId = target,
                        displayName = trustedPeers.getValue(target).peerIdentity.displayName,
                        state = CallParticipantState.INVITED,
                        muted = false,
                        videoEnabled = true,
                        joinedAt = null,
                        updatedAt = startedAt,
                    ),
                )
            }
        }
        val invitation = CallInvitation(
            callId = callId,
            roomId = roomId,
            conversationId = conversationId,
            initiatorPeerId = local.peerId,
            targetPeerIds = targetPeerIds,
            callType = callType,
            callScope = CallScope.GROUP,
            offer = offer,
            createdAt = startedAt,
        )
        val session = CallSession(
            callId = callId,
            roomId = roomId,
            conversationId = conversationId,
            initiatorPeerId = local.peerId,
            recipientPeerId = targetPeerIds.first(),
            callType = callType,
            callScope = CallScope.GROUP,
            targetPeerIds = targetPeerIds,
            status = CallState.OUTGOING,
            participants = participants,
            invitation = invitation,
            createdAt = startedAt,
            updatedAt = startedAt,
            lastSignalAt = startedAt,
            metadata = roomTitle?.let { mapOf("roomTitle" to it) } ?: emptyMap(),
        )
        persistSession(session)
        persistRoom(
            CallRoom(
                roomId = roomId,
                conversationId = conversationId,
                scope = CallScope.GROUP,
                title = roomTitle,
                createdByPeerId = local.peerId,
                participantPeerIds = targetPeerIds + local.peerId,
                activeCallId = callId,
                createdAt = startedAt,
                updatedAt = startedAt,
            ),
        )
        targetPeerIds.forEach { target ->
            val trusted = trustedPeers.getValue(target)
            sendPacket(
                targetPeerId = target,
                targetPublicKey = trusted.peerIdentity.publicKey,
                packetType = PacketType.CALL_INVITE,
                payload = CallInvite(
                    callId = callId,
                    roomId = roomId,
                    conversationId = conversationId,
                    senderPeerId = local.peerId,
                    recipientPeerId = target,
                    targetPeerIds = targetPeerIds,
                    callType = callType,
                    callScope = CallScope.GROUP,
                    offer = offer,
                    createdAt = startedAt,
                ),
                conversationId = conversationId,
            )
            appendEvent(
                callId = callId,
                roomId = roomId,
                eventType = CallEventType.INVITED,
                actorPeerId = local.peerId,
                subjectPeerId = target,
                state = CallState.OUTGOING,
                note = "Outbound group ${callType.name.lowercase()} call invite sent",
            )
        }
        nodeMetricsService.increment("call.outbound.group")
        return session
    }

    /** Обрабатывает входящий invite. */
    suspend fun handleInvite(payload: CallInvite): CallSession {
        trace(
            "handleInvite call=${payload.callId} sender=${payload.senderPeerId} recipient=${payload.recipientPeerId} " +
                "targets=${payload.targetPeerIds} type=${payload.callType} offerLength=${payload.offer.length}",
        )
        val local = localProfileService.require()
        val trustedSender = peerTrustVerificationService.requireTrusted(payload.senderPeerId)
        val session = callSessionRepositoryPort.findByCallId(payload.callId)
        val now = now()
        val updated = session?.copy(
            status = CallState.INCOMING,
            updatedAt = now,
            lastSignalAt = now,
            callType = payload.callType,
            callScope = payload.callScope,
            targetPeerIds = payload.targetPeerIds,
            invitation = CallInvitation(
                callId = payload.callId,
                roomId = payload.roomId,
                conversationId = payload.conversationId,
                initiatorPeerId = payload.senderPeerId,
                targetPeerIds = payload.targetPeerIds,
                callType = payload.callType,
                callScope = payload.callScope,
                offer = payload.offer,
                createdAt = payload.createdAt,
            ),
        ) ?: CallSession(
            callId = payload.callId,
            roomId = payload.roomId,
            conversationId = payload.conversationId,
            initiatorPeerId = payload.senderPeerId,
            recipientPeerId = local.peerId,
            callType = payload.callType,
            callScope = payload.callScope,
            targetPeerIds = payload.targetPeerIds,
            status = CallState.INCOMING,
            participants = listOf(
                CallParticipant(
                    peerId = payload.senderPeerId,
                    displayName = trustedSender?.peerIdentity?.displayName ?: payload.senderPeerId,
                    state = CallParticipantState.RINGING,
                    muted = false,
                    videoEnabled = true,
                    joinedAt = null,
                    updatedAt = now,
                ),
                CallParticipant(
                    peerId = local.peerId,
                    displayName = local.displayName,
                    state = CallParticipantState.RINGING,
                    muted = false,
                    videoEnabled = true,
                    joinedAt = null,
                    updatedAt = now,
                ),
            ),
            invitation = CallInvitation(
                callId = payload.callId,
                roomId = payload.roomId,
                conversationId = payload.conversationId,
                initiatorPeerId = payload.senderPeerId,
                targetPeerIds = payload.targetPeerIds,
                callType = payload.callType,
                callScope = payload.callScope,
                offer = payload.offer,
                createdAt = payload.createdAt,
            ),
            createdAt = payload.createdAt,
            updatedAt = now,
            lastSignalAt = now,
        )
        persistSession(updated)
        val room = callRoomRepositoryPort.findByRoomId(payload.roomId)
        if (room == null) {
            persistRoom(
                CallRoom(
                    roomId = payload.roomId,
                    conversationId = payload.conversationId,
                    scope = payload.callScope,
                    title = null,
                    createdByPeerId = payload.senderPeerId,
                    participantPeerIds = payload.targetPeerIds + payload.senderPeerId + local.peerId,
                    activeCallId = payload.callId,
                    createdAt = payload.createdAt,
                    updatedAt = now,
                ),
            )
        }
        appendEvent(
            callId = payload.callId,
            roomId = payload.roomId,
            eventType = CallEventType.INVITED,
            actorPeerId = payload.senderPeerId,
            subjectPeerId = local.peerId,
            state = CallState.INCOMING,
            note = "Incoming ${payload.callType.name.lowercase()} call invite",
        )
        nodeMetricsService.increment("call.invite.received")
        trace("handleInvite stored session call=${payload.callId} status=${updated.status}")
        return updated
    }

    /** Принимает звонок и отправляет `ACCEPT` + SDP answer. */
    suspend fun accept(callId: String, recipientPeerId: String, answer: String): CallSignal {
        trace("accept call=$callId recipient=$recipientPeerId answerLength=${answer.length}")
        val signal = sendSignalInternal(
            callId = callId,
            recipientPeerId = recipientPeerId,
            signalType = CallSignalType.ACCEPT,
            payload = answer,
            participantState = CallParticipantState.JOINING,
        )
        transitionAndPersist(callId, CallSignalType.ACCEPT, signal.senderPeerId)
        return signal
    }

    /** Отклоняет звонок. */
    suspend fun reject(callId: String, recipientPeerId: String, reason: String): CallSignal {
        val signal = sendSignalInternal(
            callId = callId,
            recipientPeerId = recipientPeerId,
            signalType = CallSignalType.REJECT,
            payload = reason,
            participantState = CallParticipantState.DECLINED,
        )
        transitionAndPersist(callId, CallSignalType.REJECT, signal.senderPeerId)
        return signal
    }

    /** Подключается к групповому звонку. */
    suspend fun join(callId: String, recipientPeerId: String, answer: String): CallSignal {
        val signal = sendSignalInternal(
            callId = callId,
            recipientPeerId = recipientPeerId,
            signalType = CallSignalType.JOIN,
            payload = answer,
            participantState = CallParticipantState.JOINING,
        )
        transitionAndPersist(callId, CallSignalType.JOIN, signal.senderPeerId)
        return signal
    }

    /** Выходит из звонка. */
    suspend fun leave(callId: String, recipientPeerId: String, reason: String): CallSignal {
        val signal = sendSignalInternal(
            callId = callId,
            recipientPeerId = recipientPeerId,
            signalType = CallSignalType.LEAVE,
            payload = reason,
            participantState = CallParticipantState.LEFT,
        )
        transitionAndPersist(callId, CallSignalType.LEAVE, signal.senderPeerId)
        return signal
    }

    /**
     * Legacy API: старт исходящего звонка.
     * Используется для обратной совместимости.
     */
    suspend fun invite(recipientPeerId: String, conversationId: String? = null, offer: String): CallSession {
        return startDirectCall(
            recipientPeerId = recipientPeerId,
            conversationId = conversationId,
            offer = offer,
            callType = CallType.AUDIO,
        )
    }

    /**
     * Legacy API: отправка произвольного сигнала.
     * Используется для обратной совместимости.
     */
    suspend fun sendSignal(
        callId: String,
        recipientPeerId: String,
        signalType: CallSignalType,
        payload: String,
        muted: Boolean? = null,
        videoEnabled: Boolean? = null,
    ): CallSignal {
        val signal = sendSignalInternal(
            callId = callId,
            recipientPeerId = recipientPeerId,
            signalType = signalType,
            payload = payload,
            muted = muted,
            videoEnabled = videoEnabled,
        )
        transitionAndPersist(callId, signalType, signal.senderPeerId, muted, videoEnabled)
        return signal
    }

    /** Обрабатывает входящий сигнальный пакет. */
    suspend fun handleSignal(payload: CallSignalPayload): CallSession? {
        val signal = payload.signal
        trace(
            "handleSignal call=${signal.callId} type=${signal.signalType} sender=${signal.senderPeerId} " +
                "recipient=${signal.recipientPeerId} payloadLength=${signal.payload.length}",
        )
        val session = callSessionRepositoryPort.findByCallId(signal.callId) ?: return null
        val current = now()
        val nextState = deriveState(signal.signalType, session.status)
        val updatedParticipants = updateParticipant(
            participants = session.participants,
            peerId = signal.senderPeerId,
            participantState = signal.participantState ?: participantStateFromSignal(signal.signalType),
            muted = signal.muted,
            videoEnabled = signal.videoEnabled,
        )
        val updated = session.copy(
            status = nextState,
            updatedAt = current,
            lastSignalAt = current,
            participants = updatedParticipants,
            callType = signal.callType ?: session.callType,
            callScope = signal.callScope ?: session.callScope,
            reconnectAttempts = if (nextState == CallState.RECONNECTING) session.reconnectAttempts + 1 else session.reconnectAttempts,
        )
        persistSession(updated)
        appendEvent(
            callId = signal.callId,
            roomId = updated.roomId,
            eventType = eventTypeFromSignal(signal.signalType),
            actorPeerId = signal.senderPeerId,
            subjectPeerId = signal.recipientPeerId,
            state = nextState,
            participantState = signal.participantState,
            note = "Inbound call signal ${signal.signalType}",
        )
        nodeMetricsService.increment("call.signal.received")
        return updated
    }

    /** Завершает звонок для локального узла. */
    suspend fun end(callId: String, reason: String): CallSession? {
        val session = callSessionRepositoryPort.findByCallId(callId) ?: return null
        val localPeerId = localProfileService.require().peerId
        val peersToNotify = session.targetPeerIds.filter { it != localPeerId }
        peersToNotify.forEach { peerId ->
            val trustedPeer = peerTrustVerificationService.requireTrusted(peerId) ?: return@forEach
            sendPacket(
                targetPeerId = peerId,
                targetPublicKey = trustedPeer.peerIdentity.publicKey,
                packetType = PacketType.CALL_HANGUP,
                payload = CallHangup(
                    callId = callId,
                    roomId = session.roomId,
                    senderPeerId = localPeerId,
                    recipientPeerId = peerId,
                    reason = reason,
                    createdAt = now(),
                ),
                conversationId = session.conversationId,
            )
        }
        val updated = session.copy(
            status = CallState.ENDED,
            updatedAt = now(),
            lastSignalAt = now(),
            participants = updateParticipant(
                participants = session.participants,
                peerId = localPeerId,
                participantState = CallParticipantState.LEFT,
                muted = null,
                videoEnabled = null,
            ),
        )
        persistSession(updated)
        appendEvent(
            callId = updated.callId,
            roomId = updated.roomId,
            eventType = CallEventType.ENDED,
            actorPeerId = localPeerId,
            state = CallState.ENDED,
            note = reason,
        )
        nodeMetricsService.increment("call.ended")
        return updated
    }

    /**
     * Legacy API: завершение звонка для указанного peer.
     * Используется для обратной совместимости.
     */
    suspend fun hangup(callId: String, recipientPeerId: String, reason: String): CallSession? {
        val session = callSessionRepositoryPort.findByCallId(callId) ?: return null
        val local = localProfileService.require()
        val trustedPeer = requireNotNull(peerTrustVerificationService.requireTrusted(recipientPeerId)) {
            "Peer $recipientPeerId is not trusted"
        }
        sendPacket(
            targetPeerId = recipientPeerId,
            targetPublicKey = trustedPeer.peerIdentity.publicKey,
            packetType = PacketType.CALL_HANGUP,
            payload = CallHangup(
                callId = callId,
                roomId = session.roomId,
                senderPeerId = local.peerId,
                recipientPeerId = recipientPeerId,
                reason = reason,
                createdAt = now(),
            ),
            conversationId = session.conversationId,
        )
        return end(callId, reason)
    }

    /** Обрабатывает входящий hangup. */
    suspend fun handleHangup(payload: CallHangup): CallSession? {
        val session = callSessionRepositoryPort.findByCallId(payload.callId) ?: return null
        val updated = session.copy(
            status = CallState.ENDED,
            updatedAt = now(),
            lastSignalAt = now(),
            participants = updateParticipant(
                participants = session.participants,
                peerId = payload.senderPeerId,
                participantState = CallParticipantState.LEFT,
                muted = null,
                videoEnabled = null,
            ),
        )
        persistSession(updated)
        appendEvent(
            callId = payload.callId,
            roomId = updated.roomId,
            eventType = CallEventType.ENDED,
            actorPeerId = payload.senderPeerId,
            subjectPeerId = payload.recipientPeerId,
            state = CallState.ENDED,
            note = payload.reason,
        )
        eventLogService.log(
            category = EventCategory.CALL,
            level = EventLevel.INFO,
            message = "Call ended",
            peerId = payload.senderPeerId,
        )
        return updated
    }

    /** Возвращает участников звонка. */
    suspend fun participants(callId: String): List<CallParticipant> = callParticipantRepositoryPort.listByCallId(callId)

    /** Возвращает историю событий звонка. */
    suspend fun events(callId: String, limit: Int = 200): List<CallEvent> = callEventRepositoryPort.listByCallId(callId, limit)

    /** Возвращает активные сессии. */
    suspend fun activeSessions(): List<CallSession> = callSessionRepositoryPort.list().filter { it.status in ACTIVE_STATES }

    /** Возвращает входящие сессии. */
    suspend fun incomingSessions(): List<CallSession> = callSessionRepositoryPort.list().filter { it.status in INCOMING_STATES }

    /** Возвращает call-сессию по id. */
    suspend fun session(callId: String): CallSession? = callSessionRepositoryPort.findByCallId(callId)

    private suspend fun sendSignalInternal(
        callId: String,
        recipientPeerId: String,
        signalType: CallSignalType,
        payload: String,
        participantState: CallParticipantState? = null,
        muted: Boolean? = null,
        videoEnabled: Boolean? = null,
    ): CallSignal {
        trace(
            "sendSignalInternal call=$callId type=$signalType recipient=$recipientPeerId " +
                "payloadLength=${payload.length} participantState=$participantState muted=$muted videoEnabled=$videoEnabled",
        )
        val local = localProfileService.require()
        val trustedPeer = requireNotNull(peerTrustVerificationService.requireTrusted(recipientPeerId)) {
            "Peer $recipientPeerId is not trusted"
        }
        val session = callSessionRepositoryPort.findByCallId(callId)
        val signal = CallSignal(
            callId = callId,
            roomId = session?.roomId,
            signalType = signalType,
            senderPeerId = local.peerId,
            recipientPeerId = recipientPeerId,
            callType = session?.callType,
            callScope = session?.callScope,
            participantState = participantState,
            muted = muted,
            videoEnabled = videoEnabled,
            correlationId = newId("signal"),
            payload = payload,
            createdAt = now(),
        )
        sendPacket(
            targetPeerId = recipientPeerId,
            targetPublicKey = trustedPeer.peerIdentity.publicKey,
            packetType = PacketType.CALL_SIGNAL,
            payload = CallSignalPayload(signal),
            conversationId = session?.conversationId,
        )
        nodeMetricsService.increment("call.signal.sent")
        return signal
    }

    private suspend fun transitionAndPersist(
        callId: String,
        signalType: CallSignalType,
        actorPeerId: String,
        muted: Boolean? = null,
        videoEnabled: Boolean? = null,
    ) {
        val session = callSessionRepositoryPort.findByCallId(callId) ?: return
        val nextState = deriveState(signalType, session.status)
        val updated = session.copy(
            status = nextState,
            updatedAt = now(),
            lastSignalAt = now(),
            participants = updateParticipant(
                participants = session.participants,
                peerId = actorPeerId,
                participantState = participantStateFromSignal(signalType),
                muted = muted,
                videoEnabled = videoEnabled,
            ),
            reconnectAttempts = if (nextState == CallState.RECONNECTING) session.reconnectAttempts + 1 else session.reconnectAttempts,
        )
        persistSession(updated)
        appendEvent(
            callId = callId,
            roomId = updated.roomId,
            eventType = eventTypeFromSignal(signalType),
            actorPeerId = actorPeerId,
            state = nextState,
            participantState = participantStateFromSignal(signalType),
            note = "Local call signal $signalType",
        )
    }

    private suspend fun persistSession(session: CallSession): CallSession {
        callSessionRepositoryPort.save(session)
        callParticipantRepositoryPort.replace(session.callId, session.participants)
        return session
    }

    private suspend fun persistRoom(room: CallRoom) {
        callRoomRepositoryPort.save(room)
    }

    private suspend fun appendEvent(
        callId: String,
        roomId: String,
        eventType: CallEventType,
        actorPeerId: String,
        subjectPeerId: String? = null,
        state: CallState? = null,
        participantState: CallParticipantState? = null,
        note: String? = null,
    ) {
        callEventRepositoryPort.append(
            CallEvent(
                eventId = newId("call-event"),
                callId = callId,
                roomId = roomId,
                eventType = eventType,
                actorPeerId = actorPeerId,
                subjectPeerId = subjectPeerId,
                state = state,
                participantState = participantState,
                note = note,
                createdAt = now(),
            ),
        )
    }

    private suspend fun sendPacket(
        targetPeerId: String,
        targetPublicKey: String,
        packetType: PacketType,
        payload: PacketPayload,
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
        val result = deliveryTrackingService.send(signed)
        require(result.success) {
            "Failed to send $packetType to $targetPeerId: ${result.errorMessage ?: "unknown delivery error"}"
        }
    }

    private fun deriveState(signalType: CallSignalType, current: CallState): CallState = when (signalType) {
        CallSignalType.INVITE -> CallState.INCOMING
        CallSignalType.RINGING -> CallState.RINGING
        CallSignalType.ACCEPT,
        CallSignalType.ACCEPTED,
        -> when (current) {
            CallState.OUTGOING, CallState.INCOMING, CallState.RINGING -> CallState.ACCEPTED
            else -> current
        }
        CallSignalType.SDP_ANSWER,
        CallSignalType.JOIN,
        -> when (current) {
            CallState.ACCEPTED, CallState.CONNECTING, CallState.OUTGOING, CallState.INCOMING, CallState.RINGING -> CallState.CONNECTING
            else -> current
        }
        CallSignalType.SDP_OFFER,
        CallSignalType.ICE_CANDIDATE,
        -> when (current) {
            CallState.ACCEPTED, CallState.CONNECTING, CallState.OUTGOING, CallState.INCOMING, CallState.RINGING -> CallState.CONNECTING
            else -> current
        }
        CallSignalType.PARTICIPANT_STATE,
        CallSignalType.QUALITY,
        CallSignalType.MUTE_CHANGED,
        CallSignalType.VIDEO_CHANGED,
        -> if (current == CallState.CONNECTING) CallState.CONNECTED else current
        CallSignalType.RECONNECTING -> CallState.RECONNECTING
        CallSignalType.RECONNECTED -> CallState.CONNECTED
        CallSignalType.REJECT,
        CallSignalType.REJECTED,
        -> CallState.REJECTED
        CallSignalType.LEAVE -> CallState.LEFT
        CallSignalType.HANGUP -> CallState.ENDED
    }

    private fun participantStateFromSignal(signalType: CallSignalType): CallParticipantState? = when (signalType) {
        CallSignalType.INVITE -> CallParticipantState.INVITED
        CallSignalType.RINGING -> CallParticipantState.RINGING
        CallSignalType.ACCEPT,
        CallSignalType.ACCEPTED,
        CallSignalType.SDP_OFFER,
        CallSignalType.SDP_ANSWER,
        CallSignalType.ICE_CANDIDATE,
        CallSignalType.JOIN,
        -> CallParticipantState.JOINING
        CallSignalType.RECONNECTING -> CallParticipantState.RECONNECTING
        CallSignalType.RECONNECTED -> CallParticipantState.CONNECTED
        CallSignalType.LEAVE,
        CallSignalType.HANGUP,
        -> CallParticipantState.LEFT
        CallSignalType.REJECT,
        CallSignalType.REJECTED,
        -> CallParticipantState.DECLINED
        CallSignalType.QUALITY,
        CallSignalType.MUTE_CHANGED,
        CallSignalType.VIDEO_CHANGED,
        CallSignalType.PARTICIPANT_STATE,
        -> null
    }

    private fun eventTypeFromSignal(signalType: CallSignalType): CallEventType = when (signalType) {
        CallSignalType.INVITE -> CallEventType.INVITED
        CallSignalType.ACCEPT, CallSignalType.ACCEPTED -> CallEventType.ACCEPTED
        CallSignalType.REJECT, CallSignalType.REJECTED -> CallEventType.REJECTED
        CallSignalType.JOIN -> CallEventType.JOINED
        CallSignalType.LEAVE -> CallEventType.LEFT
        CallSignalType.RECONNECTING -> CallEventType.RECONNECTING
        CallSignalType.RECONNECTED -> CallEventType.RECONNECTED
        CallSignalType.HANGUP -> CallEventType.ENDED
        CallSignalType.QUALITY -> CallEventType.QUALITY_UPDATED
        CallSignalType.PARTICIPANT_STATE,
        CallSignalType.MUTE_CHANGED,
        CallSignalType.VIDEO_CHANGED,
        -> CallEventType.PARTICIPANT_UPDATED
        CallSignalType.SDP_OFFER,
        CallSignalType.SDP_ANSWER,
        CallSignalType.ICE_CANDIDATE,
        CallSignalType.RINGING,
        -> CallEventType.STATE_CHANGED
    }

    private fun updateParticipant(
        participants: List<CallParticipant>,
        peerId: String,
        participantState: CallParticipantState?,
        muted: Boolean?,
        videoEnabled: Boolean?,
    ): List<CallParticipant> {
        if (participants.isEmpty()) return participants
        var found = false
        val updated = participants.map { participant ->
            if (participant.peerId != peerId) {
                participant
            } else {
                found = true
                participant.copy(
                    state = participantState ?: participant.state,
                    muted = muted ?: participant.muted,
                    videoEnabled = videoEnabled ?: participant.videoEnabled,
                    joinedAt = if (participantState == CallParticipantState.CONNECTED && participant.joinedAt == null) now() else participant.joinedAt,
                    updatedAt = now(),
                )
            }
        }
        return if (found) updated else {
            updated + CallParticipant(
                peerId = peerId,
                displayName = peerId,
                state = participantState ?: CallParticipantState.JOINING,
                muted = muted ?: false,
                videoEnabled = videoEnabled ?: true,
                joinedAt = null,
                updatedAt = now(),
            )
        }
    }

    private companion object {
        val ACTIVE_STATES = setOf(
            CallState.OUTGOING,
            CallState.INCOMING,
            CallState.RINGING,
            CallState.ACCEPTED,
            CallState.CONNECTING,
            CallState.ACTIVE,
            CallState.CONNECTED,
            CallState.RECONNECTING,
        )
        val INCOMING_STATES = setOf(CallState.INCOMING, CallState.RINGING)
    }
}
