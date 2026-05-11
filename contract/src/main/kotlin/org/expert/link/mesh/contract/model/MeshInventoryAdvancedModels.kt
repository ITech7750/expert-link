package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MeshInventoryItemType {
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
enum class MeshInventoryLocationType {
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
enum class MeshInventoryAttachmentStatus {
    AVAILABLE,
    PENDING,
    FAILED,
    ARCHIVED,
}

@Serializable
data class MeshInventoryAttachmentPreviewMetadata(
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String? = null,
)

@Serializable
enum class MeshInventoryAttributeType {
    TEXT,
    NUMBER,
    DATE,
    BOOLEAN,
    OPTION,
    MULTI_OPTION,
}

@Serializable
data class MeshInventoryAttributeDefinition(
    val attributeId: String,
    val organizationId: String,
    val key: String,
    val label: String,
    val description: String? = null,
    val type: MeshInventoryAttributeType = MeshInventoryAttributeType.TEXT,
    val required: Boolean = false,
    val unit: String? = null,
    val options: List<String> = emptyList(),
    val validationRules: List<MeshInventoryValidationRule> = emptyList(),
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class MeshInventoryAttributeValue(
    val attributeId: String,
    val value: String,
    val updatedByPeerId: String,
    val updatedAt: Instant,
)

@Serializable
data class MeshInventoryTag(
    val tagId: String,
    val organizationId: String,
    val name: String,
    val color: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class MeshInventorySubcategory(
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
data class MeshInventoryCategoryTemplate(
    val templateId: String,
    val organizationId: String,
    val categoryId: String,
    val name: String,
    val description: String? = null,
    val fields: List<MeshInventoryFieldTemplate> = emptyList(),
    val requiredFields: List<MeshInventoryRequiredFieldRule> = emptyList(),
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class MeshInventoryFieldTemplate(
    val fieldId: String,
    val key: String,
    val label: String,
    val type: MeshInventoryAttributeType = MeshInventoryAttributeType.TEXT,
    val required: Boolean = false,
    val validationRules: List<MeshInventoryValidationRule> = emptyList(),
    val helpText: String? = null,
    val order: Int = 0,
)

@Serializable
enum class MeshInventoryValidationRuleType {
    REGEX,
    RANGE,
    LENGTH,
    ENUM,
    REQUIRED,
}

@Serializable
data class MeshInventoryValidationRule(
    val type: MeshInventoryValidationRuleType,
    val params: Map<String, String> = emptyMap(),
    val message: String? = null,
)

@Serializable
data class MeshInventoryRequiredFieldRule(
    val fieldId: String,
    val message: String? = null,
)

@Serializable
enum class MeshInventoryOwnerType {
    ORGANIZATION,
    DEPARTMENT,
    PERSON,
    EXTERNAL,
}

@Serializable
data class MeshInventoryOwner(
    val ownerId: String,
    val organizationId: String,
    val name: String,
    val type: MeshInventoryOwnerType = MeshInventoryOwnerType.ORGANIZATION,
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
data class MeshInventoryDepartment(
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
data class MeshInventoryCostCenter(
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
data class MeshInventoryLegalHolder(
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
data class MeshInventorySupplier(
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
data class MeshInventoryFundingSource(
    val fundingSourceId: String,
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class MeshInventoryCodeType {
    QR,
    BARCODE,
    NFC,
    RFID,
}

@Serializable
data class MeshInventoryCodeBinding(
    val codeId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val sessionId: String? = null,
    val codeType: MeshInventoryCodeType = MeshInventoryCodeType.QR,
    val codeValue: String,
    val createdByPeerId: String,
    val createdAt: Instant,
)

@Serializable
data class MeshInventoryScanEvent(
    val scanEventId: String,
    val organizationId: String,
    val codeType: MeshInventoryCodeType,
    val codeValue: String,
    val rawValue: String = codeValue,
    val inventoryItemId: String? = null,
    val sessionId: String? = null,
    val locationId: String? = null,
    val locationHint: String? = null,
    val scannedByPeerId: String,
    val scannedAt: Instant,
    val resultStatus: MeshInventoryScanResultStatus = MeshInventoryScanResultStatus.RESOLVED,
    val deviceId: String? = null,
    val note: String? = null,
)

@Serializable
enum class MeshInventoryIncidentType {
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
enum class MeshInventoryIncidentSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

@Serializable
enum class MeshInventoryIncidentStatus {
    OPEN,
    UNDER_REVIEW,
    RESOLVED,
    DISMISSED,
}

@Serializable
data class MeshInventoryIncident(
    val incidentId: String,
    val organizationId: String,
    val inventoryItemId: String? = null,
    val sessionId: String? = null,
    val locationId: String? = null,
    val type: MeshInventoryIncidentType,
    val severity: MeshInventoryIncidentSeverity = MeshInventoryIncidentSeverity.MEDIUM,
    val status: MeshInventoryIncidentStatus = MeshInventoryIncidentStatus.OPEN,
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
data class MeshInventoryAlertEvent(
    val alertId: String,
    val organizationId: String,
    val ruleId: String? = null,
    val inventoryItemId: String? = null,
    val sessionId: String? = null,
    val severity: MeshInventoryIncidentSeverity = MeshInventoryIncidentSeverity.MEDIUM,
    val message: String,
    val createdAt: Instant,
    val acknowledgedByPeerId: String? = null,
    val acknowledgedAt: Instant? = null,
)

@Serializable
enum class MeshInventoryRuleType {
    NEXT_INVENTORY_DUE_DAYS,
    REVIEW_STALE_DAYS,
    SESSION_OVERDUE_DAYS,
    INCIDENT_OPEN_DAYS,
    CUSTOM,
}

@Serializable
data class MeshInventoryRuleThreshold(
    val ruleId: String,
    val organizationId: String,
    val ruleType: MeshInventoryRuleType,
    val thresholdValue: Long? = null,
    val active: Boolean = true,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
enum class MeshInventoryReminderStatus {
    PENDING,
    SENT,
    ACKNOWLEDGED,
    DISMISSED,
}

@Serializable
data class MeshInventoryReminder(
    val reminderId: String,
    val organizationId: String,
    val ruleId: String? = null,
    val inventoryItemId: String? = null,
    val sessionId: String? = null,
    val dueAt: Instant,
    val status: MeshInventoryReminderStatus = MeshInventoryReminderStatus.PENDING,
    val message: String? = null,
    val recipientPeerIds: Set<String> = emptySet(),
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class MeshInventoryDeadlineTarget {
    NEXT_INVENTORY,
    SESSION_END,
    REVIEW,
}

@Serializable
data class MeshInventoryDeadlineRule(
    val ruleId: String,
    val organizationId: String,
    val target: MeshInventoryDeadlineTarget,
    val daysBefore: Int,
    val active: Boolean = true,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class MeshInventoryFieldChange(
    val field: String,
    val previousValue: String? = null,
    val newValue: String? = null,
)

@Serializable
data class MeshInventoryChangeLog(
    val changeId: String,
    val organizationId: String,
    val entityType: MeshInventoryEntityType,
    val entityId: String,
    val changedByPeerId: String,
    val changedAt: Instant,
    val changes: List<MeshInventoryFieldChange> = emptyList(),
    val reason: String? = null,
    val sessionId: String? = null,
    val sourcePeerId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class MeshInventorySessionResult(
    val summary: String? = null,
    val confirmedCount: Int = 0,
    val rejectedCount: Int = 0,
    val requiresUpdateCount: Int = 0,
    val incidentCount: Int = 0,
    val completedAt: Instant? = null,
    val approvedByPeerId: String? = null,
)

@Serializable
data class MeshInventoryDashboardSnapshot(
    val snapshotId: String,
    val organizationId: String,
    val generatedAt: Instant,
    val totalItems: Int,
    val byStatus: Map<MeshInventoryStatus, Int> = emptyMap(),
    val byCondition: Map<MeshInventoryCondition, Int> = emptyMap(),
    val byCategoryId: Map<String, Int> = emptyMap(),
    val byLocationId: Map<String, Int> = emptyMap(),
    val incidentsOpen: Int = 0,
    val sessionsActive: Int = 0,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class MeshInventorySessionMetrics(
    val sessionId: String,
    val totalItems: Int,
    val confirmed: Int,
    val rejected: Int,
    val underReview: Int,
    val generatedAt: Instant,
)

@Serializable
data class MeshInventoryOrganizationMetrics(
    val organizationId: String,
    val totalItems: Int,
    val confirmed: Int,
    val rejected: Int,
    val underReview: Int,
    val incidentsOpen: Int,
    val generatedAt: Instant,
)

@Serializable
data class MeshInventoryLocationMetrics(
    val locationId: String,
    val totalItems: Int,
    val confirmed: Int,
    val rejected: Int,
    val underReview: Int,
    val generatedAt: Instant,
)

@Serializable
data class MeshInventoryCategoryMetrics(
    val categoryId: String,
    val totalItems: Int,
    val confirmed: Int,
    val rejected: Int,
    val underReview: Int,
    val generatedAt: Instant,
)

@Serializable
data class MeshInventoryFilterSet(
    val categoryIds: Set<String> = emptySet(),
    val subcategoryIds: Set<String> = emptySet(),
    val locationIds: Set<String> = emptySet(),
    val departmentIds: Set<String> = emptySet(),
    val costCenterIds: Set<String> = emptySet(),
    val ownerIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
    val status: Set<MeshInventoryStatus> = emptySet(),
    val condition: Set<MeshInventoryCondition> = emptySet(),
    val incidentOnly: Boolean = false,
    val sessionId: String? = null,
    val updatedSince: Instant? = null,
    val nextInventoryBefore: Instant? = null,
)

@Serializable
enum class MeshInventorySortMode {
    UPDATED_AT_DESC,
    UPDATED_AT_ASC,
    TITLE_ASC,
    INVENTORY_NUMBER_ASC,
    NEXT_INVENTORY_ASC,
}

@Serializable
data class MeshInventorySearchQuery(
    val organizationId: String,
    val text: String? = null,
    val inventoryNumber: String? = null,
    val localNumber: String? = null,
    val serialNumber: String? = null,
    val qrCode: String? = null,
    val barcode: String? = null,
    val filter: MeshInventoryFilterSet = MeshInventoryFilterSet(),
    val sort: MeshInventorySortMode = MeshInventorySortMode.UPDATED_AT_DESC,
    val limit: Int = 100,
    val offset: Int = 0,
)

@Serializable
data class MeshInventorySearchResult(
    val organizationId: String,
    val total: Int,
    val items: List<MeshInventoryItem>,
)

@Serializable
enum class MeshInventorySyncStatus {
    SYNCED,
    PENDING,
    CONFLICTED,
}

@Serializable
data class MeshInventoryRevision(
    val organizationId: String,
    val entityType: MeshInventoryEntityType,
    val entityId: String,
    val revision: Long,
    val updatedAt: Instant,
)

@Serializable
data class MeshInventoryConflict(
    val conflictId: String,
    val organizationId: String,
    val entityType: MeshInventoryEntityType,
    val entityId: String,
    val localRevision: Long,
    val incomingRevision: Long,
    val detectedAt: Instant,
    val status: MeshInventorySyncStatus = MeshInventorySyncStatus.CONFLICTED,
    val note: String? = null,
)

@Serializable
data class MeshInventoryMergeResult(
    val organizationId: String,
    val entityType: MeshInventoryEntityType,
    val entityId: String,
    val resolved: Boolean,
    val resolvedAt: Instant,
    val note: String? = null,
)

@Serializable
@SerialName("SubcategorySnapshot")
data class MeshInventorySubcategorySnapshot(
    val subcategory: MeshInventorySubcategory,
) : MeshInventoryEventPayload

@Serializable
@SerialName("TagSnapshot")
data class MeshInventoryTagSnapshot(
    val tag: MeshInventoryTag,
) : MeshInventoryEventPayload

@Serializable
@SerialName("AttributeDefinitionSnapshot")
data class MeshInventoryAttributeDefinitionSnapshot(
    val definition: MeshInventoryAttributeDefinition,
) : MeshInventoryEventPayload

@Serializable
@SerialName("CategoryTemplateSnapshot")
data class MeshInventoryCategoryTemplateSnapshot(
    val template: MeshInventoryCategoryTemplate,
) : MeshInventoryEventPayload

@Serializable
@SerialName("OwnerSnapshot")
data class MeshInventoryOwnerSnapshot(
    val owner: MeshInventoryOwner,
) : MeshInventoryEventPayload

@Serializable
@SerialName("DepartmentSnapshot")
data class MeshInventoryDepartmentSnapshot(
    val department: MeshInventoryDepartment,
) : MeshInventoryEventPayload

@Serializable
@SerialName("CostCenterSnapshot")
data class MeshInventoryCostCenterSnapshot(
    val costCenter: MeshInventoryCostCenter,
) : MeshInventoryEventPayload

@Serializable
@SerialName("LegalHolderSnapshot")
data class MeshInventoryLegalHolderSnapshot(
    val legalHolder: MeshInventoryLegalHolder,
) : MeshInventoryEventPayload

@Serializable
@SerialName("SupplierSnapshot")
data class MeshInventorySupplierSnapshot(
    val supplier: MeshInventorySupplier,
) : MeshInventoryEventPayload

@Serializable
@SerialName("FundingSourceSnapshot")
data class MeshInventoryFundingSourceSnapshot(
    val fundingSource: MeshInventoryFundingSource,
) : MeshInventoryEventPayload

@Serializable
@SerialName("IncidentSnapshot")
data class MeshInventoryIncidentSnapshot(
    val incident: MeshInventoryIncident,
) : MeshInventoryEventPayload

@Serializable
@SerialName("AlertSnapshot")
data class MeshInventoryAlertSnapshot(
    val alert: MeshInventoryAlertEvent,
) : MeshInventoryEventPayload

@Serializable
@SerialName("ReminderSnapshot")
data class MeshInventoryReminderSnapshot(
    val reminder: MeshInventoryReminder,
) : MeshInventoryEventPayload

@Serializable
@SerialName("RuleThresholdSnapshot")
data class MeshInventoryRuleThresholdSnapshot(
    val rule: MeshInventoryRuleThreshold,
) : MeshInventoryEventPayload

@Serializable
@SerialName("DeadlineRuleSnapshot")
data class MeshInventoryDeadlineRuleSnapshot(
    val rule: MeshInventoryDeadlineRule,
) : MeshInventoryEventPayload

@Serializable
@SerialName("ChangeLogSnapshot")
data class MeshInventoryChangeLogSnapshot(
    val changeLog: MeshInventoryChangeLog,
) : MeshInventoryEventPayload

@Serializable
@SerialName("DashboardSnapshot")
data class MeshInventoryDashboardSnapshotPayload(
    val snapshot: MeshInventoryDashboardSnapshot,
) : MeshInventoryEventPayload

@Serializable
@SerialName("CodeBindingSnapshot")
data class MeshInventoryCodeBindingSnapshot(
    val binding: MeshInventoryCodeBinding,
) : MeshInventoryEventPayload

@Serializable
@SerialName("CodeSnapshot")
data class MeshInventoryCodeSnapshot(
    val code: MeshInventoryCode,
) : MeshInventoryEventPayload

@Serializable
@SerialName("LabelTemplateSnapshot")
data class MeshInventoryLabelTemplateSnapshot(
    val template: MeshInventoryLabelTemplate,
) : MeshInventoryEventPayload

@Serializable
@SerialName("LabelSnapshot")
data class MeshInventoryLabelSnapshot(
    val label: MeshInventoryLabel,
) : MeshInventoryEventPayload

@Serializable
@SerialName("PrintTaskSnapshot")
data class MeshInventoryPrintTaskSnapshot(
    val task: MeshInventoryPrintTask,
) : MeshInventoryEventPayload

@Serializable
@SerialName("ScanEventSnapshot")
data class MeshInventoryScanEventSnapshot(
    val event: MeshInventoryScanEvent,
) : MeshInventoryEventPayload

@Serializable
@SerialName("ConflictSnapshot")
data class MeshInventoryConflictSnapshot(
    val conflict: MeshInventoryConflict,
) : MeshInventoryEventPayload

@Serializable
@SerialName("RevisionSnapshot")
data class MeshInventoryRevisionSnapshot(
    val revision: MeshInventoryRevision,
) : MeshInventoryEventPayload
