package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.inventory.InventoryCostCenter
import org.expert.link.mesh.domain.model.inventory.InventoryCostCenterSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryDepartment
import org.expert.link.mesh.domain.model.inventory.InventoryDepartmentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryFundingSource
import org.expert.link.mesh.domain.model.inventory.InventoryFundingSourceSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLegalHolder
import org.expert.link.mesh.domain.model.inventory.InventoryLegalHolderSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryOwner
import org.expert.link.mesh.domain.model.inventory.InventoryOwnerSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryOwnerType
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventorySupplier
import org.expert.link.mesh.domain.model.inventory.InventorySupplierSnapshot
import org.expert.link.mesh.domain.port.repository.InventoryCostCenterRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryDepartmentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryFundingSourceRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLegalHolderRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryOwnerRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySupplierRepositoryPort

class InventoryOwnershipService(
    private val localProfileService: LocalProfileService,
    private val ownerRepositoryPort: InventoryOwnerRepositoryPort,
    private val departmentRepositoryPort: InventoryDepartmentRepositoryPort,
    private val costCenterRepositoryPort: InventoryCostCenterRepositoryPort,
    private val legalHolderRepositoryPort: InventoryLegalHolderRepositoryPort,
    private val supplierRepositoryPort: InventorySupplierRepositoryPort,
    private val fundingSourceRepositoryPort: InventoryFundingSourceRepositoryPort,
    private val rbacService: InventoryRbacService,
    private val inventoryEventService: InventoryEventService,
    private val inventorySyncService: InventorySyncService,
) {
    suspend fun listOwners(organizationId: String): List<InventoryOwner> =
        ownerRepositoryPort.listByOrganization(organizationId)

    suspend fun createOwner(
        organizationId: String,
        name: String,
        type: InventoryOwnerType,
        legalHolderId: String?,
        contactInfo: String?,
        code: String? = null,
        departmentId: String? = null,
        locationIds: Set<String> = emptySet(),
        archived: Boolean = false,
    ): InventoryOwner {
        require(name.isNotBlank()) { "Owner name is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.OWNER_MANAGE)
        val owner = InventoryOwner(
            ownerId = newId("owner"),
            organizationId = organizationId,
            name = name,
            type = type,
            legalHolderId = legalHolderId,
            contactInfo = contactInfo,
            code = code,
            departmentId = departmentId,
            locationIds = locationIds,
            archived = archived,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        ownerRepositoryPort.save(owner)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.OWNER,
                entityId = owner.ownerId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryOwnerSnapshot(owner),
            ),
        )
        return owner
    }

    suspend fun updateOwner(
        ownerId: String,
        name: String?,
        type: InventoryOwnerType?,
        legalHolderId: String?,
        contactInfo: String?,
        code: String? = null,
        departmentId: String? = null,
        locationIds: Set<String>? = null,
        archived: Boolean? = null,
    ): InventoryOwner? {
        val current = ownerRepositoryPort.findByOwnerId(ownerId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.OWNER_MANAGE)
        val updated = current.copy(
            name = name ?: current.name,
            type = type ?: current.type,
            legalHolderId = legalHolderId ?: current.legalHolderId,
            contactInfo = contactInfo ?: current.contactInfo,
            code = code ?: current.code,
            departmentId = departmentId ?: current.departmentId,
            locationIds = locationIds ?: current.locationIds,
            archived = archived ?: current.archived,
            updatedAt = now(),
        )
        ownerRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.OWNER,
                entityId = updated.ownerId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryOwnerSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun listDepartments(organizationId: String): List<InventoryDepartment> =
        departmentRepositoryPort.listByOrganization(organizationId)

    suspend fun createDepartment(
        organizationId: String,
        name: String,
        parentDepartmentId: String?,
        code: String?,
        locationIds: Set<String> = emptySet(),
        ownerIds: Set<String> = emptySet(),
        archived: Boolean = false,
    ): InventoryDepartment {
        require(name.isNotBlank()) { "Department name is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.OWNER_MANAGE)
        val department = InventoryDepartment(
            departmentId = newId("dept"),
            organizationId = organizationId,
            name = name,
            parentDepartmentId = parentDepartmentId,
            code = code,
            locationIds = locationIds,
            ownerIds = ownerIds,
            archived = archived,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        departmentRepositoryPort.save(department)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.DEPARTMENT,
                entityId = department.departmentId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryDepartmentSnapshot(department),
            ),
        )
        return department
    }

    suspend fun updateDepartment(
        departmentId: String,
        name: String?,
        parentDepartmentId: String?,
        code: String?,
        locationIds: Set<String>? = null,
        ownerIds: Set<String>? = null,
        archived: Boolean? = null,
    ): InventoryDepartment? {
        val current = departmentRepositoryPort.findByDepartmentId(departmentId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.OWNER_MANAGE)
        val updated = current.copy(
            name = name ?: current.name,
            parentDepartmentId = parentDepartmentId ?: current.parentDepartmentId,
            code = code ?: current.code,
            locationIds = locationIds ?: current.locationIds,
            ownerIds = ownerIds ?: current.ownerIds,
            archived = archived ?: current.archived,
            updatedAt = now(),
        )
        departmentRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.DEPARTMENT,
                entityId = updated.departmentId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryDepartmentSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun listCostCenters(organizationId: String): List<InventoryCostCenter> =
        costCenterRepositoryPort.listByOrganization(organizationId)

    suspend fun createCostCenter(
        organizationId: String,
        code: String,
        name: String,
        description: String?,
    ): InventoryCostCenter {
        require(code.isNotBlank()) { "Cost center code is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.OWNER_MANAGE)
        val costCenter = InventoryCostCenter(
            costCenterId = newId("cost"),
            organizationId = organizationId,
            code = code,
            name = name,
            description = description,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        costCenterRepositoryPort.save(costCenter)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.COST_CENTER,
                entityId = costCenter.costCenterId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryCostCenterSnapshot(costCenter),
            ),
        )
        return costCenter
    }

    suspend fun updateCostCenter(
        costCenterId: String,
        code: String?,
        name: String?,
        description: String?,
    ): InventoryCostCenter? {
        val current = costCenterRepositoryPort.findByCostCenterId(costCenterId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.OWNER_MANAGE)
        val updated = current.copy(
            code = code ?: current.code,
            name = name ?: current.name,
            description = description ?: current.description,
            updatedAt = now(),
        )
        costCenterRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.COST_CENTER,
                entityId = updated.costCenterId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryCostCenterSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun listLegalHolders(organizationId: String): List<InventoryLegalHolder> =
        legalHolderRepositoryPort.listByOrganization(organizationId)

    suspend fun createLegalHolder(
        organizationId: String,
        name: String,
        taxId: String?,
        registrationNumber: String?,
        address: String?,
        bankDetails: String?,
    ): InventoryLegalHolder {
        require(name.isNotBlank()) { "Legal holder name is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.OWNER_MANAGE)
        val holder = InventoryLegalHolder(
            legalHolderId = newId("holder"),
            organizationId = organizationId,
            name = name,
            taxId = taxId,
            registrationNumber = registrationNumber,
            address = address,
            bankDetails = bankDetails,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        legalHolderRepositoryPort.save(holder)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.LEGAL_HOLDER,
                entityId = holder.legalHolderId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryLegalHolderSnapshot(holder),
            ),
        )
        return holder
    }

    suspend fun updateLegalHolder(
        legalHolderId: String,
        name: String?,
        taxId: String?,
        registrationNumber: String?,
        address: String?,
        bankDetails: String?,
    ): InventoryLegalHolder? {
        val current = legalHolderRepositoryPort.findByLegalHolderId(legalHolderId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.OWNER_MANAGE)
        val updated = current.copy(
            name = name ?: current.name,
            taxId = taxId ?: current.taxId,
            registrationNumber = registrationNumber ?: current.registrationNumber,
            address = address ?: current.address,
            bankDetails = bankDetails ?: current.bankDetails,
            updatedAt = now(),
        )
        legalHolderRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.LEGAL_HOLDER,
                entityId = updated.legalHolderId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryLegalHolderSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun listSuppliers(organizationId: String): List<InventorySupplier> =
        supplierRepositoryPort.listByOrganization(organizationId)

    suspend fun createSupplier(
        organizationId: String,
        name: String,
        contactInfo: String?,
        bankDetails: String?,
    ): InventorySupplier {
        require(name.isNotBlank()) { "Supplier name is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.OWNER_MANAGE)
        val supplier = InventorySupplier(
            supplierId = newId("supplier"),
            organizationId = organizationId,
            name = name,
            contactInfo = contactInfo,
            bankDetails = bankDetails,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        supplierRepositoryPort.save(supplier)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.SUPPLIER,
                entityId = supplier.supplierId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventorySupplierSnapshot(supplier),
            ),
        )
        return supplier
    }

    suspend fun updateSupplier(
        supplierId: String,
        name: String?,
        contactInfo: String?,
        bankDetails: String?,
    ): InventorySupplier? {
        val current = supplierRepositoryPort.findBySupplierId(supplierId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.OWNER_MANAGE)
        val updated = current.copy(
            name = name ?: current.name,
            contactInfo = contactInfo ?: current.contactInfo,
            bankDetails = bankDetails ?: current.bankDetails,
            updatedAt = now(),
        )
        supplierRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.SUPPLIER,
                entityId = updated.supplierId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventorySupplierSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun listFundingSources(organizationId: String): List<InventoryFundingSource> =
        fundingSourceRepositoryPort.listByOrganization(organizationId)

    suspend fun createFundingSource(
        organizationId: String,
        name: String,
        description: String?,
    ): InventoryFundingSource {
        require(name.isNotBlank()) { "Funding source name is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.OWNER_MANAGE)
        val source = InventoryFundingSource(
            fundingSourceId = newId("fund"),
            organizationId = organizationId,
            name = name,
            description = description,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        fundingSourceRepositoryPort.save(source)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.FUNDING_SOURCE,
                entityId = source.fundingSourceId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryFundingSourceSnapshot(source),
            ),
        )
        return source
    }

    suspend fun updateFundingSource(
        fundingSourceId: String,
        name: String?,
        description: String?,
    ): InventoryFundingSource? {
        val current = fundingSourceRepositoryPort.findByFundingSourceId(fundingSourceId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.OWNER_MANAGE)
        val updated = current.copy(
            name = name ?: current.name,
            description = description ?: current.description,
            updatedAt = now(),
        )
        fundingSourceRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.FUNDING_SOURCE,
                entityId = updated.fundingSourceId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryFundingSourceSnapshot(updated),
            ),
        )
        return updated
    }

    private suspend fun publish(event: org.expert.link.mesh.domain.model.inventory.InventoryEvent) {
        inventorySyncService.broadcastEvent(event)
    }
}
