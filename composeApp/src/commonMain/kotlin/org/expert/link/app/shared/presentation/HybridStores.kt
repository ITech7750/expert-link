package org.expert.link.app.shared.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.expert.link.mesh.contract.api.MeshCentralLoginCommand
import org.expert.link.mesh.contract.api.MeshCentralSyncCommand
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.api.MeshSelectActiveCentralOrganizationCommand
import org.expert.link.mesh.contract.config.MeshNodeMode
import org.expert.link.mesh.contract.model.MeshCentralAuthState
import org.expert.link.mesh.contract.model.MeshCentralConflict
import org.expert.link.mesh.contract.model.MeshCentralOrganizationAccess
import org.expert.link.mesh.contract.model.MeshCentralPendingChange
import org.expert.link.mesh.contract.model.MeshCentralSyncStatus
import org.expert.link.mesh.contract.model.MeshCentralHybridState
import org.expert.link.mesh.contract.model.MeshCentralConfig
import org.expert.link.mesh.contract.model.MeshCentralOrganizationWorkspaceSnapshot

data class HybridUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val hybridState: MeshCentralHybridState? = null,
    val authState: MeshCentralAuthState? = null,
    val organizations: List<MeshCentralOrganizationAccess> = emptyList(),
    val workspace: MeshCentralOrganizationWorkspaceSnapshot? = null,
    val syncStatus: MeshCentralSyncStatus? = null,
    val pendingChanges: List<MeshCentralPendingChange> = emptyList(),
    val conflicts: List<MeshCentralConflict> = emptyList(),
)

class HybridStore(
    private val session: NodeSessionController,
) {
    private val _state = MutableStateFlow(HybridUiState())
    val state: StateFlow<HybridUiState> = _state.asStateFlow()

    fun updateBaseUrl(value: String) {
        _state.update { it.copy(baseUrl = value) }
    }

    fun updateUsername(value: String) {
        _state.update { it.copy(username = value) }
    }

    fun updatePassword(value: String) {
        _state.update { it.copy(password = value) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    suspend fun refresh() {
        val currentConfig = session.state.value.config.central
        val currentBaseUrl = state.value.baseUrl.ifBlank { currentConfig?.baseUrl.orEmpty() }
        val result = session.withNode { node ->
            buildSnapshot(node, currentBaseUrl)
        }
        _state.update {
            result.getOrElse { error ->
                it.copy(
                    baseUrl = currentBaseUrl,
                    error = error.message ?: "Не удалось получить central state",
                )
            }
        }
    }

    suspend fun login() {
        val baseUrl = state.value.baseUrl.trim()
        val username = state.value.username.trim()
        val password = state.value.password
        if (baseUrl.isBlank()) {
            _state.update { it.copy(error = "Укажите адрес central backend") }
            return
        }
        if (username.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Введите логин и пароль") }
            return
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        applyCentralConfig(baseUrl)
        val result = session.withNode { node ->
            node.centralLogin(MeshCentralLoginCommand(username = username, password = password))
            buildSnapshot(node, baseUrl)
        }
        _state.update {
            result.fold(
                onSuccess = { snapshot ->
                    snapshot.copy(
                        loading = false,
                        message = "Central авторизация выполнена",
                        password = "",
                    )
                },
                onFailure = { error ->
                    it.copy(loading = false, error = error.message ?: "Не удалось выполнить вход")
                },
            )
        }
    }

    suspend fun logout() {
        _state.update { it.copy(loading = true, error = null, message = null) }
        val result = session.withNode { node ->
            node.centralLogout()
            buildSnapshot(node, state.value.baseUrl)
        }
        _state.update {
            result.fold(
                onSuccess = { snapshot -> snapshot.copy(loading = false, message = "Central session завершена", password = "") },
                onFailure = { error -> it.copy(loading = false, error = error.message ?: "Не удалось завершить сеанс") },
            )
        }
    }

    suspend fun syncNow() {
        _state.update { it.copy(loading = true, error = null, message = null) }
        val result = session.withNode { node ->
            node.syncWithCentral(MeshCentralSyncCommand())
            buildSnapshot(node, state.value.baseUrl)
        }
        _state.update {
            result.fold(
                onSuccess = { snapshot -> snapshot.copy(loading = false, message = "Central sync завершён") },
                onFailure = { error -> it.copy(loading = false, error = error.message ?: "Не удалось синхронизировать данные") },
            )
        }
    }

    suspend fun selectOrganization(organizationId: String) {
        _state.update { it.copy(loading = true, error = null) }
        val result = session.withNode { node ->
            node.selectActiveCentralOrganization(MeshSelectActiveCentralOrganizationCommand(organizationId))
            buildSnapshot(node, state.value.baseUrl)
        }
        _state.update {
            result.fold(
                onSuccess = { snapshot -> snapshot.copy(loading = false, message = "Активная организация обновлена") },
                onFailure = { error -> it.copy(loading = false, error = error.message ?: "Не удалось выбрать организацию") },
            )
        }
    }

    private suspend fun applyCentralConfig(baseUrl: String) {
        val current = session.state.value.config
        val currentCentral = current.central
        if (currentCentral?.baseUrl == baseUrl && current.nodeMode == MeshNodeMode.HYBRID) {
            return
        }
        val updated = current.copy(
            nodeMode = MeshNodeMode.HYBRID,
            central = MeshCentralConfig(
                baseUrl = baseUrl,
                authPath = currentCentral?.authPath ?: "/api/v1/auth",
                requestTimeoutMillis = currentCentral?.requestTimeoutMillis ?: 10_000,
                deviceId = currentCentral?.deviceId ?: current.displayName,
                allowMeteredSync = currentCentral?.allowMeteredSync ?: true,
                syncOnStart = currentCentral?.syncOnStart ?: true,
                syncOnResume = currentCentral?.syncOnResume ?: true,
                backgroundSyncIntervalSeconds = currentCentral?.backgroundSyncIntervalSeconds ?: 300,
            ),
        )
        session.updateConfig(updated)
        session.restart(updated)
    }

    private suspend fun buildSnapshot(node: MeshNode, baseUrl: String): HybridUiState {
        val hybridState = node.hybridState()
        val authState = node.centralAuthState()
        val organizations = runCatching { node.centralOrganizations() }.getOrDefault(emptyList())
        val activeOrganizationId = organizations.firstOrNull { it.active }?.organizationId ?: authState?.activeOrganizationId
        val workspace = activeOrganizationId?.let { organizationId ->
            val cached = runCatching { node.centralOrganizationWorkspace(organizationId) }.getOrNull()
            if (cached != null) {
                cached
            } else if (authState?.authenticated == true) {
                runCatching { node.refreshCentralOrganizationWorkspace(organizationId) }.getOrNull()
            } else {
                null
            }
        }
        val syncStatus = activeOrganizationId?.let { runCatching { node.centralSyncStatus(it) }.getOrNull() }
        val pendingChanges = activeOrganizationId?.let { runCatching { node.centralPendingChanges(it, 20) }.getOrDefault(emptyList()) }
            ?: emptyList()
        val conflicts = activeOrganizationId?.let { runCatching { node.centralConflicts(it) }.getOrDefault(emptyList()) }
            ?: emptyList()
        return HybridUiState(
            baseUrl = baseUrl,
            username = state.value.username,
            password = state.value.password,
            hybridState = hybridState,
            authState = authState,
            organizations = organizations,
            workspace = workspace,
            syncStatus = syncStatus,
            pendingChanges = pendingChanges,
            conflicts = conflicts,
        )
    }
}
