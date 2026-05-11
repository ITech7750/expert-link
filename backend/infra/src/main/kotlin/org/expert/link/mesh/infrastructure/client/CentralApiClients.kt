package org.expert.link.mesh.infrastructure.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.expert.link.mesh.domain.model.hybrid.CentralAttachmentArtifact
import org.expert.link.mesh.domain.model.hybrid.CentralAuthSession
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
import org.expert.link.mesh.domain.model.hybrid.CentralNodeConfiguration
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationDashboardSummary
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationAccess
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationParty
import org.expert.link.mesh.domain.model.hybrid.CentralPermissionDefinition
import org.expert.link.mesh.domain.model.hybrid.CentralRelayNode
import org.expert.link.mesh.domain.model.hybrid.CentralRoleDefinition
import org.expert.link.mesh.domain.model.hybrid.CentralUserProfile
import org.expert.link.mesh.domain.model.hybrid.CentralUserWorkspaceProfile
import org.expert.link.mesh.domain.model.inventory.InventoryEvent
import org.expert.link.mesh.domain.model.inventory.InventoryItem
import org.expert.link.mesh.domain.model.inventory.InventoryItemType
import org.expert.link.mesh.domain.model.inventory.InventorySession
import org.expert.link.mesh.domain.model.inventory.InventorySessionReviewStatus
import org.expert.link.mesh.domain.model.inventory.InventorySessionStatus
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.domain.model.inventory.InventorySyncStatus
import org.expert.link.mesh.domain.port.external.CentralAttachmentClientPort
import org.expert.link.mesh.domain.port.external.CentralAuthClientPort
import org.expert.link.mesh.domain.port.external.CentralClientBundle
import org.expert.link.mesh.domain.port.external.CentralExportClientPort
import org.expert.link.mesh.domain.port.external.CentralInventoryClientPort
import org.expert.link.mesh.domain.port.external.CentralOrganizationAccessClientPort
import org.expert.link.mesh.domain.port.external.CentralSyncClientPort
import org.expert.link.mesh.domain.port.external.CentralSyncPullResult
import org.expert.link.mesh.domain.port.external.CentralSyncUploadResult
import org.expert.link.mesh.domain.port.repository.CentralAuthSessionRepositoryPort
import org.expert.link.mesh.domain.port.external.CentralRelayClientPort
import org.expert.link.mesh.domain.port.repository.CentralOrganizationAccessRepositoryPort

private val centralJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

class DefaultCentralClientFactory {
    fun create(
        configuration: CentralNodeConfiguration,
        authSessionRepositoryPort: CentralAuthSessionRepositoryPort,
        organizationAccessRepositoryPort: CentralOrganizationAccessRepositoryPort,
    ): CentralClientBundle {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(centralJson)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = configuration.requestTimeoutMillis
                connectTimeoutMillis = configuration.requestTimeoutMillis
                socketTimeoutMillis = configuration.requestTimeoutMillis
            }
        }
        val tokenProvider: suspend () -> String? = { authSessionRepositoryPort.current()?.accessToken }
        return CentralClientBundle(
            authClientPort = KtorCentralAuthClient(client, configuration, authSessionRepositoryPort),
            organizationAccessClientPort = KtorCentralOrganizationAccessClient(client, configuration, tokenProvider),
            inventoryClientPort = KtorCentralInventoryClient(client, configuration, tokenProvider),
            syncClientPort = KtorCentralSyncClient(client, configuration, tokenProvider),
            attachmentClientPort = KtorCentralAttachmentClient(client, configuration, tokenProvider),
            exportClientPort = KtorCentralExportClient(client, configuration, tokenProvider),
            relayClientPort = KtorCentralRelayClient(client, configuration, tokenProvider),
        )
    }
}

private abstract class BaseCentralClient(
    protected val client: HttpClient,
    configuration: CentralNodeConfiguration,
    private val tokenProvider: (suspend () -> String?)? = null,
) {
    protected val baseUrl: String = configuration.baseUrl.trimEnd('/')

    protected suspend fun authHeader(): Pair<String, String>? {
        val token = tokenProvider?.invoke()?.takeIf { it.isNotBlank() } ?: return null
        return HttpHeaders.Authorization to "Bearer $token"
    }
}

private class KtorCentralAuthClient(
    client: HttpClient,
    configuration: CentralNodeConfiguration,
    private val authSessionRepositoryPort: CentralAuthSessionRepositoryPort,
) : BaseCentralClient(client, configuration), CentralAuthClientPort {
    private val authPath = configuration.authPath.trimStart('/').let { "/$it" }

    override suspend fun login(request: CentralLoginRequest): CentralLoginResult {
        val response = client.post("$baseUrl$authPath/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(username = request.username, password = request.password))
        }.body<LoginResponseDto>()
        val session = response.toDomain()
        authSessionRepositoryPort.save(session)
        return CentralLoginResult(session)
    }

    override suspend fun refresh(refreshToken: String): CentralLoginResult? {
        val response = client.post("$baseUrl$authPath/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequestDto(refreshToken = refreshToken))
        }
        if (response.bodyAsText().isBlank()) {
            return null
        }
        val body = centralJson.decodeFromString(LoginResponseDto.serializer(), response.bodyAsText())
        val session = body.toDomain()
        authSessionRepositoryPort.save(session)
        return CentralLoginResult(session)
    }

    override suspend fun currentUser(): CentralAuthState? {
        val header = authHeader() ?: return authSessionRepositoryPort.current()?.toState(offlineFallback = true)
        val body = client.get("$baseUrl$authPath/me") {
            header(header.first, header.second)
        }.body<UserProfileDto>()
        val current = authSessionRepositoryPort.current()
        return CentralAuthState(
            authenticated = true,
            userProfile = body.toDomain(),
            accessTokenExpiresAt = current?.accessTokenExpiresAt,
            activeOrganizationId = current?.activeOrganizationId,
            lastAuthenticatedAt = current?.lastAuthenticatedAt,
            offlineFallback = false,
        )
    }
}

private class KtorCentralOrganizationAccessClient(
    client: HttpClient,
    configuration: CentralNodeConfiguration,
    tokenProvider: suspend () -> String?,
) : BaseCentralClient(client, configuration, tokenProvider), CentralOrganizationAccessClientPort {
    override suspend fun organizations(): List<CentralOrganizationAccess> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/organizations") {
            header?.let { header(it.first, it.second) }
        }.body<List<OrganizationResponseDto>>().map { it.toDomain() }
    }

    override suspend fun memberships(organizationId: String): List<CentralOrganizationAccess> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/memberships/organization/$organizationId") {
            header?.let { header(it.first, it.second) }
        }.body<List<MembershipResponseDto>>().map { it.toDomain() }
    }

    override suspend fun roles(organizationId: String): List<CentralRoleDefinition> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/roles/organization/$organizationId") {
            header?.let { header(it.first, it.second) }
        }.body<List<RoleResponseDto>>().map { it.toDomain() }
    }

    override suspend fun activateOrganization(organizationId: String, active: Boolean): CentralOrganizationAccess? {
        val header = authHeader()
        return client.put("$baseUrl/api/v1/organizations/$organizationId/active?active=$active") {
            header?.let { header(it.first, it.second) }
        }.body<OrganizationResponseDto>().toDomain()
    }

    override suspend fun locations(organizationId: String): List<CentralLocation> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/locations?organizationId=$organizationId") {
            header?.let { header(it.first, it.second) }
        }.body<List<LocationResponseDto>>().map { it.toDomain() }
    }

    override suspend fun departments(organizationId: String): List<CentralDepartment> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/departments?organizationId=$organizationId") {
            header?.let { header(it.first, it.second) }
        }.body<List<DepartmentResponseDto>>().map { it.toDomain() }
    }

    override suspend fun costCenters(organizationId: String): List<CentralCostCenter> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/cost-centers?organizationId=$organizationId") {
            header?.let { header(it.first, it.second) }
        }.body<List<CostCenterResponseDto>>().map { it.toDomain() }
    }

    override suspend fun bankAccounts(organizationId: String): List<CentralBankAccount> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/bank-accounts?organizationId=$organizationId") {
            header?.let { header(it.first, it.second) }
        }.body<List<BankAccountResponseDto>>().map { it.toDomain() }
    }

    override suspend fun organizationParties(organizationId: String): List<CentralOrganizationParty> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/organization-parties?organizationId=$organizationId") {
            header?.let { header(it.first, it.second) }
        }.body<List<OrganizationPartyResponseDto>>().map { it.toDomain() }
    }

    override suspend fun permissionDefinitions(): List<CentralPermissionDefinition> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/permissions") {
            header?.let { header(it.first, it.second) }
        }.body<List<PermissionDefinitionResponseDto>>().map { it.toDomain() }
    }

    override suspend fun organizationDashboard(organizationId: String): CentralOrganizationDashboardSummary? {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/organizations/$organizationId/dashboard") {
            header?.let { header(it.first, it.second) }
        }.body<OrganizationDashboardSummaryResponseDto>().toDomain()
    }

    override suspend fun userProfile(userId: String): CentralUserWorkspaceProfile? {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/users/$userId/profile") {
            header?.let { header(it.first, it.second) }
        }.body<UserWorkspaceProfileDto>().toDomain()
    }
}

private class KtorCentralInventoryClient(
    client: HttpClient,
    configuration: CentralNodeConfiguration,
    tokenProvider: suspend () -> String?,
) : BaseCentralClient(client, configuration, tokenProvider), CentralInventoryClientPort {
    override suspend fun items(organizationId: String): List<InventoryItem> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/items?organizationId=$organizationId") {
            header?.let { header(it.first, it.second) }
        }.body<List<InventoryItemResponseDto>>().map { it.toDomain() }
    }

    override suspend fun item(inventoryItemId: String): InventoryItem? {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/items/$inventoryItemId") {
            header?.let { header(it.first, it.second) }
        }.body<InventoryItemResponseDto>().toDomain()
    }

    override suspend fun sessions(organizationId: String): List<InventorySession> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/sessions?organizationId=$organizationId") {
            header?.let { header(it.first, it.second) }
        }.body<List<InventorySessionResponseDto>>().map { it.toDomain() }
    }

    override suspend fun session(sessionId: String): InventorySession? {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/sessions/$sessionId") {
            header?.let { header(it.first, it.second) }
        }.body<InventorySessionResponseDto>().toDomain()
    }
}

private class KtorCentralSyncClient(
    client: HttpClient,
    configuration: CentralNodeConfiguration,
    tokenProvider: suspend () -> String?,
) : BaseCentralClient(client, configuration, tokenProvider), CentralSyncClientPort {
    override suspend fun upload(
        organizationId: String,
        deviceId: String,
        cursor: Long?,
        changes: List<InventoryEvent>,
    ): CentralSyncUploadResult {
        val header = authHeader()
        val response = client.post("$baseUrl/api/v1/sync/batches") {
            contentType(ContentType.Application.Json)
            header?.let { header(it.first, it.second) }
            setBody(
                SyncUploadBatchRequestDto(
                    batchId = "batch-${organizationId}-${System.currentTimeMillis()}",
                    organizationId = organizationId,
                    deviceId = deviceId,
                    cursor = cursor,
                    changes = changes.map { it.toDto() },
                ),
            )
        }.body<SyncUploadBatchResponseDto>()
        return CentralSyncUploadResult(
            nextCursor = response.nextCursor,
            acknowledgedChangeIds = response.acknowledgements.map { it.changeId }.toSet(),
            conflicts = response.conflicts.map { it.toDomain() },
        )
    }

    override suspend fun pull(organizationId: String, afterCursor: Long, limit: Int): CentralSyncPullResult {
        val header = authHeader()
        val response = client.post("$baseUrl/api/v1/sync/pull") {
            contentType(ContentType.Application.Json)
            header?.let { header(it.first, it.second) }
            setBody(SyncPullRequestDto(organizationId = organizationId, afterCursor = afterCursor, limit = limit))
        }.body<SyncPullResponseDto>()
        return CentralSyncPullResult(
            nextCursor = response.nextCursor,
            events = response.changes.mapNotNull { it.toInventoryEventOrNull() },
        )
    }

    override suspend fun conflicts(organizationId: String): List<CentralConflict> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/sync/conflicts/$organizationId") {
            header?.let { header(it.first, it.second) }
        }.body<List<SyncConflictDto>>().map { it.toDomain() }
    }

    override suspend fun resolveConflict(request: CentralConflictResolution): CentralConflict? {
        val header = authHeader()
        return client.post("$baseUrl/api/v1/sync/conflicts/resolve") {
            contentType(ContentType.Application.Json)
            header?.let { header(it.first, it.second) }
            setBody(ConflictResolutionRequestDto(request.conflictId, request.resolutionNote, request.winningChangeId))
        }.bodyAsText().takeIf { it.isNotBlank() }?.let {
            centralJson.decodeFromString(SyncConflictDto.serializer(), it).toDomain()
        }
    }
}

private class KtorCentralAttachmentClient(
    client: HttpClient,
    configuration: CentralNodeConfiguration,
    tokenProvider: suspend () -> String?,
) : BaseCentralClient(client, configuration, tokenProvider), CentralAttachmentClientPort {
    override suspend fun upload(
        organizationId: String,
        aggregateId: String,
        aggregateType: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray,
    ): CentralAttachmentArtifact {
        val header = authHeader()
        return client.post("$baseUrl/api/v1/attachments") {
            header?.let { header(it.first, it.second) }
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("organizationId", organizationId)
                        append("aggregateId", aggregateId)
                        append("aggregateType", aggregateType)
                        append(
                            "file",
                            bytes,
                            io.ktor.http.Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                                append(HttpHeaders.ContentType, contentType)
                            },
                        )
                    },
                ),
            )
        }.body<AttachmentResponseDto>().toDomain()
    }

    override suspend fun findByAttachmentId(attachmentId: String): CentralAttachmentArtifact? {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/attachments/$attachmentId") {
            header?.let { header(it.first, it.second) }
        }.body<AttachmentResponseDto>().toDomain()
    }
}

private class KtorCentralExportClient(
    client: HttpClient,
    configuration: CentralNodeConfiguration,
    tokenProvider: suspend () -> String?,
) : BaseCentralClient(client, configuration, tokenProvider), CentralExportClientPort {
    override suspend fun create(organizationId: String, sessionId: String, format: String): CentralExportTask {
        val header = authHeader()
        return client.post("$baseUrl/api/v1/exports") {
            contentType(ContentType.Application.Json)
            header?.let { header(it.first, it.second) }
            setBody(CreateExportTaskRequestDto(organizationId = organizationId, sessionId = sessionId, format = format))
        }.body<ExportTaskResponseDto>().toDomain()
    }

    override suspend fun findByExportTaskId(exportTaskId: String): CentralExportTask? {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/exports/$exportTaskId") {
            header?.let { header(it.first, it.second) }
        }.body<ExportTaskResponseDto>().toDomain()
    }
}

private class KtorCentralRelayClient(
    client: HttpClient,
    configuration: CentralNodeConfiguration,
    tokenProvider: suspend () -> String?,
) : BaseCentralClient(client, configuration, tokenProvider), CentralRelayClientPort {
    override suspend fun relayNodes(organizationId: String, onlineOnly: Boolean): List<CentralRelayNode> {
        val header = authHeader()
        return client.get("$baseUrl/api/v1/relay/nodes?organizationId=$organizationId&onlineOnly=$onlineOnly") {
            header?.let { header(it.first, it.second) }
        }.body<List<RelayNodeResponseDto>>().map { it.toDomain() }
    }
}

@Serializable
private data class LoginRequestDto(
    val username: String,
    val password: String,
)

@Serializable
private data class RefreshRequestDto(
    val refreshToken: String,
)

@Serializable
private data class LoginResponseDto(
    val sessionId: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val email: String? = null,
    val accessToken: String,
    val refreshToken: String? = null,
    val accessTokenExpiresAt: String? = null,
    val activeOrganizationId: String? = null,
    val authenticatedAt: String,
)

@Serializable
private data class UserProfileDto(
    val userId: String,
    val username: String,
    val displayName: String,
    val email: String? = null,
    val updatedAt: String,
)

@Serializable
private data class UserWorkspaceProfileDto(
    val userId: String,
    val displayName: String,
    val email: String,
    val locale: String,
    val timeZone: String,
    val activeOrganizationId: String? = null,
    val updatedAt: String,
)

@Serializable
private data class OrganizationResponseDto(
    val organizationId: String,
    val name: String,
    val legalName: String? = null,
    val active: Boolean,
    val updatedAt: String,
)

@Serializable
private data class MembershipResponseDto(
    val membershipId: String,
    val organizationId: String,
    val userId: String,
    val roleIds: Set<String>,
    val active: Boolean,
    val updatedAt: String,
)

@Serializable
private data class RoleResponseDto(
    val roleId: String,
    val organizationId: String,
    val name: String,
    val permissions: Set<String>,
    val systemRole: Boolean,
    val updatedAt: String,
)

@Serializable
private data class LocationResponseDto(
    val locationId: String,
    val organizationId: String,
    val name: String,
    val type: String,
    val parentLocationId: String? = null,
    val code: String? = null,
    val active: Boolean,
    val updatedAt: String,
)

@Serializable
private data class DepartmentResponseDto(
    val departmentId: String,
    val organizationId: String,
    val name: String,
    val parentDepartmentId: String? = null,
    val active: Boolean,
    val updatedAt: String,
)

@Serializable
private data class CostCenterResponseDto(
    val costCenterId: String,
    val organizationId: String,
    val code: String,
    val name: String,
    val active: Boolean,
    val updatedAt: String,
)

@Serializable
private data class BankAccountResponseDto(
    val bankAccountId: String,
    val organizationId: String,
    val bankName: String,
    val bic: String,
    val accountNumber: String,
    val correspondentAccount: String? = null,
    val active: Boolean,
    val updatedAt: String,
)

@Serializable
private data class OrganizationPartyResponseDto(
    val partyId: String,
    val organizationId: String,
    val partyType: String,
    val name: String,
    val registrationNumber: String? = null,
    val active: Boolean,
    val updatedAt: String,
)

@Serializable
private data class PermissionDefinitionResponseDto(
    val permissionId: String,
    val name: String,
    val description: String,
    val scope: String,
    val updatedAt: String,
)

@Serializable
private data class OrganizationDashboardSummaryResponseDto(
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
private data class InventoryItemResponseDto(
    val inventoryItemId: String,
    val organizationId: String,
    val inventoryNumber: String,
    val title: String,
    val locationId: String? = null,
    val status: String,
    val revision: Long,
    val updatedAt: String,
)

@Serializable
private data class InventorySessionResponseDto(
    val sessionId: String,
    val organizationId: String,
    val title: String,
    val status: String,
    val revision: Long,
    val updatedAt: String,
)

@Serializable
private data class AttachmentResponseDto(
    val attachmentId: String,
    val organizationId: String,
    val aggregateId: String,
    val aggregateType: String,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val storagePath: String,
    val createdAt: String,
)

@Serializable
private data class ExportTaskResponseDto(
    val exportTaskId: String,
    val organizationId: String,
    val sessionId: String,
    val format: String,
    val status: String,
    val requestedAt: String,
    val artifactRef: String? = null,
)

@Serializable
private data class RelayNodeResponseDto(
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
    val registeredAt: String,
    val lastSeenAt: String,
    val presenceExpiresAt: String? = null,
)

@Serializable
private data class CreateExportTaskRequestDto(
    val organizationId: String,
    val sessionId: String,
    val format: String,
)

@Serializable
private enum class SyncAggregateTypeDto {
    ORGANIZATION,
    MEMBERSHIP,
    ROLE,
    INVENTORY_ITEM,
    INVENTORY_SESSION,
    APPROVAL,
    INCIDENT,
    COMMENT,
    ATTACHMENT,
    EXPORT_TASK,
    SCAN_EVENT,
}

@Serializable
private enum class SyncChangeTypeDto {
    ITEM_CREATED,
    ITEM_UPDATED,
    ATTACHMENT_ADDED,
    APPROVAL_SUBMITTED,
    APPROVAL_CONFIRMED,
    INCIDENT_CREATED,
    INCIDENT_CLOSED,
    SESSION_CREATED,
    SESSION_CLOSED,
    COMMENT_ADDED,
    SCAN_RECORDED,
    LABEL_GENERATED,
}

@Serializable
private enum class SyncStatusDto {
    PENDING,
    ACCEPTED,
    REJECTED,
    CONFLICT,
}

@Serializable
private data class SyncChangeDto(
    val changeId: String,
    val aggregateId: String,
    val aggregateType: SyncAggregateTypeDto,
    val changeType: SyncChangeTypeDto,
    val organizationId: String,
    val authorId: String,
    val sourceNodeId: String,
    val createdAt: String,
    val baseRevision: Long,
    val payload: String,
    val syncStatus: SyncStatusDto = SyncStatusDto.PENDING,
)

@Serializable
private data class SyncConflictDto(
    val conflictId: String,
    val organizationId: String,
    val aggregateId: String,
    val aggregateType: SyncAggregateTypeDto,
    val localRevision: Long,
    val canonicalRevision: Long,
    val status: SyncStatusDto,
    val detectedAt: String,
    val note: String? = null,
)

@Serializable
private data class SyncAcknowledgementDto(
    val changeId: String,
    val status: SyncStatusDto,
    val canonicalRevision: Long,
    val conflictId: String? = null,
    val message: String? = null,
)

@Serializable
private data class SyncUploadBatchRequestDto(
    val batchId: String,
    val organizationId: String,
    val deviceId: String,
    val cursor: Long?,
    val changes: List<SyncChangeDto>,
)

@Serializable
private data class SyncUploadBatchResponseDto(
    val batchId: String,
    val organizationId: String,
    val acknowledgedAt: String,
    val nextCursor: Long,
    val acknowledgements: List<SyncAcknowledgementDto>,
    val conflicts: List<SyncConflictDto>,
)

@Serializable
private data class SyncPullRequestDto(
    val organizationId: String,
    val afterCursor: Long,
    val limit: Int = 200,
)

@Serializable
private data class SyncPullResponseDto(
    val organizationId: String,
    val fromCursor: Long,
    val nextCursor: Long,
    val changes: List<SyncChangeDto>,
)

@Serializable
private data class ConflictResolutionRequestDto(
    val conflictId: String,
    val resolutionNote: String,
    val winningChangeId: String? = null,
)

private fun LoginResponseDto.toDomain(): CentralAuthSession = CentralAuthSession(
    sessionId = sessionId,
    userProfile = CentralUserProfile(
        userId = userId,
        username = username,
        displayName = displayName,
        email = email,
        updatedAt = Instant.parse(authenticatedAt),
    ),
    accessToken = accessToken,
    refreshToken = refreshToken,
    accessTokenExpiresAt = accessTokenExpiresAt?.let(Instant::parse),
    activeOrganizationId = activeOrganizationId,
    lastAuthenticatedAt = Instant.parse(authenticatedAt),
)

private fun UserProfileDto.toDomain(): CentralUserProfile = CentralUserProfile(
    userId = userId,
    username = username,
    displayName = displayName,
    email = email,
    updatedAt = Instant.parse(updatedAt),
)

private fun UserWorkspaceProfileDto.toDomain(): CentralUserWorkspaceProfile = CentralUserWorkspaceProfile(
    userId = userId,
    displayName = displayName,
    email = email,
    locale = locale,
    timeZone = timeZone,
    activeOrganizationId = activeOrganizationId,
    updatedAt = Instant.parse(updatedAt),
)

private fun CentralAuthSession.toState(offlineFallback: Boolean): CentralAuthState = CentralAuthState(
    authenticated = true,
    userProfile = userProfile,
    accessTokenExpiresAt = accessTokenExpiresAt,
    activeOrganizationId = activeOrganizationId,
    lastAuthenticatedAt = lastAuthenticatedAt,
    offlineFallback = offlineFallback,
)

private fun OrganizationResponseDto.toDomain(): CentralOrganizationAccess = CentralOrganizationAccess(
    organizationId = organizationId,
    name = name,
    legalName = legalName,
    active = active,
    updatedAt = Instant.parse(updatedAt),
)

private fun MembershipResponseDto.toDomain(): CentralOrganizationAccess = CentralOrganizationAccess(
    organizationId = organizationId,
    name = organizationId,
    membershipId = membershipId,
    roleIds = roleIds,
    active = active,
    updatedAt = Instant.parse(updatedAt),
)

private fun RoleResponseDto.toDomain(): CentralRoleDefinition = CentralRoleDefinition(
    roleId = roleId,
    organizationId = organizationId,
    name = name,
    permissions = permissions,
    systemRole = systemRole,
    updatedAt = Instant.parse(updatedAt),
)

private fun LocationResponseDto.toDomain(): CentralLocation = CentralLocation(
    locationId = locationId,
    organizationId = organizationId,
    name = name,
    type = type,
    parentLocationId = parentLocationId,
    code = code,
    active = active,
    updatedAt = Instant.parse(updatedAt),
)

private fun DepartmentResponseDto.toDomain(): CentralDepartment = CentralDepartment(
    departmentId = departmentId,
    organizationId = organizationId,
    name = name,
    parentDepartmentId = parentDepartmentId,
    active = active,
    updatedAt = Instant.parse(updatedAt),
)

private fun CostCenterResponseDto.toDomain(): CentralCostCenter = CentralCostCenter(
    costCenterId = costCenterId,
    organizationId = organizationId,
    code = code,
    name = name,
    active = active,
    updatedAt = Instant.parse(updatedAt),
)

private fun BankAccountResponseDto.toDomain(): CentralBankAccount = CentralBankAccount(
    bankAccountId = bankAccountId,
    organizationId = organizationId,
    bankName = bankName,
    bic = bic,
    accountNumber = accountNumber,
    correspondentAccount = correspondentAccount,
    active = active,
    updatedAt = Instant.parse(updatedAt),
)

private fun OrganizationPartyResponseDto.toDomain(): CentralOrganizationParty = CentralOrganizationParty(
    partyId = partyId,
    organizationId = organizationId,
    partyType = partyType,
    name = name,
    registrationNumber = registrationNumber,
    active = active,
    updatedAt = Instant.parse(updatedAt),
)

private fun PermissionDefinitionResponseDto.toDomain(): CentralPermissionDefinition = CentralPermissionDefinition(
    permissionId = permissionId,
    name = name,
    description = description,
    scope = scope,
    updatedAt = Instant.parse(updatedAt),
)

private fun OrganizationDashboardSummaryResponseDto.toDomain(): CentralOrganizationDashboardSummary = CentralOrganizationDashboardSummary(
    organizationId = organizationId,
    memberships = memberships,
    roles = roles,
    locations = locations,
    bankAccounts = bankAccounts,
    departments = departments,
    costCenters = costCenters,
    parties = parties,
)

private fun InventoryItemResponseDto.toDomain(): InventoryItem = InventoryItem(
    inventoryItemId = inventoryItemId,
    organizationId = organizationId,
    inventoryNumber = inventoryNumber,
    title = title,
    locationId = locationId,
    createdByPeerId = REMOTE_ACTOR,
    createdAt = Instant.parse(updatedAt),
    updatedAt = Instant.parse(updatedAt),
    currentStatus = status.toInventoryStatus(),
    syncStatus = InventorySyncStatus.SYNCED,
    revision = revision,
    itemType = InventoryItemType.UNKNOWN,
)

private fun InventorySessionResponseDto.toDomain(): InventorySession = InventorySession(
    sessionId = sessionId,
    organizationId = organizationId,
    title = title,
    periodStart = Instant.parse(updatedAt),
    status = status.toInventorySessionStatus(),
    reviewStatus = InventorySessionReviewStatus.PENDING,
    createdByPeerId = REMOTE_ACTOR,
    createdAt = Instant.parse(updatedAt),
    updatedAt = Instant.parse(updatedAt),
    revision = revision,
)

private fun AttachmentResponseDto.toDomain(): CentralAttachmentArtifact = CentralAttachmentArtifact(
    attachmentId = attachmentId,
    organizationId = organizationId,
    aggregateId = aggregateId,
    aggregateType = aggregateType,
    fileName = filename,
    contentType = contentType,
    sizeBytes = sizeBytes,
    artifactRef = storagePath,
    createdAt = Instant.parse(createdAt),
)

private fun ExportTaskResponseDto.toDomain(): CentralExportTask = CentralExportTask(
    exportTaskId = exportTaskId,
    organizationId = organizationId,
    sessionId = sessionId,
    format = format,
    status = status,
    requestedAt = Instant.parse(requestedAt),
    artifactRef = artifactRef,
)

private fun RelayNodeResponseDto.toDomain(): CentralRelayNode = CentralRelayNode(
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
    registeredAt = Instant.parse(registeredAt),
    lastSeenAt = Instant.parse(lastSeenAt),
    presenceExpiresAt = presenceExpiresAt?.let(Instant::parse),
)

private fun SyncConflictDto.toDomain(): CentralConflict = CentralConflict(
    conflictId = conflictId,
    organizationId = organizationId,
    aggregateId = aggregateId,
    aggregateType = aggregateType.name,
    localRevision = localRevision,
    canonicalRevision = canonicalRevision,
    detectedAt = Instant.parse(detectedAt),
    status = when (status) {
        SyncStatusDto.PENDING -> org.expert.link.mesh.domain.model.hybrid.CentralChangeStatus.PENDING
        SyncStatusDto.ACCEPTED -> org.expert.link.mesh.domain.model.hybrid.CentralChangeStatus.ACCEPTED
        SyncStatusDto.REJECTED -> org.expert.link.mesh.domain.model.hybrid.CentralChangeStatus.REJECTED
        SyncStatusDto.CONFLICT -> org.expert.link.mesh.domain.model.hybrid.CentralChangeStatus.CONFLICT
    },
    note = note,
)

private fun SyncChangeDto.toInventoryEventOrNull(): InventoryEvent? =
    runCatching { centralJson.decodeFromString(InventoryEvent.serializer(), payload) }.getOrNull()

private fun InventoryEvent.toDto(): SyncChangeDto = SyncChangeDto(
    changeId = eventId,
    aggregateId = entityId,
    aggregateType = entityType.toSyncAggregateType(),
    changeType = eventType.toSyncChangeType(),
    organizationId = organizationId,
    authorId = actorPeerId,
    sourceNodeId = actorPeerId,
    createdAt = occurredAt.toString(),
    baseRevision = previousEntityRevision ?: ((entityRevision ?: 1) - 1).coerceAtLeast(0),
    payload = centralJson.encodeToString(InventoryEvent.serializer(), this),
)

private fun org.expert.link.mesh.domain.model.inventory.InventoryEntityType.toSyncAggregateType(): SyncAggregateTypeDto =
    when (this) {
        org.expert.link.mesh.domain.model.inventory.InventoryEntityType.ORGANIZATION -> SyncAggregateTypeDto.ORGANIZATION
        org.expert.link.mesh.domain.model.inventory.InventoryEntityType.MEMBER -> SyncAggregateTypeDto.MEMBERSHIP
        org.expert.link.mesh.domain.model.inventory.InventoryEntityType.ROLE -> SyncAggregateTypeDto.ROLE
        org.expert.link.mesh.domain.model.inventory.InventoryEntityType.ITEM -> SyncAggregateTypeDto.INVENTORY_ITEM
        org.expert.link.mesh.domain.model.inventory.InventoryEntityType.SESSION -> SyncAggregateTypeDto.INVENTORY_SESSION
        org.expert.link.mesh.domain.model.inventory.InventoryEntityType.REVIEW -> SyncAggregateTypeDto.APPROVAL
        org.expert.link.mesh.domain.model.inventory.InventoryEntityType.INCIDENT -> SyncAggregateTypeDto.INCIDENT
        org.expert.link.mesh.domain.model.inventory.InventoryEntityType.COMMENT -> SyncAggregateTypeDto.COMMENT
        org.expert.link.mesh.domain.model.inventory.InventoryEntityType.ATTACHMENT -> SyncAggregateTypeDto.ATTACHMENT
        org.expert.link.mesh.domain.model.inventory.InventoryEntityType.EXPORT -> SyncAggregateTypeDto.EXPORT_TASK
        org.expert.link.mesh.domain.model.inventory.InventoryEntityType.SCAN_EVENT -> SyncAggregateTypeDto.SCAN_EVENT
        else -> SyncAggregateTypeDto.INVENTORY_ITEM
    }

private fun org.expert.link.mesh.domain.model.inventory.InventoryEventType.toSyncChangeType(): SyncChangeTypeDto =
    when (this) {
        org.expert.link.mesh.domain.model.inventory.InventoryEventType.CREATED -> SyncChangeTypeDto.ITEM_CREATED
        org.expert.link.mesh.domain.model.inventory.InventoryEventType.UPDATED -> SyncChangeTypeDto.ITEM_UPDATED
        org.expert.link.mesh.domain.model.inventory.InventoryEventType.ATTACHMENT_ADDED -> SyncChangeTypeDto.ATTACHMENT_ADDED
        org.expert.link.mesh.domain.model.inventory.InventoryEventType.REVIEW_SUBMITTED -> SyncChangeTypeDto.APPROVAL_SUBMITTED
        org.expert.link.mesh.domain.model.inventory.InventoryEventType.INCIDENT_REPORTED -> SyncChangeTypeDto.INCIDENT_CREATED
        org.expert.link.mesh.domain.model.inventory.InventoryEventType.INCIDENT_RESOLVED -> SyncChangeTypeDto.INCIDENT_CLOSED
        org.expert.link.mesh.domain.model.inventory.InventoryEventType.SESSION_CLOSED -> SyncChangeTypeDto.SESSION_CLOSED
        org.expert.link.mesh.domain.model.inventory.InventoryEventType.COMMENT_ADDED -> SyncChangeTypeDto.COMMENT_ADDED
        org.expert.link.mesh.domain.model.inventory.InventoryEventType.CODE_GENERATED -> SyncChangeTypeDto.LABEL_GENERATED
        org.expert.link.mesh.domain.model.inventory.InventoryEventType.CODE_REGENERATED -> SyncChangeTypeDto.LABEL_GENERATED
        org.expert.link.mesh.domain.model.inventory.InventoryEventType.LABEL_PRINTED -> SyncChangeTypeDto.LABEL_GENERATED
        else -> SyncChangeTypeDto.ITEM_UPDATED
    }

private fun String.toInventoryStatus(): InventoryStatus = when (uppercase()) {
    "DRAFT" -> InventoryStatus.DRAFT
    "UNDER_REVIEW" -> InventoryStatus.UNDER_REVIEW
    "CONFIRMED" -> InventoryStatus.CONFIRMED
    "REJECTED" -> InventoryStatus.REJECTED
    "ARCHIVED" -> InventoryStatus.ARCHIVED
    else -> InventoryStatus.ADDED
}

private fun String.toInventorySessionStatus(): InventorySessionStatus = when (uppercase()) {
    "DRAFT" -> InventorySessionStatus.DRAFT
    "ACTIVE" -> InventorySessionStatus.ACTIVE
    "UNDER_REVIEW" -> InventorySessionStatus.UNDER_REVIEW
    "CLOSED" -> InventorySessionStatus.CLOSED
    "ARCHIVED" -> InventorySessionStatus.ARCHIVED
    else -> InventorySessionStatus.DRAFT
}

private const val REMOTE_ACTOR = "central-backend"
