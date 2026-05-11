package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class MeshCentralConnectivityMode {
    OFFLINE,
    MESH_ONLY,
    CENTRAL_AVAILABLE,
    CENTRAL_DEGRADED,
    RECONNECTING,
    CONFLICT_REVIEW_REQUIRED,
}

@Serializable
enum class MeshCentralSyncRunState {
    PAUSED,
    IDLE,
    RUNNING,
    ERROR,
}

@Serializable
enum class MeshCentralChangeStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CONFLICT,
}

@Serializable
data class MeshCentralConfig(
    val baseUrl: String,
    val authPath: String = "/api/v1/auth",
    val requestTimeoutMillis: Long = 10_000,
    val deviceId: String? = null,
    val allowMeteredSync: Boolean = true,
    val syncOnStart: Boolean = true,
    val syncOnResume: Boolean = true,
    val backgroundSyncIntervalSeconds: Long = 300,
)

@Serializable
data class MeshCentralUserProfile(
    val userId: String,
    val username: String,
    val displayName: String,
    val email: String? = null,
    val updatedAt: Instant,
)

@Serializable
data class MeshCentralAuthState(
    val authenticated: Boolean,
    val userProfile: MeshCentralUserProfile? = null,
    val accessTokenExpiresAt: Instant? = null,
    val activeOrganizationId: String? = null,
    val lastAuthenticatedAt: Instant? = null,
    val offlineFallback: Boolean = false,
)

@Serializable
data class MeshCentralOrganizationAccess(
    val organizationId: String,
    val name: String,
    val legalName: String? = null,
    val membershipId: String? = null,
    val memberUserId: String? = null,
    val roleIds: Set<String> = emptySet(),
    val permissions: Set<String> = emptySet(),
    val active: Boolean = false,
    val updatedAt: Instant,
)

@Serializable
data class MeshCentralUserWorkspaceProfile(
    val userId: String,
    val displayName: String,
    val email: String,
    val locale: String,
    val timeZone: String,
    val activeOrganizationId: String? = null,
    val updatedAt: Instant,
)

@Serializable
data class MeshCentralLocation(
    val locationId: String,
    val organizationId: String,
    val name: String,
    val type: String,
    val parentLocationId: String? = null,
    val code: String? = null,
    val active: Boolean = true,
    val updatedAt: Instant,
)

@Serializable
data class MeshCentralDepartment(
    val departmentId: String,
    val organizationId: String,
    val name: String,
    val parentDepartmentId: String? = null,
    val active: Boolean = true,
    val updatedAt: Instant,
)

@Serializable
data class MeshCentralCostCenter(
    val costCenterId: String,
    val organizationId: String,
    val code: String,
    val name: String,
    val active: Boolean = true,
    val updatedAt: Instant,
)

@Serializable
data class MeshCentralBankAccount(
    val bankAccountId: String,
    val organizationId: String,
    val bankName: String,
    val bic: String,
    val accountNumber: String,
    val correspondentAccount: String? = null,
    val active: Boolean = true,
    val updatedAt: Instant,
)

@Serializable
data class MeshCentralOrganizationParty(
    val partyId: String,
    val organizationId: String,
    val partyType: String,
    val name: String,
    val registrationNumber: String? = null,
    val active: Boolean = true,
    val updatedAt: Instant,
)

@Serializable
data class MeshCentralPermissionDefinition(
    val permissionId: String,
    val name: String,
    val description: String,
    val scope: String,
    val updatedAt: Instant,
)

@Serializable
data class MeshCentralOrganizationDashboardSummary(
    val organizationId: String,
    val memberships: Long,
    val roles: Long,
    val locations: Long,
    val bankAccounts: Long,
    val departments: Long,
    val costCenters: Long,
    val parties: Long,
)

@Serializable
data class MeshCentralRelayNode(
    val nodeId: String,
    val organizationId: String,
    val userId: String,
    val deviceId: String,
    val relayEndpoint: String,
    val endpointCandidates: Set<String> = emptySet(),
    val capabilities: Set<String> = emptySet(),
    val active: Boolean,
    val online: Boolean,
    val signalingReady: Boolean,
    val registeredAt: Instant,
    val lastSeenAt: Instant,
    val presenceExpiresAt: Instant? = null,
)

@Serializable
data class MeshCentralOrganizationWorkspaceSnapshot(
    val organizationId: String,
    val userProfile: MeshCentralUserWorkspaceProfile? = null,
    val dashboard: MeshCentralOrganizationDashboardSummary? = null,
    val locations: List<MeshCentralLocation> = emptyList(),
    val departments: List<MeshCentralDepartment> = emptyList(),
    val costCenters: List<MeshCentralCostCenter> = emptyList(),
    val bankAccounts: List<MeshCentralBankAccount> = emptyList(),
    val parties: List<MeshCentralOrganizationParty> = emptyList(),
    val permissionDefinitions: List<MeshCentralPermissionDefinition> = emptyList(),
    val relayNodes: List<MeshCentralRelayNode> = emptyList(),
    val fetchedAt: Instant,
)

@Serializable
data class MeshCentralPendingChange(
    val changeId: String,
    val organizationId: String,
    val aggregateId: String,
    val aggregateType: String,
    val changeType: String,
    val createdAt: Instant,
    val baseRevision: Long,
    val status: MeshCentralChangeStatus = MeshCentralChangeStatus.PENDING,
)

@Serializable
data class MeshCentralConflict(
    val conflictId: String,
    val organizationId: String,
    val aggregateId: String,
    val aggregateType: String,
    val localRevision: Long,
    val canonicalRevision: Long,
    val detectedAt: Instant,
    val status: MeshCentralChangeStatus = MeshCentralChangeStatus.CONFLICT,
    val note: String? = null,
)

@Serializable
data class MeshCentralSyncStatus(
    val organizationId: String,
    val connectivityMode: MeshCentralConnectivityMode,
    val runState: MeshCentralSyncRunState,
    val lastUploadedSequence: Long = 0,
    val lastPulledCursor: Long = 0,
    val pendingChanges: Int = 0,
    val conflicts: Int = 0,
    val lastSyncAt: Instant? = null,
    val lastError: String? = null,
)

@Serializable
data class MeshCentralExportTask(
    val exportTaskId: String,
    val organizationId: String,
    val sessionId: String,
    val format: String,
    val status: String,
    val requestedAt: Instant,
    val artifactRef: String? = null,
)

@Serializable
data class MeshCentralAttachmentArtifact(
    val attachmentId: String,
    val organizationId: String,
    val aggregateId: String,
    val aggregateType: String,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val artifactRef: String,
    val createdAt: Instant,
)

@Serializable
data class MeshCentralHybridState(
    val connectivityMode: MeshCentralConnectivityMode,
    val meshReady: Boolean,
    val centralConfigured: Boolean,
    val authenticated: Boolean,
    val activeOrganizationId: String? = null,
    val pendingChanges: Int = 0,
    val conflicts: Int = 0,
    val syncRunning: Boolean = false,
    val lastSyncAt: Instant? = null,
    val lastError: String? = null,
)
