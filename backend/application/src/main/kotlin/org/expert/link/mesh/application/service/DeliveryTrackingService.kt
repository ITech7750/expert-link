package org.expert.link.mesh.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.application.support.plusSeconds
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.messaging.MessageDeliveryStatus
import org.expert.link.mesh.domain.model.network.DeliveryAck
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.PendingAckRecord
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.model.network.TransportDeliveryResult
import org.expert.link.mesh.domain.port.external.PacketTransportPort
import org.expert.link.mesh.domain.port.external.RelayGatewayPort
import org.expert.link.mesh.domain.port.repository.MessageRepositoryPort
import org.expert.link.mesh.domain.port.repository.OutgoingQueuePort
import org.expert.link.mesh.domain.port.repository.PendingAckRepositoryPort

/** Сервис очереди, отправки и повторов исходящих пакетов. */
class DeliveryTrackingService(
    private val packetTransportPort: PacketTransportPort,
    private val relayGatewayPort: RelayGatewayPort?,
    private val routingService: RoutingService,
    private val outgoingQueuePort: OutgoingQueuePort,
    private val pendingAckRepositoryPort: PendingAckRepositoryPort,
    private val messageRepositoryPort: MessageRepositoryPort,
    private val retryPolicyService: RetryPolicyService,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
    private val topologyStateService: TopologyStateService? = null,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Queues and dispatches an outbound envelope.
     */
    suspend fun send(envelope: PacketEnvelope): TransportDeliveryResult {
        val record = PendingAckRecord(
            packetId = envelope.packetId,
            messageId = envelope.messageId ?: envelope.packetId,
            conversationId = envelope.conversationId ?: envelope.packetId,
            targetPeerId = envelope.targetPeerId,
            routeMode = envelope.routeMode,
            attempt = 0,
            nextAttemptAt = retryPolicyService.nextRetryAt(0) ?: now().plusSeconds(1),
            envelope = envelope,
            createdAt = now(),
        )
        outgoingQueuePort.enqueue(record)
        return sendQueuedRecord(requireNotNull(outgoingQueuePort.dequeue()))
    }

    /**
     * Marks a packet as acknowledged and updates message state when applicable.
     */
    suspend fun acknowledge(ack: DeliveryAck) {
        pendingAckRepositoryPort.remove(ack.acknowledgedPacketId)
        ack.messageId?.let { messageRepositoryPort.updateStatus(it, MessageDeliveryStatus.DELIVERED) }
        nodeMetricsService.increment("ack.received")
        eventLogService.log(
            category = EventCategory.MESSAGING,
            level = EventLevel.INFO,
            message = "Received delivery acknowledgement",
            packetId = ack.acknowledgedPacketId,
            attributes = mapOf(
                "messageId" to (ack.messageId ?: ""),
                "status" to ack.status,
            ),
        )
    }

    /**
     * Retries packets whose ACK deadline has elapsed.
     */
    suspend fun retryExpired() {
        val expired = pendingAckRepositoryPort.listExpired(now())
        expired.forEach { record ->
            if (!retryPolicyService.canRetry(record.attempt)) {
                pendingAckRepositoryPort.remove(record.packetId)
                routingService.invalidateRoute(record.targetPeerId)
                topologyStateService?.onRouteInvalidated(record.targetPeerId, "retry-exhausted")
                messageRepositoryPort.updateStatus(record.messageId, MessageDeliveryStatus.FAILED)
                eventLogService.log(
                    category = EventCategory.MESSAGING,
                    level = EventLevel.ERROR,
                    message = "Packet delivery failed after retries",
                    peerId = record.targetPeerId,
                    packetId = record.packetId,
                )
                return@forEach
            }
            val nextAttempt = record.attempt + 1
            val updatedRecord = record.copy(
                attempt = nextAttempt,
                nextAttemptAt = retryPolicyService.nextRetryAt(nextAttempt) ?: now().plusSeconds(1),
            )
            sendQueuedRecord(updatedRecord)
            nodeMetricsService.increment("retry.sent")
            eventLogService.log(
                category = EventCategory.MESSAGING,
                level = EventLevel.WARN,
                message = "Retried packet delivery",
                peerId = record.targetPeerId,
                packetId = record.packetId,
                attributes = mapOf("attempt" to nextAttempt.toString()),
            )
        }
    }

    private suspend fun sendQueuedRecord(record: PendingAckRecord): TransportDeliveryResult {
        val plan = routingService.resolvePlan(record.targetPeerId)
        if (plan.useRelayGateway) {
            nodeMetricsService.increment("relay.gateway.sent")
            val result = relayGatewayPort?.relayPacket(record.targetPeerId, record.envelope)
                ?: TransportDeliveryResult(success = false, errorMessage = "Relay gateway is not configured")
            handleResult(record, result)
            return result
        }
        if (plan.hops.isEmpty()) {
            val result = TransportDeliveryResult(success = false, errorMessage = "No route available")
            handleResult(record, result)
            return result
        }

        val results = plan.hops.map { hop -> packetTransportPort.sendPacket(hop.endpoint, record.envelope.copy(routeMode = plan.routeMode)) }
        val successful = results.firstOrNull { it.success }
        val result = successful ?: results.first()
        handleResult(record.copy(routeMode = plan.routeMode), result)
        return result
    }

    private suspend fun handleResult(record: PendingAckRecord, result: TransportDeliveryResult) {
        if (result.success) {
            if (record.envelope.requiresAck) {
                pendingAckRepositoryPort.save(record)
                messageRepositoryPort.updateStatus(record.messageId, MessageDeliveryStatus.ACK_PENDING)
            } else {
                messageRepositoryPort.updateStatus(record.messageId, MessageDeliveryStatus.SENT)
            }
            nodeMetricsService.increment("packet.sent")
            topologyStateService?.onDeliveryResult(
                targetPeerId = record.targetPeerId,
                routeMode = record.routeMode,
                success = true,
                viaRelayGateway = record.routeMode == RouteMode.RENDEZVOUS_RELAY,
            )
            return
        }
        logger.warn { "Failed to send packet ${record.packetId}: ${result.errorMessage}" }
        nodeMetricsService.increment("packet.send_failed")
        topologyStateService?.onDeliveryResult(
            targetPeerId = record.targetPeerId,
            routeMode = record.routeMode,
            success = false,
            viaRelayGateway = record.routeMode == RouteMode.RENDEZVOUS_RELAY,
        )
    }
}
