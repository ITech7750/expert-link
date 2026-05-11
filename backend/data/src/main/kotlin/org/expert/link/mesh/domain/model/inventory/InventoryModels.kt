package org.expert.link.mesh.domain.model.inventory

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor

@Serializable
data class Organization(
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class OrganizationMemberStatus {
    ACTIVE,
    INVITED,
    SUSPENDED,
    LEFT,
}

@Serializable
data class OrganizationMember(
    val organizationId: String,
    val peerId: String,
    val displayName: String,
    val roleIds: Set<String> = emptySet(),
    val departmentId: String? = null,
    val locationIds: Set<String> = emptySet(),
    val position: String? = null,
    val isCommissionMember: Boolean = false,
    val status: OrganizationMemberStatus = OrganizationMemberStatus.ACTIVE,
    val joinedAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class InventoryPermission {
    ORGANIZATION_MANAGE,
    MEMBER_MANAGE,
    ROLE_MANAGE,
    CATEGORY_MANAGE,
    LOCATION_MANAGE,
    TAG_MANAGE,
    ATTRIBUTE_MANAGE,
    TEMPLATE_MANAGE,
    OWNER_MANAGE,
    ITEM_CREATE,
    ITEM_EDIT,
    ITEM_VIEW,
    ITEM_SUBMIT_REVIEW,
    ITEM_CONFIRM,
    ITEM_REJECT,
    ITEM_ARCHIVE,
    ITEM_COMMENT,
    ITEM_ATTACH,
    ITEM_UPLOAD_PHOTO,
    ITEM_INCIDENT,
    SESSION_CREATE,
    SESSION_UPDATE,
    SESSION_ADD_ITEM,
    SESSION_ADD_MEMBER,
    SESSION_CLOSE,
    INCIDENT_VIEW,
    INCIDENT_CREATE,
    INCIDENT_REVIEW,
    INCIDENT_RESOLVE,
    REMINDER_MANAGE,
    DASHBOARD_VIEW,
    SEARCH,
    EXPORT_REQUEST,
    EXPORT_MANAGE,
    EXPORT_VIEW,
    AUDIT_VIEW,
}

@Serializable
data class Role(
    val roleId: String,
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val permissions: Set<InventoryPermission> = emptySet(),
    val system: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventoryCategory(
    val categoryId: String,
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val parentCategoryId: String? = null,
    val templateId: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventoryLocation(
    val locationId: String,
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val parentLocationId: String? = null,
    val locationType: InventoryLocationType = InventoryLocationType.OTHER,
    val code: String? = null,
    val path: String? = null,
    val departmentId: String? = null,
    val archived: Boolean = false,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class InventoryCondition {
    NEW,
    GOOD,
    FAIR,
    NEEDS_REPAIR,
    OUT_OF_SERVICE,
    UNKNOWN,
}

@Serializable
enum class InventoryStatus {
    DRAFT,
    ADDED,
    UNDER_REVIEW,
    CONFIRMED,
    REJECTED,
    REQUIRES_UPDATE,
    ARCHIVED,
}

@Serializable
data class InventoryItem(
    val inventoryItemId: String,
    val organizationId: String,
    val inventoryNumber: String,
    val localNumber: String? = null,
    val qrCode: String? = null,
    val barcode: String? = null,
    val categoryId: String? = null,
    val subcategoryId: String? = null,
    val itemType: InventoryItemType = InventoryItemType.UNKNOWN,
    val title: String,
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val manufacturer: String? = null,
    val purchaseDate: Instant? = null,
    val commissioningDate: Instant? = null,
    val warrantyUntil: Instant? = null,
    val depreciationGroup: String? = null,
    val usefulLifeMonths: Int? = null,
    val condition: InventoryCondition = InventoryCondition.UNKNOWN,
    val locationId: String? = null,
    val responsiblePerson: String? = null,
    val responsibleDepartment: String? = null,
    val responsibleUserId: String? = null,
    val responsibleOwnerIds: Set<String> = emptySet(),
    val ownerOrganizationId: String? = null,
    val ownerId: String? = null,
    val departmentId: String? = null,
    val costCenterId: String? = null,
    val legalHolderId: String? = null,
    val supplierId: String? = null,
    val fundingSourceId: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastInventoryAt: Instant? = null,
    val nextInventoryAt: Instant? = null,
    val photoAttachmentIds: List<String> = emptyList(),
    val attachmentIds: List<String> = emptyList(),
    val commentIds: List<String> = emptyList(),
    val incidentIds: List<String> = emptyList(),
    val tagIds: Set<String> = emptySet(),
    val attributes: List<InventoryAttributeValue> = emptyList(),
    val currentStatus: InventoryStatus = InventoryStatus.DRAFT,
    val syncStatus: InventorySyncStatus = InventorySyncStatus.SYNCED,
    val revision: Long = 1,
    val sessionIds: Set<String> = emptySet(),
    val chatId: String? = null,
    val threadRootMessageId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
enum class InventoryWorkflowStatus {
    CREATED,
    IN_PROGRESS,
    PASSED,
    FAILED,
    REQUIRES_CORRECTION,
    SENT_TO_COMMISSION,
    COMPLETED,
}

@Serializable
enum class InventorySessionStatus {
    DRAFT,
    ACTIVE,
    UNDER_REVIEW,
    CLOSED,
    ARCHIVED,
}

@Serializable
enum class InventorySessionReviewStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    REJECTED,
}

@Serializable
data class InventorySession(
    val sessionId: String,
    val organizationId: String,
    val title: String,
    val description: String? = null,
    val periodStart: Instant,
    val periodEnd: Instant? = null,
    val status: InventorySessionStatus = InventorySessionStatus.DRAFT,
    val reviewStatus: InventorySessionReviewStatus = InventorySessionReviewStatus.PENDING,
    val workflowStatus: InventoryWorkflowStatus = InventoryWorkflowStatus.CREATED,
    val result: InventorySessionResult? = null,
    val departmentIds: Set<String> = emptySet(),
    val locationIds: Set<String> = emptySet(),
    val ownerIds: Set<String> = emptySet(),
    val requiresPhotoForDiscrepancy: Boolean = true,
    val completionBlockedReason: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val itemIds: Set<String> = emptySet(),
    val memberPeerIds: Set<String> = emptySet(),
    val exportTaskIds: Set<String> = emptySet(),
    val chatId: String? = null,
    val threadRootMessageId: String? = null,
    val revision: Long = 1,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
enum class InventorySessionRole {
    CHAIR,
    MEMBER,
    REVIEWER,
    OBSERVER,
}

@Serializable
data class InventorySessionMember(
    val sessionId: String,
    val organizationId: String,
    val peerId: String,
    val role: InventorySessionRole,
    val assignedByPeerId: String,
    val assignedAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class InventoryReviewStatus {
    APPROVED,
    REJECTED,
    REQUIRES_UPDATE,
}

@Serializable
enum class InventoryPresenceStatus {
    UNCHECKED,
    PRESENT,
    ABSENT,
}

@Serializable
enum class InventoryAcceptanceStatus {
    UNCHECKED,
    ACCEPTED,
    NOT_ACCEPTED,
}

@Serializable
enum class InventoryConfirmationStatus {
    UNCHECKED,
    CONFIRMED,
    NOT_CONFIRMED,
}

@Serializable
data class InventoryReview(
    val reviewId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val sessionId: String? = null,
    val reviewerPeerId: String,
    val status: InventoryReviewStatus,
    val presenceStatus: InventoryPresenceStatus = InventoryPresenceStatus.UNCHECKED,
    val acceptanceStatus: InventoryAcceptanceStatus = InventoryAcceptanceStatus.UNCHECKED,
    val confirmationStatus: InventoryConfirmationStatus = InventoryConfirmationStatus.UNCHECKED,
    val requiresPhoto: Boolean = false,
    val comment: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class InventoryComment(
    val commentId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val sessionId: String? = null,
    val authorPeerId: String,
    val body: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class InventoryAttachmentType {
    PHOTO,
    DOCUMENT,
}

@Serializable
data class InventoryAttachment(
    val attachmentId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val sessionId: String? = null,
    val uploadedByPeerId: String,
    val descriptor: FileDescriptor,
    val transferId: String? = null,
    val attachmentType: InventoryAttachmentType = InventoryAttachmentType.DOCUMENT,
    val status: InventoryAttachmentStatus = InventoryAttachmentStatus.AVAILABLE,
    val checksum: String? = null,
    val preview: InventoryAttachmentPreviewMetadata? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val note: String? = null,
)

@Serializable
enum class InventoryExportStatus {
    REQUESTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
}

@Serializable
enum class InventoryExportFormat {
    CSV,
    XLSX,
    PDF,
}

@Serializable
data class InventoryExportTask(
    val exportTaskId: String,
    val organizationId: String,
    val sessionId: String,
    val requestedByPeerId: String,
    val format: InventoryExportFormat,
    val status: InventoryExportStatus = InventoryExportStatus.REQUESTED,
    val createdAt: Instant,
    val updatedAt: Instant,
    val resultAttachmentId: String? = null,
    val resultDescriptor: FileDescriptor? = null,
    val errorMessage: String? = null,
)

@Serializable
data class InventoryQrCode(
    val codeId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val qrCode: String,
    val barcode: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
)

@Serializable
enum class InventoryEntityType {
    ORGANIZATION,
    MEMBER,
    ROLE,
    CATEGORY,
    SUBCATEGORY,
    TAG,
    ATTRIBUTE_DEFINITION,
    CATEGORY_TEMPLATE,
    OWNER,
    DEPARTMENT,
    COST_CENTER,
    LEGAL_HOLDER,
    SUPPLIER,
    FUNDING_SOURCE,
    LOCATION,
    ITEM,
    SESSION,
    SESSION_MEMBER,
    REVIEW,
    COMMENT,
    ATTACHMENT,
    INCIDENT,
    ALERT,
    REMINDER,
    RULE_THRESHOLD,
    DEADLINE_RULE,
    CHANGELOG,
    DASHBOARD_SNAPSHOT,
    SEARCH_INDEX,
    EXPORT,
    QRCODE,
    CODE,
    CODE_BINDING,
    LABEL_TEMPLATE,
    LABEL,
    PRINT_TASK,
    SCAN_EVENT,
    CONFLICT,
    REVISION,
}

@Serializable
enum class InventoryEventType {
    CREATED,
    UPDATED,
    STATUS_CHANGED,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    REVIEW_SUBMITTED,
    COMMENT_ADDED,
    ATTACHMENT_ADDED,
    SESSION_CLOSED,
    EXPORT_REQUESTED,
    EXPORT_COMPLETED,
    EXPORT_FAILED,
    CODE_GENERATED,
    CODE_REGENERATED,
    CODE_DEACTIVATED,
    LABEL_TEMPLATE_CREATED,
    LABEL_TEMPLATE_UPDATED,
    LABEL_PRINTED,
    PRINT_TASK_CREATED,
    PRINT_TASK_COMPLETED,
    CONFLICT_DETECTED,
    INCIDENT_REPORTED,
    INCIDENT_RESOLVED,
    ALERT_TRIGGERED,
    REMINDER_CREATED,
    CHANGELOG_RECORDED,
    DASHBOARD_SNAPSHOT,
}

@Serializable
sealed interface InventoryEventPayload

@Serializable
@SerialName("OrganizationSnapshot")
data class InventoryOrganizationSnapshot(
    val organization: Organization,
) : InventoryEventPayload

@Serializable
@SerialName("MemberSnapshot")
data class InventoryMemberSnapshot(
    val member: OrganizationMember,
) : InventoryEventPayload

@Serializable
@SerialName("RoleSnapshot")
data class InventoryRoleSnapshot(
    val role: Role,
) : InventoryEventPayload

@Serializable
@SerialName("CategorySnapshot")
data class InventoryCategorySnapshot(
    val category: InventoryCategory,
) : InventoryEventPayload

@Serializable
@SerialName("LocationSnapshot")
data class InventoryLocationSnapshot(
    val location: InventoryLocation,
) : InventoryEventPayload

@Serializable
@SerialName("ItemSnapshot")
data class InventoryItemSnapshot(
    val item: InventoryItem,
) : InventoryEventPayload

@Serializable
@SerialName("SessionSnapshot")
data class InventorySessionSnapshot(
    val session: InventorySession,
) : InventoryEventPayload

@Serializable
@SerialName("ReviewSnapshot")
data class InventoryReviewSnapshot(
    val review: InventoryReview,
) : InventoryEventPayload

@Serializable
@SerialName("CommentSnapshot")
data class InventoryCommentSnapshot(
    val comment: InventoryComment,
) : InventoryEventPayload

@Serializable
@SerialName("AttachmentSnapshot")
data class InventoryAttachmentSnapshot(
    val attachment: InventoryAttachment,
) : InventoryEventPayload

@Serializable
@SerialName("ExportSnapshot")
data class InventoryExportSnapshot(
    val exportTask: InventoryExportTask,
) : InventoryEventPayload

@Serializable
@SerialName("QrSnapshot")
data class InventoryQrSnapshot(
    val qrCode: InventoryQrCode,
) : InventoryEventPayload

@Serializable
data class InventoryEvent(
    val eventId: String,
    val organizationId: String,
    val entityType: InventoryEntityType,
    val entityId: String,
    val eventType: InventoryEventType,
    val actorPeerId: String,
    val occurredAt: Instant,
    val sequence: Long,
    val entityRevision: Long? = null,
    val previousEntityRevision: Long? = null,
    val sessionId: String? = null,
    val payload: InventoryEventPayload,
    val conflict: Boolean = false,
    val notes: String? = null,
)
