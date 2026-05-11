package org.expert.link.mesh.domain.port.repository

import kotlinx.datetime.Instant
import org.expert.link.mesh.domain.model.inventory.InventoryAlertEvent
import org.expert.link.mesh.domain.model.inventory.InventoryAttachment
import org.expert.link.mesh.domain.model.inventory.InventoryCategory
import org.expert.link.mesh.domain.model.inventory.InventoryCategoryTemplate
import org.expert.link.mesh.domain.model.inventory.InventoryChangeLog
import org.expert.link.mesh.domain.model.inventory.InventoryComment
import org.expert.link.mesh.domain.model.inventory.InventoryConflict
import org.expert.link.mesh.domain.model.inventory.InventoryCostCenter
import org.expert.link.mesh.domain.model.inventory.InventoryDeadlineRule
import org.expert.link.mesh.domain.model.inventory.InventoryDepartment
import org.expert.link.mesh.domain.model.inventory.InventoryEvent
import org.expert.link.mesh.domain.model.inventory.InventoryExportTask
import org.expert.link.mesh.domain.model.inventory.InventoryFundingSource
import org.expert.link.mesh.domain.model.inventory.InventoryIncident
import org.expert.link.mesh.domain.model.inventory.InventoryItem
import org.expert.link.mesh.domain.model.inventory.InventoryLocation
import org.expert.link.mesh.domain.model.inventory.InventoryOwner
import org.expert.link.mesh.domain.model.inventory.InventoryQrCode
import org.expert.link.mesh.domain.model.inventory.InventoryReminder
import org.expert.link.mesh.domain.model.inventory.InventoryReview
import org.expert.link.mesh.domain.model.inventory.InventoryRevision
import org.expert.link.mesh.domain.model.inventory.InventoryRuleThreshold
import org.expert.link.mesh.domain.model.inventory.InventoryScanEvent
import org.expert.link.mesh.domain.model.inventory.InventorySession
import org.expert.link.mesh.domain.model.inventory.InventorySessionMember
import org.expert.link.mesh.domain.model.inventory.InventorySessionStatus
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.domain.model.inventory.InventorySubcategory
import org.expert.link.mesh.domain.model.inventory.InventorySupplier
import org.expert.link.mesh.domain.model.inventory.InventoryTag
import org.expert.link.mesh.domain.model.inventory.InventoryAttributeDefinition
import org.expert.link.mesh.domain.model.inventory.InventoryCodeBinding
import org.expert.link.mesh.domain.model.inventory.InventoryCode
import org.expert.link.mesh.domain.model.inventory.InventoryDashboardSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLegalHolder
import org.expert.link.mesh.domain.model.inventory.InventoryLabel
import org.expert.link.mesh.domain.model.inventory.InventoryLabelTemplate
import org.expert.link.mesh.domain.model.inventory.Organization
import org.expert.link.mesh.domain.model.inventory.OrganizationMember
import org.expert.link.mesh.domain.model.inventory.InventoryPrintTask
import org.expert.link.mesh.domain.model.inventory.Role

interface OrganizationRepositoryPort {
    suspend fun save(organization: Organization): Organization
    suspend fun findByOrganizationId(organizationId: String): Organization?
    suspend fun list(): List<Organization>
}

interface OrganizationMemberRepositoryPort {
    suspend fun save(member: OrganizationMember): OrganizationMember
    suspend fun findByOrganizationAndPeer(organizationId: String, peerId: String): OrganizationMember?
    suspend fun listByOrganization(organizationId: String): List<OrganizationMember>
    suspend fun listByPeer(peerId: String): List<OrganizationMember>
    suspend fun remove(organizationId: String, peerId: String)
}

interface RoleRepositoryPort {
    suspend fun save(role: Role): Role
    suspend fun findByRoleId(roleId: String): Role?
    suspend fun findByName(organizationId: String, name: String): Role?
    suspend fun listByOrganization(organizationId: String): List<Role>
    suspend fun remove(roleId: String)
}

interface InventoryCategoryRepositoryPort {
    suspend fun save(category: InventoryCategory): InventoryCategory
    suspend fun findByCategoryId(categoryId: String): InventoryCategory?
    suspend fun listByOrganization(organizationId: String): List<InventoryCategory>
}

interface InventorySubcategoryRepositoryPort {
    suspend fun save(subcategory: InventorySubcategory): InventorySubcategory
    suspend fun findBySubcategoryId(subcategoryId: String): InventorySubcategory?
    suspend fun listByCategory(categoryId: String): List<InventorySubcategory>
    suspend fun listByOrganization(organizationId: String): List<InventorySubcategory>
}

interface InventoryTagRepositoryPort {
    suspend fun save(tag: InventoryTag): InventoryTag
    suspend fun findByTagId(tagId: String): InventoryTag?
    suspend fun listByOrganization(organizationId: String): List<InventoryTag>
}

interface InventoryAttributeDefinitionRepositoryPort {
    suspend fun save(definition: InventoryAttributeDefinition): InventoryAttributeDefinition
    suspend fun findByAttributeId(attributeId: String): InventoryAttributeDefinition?
    suspend fun listByOrganization(organizationId: String): List<InventoryAttributeDefinition>
}

interface InventoryCategoryTemplateRepositoryPort {
    suspend fun save(template: InventoryCategoryTemplate): InventoryCategoryTemplate
    suspend fun findByTemplateId(templateId: String): InventoryCategoryTemplate?
    suspend fun listByOrganization(organizationId: String): List<InventoryCategoryTemplate>
    suspend fun listByCategory(categoryId: String): List<InventoryCategoryTemplate>
}

interface InventoryLocationRepositoryPort {
    suspend fun save(location: InventoryLocation): InventoryLocation
    suspend fun findByLocationId(locationId: String): InventoryLocation?
    suspend fun listByOrganization(organizationId: String): List<InventoryLocation>
}

interface InventoryOwnerRepositoryPort {
    suspend fun save(owner: InventoryOwner): InventoryOwner
    suspend fun findByOwnerId(ownerId: String): InventoryOwner?
    suspend fun listByOrganization(organizationId: String): List<InventoryOwner>
}

interface InventoryDepartmentRepositoryPort {
    suspend fun save(department: InventoryDepartment): InventoryDepartment
    suspend fun findByDepartmentId(departmentId: String): InventoryDepartment?
    suspend fun listByOrganization(organizationId: String): List<InventoryDepartment>
}

interface InventoryCostCenterRepositoryPort {
    suspend fun save(costCenter: InventoryCostCenter): InventoryCostCenter
    suspend fun findByCostCenterId(costCenterId: String): InventoryCostCenter?
    suspend fun listByOrganization(organizationId: String): List<InventoryCostCenter>
}

interface InventoryLegalHolderRepositoryPort {
    suspend fun save(holder: InventoryLegalHolder): InventoryLegalHolder
    suspend fun findByLegalHolderId(legalHolderId: String): InventoryLegalHolder?
    suspend fun listByOrganization(organizationId: String): List<InventoryLegalHolder>
}

interface InventorySupplierRepositoryPort {
    suspend fun save(supplier: InventorySupplier): InventorySupplier
    suspend fun findBySupplierId(supplierId: String): InventorySupplier?
    suspend fun listByOrganization(organizationId: String): List<InventorySupplier>
}

interface InventoryFundingSourceRepositoryPort {
    suspend fun save(source: InventoryFundingSource): InventoryFundingSource
    suspend fun findByFundingSourceId(fundingSourceId: String): InventoryFundingSource?
    suspend fun listByOrganization(organizationId: String): List<InventoryFundingSource>
}

interface InventoryItemRepositoryPort {
    suspend fun save(item: InventoryItem): InventoryItem
    suspend fun findByInventoryItemId(itemId: String): InventoryItem?
    suspend fun findByInventoryNumber(organizationId: String, inventoryNumber: String): InventoryItem?
    suspend fun findByQrCode(qrCode: String): InventoryItem?
    suspend fun findByBarcode(barcode: String): InventoryItem?
    suspend fun listByOrganization(organizationId: String): List<InventoryItem>
    suspend fun listByStatus(organizationId: String, status: InventoryStatus): List<InventoryItem>
    suspend fun listUpdatedSince(organizationId: String, since: Instant): List<InventoryItem>
}

interface InventorySessionRepositoryPort {
    suspend fun save(session: InventorySession): InventorySession
    suspend fun findBySessionId(sessionId: String): InventorySession?
    suspend fun listByOrganization(organizationId: String): List<InventorySession>
    suspend fun listByStatus(organizationId: String, status: InventorySessionStatus): List<InventorySession>
    suspend fun listUpdatedSince(organizationId: String, since: Instant): List<InventorySession>
}

interface InventorySessionMemberRepositoryPort {
    suspend fun save(member: InventorySessionMember): InventorySessionMember
    suspend fun listBySession(sessionId: String): List<InventorySessionMember>
    suspend fun listByPeer(peerId: String): List<InventorySessionMember>
    suspend fun remove(sessionId: String, peerId: String)
}

interface InventoryReviewRepositoryPort {
    suspend fun save(review: InventoryReview): InventoryReview
    suspend fun listByItem(inventoryItemId: String): List<InventoryReview>
    suspend fun listBySession(sessionId: String): List<InventoryReview>
}

interface InventoryCommentRepositoryPort {
    suspend fun save(comment: InventoryComment): InventoryComment
    suspend fun findByCommentId(commentId: String): InventoryComment?
    suspend fun listByItem(inventoryItemId: String): List<InventoryComment>
    suspend fun listBySession(sessionId: String): List<InventoryComment>
}

interface InventoryAttachmentRepositoryPort {
    suspend fun save(attachment: InventoryAttachment): InventoryAttachment
    suspend fun findByAttachmentId(attachmentId: String): InventoryAttachment?
    suspend fun listByItem(inventoryItemId: String): List<InventoryAttachment>
    suspend fun listBySession(sessionId: String): List<InventoryAttachment>
}

interface InventoryIncidentRepositoryPort {
    suspend fun save(incident: InventoryIncident): InventoryIncident
    suspend fun findByIncidentId(incidentId: String): InventoryIncident?
    suspend fun listByOrganization(organizationId: String): List<InventoryIncident>
    suspend fun listByItem(inventoryItemId: String): List<InventoryIncident>
    suspend fun listBySession(sessionId: String): List<InventoryIncident>
}

interface InventoryAlertRepositoryPort {
    suspend fun save(alert: InventoryAlertEvent): InventoryAlertEvent
    suspend fun findByAlertId(alertId: String): InventoryAlertEvent?
    suspend fun listByOrganization(organizationId: String): List<InventoryAlertEvent>
}

interface InventoryReminderRepositoryPort {
    suspend fun save(reminder: InventoryReminder): InventoryReminder
    suspend fun findByReminderId(reminderId: String): InventoryReminder?
    suspend fun listByOrganization(organizationId: String): List<InventoryReminder>
    suspend fun listByItem(inventoryItemId: String): List<InventoryReminder>
}

interface InventoryThresholdRuleRepositoryPort {
    suspend fun save(rule: InventoryRuleThreshold): InventoryRuleThreshold
    suspend fun findByRuleId(ruleId: String): InventoryRuleThreshold?
    suspend fun listByOrganization(organizationId: String): List<InventoryRuleThreshold>
}

interface InventoryDeadlineRuleRepositoryPort {
    suspend fun save(rule: InventoryDeadlineRule): InventoryDeadlineRule
    suspend fun findByRuleId(ruleId: String): InventoryDeadlineRule?
    suspend fun listByOrganization(organizationId: String): List<InventoryDeadlineRule>
}

interface InventoryEventRepositoryPort {
    suspend fun save(event: InventoryEvent): InventoryEvent
    suspend fun findByEventId(eventId: String): InventoryEvent?
    suspend fun listByOrganization(organizationId: String): List<InventoryEvent>
    suspend fun listByOrganizationSince(organizationId: String, since: Instant): List<InventoryEvent>
    suspend fun listByOrganizationSinceSequence(organizationId: String, sequence: Long): List<InventoryEvent>
    suspend fun listByEntity(entityId: String): List<InventoryEvent>
    suspend fun nextSequence(organizationId: String): Long
    suspend fun latestSequence(organizationId: String): Long
}

interface InventoryExportRepositoryPort {
    suspend fun save(task: InventoryExportTask): InventoryExportTask
    suspend fun findByExportId(exportTaskId: String): InventoryExportTask?
    suspend fun listByOrganization(organizationId: String): List<InventoryExportTask>
    suspend fun listBySession(sessionId: String): List<InventoryExportTask>
}

interface InventoryQrCodeRepositoryPort {
    suspend fun save(qrCode: InventoryQrCode): InventoryQrCode
    suspend fun findByCode(code: String): InventoryQrCode?
    suspend fun findByBarcode(barcode: String): InventoryQrCode?
    suspend fun listByItem(inventoryItemId: String): List<InventoryQrCode>
}

interface InventoryCodeBindingRepositoryPort {
    suspend fun save(binding: InventoryCodeBinding): InventoryCodeBinding
    suspend fun findByCode(codeValue: String): InventoryCodeBinding?
    suspend fun listByItem(inventoryItemId: String): List<InventoryCodeBinding>
}

interface InventoryCodeRepositoryPort {
    suspend fun save(code: InventoryCode): InventoryCode
    suspend fun findByCodeId(codeId: String): InventoryCode?
    suspend fun findByRawValue(rawValue: String): InventoryCode?
    suspend fun listByItem(inventoryItemId: String): List<InventoryCode>
    suspend fun listByOrganization(organizationId: String): List<InventoryCode>
}

interface InventoryLabelTemplateRepositoryPort {
    suspend fun save(template: InventoryLabelTemplate): InventoryLabelTemplate
    suspend fun findByTemplateId(templateId: String): InventoryLabelTemplate?
    suspend fun listByOrganization(organizationId: String): List<InventoryLabelTemplate>
}

interface InventoryLabelRepositoryPort {
    suspend fun save(label: InventoryLabel): InventoryLabel
    suspend fun findByLabelId(labelId: String): InventoryLabel?
    suspend fun listByItem(inventoryItemId: String): List<InventoryLabel>
}

interface InventoryPrintTaskRepositoryPort {
    suspend fun save(task: InventoryPrintTask): InventoryPrintTask
    suspend fun findByPrintTaskId(printTaskId: String): InventoryPrintTask?
    suspend fun listByOrganization(organizationId: String): List<InventoryPrintTask>
    suspend fun listByItem(inventoryItemId: String): List<InventoryPrintTask>
}

interface InventoryScanEventRepositoryPort {
    suspend fun save(event: InventoryScanEvent): InventoryScanEvent
    suspend fun listByOrganization(organizationId: String): List<InventoryScanEvent>
    suspend fun listByItem(inventoryItemId: String): List<InventoryScanEvent>
}

interface InventoryChangeLogRepositoryPort {
    suspend fun save(changeLog: InventoryChangeLog): InventoryChangeLog
    suspend fun listByOrganization(organizationId: String): List<InventoryChangeLog>
    suspend fun listByEntity(entityId: String): List<InventoryChangeLog>
}

interface InventoryDashboardRepositoryPort {
    suspend fun save(snapshot: InventoryDashboardSnapshot): InventoryDashboardSnapshot
    suspend fun latestSnapshot(organizationId: String): InventoryDashboardSnapshot?
    suspend fun listByOrganization(organizationId: String): List<InventoryDashboardSnapshot>
}

interface InventoryRevisionRepositoryPort {
    suspend fun save(revision: InventoryRevision): InventoryRevision
    suspend fun findByEntity(entityId: String): InventoryRevision?
    suspend fun listByOrganization(organizationId: String): List<InventoryRevision>
}

interface InventoryConflictRepositoryPort {
    suspend fun save(conflict: InventoryConflict): InventoryConflict
    suspend fun findByConflictId(conflictId: String): InventoryConflict?
    suspend fun listByOrganization(organizationId: String): List<InventoryConflict>
}
