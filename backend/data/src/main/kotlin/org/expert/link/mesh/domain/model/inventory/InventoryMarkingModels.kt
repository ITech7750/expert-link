package org.expert.link.mesh.domain.model.inventory

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor

@Serializable
enum class InventoryBarcodeFormat {
    CODE_128,
}

@Serializable
data class InventoryCode(
    val inventoryCodeId: String,
    val inventoryItemId: String,
    val organizationId: String,
    val codeType: InventoryCodeType,
    val barcodeFormat: InventoryBarcodeFormat? = null,
    val rawValue: String,
    val displayValue: String,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val createdByPeerId: String,
    val version: Long = 1,
)

@Serializable
enum class InventoryLabelTemplateType {
    SHORT,
    STANDARD,
    FULL,
}

@Serializable
enum class InventoryLabelFieldKey {
    TITLE,
    INVENTORY_NUMBER,
    RESPONSIBLE_PERSON,
    LOCATION,
    DEPARTMENT,
    ORGANIZATION,
    NEXT_INVENTORY_AT,
    STATUS,
    CONDITION,
}

@Serializable
data class InventoryLabelField(
    val key: InventoryLabelFieldKey,
    val label: String,
    val value: String,
)

@Serializable
data class InventoryLabelTemplate(
    val templateId: String,
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val templateType: InventoryLabelTemplateType,
    val fields: List<InventoryLabelFieldKey> = emptyList(),
    val includeBarcode: Boolean = true,
    val includeQr: Boolean = true,
    val isDefault: Boolean = false,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventoryLabel(
    val labelId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val templateId: String,
    val templateName: String,
    val fields: List<InventoryLabelField> = emptyList(),
    val barcodeValue: String? = null,
    val barcodeFormat: InventoryBarcodeFormat? = null,
    val qrValue: String? = null,
    val createdAt: Instant,
    val createdByPeerId: String,
    val version: Long = 1,
)

@Serializable
enum class InventoryPrintStatus {
    REQUESTED,
    GENERATED,
    FAILED,
    PRINTED,
}

@Serializable
data class InventoryPrintTask(
    val printTaskId: String,
    val organizationId: String,
    val templateId: String,
    val labelIds: List<String> = emptyList(),
    val itemIds: List<String> = emptyList(),
    val status: InventoryPrintStatus = InventoryPrintStatus.REQUESTED,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val resultDescriptor: FileDescriptor? = null,
    val localPath: String? = null,
    val errorMessage: String? = null,
)
