package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MeshOrganization(
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class MeshOrganizationMemberStatus {
    ACTIVE,
    INVITED,
    SUSPENDED,
    LEFT,
}

@Serializable
data class MeshOrganizationMember(
    val organizationId: String,
    val peerId: String,
    val displayName: String,
    val roleIds: Set<String> = emptySet(),
    val departmentId: String? = null,
    val locationIds: Set<String> = emptySet(),
    val position: String? = null,
    val isCommissionMember: Boolean = false,
    val status: MeshOrganizationMemberStatus = MeshOrganizationMemberStatus.ACTIVE,
    val joinedAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class MeshInventoryPermission {
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
data class MeshRole(
    val roleId: String,
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val permissions: Set<MeshInventoryPermission> = emptySet(),
    val system: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class MeshInventoryCategory(
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
data class MeshInventoryLocation(
    val locationId: String,
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val parentLocationId: String? = null,
    val locationType: MeshInventoryLocationType = MeshInventoryLocationType.OTHER,
    val code: String? = null,
    val path: String? = null,
    val departmentId: String? = null,
    val archived: Boolean = false,
    val createdByPeerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class MeshInventoryCondition {
    NEW,
    GOOD,
    FAIR,
    NEEDS_REPAIR,
    OUT_OF_SERVICE,
    UNKNOWN,
}

@Serializable
enum class MeshInventoryStatus {
    DRAFT,
    ADDED,
    UNDER_REVIEW,
    CONFIRMED,
    REJECTED,
    REQUIRES_UPDATE,
    ARCHIVED,
}

@Serializable
data class MeshInventoryItem(
    val inventoryItemId: String,
    val organizationId: String,
    val inventoryNumber: String,
    val localNumber: String? = null,
    val qrCode: String? = null,
    val barcode: String? = null,
    val categoryId: String? = null,
    val subcategoryId: String? = null,
    val itemType: MeshInventoryItemType = MeshInventoryItemType.UNKNOWN,
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
    val condition: MeshInventoryCondition = MeshInventoryCondition.UNKNOWN,
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
    val attributes: List<MeshInventoryAttributeValue> = emptyList(),
    val currentStatus: MeshInventoryStatus = MeshInventoryStatus.DRAFT,
    val syncStatus: MeshInventorySyncStatus = MeshInventorySyncStatus.SYNCED,
    val revision: Long = 1,
    val sessionIds: Set<String> = emptySet(),
    val chatId: String? = null,
    val threadRootMessageId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
enum class MeshInventoryWorkflowStatus {
    CREATED,
    IN_PROGRESS,
    PASSED,
    FAILED,
    REQUIRES_CORRECTION,
    SENT_TO_COMMISSION,
    COMPLETED,
}

@Serializable
enum class MeshInventorySessionStatus {
    DRAFT,
    ACTIVE,
    UNDER_REVIEW,
    CLOSED,
    ARCHIVED,
}

@Serializable
enum class MeshInventorySessionReviewStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    REJECTED,
}

@Serializable
data class MeshInventorySession(
    val sessionId: String,
    val organizationId: String,
    val title: String,
    val description: String? = null,
    val periodStart: Instant,
    val periodEnd: Instant? = null,
    val status: MeshInventorySessionStatus = MeshInventorySessionStatus.DRAFT,
    val reviewStatus: MeshInventorySessionReviewStatus = MeshInventorySessionReviewStatus.PENDING,
    val workflowStatus: MeshInventoryWorkflowStatus = MeshInventoryWorkflowStatus.CREATED,
    val result: MeshInventorySessionResult? = null,
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
enum class MeshInventorySessionRole {
    CHAIR,
    MEMBER,
    REVIEWER,
    OBSERVER,
}

@Serializable
data class MeshInventorySessionMember(
    val sessionId: String,
    val organizationId: String,
    val peerId: String,
    val role: MeshInventorySessionRole,
    val assignedByPeerId: String,
    val assignedAt: Instant,
    val updatedAt: Instant,
)

@Serializable
enum class MeshInventoryReviewStatus {
    APPROVED,
    REJECTED,
    REQUIRES_UPDATE,
}

@Serializable
enum class MeshInventoryPresenceStatus {
    UNCHECKED,
    PRESENT,
    ABSENT,
}

@Serializable
enum class MeshInventoryAcceptanceStatus {
    UNCHECKED,
    ACCEPTED,
    NOT_ACCEPTED,
}

@Serializable
enum class MeshInventoryConfirmationStatus {
    UNCHECKED,
    CONFIRMED,
    NOT_CONFIRMED,
}

@Serializable
data class MeshInventoryReview(
    val reviewId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val sessionId: String? = null,
    val reviewerPeerId: String,
    val status: MeshInventoryReviewStatus,
    val presenceStatus: MeshInventoryPresenceStatus = MeshInventoryPresenceStatus.UNCHECKED,
    val acceptanceStatus: MeshInventoryAcceptanceStatus = MeshInventoryAcceptanceStatus.UNCHECKED,
    val confirmationStatus: MeshInventoryConfirmationStatus = MeshInventoryConfirmationStatus.UNCHECKED,
    val requiresPhoto: Boolean = false,
    val comment: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class MeshInventoryComment(
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
enum class MeshInventoryAttachmentType {
    PHOTO,
    DOCUMENT,
}

@Serializable
data class MeshInventoryAttachment(
    val attachmentId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val sessionId: String? = null,
    val uploadedByPeerId: String,
    val descriptor: MeshFileDescriptor,
    val transferId: String? = null,
    val attachmentType: MeshInventoryAttachmentType = MeshInventoryAttachmentType.DOCUMENT,
    val status: MeshInventoryAttachmentStatus = MeshInventoryAttachmentStatus.AVAILABLE,
    val checksum: String? = null,
    val preview: MeshInventoryAttachmentPreviewMetadata? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val note: String? = null,
)

@Serializable
enum class MeshInventoryExportStatus {
    REQUESTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
}

@Serializable
enum class MeshInventoryExportFormat {
    CSV,
    XLSX,
    PDF,
}

@Serializable
data class MeshInventoryExportTask(
    val exportTaskId: String,
    val organizationId: String,
    val sessionId: String,
    val requestedByPeerId: String,
    val format: MeshInventoryExportFormat,
    val status: MeshInventoryExportStatus = MeshInventoryExportStatus.REQUESTED,
    val createdAt: Instant,
    val updatedAt: Instant,
    val resultAttachmentId: String? = null,
    val resultDescriptor: MeshFileDescriptor? = null,
    val errorMessage: String? = null,
)

@Serializable
data class MeshInventoryQrCode(
    val codeId: String,
    val organizationId: String,
    val inventoryItemId: String,
    val qrCode: String,
    val barcode: String? = null,
    val createdByPeerId: String,
    val createdAt: Instant,
)

@Serializable
enum class MeshInventoryEntityType {
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
enum class MeshInventoryEventType {
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
sealed interface MeshInventoryEventPayload

@Serializable
@SerialName("OrganizationSnapshot")
data class MeshInventoryOrganizationSnapshot(
    val organization: MeshOrganization,
) : MeshInventoryEventPayload

@Serializable
@SerialName("MemberSnapshot")
data class MeshInventoryMemberSnapshot(
    val member: MeshOrganizationMember,
) : MeshInventoryEventPayload

@Serializable
@SerialName("RoleSnapshot")
data class MeshInventoryRoleSnapshot(
    val role: MeshRole,
) : MeshInventoryEventPayload

@Serializable
@SerialName("CategorySnapshot")
data class MeshInventoryCategorySnapshot(
    val category: MeshInventoryCategory,
) : MeshInventoryEventPayload

@Serializable
@SerialName("LocationSnapshot")
data class MeshInventoryLocationSnapshot(
    val location: MeshInventoryLocation,
) : MeshInventoryEventPayload

@Serializable
@SerialName("ItemSnapshot")
data class MeshInventoryItemSnapshot(
    val item: MeshInventoryItem,
) : MeshInventoryEventPayload

@Serializable
@SerialName("SessionSnapshot")
data class MeshInventorySessionSnapshot(
    val session: MeshInventorySession,
) : MeshInventoryEventPayload

@Serializable
@SerialName("ReviewSnapshot")
data class MeshInventoryReviewSnapshot(
    val review: MeshInventoryReview,
) : MeshInventoryEventPayload

@Serializable
@SerialName("CommentSnapshot")
data class MeshInventoryCommentSnapshot(
    val comment: MeshInventoryComment,
) : MeshInventoryEventPayload

@Serializable
@SerialName("AttachmentSnapshot")
data class MeshInventoryAttachmentSnapshot(
    val attachment: MeshInventoryAttachment,
) : MeshInventoryEventPayload

@Serializable
@SerialName("ExportSnapshot")
data class MeshInventoryExportSnapshot(
    val exportTask: MeshInventoryExportTask,
) : MeshInventoryEventPayload

@Serializable
@SerialName("QrSnapshot")
data class MeshInventoryQrSnapshot(
    val qrCode: MeshInventoryQrCode,
) : MeshInventoryEventPayload

@Serializable
data class MeshInventoryEvent(
    val eventId: String,
    val organizationId: String,
    val entityType: MeshInventoryEntityType,
    val entityId: String,
    val eventType: MeshInventoryEventType,
    val actorPeerId: String,
    val occurredAt: Instant,
    val sequence: Long,
    val entityRevision: Long? = null,
    val previousEntityRevision: Long? = null,
    val sessionId: String? = null,
    val payload: MeshInventoryEventPayload,
    val conflict: Boolean = false,
    val notes: String? = null,
)

@Serializable
data class MeshInventorySyncResult(
    val requestId: String,
    val organizationId: String,
    val peerId: String,
    val eventsReceived: Int,
    val lastSequence: Long,
    val timedOut: Boolean = false,
)
