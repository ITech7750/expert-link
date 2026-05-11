package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.inventory.InventoryChangeLog
import org.expert.link.mesh.domain.model.inventory.InventoryChangeLogSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryFieldChange
import org.expert.link.mesh.domain.port.repository.InventoryChangeLogRepositoryPort

class InventoryChangeLogService(
    private val changeLogRepositoryPort: InventoryChangeLogRepositoryPort,
    private val inventoryEventService: InventoryEventService,
    private val inventorySyncService: InventorySyncService,
) {
    suspend fun recordChange(
        organizationId: String,
        entityType: InventoryEntityType,
        entityId: String,
        changes: List<InventoryFieldChange>,
        actorPeerId: String,
        reason: String?,
        sessionId: String?,
        sourcePeerId: String? = null,
    ): InventoryChangeLog? {
        if (changes.isEmpty()) {
            return null
        }
        val changeLog = InventoryChangeLog(
            changeId = newId("change"),
            organizationId = organizationId,
            entityType = entityType,
            entityId = entityId,
            changedByPeerId = actorPeerId,
            changedAt = now(),
            changes = changes,
            reason = reason,
            sessionId = sessionId,
            sourcePeerId = sourcePeerId,
        )
        changeLogRepositoryPort.save(changeLog)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.CHANGELOG,
                entityId = changeLog.changeId,
                eventType = InventoryEventType.CHANGELOG_RECORDED,
                actorPeerId = actorPeerId,
                payload = InventoryChangeLogSnapshot(changeLog),
            ),
        )
        return changeLog
    }

    suspend fun listByOrganization(organizationId: String): List<InventoryChangeLog> =
        changeLogRepositoryPort.listByOrganization(organizationId)

    suspend fun listByEntity(organizationId: String, entityId: String): List<InventoryChangeLog> =
        changeLogRepositoryPort.listByEntity(entityId).filter { it.organizationId == organizationId }

    private suspend fun publish(event: org.expert.link.mesh.domain.model.inventory.InventoryEvent) {
        inventorySyncService.broadcastEvent(event)
    }
}
