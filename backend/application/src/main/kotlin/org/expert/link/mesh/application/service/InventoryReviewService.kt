package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryItemSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventoryAcceptanceStatus
import org.expert.link.mesh.domain.model.inventory.InventoryConfirmationStatus
import org.expert.link.mesh.domain.model.inventory.InventoryIncidentStatus
import org.expert.link.mesh.domain.model.inventory.InventoryIncidentType
import org.expert.link.mesh.domain.model.inventory.InventoryPresenceStatus
import org.expert.link.mesh.domain.model.inventory.InventoryReview
import org.expert.link.mesh.domain.model.inventory.InventoryReviewSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryReviewStatus
import org.expert.link.mesh.domain.model.inventory.InventorySessionReviewStatus
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.domain.model.inventory.InventoryWorkflowStatus
import org.expert.link.mesh.domain.model.inventory.InventoryAttachmentType
import org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryAttachmentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryIncidentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryReviewRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySessionRepositoryPort

class InventoryReviewService(
    private val localProfileService: LocalProfileService,
    private val itemRepositoryPort: InventoryItemRepositoryPort,
    private val sessionRepositoryPort: InventorySessionRepositoryPort,
    private val reviewRepositoryPort: InventoryReviewRepositoryPort,
    private val attachmentRepositoryPort: InventoryAttachmentRepositoryPort,
    private val incidentRepositoryPort: InventoryIncidentRepositoryPort,
    private val rbacService: InventoryRbacService,
    private val inventoryEventService: InventoryEventService,
    private val inventorySyncService: InventorySyncService,
) {
    suspend fun submitReview(
        itemId: String,
        sessionId: String?,
        status: InventoryReviewStatus,
        presenceStatus: InventoryPresenceStatus = InventoryPresenceStatus.UNCHECKED,
        acceptanceStatus: InventoryAcceptanceStatus = InventoryAcceptanceStatus.UNCHECKED,
        confirmationStatus: InventoryConfirmationStatus = InventoryConfirmationStatus.UNCHECKED,
        requiresPhoto: Boolean = false,
        comment: String?,
    ): InventoryReview {
        val item = itemRepositoryPort.findByInventoryItemId(itemId)
            ?: error("Inventory item $itemId not found")
        val localProfile = localProfileService.require()
        val permission = when (status) {
            InventoryReviewStatus.APPROVED -> InventoryPermission.ITEM_CONFIRM
            InventoryReviewStatus.REJECTED -> InventoryPermission.ITEM_REJECT
            InventoryReviewStatus.REQUIRES_UPDATE -> InventoryPermission.ITEM_REJECT
        }
        rbacService.requirePermission(item.organizationId, localProfile.peerId, permission)
        val now = now()
        val review = InventoryReview(
            reviewId = newId("review"),
            organizationId = item.organizationId,
            inventoryItemId = itemId,
            sessionId = sessionId,
            reviewerPeerId = localProfile.peerId,
            status = status,
            presenceStatus = presenceStatus,
            acceptanceStatus = acceptanceStatus,
            confirmationStatus = confirmationStatus,
            requiresPhoto = requiresPhoto || presenceStatus == InventoryPresenceStatus.ABSENT ||
                acceptanceStatus == InventoryAcceptanceStatus.NOT_ACCEPTED ||
                confirmationStatus == InventoryConfirmationStatus.NOT_CONFIRMED ||
                status != InventoryReviewStatus.APPROVED,
            comment = comment,
            createdAt = now,
            updatedAt = now,
        )
        reviewRepositoryPort.save(review)
        val updatedItem = item.copy(
            currentStatus = when (status) {
                InventoryReviewStatus.APPROVED -> InventoryStatus.CONFIRMED
                InventoryReviewStatus.REJECTED -> InventoryStatus.REJECTED
                InventoryReviewStatus.REQUIRES_UPDATE -> InventoryStatus.REQUIRES_UPDATE
            },
            updatedAt = now,
            revision = item.revision + 1,
        )
        itemRepositoryPort.save(updatedItem)
        if (sessionId != null) {
            recalculateSessionState(sessionId, localProfile.peerId, now)
        }
        publish(
            inventoryEventService.recordEvent(
                organizationId = item.organizationId,
                entityType = InventoryEntityType.REVIEW,
                entityId = review.reviewId,
                eventType = InventoryEventType.REVIEW_SUBMITTED,
                actorPeerId = localProfile.peerId,
                payload = InventoryReviewSnapshot(review),
                sessionId = sessionId,
            ),
        )
        publish(
            inventoryEventService.recordEvent(
                organizationId = item.organizationId,
                entityType = InventoryEntityType.ITEM,
                entityId = item.inventoryItemId,
                eventType = InventoryEventType.STATUS_CHANGED,
                actorPeerId = localProfile.peerId,
                payload = InventoryItemSnapshot(updatedItem),
                entityRevision = updatedItem.revision,
                previousEntityRevision = item.revision,
                sessionId = sessionId,
            ),
        )
        return review
    }

    suspend fun listReviews(itemId: String, sessionId: String?): List<InventoryReview> {
        return if (sessionId == null) {
            reviewRepositoryPort.listByItem(itemId)
        } else {
            reviewRepositoryPort.listBySession(sessionId).filter { it.inventoryItemId == itemId }
        }
    }

    private suspend fun recalculateSessionState(sessionId: String, actorPeerId: String, timestamp: kotlinx.datetime.Instant) {
        val session = sessionRepositoryPort.findBySessionId(sessionId) ?: return
        val sessionReviews = reviewRepositoryPort.listBySession(sessionId)
            .groupBy { it.inventoryItemId }
            .mapValues { (_, reviews) -> reviews.maxByOrNull { it.updatedAt }!! }
        val photoEvidence = attachmentRepositoryPort.listBySession(sessionId)
            .filter { it.attachmentType == InventoryAttachmentType.PHOTO }
            .groupBy { it.inventoryItemId }
        val sessionIncidents = incidentRepositoryPort.listBySession(sessionId)
        val openCorrectionRequest = sessionIncidents.any {
            it.type == InventoryIncidentType.CORRECTION_REQUEST &&
                it.status !in setOf(InventoryIncidentStatus.RESOLVED, InventoryIncidentStatus.DISMISSED)
        }
        val allReviewed = session.itemIds.isNotEmpty() && session.itemIds.all { it in sessionReviews }
        val photoMissing = sessionReviews.any { (itemId, review) ->
            review.requiresPhoto &&
                (photoEvidence[itemId].orEmpty().none())
        }
        val hasRejected = sessionReviews.values.any { review ->
            review.status == InventoryReviewStatus.REJECTED ||
                review.presenceStatus == InventoryPresenceStatus.ABSENT ||
                review.acceptanceStatus == InventoryAcceptanceStatus.NOT_ACCEPTED
        }
        val hasRequiresUpdate = photoMissing || sessionReviews.values.any { review ->
            review.status == InventoryReviewStatus.REQUIRES_UPDATE ||
                review.confirmationStatus == InventoryConfirmationStatus.NOT_CONFIRMED
        }
        val workflowStatus = when {
            openCorrectionRequest -> InventoryWorkflowStatus.SENT_TO_COMMISSION
            sessionReviews.isEmpty() -> InventoryWorkflowStatus.CREATED
            !allReviewed -> InventoryWorkflowStatus.IN_PROGRESS
            hasRequiresUpdate -> InventoryWorkflowStatus.REQUIRES_CORRECTION
            hasRejected -> InventoryWorkflowStatus.FAILED
            session.status == org.expert.link.mesh.domain.model.inventory.InventorySessionStatus.CLOSED ->
                InventoryWorkflowStatus.COMPLETED
            else -> InventoryWorkflowStatus.PASSED
        }
        val reviewStatus = when (workflowStatus) {
            InventoryWorkflowStatus.CREATED -> InventorySessionReviewStatus.PENDING
            InventoryWorkflowStatus.IN_PROGRESS -> InventorySessionReviewStatus.IN_PROGRESS
            InventoryWorkflowStatus.PASSED,
            InventoryWorkflowStatus.COMPLETED,
            -> InventorySessionReviewStatus.COMPLETED
            InventoryWorkflowStatus.FAILED,
            InventoryWorkflowStatus.REQUIRES_CORRECTION,
            InventoryWorkflowStatus.SENT_TO_COMMISSION,
            -> InventorySessionReviewStatus.REJECTED
        }
        val confirmedCount = sessionReviews.values.count { review ->
            review.status == InventoryReviewStatus.APPROVED &&
                review.presenceStatus == InventoryPresenceStatus.PRESENT &&
                review.acceptanceStatus == InventoryAcceptanceStatus.ACCEPTED &&
                review.confirmationStatus == InventoryConfirmationStatus.CONFIRMED
        }
        val rejectedCount = sessionReviews.values.count { review ->
            review.status == InventoryReviewStatus.REJECTED ||
                review.presenceStatus == InventoryPresenceStatus.ABSENT ||
                review.acceptanceStatus == InventoryAcceptanceStatus.NOT_ACCEPTED
        }
        val requiresUpdateCount = sessionReviews.values.count { review ->
            review.status == InventoryReviewStatus.REQUIRES_UPDATE ||
                review.confirmationStatus == InventoryConfirmationStatus.NOT_CONFIRMED
        } + if (photoMissing) 1 else 0
        val updated = session.copy(
            reviewStatus = reviewStatus,
            workflowStatus = workflowStatus,
            completionBlockedReason = if (photoMissing) {
                "Не хватает обязательных фотографий для проблемных объектов"
            } else {
                null
            },
            result = org.expert.link.mesh.domain.model.inventory.InventorySessionResult(
                summary = "Подтверждено $confirmedCount из ${session.itemIds.size}",
                confirmedCount = confirmedCount,
                rejectedCount = rejectedCount,
                requiresUpdateCount = requiresUpdateCount,
                incidentCount = sessionIncidents.size,
                completedAt = if (workflowStatus in setOf(InventoryWorkflowStatus.PASSED, InventoryWorkflowStatus.COMPLETED)) timestamp else null,
                approvedByPeerId = if (workflowStatus in setOf(InventoryWorkflowStatus.PASSED, InventoryWorkflowStatus.COMPLETED)) actorPeerId else null,
            ),
            updatedAt = timestamp,
            revision = session.revision + 1,
        )
        sessionRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.SESSION,
                entityId = updated.sessionId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = actorPeerId,
                payload = org.expert.link.mesh.domain.model.inventory.InventorySessionSnapshot(updated),
                entityRevision = updated.revision,
                previousEntityRevision = session.revision,
            ),
        )
    }

    private suspend fun publish(event: org.expert.link.mesh.domain.model.inventory.InventoryEvent) {
        inventorySyncService.broadcastEvent(event)
    }
}
