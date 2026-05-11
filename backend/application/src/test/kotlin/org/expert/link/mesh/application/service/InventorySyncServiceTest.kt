package org.expert.link.mesh.application.service

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.inventory.InventoryCondition
import org.expert.link.mesh.domain.model.inventory.InventoryEvent
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryItem
import org.expert.link.mesh.domain.model.inventory.InventoryItemSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.domain.model.network.InventorySyncResponsePayload
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAttachmentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAlertRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAttributeDefinitionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCategoryRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCategoryTemplateRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryChangeLogRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCommentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeBindingRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryConflictRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCostCenterRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryDeadlineRuleRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryDepartmentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryDashboardRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryExportRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryFundingSourceRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryIncidentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryItemRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLegalHolderRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLocationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLabelRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLabelTemplateRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryOwnerRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryQrCodeRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryPrintTaskRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryReminderRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryRevisionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryReviewRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryScanEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventorySessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventorySubcategoryRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventorySupplierRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryTagRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryThresholdRuleRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryRoleRepositoryAdapter
import org.junit.jupiter.api.Test

class InventorySyncServiceTest {
    @Test
    fun `should apply incoming inventory events`() = runTest {
        val eventRepository = InMemoryInventoryEventRepositoryAdapter()
        val eventService = InventoryEventService(eventRepository)
        val itemRepository = InMemoryInventoryItemRepositoryAdapter()
        val applier = InventoryEventApplier(
            localProfileService = mockk(relaxed = true),
            organizationRepositoryPort = InMemoryOrganizationRepositoryAdapter(),
            organizationMemberRepositoryPort = InMemoryOrganizationMemberRepositoryAdapter(),
            roleRepositoryPort = InMemoryRoleRepositoryAdapter(),
            categoryRepositoryPort = InMemoryInventoryCategoryRepositoryAdapter(),
            subcategoryRepositoryPort = InMemoryInventorySubcategoryRepositoryAdapter(),
            tagRepositoryPort = InMemoryInventoryTagRepositoryAdapter(),
            attributeDefinitionRepositoryPort = InMemoryInventoryAttributeDefinitionRepositoryAdapter(),
            categoryTemplateRepositoryPort = InMemoryInventoryCategoryTemplateRepositoryAdapter(),
            locationRepositoryPort = InMemoryInventoryLocationRepositoryAdapter(),
            ownerRepositoryPort = InMemoryInventoryOwnerRepositoryAdapter(),
            departmentRepositoryPort = InMemoryInventoryDepartmentRepositoryAdapter(),
            costCenterRepositoryPort = InMemoryInventoryCostCenterRepositoryAdapter(),
            legalHolderRepositoryPort = InMemoryInventoryLegalHolderRepositoryAdapter(),
            supplierRepositoryPort = InMemoryInventorySupplierRepositoryAdapter(),
            fundingSourceRepositoryPort = InMemoryInventoryFundingSourceRepositoryAdapter(),
            itemRepositoryPort = itemRepository,
            sessionRepositoryPort = InMemoryInventorySessionRepositoryAdapter(),
            reviewRepositoryPort = InMemoryInventoryReviewRepositoryAdapter(),
            commentRepositoryPort = InMemoryInventoryCommentRepositoryAdapter(),
            attachmentRepositoryPort = InMemoryInventoryAttachmentRepositoryAdapter(),
            incidentRepositoryPort = InMemoryInventoryIncidentRepositoryAdapter(),
            alertRepositoryPort = InMemoryInventoryAlertRepositoryAdapter(),
            reminderRepositoryPort = InMemoryInventoryReminderRepositoryAdapter(),
            thresholdRuleRepositoryPort = InMemoryInventoryThresholdRuleRepositoryAdapter(),
            deadlineRuleRepositoryPort = InMemoryInventoryDeadlineRuleRepositoryAdapter(),
            changeLogRepositoryPort = InMemoryInventoryChangeLogRepositoryAdapter(),
            dashboardRepositoryPort = InMemoryInventoryDashboardRepositoryAdapter(),
            codeRepositoryPort = InMemoryInventoryCodeRepositoryAdapter(),
            codeBindingRepositoryPort = InMemoryInventoryCodeBindingRepositoryAdapter(),
            labelTemplateRepositoryPort = InMemoryInventoryLabelTemplateRepositoryAdapter(),
            labelRepositoryPort = InMemoryInventoryLabelRepositoryAdapter(),
            printTaskRepositoryPort = InMemoryInventoryPrintTaskRepositoryAdapter(),
            scanEventRepositoryPort = InMemoryInventoryScanEventRepositoryAdapter(),
            revisionRepositoryPort = InMemoryInventoryRevisionRepositoryAdapter(),
            conflictRepositoryPort = InMemoryInventoryConflictRepositoryAdapter(),
            exportRepositoryPort = InMemoryInventoryExportRepositoryAdapter(),
            qrCodeRepositoryPort = InMemoryInventoryQrCodeRepositoryAdapter(),
            eventRepositoryPort = eventRepository,
            inventoryEventService = eventService,
        )
        val syncService = InventorySyncService(
            localProfileService = mockk(relaxed = true),
            organizationMemberRepositoryPort = InMemoryOrganizationMemberRepositoryAdapter(),
            peerTrustVerificationService = mockk(relaxed = true),
            messageEncryptionService = mockk(relaxed = true),
            packetEnvelopeFactory = mockk(relaxed = true),
            packetSignatureService = mockk(relaxed = true),
            deliveryTrackingService = mockk(relaxed = true),
            inventoryEventRepositoryPort = eventRepository,
            inventoryEventApplier = applier,
            eventLogService = mockk(relaxed = true),
            nodeMetricsService = NodeMetricsService(),
        )

        val item = InventoryItem(
            inventoryItemId = "item-100",
            organizationId = "org-1",
            inventoryNumber = "INV-100",
            qrCode = "QR-100",
            barcode = null,
            categoryId = null,
            title = "Сервер",
            description = null,
            serialNumber = "SN-100",
            condition = InventoryCondition.GOOD,
            locationId = null,
            responsiblePerson = null,
            responsibleDepartment = null,
            createdByPeerId = "peer-remote",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            currentStatus = InventoryStatus.ADDED,
            revision = 1,
        )
        val event = InventoryEvent(
            eventId = "event-1",
            organizationId = "org-1",
            entityType = org.expert.link.mesh.domain.model.inventory.InventoryEntityType.ITEM,
            entityId = "item-100",
            eventType = InventoryEventType.CREATED,
            actorPeerId = "peer-remote",
            occurredAt = Instant.parse("2026-01-01T00:00:00Z"),
            sequence = 1,
            entityRevision = 1,
            payload = InventoryItemSnapshot(item),
        )
        syncService.handleSyncResponse(
            InventorySyncResponsePayload(
                requestId = "req-1",
                organizationId = "org-1",
                responderPeerId = "peer-remote",
                targetPeerId = "peer-local",
                events = listOf(event),
                sentAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        val stored = itemRepository.findByInventoryItemId("item-100")
        assertThat(stored?.inventoryNumber).isEqualTo("INV-100")
    }
}
