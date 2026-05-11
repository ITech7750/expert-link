package org.expert.link.mesh.domain.port.external

import org.expert.link.mesh.domain.model.hybrid.CentralAttachmentArtifact
import org.expert.link.mesh.domain.model.hybrid.CentralAuthState
import org.expert.link.mesh.domain.model.hybrid.CentralBankAccount
import org.expert.link.mesh.domain.model.hybrid.CentralConflict
import org.expert.link.mesh.domain.model.hybrid.CentralConflictResolution
import org.expert.link.mesh.domain.model.hybrid.CentralCostCenter
import org.expert.link.mesh.domain.model.hybrid.CentralDepartment
import org.expert.link.mesh.domain.model.hybrid.CentralExportTask
import org.expert.link.mesh.domain.model.hybrid.CentralLocation
import org.expert.link.mesh.domain.model.hybrid.CentralLoginRequest
import org.expert.link.mesh.domain.model.hybrid.CentralLoginResult
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationDashboardSummary
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationAccess
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationParty
import org.expert.link.mesh.domain.model.hybrid.CentralPermissionDefinition
import org.expert.link.mesh.domain.model.hybrid.CentralRelayNode
import org.expert.link.mesh.domain.model.hybrid.CentralRoleDefinition
import org.expert.link.mesh.domain.model.hybrid.CentralUserWorkspaceProfile
import org.expert.link.mesh.domain.model.inventory.InventoryEvent
import org.expert.link.mesh.domain.model.inventory.InventoryItem
import org.expert.link.mesh.domain.model.inventory.InventorySession

data class CentralClientBundle(
    val authClientPort: CentralAuthClientPort? = null,
    val organizationAccessClientPort: CentralOrganizationAccessClientPort? = null,
    val inventoryClientPort: CentralInventoryClientPort? = null,
    val syncClientPort: CentralSyncClientPort? = null,
    val attachmentClientPort: CentralAttachmentClientPort? = null,
    val exportClientPort: CentralExportClientPort? = null,
    val relayClientPort: CentralRelayClientPort? = null,
)

interface CentralAuthClientPort {
    suspend fun login(request: CentralLoginRequest): CentralLoginResult
    suspend fun refresh(refreshToken: String): CentralLoginResult?
    suspend fun currentUser(): CentralAuthState?
}

interface CentralOrganizationAccessClientPort {
    suspend fun organizations(): List<CentralOrganizationAccess>
    suspend fun memberships(organizationId: String): List<CentralOrganizationAccess>
    suspend fun roles(organizationId: String): List<CentralRoleDefinition>
    suspend fun activateOrganization(organizationId: String, active: Boolean = true): CentralOrganizationAccess?
    suspend fun locations(organizationId: String): List<CentralLocation>
    suspend fun departments(organizationId: String): List<CentralDepartment>
    suspend fun costCenters(organizationId: String): List<CentralCostCenter>
    suspend fun bankAccounts(organizationId: String): List<CentralBankAccount>
    suspend fun organizationParties(organizationId: String): List<CentralOrganizationParty>
    suspend fun permissionDefinitions(): List<CentralPermissionDefinition>
    suspend fun organizationDashboard(organizationId: String): CentralOrganizationDashboardSummary?
    suspend fun userProfile(userId: String): CentralUserWorkspaceProfile?
}

interface CentralInventoryClientPort {
    suspend fun items(organizationId: String): List<InventoryItem>
    suspend fun item(inventoryItemId: String): InventoryItem?
    suspend fun sessions(organizationId: String): List<InventorySession>
    suspend fun session(sessionId: String): InventorySession?
}

data class CentralSyncUploadResult(
    val nextCursor: Long,
    val acknowledgedChangeIds: Set<String>,
    val conflicts: List<CentralConflict>,
)

data class CentralSyncPullResult(
    val nextCursor: Long,
    val events: List<InventoryEvent>,
    val conflicts: List<CentralConflict> = emptyList(),
)

interface CentralSyncClientPort {
    suspend fun upload(
        organizationId: String,
        deviceId: String,
        cursor: Long?,
        changes: List<InventoryEvent>,
    ): CentralSyncUploadResult

    suspend fun pull(
        organizationId: String,
        afterCursor: Long,
        limit: Int = 200,
    ): CentralSyncPullResult

    suspend fun conflicts(organizationId: String): List<CentralConflict>
    suspend fun resolveConflict(request: CentralConflictResolution): CentralConflict?
}

interface CentralAttachmentClientPort {
    suspend fun upload(
        organizationId: String,
        aggregateId: String,
        aggregateType: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray,
    ): CentralAttachmentArtifact

    suspend fun findByAttachmentId(attachmentId: String): CentralAttachmentArtifact?
}

interface CentralExportClientPort {
    suspend fun create(
        organizationId: String,
        sessionId: String,
        format: String,
    ): CentralExportTask

    suspend fun findByExportTaskId(exportTaskId: String): CentralExportTask?
}

interface CentralRelayClientPort {
    suspend fun relayNodes(organizationId: String, onlineOnly: Boolean = false): List<CentralRelayNode>
}
