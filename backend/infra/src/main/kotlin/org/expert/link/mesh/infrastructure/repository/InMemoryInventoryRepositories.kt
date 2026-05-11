package org.expert.link.mesh.infrastructure.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import org.expert.link.mesh.domain.model.inventory.InventoryAttachment
import org.expert.link.mesh.domain.model.inventory.InventoryAlertEvent
import org.expert.link.mesh.domain.model.inventory.InventoryAttributeDefinition
import org.expert.link.mesh.domain.model.inventory.InventoryCategory
import org.expert.link.mesh.domain.model.inventory.InventoryCategoryTemplate
import org.expert.link.mesh.domain.model.inventory.InventoryChangeLog
import org.expert.link.mesh.domain.model.inventory.InventoryCode
import org.expert.link.mesh.domain.model.inventory.InventoryCodeBinding
import org.expert.link.mesh.domain.model.inventory.InventoryComment
import org.expert.link.mesh.domain.model.inventory.InventoryConflict
import org.expert.link.mesh.domain.model.inventory.InventoryCostCenter
import org.expert.link.mesh.domain.model.inventory.InventoryDeadlineRule
import org.expert.link.mesh.domain.model.inventory.InventoryDepartment
import org.expert.link.mesh.domain.model.inventory.InventoryFundingSource
import org.expert.link.mesh.domain.model.inventory.InventoryEvent
import org.expert.link.mesh.domain.model.inventory.InventoryExportTask
import org.expert.link.mesh.domain.model.inventory.InventoryIncident
import org.expert.link.mesh.domain.model.inventory.InventoryItem
import org.expert.link.mesh.domain.model.inventory.InventoryLegalHolder
import org.expert.link.mesh.domain.model.inventory.InventoryLocation
import org.expert.link.mesh.domain.model.inventory.InventoryOwner
import org.expert.link.mesh.domain.model.inventory.InventoryQrCode
import org.expert.link.mesh.domain.model.inventory.InventoryReminder
import org.expert.link.mesh.domain.model.inventory.InventoryReview
import org.expert.link.mesh.domain.model.inventory.InventoryRuleThreshold
import org.expert.link.mesh.domain.model.inventory.InventoryScanEvent
import org.expert.link.mesh.domain.model.inventory.InventorySession
import org.expert.link.mesh.domain.model.inventory.InventorySessionMember
import org.expert.link.mesh.domain.model.inventory.InventorySessionStatus
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.domain.model.inventory.InventorySubcategory
import org.expert.link.mesh.domain.model.inventory.InventorySupplier
import org.expert.link.mesh.domain.model.inventory.InventoryTag
import org.expert.link.mesh.domain.model.inventory.InventoryDashboardSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLabel
import org.expert.link.mesh.domain.model.inventory.InventoryLabelTemplate
import org.expert.link.mesh.domain.model.inventory.InventoryRevision
import org.expert.link.mesh.domain.model.inventory.InventoryPrintTask
import org.expert.link.mesh.domain.model.inventory.Organization
import org.expert.link.mesh.domain.model.inventory.OrganizationMember
import org.expert.link.mesh.domain.model.inventory.Role
import org.expert.link.mesh.domain.port.repository.InventoryAttachmentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryAlertRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryAttributeDefinitionRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCategoryTemplateRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCategoryRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryChangeLogRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCommentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCodeBindingRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCodeRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryConflictRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCostCenterRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryDeadlineRuleRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryDepartmentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryFundingSourceRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryIncidentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryExportRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryDashboardRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLabelRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLabelTemplateRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLegalHolderRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLocationRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryOwnerRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryQrCodeRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryReminderRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryRevisionRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryReviewRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryThresholdRuleRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryScanEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryPrintTaskRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySessionMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySubcategoryRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySupplierRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryTagRepositoryPort
import org.expert.link.mesh.domain.port.repository.OrganizationMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.OrganizationRepositoryPort
import org.expert.link.mesh.domain.port.repository.RoleRepositoryPort

class InMemoryOrganizationRepositoryAdapter : OrganizationRepositoryPort {
    private val mutex = Mutex()
    private val organizations = linkedMapOf<String, Organization>()

    override suspend fun save(organization: Organization): Organization = mutex.withLock {
        organizations[organization.organizationId] = organization
        organization
    }

    override suspend fun findByOrganizationId(organizationId: String): Organization? = mutex.withLock {
        organizations[organizationId]
    }

    override suspend fun list(): List<Organization> = mutex.withLock { organizations.values.toList() }
}

class InMemoryOrganizationMemberRepositoryAdapter : OrganizationMemberRepositoryPort {
    private val mutex = Mutex()
    private val members = linkedMapOf<String, OrganizationMember>()

    override suspend fun save(member: OrganizationMember): OrganizationMember = mutex.withLock {
        members[key(member.organizationId, member.peerId)] = member
        member
    }

    override suspend fun findByOrganizationAndPeer(organizationId: String, peerId: String): OrganizationMember? = mutex.withLock {
        members[key(organizationId, peerId)]
    }

    override suspend fun listByOrganization(organizationId: String): List<OrganizationMember> = mutex.withLock {
        members.values.filter { it.organizationId == organizationId }
    }

    override suspend fun listByPeer(peerId: String): List<OrganizationMember> = mutex.withLock {
        members.values.filter { it.peerId == peerId }
    }

    override suspend fun remove(organizationId: String, peerId: String) {
        mutex.withLock { members.remove(key(organizationId, peerId)) }
    }

    private fun key(organizationId: String, peerId: String): String = "$organizationId|$peerId"
}

class InMemoryRoleRepositoryAdapter : RoleRepositoryPort {
    private val mutex = Mutex()
    private val roles = linkedMapOf<String, Role>()

    override suspend fun save(role: Role): Role = mutex.withLock {
        roles[role.roleId] = role
        role
    }

    override suspend fun findByRoleId(roleId: String): Role? = mutex.withLock { roles[roleId] }

    override suspend fun findByName(organizationId: String, name: String): Role? = mutex.withLock {
        roles.values.firstOrNull { it.organizationId == organizationId && it.name == name }
    }

    override suspend fun listByOrganization(organizationId: String): List<Role> = mutex.withLock {
        roles.values.filter { it.organizationId == organizationId }
    }

    override suspend fun remove(roleId: String) {
        mutex.withLock { roles.remove(roleId) }
    }
}

class InMemoryInventoryCategoryRepositoryAdapter : InventoryCategoryRepositoryPort {
    private val mutex = Mutex()
    private val categories = linkedMapOf<String, InventoryCategory>()

    override suspend fun save(category: InventoryCategory): InventoryCategory = mutex.withLock {
        categories[category.categoryId] = category
        category
    }

    override suspend fun findByCategoryId(categoryId: String): InventoryCategory? = mutex.withLock { categories[categoryId] }

    override suspend fun listByOrganization(organizationId: String): List<InventoryCategory> = mutex.withLock {
        categories.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryLocationRepositoryAdapter : InventoryLocationRepositoryPort {
    private val mutex = Mutex()
    private val locations = linkedMapOf<String, InventoryLocation>()

    override suspend fun save(location: InventoryLocation): InventoryLocation = mutex.withLock {
        locations[location.locationId] = location
        location
    }

    override suspend fun findByLocationId(locationId: String): InventoryLocation? = mutex.withLock { locations[locationId] }

    override suspend fun listByOrganization(organizationId: String): List<InventoryLocation> = mutex.withLock {
        locations.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryItemRepositoryAdapter : InventoryItemRepositoryPort {
    private val mutex = Mutex()
    private val items = linkedMapOf<String, InventoryItem>()

    override suspend fun save(item: InventoryItem): InventoryItem = mutex.withLock {
        items[item.inventoryItemId] = item
        item
    }

    override suspend fun findByInventoryItemId(itemId: String): InventoryItem? = mutex.withLock { items[itemId] }

    override suspend fun findByInventoryNumber(organizationId: String, inventoryNumber: String): InventoryItem? = mutex.withLock {
        items.values.firstOrNull { it.organizationId == organizationId && it.inventoryNumber == inventoryNumber }
    }

    override suspend fun findByQrCode(qrCode: String): InventoryItem? = mutex.withLock {
        items.values.firstOrNull { it.qrCode == qrCode }
    }

    override suspend fun findByBarcode(barcode: String): InventoryItem? = mutex.withLock {
        items.values.firstOrNull { it.barcode == barcode }
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryItem> = mutex.withLock {
        items.values.filter { it.organizationId == organizationId }
    }

    override suspend fun listByStatus(organizationId: String, status: InventoryStatus): List<InventoryItem> = mutex.withLock {
        items.values.filter { it.organizationId == organizationId && it.currentStatus == status }
    }

    override suspend fun listUpdatedSince(organizationId: String, since: Instant): List<InventoryItem> = mutex.withLock {
        items.values.filter { it.organizationId == organizationId && it.updatedAt >= since }
    }
}

class InMemoryInventorySessionRepositoryAdapter : InventorySessionRepositoryPort {
    private val mutex = Mutex()
    private val sessions = linkedMapOf<String, InventorySession>()

    override suspend fun save(session: InventorySession): InventorySession = mutex.withLock {
        sessions[session.sessionId] = session
        session
    }

    override suspend fun findBySessionId(sessionId: String): InventorySession? = mutex.withLock { sessions[sessionId] }

    override suspend fun listByOrganization(organizationId: String): List<InventorySession> = mutex.withLock {
        sessions.values.filter { it.organizationId == organizationId }
    }

    override suspend fun listByStatus(organizationId: String, status: InventorySessionStatus): List<InventorySession> = mutex.withLock {
        sessions.values.filter { it.organizationId == organizationId && it.status == status }
    }

    override suspend fun listUpdatedSince(organizationId: String, since: Instant): List<InventorySession> = mutex.withLock {
        sessions.values.filter { it.organizationId == organizationId && it.updatedAt >= since }
    }
}

class InMemoryInventorySessionMemberRepositoryAdapter : InventorySessionMemberRepositoryPort {
    private val mutex = Mutex()
    private val members = linkedMapOf<String, InventorySessionMember>()

    override suspend fun save(member: InventorySessionMember): InventorySessionMember = mutex.withLock {
        members[key(member.sessionId, member.peerId)] = member
        member
    }

    override suspend fun listBySession(sessionId: String): List<InventorySessionMember> = mutex.withLock {
        members.values.filter { it.sessionId == sessionId }
    }

    override suspend fun listByPeer(peerId: String): List<InventorySessionMember> = mutex.withLock {
        members.values.filter { it.peerId == peerId }
    }

    override suspend fun remove(sessionId: String, peerId: String) {
        mutex.withLock { members.remove(key(sessionId, peerId)) }
    }

    private fun key(sessionId: String, peerId: String): String = "$sessionId|$peerId"
}

class InMemoryInventoryReviewRepositoryAdapter : InventoryReviewRepositoryPort {
    private val mutex = Mutex()
    private val reviews = linkedMapOf<String, InventoryReview>()

    override suspend fun save(review: InventoryReview): InventoryReview = mutex.withLock {
        reviews[review.reviewId] = review
        review
    }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryReview> = mutex.withLock {
        reviews.values.filter { it.inventoryItemId == inventoryItemId }
    }

    override suspend fun listBySession(sessionId: String): List<InventoryReview> = mutex.withLock {
        reviews.values.filter { it.sessionId == sessionId }
    }
}

class InMemoryInventoryCommentRepositoryAdapter : InventoryCommentRepositoryPort {
    private val mutex = Mutex()
    private val comments = linkedMapOf<String, InventoryComment>()

    override suspend fun save(comment: InventoryComment): InventoryComment = mutex.withLock {
        comments[comment.commentId] = comment
        comment
    }

    override suspend fun findByCommentId(commentId: String): InventoryComment? = mutex.withLock { comments[commentId] }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryComment> = mutex.withLock {
        comments.values.filter { it.inventoryItemId == inventoryItemId }
    }

    override suspend fun listBySession(sessionId: String): List<InventoryComment> = mutex.withLock {
        comments.values.filter { it.sessionId == sessionId }
    }
}

class InMemoryInventoryAttachmentRepositoryAdapter : InventoryAttachmentRepositoryPort {
    private val mutex = Mutex()
    private val attachments = linkedMapOf<String, InventoryAttachment>()

    override suspend fun save(attachment: InventoryAttachment): InventoryAttachment = mutex.withLock {
        attachments[attachment.attachmentId] = attachment
        attachment
    }

    override suspend fun findByAttachmentId(attachmentId: String): InventoryAttachment? = mutex.withLock { attachments[attachmentId] }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryAttachment> = mutex.withLock {
        attachments.values.filter { it.inventoryItemId == inventoryItemId }
    }

    override suspend fun listBySession(sessionId: String): List<InventoryAttachment> = mutex.withLock {
        attachments.values.filter { it.sessionId == sessionId }
    }
}

class InMemoryInventoryEventRepositoryAdapter : InventoryEventRepositoryPort {
    private val mutex = Mutex()
    private val events = linkedMapOf<String, InventoryEvent>()
    private val sequences = linkedMapOf<String, Long>()

    override suspend fun save(event: InventoryEvent): InventoryEvent = mutex.withLock {
        events[event.eventId] = event
        val current = sequences[event.organizationId] ?: 0
        if (event.sequence > current) {
            sequences[event.organizationId] = event.sequence
        }
        event
    }

    override suspend fun findByEventId(eventId: String): InventoryEvent? = mutex.withLock { events[eventId] }

    override suspend fun listByOrganization(organizationId: String): List<InventoryEvent> = mutex.withLock {
        events.values.filter { it.organizationId == organizationId }.sortedBy { it.sequence }
    }

    override suspend fun listByOrganizationSince(organizationId: String, since: Instant): List<InventoryEvent> = mutex.withLock {
        events.values.filter { it.organizationId == organizationId && it.occurredAt >= since }.sortedBy { it.sequence }
    }

    override suspend fun listByOrganizationSinceSequence(organizationId: String, sequence: Long): List<InventoryEvent> = mutex.withLock {
        events.values.filter { it.organizationId == organizationId && it.sequence > sequence }.sortedBy { it.sequence }
    }

    override suspend fun listByEntity(entityId: String): List<InventoryEvent> = mutex.withLock {
        events.values.filter { it.entityId == entityId }.sortedBy { it.sequence }
    }

    override suspend fun nextSequence(organizationId: String): Long = mutex.withLock {
        val next = (sequences[organizationId] ?: 0) + 1
        sequences[organizationId] = next
        next
    }

    override suspend fun latestSequence(organizationId: String): Long = mutex.withLock { sequences[organizationId] ?: 0 }
}

class InMemoryInventoryExportRepositoryAdapter : InventoryExportRepositoryPort {
    private val mutex = Mutex()
    private val exports = linkedMapOf<String, InventoryExportTask>()

    override suspend fun save(task: InventoryExportTask): InventoryExportTask = mutex.withLock {
        exports[task.exportTaskId] = task
        task
    }

    override suspend fun findByExportId(exportTaskId: String): InventoryExportTask? = mutex.withLock { exports[exportTaskId] }

    override suspend fun listByOrganization(organizationId: String): List<InventoryExportTask> = mutex.withLock {
        exports.values.filter { it.organizationId == organizationId }
    }

    override suspend fun listBySession(sessionId: String): List<InventoryExportTask> = mutex.withLock {
        exports.values.filter { it.sessionId == sessionId }
    }
}

class InMemoryInventoryQrCodeRepositoryAdapter : InventoryQrCodeRepositoryPort {
    private val mutex = Mutex()
    private val codes = linkedMapOf<String, InventoryQrCode>()

    override suspend fun save(qrCode: InventoryQrCode): InventoryQrCode = mutex.withLock {
        codes[qrCode.codeId] = qrCode
        qrCode
    }

    override suspend fun findByCode(code: String): InventoryQrCode? = mutex.withLock {
        codes.values.firstOrNull { it.qrCode == code }
    }

    override suspend fun findByBarcode(barcode: String): InventoryQrCode? = mutex.withLock {
        codes.values.firstOrNull { it.barcode == barcode }
    }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryQrCode> = mutex.withLock {
        codes.values.filter { it.inventoryItemId == inventoryItemId }
    }
}

class InMemoryInventorySubcategoryRepositoryAdapter : InventorySubcategoryRepositoryPort {
    private val mutex = Mutex()
    private val subcategories = linkedMapOf<String, InventorySubcategory>()

    override suspend fun save(subcategory: InventorySubcategory): InventorySubcategory = mutex.withLock {
        subcategories[subcategory.subcategoryId] = subcategory
        subcategory
    }

    override suspend fun findBySubcategoryId(subcategoryId: String): InventorySubcategory? = mutex.withLock {
        subcategories[subcategoryId]
    }

    override suspend fun listByCategory(categoryId: String): List<InventorySubcategory> = mutex.withLock {
        subcategories.values.filter { it.categoryId == categoryId }
    }

    override suspend fun listByOrganization(organizationId: String): List<InventorySubcategory> = mutex.withLock {
        subcategories.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryTagRepositoryAdapter : InventoryTagRepositoryPort {
    private val mutex = Mutex()
    private val tags = linkedMapOf<String, InventoryTag>()

    override suspend fun save(tag: InventoryTag): InventoryTag = mutex.withLock {
        tags[tag.tagId] = tag
        tag
    }

    override suspend fun findByTagId(tagId: String): InventoryTag? = mutex.withLock { tags[tagId] }

    override suspend fun listByOrganization(organizationId: String): List<InventoryTag> = mutex.withLock {
        tags.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryAttributeDefinitionRepositoryAdapter : InventoryAttributeDefinitionRepositoryPort {
    private val mutex = Mutex()
    private val definitions = linkedMapOf<String, InventoryAttributeDefinition>()

    override suspend fun save(definition: InventoryAttributeDefinition): InventoryAttributeDefinition = mutex.withLock {
        definitions[definition.attributeId] = definition
        definition
    }

    override suspend fun findByAttributeId(attributeId: String): InventoryAttributeDefinition? = mutex.withLock {
        definitions[attributeId]
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryAttributeDefinition> = mutex.withLock {
        definitions.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryCategoryTemplateRepositoryAdapter : InventoryCategoryTemplateRepositoryPort {
    private val mutex = Mutex()
    private val templates = linkedMapOf<String, InventoryCategoryTemplate>()

    override suspend fun save(template: InventoryCategoryTemplate): InventoryCategoryTemplate = mutex.withLock {
        templates[template.templateId] = template
        template
    }

    override suspend fun findByTemplateId(templateId: String): InventoryCategoryTemplate? = mutex.withLock {
        templates[templateId]
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryCategoryTemplate> = mutex.withLock {
        templates.values.filter { it.organizationId == organizationId }
    }

    override suspend fun listByCategory(categoryId: String): List<InventoryCategoryTemplate> = mutex.withLock {
        templates.values.filter { it.categoryId == categoryId }
    }
}

class InMemoryInventoryOwnerRepositoryAdapter : InventoryOwnerRepositoryPort {
    private val mutex = Mutex()
    private val owners = linkedMapOf<String, InventoryOwner>()

    override suspend fun save(owner: InventoryOwner): InventoryOwner = mutex.withLock {
        owners[owner.ownerId] = owner
        owner
    }

    override suspend fun findByOwnerId(ownerId: String): InventoryOwner? = mutex.withLock { owners[ownerId] }

    override suspend fun listByOrganization(organizationId: String): List<InventoryOwner> = mutex.withLock {
        owners.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryDepartmentRepositoryAdapter : InventoryDepartmentRepositoryPort {
    private val mutex = Mutex()
    private val departments = linkedMapOf<String, InventoryDepartment>()

    override suspend fun save(department: InventoryDepartment): InventoryDepartment = mutex.withLock {
        departments[department.departmentId] = department
        department
    }

    override suspend fun findByDepartmentId(departmentId: String): InventoryDepartment? = mutex.withLock {
        departments[departmentId]
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryDepartment> = mutex.withLock {
        departments.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryCostCenterRepositoryAdapter : InventoryCostCenterRepositoryPort {
    private val mutex = Mutex()
    private val costCenters = linkedMapOf<String, InventoryCostCenter>()

    override suspend fun save(costCenter: InventoryCostCenter): InventoryCostCenter = mutex.withLock {
        costCenters[costCenter.costCenterId] = costCenter
        costCenter
    }

    override suspend fun findByCostCenterId(costCenterId: String): InventoryCostCenter? = mutex.withLock {
        costCenters[costCenterId]
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryCostCenter> = mutex.withLock {
        costCenters.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryLegalHolderRepositoryAdapter : InventoryLegalHolderRepositoryPort {
    private val mutex = Mutex()
    private val holders = linkedMapOf<String, InventoryLegalHolder>()

    override suspend fun save(holder: InventoryLegalHolder): InventoryLegalHolder = mutex.withLock {
        holders[holder.legalHolderId] = holder
        holder
    }

    override suspend fun findByLegalHolderId(legalHolderId: String): InventoryLegalHolder? = mutex.withLock {
        holders[legalHolderId]
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryLegalHolder> = mutex.withLock {
        holders.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventorySupplierRepositoryAdapter : InventorySupplierRepositoryPort {
    private val mutex = Mutex()
    private val suppliers = linkedMapOf<String, InventorySupplier>()

    override suspend fun save(supplier: InventorySupplier): InventorySupplier = mutex.withLock {
        suppliers[supplier.supplierId] = supplier
        supplier
    }

    override suspend fun findBySupplierId(supplierId: String): InventorySupplier? = mutex.withLock {
        suppliers[supplierId]
    }

    override suspend fun listByOrganization(organizationId: String): List<InventorySupplier> = mutex.withLock {
        suppliers.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryFundingSourceRepositoryAdapter : InventoryFundingSourceRepositoryPort {
    private val mutex = Mutex()
    private val sources = linkedMapOf<String, InventoryFundingSource>()

    override suspend fun save(source: InventoryFundingSource): InventoryFundingSource = mutex.withLock {
        sources[source.fundingSourceId] = source
        source
    }

    override suspend fun findByFundingSourceId(fundingSourceId: String): InventoryFundingSource? = mutex.withLock {
        sources[fundingSourceId]
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryFundingSource> = mutex.withLock {
        sources.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryIncidentRepositoryAdapter : InventoryIncidentRepositoryPort {
    private val mutex = Mutex()
    private val incidents = linkedMapOf<String, InventoryIncident>()

    override suspend fun save(incident: InventoryIncident): InventoryIncident = mutex.withLock {
        incidents[incident.incidentId] = incident
        incident
    }

    override suspend fun findByIncidentId(incidentId: String): InventoryIncident? = mutex.withLock {
        incidents[incidentId]
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryIncident> = mutex.withLock {
        incidents.values.filter { it.organizationId == organizationId }
    }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryIncident> = mutex.withLock {
        incidents.values.filter { it.inventoryItemId == inventoryItemId }
    }

    override suspend fun listBySession(sessionId: String): List<InventoryIncident> = mutex.withLock {
        incidents.values.filter { it.sessionId == sessionId }
    }
}

class InMemoryInventoryAlertRepositoryAdapter : InventoryAlertRepositoryPort {
    private val mutex = Mutex()
    private val alerts = linkedMapOf<String, InventoryAlertEvent>()

    override suspend fun save(alert: InventoryAlertEvent): InventoryAlertEvent = mutex.withLock {
        alerts[alert.alertId] = alert
        alert
    }

    override suspend fun findByAlertId(alertId: String): InventoryAlertEvent? = mutex.withLock { alerts[alertId] }

    override suspend fun listByOrganization(organizationId: String): List<InventoryAlertEvent> = mutex.withLock {
        alerts.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryReminderRepositoryAdapter : InventoryReminderRepositoryPort {
    private val mutex = Mutex()
    private val reminders = linkedMapOf<String, InventoryReminder>()

    override suspend fun save(reminder: InventoryReminder): InventoryReminder = mutex.withLock {
        reminders[reminder.reminderId] = reminder
        reminder
    }

    override suspend fun findByReminderId(reminderId: String): InventoryReminder? = mutex.withLock { reminders[reminderId] }

    override suspend fun listByOrganization(organizationId: String): List<InventoryReminder> = mutex.withLock {
        reminders.values.filter { it.organizationId == organizationId }
    }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryReminder> = mutex.withLock {
        reminders.values.filter { it.inventoryItemId == inventoryItemId }
    }
}

class InMemoryInventoryThresholdRuleRepositoryAdapter : InventoryThresholdRuleRepositoryPort {
    private val mutex = Mutex()
    private val rules = linkedMapOf<String, InventoryRuleThreshold>()

    override suspend fun save(rule: InventoryRuleThreshold): InventoryRuleThreshold = mutex.withLock {
        rules[rule.ruleId] = rule
        rule
    }

    override suspend fun findByRuleId(ruleId: String): InventoryRuleThreshold? = mutex.withLock { rules[ruleId] }

    override suspend fun listByOrganization(organizationId: String): List<InventoryRuleThreshold> = mutex.withLock {
        rules.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryDeadlineRuleRepositoryAdapter : InventoryDeadlineRuleRepositoryPort {
    private val mutex = Mutex()
    private val rules = linkedMapOf<String, InventoryDeadlineRule>()

    override suspend fun save(rule: InventoryDeadlineRule): InventoryDeadlineRule = mutex.withLock {
        rules[rule.ruleId] = rule
        rule
    }

    override suspend fun findByRuleId(ruleId: String): InventoryDeadlineRule? = mutex.withLock { rules[ruleId] }

    override suspend fun listByOrganization(organizationId: String): List<InventoryDeadlineRule> = mutex.withLock {
        rules.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryChangeLogRepositoryAdapter : InventoryChangeLogRepositoryPort {
    private val mutex = Mutex()
    private val logs = linkedMapOf<String, InventoryChangeLog>()

    override suspend fun save(changeLog: InventoryChangeLog): InventoryChangeLog = mutex.withLock {
        logs[changeLog.changeId] = changeLog
        changeLog
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryChangeLog> = mutex.withLock {
        logs.values.filter { it.organizationId == organizationId }
    }

    override suspend fun listByEntity(entityId: String): List<InventoryChangeLog> = mutex.withLock {
        logs.values.filter { it.entityId == entityId }
    }
}

class InMemoryInventoryDashboardRepositoryAdapter : InventoryDashboardRepositoryPort {
    private val mutex = Mutex()
    private val snapshots = linkedMapOf<String, InventoryDashboardSnapshot>()

    override suspend fun save(snapshot: InventoryDashboardSnapshot): InventoryDashboardSnapshot = mutex.withLock {
        snapshots[snapshot.snapshotId] = snapshot
        snapshot
    }

    override suspend fun latestSnapshot(organizationId: String): InventoryDashboardSnapshot? = mutex.withLock {
        snapshots.values.filter { it.organizationId == organizationId }.maxByOrNull { it.generatedAt }
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryDashboardSnapshot> = mutex.withLock {
        snapshots.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryCodeBindingRepositoryAdapter : InventoryCodeBindingRepositoryPort {
    private val mutex = Mutex()
    private val bindings = linkedMapOf<String, InventoryCodeBinding>()

    override suspend fun save(binding: InventoryCodeBinding): InventoryCodeBinding = mutex.withLock {
        bindings[binding.codeId] = binding
        binding
    }

    override suspend fun findByCode(codeValue: String): InventoryCodeBinding? = mutex.withLock {
        bindings.values.firstOrNull { it.codeValue == codeValue }
    }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryCodeBinding> = mutex.withLock {
        bindings.values.filter { it.inventoryItemId == inventoryItemId }
    }
}

class InMemoryInventoryCodeRepositoryAdapter : InventoryCodeRepositoryPort {
    private val mutex = Mutex()
    private val codes = linkedMapOf<String, InventoryCode>()

    override suspend fun save(code: InventoryCode): InventoryCode = mutex.withLock {
        codes[code.inventoryCodeId] = code
        code
    }

    override suspend fun findByCodeId(codeId: String): InventoryCode? = mutex.withLock { codes[codeId] }

    override suspend fun findByRawValue(rawValue: String): InventoryCode? = mutex.withLock {
        codes.values.firstOrNull { it.rawValue == rawValue }
    }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryCode> = mutex.withLock {
        codes.values.filter { it.inventoryItemId == inventoryItemId }
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryCode> = mutex.withLock {
        codes.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryLabelTemplateRepositoryAdapter : InventoryLabelTemplateRepositoryPort {
    private val mutex = Mutex()
    private val templates = linkedMapOf<String, InventoryLabelTemplate>()

    override suspend fun save(template: InventoryLabelTemplate): InventoryLabelTemplate = mutex.withLock {
        templates[template.templateId] = template
        template
    }

    override suspend fun findByTemplateId(templateId: String): InventoryLabelTemplate? = mutex.withLock {
        templates[templateId]
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryLabelTemplate> = mutex.withLock {
        templates.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryLabelRepositoryAdapter : InventoryLabelRepositoryPort {
    private val mutex = Mutex()
    private val labels = linkedMapOf<String, InventoryLabel>()

    override suspend fun save(label: InventoryLabel): InventoryLabel = mutex.withLock {
        labels[label.labelId] = label
        label
    }

    override suspend fun findByLabelId(labelId: String): InventoryLabel? = mutex.withLock { labels[labelId] }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryLabel> = mutex.withLock {
        labels.values.filter { it.inventoryItemId == inventoryItemId }
    }
}

class InMemoryInventoryPrintTaskRepositoryAdapter : InventoryPrintTaskRepositoryPort {
    private val mutex = Mutex()
    private val tasks = linkedMapOf<String, InventoryPrintTask>()

    override suspend fun save(task: InventoryPrintTask): InventoryPrintTask = mutex.withLock {
        tasks[task.printTaskId] = task
        task
    }

    override suspend fun findByPrintTaskId(printTaskId: String): InventoryPrintTask? = mutex.withLock {
        tasks[printTaskId]
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryPrintTask> = mutex.withLock {
        tasks.values.filter { it.organizationId == organizationId }
    }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryPrintTask> = mutex.withLock {
        tasks.values.filter { it.itemIds.contains(inventoryItemId) }
    }
}

class InMemoryInventoryScanEventRepositoryAdapter : InventoryScanEventRepositoryPort {
    private val mutex = Mutex()
    private val events = linkedMapOf<String, InventoryScanEvent>()

    override suspend fun save(event: InventoryScanEvent): InventoryScanEvent = mutex.withLock {
        events[event.scanEventId] = event
        event
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryScanEvent> = mutex.withLock {
        events.values.filter { it.organizationId == organizationId }
    }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryScanEvent> = mutex.withLock {
        events.values.filter { it.inventoryItemId == inventoryItemId }
    }
}

class InMemoryInventoryRevisionRepositoryAdapter : InventoryRevisionRepositoryPort {
    private val mutex = Mutex()
    private val revisions = linkedMapOf<String, InventoryRevision>()

    override suspend fun save(revision: InventoryRevision): InventoryRevision = mutex.withLock {
        revisions[revision.entityId] = revision
        revision
    }

    override suspend fun findByEntity(entityId: String): InventoryRevision? = mutex.withLock {
        revisions[entityId]
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryRevision> = mutex.withLock {
        revisions.values.filter { it.organizationId == organizationId }
    }
}

class InMemoryInventoryConflictRepositoryAdapter : InventoryConflictRepositoryPort {
    private val mutex = Mutex()
    private val conflicts = linkedMapOf<String, InventoryConflict>()

    override suspend fun save(conflict: InventoryConflict): InventoryConflict = mutex.withLock {
        conflicts[conflict.conflictId] = conflict
        conflict
    }

    override suspend fun findByConflictId(conflictId: String): InventoryConflict? = mutex.withLock {
        conflicts[conflictId]
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryConflict> = mutex.withLock {
        conflicts.values.filter { it.organizationId == organizationId }
    }
}
