package org.expert.link.app.shared.screen.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import org.expert.link.app.shared.presentation.InventoryItemState
import org.expert.link.app.shared.ui.components.BadgeChip
import org.expert.link.app.shared.ui.components.BarcodeCard
import org.expert.link.app.shared.ui.components.ChipTone
import org.expert.link.app.shared.ui.components.InfoRow
import org.expert.link.app.shared.ui.components.QrCodeCard
import org.expert.link.app.shared.ui.components.SectionCard
import org.expert.link.app.shared.ui.components.StatusBanner
import org.expert.link.app.shared.ui.components.asTone
import org.expert.link.app.shared.ui.components.asUiText
import org.expert.link.app.shared.ui.components.asUiTime
import org.expert.link.app.shared.ui.components.shortId
import org.expert.link.mesh.contract.model.MeshInventoryAttachment
import org.expert.link.mesh.contract.model.MeshInventoryAttachmentType
import org.expert.link.mesh.contract.model.MeshInventoryAttributeValue
import org.expert.link.mesh.contract.model.MeshInventoryCodeBinding
import org.expert.link.mesh.contract.model.MeshInventoryCodeType
import org.expert.link.mesh.contract.model.MeshInventoryCondition
import org.expert.link.mesh.contract.model.MeshInventoryLabelFieldKey
import org.expert.link.mesh.contract.model.MeshInventoryIncidentSeverity
import org.expert.link.mesh.contract.model.MeshInventoryIncidentStatus
import org.expert.link.mesh.contract.model.MeshInventoryIncidentType
import org.expert.link.mesh.contract.model.MeshInventoryReview
import org.expert.link.mesh.contract.model.MeshInventoryReviewStatus
import org.expert.link.mesh.contract.model.MeshInventoryStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemScreen(component: InventoryItemComponent) {
    val state by component.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            component.clearError()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            component.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Карточка объекта")
                        state.item?.title?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = component::goBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = component::openDiscussion) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Обсуждение")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ItemSummaryCard(state)
            }
            item {
                ItemOwnershipCard(state)
            }
            item {
                ItemAttributesCard(component, state)
            }
            item {
                ItemActionsCard(component, state)
            }
            item {
                ItemMarkingCard(component, state)
            }
            item {
                ItemQrCard(component, state)
            }
            item {
                ItemCodeBindingsCard(component, state)
            }
            item {
                ItemEditCard(component, state)
            }
            item {
                ItemIncidentsCard(component, state)
            }
            item {
                ItemAttachmentsCard(component, state)
            }
            item {
                ItemCommentsCard(component, state)
            }
            item {
                ItemReviewsCard(component, state)
            }
            item {
                ItemChangeLogCard(state)
            }
            item {
                ItemHistoryCard(state)
            }
        }
    }
}

@Composable
private fun ItemSummaryCard(state: InventoryItemState) {
    val item = state.item ?: return
    SectionCard(title = "Характеристики", subtitle = "Основные данные по объекту и состоянию") {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BadgeChip(item.currentStatus.asUiText(), item.currentStatus.asTone())
                    BadgeChip(item.condition.asUiText(), when (item.condition) {
                        MeshInventoryCondition.GOOD,
                        MeshInventoryCondition.NEW,
                        -> ChipTone.SUCCESS
                        MeshInventoryCondition.FAIR,
                        MeshInventoryCondition.UNKNOWN,
                        -> ChipTone.INFO
                        MeshInventoryCondition.NEEDS_REPAIR,
                        MeshInventoryCondition.OUT_OF_SERVICE,
                        -> ChipTone.WARNING
                    })
                }
            }
        }
        InfoRow("ID", item.inventoryItemId.shortId(12))
        InfoRow("Номер", item.inventoryNumber)
        item.localNumber?.let { InfoRow("Локальный номер", it) }
        InfoRow("Тип", item.itemType.name.lowercase())
        item.brand?.let { InfoRow("Бренд", it) }
        item.model?.let { InfoRow("Модель", it) }
        item.serialNumber?.let { InfoRow("Серийный номер", it) }
        item.manufacturer?.let { InfoRow("Производитель", it) }
        val ownerNames = ownerNames(item, state.owners)
        if (ownerNames.isNotEmpty()) {
            InfoRow("МОЛы", ownerNames.joinToString())
        } else {
            item.responsiblePerson?.let { InfoRow("МОЛ", it) }
        }
        item.responsibleDepartment?.let { InfoRow("Подразделение", it) }
        item.lastInventoryAt?.let { InfoRow("Последняя инвентаризация", it.asUiTime()) }
        item.nextInventoryAt?.let { InfoRow("Следующая инвентаризация", it.asUiTime()) }
        InfoRow("Синхронизация", item.syncStatus.asUiText())
        InfoRow("Версия", item.revision.toString())
        val category = state.categories.firstOrNull { it.categoryId == item.categoryId }
        category?.let { InfoRow("Категория", it.name) }
        val subcategory = state.subcategories.firstOrNull { it.subcategoryId == item.subcategoryId }
        subcategory?.let { InfoRow("Подкатегория", it.name) }
        val location = state.locations.firstOrNull { it.locationId == item.locationId }
        location?.let { InfoRow("Локация", it.name) }
    }
}

@Composable
private fun ItemActionsCard(component: InventoryItemComponent, state: InventoryItemState) {
    val item = state.item ?: return
    SectionCard(title = "Быстрые действия") {
        OutlinedButton(
            onClick = { component.updateStatus(MeshInventoryStatus.UNDER_REVIEW, "Отправлено на согласование") },
            enabled = (item.currentStatus == MeshInventoryStatus.DRAFT || item.currentStatus == MeshInventoryStatus.ADDED) &&
                state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_SUBMIT_REVIEW),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Отправить на согласование")
        }
        OutlinedButton(
            onClick = { component.updateStatus(MeshInventoryStatus.ARCHIVED, "Архивировано") },
            enabled = item.currentStatus != MeshInventoryStatus.ARCHIVED &&
                state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_ARCHIVE),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Перенести в архив")
        }
        if (item.currentStatus == MeshInventoryStatus.UNDER_REVIEW || item.currentStatus == MeshInventoryStatus.REQUIRES_UPDATE) {
            Button(
                onClick = { component.submitReview(MeshInventoryReviewStatus.APPROVED, "Подтверждено") },
                enabled = state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_CONFIRM),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Подтвердить")
            }
            OutlinedButton(
                onClick = { component.submitReview(MeshInventoryReviewStatus.REJECTED, "Отклонено") },
                enabled = state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_REJECT),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Отклонить")
            }
            OutlinedButton(
                onClick = { component.submitReview(MeshInventoryReviewStatus.REQUIRES_UPDATE, "Нужны правки") },
                enabled = state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_REJECT),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Вернуть на доработку")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemMarkingCard(component: InventoryItemComponent, state: InventoryItemState) {
    val item = state.item ?: return
    val barcodeCode = state.codes.firstOrNull { it.codeType == MeshInventoryCodeType.BARCODE && it.isActive }
    val qrCode = state.codes.firstOrNull { it.codeType == MeshInventoryCodeType.QR && it.isActive }
    val barcodeValue = barcodeCode?.rawValue ?: item.barcode
    val qrValue = qrCode?.rawValue ?: item.qrCode
    val templates = state.labelTemplates
    val defaultTemplateId = templates.firstOrNull { it.isDefault }?.templateId ?: templates.firstOrNull()?.templateId
    val canEdit = state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_EDIT)
    val canLabel = state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_VIEW)
    var selectedTemplateId by remember(item.inventoryItemId, templates.size) { mutableStateOf(defaultTemplateId) }
    val selectedTemplate = templates.firstOrNull { it.templateId == selectedTemplateId }
    var includeBarcode by remember(item.inventoryItemId, selectedTemplateId) {
        mutableStateOf(selectedTemplate?.includeBarcode ?: true)
    }
    var includeQr by remember(item.inventoryItemId, selectedTemplateId) {
        mutableStateOf(selectedTemplate?.includeQr ?: true)
    }
    var selectedFields by remember(item.inventoryItemId, selectedTemplateId) {
        mutableStateOf(selectedTemplate?.fields?.toSet() ?: emptySet())
    }
    var showBarcode by remember(item.inventoryItemId) { mutableStateOf(false) }
    var showQr by remember(item.inventoryItemId) { mutableStateOf(false) }
    val orderedFields = MeshInventoryLabelFieldKey.values().filter { it in selectedFields }
    val previewLabel = state.labelPreview ?: state.labels.firstOrNull()
    val readyPdfTask = state.lastLabelPdf
        ?: state.printTasks.firstOrNull { !it.localPath.isNullOrBlank() }
    val molLabel = ownerNames(item, state.owners).joinToString().ifBlank { item.responsiblePerson.orEmpty() }
    val shareText = buildMarkingShareText(item.title, item.inventoryNumber, molLabel, barcodeValue, qrValue)
    val barcodeMatrix = remember(showBarcode, barcodeValue) {
        if (showBarcode && !barcodeValue.isNullOrBlank() && component.capabilities.canRenderBarcode) {
            component.buildBarcode(barcodeValue)
        } else {
            null
        }
    }
    val qrMatrix = remember(showQr, qrValue) {
        if (showQr && !qrValue.isNullOrBlank() && component.capabilities.canRenderQr) {
            component.buildQrCode(qrValue)
        } else {
            null
        }
    }

    SectionCard(title = "Маркировка", subtitle = "QR, штрихкод, этикетка и история сканирования") {
        InfoRow("Инвентарный номер", item.inventoryNumber)
        barcodeCode?.let { InfoRow("Статус штрихкода", if (it.isActive) "Активен" else "Неактивен") }
        qrCode?.let { InfoRow("Статус QR", if (it.isActive) "Активен" else "Неактивен") }
        barcodeCode?.let {
            InfoRow("Штрихкод создан", "${it.createdByPeerId.shortId(10)} · ${it.createdAt.asUiTime()}")
        }
        qrCode?.let {
            InfoRow("QR создан", "${it.createdByPeerId.shortId(10)} · ${it.createdAt.asUiTime()}")
        }
        if (barcodeCode == null && qrCode == null && barcodeValue.isNullOrBlank() && qrValue.isNullOrBlank()) {
            StatusBanner("Код ещё не выпущен", ChipTone.WARNING)
            Button(
                onClick = { component.generateCode(MeshInventoryCodeType.QR) },
                enabled = canEdit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сгенерировать код")
            }
        }

        Text("Управление кодами", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { component.generateCode(MeshInventoryCodeType.BARCODE) },
                enabled = canEdit && barcodeCode == null,
            ) {
                Text("Сгенерировать штрихкод")
            }
            OutlinedButton(
                onClick = { component.generateCode(MeshInventoryCodeType.BARCODE, regenerate = true) },
                enabled = canEdit,
            ) {
                Text("Перевыпустить")
            }
        }
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { component.generateCode(MeshInventoryCodeType.QR) },
                enabled = canEdit && qrCode == null,
            ) {
                Text("Сгенерировать QR")
            }
            OutlinedButton(
                onClick = { component.generateCode(MeshInventoryCodeType.QR, regenerate = true) },
                enabled = canEdit,
            ) {
                Text("Перевыпустить")
            }
        }
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            barcodeCode?.let { activeCode ->
                OutlinedButton(onClick = { component.deactivateCode(activeCode.inventoryCodeId) }, enabled = canEdit) {
                    Text("Деактивировать штрихкод")
                }
            }
            qrCode?.let { activeCode ->
                OutlinedButton(onClick = { component.deactivateCode(activeCode.inventoryCodeId) }, enabled = canEdit) {
                    Text("Деактивировать QR")
                }
            }
        }

        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected = showBarcode,
                onClick = { showBarcode = !showBarcode },
                label = { Text(if (showBarcode) "Скрыть штрихкод" else "Показать штрихкод") },
            )
            FilterChip(
                selected = showQr,
                onClick = { showQr = !showQr },
                label = { Text(if (showQr) "Скрыть QR" else "Показать QR") },
            )
        }

        if (showBarcode && !barcodeValue.isNullOrBlank()) {
            if (barcodeMatrix != null) {
                BarcodeCard(
                    matrix = barcodeMatrix,
                    title = "Штрихкод",
                    subtitle = barcodeValue,
                )
            } else {
                StatusBanner("Штрихкод: $barcodeValue", ChipTone.INFO)
            }
        }
        if (showQr && !qrValue.isNullOrBlank()) {
            if (qrMatrix != null) {
                QrCodeCard(
                    matrix = qrMatrix,
                    title = "QR код",
                    subtitle = qrValue,
                )
            } else {
                StatusBanner("QR: $qrValue", ChipTone.INFO)
            }
        }

        if (templates.isNotEmpty()) {
            Text("Шаблон этикетки", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templates) { template ->
                    FilterChip(
                        selected = template.templateId == selectedTemplateId,
                        onClick = {
                            selectedTemplateId = template.templateId
                            includeBarcode = template.includeBarcode
                            includeQr = template.includeQr
                            selectedFields = template.fields.toSet()
                        },
                        label = { Text(template.name) },
                    )
                }
            }
        }
        Text("Поля этикетки", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventoryLabelFieldKey.values()) { key ->
                FilterChip(
                    selected = key in selectedFields,
                    onClick = {
                        selectedFields = if (key in selectedFields) {
                            selectedFields - key
                        } else {
                            selectedFields + key
                        }
                    },
                    label = { Text(markingFieldLabel(key)) },
                )
            }
        }
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected = includeBarcode,
                onClick = { includeBarcode = !includeBarcode },
                label = { Text("Штрихкод") },
            )
            FilterChip(
                selected = includeQr,
                onClick = { includeQr = !includeQr },
                label = { Text("QR") },
            )
        }
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilledTonalButton(
                onClick = {
                    component.generateLabelPreview(
                        templateId = selectedTemplateId,
                        fields = orderedFields.takeIf { it.isNotEmpty() },
                        includeBarcode = includeBarcode,
                        includeQr = includeQr,
                    )
                },
                enabled = canLabel,
            ) {
                Icon(Icons.Outlined.Visibility, contentDescription = null)
                Text("Предпросмотр печати")
            }
            OutlinedButton(
                onClick = {
                    component.generateLabelPdf(
                        templateId = selectedTemplateId,
                        fields = orderedFields.takeIf { it.isNotEmpty() },
                        includeBarcode = includeBarcode,
                        includeQr = includeQr,
                    )
                },
                enabled = canLabel,
            ) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                Text("Сохранить в PDF")
            }
            Button(
                onClick = {
                    component.printLabel(
                        templateId = selectedTemplateId,
                        fields = orderedFields.takeIf { it.isNotEmpty() },
                        includeBarcode = includeBarcode,
                        includeQr = includeQr,
                    )
                },
                enabled = canLabel,
            ) {
                Icon(Icons.Outlined.Print, contentDescription = null)
                Text("Печать")
            }
        }

        readyPdfTask?.let { task ->
            val localPath = task.localPath
            if (!localPath.isNullOrBlank()) {
                FlowRow(
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (component.capabilities.canPrintFiles) {
                        OutlinedButton(onClick = { component.printPdf(localPath) }) {
                            Text("Печать")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            component.savePdf(
                                path = localPath,
                                fileName = task.resultDescriptor?.fileName ?: "label-${item.inventoryNumber}.pdf",
                            )
                        },
                    ) {
                        Text("Скачать PDF")
                    }
                    OutlinedButton(onClick = { component.sharePdf(localPath) }) {
                        Text("Поделиться")
                    }
                    if (!qrValue.isNullOrBlank() && component.capabilities.canShareFiles) {
                        OutlinedButton(onClick = { component.shareQrImage(qrValue) }) {
                            Text("Поделиться QR фото")
                        }
                    }
                }
            } else {
                OutlinedButton(onClick = { component.shareText("Маркировка", shareText) }) {
                    Text("Поделиться")
                }
                if (!qrValue.isNullOrBlank() && component.capabilities.canShareFiles) {
                    OutlinedButton(onClick = { component.shareQrImage(qrValue) }) {
                        Text("Поделиться QR фото")
                    }
                }
            }
        } ?: OutlinedButton(onClick = { component.shareText("Маркировка", shareText) }) {
            Text("Поделиться")
        }
        if (!qrValue.isNullOrBlank() && component.capabilities.canShareFiles) {
            OutlinedButton(onClick = { component.shareQrImage(qrValue) }) {
                Text("Поделиться QR фото")
            }
        }

        previewLabel?.let { label ->
            Text("Превью этикетки", style = MaterialTheme.typography.labelLarge)
            label.fields.forEach { field ->
                InfoRow(field.label, field.value)
            }
            label.barcodeValue?.let { InfoRow("Штрихкод", it) }
            label.qrValue?.let { InfoRow("QR", it) }
        }

        Text("История кодов", style = MaterialTheme.typography.labelLarge)
        if (state.codes.isEmpty()) {
            Text("Коды ещё не выпускались", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.codes.forEach { code ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BadgeChip(code.codeType.name, ChipTone.INFO)
                                BadgeChip(if (code.isActive) "Активен" else "Неактивен", if (code.isActive) ChipTone.SUCCESS else ChipTone.WARNING)
                            }
                            Text(code.rawValue, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${code.createdByPeerId.shortId(10)} · ${code.createdAt.asUiTime()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Text("История печати", style = MaterialTheme.typography.labelLarge)
        if (state.printTasks.isEmpty()) {
            Text("Печать ещё не выполнялась", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.printTasks.take(5).forEach { task ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BadgeChip(task.status.asUiText(), task.status.asTone())
                                Text(task.updatedAt.asUiTime(), style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Задача ${task.printTaskId.shortId(10)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Text("История сканирования", style = MaterialTheme.typography.labelLarge)
        if (state.scanEvents.isEmpty()) {
            Text("Сканирований пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.scanEvents.take(5).forEach { event ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BadgeChip(event.codeType.name, ChipTone.INFO)
                                BadgeChip(event.resultStatus.asUiText(), ChipTone.INFO)
                            }
                            Text(event.rawValue, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${event.scannedByPeerId.shortId(10)} · ${event.scannedAt.asUiTime()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun markingFieldLabel(key: MeshInventoryLabelFieldKey): String = when (key) {
    MeshInventoryLabelFieldKey.TITLE -> "Название"
    MeshInventoryLabelFieldKey.INVENTORY_NUMBER -> "Инв. номер"
    MeshInventoryLabelFieldKey.RESPONSIBLE_PERSON -> "МОЛ"
    MeshInventoryLabelFieldKey.LOCATION -> "Локация"
    MeshInventoryLabelFieldKey.DEPARTMENT -> "Подразделение"
    MeshInventoryLabelFieldKey.ORGANIZATION -> "Организация"
    MeshInventoryLabelFieldKey.NEXT_INVENTORY_AT -> "Следующая инвентаризация"
    MeshInventoryLabelFieldKey.STATUS -> "Статус"
    MeshInventoryLabelFieldKey.CONDITION -> "Состояние"
}

private fun buildMarkingShareText(
    title: String,
    inventoryNumber: String,
    molLabel: String?,
    barcodeValue: String?,
    qrValue: String?,
): String = buildString {
    appendLine("Объект: $title")
    appendLine("Инвентарный номер: $inventoryNumber")
    molLabel?.takeIf { it.isNotBlank() }?.let { appendLine("МОЛ: $it") }
    barcodeValue?.let { appendLine("Штрихкод: $it") }
    qrValue?.let { appendLine("QR: $it") }
}.trim()

@Composable
private fun ItemQrCard(component: InventoryItemComponent, state: InventoryItemState) {
    val item = state.item ?: return
    val qrCode = item.qrCode
    if (!qrCode.isNullOrBlank() && component.capabilities.canRenderQr) {
        component.buildQrCode(qrCode)?.let { matrix ->
            QrCodeCard(
                matrix = matrix,
                title = "QR код",
                subtitle = listOfNotNull(
                    "Инв. № ${item.inventoryNumber}",
                    ownerNames(item, state.owners).joinToString().ifBlank { item.responsiblePerson.orEmpty() }
                        .ifBlank { null },
                ).joinToString(" • "),
            )
        }
    } else {
        SectionCard(title = "QR / штрихкод") {
            Text(qrCode ?: "QR не задан")
            item.barcode?.let { Text("Штрихкод: $it") }
            if (qrCode.isNullOrBlank()) {
                Button(onClick = component::generateQrCode) { Text("Сгенерировать QR") }
            }
        }
    }
}

@Composable
private fun ItemEditCard(component: InventoryItemComponent, state: InventoryItemState) {
    val item = state.item ?: return
    var inventoryNumber by remember(item.inventoryItemId) { mutableStateOf(item.inventoryNumber) }
    var localNumber by remember(item.inventoryItemId) { mutableStateOf(item.localNumber.orEmpty()) }
    var title by remember(item.inventoryItemId) { mutableStateOf(item.title) }
    var description by remember(item.inventoryItemId) { mutableStateOf(item.description.orEmpty()) }
    var serialNumber by remember(item.inventoryItemId) { mutableStateOf(item.serialNumber.orEmpty()) }
    var brand by remember(item.inventoryItemId) { mutableStateOf(item.brand.orEmpty()) }
    var model by remember(item.inventoryItemId) { mutableStateOf(item.model.orEmpty()) }
    var manufacturer by remember(item.inventoryItemId) { mutableStateOf(item.manufacturer.orEmpty()) }
    var selectedCondition by remember(item.inventoryItemId) { mutableStateOf(item.condition) }
    var selectedCategoryId by remember(item.inventoryItemId) { mutableStateOf(item.categoryId) }
    var selectedSubcategoryId by remember(item.inventoryItemId) { mutableStateOf(item.subcategoryId) }
    var selectedLocationId by remember(item.inventoryItemId) { mutableStateOf(item.locationId) }
    var departmentId by remember(item.inventoryItemId) { mutableStateOf(item.departmentId) }
    var selectedOwnerIds by remember(item.inventoryItemId) { mutableStateOf(item.allOwnerIds()) }
    var costCenterId by remember(item.inventoryItemId) { mutableStateOf(item.costCenterId) }
    var legalHolderId by remember(item.inventoryItemId) { mutableStateOf(item.legalHolderId) }
    var supplierId by remember(item.inventoryItemId) { mutableStateOf(item.supplierId) }
    var fundingSourceId by remember(item.inventoryItemId) { mutableStateOf(item.fundingSourceId) }
    var showAdvanced by remember(item.inventoryItemId) { mutableStateOf(false) }
    val filteredOwners = availableOwnersForSelection(
        owners = molOwners(state.owners),
        selectedDepartmentIds = setOfNotNull(departmentId),
        selectedLocationIds = setOfNotNull(selectedLocationId),
        locations = state.locations,
    )

    SectionCard(title = "Редактирование") {
        OutlinedTextField(
            value = inventoryNumber,
            onValueChange = { inventoryNumber = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Инвентарный номер") },
            singleLine = true,
        )
        OutlinedTextField(
            value = localNumber,
            onValueChange = { localNumber = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Локальный номер") },
            singleLine = true,
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название") },
            singleLine = true,
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Описание") },
            minLines = 2,
        )
        OutlinedTextField(
            value = serialNumber,
            onValueChange = { serialNumber = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Серийный номер") },
            singleLine = true,
        )
        OutlinedTextField(
            value = brand,
            onValueChange = { brand = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Бренд") },
            singleLine = true,
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Модель") },
            singleLine = true,
        )
        OutlinedTextField(
            value = manufacturer,
            onValueChange = { manufacturer = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Производитель") },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeshInventoryCondition.values().forEach { value ->
                FilterChip(
                    selected = selectedCondition == value,
                    onClick = { selectedCondition = value },
                    label = { Text(value.asUiText()) },
                )
            }
        }
        if (state.categories.isNotEmpty()) {
            Text("Категория", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.categoryId,
                        onClick = {
                            selectedCategoryId = category.categoryId
                            selectedSubcategoryId = null
                        },
                        label = { Text(category.name) },
                    )
                }
            }
        }
        val availableSubcategories = state.subcategories.filter { it.categoryId == selectedCategoryId }
        if (availableSubcategories.isNotEmpty()) {
            Text("Подкатегория", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableSubcategories) { subcategory ->
                    FilterChip(
                        selected = selectedSubcategoryId == subcategory.subcategoryId,
                        onClick = { selectedSubcategoryId = subcategory.subcategoryId },
                        label = { Text(subcategory.name) },
                    )
                }
            }
        }
        if (state.locations.isNotEmpty()) {
            Text("Локация", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.locations) { location ->
                    FilterChip(
                        selected = selectedLocationId == location.locationId,
                        onClick = { selectedLocationId = location.locationId },
                        label = { Text(location.name) },
                    )
                }
            }
        }
        if (state.departments.isNotEmpty()) {
            Text("Подразделение", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.departments) { department ->
                    FilterChip(
                        selected = departmentId == department.departmentId,
                        onClick = { departmentId = department.departmentId },
                        label = { Text(department.name) },
                    )
                }
            }
        }
        Text("МОЛы", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredOwners) { owner ->
                FilterChip(
                    selected = owner.ownerId in selectedOwnerIds,
                    onClick = {
                        selectedOwnerIds = if (owner.ownerId in selectedOwnerIds) {
                            selectedOwnerIds - owner.ownerId
                        } else {
                            selectedOwnerIds + owner.ownerId
                        }
                    },
                    label = { Text(owner.name) },
                )
            }
        }
        OutlinedButton(onClick = { showAdvanced = !showAdvanced }) {
            Text(if (showAdvanced) "Скрыть расширенные поля" else "Расширенные поля")
        }
        if (showAdvanced) {
            if (state.costCenters.isNotEmpty()) {
                Text("Центр затрат", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.costCenters) { center ->
                        FilterChip(
                            selected = costCenterId == center.costCenterId,
                            onClick = { costCenterId = center.costCenterId },
                            label = { Text(center.name) },
                        )
                    }
                }
            }
            if (state.legalHolders.isNotEmpty()) {
                Text("Балансодержатель", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.legalHolders) { holder ->
                        FilterChip(
                            selected = legalHolderId == holder.legalHolderId,
                            onClick = { legalHolderId = holder.legalHolderId },
                            label = { Text(holder.name) },
                        )
                    }
                }
            }
            if (state.suppliers.isNotEmpty()) {
                Text("Поставщик", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.suppliers) { supplier ->
                        FilterChip(
                            selected = supplierId == supplier.supplierId,
                            onClick = { supplierId = supplier.supplierId },
                            label = { Text(supplier.name) },
                        )
                    }
                }
            }
            if (state.fundingSources.isNotEmpty()) {
                Text("Источник финансирования", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.fundingSources) { source ->
                        FilterChip(
                            selected = fundingSourceId == source.fundingSourceId,
                            onClick = { fundingSourceId = source.fundingSourceId },
                            label = { Text(source.name) },
                        )
                    }
                }
            }
        }
        Button(
            onClick = {
                component.updateItem(
                    inventoryNumber = inventoryNumber,
                    localNumber = localNumber,
                    title = title,
                    description = description,
                    brand = brand,
                    model = model,
                    serialNumber = serialNumber,
                    manufacturer = manufacturer,
                    condition = selectedCondition,
                    categoryId = selectedCategoryId,
                    subcategoryId = selectedSubcategoryId,
                    locationId = selectedLocationId,
                    responsiblePerson = null,
                    responsibleDepartment = null,
                    ownerId = selectedOwnerIds.firstOrNull(),
                    departmentId = departmentId,
                    responsibleOwnerIds = selectedOwnerIds,
                    costCenterId = costCenterId,
                    legalHolderId = legalHolderId,
                    supplierId = supplierId,
                    fundingSourceId = fundingSourceId,
                    tagIds = null,
                    attributes = null,
                )
            },
            enabled = state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_EDIT),
        ) {
            Text("Сохранить изменения")
        }
    }
}

@Composable
private fun ItemOwnershipCard(state: InventoryItemState) {
    val item = state.item ?: return
    SectionCard(title = "Подразделение и МОЛы") {
        val owner = state.owners.firstOrNull { it.ownerId == item.ownerId }
        val responsibleOwners = item.allOwnerIds().mapNotNull { ownerId ->
            state.owners.firstOrNull { it.ownerId == ownerId }
        }
        val department = state.departments.firstOrNull { it.departmentId == item.departmentId }
        val costCenter = state.costCenters.firstOrNull { it.costCenterId == item.costCenterId }
        val legalHolder = state.legalHolders.firstOrNull { it.legalHolderId == item.legalHolderId }
        val supplier = state.suppliers.firstOrNull { it.supplierId == item.supplierId }
        val funding = state.fundingSources.firstOrNull { it.fundingSourceId == item.fundingSourceId }
        if (responsibleOwners.isNotEmpty()) {
            InfoRow("МОЛы", responsibleOwners.joinToString { it.name })
        } else {
            owner?.let { InfoRow("МОЛ", it.name) }
        }
        department?.let { InfoRow("Подразделение", it.name) }
        costCenter?.let { InfoRow("Центр затрат", it.name) }
        legalHolder?.let { InfoRow("Балансодержатель", it.name) }
        supplier?.let { InfoRow("Поставщик", it.name) }
        funding?.let { InfoRow("Источник", it.name) }
        if (owner == null && responsibleOwners.isEmpty() && department == null && costCenter == null && legalHolder == null && supplier == null && funding == null) {
            Text("Нет данных о МОЛах или подразделении", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ItemAttributesCard(component: InventoryItemComponent, state: InventoryItemState) {
    val item = state.item ?: return
    val attributeValues = remember(item.inventoryItemId) {
        androidx.compose.runtime.mutableStateMapOf<String, String>().apply {
            item.attributes.forEach { value -> put(value.attributeId, value.value) }
        }
    }
    var selectedTags by remember(item.inventoryItemId) { mutableStateOf(item.tagIds) }
    val canEdit = state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_EDIT)
    val peerId = state.localPeerId

    SectionCard(title = "Теги и атрибуты") {
        if (state.tags.isEmpty()) {
            Text("Теги не настроены", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.tags) { tag ->
                    FilterChip(
                        selected = tag.tagId in selectedTags,
                        onClick = {
                            selectedTags = if (tag.tagId in selectedTags) {
                                selectedTags - tag.tagId
                            } else {
                                selectedTags + tag.tagId
                            }
                        },
                        label = { Text(tag.name) },
                    )
                }
            }
        }
        if (state.attributeDefinitions.isEmpty()) {
            Text("Атрибуты не заданы", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.attributeDefinitions.forEach { definition ->
                    OutlinedTextField(
                        value = attributeValues[definition.attributeId].orEmpty(),
                        onValueChange = { attributeValues[definition.attributeId] = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(definition.label) },
                        singleLine = true,
                    )
                }
            }
        }
        Button(
            onClick = {
                val now = Clock.System.now()
                val values = attributeValues.mapNotNull { (attributeId, value) ->
                    value.takeIf { it.isNotBlank() }?.let {
                        MeshInventoryAttributeValue(
                            attributeId = attributeId,
                            value = it,
                            updatedByPeerId = peerId ?: "local",
                            updatedAt = now,
                        )
                    }
                }
                component.updateItem(
                    inventoryNumber = null,
                    localNumber = null,
                    title = null,
                    description = null,
                    brand = null,
                    model = null,
                    serialNumber = null,
                    manufacturer = null,
                    condition = null,
                    categoryId = null,
                    subcategoryId = null,
                    locationId = null,
                    responsiblePerson = null,
                    responsibleDepartment = null,
                    ownerId = null,
                    departmentId = null,
                    responsibleOwnerIds = null,
                    costCenterId = null,
                    legalHolderId = null,
                    supplierId = null,
                    fundingSourceId = null,
                    tagIds = selectedTags,
                    attributes = values,
                )
            },
            enabled = canEdit && peerId != null,
        ) {
            Text("Сохранить теги и атрибуты")
        }
    }
}

@Composable
private fun ItemCodeBindingsCard(component: InventoryItemComponent, state: InventoryItemState) {
    val item = state.item ?: return
    var codeValue by remember(item.inventoryItemId) { mutableStateOf("") }
    var codeType by remember(item.inventoryItemId) { mutableStateOf(MeshInventoryCodeType.QR) }
    SectionCard(title = "Коды и привязки") {
        if (state.codeBindings.isEmpty()) {
            Text("Коды не привязаны", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.codeBindings.forEach { binding ->
                    CodeBindingRow(binding)
                }
            }
        }
        Text("Новый код", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventoryCodeType.values()) { type ->
                FilterChip(
                    selected = codeType == type,
                    onClick = { codeType = type },
                    label = { Text(type.name.lowercase()) },
                )
            }
        }
        OutlinedTextField(
            value = codeValue,
            onValueChange = { codeValue = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Значение кода") },
            singleLine = true,
        )
        Button(
            onClick = {
                component.createCodeBinding(codeType, codeValue)
                codeValue = ""
            },
            enabled = state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_EDIT),
        ) {
            Text("Привязать")
        }
    }
}

@Composable
private fun CodeBindingRow(binding: MeshInventoryCodeBinding) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(binding.codeType.name.lowercase(), fontWeight = FontWeight.SemiBold)
                Text(binding.codeValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(binding.createdAt.asUiTime(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ItemIncidentsCard(component: InventoryItemComponent, state: InventoryItemState) {
    val item = state.item ?: return
    var incidentTitle by remember(item.inventoryItemId) { mutableStateOf("") }
    var incidentDescription by remember(item.inventoryItemId) { mutableStateOf("") }
    var incidentType by remember(item.inventoryItemId) { mutableStateOf(MeshInventoryIncidentType.OTHER) }
    var incidentSeverity by remember(item.inventoryItemId) { mutableStateOf(MeshInventoryIncidentSeverity.MEDIUM) }
    SectionCard(title = "Инциденты") {
        if (state.incidents.isEmpty()) {
            Text("Инцидентов нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.incidents.forEach { incident ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BadgeChip(incident.severity.asUiText(), incident.severity.asTone())
                                BadgeChip(incident.status.asUiText(), ChipTone.INFO)
                            }
                            Text(incident.title, fontWeight = FontWeight.SemiBold)
                            incident.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            if (state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.INCIDENT_RESOLVE)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { component.updateIncidentStatus(incident.incidentId, MeshInventoryIncidentStatus.RESOLVED, null) },
                                        enabled = incident.status != MeshInventoryIncidentStatus.RESOLVED,
                                    ) {
                                        Text("Решено")
                                    }
                                    OutlinedButton(
                                        onClick = { component.updateIncidentStatus(incident.incidentId, MeshInventoryIncidentStatus.DISMISSED, null) },
                                        enabled = incident.status != MeshInventoryIncidentStatus.DISMISSED,
                                    ) {
                                        Text("Отклонить")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Text("Новый инцидент", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventoryIncidentType.values()) { value ->
                FilterChip(
                    selected = incidentType == value,
                    onClick = { incidentType = value },
                    label = { Text(value.name.lowercase()) },
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventoryIncidentSeverity.values()) { value ->
                FilterChip(
                    selected = incidentSeverity == value,
                    onClick = { incidentSeverity = value },
                    label = { Text(value.asUiText()) },
                )
            }
        }
        OutlinedTextField(
            value = incidentTitle,
            onValueChange = { incidentTitle = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Описание проблемы") },
            singleLine = true,
        )
        OutlinedTextField(
            value = incidentDescription,
            onValueChange = { incidentDescription = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Комментарий") },
            minLines = 2,
        )
        Button(
            onClick = {
                component.reportIncident(incidentType, incidentSeverity, incidentTitle, incidentDescription)
                incidentTitle = ""
                incidentDescription = ""
            },
            enabled = state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.INCIDENT_CREATE),
        ) {
            Text("Зафиксировать")
        }
    }
}

@Composable
private fun ItemChangeLogCard(state: InventoryItemState) {
    SectionCard(title = "Журнал изменений") {
        if (!state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.AUDIT_VIEW)) {
            StatusBanner("Нет доступа к журналу изменений", ChipTone.WARNING)
        } else if (state.changeLogs.isEmpty()) {
            Text("Изменений нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.changeLogs.forEach { change ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "Изменение ${change.changeId.shortId(8)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("Автор: ${change.changedByPeerId.shortId(10)}", style = MaterialTheme.typography.bodySmall)
                            Text("Время: ${change.changedAt.asUiTime()}", style = MaterialTheme.typography.bodySmall)
                            change.changes.forEach { field ->
                                Text(
                                    "${field.field}: ${field.previousValue ?: "-"} → ${field.newValue ?: "-"}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemAttachmentsCard(component: InventoryItemComponent, state: InventoryItemState) {
    SectionCard(title = "Фото и вложения") {
        if (state.attachments.isEmpty()) {
            Text("Вложений пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.attachments.forEach { attachment ->
                    AttachmentRow(component, attachment)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { component.addAttachment(MeshInventoryAttachmentType.PHOTO, "Фото") },
                enabled = state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_UPLOAD_PHOTO),
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Фото", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(
                onClick = { component.addAttachment(MeshInventoryAttachmentType.DOCUMENT, "Документ") },
                enabled = state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_ATTACH),
            ) {
                Text("Файл")
            }
        }
    }
}

@Composable
private fun AttachmentRow(component: InventoryItemComponent, attachment: MeshInventoryAttachment) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(attachment.descriptor.fileName, fontWeight = FontWeight.SemiBold)
            Text(
                "${attachment.descriptor.sizeBytes} байт",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (attachment.attachmentType == MeshInventoryAttachmentType.PHOTO) "Фотография" else "Документ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            attachment.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (component.capabilities.canOpenFiles) {
                OutlinedButton(onClick = { component.openAttachment(attachment) }) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Открыть", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun ItemCommentsCard(component: InventoryItemComponent, state: InventoryItemState) {
    var commentText by remember { mutableStateOf("") }
    SectionCard(title = "Комментарии") {
        if (state.comments.isEmpty()) {
            Text("Комментариев пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.comments.forEach { comment ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(comment.body)
                            Text(
                                comment.createdAt.asUiTime(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Комментарий") },
            minLines = 2,
        )
        Button(
            onClick = { component.addComment(commentText).also { commentText = "" } },
            enabled = state.permissions.contains(org.expert.link.mesh.contract.model.MeshInventoryPermission.ITEM_COMMENT),
        ) {
            Text("Добавить")
        }
    }
}

@Composable
private fun ItemReviewsCard(component: InventoryItemComponent, state: InventoryItemState) {
    SectionCard(title = "Инвентаризация") {
        if (state.reviews.isEmpty()) {
            Text("Проверок пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.reviews.forEach { review ->
                    ReviewRow(review)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { component.submitReview(MeshInventoryReviewStatus.APPROVED, "Подтверждено") }) {
                Text("Подтвердить")
            }
            OutlinedButton(onClick = { component.submitReview(MeshInventoryReviewStatus.REJECTED, "Отклонено") }) {
                Text("Отклонить")
            }
        }
    }
}

@Composable
private fun ReviewRow(review: MeshInventoryReview) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(review.status.asUiText(), fontWeight = FontWeight.SemiBold)
            review.comment?.let { Text(it) }
            Text(
                review.updatedAt.asUiTime(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ItemHistoryCard(state: InventoryItemState) {
    SectionCard(title = "История изменений") {
        if (state.history.isEmpty()) {
            Text("История пуста", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.history.forEach { event ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(event.eventType.name.lowercase().replace('_', ' '), fontWeight = FontWeight.SemiBold)
                            Text(
                                event.occurredAt.asUiTime(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
