package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.inventory.InventoryDashboardSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryDashboardSnapshotPayload
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryIncidentStatus
import org.expert.link.mesh.domain.model.inventory.InventorySessionStatus
import org.expert.link.mesh.domain.port.repository.InventoryDashboardRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryIncidentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySessionRepositoryPort

class InventoryDashboardService(
    private val localProfileService: LocalProfileService,
    private val itemRepositoryPort: InventoryItemRepositoryPort,
    private val incidentRepositoryPort: InventoryIncidentRepositoryPort,
    private val sessionRepositoryPort: InventorySessionRepositoryPort,
    private val dashboardRepositoryPort: InventoryDashboardRepositoryPort,
    private val inventoryEventService: InventoryEventService,
    private val inventorySyncService: InventorySyncService,
) {
    suspend fun snapshot(organizationId: String): InventoryDashboardSnapshot {
        val items = itemRepositoryPort.listByOrganization(organizationId)
        val incidents = incidentRepositoryPort.listByOrganization(organizationId)
        val sessions = sessionRepositoryPort.listByOrganization(organizationId)
        val byStatus = items.groupingBy { it.currentStatus }.eachCount()
        val byCondition = items.groupingBy { it.condition }.eachCount()
        val byCategoryId = items.groupingBy { it.categoryId ?: "uncategorized" }.eachCount()
        val byLocationId = items.groupingBy { it.locationId ?: "unassigned" }.eachCount()
        val incidentsOpen = incidents.count {
            it.status == InventoryIncidentStatus.OPEN || it.status == InventoryIncidentStatus.UNDER_REVIEW
        }
        val sessionsActive = sessions.count { it.status == InventorySessionStatus.ACTIVE }
        val snapshot = InventoryDashboardSnapshot(
            snapshotId = newId("dash"),
            organizationId = organizationId,
            generatedAt = now(),
            totalItems = items.size,
            byStatus = byStatus,
            byCondition = byCondition,
            byCategoryId = byCategoryId,
            byLocationId = byLocationId,
            incidentsOpen = incidentsOpen,
            sessionsActive = sessionsActive,
        )
        dashboardRepositoryPort.save(snapshot)
        val localProfile = localProfileService.require()
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.DASHBOARD_SNAPSHOT,
                entityId = snapshot.snapshotId,
                eventType = InventoryEventType.DASHBOARD_SNAPSHOT,
                actorPeerId = localProfile.peerId,
                payload = InventoryDashboardSnapshotPayload(snapshot),
            ),
        )
        return snapshot
    }

    private suspend fun publish(event: org.expert.link.mesh.domain.model.inventory.InventoryEvent) {
        inventorySyncService.broadcastEvent(event)
    }
}
