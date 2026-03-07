package org.expert.link.mesh.controller

import org.expert.link.mesh.bootstrap.NodeLifecycleService
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.TransportDeliveryResult

/** Контроллер входящих пакетов. */
class PacketController(
    private val nodeLifecycleService: NodeLifecycleService,
) {
    /** Обрабатывает входящий пакет. */
    suspend fun handleEnvelope(envelope: PacketEnvelope): TransportDeliveryResult = nodeLifecycleService.handleIncomingPacket(envelope)
}
