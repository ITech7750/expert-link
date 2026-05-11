package org.expert.link.app.shared.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.expert.link.mesh.contract.api.MeshAddInventoryAttachmentCommand
import org.expert.link.mesh.contract.api.MeshAddInventoryCommentCommand
import org.expert.link.mesh.contract.api.MeshAddItemsToSessionCommand
import org.expert.link.mesh.contract.api.MeshAddInventorySessionMemberCommand
import org.expert.link.mesh.contract.api.MeshCloseInventorySessionCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryCodeBindingCommand
import org.expert.link.mesh.contract.api.MeshDeactivateInventoryCodeCommand
import org.expert.link.mesh.contract.api.MeshGenerateInventoryCodeCommand
import org.expert.link.mesh.contract.api.MeshGenerateInventoryLabelRequest
import org.expert.link.mesh.contract.api.MeshLinkInventoryDiscussionCommand
import org.expert.link.mesh.contract.api.MeshPrintInventoryLabelRequest
import org.expert.link.mesh.contract.api.MeshReportInventoryIncidentCommand
import org.expert.link.mesh.contract.api.MeshResolveScannedCodeRequest
import org.expert.link.mesh.contract.api.MeshResolveScannedCodeResponse
import org.expert.link.mesh.contract.api.MeshRecordInventoryScanCommand
import org.expert.link.mesh.contract.api.MeshRegenerateInventoryCodeCommand
import org.expert.link.mesh.contract.api.MeshRequestInventoryExportCommand
import org.expert.link.mesh.contract.api.MeshSubmitInventoryReviewCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryIncidentStatusCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryItemCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryStatusCommand
import org.expert.link.mesh.contract.model.MeshFileDescriptor
import org.expert.link.mesh.contract.model.MeshInventoryAlertEvent
import org.expert.link.mesh.contract.model.MeshInventoryAcceptanceStatus
import org.expert.link.mesh.contract.model.MeshInventoryAttachment
import org.expert.link.mesh.contract.model.MeshInventoryAttachmentSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryAttachmentType
import org.expert.link.mesh.contract.model.MeshInventoryAttributeDefinition
import org.expert.link.mesh.contract.model.MeshInventoryAttributeValue
import org.expert.link.mesh.contract.model.MeshInventoryCategory
import org.expert.link.mesh.contract.model.MeshInventoryCategoryTemplate
import org.expert.link.mesh.contract.model.MeshInventoryChangeLog
import org.expert.link.mesh.contract.model.MeshInventoryCode
import org.expert.link.mesh.contract.model.MeshInventoryCodeBinding
import org.expert.link.mesh.contract.model.MeshInventoryCodeType
import org.expert.link.mesh.contract.model.MeshInventoryComment
import org.expert.link.mesh.contract.model.MeshInventoryCommentSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryCondition
import org.expert.link.mesh.contract.model.MeshInventoryCostCenter
import org.expert.link.mesh.contract.model.MeshInventoryDepartment
import org.expert.link.mesh.contract.model.MeshInventoryEntityType
import org.expert.link.mesh.contract.model.MeshInventoryEvent
import org.expert.link.mesh.contract.model.MeshInventoryExportFormat
import org.expert.link.mesh.contract.model.MeshInventoryExportTask
import org.expert.link.mesh.contract.model.MeshInventoryFundingSource
import org.expert.link.mesh.contract.model.MeshInventoryIncident
import org.expert.link.mesh.contract.model.MeshInventoryIncidentSeverity
import org.expert.link.mesh.contract.model.MeshInventoryIncidentStatus
import org.expert.link.mesh.contract.model.MeshInventoryIncidentType
import org.expert.link.mesh.contract.model.MeshInventoryItem
import org.expert.link.mesh.contract.model.MeshInventoryItemSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryLabel
import org.expert.link.mesh.contract.model.MeshInventoryLabelFieldKey
import org.expert.link.mesh.contract.model.MeshInventoryLabelSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryLabelTemplate
import org.expert.link.mesh.contract.model.MeshInventoryLegalHolder
import org.expert.link.mesh.contract.model.MeshInventoryLocation
import org.expert.link.mesh.contract.model.MeshInventoryOwner
import org.expert.link.mesh.contract.model.MeshInventoryPermission
import org.expert.link.mesh.contract.model.MeshInventoryPrintTask
import org.expert.link.mesh.contract.model.MeshInventoryPrintTaskSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryPresenceStatus
import org.expert.link.mesh.contract.model.MeshInventoryReminder
import org.expert.link.mesh.contract.model.MeshInventoryReview
import org.expert.link.mesh.contract.model.MeshInventoryConfirmationStatus
import org.expert.link.mesh.contract.model.MeshInventoryReviewStatus
import org.expert.link.mesh.contract.model.MeshInventoryScanEvent
import org.expert.link.mesh.contract.model.MeshInventoryScanEventSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryScanResultStatus
import org.expert.link.mesh.contract.model.MeshInventorySession
import org.expert.link.mesh.contract.model.MeshInventorySessionMember
import org.expert.link.mesh.contract.model.MeshInventorySessionRole
import org.expert.link.mesh.contract.model.MeshInventoryStatus
import org.expert.link.mesh.contract.model.MeshInventorySubcategory
import org.expert.link.mesh.contract.model.MeshInventorySupplier
import org.expert.link.mesh.contract.model.MeshInventoryTag
import org.expert.link.mesh.contract.model.MeshInventoryWorkflowStatus
import org.expert.link.mesh.contract.model.MeshOrganization
import org.expert.link.mesh.contract.model.MeshOrganizationMember
import org.expert.link.mesh.contract.model.MeshRole

enum class InventoryItemBusyAction {
    REFRESH,
    UPDATE_ITEM,
    UPDATE_STATUS,
    ADD_COMMENT,
    ADD_ATTACHMENT,
    SUBMIT_REVIEW,
    REPORT_INCIDENT,
    UPDATE_INCIDENT,
    CREATE_CODE_BINDING,
    GENERATE_CODE,
    REGENERATE_CODE,
    DEACTIVATE_CODE,
    GENERATE_LABEL_PREVIEW,
    GENERATE_LABEL_PDF,
    PRINT_LABEL,
    LINK_DISCUSSION,
}

data class InventoryItemState(
    val runtimeStatus: NodeRuntimeStatus = NodeRuntimeStatus.STOPPED,
    val localPeerId: String? = null,
    val item: MeshInventoryItem? = null,
    val organization: MeshOrganization? = null,
    val categories: List<MeshInventoryCategory> = emptyList(),
    val subcategories: List<MeshInventorySubcategory> = emptyList(),
    val tags: List<MeshInventoryTag> = emptyList(),
    val attributeDefinitions: List<MeshInventoryAttributeDefinition> = emptyList(),
    val templates: List<MeshInventoryCategoryTemplate> = emptyList(),
    val locations: List<MeshInventoryLocation> = emptyList(),
    val owners: List<MeshInventoryOwner> = emptyList(),
    val departments: List<MeshInventoryDepartment> = emptyList(),
    val costCenters: List<MeshInventoryCostCenter> = emptyList(),
    val legalHolders: List<MeshInventoryLegalHolder> = emptyList(),
    val suppliers: List<MeshInventorySupplier> = emptyList(),
    val fundingSources: List<MeshInventoryFundingSource> = emptyList(),
    val sessions: List<MeshInventorySession> = emptyList(),
    val reviews: List<MeshInventoryReview> = emptyList(),
    val comments: List<MeshInventoryComment> = emptyList(),
    val attachments: List<MeshInventoryAttachment> = emptyList(),
    val incidents: List<MeshInventoryIncident> = emptyList(),
    val alerts: List<MeshInventoryAlertEvent> = emptyList(),
    val reminders: List<MeshInventoryReminder> = emptyList(),
    val codes: List<MeshInventoryCode> = emptyList(),
    val labelTemplates: List<MeshInventoryLabelTemplate> = emptyList(),
    val labels: List<MeshInventoryLabel> = emptyList(),
    val labelPreview: MeshInventoryLabel? = null,
    val lastLabelPdf: MeshInventoryPrintTask? = null,
    val printTasks: List<MeshInventoryPrintTask> = emptyList(),
    val scanEvents: List<MeshInventoryScanEvent> = emptyList(),
    val codeBindings: List<MeshInventoryCodeBinding> = emptyList(),
    val history: List<MeshInventoryEvent> = emptyList(),
    val changeLogs: List<MeshInventoryChangeLog> = emptyList(),
    val permissions: Set<MeshInventoryPermission> = emptySet(),
    val busyAction: InventoryItemBusyAction? = null,
    val error: String? = null,
    val message: String? = null,
)

class InventoryItemStore(
    private val session: NodeSessionController,
) {
    private val _state = MutableStateFlow(InventoryItemState())
    val state: StateFlow<InventoryItemState> = _state.asStateFlow()

    fun updateRuntime(status: NodeRuntimeStatus, localPeerId: String?) {
        _state.update { it.copy(runtimeStatus = status, localPeerId = localPeerId) }
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

    suspend fun refresh(itemId: String) {
        if (state.value.runtimeStatus != NodeRuntimeStatus.RUNNING) return
        _state.update { it.copy(busyAction = InventoryItemBusyAction.REFRESH, error = null) }
        val previous = state.value
        val result = session.withNode { node ->
            val item = node.inventoryItem(itemId) ?: error("Объект не найден")
            val organization = node.organizations().firstOrNull { it.organizationId == item.organizationId }
            val categories = node.inventoryCategories(item.organizationId)
            val subcategories = node.inventorySubcategories(item.organizationId)
            val tags = node.inventoryTags(item.organizationId)
            val attributeDefinitions = node.inventoryAttributeDefinitions(item.organizationId)
            val templates = node.inventoryCategoryTemplates(item.organizationId)
            val locations = node.inventoryLocations(item.organizationId)
            val owners = node.inventoryOwners(item.organizationId)
            val departments = node.inventoryDepartments(item.organizationId)
            val costCenters = node.inventoryCostCenters(item.organizationId)
            val legalHolders = node.inventoryLegalHolders(item.organizationId)
            val suppliers = node.inventorySuppliers(item.organizationId)
            val fundingSources = node.inventoryFundingSources(item.organizationId)
            val sessions = node.inventorySessions(item.organizationId).filter { it.sessionId in item.sessionIds }
            val codes = node.inventoryCodes(item.inventoryItemId)
            val labelTemplates = node.inventoryLabelTemplates(item.organizationId)
            val printTasks = node.inventoryPrintTasks(item.organizationId)
                .filter { item.inventoryItemId in it.itemIds }
            val mergedPrintTasks = printTasks.map { task ->
                val previousTask = previous.printTasks.firstOrNull { it.printTaskId == task.printTaskId }
                if (task.localPath.isNullOrBlank() && !previousTask?.localPath.isNullOrBlank()) {
                    task.copy(localPath = previousTask?.localPath)
                } else {
                    task
                }
            }
            val latestPrintTask = mergedPrintTasks.maxByOrNull { it.updatedAt }?.let { task ->
                val previousTask = previous.lastLabelPdf?.takeIf { it.printTaskId == task.printTaskId }
                if (task.localPath.isNullOrBlank() && !previousTask?.localPath.isNullOrBlank()) {
                    task.copy(localPath = previousTask?.localPath)
                } else {
                    task
                }
            } ?: previous.lastLabelPdf
            val reviews = node.inventoryReviews(item.inventoryItemId)
            val incidents = node.inventoryIncidents(item.organizationId, item.inventoryItemId, null)
            val alerts = node.inventoryAlerts(item.organizationId)
                .filter { it.inventoryItemId == item.inventoryItemId }
            val reminders = node.inventoryReminders(item.organizationId, item.inventoryItemId)
            val codeBindings = node.inventoryCodeBindings(item.inventoryItemId)
            val changeLogs = node.inventoryChangeLogs(item.organizationId, item.inventoryItemId)
            val events = node.inventoryEvents(item.organizationId, null)
            val comments = events.mapNotNull { event ->
                (event.payload as? MeshInventoryCommentSnapshot)?.comment
            }.filter { it.inventoryItemId == item.inventoryItemId }
            val attachments = events.mapNotNull { event ->
                (event.payload as? MeshInventoryAttachmentSnapshot)?.attachment
            }.filter { it.inventoryItemId == item.inventoryItemId }
            val labels = events.mapNotNull { event ->
                (event.payload as? MeshInventoryLabelSnapshot)?.label
            }.filter { it.inventoryItemId == item.inventoryItemId }
            val scanEvents = events.mapNotNull { event ->
                (event.payload as? MeshInventoryScanEventSnapshot)?.event
            }.filter { it.inventoryItemId == item.inventoryItemId }
            val history = events.filter { event ->
                event.entityId == item.inventoryItemId && event.entityType == org.expert.link.mesh.contract.model.MeshInventoryEntityType.ITEM
            }.sortedByDescending { it.occurredAt }
            val members = node.organizationMembers(item.organizationId)
            val roles = node.roles(item.organizationId)
            previous.copy(
                item = item,
                organization = organization,
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
                sessions = sessions,
                codes = codes.sortedByDescending { it.createdAt },
                labelTemplates = labelTemplates.sortedBy { it.name },
                labels = labels.sortedByDescending { it.createdAt },
                lastLabelPdf = latestPrintTask,
                printTasks = mergedPrintTasks.sortedByDescending { it.updatedAt },
                scanEvents = scanEvents.sortedByDescending { it.scannedAt },
                reviews = reviews.sortedByDescending { it.updatedAt },
                comments = comments.sortedByDescending { it.createdAt },
                attachments = attachments.sortedByDescending { it.createdAt },
                incidents = incidents.sortedByDescending { it.updatedAt },
                alerts = alerts.sortedByDescending { it.createdAt },
                reminders = reminders.sortedByDescending { it.updatedAt },
                codeBindings = codeBindings,
                history = history,
                changeLogs = changeLogs.sortedByDescending { it.changedAt },
                permissions = resolveInventoryPermissions(
                    localPeerId = previous.localPeerId,
                    members = members,
                    roles = roles,
                ),
                busyAction = null,
                error = null,
            )
        }
        _state.update {
            result.getOrElse { error ->
                it.copy(busyAction = null, error = error.message ?: "Не удалось загрузить объект")
            }
        }
    }

    suspend fun updateItem(
        itemId: String,
        expectedRevision: Long?,
        inventoryNumber: String?,
        localNumber: String?,
        title: String?,
        description: String?,
        brand: String?,
        model: String?,
        serialNumber: String?,
        manufacturer: String?,
        condition: MeshInventoryCondition?,
        categoryId: String?,
        subcategoryId: String?,
        locationId: String?,
        responsiblePerson: String?,
        responsibleDepartment: String?,
        ownerId: String?,
        departmentId: String?,
        responsibleOwnerIds: Set<String>?,
        costCenterId: String?,
        legalHolderId: String?,
        supplierId: String?,
        fundingSourceId: String?,
        tagIds: Set<String>?,
        attributes: List<MeshInventoryAttributeValue>?,
    ) {
        _state.update { it.copy(busyAction = InventoryItemBusyAction.UPDATE_ITEM, error = null) }
        val result = session.withNode { node ->
            node.updateInventoryItem(
                MeshUpdateInventoryItemCommand(
                    inventoryItemId = itemId,
                    expectedRevision = expectedRevision,
                    inventoryNumber = inventoryNumber?.trim().orEmpty().ifBlank { null },
                    localNumber = localNumber?.trim().orEmpty().ifBlank { null },
                    title = title?.trim().orEmpty().ifBlank { null },
                    description = description?.trim().orEmpty().ifBlank { null },
                    brand = brand?.trim().orEmpty().ifBlank { null },
                    model = model?.trim().orEmpty().ifBlank { null },
                    serialNumber = serialNumber?.trim().orEmpty().ifBlank { null },
                    manufacturer = manufacturer?.trim().orEmpty().ifBlank { null },
                    condition = condition,
                    categoryId = categoryId,
                    subcategoryId = subcategoryId,
                    locationId = locationId,
                    responsiblePerson = responsiblePerson?.trim().orEmpty().ifBlank { null },
                    responsibleDepartment = responsibleDepartment?.trim().orEmpty().ifBlank { null },
                    ownerId = ownerId,
                    departmentId = departmentId,
                    responsibleOwnerIds = responsibleOwnerIds,
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
                onSuccess = { current.copy(busyAction = null, message = "Карточка обновлена") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось обновить карточку")
                },
            )
        }
        refresh(itemId)
    }

    suspend fun updateStatus(itemId: String, expectedRevision: Long?, status: MeshInventoryStatus, note: String?) {
        _state.update { it.copy(busyAction = InventoryItemBusyAction.UPDATE_STATUS, error = null) }
        val result = session.withNode { node ->
            node.updateInventoryStatus(
                MeshUpdateInventoryStatusCommand(
                    inventoryItemId = itemId,
                    expectedRevision = expectedRevision,
                    status = status,
                    note = note?.trim().orEmpty().ifBlank { null },
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Статус обновлён") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось обновить статус")
                },
            )
        }
        refresh(itemId)
    }

    suspend fun addComment(itemId: String, sessionId: String?, body: String) {
        if (body.isBlank()) {
            _state.update { it.copy(error = "Введите комментарий") }
            return
        }
        _state.update { it.copy(busyAction = InventoryItemBusyAction.ADD_COMMENT, error = null) }
        val result = session.withNode { node ->
            node.addInventoryComment(
                MeshAddInventoryCommentCommand(
                    inventoryItemId = itemId,
                    sessionId = sessionId,
                    body = body.trim(),
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Комментарий добавлен") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось добавить комментарий")
                },
            )
        }
        refresh(itemId)
    }

    suspend fun addAttachment(
        itemId: String,
        sessionId: String?,
        descriptor: MeshFileDescriptor,
        attachmentType: MeshInventoryAttachmentType,
        note: String?,
    ) {
        _state.update { it.copy(busyAction = InventoryItemBusyAction.ADD_ATTACHMENT, error = null) }
        val result = session.withNode { node ->
            node.addInventoryAttachment(
                MeshAddInventoryAttachmentCommand(
                    inventoryItemId = itemId,
                    sessionId = sessionId,
                    descriptor = descriptor,
                    attachmentType = attachmentType,
                    note = note?.trim().orEmpty().ifBlank { null },
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Вложение добавлено") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось добавить вложение")
                },
            )
        }
        refresh(itemId)
    }

    suspend fun createCodeBinding(
        organizationId: String,
        itemId: String,
        codeType: MeshInventoryCodeType,
        codeValue: String,
    ) {
        if (codeValue.isBlank()) {
            _state.update { it.copy(error = "Введите значение кода") }
            return
        }
        _state.update { it.copy(busyAction = InventoryItemBusyAction.CREATE_CODE_BINDING, error = null) }
        val result = session.withNode { node ->
            node.createInventoryCodeBinding(
                MeshCreateInventoryCodeBindingCommand(
                    organizationId = organizationId,
                    inventoryItemId = itemId,
                    codeType = codeType,
                    codeValue = codeValue.trim(),
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Код привязан") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось связать код")
                },
            )
        }
        refresh(itemId)
    }

    suspend fun generateCode(
        organizationId: String,
        itemId: String,
        codeType: MeshInventoryCodeType,
        regenerate: Boolean = false,
    ) {
        _state.update {
            it.copy(
                busyAction = if (regenerate) InventoryItemBusyAction.REGENERATE_CODE else InventoryItemBusyAction.GENERATE_CODE,
                error = null,
            )
        }
        val result = session.withNode { node ->
            if (regenerate) {
                node.regenerateInventoryCode(
                    MeshRegenerateInventoryCodeCommand(
                        organizationId = organizationId,
                        inventoryItemId = itemId,
                        codeType = codeType,
                    ),
                )
            } else {
                node.generateInventoryCode(
                    MeshGenerateInventoryCodeCommand(
                        organizationId = organizationId,
                        inventoryItemId = itemId,
                        codeType = codeType,
                    ),
                )
            }
        }
        _state.update { current ->
            result.fold(
                onSuccess = {
                    current.copy(
                        busyAction = null,
                        message = if (regenerate) "Код перевыпущен" else "Код сгенерирован",
                    )
                },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось подготовить код")
                },
            )
        }
        refresh(itemId)
    }

    suspend fun deactivateCode(itemId: String, codeId: String) {
        _state.update { it.copy(busyAction = InventoryItemBusyAction.DEACTIVATE_CODE, error = null) }
        val result = session.withNode { node ->
            node.deactivateInventoryCode(MeshDeactivateInventoryCodeCommand(codeId))
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Код деактивирован") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось деактивировать код")
                },
            )
        }
        refresh(itemId)
    }

    suspend fun generateLabelPreview(
        organizationId: String,
        itemId: String,
        templateId: String?,
        fields: List<MeshInventoryLabelFieldKey>?,
        includeBarcode: Boolean,
        includeQr: Boolean,
    ) {
        _state.update { it.copy(busyAction = InventoryItemBusyAction.GENERATE_LABEL_PREVIEW, error = null) }
        val result = session.withNode { node ->
            node.generateInventoryLabelPreview(
                MeshGenerateInventoryLabelRequest(
                    organizationId = organizationId,
                    inventoryItemId = itemId,
                    templateId = templateId,
                    fields = fields,
                    includeBarcode = includeBarcode,
                    includeQr = includeQr,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { response ->
                    current.copy(
                        busyAction = null,
                        labelPreview = response.label,
                        message = "Превью этикетки обновлено",
                    )
                },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось собрать превью")
                },
            )
        }
    }

    suspend fun generateLabelPdf(
        organizationId: String,
        itemId: String,
        templateId: String?,
        fields: List<MeshInventoryLabelFieldKey>?,
        includeBarcode: Boolean,
        includeQr: Boolean,
    ) {
        _state.update { it.copy(busyAction = InventoryItemBusyAction.GENERATE_LABEL_PDF, error = null) }
        val result = session.withNode { node ->
            node.generateInventoryLabelPdf(
                MeshGenerateInventoryLabelRequest(
                    organizationId = organizationId,
                    inventoryItemId = itemId,
                    templateId = templateId,
                    fields = fields,
                    includeBarcode = includeBarcode,
                    includeQr = includeQr,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { response ->
                    val pdfTask = response.pdfDescriptor?.let { descriptor ->
                        MeshInventoryPrintTask(
                            printTaskId = "preview-${itemId}-${descriptor.fileId}",
                            organizationId = organizationId,
                            templateId = response.template.templateId,
                            labelIds = listOf(response.label.labelId),
                            itemIds = listOf(itemId),
                            status = org.expert.link.mesh.contract.model.MeshInventoryPrintStatus.GENERATED,
                            createdByPeerId = current.localPeerId.orEmpty(),
                            createdAt = response.label.createdAt,
                            updatedAt = response.label.createdAt,
                            resultDescriptor = descriptor,
                            localPath = response.localPath,
                        )
                    }
                    current.copy(
                        busyAction = null,
                        labelPreview = response.label,
                        lastLabelPdf = pdfTask ?: current.lastLabelPdf,
                        message = "PDF подготовлен",
                    )
                },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось подготовить PDF")
                },
            )
        }
        refresh(itemId)
    }

    suspend fun printLabel(
        organizationId: String,
        itemId: String,
        templateId: String?,
        fields: List<MeshInventoryLabelFieldKey>?,
        includeBarcode: Boolean,
        includeQr: Boolean,
    ) {
        _state.update { it.copy(busyAction = InventoryItemBusyAction.PRINT_LABEL, error = null) }
        val result = session.withNode { node ->
            node.printInventoryLabel(
                MeshPrintInventoryLabelRequest(
                    organizationId = organizationId,
                    inventoryItemId = itemId,
                    templateId = templateId,
                    fields = fields,
                    includeBarcode = includeBarcode,
                    includeQr = includeQr,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { task ->
                    current.copy(
                        busyAction = null,
                        lastLabelPdf = task,
                        message = "Печать подготовлена",
                    )
                },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось подготовить печать")
                },
            )
        }
        refresh(itemId)
    }

    suspend fun reportIncident(
        organizationId: String,
        itemId: String,
        sessionId: String?,
        locationId: String?,
        type: MeshInventoryIncidentType,
        severity: MeshInventoryIncidentSeverity,
        title: String,
        description: String?,
        assigneePeerIds: Set<String> = emptySet(),
    ) {
        if (title.isBlank()) {
            _state.update { it.copy(error = "Введите описание инцидента") }
            return
        }
        _state.update { it.copy(busyAction = InventoryItemBusyAction.REPORT_INCIDENT, error = null) }
        val result = session.withNode { node ->
            node.reportInventoryIncident(
                MeshReportInventoryIncidentCommand(
                    organizationId = organizationId,
                    inventoryItemId = itemId,
                    sessionId = sessionId,
                    locationId = locationId,
                    type = type,
                    severity = severity,
                    title = title.trim(),
                    description = description?.trim().orEmpty().ifBlank { null },
                    assigneePeerIds = assigneePeerIds,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Инцидент зарегистрирован") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось добавить инцидент")
                },
            )
        }
        refresh(itemId)
    }

    suspend fun updateIncidentStatus(
        itemId: String,
        incidentId: String,
        status: MeshInventoryIncidentStatus,
        comment: String?,
    ) {
        _state.update { it.copy(busyAction = InventoryItemBusyAction.UPDATE_INCIDENT, error = null) }
        val result = session.withNode { node ->
            node.updateInventoryIncidentStatus(
                MeshUpdateInventoryIncidentStatusCommand(
                    incidentId = incidentId,
                    status = status,
                    reviewComment = comment?.trim().orEmpty().ifBlank { null },
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Инцидент обновлён") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось обновить инцидент")
                },
            )
        }
        refresh(itemId)
    }

    suspend fun submitReview(
        itemId: String,
        sessionId: String?,
        status: MeshInventoryReviewStatus,
        presenceStatus: MeshInventoryPresenceStatus,
        acceptanceStatus: MeshInventoryAcceptanceStatus,
        confirmationStatus: MeshInventoryConfirmationStatus,
        requiresPhoto: Boolean,
        comment: String?,
    ) {
        _state.update { it.copy(busyAction = InventoryItemBusyAction.SUBMIT_REVIEW, error = null) }
        val result = session.withNode { node ->
            node.submitInventoryReview(
                MeshSubmitInventoryReviewCommand(
                    inventoryItemId = itemId,
                    sessionId = sessionId,
                    status = status,
                    presenceStatus = presenceStatus,
                    acceptanceStatus = acceptanceStatus,
                    confirmationStatus = confirmationStatus,
                    requiresPhoto = requiresPhoto,
                    comment = comment?.trim().orEmpty().ifBlank { null },
                ),
            )
            node.updateInventoryStatus(
                MeshUpdateInventoryStatusCommand(
                    inventoryItemId = itemId,
                    status = reviewStatusToInventoryStatus(status),
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Решение комиссии зафиксировано") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось сохранить решение")
                },
            )
        }
        refresh(itemId)
    }

    suspend fun linkDiscussion(itemId: String, chatId: String?, threadRootMessageId: String?) {
        _state.update { it.copy(busyAction = InventoryItemBusyAction.LINK_DISCUSSION, error = null) }
        val result = session.withNode { node ->
            node.linkInventoryDiscussion(
                MeshLinkInventoryDiscussionCommand(
                    inventoryItemId = itemId,
                    chatId = chatId,
                    threadRootMessageId = threadRootMessageId,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Обсуждение обновлено") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось связать обсуждение")
                },
            )
        }
        refresh(itemId)
    }
}

enum class InventorySessionBusyAction {
    REFRESH,
    ADD_MEMBER,
    ADD_ITEM,
    SAVE_REVIEWS,
    ADD_ATTACHMENT,
    REPORT_CORRECTION,
    CLOSE_SESSION,
    REQUEST_EXPORT,
    PRINT_LABELS,
}

data class InventorySessionReviewDraft(
    val itemId: String,
    val status: MeshInventoryReviewStatus,
    val presenceStatus: MeshInventoryPresenceStatus,
    val acceptanceStatus: MeshInventoryAcceptanceStatus,
    val confirmationStatus: MeshInventoryConfirmationStatus,
    val requiresPhoto: Boolean,
    val comment: String? = null,
)

data class InventorySessionState(
    val runtimeStatus: NodeRuntimeStatus = NodeRuntimeStatus.STOPPED,
    val localPeerId: String? = null,
    val session: MeshInventorySession? = null,
    val organization: MeshOrganization? = null,
    val members: List<MeshInventorySessionMember> = emptyList(),
    val organizationMembers: List<MeshOrganizationMember> = emptyList(),
    val locations: List<MeshInventoryLocation> = emptyList(),
    val owners: List<MeshInventoryOwner> = emptyList(),
    val departments: List<MeshInventoryDepartment> = emptyList(),
    val allItems: List<MeshInventoryItem> = emptyList(),
    val items: List<MeshInventoryItem> = emptyList(),
    val reviews: List<MeshInventoryReview> = emptyList(),
    val attachments: List<MeshInventoryAttachment> = emptyList(),
    val incidents: List<MeshInventoryIncident> = emptyList(),
    val labelTemplates: List<MeshInventoryLabelTemplate> = emptyList(),
    val printTasks: List<MeshInventoryPrintTask> = emptyList(),
    val exports: List<MeshInventoryExportTask> = emptyList(),
    val permissions: Set<MeshInventoryPermission> = emptySet(),
    val busyAction: InventorySessionBusyAction? = null,
    val error: String? = null,
    val message: String? = null,
)

class InventorySessionStore(
    private val session: NodeSessionController,
) {
    private val _state = MutableStateFlow(InventorySessionState())
    val state: StateFlow<InventorySessionState> = _state.asStateFlow()

    fun updateRuntime(status: NodeRuntimeStatus, localPeerId: String?) {
        _state.update { it.copy(runtimeStatus = status, localPeerId = localPeerId) }
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

    suspend fun refresh(sessionId: String) {
        if (state.value.runtimeStatus != NodeRuntimeStatus.RUNNING) return
        _state.update { it.copy(busyAction = InventorySessionBusyAction.REFRESH, error = null) }
        val previous = state.value
        val result = session.withNode { node ->
            val sessionEntity = node.inventorySession(sessionId) ?: error("Инвентаризация не найдена")
            val organization = node.organizations().firstOrNull { it.organizationId == sessionEntity.organizationId }
            val organizationMembers = node.organizationMembers(sessionEntity.organizationId)
            val roles = node.roles(sessionEntity.organizationId)
            val locations = node.inventoryLocations(sessionEntity.organizationId)
            val owners = node.inventoryOwners(sessionEntity.organizationId)
            val departments = node.inventoryDepartments(sessionEntity.organizationId)
            val members = node.inventorySessionMembers(sessionId)
            val allItems = node.inventoryItems(sessionEntity.organizationId)
            val items = allItems.filter { it.inventoryItemId in sessionEntity.itemIds }
            val reviews = items.flatMap { item ->
                node.inventoryReviews(item.inventoryItemId, sessionId)
            }
            val events = node.inventoryEvents(sessionEntity.organizationId, null)
            val attachments = events.mapNotNull { event ->
                (event.payload as? MeshInventoryAttachmentSnapshot)?.attachment
            }.filter { attachment ->
                attachment.sessionId == sessionId || attachment.inventoryItemId in sessionEntity.itemIds
            }
            val incidents = node.inventoryIncidents(sessionEntity.organizationId, null, sessionId)
            val labelTemplates = node.inventoryLabelTemplates(sessionEntity.organizationId)
            val printTasks = node.inventoryPrintTasks(sessionEntity.organizationId)
                .filter { task -> task.itemIds.any { it in sessionEntity.itemIds } }
            val exports = node.inventoryExports(sessionEntity.organizationId)
                .filter { it.sessionId == sessionId }
            previous.copy(
                session = sessionEntity,
                organization = organization,
                organizationMembers = organizationMembers,
                members = members,
                locations = locations.sortedBy { it.path ?: it.name },
                owners = owners.sortedBy { it.name },
                departments = departments.sortedBy { it.name },
                allItems = allItems.sortedBy { it.inventoryNumber },
                items = items.sortedBy { it.inventoryNumber },
                reviews = reviews.sortedByDescending { it.updatedAt },
                attachments = attachments.sortedByDescending { it.updatedAt },
                incidents = incidents.sortedByDescending { it.updatedAt },
                labelTemplates = labelTemplates.sortedBy { it.name },
                printTasks = printTasks.sortedByDescending { it.updatedAt },
                exports = exports.sortedByDescending { it.updatedAt },
                permissions = resolveInventoryPermissions(
                    localPeerId = previous.localPeerId,
                    members = organizationMembers,
                    roles = roles,
                ),
                busyAction = null,
                error = null,
            )
        }
        _state.update {
            result.getOrElse { error ->
                it.copy(busyAction = null, error = error.message ?: "Не удалось загрузить сессию")
            }
        }
    }

    suspend fun saveReviews(sessionId: String, drafts: List<InventorySessionReviewDraft>) {
        if (drafts.isEmpty()) {
            _state.update { it.copy(error = "Нет изменений для сохранения") }
            return
        }
        _state.update { it.copy(busyAction = InventorySessionBusyAction.SAVE_REVIEWS, error = null) }
        val result = session.withNode { node ->
            drafts.forEach { draft ->
                node.submitInventoryReview(
                    MeshSubmitInventoryReviewCommand(
                        inventoryItemId = draft.itemId,
                        sessionId = sessionId,
                        status = draft.status,
                        presenceStatus = draft.presenceStatus,
                        acceptanceStatus = draft.acceptanceStatus,
                        confirmationStatus = draft.confirmationStatus,
                        requiresPhoto = draft.requiresPhoto,
                        comment = draft.comment?.trim().orEmpty().ifBlank { null },
                    ),
                )
            }
            node.inventorySession(sessionId)
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Результаты инвентаризации сохранены") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось сохранить результаты")
                },
            )
        }
        refresh(sessionId)
    }

    suspend fun addMember(sessionId: String, peerId: String, role: MeshInventorySessionRole) {
        if (peerId.isBlank()) {
            _state.update { it.copy(error = "Введите ID участника") }
            return
        }
        _state.update { it.copy(busyAction = InventorySessionBusyAction.ADD_MEMBER, error = null) }
        val result = session.withNode { node ->
            node.addInventorySessionMember(
                MeshAddInventorySessionMemberCommand(
                    sessionId = sessionId,
                    peerId = peerId.trim(),
                    role = role,
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
        refresh(sessionId)
    }

    suspend fun addAttachment(
        sessionId: String,
        itemId: String,
        descriptor: MeshFileDescriptor,
        note: String?,
    ) {
        _state.update { it.copy(busyAction = InventorySessionBusyAction.ADD_ATTACHMENT, error = null) }
        val result = session.withNode { node ->
            node.addInventoryAttachment(
                MeshAddInventoryAttachmentCommand(
                    inventoryItemId = itemId,
                    sessionId = sessionId,
                    descriptor = descriptor,
                    attachmentType = MeshInventoryAttachmentType.PHOTO,
                    note = note?.trim().orEmpty().ifBlank { null },
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Фотография прикреплена") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось прикрепить фотографию")
                },
            )
        }
        refresh(sessionId)
    }

    suspend fun reportCorrectionRequest(
        sessionId: String,
        organizationId: String,
        title: String,
        description: String?,
        assigneePeerIds: Set<String>,
    ) {
        if (title.isBlank()) {
            _state.update { it.copy(error = "Опишите причину заявки на исправление") }
            return
        }
        _state.update { it.copy(busyAction = InventorySessionBusyAction.REPORT_CORRECTION, error = null) }
        val result = session.withNode { node ->
            node.reportInventoryIncident(
                MeshReportInventoryIncidentCommand(
                    organizationId = organizationId,
                    sessionId = sessionId,
                    type = MeshInventoryIncidentType.CORRECTION_REQUEST,
                    severity = MeshInventoryIncidentSeverity.MEDIUM,
                    title = title.trim(),
                    description = description?.trim().orEmpty().ifBlank { null },
                    assigneePeerIds = assigneePeerIds,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Заявка на исправление отправлена в комиссию") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось отправить заявку на исправление")
                },
            )
        }
        refresh(sessionId)
    }

    suspend fun addItems(sessionId: String, itemIds: Set<String>) {
        if (itemIds.isEmpty()) {
            _state.update { it.copy(error = "Выберите объекты") }
            return
        }
        _state.update { it.copy(busyAction = InventorySessionBusyAction.ADD_ITEM, error = null) }
        val result = session.withNode { node ->
            node.addItemsToSession(
                MeshAddItemsToSessionCommand(
                    sessionId = sessionId,
                    itemIds = itemIds,
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Объекты добавлены") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось добавить объекты")
                },
            )
        }
        refresh(sessionId)
    }

    suspend fun closeSession(sessionId: String) {
        _state.update { it.copy(busyAction = InventorySessionBusyAction.CLOSE_SESSION, error = null) }
        val result = session.withNode { node ->
            node.closeInventorySession(
                MeshCloseInventorySessionCommand(sessionId = sessionId),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { current.copy(busyAction = null, message = "Инвентаризация завершена") },
                onFailure = { error ->
                    current.copy(busyAction = null, error = error.message ?: "Не удалось завершить инвентаризацию")
                },
            )
        }
        refresh(sessionId)
    }

    suspend fun requestExport(sessionId: String, organizationId: String, format: MeshInventoryExportFormat) {
        _state.update { it.copy(busyAction = InventorySessionBusyAction.REQUEST_EXPORT, error = null) }
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
        refresh(sessionId)
    }
}

data class InventoryScannerState(
    val runtimeStatus: NodeRuntimeStatus = NodeRuntimeStatus.STOPPED,
    val code: String = "",
    val resolution: MeshResolveScannedCodeResponse? = null,
    val result: MeshInventoryItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class InventoryScannerStore(
    private val session: NodeSessionController,
) {
    private val _state = MutableStateFlow(InventoryScannerState())
    val state: StateFlow<InventoryScannerState> = _state.asStateFlow()

    fun updateRuntime(status: NodeRuntimeStatus) {
        _state.update { it.copy(runtimeStatus = status) }
    }

    fun updateCode(code: String) {
        _state.update { it.copy(code = code) }
    }

    fun clearNotifications() {
        _state.update { it.copy(error = null, message = null) }
    }

    fun reportError(message: String) {
        _state.update { it.copy(error = message, message = null) }
    }

    fun reportMessage(message: String) {
        _state.update { it.copy(message = message, error = null) }
    }

    fun clearResult() {
        _state.update { it.copy(result = null, resolution = null, error = null, message = null, isLoading = false) }
    }

    suspend fun lookupQr(code: String) {
        resolve(code)
    }

    suspend fun lookupBarcode(code: String) {
        resolve(code)
    }

    private suspend fun resolve(code: String) {
        if (code.isBlank()) {
            _state.update { it.copy(error = "Введите код") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null, message = null) }
        val result = session.withNode { node ->
            node.resolveScannedCode(
                MeshResolveScannedCodeRequest(
                    rawValue = code.trim(),
                ),
            )
        }
        _state.update { current ->
            result.fold(
                onSuccess = { response ->
                    current.copy(
                        isLoading = false,
                        result = response.item,
                        resolution = response,
                        error = when (response.status) {
                            MeshInventoryScanResultStatus.NOT_FOUND -> "Объект не найден"
                            MeshInventoryScanResultStatus.INVALID -> "Неверный код"
                            MeshInventoryScanResultStatus.ERROR -> "Не удалось обработать код"
                            else -> null
                        },
                        message = when (response.status) {
                            MeshInventoryScanResultStatus.RESOLVED -> "Объект найден"
                            MeshInventoryScanResultStatus.INACTIVE -> "Найден объект с неактивным кодом"
                            else -> null
                        },
                    )
                },
                onFailure = { error ->
                    current.copy(isLoading = false, error = error.message ?: "Не удалось найти объект")
                },
            )
        }
    }
}
