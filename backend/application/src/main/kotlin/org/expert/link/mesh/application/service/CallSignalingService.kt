package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.factory.PacketEnvelopeFactory
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.call.CallSession
import org.expert.link.mesh.domain.model.call.CallSignal
import org.expert.link.mesh.domain.model.call.CallSignalType
import org.expert.link.mesh.domain.model.call.CallStatus
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.network.CallHangup
import org.expert.link.mesh.domain.model.network.CallInvite
import org.expert.link.mesh.domain.model.network.CallSignalPayload
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.port.repository.CallSessionRepositoryPort

/** Сервис управления сигнальным жизненным циклом звонка. */
class CallSignalingService(
    private val localProfileService: LocalProfileService,
    private val peerTrustVerificationService: PeerTrustVerificationService,
    private val messageEncryptionService: MessageEncryptionService,
    private val packetEnvelopeFactory: PacketEnvelopeFactory,
    private val packetSignatureService: PacketSignatureService,
    private val deliveryTrackingService: DeliveryTrackingService,
    private val callSessionRepositoryPort: CallSessionRepositoryPort,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
) {
    /**
     * Starts an outbound call invitation.
     */
    suspend fun invite(recipientPeerId: String, conversationId: String? = null, offer: String): CallSession {
        val localProfile = localProfileService.require()
        val trustedPeer = requireNotNull(peerTrustVerificationService.requireTrusted(recipientPeerId)) {
            "Peer $recipientPeerId is not trusted"
        }
        val session = CallSession(
            callId = newId("call"),
            conversationId = conversationId,
            initiatorPeerId = localProfile.peerId,
            recipientPeerId = recipientPeerId,
            status = CallStatus.INVITED,
            createdAt = now(),
            updatedAt = now(),
        )
        callSessionRepositoryPort.save(session)
        val payload = CallInvite(session.callId, conversationId, localProfile.peerId, recipientPeerId, offer, now())
        sendPacket(recipientPeerId, trustedPeer.peerIdentity.publicKey, PacketType.CALL_INVITE, payload, conversationId)
        return session
    }

    /**
     * Handles an inbound call invite.
     */
    suspend fun handleInvite(payload: CallInvite): CallSession {
        val session = CallSession(
            callId = payload.callId,
            conversationId = payload.conversationId,
            initiatorPeerId = payload.senderPeerId,
            recipientPeerId = payload.recipientPeerId,
            status = CallStatus.RINGING,
            createdAt = payload.createdAt,
            updatedAt = now(),
            lastSignalAt = now(),
        )
        callSessionRepositoryPort.save(session)
        nodeMetricsService.increment("call.invite.received")
        return session
    }

    /**
     * Sends an arbitrary signaling payload for an existing call.
     */
    suspend fun sendSignal(callId: String, recipientPeerId: String, signalType: CallSignalType, payload: String): CallSignal {
        val localProfile = localProfileService.require()
        val trustedPeer = requireNotNull(peerTrustVerificationService.requireTrusted(recipientPeerId)) {
            "Peer $recipientPeerId is not trusted"
        }
        val signal = CallSignal(callId, signalType, localProfile.peerId, recipientPeerId, payload, now())
        sendPacket(recipientPeerId, trustedPeer.peerIdentity.publicKey, PacketType.CALL_SIGNAL, CallSignalPayload(signal))
        callSessionRepositoryPort.findByCallId(callId)?.let {
            callSessionRepositoryPort.save(it.copy(status = deriveStatus(signalType, it.status), updatedAt = now(), lastSignalAt = now()))
        }
        return signal
    }

    /**
     * Applies an inbound signaling payload to the local call session state.
     */
    suspend fun handleSignal(payload: CallSignalPayload): CallSession? {
        val session = callSessionRepositoryPort.findByCallId(payload.signal.callId) ?: return null
        return callSessionRepositoryPort.save(
            session.copy(
                status = deriveStatus(payload.signal.signalType, session.status),
                updatedAt = now(),
                lastSignalAt = now(),
            ),
        )
    }

    /**
     * Terminates a call session and notifies the remote peer.
     */
    suspend fun hangup(callId: String, recipientPeerId: String, reason: String): CallSession? {
        val localProfile = localProfileService.require()
        val trustedPeer = requireNotNull(peerTrustVerificationService.requireTrusted(recipientPeerId)) {
            "Peer $recipientPeerId is not trusted"
        }
        val session = callSessionRepositoryPort.findByCallId(callId) ?: return null
        sendPacket(recipientPeerId, trustedPeer.peerIdentity.publicKey, PacketType.CALL_HANGUP, CallHangup(callId, localProfile.peerId, recipientPeerId, reason, now()))
        return callSessionRepositoryPort.save(session.copy(status = CallStatus.ENDED, updatedAt = now(), lastSignalAt = now()))
    }

    /**
     * Handles an inbound call hangup.
     */
    suspend fun handleHangup(payload: CallHangup): CallSession? {
        val session = callSessionRepositoryPort.findByCallId(payload.callId) ?: return null
        eventLogService.log(
            category = EventCategory.CALL,
            level = EventLevel.INFO,
            message = "Call ended",
            peerId = payload.senderPeerId,
        )
        return callSessionRepositoryPort.save(session.copy(status = CallStatus.ENDED, updatedAt = now(), lastSignalAt = now()))
    }

    private suspend fun sendPacket(
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
        nodeMetricsService.increment("call.signal.sent")
    }

    private fun deriveStatus(signalType: CallSignalType, currentStatus: CallStatus): CallStatus = when (signalType) {
        CallSignalType.RINGING -> CallStatus.RINGING
        CallSignalType.ACCEPTED,
        CallSignalType.SDP_ANSWER,
        -> CallStatus.ACTIVE
        CallSignalType.REJECTED -> CallStatus.REJECTED
        else -> currentStatus
    }
}
