package org.expert.link.database

import org.expert.link.mesh.domain.model.hybrid.CentralAuthSession
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationAccess
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationWorkspaceSnapshot
import org.expert.link.mesh.domain.model.hybrid.CentralSyncState
import org.expert.link.mesh.domain.port.repository.CentralAuthSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.CentralOrganizationAccessRepositoryPort
import org.expert.link.mesh.domain.port.repository.CentralOrganizationWorkspaceRepositoryPort
import org.expert.link.mesh.domain.port.repository.CentralSyncStateRepositoryPort

internal class RoomCentralAuthSessionRepository(
    private val inventoryDao: InventoryDao,
) : CentralAuthSessionRepositoryPort {
    override suspend fun save(session: CentralAuthSession): CentralAuthSession {
        inventoryDao.upsertCentralAuthSession(
            CentralAuthSessionRecord(
                sessionId = session.sessionId,
                userId = session.userProfile.userId,
                username = session.userProfile.username,
                displayName = session.userProfile.displayName,
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                accessTokenExpiresAt = session.accessTokenExpiresAt?.toString(),
                activeOrganizationId = session.activeOrganizationId,
                lastAuthenticatedAt = session.lastAuthenticatedAt.toString(),
                payloadJson = encodePayload(session),
            ),
        )
        return session
    }

    override suspend fun current(): CentralAuthSession? =
        inventoryDao.findCurrentCentralAuthSession()?.let { decodePayload(it.payloadJson) }

    override suspend fun clear() {
        inventoryDao.clearCentralAuthSessions()
    }
}

internal class RoomCentralOrganizationAccessRepository(
    private val inventoryDao: InventoryDao,
) : CentralOrganizationAccessRepositoryPort {
    override suspend fun save(access: CentralOrganizationAccess): CentralOrganizationAccess {
        inventoryDao.upsertCentralOrganizationAccess(
            CentralOrganizationAccessRecord(
                organizationId = access.organizationId,
                name = access.name,
                active = access.active,
                updatedAt = access.updatedAt.toString(),
                payloadJson = encodePayload(access),
            ),
        )
        return access
    }

    override suspend fun findByOrganizationId(organizationId: String): CentralOrganizationAccess? =
        inventoryDao.findCentralOrganizationAccess(organizationId)?.let { decodePayload(it.payloadJson) }

    override suspend fun list(): List<CentralOrganizationAccess> =
        inventoryDao.listCentralOrganizationAccess().map { decodePayload(it.payloadJson) }

    override suspend fun setActiveOrganization(organizationId: String) {
        inventoryDao.setActiveCentralOrganization(organizationId)
    }

    override suspend fun activeOrganizationId(): String? = inventoryDao.findActiveCentralOrganizationId()

    override suspend fun clear() {
        inventoryDao.clearCentralOrganizationAccess()
    }
}

internal class RoomCentralSyncStateRepository(
    private val inventoryDao: InventoryDao,
) : CentralSyncStateRepositoryPort {
    override suspend fun save(state: CentralSyncState): CentralSyncState {
        inventoryDao.upsertCentralSyncState(
            CentralSyncStateRecord(
                organizationId = state.organizationId,
                connectivityMode = state.connectivityMode.name,
                runState = state.runState.name,
                lastUploadedSequence = state.lastUploadedSequence,
                lastPulledCursor = state.lastPulledCursor,
                pendingChanges = state.pendingChanges,
                conflicts = state.conflicts,
                lastSyncAt = state.lastSyncAt?.toString(),
                lastError = state.lastError,
                payloadJson = encodePayload(state),
            ),
        )
        return state
    }

    override suspend fun findByOrganizationId(organizationId: String): CentralSyncState? =
        inventoryDao.findCentralSyncState(organizationId)?.let { decodePayload(it.payloadJson) }

    override suspend fun list(): List<CentralSyncState> =
        inventoryDao.listCentralSyncStates().map { decodePayload(it.payloadJson) }
}

internal class RoomCentralOrganizationWorkspaceRepository(
    private val inventoryDao: InventoryDao,
) : CentralOrganizationWorkspaceRepositoryPort {
    override suspend fun save(snapshot: CentralOrganizationWorkspaceSnapshot): CentralOrganizationWorkspaceSnapshot {
        inventoryDao.upsertCentralOrganizationWorkspace(
            CentralOrganizationWorkspaceRecord(
                organizationId = snapshot.organizationId,
                fetchedAt = snapshot.fetchedAt.toString(),
                payloadJson = encodePayload(snapshot),
            ),
        )
        return snapshot
    }

    override suspend fun findByOrganizationId(organizationId: String): CentralOrganizationWorkspaceSnapshot? =
        inventoryDao.findCentralOrganizationWorkspace(organizationId)?.let { decodePayload(it.payloadJson) }

    override suspend fun list(): List<CentralOrganizationWorkspaceSnapshot> =
        inventoryDao.listCentralOrganizationWorkspaces().map { decodePayload(it.payloadJson) }

    override suspend fun clear() {
        inventoryDao.clearCentralOrganizationWorkspaces()
    }
}
