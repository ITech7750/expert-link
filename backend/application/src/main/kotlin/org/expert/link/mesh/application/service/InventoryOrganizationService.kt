package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryMemberSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryOrganizationSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryRoleSnapshot
import org.expert.link.mesh.domain.model.inventory.Organization
import org.expert.link.mesh.domain.model.inventory.OrganizationMember
import org.expert.link.mesh.domain.model.inventory.OrganizationMemberStatus
import org.expert.link.mesh.domain.model.inventory.Role
import org.expert.link.mesh.domain.port.repository.OrganizationMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.OrganizationRepositoryPort
import org.expert.link.mesh.domain.port.repository.RoleRepositoryPort

class InventoryOrganizationService(
    private val localProfileService: LocalProfileService,
    private val organizationRepositoryPort: OrganizationRepositoryPort,
    private val organizationMemberRepositoryPort: OrganizationMemberRepositoryPort,
    private val roleRepositoryPort: RoleRepositoryPort,
    private val rbacService: InventoryRbacService,
    private val inventoryEventService: InventoryEventService,
    private val inventorySyncService: InventorySyncService,
) {
    suspend fun createOrganization(name: String, description: String?): Organization {
        require(name.isNotBlank()) { "Organization name is blank" }
        val localProfile = localProfileService.require()
        val organization = Organization(
            organizationId = newId("org"),
            name = name,
            description = description,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        organizationRepositoryPort.save(organization)
        val roles = rbacService.ensureDefaultRoles(organization.organizationId, localProfile.peerId)
        val creator = OrganizationMember(
            organizationId = organization.organizationId,
            peerId = localProfile.peerId,
            displayName = localProfile.displayName,
            roleIds = setOf(
                InventoryRbacService.BuiltInRole.SYSTEM_ADMIN.roleId(organization.organizationId),
                InventoryRbacService.BuiltInRole.ORG_ADMIN.roleId(organization.organizationId),
            ),
            status = OrganizationMemberStatus.ACTIVE,
            joinedAt = now(),
            updatedAt = now(),
        )
        organizationMemberRepositoryPort.save(creator)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organization.organizationId,
                entityType = InventoryEntityType.ORGANIZATION,
                entityId = organization.organizationId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryOrganizationSnapshot(organization),
            ),
        )
        publish(
            inventoryEventService.recordEvent(
                organizationId = organization.organizationId,
                entityType = InventoryEntityType.MEMBER,
                entityId = creator.peerId,
                eventType = InventoryEventType.MEMBER_ADDED,
                actorPeerId = localProfile.peerId,
                payload = InventoryMemberSnapshot(creator),
            ),
        )
        roles.forEach { role ->
            publish(
                inventoryEventService.recordEvent(
                    organizationId = organization.organizationId,
                    entityType = InventoryEntityType.ROLE,
                    entityId = role.roleId,
                    eventType = InventoryEventType.CREATED,
                    actorPeerId = localProfile.peerId,
                    payload = InventoryRoleSnapshot(role),
                ),
            )
        }
        return organization
    }

    suspend fun updateOrganization(organizationId: String, name: String, description: String?): Organization? {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, orgPermission())
        val current = organizationRepositoryPort.findByOrganizationId(organizationId) ?: return null
        val updated = current.copy(
            name = name,
            description = description,
            updatedAt = now(),
        )
        organizationRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.ORGANIZATION,
                entityId = organizationId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryOrganizationSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun listOrganizations(): List<Organization> = organizationRepositoryPort.list()

    suspend fun organizationMembers(organizationId: String): List<OrganizationMember> =
        organizationMemberRepositoryPort.listByOrganization(organizationId)

    suspend fun addMember(
        organizationId: String,
        peerId: String,
        displayName: String,
        roleIds: Set<String>,
        departmentId: String? = null,
        locationIds: Set<String> = emptySet(),
        position: String? = null,
        isCommissionMember: Boolean = false,
    ): OrganizationMember {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, memberPermission())
        val member = OrganizationMember(
            organizationId = organizationId,
            peerId = peerId,
            displayName = displayName,
            roleIds = roleIds,
            departmentId = departmentId,
            locationIds = locationIds,
            position = position,
            isCommissionMember = isCommissionMember,
            status = OrganizationMemberStatus.ACTIVE,
            joinedAt = now(),
            updatedAt = now(),
        )
        organizationMemberRepositoryPort.save(member)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.MEMBER,
                entityId = peerId,
                eventType = InventoryEventType.MEMBER_ADDED,
                actorPeerId = localProfile.peerId,
                payload = InventoryMemberSnapshot(member),
            ),
        )
        return member
    }

    suspend fun updateMemberRoles(
        organizationId: String,
        peerId: String,
        roleIds: Set<String>,
    ): OrganizationMember? {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, memberPermission())
        val current = organizationMemberRepositoryPort.findByOrganizationAndPeer(organizationId, peerId) ?: return null
        val updated = current.copy(roleIds = roleIds, updatedAt = now())
        organizationMemberRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.MEMBER,
                entityId = peerId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryMemberSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun updateMember(
        organizationId: String,
        peerId: String,
        displayName: String?,
        roleIds: Set<String>?,
        departmentId: String?,
        locationIds: Set<String>?,
        position: String?,
        isCommissionMember: Boolean?,
        status: OrganizationMemberStatus?,
    ): OrganizationMember? {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, memberPermission())
        val current = organizationMemberRepositoryPort.findByOrganizationAndPeer(organizationId, peerId) ?: return null
        val updated = current.copy(
            displayName = displayName ?: current.displayName,
            roleIds = roleIds ?: current.roleIds,
            departmentId = departmentId ?: current.departmentId,
            locationIds = locationIds ?: current.locationIds,
            position = position ?: current.position,
            isCommissionMember = isCommissionMember ?: current.isCommissionMember,
            status = status ?: current.status,
            updatedAt = now(),
        )
        organizationMemberRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.MEMBER,
                entityId = peerId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryMemberSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun removeMember(organizationId: String, peerId: String) {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, memberPermission())
        organizationMemberRepositoryPort.remove(organizationId, peerId)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.MEMBER,
                entityId = peerId,
                eventType = InventoryEventType.MEMBER_REMOVED,
                actorPeerId = localProfile.peerId,
                payload = InventoryMemberSnapshot(
                    OrganizationMember(
                        organizationId = organizationId,
                        peerId = peerId,
                        displayName = "",
                        roleIds = emptySet(),
                        status = OrganizationMemberStatus.LEFT,
                        joinedAt = now(),
                        updatedAt = now(),
                    ),
                ),
            ),
        )
    }

    suspend fun roles(organizationId: String): List<Role> = roleRepositoryPort.listByOrganization(organizationId)

    suspend fun createRole(organizationId: String, name: String, description: String?, permissions: Set<org.expert.link.mesh.domain.model.inventory.InventoryPermission>): Role {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, rolePermission())
        val role = Role(
            roleId = newId("role"),
            organizationId = organizationId,
            name = name,
            description = description,
            permissions = permissions,
            system = false,
            createdAt = now(),
            updatedAt = now(),
        )
        roleRepositoryPort.save(role)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.ROLE,
                entityId = role.roleId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryRoleSnapshot(role),
            ),
        )
        return role
    }

    suspend fun updateRole(roleId: String, name: String, description: String?, permissions: Set<org.expert.link.mesh.domain.model.inventory.InventoryPermission>): Role? {
        val role = roleRepositoryPort.findByRoleId(roleId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(role.organizationId, localProfile.peerId, rolePermission())
        val updated = role.copy(
            name = name,
            description = description,
            permissions = permissions,
            updatedAt = now(),
        )
        roleRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = role.organizationId,
                entityType = InventoryEntityType.ROLE,
                entityId = roleId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryRoleSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun removeRole(roleId: String) {
        val role = roleRepositoryPort.findByRoleId(roleId) ?: return
        val localProfile = localProfileService.require()
        rbacService.requirePermission(role.organizationId, localProfile.peerId, rolePermission())
        roleRepositoryPort.remove(roleId)
        publish(
            inventoryEventService.recordEvent(
                organizationId = role.organizationId,
                entityType = InventoryEntityType.ROLE,
                entityId = roleId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryRoleSnapshot(role.copy(updatedAt = now())),
            ),
        )
    }

    private fun memberPermission() = org.expert.link.mesh.domain.model.inventory.InventoryPermission.MEMBER_MANAGE
    private fun rolePermission() = org.expert.link.mesh.domain.model.inventory.InventoryPermission.ROLE_MANAGE
    private fun orgPermission() = org.expert.link.mesh.domain.model.inventory.InventoryPermission.ORGANIZATION_MANAGE

    private suspend fun publish(event: org.expert.link.mesh.domain.model.inventory.InventoryEvent) {
        inventorySyncService.broadcastEvent(event)
    }
}
