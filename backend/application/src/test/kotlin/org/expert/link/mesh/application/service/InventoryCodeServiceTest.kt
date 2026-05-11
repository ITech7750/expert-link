package org.expert.link.mesh.application.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.inventory.InventoryCodeType
import org.expert.link.mesh.domain.model.inventory.InventoryCondition
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryItemType
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventoryScanResultStatus
import org.expert.link.mesh.domain.model.security.CryptoMaterialRef
import org.expert.link.mesh.domain.model.security.CryptoStorageType
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAttachmentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryChangeLogRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeBindingRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCommentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryItemRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryQrCodeRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryScanEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryRoleRepositoryAdapter
import org.junit.jupiter.api.Test

class InventoryCodeServiceTest {
    @Test
    fun `should generate unique barcode and versioned qr payload`() = runTest {
        val fixture = createFixture()
        val itemA = fixture.createItem("org-1", "INV-100", "Ноутбук A")
        val itemB = fixture.createItem("org-1", "INV-101", "Ноутбук B")

        val barcodeA = fixture.codeService.generateCode(
            organizationId = "org-1",
            inventoryItemId = itemA.inventoryItemId,
            codeType = InventoryCodeType.BARCODE,
        )
        val barcodeB = fixture.codeService.generateCode(
            organizationId = "org-1",
            inventoryItemId = itemB.inventoryItemId,
            codeType = InventoryCodeType.BARCODE,
        )
        val qrA = fixture.codeService.generateCode(
            organizationId = "org-1",
            inventoryItemId = itemA.inventoryItemId,
            codeType = InventoryCodeType.QR,
        )

        assertThat(barcodeA.rawValue).startsWith("EL-")
        assertThat(barcodeB.rawValue).startsWith("EL-")
        assertThat(barcodeA.rawValue).isNotEqualTo(barcodeB.rawValue)
        assertThat(qrA.rawValue).isEqualTo("inventory:v1:org-1:${itemA.inventoryItemId}:${barcodeA.rawValue}")

        val updatedItem = fixture.itemRepository.findByInventoryItemId(itemA.inventoryItemId)
        assertThat(updatedItem?.barcode).isEqualTo(barcodeA.rawValue)
        assertThat(updatedItem?.qrCode).isEqualTo(qrA.rawValue)
    }

    @Test
    fun `should regenerate barcode and deactivate previous value`() = runTest {
        val fixture = createFixture()
        val item = fixture.createItem("org-1", "INV-200", "Проектор")
        val initial = fixture.codeService.generateCode("org-1", item.inventoryItemId, InventoryCodeType.BARCODE)

        val regenerated = fixture.codeService.regenerateCode(
            organizationId = "org-1",
            inventoryItemId = item.inventoryItemId,
            codeType = InventoryCodeType.BARCODE,
        )

        val allCodes = fixture.codeService.listCodes(item.inventoryItemId)
        assertThat(allCodes).hasSize(2)
        assertThat(allCodes.count { it.isActive }).isEqualTo(1)
        assertThat(allCodes.any { it.inventoryCodeId == initial.inventoryCodeId && !it.isActive }).isTrue
        assertThat(regenerated.rawValue).isNotEqualTo(initial.rawValue)

        val events = fixture.eventRepository.listByOrganization("org-1")
        assertThat(events.any { it.eventType == InventoryEventType.CODE_REGENERATED }).isTrue
    }

    @Test
    fun `should resolve inactive status for deactivated code`() = runTest {
        val fixture = createFixture()
        val item = fixture.createItem("org-1", "INV-300", "Сканер")
        val barcode = fixture.codeService.generateCode("org-1", item.inventoryItemId, InventoryCodeType.BARCODE)
        fixture.codeService.deactivateCode(barcode.inventoryCodeId)

        val resolution = fixture.codeService.resolveScannedCode("org-1", barcode.rawValue)

        assertThat(resolution.status).isEqualTo(InventoryScanResultStatus.INACTIVE)
        assertThat(resolution.item?.inventoryItemId).isEqualTo(item.inventoryItemId)
        assertThat(resolution.code?.isActive).isFalse
    }

    @Test
    fun `should register scan event with result status`() = runTest {
        val fixture = createFixture()
        val item = fixture.createItem("org-1", "INV-400", "Принтер")

        val event = fixture.codeService.recordScan(
            organizationId = "org-1",
            codeType = InventoryCodeType.BARCODE,
            codeValue = "EL-TEST1234",
            rawValue = "EL-TEST1234",
            inventoryItemId = item.inventoryItemId,
            locationHint = "Склад",
            resultStatus = InventoryScanResultStatus.NOT_FOUND,
            note = "Проверка вручную",
        )

        val stored = fixture.scanEventRepository.listByOrganization("org-1")
        assertThat(stored).hasSize(1)
        assertThat(stored.first().scanEventId).isEqualTo(event.scanEventId)
        assertThat(stored.first().resultStatus).isEqualTo(InventoryScanResultStatus.NOT_FOUND)
        assertThat(stored.first().locationHint).isEqualTo("Склад")
    }

    private suspend fun createFixture(): Fixture {
        val localProfileService = mockk<LocalProfileService>()
        coEvery { localProfileService.require() } returns localProfile("Alice", "peer-1")

        val roleRepository = InMemoryRoleRepositoryAdapter()
        val memberRepository = InMemoryOrganizationMemberRepositoryAdapter()
        seedRolesAndMember(roleRepository, memberRepository, "org-1", "peer-1")
        val rbacService = InventoryRbacService(roleRepository, memberRepository)

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
        val codeRepository = InMemoryInventoryCodeRepositoryAdapter()
        val itemService = InventoryItemService(
            localProfileService = localProfileService,
            itemRepositoryPort = itemRepository,
            commentRepositoryPort = InMemoryInventoryCommentRepositoryAdapter(),
            attachmentRepositoryPort = InMemoryInventoryAttachmentRepositoryAdapter(),
            qrCodeRepositoryPort = InMemoryInventoryQrCodeRepositoryAdapter(),
            codeRepositoryPort = codeRepository,
            rbacService = rbacService,
            inventoryEventService = eventService,
            inventoryChangeLogService = changeLogService,
            inventorySyncService = syncService,
        )

        val scanEventRepository = InMemoryInventoryScanEventRepositoryAdapter()
        val codeService = InventoryCodeService(
            localProfileService = localProfileService,
            codeBindingRepositoryPort = InMemoryInventoryCodeBindingRepositoryAdapter(),
            scanEventRepositoryPort = scanEventRepository,
            qrCodeRepositoryPort = InMemoryInventoryQrCodeRepositoryAdapter(),
            codeRepositoryPort = codeRepository,
            itemRepositoryPort = itemRepository,
            inventoryItemService = itemService,
            rbacService = rbacService,
            inventoryEventService = eventService,
            inventorySyncService = syncService,
        )

        return Fixture(
            codeService = codeService,
            itemService = itemService,
            itemRepository = itemRepository,
            scanEventRepository = scanEventRepository,
            eventRepository = eventRepository,
        )
    }

    private suspend fun Fixture.createItem(organizationId: String, number: String, title: String) =
        itemService.createItem(
            organizationId = organizationId,
            inventoryNumber = number,
            localNumber = null,
            qrCode = null,
            barcode = null,
            categoryId = null,
            subcategoryId = null,
            itemType = InventoryItemType.UNKNOWN,
            title = title,
            description = null,
            brand = null,
            model = null,
            serialNumber = "$number-SN",
            manufacturer = null,
            purchaseDate = null,
            commissioningDate = null,
            warrantyUntil = null,
            depreciationGroup = null,
            usefulLifeMonths = null,
            condition = InventoryCondition.GOOD,
            locationId = null,
            responsiblePerson = "Иванов И.И.",
            responsibleDepartment = "ИТ",
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

    private data class Fixture(
        val codeService: InventoryCodeService,
        val itemService: InventoryItemService,
        val itemRepository: InMemoryInventoryItemRepositoryAdapter,
        val scanEventRepository: InMemoryInventoryScanEventRepositoryAdapter,
        val eventRepository: InMemoryInventoryEventRepositoryAdapter,
    )
}
