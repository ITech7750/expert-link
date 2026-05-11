package org.expert.link.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "organization",
    indices = [
        Index("updatedAt"),
    ],
)
data class OrganizationRecord(
    @PrimaryKey val organizationId: String,
    val name: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "organization_member",
    primaryKeys = ["organizationId", "peerId"],
    indices = [
        Index("peerId"),
        Index("updatedAt"),
    ],
)
data class OrganizationMemberRecord(
    val organizationId: String,
    val peerId: String,
    val roleKey: String,
    val status: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "role",
    indices = [
        Index("organizationId"),
        Index("name"),
    ],
)
data class RoleRecord(
    @PrimaryKey val roleId: String,
    val organizationId: String,
    val name: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_category",
    indices = [
        Index("organizationId"),
        Index("updatedAt"),
    ],
)
data class InventoryCategoryRecord(
    @PrimaryKey val categoryId: String,
    val organizationId: String,
    val name: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_location",
    indices = [
        Index("organizationId"),
        Index("updatedAt"),
    ],
)
data class InventoryLocationRecord(
    @PrimaryKey val locationId: String,
    val organizationId: String,
    val name: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_item",
    indices = [
        Index("organizationId"),
        Index("inventoryNumber"),
        Index("qrCode"),
        Index("barcode"),
        Index("status"),
        Index("updatedAt"),
    ],
)
data class InventoryItemRecord(
    @PrimaryKey val inventoryItemId: String,
    val organizationId: String,
    val inventoryNumber: String,
    val qrCode: String?,
    val barcode: String?,
    val status: String,
    val updatedAt: String,
    val revision: Long,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_session",
    indices = [
        Index("organizationId"),
        Index("status"),
        Index("updatedAt"),
    ],
)
data class InventorySessionRecord(
    @PrimaryKey val sessionId: String,
    val organizationId: String,
    val status: String,
    val periodStart: String,
    val periodEnd: String?,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_session_member",
    primaryKeys = ["sessionId", "peerId"],
    indices = [
        Index("organizationId"),
        Index("peerId"),
        Index("updatedAt"),
    ],
)
data class InventorySessionMemberRecord(
    val sessionId: String,
    val organizationId: String,
    val peerId: String,
    val role: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_review",
    indices = [
        Index("organizationId"),
        Index("inventoryItemId"),
        Index("sessionId"),
        Index("status"),
        Index("createdAt"),
    ],
)
data class InventoryReviewRecord(
    @PrimaryKey val reviewId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val sessionId: String?,
    val status: String,
    val createdAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_comment",
    indices = [
        Index("organizationId"),
        Index("inventoryItemId"),
        Index("sessionId"),
        Index("createdAt"),
    ],
)
data class InventoryCommentRecord(
    @PrimaryKey val commentId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val sessionId: String?,
    val createdAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_attachment",
    indices = [
        Index("organizationId"),
        Index("inventoryItemId"),
        Index("sessionId"),
        Index("createdAt"),
    ],
)
data class InventoryAttachmentRecord(
    @PrimaryKey val attachmentId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val sessionId: String?,
    val createdAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_event",
    indices = [
        Index("organizationId"),
        Index("entityId"),
        Index("sequence"),
        Index("occurredAt"),
    ],
)
data class InventoryEventRecord(
    @PrimaryKey val eventId: String,
    val organizationId: String,
    val entityId: String,
    val entityType: String,
    val eventType: String,
    val occurredAt: String,
    val sequence: Long,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_export",
    indices = [
        Index("organizationId"),
        Index("sessionId"),
        Index("status"),
        Index("updatedAt"),
    ],
)
data class InventoryExportRecord(
    @PrimaryKey val exportTaskId: String,
    val organizationId: String,
    val sessionId: String,
    val status: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_qrcode",
    indices = [
        Index("organizationId"),
        Index("inventoryItemId"),
        Index("qrCode"),
        Index("barcode"),
    ],
)
data class InventoryQrCodeRecord(
    @PrimaryKey val codeId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val qrCode: String,
    val barcode: String?,
    val createdAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_subcategory",
    indices = [
        Index("organizationId"),
        Index("categoryId"),
        Index("updatedAt"),
    ],
)
data class InventorySubcategoryRecord(
    @PrimaryKey val subcategoryId: String,
    val organizationId: String,
    val categoryId: String,
    val name: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_tag",
    indices = [
        Index("organizationId"),
        Index("name"),
    ],
)
data class InventoryTagRecord(
    @PrimaryKey val tagId: String,
    val organizationId: String,
    val name: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_attribute_definition",
    indices = [
        Index("organizationId"),
        Index("key"),
    ],
)
data class InventoryAttributeDefinitionRecord(
    @PrimaryKey val attributeId: String,
    val organizationId: String,
    val key: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_category_template",
    indices = [
        Index("organizationId"),
        Index("categoryId"),
        Index("updatedAt"),
    ],
)
data class InventoryCategoryTemplateRecord(
    @PrimaryKey val templateId: String,
    val organizationId: String,
    val categoryId: String,
    val name: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_owner",
    indices = [
        Index("organizationId"),
        Index("name"),
        Index("updatedAt"),
    ],
)
data class InventoryOwnerRecord(
    @PrimaryKey val ownerId: String,
    val organizationId: String,
    val name: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_department",
    indices = [
        Index("organizationId"),
        Index("name"),
        Index("updatedAt"),
    ],
)
data class InventoryDepartmentRecord(
    @PrimaryKey val departmentId: String,
    val organizationId: String,
    val name: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_cost_center",
    indices = [
        Index("organizationId"),
        Index("code"),
        Index("updatedAt"),
    ],
)
data class InventoryCostCenterRecord(
    @PrimaryKey val costCenterId: String,
    val organizationId: String,
    val code: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_legal_holder",
    indices = [
        Index("organizationId"),
        Index("name"),
        Index("updatedAt"),
    ],
)
data class InventoryLegalHolderRecord(
    @PrimaryKey val legalHolderId: String,
    val organizationId: String,
    val name: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_supplier",
    indices = [
        Index("organizationId"),
        Index("name"),
        Index("updatedAt"),
    ],
)
data class InventorySupplierRecord(
    @PrimaryKey val supplierId: String,
    val organizationId: String,
    val name: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_funding_source",
    indices = [
        Index("organizationId"),
        Index("name"),
        Index("updatedAt"),
    ],
)
data class InventoryFundingSourceRecord(
    @PrimaryKey val fundingSourceId: String,
    val organizationId: String,
    val name: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_incident",
    indices = [
        Index("organizationId"),
        Index("inventoryItemId"),
        Index("sessionId"),
        Index("status"),
        Index("reportedAt"),
    ],
)
data class InventoryIncidentRecord(
    @PrimaryKey val incidentId: String,
    val organizationId: String,
    val inventoryItemId: String?,
    val sessionId: String?,
    val status: String,
    val reportedAt: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_alert",
    indices = [
        Index("organizationId"),
        Index("inventoryItemId"),
        Index("sessionId"),
        Index("createdAt"),
    ],
)
data class InventoryAlertRecord(
    @PrimaryKey val alertId: String,
    val organizationId: String,
    val inventoryItemId: String?,
    val sessionId: String?,
    val createdAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_reminder",
    indices = [
        Index("organizationId"),
        Index("inventoryItemId"),
        Index("sessionId"),
        Index("status"),
        Index("dueAt"),
    ],
)
data class InventoryReminderRecord(
    @PrimaryKey val reminderId: String,
    val organizationId: String,
    val inventoryItemId: String?,
    val sessionId: String?,
    val status: String,
    val dueAt: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_rule_threshold",
    indices = [
        Index("organizationId"),
        Index("ruleType"),
        Index("updatedAt"),
    ],
)
data class InventoryRuleThresholdRecord(
    @PrimaryKey val ruleId: String,
    val organizationId: String,
    val ruleType: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_deadline_rule",
    indices = [
        Index("organizationId"),
        Index("target"),
        Index("updatedAt"),
    ],
)
data class InventoryDeadlineRuleRecord(
    @PrimaryKey val ruleId: String,
    val organizationId: String,
    val target: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_change_log",
    indices = [
        Index("organizationId"),
        Index("entityId"),
        Index("entityType"),
        Index("changedAt"),
    ],
)
data class InventoryChangeLogRecord(
    @PrimaryKey val changeId: String,
    val organizationId: String,
    val entityId: String,
    val entityType: String,
    val changedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_dashboard_snapshot",
    indices = [
        Index("organizationId"),
        Index("generatedAt"),
    ],
)
data class InventoryDashboardSnapshotRecord(
    @PrimaryKey val snapshotId: String,
    val organizationId: String,
    val generatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_code_binding",
    indices = [
        Index("organizationId"),
        Index("inventoryItemId"),
        Index("codeValue"),
        Index("createdAt"),
    ],
)
data class InventoryCodeBindingRecord(
    @PrimaryKey val codeId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val codeValue: String,
    val createdAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_code",
    indices = [
        Index("organizationId"),
        Index("inventoryItemId"),
        Index("rawValue"),
        Index("isActive"),
        Index("createdAt"),
    ],
)
data class InventoryCodeRecord(
    @PrimaryKey val inventoryCodeId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val rawValue: String,
    val isActive: Int,
    val createdAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_label_template",
    indices = [
        Index("organizationId"),
        Index("name"),
        Index("isDefault"),
        Index("updatedAt"),
    ],
)
data class InventoryLabelTemplateRecord(
    @PrimaryKey val templateId: String,
    val organizationId: String,
    val name: String,
    val isDefault: Int,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_label",
    indices = [
        Index("organizationId"),
        Index("inventoryItemId"),
        Index("templateId"),
        Index("createdAt"),
    ],
)
data class InventoryLabelRecord(
    @PrimaryKey val labelId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val templateId: String,
    val createdAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_print_task",
    indices = [
        Index("organizationId"),
        Index("templateId"),
        Index("status"),
        Index("updatedAt"),
    ],
)
data class InventoryPrintTaskRecord(
    @PrimaryKey val printTaskId: String,
    val organizationId: String,
    val templateId: String,
    val status: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_scan_event",
    indices = [
        Index("organizationId"),
        Index("inventoryItemId"),
        Index("scannedAt"),
    ],
)
data class InventoryScanEventRecord(
    @PrimaryKey val scanEventId: String,
    val organizationId: String,
    val inventoryItemId: String?,
    val scannedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_revision",
    indices = [
        Index("organizationId"),
        Index("entityType"),
        Index("updatedAt"),
    ],
)
data class InventoryRevisionRecord(
    @PrimaryKey val entityId: String,
    val organizationId: String,
    val entityType: String,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "inventory_conflict",
    indices = [
        Index("organizationId"),
        Index("entityId"),
        Index("detectedAt"),
    ],
)
data class InventoryConflictRecord(
    @PrimaryKey val conflictId: String,
    val organizationId: String,
    val entityId: String,
    val entityType: String,
    val detectedAt: String,
    val payloadJson: String,
)
