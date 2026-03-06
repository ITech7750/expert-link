package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.factory.PacketEnvelopeFactory
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.messaging.ChatMessage
import org.expert.link.mesh.domain.model.messaging.Conversation
import org.expert.link.mesh.domain.model.messaging.MessageDeliveryStatus
import org.expert.link.mesh.domain.model.messaging.MessageReceipt
import org.expert.link.mesh.domain.model.messaging.OutgoingMessage
import org.expert.link.mesh.domain.model.network.ChatMessagePayload
import org.expert.link.mesh.domain.model.network.DeliveryAck
import org.expert.link.mesh.domain.model.network.DeliveryAckPayload
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.port.repository.ConversationRepositoryPort
import org.expert.link.mesh.domain.port.repository.MessageRepositoryPort
import java.util.ArrayDeque
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Сервис зашифрованного чата.
 *
 * Отвечает за отправку и приём сообщений, сохранение диалогов и обработку ACK.
 */
class ChatMessagingService(
    private val localProfileService: LocalProfileService,
    private val conversationRepositoryPort: ConversationRepositoryPort,
    private val messageRepositoryPort: MessageRepositoryPort,
    private val peerTrustVerificationService: PeerTrustVerificationService,
    private val blockListService: BlockListService,
    private val messageEncryptionService: MessageEncryptionService,
    private val packetEnvelopeFactory: PacketEnvelopeFactory,
    private val packetSignatureService: PacketSignatureService,
    private val deliveryTrackingService: DeliveryTrackingService,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
) {
    private val receiptsMutex = Mutex()
    private val receipts = ArrayDeque<MessageReceipt>()

    /** Открывает существующий или создаёт новый диалог. */
    suspend fun openConversation(targetPeerId: String): Conversation {
        require(!blockListService.isBlocked(targetPeerId)) { "Peer $targetPeerId is blocked" }
        requireNotNull(peerTrustVerificationService.requireTrusted(targetPeerId)) { "Peer $targetPeerId is not trusted" }
        val localProfile = localProfileService.require()
        return createOrLoadConversation(localProfile.peerId, targetPeerId)
    }

    /** Возвращает последние квитанции доставки. */
    suspend fun recentReceipts(limit: Int = 100): List<MessageReceipt> = receiptsMutex.withLock {
        receipts.toList().takeLast(limit)
    }

    /** Отправляет новое сообщение. */
    suspend fun send(outgoingMessage: OutgoingMessage): ChatMessage {
        require(!blockListService.isBlocked(outgoingMessage.targetPeerId)) { "Peer ${outgoingMessage.targetPeerId} is blocked" }
        val localProfile = localProfileService.require()
        val trustedPeer = requireNotNull(peerTrustVerificationService.requireTrusted(outgoingMessage.targetPeerId)) {
            "Peer ${outgoingMessage.targetPeerId} is not trusted"
        }
        val conversationId = outgoingMessage.conversationId
        val conversation = if (conversationId != null) {
            conversationRepositoryPort.findByConversationId(conversationId)
        } else {
            null
        } ?: createOrLoadConversation(localProfile.peerId, outgoingMessage.targetPeerId)
        val message = ChatMessage(
            messageId = newId("message"),
            conversationId = conversation.conversationId,
            senderPeerId = localProfile.peerId,
            recipientPeerId = outgoingMessage.targetPeerId,
            body = outgoingMessage.body,
            deliveryStatus = MessageDeliveryStatus.QUEUED,
            createdAt = now(),
        )
        conversationRepositoryPort.save(
            conversation.copy(
                updatedAt = now(),
                lastMessageId = message.messageId,
            ),
        )
        messageRepositoryPort.save(message)
        val payload = ChatMessagePayload(
            messageId = message.messageId,
            conversationId = conversation.conversationId,
            senderPeerId = localProfile.peerId,
            recipientPeerId = outgoingMessage.targetPeerId,
            body = outgoingMessage.body,
            sentAt = now(),
        )
        val encrypted = messageEncryptionService.encryptPayload(trustedPeer.peerIdentity.publicKey, payload)
        val unsigned = packetEnvelopeFactory.create(
            packetType = PacketType.CHAT_MESSAGE,
            sourcePeerId = localProfile.peerId,
            targetPeerId = outgoingMessage.targetPeerId,
            encryptedPayload = encrypted,
            routeMode = RouteMode.LOCAL_DIRECT,
            ttl = 5,
            requiresAck = true,
            messageId = message.messageId,
            conversationId = conversation.conversationId,
        )
        val signed = packetSignatureService.signEnvelope(localProfile.privateKey, unsigned)
        val result = deliveryTrackingService.send(signed)
        val updatedStatus = if (result.success) MessageDeliveryStatus.ACK_PENDING else MessageDeliveryStatus.FAILED
        val updated = messageRepositoryPort.updateStatus(message.messageId, updatedStatus) ?: message.copy(deliveryStatus = updatedStatus)
        eventLogService.log(
            category = EventCategory.MESSAGING,
            level = if (result.success) EventLevel.INFO else EventLevel.ERROR,
            message = if (result.success) "Sent chat message" else "Failed to send chat message",
            peerId = outgoingMessage.targetPeerId,
            packetId = signed.packetId,
        )
        nodeMetricsService.increment("chat.outbound")
        return updated
    }

    /**
     * Persists an inbound chat message and sends a delivery acknowledgement.
     */
    suspend fun handleIncomingMessage(envelope: PacketEnvelope, payload: ChatMessagePayload): ChatMessage {
        require(!blockListService.isBlocked(payload.senderPeerId)) { "Peer ${payload.senderPeerId} is blocked" }
        requireNotNull(peerTrustVerificationService.requireTrusted(payload.senderPeerId)) {
            "Peer ${payload.senderPeerId} is not trusted"
        }
        val conversation = conversationRepositoryPort.findByConversationId(payload.conversationId)
            ?: conversationRepositoryPort.save(
                Conversation(
                    conversationId = payload.conversationId,
                    participantPeerIds = setOf(payload.senderPeerId, payload.recipientPeerId),
                    createdAt = now(),
                    updatedAt = now(),
                    lastMessageId = payload.messageId,
                ),
            )
        conversationRepositoryPort.save(
            conversation.copy(
                updatedAt = now(),
                lastMessageId = payload.messageId,
            ),
        )
        val message = ChatMessage(
            messageId = payload.messageId,
            conversationId = conversation.conversationId,
            senderPeerId = payload.senderPeerId,
            recipientPeerId = payload.recipientPeerId,
            body = payload.body,
            deliveryStatus = MessageDeliveryStatus.DELIVERED,
            createdAt = payload.sentAt,
            deliveredAt = now(),
        )
        messageRepositoryPort.save(message)
        if (envelope.requiresAck) {
            sendDeliveryAck(envelope)
        }
        nodeMetricsService.increment("chat.inbound")
        eventLogService.log(
            category = EventCategory.MESSAGING,
            level = EventLevel.INFO,
            message = "Received chat message",
            peerId = payload.senderPeerId,
            packetId = envelope.packetId,
        )
        return message
    }

    /**
     * Emits an ACK for a previously received message packet.
     */
    suspend fun sendDeliveryAck(originalEnvelope: PacketEnvelope) {
        val localProfile = localProfileService.require()
        val trustedSource = requireNotNull(peerTrustVerificationService.requireTrusted(originalEnvelope.sourcePeerId)) {
            "Peer ${originalEnvelope.sourcePeerId} is not trusted"
        }
        val ack = DeliveryAckPayload(
            DeliveryAck(
                acknowledgedPacketId = originalEnvelope.packetId,
                messageId = originalEnvelope.messageId,
                conversationId = originalEnvelope.conversationId,
                receivedAt = now(),
            ),
        )
        val encrypted = messageEncryptionService.encryptPayload(trustedSource.peerIdentity.publicKey, ack)
        val unsigned = packetEnvelopeFactory.create(
            packetType = PacketType.DELIVERY_ACK,
            sourcePeerId = localProfile.peerId,
            targetPeerId = originalEnvelope.sourcePeerId,
            encryptedPayload = encrypted,
            routeMode = RouteMode.RELAY_FLOOD,
            ttl = 5,
            requiresAck = false,
            messageId = originalEnvelope.messageId,
            conversationId = originalEnvelope.conversationId,
        )
        val signed = packetSignatureService.signEnvelope(localProfile.privateKey, unsigned)
        deliveryTrackingService.send(signed)
    }

    /**
     * Processes an inbound delivery acknowledgement.
     */
    suspend fun handleDeliveryAck(payload: DeliveryAckPayload): MessageReceipt {
        deliveryTrackingService.acknowledge(payload.ack)
        nodeMetricsService.increment("chat.ack")
        val receipt = MessageReceipt(
            messageId = payload.ack.messageId ?: payload.ack.acknowledgedPacketId,
            packetId = payload.ack.acknowledgedPacketId,
            conversationId = payload.ack.conversationId ?: payload.ack.acknowledgedPacketId,
            receivedAt = payload.ack.receivedAt,
            deliveryStatus = MessageDeliveryStatus.DELIVERED,
        )
        rememberReceipt(receipt)
        return receipt
    }

    private suspend fun createOrLoadConversation(localPeerId: String, targetPeerId: String): Conversation {
        return conversationRepositoryPort.findByParticipants(setOf(localPeerId, targetPeerId))
            ?: conversationRepositoryPort.save(
                Conversation(
                    conversationId = newId("conversation"),
                    participantPeerIds = setOf(localPeerId, targetPeerId),
                    createdAt = now(),
                    updatedAt = now(),
                ),
            )
    }

    private suspend fun rememberReceipt(receipt: MessageReceipt) {
        receiptsMutex.withLock {
            if (receipts.size >= 1_000) {
                receipts.removeFirst()
            }
            receipts.addLast(receipt)
        }
    }
}
