package org.expert.link.mesh.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.port.external.PacketTransportPort
import org.expert.link.mesh.domain.port.external.RelayGatewayPort

/** Сервис пересылки пакетов для других узлов. */
class RelayService(
    private val packetTransportPort: PacketTransportPort,
    private val relayGatewayPort: RelayGatewayPort?,
    private val routingService: RoutingService,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Relays a packet towards its destination if TTL still permits forwarding.
     *
     * @param localPeerId identity of the current node acting as a relay.
     * @param envelope packet to relay.
     * @return `true` when at least one forwarding attempt succeeded.
     */
    suspend fun relay(localPeerId: String, envelope: PacketEnvelope): Boolean {
        if (!envelope.canRelay()) {
            eventLogService.log(
                category = EventCategory.ROUTING,
                level = EventLevel.WARN,
                message = "Dropped packet because ttl is exhausted",
                peerId = envelope.targetPeerId,
                packetId = envelope.packetId,
            )
            return false
        }

        val relayEnvelope = envelope.decrementTtl(localPeerId)
        val plan = routingService.resolvePlan(envelope.targetPeerId, envelope.previousHopPeerId)
        if (plan.useRelayGateway) {
            val result = relayGatewayPort?.relayPacket(envelope.targetPeerId, relayEnvelope)
            val success = result?.success == true
            if (success) {
                nodeMetricsService.increment("relay.gateway.success")
            }
            return success
        }

        var forwarded = false
        plan.hops.forEach { hop ->
            val result = packetTransportPort.sendPacket(hop.endpoint, relayEnvelope)
            if (result.success) {
                forwarded = true
                nodeMetricsService.increment("relay.forwarded")
            }
        }
        if (forwarded) {
            logger.debug { "Relayed packet ${envelope.packetId} towards ${envelope.targetPeerId}" }
            eventLogService.log(
                category = EventCategory.ROUTING,
                level = EventLevel.INFO,
                message = "Relayed packet",
                peerId = envelope.targetPeerId,
                packetId = envelope.packetId,
            )
        }
        return forwarded
    }
}
