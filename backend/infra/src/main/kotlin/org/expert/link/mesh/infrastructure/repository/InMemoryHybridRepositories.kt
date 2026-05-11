package org.expert.link.mesh.infrastructure.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.expert.link.mesh.domain.model.hybrid.CentralAuthSession
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationAccess
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationWorkspaceSnapshot
import org.expert.link.mesh.domain.model.hybrid.CentralSyncState
import org.expert.link.mesh.domain.port.repository.CentralAuthSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.CentralOrganizationAccessRepositoryPort
import org.expert.link.mesh.domain.port.repository.CentralOrganizationWorkspaceRepositoryPort
import org.expert.link.mesh.domain.port.repository.CentralSyncStateRepositoryPort

class InMemoryCentralAuthSessionRepositoryAdapter : CentralAuthSessionRepositoryPort {
    private val mutex = Mutex()
    private var currentSession: CentralAuthSession? = null

    override suspend fun save(session: CentralAuthSession): CentralAuthSession = mutex.withLock {
        currentSession = session
        session
    }

    override suspend fun current(): CentralAuthSession? = mutex.withLock {
        currentSession
    }

    override suspend fun clear() {
        mutex.withLock {
            currentSession = null
        }
    }
}

class InMemoryCentralOrganizationAccessRepositoryAdapter : CentralOrganizationAccessRepositoryPort {
    private val mutex = Mutex()
    private val access = linkedMapOf<String, CentralOrganizationAccess>()
    private var activeOrganizationId: String? = null

    override suspend fun save(access: CentralOrganizationAccess): CentralOrganizationAccess = mutex.withLock {
        this.access[access.organizationId] = access
        if (access.active) {
            activeOrganizationId = access.organizationId
        }
        access
    }

    override suspend fun findByOrganizationId(organizationId: String): CentralOrganizationAccess? = mutex.withLock {
        access[organizationId]
    }

    override suspend fun list(): List<CentralOrganizationAccess> = mutex.withLock {
        access.values.toList()
    }

    override suspend fun setActiveOrganization(organizationId: String) {
        mutex.withLock {
            activeOrganizationId = organizationId
            access.replaceAll { _, value ->
                value.copy(active = value.organizationId == organizationId)
            }
        }
    }

    override suspend fun activeOrganizationId(): String? = mutex.withLock {
        activeOrganizationId
    }

    override suspend fun clear() {
        mutex.withLock {
            access.clear()
            activeOrganizationId = null
        }
    }
}

class InMemoryCentralSyncStateRepositoryAdapter : CentralSyncStateRepositoryPort {
    private val mutex = Mutex()
    private val states = linkedMapOf<String, CentralSyncState>()

    override suspend fun save(state: CentralSyncState): CentralSyncState = mutex.withLock {
        states[state.organizationId] = state
        state
    }

    override suspend fun findByOrganizationId(organizationId: String): CentralSyncState? = mutex.withLock {
        states[organizationId]
    }

    override suspend fun list(): List<CentralSyncState> = mutex.withLock {
        states.values.toList()
    }
}

class InMemoryCentralOrganizationWorkspaceRepositoryAdapter : CentralOrganizationWorkspaceRepositoryPort {
    private val mutex = Mutex()
    private val snapshots = linkedMapOf<String, CentralOrganizationWorkspaceSnapshot>()

    override suspend fun save(snapshot: CentralOrganizationWorkspaceSnapshot): CentralOrganizationWorkspaceSnapshot = mutex.withLock {
        snapshots[snapshot.organizationId] = snapshot
        snapshot
    }

    override suspend fun findByOrganizationId(organizationId: String): CentralOrganizationWorkspaceSnapshot? = mutex.withLock {
        snapshots[organizationId]
    }

    override suspend fun list(): List<CentralOrganizationWorkspaceSnapshot> = mutex.withLock {
        snapshots.values.toList()
    }

    override suspend fun clear() {
        mutex.withLock {
            snapshots.clear()
        }
    }
}
