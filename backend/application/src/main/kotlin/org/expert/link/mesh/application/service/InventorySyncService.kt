package org.expert.link.mesh.application.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.expert.link.mesh.application.factory.PacketEnvelopeFactory
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.inventory.InventoryEvent
import org.expert.link.mesh.domain.model.network.InventoryEventPacket
import org.expert.link.mesh.domain.model.network.InventorySyncRequestPayload
import org.expert.link.mesh.domain.model.network.InventorySyncResponsePayload
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.port.repository.InventoryEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.OrganizationMemberRepositoryPort

data class InventorySyncResult(
    val requestId: String,
    val organizationId: String,
    val peerId: String,
    val eventsReceived: Int,
    val lastSequence: Long,
    val timedOut: Boolean = false,
)

class InventorySyncService(
    private val localProfileService: LocalProfileService,
    private val organizationMemberRepositoryPort: OrganizationMemberRepositoryPort,
    private val peerTrustVerificationService: PeerTrustVerificationService,
    private val messageEncryptionService: MessageEncryptionService,
    private val packetEnvelopeFactory: PacketEnvelopeFactory,
    private val packetSignatureService: PacketSignatureService,
    private val deliveryTrackingService: DeliveryTrackingService,
    private val inventoryEventRepositoryPort: InventoryEventRepositoryPort,
    private val inventoryEventApplier: InventoryEventApplier,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
) {
    private val pendingMutex = Mutex()
    private val pending = linkedMapOf<String, CompletableDeferred<InventorySyncResult>>()

    suspend fun broadcastEvent(event: InventoryEvent) {
        val localProfile = localProfileService.require()
        val members = organizationMemberRepositoryPort.listByOrganization(event.organizationId)
        members.map { it.peerId }
            .filterNot { it == localProfile.peerId }
            .distinct()
            .forEach { peerId ->
                runCatching { sendEvent(peerId, event) }
            }
    }

    suspend fun handleIncomingEvent(envelope: PacketEnvelope, payload: InventoryEventPacket) {
        inventoryEventApplier.apply(payload.event)
        nodeMetricsService.increment("inventory.events.received")
        eventLogService.log(
            category = EventCategory.INVENTORY,
            level = EventLevel.INFO,
            message = "Inventory event received",
            peerId = envelope.sourcePeerId,
            packetId = envelope.packetId,
            attributes = mapOf("eventId" to payload.event.eventId, "entityId" to payload.event.entityId),
        )
    }

    suspend fun requestSync(
        targetPeerId: String,
        organizationId: String,
        sinceSequence: Long? = null,
        timeoutMillis: Long = 4_000,
    ): InventorySyncResult {
        val localProfile = localProfileService.require()
        val trusted = requireNotNull(peerTrustVerificationService.requireTrusted(targetPeerId)) {
            "Peer $targetPeerId is not trusted"
        }
        val requestId = newId("inv-sync")
        val deferred = CompletableDeferred<InventorySyncResult>()
        pendingMutex.withLock { pending[requestId] = deferred }
        val payload = InventorySyncRequestPayload(
            requestId = requestId,
            organizationId = organizationId,
            requesterPeerId = localProfile.peerId,
            sinceSequence = sinceSequence,
            requestedAt = now(),
        )
        val encrypted = messageEncryptionService.encryptPayload(trusted.peerIdentity.publicKey, payload)
        val unsigned = packetEnvelopeFactory.create(
            packetType = PacketType.INVENTORY_SYNC_REQUEST,
            sourcePeerId = localProfile.peerId,
            targetPeerId = targetPeerId,
            encryptedPayload = encrypted,
            routeMode = RouteMode.LOCAL_DIRECT,
            ttl = 5,
            requiresAck = true,
            messageId = requestId,
        )
        val signed = packetSignatureService.signEnvelope(localProfile.privateKey, unsigned)
        deliveryTrackingService.send(signed)
        val result = withTimeoutOrNull(timeoutMillis) { deferred.await() }
        pendingMutex.withLock { pending.remove(requestId) }
        if (result != null) {
            return result
        }
        return InventorySyncResult(
            requestId = requestId,
            organizationId = organizationId,
            peerId = targetPeerId,
            eventsReceived = 0,
            lastSequence = inventoryEventRepositoryPort.latestSequence(organizationId),
            timedOut = true,
        )
    }

    suspend fun handleSyncRequest(envelope: PacketEnvelope, payload: InventorySyncRequestPayload) {
        val localProfile = localProfileService.require()
        val sinceSequence = payload.sinceSequence
        val events = if (sinceSequence != null) {
            inventoryEventRepositoryPort.listByOrganizationSinceSequence(payload.organizationId, sinceSequence)
        } else {
            inventoryEventRepositoryPort.listByOrganization(payload.organizationId)
        }
        val responsePayload = InventorySyncResponsePayload(
            requestId = payload.requestId,
            organizationId = payload.organizationId,
            responderPeerId = localProfile.peerId,
            targetPeerId = payload.requesterPeerId,
            events = events,
            sentAt = now(),
        )
        val trusted = requireNotNull(peerTrustVerificationService.requireTrusted(payload.requesterPeerId)) {
            "Peer ${payload.requesterPeerId} is not trusted"
        }
        val encrypted = messageEncryptionService.encryptPayload(trusted.peerIdentity.publicKey, responsePayload)
        val unsigned = packetEnvelopeFactory.create(
            packetType = PacketType.INVENTORY_SYNC_RESPONSE,
            sourcePeerId = localProfile.peerId,
            targetPeerId = payload.requesterPeerId,
            encryptedPayload = encrypted,
            routeMode = RouteMode.LOCAL_DIRECT,
            ttl = 5,
            requiresAck = true,
            messageId = payload.requestId,
        )
        val signed = packetSignatureService.signEnvelope(localProfile.privateKey, unsigned)
        deliveryTrackingService.send(signed)
        nodeMetricsService.increment("inventory.sync.sent")
        eventLogService.log(
            category = EventCategory.INVENTORY,
            level = EventLevel.INFO,
            message = "Inventory sync response sent",
            peerId = envelope.sourcePeerId,
            packetId = envelope.packetId,
            attributes = mapOf("requestId" to payload.requestId, "events" to events.size.toString()),
        )
    }

    suspend fun handleSyncResponse(payload: InventorySyncResponsePayload) {
        val applied = payload.events.count { inventoryEventApplier.apply(it) }
        val latest = inventoryEventRepositoryPort.latestSequence(payload.organizationId)
        val deferred = pendingMutex.withLock { pending[payload.requestId] }
        deferred?.complete(
            InventorySyncResult(
                requestId = payload.requestId,
                organizationId = payload.organizationId,
                peerId = payload.responderPeerId,
                eventsReceived = applied,
                lastSequence = latest,
            ),
        )
    }

    private suspend fun sendEvent(targetPeerId: String, event: InventoryEvent) {
        val localProfile = localProfileService.require()
        val trusted = requireNotNull(peerTrustVerificationService.requireTrusted(targetPeerId)) {
            "Peer $targetPeerId is not trusted"
        }
        val payload = InventoryEventPacket(event = event, sentAt = now())
        val encrypted = messageEncryptionService.encryptPayload(trusted.peerIdentity.publicKey, payload)
        val unsigned = packetEnvelopeFactory.create(
            packetType = PacketType.INVENTORY_EVENT,
            sourcePeerId = localProfile.peerId,
            targetPeerId = targetPeerId,
            encryptedPayload = encrypted,
            routeMode = RouteMode.LOCAL_DIRECT,
            ttl = 5,
            requiresAck = true,
            messageId = event.eventId,
        )
        val signed = packetSignatureService.signEnvelope(localProfile.privateKey, unsigned)
        deliveryTrackingService.send(signed)
        nodeMetricsService.increment("inventory.events.sent")
    }
}
