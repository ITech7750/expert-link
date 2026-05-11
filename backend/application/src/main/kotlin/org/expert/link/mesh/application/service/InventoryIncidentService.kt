package org.expert.link.mesh.application.service

import kotlinx.datetime.Instant
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.inventory.InventoryAlertEvent
import org.expert.link.mesh.domain.model.inventory.InventoryDeadlineRule
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryIncident
import org.expert.link.mesh.domain.model.inventory.InventoryIncidentSeverity
import org.expert.link.mesh.domain.model.inventory.InventoryIncidentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryIncidentStatus
import org.expert.link.mesh.domain.model.inventory.InventoryIncidentType
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventoryReminder
import org.expert.link.mesh.domain.model.inventory.InventoryReminderSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryReminderStatus
import org.expert.link.mesh.domain.model.inventory.InventoryRuleThreshold
import org.expert.link.mesh.domain.model.inventory.InventoryRuleType
import org.expert.link.mesh.domain.model.inventory.InventoryRuleThresholdSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryDeadlineRuleSnapshot
import org.expert.link.mesh.domain.model.inventory.InventorySessionStatus
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.domain.model.inventory.InventoryWorkflowStatus
import org.expert.link.mesh.domain.port.repository.InventoryAlertRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryDeadlineRuleRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryIncidentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryReminderRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryThresholdRuleRepositoryPort

class InventoryIncidentService(
    private val localProfileService: LocalProfileService,
    private val incidentRepositoryPort: InventoryIncidentRepositoryPort,
    private val alertRepositoryPort: InventoryAlertRepositoryPort,
    private val reminderRepositoryPort: InventoryReminderRepositoryPort,
    private val thresholdRuleRepositoryPort: InventoryThresholdRuleRepositoryPort,
    private val deadlineRuleRepositoryPort: InventoryDeadlineRuleRepositoryPort,
    private val itemRepositoryPort: InventoryItemRepositoryPort,
    private val sessionRepositoryPort: InventorySessionRepositoryPort,
    private val rbacService: InventoryRbacService,
    private val inventoryEventService: InventoryEventService,
    private val inventorySyncService: InventorySyncService,
) {
    suspend fun listIncidents(
        organizationId: String,
        inventoryItemId: String? = null,
        sessionId: String? = null,
    ): List<InventoryIncident> {
        return when {
            inventoryItemId != null -> incidentRepositoryPort.listByItem(inventoryItemId)
            sessionId != null -> incidentRepositoryPort.listBySession(sessionId)
            else -> incidentRepositoryPort.listByOrganization(organizationId)
        }
    }

    suspend fun reportIncident(
        organizationId: String,
        inventoryItemId: String?,
        sessionId: String?,
        locationId: String?,
        type: InventoryIncidentType,
        severity: InventoryIncidentSeverity,
        title: String,
        description: String?,
        assigneePeerIds: Set<String> = emptySet(),
        attachmentIds: List<String>,
        comment: String?,
    ): InventoryIncident {
        require(title.isNotBlank()) { "Incident title is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.INCIDENT_CREATE)
        val now = now()
        val incident = InventoryIncident(
            incidentId = newId("incident"),
            organizationId = organizationId,
            inventoryItemId = inventoryItemId,
            sessionId = sessionId,
            locationId = locationId,
            type = type,
            severity = severity,
            status = InventoryIncidentStatus.OPEN,
            title = title,
            description = description,
            assigneePeerIds = assigneePeerIds,
            reportedByPeerId = localProfile.peerId,
            reportedAt = now,
            updatedAt = now,
            attachmentIds = attachmentIds,
            commentIds = emptyList(),
            metadata = comment?.let { mapOf("comment" to it) } ?: emptyMap(),
        )
        incidentRepositoryPort.save(incident)
        inventoryItemId?.let { linkIncidentToItem(it, incident.incidentId, localProfile.peerId) }
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.INCIDENT,
                entityId = incident.incidentId,
                eventType = InventoryEventType.INCIDENT_REPORTED,
                actorPeerId = localProfile.peerId,
                payload = InventoryIncidentSnapshot(incident),
                sessionId = sessionId,
            ),
        )
        if (type == InventoryIncidentType.CORRECTION_REQUEST && sessionId != null) {
            val session = sessionRepositoryPort.findBySessionId(sessionId)
            if (session != null) {
                sessionRepositoryPort.save(
                    session.copy(
                        workflowStatus = InventoryWorkflowStatus.SENT_TO_COMMISSION,
                        completionBlockedReason = "Создана заявка на исправление для комиссии",
                        updatedAt = now,
                        revision = session.revision + 1,
                    ),
                )
            }
        }
        return incident
    }

    suspend fun updateIncidentStatus(
        incidentId: String,
        status: InventoryIncidentStatus,
        reviewComment: String?,
    ): InventoryIncident? {
        val current = incidentRepositoryPort.findByIncidentId(incidentId) ?: return null
        val localProfile = localProfileService.require()
        val permission = if (status == InventoryIncidentStatus.RESOLVED) {
            InventoryPermission.INCIDENT_RESOLVE
        } else {
            InventoryPermission.INCIDENT_REVIEW
        }
        rbacService.requirePermission(current.organizationId, localProfile.peerId, permission)
        val now = now()
        val updated = current.copy(
            status = status,
            reviewComment = reviewComment ?: current.reviewComment,
            reviewedByPeerId = localProfile.peerId,
            reviewedAt = now,
            resolvedByPeerId = if (status == InventoryIncidentStatus.RESOLVED) localProfile.peerId else current.resolvedByPeerId,
            resolvedAt = if (status == InventoryIncidentStatus.RESOLVED) now else current.resolvedAt,
            updatedAt = now,
        )
        incidentRepositoryPort.save(updated)
        val eventType = if (status == InventoryIncidentStatus.RESOLVED) {
            InventoryEventType.INCIDENT_RESOLVED
        } else {
            InventoryEventType.UPDATED
        }
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.INCIDENT,
                entityId = updated.incidentId,
                eventType = eventType,
                actorPeerId = localProfile.peerId,
                payload = InventoryIncidentSnapshot(updated),
                sessionId = updated.sessionId,
                notes = reviewComment,
            ),
        )
        return updated
    }

    suspend fun listAlerts(organizationId: String): List<InventoryAlertEvent> =
        alertRepositoryPort.listByOrganization(organizationId)

    suspend fun listReminders(organizationId: String, itemId: String? = null): List<InventoryReminder> {
        generateReminders(organizationId)
        val reminders = reminderRepositoryPort.listByOrganization(organizationId)
        return if (itemId == null) reminders else reminders.filter { it.inventoryItemId == itemId }
    }

    suspend fun listRuleThresholds(organizationId: String): List<InventoryRuleThreshold> =
        thresholdRuleRepositoryPort.listByOrganization(organizationId)

    suspend fun createRuleThreshold(
        organizationId: String,
        ruleType: InventoryRuleType,
        thresholdValue: Long?,
        active: Boolean,
        metadata: Map<String, String>,
    ): InventoryRuleThreshold {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.REMINDER_MANAGE)
        val now = now()
        val rule = InventoryRuleThreshold(
            ruleId = newId("rule"),
            organizationId = organizationId,
            ruleType = ruleType,
            thresholdValue = thresholdValue,
            active = active,
            createdByPeerId = localProfile.peerId,
            createdAt = now,
            updatedAt = now,
            metadata = metadata,
        )
        thresholdRuleRepositoryPort.save(rule)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.RULE_THRESHOLD,
                entityId = rule.ruleId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryRuleThresholdSnapshot(rule),
            ),
        )
        return rule
    }

    suspend fun updateRuleThreshold(
        ruleId: String,
        ruleType: InventoryRuleType?,
        thresholdValue: Long?,
        active: Boolean?,
        metadata: Map<String, String>?,
    ): InventoryRuleThreshold? {
        val current = thresholdRuleRepositoryPort.findByRuleId(ruleId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.REMINDER_MANAGE)
        val updated = current.copy(
            ruleType = ruleType ?: current.ruleType,
            thresholdValue = thresholdValue ?: current.thresholdValue,
            active = active ?: current.active,
            metadata = metadata ?: current.metadata,
            updatedAt = now(),
        )
        thresholdRuleRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.RULE_THRESHOLD,
                entityId = updated.ruleId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryRuleThresholdSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun listDeadlineRules(organizationId: String): List<InventoryDeadlineRule> =
        deadlineRuleRepositoryPort.listByOrganization(organizationId)

    suspend fun createDeadlineRule(
        organizationId: String,
        target: org.expert.link.mesh.domain.model.inventory.InventoryDeadlineTarget,
        daysBefore: Int,
        active: Boolean,
    ): InventoryDeadlineRule {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.REMINDER_MANAGE)
        val now = now()
        val rule = InventoryDeadlineRule(
            ruleId = newId("deadline"),
            organizationId = organizationId,
            target = target,
            daysBefore = daysBefore,
            active = active,
            createdByPeerId = localProfile.peerId,
            createdAt = now,
            updatedAt = now,
        )
        deadlineRuleRepositoryPort.save(rule)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.DEADLINE_RULE,
                entityId = rule.ruleId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryDeadlineRuleSnapshot(rule),
            ),
        )
        return rule
    }

    suspend fun updateDeadlineRule(
        ruleId: String,
        target: org.expert.link.mesh.domain.model.inventory.InventoryDeadlineTarget?,
        daysBefore: Int?,
        active: Boolean?,
    ): InventoryDeadlineRule? {
        val current = deadlineRuleRepositoryPort.findByRuleId(ruleId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.REMINDER_MANAGE)
        val updated = current.copy(
            target = target ?: current.target,
            daysBefore = daysBefore ?: current.daysBefore,
            active = active ?: current.active,
            updatedAt = now(),
        )
        deadlineRuleRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.DEADLINE_RULE,
                entityId = updated.ruleId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryDeadlineRuleSnapshot(updated),
            ),
        )
        return updated
    }

    private suspend fun linkIncidentToItem(itemId: String, incidentId: String, actorPeerId: String) {
        val item = itemRepositoryPort.findByInventoryItemId(itemId) ?: return
        if (incidentId in item.incidentIds) {
            return
        }
        val updatedItem = item.copy(
            incidentIds = (item.incidentIds + incidentId).distinct(),
            updatedAt = now(),
            revision = item.revision + 1,
        )
        itemRepositoryPort.save(updatedItem)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updatedItem.organizationId,
                entityType = InventoryEntityType.ITEM,
                entityId = updatedItem.inventoryItemId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = actorPeerId,
                payload = org.expert.link.mesh.domain.model.inventory.InventoryItemSnapshot(updatedItem),
                entityRevision = updatedItem.revision,
                previousEntityRevision = item.revision,
            ),
        )
    }

    private suspend fun generateReminders(organizationId: String) {
        val rules = thresholdRuleRepositoryPort.listByOrganization(organizationId).filter { it.active }
        if (rules.isEmpty()) {
            return
        }
        val now = now()
        val items = itemRepositoryPort.listByOrganization(organizationId)
        val sessions = sessionRepositoryPort.listByOrganization(organizationId)
        val incidents = incidentRepositoryPort.listByOrganization(organizationId)
        val existing = reminderRepositoryPort.listByOrganization(organizationId)

        fun exists(ruleId: String, itemId: String?, sessionId: String?): Boolean {
            return existing.any {
                it.ruleId == ruleId && it.inventoryItemId == itemId && it.sessionId == sessionId &&
                    it.status != InventoryReminderStatus.DISMISSED
            }
        }

        suspend fun createReminder(ruleId: String, itemId: String?, sessionId: String?, message: String) {
            if (exists(ruleId, itemId, sessionId)) {
                return
            }
            val reminder = InventoryReminder(
                reminderId = newId("reminder"),
                organizationId = organizationId,
                ruleId = ruleId,
                inventoryItemId = itemId,
                sessionId = sessionId,
                dueAt = now,
                status = InventoryReminderStatus.PENDING,
                message = message,
                recipientPeerIds = emptySet(),
                createdAt = now,
                updatedAt = now,
            )
            reminderRepositoryPort.save(reminder)
            publish(
                inventoryEventService.recordEvent(
                    organizationId = organizationId,
                    entityType = InventoryEntityType.REMINDER,
                    entityId = reminder.reminderId,
                    eventType = InventoryEventType.REMINDER_CREATED,
                    actorPeerId = localProfileService.require().peerId,
                    payload = InventoryReminderSnapshot(reminder),
                ),
            )
        }

        for (rule in rules) {
            val days = rule.thresholdValue ?: continue
            val thresholdSeconds = days * 86_400
            val deadline = Instant.fromEpochSeconds(now.epochSeconds + thresholdSeconds)
            when (rule.ruleType) {
                InventoryRuleType.NEXT_INVENTORY_DUE_DAYS -> {
                    items.filter { item ->
                        item.nextInventoryAt?.let { it <= deadline } == true
                    }
                        .forEach { item ->
                            createReminder(rule.ruleId, item.inventoryItemId, null, "Inventory due soon")
                        }
                }
                InventoryRuleType.REVIEW_STALE_DAYS -> {
                    val stale = Instant.fromEpochSeconds(now.epochSeconds - thresholdSeconds)
                    items.filter { it.currentStatus == InventoryStatus.UNDER_REVIEW && it.updatedAt <= stale }
                        .forEach { item ->
                            createReminder(rule.ruleId, item.inventoryItemId, null, "Review overdue")
                        }
                }
                InventoryRuleType.SESSION_OVERDUE_DAYS -> {
                    val stale = Instant.fromEpochSeconds(now.epochSeconds - thresholdSeconds)
                    sessions.filter { session ->
                        session.status != InventorySessionStatus.CLOSED &&
                            session.periodEnd?.let { it <= stale } == true
                    }
                        .forEach { session ->
                            createReminder(rule.ruleId, null, session.sessionId, "Session overdue")
                        }
                }
                InventoryRuleType.INCIDENT_OPEN_DAYS -> {
                    val stale = Instant.fromEpochSeconds(now.epochSeconds - thresholdSeconds)
                    incidents.filter {
                        it.status == InventoryIncidentStatus.OPEN || it.status == InventoryIncidentStatus.UNDER_REVIEW
                    }.filter { it.reportedAt <= stale }
                        .forEach { incident ->
                            createReminder(rule.ruleId, incident.inventoryItemId, incident.sessionId, "Incident unresolved")
                        }
                }
                InventoryRuleType.CUSTOM -> Unit
            }
        }
    }

    private suspend fun publish(event: org.expert.link.mesh.domain.model.inventory.InventoryEvent) {
        inventorySyncService.broadcastEvent(event)
    }
}
