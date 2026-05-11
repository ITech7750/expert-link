package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.domain.model.inventory.InventoryAttachmentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryAttributeDefinitionSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCategoryTemplateSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryChangeLogSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCodeSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCodeBindingSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCommentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryConflictSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryConflict
import org.expert.link.mesh.domain.model.inventory.InventoryCostCenterSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryDeadlineRuleSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryDepartmentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryFundingSourceSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryIncidentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryAlertSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryDashboardSnapshotPayload
import org.expert.link.mesh.domain.model.inventory.InventoryExportSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryItemSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLabelSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLabelTemplateSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLegalHolderSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryMemberSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryOrganizationSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryOwnerSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryPrintTaskSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryQrSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryReminderSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryRevisionSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryReviewSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryRoleSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryRuleThresholdSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryScanEventSnapshot
import org.expert.link.mesh.domain.model.inventory.InventorySessionSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.domain.model.inventory.InventoryCategorySnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLocationSnapshot
import org.expert.link.mesh.domain.model.inventory.InventorySubcategorySnapshot
import org.expert.link.mesh.domain.model.inventory.InventorySupplierSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryTagSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEvent
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.port.repository.InventoryAttachmentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryAlertRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryAttributeDefinitionRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCategoryTemplateRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCategoryRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryChangeLogRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCommentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCodeBindingRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryConflictRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCostCenterRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryDeadlineRuleRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryDepartmentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryFundingSourceRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryIncidentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryExportRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryDashboardRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCodeRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLabelRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLabelTemplateRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLegalHolderRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLocationRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryOwnerRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryPrintTaskRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryQrCodeRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryReminderRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryRevisionRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryReviewRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryThresholdRuleRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryScanEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySubcategoryRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySupplierRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryTagRepositoryPort
import org.expert.link.mesh.domain.port.repository.OrganizationMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.OrganizationRepositoryPort
import org.expert.link.mesh.domain.port.repository.RoleRepositoryPort

class InventoryEventApplier(
    private val localProfileService: LocalProfileService,
    private val organizationRepositoryPort: OrganizationRepositoryPort,
    private val organizationMemberRepositoryPort: OrganizationMemberRepositoryPort,
    private val roleRepositoryPort: RoleRepositoryPort,
    private val categoryRepositoryPort: InventoryCategoryRepositoryPort,
    private val subcategoryRepositoryPort: InventorySubcategoryRepositoryPort,
    private val tagRepositoryPort: InventoryTagRepositoryPort,
    private val attributeDefinitionRepositoryPort: InventoryAttributeDefinitionRepositoryPort,
    private val categoryTemplateRepositoryPort: InventoryCategoryTemplateRepositoryPort,
    private val locationRepositoryPort: InventoryLocationRepositoryPort,
    private val ownerRepositoryPort: InventoryOwnerRepositoryPort,
    private val departmentRepositoryPort: InventoryDepartmentRepositoryPort,
    private val costCenterRepositoryPort: InventoryCostCenterRepositoryPort,
    private val legalHolderRepositoryPort: InventoryLegalHolderRepositoryPort,
    private val supplierRepositoryPort: InventorySupplierRepositoryPort,
    private val fundingSourceRepositoryPort: InventoryFundingSourceRepositoryPort,
    private val itemRepositoryPort: InventoryItemRepositoryPort,
    private val sessionRepositoryPort: InventorySessionRepositoryPort,
    private val reviewRepositoryPort: InventoryReviewRepositoryPort,
    private val commentRepositoryPort: InventoryCommentRepositoryPort,
    private val attachmentRepositoryPort: InventoryAttachmentRepositoryPort,
    private val incidentRepositoryPort: InventoryIncidentRepositoryPort,
    private val alertRepositoryPort: InventoryAlertRepositoryPort,
    private val reminderRepositoryPort: InventoryReminderRepositoryPort,
    private val thresholdRuleRepositoryPort: InventoryThresholdRuleRepositoryPort,
    private val deadlineRuleRepositoryPort: InventoryDeadlineRuleRepositoryPort,
    private val changeLogRepositoryPort: InventoryChangeLogRepositoryPort,
    private val dashboardRepositoryPort: InventoryDashboardRepositoryPort,
    private val codeRepositoryPort: InventoryCodeRepositoryPort,
    private val codeBindingRepositoryPort: InventoryCodeBindingRepositoryPort,
    private val labelTemplateRepositoryPort: InventoryLabelTemplateRepositoryPort,
    private val labelRepositoryPort: InventoryLabelRepositoryPort,
    private val printTaskRepositoryPort: InventoryPrintTaskRepositoryPort,
    private val scanEventRepositoryPort: InventoryScanEventRepositoryPort,
    private val revisionRepositoryPort: InventoryRevisionRepositoryPort,
    private val conflictRepositoryPort: InventoryConflictRepositoryPort,
    private val exportRepositoryPort: InventoryExportRepositoryPort,
    private val qrCodeRepositoryPort: InventoryQrCodeRepositoryPort,
    private val eventRepositoryPort: InventoryEventRepositoryPort,
    private val inventoryEventService: InventoryEventService,
) {
    suspend fun apply(event: InventoryEvent): Boolean {
        if (eventRepositoryPort.findByEventId(event.eventId) != null) {
            return false
        }
        when (val payload = event.payload) {
            is InventoryOrganizationSnapshot -> organizationRepositoryPort.save(payload.organization)
            is InventoryMemberSnapshot -> organizationMemberRepositoryPort.save(payload.member)
            is InventoryRoleSnapshot -> roleRepositoryPort.save(payload.role)
            is InventoryCategorySnapshot -> categoryRepositoryPort.save(payload.category)
            is InventorySubcategorySnapshot -> subcategoryRepositoryPort.save(payload.subcategory)
            is InventoryTagSnapshot -> tagRepositoryPort.save(payload.tag)
            is InventoryAttributeDefinitionSnapshot -> attributeDefinitionRepositoryPort.save(payload.definition)
            is InventoryCategoryTemplateSnapshot -> categoryTemplateRepositoryPort.save(payload.template)
            is InventoryLocationSnapshot -> locationRepositoryPort.save(payload.location)
            is InventoryOwnerSnapshot -> ownerRepositoryPort.save(payload.owner)
            is InventoryDepartmentSnapshot -> departmentRepositoryPort.save(payload.department)
            is InventoryCostCenterSnapshot -> costCenterRepositoryPort.save(payload.costCenter)
            is InventoryLegalHolderSnapshot -> legalHolderRepositoryPort.save(payload.legalHolder)
            is InventorySupplierSnapshot -> supplierRepositoryPort.save(payload.supplier)
            is InventoryFundingSourceSnapshot -> fundingSourceRepositoryPort.save(payload.fundingSource)
            is InventoryItemSnapshot -> applyItem(payload.item, event)
            is InventorySessionSnapshot -> applySession(payload.session, event)
            is InventoryReviewSnapshot -> reviewRepositoryPort.save(payload.review)
            is InventoryCommentSnapshot -> applyComment(payload, event)
            is InventoryAttachmentSnapshot -> applyAttachment(payload, event)
            is InventoryIncidentSnapshot -> incidentRepositoryPort.save(payload.incident)
            is InventoryAlertSnapshot -> alertRepositoryPort.save(payload.alert)
            is InventoryReminderSnapshot -> reminderRepositoryPort.save(payload.reminder)
            is InventoryRuleThresholdSnapshot -> thresholdRuleRepositoryPort.save(payload.rule)
            is InventoryDeadlineRuleSnapshot -> deadlineRuleRepositoryPort.save(payload.rule)
            is InventoryChangeLogSnapshot -> changeLogRepositoryPort.save(payload.changeLog)
            is InventoryDashboardSnapshotPayload -> dashboardRepositoryPort.save(payload.snapshot)
            is InventoryCodeSnapshot -> codeRepositoryPort.save(payload.code)
            is InventoryCodeBindingSnapshot -> codeBindingRepositoryPort.save(payload.binding)
            is InventoryLabelTemplateSnapshot -> labelTemplateRepositoryPort.save(payload.template)
            is InventoryLabelSnapshot -> labelRepositoryPort.save(payload.label)
            is InventoryPrintTaskSnapshot -> printTaskRepositoryPort.save(payload.task)
            is InventoryScanEventSnapshot -> scanEventRepositoryPort.save(payload.event)
            is InventoryRevisionSnapshot -> revisionRepositoryPort.save(payload.revision)
            is InventoryConflictSnapshot -> conflictRepositoryPort.save(payload.conflict)
            is InventoryExportSnapshot -> exportRepositoryPort.save(payload.exportTask)
            is InventoryQrSnapshot -> qrCodeRepositoryPort.save(payload.qrCode)
        }
        eventRepositoryPort.save(event)
        return true
    }

    private suspend fun applyItem(item: org.expert.link.mesh.domain.model.inventory.InventoryItem, event: InventoryEvent) {
        val current = itemRepositoryPort.findByInventoryItemId(item.inventoryItemId)
        if (current == null) {
            itemRepositoryPort.save(item)
            return
        }
        val eventRevision = event.entityRevision
        if (eventRevision != null && eventRevision <= current.revision) {
            return
        }
        val conflict = event.previousEntityRevision != null && event.previousEntityRevision != current.revision
        val updated = if (conflict) {
            item.copy(
                currentStatus = InventoryStatus.REQUIRES_UPDATE,
                syncStatus = org.expert.link.mesh.domain.model.inventory.InventorySyncStatus.CONFLICTED,
            )
        } else {
            item
        }
        itemRepositoryPort.save(updated)
        if (conflict) {
            recordConflict(
                organizationId = item.organizationId,
                entityType = InventoryEntityType.ITEM,
                entityId = item.inventoryItemId,
                payload = InventoryItemSnapshot(updated),
                note = "Incoming revision ${event.entityRevision} mismatched local ${current.revision}",
                previousRevision = current.revision,
                incomingRevision = event.entityRevision,
            )
        }
    }

    private suspend fun applySession(session: org.expert.link.mesh.domain.model.inventory.InventorySession, event: InventoryEvent) {
        val current = sessionRepositoryPort.findBySessionId(session.sessionId)
        if (current == null) {
            sessionRepositoryPort.save(session)
            return
        }
        val eventRevision = event.entityRevision
        if (eventRevision != null && eventRevision <= current.revision) {
            return
        }
        val conflict = event.previousEntityRevision != null && event.previousEntityRevision != current.revision
        val updated = if (conflict) {
            session.copy(reviewStatus = org.expert.link.mesh.domain.model.inventory.InventorySessionReviewStatus.REJECTED)
        } else {
            session
        }
        sessionRepositoryPort.save(updated)
        if (conflict) {
            recordConflict(
                organizationId = session.organizationId,
                entityType = InventoryEntityType.SESSION,
                entityId = session.sessionId,
                payload = InventorySessionSnapshot(updated),
                note = "Incoming revision ${event.entityRevision} mismatched local ${current.revision}",
                previousRevision = current.revision,
                incomingRevision = event.entityRevision,
            )
        }
    }

    private suspend fun applyComment(payload: InventoryCommentSnapshot, event: InventoryEvent) {
        commentRepositoryPort.save(payload.comment)
        val item = itemRepositoryPort.findByInventoryItemId(payload.comment.inventoryItemId) ?: return
        if (payload.comment.commentId !in item.commentIds) {
            val updated = item.copy(
                commentIds = (item.commentIds + payload.comment.commentId).distinct(),
                updatedAt = now(),
                revision = maxOf(item.revision + 1, event.entityRevision ?: item.revision + 1),
            )
            itemRepositoryPort.save(updated)
        }
    }

    private suspend fun applyAttachment(payload: InventoryAttachmentSnapshot, event: InventoryEvent) {
        attachmentRepositoryPort.save(payload.attachment)
        val item = itemRepositoryPort.findByInventoryItemId(payload.attachment.inventoryItemId) ?: return
        if (payload.attachment.attachmentId !in item.attachmentIds) {
            val updated = item.copy(
                attachmentIds = (item.attachmentIds + payload.attachment.attachmentId).distinct(),
                photoAttachmentIds = if (payload.attachment.attachmentType == org.expert.link.mesh.domain.model.inventory.InventoryAttachmentType.PHOTO) {
                    (item.photoAttachmentIds + payload.attachment.attachmentId).distinct()
                } else {
                    item.photoAttachmentIds
                },
                updatedAt = now(),
                revision = maxOf(item.revision + 1, event.entityRevision ?: item.revision + 1),
            )
            itemRepositoryPort.save(updated)
        }
    }

    private suspend fun recordConflict(
        organizationId: String,
        entityType: InventoryEntityType,
        entityId: String,
        payload: org.expert.link.mesh.domain.model.inventory.InventoryEventPayload,
        note: String,
        previousRevision: Long,
        incomingRevision: Long?,
    ) {
        val localProfile = localProfileService.require()
        val conflict = InventoryConflict(
            conflictId = newId("conflict"),
            organizationId = organizationId,
            entityType = entityType,
            entityId = entityId,
            localRevision = previousRevision,
            incomingRevision = incomingRevision ?: previousRevision,
            detectedAt = now(),
            status = org.expert.link.mesh.domain.model.inventory.InventorySyncStatus.CONFLICTED,
            note = note,
        )
        conflictRepositoryPort.save(conflict)
        inventoryEventService.recordEvent(
            organizationId = organizationId,
            entityType = InventoryEntityType.CONFLICT,
            entityId = conflict.conflictId,
            eventType = InventoryEventType.CONFLICT_DETECTED,
            actorPeerId = localProfile.peerId,
            payload = InventoryConflictSnapshot(conflict),
            previousEntityRevision = previousRevision,
            conflict = true,
            notes = note,
        )
    }
}
