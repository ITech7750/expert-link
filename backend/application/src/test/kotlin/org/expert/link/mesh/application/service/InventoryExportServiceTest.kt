package org.expert.link.mesh.application.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.inventory.InventoryExportFormat
import org.expert.link.mesh.domain.model.inventory.InventoryExportStatus
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventorySession
import org.expert.link.mesh.domain.model.inventory.InventorySessionReviewStatus
import org.expert.link.mesh.domain.model.inventory.InventorySessionStatus
import org.expert.link.mesh.domain.model.security.CryptoMaterialRef
import org.expert.link.mesh.domain.model.security.CryptoStorageType
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryExportRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventorySessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryRoleRepositoryAdapter
import org.junit.jupiter.api.Test

class InventoryExportServiceTest {
    @Test
    fun `should request and complete export`() = runTest {
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

        val sessionRepository = InMemoryInventorySessionRepositoryAdapter()
        sessionRepository.save(
            InventorySession(
                sessionId = "session-1",
                organizationId = "org-1",
                title = "Отчёт",
                description = null,
                periodStart = Instant.parse("2026-01-01T00:00:00Z"),
                periodEnd = null,
                status = InventorySessionStatus.CLOSED,
                reviewStatus = InventorySessionReviewStatus.COMPLETED,
                createdByPeerId = "peer-1",
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        val exportService = InventoryExportService(
            localProfileService = localProfileService,
            sessionRepositoryPort = sessionRepository,
            exportRepositoryPort = InMemoryInventoryExportRepositoryAdapter(),
            rbacService = rbacService,
            inventoryEventService = eventService,
            inventorySyncService = syncService,
        )

        val task = exportService.requestExport("org-1", "session-1", InventoryExportFormat.PDF)
        val updated = exportService.updateExportStatus(
            task.exportTaskId,
            InventoryExportStatus.COMPLETED,
            FileDescriptor("file-1", "report.pdf", 1200, "hash", "application/pdf"),
            null,
        )

        assertThat(task.status).isEqualTo(InventoryExportStatus.REQUESTED)
        assertThat(updated?.status).isEqualTo(InventoryExportStatus.COMPLETED)
    }

    private suspend fun seedRolesAndMember(
        roleRepository: InMemoryRoleRepositoryAdapter,
        memberRepository: InMemoryOrganizationMemberRepositoryAdapter,
        organizationId: String,
        peerId: String,
    ) {
        val role = org.expert.link.mesh.domain.model.inventory.Role(
            roleId = "role-$organizationId-export",
            organizationId = organizationId,
            name = "Exporter",
            description = null,
            permissions = setOf(InventoryPermission.EXPORT_REQUEST, InventoryPermission.EXPORT_MANAGE),
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
