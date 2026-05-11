package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.inventory.InventoryBarcodeFormat
import org.expert.link.mesh.domain.model.inventory.InventoryCode
import org.expert.link.mesh.domain.model.inventory.InventoryCodeBinding
import org.expert.link.mesh.domain.model.inventory.InventoryCodeBindingSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCodeSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCodeType
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryItem
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventoryQrCode
import org.expert.link.mesh.domain.model.inventory.InventoryQrSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryScanEvent
import org.expert.link.mesh.domain.model.inventory.InventoryScanEventSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryScanResultStatus
import org.expert.link.mesh.domain.port.repository.InventoryCodeBindingRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCodeRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryQrCodeRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryScanEventRepositoryPort

data class InventoryScanResolution(
    val item: InventoryItem? = null,
    val code: InventoryCode? = null,
    val codeType: InventoryCodeType? = null,
    val status: InventoryScanResultStatus,
)

class InventoryCodeService(
    private val localProfileService: LocalProfileService,
    private val codeBindingRepositoryPort: InventoryCodeBindingRepositoryPort,
    private val scanEventRepositoryPort: InventoryScanEventRepositoryPort,
    private val qrCodeRepositoryPort: InventoryQrCodeRepositoryPort,
    private val codeRepositoryPort: InventoryCodeRepositoryPort,
    private val itemRepositoryPort: InventoryItemRepositoryPort,
    private val inventoryItemService: InventoryItemService,
    private val rbacService: InventoryRbacService,
    private val inventoryEventService: InventoryEventService,
    private val inventorySyncService: InventorySyncService,
) {
    suspend fun listBindings(itemId: String): List<InventoryCodeBinding> =
        codeBindingRepositoryPort.listByItem(itemId)

    suspend fun listCodes(itemId: String): List<InventoryCode> =
        codeRepositoryPort.listByItem(itemId).sortedByDescending { it.createdAt }

    suspend fun getActiveCode(itemId: String, codeType: InventoryCodeType): InventoryCode? =
        listCodes(itemId).firstOrNull { it.codeType == codeType && it.isActive }

    suspend fun generateCode(
        organizationId: String,
        inventoryItemId: String,
        codeType: InventoryCodeType,
        barcodeFormat: InventoryBarcodeFormat = InventoryBarcodeFormat.CODE_128,
    ): InventoryCode {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.ITEM_EDIT)
        val item = requireNotNull(itemRepositoryPort.findByInventoryItemId(inventoryItemId)) {
            "Item $inventoryItemId not found"
        }
        val rawValue = when (codeType) {
            InventoryCodeType.BARCODE -> generateUniqueAssetCode()
            InventoryCodeType.QR -> buildQrPayload(
                organizationId = organizationId,
                item = item,
                assetCode = getActiveCode(inventoryItemId, InventoryCodeType.BARCODE)?.rawValue ?: item.barcode,
            )
            else -> error("Unsupported code type: $codeType")
        }
        val code = InventoryCode(
            inventoryCodeId = newId("code"),
            inventoryItemId = inventoryItemId,
            organizationId = organizationId,
            codeType = codeType,
            barcodeFormat = if (codeType == InventoryCodeType.BARCODE) barcodeFormat else null,
            rawValue = rawValue,
            displayValue = item.inventoryNumber,
            isActive = true,
            createdAt = now(),
            createdByPeerId = localProfile.peerId,
            version = 1,
        )
        codeRepositoryPort.save(code)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.CODE,
                entityId = code.inventoryCodeId,
                eventType = InventoryEventType.CODE_GENERATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryCodeSnapshot(code),
            ),
        )
        when (codeType) {
            InventoryCodeType.QR -> inventoryItemService.updateItem(
                itemId = inventoryItemId,
                expectedRevision = item.revision,
                update = InventoryItemUpdate(qrCode = rawValue),
            )
            InventoryCodeType.BARCODE -> inventoryItemService.updateItem(
                itemId = inventoryItemId,
                expectedRevision = item.revision,
                update = InventoryItemUpdate(barcode = rawValue),
            )
            else -> Unit
        }
        return code
    }

    suspend fun regenerateCode(
        organizationId: String,
        inventoryItemId: String,
        codeType: InventoryCodeType,
        barcodeFormat: InventoryBarcodeFormat = InventoryBarcodeFormat.CODE_128,
    ): InventoryCode {
        val existing = getActiveCode(inventoryItemId, codeType)
        if (existing != null) {
            deactivateCode(existing.inventoryCodeId)
        }
        val regenerated = generateCode(organizationId, inventoryItemId, codeType, barcodeFormat)
        val localProfile = localProfileService.require()
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.CODE,
                entityId = regenerated.inventoryCodeId,
                eventType = InventoryEventType.CODE_REGENERATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryCodeSnapshot(regenerated),
            ),
        )
        return regenerated
    }

    suspend fun deactivateCode(codeId: String): InventoryCode? {
        val existing = codeRepositoryPort.findByCodeId(codeId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(existing.organizationId, localProfile.peerId, InventoryPermission.ITEM_EDIT)
        val updated = existing.copy(isActive = false, version = existing.version + 1)
        codeRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = existing.organizationId,
                entityType = InventoryEntityType.CODE,
                entityId = existing.inventoryCodeId,
                eventType = InventoryEventType.CODE_DEACTIVATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryCodeSnapshot(updated),
            ),
        )
        val item = itemRepositoryPort.findByInventoryItemId(existing.inventoryItemId)
        if (item != null) {
            when (existing.codeType) {
                InventoryCodeType.QR -> if (item.qrCode == existing.rawValue) {
                    inventoryItemService.updateItem(
                        itemId = item.inventoryItemId,
                        expectedRevision = item.revision,
                        update = InventoryItemUpdate(qrCode = null),
                    )
                }
                InventoryCodeType.BARCODE -> if (item.barcode == existing.rawValue) {
                    inventoryItemService.updateItem(
                        itemId = item.inventoryItemId,
                        expectedRevision = item.revision,
                        update = InventoryItemUpdate(barcode = null),
                    )
                }
                else -> Unit
            }
        }
        return updated
    }

    suspend fun createBinding(
        organizationId: String,
        inventoryItemId: String,
        sessionId: String?,
        codeType: InventoryCodeType,
        codeValue: String,
    ): InventoryCodeBinding {
        require(codeValue.isNotBlank()) { "Code value is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.ITEM_EDIT)
        val binding = InventoryCodeBinding(
            codeId = newId("code"),
            organizationId = organizationId,
            inventoryItemId = inventoryItemId,
            sessionId = sessionId,
            codeType = codeType,
            codeValue = codeValue,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
        )
        codeBindingRepositoryPort.save(binding)
        if (codeType == InventoryCodeType.QR || codeType == InventoryCodeType.BARCODE) {
            val qr = InventoryQrCode(
                codeId = newId("qr"),
                organizationId = organizationId,
                inventoryItemId = inventoryItemId,
                qrCode = if (codeType == InventoryCodeType.QR) codeValue else "",
                barcode = if (codeType == InventoryCodeType.BARCODE) codeValue else null,
                createdByPeerId = localProfile.peerId,
                createdAt = now(),
            )
            qrCodeRepositoryPort.save(qr)
            publish(
                inventoryEventService.recordEvent(
                    organizationId = organizationId,
                    entityType = InventoryEntityType.QRCODE,
                    entityId = qr.codeId,
                    eventType = InventoryEventType.CREATED,
                    actorPeerId = localProfile.peerId,
                    payload = InventoryQrSnapshot(qr),
                ),
            )
        }
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.CODE_BINDING,
                entityId = binding.codeId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryCodeBindingSnapshot(binding),
            ),
        )
        return binding
    }

    suspend fun resolveScannedCode(
        organizationId: String?,
        rawValue: String,
    ): InventoryScanResolution {
        val normalized = rawValue.trim()
        if (normalized.isBlank()) {
            return InventoryScanResolution(status = InventoryScanResultStatus.INVALID)
        }
        val parsed = parseQrPayload(normalized)
        if (parsed != null) {
            if (organizationId != null && parsed.organizationId != null && parsed.organizationId != organizationId) {
                return InventoryScanResolution(codeType = InventoryCodeType.QR, status = InventoryScanResultStatus.NOT_FOUND)
            }
            val item = itemRepositoryPort.findByInventoryItemId(parsed.inventoryItemId)
            val code = codeRepositoryPort.findByRawValue(normalized)
            return InventoryScanResolution(
                item = item,
                code = code ?: item?.let { getActiveCode(it.inventoryItemId, InventoryCodeType.QR) },
                codeType = InventoryCodeType.QR,
                status = when {
                    item == null -> InventoryScanResultStatus.NOT_FOUND
                    code != null && !code.isActive -> InventoryScanResultStatus.INACTIVE
                    else -> InventoryScanResultStatus.RESOLVED
                },
            )
        }
        val code = codeRepositoryPort.findByRawValue(normalized)
        val item = when {
            code != null -> itemRepositoryPort.findByInventoryItemId(code.inventoryItemId)
            else -> itemRepositoryPort.findByQrCode(normalized)
                ?: itemRepositoryPort.findByBarcode(normalized)
                ?: organizationId?.let { itemRepositoryPort.findByInventoryNumber(it, normalized) }
        }
        val status = when {
            item == null -> InventoryScanResultStatus.NOT_FOUND
            code != null && !code.isActive -> InventoryScanResultStatus.INACTIVE
            else -> InventoryScanResultStatus.RESOLVED
        }
        return InventoryScanResolution(
            item = item,
            code = code,
            codeType = code?.codeType ?: when {
                item?.barcode == normalized -> InventoryCodeType.BARCODE
                item?.qrCode == normalized -> InventoryCodeType.QR
                else -> null
            },
            status = status,
        )
    }

    suspend fun recordScan(
        organizationId: String,
        codeType: InventoryCodeType,
        codeValue: String,
        rawValue: String = codeValue,
        inventoryItemId: String? = null,
        sessionId: String? = null,
        locationId: String? = null,
        locationHint: String? = null,
        deviceId: String? = null,
        resultStatus: InventoryScanResultStatus = InventoryScanResultStatus.RESOLVED,
        note: String? = null,
    ): InventoryScanEvent {
        require(codeValue.isNotBlank()) { "Code value is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.ITEM_VIEW)
        val event = InventoryScanEvent(
            scanEventId = newId("scan"),
            organizationId = organizationId,
            codeType = codeType,
            codeValue = codeValue,
            rawValue = rawValue,
            inventoryItemId = inventoryItemId,
            sessionId = sessionId,
            locationId = locationId,
            locationHint = locationHint,
            scannedByPeerId = localProfile.peerId,
            scannedAt = now(),
            resultStatus = resultStatus,
            deviceId = deviceId,
            note = note,
        )
        scanEventRepositoryPort.save(event)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.SCAN_EVENT,
                entityId = event.scanEventId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryScanEventSnapshot(event),
                sessionId = sessionId,
            ),
        )
        return event
    }

    private fun generateAssetCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return "EL-" + buildString {
            repeat(8) {
                append(alphabet.random())
            }
        }
    }

    private suspend fun generateUniqueAssetCode(): String {
        repeat(12) {
            val candidate = generateAssetCode()
            if (codeRepositoryPort.findByRawValue(candidate) == null) {
                return candidate
            }
        }
        return generateAssetCode()
    }

    private fun buildQrPayload(
        organizationId: String,
        item: InventoryItem,
        assetCode: String?,
    ): String {
        return listOf(
            "inventory",
            "v2",
            organizationId,
            item.inventoryItemId,
            assetCode.orEmpty(),
            item.inventoryNumber,
            item.title,
            item.responsiblePerson.orEmpty(),
        ).joinToString(":")
    }

    private fun parseQrPayload(rawValue: String): ParsedQrPayload? {
        val parts = rawValue.split(":")
        if (parts.firstOrNull() != "inventory") return null
        return when {
            parts.size >= 8 && parts[1] == "v2" -> ParsedQrPayload(
                organizationId = parts[2],
                inventoryItemId = parts[3],
                assetCode = parts.getOrNull(4)?.ifBlank { null },
                inventoryNumber = parts.getOrNull(5)?.ifBlank { null },
                title = parts.getOrNull(6)?.ifBlank { null },
                responsiblePerson = parts.getOrNull(7)?.ifBlank { null },
            )
            parts.size >= 5 && parts[1] == "v1" -> ParsedQrPayload(
                organizationId = parts[2],
                inventoryItemId = parts[3],
                assetCode = parts.getOrNull(4)?.ifBlank { null },
            )
            parts.size >= 3 -> ParsedQrPayload(
                organizationId = parts[1],
                inventoryItemId = parts[2],
                assetCode = null,
            )
            else -> null
        }
    }

    private suspend fun publish(event: org.expert.link.mesh.domain.model.inventory.InventoryEvent) {
        inventorySyncService.broadcastEvent(event)
    }

    private data class ParsedQrPayload(
        val organizationId: String?,
        val inventoryItemId: String,
        val assetCode: String?,
        val inventoryNumber: String? = null,
        val title: String? = null,
        val responsiblePerson: String? = null,
    )
}
