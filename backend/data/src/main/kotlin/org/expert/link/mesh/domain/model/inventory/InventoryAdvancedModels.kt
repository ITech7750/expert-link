package org.expert.link.mesh.domain.model.inventory

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class InventoryItemType {
    EQUIPMENT,
    IT,
    FURNITURE,
    LICENSE,
    INTANGIBLE,
    CONSUMABLE,
    OTHER,
    UNKNOWN,
}

@Serializable
enum class InventoryLocationType {
    CAMPUS,
    BUILDING,
    FLOOR,
    ROOM,
    ZONE,
    STORAGE,
    CABINET,
    SHELF,
    OTHER,
}

@Serializable
enum class InventoryAttachmentStatus {
    AVAILABLE,
    PENDING,
    FAILED,
    ARCHIVED,
}

@Serializable
data class InventoryAttachmentPreviewMetadata(
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String? = null,
)

@Serializable
enum class InventoryAttributeType {
    TEXT,
    NUMBER,
    DATE,
    BOOLEAN,
    OPTION,
    MULTI_OPTION,
}

@Serializable
data class InventoryAttributeDefinition(
    val attributeId: String,
    val organizationId: String,
    val key: String,
    val label: String,
    val description: String? = null,
    val type: InventoryAttributeType = InventoryAttributeType.TEXT,
    val required: Boolean = false,
    val unit: String? = null,
    val options: List<String> = emptyList(),
    val validationRules: List<InventoryValidationRule> = emptyList(),
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventoryAttributeValue(
    val attributeId: String,
    val value: String,
    val updatedByPeerId: String,
    val updatedAt: Instant,
)

@Serializable
data class InventoryTag(
    val tagId: String,
    val organizationId: String,
    val name: String,
    val color: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventorySubcategory(
    val subcategoryId: String,
    val categoryId: String,
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventoryCategoryTemplate(
    val templateId: String,
    val organizationId: String,
    val categoryId: String,
    val name: String,
    val description: String? = null,
    val fields: List<InventoryFieldTemplate> = emptyList(),
    val requiredFields: List<InventoryRequiredFieldRule> = emptyList(),
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventoryFieldTemplate(
    val fieldId: String,
    val key: String,
    val label: String,
    val type: InventoryAttributeType = InventoryAttributeType.TEXT,
    val required: Boolean = false,
    val validationRules: List<InventoryValidationRule> = emptyList(),
    val helpText: String? = null,
    val order: Int = 0,
)

@Serializable
enum class InventoryValidationRuleType {
    REGEX,
    RANGE,
    LENGTH,
    ENUM,
    REQUIRED,
}

@Serializable
data class InventoryValidationRule(
    val type: InventoryValidationRuleType,
    val params: Map<String, String> = emptyMap(),
    val message: String? = null,
)

@Serializable
data class InventoryRequiredFieldRule(
    val fieldId: String,
    val message: String? = null,
)

@Serializable
enum class InventoryOwnerType {
    ORGANIZATION,
    DEPARTMENT,
    PERSON,
    EXTERNAL,
}

@Serializable
data class InventoryOwner(
    val ownerId: String,
    val organizationId: String,
    val name: String,
    val type: InventoryOwnerType = InventoryOwnerType.ORGANIZATION,
    val legalHolderId: String? = null,
    val contactInfo: String? = null,
    val code: String? = null,
    val departmentId: String? = null,
    val locationIds: Set<String> = emptySet(),
    val archived: Boolean = false,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventoryDepartment(
    val departmentId: String,
    val organizationId: String,
    val name: String,
    val parentDepartmentId: String? = null,
    val code: String? = null,
    val locationIds: Set<String> = emptySet(),
    val ownerIds: Set<String> = emptySet(),
    val archived: Boolean = false,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventoryCostCenter(
    val costCenterId: String,
    val organizationId: String,
    val code: String,
    val name: String,
    val description: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventoryLegalHolder(
    val legalHolderId: String,
    val organizationId: String,
    val name: String,
    val taxId: String? = null,
    val registrationNumber: String? = null,
    val address: String? = null,
    val bankDetails: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventorySupplier(
    val supplierId: String,
    val organizationId: String,
    val name: String,
    val contactInfo: String? = null,
    val bankDetails: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventoryFundingSource(
    val fundingSourceId: String,
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class InventoryCodeType {
    QR,
    BARCODE,
    NFC,
    RFID,
}

@Serializable
data class InventoryCodeBinding(
    val codeId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val sessionId: String? = null,
    val codeType: InventoryCodeType = InventoryCodeType.QR,
    val codeValue: String,
    val createdByPeerId: String,
    val createdAt: Instant,
)

@Serializable
data class InventoryScanEvent(
    val scanEventId: String,
    val organizationId: String,
    val codeType: InventoryCodeType,
    val codeValue: String,
    val rawValue: String = codeValue,
    val inventoryItemId: String? = null,
    val sessionId: String? = null,
    val locationId: String? = null,
    val locationHint: String? = null,
    val scannedByPeerId: String,
    val scannedAt: Instant,
    val resultStatus: InventoryScanResultStatus = InventoryScanResultStatus.RESOLVED,
    val deviceId: String? = null,
    val note: String? = null,
)

@Serializable
enum class InventoryScanResultStatus {
    RESOLVED,
    INACTIVE,
    NOT_FOUND,
    INVALID,
    ERROR,
}

@Serializable
enum class InventoryIncidentType {
    NOT_FOUND,
    WRONG_LOCATION,
    DAMAGED,
    SERIAL_MISMATCH,
    QR_MISSING,
    QR_UNREADABLE,
    FOUND_UNEXPECTED,
    CARD_INCOMPLETE,
    INVENTORY_OVERDUE,
    CORRECTION_REQUEST,
    OTHER,
}

@Serializable
enum class InventoryIncidentSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

@Serializable
enum class InventoryIncidentStatus {
    OPEN,
    UNDER_REVIEW,
    RESOLVED,
    DISMISSED,
}

@Serializable
data class InventoryIncident(
    val incidentId: String,
    val organizationId: String,
    val inventoryItemId: String? = null,
    val sessionId: String? = null,
    val locationId: String? = null,
    val type: InventoryIncidentType,
    val severity: InventoryIncidentSeverity = InventoryIncidentSeverity.MEDIUM,
    val status: InventoryIncidentStatus = InventoryIncidentStatus.OPEN,
    val title: String,
    val description: String? = null,
    val assigneePeerIds: Set<String> = emptySet(),
    val reportedByPeerId: String,
    val reportedAt: Instant,
    val updatedAt: Instant,
    val reviewedByPeerId: String? = null,
    val reviewedAt: Instant? = null,
    val reviewComment: String? = null,
    val resolvedByPeerId: String? = null,
    val resolvedAt: Instant? = null,
    val attachmentIds: List<String> = emptyList(),
    val commentIds: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class InventoryAlertEvent(
    val alertId: String,
    val organizationId: String,
    val ruleId: String? = null,
    val inventoryItemId: String? = null,
    val sessionId: String? = null,
    val severity: InventoryIncidentSeverity = InventoryIncidentSeverity.MEDIUM,
    val message: String,
    val createdAt: Instant,
    val acknowledgedByPeerId: String? = null,
    val acknowledgedAt: Instant? = null,
)

@Serializable
enum class InventoryRuleType {
    NEXT_INVENTORY_DUE_DAYS,
    REVIEW_STALE_DAYS,
    SESSION_OVERDUE_DAYS,
    INCIDENT_OPEN_DAYS,
    CUSTOM,
}

@Serializable
data class InventoryRuleThreshold(
    val ruleId: String,
    val organizationId: String,
    val ruleType: InventoryRuleType,
    val thresholdValue: Long? = null,
    val active: Boolean = true,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
enum class InventoryReminderStatus {
    PENDING,
    SENT,
    ACKNOWLEDGED,
    DISMISSED,
}

@Serializable
data class InventoryReminder(
    val reminderId: String,
    val organizationId: String,
    val ruleId: String? = null,
    val inventoryItemId: String? = null,
    val sessionId: String? = null,
    val dueAt: Instant,
    val status: InventoryReminderStatus = InventoryReminderStatus.PENDING,
    val message: String? = null,
    val recipientPeerIds: Set<String> = emptySet(),
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class InventoryDeadlineTarget {
    NEXT_INVENTORY,
    SESSION_END,
    REVIEW,
}

@Serializable
data class InventoryDeadlineRule(
    val ruleId: String,
    val organizationId: String,
    val target: InventoryDeadlineTarget,
    val daysBefore: Int,
    val active: Boolean = true,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventoryFieldChange(
    val field: String,
    val previousValue: String? = null,
    val newValue: String? = null,
)

@Serializable
data class InventoryChangeLog(
    val changeId: String,
    val organizationId: String,
    val entityType: InventoryEntityType,
    val entityId: String,
    val changedByPeerId: String,
    val changedAt: Instant,
    val changes: List<InventoryFieldChange> = emptyList(),
    val reason: String? = null,
    val sessionId: String? = null,
    val sourcePeerId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class InventorySessionResult(
    val summary: String? = null,
    val confirmedCount: Int = 0,
    val rejectedCount: Int = 0,
    val requiresUpdateCount: Int = 0,
    val incidentCount: Int = 0,
    val completedAt: Instant? = null,
    val approvedByPeerId: String? = null,
)

@Serializable
data class InventoryDashboardSnapshot(
    val snapshotId: String,
    val organizationId: String,
    val generatedAt: Instant,
    val totalItems: Int,
    val byStatus: Map<InventoryStatus, Int> = emptyMap(),
    val byCondition: Map<InventoryCondition, Int> = emptyMap(),
    val byCategoryId: Map<String, Int> = emptyMap(),
    val byLocationId: Map<String, Int> = emptyMap(),
    val incidentsOpen: Int = 0,
    val sessionsActive: Int = 0,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class InventorySessionMetrics(
    val sessionId: String,
    val totalItems: Int,
    val confirmed: Int,
    val rejected: Int,
    val underReview: Int,
    val generatedAt: Instant,
)

@Serializable
data class InventoryOrganizationMetrics(
    val organizationId: String,
    val totalItems: Int,
    val confirmed: Int,
    val rejected: Int,
    val underReview: Int,
    val incidentsOpen: Int,
    val generatedAt: Instant,
)

@Serializable
data class InventoryLocationMetrics(
    val locationId: String,
    val totalItems: Int,
    val confirmed: Int,
    val rejected: Int,
    val underReview: Int,
    val generatedAt: Instant,
)

@Serializable
data class InventoryCategoryMetrics(
    val categoryId: String,
    val totalItems: Int,
    val confirmed: Int,
    val rejected: Int,
    val underReview: Int,
    val generatedAt: Instant,
)

@Serializable
data class InventoryFilterSet(
    val categoryIds: Set<String> = emptySet(),
    val subcategoryIds: Set<String> = emptySet(),
    val locationIds: Set<String> = emptySet(),
    val departmentIds: Set<String> = emptySet(),
    val costCenterIds: Set<String> = emptySet(),
    val ownerIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
    val status: Set<InventoryStatus> = emptySet(),
    val condition: Set<InventoryCondition> = emptySet(),
    val incidentOnly: Boolean = false,
    val sessionId: String? = null,
    val updatedSince: Instant? = null,
    val nextInventoryBefore: Instant? = null,
)

@Serializable
enum class InventorySortMode {
    UPDATED_AT_DESC,
    UPDATED_AT_ASC,
    TITLE_ASC,
    INVENTORY_NUMBER_ASC,
    NEXT_INVENTORY_ASC,
}

@Serializable
data class InventorySearchQuery(
    val organizationId: String,
    val text: String? = null,
    val inventoryNumber: String? = null,
    val localNumber: String? = null,
    val serialNumber: String? = null,
    val qrCode: String? = null,
    val barcode: String? = null,
    val filter: InventoryFilterSet = InventoryFilterSet(),
    val sort: InventorySortMode = InventorySortMode.UPDATED_AT_DESC,
    val limit: Int = 100,
    val offset: Int = 0,
)

@Serializable
data class InventorySearchResult(
    val organizationId: String,
    val total: Int,
    val items: List<InventoryItem>,
)

@Serializable
enum class InventorySyncStatus {
    SYNCED,
    PENDING,
    CONFLICTED,
}

@Serializable
data class InventoryRevision(
    val organizationId: String,
    val entityType: InventoryEntityType,
    val entityId: String,
    val revision: Long,
    val updatedAt: Instant,
)

@Serializable
data class InventoryConflict(
    val conflictId: String,
    val organizationId: String,
    val entityType: InventoryEntityType,
    val entityId: String,
    val localRevision: Long,
    val incomingRevision: Long,
    val detectedAt: Instant,
    val status: InventorySyncStatus = InventorySyncStatus.CONFLICTED,
    val note: String? = null,
)

@Serializable
data class InventoryMergeResult(
    val organizationId: String,
    val entityType: InventoryEntityType,
    val entityId: String,
    val resolved: Boolean,
    val resolvedAt: Instant,
    val note: String? = null,
)

@Serializable
@SerialName("SubcategorySnapshot")
data class InventorySubcategorySnapshot(
    val subcategory: InventorySubcategory,
) : InventoryEventPayload

@Serializable
@SerialName("TagSnapshot")
data class InventoryTagSnapshot(
    val tag: InventoryTag,
) : InventoryEventPayload

@Serializable
@SerialName("AttributeDefinitionSnapshot")
data class InventoryAttributeDefinitionSnapshot(
    val definition: InventoryAttributeDefinition,
) : InventoryEventPayload

@Serializable
@SerialName("CategoryTemplateSnapshot")
data class InventoryCategoryTemplateSnapshot(
    val template: InventoryCategoryTemplate,
) : InventoryEventPayload

@Serializable
@SerialName("OwnerSnapshot")
data class InventoryOwnerSnapshot(
    val owner: InventoryOwner,
) : InventoryEventPayload

@Serializable
@SerialName("DepartmentSnapshot")
data class InventoryDepartmentSnapshot(
    val department: InventoryDepartment,
) : InventoryEventPayload

@Serializable
@SerialName("CostCenterSnapshot")
data class InventoryCostCenterSnapshot(
    val costCenter: InventoryCostCenter,
) : InventoryEventPayload

@Serializable
@SerialName("LegalHolderSnapshot")
data class InventoryLegalHolderSnapshot(
    val legalHolder: InventoryLegalHolder,
) : InventoryEventPayload

@Serializable
@SerialName("SupplierSnapshot")
data class InventorySupplierSnapshot(
    val supplier: InventorySupplier,
) : InventoryEventPayload

@Serializable
@SerialName("FundingSourceSnapshot")
data class InventoryFundingSourceSnapshot(
    val fundingSource: InventoryFundingSource,
) : InventoryEventPayload

@Serializable
@SerialName("IncidentSnapshot")
data class InventoryIncidentSnapshot(
    val incident: InventoryIncident,
) : InventoryEventPayload

@Serializable
@SerialName("AlertSnapshot")
data class InventoryAlertSnapshot(
    val alert: InventoryAlertEvent,
) : InventoryEventPayload

@Serializable
@SerialName("ReminderSnapshot")
data class InventoryReminderSnapshot(
    val reminder: InventoryReminder,
) : InventoryEventPayload

@Serializable
@SerialName("RuleThresholdSnapshot")
data class InventoryRuleThresholdSnapshot(
    val rule: InventoryRuleThreshold,
) : InventoryEventPayload

@Serializable
@SerialName("DeadlineRuleSnapshot")
data class InventoryDeadlineRuleSnapshot(
    val rule: InventoryDeadlineRule,
) : InventoryEventPayload

@Serializable
@SerialName("ChangeLogSnapshot")
data class InventoryChangeLogSnapshot(
    val changeLog: InventoryChangeLog,
) : InventoryEventPayload

@Serializable
@SerialName("DashboardSnapshot")
data class InventoryDashboardSnapshotPayload(
    val snapshot: InventoryDashboardSnapshot,
) : InventoryEventPayload

@Serializable
@SerialName("CodeBindingSnapshot")
data class InventoryCodeBindingSnapshot(
    val binding: InventoryCodeBinding,
) : InventoryEventPayload

@Serializable
@SerialName("CodeSnapshot")
data class InventoryCodeSnapshot(
    val code: InventoryCode,
) : InventoryEventPayload

@Serializable
@SerialName("LabelTemplateSnapshot")
data class InventoryLabelTemplateSnapshot(
    val template: InventoryLabelTemplate,
) : InventoryEventPayload

@Serializable
@SerialName("LabelSnapshot")
data class InventoryLabelSnapshot(
    val label: InventoryLabel,
) : InventoryEventPayload

@Serializable
@SerialName("PrintTaskSnapshot")
data class InventoryPrintTaskSnapshot(
    val task: InventoryPrintTask,
) : InventoryEventPayload

@Serializable
@SerialName("ScanEventSnapshot")
data class InventoryScanEventSnapshot(
    val event: InventoryScanEvent,
) : InventoryEventPayload

@Serializable
@SerialName("ConflictSnapshot")
data class InventoryConflictSnapshot(
    val conflict: InventoryConflict,
) : InventoryEventPayload

@Serializable
@SerialName("RevisionSnapshot")
data class InventoryRevisionSnapshot(
    val revision: InventoryRevision,
) : InventoryEventPayload
