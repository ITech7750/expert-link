package org.expert.link.mesh.application.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.inventory.InventoryCondition
import org.expert.link.mesh.domain.model.inventory.InventoryItemType
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventorySessionStatus
import org.expert.link.mesh.domain.model.security.CryptoMaterialRef
import org.expert.link.mesh.domain.model.security.CryptoStorageType
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryItemRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventorySessionMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventorySessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryChangeLogRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryRoleRepositoryAdapter
import org.junit.jupiter.api.Test

class InventorySessionServiceTest {
    @Test
    fun `should create session and link items`() = runTest {
        val localProfileService = mockk<LocalProfileService>()
        coEvery { localProfileService.require() } returns localProfile("alice", "peer-1")
        val roleRepository = InMemoryRoleRepositoryAdapter()
        val memberRepository = InMemoryOrganizationMemberRepositoryAdapter()
        val rbacService = InventoryRbacService(roleRepository, memberRepository)
        seedRolesAndMember(roleRepository, memberRepository, "org-1", "peer-1")
        val eventRepository = InMemoryInventoryEventRepositoryAdapter()
        val eventService = InventoryEventService(eventRepository)
        val syncService = mockk<InventorySyncService>()
        coEvery { syncService.broadcastEvent(any()) } returns Unit
        val changeLogService = InventoryChangeLogService(
            changeLogRepositoryPort = InMemoryInventoryChangeLogRepositoryAdapter(),
            inventoryEventService = eventService,
            inventorySyncService = syncService,
        )

        val itemRepository = InMemoryInventoryItemRepositoryAdapter()
        val itemService = InventoryItemService(
            localProfileService = localProfileService,
            itemRepositoryPort = itemRepository,
            commentRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCommentRepositoryAdapter(),
            attachmentRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAttachmentRepositoryAdapter(),
            qrCodeRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryQrCodeRepositoryAdapter(),
            codeRepositoryPort = InMemoryInventoryCodeRepositoryAdapter(),
            rbacService = rbacService,
            inventoryEventService = eventService,
            inventoryChangeLogService = changeLogService,
            inventorySyncService = syncService,
        )
        val item = itemService.createItem(
            organizationId = "org-1",
            inventoryNumber = "INV-3",
            localNumber = null,
            qrCode = null,
            barcode = null,
            categoryId = null,
            subcategoryId = null,
            itemType = InventoryItemType.UNKNOWN,
            title = "Ноутбук",
            description = null,
            brand = null,
            model = null,
            serialNumber = "SN-3",
            manufacturer = null,
            purchaseDate = null,
            commissioningDate = null,
            warrantyUntil = null,
            depreciationGroup = null,
            usefulLifeMonths = null,
            condition = InventoryCondition.GOOD,
            locationId = null,
            responsiblePerson = null,
            responsibleDepartment = null,
            responsibleUserId = null,
            ownerOrganizationId = null,
            ownerId = null,
            departmentId = null,
            costCenterId = null,
            legalHolderId = null,
            supplierId = null,
            fundingSourceId = null,
            lastInventoryAt = null,
            nextInventoryAt = null,
            tagIds = emptySet(),
            attributes = emptyList(),
            metadata = emptyMap(),
        )

        val sessionService = InventorySessionService(
            localProfileService = localProfileService,
            sessionRepositoryPort = InMemoryInventorySessionRepositoryAdapter(),
            sessionMemberRepositoryPort = InMemoryInventorySessionMemberRepositoryAdapter(),
            itemRepositoryPort = itemRepository,
            rbacService = rbacService,
            inventoryEventService = eventService,
            inventoryChangeLogService = changeLogService,
            inventorySyncService = syncService,
        )

        val session = sessionService.createSession(
            organizationId = "org-1",
            title = "Весна 2026",
            description = null,
            periodStart = Instant.parse("2026-03-01T00:00:00Z"),
            periodEnd = Instant.parse("2026-03-31T00:00:00Z"),
            itemIds = setOf(item.inventoryItemId),
            memberPeerIds = setOf("peer-1"),
        )

        val updatedItem = itemRepository.findByInventoryItemId(item.inventoryItemId)

        assertThat(session.status).isEqualTo(InventorySessionStatus.DRAFT)
        assertThat(updatedItem?.sessionIds).contains(session.sessionId)
    }

    @Test
    fun `should close session`() = runTest {
        val localProfileService = mockk<LocalProfileService>()
        coEvery { localProfileService.require() } returns localProfile("alice", "peer-1")
        val roleRepository = InMemoryRoleRepositoryAdapter()
        val memberRepository = InMemoryOrganizationMemberRepositoryAdapter()
        val rbacService = InventoryRbacService(roleRepository, memberRepository)
        seedRolesAndMember(roleRepository, memberRepository, "org-1", "peer-1")
        val eventRepository = InMemoryInventoryEventRepositoryAdapter()
        val eventService = InventoryEventService(eventRepository)
        val syncService = mockk<InventorySyncService>()
        coEvery { syncService.broadcastEvent(any()) } returns Unit
        val changeLogService = InventoryChangeLogService(
            changeLogRepositoryPort = InMemoryInventoryChangeLogRepositoryAdapter(),
            inventoryEventService = eventService,
            inventorySyncService = syncService,
        )

        val sessionRepository = InMemoryInventorySessionRepositoryAdapter()
        val sessionService = InventorySessionService(
            localProfileService = localProfileService,
            sessionRepositoryPort = sessionRepository,
            sessionMemberRepositoryPort = InMemoryInventorySessionMemberRepositoryAdapter(),
            itemRepositoryPort = InMemoryInventoryItemRepositoryAdapter(),
            rbacService = rbacService,
            inventoryEventService = eventService,
            inventoryChangeLogService = changeLogService,
            inventorySyncService = syncService,
        )

        val session = sessionService.createSession(
            organizationId = "org-1",
            title = "Осень 2026",
            description = null,
            periodStart = Instant.parse("2026-09-01T00:00:00Z"),
            periodEnd = Instant.parse("2026-09-30T00:00:00Z"),
            itemIds = emptySet(),
            memberPeerIds = setOf("peer-1"),
        )
        val closed = sessionService.closeSession(session.sessionId, "done")

        assertThat(closed?.status).isEqualTo(InventorySessionStatus.CLOSED)
    }

    private suspend fun seedRolesAndMember(
        roleRepository: InMemoryRoleRepositoryAdapter,
        memberRepository: InMemoryOrganizationMemberRepositoryAdapter,
        organizationId: String,
        peerId: String,
    ) {
        val role = org.expert.link.mesh.domain.model.inventory.Role(
            roleId = "role-$organizationId-admin",
            organizationId = organizationId,
            name = "Admin",
            description = null,
            permissions = InventoryPermission.entries.toSet(),
            system = false,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        roleRepository.save(role)
        memberRepository.save(
            org.expert.link.mesh.domain.model.inventory.OrganizationMember(
                organizationId = organizationId,
                peerId = peerId,
                displayName = "Alice",
                roleIds = setOf(role.roleId),
                status = org.expert.link.mesh.domain.model.inventory.OrganizationMemberStatus.ACTIVE,
                joinedAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
    }

    private fun localProfile(displayName: String, peerId: String): LocalProfile = LocalProfile(
        peerId = peerId,
        displayName = displayName,
        publicKey = "public-$peerId",
        privateKey = "private-$peerId",
        keyMaterialRef = CryptoMaterialRef(
            alias = "alias-$peerId",
            storageType = CryptoStorageType.IN_MEMORY,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        ),
        capabilities = setOf("inventory"),
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
