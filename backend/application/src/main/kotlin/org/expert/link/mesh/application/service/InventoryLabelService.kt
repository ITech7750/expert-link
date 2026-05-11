package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.inventory.InventoryBarcodeFormat
import org.expert.link.mesh.domain.model.inventory.InventoryCodeType
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryLabel
import org.expert.link.mesh.domain.model.inventory.InventoryLabelField
import org.expert.link.mesh.domain.model.inventory.InventoryLabelFieldKey
import org.expert.link.mesh.domain.model.inventory.InventoryLabelSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLabelTemplate
import org.expert.link.mesh.domain.model.inventory.InventoryLabelTemplateSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLabelTemplateType
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventoryPrintStatus
import org.expert.link.mesh.domain.model.inventory.InventoryPrintTask
import org.expert.link.mesh.domain.model.inventory.InventoryPrintTaskSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.domain.model.inventory.InventoryCondition
import org.expert.link.mesh.domain.model.inventory.InventoryItem
import org.expert.link.mesh.domain.model.inventory.InventoryCode
import org.expert.link.mesh.domain.port.external.InventoryLabelRendererPort
import org.expert.link.mesh.domain.port.repository.InventoryCodeRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryDepartmentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLabelRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLabelTemplateRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLocationRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryPrintTaskRepositoryPort
import org.expert.link.mesh.domain.port.repository.OrganizationRepositoryPort

data class InventoryLabelGenerationOptions(
    val templateId: String? = null,
    val fields: List<InventoryLabelFieldKey>? = null,
    val includeBarcode: Boolean? = null,
    val includeQr: Boolean? = null,
)

data class InventoryLabelPreviewResult(
    val label: InventoryLabel,
    val template: InventoryLabelTemplate,
)

data class InventoryLabelPdfResult(
    val label: InventoryLabel,
    val template: InventoryLabelTemplate,
    val task: InventoryPrintTask,
)

data class InventoryLabelBatchPrintResult(
    val labels: List<InventoryLabel>,
    val template: InventoryLabelTemplate,
    val task: InventoryPrintTask,
)

class InventoryLabelService(
    private val localProfileService: LocalProfileService,
    private val labelTemplateRepositoryPort: InventoryLabelTemplateRepositoryPort,
    private val labelRepositoryPort: InventoryLabelRepositoryPort,
    private val printTaskRepositoryPort: InventoryPrintTaskRepositoryPort,
    private val codeRepositoryPort: InventoryCodeRepositoryPort,
    private val itemRepositoryPort: InventoryItemRepositoryPort,
    private val organizationRepositoryPort: OrganizationRepositoryPort,
    private val locationRepositoryPort: InventoryLocationRepositoryPort,
    private val departmentRepositoryPort: InventoryDepartmentRepositoryPort,
    private val rbacService: InventoryRbacService,
    private val inventoryEventService: InventoryEventService,
    private val inventorySyncService: InventorySyncService,
    private val rendererPort: InventoryLabelRendererPort,
) {
    suspend fun listTemplates(organizationId: String): List<InventoryLabelTemplate> {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.ITEM_VIEW)
        val existing = labelTemplateRepositoryPort.listByOrganization(organizationId)
        if (existing.isNotEmpty()) return existing.sortedBy { it.name }
        return seedDefaultTemplates(organizationId)
    }

    suspend fun createTemplate(
        organizationId: String,
        name: String,
        description: String?,
        templateType: InventoryLabelTemplateType,
        fields: List<InventoryLabelFieldKey>,
        includeBarcode: Boolean,
        includeQr: Boolean,
    ): InventoryLabelTemplate {
        require(name.isNotBlank()) { "Template name is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.TEMPLATE_MANAGE)
        val now = now()
        val template = InventoryLabelTemplate(
            templateId = newId("label-template"),
            organizationId = organizationId,
            name = name.trim(),
            description = description?.trim().orEmpty().ifBlank { null },
            templateType = templateType,
            fields = fields,
            includeBarcode = includeBarcode,
            includeQr = includeQr,
            isDefault = false,
            createdByPeerId = localProfile.peerId,
            createdAt = now,
            updatedAt = now,
        )
        labelTemplateRepositoryPort.save(template)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.LABEL_TEMPLATE,
                entityId = template.templateId,
                eventType = InventoryEventType.LABEL_TEMPLATE_CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryLabelTemplateSnapshot(template),
            ),
        )
        return template
    }

    suspend fun updateTemplate(
        templateId: String,
        name: String?,
        description: String?,
        fields: List<InventoryLabelFieldKey>?,
        includeBarcode: Boolean?,
        includeQr: Boolean?,
    ): InventoryLabelTemplate? {
        val existing = labelTemplateRepositoryPort.findByTemplateId(templateId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(existing.organizationId, localProfile.peerId, InventoryPermission.TEMPLATE_MANAGE)
        val updated = existing.copy(
            name = name?.trim().orEmpty().ifBlank { existing.name },
            description = description?.trim().orEmpty().ifBlank { existing.description },
            fields = fields ?: existing.fields,
            includeBarcode = includeBarcode ?: existing.includeBarcode,
            includeQr = includeQr ?: existing.includeQr,
            updatedAt = now(),
        )
        labelTemplateRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.LABEL_TEMPLATE,
                entityId = updated.templateId,
                eventType = InventoryEventType.LABEL_TEMPLATE_UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryLabelTemplateSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun selectDefaultTemplate(organizationId: String, templateId: String): InventoryLabelTemplate? {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.TEMPLATE_MANAGE)
        val templates = labelTemplateRepositoryPort.listByOrganization(organizationId)
        if (templates.isEmpty()) return null
        var selected: InventoryLabelTemplate? = null
        templates.forEach { template ->
            val updated = template.copy(
                isDefault = template.templateId == templateId,
                updatedAt = now(),
            )
            labelTemplateRepositoryPort.save(updated)
            if (updated.isDefault) {
                selected = updated
            }
        }
        return selected
    }

    suspend fun generateLabelPreview(
        organizationId: String,
        inventoryItemId: String,
        options: InventoryLabelGenerationOptions = InventoryLabelGenerationOptions(),
    ): InventoryLabelPreviewResult {
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.ITEM_VIEW)
        val template = resolveTemplate(organizationId, options)
        val item = requireNotNull(itemRepositoryPort.findByInventoryItemId(inventoryItemId)) {
            "Item $inventoryItemId not found"
        }
        val label = buildLabel(item, template, localProfile.peerId)
        labelRepositoryPort.save(label)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.LABEL,
                entityId = label.labelId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryLabelSnapshot(label),
            ),
        )
        return InventoryLabelPreviewResult(label = label, template = template)
    }

    suspend fun generateLabelPdf(
        organizationId: String,
        inventoryItemId: String,
        options: InventoryLabelGenerationOptions = InventoryLabelGenerationOptions(),
    ): InventoryLabelPdfResult {
        val preview = generateLabelPreview(organizationId, inventoryItemId, options)
        return createPdfTask(
            organizationId = organizationId,
            labels = listOf(preview.label),
            template = preview.template,
            fileName = "label-$inventoryItemId",
            status = InventoryPrintStatus.GENERATED,
        )
    }

    suspend fun printLabel(
        organizationId: String,
        inventoryItemId: String,
        options: InventoryLabelGenerationOptions = InventoryLabelGenerationOptions(),
    ): InventoryLabelPdfResult {
        val preview = generateLabelPreview(organizationId, inventoryItemId, options)
        val result = createPdfTask(
            organizationId = organizationId,
            labels = listOf(preview.label),
            template = preview.template,
            fileName = "label-$inventoryItemId",
            status = InventoryPrintStatus.PRINTED,
        )
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.LABEL,
                entityId = preview.label.labelId,
                eventType = InventoryEventType.LABEL_PRINTED,
                actorPeerId = result.task.createdByPeerId,
                payload = InventoryLabelSnapshot(preview.label),
            ),
        )
        return result
    }

    suspend fun printLabelsBatch(
        organizationId: String,
        itemIds: List<String>,
        options: InventoryLabelGenerationOptions = InventoryLabelGenerationOptions(),
    ): InventoryLabelBatchPrintResult {
        require(itemIds.isNotEmpty()) { "Item list is empty" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.EXPORT_REQUEST)
        val template = resolveTemplate(organizationId, options)
        val labels = itemIds.mapNotNull { itemId ->
            itemRepositoryPort.findByInventoryItemId(itemId)?.let { item ->
                buildLabel(item, template, localProfile.peerId)
            }
        }
        require(labels.isNotEmpty()) { "No items available for printing" }
        labels.forEach { labelRepositoryPort.save(it) }
        labels.forEach { label ->
            publish(
                inventoryEventService.recordEvent(
                    organizationId = organizationId,
                    entityType = InventoryEntityType.LABEL,
                    entityId = label.labelId,
                    eventType = InventoryEventType.CREATED,
                    actorPeerId = localProfile.peerId,
                    payload = InventoryLabelSnapshot(label),
                ),
            )
        }
        val result = createBatchPdfTask(
            organizationId = organizationId,
            labels = labels,
            template = template,
            fileName = "labels-${organizationId.takeLast(6)}",
            status = InventoryPrintStatus.PRINTED,
        )
        labels.forEach { label ->
            publish(
                inventoryEventService.recordEvent(
                    organizationId = organizationId,
                    entityType = InventoryEntityType.LABEL,
                    entityId = label.labelId,
                    eventType = InventoryEventType.LABEL_PRINTED,
                    actorPeerId = localProfile.peerId,
                    payload = InventoryLabelSnapshot(label),
                ),
            )
        }
        return result
    }

    suspend fun listPrintTasks(organizationId: String): List<InventoryPrintTask> =
        printTaskRepositoryPort.listByOrganization(organizationId)

    suspend fun listLabels(itemId: String): List<InventoryLabel> =
        labelRepositoryPort.listByItem(itemId)

    private suspend fun resolveTemplate(
        organizationId: String,
        options: InventoryLabelGenerationOptions,
    ): InventoryLabelTemplate {
        val templates = listTemplates(organizationId)
        val baseTemplate = if (options.templateId != null) {
            templates.firstOrNull { it.templateId == options.templateId }
                ?: error("Template ${options.templateId} not found")
        } else {
            templates.firstOrNull { it.isDefault } ?: templates.first()
        }
        return baseTemplate.copy(
            fields = options.fields ?: baseTemplate.fields,
            includeBarcode = options.includeBarcode ?: baseTemplate.includeBarcode,
            includeQr = options.includeQr ?: baseTemplate.includeQr,
        )
    }

    private suspend fun seedDefaultTemplates(organizationId: String): List<InventoryLabelTemplate> {
        val localProfile = localProfileService.require()
        val now = now()
        val templates = listOf(
            InventoryLabelTemplate(
                templateId = newId("label-template"),
                organizationId = organizationId,
                name = "Короткий",
                description = "Название и инвентарный номер",
                templateType = InventoryLabelTemplateType.SHORT,
                fields = listOf(
                    InventoryLabelFieldKey.TITLE,
                    InventoryLabelFieldKey.INVENTORY_NUMBER,
                ),
                includeBarcode = true,
                includeQr = false,
                isDefault = false,
                createdByPeerId = localProfile.peerId,
                createdAt = now,
                updatedAt = now,
            ),
            InventoryLabelTemplate(
                templateId = newId("label-template"),
                organizationId = organizationId,
                name = "Стандартный",
                description = "Название, номер, ответственный, локация",
                templateType = InventoryLabelTemplateType.STANDARD,
                fields = listOf(
                    InventoryLabelFieldKey.TITLE,
                    InventoryLabelFieldKey.INVENTORY_NUMBER,
                    InventoryLabelFieldKey.RESPONSIBLE_PERSON,
                    InventoryLabelFieldKey.LOCATION,
                ),
                includeBarcode = true,
                includeQr = true,
                isDefault = true,
                createdByPeerId = localProfile.peerId,
                createdAt = now,
                updatedAt = now,
            ),
            InventoryLabelTemplate(
                templateId = newId("label-template"),
                organizationId = organizationId,
                name = "Расширенный",
                description = "Полные данные для комиссии",
                templateType = InventoryLabelTemplateType.FULL,
                fields = listOf(
                    InventoryLabelFieldKey.TITLE,
                    InventoryLabelFieldKey.INVENTORY_NUMBER,
                    InventoryLabelFieldKey.RESPONSIBLE_PERSON,
                    InventoryLabelFieldKey.LOCATION,
                    InventoryLabelFieldKey.DEPARTMENT,
                    InventoryLabelFieldKey.ORGANIZATION,
                    InventoryLabelFieldKey.NEXT_INVENTORY_AT,
                    InventoryLabelFieldKey.STATUS,
                    InventoryLabelFieldKey.CONDITION,
                ),
                includeBarcode = true,
                includeQr = true,
                isDefault = false,
                createdByPeerId = localProfile.peerId,
                createdAt = now,
                updatedAt = now,
            ),
        )
        templates.forEach { labelTemplateRepositoryPort.save(it) }
        templates.forEach { template ->
            publish(
                inventoryEventService.recordEvent(
                    organizationId = organizationId,
                    entityType = InventoryEntityType.LABEL_TEMPLATE,
                    entityId = template.templateId,
                    eventType = InventoryEventType.LABEL_TEMPLATE_CREATED,
                    actorPeerId = localProfile.peerId,
                    payload = InventoryLabelTemplateSnapshot(template),
                ),
            )
        }
        return templates
    }

    private suspend fun buildLabel(
        item: InventoryItem,
        template: InventoryLabelTemplate,
        actorPeerId: String,
    ): InventoryLabel {
        val organization = organizationRepositoryPort.findByOrganizationId(item.organizationId)
        val location = item.locationId?.let { locationRepositoryPort.findByLocationId(it) }
        val department = item.departmentId?.let { departmentRepositoryPort.findByDepartmentId(it) }
        val barcodeCode = resolveCode(item.inventoryItemId, InventoryCodeType.BARCODE)
        val qrCode = resolveCode(item.inventoryItemId, InventoryCodeType.QR)
        val barcodeValue = if (template.includeBarcode) {
            barcodeCode?.rawValue ?: item.barcode
        } else {
            null
        }
        val qrValue = if (template.includeQr) {
            qrCode?.rawValue ?: item.qrCode
        } else {
            null
        }
        if (template.includeBarcode && barcodeValue.isNullOrBlank()) {
            error("Barcode is missing for item ${item.inventoryItemId}")
        }
        if (template.includeQr && qrValue.isNullOrBlank()) {
            error("QR code is missing for item ${item.inventoryItemId}")
        }
        val fields = template.fields.mapNotNull { key ->
            val value = when (key) {
                InventoryLabelFieldKey.TITLE -> item.title
                InventoryLabelFieldKey.INVENTORY_NUMBER -> item.inventoryNumber
                InventoryLabelFieldKey.RESPONSIBLE_PERSON -> item.responsiblePerson ?: ""
                InventoryLabelFieldKey.LOCATION -> location?.name ?: item.locationId ?: ""
                InventoryLabelFieldKey.DEPARTMENT -> department?.name ?: item.responsibleDepartment ?: ""
                InventoryLabelFieldKey.ORGANIZATION -> organization?.name ?: ""
                InventoryLabelFieldKey.NEXT_INVENTORY_AT -> item.nextInventoryAt?.toString() ?: ""
                InventoryLabelFieldKey.STATUS -> statusLabel(item.currentStatus)
                InventoryLabelFieldKey.CONDITION -> conditionLabel(item.condition)
            }
            if (value.isBlank()) null else InventoryLabelField(key, labelFor(key), value)
        }
        return InventoryLabel(
            labelId = newId("label"),
            organizationId = item.organizationId,
            inventoryItemId = item.inventoryItemId,
            templateId = template.templateId,
            templateName = template.name,
            fields = fields,
            barcodeValue = barcodeValue,
            barcodeFormat = if (barcodeValue != null) InventoryBarcodeFormat.CODE_128 else null,
            qrValue = qrValue,
            createdAt = now(),
            createdByPeerId = actorPeerId,
            version = 1,
        )
    }

    private suspend fun resolveCode(itemId: String, codeType: InventoryCodeType): InventoryCode? {
        return codeRepositoryPort.listByItem(itemId).firstOrNull { it.codeType == codeType && it.isActive }
    }

    private fun labelFor(key: InventoryLabelFieldKey): String = when (key) {
        InventoryLabelFieldKey.TITLE -> "Название"
        InventoryLabelFieldKey.INVENTORY_NUMBER -> "Инв. номер"
        InventoryLabelFieldKey.RESPONSIBLE_PERSON -> "Ответственный"
        InventoryLabelFieldKey.LOCATION -> "Локация"
        InventoryLabelFieldKey.DEPARTMENT -> "Подразделение"
        InventoryLabelFieldKey.ORGANIZATION -> "Организация"
        InventoryLabelFieldKey.NEXT_INVENTORY_AT -> "Следующая проверка"
        InventoryLabelFieldKey.STATUS -> "Статус"
        InventoryLabelFieldKey.CONDITION -> "Состояние"
    }

    private fun statusLabel(status: InventoryStatus): String = when (status) {
        InventoryStatus.DRAFT -> "Черновик"
        InventoryStatus.ADDED -> "Добавлен"
        InventoryStatus.UNDER_REVIEW -> "На проверке"
        InventoryStatus.CONFIRMED -> "Подтвержден"
        InventoryStatus.REJECTED -> "Отклонен"
        InventoryStatus.REQUIRES_UPDATE -> "Нужны правки"
        InventoryStatus.ARCHIVED -> "Архив"
    }

    private fun conditionLabel(condition: InventoryCondition): String = when (condition) {
        InventoryCondition.NEW -> "Новый"
        InventoryCondition.GOOD -> "Хорошее"
        InventoryCondition.FAIR -> "Рабочее"
        InventoryCondition.NEEDS_REPAIR -> "Нужен ремонт"
        InventoryCondition.OUT_OF_SERVICE -> "Неисправен"
        InventoryCondition.UNKNOWN -> "Неизвестно"
    }

    private suspend fun createPdfTask(
        organizationId: String,
        labels: List<InventoryLabel>,
        template: InventoryLabelTemplate,
        fileName: String,
        status: InventoryPrintStatus,
    ): InventoryLabelPdfResult {
        val batchResult = createBatchPdfTask(organizationId, labels, template, fileName, status)
        return InventoryLabelPdfResult(
            label = labels.first(),
            template = template,
            task = batchResult.task,
        )
    }

    private suspend fun createBatchPdfTask(
        organizationId: String,
        labels: List<InventoryLabel>,
        template: InventoryLabelTemplate,
        fileName: String,
        status: InventoryPrintStatus,
    ): InventoryLabelBatchPrintResult {
        val artifact = rendererPort.renderPdfBatch(labels, fileName)
        val localProfile = localProfileService.require()
        val now = now()
        val task = InventoryPrintTask(
            printTaskId = newId("print"),
            organizationId = organizationId,
            templateId = template.templateId,
            labelIds = labels.map { it.labelId },
            itemIds = labels.map { it.inventoryItemId },
            status = status,
            createdByPeerId = localProfile.peerId,
            createdAt = now,
            updatedAt = now,
            resultDescriptor = artifact.descriptor,
            localPath = artifact.localPath,
        )
        printTaskRepositoryPort.save(task)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.PRINT_TASK,
                entityId = task.printTaskId,
                eventType = InventoryEventType.PRINT_TASK_CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryPrintTaskSnapshot(task),
            ),
        )
        return InventoryLabelBatchPrintResult(labels = labels, template = template, task = task)
    }

    private suspend fun publish(event: org.expert.link.mesh.domain.model.inventory.InventoryEvent) {
        inventorySyncService.broadcastEvent(event)
    }
}
