package org.expert.link.mesh.application.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.inventory.InventoryAttachmentType
import org.expert.link.mesh.domain.model.inventory.InventoryCondition
import org.expert.link.mesh.domain.model.inventory.InventoryItemType
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.domain.model.security.CryptoMaterialRef
import org.expert.link.mesh.domain.model.security.CryptoStorageType
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAttachmentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryChangeLogRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCommentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryItemRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryQrCodeRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryRoleRepositoryAdapter
import org.junit.jupiter.api.Test

class InventoryItemServiceTest {
    @Test
    fun `should move item through lifecycle`() = runTest {
        val localProfileService = mockk<LocalProfileService>()
        coEvery { localProfileService.require() } returns localProfile("alice", "peer-1")
        val roleRepository = InMemoryRoleRepositoryAdapter()
        val memberRepository = InMemoryOrganizationMemberRepositoryAdapter()
        val rbacService = InventoryRbacService(roleRepository, memberRepository)
        seedRolesAndMember(roleRepository, memberRepository, "org-1", "peer-1")
        val eventRepository = InMemoryInventoryEventRepositoryAdapter()
        val eventService = InventoryEventService(eventRepository)
        val syncService = mockk<InventorySyncService>()
        coEvery { syncService.broadcastEvent(any()) } returns Unit
        val changeLogService = InventoryChangeLogService(
            changeLogRepositoryPort = InMemoryInventoryChangeLogRepositoryAdapter(),
            inventoryEventService = eventService,
            inventorySyncService = syncService,
        )

        val service = InventoryItemService(
            localProfileService = localProfileService,
            itemRepositoryPort = InMemoryInventoryItemRepositoryAdapter(),
            commentRepositoryPort = InMemoryInventoryCommentRepositoryAdapter(),
            attachmentRepositoryPort = InMemoryInventoryAttachmentRepositoryAdapter(),
            qrCodeRepositoryPort = InMemoryInventoryQrCodeRepositoryAdapter(),
            codeRepositoryPort = InMemoryInventoryCodeRepositoryAdapter(),
            rbacService = rbacService,
            inventoryEventService = eventService,
            inventoryChangeLogService = changeLogService,
            inventorySyncService = syncService,
        )

        val item = service.createItem(
            organizationId = "org-1",
            inventoryNumber = "INV-1",
            localNumber = null,
            qrCode = "QR-1",
            barcode = "BAR-1",
            categoryId = null,
            subcategoryId = null,
            itemType = InventoryItemType.UNKNOWN,
            title = "Проектор",
            description = "Учебный проектор",
            brand = null,
            model = null,
            serialNumber = "SN-1",
            manufacturer = null,
            purchaseDate = null,
            commissioningDate = null,
            warrantyUntil = null,
            depreciationGroup = null,
            usefulLifeMonths = null,
            condition = InventoryCondition.GOOD,
            locationId = null,
            responsiblePerson = "Alice",
            responsibleDepartment = "IT",
            responsibleUserId = null,
            ownerOrganizationId = null,
            ownerId = null,
            departmentId = null,
            costCenterId = null,
            legalHolderId = null,
            supplierId = null,
            fundingSourceId = null,
            lastInventoryAt = null,
            nextInventoryAt = null,
            tagIds = emptySet(),
            attributes = emptyList(),
            metadata = emptyMap(),
        )
        val added = service.updateStatus(item.inventoryItemId, item.revision, InventoryStatus.ADDED, null)
        val underReview = service.updateStatus(item.inventoryItemId, added?.revision, InventoryStatus.UNDER_REVIEW, null)
        val confirmed = service.updateStatus(item.inventoryItemId, underReview?.revision, InventoryStatus.CONFIRMED, "OK")

        assertThat(item.currentStatus).isEqualTo(InventoryStatus.DRAFT)
        assertThat(confirmed?.currentStatus).isEqualTo(InventoryStatus.CONFIRMED)
        assertThat(eventRepository.listByOrganization("org-1")).isNotEmpty
    }

    @Test
    fun `should attach file metadata to item`() = runTest {
        val localProfileService = mockk<LocalProfileService>()
        coEvery { localProfileService.require() } returns localProfile("alice", "peer-1")
        val roleRepository = InMemoryRoleRepositoryAdapter()
        val memberRepository = InMemoryOrganizationMemberRepositoryAdapter()
        val rbacService = InventoryRbacService(roleRepository, memberRepository)
        seedRolesAndMember(roleRepository, memberRepository, "org-1", "peer-1")
        val eventRepository = InMemoryInventoryEventRepositoryAdapter()
        val eventService = InventoryEventService(eventRepository)
        val syncService = mockk<InventorySyncService>()
        coEvery { syncService.broadcastEvent(any()) } returns Unit
        val changeLogService = InventoryChangeLogService(
            changeLogRepositoryPort = InMemoryInventoryChangeLogRepositoryAdapter(),
            inventoryEventService = eventService,
            inventorySyncService = syncService,
        )

        val itemRepository = InMemoryInventoryItemRepositoryAdapter()
        val service = InventoryItemService(
            localProfileService = localProfileService,
            itemRepositoryPort = itemRepository,
            commentRepositoryPort = InMemoryInventoryCommentRepositoryAdapter(),
            attachmentRepositoryPort = InMemoryInventoryAttachmentRepositoryAdapter(),
            qrCodeRepositoryPort = InMemoryInventoryQrCodeRepositoryAdapter(),
            codeRepositoryPort = InMemoryInventoryCodeRepositoryAdapter(),
            rbacService = rbacService,
            inventoryEventService = eventService,
            inventoryChangeLogService = changeLogService,
            inventorySyncService = syncService,
        )

        val item = service.createItem(
            organizationId = "org-1",
            inventoryNumber = "INV-2",
            localNumber = null,
            qrCode = null,
            barcode = null,
            categoryId = null,
            subcategoryId = null,
            itemType = InventoryItemType.UNKNOWN,
            title = "Сканер",
            description = null,
            brand = null,
            model = null,
            serialNumber = "SN-2",
            manufacturer = null,
            purchaseDate = null,
            commissioningDate = null,
            warrantyUntil = null,
            depreciationGroup = null,
            usefulLifeMonths = null,
            condition = InventoryCondition.GOOD,
            locationId = null,
            responsiblePerson = null,
            responsibleDepartment = null,
            responsibleUserId = null,
            ownerOrganizationId = null,
            ownerId = null,
            departmentId = null,
            costCenterId = null,
            legalHolderId = null,
            supplierId = null,
            fundingSourceId = null,
            lastInventoryAt = null,
            nextInventoryAt = null,
            tagIds = emptySet(),
            attributes = emptyList(),
            metadata = emptyMap(),
        )
        val attachment = service.addAttachment(
            itemId = item.inventoryItemId,
            sessionId = null,
            descriptor = FileDescriptor("file-1", "photo.jpg", 100, "hash", "image/jpeg"),
            transferId = "transfer-1",
            attachmentType = InventoryAttachmentType.PHOTO,
            note = "front view",
        )

        val updated = itemRepository.findByInventoryItemId(item.inventoryItemId)
        assertThat(attachment.attachmentId).isNotBlank()
        assertThat(updated?.photoAttachmentIds).contains(attachment.attachmentId)
    }

    private suspend fun seedRolesAndMember(
        roleRepository: InMemoryRoleRepositoryAdapter,
        memberRepository: InMemoryOrganizationMemberRepositoryAdapter,
        organizationId: String,
        peerId: String,
    ) {
        val role = org.expert.link.mesh.domain.model.inventory.Role(
            roleId = "role-$organizationId-admin",
            organizationId = organizationId,
            name = "Admin",
            description = null,
            permissions = InventoryPermission.entries.toSet(),
            system = false,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        roleRepository.save(role)
        memberRepository.save(
            org.expert.link.mesh.domain.model.inventory.OrganizationMember(
                organizationId = organizationId,
                peerId = peerId,
                displayName = "Alice",
                roleIds = setOf(role.roleId),
                status = org.expert.link.mesh.domain.model.inventory.OrganizationMemberStatus.ACTIVE,
                joinedAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
    }

    private fun localProfile(displayName: String, peerId: String): LocalProfile = LocalProfile(
        peerId = peerId,
        displayName = displayName,
        publicKey = "public-$peerId",
        privateKey = "private-$peerId",
        keyMaterialRef = CryptoMaterialRef(
            alias = "alias-$peerId",
            storageType = CryptoStorageType.IN_MEMORY,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        ),
        capabilities = setOf("inventory"),
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
