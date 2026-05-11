package org.expert.link.app.shared.screen.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.expert.link.app.shared.presentation.InventorySessionReviewDraft
import org.expert.link.app.shared.presentation.InventorySessionState
import org.expert.link.app.shared.ui.components.BadgeChip
import org.expert.link.app.shared.ui.components.ChipTone
import org.expert.link.app.shared.ui.components.InfoRow
import org.expert.link.app.shared.ui.components.SectionCard
import org.expert.link.app.shared.ui.components.StatusBanner
import org.expert.link.app.shared.ui.components.asTone
import org.expert.link.app.shared.ui.components.asUiText
import org.expert.link.app.shared.ui.components.asUiTime
import org.expert.link.app.shared.ui.components.shortId
import org.expert.link.mesh.contract.model.MeshInventoryAcceptanceStatus
import org.expert.link.mesh.contract.model.MeshInventoryExportFormat
import org.expert.link.mesh.contract.model.MeshInventoryLabelFieldKey
import org.expert.link.mesh.contract.model.MeshInventoryPermission
import org.expert.link.mesh.contract.model.MeshInventoryPresenceStatus
import org.expert.link.mesh.contract.model.MeshInventoryConfirmationStatus
import org.expert.link.mesh.contract.model.MeshInventoryReviewStatus
import org.expert.link.mesh.contract.model.MeshInventorySessionRole
import org.expert.link.mesh.contract.model.MeshOrganizationMemberStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorySessionScreen(component: InventorySessionComponent) {
    val state by component.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var printFeedback by remember { mutableStateOf<String?>(null) }

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

    LaunchedEffect(printFeedback) {
        printFeedback?.let {
            snackbarHostState.showSnackbar(it)
            printFeedback = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Инвентаризация")
                        state.session?.title?.let {
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
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "Чат комиссии")
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
            item { SessionSummaryCard(state) }
            item { SessionWorkflowCard(component, state) }
            item { SessionMembersCard(component, state) }
            item { SessionInventoryCard(component, state) }
            item { SessionPrintCard(component, state, onFeedback = { printFeedback = it }) }
            item { SessionExportsCard(component, state) }
        }
    }
}

@Composable
private fun SessionSummaryCard(state: InventorySessionState) {
    val session = state.session ?: return
    val ownerLabels = session.ownerIds.mapNotNull { ownerId ->
        state.owners.firstOrNull { it.ownerId == ownerId }?.name
    }
    val departmentLabels = session.departmentIds.mapNotNull { departmentId ->
        state.departments.firstOrNull { it.departmentId == departmentId }?.name
    }
    val locationLabels = session.locationIds.mapNotNull { locationId ->
        locationPath(locationId, state.locations)
    }
    SectionCard(title = "Параметры инвентаризации") {
        InfoRow("ID", session.sessionId.shortId(12))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BadgeChip(session.workflowStatus.asUiText(), session.workflowStatus.asTone())
            BadgeChip(session.status.asUiText(), ChipTone.INFO)
        }
        InfoRow("Статус проверки", session.reviewStatus.asUiText())
        InfoRow("Начало", session.periodStart.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString())
        session.periodEnd?.let {
            InfoRow("Конец", it.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString())
        }
        if (departmentLabels.isNotEmpty()) {
            InfoRow("Подразделения", departmentLabels.joinToString())
        }
        if (ownerLabels.isNotEmpty()) {
            InfoRow("МОЛы", ownerLabels.joinToString())
        }
        if (locationLabels.isNotEmpty()) {
            InfoRow("Локации", locationLabels.joinToString())
        }
        Text(
            "Объектов: ${session.itemIds.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        session.completionBlockedReason?.let {
            StatusBanner(it, ChipTone.WARNING)
        }
        session.result?.let { result ->
            InfoRow("Подтверждено", result.confirmedCount.toString())
            InfoRow("Отклонено", result.rejectedCount.toString())
            InfoRow("На доработке", result.requiresUpdateCount.toString())
            InfoRow("Инциденты", result.incidentCount.toString())
        }
    }
}

@Composable
private fun SessionWorkflowCard(component: InventorySessionComponent, state: InventorySessionState) {
    val session = state.session ?: return
    var correctionTitle by remember(session.sessionId) { mutableStateOf("") }
    var correctionDescription by remember(session.sessionId) { mutableStateOf("") }
    val commissionPeers = state.organizationMembers
        .filter { it.isCommissionMember && it.status == MeshOrganizationMemberStatus.ACTIVE }
        .map { it.peerId }
        .toSet()
    SectionCard(title = "Действия по инвентаризации") {
        if (session.requiresPhotoForDiscrepancy) {
            StatusBanner("При расхождениях обязательно приложить фото. Без этого инвентаризация не считается успешно завершённой.", ChipTone.INFO)
        }
        OutlinedButton(
            onClick = component::closeSession,
            enabled = state.permissions.contains(MeshInventoryPermission.SESSION_CLOSE) &&
                session.completionBlockedReason == null,
            modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
        ) {
            Text("Завершить инвентаризацию")
        }
        session.completionBlockedReason?.let {
            StatusBanner(it, ChipTone.WARNING)
        }
        Text("Заявка на исправление", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = correctionTitle,
            onValueChange = { correctionTitle = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Что нужно исправить") },
            singleLine = true,
        )
        OutlinedTextField(
            value = correctionDescription,
            onValueChange = { correctionDescription = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Комментарий для комиссии") },
            minLines = 2,
        )
        Button(
            onClick = {
                component.reportCorrectionRequest(
                    title = correctionTitle,
                    description = correctionDescription,
                    assigneePeerIds = commissionPeers,
                )
                correctionTitle = ""
                correctionDescription = ""
            },
            enabled = state.permissions.contains(MeshInventoryPermission.INCIDENT_CREATE),
            modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
        ) {
            Text("Отправить заявку на исправление")
        }
        Text("Экспорт", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventoryExportFormat.values()) { format ->
                OutlinedButton(
                    onClick = { component.requestExport(format) },
                    enabled = state.permissions.contains(MeshInventoryPermission.EXPORT_REQUEST),
                ) {
                    Text(format.name)
                }
            }
        }
    }
}

@Composable
private fun SessionMembersCard(component: InventorySessionComponent, state: InventorySessionState) {
    val session = state.session ?: return
    var selectedPeerId by remember(session.sessionId) { mutableStateOf<String?>(null) }
    var role by remember { mutableStateOf(MeshInventorySessionRole.MEMBER) }
    val activeSessionMemberIds = remember(state.members) { state.members.map { it.peerId }.toSet() }
    val commissionCandidates = remember(state.organizationMembers, state.members) {
        state.organizationMembers
            .filter { member ->
                member.isCommissionMember &&
                    member.status == MeshOrganizationMemberStatus.ACTIVE &&
                    member.peerId !in activeSessionMemberIds
            }
            .sortedBy { it.displayName.ifBlank { it.peerId } }
    }
    SectionCard(title = "Комиссия") {
        if (state.members.isEmpty()) {
            Text("Участников пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.members.forEach { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            val displayName = state.organizationMembers.firstOrNull { it.peerId == member.peerId }?.displayName
                            Text(displayName ?: member.peerId.shortId(8), fontWeight = FontWeight.SemiBold)
                            Text(member.peerId.shortId(10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        BadgeChip(sessionRoleLabel(member.role), ChipTone.INFO)
                    }
                }
            }
        }
        if (commissionCandidates.isNotEmpty()) {
            Text("Добавить из комиссии", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(commissionCandidates) { member ->
                    FilterChip(
                        selected = selectedPeerId == member.peerId,
                        onClick = { selectedPeerId = member.peerId },
                        label = {
                            Text(
                                member.displayName.ifBlank { member.peerId.shortId(8) },
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        } else {
            StatusBanner("Все участники комиссии уже добавлены в эту инвентаризацию.", ChipTone.INFO)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventorySessionRole.values()) { value ->
                FilterChip(
                    selected = role == value,
                    onClick = { role = value },
                    label = {
                        Text(
                            text = sessionRoleLabel(value),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
        Button(
            onClick = {
                selectedPeerId?.let { peerId ->
                    component.addMember(peerId, role)
                    selectedPeerId = null
                }
            },
            enabled = selectedPeerId != null && state.permissions.contains(MeshInventoryPermission.SESSION_ADD_MEMBER),
            modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
        ) {
            Text("Добавить")
        }
    }
}

@Composable
private fun SessionInventoryCard(component: InventorySessionComponent, state: InventorySessionState) {
    val session = state.session ?: return
    val personOwners = remember(state.owners) { molOwners(state.owners) }
    val reviewMap = remember(state.reviews) { state.reviews.associateBy { it.inventoryItemId } }
    val drafts = remember(session.sessionId) { mutableStateMapOf<String, SessionReviewEditorState>() }
    val dirtyFlags = remember(session.sessionId) { mutableStateMapOf<String, Boolean>() }
    val attachmentCountByItem = remember(state.attachments) {
        state.attachments.groupingBy { it.inventoryItemId }.eachCount()
    }
    var searchText by remember(session.sessionId) { mutableStateOf("") }
    var selectedOwnerIds by remember(session.sessionId) { mutableStateOf(setOf<String>()) }
    var selectedLocationIds by remember(session.sessionId) { mutableStateOf(setOf<String>()) }
    var selectedPresenceStatuses by remember(session.sessionId) { mutableStateOf(setOf<MeshInventoryPresenceStatus>()) }
    var selectedAcceptanceStatuses by remember(session.sessionId) { mutableStateOf(setOf<MeshInventoryAcceptanceStatus>()) }
    var selectedConfirmationStatuses by remember(session.sessionId) { mutableStateOf(setOf<MeshInventoryConfirmationStatus>()) }
    LaunchedEffect(session.sessionId, state.reviews, state.items) {
        state.items.forEach { item ->
            if (dirtyFlags[item.inventoryItemId] != true || item.inventoryItemId !in drafts) {
                drafts[item.inventoryItemId] = reviewMap[item.inventoryItemId].toEditorState()
            }
        }
    }
    val filteredItems = state.items.filter { item ->
        val currentDraft = drafts[item.inventoryItemId] ?: reviewMap[item.inventoryItemId].toEditorState()
        val query = searchText.trim()
        val queryMatches = query.isBlank() ||
            item.title.contains(query, ignoreCase = true) ||
            item.inventoryNumber.contains(query, ignoreCase = true) ||
            (locationPath(item.locationId, state.locations) ?: "").contains(query, ignoreCase = true)
        val ownerMatches = selectedOwnerIds.isEmpty() || item.allOwnerIds().any { it in selectedOwnerIds }
        val locationMatches = matchesLocationFilter(item.locationId, selectedLocationIds, state.locations)
        val presenceMatches = selectedPresenceStatuses.isEmpty() || currentDraft.presenceStatus in selectedPresenceStatuses
        val acceptanceMatches = selectedAcceptanceStatuses.isEmpty() || currentDraft.acceptanceStatus in selectedAcceptanceStatuses
        val confirmationMatches = selectedConfirmationStatuses.isEmpty() || currentDraft.confirmationStatus in selectedConfirmationStatuses
        queryMatches && ownerMatches && locationMatches && presenceMatches && acceptanceMatches && confirmationMatches
    }
    val groupedItems = filteredItems.groupBy { item ->
        ownerNames(item, personOwners).firstOrNull() ?: item.responsiblePerson ?: "Без МОЛа"
    }.toSortedMap()
    val pendingDrafts = drafts.entries
        .filter { dirtyFlags[it.key] == true }
        .map { (itemId, draft) ->
            InventorySessionReviewDraft(
                itemId = itemId,
                status = draft.status,
                presenceStatus = draft.presenceStatus,
                acceptanceStatus = draft.acceptanceStatus,
                confirmationStatus = draft.confirmationStatus,
                requiresPhoto = draft.requiresPhoto,
                comment = draft.comment,
            )
        }
    var selectedItems by remember(session.sessionId) { mutableStateOf(setOf<String>()) }

    SectionCard(
        title = "Оборудование по МОЛам",
        subtitle = if (component.capabilities.prefersWideLayout) {
            "Дашборд для desktop: фильтры и таблица по МОЛам"
        } else {
            "Карточки для mobile: отметьте наличие, принятие и подтверждение"
        },
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск по оборудованию, номеру или локации") },
            singleLine = true,
        )
        FlowRow(
            maxItemsInEachRow = 3,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BadgeChip("Показано: ${filteredItems.size}", ChipTone.INFO)
            BadgeChip("Несохранено: ${pendingDrafts.size}", if (pendingDrafts.isNotEmpty()) ChipTone.WARNING else ChipTone.SUCCESS)
            if (state.attachments.isNotEmpty()) {
                BadgeChip("Фото: ${state.attachments.size}", ChipTone.INFO)
            }
        }
        if (personOwners.isNotEmpty()) {
            Text("МОЛы", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(personOwners) { owner ->
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
        }
        if (state.locations.isNotEmpty()) {
            Text("Локации", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.locations) { location ->
                    FilterChip(
                        selected = location.locationId in selectedLocationIds,
                        onClick = {
                            selectedLocationIds = if (location.locationId in selectedLocationIds) {
                                selectedLocationIds - location.locationId
                            } else {
                                selectedLocationIds + location.locationId
                            }
                        },
                        label = { Text(location.name) },
                    )
                }
            }
        }
        Text("Наличие", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(MeshInventoryPresenceStatus.PRESENT, MeshInventoryPresenceStatus.ABSENT, MeshInventoryPresenceStatus.UNCHECKED)) { value ->
                FilterChip(
                    selected = value in selectedPresenceStatuses,
                    onClick = {
                        selectedPresenceStatuses = if (value in selectedPresenceStatuses) {
                            selectedPresenceStatuses - value
                        } else {
                            selectedPresenceStatuses + value
                        }
                    },
                    label = { Text(value.asUiText()) },
                )
            }
        }
        Text("Принятие", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(MeshInventoryAcceptanceStatus.ACCEPTED, MeshInventoryAcceptanceStatus.NOT_ACCEPTED, MeshInventoryAcceptanceStatus.UNCHECKED)) { value ->
                FilterChip(
                    selected = value in selectedAcceptanceStatuses,
                    onClick = {
                        selectedAcceptanceStatuses = if (value in selectedAcceptanceStatuses) {
                            selectedAcceptanceStatuses - value
                        } else {
                            selectedAcceptanceStatuses + value
                        }
                    },
                    label = { Text(value.asUiText()) },
                )
            }
        }
        Text("Подтверждение", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(MeshInventoryConfirmationStatus.CONFIRMED, MeshInventoryConfirmationStatus.NOT_CONFIRMED, MeshInventoryConfirmationStatus.UNCHECKED)) { value ->
                FilterChip(
                    selected = value in selectedConfirmationStatuses,
                    onClick = {
                        selectedConfirmationStatuses = if (value in selectedConfirmationStatuses) {
                            selectedConfirmationStatuses - value
                        } else {
                            selectedConfirmationStatuses + value
                        }
                    },
                    label = { Text(value.asUiText()) },
                )
            }
        }
        if (groupedItems.isEmpty()) {
            Text("По выбранным фильтрам оборудование не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            if (component.capabilities.prefersWideLayout) {
                groupedItems.forEach { (molLabel, itemsByOwner) ->
                    SessionInventoryTableGroup(
                        title = molLabel,
                        items = itemsByOwner,
                        state = state,
                        drafts = drafts,
                        dirtyFlags = dirtyFlags,
                        reviewMap = reviewMap,
                        attachmentCountByItem = attachmentCountByItem,
                        onAddAttachment = { itemId -> component.addAttachment(itemId, "Фото по инвентаризации") },
                        onOpenItem = component::openItem,
                    )
                }
            } else {
                groupedItems.forEach { (molLabel, itemsByOwner) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(molLabel, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            itemsByOwner.forEach { item ->
                                SessionInventoryEditorCard(
                                    item = item,
                                    state = state,
                                    currentDraft = drafts[item.inventoryItemId] ?: reviewMap[item.inventoryItemId].toEditorState(),
                                    photoCount = attachmentCountByItem[item.inventoryItemId] ?: 0,
                                    onDraftChange = { updated ->
                                        drafts[item.inventoryItemId] = updated
                                        dirtyFlags[item.inventoryItemId] = true
                                    },
                                    onAddAttachment = { component.addAttachment(item.inventoryItemId, "Фото по инвентаризации") },
                                    onOpenItem = { component.openItem(item.inventoryItemId) },
                                )
                            }
                        }
                    }
                }
            }
        }
        Text("Добавить объекты", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.allItems) { item ->
                FilterChip(
                    selected = item.inventoryItemId in selectedItems,
                    onClick = {
                        selectedItems = if (item.inventoryItemId in selectedItems) {
                            selectedItems - item.inventoryItemId
                        } else {
                            selectedItems + item.inventoryItemId
                        }
                    },
                    label = { Text(item.inventoryNumber, maxLines = 1, softWrap = false) },
                )
            }
        }
        Button(
            onClick = { component.addItems(selectedItems).also { selectedItems = emptySet() } },
            enabled = state.permissions.contains(MeshInventoryPermission.SESSION_ADD_ITEM),
            modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
        ) {
            Text("Добавить выбранное оборудование")
        }
        Button(
            onClick = {
                component.saveReviews(pendingDrafts)
                pendingDrafts.forEach { dirtyFlags[it.itemId] = false }
            },
            enabled = pendingDrafts.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
        ) {
            Text("Сохранить")
        }
    }
}

@Composable
private fun SessionInventoryEditorCard(
    item: org.expert.link.mesh.contract.model.MeshInventoryItem,
    state: InventorySessionState,
    currentDraft: SessionReviewEditorState,
    photoCount: Int,
    onDraftChange: (SessionReviewEditorState) -> Unit,
    onAddAttachment: () -> Unit,
    onOpenItem: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "№ ${item.inventoryNumber} • ${locationPath(item.locationId, state.locations) ?: "Локация не указана"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                BadgeChip("Фото: $photoCount", if (photoCount > 0) ChipTone.SUCCESS else ChipTone.WARNING)
                OutlinedButton(onClick = onAddAttachment) {
                    Text("Фото")
                }
                OutlinedButton(
                    onClick = onOpenItem,
                    modifier = Modifier.wrapContentWidth(),
                ) {
                    Text("Карточка", maxLines = 1, softWrap = false)
                }
            }
        }
        Text("Наличие", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(MeshInventoryPresenceStatus.PRESENT, MeshInventoryPresenceStatus.ABSENT)) { value ->
                FilterChip(
                    selected = currentDraft.presenceStatus == value,
                    onClick = {
                        onDraftChange(
                            currentDraft.copy(presenceStatus = value).normalized(),
                        )
                    },
                    label = { Text(value.asUiText()) },
                )
            }
        }
        Text("Принятие", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(MeshInventoryAcceptanceStatus.ACCEPTED, MeshInventoryAcceptanceStatus.NOT_ACCEPTED)) { value ->
                FilterChip(
                    selected = currentDraft.acceptanceStatus == value,
                    onClick = {
                        onDraftChange(
                            currentDraft.copy(acceptanceStatus = value).normalized(),
                        )
                    },
                    label = { Text(value.asUiText()) },
                )
            }
        }
        Text("Подтверждение", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(MeshInventoryConfirmationStatus.CONFIRMED, MeshInventoryConfirmationStatus.NOT_CONFIRMED)) { value ->
                FilterChip(
                    selected = currentDraft.confirmationStatus == value,
                    onClick = {
                        onDraftChange(
                            currentDraft.copy(confirmationStatus = value).normalized(),
                        )
                    },
                    label = { Text(value.asUiText()) },
                )
            }
        }
        if (currentDraft.requiresPhoto && photoCount == 0) {
            StatusBanner("Для этого объекта требуется фотография подтверждения.", ChipTone.WARNING)
        }
    }
}

@Composable
private fun SessionInventoryTableGroup(
    title: String,
    items: List<org.expert.link.mesh.contract.model.MeshInventoryItem>,
    state: InventorySessionState,
    drafts: MutableMap<String, SessionReviewEditorState>,
    dirtyFlags: MutableMap<String, Boolean>,
    reviewMap: Map<String, org.expert.link.mesh.contract.model.MeshInventoryReview>,
    attachmentCountByItem: Map<String, Int>,
    onAddAttachment: (String) -> Unit,
    onOpenItem: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val tableWidth = 1420.dp
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                val changedCount = items.count { dirtyFlags[it.inventoryItemId] == true }
                if (changedCount > 0) {
                    BadgeChip("Изменений: $changedCount", ChipTone.WARNING)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.width(tableWidth),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Инв.№", modifier = Modifier.width(110.dp), fontWeight = FontWeight.SemiBold)
                        Text("Название", modifier = Modifier.width(220.dp), fontWeight = FontWeight.SemiBold)
                        Text("Локация", modifier = Modifier.width(210.dp), fontWeight = FontWeight.SemiBold)
                        Text("Наличие", modifier = Modifier.width(165.dp), fontWeight = FontWeight.SemiBold)
                        Text("Принятие", modifier = Modifier.width(165.dp), fontWeight = FontWeight.SemiBold)
                        Text("Подтверждение", modifier = Modifier.width(185.dp), fontWeight = FontWeight.SemiBold)
                        Text("Фото", modifier = Modifier.width(120.dp), fontWeight = FontWeight.SemiBold)
                        Text("Действия", modifier = Modifier.width(180.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
                items.forEachIndexed { index, item ->
                    val currentDraft = drafts[item.inventoryItemId] ?: reviewMap[item.inventoryItemId].toEditorState()
                    val photoCount = attachmentCountByItem[item.inventoryItemId] ?: 0
                    Surface(
                        modifier = Modifier.width(tableWidth),
                        shape = MaterialTheme.shapes.medium,
                        color = if (index % 2 == 0) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(item.inventoryNumber, modifier = Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall)
                            Text(item.title, modifier = Modifier.width(220.dp), style = MaterialTheme.typography.bodySmall)
                            Text(
                                locationPath(item.locationId, state.locations) ?: "Локация не указана",
                                modifier = Modifier.width(210.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Column(modifier = Modifier.width(165.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(MeshInventoryPresenceStatus.PRESENT, MeshInventoryPresenceStatus.ABSENT).forEach { value ->
                                    FilterChip(
                                        selected = currentDraft.presenceStatus == value,
                                        onClick = {
                                            val updated = currentDraft.copy(presenceStatus = value).normalized()
                                            drafts[item.inventoryItemId] = updated
                                            dirtyFlags[item.inventoryItemId] = true
                                        },
                                        label = { Text(value.asUiText(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    )
                                }
                            }
                            Column(modifier = Modifier.width(165.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(MeshInventoryAcceptanceStatus.ACCEPTED, MeshInventoryAcceptanceStatus.NOT_ACCEPTED).forEach { value ->
                                    FilterChip(
                                        selected = currentDraft.acceptanceStatus == value,
                                        onClick = {
                                            val updated = currentDraft.copy(acceptanceStatus = value).normalized()
                                            drafts[item.inventoryItemId] = updated
                                            dirtyFlags[item.inventoryItemId] = true
                                        },
                                        label = { Text(value.asUiText(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    )
                                }
                            }
                            Column(modifier = Modifier.width(185.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(MeshInventoryConfirmationStatus.CONFIRMED, MeshInventoryConfirmationStatus.NOT_CONFIRMED).forEach { value ->
                                    FilterChip(
                                        selected = currentDraft.confirmationStatus == value,
                                        onClick = {
                                            val updated = currentDraft.copy(confirmationStatus = value).normalized()
                                            drafts[item.inventoryItemId] = updated
                                            dirtyFlags[item.inventoryItemId] = true
                                        },
                                        label = { Text(value.asUiText(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    )
                                }
                            }
                            Column(modifier = Modifier.width(120.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                BadgeChip("Фото: $photoCount", if (photoCount > 0) ChipTone.SUCCESS else ChipTone.WARNING)
                                if (currentDraft.requiresPhoto && photoCount == 0) {
                                    Text(
                                        "Нужно фото",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            Column(modifier = Modifier.width(180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { onAddAttachment(item.inventoryItemId) }) {
                                    Text("Фото")
                                }
                                OutlinedButton(onClick = { onOpenItem(item.inventoryItemId) }) {
                                    Text("Карточка")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sessionRoleLabel(role: MeshInventorySessionRole): String = when (role) {
    MeshInventorySessionRole.CHAIR -> "Председатель"
    MeshInventorySessionRole.REVIEWER -> "Аудитор"
    MeshInventorySessionRole.MEMBER -> "Участник"
    MeshInventorySessionRole.OBSERVER -> "Наблюдатель"
}

private data class SessionReviewEditorState(
    val status: MeshInventoryReviewStatus,
    val presenceStatus: MeshInventoryPresenceStatus,
    val acceptanceStatus: MeshInventoryAcceptanceStatus,
    val confirmationStatus: MeshInventoryConfirmationStatus,
    val requiresPhoto: Boolean,
    val comment: String? = null,
)

private fun org.expert.link.mesh.contract.model.MeshInventoryReview?.toEditorState(): SessionReviewEditorState =
    SessionReviewEditorState(
        status = this?.status ?: MeshInventoryReviewStatus.REQUIRES_UPDATE,
        presenceStatus = this?.presenceStatus ?: MeshInventoryPresenceStatus.UNCHECKED,
        acceptanceStatus = this?.acceptanceStatus ?: MeshInventoryAcceptanceStatus.UNCHECKED,
        confirmationStatus = this?.confirmationStatus ?: MeshInventoryConfirmationStatus.UNCHECKED,
        requiresPhoto = this?.requiresPhoto ?: false,
        comment = this?.comment,
    ).normalized()

private fun SessionReviewEditorState.normalized(): SessionReviewEditorState {
    val negative = presenceStatus == MeshInventoryPresenceStatus.ABSENT ||
        acceptanceStatus == MeshInventoryAcceptanceStatus.NOT_ACCEPTED ||
        confirmationStatus == MeshInventoryConfirmationStatus.NOT_CONFIRMED
    val unresolved = presenceStatus == MeshInventoryPresenceStatus.UNCHECKED ||
        acceptanceStatus == MeshInventoryAcceptanceStatus.UNCHECKED ||
        confirmationStatus == MeshInventoryConfirmationStatus.UNCHECKED
    return copy(
        status = when {
            unresolved -> MeshInventoryReviewStatus.REQUIRES_UPDATE
            negative -> MeshInventoryReviewStatus.REJECTED
            else -> MeshInventoryReviewStatus.APPROVED
        },
        requiresPhoto = negative || requiresPhoto,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionPrintCard(
    component: InventorySessionComponent,
    state: InventorySessionState,
    onFeedback: (String) -> Unit,
) {
    if (state.session == null) return
    val defaultTemplateId = state.labelTemplates.firstOrNull { it.isDefault }?.templateId
        ?: state.labelTemplates.firstOrNull()?.templateId
    var selectedTemplateId by remember(state.session.sessionId, state.labelTemplates.size) {
        mutableStateOf(defaultTemplateId)
    }
    val selectedTemplate = state.labelTemplates.firstOrNull { it.templateId == selectedTemplateId }
    var includeBarcode by remember(state.session.sessionId, selectedTemplateId) {
        mutableStateOf(selectedTemplate?.includeBarcode ?: true)
    }
    var includeQr by remember(state.session.sessionId, selectedTemplateId) {
        mutableStateOf(selectedTemplate?.includeQr ?: true)
    }
    var selectedFields by remember(state.session.sessionId, selectedTemplateId) {
        mutableStateOf(selectedTemplate?.fields?.toSet() ?: emptySet())
    }
    var selectedItems by remember(state.session.sessionId, state.items.size) {
        mutableStateOf(state.items.map { it.inventoryItemId }.toSet())
    }
    val canPrint = state.permissions.contains(MeshInventoryPermission.EXPORT_REQUEST)
    val orderedFields = MeshInventoryLabelFieldKey.values().filter { it in selectedFields }

    SectionCard(title = "Печать маркировки", subtitle = "Пакетная печать по объектам инвентаризации") {
        if (state.items.isEmpty()) {
            Text("В инвентаризации нет объектов для печати", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SectionCard
        }

        Text("Шаблон", style = MaterialTheme.typography.labelLarge)
        if (state.labelTemplates.isEmpty()) {
            StatusBanner("Шаблоны не настроены", ChipTone.WARNING)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.labelTemplates) { template ->
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

        Text("Объекты", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.items) { item ->
                FilterChip(
                    selected = item.inventoryItemId in selectedItems,
                    onClick = {
                        selectedItems = if (item.inventoryItemId in selectedItems) {
                            selectedItems - item.inventoryItemId
                        } else {
                            selectedItems + item.inventoryItemId
                        }
                    },
                    label = { Text(item.inventoryNumber) },
                )
            }
        }

        Text("Поля", style = MaterialTheme.typography.labelLarge)
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
            Button(
                onClick = {
                    component.printLabelsBatch(
                        itemIds = selectedItems.toList(),
                        templateId = selectedTemplateId,
                        fields = orderedFields.takeIf { it.isNotEmpty() },
                        includeBarcode = includeBarcode,
                        includeQr = includeQr,
                    ) { result ->
                        result.onSuccess {
                            onFeedback("Печать запущена: ${it.printTaskId.shortId(10)}")
                        }.onFailure { error ->
                            onFeedback(error.message ?: "Не удалось запустить печать")
                        }
                    }
                },
                enabled = canPrint && selectedItems.isNotEmpty(),
            ) {
                Text("Печать")
            }
            OutlinedButton(
                onClick = { selectedItems = state.items.map { it.inventoryItemId }.toSet() },
                enabled = state.items.isNotEmpty(),
            ) {
                Text("Выбрать все")
            }
        }

        if (state.printTasks.isNotEmpty()) {
            Text("История печати", style = MaterialTheme.typography.labelLarge)
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
                            task.localPath?.let { path ->
                                FlowRow(
                                    maxItemsInEachRow = 2,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    if (component.capabilities.canPrintFiles) {
                                        OutlinedButton(
                                            onClick = {
                                                component.printPdf(path) { result ->
                                                    result.onSuccess {
                                                        onFeedback("PDF отправлен на печать")
                                                    }.onFailure { error ->
                                                        onFeedback(error.message ?: "Не удалось напечатать PDF")
                                                    }
                                                }
                                            },
                                        ) {
                                            Text("Печать")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            component.savePdf(
                                                path = path,
                                                fileName = task.resultDescriptor?.fileName ?: "labels-${task.printTaskId.shortId(6)}.pdf",
                                            ) { result ->
                                                result.onSuccess {
                                                    onFeedback("PDF сохранён")
                                                }.onFailure { error ->
                                                    onFeedback(error.message ?: "Не удалось сохранить PDF")
                                                }
                                            }
                                        },
                                    ) {
                                        Text("Скачать PDF")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            component.sharePdf(path) { result ->
                                                result.onSuccess {
                                                    onFeedback("PDF отправлен в «Поделиться»")
                                                }.onFailure { error ->
                                                    onFeedback(error.message ?: "Не удалось поделиться PDF")
                                                }
                                            }
                                        },
                                    ) {
                                        Text("Поделиться")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionExportsCard(component: InventorySessionComponent, state: InventorySessionState) {
    SectionCard(title = "Экспорт") {
        if (state.exports.isEmpty()) {
            Text("Экспортов пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.exports.forEach { export ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(export.format.name, fontWeight = FontWeight.SemiBold)
                            Text(export.status.asUiText(), style = MaterialTheme.typography.bodySmall)
                            Text(export.updatedAt.asUiTime(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
