package org.expert.link.mesh.application.service

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.OrganizationMember
import org.expert.link.mesh.domain.model.inventory.OrganizationMemberStatus
import org.expert.link.mesh.domain.model.inventory.Role
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryRoleRepositoryAdapter
import org.junit.jupiter.api.Test

class InventoryRbacServiceTest {
    @Test
    fun `should grant permission for assigned role`() = runTest {
        val roleRepository = InMemoryRoleRepositoryAdapter()
        val memberRepository = InMemoryOrganizationMemberRepositoryAdapter()
        val rbacService = InventoryRbacService(roleRepository, memberRepository)

        val role = Role(
            roleId = "role-1",
            organizationId = "org-1",
            name = "Operator",
            description = null,
            permissions = setOf(InventoryPermission.ITEM_CREATE, InventoryPermission.ITEM_EDIT),
            system = false,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        roleRepository.save(role)
        memberRepository.save(
            OrganizationMember(
                organizationId = "org-1",
                peerId = "peer-1",
                displayName = "Alice",
                roleIds = setOf(role.roleId),
                status = OrganizationMemberStatus.ACTIVE,
                joinedAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        val allowed = rbacService.hasPermission("org-1", "peer-1", InventoryPermission.ITEM_CREATE)

        assertThat(allowed).isTrue()
    }

    @Test
    fun `should deny permission for missing role`() = runTest {
        val roleRepository = InMemoryRoleRepositoryAdapter()
        val memberRepository = InMemoryOrganizationMemberRepositoryAdapter()
        val rbacService = InventoryRbacService(roleRepository, memberRepository)

        memberRepository.save(
            OrganizationMember(
                organizationId = "org-1",
                peerId = "peer-2",
                displayName = "Bob",
                roleIds = emptySet(),
                status = OrganizationMemberStatus.ACTIVE,
                joinedAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        val allowed = rbacService.hasPermission("org-1", "peer-2", InventoryPermission.ITEM_CREATE)

        assertThat(allowed).isFalse()
    }
}
