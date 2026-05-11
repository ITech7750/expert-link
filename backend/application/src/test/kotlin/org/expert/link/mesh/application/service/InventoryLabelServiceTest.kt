package org.expert.link.mesh.application.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.inventory.InventoryCodeType
import org.expert.link.mesh.domain.model.inventory.InventoryCondition
import org.expert.link.mesh.domain.model.inventory.InventoryItemType
import org.expert.link.mesh.domain.model.inventory.InventoryLabelFieldKey
import org.expert.link.mesh.domain.model.inventory.InventoryLabelTemplateType
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventoryPrintStatus
import org.expert.link.mesh.domain.model.inventory.InventoryScanResultStatus
import org.expert.link.mesh.domain.model.inventory.Organization
import org.expert.link.mesh.domain.model.security.CryptoMaterialRef
import org.expert.link.mesh.domain.model.security.CryptoStorageType
import org.expert.link.mesh.domain.port.external.InventoryLabelArtifact
import org.expert.link.mesh.domain.port.external.InventoryLabelRendererPort
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAttachmentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryChangeLogRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeBindingRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCommentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryDepartmentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryItemRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLabelRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLabelTemplateRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLocationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryPrintTaskRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryQrCodeRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryScanEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryRoleRepositoryAdapter
import org.junit.jupiter.api.Test

class InventoryLabelServiceTest {
    @Test
    fun `should seed default templates and allow selection`() = runTest {
        val fixture = createFixture()

        val templates = fixture.labelService.listTemplates("org-1")

        assertThat(templates).hasSize(3)
        assertThat(templates.map { it.templateType })
            .containsExactlyInAnyOrder(
                InventoryLabelTemplateType.SHORT,
                InventoryLabelTemplateType.STANDARD,
                InventoryLabelTemplateType.FULL,
            )
        val selected = fixture.labelService.selectDefaultTemplate("org-1", templates.first { it.templateType == InventoryLabelTemplateType.FULL }.templateId)
        assertThat(selected?.templateType).isEqualTo(InventoryLabelTemplateType.FULL)
    }

    @Test
    fun `should generate label preview with barcode and qr`() = runTest {
        val fixture = createFixture()
        val item = fixture.createItem("org-1", "INV-500", "Монитор", locationId = "loc-1")

        fixture.codeService.generateCode("org-1", item.inventoryItemId, InventoryCodeType.BARCODE)
        fixture.codeService.generateCode("org-1", item.inventoryItemId, InventoryCodeType.QR)
        val standardTemplate = fixture.labelService
            .listTemplates("org-1")
            .first { it.templateType == InventoryLabelTemplateType.STANDARD }

        val preview = fixture.labelService.generateLabelPreview(
            organizationId = "org-1",
            inventoryItemId = item.inventoryItemId,
            options = InventoryLabelGenerationOptions(
                templateId = standardTemplate.templateId,
                includeBarcode = true,
                includeQr = true,
            ),
        )

        assertThat(preview.label.barcodeValue).isNotBlank()
        assertThat(preview.label.qrValue).startsWith("inventory:v1:org-1")
        assertThat(preview.label.fields.map { it.key }).contains(
            InventoryLabelFieldKey.TITLE,
            InventoryLabelFieldKey.INVENTORY_NUMBER,
            InventoryLabelFieldKey.RESPONSIBLE_PERSON,
            InventoryLabelFieldKey.LOCATION,
        )
    }

    @Test
    fun `should generate pdf and print batch for selected items`() = runTest {
        val fixture = createFixture()
        val itemA = fixture.createItem("org-1", "INV-601", "Принтер A")
        val itemB = fixture.createItem("org-1", "INV-602", "Принтер B")
        fixture.codeService.generateCode("org-1", itemA.inventoryItemId, InventoryCodeType.BARCODE)
        fixture.codeService.generateCode("org-1", itemA.inventoryItemId, InventoryCodeType.QR)
        fixture.codeService.generateCode("org-1", itemB.inventoryItemId, InventoryCodeType.BARCODE)
        fixture.codeService.generateCode("org-1", itemB.inventoryItemId, InventoryCodeType.QR)

        val singlePdf = fixture.labelService.generateLabelPdf(
            organizationId = "org-1",
            inventoryItemId = itemA.inventoryItemId,
            options = InventoryLabelGenerationOptions(),
        )
        val batch = fixture.labelService.printLabelsBatch(
            organizationId = "org-1",
            itemIds = listOf(itemA.inventoryItemId, itemB.inventoryItemId),
            options = InventoryLabelGenerationOptions(includeBarcode = true, includeQr = true),
        )

        assertThat(singlePdf.task.status).isEqualTo(InventoryPrintStatus.GENERATED)
        assertThat(singlePdf.task.resultDescriptor?.fileName).endsWith(".pdf")
        assertThat(batch.task.status).isEqualTo(InventoryPrintStatus.PRINTED)
        assertThat(batch.labels).hasSize(2)
        assertThat(fixture.printTaskRepository.listByOrganization("org-1")).hasSizeGreaterThanOrEqualTo(2)
    }

    @Test
    fun `should keep integration item code label scan chain`() = runTest {
        val fixture = createFixture()
        val item = fixture.createItem("org-1", "INV-777", "Терминал")
        val barcode = fixture.codeService.generateCode("org-1", item.inventoryItemId, InventoryCodeType.BARCODE)
        fixture.codeService.generateCode("org-1", item.inventoryItemId, InventoryCodeType.QR)

        val label = fixture.labelService.generateLabelPreview("org-1", item.inventoryItemId).label
        val resolution = fixture.codeService.resolveScannedCode("org-1", barcode.rawValue)
        val scan = fixture.codeService.recordScan(
            organizationId = "org-1",
            codeType = InventoryCodeType.BARCODE,
            codeValue = barcode.rawValue,
            rawValue = barcode.rawValue,
            inventoryItemId = resolution.item?.inventoryItemId,
            resultStatus = resolution.status,
        )

        assertThat(label.inventoryItemId).isEqualTo(item.inventoryItemId)
        assertThat(resolution.status).isEqualTo(InventoryScanResultStatus.RESOLVED)
        assertThat(scan.inventoryItemId).isEqualTo(item.inventoryItemId)
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

        val organizationRepository = InMemoryOrganizationRepositoryAdapter().apply {
            save(
                Organization(
                    organizationId = "org-1",
                    name = "Организация 1",
                    description = null,
                    createdByPeerId = "peer-1",
                    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                    updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                ),
            )
        }
        val locationRepository = InMemoryInventoryLocationRepositoryAdapter()
        val departmentRepository = InMemoryInventoryDepartmentRepositoryAdapter()

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
        val codeService = InventoryCodeService(
            localProfileService = localProfileService,
            codeBindingRepositoryPort = InMemoryInventoryCodeBindingRepositoryAdapter(),
            scanEventRepositoryPort = InMemoryInventoryScanEventRepositoryAdapter(),
            qrCodeRepositoryPort = InMemoryInventoryQrCodeRepositoryAdapter(),
            codeRepositoryPort = codeRepository,
            itemRepositoryPort = itemRepository,
            inventoryItemService = itemService,
            rbacService = rbacService,
            inventoryEventService = eventService,
            inventorySyncService = syncService,
        )

        val printTaskRepository = InMemoryInventoryPrintTaskRepositoryAdapter()
        val labelService = InventoryLabelService(
            localProfileService = localProfileService,
            labelTemplateRepositoryPort = InMemoryInventoryLabelTemplateRepositoryAdapter(),
            labelRepositoryPort = InMemoryInventoryLabelRepositoryAdapter(),
            printTaskRepositoryPort = printTaskRepository,
            codeRepositoryPort = codeRepository,
            itemRepositoryPort = itemRepository,
            organizationRepositoryPort = organizationRepository,
            locationRepositoryPort = locationRepository,
            departmentRepositoryPort = departmentRepository,
            rbacService = rbacService,
            inventoryEventService = eventService,
            inventorySyncService = syncService,
            rendererPort = FakeLabelRenderer(),
        )

        return Fixture(
            itemService = itemService,
            codeService = codeService,
            labelService = labelService,
            printTaskRepository = printTaskRepository,
        )
    }

    private suspend fun Fixture.createItem(
        organizationId: String,
        number: String,
        title: String,
        locationId: String? = null,
    ) =
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
            locationId = locationId,
            responsiblePerson = "Петров П.П.",
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
        val itemService: InventoryItemService,
        val codeService: InventoryCodeService,
        val labelService: InventoryLabelService,
        val printTaskRepository: InMemoryInventoryPrintTaskRepositoryAdapter,
    )

    private class FakeLabelRenderer : InventoryLabelRendererPort {
        override suspend fun renderPdf(
            label: org.expert.link.mesh.domain.model.inventory.InventoryLabel,
            fileName: String,
        ): InventoryLabelArtifact = renderPdfBatch(listOf(label), fileName)

        override suspend fun renderPdfBatch(
            labels: List<org.expert.link.mesh.domain.model.inventory.InventoryLabel>,
            fileName: String,
        ): InventoryLabelArtifact {
            val resolvedName = if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf"
            return InventoryLabelArtifact(
                descriptor = FileDescriptor(
                    fileId = "pdf-${labels.size}",
                    fileName = resolvedName,
                    sizeBytes = 4096,
                    sha256 = "test-sha256",
                    contentType = "application/pdf",
                ),
                localPath = "/tmp/$resolvedName",
            )
        }
    }
}
