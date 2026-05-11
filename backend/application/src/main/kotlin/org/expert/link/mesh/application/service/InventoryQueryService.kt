package org.expert.link.mesh.application.service

import org.expert.link.mesh.domain.model.inventory.InventoryItem
import org.expert.link.mesh.domain.port.repository.InventoryCodeRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryQrCodeRepositoryPort

class InventoryQueryService(
    private val itemRepositoryPort: InventoryItemRepositoryPort,
    private val qrCodeRepositoryPort: InventoryQrCodeRepositoryPort,
    private val codeRepositoryPort: InventoryCodeRepositoryPort,
) {
    suspend fun findByQrCode(code: String): InventoryItem? {
        resolveByPayload(code)?.let { return it }
        itemRepositoryPort.findByQrCode(code)?.let { return it }
        val qr = qrCodeRepositoryPort.findByCode(code) ?: return null
        return itemRepositoryPort.findByInventoryItemId(qr.inventoryItemId)
    }

    suspend fun findByBarcode(code: String): InventoryItem? {
        itemRepositoryPort.findByBarcode(code)?.let { return it }
        val qr = qrCodeRepositoryPort.findByBarcode(code) ?: return null
        return itemRepositoryPort.findByInventoryItemId(qr.inventoryItemId)
    }

    suspend fun findByAnyCode(code: String): InventoryItem? {
        resolveByPayload(code)?.let { return it }
        val binding = codeRepositoryPort.findByRawValue(code)
        if (binding != null) {
            return itemRepositoryPort.findByInventoryItemId(binding.inventoryItemId)
        }
        return findByQrCode(code) ?: findByBarcode(code)
    }

    private suspend fun resolveByPayload(code: String): InventoryItem? {
        val parts = code.split(":")
        if (parts.size < 3 || parts.first() != "inventory") return null
        return itemRepositoryPort.findByInventoryItemId(parts[2])
    }
}
