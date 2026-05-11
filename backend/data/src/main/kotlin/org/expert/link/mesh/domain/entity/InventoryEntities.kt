package org.expert.link.mesh.domain.entity

import kotlinx.serialization.Serializable

/** Сущность хранения инвентарного кода (штрихкод/QR). */
@Serializable
data class InventoryCodeEntity(
    val inventoryCodeId: String,
    val inventoryItemId: String,
    val organizationId: String,
    val codeType: String,
    val barcodeFormat: String? = null,
    val rawValue: String,
    val displayValue: String,
    val isActive: Boolean = true,
    val createdAt: String,
    val createdByPeerId: String,
    val version: Long = 1,
)

/** Сущность хранения шаблона этикетки. */
@Serializable
data class InventoryLabelTemplateEntity(
    val templateId: String,
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val templateType: String,
    val fieldsJson: String,
    val includeBarcode: Boolean = true,
    val includeQr: Boolean = true,
    val isDefault: Boolean = false,
    val createdByPeerId: String,
    val createdAt: String,
    val updatedAt: String,
)

/** Сущность хранения задачи печати этикеток. */
@Serializable
data class InventoryPrintTaskEntity(
    val printTaskId: String,
    val organizationId: String,
    val templateId: String,
    val labelIdsJson: String,
    val itemIdsJson: String,
    val status: String,
    val createdByPeerId: String,
    val createdAt: String,
    val updatedAt: String,
    val resultDescriptorJson: String? = null,
    val localPath: String? = null,
    val errorMessage: String? = null,
)

/** Сущность хранения события сканирования. */
@Serializable
data class InventoryScanEventEntity(
    val scanEventId: String,
    val inventoryItemId: String? = null,
    val organizationId: String,
    val codeType: String,
    val codeValue: String,
    val rawValue: String,
    val scannedByPeerId: String,
    val scannedAt: String,
    val resultStatus: String,
    val sessionId: String? = null,
    val locationId: String? = null,
    val locationHint: String? = null,
    val deviceId: String? = null,
    val note: String? = null,
)
