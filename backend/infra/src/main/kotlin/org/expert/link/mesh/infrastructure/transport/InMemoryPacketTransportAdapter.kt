package org.expert.link.mesh.infrastructure.transport

import kotlinx.datetime.Clock
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.TransportDeliveryResult
import org.expert.link.mesh.domain.port.external.PacketTransportPort
import java.util.concurrent.ConcurrentHashMap

/** In-memory transport-адаптер для тестов и симулятора. */
class InMemoryPacketTransportAdapter : PacketTransportPort {
    override suspend fun sendPacket(endpoint: PeerEndpoint, envelope: PacketEnvelope): TransportDeliveryResult {
        val dropKey = endpoint.asUrl() to envelope.packetType
        if (dropOnce.remove(dropKey) == true) {
            return TransportDeliveryResult(success = false, errorMessage = "Dropped by in-memory transport test rule")
        }
        val handler = handlers[endpoint.asUrl()]
            ?: return TransportDeliveryResult(success = false, errorMessage = "No in-memory handler registered for ${endpoint.asUrl()}")
        return handler(envelope)
    }

    companion object Hub {
        private val handlers = ConcurrentHashMap<String, suspend (PacketEnvelope) -> TransportDeliveryResult>()
        private val dropOnce = ConcurrentHashMap<Pair<String, PacketType>, Boolean>()

        /**
         * Registers an inbound handler for the supplied endpoint URL.
         */
        fun register(endpoint: PeerEndpoint, handler: suspend (PacketEnvelope) -> TransportDeliveryResult) {
            handlers[endpoint.asUrl()] = handler
        }

        /**
         * Removes a previously registered handler.
         */
        fun unregister(endpoint: PeerEndpoint) {
            handlers.remove(endpoint.asUrl())
        }

        /**
         * Drops the next packet of the supplied type sent to the endpoint.
         */
        fun dropNext(endpoint: PeerEndpoint, packetType: PacketType) {
            dropOnce[endpoint.asUrl() to packetType] = true
        }

        /**
         * Convenience success result for handlers.
         */
        fun accepted(): TransportDeliveryResult = TransportDeliveryResult(success = true, deliveredAt = Clock.System.now())
    }
}
