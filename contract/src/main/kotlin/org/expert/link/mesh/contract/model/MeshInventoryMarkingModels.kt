package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class MeshInventoryBarcodeFormat {
    CODE_128,
}

@Serializable
data class MeshInventoryCode(
    val inventoryCodeId: String,
    val inventoryItemId: String,
    val organizationId: String,
    val codeType: MeshInventoryCodeType,
    val barcodeFormat: MeshInventoryBarcodeFormat? = null,
    val rawValue: String,
    val displayValue: String,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val createdByPeerId: String,
    val version: Long = 1,
)

@Serializable
enum class MeshInventoryLabelTemplateType {
    SHORT,
    STANDARD,
    FULL,
}

@Serializable
enum class MeshInventoryLabelFieldKey {
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
data class MeshInventoryLabelField(
    val key: MeshInventoryLabelFieldKey,
    val label: String,
    val value: String,
)

@Serializable
data class MeshInventoryLabelTemplate(
    val templateId: String,
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val templateType: MeshInventoryLabelTemplateType,
    val fields: List<MeshInventoryLabelFieldKey> = emptyList(),
    val includeBarcode: Boolean = true,
    val includeQr: Boolean = true,
    val isDefault: Boolean = false,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class MeshInventoryLabel(
    val labelId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val templateId: String,
    val templateName: String,
    val fields: List<MeshInventoryLabelField> = emptyList(),
    val barcodeValue: String? = null,
    val barcodeFormat: MeshInventoryBarcodeFormat? = null,
    val qrValue: String? = null,
    val createdAt: Instant,
    val createdByPeerId: String,
    val version: Long = 1,
)

@Serializable
enum class MeshInventoryPrintStatus {
    REQUESTED,
    GENERATED,
    FAILED,
    PRINTED,
}

@Serializable
data class MeshInventoryPrintTask(
    val printTaskId: String,
    val organizationId: String,
    val templateId: String,
    val labelIds: List<String> = emptyList(),
    val itemIds: List<String> = emptyList(),
    val status: MeshInventoryPrintStatus = MeshInventoryPrintStatus.REQUESTED,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val resultDescriptor: MeshFileDescriptor? = null,
    val localPath: String? = null,
    val errorMessage: String? = null,
)

@Serializable
enum class MeshInventoryScanResultStatus {
    RESOLVED,
    INACTIVE,
    NOT_FOUND,
    INVALID,
    ERROR,
}
