package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.factory.PacketEnvelopeFactory
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.messaging.ChatMember
import org.expert.link.mesh.domain.model.messaging.ChatMemberRole
import org.expert.link.mesh.domain.model.messaging.ChatMessage
import org.expert.link.mesh.domain.model.messaging.ChatType
import org.expert.link.mesh.domain.model.messaging.Conversation
import org.expert.link.mesh.domain.model.messaging.MessageDeliveryStatus
import org.expert.link.mesh.domain.model.messaging.MessageType
import org.expert.link.mesh.domain.model.messaging.MessageReceipt
import org.expert.link.mesh.domain.model.messaging.OutgoingMessage
import org.expert.link.mesh.domain.model.messaging.ThreadSummary
import org.expert.link.mesh.domain.model.network.ChatMessagePayload
import org.expert.link.mesh.domain.model.network.DeliveryAck
import org.expert.link.mesh.domain.model.network.DeliveryAckPayload
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.port.repository.ConversationRepositoryPort
import org.expert.link.mesh.domain.port.repository.MessageRepositoryPort
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

    /** Создаёт групповой чат. */
    suspend fun createGroupConversation(title: String, description: String?, participantPeerIds: Set<String>): Conversation {
        require(title.isNotBlank()) { "Group title is blank" }
        val localProfile = localProfileService.require()
        val members = (participantPeerIds + localProfile.peerId).map { peerId ->
            val trusted = peerTrustVerificationService.requireTrusted(peerId)
            ChatMember(
                peerId = peerId,
                displayName = trusted?.peerIdentity?.displayName ?: if (peerId == localProfile.peerId) localProfile.displayName else peerId,
                role = if (peerId == localProfile.peerId) ChatMemberRole.OWNER else ChatMemberRole.MEMBER,
                joinedAt = now(),
            )
        }
        val conversation = Conversation(
            conversationId = newId("conversation"),
            chatType = ChatType.GROUP,
            title = title,
            description = description,
            createdByPeerId = localProfile.peerId,
            participantPeerIds = members.map { it.peerId }.toSet(),
            members = members,
            createdAt = now(),
            updatedAt = now(),
        )
        return conversationRepositoryPort.save(conversation)
    }

    /** Обновляет название чата. */
    suspend fun renameConversation(conversationId: String, title: String): Conversation? {
        require(title.isNotBlank()) { "Title is blank" }
        val current = conversationRepositoryPort.findByConversationId(conversationId) ?: return null
        val updated = conversationRepositoryPort.save(current.copy(title = title, updatedAt = now()))
        createSystemMessage(updated, "Название чата изменено")
        return updated
    }

    /** Добавляет участника в групповой чат. */
    suspend fun addParticipant(conversationId: String, peer: PeerIdentity): Conversation? {
        val current = conversationRepositoryPort.findByConversationId(conversationId) ?: return null
        if (current.chatType != ChatType.GROUP || current.participantPeerIds.contains(peer.peerId)) {
            return current
        }
        val member = ChatMember(
            peerId = peer.peerId,
            displayName = peer.displayName,
            role = ChatMemberRole.MEMBER,
            joinedAt = now(),
        )
        val updated = conversationRepositoryPort.save(
            current.copy(
                participantPeerIds = current.participantPeerIds + peer.peerId,
                members = current.members + member,
                updatedAt = now(),
            ),
        )
        createSystemMessage(updated, "Участник ${peer.displayName} добавлен")
        return updated
    }

    /** Удаляет участника из группового чата. */
    suspend fun removeParticipant(conversationId: String, peerId: String): Conversation? {
        val current = conversationRepositoryPort.findByConversationId(conversationId) ?: return null
        if (current.chatType != ChatType.GROUP || !current.participantPeerIds.contains(peerId)) {
            return current
        }
        val removedName = current.members.firstOrNull { it.peerId == peerId }?.displayName ?: peerId
        val updated = conversationRepositoryPort.save(
            current.copy(
                participantPeerIds = current.participantPeerIds - peerId,
                members = current.members.filterNot { it.peerId == peerId },
                updatedAt = now(),
            ),
        )
        createSystemMessage(updated, "Участник $removedName удалён")
        return updated
    }

    /** Возвращает последние квитанции доставки. */
    suspend fun recentReceipts(limit: Int = 100): List<MessageReceipt> = receiptsMutex.withLock {
        val items = receipts.toList()
        if (limit >= items.size) items else items.subList(items.size - limit, items.size)
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
            messageType = MessageType.TEXT,
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
            chatType = conversation.chatType,
            chatTitle = conversation.title,
            chatDescription = conversation.description,
            participantPeerIds = conversation.participantPeerIds,
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

    /** Отправляет сообщение в групповой или direct чат по conversationId. */
    suspend fun sendToConversation(conversationId: String, body: String): ChatMessage {
        val conversation = requireNotNull(conversationRepositoryPort.findByConversationId(conversationId)) {
            "Conversation $conversationId not found"
        }
        return if (conversation.chatType == ChatType.DIRECT) {
            val localPeerId = localProfileService.require().peerId
            val targetPeer = conversation.participantPeerIds.first { it != localPeerId }
            send(OutgoingMessage(targetPeerId = targetPeer, conversationId = conversationId, body = body, requestedAt = now()))
        } else {
            sendGroupMessage(conversation, body, messageType = MessageType.TEXT, threadRootMessageId = null, parentMessageId = null, replyToMessageId = null)
        }
    }

    /** Отправляет ответ в тред. */
    suspend fun sendThreadMessage(conversationId: String, rootMessageId: String, body: String, parentMessageId: String? = null): ChatMessage {
        val conversation = requireNotNull(conversationRepositoryPort.findByConversationId(conversationId)) {
            "Conversation $conversationId not found"
        }
        val rootMessage = requireNotNull(messageRepositoryPort.findByMessageId(rootMessageId)) {
            "Root message $rootMessageId not found"
        }
        require(rootMessage.conversationId == conversationId) { "Root message does not belong to conversation" }
        return if (conversation.chatType == ChatType.DIRECT) {
            val localPeerId = localProfileService.require().peerId
            val targetPeer = conversation.participantPeerIds.first { it != localPeerId }
            sendDirectThreadMessage(conversation, targetPeer, body, rootMessageId, parentMessageId)
        } else {
            sendGroupMessage(conversation, body, MessageType.TEXT, rootMessageId, parentMessageId, rootMessageId)
        }
    }

    /** Возвращает сообщения треда. */
    suspend fun threadMessages(conversationId: String, rootMessageId: String): List<ChatMessage> {
        return messageRepositoryPort.listByThread(conversationId, rootMessageId)
    }

    /** Возвращает сводку треда. */
    suspend fun threadSummary(conversationId: String, rootMessageId: String): ThreadSummary? {
        return messageRepositoryPort.getThreadSummary(conversationId, rootMessageId)
    }

    /** Возвращает сообщение по id. */
    suspend fun message(messageId: String): ChatMessage? {
        return messageRepositoryPort.findByMessageId(messageId)
    }

    /**
     * Persists an inbound chat message and sends a delivery acknowledgement.
     */
    suspend fun handleIncomingMessage(envelope: PacketEnvelope, payload: ChatMessagePayload): ChatMessage {
        require(!blockListService.isBlocked(payload.senderPeerId)) { "Peer ${payload.senderPeerId} is blocked" }
        requireNotNull(peerTrustVerificationService.requireTrusted(payload.senderPeerId)) {
            "Peer ${payload.senderPeerId} is not trusted"
        }
        val existingConversation = conversationRepositoryPort.findByConversationId(payload.conversationId)
        val conversation = when {
            existingConversation == null -> conversationRepositoryPort.save(
                Conversation(
                    conversationId = payload.conversationId,
                    chatType = payload.chatType,
                    title = payload.chatTitle ?: payload.senderPeerId,
                    description = payload.chatDescription,
                    createdByPeerId = payload.senderPeerId,
                    participantPeerIds = inboundParticipantIds(payload),
                    members = inboundMembers(payload, emptyList()),
                    createdAt = now(),
                    updatedAt = now(),
                    lastMessageId = payload.messageId,
                ),
            )

            existingConversation.chatType == ChatType.DIRECT && payload.chatType == ChatType.GROUP -> {
                val upgraded = existingConversation.copy(
                    chatType = ChatType.GROUP,
                    title = payload.chatTitle ?: existingConversation.title,
                    description = payload.chatDescription ?: existingConversation.description,
                    createdByPeerId = payload.senderPeerId,
                    participantPeerIds = existingConversation.participantPeerIds + inboundParticipantIds(payload),
                    members = inboundMembers(payload, existingConversation.members),
                    updatedAt = now(),
                    lastMessageId = payload.messageId,
                )
                conversationRepositoryPort.save(upgraded)
            }

            else -> conversationRepositoryPort.save(
                existingConversation.copy(
                    participantPeerIds = existingConversation.participantPeerIds + inboundParticipantIds(payload),
                    members = inboundMembers(payload, existingConversation.members),
                    updatedAt = now(),
                    lastMessageId = payload.messageId,
                ),
            )
        }
        val message = ChatMessage(
            messageId = payload.messageId,
            conversationId = conversation.conversationId,
            senderPeerId = payload.senderPeerId,
            recipientPeerId = payload.recipientPeerId,
            body = payload.body,
            messageType = payload.messageType,
            threadRootMessageId = payload.threadRootMessageId,
            parentMessageId = payload.parentMessageId,
            replyToMessageId = payload.replyToMessageId,
            deliveryStatus = MessageDeliveryStatus.DELIVERED,
            createdAt = payload.sentAt,
            deliveredAt = now(),
        )
        messageRepositoryPort.save(message)
        payload.threadRootMessageId?.let { rootId ->
            val summary = messageRepositoryPort.getThreadSummary(conversation.conversationId, rootId)
            messageRepositoryPort.updateThreadReplyCount(rootId, summary?.replyCount ?: 0)
        }
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
        val participants = setOf(localPeerId, targetPeerId)
        val existingDirect = conversationRepositoryPort.list().firstOrNull {
            it.chatType == ChatType.DIRECT && it.participantPeerIds == participants
        }
        return existingDirect
            ?: conversationRepositoryPort.save(
                Conversation(
                    conversationId = newId("conversation"),
                    chatType = ChatType.DIRECT,
                    title = targetPeerId,
                    createdByPeerId = localPeerId,
                    participantPeerIds = participants,
                    members = listOf(
                        ChatMember(localPeerId, localPeerId, ChatMemberRole.OWNER, now()),
                        ChatMember(targetPeerId, targetPeerId, ChatMemberRole.MEMBER, now()),
                    ),
                    createdAt = now(),
                    updatedAt = now(),
                ),
            )
    }

    private suspend fun sendDirectThreadMessage(
        conversation: Conversation,
        targetPeerId: String,
        body: String,
        rootMessageId: String,
        parentMessageId: String?,
    ): ChatMessage {
        val localProfile = localProfileService.require()
        val trustedPeer = requireNotNull(peerTrustVerificationService.requireTrusted(targetPeerId)) {
            "Peer $targetPeerId is not trusted"
        }
        val message = ChatMessage(
            messageId = newId("message"),
            conversationId = conversation.conversationId,
            senderPeerId = localProfile.peerId,
            recipientPeerId = targetPeerId,
            body = body,
            messageType = MessageType.TEXT,
            threadRootMessageId = rootMessageId,
            parentMessageId = parentMessageId,
            replyToMessageId = rootMessageId,
            deliveryStatus = MessageDeliveryStatus.QUEUED,
            createdAt = now(),
        )
        conversationRepositoryPort.save(conversation.copy(updatedAt = now(), lastMessageId = message.messageId))
        messageRepositoryPort.save(message)
        val payload = ChatMessagePayload(
            messageId = message.messageId,
            conversationId = conversation.conversationId,
            senderPeerId = localProfile.peerId,
            recipientPeerId = targetPeerId,
            body = body,
            chatType = conversation.chatType,
            chatTitle = conversation.title,
            chatDescription = conversation.description,
            participantPeerIds = conversation.participantPeerIds,
            messageType = message.messageType,
            threadRootMessageId = rootMessageId,
            parentMessageId = parentMessageId,
            replyToMessageId = rootMessageId,
            sentAt = now(),
        )
        val encrypted = messageEncryptionService.encryptPayload(trustedPeer.peerIdentity.publicKey, payload)
        val unsigned = packetEnvelopeFactory.create(
            packetType = PacketType.CHAT_MESSAGE,
            sourcePeerId = localProfile.peerId,
            targetPeerId = targetPeerId,
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
        val summary = messageRepositoryPort.getThreadSummary(conversation.conversationId, rootMessageId)
        messageRepositoryPort.updateThreadReplyCount(rootMessageId, summary?.replyCount ?: 0)
        return updated
    }

    private suspend fun sendGroupMessage(
        conversation: Conversation,
        body: String,
        messageType: MessageType,
        threadRootMessageId: String?,
        parentMessageId: String?,
        replyToMessageId: String?,
    ): ChatMessage {
        val localProfile = localProfileService.require()
        val recipients = conversation.participantPeerIds.filter { it != localProfile.peerId }
        require(recipients.isNotEmpty()) { "Group has no recipients" }
        val message = ChatMessage(
            messageId = newId("message"),
            conversationId = conversation.conversationId,
            senderPeerId = localProfile.peerId,
            recipientPeerId = "group:${conversation.conversationId}",
            body = body,
            messageType = messageType,
            threadRootMessageId = threadRootMessageId,
            parentMessageId = parentMessageId,
            replyToMessageId = replyToMessageId,
            deliveryStatus = MessageDeliveryStatus.QUEUED,
            createdAt = now(),
        )
        conversationRepositoryPort.save(conversation.copy(updatedAt = now(), lastMessageId = message.messageId))
        messageRepositoryPort.save(message)

        var failed = false
        recipients.forEach { targetPeerId ->
            val trusted = peerTrustVerificationService.requireTrusted(targetPeerId) ?: run {
                failed = true
                return@forEach
            }
            val payload = ChatMessagePayload(
                messageId = message.messageId,
                conversationId = conversation.conversationId,
                senderPeerId = localProfile.peerId,
                recipientPeerId = targetPeerId,
                body = body,
                chatType = ChatType.GROUP,
                chatTitle = conversation.title,
                chatDescription = conversation.description,
                participantPeerIds = conversation.participantPeerIds,
                messageType = messageType,
                threadRootMessageId = threadRootMessageId,
                parentMessageId = parentMessageId,
                replyToMessageId = replyToMessageId,
                sentAt = now(),
            )
            val encrypted = messageEncryptionService.encryptPayload(trusted.peerIdentity.publicKey, payload)
            val unsigned = packetEnvelopeFactory.create(
                packetType = PacketType.CHAT_MESSAGE,
                sourcePeerId = localProfile.peerId,
                targetPeerId = targetPeerId,
                encryptedPayload = encrypted,
                routeMode = RouteMode.RELAY_FLOOD,
                ttl = 7,
                requiresAck = true,
                messageId = message.messageId,
                conversationId = conversation.conversationId,
            )
            val signed = packetSignatureService.signEnvelope(localProfile.privateKey, unsigned)
            val result = deliveryTrackingService.send(signed)
            if (!result.success) {
                failed = true
            }
        }

        val status = if (failed) MessageDeliveryStatus.FAILED else MessageDeliveryStatus.ACK_PENDING
        val updated = messageRepositoryPort.updateStatus(message.messageId, status) ?: message.copy(deliveryStatus = status)
        threadRootMessageId?.let { rootId ->
            val summary = messageRepositoryPort.getThreadSummary(conversation.conversationId, rootId)
            messageRepositoryPort.updateThreadReplyCount(rootId, summary?.replyCount ?: 0)
        }
        return updated
    }

    private fun inboundParticipantIds(payload: ChatMessagePayload): Set<String> {
        return if (payload.participantPeerIds.isNotEmpty()) {
            payload.participantPeerIds
        } else {
            setOf(payload.senderPeerId, payload.recipientPeerId)
        }
    }

    private fun inboundMembers(payload: ChatMessagePayload, existing: List<ChatMember>): List<ChatMember> {
        val byPeerId = existing.associateBy { it.peerId }.toMutableMap()
        inboundParticipantIds(payload).forEach { peerId ->
            if (!byPeerId.containsKey(peerId)) {
                val role = if (peerId == payload.senderPeerId) {
                    ChatMemberRole.OWNER
                } else {
                    ChatMemberRole.MEMBER
                }
                byPeerId[peerId] = ChatMember(
                    peerId = peerId,
                    displayName = peerId,
                    role = role,
                    joinedAt = now(),
                )
            }
        }
        return byPeerId.values.toList()
    }

    private suspend fun createSystemMessage(conversation: Conversation, body: String) {
        val localProfile = localProfileService.require()
        val systemMessage = ChatMessage(
            messageId = newId("message"),
            conversationId = conversation.conversationId,
            senderPeerId = localProfile.peerId,
            recipientPeerId = "group:${conversation.conversationId}",
            body = body,
            messageType = MessageType.SYSTEM,
            deliveryStatus = MessageDeliveryStatus.DELIVERED,
            createdAt = now(),
            deliveredAt = now(),
        )
        messageRepositoryPort.save(systemMessage)
        conversationRepositoryPort.save(conversation.copy(updatedAt = now(), lastMessageId = systemMessage.messageId))
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
