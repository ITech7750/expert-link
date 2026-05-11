package org.expert.link.mesh.application.service

import java.io.File
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.model.hybrid.CentralAttachmentArtifact
import org.expert.link.mesh.domain.model.hybrid.CentralAuthState
import org.expert.link.mesh.domain.model.hybrid.CentralAuthSession
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationWorkspaceSnapshot
import org.expert.link.mesh.domain.model.hybrid.CentralChangeStatus
import org.expert.link.mesh.domain.model.hybrid.CentralConflict
import org.expert.link.mesh.domain.model.hybrid.CentralConflictResolution
import org.expert.link.mesh.domain.model.hybrid.CentralConnectivityMode
import org.expert.link.mesh.domain.model.hybrid.CentralExportTask
import org.expert.link.mesh.domain.model.hybrid.CentralHybridState
import org.expert.link.mesh.domain.model.hybrid.CentralLoginRequest
import org.expert.link.mesh.domain.model.hybrid.CentralNodeConfiguration
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationAccess
import org.expert.link.mesh.domain.model.hybrid.CentralPendingChange
import org.expert.link.mesh.domain.model.hybrid.CentralRoleDefinition
import org.expert.link.mesh.domain.model.hybrid.CentralSyncRunState
import org.expert.link.mesh.domain.model.hybrid.CentralSyncState
import org.expert.link.mesh.domain.model.inventory.InventoryAttachment
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEvent
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryExportFormat
import org.expert.link.mesh.domain.model.inventory.InventoryExportStatus
import org.expert.link.mesh.domain.model.inventory.InventoryLocation
import org.expert.link.mesh.domain.model.inventory.InventoryLocationType
import org.expert.link.mesh.domain.model.inventory.InventoryDepartment
import org.expert.link.mesh.domain.model.inventory.InventoryCostCenter
import org.expert.link.mesh.domain.model.inventory.InventoryExportTask as LocalExportTask
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.Organization
import org.expert.link.mesh.domain.model.inventory.OrganizationMember
import org.expert.link.mesh.domain.model.inventory.OrganizationMemberStatus
import org.expert.link.mesh.domain.model.inventory.Role
import org.expert.link.mesh.domain.port.external.CentralAttachmentClientPort
import org.expert.link.mesh.domain.port.external.CentralAuthClientPort
import org.expert.link.mesh.domain.port.external.CentralExportClientPort
import org.expert.link.mesh.domain.port.external.CentralInventoryClientPort
import org.expert.link.mesh.domain.port.external.CentralOrganizationAccessClientPort
import org.expert.link.mesh.domain.port.external.CentralRelayClientPort
import org.expert.link.mesh.domain.port.external.CentralSyncClientPort
import org.expert.link.mesh.domain.port.repository.CentralAuthSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.CentralOrganizationAccessRepositoryPort
import org.expert.link.mesh.domain.port.repository.CentralOrganizationWorkspaceRepositoryPort
import org.expert.link.mesh.domain.port.repository.CentralSyncStateRepositoryPort
import org.expert.link.mesh.domain.port.repository.FileTransferRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryAttachmentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryConflictRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCostCenterRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryDepartmentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryExportRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLocationRepositoryPort
import org.expert.link.mesh.domain.port.repository.OrganizationMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.OrganizationRepositoryPort
import org.expert.link.mesh.domain.port.repository.RoleRepositoryPort

class ConnectivityModeService(
    private val configuration: CentralNodeConfiguration?,
    private val syncStateRepositoryPort: CentralSyncStateRepositoryPort,
) {
    suspend fun state(organizationId: String): CentralSyncState =
        syncStateRepositoryPort.findByOrganizationId(organizationId)
            ?: CentralSyncState(
                organizationId = organizationId,
                connectivityMode = if (configuration == null) {
                    CentralConnectivityMode.MESH_ONLY
                } else {
                    CentralConnectivityMode.RECONNECTING
                },
                runState = if (configuration == null) CentralSyncRunState.PAUSED else CentralSyncRunState.IDLE,
            )

    suspend fun markRunning(organizationId: String): CentralSyncState =
        syncStateRepositoryPort.save(state(organizationId).copy(runState = CentralSyncRunState.RUNNING))

    suspend fun markAvailable(
        organizationId: String,
        pendingChanges: Int,
        conflicts: Int,
        lastUploadedSequence: Long,
        lastPulledCursor: Long,
        lastError: String? = null,
    ): CentralSyncState {
        val mode = if (conflicts > 0) {
            CentralConnectivityMode.CONFLICT_REVIEW_REQUIRED
        } else {
            CentralConnectivityMode.CENTRAL_AVAILABLE
        }
        return syncStateRepositoryPort.save(
            state(organizationId).copy(
                connectivityMode = mode,
                runState = CentralSyncRunState.IDLE,
                pendingChanges = pendingChanges,
                conflicts = conflicts,
                lastUploadedSequence = lastUploadedSequence,
                lastPulledCursor = lastPulledCursor,
                lastSyncAt = now(),
                lastError = lastError,
            ),
        )
    }

    suspend fun markError(organizationId: String, message: String): CentralSyncState =
        syncStateRepositoryPort.save(
            state(organizationId).copy(
                connectivityMode = if (configuration == null) {
                    CentralConnectivityMode.MESH_ONLY
                } else {
                    CentralConnectivityMode.CENTRAL_DEGRADED
                },
                runState = CentralSyncRunState.ERROR,
                lastError = message,
            ),
        )
}

class SyncQueueService(
    private val inventoryEventRepositoryPort: InventoryEventRepositoryPort,
    private val syncStateRepositoryPort: CentralSyncStateRepositoryPort,
) {
    suspend fun pendingEvents(organizationId: String): List<InventoryEvent> {
        val state = syncStateRepositoryPort.findByOrganizationId(organizationId)
        val lastUploadedSequence = state?.lastUploadedSequence ?: 0L
        return if (lastUploadedSequence <= 0L) {
            inventoryEventRepositoryPort.listByOrganization(organizationId)
        } else {
            inventoryEventRepositoryPort.listByOrganizationSinceSequence(organizationId, lastUploadedSequence)
        }
    }

    suspend fun pendingChanges(organizationId: String, limit: Int = 100): List<CentralPendingChange> =
        pendingEvents(organizationId)
            .takeLast(limit)
            .map { event ->
                CentralPendingChange(
                    changeId = event.eventId,
                    organizationId = event.organizationId,
                    aggregateId = event.entityId,
                    aggregateType = event.entityType.name,
                    changeType = event.eventType.name,
                    createdAt = event.occurredAt,
                    baseRevision = event.previousEntityRevision ?: ((event.entityRevision ?: 1) - 1).coerceAtLeast(0),
                    status = if (event.conflict) CentralChangeStatus.CONFLICT else CentralChangeStatus.PENDING,
                )
            }

    suspend fun pendingCount(organizationId: String): Int = pendingEvents(organizationId).size
}

class CentralAuthService(
    private val configuration: CentralNodeConfiguration?,
    private val authClientPort: CentralAuthClientPort?,
    private val authSessionRepositoryPort: CentralAuthSessionRepositoryPort,
) {
    suspend fun login(request: CentralLoginRequest): CentralAuthState {
        require(configuration != null && authClientPort != null) { "Central backend не настроен" }
        val result = authClientPort.login(request)
        val session = result.session
        authSessionRepositoryPort.save(session)
        return session.toState()
    }

    suspend fun refresh(): CentralAuthState? {
        val current = authSessionRepositoryPort.current() ?: return null
        if (authClientPort == null) {
            return current.toState(offlineFallback = true)
        }
        val refreshToken = current.refreshToken ?: return current.toState(offlineFallback = true)
        val refreshed = authClientPort.refresh(refreshToken)?.session ?: return current.toState(offlineFallback = true)
        authSessionRepositoryPort.save(refreshed)
        return refreshed.toState()
    }

    suspend fun logout() {
        authSessionRepositoryPort.clear()
    }

    suspend fun authState(): CentralAuthState? =
        authSessionRepositoryPort.current()?.toState(offlineFallback = authClientPort == null)
}

class CentralOrganizationAccessService(
    private val localProfileService: LocalProfileService,
    private val organizationAccessClientPort: CentralOrganizationAccessClientPort?,
    private val authSessionRepositoryPort: CentralAuthSessionRepositoryPort,
    private val centralOrganizationAccessRepositoryPort: CentralOrganizationAccessRepositoryPort,
    private val organizationRepositoryPort: OrganizationRepositoryPort,
    private val organizationMemberRepositoryPort: OrganizationMemberRepositoryPort,
    private val roleRepositoryPort: RoleRepositoryPort,
) {
    suspend fun refreshFromCentral(): List<CentralOrganizationAccess> {
        if (organizationAccessClientPort == null) {
            return centralOrganizationAccessRepositoryPort.list()
        }
        val session = authSessionRepositoryPort.current()
        val currentUserId = session?.userProfile?.userId
        val currentDisplayName = session?.userProfile?.displayName ?: "Central user"
        val localPeerId = runCatching { localProfileService.require().peerId }.getOrNull() ?: currentUserId
        val organizations = organizationAccessClientPort.organizations()
        val merged = organizations.map { organization ->
            val memberships = organizationAccessClientPort.memberships(organization.organizationId)
            val membership = memberships.firstOrNull { it.memberUserId == currentUserId } ?: memberships.firstOrNull()
            val roles = organizationAccessClientPort.roles(organization.organizationId)
            roles.forEach { roleRepositoryPort.save(it.toLocalRole()) }
            val permissions = roles
                .filter { membership?.roleIds?.contains(it.roleId) == true }
                .flatMap { it.permissions }
                .toSet()
            val access = organization.copy(
                membershipId = membership?.membershipId,
                memberUserId = membership?.memberUserId,
                roleIds = membership?.roleIds ?: emptySet(),
                permissions = permissions,
                active = organization.active || organization.organizationId == session?.activeOrganizationId,
            )
            centralOrganizationAccessRepositoryPort.save(access)
            organizationRepositoryPort.save(
                Organization(
                    organizationId = access.organizationId,
                    name = access.name,
                    description = access.legalName,
                    createdByPeerId = "central-backend",
                    createdAt = access.updatedAt,
                    updatedAt = access.updatedAt,
                ),
            )
            localPeerId?.let { peerId ->
                organizationMemberRepositoryPort.save(
                    OrganizationMember(
                        organizationId = access.organizationId,
                        peerId = peerId,
                        displayName = currentDisplayName,
                        roleIds = access.roleIds,
                        status = OrganizationMemberStatus.ACTIVE,
                        joinedAt = access.updatedAt,
                        updatedAt = access.updatedAt,
                    ),
                )
            }
            access
        }
        val activeOrganization = merged.firstOrNull { it.active } ?: merged.firstOrNull()
        activeOrganization?.let { centralOrganizationAccessRepositoryPort.setActiveOrganization(it.organizationId) }
        return centralOrganizationAccessRepositoryPort.list()
    }

    suspend fun organizations(): List<CentralOrganizationAccess> = centralOrganizationAccessRepositoryPort.list()

    suspend fun selectActiveOrganization(organizationId: String): CentralOrganizationAccess? {
        centralOrganizationAccessRepositoryPort.setActiveOrganization(organizationId)
        val current = authSessionRepositoryPort.current()
        if (current != null) {
            authSessionRepositoryPort.save(current.copy(activeOrganizationId = organizationId))
        }
        return centralOrganizationAccessRepositoryPort.findByOrganizationId(organizationId)
    }

    suspend fun activeOrganizationId(): String? =
        centralOrganizationAccessRepositoryPort.activeOrganizationId()
            ?: authSessionRepositoryPort.current()?.activeOrganizationId
}

class CentralOrganizationWorkspaceService(
    private val organizationAccessClientPort: CentralOrganizationAccessClientPort?,
    private val relayClientPort: CentralRelayClientPort?,
    private val authSessionRepositoryPort: CentralAuthSessionRepositoryPort,
    private val workspaceRepositoryPort: CentralOrganizationWorkspaceRepositoryPort,
    private val locationRepositoryPort: InventoryLocationRepositoryPort,
    private val departmentRepositoryPort: InventoryDepartmentRepositoryPort,
    private val costCenterRepositoryPort: InventoryCostCenterRepositoryPort,
) {
    suspend fun snapshot(organizationId: String): CentralOrganizationWorkspaceSnapshot? =
        workspaceRepositoryPort.findByOrganizationId(organizationId)

    suspend fun refreshFromCentral(organizationId: String): CentralOrganizationWorkspaceSnapshot? {
        val accessClient = organizationAccessClientPort ?: return workspaceRepositoryPort.findByOrganizationId(organizationId)
        val userId = authSessionRepositoryPort.current()?.userProfile?.userId
        val snapshot = CentralOrganizationWorkspaceSnapshot(
            organizationId = organizationId,
            userProfile = userId?.let { runCatching { accessClient.userProfile(it) }.getOrNull() },
            dashboard = runCatching { accessClient.organizationDashboard(organizationId) }.getOrNull(),
            locations = runCatching { accessClient.locations(organizationId) }.getOrDefault(emptyList()),
            departments = runCatching { accessClient.departments(organizationId) }.getOrDefault(emptyList()),
            costCenters = runCatching { accessClient.costCenters(organizationId) }.getOrDefault(emptyList()),
            bankAccounts = runCatching { accessClient.bankAccounts(organizationId) }.getOrDefault(emptyList()),
            parties = runCatching { accessClient.organizationParties(organizationId) }.getOrDefault(emptyList()),
            permissionDefinitions = runCatching { accessClient.permissionDefinitions() }.getOrDefault(emptyList()),
            relayNodes = runCatching { relayClientPort?.relayNodes(organizationId).orEmpty() }.getOrDefault(emptyList()),
            fetchedAt = now(),
        )
        snapshot.locations.forEach { locationRepositoryPort.save(it.toInventoryLocation()) }
        snapshot.departments.forEach { departmentRepositoryPort.save(it.toInventoryDepartment()) }
        snapshot.costCenters.forEach { costCenterRepositoryPort.save(it.toInventoryCostCenter()) }
        return workspaceRepositoryPort.save(snapshot)
    }
}

class ConflictStateService(
    private val syncClientPort: CentralSyncClientPort?,
    private val inventoryConflictRepositoryPort: InventoryConflictRepositoryPort,
) {
    suspend fun list(organizationId: String): List<CentralConflict> {
        val remote = runCatching { syncClientPort?.conflicts(organizationId) }.getOrNull() ?: emptyList()
        if (remote.isNotEmpty()) {
            remote.forEach { inventoryConflictRepositoryPort.save(it.toInventoryConflict()) }
            return remote
        }
        return inventoryConflictRepositoryPort.listByOrganization(organizationId).map { conflict ->
            CentralConflict(
                conflictId = conflict.conflictId,
                organizationId = conflict.organizationId,
                aggregateId = conflict.entityId,
                aggregateType = conflict.entityType.name,
                localRevision = conflict.localRevision,
                canonicalRevision = conflict.incomingRevision,
                detectedAt = conflict.detectedAt,
                note = conflict.note,
            )
        }
    }

    suspend fun resolve(request: CentralConflictResolution): CentralConflict? =
        syncClientPort?.resolveConflict(request)
}

class OnlineOfflineStateService(
    private val configuration: CentralNodeConfiguration?,
    private val authSessionRepositoryPort: CentralAuthSessionRepositoryPort,
    private val centralOrganizationAccessRepositoryPort: CentralOrganizationAccessRepositoryPort,
    private val centralSyncStateRepositoryPort: CentralSyncStateRepositoryPort,
) {
    suspend fun snapshot(meshReady: Boolean): CentralHybridState {
        val session = authSessionRepositoryPort.current()
        val activeOrganizationId = centralOrganizationAccessRepositoryPort.activeOrganizationId() ?: session?.activeOrganizationId
        val syncState = activeOrganizationId?.let { centralSyncStateRepositoryPort.findByOrganizationId(it) }
        val connectivityMode = when {
            syncState != null -> syncState.connectivityMode
            configuration == null && meshReady -> CentralConnectivityMode.MESH_ONLY
            configuration == null -> CentralConnectivityMode.OFFLINE
            meshReady -> CentralConnectivityMode.RECONNECTING
            else -> CentralConnectivityMode.OFFLINE
        }
        return CentralHybridState(
            connectivityMode = connectivityMode,
            meshReady = meshReady,
            centralConfigured = configuration != null,
            authenticated = session != null,
            activeOrganizationId = activeOrganizationId,
            pendingChanges = syncState?.pendingChanges ?: 0,
            conflicts = syncState?.conflicts ?: 0,
            syncRunning = syncState?.runState == CentralSyncRunState.RUNNING,
            lastSyncAt = syncState?.lastSyncAt,
            lastError = syncState?.lastError,
        )
    }
}

class CentralAttachmentSyncService(
    private val attachmentClientPort: CentralAttachmentClientPort?,
    private val attachmentRepositoryPort: InventoryAttachmentRepositoryPort,
    private val fileTransferRepositoryPort: FileTransferRepositoryPort,
    private val syncQueueService: SyncQueueService,
) {
    suspend fun sync(organizationId: String): List<CentralAttachmentArtifact> {
        val client = attachmentClientPort ?: return emptyList()
        val pendingAttachmentIds = syncQueueService.pendingEvents(organizationId)
            .filter { it.entityType == InventoryEntityType.ATTACHMENT || it.eventType == InventoryEventType.ATTACHMENT_ADDED }
            .map { it.entityId }
            .toSet()
        return pendingAttachmentIds.mapNotNull { attachmentId ->
            val attachment = attachmentRepositoryPort.findByAttachmentId(attachmentId) ?: return@mapNotNull null
            attachment.toUploadPayload(fileTransferRepositoryPort)?.let { payload ->
                client.upload(
                    organizationId = attachment.organizationId,
                    aggregateId = attachment.inventoryItemId,
                    aggregateType = InventoryEntityType.ATTACHMENT.name,
                    fileName = payload.first,
                    contentType = payload.second,
                    bytes = payload.third,
                )
            }
        }
    }

    private suspend fun InventoryAttachment.toUploadPayload(
        fileTransferRepositoryPort: FileTransferRepositoryPort,
    ): Triple<String, String, ByteArray>? {
        val transferId = transferId ?: return null
        val transfer = fileTransferRepositoryPort.findByTransferId(transferId) ?: return null
        if (transfer.status != FileTransferStatus.COMPLETED) return null
        val localPath = transfer.localPath ?: return null
        val file = File(localPath)
        if (!file.exists() || !file.isFile) return null
        return Triple(
            descriptor.fileName,
            descriptor.contentType ?: "application/octet-stream",
            file.readBytes(),
        )
    }
}

class CentralExportIntegrationService(
    private val localProfileService: LocalProfileService,
    private val exportClientPort: CentralExportClientPort?,
    private val exportRepositoryPort: InventoryExportRepositoryPort,
) {
    suspend fun requestExport(
        organizationId: String,
        sessionId: String,
        format: InventoryExportFormat,
    ): CentralExportTask {
        requireNotNull(exportClientPort) { "Central export client не настроен" }
        val export = exportClientPort.create(organizationId, sessionId, format.name)
        val localProfile = localProfileService.require()
        exportRepositoryPort.save(
            LocalExportTask(
                exportTaskId = export.exportTaskId,
                organizationId = export.organizationId,
                sessionId = export.sessionId,
                requestedByPeerId = localProfile.peerId,
                format = format,
                status = export.status.toInventoryExportStatus(),
                createdAt = export.requestedAt,
                updatedAt = export.requestedAt,
            ),
        )
        return export
    }

    suspend fun exports(organizationId: String): List<CentralExportTask> =
        exportRepositoryPort.listByOrganization(organizationId).map { task ->
            CentralExportTask(
                exportTaskId = task.exportTaskId,
                organizationId = task.organizationId,
                sessionId = task.sessionId,
                format = task.format.name,
                status = task.status.name,
                requestedAt = task.createdAt,
                artifactRef = task.resultDescriptor?.fileId,
            )
        }
}

class CentralSyncOrchestrationService(
    private val configuration: CentralNodeConfiguration?,
    private val localProfileService: LocalProfileService,
    private val connectivityModeService: ConnectivityModeService,
    private val syncQueueService: SyncQueueService,
    private val centralOrganizationAccessService: CentralOrganizationAccessService,
    private val centralOrganizationWorkspaceService: CentralOrganizationWorkspaceService,
    private val inventoryClientPort: CentralInventoryClientPort?,
    private val syncClientPort: CentralSyncClientPort?,
    private val attachmentSyncService: CentralAttachmentSyncService,
    private val inventoryEventApplier: InventoryEventApplier,
    private val inventoryConflictRepositoryPort: InventoryConflictRepositoryPort,
    private val organizationRepositoryPort: OrganizationRepositoryPort,
    private val organizationMemberRepositoryPort: OrganizationMemberRepositoryPort,
    private val roleRepositoryPort: RoleRepositoryPort,
    private val itemRepositoryPort: org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort,
    private val sessionRepositoryPort: org.expert.link.mesh.domain.port.repository.InventorySessionRepositoryPort,
    private val centralSyncStateRepositoryPort: CentralSyncStateRepositoryPort,
) {
    suspend fun syncNow(
        organizationId: String,
        forcePull: Boolean = true,
        forcePush: Boolean = true,
    ): CentralSyncState {
        if (configuration == null || syncClientPort == null) {
            return connectivityModeService.markError(organizationId, "Central backend не настроен")
        }
        val config = requireNotNull(configuration)
        val syncClient = requireNotNull(syncClientPort)
        connectivityModeService.markRunning(organizationId)
        return runCatching {
            centralOrganizationAccessService.refreshFromCentral()
            attachmentSyncService.sync(organizationId)
            val localProfile = localProfileService.require()
            val currentState = connectivityModeService.state(organizationId)
            val pendingEvents = if (forcePush) syncQueueService.pendingEvents(organizationId) else emptyList()
            val uploadedSequence = if (pendingEvents.isNotEmpty()) {
                val upload = syncClient.upload(
                    organizationId = organizationId,
                    deviceId = config.deviceId ?: localProfile.peerId,
                    cursor = currentState.lastPulledCursor.takeIf { it > 0L },
                    changes = pendingEvents,
                )
                upload.conflicts.forEach { inventoryConflictRepositoryPort.save(it.toInventoryConflict()) }
                pendingEvents
                    .filter { it.eventId in upload.acknowledgedChangeIds }
                    .maxOfOrNull { it.sequence }
                    ?: currentState.lastUploadedSequence
            } else {
                currentState.lastUploadedSequence
            }
            val pulledCursor = if (forcePull) {
                val pull = syncClient.pull(organizationId, currentState.lastPulledCursor, 200)
                pull.events.forEach { inventoryEventApplier.apply(it) }
                pull.nextCursor
            } else {
                currentState.lastPulledCursor
            }
            refreshCanonicalSnapshot(organizationId)
            val pendingChanges = syncQueueService.pendingCount(organizationId)
            val conflicts = runCatching { syncClient.conflicts(organizationId) }.getOrNull()
                ?.also { items -> items.forEach { inventoryConflictRepositoryPort.save(it.toInventoryConflict()) } }
                ?.size
                ?: inventoryConflictRepositoryPort.listByOrganization(organizationId).size
            connectivityModeService.markAvailable(
                organizationId = organizationId,
                pendingChanges = pendingChanges,
                conflicts = conflicts,
                lastUploadedSequence = uploadedSequence,
                lastPulledCursor = pulledCursor,
            )
        }.getOrElse { error ->
            connectivityModeService.markError(organizationId, error.message ?: "Ошибка central sync")
        }
    }

    suspend fun syncStatus(organizationId: String): CentralSyncState = connectivityModeService.state(organizationId).copy(
        pendingChanges = syncQueueService.pendingCount(organizationId),
        conflicts = runCatching { syncClientPort?.conflicts(organizationId)?.size }
            .getOrNull()
            ?: inventoryConflictRepositoryPort.listByOrganization(organizationId).size,
    )

    private suspend fun refreshCanonicalSnapshot(organizationId: String) {
        centralOrganizationAccessService.refreshFromCentral()
        centralOrganizationWorkspaceService.refreshFromCentral(organizationId)
        inventoryClientPort?.items(organizationId)?.forEach { itemRepositoryPort.save(it.copy(syncStatus = org.expert.link.mesh.domain.model.inventory.InventorySyncStatus.SYNCED)) }
        inventoryClientPort?.sessions(organizationId)?.forEach { sessionRepositoryPort.save(it) }
    }
}

private fun CentralAuthSession.toState(offlineFallback: Boolean = false): CentralAuthState = CentralAuthState(
    authenticated = true,
    userProfile = userProfile,
    accessTokenExpiresAt = accessTokenExpiresAt,
    activeOrganizationId = activeOrganizationId,
    lastAuthenticatedAt = lastAuthenticatedAt,
    offlineFallback = offlineFallback,
)

private fun CentralRoleDefinition.toLocalRole(): Role = Role(
    roleId = roleId,
    organizationId = organizationId,
    name = name,
    description = null,
    permissions = this.permissions.mapNotNull { it.toInventoryPermission() }.toSet(),
    system = systemRole,
    createdAt = updatedAt,
    updatedAt = updatedAt,
)

private fun String.toInventoryPermission(): InventoryPermission? =
    runCatching { InventoryPermission.valueOf(this) }.getOrNull()

private fun org.expert.link.mesh.domain.model.hybrid.CentralLocation.toInventoryLocation(): InventoryLocation = InventoryLocation(
    locationId = locationId,
    organizationId = organizationId,
    name = name,
    parentLocationId = parentLocationId,
    locationType = runCatching { InventoryLocationType.valueOf(type) }.getOrDefault(InventoryLocationType.OTHER),
    code = code,
    createdByPeerId = "central-backend",
    createdAt = updatedAt,
    updatedAt = updatedAt,
)

private fun org.expert.link.mesh.domain.model.hybrid.CentralDepartment.toInventoryDepartment(): InventoryDepartment = InventoryDepartment(
    departmentId = departmentId,
    organizationId = organizationId,
    name = name,
    parentDepartmentId = parentDepartmentId,
    createdByPeerId = "central-backend",
    createdAt = updatedAt,
    updatedAt = updatedAt,
)

private fun org.expert.link.mesh.domain.model.hybrid.CentralCostCenter.toInventoryCostCenter(): InventoryCostCenter = InventoryCostCenter(
    costCenterId = costCenterId,
    organizationId = organizationId,
    code = code,
    name = name,
    createdByPeerId = "central-backend",
    createdAt = updatedAt,
    updatedAt = updatedAt,
)

private fun String.toInventoryExportStatus(): InventoryExportStatus = when (uppercase()) {
    "REQUESTED" -> InventoryExportStatus.REQUESTED
    "IN_PROGRESS" -> InventoryExportStatus.IN_PROGRESS
    "COMPLETED" -> InventoryExportStatus.COMPLETED
    "FAILED" -> InventoryExportStatus.FAILED
    "CANCELLED" -> InventoryExportStatus.CANCELLED
    else -> InventoryExportStatus.REQUESTED
}

private fun CentralConflict.toInventoryConflict(): org.expert.link.mesh.domain.model.inventory.InventoryConflict =
    org.expert.link.mesh.domain.model.inventory.InventoryConflict(
        conflictId = conflictId,
        organizationId = organizationId,
        entityType = runCatching { InventoryEntityType.valueOf(aggregateType) }.getOrElse { InventoryEntityType.ITEM },
        entityId = aggregateId,
        localRevision = localRevision,
        incomingRevision = canonicalRevision,
        detectedAt = detectedAt,
        note = note,
    )
