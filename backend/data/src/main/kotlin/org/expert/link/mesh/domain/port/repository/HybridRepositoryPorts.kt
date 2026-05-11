package org.expert.link.mesh.domain.port.repository

import org.expert.link.mesh.domain.model.hybrid.CentralAuthSession
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationAccess
import org.expert.link.mesh.domain.model.hybrid.CentralOrganizationWorkspaceSnapshot
import org.expert.link.mesh.domain.model.hybrid.CentralSyncState

interface CentralAuthSessionRepositoryPort {
    suspend fun save(session: CentralAuthSession): CentralAuthSession
    suspend fun current(): CentralAuthSession?
    suspend fun clear()
}

interface CentralOrganizationAccessRepositoryPort {
    suspend fun save(access: CentralOrganizationAccess): CentralOrganizationAccess
    suspend fun findByOrganizationId(organizationId: String): CentralOrganizationAccess?
    suspend fun list(): List<CentralOrganizationAccess>
    suspend fun setActiveOrganization(organizationId: String)
    suspend fun activeOrganizationId(): String?
    suspend fun clear()
}

interface CentralSyncStateRepositoryPort {
    suspend fun save(state: CentralSyncState): CentralSyncState
    suspend fun findByOrganizationId(organizationId: String): CentralSyncState?
    suspend fun list(): List<CentralSyncState>
}

interface CentralOrganizationWorkspaceRepositoryPort {
    suspend fun save(snapshot: CentralOrganizationWorkspaceSnapshot): CentralOrganizationWorkspaceSnapshot
    suspend fun findByOrganizationId(organizationId: String): CentralOrganizationWorkspaceSnapshot?
    suspend fun list(): List<CentralOrganizationWorkspaceSnapshot>
    suspend fun clear()
}
