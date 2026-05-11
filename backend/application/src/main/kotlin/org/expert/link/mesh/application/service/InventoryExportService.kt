package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryExportFormat
import org.expert.link.mesh.domain.model.inventory.InventoryExportSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryExportStatus
import org.expert.link.mesh.domain.model.inventory.InventoryExportTask
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.port.repository.InventoryExportRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySessionRepositoryPort

class InventoryExportService(
    private val localProfileService: LocalProfileService,
    private val sessionRepositoryPort: InventorySessionRepositoryPort,
    private val exportRepositoryPort: InventoryExportRepositoryPort,
    private val rbacService: InventoryRbacService,
    private val inventoryEventService: InventoryEventService,
    private val inventorySyncService: InventorySyncService,
) {
    suspend fun requestExport(
        organizationId: String,
        sessionId: String,
        format: InventoryExportFormat,
    ): InventoryExportTask {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.EXPORT_REQUEST)
        requireNotNull(sessionRepositoryPort.findBySessionId(sessionId)) { "Session $sessionId not found" }
        val now = now()
        val task = InventoryExportTask(
            exportTaskId = newId("export"),
            organizationId = organizationId,
            sessionId = sessionId,
            requestedByPeerId = localProfile.peerId,
            format = format,
            status = InventoryExportStatus.REQUESTED,
            createdAt = now,
            updatedAt = now,
        )
        exportRepositoryPort.save(task)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.EXPORT,
                entityId = task.exportTaskId,
                eventType = InventoryEventType.EXPORT_REQUESTED,
                actorPeerId = localProfile.peerId,
                payload = InventoryExportSnapshot(task),
                sessionId = sessionId,
            ),
        )
        return task
    }

    suspend fun updateExportStatus(
        exportTaskId: String,
        status: InventoryExportStatus,
        resultDescriptor: FileDescriptor?,
        errorMessage: String?,
    ): InventoryExportTask? {
        val task = exportRepositoryPort.findByExportId(exportTaskId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(task.organizationId, localProfile.peerId, InventoryPermission.EXPORT_MANAGE)
        val updated = task.copy(
            status = status,
            updatedAt = now(),
            resultDescriptor = resultDescriptor ?: task.resultDescriptor,
            errorMessage = errorMessage,
        )
        exportRepositoryPort.save(updated)
        val eventType = when (status) {
            InventoryExportStatus.COMPLETED -> InventoryEventType.EXPORT_COMPLETED
            InventoryExportStatus.FAILED -> InventoryEventType.EXPORT_FAILED
            else -> InventoryEventType.UPDATED
        }
        publish(
            inventoryEventService.recordEvent(
                organizationId = task.organizationId,
                entityType = InventoryEntityType.EXPORT,
                entityId = task.exportTaskId,
                eventType = eventType,
                actorPeerId = localProfile.peerId,
                payload = InventoryExportSnapshot(updated),
                sessionId = task.sessionId,
            ),
        )
        return updated
    }

    suspend fun listExports(organizationId: String): List<InventoryExportTask> =
        exportRepositoryPort.listByOrganization(organizationId)

    private suspend fun publish(event: org.expert.link.mesh.domain.model.inventory.InventoryEvent) {
        inventorySyncService.broadcastEvent(event)
    }
}
