package org.expert.link.mesh.application.service

import kotlinx.datetime.Instant
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.inventory.InventoryAttachment
import org.expert.link.mesh.domain.model.inventory.InventoryAttachmentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryAttachmentType
import org.expert.link.mesh.domain.model.inventory.InventoryAttributeValue
import org.expert.link.mesh.domain.model.inventory.InventoryComment
import org.expert.link.mesh.domain.model.inventory.InventoryCommentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCondition
import org.expert.link.mesh.domain.model.inventory.InventoryBarcodeFormat
import org.expert.link.mesh.domain.model.inventory.InventoryCode
import org.expert.link.mesh.domain.model.inventory.InventoryCodeSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryFieldChange
import org.expert.link.mesh.domain.model.inventory.InventoryItem
import org.expert.link.mesh.domain.model.inventory.InventoryItemSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryItemType
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventoryQrCode
import org.expert.link.mesh.domain.model.inventory.InventoryQrSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.domain.model.inventory.InventorySyncStatus
import org.expert.link.mesh.domain.port.repository.InventoryAttachmentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCommentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCodeRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryQrCodeRepositoryPort

data class InventoryItemUpdate(
    val inventoryNumber: String? = null,
    val localNumber: String? = null,
    val qrCode: String? = null,
    val barcode: String? = null,
    val categoryId: String? = null,
    val subcategoryId: String? = null,
    val itemType: InventoryItemType? = null,
    val title: String? = null,
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val manufacturer: String? = null,
    val purchaseDate: Instant? = null,
    val commissioningDate: Instant? = null,
    val warrantyUntil: Instant? = null,
    val depreciationGroup: String? = null,
    val usefulLifeMonths: Int? = null,
    val condition: InventoryCondition? = null,
    val locationId: String? = null,
    val responsiblePerson: String? = null,
    val responsibleDepartment: String? = null,
    val responsibleUserId: String? = null,
    val responsibleOwnerIds: Set<String>? = null,
    val ownerOrganizationId: String? = null,
    val ownerId: String? = null,
    val departmentId: String? = null,
    val costCenterId: String? = null,
    val legalHolderId: String? = null,
    val supplierId: String? = null,
    val fundingSourceId: String? = null,
    val lastInventoryAt: Instant? = null,
    val nextInventoryAt: Instant? = null,
    val tagIds: Set<String>? = null,
    val attributes: List<InventoryAttributeValue>? = null,
    val syncStatus: InventorySyncStatus? = null,
    val chatId: String? = null,
    val threadRootMessageId: String? = null,
    val metadata: Map<String, String>? = null,
)

class InventoryItemService(
    private val localProfileService: LocalProfileService,
    private val itemRepositoryPort: InventoryItemRepositoryPort,
    private val commentRepositoryPort: InventoryCommentRepositoryPort,
    private val attachmentRepositoryPort: InventoryAttachmentRepositoryPort,
    private val qrCodeRepositoryPort: InventoryQrCodeRepositoryPort,
    private val codeRepositoryPort: InventoryCodeRepositoryPort,
    private val rbacService: InventoryRbacService,
    private val inventoryEventService: InventoryEventService,
    private val inventoryChangeLogService: InventoryChangeLogService,
    private val inventorySyncService: InventorySyncService,
) {
    suspend fun createItem(
        organizationId: String,
        inventoryNumber: String,
        localNumber: String?,
        qrCode: String?,
        barcode: String?,
        categoryId: String?,
        subcategoryId: String?,
        itemType: InventoryItemType,
        title: String,
        description: String?,
        brand: String?,
        model: String?,
        serialNumber: String?,
        manufacturer: String?,
        purchaseDate: Instant?,
        commissioningDate: Instant?,
        warrantyUntil: Instant?,
        depreciationGroup: String?,
        usefulLifeMonths: Int?,
        condition: InventoryCondition,
        locationId: String?,
        responsiblePerson: String?,
        responsibleDepartment: String?,
        responsibleUserId: String?,
        responsibleOwnerIds: Set<String> = emptySet(),
        ownerOrganizationId: String?,
        ownerId: String?,
        departmentId: String?,
        costCenterId: String?,
        legalHolderId: String?,
        supplierId: String?,
        fundingSourceId: String?,
        lastInventoryAt: Instant?,
        nextInventoryAt: Instant?,
        tagIds: Set<String>,
        attributes: List<InventoryAttributeValue>,
        metadata: Map<String, String>,
    ): InventoryItem {
        require(inventoryNumber.isNotBlank()) { "Inventory number is blank" }
        require(title.isNotBlank()) { "Inventory title is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.ITEM_CREATE)
        val now = now()
        val item = InventoryItem(
            inventoryItemId = newId("item"),
            organizationId = organizationId,
            inventoryNumber = inventoryNumber,
            localNumber = localNumber,
            qrCode = qrCode,
            barcode = barcode,
            categoryId = categoryId,
            subcategoryId = subcategoryId,
            itemType = itemType,
            title = title,
            description = description,
            brand = brand,
            model = model,
            serialNumber = serialNumber,
            manufacturer = manufacturer,
            purchaseDate = purchaseDate,
            commissioningDate = commissioningDate,
            warrantyUntil = warrantyUntil,
            depreciationGroup = depreciationGroup,
            usefulLifeMonths = usefulLifeMonths,
            condition = condition,
            locationId = locationId,
            responsiblePerson = responsiblePerson,
            responsibleDepartment = responsibleDepartment,
            responsibleUserId = responsibleUserId,
            responsibleOwnerIds = responsibleOwnerIds,
            ownerOrganizationId = ownerOrganizationId,
            ownerId = ownerId,
            departmentId = departmentId,
            costCenterId = costCenterId,
            legalHolderId = legalHolderId,
            supplierId = supplierId,
            fundingSourceId = fundingSourceId,
            createdByPeerId = localProfile.peerId,
            createdAt = now,
            updatedAt = now,
            lastInventoryAt = lastInventoryAt,
            nextInventoryAt = nextInventoryAt,
            tagIds = tagIds,
            attributes = attributes,
            metadata = metadata,
            currentStatus = InventoryStatus.DRAFT,
            revision = 1,
        )
        itemRepositoryPort.save(item)
        qrCode?.let { registerQrCode(organizationId, item.inventoryItemId, it, barcode, localProfile.peerId) }
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.ITEM,
                entityId = item.inventoryItemId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryItemSnapshot(item),
                entityRevision = item.revision,
            ),
        )
        return item
    }

    suspend fun updateItem(itemId: String, expectedRevision: Long?, update: InventoryItemUpdate): InventoryItem? {
        val current = itemRepositoryPort.findByInventoryItemId(itemId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.ITEM_EDIT)
        if (expectedRevision != null && expectedRevision != current.revision) {
            return markConflict(current, localProfile.peerId, "Revision mismatch: expected=$expectedRevision actual=${current.revision}")
        }
        val updated = current.copy(
            inventoryNumber = update.inventoryNumber ?: current.inventoryNumber,
            localNumber = update.localNumber ?: current.localNumber,
            qrCode = update.qrCode ?: current.qrCode,
            barcode = update.barcode ?: current.barcode,
            categoryId = update.categoryId ?: current.categoryId,
            subcategoryId = update.subcategoryId ?: current.subcategoryId,
            itemType = update.itemType ?: current.itemType,
            title = update.title ?: current.title,
            description = update.description ?: current.description,
            brand = update.brand ?: current.brand,
            model = update.model ?: current.model,
            serialNumber = update.serialNumber ?: current.serialNumber,
            manufacturer = update.manufacturer ?: current.manufacturer,
            purchaseDate = update.purchaseDate ?: current.purchaseDate,
            commissioningDate = update.commissioningDate ?: current.commissioningDate,
            warrantyUntil = update.warrantyUntil ?: current.warrantyUntil,
            depreciationGroup = update.depreciationGroup ?: current.depreciationGroup,
            usefulLifeMonths = update.usefulLifeMonths ?: current.usefulLifeMonths,
            condition = update.condition ?: current.condition,
            locationId = update.locationId ?: current.locationId,
            responsiblePerson = update.responsiblePerson ?: current.responsiblePerson,
            responsibleDepartment = update.responsibleDepartment ?: current.responsibleDepartment,
            responsibleUserId = update.responsibleUserId ?: current.responsibleUserId,
            responsibleOwnerIds = update.responsibleOwnerIds ?: current.responsibleOwnerIds,
            ownerOrganizationId = update.ownerOrganizationId ?: current.ownerOrganizationId,
            ownerId = update.ownerId ?: current.ownerId,
            departmentId = update.departmentId ?: current.departmentId,
            costCenterId = update.costCenterId ?: current.costCenterId,
            legalHolderId = update.legalHolderId ?: current.legalHolderId,
            supplierId = update.supplierId ?: current.supplierId,
            fundingSourceId = update.fundingSourceId ?: current.fundingSourceId,
            lastInventoryAt = update.lastInventoryAt ?: current.lastInventoryAt,
            nextInventoryAt = update.nextInventoryAt ?: current.nextInventoryAt,
            tagIds = update.tagIds ?: current.tagIds,
            attributes = update.attributes ?: current.attributes,
            syncStatus = update.syncStatus ?: current.syncStatus,
            chatId = update.chatId ?: current.chatId,
            threadRootMessageId = update.threadRootMessageId ?: current.threadRootMessageId,
            metadata = update.metadata ?: current.metadata,
            updatedAt = now(),
            revision = current.revision + 1,
        )
        itemRepositoryPort.save(updated)
        if (update.qrCode != null || update.barcode != null) {
            registerQrCode(updated.organizationId, updated.inventoryItemId, updated.qrCode ?: "", updated.barcode, localProfile.peerId)
        }
        val changes = buildFieldChanges(current, update)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.ITEM,
                entityId = updated.inventoryItemId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryItemSnapshot(updated),
                entityRevision = updated.revision,
                previousEntityRevision = current.revision,
            ),
        )
        if (changes.isNotEmpty()) {
            inventoryChangeLogService.recordChange(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.ITEM,
                entityId = updated.inventoryItemId,
                changes = changes,
                actorPeerId = localProfile.peerId,
                reason = "update",
                sessionId = null,
            )
        }
        return updated
    }

    suspend fun updateStatus(itemId: String, expectedRevision: Long?, status: InventoryStatus, note: String?): InventoryItem? {
        val current = itemRepositoryPort.findByInventoryItemId(itemId) ?: return null
        val localProfile = localProfileService.require()
        requireStatusTransition(current.currentStatus, status)
        val permission = when (status) {
            InventoryStatus.UNDER_REVIEW -> InventoryPermission.ITEM_SUBMIT_REVIEW
            InventoryStatus.CONFIRMED -> InventoryPermission.ITEM_CONFIRM
            InventoryStatus.REJECTED -> InventoryPermission.ITEM_REJECT
            InventoryStatus.ARCHIVED -> InventoryPermission.ITEM_ARCHIVE
            else -> InventoryPermission.ITEM_EDIT
        }
        rbacService.requirePermission(current.organizationId, localProfile.peerId, permission)
        if (expectedRevision != null && expectedRevision != current.revision) {
            return markConflict(current, localProfile.peerId, "Revision mismatch: expected=$expectedRevision actual=${current.revision}")
        }
        val updated = current.copy(
            currentStatus = status,
            updatedAt = now(),
            revision = current.revision + 1,
        )
        itemRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.ITEM,
                entityId = updated.inventoryItemId,
                eventType = InventoryEventType.STATUS_CHANGED,
                actorPeerId = localProfile.peerId,
                payload = InventoryItemSnapshot(updated),
                entityRevision = updated.revision,
                previousEntityRevision = current.revision,
                notes = note,
            ),
        )
        inventoryChangeLogService.recordChange(
            organizationId = updated.organizationId,
            entityType = InventoryEntityType.ITEM,
            entityId = updated.inventoryItemId,
            changes = listOf(
                InventoryFieldChange(
                    field = "currentStatus",
                    previousValue = current.currentStatus.name,
                    newValue = updated.currentStatus.name,
                ),
            ),
            actorPeerId = localProfile.peerId,
            reason = note ?: "status-change",
            sessionId = null,
        )
        return updated
    }

    suspend fun addComment(
        itemId: String,
        sessionId: String?,
        body: String,
    ): InventoryComment {
        require(body.isNotBlank()) { "Comment body is blank" }
        val item = itemRepositoryPort.findByInventoryItemId(itemId)
            ?: error("Inventory item $itemId not found")
        val localProfile = localProfileService.require()
        rbacService.requirePermission(item.organizationId, localProfile.peerId, InventoryPermission.ITEM_COMMENT)
        val now = now()
        val comment = InventoryComment(
            commentId = newId("comment"),
            organizationId = item.organizationId,
            inventoryItemId = itemId,
            sessionId = sessionId,
            authorPeerId = localProfile.peerId,
            body = body,
            createdAt = now,
            updatedAt = now,
        )
        commentRepositoryPort.save(comment)
        val updatedItem = item.copy(
            commentIds = (item.commentIds + comment.commentId).distinct(),
            updatedAt = now,
            revision = item.revision + 1,
        )
        itemRepositoryPort.save(updatedItem)
        publish(
            inventoryEventService.recordEvent(
                organizationId = item.organizationId,
                entityType = InventoryEntityType.COMMENT,
                entityId = comment.commentId,
                eventType = InventoryEventType.COMMENT_ADDED,
                actorPeerId = localProfile.peerId,
                payload = InventoryCommentSnapshot(comment),
                entityRevision = updatedItem.revision,
                previousEntityRevision = item.revision,
                sessionId = sessionId,
            ),
        )
        return comment
    }

    suspend fun addAttachment(
        itemId: String,
        sessionId: String?,
        descriptor: FileDescriptor,
        transferId: String?,
        attachmentType: InventoryAttachmentType,
        note: String?,
    ): InventoryAttachment {
        val item = itemRepositoryPort.findByInventoryItemId(itemId)
            ?: error("Inventory item $itemId not found")
        val localProfile = localProfileService.require()
        val permission = if (attachmentType == InventoryAttachmentType.PHOTO) {
            InventoryPermission.ITEM_UPLOAD_PHOTO
        } else {
            InventoryPermission.ITEM_ATTACH
        }
        rbacService.requirePermission(item.organizationId, localProfile.peerId, permission)
        val now = now()
        val attachment = InventoryAttachment(
            attachmentId = newId("attach"),
            organizationId = item.organizationId,
            inventoryItemId = itemId,
            sessionId = sessionId,
            uploadedByPeerId = localProfile.peerId,
            descriptor = descriptor,
            transferId = transferId,
            attachmentType = attachmentType,
            createdAt = now,
            updatedAt = now,
            note = note,
        )
        attachmentRepositoryPort.save(attachment)
        val updatedItem = item.copy(
            attachmentIds = (item.attachmentIds + attachment.attachmentId).distinct(),
            photoAttachmentIds = if (attachmentType == InventoryAttachmentType.PHOTO) {
                (item.photoAttachmentIds + attachment.attachmentId).distinct()
            } else {
                item.photoAttachmentIds
            },
            updatedAt = now,
            revision = item.revision + 1,
        )
        itemRepositoryPort.save(updatedItem)
        publish(
            inventoryEventService.recordEvent(
                organizationId = item.organizationId,
                entityType = InventoryEntityType.ATTACHMENT,
                entityId = attachment.attachmentId,
                eventType = InventoryEventType.ATTACHMENT_ADDED,
                actorPeerId = localProfile.peerId,
                payload = InventoryAttachmentSnapshot(attachment),
                entityRevision = updatedItem.revision,
                previousEntityRevision = item.revision,
                sessionId = sessionId,
            ),
        )
        return attachment
    }

    suspend fun linkDiscussion(itemId: String, chatId: String?, threadRootMessageId: String?): InventoryItem? {
        val current = itemRepositoryPort.findByInventoryItemId(itemId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.ITEM_EDIT)
        val updated = current.copy(
            chatId = chatId ?: current.chatId,
            threadRootMessageId = threadRootMessageId ?: current.threadRootMessageId,
            updatedAt = now(),
            revision = current.revision + 1,
        )
        itemRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.ITEM,
                entityId = updated.inventoryItemId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryItemSnapshot(updated),
                entityRevision = updated.revision,
                previousEntityRevision = current.revision,
            ),
        )
        return updated
    }

    private suspend fun markConflict(item: InventoryItem, actorPeerId: String, note: String): InventoryItem {
        val updated = item.copy(
            currentStatus = InventoryStatus.REQUIRES_UPDATE,
            syncStatus = InventorySyncStatus.CONFLICTED,
            updatedAt = now(),
            revision = item.revision + 1,
        )
        itemRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = item.organizationId,
                entityType = InventoryEntityType.ITEM,
                entityId = item.inventoryItemId,
                eventType = InventoryEventType.CONFLICT_DETECTED,
                actorPeerId = actorPeerId,
                payload = InventoryItemSnapshot(updated),
                entityRevision = updated.revision,
                previousEntityRevision = item.revision,
                conflict = true,
                notes = note,
            ),
        )
        return updated
    }

    private fun buildFieldChanges(current: InventoryItem, update: InventoryItemUpdate): List<InventoryFieldChange> {
        val changes = mutableListOf<InventoryFieldChange>()
        fun add(field: String, previous: Any?, next: Any?) {
            if (previous != next) {
                changes += InventoryFieldChange(field, previous?.toString(), next?.toString())
            }
        }
        update.inventoryNumber?.let { add("inventoryNumber", current.inventoryNumber, it) }
        update.localNumber?.let { add("localNumber", current.localNumber, it) }
        update.qrCode?.let { add("qrCode", current.qrCode, it) }
        update.barcode?.let { add("barcode", current.barcode, it) }
        update.categoryId?.let { add("categoryId", current.categoryId, it) }
        update.subcategoryId?.let { add("subcategoryId", current.subcategoryId, it) }
        update.itemType?.let { add("itemType", current.itemType, it) }
        update.title?.let { add("title", current.title, it) }
        update.description?.let { add("description", current.description, it) }
        update.brand?.let { add("brand", current.brand, it) }
        update.model?.let { add("model", current.model, it) }
        update.serialNumber?.let { add("serialNumber", current.serialNumber, it) }
        update.manufacturer?.let { add("manufacturer", current.manufacturer, it) }
        update.purchaseDate?.let { add("purchaseDate", current.purchaseDate, it) }
        update.commissioningDate?.let { add("commissioningDate", current.commissioningDate, it) }
        update.warrantyUntil?.let { add("warrantyUntil", current.warrantyUntil, it) }
        update.depreciationGroup?.let { add("depreciationGroup", current.depreciationGroup, it) }
        update.usefulLifeMonths?.let { add("usefulLifeMonths", current.usefulLifeMonths, it) }
        update.condition?.let { add("condition", current.condition, it) }
        update.locationId?.let { add("locationId", current.locationId, it) }
        update.responsiblePerson?.let { add("responsiblePerson", current.responsiblePerson, it) }
        update.responsibleDepartment?.let { add("responsibleDepartment", current.responsibleDepartment, it) }
        update.responsibleUserId?.let { add("responsibleUserId", current.responsibleUserId, it) }
        update.responsibleOwnerIds?.let { add("responsibleOwnerIds", current.responsibleOwnerIds, it) }
        update.ownerOrganizationId?.let { add("ownerOrganizationId", current.ownerOrganizationId, it) }
        update.ownerId?.let { add("ownerId", current.ownerId, it) }
        update.departmentId?.let { add("departmentId", current.departmentId, it) }
        update.costCenterId?.let { add("costCenterId", current.costCenterId, it) }
        update.legalHolderId?.let { add("legalHolderId", current.legalHolderId, it) }
        update.supplierId?.let { add("supplierId", current.supplierId, it) }
        update.fundingSourceId?.let { add("fundingSourceId", current.fundingSourceId, it) }
        update.lastInventoryAt?.let { add("lastInventoryAt", current.lastInventoryAt, it) }
        update.nextInventoryAt?.let { add("nextInventoryAt", current.nextInventoryAt, it) }
        update.tagIds?.let { add("tagIds", current.tagIds, it) }
        update.attributes?.let {
            val currentMap = current.attributes.associate { attr -> attr.attributeId to attr.value }
            val updatedMap = it.associate { attr -> attr.attributeId to attr.value }
            add("attributes", currentMap, updatedMap)
        }
        update.syncStatus?.let { add("syncStatus", current.syncStatus, it) }
        update.chatId?.let { add("chatId", current.chatId, it) }
        update.threadRootMessageId?.let { add("threadRootMessageId", current.threadRootMessageId, it) }
        update.metadata?.let { add("metadata", current.metadata, it) }
        return changes
    }

    private fun requireStatusTransition(current: InventoryStatus, target: InventoryStatus) {
        val allowed = STATUS_TRANSITIONS[current].orEmpty()
        require(target in allowed) { "Invalid status transition: $current -> $target" }
    }

    private suspend fun registerQrCode(
        organizationId: String,
        itemId: String,
        qrCode: String,
        barcode: String?,
        actorPeerId: String,
    ) {
        if (qrCode.isBlank() && barcode.isNullOrBlank()) {
            return
        }
        val item = itemRepositoryPort.findByInventoryItemId(itemId)
        if (qrCode.isNotBlank()) {
            val existing = codeRepositoryPort.findByRawValue(qrCode)
            if (existing == null) {
                val code = InventoryCode(
                    inventoryCodeId = newId("code"),
                    inventoryItemId = itemId,
                    organizationId = organizationId,
                    codeType = org.expert.link.mesh.domain.model.inventory.InventoryCodeType.QR,
                    barcodeFormat = null,
                    rawValue = qrCode,
                    displayValue = item?.inventoryNumber ?: qrCode,
                    isActive = true,
                    createdAt = now(),
                    createdByPeerId = actorPeerId,
                    version = 1,
                )
                codeRepositoryPort.save(code)
                publish(
                    inventoryEventService.recordEvent(
                        organizationId = organizationId,
                        entityType = InventoryEntityType.CODE,
                        entityId = code.inventoryCodeId,
                        eventType = InventoryEventType.CODE_GENERATED,
                        actorPeerId = actorPeerId,
                        payload = InventoryCodeSnapshot(code),
                    ),
                )
            }
        }
        val resolvedBarcode = barcode?.trim().orEmpty()
        if (resolvedBarcode.isNotBlank()) {
            val existing = codeRepositoryPort.findByRawValue(resolvedBarcode)
            if (existing == null) {
                val code = InventoryCode(
                    inventoryCodeId = newId("code"),
                    inventoryItemId = itemId,
                    organizationId = organizationId,
                    codeType = org.expert.link.mesh.domain.model.inventory.InventoryCodeType.BARCODE,
                    barcodeFormat = InventoryBarcodeFormat.CODE_128,
                    rawValue = resolvedBarcode,
                    displayValue = item?.inventoryNumber ?: resolvedBarcode,
                    isActive = true,
                    createdAt = now(),
                    createdByPeerId = actorPeerId,
                    version = 1,
                )
                codeRepositoryPort.save(code)
                publish(
                    inventoryEventService.recordEvent(
                        organizationId = organizationId,
                        entityType = InventoryEntityType.CODE,
                        entityId = code.inventoryCodeId,
                        eventType = InventoryEventType.CODE_GENERATED,
                        actorPeerId = actorPeerId,
                        payload = InventoryCodeSnapshot(code),
                    ),
                )
            }
        }
        val qr = InventoryQrCode(
            codeId = newId("qr"),
            organizationId = organizationId,
            inventoryItemId = itemId,
            qrCode = qrCode,
            barcode = resolvedBarcode.ifBlank { null },
            createdByPeerId = actorPeerId,
            createdAt = now(),
        )
        qrCodeRepositoryPort.save(qr)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.QRCODE,
                entityId = qr.codeId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = actorPeerId,
                payload = InventoryQrSnapshot(qr),
            ),
        )
    }

    private suspend fun publish(event: org.expert.link.mesh.domain.model.inventory.InventoryEvent) {
        inventorySyncService.broadcastEvent(event)
    }

    private companion object {
        val STATUS_TRANSITIONS: Map<InventoryStatus, Set<InventoryStatus>> = mapOf(
            InventoryStatus.DRAFT to setOf(InventoryStatus.ADDED, InventoryStatus.ARCHIVED),
            InventoryStatus.ADDED to setOf(InventoryStatus.UNDER_REVIEW, InventoryStatus.ARCHIVED),
            InventoryStatus.UNDER_REVIEW to setOf(
                InventoryStatus.CONFIRMED,
                InventoryStatus.REJECTED,
                InventoryStatus.REQUIRES_UPDATE,
            ),
            InventoryStatus.REQUIRES_UPDATE to setOf(InventoryStatus.UNDER_REVIEW, InventoryStatus.ARCHIVED),
            InventoryStatus.REJECTED to setOf(InventoryStatus.REQUIRES_UPDATE, InventoryStatus.ARCHIVED),
            InventoryStatus.CONFIRMED to setOf(InventoryStatus.ARCHIVED),
            InventoryStatus.ARCHIVED to emptySet(),
        )
    }
}
