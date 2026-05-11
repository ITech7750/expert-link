package org.expert.link.mesh.application.service

import kotlinx.datetime.Instant
import org.expert.link.mesh.domain.model.inventory.InventorySearchQuery
import org.expert.link.mesh.domain.model.inventory.InventorySearchResult
import org.expert.link.mesh.domain.model.inventory.InventorySortMode
import org.expert.link.mesh.domain.port.repository.InventoryIncidentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort

class InventorySearchService(
    private val itemRepositoryPort: InventoryItemRepositoryPort,
    private val incidentRepositoryPort: InventoryIncidentRepositoryPort,
) {
    suspend fun search(query: InventorySearchQuery): InventorySearchResult {
        val filter = query.filter
        val items = itemRepositoryPort.listByOrganization(query.organizationId)
        val incidents = if (filter.incidentOnly) {
            incidentRepositoryPort.listByOrganization(query.organizationId)
        } else {
            emptyList()
        }
        val incidentItemIds = incidents.mapNotNull { it.inventoryItemId }.toSet()
        val text = query.text?.trim()?.lowercase()

        val filtered = items.asSequence()
            .filter { item ->
                query.inventoryNumber?.let { item.inventoryNumber == it } ?: true
            }
            .filter { item ->
                query.localNumber?.let { item.localNumber == it } ?: true
            }
            .filter { item ->
                query.serialNumber?.let { item.serialNumber == it } ?: true
            }
            .filter { item ->
                query.qrCode?.let { item.qrCode == it } ?: true
            }
            .filter { item ->
                query.barcode?.let { item.barcode == it } ?: true
            }
            .filter { item ->
                if (text.isNullOrBlank()) {
                    true
                } else {
                    listOf(
                        item.inventoryNumber,
                        item.localNumber,
                        item.title,
                        item.description,
                        item.serialNumber,
                        item.brand,
                        item.model,
                        item.manufacturer,
                    ).any { value ->
                        value?.lowercase()?.contains(text) == true
                    }
                }
            }
            .filter { item ->
                filter.categoryIds.isEmpty() || filter.categoryIds.contains(item.categoryId)
            }
            .filter { item ->
                filter.subcategoryIds.isEmpty() || filter.subcategoryIds.contains(item.subcategoryId)
            }
            .filter { item ->
                filter.locationIds.isEmpty() || filter.locationIds.contains(item.locationId)
            }
            .filter { item ->
                filter.departmentIds.isEmpty() || filter.departmentIds.contains(item.departmentId)
            }
            .filter { item ->
                filter.costCenterIds.isEmpty() || filter.costCenterIds.contains(item.costCenterId)
            }
            .filter { item ->
                filter.ownerIds.isEmpty() || filter.ownerIds.contains(item.ownerId)
            }
            .filter { item ->
                filter.tagIds.isEmpty() || item.tagIds.any { it in filter.tagIds }
            }
            .filter { item ->
                filter.status.isEmpty() || item.currentStatus in filter.status
            }
            .filter { item ->
                filter.condition.isEmpty() || item.condition in filter.condition
            }
            .filter { item ->
                if (!filter.incidentOnly) true else item.inventoryItemId in incidentItemIds || item.incidentIds.isNotEmpty()
            }
            .filter { item ->
                filter.sessionId?.let { sessionId -> sessionId in item.sessionIds } ?: true
            }
            .filter { item ->
                filter.updatedSince?.let { item.updatedAt >= it } ?: true
            }
            .filter { item ->
                filter.nextInventoryBefore?.let { deadline ->
                    item.nextInventoryAt?.let { it <= deadline } == true
                } ?: true
            }
            .toList()

        val sorted = when (query.sort) {
            InventorySortMode.UPDATED_AT_DESC -> filtered.sortedByDescending { it.updatedAt }
            InventorySortMode.UPDATED_AT_ASC -> filtered.sortedBy { it.updatedAt }
            InventorySortMode.TITLE_ASC -> filtered.sortedBy { it.title }
            InventorySortMode.INVENTORY_NUMBER_ASC -> filtered.sortedBy { it.inventoryNumber }
            InventorySortMode.NEXT_INVENTORY_ASC -> filtered.sortedBy { it.nextInventoryAt ?: Instant.DISTANT_FUTURE }
        }

        val total = sorted.size
        val page = sorted.drop(query.offset).take(query.limit)
        return InventorySearchResult(
            organizationId = query.organizationId,
            total = total,
            items = page,
        )
    }
}
