package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEvent
import org.expert.link.mesh.domain.model.inventory.InventoryEventPayload
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.port.repository.InventoryEventRepositoryPort

class InventoryEventService(
    private val inventoryEventRepositoryPort: InventoryEventRepositoryPort,
) {
    suspend fun recordEvent(
        organizationId: String,
        entityType: InventoryEntityType,
        entityId: String,
        eventType: InventoryEventType,
        actorPeerId: String,
        payload: InventoryEventPayload,
        entityRevision: Long? = null,
        previousEntityRevision: Long? = null,
        sessionId: String? = null,
        conflict: Boolean = false,
        notes: String? = null,
    ): InventoryEvent {
        val sequence = inventoryEventRepositoryPort.nextSequence(organizationId)
        val event = InventoryEvent(
            eventId = newId("inv-event"),
            organizationId = organizationId,
            entityType = entityType,
            entityId = entityId,
            eventType = eventType,
            actorPeerId = actorPeerId,
            occurredAt = now(),
            sequence = sequence,
            entityRevision = entityRevision,
            previousEntityRevision = previousEntityRevision,
            sessionId = sessionId,
            payload = payload,
            conflict = conflict,
            notes = notes,
        )
        return inventoryEventRepositoryPort.save(event)
    }
}
