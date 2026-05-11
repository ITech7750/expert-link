package org.expert.link.app.shared.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import org.expert.link.mesh.contract.model.MeshInventoryPermission
import org.expert.link.mesh.contract.model.MeshInventoryReviewStatus
import org.expert.link.mesh.contract.model.MeshInventoryStatus
import org.expert.link.mesh.contract.model.MeshInventoryItem
import org.expert.link.mesh.contract.model.MeshOrganizationMember
import org.expert.link.mesh.contract.model.MeshOrganizationMemberStatus
import org.expert.link.mesh.contract.model.MeshRole

class InventoryPresentationTest {
    @Test
    fun `resolveInventoryPermissions aggregates role permissions`() {
        val roles = listOf(
            MeshRole(
                roleId = "role-1",
                organizationId = "org-1",
                name = "Operator",
                description = null,
                permissions = setOf(MeshInventoryPermission.ITEM_CREATE),
                system = false,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
            MeshRole(
                roleId = "role-2",
                organizationId = "org-1",
                name = "Exporter",
                description = null,
                permissions = setOf(MeshInventoryPermission.EXPORT_REQUEST),
                system = false,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        val members = listOf(
            MeshOrganizationMember(
                organizationId = "org-1",
                peerId = "peer-1",
                displayName = "Alice",
                roleIds = setOf("role-1", "role-2"),
                status = MeshOrganizationMemberStatus.ACTIVE,
                joinedAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        val permissions = resolveInventoryPermissions("peer-1", members, roles)

        assertEquals(
            setOf(MeshInventoryPermission.ITEM_CREATE, MeshInventoryPermission.EXPORT_REQUEST),
            permissions,
        )
    }

    @Test
    fun `filterReviewItems keeps only review statuses`() {
        val base = MeshInventoryItem(
            inventoryItemId = "item-1",
            organizationId = "org-1",
            inventoryNumber = "INV-1",
            title = "Проектор",
            createdByPeerId = "peer-1",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val items = listOf(
            base.copy(inventoryItemId = "item-2", currentStatus = MeshInventoryStatus.DRAFT),
            base.copy(inventoryItemId = "item-3", currentStatus = MeshInventoryStatus.UNDER_REVIEW),
            base.copy(inventoryItemId = "item-4", currentStatus = MeshInventoryStatus.REQUIRES_UPDATE),
        )

        val filtered = filterReviewItems(items)

        assertEquals(listOf("item-3", "item-4"), filtered.map { it.inventoryItemId })
    }

    @Test
    fun `review status maps to inventory status`() {
        val approved = reviewStatusToInventoryStatus(MeshInventoryReviewStatus.APPROVED)
        val rejected = reviewStatusToInventoryStatus(MeshInventoryReviewStatus.REJECTED)
        val update = reviewStatusToInventoryStatus(MeshInventoryReviewStatus.REQUIRES_UPDATE)

        assertTrue(approved == MeshInventoryStatus.CONFIRMED)
        assertTrue(rejected == MeshInventoryStatus.REJECTED)
        assertTrue(update == MeshInventoryStatus.REQUIRES_UPDATE)
    }
}
