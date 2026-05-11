package org.expert.link.mesh.application.service

import kotlinx.datetime.Instant
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryFieldChange
import org.expert.link.mesh.domain.model.inventory.InventoryItemSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventorySession
import org.expert.link.mesh.domain.model.inventory.InventorySessionMember
import org.expert.link.mesh.domain.model.inventory.InventorySessionReviewStatus
import org.expert.link.mesh.domain.model.inventory.InventorySessionResult
import org.expert.link.mesh.domain.model.inventory.InventorySessionRole
import org.expert.link.mesh.domain.model.inventory.InventorySessionSnapshot
import org.expert.link.mesh.domain.model.inventory.InventorySessionStatus
import org.expert.link.mesh.domain.model.inventory.InventoryWorkflowStatus
import org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySessionMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySessionRepositoryPort

data class InventorySessionUpdate(
    val title: String? = null,
    val description: String? = null,
    val periodStart: Instant? = null,
    val periodEnd: Instant? = null,
    val status: InventorySessionStatus? = null,
    val reviewStatus: InventorySessionReviewStatus? = null,
    val workflowStatus: InventoryWorkflowStatus? = null,
    val result: InventorySessionResult? = null,
    val departmentIds: Set<String>? = null,
    val locationIds: Set<String>? = null,
    val ownerIds: Set<String>? = null,
    val requiresPhotoForDiscrepancy: Boolean? = null,
    val completionBlockedReason: String? = null,
    val chatId: String? = null,
    val threadRootMessageId: String? = null,
    val metadata: Map<String, String>? = null,
)

class InventorySessionService(
    private val localProfileService: LocalProfileService,
    private val sessionRepositoryPort: InventorySessionRepositoryPort,
    private val sessionMemberRepositoryPort: InventorySessionMemberRepositoryPort,
    private val itemRepositoryPort: InventoryItemRepositoryPort,
    private val rbacService: InventoryRbacService,
    private val inventoryEventService: InventoryEventService,
    private val inventoryChangeLogService: InventoryChangeLogService,
    private val inventorySyncService: InventorySyncService,
) {
    suspend fun createSession(
        organizationId: String,
        title: String,
        description: String?,
        periodStart: Instant,
        periodEnd: Instant?,
        departmentIds: Set<String> = emptySet(),
        locationIds: Set<String> = emptySet(),
        ownerIds: Set<String> = emptySet(),
        workflowStatus: InventoryWorkflowStatus = InventoryWorkflowStatus.CREATED,
        requiresPhotoForDiscrepancy: Boolean = true,
        itemIds: Set<String>,
        memberPeerIds: Set<String>,
    ): InventorySession {
        require(title.isNotBlank()) { "Session title is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.SESSION_CREATE)
        val now = now()
        val session = InventorySession(
            sessionId = newId("session"),
            organizationId = organizationId,
            title = title,
            description = description,
            periodStart = periodStart,
            periodEnd = periodEnd,
            status = InventorySessionStatus.DRAFT,
            reviewStatus = InventorySessionReviewStatus.PENDING,
            workflowStatus = workflowStatus,
            departmentIds = departmentIds,
            locationIds = locationIds,
            ownerIds = ownerIds,
            requiresPhotoForDiscrepancy = requiresPhotoForDiscrepancy,
            createdByPeerId = localProfile.peerId,
            createdAt = now,
            updatedAt = now,
            itemIds = itemIds,
            memberPeerIds = memberPeerIds,
            revision = 1,
        )
        sessionRepositoryPort.save(session)
        memberPeerIds.forEach { peerId ->
            sessionMemberRepositoryPort.save(
                InventorySessionMember(
                    sessionId = session.sessionId,
                    organizationId = organizationId,
                    peerId = peerId,
                    role = InventorySessionRole.MEMBER,
                    assignedByPeerId = localProfile.peerId,
                    assignedAt = now,
                    updatedAt = now,
                ),
            )
        }
        itemIds.forEach { linkItemToSession(it, session, localProfile.peerId) }
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.SESSION,
                entityId = session.sessionId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventorySessionSnapshot(session),
                entityRevision = session.revision,
            ),
        )
        return session
    }

    suspend fun updateSession(sessionId: String, expectedRevision: Long?, update: InventorySessionUpdate): InventorySession? {
        val current = sessionRepositoryPort.findBySessionId(sessionId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.SESSION_UPDATE)
        if (expectedRevision != null && expectedRevision != current.revision) {
            return markConflict(current, localProfile.peerId, "Revision mismatch: expected=$expectedRevision actual=${current.revision}")
        }
        val targetStatus = update.status ?: current.status
        if (update.status != null) {
            requireSessionTransition(current.status, targetStatus)
        }
        val updated = current.copy(
            title = update.title ?: current.title,
            description = update.description ?: current.description,
            periodStart = update.periodStart ?: current.periodStart,
            periodEnd = update.periodEnd ?: current.periodEnd,
            status = targetStatus,
            reviewStatus = update.reviewStatus ?: current.reviewStatus,
            workflowStatus = update.workflowStatus ?: current.workflowStatus,
            result = update.result ?: current.result,
            departmentIds = update.departmentIds ?: current.departmentIds,
            locationIds = update.locationIds ?: current.locationIds,
            ownerIds = update.ownerIds ?: current.ownerIds,
            requiresPhotoForDiscrepancy = update.requiresPhotoForDiscrepancy ?: current.requiresPhotoForDiscrepancy,
            completionBlockedReason = update.completionBlockedReason ?: current.completionBlockedReason,
            chatId = update.chatId ?: current.chatId,
            threadRootMessageId = update.threadRootMessageId ?: current.threadRootMessageId,
            metadata = update.metadata ?: current.metadata,
            updatedAt = now(),
            revision = current.revision + 1,
        )
        sessionRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.SESSION,
                entityId = updated.sessionId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventorySessionSnapshot(updated),
                entityRevision = updated.revision,
                previousEntityRevision = current.revision,
            ),
        )
        val changes = buildFieldChanges(current, update)
        if (changes.isNotEmpty()) {
            inventoryChangeLogService.recordChange(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.SESSION,
                entityId = updated.sessionId,
                changes = changes,
                actorPeerId = localProfile.peerId,
                reason = "update",
                sessionId = updated.sessionId,
            )
        }
        return updated
    }

    suspend fun addItemsToSession(sessionId: String, itemIds: Set<String>): InventorySession? {
        val current = sessionRepositoryPort.findBySessionId(sessionId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.SESSION_ADD_ITEM)
        val updated = current.copy(
            itemIds = (current.itemIds + itemIds).toSet(),
            updatedAt = now(),
            revision = current.revision + 1,
        )
        sessionRepositoryPort.save(updated)
        itemIds.forEach { linkItemToSession(it, updated, localProfile.peerId) }
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.SESSION,
                entityId = updated.sessionId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventorySessionSnapshot(updated),
                entityRevision = updated.revision,
                previousEntityRevision = current.revision,
            ),
        )
        return updated
    }

    suspend fun addMember(
        sessionId: String,
        peerId: String,
        role: InventorySessionRole,
    ): InventorySessionMember {
        val session = sessionRepositoryPort.findBySessionId(sessionId)
            ?: error("Session $sessionId not found")
        val localProfile = localProfileService.require()
        rbacService.requirePermission(session.organizationId, localProfile.peerId, InventoryPermission.SESSION_ADD_MEMBER)
        val now = now()
        val member = InventorySessionMember(
            sessionId = sessionId,
            organizationId = session.organizationId,
            peerId = peerId,
            role = role,
            assignedByPeerId = localProfile.peerId,
            assignedAt = now,
            updatedAt = now,
        )
        sessionMemberRepositoryPort.save(member)
        val updatedSession = session.copy(
            memberPeerIds = (session.memberPeerIds + peerId).toSet(),
            updatedAt = now,
            revision = session.revision + 1,
        )
        sessionRepositoryPort.save(updatedSession)
        publish(
            inventoryEventService.recordEvent(
                organizationId = session.organizationId,
                entityType = InventoryEntityType.SESSION,
                entityId = session.sessionId,
                eventType = InventoryEventType.MEMBER_ADDED,
                actorPeerId = localProfile.peerId,
                payload = InventorySessionSnapshot(updatedSession),
                entityRevision = updatedSession.revision,
                previousEntityRevision = session.revision,
            ),
        )
        return member
    }

    suspend fun listMembers(sessionId: String): List<InventorySessionMember> =
        sessionMemberRepositoryPort.listBySession(sessionId)

    suspend fun closeSession(sessionId: String, note: String?): InventorySession? {
        val current = sessionRepositoryPort.findBySessionId(sessionId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.SESSION_CLOSE)
        val updated = current.copy(
            status = InventorySessionStatus.CLOSED,
            reviewStatus = if (current.workflowStatus == InventoryWorkflowStatus.PASSED) {
                InventorySessionReviewStatus.COMPLETED
            } else {
                current.reviewStatus
            },
            workflowStatus = if (current.workflowStatus == InventoryWorkflowStatus.PASSED) {
                InventoryWorkflowStatus.COMPLETED
            } else {
                current.workflowStatus
            },
            updatedAt = now(),
            revision = current.revision + 1,
        )
        sessionRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.SESSION,
                entityId = updated.sessionId,
                eventType = InventoryEventType.SESSION_CLOSED,
                actorPeerId = localProfile.peerId,
                payload = InventorySessionSnapshot(updated),
                entityRevision = updated.revision,
                previousEntityRevision = current.revision,
                notes = note,
            ),
        )
        inventoryChangeLogService.recordChange(
            organizationId = updated.organizationId,
            entityType = InventoryEntityType.SESSION,
            entityId = updated.sessionId,
            changes = listOf(
                InventoryFieldChange(
                    field = "status",
                    previousValue = current.status.name,
                    newValue = updated.status.name,
                ),
                InventoryFieldChange(
                    field = "reviewStatus",
                    previousValue = current.reviewStatus.name,
                    newValue = updated.reviewStatus.name,
                ),
            ),
            actorPeerId = localProfile.peerId,
            reason = note ?: "close",
            sessionId = updated.sessionId,
        )
        return updated
    }

    private fun buildFieldChanges(current: InventorySession, update: InventorySessionUpdate): List<InventoryFieldChange> {
        val changes = mutableListOf<InventoryFieldChange>()
        fun add(field: String, previous: Any?, next: Any?) {
            if (previous != next) {
                changes += InventoryFieldChange(field, previous?.toString(), next?.toString())
            }
        }
        update.title?.let { add("title", current.title, it) }
        update.description?.let { add("description", current.description, it) }
        update.periodStart?.let { add("periodStart", current.periodStart, it) }
        update.periodEnd?.let { add("periodEnd", current.periodEnd, it) }
        update.status?.let { add("status", current.status, it) }
        update.reviewStatus?.let { add("reviewStatus", current.reviewStatus, it) }
        update.workflowStatus?.let { add("workflowStatus", current.workflowStatus, it) }
        update.result?.let { add("result", current.result, it) }
        update.departmentIds?.let { add("departmentIds", current.departmentIds, it) }
        update.locationIds?.let { add("locationIds", current.locationIds, it) }
        update.ownerIds?.let { add("ownerIds", current.ownerIds, it) }
        update.requiresPhotoForDiscrepancy?.let { add("requiresPhotoForDiscrepancy", current.requiresPhotoForDiscrepancy, it) }
        update.completionBlockedReason?.let { add("completionBlockedReason", current.completionBlockedReason, it) }
        update.chatId?.let { add("chatId", current.chatId, it) }
        update.threadRootMessageId?.let { add("threadRootMessageId", current.threadRootMessageId, it) }
        update.metadata?.let { add("metadata", current.metadata, it) }
        return changes
    }

    private suspend fun linkItemToSession(itemId: String, session: InventorySession, actorPeerId: String) {
        val item = itemRepositoryPort.findByInventoryItemId(itemId) ?: return
        if (session.sessionId in item.sessionIds) {
            return
        }
        val updatedItem = item.copy(
            sessionIds = item.sessionIds + session.sessionId,
            updatedAt = now(),
            revision = item.revision + 1,
        )
        itemRepositoryPort.save(updatedItem)
        publish(
            inventoryEventService.recordEvent(
                organizationId = session.organizationId,
                entityType = InventoryEntityType.ITEM,
                entityId = updatedItem.inventoryItemId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = actorPeerId,
                payload = InventoryItemSnapshot(updatedItem),
                entityRevision = updatedItem.revision,
                previousEntityRevision = item.revision,
                sessionId = session.sessionId,
            ),
        )
    }

    private suspend fun markConflict(session: InventorySession, actorPeerId: String, note: String): InventorySession {
        val updated = session.copy(
            reviewStatus = InventorySessionReviewStatus.REJECTED,
            updatedAt = now(),
            revision = session.revision + 1,
        )
        sessionRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = session.organizationId,
                entityType = InventoryEntityType.SESSION,
                entityId = session.sessionId,
                eventType = InventoryEventType.CONFLICT_DETECTED,
                actorPeerId = actorPeerId,
                payload = InventorySessionSnapshot(updated),
                entityRevision = updated.revision,
                previousEntityRevision = session.revision,
                conflict = true,
                notes = note,
            ),
        )
        return updated
    }

    private fun requireSessionTransition(current: InventorySessionStatus, target: InventorySessionStatus) {
        val allowed = SESSION_TRANSITIONS[current].orEmpty()
        require(target in allowed) { "Invalid session transition: $current -> $target" }
    }

    private suspend fun publish(event: org.expert.link.mesh.domain.model.inventory.InventoryEvent) {
        inventorySyncService.broadcastEvent(event)
    }

    private companion object {
        val SESSION_TRANSITIONS: Map<InventorySessionStatus, Set<InventorySessionStatus>> = mapOf(
            InventorySessionStatus.DRAFT to setOf(InventorySessionStatus.ACTIVE, InventorySessionStatus.ARCHIVED),
            InventorySessionStatus.ACTIVE to setOf(InventorySessionStatus.UNDER_REVIEW, InventorySessionStatus.CLOSED),
            InventorySessionStatus.UNDER_REVIEW to setOf(InventorySessionStatus.CLOSED),
            InventorySessionStatus.CLOSED to setOf(InventorySessionStatus.ARCHIVED),
            InventorySessionStatus.ARCHIVED to emptySet(),
        )
    }
}
