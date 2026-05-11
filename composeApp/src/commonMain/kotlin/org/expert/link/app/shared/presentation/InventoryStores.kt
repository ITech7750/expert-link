package org.expert.link.app.shared.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.expert.link.mesh.contract.api.MeshAddOrganizationMemberCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryAttributeDefinitionCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryCategoryCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryCategoryTemplateCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryCostCenterCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryDeadlineRuleCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryDepartmentCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryFundingSourceCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryItemCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryLegalHolderCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryLocationCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryOwnerCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryRuleThresholdCommand
import org.expert.link.mesh.contract.api.MeshCreateInventorySessionCommand
import org.expert.link.mesh.contract.api.MeshCreateInventorySubcategoryCommand
import org.expert.link.mesh.contract.api.MeshCreateInventorySupplierCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryTagCommand
import org.expert.link.mesh.contract.api.MeshCreateOrganizationCommand
import org.expert.link.mesh.contract.api.MeshCreateRoleCommand
import org.expert.link.mesh.contract.api.MeshInventorySyncCommand
import org.expert.link.mesh.contract.api.MeshInventorySearchCommand
import org.expert.link.mesh.contract.api.MeshRequestInventoryExportCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryDepartmentCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryLocationCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryOwnerCommand
import org.expert.link.mesh.contract.api.MeshUpdateOrganizationMemberCommand
import org.expert.link.mesh.contract.model.MeshInventoryAttributeType
import org.expert.link.mesh.contract.model.MeshInventoryCategory
import org.expert.link.mesh.contract.model.MeshInventoryCondition
import org.expert.link.mesh.contract.model.MeshInventoryDeadlineTarget
import org.expert.link.mesh.contract.model.MeshInventoryExportFormat
import org.expert.link.mesh.contract.model.MeshInventoryExportTask
import org.expert.link.mesh.contract.model.MeshInventoryFilterSet
import org.expert.link.mesh.contract.model.MeshInventoryFieldTemplate
import org.expert.link.mesh.contract.model.MeshInventoryItem
import org.expert.link.mesh.contract.model.MeshInventoryLabelTemplate
import org.expert.link.mesh.contract.model.MeshInventoryLocation
import org.expert.link.mesh.contract.model.MeshInventoryLocationType
import org.expert.link.mesh.contract.model.MeshInventoryOwnerType
import org.expert.link.mesh.contract.model.MeshInventoryPermission
import org.expert.link.mesh.contract.model.MeshInventoryPrintTask
import org.expert.link.mesh.contract.model.MeshInventoryRuleType
import org.expert.link.mesh.contract.model.MeshInventorySearchQuery
import org.expert.link.mesh.contract.model.MeshInventorySortMode
import org.expert.link.mesh.contract.model.MeshInventorySession
import org.expert.link.mesh.contract.model.MeshInventoryWorkflowStatus
import org.expert.link.mesh.contract.model.MeshInventorySessionRole
import org.expert.link.mesh.contract.model.MeshInventoryStatus
import org.expert.link.mesh.contract.model.MeshInventorySyncResult
import org.expert.link.mesh.contract.model.MeshOrganization
import org.expert.link.mesh.contract.model.MeshOrganizationMember
import org.expert.link.mesh.contract.model.MeshOrganizationMemberStatus
import org.expert.link.mesh.contract.model.MeshPairedPeer
import org.expert.link.mesh.contract.model.MeshInventoryRequiredFieldRule
import org.expert.link.mesh.contract.model.MeshRole

enum class InventorySection(val title: String) {
    DASHBOARD("Обзор"),
    ORGANIZATIONS("Организации"),
    ITEMS("Оборудование"),
    SESSIONS("Инвентаризация"),
    REVIEW("Согласование"),
    INCIDENTS("Инциденты"),
    REPORTS("Отчёты"),
    CATALOGS("Справочники"),
}

enum class InventoryBusyAction {
    REFRESH,
    SEARCH,
    CREATE_ORG,
    CREATE_ROLE,
    ADD_MEMBER,
    UPDATE_MEMBER,
    CREATE_CATEGORY,
    CREATE_SUBCATEGORY,
    CREATE_TAG,
    CREATE_ATTRIBUTE,
    CREATE_TEMPLATE,
    CREATE_LOCATION,
    UPDATE_LOCATION,
    CREATE_OWNER,
    UPDATE_OWNER,
    CREATE_DEPARTMENT,
    UPDATE_DEPARTMENT,
    CREATE_COST_CENTER,
    CREATE_LEGAL_HOLDER,
    CREATE_SUPPLIER,
    CREATE_FUNDING_SOURCE,
    CREATE_RULE_THRESHOLD,
    CREATE_DEADLINE_RULE,
    CREATE_ITEM,
    CREATE_SESSION,
    REQUEST_EXPORT,
    SYNC,
}

data class InventoryOverviewState(
    val runtimeStatus: NodeRuntimeStatus = NodeRuntimeStatus.STOPPED,
    val localPeerId: String? = null,
    val selectedSection: InventorySection = InventorySection.ITEMS,
    val organizations: List<MeshOrganization> = emptyList(),
    val selectedOrganizationId: String? = null,
    val members: List<MeshOrganizationMember> = emptyList(),
    val roles: List<MeshRole> = emptyList(),
    val permissions: Set<MeshInventoryPermission> = emptySet(),
    val categories: List<MeshInventoryCategory> = emptyList(),
    val subcategories: List<org.expert.link.mesh.contract.model.MeshInventorySubcategory> = emptyList(),
    val tags: List<org.expert.link.mesh.contract.model.MeshInventoryTag> = emptyList(),
    val attributeDefinitions: List<org.expert.link.mesh.contract.model.MeshInventoryAttributeDefinition> = emptyList(),
    val templates: List<org.expert.link.mesh.contract.model.MeshInventoryCategoryTemplate> = emptyList(),
    val locations: List<MeshInventoryLocation> = emptyList(),
    val owners: List<org.expert.link.mesh.contract.model.MeshInventoryOwner> = emptyList(),
    val departments: List<org.expert.link.mesh.contract.model.MeshInventoryDepartment> = emptyList(),
    val costCenters: List<org.expert.link.mesh.contract.model.MeshInventoryCostCenter> = emptyList(),
    val legalHolders: List<org.expert.link.mesh.contract.model.MeshInventoryLegalHolder> = emptyList(),
    val suppliers: List<org.expert.link.mesh.contract.model.MeshInventorySupplier> = emptyList(),
    val fundingSources: List<org.expert.link.mesh.contract.model.MeshInventoryFundingSource> = emptyList(),
    val items: List<MeshInventoryItem> = emptyList(),
    val labelTemplates: List<MeshInventoryLabelTemplate> = emptyList(),
    val printTasks: List<MeshInventoryPrintTask> = emptyList(),
    val sessions: List<MeshInventorySession> = emptyList(),
    val exports: List<MeshInventoryExportTask> = emptyList(),
    val reviewItems: List<MeshInventoryItem> = emptyList(),
    val incidents: List<org.expert.link.mesh.contract.model.MeshInventoryIncident> = emptyList(),
    val alerts: List<org.expert.link.mesh.contract.model.MeshInventoryAlertEvent> = emptyList(),
    val reminders: List<org.expert.link.mesh.contract.model.MeshInventoryReminder> = emptyList(),
    val ruleThresholds: List<org.expert.link.mesh.contract.model.MeshInventoryRuleThreshold> = emptyList(),
    val deadlineRules: List<org.expert.link.mesh.contract.model.MeshInventoryDeadlineRule> = emptyList(),
    val changeLogs: List<org.expert.link.mesh.contract.model.MeshInventoryChangeLog> = emptyList(),
    val dashboard: org.expert.link.mesh.contract.model.MeshInventoryDashboardSnapshot? = null,
    val searchResult: org.expert.link.mesh.contract.model.MeshInventorySearchResult? = null,
    val searchLoading: Boolean = false,
    val searchError: String? = null,
    val peers: List<MeshPairedPeer> = emptyList(),
    val lastSyncResult: MeshInventorySyncResult? = null,
    val lastSyncAt: kotlinx.datetime.Instant? = null,
    val lastEventSequence: Long? = null,
    val busyAction: InventoryBusyAction? = null,
    val error: String? = null,
    val message: String? = null,
)

class InventoryStore(
    private val session: NodeSessionController,
) {
    private val _state = MutableStateFlow(InventoryOverviewState())
    val state: StateFlow<InventoryOverviewState> = _state.asStateFlow()

    fun updateRuntime(status: NodeRuntimeStatus, localPeerId: String?) {
        _state.update { it.copy(runtimeStatus = status, localPeerId = localPeerId) }
    }

    fun selectSection(section: InventorySection) {
        _state.update { it.copy(selectedSection = section) }
    }

    fun selectOrganization(organizationId: String?) {
        _state.update { it.copy(selectedOrganizationId = organizationId) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun reportError(message: String) {
        _state.update { it.copy(error = message, busyAction = null) }
    }

    fun reportMessage(message: String) {
        _state.update { it.copy(message = message, busyAction = null) }
    }

    suspend fun refresh() {
        if (state.value.runtimeStatus != NodeRuntimeStatus.RUNNING) return
        _state.update { it.copy(busyAction = InventoryBusyAction.REFRESH, error = null) }
        val previous = state.value
        val result = session.withNode { node ->
            val organizations = node.organizations().sortedBy { it.name.lowercase() }
            val resolvedOrgId = previous.selectedOrganizationId
                ?.takeIf { id -> organizations.any { it.organizationId == id } }
                ?: organizations.firstOrNull()?.organizationId
            val members = resolvedOrgId?.let { node.organizationMembers(it) }.orEmpty()
            val roles = resolvedOrgId?.let { node.roles(it) }.orEmpty()
            val categories = resolvedOrgId?.let { node.inventoryCategories(it) }.orEmpty()
            val subcategories = resolvedOrgId?.let { node.inventorySubcategories(it) }.orEmpty()
            val tags = resolvedOrgId?.let { node.inventoryTags(it) }.orEmpty()
            val attributeDefinitions = resolvedOrgId?.let { node.inventoryAttributeDefinitions(it) }.orEmpty()
            val templates = resolvedOrgId?.let { node.inventoryCategoryTemplates(it) }.orEmpty()
            val locations = resolvedOrgId?.let { node.inventoryLocations(it) }.orEmpty()
            val owners = resolvedOrgId?.let { node.inventoryOwners(it) }.orEmpty()
            val departments = resolvedOrgId?.let { node.inventoryDepartments(it) }.orEmpty()
            val costCenters = resolvedOrgId?.let { node.inventoryCostCenters(it) }.orEmpty()
            val legalHolders = resolvedOrgId?.let { node.inventoryLegalHolders(it) }.orEmpty()
            val suppliers = resolvedOrgId?.let { node.inventorySuppliers(it) }.orEmpty()
            val fundingSources = resolvedOrgId?.let { node.inventoryFundingSources(it) }.orEmpty()
            val items = resolvedOrgId?.let { node.inventoryItems(it) }.orEmpty()
            val labelTemplates = resolvedOrgId?.let { node.inventoryLabelTemplates(it) }.orEmpty()
            val printTasks = resolvedOrgId?.let { node.inventoryPrintTasks(it) }.orEmpty()
            val sessions = resolvedOrgId?.let { node.inventorySessions(it) }.orEmpty()
            val exports = resolvedOrgId?.let { node.inventoryExports(it) }.orEmpty()
            val incidents = resolvedOrgId?.let { node.inventoryIncidents(it, null, null) }.orEmpty()
            val alerts = resolvedOrgId?.let { node.inventoryAlerts(it) }.orEmpty()
            val reminders = resolvedOrgId?.let { node.inventoryReminders(it) }.orEmpty()
            val ruleThresholds = resolvedOrgId?.let { node.inventoryRuleThresholds(it) }.orEmpty()
            val deadlineRules = resolvedOrgId?.let { node.inventoryDeadlineRules(it) }.orEmpty()
            val changeLogs = resolvedOrgId?.let { node.inventoryChangeLogs(it, null) }.orEmpty()
            val dashboard = resolvedOrgId?.let { node.inventoryDashboardSnapshot(it) }
            val peers = node.peers()
            val events = resolvedOrgId?.let { node.inventoryEvents(it, previous.lastEventSequence) }.orEmpty()
            val lastSequence = (listOfNotNull(previous.lastEventSequence) + events.map { it.sequence }).maxOrNull()
            previous.copy(
                organizations = organizations,
                selectedOrganizationId = resolvedOrgId,
                members = members,
                roles = roles,
                permissions = resolveInventoryPermissions(
                    localPeerId = previous.localPeerId,
                    members = members,
                    roles = roles,
                ),
                categories = categories,
                subcategories = subcategories,
                tags = tags,
                attributeDefinitions = attributeDefinitions,
                templates = templates,
                locations = locations,
                owners = owners,
                departments = departments,
                costCenters = costCenters,
                legalHolders = legalHolders,
                suppliers = suppliers,
                fundingSources = fundingSources,
                items = items.sortedByDescending { it.updatedAt },
                labelTemplates = labelTemplates.sortedBy { it.name },
                printTasks = printTasks.sortedByDescending { it.updatedAt },
                sessions = sessions.sortedByDescending { it.updatedAt },
                exports = exports.sortedByDescending { it.updatedAt },
                reviewItems = filterReviewItems(items),
                incidents = incidents.sortedByDescending { it.updatedAt },
                alerts = alerts.sortedByDescending { it.createdAt },
                reminders = reminders.sortedByDescending { it.updatedAt },
                ruleThresholds = ruleThresholds,
                deadlineRules = deadlineRules,
                changeLogs = changeLogs.sortedByDescending { it.changedAt },
                dashboard = dashboard,
                peers = peers,
                lastEventSequence = lastSequence,
                busyAction = null,
                error = null,
            )
        }
        _state.update {
            result.getOrElse { error ->
                it.copy(
                    busyAction = null,
                    error = error.message ?: "Не удалось обновить инвентарь",
                )
            }
        }
    }

    suspend fun createOrganization(name: String, description: String?) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название организации") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_ORG, error = null) }
        val result = session.withNode { node ->
            node.createOrganization(
                MeshCreateOrganizationCommand(
                    name = name.trim(),
                    description = description?.trim().orEmpty().ifBlank { null },
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { created ->
                    current.copy(
                        busyAction = null,
                        message = "Организация создана",
                        selectedOrganizationId = created.organizationId,
                    )
                },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать организацию")
                },
            )
        }
        refresh()
    }

    suspend fun createRole(organizationId: String, name: String, description: String?, permissions: Set<MeshInventoryPermission>) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название роли") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_ROLE, error = null) }
        val result = session.withNode { node ->
            node.createRole(
                MeshCreateRoleCommand(
                    organizationId = organizationId,
                    name = name.trim(),
                    description = description?.trim().orEmpty().ifBlank { null },
                    permissions = permissions,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Роль создана") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать роль")
                },
            )
        }
        refresh()
    }

    suspend fun addMember(
        organizationId: String,
        peerId: String,
        displayName: String,
        roleIds: Set<String>,
        departmentId: String? = null,
        locationIds: Set<String> = emptySet(),
        position: String? = null,
        isCommissionMember: Boolean = false,
    ) {
        if (peerId.isBlank()) {
            _state.update { it.copy(error = "Введите ID узла") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.ADD_MEMBER, error = null) }
        val result = session.withNode { node ->
            node.addOrganizationMember(
                MeshAddOrganizationMemberCommand(
                    organizationId = organizationId,
                    peerId = peerId.trim(),
                    displayName = displayName.trim().ifBlank { peerId.trim() },
                    roleIds = roleIds,
                    departmentId = departmentId,
                    locationIds = locationIds,
                    position = position?.trim().orEmpty().ifBlank { null },
                    isCommissionMember = isCommissionMember,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Участник добавлен") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось добавить участника")
                },
            )
        }
        refresh()
    }

    suspend fun updateMember(
        organizationId: String,
        peerId: String,
        displayName: String,
        roleIds: Set<String>,
        departmentId: String?,
        locationIds: Set<String>,
        position: String?,
        isCommissionMember: Boolean,
        status: MeshOrganizationMemberStatus,
    ) {
        if (peerId.isBlank()) {
            _state.update { it.copy(error = "Не указан участник для обновления") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.UPDATE_MEMBER, error = null) }
        val result = session.withNode { node ->
            node.updateOrganizationMember(
                MeshUpdateOrganizationMemberCommand(
                    organizationId = organizationId,
                    peerId = peerId,
                    displayName = displayName.trim().ifBlank { null },
                    roleIds = roleIds,
                    departmentId = departmentId,
                    locationIds = locationIds,
                    position = position?.trim().orEmpty().ifBlank { null },
                    isCommissionMember = isCommissionMember,
                    status = status,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Участник обновлён") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось обновить участника")
                },
            )
        }
        refresh()
    }

    suspend fun searchInventory(
        organizationId: String,
        text: String?,
        inventoryNumber: String?,
        serialNumber: String?,
        qrCode: String?,
        barcode: String?,
        status: Set<MeshInventoryStatus>,
        condition: Set<MeshInventoryCondition>,
        categoryIds: Set<String>,
        locationIds: Set<String>,
        departmentIds: Set<String>,
        ownerIds: Set<String>,
        incidentOnly: Boolean,
        sessionId: String?,
    ) {
        _state.update { it.copy(searchLoading = true, searchError = null, busyAction = InventoryBusyAction.SEARCH) }
        val result = session.withNode { node ->
            node.searchInventory(
                MeshInventorySearchCommand(
                    MeshInventorySearchQuery(
                        organizationId = organizationId,
                        text = text?.trim().orEmpty().ifBlank { null },
                        inventoryNumber = inventoryNumber?.trim().orEmpty().ifBlank { null },
                        serialNumber = serialNumber?.trim().orEmpty().ifBlank { null },
                        qrCode = qrCode?.trim().orEmpty().ifBlank { null },
                        barcode = barcode?.trim().orEmpty().ifBlank { null },
                        filter = MeshInventoryFilterSet(
                            categoryIds = categoryIds,
                            locationIds = locationIds,
                            departmentIds = departmentIds,
                            ownerIds = ownerIds,
                            status = status,
                            condition = condition,
                            incidentOnly = incidentOnly,
                            sessionId = sessionId,
                        ),
                        sort = MeshInventorySortMode.UPDATED_AT_DESC,
                    ),
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { search ->
                    current.copy(
                        searchResult = search,
                        searchLoading = false,
                        searchError = null,
                        busyAction = null,
                    )
                },
                onFailure = { error ->
                    current.copy(
                        searchLoading = false,
                        searchError = error.message ?: "Не удалось выполнить поиск",
                        busyAction = null,
                    )
                },
            )
        }
    }

    fun clearSearch() {
        _state.update { it.copy(searchResult = null, searchError = null, searchLoading = false) }
    }

    suspend fun createCategory(organizationId: String, name: String, description: String?) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название категории") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_CATEGORY, error = null) }
        val result = session.withNode { node ->
            node.createInventoryCategory(
                MeshCreateInventoryCategoryCommand(
                    organizationId = organizationId,
                    name = name.trim(),
                    description = description?.trim().orEmpty().ifBlank { null },
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Категория создана") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать категорию")
                },
            )
        }
        refresh()
    }

    suspend fun createSubcategory(organizationId: String, categoryId: String, name: String, description: String?) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название подкатегории") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_SUBCATEGORY, error = null) }
        val result = session.withNode { node ->
            node.createInventorySubcategory(
                MeshCreateInventorySubcategoryCommand(
                    organizationId = organizationId,
                    categoryId = categoryId,
                    name = name.trim(),
                    description = description?.trim().orEmpty().ifBlank { null },
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Подкатегория создана") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать подкатегорию")
                },
            )
        }
        refresh()
    }

    suspend fun createTag(organizationId: String, name: String, color: String?) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название тега") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_TAG, error = null) }
        val result = session.withNode { node ->
            node.createInventoryTag(
                MeshCreateInventoryTagCommand(
                    organizationId = organizationId,
                    name = name.trim(),
                    color = color?.trim().orEmpty().ifBlank { null },
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Тег создан") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать тег")
                },
            )
        }
        refresh()
    }

    suspend fun createAttributeDefinition(
        organizationId: String,
        key: String,
        label: String,
        type: MeshInventoryAttributeType,
        required: Boolean,
    ) {
        if (key.isBlank() || label.isBlank()) {
            _state.update { it.copy(error = "Заполните ключ и название атрибута") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_ATTRIBUTE, error = null) }
        val result = session.withNode { node ->
            node.createInventoryAttributeDefinition(
                MeshCreateInventoryAttributeDefinitionCommand(
                    organizationId = organizationId,
                    key = key.trim(),
                    label = label.trim(),
                    type = type,
                    required = required,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Атрибут создан") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать атрибут")
                },
            )
        }
        refresh()
    }

    suspend fun createTemplate(
        organizationId: String,
        categoryId: String,
        name: String,
        fields: List<MeshInventoryFieldTemplate>,
    ) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название шаблона") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_TEMPLATE, error = null) }
        val result = session.withNode { node ->
            node.createInventoryCategoryTemplate(
                MeshCreateInventoryCategoryTemplateCommand(
                    organizationId = organizationId,
                    categoryId = categoryId,
                    name = name.trim(),
                    fields = fields,
                    requiredFields = fields.filter { it.required }
                        .map { MeshInventoryRequiredFieldRule(it.fieldId, null) },
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Шаблон создан") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать шаблон")
                },
            )
        }
        refresh()
    }

    suspend fun createLocation(
        organizationId: String,
        name: String,
        description: String?,
        parentLocationId: String?,
        departmentId: String?,
        locationType: MeshInventoryLocationType = MeshInventoryLocationType.OTHER,
        archived: Boolean = false,
    ) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название локации") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_LOCATION, error = null) }
        val result = session.withNode { node ->
            node.createInventoryLocation(
                MeshCreateInventoryLocationCommand(
                    organizationId = organizationId,
                    name = name.trim(),
                    description = description?.trim().orEmpty().ifBlank { null },
                    parentLocationId = parentLocationId,
                    departmentId = departmentId,
                    locationType = locationType,
                    archived = archived,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Локация создана") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать локацию")
                },
            )
        }
        refresh()
    }

    suspend fun updateLocation(
        locationId: String,
        name: String,
        description: String?,
        parentLocationId: String?,
        departmentId: String?,
        locationType: MeshInventoryLocationType?,
        archived: Boolean,
    ) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название локации") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.UPDATE_LOCATION, error = null) }
        val result = session.withNode { node ->
            node.updateInventoryLocation(
                MeshUpdateInventoryLocationCommand(
                    locationId = locationId,
                    name = name.trim(),
                    description = description?.trim().orEmpty().ifBlank { null },
                    parentLocationId = parentLocationId,
                    departmentId = departmentId,
                    locationType = locationType,
                    archived = archived,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Локация обновлена") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось обновить локацию")
                },
            )
        }
        refresh()
    }

    suspend fun createOwner(
        organizationId: String,
        name: String,
        code: String?,
        departmentId: String?,
        locationIds: Set<String>,
        archived: Boolean = false,
        type: MeshInventoryOwnerType = MeshInventoryOwnerType.PERSON,
    ) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название МОЛа") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_OWNER, error = null) }
        val result = session.withNode { node ->
            node.createInventoryOwner(
                MeshCreateInventoryOwnerCommand(
                    organizationId = organizationId,
                    name = name.trim(),
                    type = type,
                    code = code?.trim().orEmpty().ifBlank { null },
                    departmentId = departmentId,
                    locationIds = locationIds,
                    archived = archived,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "МОЛ сохранён") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось сохранить МОЛа")
                },
            )
        }
        refresh()
    }

    suspend fun updateOwner(
        ownerId: String,
        name: String,
        code: String?,
        departmentId: String?,
        locationIds: Set<String>,
        archived: Boolean,
        type: MeshInventoryOwnerType = MeshInventoryOwnerType.PERSON,
    ) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название МОЛа") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.UPDATE_OWNER, error = null) }
        val result = session.withNode { node ->
            node.updateInventoryOwner(
                MeshUpdateInventoryOwnerCommand(
                    ownerId = ownerId,
                    name = name.trim(),
                    type = type,
                    code = code?.trim().orEmpty().ifBlank { null },
                    departmentId = departmentId,
                    locationIds = locationIds,
                    archived = archived,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "МОЛ обновлён") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось обновить МОЛа")
                },
            )
        }
        refresh()
    }

    suspend fun createDepartment(
        organizationId: String,
        name: String,
        parentDepartmentId: String?,
        code: String?,
        locationIds: Set<String>,
        ownerIds: Set<String>,
        archived: Boolean = false,
    ) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название подразделения") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_DEPARTMENT, error = null) }
        val result = session.withNode { node ->
            node.createInventoryDepartment(
                MeshCreateInventoryDepartmentCommand(
                    organizationId = organizationId,
                    name = name.trim(),
                    parentDepartmentId = parentDepartmentId,
                    code = code?.trim().orEmpty().ifBlank { null },
                    locationIds = locationIds,
                    ownerIds = ownerIds,
                    archived = archived,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Подразделение создано") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать подразделение")
                },
            )
        }
        refresh()
    }

    suspend fun updateDepartment(
        departmentId: String,
        name: String,
        parentDepartmentId: String?,
        code: String?,
        locationIds: Set<String>,
        ownerIds: Set<String>,
        archived: Boolean,
    ) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название подразделения") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.UPDATE_DEPARTMENT, error = null) }
        val result = session.withNode { node ->
            node.updateInventoryDepartment(
                MeshUpdateInventoryDepartmentCommand(
                    departmentId = departmentId,
                    name = name.trim(),
                    parentDepartmentId = parentDepartmentId,
                    code = code?.trim().orEmpty().ifBlank { null },
                    locationIds = locationIds,
                    ownerIds = ownerIds,
                    archived = archived,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Подразделение обновлено") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось обновить подразделение")
                },
            )
        }
        refresh()
    }

    suspend fun createCostCenter(organizationId: String, name: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название центра затрат") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_COST_CENTER, error = null) }
        val result = session.withNode { node ->
            node.createInventoryCostCenter(
                MeshCreateInventoryCostCenterCommand(
                    organizationId = organizationId,
                    code = name.trim(),
                    name = name.trim(),
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Центр затрат создан") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать центр затрат")
                },
            )
        }
        refresh()
    }

    suspend fun createLegalHolder(organizationId: String, name: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название балансодержателя") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_LEGAL_HOLDER, error = null) }
        val result = session.withNode { node ->
            node.createInventoryLegalHolder(
                MeshCreateInventoryLegalHolderCommand(
                    organizationId = organizationId,
                    name = name.trim(),
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Балансодержатель создан") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать балансодержателя")
                },
            )
        }
        refresh()
    }

    suspend fun createSupplier(organizationId: String, name: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите название поставщика") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_SUPPLIER, error = null) }
        val result = session.withNode { node ->
            node.createInventorySupplier(
                MeshCreateInventorySupplierCommand(
                    organizationId = organizationId,
                    name = name.trim(),
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Поставщик создан") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать поставщика")
                },
            )
        }
        refresh()
    }

    suspend fun createFundingSource(organizationId: String, name: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Введите источник финансирования") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_FUNDING_SOURCE, error = null) }
        val result = session.withNode { node ->
            node.createInventoryFundingSource(
                MeshCreateInventoryFundingSourceCommand(
                    organizationId = organizationId,
                    name = name.trim(),
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Источник финансирования создан") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать источник")
                },
            )
        }
        refresh()
    }

    suspend fun createRuleThreshold(
        organizationId: String,
        ruleType: MeshInventoryRuleType,
        thresholdValue: Long?,
    ) {
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_RULE_THRESHOLD, error = null) }
        val result = session.withNode { node ->
            node.createInventoryRuleThreshold(
                MeshCreateInventoryRuleThresholdCommand(
                    organizationId = organizationId,
                    ruleType = ruleType,
                    thresholdValue = thresholdValue,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Правило создано") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать правило")
                },
            )
        }
        refresh()
    }

    suspend fun createDeadlineRule(
        organizationId: String,
        target: MeshInventoryDeadlineTarget,
        daysBefore: Int,
    ) {
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_DEADLINE_RULE, error = null) }
        val result = session.withNode { node ->
            node.createInventoryDeadlineRule(
                MeshCreateInventoryDeadlineRuleCommand(
                    organizationId = organizationId,
                    target = target,
                    daysBefore = daysBefore,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Дедлайн-правило создано") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать правило")
                },
            )
        }
        refresh()
    }

    suspend fun createInventoryItem(
        organizationId: String,
        inventoryNumber: String,
        localNumber: String?,
        title: String,
        description: String?,
        serialNumber: String?,
        brand: String?,
        model: String?,
        manufacturer: String?,
        condition: MeshInventoryCondition,
        categoryId: String?,
        subcategoryId: String?,
        categoryName: String?,
        locationId: String?,
        locationName: String?,
        responsiblePerson: String?,
        responsibleDepartment: String?,
        ownerId: String?,
        departmentId: String?,
        responsibleOwnerIds: Set<String>,
        costCenterId: String?,
        legalHolderId: String?,
        supplierId: String?,
        fundingSourceId: String?,
        tagIds: Set<String>,
        attributes: List<org.expert.link.mesh.contract.model.MeshInventoryAttributeValue>,
        qrCode: String?,
        barcode: String?,
    ) {
        if (inventoryNumber.isBlank() || title.isBlank()) {
            _state.update { it.copy(error = "Заполните инвентарный номер и название оборудования") }
            return
        }
        if (departmentId.isNullOrBlank()) {
            _state.update { it.copy(error = "Выберите подразделение") }
            return
        }
        if (locationId.isNullOrBlank() && locationName.isNullOrBlank()) {
            _state.update { it.copy(error = "Выберите локацию") }
            return
        }
        if (ownerId.isNullOrBlank() && responsibleOwnerIds.isEmpty()) {
            _state.update { it.copy(error = "Выберите МОЛа") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_ITEM, error = null) }
        val result = session.withNode { node ->
            val resolvedCategoryId = categoryId
                ?: resolveCategoryId(node.inventoryCategories(organizationId), organizationId, categoryName, node)
            val resolvedLocationId = locationId
                ?: resolveLocationId(node.inventoryLocations(organizationId), organizationId, locationName, node)
            val ownerMap = node.inventoryOwners(organizationId).associateBy { it.ownerId }
            val departmentMap = node.inventoryDepartments(organizationId).associateBy { it.departmentId }
            val resolvedOwnerIds = (setOfNotNull(ownerId) + responsibleOwnerIds).filter { ownerMap.containsKey(it) }.toSet()
            val resolvedOwnerId = ownerId ?: resolvedOwnerIds.firstOrNull()
            val resolvedDepartmentId = departmentId ?: resolvedOwnerId?.let { ownerMap[it]?.departmentId }
            val resolvedResponsiblePerson = responsiblePerson?.trim().orEmpty().ifBlank {
                resolvedOwnerIds.mapNotNull { ownerMap[it]?.name }.takeIf { it.isNotEmpty() }?.joinToString(", ")
            }
            val resolvedResponsibleDepartment = responsibleDepartment?.trim().orEmpty().ifBlank {
                resolvedDepartmentId?.let { departmentMap[it]?.name }
            }
            node.createInventoryItem(
                MeshCreateInventoryItemCommand(
                    organizationId = organizationId,
                    inventoryNumber = inventoryNumber.trim(),
                    localNumber = localNumber?.trim().orEmpty().ifBlank { null },
                    qrCode = qrCode?.trim().orEmpty().ifBlank { null },
                    barcode = barcode?.trim().orEmpty().ifBlank { null },
                    categoryId = resolvedCategoryId,
                    subcategoryId = subcategoryId,
                    title = title.trim(),
                    description = description?.trim().orEmpty().ifBlank { null },
                    brand = brand?.trim().orEmpty().ifBlank { null },
                    model = model?.trim().orEmpty().ifBlank { null },
                    serialNumber = serialNumber?.trim().orEmpty().ifBlank { null },
                    manufacturer = manufacturer?.trim().orEmpty().ifBlank { null },
                    condition = condition,
                    locationId = resolvedLocationId,
                    responsiblePerson = resolvedResponsiblePerson,
                    responsibleDepartment = resolvedResponsibleDepartment,
                    ownerId = resolvedOwnerId,
                    departmentId = resolvedDepartmentId,
                    responsibleOwnerIds = resolvedOwnerIds,
                    costCenterId = costCenterId,
                    legalHolderId = legalHolderId,
                    supplierId = supplierId,
                    fundingSourceId = fundingSourceId,
                    tagIds = tagIds,
                    attributes = attributes,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Объект создан") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать объект")
                },
            )
        }
        refresh()
    }

    suspend fun createSession(
        organizationId: String,
        title: String,
        description: String?,
        periodStart: kotlinx.datetime.Instant,
        periodEnd: kotlinx.datetime.Instant?,
        departmentIds: Set<String>,
        locationIds: Set<String>,
        ownerIds: Set<String>,
        requiresPhotoForDiscrepancy: Boolean,
        itemIds: Set<String>,
        memberPeerIds: Set<String>,
    ) {
        if (title.isBlank()) {
            _state.update { it.copy(error = "Введите название инвентаризации") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.CREATE_SESSION, error = null) }
        val result = session.withNode { node ->
            node.createInventorySession(
                MeshCreateInventorySessionCommand(
                    organizationId = organizationId,
                    title = title.trim(),
                    description = description?.trim().orEmpty().ifBlank { null },
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                    departmentIds = departmentIds,
                    locationIds = locationIds,
                    ownerIds = ownerIds,
                    workflowStatus = MeshInventoryWorkflowStatus.CREATED,
                    requiresPhotoForDiscrepancy = requiresPhotoForDiscrepancy,
                    itemIds = itemIds,
                    memberPeerIds = memberPeerIds,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Инвентаризация создана") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось создать инвентаризацию")
                },
            )
        }
        refresh()
    }

    suspend fun requestExport(
        organizationId: String,
        sessionId: String,
        format: MeshInventoryExportFormat,
    ) {
        if (sessionId.isBlank()) {
            _state.update { it.copy(error = "Выберите сессию для отчёта") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.REQUEST_EXPORT, error = null) }
        val result = session.withNode { node ->
            node.requestInventoryExport(
                MeshRequestInventoryExportCommand(
                    organizationId = organizationId,
                    sessionId = sessionId,
                    format = format,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Экспорт запрошен") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось запросить экспорт")
                },
            )
        }
        refresh()
    }

    suspend fun syncWithPeer(targetPeerId: String, organizationId: String) {
        if (targetPeerId.isBlank()) {
            _state.update { it.copy(error = "Выберите peer для синхронизации") }
            return
        }
        _state.update { it.copy(busyAction = InventoryBusyAction.SYNC, error = null) }
        val result = session.withNode { node ->
            node.syncInventoryWithPeer(
                MeshInventorySyncCommand(
                    targetPeerId = targetPeerId,
                    organizationId = organizationId,
                    sinceSequence = state.value.lastEventSequence,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { sync ->
                    current.copy(
                        busyAction = null,
                        lastSyncResult = sync,
                        lastSyncAt = Clock.System.now(),
                        message = "Синхронизация завершена",
                    )
                },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось синхронизировать")
                },
            )
        }
        refresh()
    }

    private suspend fun resolveCategoryId(
        current: List<MeshInventoryCategory>,
        organizationId: String,
        name: String?,
        node: org.expert.link.mesh.contract.api.MeshNode,
    ): String? {
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val existing = current.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) return existing.categoryId
        return node.createInventoryCategory(
            MeshCreateInventoryCategoryCommand(
                organizationId = organizationId,
                name = trimmed,
            ),
        ).categoryId
    }

    private suspend fun resolveLocationId(
        current: List<MeshInventoryLocation>,
        organizationId: String,
        name: String?,
        node: org.expert.link.mesh.contract.api.MeshNode,
    ): String? {
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val existing = current.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) return existing.locationId
        return node.createInventoryLocation(
            MeshCreateInventoryLocationCommand(
                organizationId = organizationId,
                name = trimmed,
            ),
        ).locationId
    }
}

fun resolveInventoryPermissions(
    localPeerId: String?,
    members: List<MeshOrganizationMember>,
    roles: List<MeshRole>,
): Set<MeshInventoryPermission> {
    if (localPeerId.isNullOrBlank()) return emptySet()
    val member = members.firstOrNull { it.peerId == localPeerId } ?: return emptySet()
    return roles.filter { it.roleId in member.roleIds }
        .flatMap { it.permissions }
        .toSet()
}

fun filterReviewItems(items: List<MeshInventoryItem>): List<MeshInventoryItem> =
    items.filter { it.currentStatus == MeshInventoryStatus.UNDER_REVIEW || it.currentStatus == MeshInventoryStatus.REQUIRES_UPDATE }

fun reviewStatusToInventoryStatus(status: org.expert.link.mesh.contract.model.MeshInventoryReviewStatus): MeshInventoryStatus =
    when (status) {
        org.expert.link.mesh.contract.model.MeshInventoryReviewStatus.APPROVED -> MeshInventoryStatus.CONFIRMED
        org.expert.link.mesh.contract.model.MeshInventoryReviewStatus.REJECTED -> MeshInventoryStatus.REJECTED
        org.expert.link.mesh.contract.model.MeshInventoryReviewStatus.REQUIRES_UPDATE -> MeshInventoryStatus.REQUIRES_UPDATE
    }
