package org.expert.link.mesh.application.service

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.inventory.InventoryCondition
import org.expert.link.mesh.domain.model.inventory.InventoryItem
import org.expert.link.mesh.domain.model.inventory.InventoryQrCode
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryItemRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryQrCodeRepositoryAdapter
import org.junit.jupiter.api.Test

class InventoryQueryServiceTest {
    @Test
    fun `should find item by qr code`() = runTest {
        val itemRepository = InMemoryInventoryItemRepositoryAdapter()
        val qrRepository = InMemoryInventoryQrCodeRepositoryAdapter()
        val service = InventoryQueryService(itemRepository, qrRepository, InMemoryInventoryCodeRepositoryAdapter())
        val item = InventoryItem(
            inventoryItemId = "item-1",
            organizationId = "org-1",
            inventoryNumber = "INV-10",
            qrCode = "QR-10",
            barcode = null,
            categoryId = null,
            title = "Микрофон",
            description = null,
            serialNumber = null,
            condition = InventoryCondition.GOOD,
            locationId = null,
            responsiblePerson = null,
            responsibleDepartment = null,
            createdByPeerId = "peer-1",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            currentStatus = InventoryStatus.DRAFT,
        )
        itemRepository.save(item)

        val found = service.findByQrCode("QR-10")

        assertThat(found?.inventoryItemId).isEqualTo("item-1")
    }

    @Test
    fun `should find item by barcode via qr repository`() = runTest {
        val itemRepository = InMemoryInventoryItemRepositoryAdapter()
        val qrRepository = InMemoryInventoryQrCodeRepositoryAdapter()
        val service = InventoryQueryService(itemRepository, qrRepository, InMemoryInventoryCodeRepositoryAdapter())
        val item = InventoryItem(
            inventoryItemId = "item-2",
            organizationId = "org-1",
            inventoryNumber = "INV-11",
            qrCode = null,
            barcode = null,
            categoryId = null,
            title = "Пульт",
            description = null,
            serialNumber = null,
            condition = InventoryCondition.GOOD,
            locationId = null,
            responsiblePerson = null,
            responsibleDepartment = null,
            createdByPeerId = "peer-1",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            currentStatus = InventoryStatus.DRAFT,
        )
        itemRepository.save(item)
        qrRepository.save(
            InventoryQrCode(
                codeId = "qr-1",
                organizationId = "org-1",
                inventoryItemId = item.inventoryItemId,
                qrCode = "QR-11",
                barcode = "BAR-11",
                createdByPeerId = "peer-1",
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        val found = service.findByBarcode("BAR-11")

        assertThat(found?.inventoryItemId).isEqualTo(item.inventoryItemId)
    }
}
