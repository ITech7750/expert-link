package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.OrganizationMemberStatus
import org.expert.link.mesh.domain.model.inventory.Role
import org.expert.link.mesh.domain.port.repository.OrganizationMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.RoleRepositoryPort

class InventoryRbacService(
    private val roleRepositoryPort: RoleRepositoryPort,
    private val organizationMemberRepositoryPort: OrganizationMemberRepositoryPort,
) {
    suspend fun ensureDefaultRoles(organizationId: String, createdByPeerId: String): List<Role> {
        val now = now()
        return BuiltInRole.entries.map { builtIn ->
            val existing = roleRepositoryPort.findByRoleId(builtIn.roleId(organizationId))
            existing ?: roleRepositoryPort.save(
                Role(
                    roleId = builtIn.roleId(organizationId),
                    organizationId = organizationId,
                    name = builtIn.displayName,
                    description = builtIn.description,
                    permissions = builtIn.permissions,
                    system = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun hasPermission(organizationId: String, peerId: String, permission: InventoryPermission): Boolean {
        val member = organizationMemberRepositoryPort.findByOrganizationAndPeer(organizationId, peerId)
            ?: return false
        if (member.status != OrganizationMemberStatus.ACTIVE) {
            return false
        }
        if (member.roleIds.isEmpty()) {
            return false
        }
        val roles = member.roleIds.mapNotNull { roleRepositoryPort.findByRoleId(it) }
        return roles.any { permission in it.permissions }
    }

    suspend fun requirePermission(organizationId: String, peerId: String, permission: InventoryPermission) {
        require(hasPermission(organizationId, peerId, permission)) {
            "Permission $permission denied for $peerId in $organizationId"
        }
    }

    enum class BuiltInRole(
        val displayName: String,
        val description: String,
        val permissions: Set<InventoryPermission>,
    ) {
        SYSTEM_ADMIN(
            displayName = "System Admin",
            description = "Full access to all inventory features.",
            permissions = InventoryPermission.entries.toSet(),
        ),
        ORG_ADMIN(
            displayName = "Organization Admin",
            description = "Manages organization and inventory.",
            permissions = InventoryPermission.entries.toSet(),
        ),
        INVENTORY_OPERATOR(
            displayName = "Inventory Operator",
            description = "Creates and updates inventory items.",
            permissions = setOf(
                InventoryPermission.ITEM_CREATE,
                InventoryPermission.ITEM_EDIT,
                InventoryPermission.ITEM_VIEW,
                InventoryPermission.ITEM_SUBMIT_REVIEW,
                InventoryPermission.ITEM_COMMENT,
                InventoryPermission.ITEM_ATTACH,
                InventoryPermission.ITEM_UPLOAD_PHOTO,
                InventoryPermission.ITEM_INCIDENT,
                InventoryPermission.INCIDENT_CREATE,
                InventoryPermission.SEARCH,
                InventoryPermission.DASHBOARD_VIEW,
                InventoryPermission.CATEGORY_MANAGE,
                InventoryPermission.LOCATION_MANAGE,
                InventoryPermission.TAG_MANAGE,
                InventoryPermission.ATTRIBUTE_MANAGE,
                InventoryPermission.TEMPLATE_MANAGE,
                InventoryPermission.SESSION_ADD_ITEM,
                InventoryPermission.EXPORT_VIEW,
            ),
        ),
        COMMISSION_MEMBER(
            displayName = "Commission Member",
            description = "Reviews and confirms inventory items.",
            permissions = setOf(
                InventoryPermission.ITEM_VIEW,
                InventoryPermission.ITEM_CONFIRM,
                InventoryPermission.ITEM_REJECT,
                InventoryPermission.ITEM_COMMENT,
                InventoryPermission.ITEM_INCIDENT,
                InventoryPermission.INCIDENT_VIEW,
                InventoryPermission.INCIDENT_REVIEW,
                InventoryPermission.INCIDENT_RESOLVE,
                InventoryPermission.SEARCH,
                InventoryPermission.DASHBOARD_VIEW,
                InventoryPermission.SESSION_UPDATE,
                InventoryPermission.SESSION_CLOSE,
                InventoryPermission.EXPORT_VIEW,
                InventoryPermission.AUDIT_VIEW,
            ),
        ),
        REVIEWER(
            displayName = "Reviewer",
            description = "Reviews inventory items.",
            permissions = setOf(
                InventoryPermission.ITEM_VIEW,
                InventoryPermission.ITEM_CONFIRM,
                InventoryPermission.ITEM_REJECT,
                InventoryPermission.ITEM_COMMENT,
                InventoryPermission.ITEM_INCIDENT,
                InventoryPermission.INCIDENT_VIEW,
                InventoryPermission.INCIDENT_REVIEW,
                InventoryPermission.SEARCH,
                InventoryPermission.DASHBOARD_VIEW,
                InventoryPermission.EXPORT_VIEW,
                InventoryPermission.AUDIT_VIEW,
            ),
        ),
        OBSERVER(
            displayName = "Observer",
            description = "Read-only access for audit.",
            permissions = setOf(
                InventoryPermission.ITEM_VIEW,
                InventoryPermission.INCIDENT_VIEW,
                InventoryPermission.SEARCH,
                InventoryPermission.DASHBOARD_VIEW,
                InventoryPermission.EXPORT_VIEW,
                InventoryPermission.AUDIT_VIEW,
            ),
        );

        fun roleId(organizationId: String): String = "role-$organizationId-${name.lowercase()}"
    }
}
