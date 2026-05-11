package org.expert.link.app.shared.screen.inventory

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.expert.link.app.shared.navigation.BaseComponent
import org.expert.link.app.shared.navigation.Config
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.platform.InventoryExportDocument
import org.expert.link.app.shared.platform.LocalArtifact
import org.expert.link.app.shared.presentation.InventorySection
import org.expert.link.app.shared.presentation.InventoryOverviewState
import org.expert.link.app.shared.presentation.InventoryStore
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.presentation.NodeSessionController
import org.expert.link.app.shared.presentation.NodeSessionState
import org.expert.link.app.shared.ui.components.asUiText
import org.expert.link.app.shared.ui.components.shortId
import org.expert.link.mesh.contract.api.MeshBatchPrintInventoryLabelsRequest
import org.expert.link.mesh.contract.api.MeshCreateInventoryCategoryCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryCostCenterCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryDepartmentCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryItemCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryLocationCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryOwnerCommand
import org.expert.link.mesh.contract.api.MeshCreateInventorySessionCommand
import org.expert.link.mesh.contract.api.MeshCreateOrganizationCommand
import org.expert.link.mesh.contract.api.MeshGenerateInventoryCodeCommand
import org.expert.link.mesh.contract.api.MeshRegenerateInventoryCodeCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryExportStatusCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryDepartmentCommand
import org.expert.link.mesh.contract.model.MeshInventoryCondition
import org.expert.link.mesh.contract.model.MeshInventoryAttributeType
import org.expert.link.mesh.contract.model.MeshInventoryAttributeValue
import org.expert.link.mesh.contract.model.MeshInventoryBarcodeFormat
import org.expert.link.mesh.contract.model.MeshInventoryCodeType
import org.expert.link.mesh.contract.model.MeshInventoryDeadlineTarget
import org.expert.link.mesh.contract.model.MeshInventoryExportFormat
import org.expert.link.mesh.contract.model.MeshInventoryExportStatus
import org.expert.link.mesh.contract.model.MeshInventoryExportTask
import org.expert.link.mesh.contract.model.MeshInventoryFieldTemplate
import org.expert.link.mesh.contract.model.MeshInventoryLabelFieldKey
import org.expert.link.mesh.contract.model.MeshInventoryLocationType
import org.expert.link.mesh.contract.model.MeshInventoryOwnerType
import org.expert.link.mesh.contract.model.MeshInventoryPermission
import org.expert.link.mesh.contract.model.MeshInventoryPrintTask
import org.expert.link.mesh.contract.model.MeshInventoryReviewStatus
import org.expert.link.mesh.contract.model.MeshInventoryRuleType
import org.expert.link.mesh.contract.model.MeshInventoryStatus
import org.expert.link.mesh.contract.api.MeshSubmitInventoryReviewCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryItemCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryOwnerCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryLocationCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryStatusCommand
import org.expert.link.mesh.contract.api.MeshUpdateOrganizationMemberCommand
import org.expert.link.mesh.contract.api.MeshUpdateOrganizationCommand
import org.expert.link.mesh.contract.model.MeshOrganizationMemberStatus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InventoryComponent(
    context: ComponentContext,
    onNavigate: (Config) -> Unit,
    onBack: () -> Unit,
) : BaseComponent(context, onNavigate, onBack), KoinComponent {
    private val session: NodeSessionController by inject()
    private val services: AppPlatformServices by inject()
    private val store = InventoryStore(session)

    val state = store.state
    val sessionState: StateFlow<NodeSessionState> = session.state
    val capabilities
        get() = services.capabilities

    init {
        observeSession()
        startPolling()
    }

    private fun observeSession() {
        componentScope.launch {
            session.state.collectLatest { sessionState ->
                store.updateRuntime(sessionState.status, sessionState.profile?.peerId)
                if (sessionState.status == NodeRuntimeStatus.RUNNING) {
                    store.refresh()
                }
            }
        }
    }

    private fun startPolling() {
        componentScope.launch {
            while (isActive) {
                if (session.state.value.status == NodeRuntimeStatus.RUNNING) {
                    store.refresh()
                }
                delay(4_000)
            }
        }
    }

    fun selectSection(section: InventorySection) {
        store.selectSection(section)
    }

    fun selectOrganization(organizationId: String?) {
        store.selectOrganization(organizationId)
        componentScope.launch { store.refresh() }
    }

    fun refreshNow() {
        componentScope.launch { store.refresh() }
    }

    fun clearError() = store.clearError()

    fun clearMessage() = store.clearMessage()

    fun updateOrganizationInline(
        organizationId: String,
        name: String,
        description: String?,
    ) {
        if (name.isBlank()) {
            store.reportError("Название организации не может быть пустым")
            return
        }
        componentScope.launch {
            val result = session.withNode { node ->
                node.updateOrganization(
                    MeshUpdateOrganizationCommand(
                        organizationId = organizationId,
                        name = name.trim(),
                        description = description?.trim().orEmpty().ifBlank { null },
                    ),
                )
            }
            result.fold(
                onSuccess = {
                    store.reportMessage("Организация обновлена")
                    store.refresh()
                },
                onFailure = { error ->
                    store.reportError(error.message ?: "Не удалось обновить организацию")
                },
            )
        }
    }

    fun updateLocationInline(
        locationId: String,
        name: String,
        description: String?,
        parentLocationId: String?,
        departmentId: String? = null,
        locationType: MeshInventoryLocationType? = null,
        archived: Boolean? = null,
    ) {
        if (name.isBlank()) {
            store.reportError("Название локации не может быть пустым")
            return
        }
        componentScope.launch {
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
            result.fold(
                onSuccess = {
                    store.reportMessage("Локация обновлена")
                    store.refresh()
                },
                onFailure = { error ->
                    store.reportError(error.message ?: "Не удалось обновить локацию")
                },
            )
        }
    }

    fun updateItemInline(
        inventoryItemId: String,
        expectedRevision: Long?,
        inventoryNumber: String,
        title: String,
        status: MeshInventoryStatus,
        condition: MeshInventoryCondition,
        locationId: String?,
        departmentId: String?,
        ownerId: String?,
        responsibleOwnerIds: Set<String>,
    ) {
        if (inventoryNumber.isBlank() || title.isBlank()) {
            store.reportError("Заполните номер и название объекта")
            return
        }
        componentScope.launch {
            val result = session.withNode { node ->
                val organizationId = store.state.value.selectedOrganizationId
                val ownerMap = organizationId?.let { node.inventoryOwners(it).associateBy { owner -> owner.ownerId } }.orEmpty()
                val departmentMap = organizationId?.let { node.inventoryDepartments(it).associateBy { department -> department.departmentId } }.orEmpty()
                val effectiveOwnerIds = (setOfNotNull(ownerId) + responsibleOwnerIds).filter { ownerMap.containsKey(it) }.toSet()
                val updated = node.updateInventoryItem(
                    MeshUpdateInventoryItemCommand(
                        inventoryItemId = inventoryItemId,
                        expectedRevision = expectedRevision,
                        inventoryNumber = inventoryNumber.trim(),
                        title = title.trim(),
                        condition = condition,
                        locationId = locationId,
                        departmentId = departmentId,
                        ownerId = ownerId ?: effectiveOwnerIds.firstOrNull(),
                        responsibleOwnerIds = effectiveOwnerIds,
                        responsiblePerson = effectiveOwnerIds.mapNotNull { ownerMap[it]?.name }
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString(", "),
                        responsibleDepartment = departmentId?.let { departmentMap[it]?.name },
                    ),
                )
                val revisionForStatus = updated?.revision ?: expectedRevision
                node.updateInventoryStatus(
                    MeshUpdateInventoryStatusCommand(
                        inventoryItemId = inventoryItemId,
                        expectedRevision = revisionForStatus,
                        status = status,
                    ),
                )
            }
            result.fold(
                onSuccess = {
                    store.reportMessage("Объект обновлён")
                    store.refresh()
                },
                onFailure = { error ->
                    store.reportError(error.message ?: "Не удалось обновить объект")
                },
            )
        }
    }

    fun refreshItemCode(
        organizationId: String,
        inventoryItemId: String,
        codeType: MeshInventoryCodeType,
    ) {
        componentScope.launch {
            val result = session.withNode { node ->
                val current = node.getInventoryCode(inventoryItemId, codeType)
                if (current == null) {
                    node.generateInventoryCode(
                        MeshGenerateInventoryCodeCommand(
                            organizationId = organizationId,
                            inventoryItemId = inventoryItemId,
                            codeType = codeType,
                            barcodeFormat = MeshInventoryBarcodeFormat.CODE_128,
                        ),
                    )
                } else {
                    node.regenerateInventoryCode(
                        MeshRegenerateInventoryCodeCommand(
                            organizationId = organizationId,
                            inventoryItemId = inventoryItemId,
                            codeType = codeType,
                            barcodeFormat = MeshInventoryBarcodeFormat.CODE_128,
                        ),
                    )
                }
            }
            result.fold(
                onSuccess = { code ->
                    val label = if (codeType == MeshInventoryCodeType.BARCODE) "Штрихкод" else "QR"
                    store.reportMessage("$label обновлён: ${code.displayValue}")
                    store.refresh()
                },
                onFailure = { error ->
                    store.reportError(error.message ?: "Не удалось обновить код")
                },
            )
        }
    }

    fun seedDemoData() {
        componentScope.launch {
            val result = session.withNode { node ->
                val organization = node.organizations()
                    .firstOrNull { it.name.equals("Демо организация", ignoreCase = true) }
                    ?: node.createOrganization(
                        MeshCreateOrganizationCommand(
                            name = "Демо организация",
                            description = "Тестовый набор для проверки интерфейса",
                        ),
                    )
                val organizationId = organization.organizationId

                val categories = node.inventoryCategories(organizationId)
                val laptops = categories.firstOrNull { it.name.equals("Ноутбуки", ignoreCase = true) }
                    ?: node.createInventoryCategory(
                        MeshCreateInventoryCategoryCommand(
                            organizationId = organizationId,
                            name = "Ноутбуки",
                            description = "Мобильные рабочие станции",
                        ),
                    )
                val displays = node.inventoryCategories(organizationId)
                    .firstOrNull { it.name.equals("Мониторы", ignoreCase = true) }
                    ?: node.createInventoryCategory(
                        MeshCreateInventoryCategoryCommand(
                            organizationId = organizationId,
                            name = "Мониторы",
                            description = "Панели и дисплеи",
                        ),
                    )

                val locations = node.inventoryLocations(organizationId)
                val mainLocation = locations.firstOrNull { it.name.equals("Главный склад", ignoreCase = true) }
                    ?: node.createInventoryLocation(
                        MeshCreateInventoryLocationCommand(
                            organizationId = organizationId,
                            name = "Главный склад",
                            description = "Центральное хранение",
                        ),
                    )
                val officeLocation = node.inventoryLocations(organizationId)
                    .firstOrNull { it.name.equals("Офис 3 этаж", ignoreCase = true) }
                    ?: node.createInventoryLocation(
                        MeshCreateInventoryLocationCommand(
                            organizationId = organizationId,
                            name = "Офис 3 этаж",
                            parentLocationId = mainLocation.locationId,
                        ),
                    )

                val owners = node.inventoryOwners(organizationId)
                if (owners.none { it.name.equals("Оргкомитет", ignoreCase = true) }) {
                    node.createInventoryOwner(
                        MeshCreateInventoryOwnerCommand(
                            organizationId = organizationId,
                            name = "Оргкомитет",
                        ),
                    )
                }
                val departments = node.inventoryDepartments(organizationId)
                if (departments.none { it.name.equals("ИТ-отдел", ignoreCase = true) }) {
                    node.createInventoryDepartment(
                        MeshCreateInventoryDepartmentCommand(
                            organizationId = organizationId,
                            name = "ИТ-отдел",
                        ),
                    )
                }
                val costCenters = node.inventoryCostCenters(organizationId)
                if (costCenters.none { it.name.equals("ЦФО-IT", ignoreCase = true) }) {
                    node.createInventoryCostCenter(
                        MeshCreateInventoryCostCenterCommand(
                            organizationId = organizationId,
                            code = "CC-IT",
                            name = "ЦФО-IT",
                        ),
                    )
                }

                val targetSize = 14
                val existingItems = node.inventoryItems(organizationId)
                if (existingItems.size < targetSize) {
                    val toCreate = targetSize - existingItems.size
                    repeat(toCreate) { index ->
                        val number = existingItems.size + index + 1
                        val created = node.createInventoryItem(
                            MeshCreateInventoryItemCommand(
                                organizationId = organizationId,
                                inventoryNumber = "DEMO-${number.toString().padStart(4, '0')}",
                                title = buildDemoItemTitle(number),
                                categoryId = if (number % 2 == 0) laptops.categoryId else displays.categoryId,
                                locationId = if (number % 2 == 0) mainLocation.locationId else officeLocation.locationId,
                                serialNumber = "SN-${number.toString().padStart(6, '0')}",
                                responsiblePerson = if (number % 2 == 0) "Иванов И.И." else "Петрова А.А.",
                                condition = if (number % 3 == 0) {
                                    MeshInventoryCondition.FAIR
                                } else {
                                    MeshInventoryCondition.GOOD
                                },
                            ),
                        )
                        runCatching {
                            node.generateInventoryCode(
                                MeshGenerateInventoryCodeCommand(
                                    organizationId = organizationId,
                                    inventoryItemId = created.inventoryItemId,
                                    codeType = MeshInventoryCodeType.QR,
                                ),
                            )
                        }
                        runCatching {
                            node.generateInventoryCode(
                                MeshGenerateInventoryCodeCommand(
                                    organizationId = organizationId,
                                    inventoryItemId = created.inventoryItemId,
                                    codeType = MeshInventoryCodeType.BARCODE,
                                    barcodeFormat = MeshInventoryBarcodeFormat.CODE_128,
                                ),
                            )
                        }
                    }
                }

                val sessions = node.inventorySessions(organizationId)
                if (sessions.none { it.title.startsWith("Демо сессия") }) {
                    val items = node.inventoryItems(organizationId)
                    val start = Clock.System.now()
                    val end = start.plus(14, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                    val memberPeerIds = node.organizationMembers(organizationId)
                        .map { it.peerId }
                        .toSet()
                        .ifEmpty { setOf(node.profile.peerId) }
                    node.createInventorySession(
                        MeshCreateInventorySessionCommand(
                            organizationId = organizationId,
                            title = "Демо сессия",
                            description = "Тестовая сессия для визуальной проверки экрана",
                            periodStart = start,
                            periodEnd = end,
                            itemIds = items.take(10).map { it.inventoryItemId }.toSet(),
                            memberPeerIds = memberPeerIds,
                        ),
                    )
                }

                organizationId
            }

            result.fold(
                onSuccess = { organizationId ->
                    store.selectOrganization(organizationId)
                    store.reportMessage("Демо-данные загружены")
                    store.refresh()
                },
                onFailure = { error ->
                    store.reportError(error.message ?: "Не удалось загрузить демо-данные")
                },
            )
        }
    }

    fun goBack() {
        onBack()
    }

    fun openItem(itemId: String) {
        onNavigate(Config.InventoryItem(itemId))
    }

    fun openSession(sessionId: String) {
        onNavigate(Config.InventorySession(sessionId))
    }

    fun openScanner() {
        onNavigate(Config.InventoryScanner)
    }

    fun printLabelsBatch(
        organizationId: String,
        itemIds: List<String>,
        templateId: String?,
        fields: List<MeshInventoryLabelFieldKey>?,
        includeBarcode: Boolean,
        includeQr: Boolean,
        onResult: (Result<MeshInventoryPrintTask>) -> Unit,
    ) {
        componentScope.launch {
            val result = session.withNode { node ->
                node.printInventoryLabelsBatch(
                    MeshBatchPrintInventoryLabelsRequest(
                        organizationId = organizationId,
                        itemIds = itemIds,
                        templateId = templateId,
                        fields = fields,
                        includeBarcode = includeBarcode,
                        includeQr = includeQr,
                    ),
                )
            }
            onResult(result)
            if (result.isSuccess) {
                store.refresh()
            }
        }
    }

    fun savePdf(path: String, fileName: String, onResult: (Result<String?>) -> Unit) {
        componentScope.launch {
            onResult(services.saveFileToDownloads(path, fileName))
        }
    }

    fun sharePdf(path: String, onResult: (Result<Unit>) -> Unit) {
        componentScope.launch {
            onResult(services.shareFile("Маркировка", path))
        }
    }

    fun printPdf(path: String, onResult: (Result<Unit>) -> Unit) {
        componentScope.launch {
            onResult(services.printFile(path))
        }
    }

    fun downloadExport(task: MeshInventoryExportTask, onResult: (Result<String?>) -> Unit) {
        componentScope.launch {
            val artifact = resolveOrCreateExportArtifact(task)
                .getOrElse { error ->
                    onResult(Result.failure(error))
                    return@launch
                }
            val saveResult = services.saveFileToDownloads(artifact.path, artifact.descriptor.fileName)
            onResult(
                saveResult.fold(
                    onSuccess = { savedPath ->
                        if (savedPath.isNullOrBlank()) {
                            Result.failure(IllegalStateException("Сохранение отменено"))
                        } else {
                            Result.success(savedPath)
                        }
                    },
                    onFailure = { error -> Result.failure(error) },
                ),
            )
        }
    }

    fun shareExport(task: MeshInventoryExportTask, onResult: (Result<Unit>) -> Unit) {
        componentScope.launch {
            val artifact = resolveOrCreateExportArtifact(task)
                .getOrElse { error ->
                    onResult(Result.failure(error))
                    return@launch
                }
            onResult(services.shareFile("Отчёт", artifact.path))
        }
    }

    fun openExport(task: MeshInventoryExportTask, onResult: (Result<Unit>) -> Unit) {
        componentScope.launch {
            val artifact = resolveOrCreateExportArtifact(task)
                .getOrElse { error ->
                    onResult(Result.failure(error))
                    return@launch
                }
            onResult(services.openFile(artifact.path))
        }
    }

    fun printExport(task: MeshInventoryExportTask, onResult: (Result<Unit>) -> Unit) {
        componentScope.launch {
            val artifact = resolveOrCreateExportArtifact(task)
                .getOrElse { error ->
                    onResult(Result.failure(error))
                    return@launch
                }
            onResult(services.printFile(artifact.path))
        }
    }

    private suspend fun resolveOrCreateExportArtifact(task: MeshInventoryExportTask): Result<LocalArtifact> {
        val existingDescriptor = task.resultDescriptor
        if (existingDescriptor != null) {
            val resolvedPath = services.resolveLocalArtifactPath(existingDescriptor.fileName).getOrNull()
            if (!resolvedPath.isNullOrBlank()) {
                return Result.success(LocalArtifact(resolvedPath, existingDescriptor))
            }
        }
        val document = buildExportDocument(state.value, task)
            ?: return Result.failure(IllegalStateException("Не удалось собрать данные для отчёта"))
        val sessionTitle = state.value.sessions.firstOrNull { it.sessionId == task.sessionId }?.title ?: "inventory-report"
        val artifact = services.createInventoryExportArtifact(
            document = document,
            format = task.format,
            suggestedFileName = "${sessionTitle}-${task.exportTaskId.shortId(6)}",
        )
        artifact.onSuccess { localArtifact ->
            session.withNode { node ->
                node.updateInventoryExportStatus(
                    MeshUpdateInventoryExportStatusCommand(
                        exportTaskId = task.exportTaskId,
                        status = MeshInventoryExportStatus.COMPLETED,
                        resultDescriptor = localArtifact.descriptor,
                    ),
                )
            }
            store.refresh()
        }
        return artifact
    }

    fun createOrganization(name: String, description: String?) {
        componentScope.launch {
            store.createOrganization(name, description)
        }
    }

    fun createRole(organizationId: String, name: String, description: String?, permissions: Set<MeshInventoryPermission>) {
        componentScope.launch {
            store.createRole(organizationId, name, description, permissions)
        }
    }

    fun addMember(
        organizationId: String,
        peerId: String,
        displayName: String,
        roleIds: Set<String>,
        departmentId: String? = null,
        locationIds: Set<String> = emptySet(),
        position: String? = null,
        isCommissionMember: Boolean = false,
    ) {
        componentScope.launch {
            store.addMember(
                organizationId = organizationId,
                peerId = peerId,
                displayName = displayName,
                roleIds = roleIds,
                departmentId = departmentId,
                locationIds = locationIds,
                position = position,
                isCommissionMember = isCommissionMember,
            )
        }
    }

    fun updateMember(
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
        componentScope.launch {
            store.updateMember(
                organizationId = organizationId,
                peerId = peerId,
                displayName = displayName,
                roleIds = roleIds,
                departmentId = departmentId,
                locationIds = locationIds,
                position = position,
                isCommissionMember = isCommissionMember,
                status = status,
            )
        }
    }

    fun createItem(
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
        attributes: List<MeshInventoryAttributeValue>,
        qrCode: String?,
        barcode: String?,
    ) {
        componentScope.launch {
            store.createInventoryItem(
                organizationId = organizationId,
                inventoryNumber = inventoryNumber,
                localNumber = localNumber,
                title = title,
                description = description,
                serialNumber = serialNumber,
                brand = brand,
                model = model,
                manufacturer = manufacturer,
                condition = condition,
                categoryId = categoryId,
                subcategoryId = subcategoryId,
                categoryName = categoryName,
                locationId = locationId,
                locationName = locationName,
                responsiblePerson = responsiblePerson,
                responsibleDepartment = responsibleDepartment,
                ownerId = ownerId,
                departmentId = departmentId,
                responsibleOwnerIds = responsibleOwnerIds,
                costCenterId = costCenterId,
                legalHolderId = legalHolderId,
                supplierId = supplierId,
                fundingSourceId = fundingSourceId,
                tagIds = tagIds,
                attributes = attributes,
                qrCode = qrCode,
                barcode = barcode,
            )
        }
    }

    fun searchInventory(
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
        componentScope.launch {
            store.searchInventory(
                organizationId = organizationId,
                text = text,
                inventoryNumber = inventoryNumber,
                serialNumber = serialNumber,
                qrCode = qrCode,
                barcode = barcode,
                status = status,
                condition = condition,
                categoryIds = categoryIds,
                locationIds = locationIds,
                departmentIds = departmentIds,
                ownerIds = ownerIds,
                incidentOnly = incidentOnly,
                sessionId = sessionId,
            )
        }
    }

    fun clearSearch() {
        store.clearSearch()
    }

    fun createCategory(organizationId: String, name: String, description: String?) {
        componentScope.launch { store.createCategory(organizationId, name, description) }
    }

    fun createSubcategory(organizationId: String, categoryId: String, name: String, description: String?) {
        componentScope.launch { store.createSubcategory(organizationId, categoryId, name, description) }
    }

    fun createTag(organizationId: String, name: String, color: String?) {
        componentScope.launch { store.createTag(organizationId, name, color) }
    }

    fun createAttributeDefinition(
        organizationId: String,
        key: String,
        label: String,
        type: MeshInventoryAttributeType,
        required: Boolean,
    ) {
        componentScope.launch { store.createAttributeDefinition(organizationId, key, label, type, required) }
    }

    fun createTemplate(
        organizationId: String,
        categoryId: String,
        name: String,
        fields: List<MeshInventoryFieldTemplate>,
    ) {
        componentScope.launch { store.createTemplate(organizationId, categoryId, name, fields) }
    }

    fun createLocation(
        organizationId: String,
        name: String,
        description: String?,
        parentLocationId: String?,
        departmentId: String?,
        locationType: MeshInventoryLocationType = MeshInventoryLocationType.OTHER,
        archived: Boolean = false,
    ) {
        componentScope.launch {
            store.createLocation(
                organizationId = organizationId,
                name = name,
                description = description,
                parentLocationId = parentLocationId,
                departmentId = departmentId,
                locationType = locationType,
                archived = archived,
            )
        }
    }

    fun updateLocation(
        locationId: String,
        name: String,
        description: String?,
        parentLocationId: String?,
        departmentId: String?,
        locationType: MeshInventoryLocationType?,
        archived: Boolean,
    ) {
        componentScope.launch {
            store.updateLocation(
                locationId = locationId,
                name = name,
                description = description,
                parentLocationId = parentLocationId,
                departmentId = departmentId,
                locationType = locationType,
                archived = archived,
            )
        }
    }

    fun createOwner(
        organizationId: String,
        name: String,
        code: String?,
        departmentId: String?,
        locationIds: Set<String>,
        archived: Boolean = false,
        type: MeshInventoryOwnerType = MeshInventoryOwnerType.PERSON,
    ) {
        componentScope.launch {
            store.createOwner(
                organizationId = organizationId,
                name = name,
                code = code,
                departmentId = departmentId,
                locationIds = locationIds,
                archived = archived,
                type = type,
            )
        }
    }

    fun updateOwner(
        ownerId: String,
        name: String,
        code: String?,
        departmentId: String?,
        locationIds: Set<String>,
        archived: Boolean,
        type: MeshInventoryOwnerType = MeshInventoryOwnerType.PERSON,
    ) {
        componentScope.launch {
            store.updateOwner(
                ownerId = ownerId,
                name = name,
                code = code,
                departmentId = departmentId,
                locationIds = locationIds,
                archived = archived,
                type = type,
            )
        }
    }

    fun createDepartment(
        organizationId: String,
        name: String,
        parentDepartmentId: String?,
        code: String?,
        locationIds: Set<String>,
        ownerIds: Set<String>,
        archived: Boolean = false,
    ) {
        componentScope.launch {
            store.createDepartment(
                organizationId = organizationId,
                name = name,
                parentDepartmentId = parentDepartmentId,
                code = code,
                locationIds = locationIds,
                ownerIds = ownerIds,
                archived = archived,
            )
        }
    }

    fun updateDepartment(
        departmentId: String,
        name: String,
        parentDepartmentId: String?,
        code: String?,
        locationIds: Set<String>,
        ownerIds: Set<String>,
        archived: Boolean,
    ) {
        componentScope.launch {
            store.updateDepartment(
                departmentId = departmentId,
                name = name,
                parentDepartmentId = parentDepartmentId,
                code = code,
                locationIds = locationIds,
                ownerIds = ownerIds,
                archived = archived,
            )
        }
    }

    fun createCostCenter(organizationId: String, name: String) {
        componentScope.launch { store.createCostCenter(organizationId, name) }
    }

    fun createLegalHolder(organizationId: String, name: String) {
        componentScope.launch { store.createLegalHolder(organizationId, name) }
    }

    fun createSupplier(organizationId: String, name: String) {
        componentScope.launch { store.createSupplier(organizationId, name) }
    }

    fun createFundingSource(organizationId: String, name: String) {
        componentScope.launch { store.createFundingSource(organizationId, name) }
    }

    fun createRuleThreshold(organizationId: String, ruleType: MeshInventoryRuleType, thresholdValue: Long?) {
        componentScope.launch { store.createRuleThreshold(organizationId, ruleType, thresholdValue) }
    }

    fun createDeadlineRule(organizationId: String, target: MeshInventoryDeadlineTarget, daysBefore: Int) {
        componentScope.launch { store.createDeadlineRule(organizationId, target, daysBefore) }
    }

    fun createSession(
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
        componentScope.launch {
            store.createSession(
                organizationId = organizationId,
                title = title,
                description = description,
                periodStart = periodStart,
                periodEnd = periodEnd,
                departmentIds = departmentIds,
                locationIds = locationIds,
                ownerIds = ownerIds,
                requiresPhotoForDiscrepancy = requiresPhotoForDiscrepancy,
                itemIds = itemIds,
                memberPeerIds = memberPeerIds,
            )
        }
    }

    fun requestExport(organizationId: String, sessionId: String, format: MeshInventoryExportFormat) {
        componentScope.launch {
            store.requestExport(organizationId, sessionId, format)
        }
    }

    fun syncWithPeer(targetPeerId: String, organizationId: String) {
        componentScope.launch {
            store.syncWithPeer(targetPeerId, organizationId)
        }
    }

    fun submitReview(itemId: String, status: MeshInventoryReviewStatus, comment: String?) {
        componentScope.launch {
            session.withNode { node ->
                node.submitInventoryReview(
                    MeshSubmitInventoryReviewCommand(
                        inventoryItemId = itemId,
                        status = status,
                        comment = comment?.trim().orEmpty().ifBlank { null },
                    ),
                )
                node.updateInventoryStatus(
                    MeshUpdateInventoryStatusCommand(
                        inventoryItemId = itemId,
                        status = when (status) {
                            MeshInventoryReviewStatus.APPROVED -> MeshInventoryStatus.CONFIRMED
                            MeshInventoryReviewStatus.REJECTED -> MeshInventoryStatus.REJECTED
                            MeshInventoryReviewStatus.REQUIRES_UPDATE -> MeshInventoryStatus.REQUIRES_UPDATE
                        },
                    ),
                )
            }
            store.refresh()
        }
    }

    private fun buildDemoItemTitle(index: Int): String = when (index % 6) {
        0 -> "Ноутбук Lenovo ThinkPad T14"
        1 -> "Монитор Dell UltraSharp 27"
        2 -> "Док-станция USB-C Pro"
        3 -> "Мини-ПК Intel NUC"
        4 -> "Принтер HP LaserJet"
        else -> "Сканер Zebra DS2208"
    }
}

private fun buildExportDocument(
    state: InventoryOverviewState,
    task: MeshInventoryExportTask,
): InventoryExportDocument? {
    val session = state.sessions.firstOrNull { it.sessionId == task.sessionId } ?: return null
    val items = state.items.filter { it.inventoryItemId in session.itemIds }
    val ownerMap = state.owners.associateBy { it.ownerId }
    val departmentMap = state.departments.associateBy { it.departmentId }
    val locationMap = state.locations.associateBy { it.locationId }
    val ownerLabels = session.ownerIds.mapNotNull { ownerMap[it]?.name }
    val departmentLabels = session.departmentIds.mapNotNull { departmentMap[it]?.name }
    val locationLabels = session.locationIds.mapNotNull { locationPath(it, state.locations) }
    return InventoryExportDocument(
        title = "Отчёт по инвентаризации: ${session.title}",
        subtitle = session.description,
        summary = buildList {
            add("Дата начала" to session.periodStart.toString())
            session.periodEnd?.let { add("Дата окончания" to it.toString()) }
            add("Статус" to session.workflowStatus.asUiText())
            add("Объектов" to items.size.toString())
            if (ownerLabels.isNotEmpty()) add("МОЛы" to ownerLabels.joinToString())
            if (departmentLabels.isNotEmpty()) add("Подразделения" to departmentLabels.joinToString())
            if (locationLabels.isNotEmpty()) add("Локации" to locationLabels.joinToString())
        },
        columns = listOf("Инв. номер", "Название", "Подразделение", "Локация", "МОЛы", "Статус", "Состояние"),
        rows = items.map { item ->
            listOf(
                item.inventoryNumber,
                item.title,
                departmentMap[item.departmentId]?.name ?: item.responsibleDepartment.orEmpty(),
                locationMap[item.locationId]?.name ?: locationPath(item.locationId, state.locations).orEmpty(),
                ownerNames(item, state.owners).joinToString(),
                item.currentStatus.asUiText(),
                item.condition.asUiText(),
            )
        },
    )
}
