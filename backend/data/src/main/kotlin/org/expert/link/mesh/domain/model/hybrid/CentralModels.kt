package org.expert.link.mesh.domain.model.hybrid

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class CentralConnectivityMode {
    OFFLINE,
    MESH_ONLY,
    CENTRAL_AVAILABLE,
    CENTRAL_DEGRADED,
    RECONNECTING,
    CONFLICT_REVIEW_REQUIRED,
}

@Serializable
enum class CentralSyncRunState {
    PAUSED,
    IDLE,
    RUNNING,
    ERROR,
}

@Serializable
enum class CentralChangeStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CONFLICT,
}

@Serializable
data class CentralNodeConfiguration(
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
data class CentralUserProfile(
    val userId: String,
    val username: String,
    val displayName: String,
    val email: String? = null,
    val updatedAt: Instant,
)

@Serializable
data class CentralAuthSession(
    val sessionId: String,
    val userProfile: CentralUserProfile,
    val accessToken: String,
    val refreshToken: String? = null,
    val accessTokenExpiresAt: Instant? = null,
    val activeOrganizationId: String? = null,
    val lastAuthenticatedAt: Instant,
)

@Serializable
data class CentralAuthState(
    val authenticated: Boolean,
    val userProfile: CentralUserProfile? = null,
    val accessTokenExpiresAt: Instant? = null,
    val activeOrganizationId: String? = null,
    val lastAuthenticatedAt: Instant? = null,
    val offlineFallback: Boolean = false,
)

@Serializable
data class CentralOrganizationAccess(
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
data class CentralRoleDefinition(
    val roleId: String,
    val organizationId: String,
    val name: String,
    val permissions: Set<String> = emptySet(),
    val systemRole: Boolean = false,
    val updatedAt: Instant,
)

@Serializable
data class CentralUserWorkspaceProfile(
    val userId: String,
    val displayName: String,
    val email: String,
    val locale: String,
    val timeZone: String,
    val activeOrganizationId: String? = null,
    val updatedAt: Instant,
)

@Serializable
data class CentralLocation(
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
data class CentralDepartment(
    val departmentId: String,
    val organizationId: String,
    val name: String,
    val parentDepartmentId: String? = null,
    val active: Boolean = true,
    val updatedAt: Instant,
)

@Serializable
data class CentralCostCenter(
    val costCenterId: String,
    val organizationId: String,
    val code: String,
    val name: String,
    val active: Boolean = true,
    val updatedAt: Instant,
)

@Serializable
data class CentralBankAccount(
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
data class CentralOrganizationParty(
    val partyId: String,
    val organizationId: String,
    val partyType: String,
    val name: String,
    val registrationNumber: String? = null,
    val active: Boolean = true,
    val updatedAt: Instant,
)

@Serializable
data class CentralPermissionDefinition(
    val permissionId: String,
    val name: String,
    val description: String,
    val scope: String,
    val updatedAt: Instant,
)

@Serializable
data class CentralOrganizationDashboardSummary(
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
data class CentralRelayNode(
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
data class CentralOrganizationWorkspaceSnapshot(
    val organizationId: String,
    val userProfile: CentralUserWorkspaceProfile? = null,
    val dashboard: CentralOrganizationDashboardSummary? = null,
    val locations: List<CentralLocation> = emptyList(),
    val departments: List<CentralDepartment> = emptyList(),
    val costCenters: List<CentralCostCenter> = emptyList(),
    val bankAccounts: List<CentralBankAccount> = emptyList(),
    val parties: List<CentralOrganizationParty> = emptyList(),
    val permissionDefinitions: List<CentralPermissionDefinition> = emptyList(),
    val relayNodes: List<CentralRelayNode> = emptyList(),
    val fetchedAt: Instant,
)

@Serializable
data class CentralPendingChange(
    val changeId: String,
    val organizationId: String,
    val aggregateId: String,
    val aggregateType: String,
    val changeType: String,
    val createdAt: Instant,
    val baseRevision: Long,
    val status: CentralChangeStatus = CentralChangeStatus.PENDING,
)

@Serializable
data class CentralConflict(
    val conflictId: String,
    val organizationId: String,
    val aggregateId: String,
    val aggregateType: String,
    val localRevision: Long,
    val canonicalRevision: Long,
    val detectedAt: Instant,
    val status: CentralChangeStatus = CentralChangeStatus.CONFLICT,
    val note: String? = null,
)

@Serializable
data class CentralSyncState(
    val organizationId: String,
    val connectivityMode: CentralConnectivityMode = CentralConnectivityMode.MESH_ONLY,
    val runState: CentralSyncRunState = CentralSyncRunState.PAUSED,
    val lastUploadedSequence: Long = 0,
    val lastPulledCursor: Long = 0,
    val pendingChanges: Int = 0,
    val conflicts: Int = 0,
    val lastSyncAt: Instant? = null,
    val lastError: String? = null,
)

@Serializable
data class CentralAttachmentArtifact(
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
data class CentralExportTask(
    val exportTaskId: String,
    val organizationId: String,
    val sessionId: String,
    val format: String,
    val status: String,
    val requestedAt: Instant,
    val artifactRef: String? = null,
)

@Serializable
data class CentralHybridState(
    val connectivityMode: CentralConnectivityMode,
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

@Serializable
data class CentralLoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class CentralLoginResult(
    val session: CentralAuthSession,
)

@Serializable
data class CentralConflictResolution(
    val conflictId: String,
    val resolutionNote: String,
    val winningChangeId: String? = null,
)
