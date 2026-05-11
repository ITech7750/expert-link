package org.expert.link.database

import kotlinx.datetime.Instant
import org.expert.link.mesh.domain.model.inventory.InventoryAlertEvent
import org.expert.link.mesh.domain.model.inventory.InventoryAttachment
import org.expert.link.mesh.domain.model.inventory.InventoryAttributeDefinition
import org.expert.link.mesh.domain.model.inventory.InventoryCategory
import org.expert.link.mesh.domain.model.inventory.InventoryCategoryTemplate
import org.expert.link.mesh.domain.model.inventory.InventoryChangeLog
import org.expert.link.mesh.domain.model.inventory.InventoryCodeBinding
import org.expert.link.mesh.domain.model.inventory.InventoryCode
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
import org.expert.link.mesh.domain.model.inventory.InventoryLabel
import org.expert.link.mesh.domain.model.inventory.InventoryLabelTemplate
import org.expert.link.mesh.domain.model.inventory.InventoryLegalHolder
import org.expert.link.mesh.domain.model.inventory.InventoryLocation
import org.expert.link.mesh.domain.model.inventory.InventoryOwner
import org.expert.link.mesh.domain.model.inventory.InventoryQrCode
import org.expert.link.mesh.domain.model.inventory.InventoryPrintTask
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
import org.expert.link.mesh.domain.model.inventory.InventoryRevision
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

internal class RoomOrganizationRepository(
    private val inventoryDao: InventoryDao,
) : OrganizationRepositoryPort {
    override suspend fun save(organization: Organization): Organization {
        inventoryDao.upsertOrganization(
            OrganizationRecord(
                organizationId = organization.organizationId,
                name = organization.name,
                updatedAt = organization.updatedAt.toString(),
                payloadJson = encodePayload(organization),
            ),
        )
        return organization
    }

    override suspend fun findByOrganizationId(organizationId: String): Organization? =
        inventoryDao.findOrganization(organizationId)?.let { decodePayload(it.payloadJson) }

    override suspend fun list(): List<Organization> =
        inventoryDao.listOrganizations().map { decodePayload(it.payloadJson) }
}

internal class RoomOrganizationMemberRepository(
    private val inventoryDao: InventoryDao,
) : OrganizationMemberRepositoryPort {
    override suspend fun save(member: OrganizationMember): OrganizationMember {
        inventoryDao.upsertOrganizationMember(
            OrganizationMemberRecord(
                organizationId = member.organizationId,
                peerId = member.peerId,
                roleKey = roleKey(member.roleIds),
                status = member.status.name,
                updatedAt = member.updatedAt.toString(),
                payloadJson = encodePayload(member),
            ),
        )
        return member
    }

    override suspend fun findByOrganizationAndPeer(organizationId: String, peerId: String): OrganizationMember? =
        inventoryDao.listOrganizationMembers(organizationId)
            .firstOrNull { it.peerId == peerId }
            ?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<OrganizationMember> =
        inventoryDao.listOrganizationMembers(organizationId).map { decodePayload(it.payloadJson) }

    override suspend fun listByPeer(peerId: String): List<OrganizationMember> =
        inventoryDao.listOrganizationMembersByPeer(peerId).map { decodePayload(it.payloadJson) }

    override suspend fun remove(organizationId: String, peerId: String) {
        inventoryDao.deleteOrganizationMember(organizationId, peerId)
    }

    private fun roleKey(roleIds: Set<String>): String = roleIds.sorted().joinToString("|")
}

internal class RoomRoleRepository(
    private val inventoryDao: InventoryDao,
) : RoleRepositoryPort {
    override suspend fun save(role: Role): Role {
        inventoryDao.upsertRole(
            RoleRecord(
                roleId = role.roleId,
                organizationId = role.organizationId,
                name = role.name,
                updatedAt = role.updatedAt.toString(),
                payloadJson = encodePayload(role),
            ),
        )
        return role
    }

    override suspend fun findByRoleId(roleId: String): Role? =
        inventoryDao.findRole(roleId)?.let { decodePayload(it.payloadJson) }

    override suspend fun findByName(organizationId: String, name: String): Role? =
        inventoryDao.findRoleByName(organizationId, name)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<Role> =
        inventoryDao.listRoles(organizationId).map { decodePayload(it.payloadJson) }

    override suspend fun remove(roleId: String) {
        inventoryDao.deleteRole(roleId)
    }
}

internal class RoomInventoryCategoryRepository(
    private val inventoryDao: InventoryDao,
) : InventoryCategoryRepositoryPort {
    override suspend fun save(category: InventoryCategory): InventoryCategory {
        inventoryDao.upsertCategory(
            InventoryCategoryRecord(
                categoryId = category.categoryId,
                organizationId = category.organizationId,
                name = category.name,
                updatedAt = category.updatedAt.toString(),
                payloadJson = encodePayload(category),
            ),
        )
        return category
    }

    override suspend fun findByCategoryId(categoryId: String): InventoryCategory? =
        inventoryDao.findCategory(categoryId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryCategory> =
        inventoryDao.listCategories(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryLocationRepository(
    private val inventoryDao: InventoryDao,
) : InventoryLocationRepositoryPort {
    override suspend fun save(location: InventoryLocation): InventoryLocation {
        inventoryDao.upsertLocation(
            InventoryLocationRecord(
                locationId = location.locationId,
                organizationId = location.organizationId,
                name = location.name,
                updatedAt = location.updatedAt.toString(),
                payloadJson = encodePayload(location),
            ),
        )
        return location
    }

    override suspend fun findByLocationId(locationId: String): InventoryLocation? =
        inventoryDao.findLocation(locationId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryLocation> =
        inventoryDao.listLocations(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryItemRepository(
    private val inventoryDao: InventoryDao,
) : InventoryItemRepositoryPort {
    override suspend fun save(item: InventoryItem): InventoryItem {
        inventoryDao.upsertItem(
            InventoryItemRecord(
                inventoryItemId = item.inventoryItemId,
                organizationId = item.organizationId,
                inventoryNumber = item.inventoryNumber,
                qrCode = item.qrCode,
                barcode = item.barcode,
                status = item.currentStatus.name,
                updatedAt = item.updatedAt.toString(),
                revision = item.revision,
                payloadJson = encodePayload(item),
            ),
        )
        return item
    }

    override suspend fun findByInventoryItemId(itemId: String): InventoryItem? =
        inventoryDao.findItem(itemId)?.let { decodePayload(it.payloadJson) }

    override suspend fun findByInventoryNumber(organizationId: String, inventoryNumber: String): InventoryItem? =
        inventoryDao.findItemByInventoryNumber(organizationId, inventoryNumber)?.let { decodePayload(it.payloadJson) }

    override suspend fun findByQrCode(qrCode: String): InventoryItem? =
        inventoryDao.findItemByQrCode(qrCode)?.let { decodePayload(it.payloadJson) }

    override suspend fun findByBarcode(barcode: String): InventoryItem? =
        inventoryDao.findItemByBarcode(barcode)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryItem> =
        inventoryDao.listItems(organizationId).map { decodePayload(it.payloadJson) }

    override suspend fun listByStatus(organizationId: String, status: InventoryStatus): List<InventoryItem> =
        inventoryDao.listItemsByStatus(organizationId, status.name).map { decodePayload(it.payloadJson) }

    override suspend fun listUpdatedSince(organizationId: String, since: Instant): List<InventoryItem> =
        inventoryDao.listItemsUpdatedSince(organizationId, since.toString()).map { decodePayload(it.payloadJson) }
}

internal class RoomInventorySessionRepository(
    private val inventoryDao: InventoryDao,
) : InventorySessionRepositoryPort {
    override suspend fun save(session: InventorySession): InventorySession {
        inventoryDao.upsertSession(
            InventorySessionRecord(
                sessionId = session.sessionId,
                organizationId = session.organizationId,
                status = session.status.name,
                periodStart = session.periodStart.toString(),
                periodEnd = session.periodEnd?.toString(),
                updatedAt = session.updatedAt.toString(),
                payloadJson = encodePayload(session),
            ),
        )
        return session
    }

    override suspend fun findBySessionId(sessionId: String): InventorySession? =
        inventoryDao.findSession(sessionId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventorySession> =
        inventoryDao.listSessions(organizationId).map { decodePayload(it.payloadJson) }

    override suspend fun listByStatus(organizationId: String, status: InventorySessionStatus): List<InventorySession> =
        inventoryDao.listSessionsByStatus(organizationId, status.name).map { decodePayload(it.payloadJson) }

    override suspend fun listUpdatedSince(organizationId: String, since: Instant): List<InventorySession> =
        inventoryDao.listSessionsUpdatedSince(organizationId, since.toString()).map { decodePayload(it.payloadJson) }
}

internal class RoomInventorySessionMemberRepository(
    private val inventoryDao: InventoryDao,
) : InventorySessionMemberRepositoryPort {
    override suspend fun save(member: InventorySessionMember): InventorySessionMember {
        inventoryDao.upsertSessionMember(
            InventorySessionMemberRecord(
                sessionId = member.sessionId,
                organizationId = member.organizationId,
                peerId = member.peerId,
                role = member.role.name,
                updatedAt = member.updatedAt.toString(),
                payloadJson = encodePayload(member),
            ),
        )
        return member
    }

    override suspend fun listBySession(sessionId: String): List<InventorySessionMember> =
        inventoryDao.listSessionMembers(sessionId).map { decodePayload(it.payloadJson) }

    override suspend fun listByPeer(peerId: String): List<InventorySessionMember> =
        inventoryDao.listSessionMembersByPeer(peerId).map { decodePayload(it.payloadJson) }

    override suspend fun remove(sessionId: String, peerId: String) {
        inventoryDao.deleteSessionMember(sessionId, peerId)
    }
}

internal class RoomInventoryReviewRepository(
    private val inventoryDao: InventoryDao,
) : InventoryReviewRepositoryPort {
    override suspend fun save(review: InventoryReview): InventoryReview {
        inventoryDao.upsertReview(
            InventoryReviewRecord(
                reviewId = review.reviewId,
                organizationId = review.organizationId,
                inventoryItemId = review.inventoryItemId,
                sessionId = review.sessionId,
                status = review.status.name,
                createdAt = review.createdAt.toString(),
                payloadJson = encodePayload(review),
            ),
        )
        return review
    }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryReview> =
        inventoryDao.listReviewsByItem(inventoryItemId).map { decodePayload(it.payloadJson) }

    override suspend fun listBySession(sessionId: String): List<InventoryReview> =
        inventoryDao.listReviewsBySession(sessionId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryCommentRepository(
    private val inventoryDao: InventoryDao,
) : InventoryCommentRepositoryPort {
    override suspend fun save(comment: InventoryComment): InventoryComment {
        inventoryDao.upsertComment(
            InventoryCommentRecord(
                commentId = comment.commentId,
                organizationId = comment.organizationId,
                inventoryItemId = comment.inventoryItemId,
                sessionId = comment.sessionId,
                createdAt = comment.createdAt.toString(),
                payloadJson = encodePayload(comment),
            ),
        )
        return comment
    }

    override suspend fun findByCommentId(commentId: String): InventoryComment? =
        inventoryDao.findComment(commentId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryComment> =
        inventoryDao.listCommentsByItem(inventoryItemId).map { decodePayload(it.payloadJson) }

    override suspend fun listBySession(sessionId: String): List<InventoryComment> =
        inventoryDao.listCommentsBySession(sessionId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryAttachmentRepository(
    private val inventoryDao: InventoryDao,
) : InventoryAttachmentRepositoryPort {
    override suspend fun save(attachment: InventoryAttachment): InventoryAttachment {
        inventoryDao.upsertAttachment(
            InventoryAttachmentRecord(
                attachmentId = attachment.attachmentId,
                organizationId = attachment.organizationId,
                inventoryItemId = attachment.inventoryItemId,
                sessionId = attachment.sessionId,
                createdAt = attachment.createdAt.toString(),
                payloadJson = encodePayload(attachment),
            ),
        )
        return attachment
    }

    override suspend fun findByAttachmentId(attachmentId: String): InventoryAttachment? =
        inventoryDao.findAttachment(attachmentId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryAttachment> =
        inventoryDao.listAttachmentsByItem(inventoryItemId).map { decodePayload(it.payloadJson) }

    override suspend fun listBySession(sessionId: String): List<InventoryAttachment> =
        inventoryDao.listAttachmentsBySession(sessionId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryEventRepository(
    private val inventoryDao: InventoryDao,
) : InventoryEventRepositoryPort {
    override suspend fun save(event: InventoryEvent): InventoryEvent {
        inventoryDao.upsertEvent(
            InventoryEventRecord(
                eventId = event.eventId,
                organizationId = event.organizationId,
                entityId = event.entityId,
                entityType = event.entityType.name,
                eventType = event.eventType.name,
                occurredAt = event.occurredAt.toString(),
                sequence = event.sequence,
                payloadJson = encodePayload(event),
            ),
        )
        return event
    }

    override suspend fun findByEventId(eventId: String): InventoryEvent? =
        inventoryDao.findEvent(eventId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryEvent> =
        inventoryDao.listEvents(organizationId).map { decodePayload(it.payloadJson) }

    override suspend fun listByOrganizationSince(organizationId: String, since: Instant): List<InventoryEvent> =
        inventoryDao.listEventsSince(organizationId, since.toString()).map { decodePayload(it.payloadJson) }

    override suspend fun listByOrganizationSinceSequence(organizationId: String, sequence: Long): List<InventoryEvent> =
        inventoryDao.listEventsSinceSequence(organizationId, sequence).map { decodePayload(it.payloadJson) }

    override suspend fun listByEntity(entityId: String): List<InventoryEvent> =
        inventoryDao.listEventsByEntity(entityId).map { decodePayload(it.payloadJson) }

    override suspend fun nextSequence(organizationId: String): Long =
        (inventoryDao.maxEventSequence(organizationId) ?: 0L) + 1

    override suspend fun latestSequence(organizationId: String): Long =
        inventoryDao.maxEventSequence(organizationId) ?: 0L
}

internal class RoomInventoryExportRepository(
    private val inventoryDao: InventoryDao,
) : InventoryExportRepositoryPort {
    override suspend fun save(task: InventoryExportTask): InventoryExportTask {
        inventoryDao.upsertExport(
            InventoryExportRecord(
                exportTaskId = task.exportTaskId,
                organizationId = task.organizationId,
                sessionId = task.sessionId,
                status = task.status.name,
                updatedAt = task.updatedAt.toString(),
                payloadJson = encodePayload(task),
            ),
        )
        return task
    }

    override suspend fun findByExportId(exportTaskId: String): InventoryExportTask? =
        inventoryDao.findExport(exportTaskId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryExportTask> =
        inventoryDao.listExports(organizationId).map { decodePayload(it.payloadJson) }

    override suspend fun listBySession(sessionId: String): List<InventoryExportTask> =
        inventoryDao.listExportsBySession(sessionId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryQrCodeRepository(
    private val inventoryDao: InventoryDao,
) : InventoryQrCodeRepositoryPort {
    override suspend fun save(qrCode: InventoryQrCode): InventoryQrCode {
        inventoryDao.upsertQrCode(
            InventoryQrCodeRecord(
                codeId = qrCode.codeId,
                organizationId = qrCode.organizationId,
                inventoryItemId = qrCode.inventoryItemId,
                qrCode = qrCode.qrCode,
                barcode = qrCode.barcode,
                createdAt = qrCode.createdAt.toString(),
                payloadJson = encodePayload(qrCode),
            ),
        )
        return qrCode
    }

    override suspend fun findByCode(code: String): InventoryQrCode? =
        inventoryDao.findQrCodeByCode(code)?.let { decodePayload(it.payloadJson) }

    override suspend fun findByBarcode(barcode: String): InventoryQrCode? =
        inventoryDao.findQrCodeByBarcode(barcode)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryQrCode> =
        inventoryDao.listQrCodesByItem(inventoryItemId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventorySubcategoryRepository(
    private val inventoryDao: InventoryDao,
) : InventorySubcategoryRepositoryPort {
    override suspend fun save(subcategory: InventorySubcategory): InventorySubcategory {
        inventoryDao.upsertSubcategory(
            InventorySubcategoryRecord(
                subcategoryId = subcategory.subcategoryId,
                organizationId = subcategory.organizationId,
                categoryId = subcategory.categoryId,
                name = subcategory.name,
                updatedAt = subcategory.updatedAt.toString(),
                payloadJson = encodePayload(subcategory),
            ),
        )
        return subcategory
    }

    override suspend fun findBySubcategoryId(subcategoryId: String): InventorySubcategory? =
        inventoryDao.findSubcategory(subcategoryId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByCategory(categoryId: String): List<InventorySubcategory> =
        inventoryDao.listSubcategoriesByCategory(categoryId).map { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventorySubcategory> =
        inventoryDao.listSubcategoriesByOrganization(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryTagRepository(
    private val inventoryDao: InventoryDao,
) : InventoryTagRepositoryPort {
    override suspend fun save(tag: InventoryTag): InventoryTag {
        inventoryDao.upsertTag(
            InventoryTagRecord(
                tagId = tag.tagId,
                organizationId = tag.organizationId,
                name = tag.name,
                updatedAt = tag.updatedAt.toString(),
                payloadJson = encodePayload(tag),
            ),
        )
        return tag
    }

    override suspend fun findByTagId(tagId: String): InventoryTag? =
        inventoryDao.findTag(tagId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryTag> =
        inventoryDao.listTags(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryAttributeDefinitionRepository(
    private val inventoryDao: InventoryDao,
) : InventoryAttributeDefinitionRepositoryPort {
    override suspend fun save(definition: InventoryAttributeDefinition): InventoryAttributeDefinition {
        inventoryDao.upsertAttributeDefinition(
            InventoryAttributeDefinitionRecord(
                attributeId = definition.attributeId,
                organizationId = definition.organizationId,
                key = definition.key,
                updatedAt = definition.updatedAt.toString(),
                payloadJson = encodePayload(definition),
            ),
        )
        return definition
    }

    override suspend fun findByAttributeId(attributeId: String): InventoryAttributeDefinition? =
        inventoryDao.findAttributeDefinition(attributeId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryAttributeDefinition> =
        inventoryDao.listAttributeDefinitions(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryCategoryTemplateRepository(
    private val inventoryDao: InventoryDao,
) : InventoryCategoryTemplateRepositoryPort {
    override suspend fun save(template: InventoryCategoryTemplate): InventoryCategoryTemplate {
        inventoryDao.upsertCategoryTemplate(
            InventoryCategoryTemplateRecord(
                templateId = template.templateId,
                organizationId = template.organizationId,
                categoryId = template.categoryId,
                name = template.name,
                updatedAt = template.updatedAt.toString(),
                payloadJson = encodePayload(template),
            ),
        )
        return template
    }

    override suspend fun findByTemplateId(templateId: String): InventoryCategoryTemplate? =
        inventoryDao.findCategoryTemplate(templateId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryCategoryTemplate> =
        inventoryDao.listCategoryTemplatesByOrganization(organizationId).map { decodePayload(it.payloadJson) }

    override suspend fun listByCategory(categoryId: String): List<InventoryCategoryTemplate> =
        inventoryDao.listCategoryTemplatesByCategory(categoryId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryOwnerRepository(
    private val inventoryDao: InventoryDao,
) : InventoryOwnerRepositoryPort {
    override suspend fun save(owner: InventoryOwner): InventoryOwner {
        inventoryDao.upsertOwner(
            InventoryOwnerRecord(
                ownerId = owner.ownerId,
                organizationId = owner.organizationId,
                name = owner.name,
                updatedAt = owner.updatedAt.toString(),
                payloadJson = encodePayload(owner),
            ),
        )
        return owner
    }

    override suspend fun findByOwnerId(ownerId: String): InventoryOwner? =
        inventoryDao.findOwner(ownerId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryOwner> =
        inventoryDao.listOwners(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryDepartmentRepository(
    private val inventoryDao: InventoryDao,
) : InventoryDepartmentRepositoryPort {
    override suspend fun save(department: InventoryDepartment): InventoryDepartment {
        inventoryDao.upsertDepartment(
            InventoryDepartmentRecord(
                departmentId = department.departmentId,
                organizationId = department.organizationId,
                name = department.name,
                updatedAt = department.updatedAt.toString(),
                payloadJson = encodePayload(department),
            ),
        )
        return department
    }

    override suspend fun findByDepartmentId(departmentId: String): InventoryDepartment? =
        inventoryDao.findDepartment(departmentId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryDepartment> =
        inventoryDao.listDepartments(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryCostCenterRepository(
    private val inventoryDao: InventoryDao,
) : InventoryCostCenterRepositoryPort {
    override suspend fun save(costCenter: InventoryCostCenter): InventoryCostCenter {
        inventoryDao.upsertCostCenter(
            InventoryCostCenterRecord(
                costCenterId = costCenter.costCenterId,
                organizationId = costCenter.organizationId,
                code = costCenter.code,
                updatedAt = costCenter.updatedAt.toString(),
                payloadJson = encodePayload(costCenter),
            ),
        )
        return costCenter
    }

    override suspend fun findByCostCenterId(costCenterId: String): InventoryCostCenter? =
        inventoryDao.findCostCenter(costCenterId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryCostCenter> =
        inventoryDao.listCostCenters(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryLegalHolderRepository(
    private val inventoryDao: InventoryDao,
) : InventoryLegalHolderRepositoryPort {
    override suspend fun save(holder: InventoryLegalHolder): InventoryLegalHolder {
        inventoryDao.upsertLegalHolder(
            InventoryLegalHolderRecord(
                legalHolderId = holder.legalHolderId,
                organizationId = holder.organizationId,
                name = holder.name,
                updatedAt = holder.updatedAt.toString(),
                payloadJson = encodePayload(holder),
            ),
        )
        return holder
    }

    override suspend fun findByLegalHolderId(legalHolderId: String): InventoryLegalHolder? =
        inventoryDao.findLegalHolder(legalHolderId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryLegalHolder> =
        inventoryDao.listLegalHolders(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventorySupplierRepository(
    private val inventoryDao: InventoryDao,
) : InventorySupplierRepositoryPort {
    override suspend fun save(supplier: InventorySupplier): InventorySupplier {
        inventoryDao.upsertSupplier(
            InventorySupplierRecord(
                supplierId = supplier.supplierId,
                organizationId = supplier.organizationId,
                name = supplier.name,
                updatedAt = supplier.updatedAt.toString(),
                payloadJson = encodePayload(supplier),
            ),
        )
        return supplier
    }

    override suspend fun findBySupplierId(supplierId: String): InventorySupplier? =
        inventoryDao.findSupplier(supplierId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventorySupplier> =
        inventoryDao.listSuppliers(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryFundingSourceRepository(
    private val inventoryDao: InventoryDao,
) : InventoryFundingSourceRepositoryPort {
    override suspend fun save(source: InventoryFundingSource): InventoryFundingSource {
        inventoryDao.upsertFundingSource(
            InventoryFundingSourceRecord(
                fundingSourceId = source.fundingSourceId,
                organizationId = source.organizationId,
                name = source.name,
                updatedAt = source.updatedAt.toString(),
                payloadJson = encodePayload(source),
            ),
        )
        return source
    }

    override suspend fun findByFundingSourceId(fundingSourceId: String): InventoryFundingSource? =
        inventoryDao.findFundingSource(fundingSourceId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryFundingSource> =
        inventoryDao.listFundingSources(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryIncidentRepository(
    private val inventoryDao: InventoryDao,
) : InventoryIncidentRepositoryPort {
    override suspend fun save(incident: InventoryIncident): InventoryIncident {
        inventoryDao.upsertIncident(
            InventoryIncidentRecord(
                incidentId = incident.incidentId,
                organizationId = incident.organizationId,
                inventoryItemId = incident.inventoryItemId,
                sessionId = incident.sessionId,
                status = incident.status.name,
                reportedAt = incident.reportedAt.toString(),
                updatedAt = incident.updatedAt.toString(),
                payloadJson = encodePayload(incident),
            ),
        )
        return incident
    }

    override suspend fun findByIncidentId(incidentId: String): InventoryIncident? =
        inventoryDao.findIncident(incidentId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryIncident> =
        inventoryDao.listIncidents(organizationId).map { decodePayload(it.payloadJson) }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryIncident> =
        inventoryDao.listIncidentsByItem(inventoryItemId).map { decodePayload(it.payloadJson) }

    override suspend fun listBySession(sessionId: String): List<InventoryIncident> =
        inventoryDao.listIncidentsBySession(sessionId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryAlertRepository(
    private val inventoryDao: InventoryDao,
) : InventoryAlertRepositoryPort {
    override suspend fun save(alert: InventoryAlertEvent): InventoryAlertEvent {
        inventoryDao.upsertAlert(
            InventoryAlertRecord(
                alertId = alert.alertId,
                organizationId = alert.organizationId,
                inventoryItemId = alert.inventoryItemId,
                sessionId = alert.sessionId,
                createdAt = alert.createdAt.toString(),
                payloadJson = encodePayload(alert),
            ),
        )
        return alert
    }

    override suspend fun findByAlertId(alertId: String): InventoryAlertEvent? =
        inventoryDao.findAlert(alertId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryAlertEvent> =
        inventoryDao.listAlerts(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryReminderRepository(
    private val inventoryDao: InventoryDao,
) : InventoryReminderRepositoryPort {
    override suspend fun save(reminder: InventoryReminder): InventoryReminder {
        inventoryDao.upsertReminder(
            InventoryReminderRecord(
                reminderId = reminder.reminderId,
                organizationId = reminder.organizationId,
                inventoryItemId = reminder.inventoryItemId,
                sessionId = reminder.sessionId,
                status = reminder.status.name,
                dueAt = reminder.dueAt.toString(),
                updatedAt = reminder.updatedAt.toString(),
                payloadJson = encodePayload(reminder),
            ),
        )
        return reminder
    }

    override suspend fun findByReminderId(reminderId: String): InventoryReminder? =
        inventoryDao.findReminder(reminderId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryReminder> =
        inventoryDao.listReminders(organizationId).map { decodePayload(it.payloadJson) }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryReminder> =
        inventoryDao.listRemindersByItem(inventoryItemId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryThresholdRuleRepository(
    private val inventoryDao: InventoryDao,
) : InventoryThresholdRuleRepositoryPort {
    override suspend fun save(rule: InventoryRuleThreshold): InventoryRuleThreshold {
        inventoryDao.upsertRuleThreshold(
            InventoryRuleThresholdRecord(
                ruleId = rule.ruleId,
                organizationId = rule.organizationId,
                ruleType = rule.ruleType.name,
                updatedAt = rule.updatedAt.toString(),
                payloadJson = encodePayload(rule),
            ),
        )
        return rule
    }

    override suspend fun findByRuleId(ruleId: String): InventoryRuleThreshold? =
        inventoryDao.findRuleThreshold(ruleId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryRuleThreshold> =
        inventoryDao.listRuleThresholds(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryDeadlineRuleRepository(
    private val inventoryDao: InventoryDao,
) : InventoryDeadlineRuleRepositoryPort {
    override suspend fun save(rule: InventoryDeadlineRule): InventoryDeadlineRule {
        inventoryDao.upsertDeadlineRule(
            InventoryDeadlineRuleRecord(
                ruleId = rule.ruleId,
                organizationId = rule.organizationId,
                target = rule.target.name,
                updatedAt = rule.updatedAt.toString(),
                payloadJson = encodePayload(rule),
            ),
        )
        return rule
    }

    override suspend fun findByRuleId(ruleId: String): InventoryDeadlineRule? =
        inventoryDao.findDeadlineRule(ruleId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryDeadlineRule> =
        inventoryDao.listDeadlineRules(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryChangeLogRepository(
    private val inventoryDao: InventoryDao,
) : InventoryChangeLogRepositoryPort {
    override suspend fun save(changeLog: InventoryChangeLog): InventoryChangeLog {
        inventoryDao.upsertChangeLog(
            InventoryChangeLogRecord(
                changeId = changeLog.changeId,
                organizationId = changeLog.organizationId,
                entityId = changeLog.entityId,
                entityType = changeLog.entityType.name,
                changedAt = changeLog.changedAt.toString(),
                payloadJson = encodePayload(changeLog),
            ),
        )
        return changeLog
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryChangeLog> =
        inventoryDao.listChangeLogs(organizationId).map { decodePayload(it.payloadJson) }

    override suspend fun listByEntity(entityId: String): List<InventoryChangeLog> =
        inventoryDao.listChangeLogsByEntity(entityId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryDashboardRepository(
    private val inventoryDao: InventoryDao,
) : InventoryDashboardRepositoryPort {
    override suspend fun save(snapshot: InventoryDashboardSnapshot): InventoryDashboardSnapshot {
        inventoryDao.upsertDashboardSnapshot(
            InventoryDashboardSnapshotRecord(
                snapshotId = snapshot.snapshotId,
                organizationId = snapshot.organizationId,
                generatedAt = snapshot.generatedAt.toString(),
                payloadJson = encodePayload(snapshot),
            ),
        )
        return snapshot
    }

    override suspend fun latestSnapshot(organizationId: String): InventoryDashboardSnapshot? =
        inventoryDao.latestDashboardSnapshot(organizationId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryDashboardSnapshot> =
        inventoryDao.listDashboardSnapshots(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryCodeBindingRepository(
    private val inventoryDao: InventoryDao,
) : InventoryCodeBindingRepositoryPort {
    override suspend fun save(binding: InventoryCodeBinding): InventoryCodeBinding {
        inventoryDao.upsertCodeBinding(
            InventoryCodeBindingRecord(
                codeId = binding.codeId,
                organizationId = binding.organizationId,
                inventoryItemId = binding.inventoryItemId,
                codeValue = binding.codeValue,
                createdAt = binding.createdAt.toString(),
                payloadJson = encodePayload(binding),
            ),
        )
        return binding
    }

    override suspend fun findByCode(codeValue: String): InventoryCodeBinding? =
        inventoryDao.findCodeBinding(codeValue)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryCodeBinding> =
        inventoryDao.listCodeBindingsByItem(inventoryItemId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryCodeRepository(
    private val inventoryDao: InventoryDao,
) : InventoryCodeRepositoryPort {
    override suspend fun save(code: InventoryCode): InventoryCode {
        inventoryDao.upsertCode(
            InventoryCodeRecord(
                inventoryCodeId = code.inventoryCodeId,
                organizationId = code.organizationId,
                inventoryItemId = code.inventoryItemId,
                rawValue = code.rawValue,
                isActive = if (code.isActive) 1 else 0,
                createdAt = code.createdAt.toString(),
                payloadJson = encodePayload(code),
            ),
        )
        return code
    }

    override suspend fun findByCodeId(codeId: String): InventoryCode? =
        inventoryDao.findCode(codeId)?.let { decodePayload(it.payloadJson) }

    override suspend fun findByRawValue(rawValue: String): InventoryCode? =
        inventoryDao.findCodeByRawValue(rawValue)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryCode> =
        inventoryDao.listCodesByItem(inventoryItemId).map { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryCode> =
        inventoryDao.listCodesByOrganization(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryLabelTemplateRepository(
    private val inventoryDao: InventoryDao,
) : InventoryLabelTemplateRepositoryPort {
    override suspend fun save(template: InventoryLabelTemplate): InventoryLabelTemplate {
        inventoryDao.upsertLabelTemplate(
            InventoryLabelTemplateRecord(
                templateId = template.templateId,
                organizationId = template.organizationId,
                name = template.name,
                isDefault = if (template.isDefault) 1 else 0,
                updatedAt = template.updatedAt.toString(),
                payloadJson = encodePayload(template),
            ),
        )
        return template
    }

    override suspend fun findByTemplateId(templateId: String): InventoryLabelTemplate? =
        inventoryDao.findLabelTemplate(templateId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryLabelTemplate> =
        inventoryDao.listLabelTemplatesByOrganization(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryLabelRepository(
    private val inventoryDao: InventoryDao,
) : InventoryLabelRepositoryPort {
    override suspend fun save(label: InventoryLabel): InventoryLabel {
        inventoryDao.upsertLabel(
            InventoryLabelRecord(
                labelId = label.labelId,
                organizationId = label.organizationId,
                inventoryItemId = label.inventoryItemId,
                templateId = label.templateId,
                createdAt = label.createdAt.toString(),
                payloadJson = encodePayload(label),
            ),
        )
        return label
    }

    override suspend fun findByLabelId(labelId: String): InventoryLabel? =
        inventoryDao.findLabel(labelId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryLabel> =
        inventoryDao.listLabelsByItem(inventoryItemId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryPrintTaskRepository(
    private val inventoryDao: InventoryDao,
) : InventoryPrintTaskRepositoryPort {
    override suspend fun save(task: InventoryPrintTask): InventoryPrintTask {
        inventoryDao.upsertPrintTask(
            InventoryPrintTaskRecord(
                printTaskId = task.printTaskId,
                organizationId = task.organizationId,
                templateId = task.templateId,
                status = task.status.name,
                updatedAt = task.updatedAt.toString(),
                payloadJson = encodePayload(task),
            ),
        )
        return task
    }

    override suspend fun findByPrintTaskId(printTaskId: String): InventoryPrintTask? =
        inventoryDao.findPrintTask(printTaskId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryPrintTask> =
        inventoryDao.listPrintTasksByOrganization(organizationId).map { decodePayload(it.payloadJson) }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryPrintTask> =
        inventoryDao.listPrintTasksByItem(inventoryItemId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryScanEventRepository(
    private val inventoryDao: InventoryDao,
) : InventoryScanEventRepositoryPort {
    override suspend fun save(event: InventoryScanEvent): InventoryScanEvent {
        inventoryDao.upsertScanEvent(
            InventoryScanEventRecord(
                scanEventId = event.scanEventId,
                organizationId = event.organizationId,
                inventoryItemId = event.inventoryItemId,
                scannedAt = event.scannedAt.toString(),
                payloadJson = encodePayload(event),
            ),
        )
        return event
    }

    override suspend fun listByOrganization(organizationId: String): List<InventoryScanEvent> =
        inventoryDao.listScanEvents(organizationId).map { decodePayload(it.payloadJson) }

    override suspend fun listByItem(inventoryItemId: String): List<InventoryScanEvent> =
        inventoryDao.listScanEventsByItem(inventoryItemId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryRevisionRepository(
    private val inventoryDao: InventoryDao,
) : InventoryRevisionRepositoryPort {
    override suspend fun save(revision: InventoryRevision): InventoryRevision {
        inventoryDao.upsertRevision(
            InventoryRevisionRecord(
                entityId = revision.entityId,
                organizationId = revision.organizationId,
                entityType = revision.entityType.name,
                updatedAt = revision.updatedAt.toString(),
                payloadJson = encodePayload(revision),
            ),
        )
        return revision
    }

    override suspend fun findByEntity(entityId: String): InventoryRevision? =
        inventoryDao.findRevision(entityId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryRevision> =
        inventoryDao.listRevisions(organizationId).map { decodePayload(it.payloadJson) }
}

internal class RoomInventoryConflictRepository(
    private val inventoryDao: InventoryDao,
) : InventoryConflictRepositoryPort {
    override suspend fun save(conflict: InventoryConflict): InventoryConflict {
        inventoryDao.upsertConflict(
            InventoryConflictRecord(
                conflictId = conflict.conflictId,
                organizationId = conflict.organizationId,
                entityId = conflict.entityId,
                entityType = conflict.entityType.name,
                detectedAt = conflict.detectedAt.toString(),
                payloadJson = encodePayload(conflict),
            ),
        )
        return conflict
    }

    override suspend fun findByConflictId(conflictId: String): InventoryConflict? =
        inventoryDao.findConflict(conflictId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByOrganization(organizationId: String): List<InventoryConflict> =
        inventoryDao.listConflicts(organizationId).map { decodePayload(it.payloadJson) }
}
