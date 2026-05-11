package org.expert.link.mesh.backend.internal

import org.expert.link.mesh.contract.api.MeshCentralLoginCommand
import org.expert.link.mesh.contract.api.MeshResolveCentralConflictCommand
import org.expert.link.mesh.contract.model.MeshCentralAttachmentArtifact
import org.expert.link.mesh.contract.model.MeshCentralAuthState
import org.expert.link.mesh.contract.model.MeshCentralBankAccount
import org.expert.link.mesh.contract.model.MeshCentralChangeStatus
import org.expert.link.mesh.contract.model.MeshCentralConflict
import org.expert.link.mesh.contract.model.MeshCentralConnectivityMode
import org.expert.link.mesh.contract.model.MeshCentralCostCenter
import org.expert.link.mesh.contract.model.MeshCentralDepartment
import org.expert.link.mesh.contract.model.MeshCentralExportTask
import org.expert.link.mesh.contract.model.MeshCentralHybridState
import org.expert.link.mesh.contract.model.MeshCentralLocation
import org.expert.link.mesh.contract.model.MeshCentralOrganizationDashboardSummary
import org.expert.link.mesh.contract.model.MeshCentralOrganizationAccess
import org.expert.link.mesh.contract.model.MeshCentralOrganizationParty
import org.expert.link.mesh.contract.model.MeshCentralOrganizationWorkspaceSnapshot
import org.expert.link.mesh.contract.model.MeshCentralPendingChange
import org.expert.link.mesh.contract.model.MeshCentralPermissionDefinition
import org.expert.link.mesh.contract.model.MeshCentralRelayNode
import org.expert.link.mesh.contract.model.MeshCentralSyncRunState
import org.expert.link.mesh.contract.model.MeshCentralSyncStatus
import org.expert.link.mesh.contract.model.MeshCentralUserProfile
import org.expert.link.mesh.contract.model.MeshCentralUserWorkspaceProfile
import org.expert.link.mesh.domain.model.hybrid.CentralAttachmentArtifact
import org.expert.link.mesh.domain.model.hybrid.CentralAuthState
import org.expert.link.mesh.domain.model.hybrid.CentralBankAccount
import org.expert.link.mesh.domain.model.hybrid.CentralConflict
import org.expert.link.mesh.domain.model.hybrid.CentralConflictResolution
import org.expert.link.mesh.domain.model.hybrid.CentralConnectivityMode
import org.expert.link.mesh.domain.model.hybrid.CentralCostCenter
import org.expert.link.mesh.domain.model.hybrid.CentralDepartment
import org.expert.link.mesh.domain.model.hybrid.CentralExportTask
import org.expert.link.mesh.domain.model.hybrid.CentralHybridState
import org.expert.link.mesh.domain.model.hybrid.CentralLoginRequest
import org.expert.link.mesh.domain.model.hybrid.CentralLocation
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationDashboardSummary
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationAccess
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationParty
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationWorkspaceSnapshot
import org.expert.link.mesh.domain.model.hybrid.CentralPendingChange
import org.expert.link.mesh.domain.model.hybrid.CentralPermissionDefinition
import org.expert.link.mesh.domain.model.hybrid.CentralRelayNode
import org.expert.link.mesh.domain.model.hybrid.CentralSyncRunState
import org.expert.link.mesh.domain.model.hybrid.CentralSyncState
import org.expert.link.mesh.domain.model.hybrid.CentralUserProfile
import org.expert.link.mesh.domain.model.hybrid.CentralUserWorkspaceProfile

internal fun MeshCentralLoginCommand.toDomain(): CentralLoginRequest = CentralLoginRequest(
    username = username,
    password = password,
)

internal fun MeshResolveCentralConflictCommand.toDomain(): CentralConflictResolution = CentralConflictResolution(
    conflictId = conflictId,
    resolutionNote = resolutionNote,
    winningChangeId = winningChangeId,
)

internal fun CentralUserProfile.toContract(): MeshCentralUserProfile = MeshCentralUserProfile(
    userId = userId,
    username = username,
    displayName = displayName,
    email = email,
    updatedAt = updatedAt,
)

internal fun CentralAuthState.toContract(): MeshCentralAuthState = MeshCentralAuthState(
    authenticated = authenticated,
    userProfile = userProfile?.toContract(),
    accessTokenExpiresAt = accessTokenExpiresAt,
    activeOrganizationId = activeOrganizationId,
    lastAuthenticatedAt = lastAuthenticatedAt,
    offlineFallback = offlineFallback,
)

internal fun CentralOrganizationAccess.toContract(): MeshCentralOrganizationAccess = MeshCentralOrganizationAccess(
    organizationId = organizationId,
    name = name,
    legalName = legalName,
    membershipId = membershipId,
    memberUserId = memberUserId,
    roleIds = roleIds,
    permissions = permissions,
    active = active,
    updatedAt = updatedAt,
)

internal fun CentralUserWorkspaceProfile.toContract(): MeshCentralUserWorkspaceProfile = MeshCentralUserWorkspaceProfile(
    userId = userId,
    displayName = displayName,
    email = email,
    locale = locale,
    timeZone = timeZone,
    activeOrganizationId = activeOrganizationId,
    updatedAt = updatedAt,
)

internal fun CentralLocation.toContract(): MeshCentralLocation = MeshCentralLocation(
    locationId = locationId,
    organizationId = organizationId,
    name = name,
    type = type,
    parentLocationId = parentLocationId,
    code = code,
    active = active,
    updatedAt = updatedAt,
)

internal fun CentralDepartment.toContract(): MeshCentralDepartment = MeshCentralDepartment(
    departmentId = departmentId,
    organizationId = organizationId,
    name = name,
    parentDepartmentId = parentDepartmentId,
    active = active,
    updatedAt = updatedAt,
)

internal fun CentralCostCenter.toContract(): MeshCentralCostCenter = MeshCentralCostCenter(
    costCenterId = costCenterId,
    organizationId = organizationId,
    code = code,
    name = name,
    active = active,
    updatedAt = updatedAt,
)

internal fun CentralBankAccount.toContract(): MeshCentralBankAccount = MeshCentralBankAccount(
    bankAccountId = bankAccountId,
    organizationId = organizationId,
    bankName = bankName,
    bic = bic,
    accountNumber = accountNumber,
    correspondentAccount = correspondentAccount,
    active = active,
    updatedAt = updatedAt,
)

internal fun CentralOrganizationParty.toContract(): MeshCentralOrganizationParty = MeshCentralOrganizationParty(
    partyId = partyId,
    organizationId = organizationId,
    partyType = partyType,
    name = name,
    registrationNumber = registrationNumber,
    active = active,
    updatedAt = updatedAt,
)

internal fun CentralPermissionDefinition.toContract(): MeshCentralPermissionDefinition = MeshCentralPermissionDefinition(
    permissionId = permissionId,
    name = name,
    description = description,
    scope = scope,
    updatedAt = updatedAt,
)

internal fun CentralOrganizationDashboardSummary.toContract(): MeshCentralOrganizationDashboardSummary = MeshCentralOrganizationDashboardSummary(
    organizationId = organizationId,
    memberships = memberships,
    roles = roles,
    locations = locations,
    bankAccounts = bankAccounts,
    departments = departments,
    costCenters = costCenters,
    parties = parties,
)

internal fun CentralRelayNode.toContract(): MeshCentralRelayNode = MeshCentralRelayNode(
    nodeId = nodeId,
    organizationId = organizationId,
    userId = userId,
    deviceId = deviceId,
    relayEndpoint = relayEndpoint,
    endpointCandidates = endpointCandidates,
    capabilities = capabilities,
    active = active,
    online = online,
    signalingReady = signalingReady,
    registeredAt = registeredAt,
    lastSeenAt = lastSeenAt,
    presenceExpiresAt = presenceExpiresAt,
)

internal fun CentralOrganizationWorkspaceSnapshot.toContract(): MeshCentralOrganizationWorkspaceSnapshot = MeshCentralOrganizationWorkspaceSnapshot(
    organizationId = organizationId,
    userProfile = userProfile?.toContract(),
    dashboard = dashboard?.toContract(),
    locations = locations.map { it.toContract() },
    departments = departments.map { it.toContract() },
    costCenters = costCenters.map { it.toContract() },
    bankAccounts = bankAccounts.map { it.toContract() },
    parties = parties.map { it.toContract() },
    permissionDefinitions = permissionDefinitions.map { it.toContract() },
    relayNodes = relayNodes.map { it.toContract() },
    fetchedAt = fetchedAt,
)

internal fun CentralPendingChange.toContract(): MeshCentralPendingChange = MeshCentralPendingChange(
    changeId = changeId,
    organizationId = organizationId,
    aggregateId = aggregateId,
    aggregateType = aggregateType,
    changeType = changeType,
    createdAt = createdAt,
    baseRevision = baseRevision,
    status = status.toContract(),
)

internal fun CentralConflict.toContract(): MeshCentralConflict = MeshCentralConflict(
    conflictId = conflictId,
    organizationId = organizationId,
    aggregateId = aggregateId,
    aggregateType = aggregateType,
    localRevision = localRevision,
    canonicalRevision = canonicalRevision,
    detectedAt = detectedAt,
    status = status.toContract(),
    note = note,
)

internal fun CentralSyncState.toContract(): MeshCentralSyncStatus = MeshCentralSyncStatus(
    organizationId = organizationId,
    connectivityMode = connectivityMode.toContract(),
    runState = runState.toContract(),
    lastUploadedSequence = lastUploadedSequence,
    lastPulledCursor = lastPulledCursor,
    pendingChanges = pendingChanges,
    conflicts = conflicts,
    lastSyncAt = lastSyncAt,
    lastError = lastError,
)

internal fun CentralAttachmentArtifact.toContract(): MeshCentralAttachmentArtifact = MeshCentralAttachmentArtifact(
    attachmentId = attachmentId,
    organizationId = organizationId,
    aggregateId = aggregateId,
    aggregateType = aggregateType,
    fileName = fileName,
    contentType = contentType,
    sizeBytes = sizeBytes,
    artifactRef = artifactRef,
    createdAt = createdAt,
)

internal fun CentralExportTask.toContract(): MeshCentralExportTask = MeshCentralExportTask(
    exportTaskId = exportTaskId,
    organizationId = organizationId,
    sessionId = sessionId,
    format = format,
    status = status,
    requestedAt = requestedAt,
    artifactRef = artifactRef,
)

internal fun CentralHybridState.toContract(): MeshCentralHybridState = MeshCentralHybridState(
    connectivityMode = connectivityMode.toContract(),
    meshReady = meshReady,
    centralConfigured = centralConfigured,
    authenticated = authenticated,
    activeOrganizationId = activeOrganizationId,
    pendingChanges = pendingChanges,
    conflicts = conflicts,
    syncRunning = syncRunning,
    lastSyncAt = lastSyncAt,
    lastError = lastError,
)

private fun org.expert.link.mesh.domain.model.hybrid.CentralChangeStatus.toContract(): MeshCentralChangeStatus =
    when (this) {
        org.expert.link.mesh.domain.model.hybrid.CentralChangeStatus.PENDING -> MeshCentralChangeStatus.PENDING
        org.expert.link.mesh.domain.model.hybrid.CentralChangeStatus.ACCEPTED -> MeshCentralChangeStatus.ACCEPTED
        org.expert.link.mesh.domain.model.hybrid.CentralChangeStatus.REJECTED -> MeshCentralChangeStatus.REJECTED
        org.expert.link.mesh.domain.model.hybrid.CentralChangeStatus.CONFLICT -> MeshCentralChangeStatus.CONFLICT
    }

private fun CentralConnectivityMode.toContract(): MeshCentralConnectivityMode =
    when (this) {
        CentralConnectivityMode.OFFLINE -> MeshCentralConnectivityMode.OFFLINE
        CentralConnectivityMode.MESH_ONLY -> MeshCentralConnectivityMode.MESH_ONLY
        CentralConnectivityMode.CENTRAL_AVAILABLE -> MeshCentralConnectivityMode.CENTRAL_AVAILABLE
        CentralConnectivityMode.CENTRAL_DEGRADED -> MeshCentralConnectivityMode.CENTRAL_DEGRADED
        CentralConnectivityMode.RECONNECTING -> MeshCentralConnectivityMode.RECONNECTING
        CentralConnectivityMode.CONFLICT_REVIEW_REQUIRED -> MeshCentralConnectivityMode.CONFLICT_REVIEW_REQUIRED
    }

private fun CentralSyncRunState.toContract(): MeshCentralSyncRunState =
    when (this) {
        CentralSyncRunState.PAUSED -> MeshCentralSyncRunState.PAUSED
        CentralSyncRunState.IDLE -> MeshCentralSyncRunState.IDLE
        CentralSyncRunState.RUNNING -> MeshCentralSyncRunState.RUNNING
        CentralSyncRunState.ERROR -> MeshCentralSyncRunState.ERROR
    }
