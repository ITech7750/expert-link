package org.expert.link.mesh.application.service

import io.mockk.coEvery
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.model.filetransfer.TransferDirection
import org.expert.link.mesh.domain.model.hybrid.CentralAttachmentArtifact
import org.expert.link.mesh.domain.model.hybrid.CentralAuthSession
import org.expert.link.mesh.domain.model.hybrid.CentralChangeStatus
import org.expert.link.mesh.domain.model.hybrid.CentralConflict
import org.expert.link.mesh.domain.model.hybrid.CentralConflictResolution
import org.expert.link.mesh.domain.model.hybrid.CentralConnectivityMode
import org.expert.link.mesh.domain.model.hybrid.CentralCostCenter
import org.expert.link.mesh.domain.model.hybrid.CentralDepartment
import org.expert.link.mesh.domain.model.hybrid.CentralExportTask
import org.expert.link.mesh.domain.model.hybrid.CentralLocation
import org.expert.link.mesh.domain.model.hybrid.CentralLoginRequest
import org.expert.link.mesh.domain.model.hybrid.CentralLoginResult
import org.expert.link.mesh.domain.model.hybrid.CentralNodeConfiguration
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationAccess
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationDashboardSummary
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationParty
import org.expert.link.mesh.domain.model.hybrid.CentralPermissionDefinition
import org.expert.link.mesh.domain.model.hybrid.CentralRelayNode
import org.expert.link.mesh.domain.model.hybrid.CentralRoleDefinition
import org.expert.link.mesh.domain.model.hybrid.CentralSyncRunState
import org.expert.link.mesh.domain.model.hybrid.CentralSyncState
import org.expert.link.mesh.domain.model.hybrid.CentralUserProfile
import org.expert.link.mesh.domain.model.hybrid.CentralUserWorkspaceProfile
import org.expert.link.mesh.domain.model.hybrid.CentralBankAccount
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.inventory.InventoryAttachment
import org.expert.link.mesh.domain.model.inventory.InventoryAttachmentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryAttachmentType
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEvent
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryExportFormat
import org.expert.link.mesh.domain.model.inventory.InventoryItem
import org.expert.link.mesh.domain.model.inventory.InventoryItemSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.domain.model.security.CryptoMaterialRef
import org.expert.link.mesh.domain.model.security.CryptoStorageType
import org.expert.link.mesh.domain.port.external.CentralAttachmentClientPort
import org.expert.link.mesh.domain.port.external.CentralAuthClientPort
import org.expert.link.mesh.domain.port.external.CentralExportClientPort
import org.expert.link.mesh.domain.port.external.CentralOrganizationAccessClientPort
import org.expert.link.mesh.domain.port.external.CentralRelayClientPort
import org.expert.link.mesh.domain.port.external.CentralSyncClientPort
import org.expert.link.mesh.domain.port.external.CentralSyncPullResult
import org.expert.link.mesh.domain.port.external.CentralSyncUploadResult
import org.expert.link.mesh.infrastructure.repository.InMemoryCentralAuthSessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCentralOrganizationAccessRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCentralOrganizationWorkspaceRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCentralSyncStateRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryFileTransferRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAttachmentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryConflictRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryExportRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryItemRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCostCenterRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryDepartmentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLocationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventorySessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryRoleRepositoryAdapter
import org.junit.jupiter.api.Test

class CentralHybridServicesTest {
    @Test
    fun `central auth login stores session and exposes auth state`() = runTest {
        val authRepo = InMemoryCentralAuthSessionRepositoryAdapter()
        val service = CentralAuthService(
            configuration = CentralNodeConfiguration(baseUrl = "http://central"),
            authClientPort = object : CentralAuthClientPort {
                override suspend fun login(request: CentralLoginRequest): CentralLoginResult = CentralLoginResult(
                    session = CentralAuthSession(
                        sessionId = "session-1",
                        userProfile = CentralUserProfile(
                            userId = "user-1",
                            username = request.username,
                            displayName = "Alice",
                            updatedAt = Instant.parse("2026-03-01T00:00:00Z"),
                        ),
                        accessToken = "token-1",
                        refreshToken = "refresh-1",
                        activeOrganizationId = "org-1",
                        lastAuthenticatedAt = Instant.parse("2026-03-01T00:00:00Z"),
                    ),
                )

                override suspend fun refresh(refreshToken: String): CentralLoginResult? = null

                override suspend fun currentUser() = null
            },
            authSessionRepositoryPort = authRepo,
        )

        val state = service.login(CentralLoginRequest(username = "alice", password = "demo"))
        val cached = service.authState()

        assertThat(state.authenticated).isTrue()
        assertThat(state.userProfile?.username).isEqualTo("alice")
        assertThat(cached?.activeOrganizationId).isEqualTo("org-1")
        assertThat(authRepo.current()?.accessToken).isEqualTo("token-1")
    }

    @Test
    fun `organization access refresh maps roles and binds membership to local peer`() = runTest {
        val localProfileService = mockk<LocalProfileService>()
        coEvery { localProfileService.require() } returns localProfile(peerId = "peer-local")
        val authRepo = InMemoryCentralAuthSessionRepositoryAdapter()
        authRepo.save(
            CentralAuthSession(
                sessionId = "session-1",
                userProfile = CentralUserProfile(
                    userId = "user-1",
                    username = "alice",
                    displayName = "Alice",
                    updatedAt = Instant.parse("2026-03-01T00:00:00Z"),
                ),
                accessToken = "token-1",
                activeOrganizationId = "org-1",
                lastAuthenticatedAt = Instant.parse("2026-03-01T00:00:00Z"),
            ),
        )
        val centralOrgRepo = InMemoryCentralOrganizationAccessRepositoryAdapter()
        val organizationRepo = InMemoryOrganizationRepositoryAdapter()
        val memberRepo = InMemoryOrganizationMemberRepositoryAdapter()
        val roleRepo = InMemoryRoleRepositoryAdapter()
        val service = CentralOrganizationAccessService(
            localProfileService = localProfileService,
            organizationAccessClientPort = object : CentralOrganizationAccessClientPort {
                override suspend fun organizations(): List<CentralOrganizationAccess> = listOf(
                    CentralOrganizationAccess(
                        organizationId = "org-1",
                        name = "Org One",
                        active = true,
                        updatedAt = Instant.parse("2026-03-01T00:00:00Z"),
                    ),
                )

                override suspend fun memberships(organizationId: String): List<CentralOrganizationAccess> = listOf(
                    CentralOrganizationAccess(
                        organizationId = organizationId,
                        name = "Org One",
                        membershipId = "membership-1",
                        memberUserId = "user-1",
                        roleIds = setOf("role-operator"),
                        active = true,
                        updatedAt = Instant.parse("2026-03-01T00:00:00Z"),
                    ),
                )

                override suspend fun roles(organizationId: String): List<CentralRoleDefinition> = listOf(
                    CentralRoleDefinition(
                        roleId = "role-operator",
                        organizationId = organizationId,
                        name = "Operator",
                        permissions = setOf(InventoryPermission.ITEM_CREATE.name),
                        systemRole = true,
                        updatedAt = Instant.parse("2026-03-01T00:00:00Z"),
                    ),
                )

                override suspend fun activateOrganization(
                    organizationId: String,
                    active: Boolean,
                ): CentralOrganizationAccess? = null

                override suspend fun locations(organizationId: String): List<CentralLocation> = emptyList()

                override suspend fun departments(organizationId: String): List<CentralDepartment> = emptyList()

                override suspend fun costCenters(organizationId: String): List<CentralCostCenter> = emptyList()

                override suspend fun bankAccounts(organizationId: String): List<CentralBankAccount> = emptyList()

                override suspend fun organizationParties(organizationId: String): List<CentralOrganizationParty> = emptyList()

                override suspend fun permissionDefinitions(): List<CentralPermissionDefinition> = emptyList()

                override suspend fun organizationDashboard(organizationId: String): CentralOrganizationDashboardSummary? = null

                override suspend fun userProfile(userId: String): CentralUserWorkspaceProfile? = null
            },
            authSessionRepositoryPort = authRepo,
            centralOrganizationAccessRepositoryPort = centralOrgRepo,
            organizationRepositoryPort = organizationRepo,
            organizationMemberRepositoryPort = memberRepo,
            roleRepositoryPort = roleRepo,
        )

        val organizations = service.refreshFromCentral()
        val localMember = memberRepo.findByOrganizationAndPeer("org-1", "peer-local")
        val localRole = roleRepo.findByRoleId("role-operator")

        assertThat(organizations).hasSize(1)
        assertThat(localMember?.roleIds).contains("role-operator")
        assertThat(localRole?.permissions).contains(InventoryPermission.ITEM_CREATE)
    }

    @Test
    fun `sync queue builds pending changes from inventory events and cursor`() = runTest {
        val eventRepo = InMemoryInventoryEventRepositoryAdapter()
        eventRepo.save(sampleEvent("event-1", sequence = 1, previousRevision = 0))
        eventRepo.save(sampleEvent("event-2", sequence = 2, previousRevision = 1))
        val stateRepo = InMemoryCentralSyncStateRepositoryAdapter()
        val queue = SyncQueueService(eventRepo, stateRepo)

        val pendingWithoutCursor = queue.pendingChanges("org-1")
        stateRepo.save(
            CentralSyncState(
                organizationId = "org-1",
                runState = CentralSyncRunState.IDLE,
                connectivityMode = CentralConnectivityMode.CENTRAL_AVAILABLE,
                lastUploadedSequence = 1,
            ),
        )
        val pendingAfterCursor = queue.pendingChanges("org-1")

        assertThat(pendingWithoutCursor).hasSize(2)
        assertThat(pendingAfterCursor).hasSize(1)
        assertThat(pendingAfterCursor.first().changeId).isEqualTo("event-2")
    }

    @Test
    fun `central sync orchestration marks conflict state when server reports conflict`() = runTest {
        val orgRepo = InMemoryOrganizationRepositoryAdapter()
        val memberRepo = InMemoryOrganizationMemberRepositoryAdapter()
        val roleRepo = InMemoryRoleRepositoryAdapter()
        val eventRepo = InMemoryInventoryEventRepositoryAdapter()
        eventRepo.save(sampleEvent("event-1", sequence = 1, previousRevision = 0))
        val conflictRepo = InMemoryInventoryConflictRepositoryAdapter()
        val syncStateRepo = InMemoryCentralSyncStateRepositoryAdapter()
        val connectivity = ConnectivityModeService(CentralNodeConfiguration(baseUrl = "http://central"), syncStateRepo)
        val queue = SyncQueueService(eventRepo, syncStateRepo)
        val localProfileService = mockk<LocalProfileService>()
        coEvery { localProfileService.require() } returns localProfile("peer-sync")
        val authRepo = InMemoryCentralAuthSessionRepositoryAdapter()
        authRepo.save(
            CentralAuthSession(
                sessionId = "session-1",
                userProfile = CentralUserProfile("user-1", "alice", "Alice", updatedAt = Instant.parse("2026-03-01T00:00:00Z")),
                accessToken = "token-1",
                activeOrganizationId = "org-1",
                lastAuthenticatedAt = Instant.parse("2026-03-01T00:00:00Z"),
            ),
        )
        val centralOrgRepo = InMemoryCentralOrganizationAccessRepositoryAdapter()
        centralOrgRepo.save(
            CentralOrganizationAccess(
                organizationId = "org-1",
                name = "Org",
                active = true,
                updatedAt = Instant.parse("2026-03-01T00:00:00Z"),
            ),
        )
        val organizationAccess = CentralOrganizationAccessService(
            localProfileService = localProfileService,
            organizationAccessClientPort = null,
            authSessionRepositoryPort = authRepo,
            centralOrganizationAccessRepositoryPort = centralOrgRepo,
            organizationRepositoryPort = orgRepo,
            organizationMemberRepositoryPort = memberRepo,
            roleRepositoryPort = roleRepo,
        )
        val workspaceService = CentralOrganizationWorkspaceService(
            organizationAccessClientPort = null,
            relayClientPort = null,
            authSessionRepositoryPort = authRepo,
            workspaceRepositoryPort = InMemoryCentralOrganizationWorkspaceRepositoryAdapter(),
            locationRepositoryPort = InMemoryInventoryLocationRepositoryAdapter(),
            departmentRepositoryPort = InMemoryInventoryDepartmentRepositoryAdapter(),
            costCenterRepositoryPort = InMemoryInventoryCostCenterRepositoryAdapter(),
        )
        val syncClient = object : CentralSyncClientPort {
            override suspend fun upload(
                organizationId: String,
                deviceId: String,
                cursor: Long?,
                changes: List<InventoryEvent>,
            ): CentralSyncUploadResult = CentralSyncUploadResult(
                nextCursor = 2,
                acknowledgedChangeIds = emptySet(),
                conflicts = listOf(
                    CentralConflict(
                        conflictId = "conflict-1",
                        organizationId = organizationId,
                        aggregateId = "item-1",
                        aggregateType = InventoryEntityType.ITEM.name,
                        localRevision = 1,
                        canonicalRevision = 2,
                        detectedAt = Instant.parse("2026-03-02T00:00:00Z"),
                        status = CentralChangeStatus.CONFLICT,
                        note = "stale",
                    ),
                ),
            )

            override suspend fun pull(organizationId: String, afterCursor: Long, limit: Int): CentralSyncPullResult =
                CentralSyncPullResult(nextCursor = afterCursor, events = emptyList())

            override suspend fun conflicts(organizationId: String): List<CentralConflict> = listOf(
                CentralConflict(
                    conflictId = "conflict-1",
                    organizationId = organizationId,
                    aggregateId = "item-1",
                    aggregateType = InventoryEntityType.ITEM.name,
                    localRevision = 1,
                    canonicalRevision = 2,
                    detectedAt = Instant.parse("2026-03-02T00:00:00Z"),
                    status = CentralChangeStatus.CONFLICT,
                    note = "stale",
                ),
            )

            override suspend fun resolveConflict(request: CentralConflictResolution): CentralConflict? = null
        }
        val orchestrator = CentralSyncOrchestrationService(
            configuration = CentralNodeConfiguration(baseUrl = "http://central"),
            localProfileService = localProfileService,
            connectivityModeService = connectivity,
            syncQueueService = queue,
            centralOrganizationAccessService = organizationAccess,
            centralOrganizationWorkspaceService = workspaceService,
            inventoryClientPort = null,
            syncClientPort = syncClient,
            attachmentSyncService = CentralAttachmentSyncService(
                attachmentClientPort = null,
                attachmentRepositoryPort = mockk(relaxed = true),
                fileTransferRepositoryPort = mockk(relaxed = true),
                syncQueueService = queue,
            ),
            inventoryEventApplier = mockk(relaxed = true),
            inventoryConflictRepositoryPort = conflictRepo,
            organizationRepositoryPort = orgRepo,
            organizationMemberRepositoryPort = memberRepo,
            roleRepositoryPort = roleRepo,
            itemRepositoryPort = InMemoryInventoryItemRepositoryAdapter(),
            sessionRepositoryPort = InMemoryInventorySessionRepositoryAdapter(),
            centralSyncStateRepositoryPort = syncStateRepo,
        )

        val state = orchestrator.syncNow("org-1", forcePull = true, forcePush = true)

        assertThat(state.connectivityMode).isEqualTo(CentralConnectivityMode.CONFLICT_REVIEW_REQUIRED)
        assertThat(state.conflicts).isEqualTo(1)
        assertThat(conflictRepo.listByOrganization("org-1")).hasSize(1)
    }

    @Test
    fun `online offline state falls back to mesh-only when central is not configured`() = runTest {
        val stateService = OnlineOfflineStateService(
            configuration = null,
            authSessionRepositoryPort = InMemoryCentralAuthSessionRepositoryAdapter(),
            centralOrganizationAccessRepositoryPort = InMemoryCentralOrganizationAccessRepositoryAdapter(),
            centralSyncStateRepositoryPort = InMemoryCentralSyncStateRepositoryAdapter(),
        )

        val snapshot = stateService.snapshot(meshReady = true)

        assertThat(snapshot.centralConfigured).isFalse()
        assertThat(snapshot.connectivityMode).isEqualTo(CentralConnectivityMode.MESH_ONLY)
    }

    @Test
    fun `workspace refresh loads catalogs and relay nodes into local cache`() = runTest {
        val authRepo = InMemoryCentralAuthSessionRepositoryAdapter()
        val workspaceRepo = InMemoryCentralOrganizationWorkspaceRepositoryAdapter()
        val locationRepo = InMemoryInventoryLocationRepositoryAdapter()
        val departmentRepo = InMemoryInventoryDepartmentRepositoryAdapter()
        val costCenterRepo = InMemoryInventoryCostCenterRepositoryAdapter()
        val updatedAt = Instant.parse("2026-03-05T10:00:00Z")

        authRepo.save(
            CentralAuthSession(
                sessionId = "session-workspace",
                userProfile = CentralUserProfile(
                    userId = "user-1",
                    username = "alice",
                    displayName = "Alice",
                    updatedAt = updatedAt,
                ),
                accessToken = "token",
                activeOrganizationId = "org-1",
                lastAuthenticatedAt = updatedAt,
            ),
        )

        val organizationAccessClient = object : CentralOrganizationAccessClientPort {
            override suspend fun organizations(): List<CentralOrganizationAccess> = emptyList()
            override suspend fun memberships(organizationId: String): List<CentralOrganizationAccess> = emptyList()
            override suspend fun roles(organizationId: String): List<CentralRoleDefinition> = emptyList()
            override suspend fun activateOrganization(organizationId: String, active: Boolean): CentralOrganizationAccess? = null

            override suspend fun locations(organizationId: String): List<CentralLocation> = listOf(
                CentralLocation(
                    locationId = "loc-1",
                    organizationId = organizationId,
                    name = "Склад",
                    type = "STORAGE",
                    updatedAt = updatedAt,
                ),
            )

            override suspend fun departments(organizationId: String): List<CentralDepartment> = listOf(
                CentralDepartment(
                    departmentId = "dep-1",
                    organizationId = organizationId,
                    name = "ИТ",
                    updatedAt = updatedAt,
                ),
            )

            override suspend fun costCenters(organizationId: String): List<CentralCostCenter> = listOf(
                CentralCostCenter(
                    costCenterId = "cc-1",
                    organizationId = organizationId,
                    code = "100",
                    name = "Основной",
                    updatedAt = updatedAt,
                ),
            )

            override suspend fun bankAccounts(organizationId: String): List<CentralBankAccount> = emptyList()
            override suspend fun organizationParties(organizationId: String): List<CentralOrganizationParty> = emptyList()

            override suspend fun permissionDefinitions(): List<CentralPermissionDefinition> = listOf(
                CentralPermissionDefinition(
                    permissionId = "inventory.read",
                    name = "inventory.read",
                    description = "Read inventory",
                    scope = "ORG",
                    updatedAt = updatedAt,
                ),
            )

            override suspend fun organizationDashboard(organizationId: String): CentralOrganizationDashboardSummary = CentralOrganizationDashboardSummary(
                organizationId = organizationId,
                memberships = 10,
                roles = 3,
                locations = 1,
                bankAccounts = 0,
                departments = 1,
                costCenters = 1,
                parties = 0,
            )

            override suspend fun userProfile(userId: String): CentralUserWorkspaceProfile = CentralUserWorkspaceProfile(
                userId = userId,
                displayName = "Alice",
                email = "alice@example.com",
                locale = "ru-RU",
                timeZone = "Europe/Moscow",
                activeOrganizationId = "org-1",
                updatedAt = updatedAt,
            )
        }

        val relayClient = object : CentralRelayClientPort {
            override suspend fun relayNodes(organizationId: String, onlineOnly: Boolean): List<CentralRelayNode> = listOf(
                CentralRelayNode(
                    nodeId = "relay-1",
                    organizationId = organizationId,
                    userId = "user-1",
                    deviceId = "device-1",
                    relayEndpoint = "wss://relay.example/ws",
                    active = true,
                    online = true,
                    signalingReady = true,
                    registeredAt = updatedAt,
                    lastSeenAt = updatedAt,
                ),
            )
        }

        val service = CentralOrganizationWorkspaceService(
            organizationAccessClientPort = organizationAccessClient,
            relayClientPort = relayClient,
            authSessionRepositoryPort = authRepo,
            workspaceRepositoryPort = workspaceRepo,
            locationRepositoryPort = locationRepo,
            departmentRepositoryPort = departmentRepo,
            costCenterRepositoryPort = costCenterRepo,
        )

        val snapshot = service.refreshFromCentral("org-1")

        assertThat(snapshot).isNotNull
        assertThat(snapshot?.locations).hasSize(1)
        assertThat(snapshot?.departments).hasSize(1)
        assertThat(snapshot?.costCenters).hasSize(1)
        assertThat(snapshot?.relayNodes).hasSize(1)
        assertThat(snapshot?.permissionDefinitions).hasSize(1)
        assertThat(locationRepo.listByOrganization("org-1")).hasSize(1)
        assertThat(departmentRepo.listByOrganization("org-1")).hasSize(1)
        assertThat(costCenterRepo.listByOrganization("org-1")).hasSize(1)
    }

    @Test
    fun `attachment and export services keep artifact refs opaque for demo and s3 backends`() = runTest {
        val localProfileService = mockk<LocalProfileService>()
        coEvery { localProfileService.require() } returns localProfile("peer-storage")

        val now = Instant.parse("2026-03-03T00:00:00Z")
        val descriptor = FileDescriptor(
            fileId = "file-1",
            fileName = "photo.jpg",
            sizeBytes = 4,
            sha256 = "hash",
            contentType = "image/jpeg",
        )
        val transferRepo = InMemoryFileTransferRepositoryAdapter()
        val attachmentRepo = InMemoryInventoryAttachmentRepositoryAdapter()
        val eventRepo = InMemoryInventoryEventRepositoryAdapter()
        val stateRepo = InMemoryCentralSyncStateRepositoryAdapter()
        val queue = SyncQueueService(eventRepo, stateRepo)

        val tempFile = File.createTempFile("expert-link-", ".bin").apply { writeText("demo") }
        val transfer = FileTransferSession(
            transferId = "transfer-1",
            descriptor = descriptor,
            senderPeerId = "peer-storage",
            recipientPeerId = "peer-central",
            direction = TransferDirection.OUTGOING,
            status = FileTransferStatus.COMPLETED,
            chunkSizeBytes = 4,
            totalChunks = 1,
            localPath = tempFile.absolutePath,
            createdAt = now,
            updatedAt = now,
        )
        transferRepo.save(transfer)

        val attachment = InventoryAttachment(
            attachmentId = "attachment-1",
            organizationId = "org-1",
            inventoryItemId = "item-1",
            uploadedByPeerId = "peer-storage",
            descriptor = descriptor,
            transferId = transfer.transferId,
            attachmentType = InventoryAttachmentType.PHOTO,
            createdAt = now,
            updatedAt = now,
        )
        attachmentRepo.save(attachment)
        eventRepo.save(
            InventoryEvent(
                eventId = "event-att-1",
                organizationId = "org-1",
                entityType = InventoryEntityType.ATTACHMENT,
                entityId = attachment.attachmentId,
                eventType = InventoryEventType.ATTACHMENT_ADDED,
                actorPeerId = "peer-storage",
                occurredAt = now,
                sequence = 1,
                payload = InventoryAttachmentSnapshot(attachment),
            ),
        )

        val demoRef = "https://central.local/demo/files/attachment-1"
        val s3Ref = "https://s3.example/bucket/attachment-1?sig=abc"

        val demoService = CentralAttachmentSyncService(
            attachmentClientPort = attachmentClientReturning(demoRef, now),
            attachmentRepositoryPort = attachmentRepo,
            fileTransferRepositoryPort = transferRepo,
            syncQueueService = queue,
        )
        val s3Service = CentralAttachmentSyncService(
            attachmentClientPort = attachmentClientReturning(s3Ref, now),
            attachmentRepositoryPort = attachmentRepo,
            fileTransferRepositoryPort = transferRepo,
            syncQueueService = queue,
        )

        val demoArtifact = demoService.sync("org-1").single()
        val s3Artifact = s3Service.sync("org-1").single()
        assertThat(demoArtifact.artifactRef).isEqualTo(demoRef)
        assertThat(s3Artifact.artifactRef).isEqualTo(s3Ref)

        val exportRepo = InMemoryInventoryExportRepositoryAdapter()
        val demoExportService = CentralExportIntegrationService(
            localProfileService = localProfileService,
            exportClientPort = exportClientReturning(demoRef, now),
            exportRepositoryPort = exportRepo,
        )
        val s3ExportService = CentralExportIntegrationService(
            localProfileService = localProfileService,
            exportClientPort = exportClientReturning(s3Ref, now),
            exportRepositoryPort = exportRepo,
        )

        val demoExport = demoExportService.requestExport("org-1", "session-1", InventoryExportFormat.PDF)
        val s3Export = s3ExportService.requestExport("org-1", "session-2", InventoryExportFormat.CSV)
        assertThat(demoExport.artifactRef).isEqualTo(demoRef)
        assertThat(s3Export.artifactRef).isEqualTo(s3Ref)
        assertThat(exportRepo.listByOrganization("org-1")).hasSize(2)

        tempFile.delete()
    }

    private fun attachmentClientReturning(artifactRef: String, createdAt: Instant): CentralAttachmentClientPort =
        object : CentralAttachmentClientPort {
            override suspend fun upload(
                organizationId: String,
                aggregateId: String,
                aggregateType: String,
                fileName: String,
                contentType: String,
                bytes: ByteArray,
            ): CentralAttachmentArtifact = CentralAttachmentArtifact(
                attachmentId = "remote-$aggregateId",
                organizationId = organizationId,
                aggregateId = aggregateId,
                aggregateType = aggregateType,
                fileName = fileName,
                contentType = contentType,
                sizeBytes = bytes.size.toLong(),
                artifactRef = artifactRef,
                createdAt = createdAt,
            )

            override suspend fun findByAttachmentId(attachmentId: String): CentralAttachmentArtifact? = null
        }

    private fun exportClientReturning(artifactRef: String, requestedAt: Instant): CentralExportClientPort =
        object : CentralExportClientPort {
            override suspend fun create(
                organizationId: String,
                sessionId: String,
                format: String,
            ): CentralExportTask = CentralExportTask(
                exportTaskId = "export-$sessionId",
                organizationId = organizationId,
                sessionId = sessionId,
                format = format,
                status = "REQUESTED",
                requestedAt = requestedAt,
                artifactRef = artifactRef,
            )

            override suspend fun findByExportTaskId(exportTaskId: String): CentralExportTask? = null
        }

    private fun localProfile(peerId: String): LocalProfile = LocalProfile(
        peerId = peerId,
        displayName = "peer-$peerId",
        publicKey = "pub-$peerId",
        privateKey = "priv-$peerId",
        keyMaterialRef = CryptoMaterialRef(
            alias = "key-$peerId",
            storageType = CryptoStorageType.IN_MEMORY,
            createdAt = Instant.parse("2026-03-01T00:00:00Z"),
        ),
        capabilities = setOf("chat", "inventory"),
        createdAt = Instant.parse("2026-03-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-03-01T00:00:00Z"),
    )

    private fun sampleEvent(eventId: String, sequence: Long, previousRevision: Long): InventoryEvent {
        val item = InventoryItem(
            inventoryItemId = "item-1",
            organizationId = "org-1",
            inventoryNumber = "INV-1",
            title = "Item",
            createdByPeerId = "peer-1",
            createdAt = Instant.parse("2026-03-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-03-01T00:00:00Z"),
            revision = sequence,
            currentStatus = InventoryStatus.ADDED,
        )
        return InventoryEvent(
            eventId = eventId,
            organizationId = "org-1",
            entityType = InventoryEntityType.ITEM,
            entityId = "item-1",
            eventType = InventoryEventType.UPDATED,
            actorPeerId = "peer-1",
            occurredAt = Instant.parse("2026-03-01T00:00:00Z"),
            sequence = sequence,
            entityRevision = sequence,
            previousEntityRevision = previousRevision,
            payload = InventoryItemSnapshot(item),
        )
    }
}
