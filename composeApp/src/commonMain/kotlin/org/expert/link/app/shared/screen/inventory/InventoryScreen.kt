package org.expert.link.app.shared.screen.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.expert.link.app.shared.presentation.InventoryBusyAction
import org.expert.link.app.shared.presentation.InventorySection
import org.expert.link.app.shared.presentation.InventoryOverviewState
import org.expert.link.app.shared.presentation.filterReviewItems
import org.expert.link.app.shared.ui.components.BadgeChip
import org.expert.link.app.shared.ui.components.ChipTone
import org.expert.link.app.shared.ui.components.EmptyState
import org.expert.link.app.shared.ui.components.InfoRow
import org.expert.link.app.shared.ui.components.SectionCard
import org.expert.link.app.shared.ui.components.StatusBanner
import org.expert.link.app.shared.ui.components.asTone
import org.expert.link.app.shared.ui.components.asUiText
import org.expert.link.app.shared.ui.components.asUiTime
import org.expert.link.app.shared.ui.components.shortId
import org.expert.link.mesh.contract.model.MeshInventoryCondition
import org.expert.link.mesh.contract.model.MeshInventoryAttributeType
import org.expert.link.mesh.contract.model.MeshInventoryDeadlineTarget
import org.expert.link.mesh.contract.model.MeshInventoryExportFormat
import org.expert.link.mesh.contract.model.MeshInventoryFieldTemplate
import org.expert.link.mesh.contract.model.MeshInventoryItem
import org.expert.link.mesh.contract.model.MeshInventoryLocation
import org.expert.link.mesh.contract.model.MeshInventoryCodeType
import org.expert.link.mesh.contract.model.MeshInventoryLabelFieldKey
import org.expert.link.mesh.contract.model.MeshInventoryPermission
import org.expert.link.mesh.contract.model.MeshInventoryReviewStatus
import org.expert.link.mesh.contract.model.MeshInventoryRuleType
import org.expert.link.mesh.contract.model.MeshInventorySession
import org.expert.link.mesh.contract.model.MeshInventoryStatus
import org.expert.link.mesh.contract.model.MeshOrganizationMemberStatus
import org.expert.link.mesh.contract.model.MeshOrganization
import org.expert.link.mesh.contract.model.MeshRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    component: InventoryComponent,
    showBack: Boolean = true,
    showTopBar: Boolean = true,
    showSectionTabs: Boolean = true,
    showContextPanels: Boolean = true,
    forcedSection: InventorySection? = null,
) {
    val state by component.state.collectAsState()
    val sessionState by component.sessionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var printFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(forcedSection) {
        if (forcedSection != null && state.selectedSection != forcedSection) {
            component.selectSection(forcedSection)
        }
    }

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
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Инвентаризация") },
                    navigationIcon = {
                        if (showBack) {
                            IconButton(onClick = component::goBack) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = component::openScanner) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Сканировать")
                        }
                        IconButton(onClick = component::refreshNow) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Обновить")
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
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
        ) {
            if (showSectionTabs) {
                InventorySectionTabs(state, component)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val showOverviewPanels =
                    showContextPanels && (forcedSection == null || state.selectedSection == InventorySection.DASHBOARD)
                if (showOverviewPanels) {
                    item {
                        InventoryEntryCard(
                            state = state,
                            onLoadDemo = component::seedDemoData,
                            onOpenScanner = component::openScanner,
                        )
                    }
                    item {
                        SyncSection(state = state, onSync = component::syncWithPeer)
                    }

                    item {
                        state.selectedOrganizationId?.let { selected ->
                            val organization = state.organizations.firstOrNull { it.organizationId == selected }
                            if (organization != null) {
                                SectionCard(title = "Активная организация") {
                                    InfoRow("Название", organization.name)
                                    InfoRow("ID", organization.organizationId.shortId(12))
                                    organization.description?.let { InfoRow("Описание", it) }
                                }
                            } else {
                                StatusBanner("Организация не выбрана", ChipTone.WARNING)
                            }
                        } ?: StatusBanner("Выберите организацию для работы", ChipTone.WARNING)
                    }
                }

                when (state.selectedSection) {
                    InventorySection.DASHBOARD -> item { dashboardSection(state) }
                    InventorySection.ORGANIZATIONS -> item { organizationsSection(state, component) }
                    InventorySection.ITEMS -> item { itemsSection(state, component, onPrintFeedback = { printFeedback = it }) }
                    InventorySection.SESSIONS -> item { sessionsSection(state, component) }
                    InventorySection.REVIEW -> item { reviewSection(state, component) }
                    InventorySection.INCIDENTS -> item { incidentsSection(state, component) }
                    InventorySection.REPORTS -> item { reportsSection(state, component, onFeedback = { printFeedback = it }) }
                    InventorySection.CATALOGS -> item { catalogsSection(state, component) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InventoryEntryCard(
    state: InventoryOverviewState,
    onLoadDemo: () -> Unit,
    onOpenScanner: () -> Unit,
) {
    SectionCard(title = "Рабочая панель", subtitle = "Быстрый старт для оборудования, проверок и отчётов") {
        val activeOrg = state.selectedOrganizationId?.let { selected ->
            state.organizations.firstOrNull { it.organizationId == selected }
        }
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = activeOrg?.name ?: "Организация не выбрана",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = if (activeOrg == null) {
                        "Подключите демо-данные или выберите организацию, чтобы открыть полный рабочий сценарий."
                    } else {
                        "Можно сразу перейти к карточкам оборудования, печати маркировки или сканированию."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                )
            }
        }
        Button(onClick = onOpenScanner, modifier = Modifier.fillMaxWidth()) { Text("Сканировать код") }
        OutlinedButton(onClick = onLoadDemo, modifier = Modifier.fillMaxWidth()) { Text("Загрузить демо-данные") }
        Text(
            text = "Если разделы пустые, сначала загрузите демо-данные или выберите организацию в разделе «Организации».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InventorySectionTabs(state: InventoryOverviewState, component: InventoryComponent) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(InventorySection.values()) { section ->
            FilterChip(
                selected = state.selectedSection == section,
                onClick = { component.selectSection(section) },
                label = { Text(section.title) },
            )
        }
    }
}

@Composable
private fun SyncSection(
    state: InventoryOverviewState,
    onSync: (String, String) -> Unit,
) {
    SectionCard(
        title = "Синхронизация",
        subtitle = "Mesh-first обмен данными между узлами",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BadgeChip(
                text = "Организаций: ${state.organizations.size}",
                tone = ChipTone.INFO,
            )
            state.lastEventSequence?.let { sequence ->
                BadgeChip("События: $sequence", ChipTone.INFO)
            }
        }
        state.lastSyncResult?.let { result ->
            InfoRow("Получено событий", result.eventsReceived.toString())
            InfoRow("Последняя последовательность", result.lastSequence.toString())
            InfoRow("Тайм-аут", if (result.timedOut) "да" else "нет")
        }
        state.lastSyncAt?.let {
            InfoRow("Последняя синхронизация", it.asUiTime())
        }
        if (state.peers.isEmpty()) {
            StatusBanner("Нет доверенных узлов для синхронизации", ChipTone.WARNING)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.peers.forEach { peer ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(peer.identity.displayName.ifBlank { peer.identity.peerId.shortId(10) })
                            Text(
                                peer.identity.peerId.shortId(10),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val organizationId = state.selectedOrganizationId
                        OutlinedButton(
                            onClick = { if (organizationId != null) onSync(peer.identity.peerId, organizationId) },
                            enabled = organizationId != null && state.busyAction != InventoryBusyAction.SYNC,
                        ) {
                            Text("Синхронизировать")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun organizationsSection(state: InventoryOverviewState, component: InventoryComponent) {
    var editingOrganization by remember { mutableStateOf<MeshOrganization?>(null) }
    var showCreateForm by remember { mutableStateOf(false) }
    var showMembersAndRoles by remember { mutableStateOf(true) }
    editingOrganization?.let { organization ->
        OrganizationInlineEditDialog(
            organization = organization,
            onDismiss = { editingOrganization = null },
            onSave = { name, description ->
                component.updateOrganizationInline(
                    organizationId = organization.organizationId,
                    name = name,
                    description = description,
                )
                editingOrganization = null
            },
        )
    }
    SectionCard(title = "Дашборд организаций", subtitle = "Структура и доступы") {
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BadgeChip("Организаций: ${state.organizations.size}", ChipTone.INFO)
            BadgeChip("Участников: ${state.members.size}", ChipTone.INFO)
            BadgeChip("Ролей: ${state.roles.size}", ChipTone.INFO)
        }
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(onClick = { showCreateForm = !showCreateForm }) {
                Text(if (showCreateForm) "Скрыть создание" else "Создать организацию")
            }
            OutlinedButton(onClick = { showMembersAndRoles = !showMembersAndRoles }) {
                Text(if (showMembersAndRoles) "Скрыть доступы" else "Показать доступы")
            }
            OutlinedButton(onClick = component::seedDemoData) {
                Text("Загрузить демо-данные")
            }
        }
    }
    itemOrganizationsList(state, component)
    itemOrganizationsTable(state, component, onEdit = { editingOrganization = it })
    if (showCreateForm) {
        itemOrganizationCreate(state, component)
    }
    if (showMembersAndRoles && state.selectedOrganizationId != null) {
        itemMembersList(state)
        itemMemberCreate(state, component)
        itemRolesList(state)
        itemRoleCreate(state, component)
    }
}

@Composable
private fun itemOrganizationsTable(
    state: InventoryOverviewState,
    component: InventoryComponent,
    onEdit: (MeshOrganization) -> Unit,
) {
    if (state.organizations.isEmpty()) return
    val scroll = rememberScrollState()
    SectionCard(title = "Реестр организаций", subtitle = "Табличный просмотр и быстрые действия") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RegistryHeaderRow(
                columns = listOf(
                    "Название" to 240.dp,
                    "Описание" to 300.dp,
                    "ID" to 140.dp,
                    "Статус" to 120.dp,
                    "Действия" to 280.dp,
                ),
            )
            state.organizations.forEachIndexed { index, organization ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = if (index % 2 == 0) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RegistryCell(organization.name, 240.dp, true)
                        RegistryCell(organization.description ?: "—", 300.dp)
                        RegistryCell(organization.organizationId.shortId(12), 140.dp)
                        RegistryCell(
                            if (state.selectedOrganizationId == organization.organizationId) "Активна" else "—",
                            120.dp,
                        )
                        FlowRow(
                            maxItemsInEachRow = 3,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.width(280.dp),
                        ) {
                            OutlinedButton(onClick = { component.selectOrganization(organization.organizationId) }) {
                                Text("Сделать активной")
                            }
                            OutlinedButton(onClick = { onEdit(organization) }) {
                                Text("Редактировать")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun itemOrganizationsList(state: InventoryOverviewState, component: InventoryComponent) {
    if (state.organizations.isEmpty()) {
        EmptyState("Организации", "Список пуст. Создайте организацию.")
        return
    }
    SectionCard(title = "Организации", subtitle = "Доступные для вас организации") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.organizations.forEach { organization ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(organization.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        organization.description?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        BadgeChip(
                            text = if (state.selectedOrganizationId == organization.organizationId) "Активная организация" else "Доступно для выбора",
                            tone = if (state.selectedOrganizationId == organization.organizationId) ChipTone.SUCCESS else ChipTone.INFO,
                        )
                        OutlinedButton(
                            onClick = { component.selectOrganization(organization.organizationId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Открыть в работе")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun itemOrganizationCreate(state: InventoryOverviewState, component: InventoryComponent) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    SectionCard(title = "Новая организация", subtitle = "Создайте организацию для инвентаризации") {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
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
        Button(
            onClick = {
                component.createOrganization(name, description)
                name = ""
                description = ""
            },
            enabled = state.busyAction != InventoryBusyAction.CREATE_ORG,
        ) {
            Text("Создать")
        }
    }
}

@Composable
private fun itemMembersList(state: InventoryOverviewState) {
    SectionCard(title = "Участники", subtitle = "Состав организации") {
        if (state.members.isEmpty()) {
            Text("Участников пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.members.forEach { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(member.displayName.ifBlank { member.peerId.shortId(10) })
                            Text(
                                member.peerId.shortId(10),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        BadgeChip(
                            text = when (member.status) {
                                MeshOrganizationMemberStatus.ACTIVE -> "Активен"
                                MeshOrganizationMemberStatus.INVITED -> "Приглашён"
                                MeshOrganizationMemberStatus.SUSPENDED -> "Приостановлен"
                                MeshOrganizationMemberStatus.LEFT -> "Вышел"
                            },
                            tone = if (member.status == MeshOrganizationMemberStatus.ACTIVE) ChipTone.SUCCESS else ChipTone.WARNING,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun itemMemberCreate(state: InventoryOverviewState, component: InventoryComponent) {
    var peerId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var selectedRoles by remember { mutableStateOf(setOf<String>()) }
    val organizationId = state.selectedOrganizationId ?: return
    SectionCard(title = "Добавить участника", subtitle = "Назначьте роли и доступ") {
        OutlinedTextField(
            value = peerId,
            onValueChange = { peerId = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ID узла") },
            singleLine = true,
        )
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Имя") },
            singleLine = true,
        )
        if (state.roles.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.roles) { role ->
                    FilterChip(
                        selected = role.roleId in selectedRoles,
                        onClick = {
                            selectedRoles = if (role.roleId in selectedRoles) {
                                selectedRoles - role.roleId
                            } else {
                                selectedRoles + role.roleId
                            }
                        },
                        label = {
                            Text(
                                text = roleDisplayName(role.name),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }
        Button(
            onClick = {
                component.addMember(organizationId, peerId, displayName, selectedRoles)
                peerId = ""
                displayName = ""
                selectedRoles = emptySet()
            },
            enabled = state.busyAction != InventoryBusyAction.ADD_MEMBER &&
                state.permissions.contains(MeshInventoryPermission.MEMBER_MANAGE),
        ) {
            Text("Добавить")
        }
    }
}

@Composable
private fun itemRolesList(state: InventoryOverviewState) {
    SectionCard(title = "Роли", subtitle = "Права доступа в организации") {
        if (state.roles.isEmpty()) {
            Text("Ролей пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.roles.forEach { role ->
                    RoleCard(role)
                }
            }
        }
    }
}

@Composable
private fun RoleCard(role: MeshRole) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(roleDisplayName(role.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            role.description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(role.permissions.toList()) { permission ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = permissionRu(permission),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun itemRoleCreate(state: InventoryOverviewState, component: InventoryComponent) {
    val organizationId = state.selectedOrganizationId ?: return
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPermissions by remember { mutableStateOf(setOf<MeshInventoryPermission>()) }
    SectionCard(title = "Новая роль", subtitle = "Выберите набор прав") {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название роли") },
            singleLine = true,
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Описание") },
            minLines = 2,
        )
        LazyRow(
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(MeshInventoryPermission.values()) { permission ->
                FilterChip(
                    selected = permission in selectedPermissions,
                    onClick = {
                        selectedPermissions = if (permission in selectedPermissions) {
                            selectedPermissions - permission
                        } else {
                            selectedPermissions + permission
                        }
                    },
                    label = {
                        Text(
                            text = permissionRu(permission),
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
                component.createRole(organizationId, name, description, selectedPermissions)
                name = ""
                description = ""
                selectedPermissions = emptySet()
            },
            enabled = state.busyAction != InventoryBusyAction.CREATE_ROLE &&
                state.permissions.contains(MeshInventoryPermission.ROLE_MANAGE),
        ) {
            Text("Создать роль")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun itemsSection(
    state: InventoryOverviewState,
    component: InventoryComponent,
    onPrintFeedback: (String) -> Unit,
) {
    val organizationId = state.selectedOrganizationId
    var searchText by remember(organizationId) { mutableStateOf("") }
    var inventoryNumber by remember(organizationId) { mutableStateOf("") }
    var serialNumber by remember(organizationId) { mutableStateOf("") }
    var incidentOnly by remember(organizationId) { mutableStateOf(false) }
    var selectedSessionId by remember(organizationId) { mutableStateOf<String?>(null) }
    var selectedStatus by remember(organizationId) { mutableStateOf(setOf<MeshInventoryStatus>()) }
    var selectedCondition by remember(organizationId) { mutableStateOf(setOf<MeshInventoryCondition>()) }
    var selectedCategoryIds by remember(organizationId) { mutableStateOf(setOf<String>()) }
    var selectedLocationIds by remember(organizationId) { mutableStateOf(setOf<String>()) }
    var selectedDepartmentIds by remember(organizationId) { mutableStateOf(setOf<String>()) }
    var selectedOwnerIds by remember(organizationId) { mutableStateOf(setOf<String>()) }
    var ownerSearchQuery by remember(organizationId) { mutableStateOf("") }
    var showTable by remember(organizationId) { mutableStateOf(true) }
    var showSearchPanel by remember(organizationId) { mutableStateOf(false) }
    var showCreatePanel by remember(organizationId) { mutableStateOf(false) }
    var showPrintPanel by remember(organizationId) { mutableStateOf(false) }
    var editingItem by remember(organizationId) { mutableStateOf<MeshInventoryItem?>(null) }

    editingItem?.let { item ->
        ItemInlineEditDialog(
            state = state,
            item = item,
            onDismiss = { editingItem = null },
            onSave = { inventoryNumber, title, status, condition, locationId, departmentId, ownerId, responsibleOwnerIds ->
                component.updateItemInline(
                    inventoryItemId = item.inventoryItemId,
                    expectedRevision = item.revision,
                    inventoryNumber = inventoryNumber,
                    title = title,
                    status = status,
                    condition = condition,
                    locationId = locationId,
                    departmentId = departmentId,
                    ownerId = ownerId,
                    responsibleOwnerIds = responsibleOwnerIds,
                )
                editingItem = null
            },
        )
    }

    if (organizationId == null) {
        SectionCard(title = "Оборудование", subtitle = "Нет данных для отображения") {
            StatusBanner("Сначала выберите организацию или загрузите демо-данные", ChipTone.WARNING)
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(onClick = component::seedDemoData) {
                    Text("Загрузить демо-данные")
                }
                OutlinedButton(onClick = component::openScanner) {
                    Text("Сканировать код")
                }
            }
            Text(
                text = "Раздел станет доступен после выбора организации в разделе «Организации».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    SectionCard(title = "Дашборд оборудования", subtitle = "Таблица, карточки и операции") {
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected = showTable,
                onClick = { showTable = !showTable },
                label = { Text(if (showTable) "Режим: таблица" else "Режим: карточки") },
            )
            OutlinedButton(onClick = { showSearchPanel = !showSearchPanel }) {
                Text(if (showSearchPanel) "Скрыть фильтры" else "Показать фильтры")
            }
            OutlinedButton(onClick = { showCreatePanel = !showCreatePanel }) {
                Text(if (showCreatePanel) "Скрыть создание" else "Новый объект")
            }
            OutlinedButton(onClick = { showPrintPanel = !showPrintPanel }) {
                Text(if (showPrintPanel) "Скрыть печать" else "Панель печати")
            }
            OutlinedButton(onClick = component::seedDemoData) {
                Text("Загрузить демо-данные")
            }
            OutlinedButton(onClick = component::openScanner) {
                Text("Сканировать")
            }
        }
    }

    if (showSearchPanel) {
        SectionCard(title = "Поиск оборудования", subtitle = "Расширенный поиск по карточкам") {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Текст") },
            singleLine = true,
        )
        OutlinedTextField(
            value = inventoryNumber,
            onValueChange = { inventoryNumber = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Инвентарный номер") },
            singleLine = true,
        )
        OutlinedTextField(
            value = serialNumber,
            onValueChange = { serialNumber = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Серийный номер") },
            singleLine = true,
        )
        OutlinedButton(onClick = component::openScanner) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Сканировать код")
        }
        Text(
            text = "Поиск по QR/штрихкоду выполняется через экран сканирования.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Статус", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventoryStatus.values()) { value ->
                FilterChip(
                    selected = value in selectedStatus,
                    onClick = {
                        selectedStatus = if (value in selectedStatus) {
                            selectedStatus - value
                        } else {
                            selectedStatus + value
                        }
                    },
                    label = { Text(value.asUiText()) },
                )
            }
        }

        Text("Состояние", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventoryCondition.values()) { value ->
                FilterChip(
                    selected = value in selectedCondition,
                    onClick = {
                        selectedCondition = if (value in selectedCondition) {
                            selectedCondition - value
                        } else {
                            selectedCondition + value
                        }
                    },
                    label = { Text(value.asUiText()) },
                )
            }
        }

        if (state.categories.isNotEmpty()) {
            Text("Категории", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories) { category ->
                    FilterChip(
                        selected = category.categoryId in selectedCategoryIds,
                        onClick = {
                            selectedCategoryIds = if (category.categoryId in selectedCategoryIds) {
                                selectedCategoryIds - category.categoryId
                            } else {
                                selectedCategoryIds + category.categoryId
                            }
                        },
                        label = { Text(category.name) },
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

        if (state.departments.isNotEmpty()) {
            Text("Подразделения", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.departments) { department ->
                    FilterChip(
                        selected = department.departmentId in selectedDepartmentIds,
                        onClick = {
                            selectedDepartmentIds = if (department.departmentId in selectedDepartmentIds) {
                                selectedDepartmentIds - department.departmentId
                            } else {
                                selectedDepartmentIds + department.departmentId
                            }
                        },
                        label = { Text(department.name) },
                    )
                }
            }
        }

        val availableOwners = availableOwnersForSelection(
            owners = molOwners(state.owners),
            selectedDepartmentIds = selectedDepartmentIds,
            selectedLocationIds = selectedLocationIds,
            locations = state.locations,
        ).filter { owner ->
            ownerSearchQuery.isBlank() || owner.name.contains(ownerSearchQuery, ignoreCase = true)
        }
        if (availableOwners.isNotEmpty()) {
            Text("МОЛы", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = ownerSearchQuery,
                onValueChange = { ownerSearchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск МОЛа") },
                singleLine = true,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableOwners) { owner ->
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

        if (state.sessions.isNotEmpty()) {
            Text("Инвентаризация", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.sessions) { session ->
                    FilterChip(
                        selected = session.sessionId == selectedSessionId,
                        onClick = {
                            selectedSessionId = if (selectedSessionId == session.sessionId) null else session.sessionId
                        },
                        label = { Text(session.title) },
                    )
                }
            }
        }

        FilterChip(
            selected = incidentOnly,
            onClick = { incidentOnly = !incidentOnly },
            label = { Text("Только с инцидентами") },
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = {
                    component.searchInventory(
                        organizationId = organizationId,
                        text = searchText,
                        inventoryNumber = inventoryNumber,
                        serialNumber = serialNumber,
                        qrCode = null,
                        barcode = null,
                        status = selectedStatus,
                        condition = selectedCondition,
                        categoryIds = selectedCategoryIds,
                        locationIds = selectedLocationIds,
                        departmentIds = selectedDepartmentIds,
                        ownerIds = selectedOwnerIds,
                        incidentOnly = incidentOnly,
                        sessionId = selectedSessionId,
                    )
                },
                enabled = state.permissions.contains(MeshInventoryPermission.SEARCH) && !state.searchLoading,
            ) {
                Text(if (state.searchLoading) "Поиск..." else "Искать")
            }
            OutlinedButton(
                onClick = {
                    component.clearSearch()
                    searchText = ""
                    inventoryNumber = ""
                    serialNumber = ""
                    selectedStatus = emptySet()
                    selectedCondition = emptySet()
                    selectedCategoryIds = emptySet()
                    selectedLocationIds = emptySet()
                    selectedDepartmentIds = emptySet()
                    selectedOwnerIds = emptySet()
                    ownerSearchQuery = ""
                    selectedSessionId = null
                    incidentOnly = false
                },
            ) {
                Text("Сбросить")
            }
        }

        state.searchError?.let { StatusBanner(it, ChipTone.ERROR) }
    }
    }

    val fallbackItems = filterItemsByStructure(
        items = state.items,
        selectedDepartmentIds = selectedDepartmentIds,
        selectedLocationIds = selectedLocationIds,
        selectedOwnerIds = selectedOwnerIds,
        locations = state.locations,
    ).filter { item ->
        val query = searchText.trim()
        if (query.isBlank()) true else {
            listOf(
                item.inventoryNumber,
                item.title,
                item.serialNumber.orEmpty(),
            ).any { it.contains(query, ignoreCase = true) }
        }
    }
    val resultItems = state.searchResult?.items ?: fallbackItems
    val totalItems = state.searchResult?.total ?: resultItems.size

    if (resultItems.isEmpty()) {
        EmptyState("Оборудование", "Объектов пока нет")
    } else {
        if (showTable) {
            InventoryItemsTableCard(
                state = state,
                items = resultItems,
                onOpen = { component.openItem(it.inventoryItemId) },
                onEdit = { editingItem = it },
                onRefreshQr = {
                    component.refreshItemCode(
                        organizationId = organizationId,
                        inventoryItemId = it.inventoryItemId,
                        codeType = MeshInventoryCodeType.QR,
                    )
                },
                onRefreshBarcode = {
                    component.refreshItemCode(
                        organizationId = organizationId,
                        inventoryItemId = it.inventoryItemId,
                        codeType = MeshInventoryCodeType.BARCODE,
                    )
                },
            )
        } else {
            SectionCard(title = "Оборудование", subtitle = "Всего: $totalItems") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    resultItems.forEach { item ->
                        InventoryItemRow(
                            item = item,
                            onOpen = { component.openItem(item.inventoryItemId) },
                            onEdit = { editingItem = item },
                            onRefreshQr = {
                                component.refreshItemCode(
                                    organizationId = organizationId,
                                    inventoryItemId = item.inventoryItemId,
                                    codeType = MeshInventoryCodeType.QR,
                                )
                            },
                            onRefreshBarcode = {
                                component.refreshItemCode(
                                    organizationId = organizationId,
                                    inventoryItemId = item.inventoryItemId,
                                    codeType = MeshInventoryCodeType.BARCODE,
                                )
                            },
                        )
                    }
                }
            }
        }
        if (showPrintPanel) {
            ItemsPrintCard(
                state = state,
                component = component,
                organizationId = organizationId,
                items = resultItems,
                onFeedback = onPrintFeedback,
            )
        }
    }

    if (showCreatePanel) {
        itemCreateForm(state, component, organizationId)
    }
}

@Composable
private fun InventoryItemRow(
    item: MeshInventoryItem,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onRefreshQr: () -> Unit,
    onRefreshBarcode: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 620.dp
            if (compact) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Инвентарный номер: ${item.inventoryNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    item.responsiblePerson?.let {
                        Text(
                            "МОЛ: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BadgeChip(item.currentStatus.asUiText(), item.currentStatus.asTone())
                    Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("Открыть карточку") }
                    OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Редактировать") }
                    OutlinedButton(onClick = onRefreshQr, modifier = Modifier.fillMaxWidth()) { Text("Обновить QR") }
                    OutlinedButton(onClick = onRefreshBarcode, modifier = Modifier.fillMaxWidth()) { Text("Обновить штрихкод") }
                }
            } else {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "№ ${item.inventoryNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        BadgeChip(item.currentStatus.asUiText(), item.currentStatus.asTone())
                    }
                    FlowRow(
                        maxItemsInEachRow = 2,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.width(280.dp),
                    ) {
                        OutlinedButton(onClick = onOpen) { Text("Карточка") }
                        OutlinedButton(onClick = onEdit) { Text("Правка") }
                        OutlinedButton(onClick = onRefreshQr) { Text("QR") }
                        OutlinedButton(onClick = onRefreshBarcode) { Text("ШК") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InventoryItemsTableCard(
    state: InventoryOverviewState,
    items: List<MeshInventoryItem>,
    onOpen: (MeshInventoryItem) -> Unit,
    onEdit: (MeshInventoryItem) -> Unit,
    onRefreshQr: (MeshInventoryItem) -> Unit,
    onRefreshBarcode: (MeshInventoryItem) -> Unit,
) {
    val locationMap = remember(state.locations) { state.locations.associateBy { it.locationId } }
    val departmentMap = remember(state.departments) { state.departments.associateBy { it.departmentId } }
    val ownerMap = remember(state.owners) { state.owners.associateBy { it.ownerId } }
    val columns = remember {
        listOf(
            "Инв.№" to 120.dp,
            "Название" to 220.dp,
            "Действия" to 200.dp,
            "Статус" to 130.dp,
            "Состояние" to 130.dp,
            "Серийный №" to 140.dp,
            "Подразделение" to 160.dp,
            "Локация" to 170.dp,
            "МОЛы" to 200.dp,
            "QR" to 120.dp,
            "Штрихкод" to 140.dp,
        )
    }
    val tableWidth = columns.fold(0.dp) { acc, (_, width) -> acc + width } + (8.dp * (columns.size - 1)) + 20.dp
    SectionCard(title = "Таблица оборудования", subtitle = "Полный список с действиями по строке") {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = true,
        ) {
            item {
                Column(
                    modifier = Modifier.width(tableWidth),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RegistryHeaderRow(columns = columns, modifier = Modifier.width(tableWidth))
                    items.forEachIndexed { index, item ->
                        Surface(
                            modifier = Modifier.width(tableWidth),
                            shape = MaterialTheme.shapes.medium,
                            color = if (index % 2 == 0) {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RegistryCell(item.inventoryNumber, 120.dp, true)
                                RegistryCell(item.title, 220.dp)
                                Row(
                                    modifier = Modifier.width(200.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(onClick = { onOpen(item) }) {
                                        Icon(Icons.Outlined.OpenInNew, contentDescription = "Открыть карточку")
                                    }
                                    IconButton(onClick = { onEdit(item) }) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Редактировать")
                                    }
                                    IconButton(onClick = { onRefreshQr(item) }) {
                                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Обновить QR")
                                    }
                                    IconButton(onClick = { onRefreshBarcode(item) }) {
                                        Icon(Icons.Outlined.Refresh, contentDescription = "Обновить штрихкод")
                                    }
                                }
                                Row(modifier = Modifier.width(130.dp)) {
                                    BadgeChip(item.currentStatus.asUiText(), item.currentStatus.asTone())
                                }
                                Row(modifier = Modifier.width(130.dp)) {
                                    BadgeChip(
                                        item.condition.asUiText(),
                                        when (item.condition) {
                                            MeshInventoryCondition.NEW,
                                            MeshInventoryCondition.GOOD,
                                            -> ChipTone.SUCCESS
                                            MeshInventoryCondition.FAIR,
                                            MeshInventoryCondition.UNKNOWN,
                                            -> ChipTone.INFO
                                            MeshInventoryCondition.NEEDS_REPAIR,
                                            MeshInventoryCondition.OUT_OF_SERVICE,
                                            -> ChipTone.WARNING
                                        },
                                    )
                                }
                                RegistryCell(item.serialNumber ?: "—", 140.dp)
                                RegistryCell(departmentMap[item.departmentId]?.name ?: item.responsibleDepartment ?: "—", 160.dp)
                                RegistryCell(locationMap[item.locationId]?.name ?: "—", 170.dp)
                                RegistryCell(
                                    item.allOwnerIds().mapNotNull { ownerMap[it]?.name }.ifEmpty {
                                        listOfNotNull(item.responsiblePerson)
                                    }.joinToString(", ").ifBlank { "—" },
                                    200.dp,
                                )
                                RegistryCell(item.qrCode?.shortId(12) ?: "—", 120.dp)
                                RegistryCell(item.barcode?.shortId(12) ?: "—", 140.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemsPrintCard(
    state: InventoryOverviewState,
    component: InventoryComponent,
    organizationId: String,
    items: List<MeshInventoryItem>,
    onFeedback: (String) -> Unit,
) {
    val defaultTemplateId = state.labelTemplates.firstOrNull { it.isDefault }?.templateId
        ?: state.labelTemplates.firstOrNull()?.templateId
    var selectedTemplateId by remember(organizationId, state.labelTemplates.size) { mutableStateOf(defaultTemplateId) }
    val selectedTemplate = state.labelTemplates.firstOrNull { it.templateId == selectedTemplateId }
    var includeBarcode by remember(organizationId, selectedTemplateId) { mutableStateOf(selectedTemplate?.includeBarcode ?: true) }
    var includeQr by remember(organizationId, selectedTemplateId) { mutableStateOf(selectedTemplate?.includeQr ?: true) }
    var selectedFields by remember(organizationId, selectedTemplateId) {
        mutableStateOf(selectedTemplate?.fields?.toSet() ?: emptySet())
    }
    var selectedItems by remember(organizationId, items.size) { mutableStateOf(items.map { it.inventoryItemId }.toSet()) }
    val canPrint = state.permissions.contains(MeshInventoryPermission.EXPORT_REQUEST)
    val orderedFields = MeshInventoryLabelFieldKey.values().filter { it in selectedFields }

    SectionCard(title = "Печать маркировки", subtitle = "Печать из списка объектов") {
        if (state.labelTemplates.isEmpty()) {
            StatusBanner("Нет шаблонов для печати", ChipTone.WARNING)
        } else {
            Text("Шаблон", style = MaterialTheme.typography.labelLarge)
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

        Text("Выбор объектов", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
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
                        organizationId = organizationId,
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
                Text("Печать выбранных")
            }
            OutlinedButton(onClick = { selectedItems = items.map { it.inventoryItemId }.toSet() }) {
                Text("Выбрать все")
            }
        }

        if (state.printTasks.isNotEmpty()) {
            Text("Последние задачи", style = MaterialTheme.typography.labelLarge)
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
                                                    result.onSuccess { onFeedback("PDF отправлен на печать") }
                                                        .onFailure { error -> onFeedback(error.message ?: "Не удалось напечатать PDF") }
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
                                                result.onSuccess { onFeedback("PDF сохранён") }
                                                    .onFailure { error -> onFeedback(error.message ?: "Не удалось сохранить PDF") }
                                            }
                                        },
                                    ) {
                                        Text("Скачать PDF")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            component.sharePdf(path) { result ->
                                                result.onSuccess { onFeedback("PDF отправлен в «Поделиться»") }
                                                    .onFailure { error -> onFeedback(error.message ?: "Не удалось поделиться PDF") }
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

@Composable
private fun itemCreateForm(state: InventoryOverviewState, component: InventoryComponent, organizationId: String) {
    var inventoryNumber by remember { mutableStateOf("") }
    var localNumber by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var manufacturer by remember { mutableStateOf("") }
    var categoryName by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedSubcategoryId by remember { mutableStateOf<String?>(null) }
    var selectedLocationId by remember { mutableStateOf<String?>(null) }
    var selectedOwnerIds by remember { mutableStateOf(setOf<String>()) }
    var departmentId by remember { mutableStateOf<String?>(null) }
    var costCenterId by remember { mutableStateOf<String?>(null) }
    var legalHolderId by remember { mutableStateOf<String?>(null) }
    var supplierId by remember { mutableStateOf<String?>(null) }
    var fundingSourceId by remember { mutableStateOf<String?>(null) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var condition by remember { mutableStateOf(MeshInventoryCondition.UNKNOWN) }
    var showAdvanced by remember { mutableStateOf(false) }

    SectionCard(title = "Новый объект", subtitle = "Создайте карточку оборудования") {
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
        if (state.departments.isNotEmpty()) {
            Text("Подразделение", style = MaterialTheme.typography.labelLarge)
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
        if (state.categories.isNotEmpty()) {
            Text("Категория", style = MaterialTheme.typography.labelLarge)
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
        if (selectedCategoryId != null) {
            val availableSubcategories = state.subcategories.filter { it.categoryId == selectedCategoryId }
            if (availableSubcategories.isNotEmpty()) {
                Text("Подкатегория", style = MaterialTheme.typography.labelLarge)
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
        }
        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Новая категория") },
            singleLine = true,
        )
        if (state.locations.isNotEmpty()) {
            Text("Локация", style = MaterialTheme.typography.labelLarge)
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
        val availableOwners = availableOwnersForSelection(
            owners = molOwners(state.owners),
            selectedDepartmentIds = setOfNotNull(departmentId),
            selectedLocationIds = setOfNotNull(selectedLocationId),
            locations = state.locations,
        )
        if (availableOwners.isNotEmpty()) {
            Text("МОЛы", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableOwners) { owner ->
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
        } else {
            StatusBanner("Для выбранной локации и подразделения пока нет доступных МОЛов.", ChipTone.WARNING)
        }
        StatusBanner("QR и штрихкод генерируются в карточке объекта в разделе «Маркировка».", ChipTone.INFO)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventoryCondition.values()) { value ->
                FilterChip(
                    selected = condition == value,
                    onClick = { condition = value },
                    label = { Text(value.asUiText()) },
                )
            }
        }
        OutlinedButton(onClick = { showAdvanced = !showAdvanced }) {
            Text(if (showAdvanced) "Скрыть расширенные поля" else "Расширенные поля")
        }
        if (showAdvanced) {
            if (state.costCenters.isNotEmpty()) {
                Text("Центр затрат", style = MaterialTheme.typography.labelLarge)
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
                Text("Балансодержатель", style = MaterialTheme.typography.labelLarge)
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
                Text("Поставщик", style = MaterialTheme.typography.labelLarge)
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
                Text("Источник финансирования", style = MaterialTheme.typography.labelLarge)
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
            if (state.tags.isNotEmpty()) {
                Text("Теги", style = MaterialTheme.typography.labelLarge)
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
        }
        Button(
            onClick = {
                component.createItem(
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
                    categoryId = selectedCategoryId,
                    subcategoryId = selectedSubcategoryId,
                    categoryName = categoryName,
                    locationId = selectedLocationId,
                    locationName = null,
                    responsiblePerson = null,
                    responsibleDepartment = null,
                    ownerId = selectedOwnerIds.firstOrNull(),
                    departmentId = departmentId,
                    responsibleOwnerIds = selectedOwnerIds,
                    costCenterId = costCenterId,
                    legalHolderId = legalHolderId,
                    supplierId = supplierId,
                    fundingSourceId = fundingSourceId,
                    tagIds = selectedTags,
                    attributes = emptyList(),
                    qrCode = null,
                    barcode = null,
                )
                inventoryNumber = ""
                localNumber = ""
                title = ""
                description = ""
                serialNumber = ""
                brand = ""
                model = ""
                manufacturer = ""
                categoryName = ""
                selectedCategoryId = null
                selectedSubcategoryId = null
                selectedLocationId = null
                selectedOwnerIds = emptySet()
                departmentId = null
                costCenterId = null
                legalHolderId = null
                supplierId = null
                fundingSourceId = null
                selectedTags = emptySet()
                condition = MeshInventoryCondition.UNKNOWN
            },
            enabled = state.busyAction != InventoryBusyAction.CREATE_ITEM &&
                state.permissions.contains(MeshInventoryPermission.ITEM_CREATE),
        ) {
            Text("Создать")
        }
    }
}

@Composable
private fun sessionsSection(state: InventoryOverviewState, component: InventoryComponent) {
    val organizationId = state.selectedOrganizationId
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val timeZone = TimeZone.currentSystemDefault()
    val today = remember { Clock.System.now().toLocalDateTime(timeZone).date.toString() }
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today) }
    var selectedDepartmentIds by remember { mutableStateOf(setOf<String>()) }
    var selectedLocationIds by remember { mutableStateOf(setOf<String>()) }
    var selectedOwnerIds by remember { mutableStateOf(setOf<String>()) }
    var ownerSearchQuery by remember { mutableStateOf("") }
    var selectedMembers by remember { mutableStateOf(setOf<String>()) }
    var selectedItems by remember { mutableStateOf(setOf<String>()) }
    var requiresPhotoForDiscrepancy by remember { mutableStateOf(true) }
    var showCreateSession by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    val presets = remember {
        listOf(
            SessionTemplatePreset("Квартальная", "Квартальная инвентаризация", "Сверка техники по всем выбранным МОЛам и локациям"),
            SessionTemplatePreset("Плановая", "Плановая инвентаризация", "Плановая сверка подразделений и закреплённого оборудования"),
            SessionTemplatePreset("Выборочная", "Выборочная инвентаризация", "Точечная проверка по конкретным кабинетам, этажам и МОЛам"),
        )
    }
    val commissionMembers = remember(state.members) { state.members.filter { it.isCommissionMember } }
    val availableOwners = availableOwnersForSelection(
        owners = molOwners(state.owners),
        selectedDepartmentIds = selectedDepartmentIds,
        selectedLocationIds = selectedLocationIds,
        locations = state.locations,
    ).filter { owner ->
        ownerSearchQuery.isBlank() || owner.name.contains(ownerSearchQuery, ignoreCase = true)
    }
    val filteredItems = filterItemsByStructure(
        items = state.items,
        selectedDepartmentIds = selectedDepartmentIds,
        selectedLocationIds = selectedLocationIds,
        selectedOwnerIds = selectedOwnerIds,
        locations = state.locations,
    )
    val effectiveItemIds = selectedItems.ifEmpty { filteredItems.map { it.inventoryItemId }.toSet() }
    val effectiveOwnerIds = selectedOwnerIds.ifEmpty {
        filteredItems.flatMap { it.allOwnerIds().toList() }.toSet()
    }

    if (organizationId == null) {
        SectionCard(title = "Инвентаризация", subtitle = "Нет данных для отображения") {
            StatusBanner("Сначала выберите организацию или загрузите демо-данные", ChipTone.WARNING)
            OutlinedButton(onClick = component::seedDemoData) { Text("Загрузить демо-данные") }
        }
        return
    }

    SectionCard(title = "Типовые варианты", subtitle = "Быстрый старт для новой инвентаризации") {
        FlowRow(
            maxItemsInEachRow = 3,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            presets.forEach { preset ->
                FilterChip(
                    selected = title == preset.defaultTitle,
                    onClick = {
                        title = preset.defaultTitle
                        description = preset.defaultDescription
                        showCreateSession = true
                    },
                    label = { Text(preset.label) },
                )
            }
        }
    }

    SectionCard(title = "Дашборд инвентаризации", subtitle = "Комиссия, параметры и прогресс") {
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BadgeChip("Инвентаризаций: ${state.sessions.size}", ChipTone.INFO)
            BadgeChip("Объектов: ${state.items.size}", ChipTone.INFO)
            BadgeChip("МОЛов: ${molOwners(state.owners).size}", ChipTone.INFO)
            BadgeChip("Комиссия: ${commissionMembers.size}", ChipTone.INFO)
            OutlinedButton(onClick = { showCreateSession = !showCreateSession }) {
                Text(if (showCreateSession) "Скрыть форму" else "Создать новую инвентаризацию")
            }
        }
    }

    if (showCreateSession) {
        SectionCard(title = "Создать новую инвентаризацию", subtitle = "Выберите даты, локации, подразделения, МОЛов и комиссию") {
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
                value = startDate,
                onValueChange = { startDate = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Дата начала (ГГГГ-ММ-ДД)") },
                singleLine = true,
            )
            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Дата окончания (ГГГГ-ММ-ДД)") },
                singleLine = true,
            )
            FilterChip(
                selected = requiresPhotoForDiscrepancy,
                onClick = { requiresPhotoForDiscrepancy = !requiresPhotoForDiscrepancy },
                label = { Text("Фото обязательно при расхождениях") },
            )
            if (state.departments.isNotEmpty()) {
                Text("Подразделения", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.departments) { department ->
                        FilterChip(
                            selected = department.departmentId in selectedDepartmentIds,
                            onClick = {
                                selectedDepartmentIds = if (department.departmentId in selectedDepartmentIds) {
                                    selectedDepartmentIds - department.departmentId
                                } else {
                                    selectedDepartmentIds + department.departmentId
                                }
                            },
                            label = { Text(department.name) },
                        )
                    }
                }
            }
            if (state.locations.isNotEmpty()) {
                Text("Локации", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
            if (availableOwners.isNotEmpty()) {
                Text("МОЛы", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = ownerSearchQuery,
                    onValueChange = { ownerSearchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Поиск МОЛа") },
                    singleLine = true,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableOwners) { owner ->
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
                if (effectiveOwnerIds.isNotEmpty()) {
                    Text(
                        "Назначено МОЛов: ${effectiveOwnerIds.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                StatusBanner("Для выбранных подразделений и локаций подходящие МОЛы не найдены.", ChipTone.WARNING)
            }
            if (commissionMembers.isNotEmpty()) {
                Text("Комиссия", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(commissionMembers) { member ->
                        FilterChip(
                            selected = member.peerId in selectedMembers,
                            onClick = {
                                selectedMembers = if (member.peerId in selectedMembers) {
                                    selectedMembers - member.peerId
                                } else {
                                    selectedMembers + member.peerId
                                }
                            },
                            label = { Text(member.displayName.ifBlank { member.peerId.shortId(6) }) },
                        )
                    }
                }
            }
            if (filteredItems.isNotEmpty()) {
                Text("Оборудование по выбранным параметрам", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Будет включено объектов: ${filteredItems.size}. Можно оставить автоматический выбор или отметить нужные вручную.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredItems) { item ->
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
            } else {
                StatusBanner("По выбранным параметрам оборудование не найдено.", ChipTone.WARNING)
            }
            createError?.let { StatusBanner(it, ChipTone.ERROR) }
            Button(
                onClick = {
                    val start = parseInventoryDate(startDate)
                    val end = parseInventoryDate(endDate)
                    when {
                        start == null -> createError = "Укажите корректную дату начала"
                        end == null -> createError = "Укажите корректную дату окончания"
                        end < start -> createError = "Дата окончания не может быть раньше даты начала"
                        effectiveItemIds.isEmpty() -> createError = "Для выбранных параметров нет оборудования"
                        else -> {
                            createError = null
                            component.createSession(
                                organizationId = organizationId,
                                title = title,
                                description = description,
                                periodStart = inventoryDateStart(start, timeZone),
                                periodEnd = inventoryDateEnd(end, timeZone),
                                departmentIds = selectedDepartmentIds,
                                locationIds = selectedLocationIds,
                                ownerIds = effectiveOwnerIds,
                                requiresPhotoForDiscrepancy = requiresPhotoForDiscrepancy,
                                itemIds = effectiveItemIds,
                                memberPeerIds = selectedMembers.ifEmpty { commissionMembers.map { it.peerId }.toSet() },
                            )
                            title = ""
                            description = ""
                            startDate = today
                            endDate = today
                            selectedDepartmentIds = emptySet()
                            selectedLocationIds = emptySet()
                            selectedOwnerIds = emptySet()
                            ownerSearchQuery = ""
                            selectedMembers = emptySet()
                            selectedItems = emptySet()
                            requiresPhotoForDiscrepancy = true
                        }
                    }
                },
                enabled = state.busyAction != InventoryBusyAction.CREATE_SESSION &&
                    state.permissions.contains(MeshInventoryPermission.SESSION_CREATE),
            ) {
                Text("Создать инвентаризацию")
            }
        }
    }

    if (state.sessions.isEmpty()) {
        EmptyState("Инвентаризация", "Нет созданных инвентаризаций")
    } else {
        SectionCard(title = "Список инвентаризаций", subtitle = "Всего: ${state.sessions.size}") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.sessions.forEach { session ->
                    SessionRow(session = session, state = state, onOpen = { component.openSession(session.sessionId) })
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: MeshInventorySession, state: InventoryOverviewState, onOpen: () -> Unit) {
    val ownerNames = session.ownerIds.mapNotNull { ownerId ->
        state.owners.firstOrNull { it.ownerId == ownerId }?.name
    }
    val departmentNames = session.departmentIds.mapNotNull { departmentId ->
        state.departments.firstOrNull { it.departmentId == departmentId }?.name
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 620.dp
            if (compact) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    BadgeChip(session.workflowStatus.asUiText(), session.workflowStatus.asTone())
                    Text(
                        "${session.periodStart.toLocalDateTime(TimeZone.currentSystemDefault()).date} - ${
                            session.periodEnd?.toLocalDateTime(TimeZone.currentSystemDefault())?.date ?: "без даты"
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (ownerNames.isNotEmpty()) {
                        Text(
                            "МОЛы: ${ownerNames.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (departmentNames.isNotEmpty()) {
                        Text(
                            "Подразделения: ${departmentNames.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                        Text("Открыть детали")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        BadgeChip(session.workflowStatus.asUiText(), session.workflowStatus.asTone())
                        Text(
                            "${session.periodStart.toLocalDateTime(TimeZone.currentSystemDefault()).date} - ${
                                session.periodEnd?.toLocalDateTime(TimeZone.currentSystemDefault())?.date ?: "без даты"
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (ownerNames.isNotEmpty()) {
                            Text(
                                "МОЛы: ${ownerNames.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(onClick = onOpen) {
                        Text("Детали")
                    }
                }
            }
        }
    }
}

private data class SessionTemplatePreset(
    val label: String,
    val defaultTitle: String,
    val defaultDescription: String,
)

@Composable
private fun reviewSection(state: InventoryOverviewState, component: InventoryComponent) {
    val reviewItems = filterReviewItems(state.items)
    var pendingReview by remember { mutableStateOf<PendingReview?>(null) }
    val canApprove = state.permissions.contains(MeshInventoryPermission.ITEM_CONFIRM)
    val canReject = state.permissions.contains(MeshInventoryPermission.ITEM_REJECT)

    if (reviewItems.isEmpty()) {
        EmptyState("Согласование", "Нет объектов на согласовании")
    } else {
        SectionCard(title = "На согласовании", subtitle = "Объекты, требующие решения") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                reviewItems.forEach { item ->
                    ReviewItemRow(
                        item = item,
                        onApprove = { if (canApprove) pendingReview = PendingReview(item, MeshInventoryReviewStatus.APPROVED) },
                        onReject = { if (canReject) pendingReview = PendingReview(item, MeshInventoryReviewStatus.REJECTED) },
                        onRequestUpdate = { if (canReject) pendingReview = PendingReview(item, MeshInventoryReviewStatus.REQUIRES_UPDATE) },
                        approveEnabled = canApprove,
                        rejectEnabled = canReject,
                    )
                }
            }
        }
    }

    pendingReview?.let { review ->
        ReviewDialog(
            item = review.item,
            status = review.status,
            onDismiss = { pendingReview = null },
            onConfirm = { comment ->
                component.submitReview(review.item.inventoryItemId, review.status, comment)
                pendingReview = null
            },
        )
    }
}

private data class PendingReview(
    val item: MeshInventoryItem,
    val status: MeshInventoryReviewStatus,
)

@Composable
private fun ReviewItemRow(
    item: MeshInventoryItem,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRequestUpdate: () -> Unit,
    approveEnabled: Boolean,
    rejectEnabled: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("№ ${item.inventoryNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onApprove, enabled = approveEnabled) { Text("Подтвердить") }
                OutlinedButton(onClick = onReject, enabled = rejectEnabled) { Text("Отклонить") }
                OutlinedButton(onClick = onRequestUpdate, enabled = rejectEnabled) { Text("На доработку") }
            }
        }
    }
}

@Composable
private fun ReviewDialog(
    item: MeshInventoryItem,
    status: MeshInventoryReviewStatus,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Решение по объекту") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.title, fontWeight = FontWeight.SemiBold)
                Text(status.asUiText(), color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Комментарий") },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(comment) }) { Text("Сохранить") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun OrganizationInlineEditDialog(
    organization: MeshOrganization,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String?) -> Unit,
) {
    var name by remember(organization.organizationId) { mutableStateOf(organization.name) }
    var description by remember(organization.organizationId) { mutableStateOf(organization.description.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать организацию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
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
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, description.ifBlank { null }) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun ItemInlineEditDialog(
    state: InventoryOverviewState,
    item: MeshInventoryItem,
    onDismiss: () -> Unit,
    onSave: (
        inventoryNumber: String,
        title: String,
        status: MeshInventoryStatus,
        condition: MeshInventoryCondition,
        locationId: String?,
        departmentId: String?,
        ownerId: String?,
        responsibleOwnerIds: Set<String>,
    ) -> Unit,
) {
    var inventoryNumber by remember(item.inventoryItemId) { mutableStateOf(item.inventoryNumber) }
    var title by remember(item.inventoryItemId) { mutableStateOf(item.title) }
    var status by remember(item.inventoryItemId) { mutableStateOf(item.currentStatus) }
    var condition by remember(item.inventoryItemId) { mutableStateOf(item.condition) }
    var selectedLocationId by remember(item.inventoryItemId) { mutableStateOf(item.locationId) }
    var selectedDepartmentId by remember(item.inventoryItemId) { mutableStateOf(item.departmentId) }
    var selectedOwnerIds by remember(item.inventoryItemId) {
        mutableStateOf(item.allOwnerIds().ifEmpty { setOfNotNull(item.ownerId) })
    }
    val availableOwners = availableOwnersForSelection(
        owners = molOwners(state.owners),
        selectedDepartmentIds = setOfNotNull(selectedDepartmentId),
        selectedLocationIds = setOfNotNull(selectedLocationId),
        locations = state.locations,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактирование оборудования") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = inventoryNumber,
                    onValueChange = { inventoryNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Инвентарный номер") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название") },
                    singleLine = true,
                )
                Text("Подразделение", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.departments) { department ->
                        FilterChip(
                            selected = selectedDepartmentId == department.departmentId,
                            onClick = { selectedDepartmentId = department.departmentId },
                            label = { Text(department.name) },
                        )
                    }
                }
                Text("Локация", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.locations) { location ->
                        FilterChip(
                            selected = selectedLocationId == location.locationId,
                            onClick = { selectedLocationId = location.locationId },
                            label = { Text(location.name) },
                        )
                    }
                }
                Text("МОЛы", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableOwners) { owner ->
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
                Text("Статус", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MeshInventoryStatus.values()) { value ->
                        FilterChip(
                            selected = status == value,
                            onClick = { status = value },
                            label = { Text(value.asUiText()) },
                        )
                    }
                }
                Text("Состояние", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MeshInventoryCondition.values()) { value ->
                        FilterChip(
                            selected = condition == value,
                            onClick = { condition = value },
                            label = { Text(value.asUiText()) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        inventoryNumber,
                        title,
                        status,
                        condition,
                        selectedLocationId,
                        selectedDepartmentId,
                        selectedOwnerIds.firstOrNull(),
                        selectedOwnerIds,
                    )
                },
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun LocationInlineEditDialog(
    location: MeshInventoryLocation,
    allLocations: List<MeshInventoryLocation>,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String?, parentLocationId: String?) -> Unit,
) {
    var name by remember(location.locationId) { mutableStateOf(location.name) }
    var description by remember(location.locationId) { mutableStateOf(location.description.orEmpty()) }
    var parentLocationId by remember(location.locationId) { mutableStateOf(location.parentLocationId) }
    val availableParents = allLocations.filter { it.locationId != location.locationId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать локацию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
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
                Text("Родительская локация", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = parentLocationId == null,
                            onClick = { parentLocationId = null },
                            label = { Text("Без родителя") },
                        )
                    }
                    items(availableParents) { parent ->
                        FilterChip(
                            selected = parentLocationId == parent.locationId,
                            onClick = { parentLocationId = parent.locationId },
                            label = { Text(parent.name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, description.ifBlank { null }, parentLocationId) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun RegistryHeaderRow(
    columns: List<Pair<String, androidx.compose.ui.unit.Dp>>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            columns.forEach { (title, width) ->
                Text(
                    text = title,
                    modifier = Modifier.width(width),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun RegistryCell(
    value: String,
    width: androidx.compose.ui.unit.Dp,
    emphasized: Boolean = false,
) {
    Text(
        text = value,
        modifier = Modifier.width(width),
        style = if (emphasized) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
        fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CatalogRegistryTable(
    state: InventoryOverviewState,
    onEditLocation: (MeshInventoryLocation) -> Unit,
) {
    val scroll = rememberScrollState()
    SectionCard(title = "Реестры справочников", subtitle = "Локации, подразделения и центры затрат") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RegistryHeaderRow(
                columns = listOf(
                    "Тип" to 150.dp,
                    "Название" to 220.dp,
                    "Родитель / код" to 220.dp,
                    "ID" to 130.dp,
                    "Действия" to 200.dp,
                ),
            )

            state.locations.forEach { location ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RegistryCell("Локация", 150.dp, true)
                        RegistryCell(location.name, 220.dp)
                        RegistryCell(location.parentLocationId?.shortId(10) ?: "—", 220.dp)
                        RegistryCell(location.locationId.shortId(12), 130.dp)
                        FlowRow(
                            maxItemsInEachRow = 2,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.width(200.dp),
                        ) {
                            OutlinedButton(onClick = { onEditLocation(location) }) {
                                Text("Правка")
                            }
                        }
                    }
                }
            }

            state.departments.forEach { department ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RegistryCell("Подразделение", 150.dp, true)
                        RegistryCell(department.name, 220.dp)
                        RegistryCell(department.parentDepartmentId?.shortId(10) ?: (department.code ?: "—"), 220.dp)
                        RegistryCell(department.departmentId.shortId(12), 130.dp)
                        RegistryCell("Редактирование в форме ниже", 200.dp)
                    }
                }
            }

            state.costCenters.forEach { center ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RegistryCell("Центр затрат", 150.dp, true)
                        RegistryCell(center.name, 220.dp)
                        RegistryCell(center.code, 220.dp)
                        RegistryCell(center.costCenterId.shortId(12), 130.dp)
                        RegistryCell("Редактирование в форме ниже", 200.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun dashboardSection(state: InventoryOverviewState) {
    val dashboard = state.dashboard
    SectionCard(title = "Дашборд", subtitle = "Сводка по инвентаризации") {
        if (dashboard == null) {
            StatusBanner("Снимок дашборда ещё не получен", ChipTone.WARNING)
        } else {
            InfoRow("Всего объектов", dashboard.totalItems.toString())
            InfoRow("Активные инвентаризации", dashboard.sessionsActive.toString())
            InfoRow("Открытые инциденты", dashboard.incidentsOpen.toString())
            if (dashboard.byStatus.isNotEmpty()) {
                Text("Статусы", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dashboard.byStatus.entries.forEach { (status, count) ->
                        BadgeChip("${status.asUiText()}: $count", status.asTone())
                    }
                }
            }
            if (dashboard.byCategoryId.isNotEmpty()) {
                Text("Категории", style = MaterialTheme.typography.labelLarge)
                dashboard.byCategoryId.entries.take(6).forEach { entry ->
                    val name = state.categories.firstOrNull { it.categoryId == entry.key }?.name
                        ?: entry.key.shortId(10)
                    InfoRow(name, entry.value.toString())
                }
            }
            if (dashboard.byLocationId.isNotEmpty()) {
                Text("Локации", style = MaterialTheme.typography.labelLarge)
                dashboard.byLocationId.entries.take(6).forEach { entry ->
                    val name = state.locations.firstOrNull { it.locationId == entry.key }?.name
                        ?: entry.key.shortId(10)
                    InfoRow(name, entry.value.toString())
                }
            }
        }
        if (!state.permissions.contains(MeshInventoryPermission.DASHBOARD_VIEW)) {
            StatusBanner("Нет прав на просмотр дашборда", ChipTone.WARNING)
        }
    }

    SectionCard(title = "Алерты") {
        if (state.alerts.isEmpty()) {
            Text("Алертов нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.alerts.take(8).forEach { alert ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BadgeChip(alert.severity.asUiText(), alert.severity.asTone())
                        Text(alert.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    SectionCard(title = "Напоминания") {
        if (state.reminders.isEmpty()) {
            Text("Напоминаний нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.reminders.take(8).forEach { reminder ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BadgeChip(reminder.status.asUiText(), ChipTone.INFO)
                        Text(reminder.message ?: "Напоминание", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    SectionCard(title = "Правила") {
        if (state.ruleThresholds.isEmpty() && state.deadlineRules.isEmpty()) {
            Text("Правила не настроены", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.ruleThresholds.forEach { rule ->
                InfoRow("Порог ${rule.ruleType.name.lowercase()}", rule.thresholdValue?.toString() ?: "—")
            }
            state.deadlineRules.forEach { rule ->
                InfoRow("Дедлайн ${rule.target.name.lowercase()}", "${rule.daysBefore} дн.")
            }
        }
        if (!state.permissions.contains(MeshInventoryPermission.REMINDER_MANAGE)) {
            StatusBanner("Нет прав на управление правилами", ChipTone.WARNING)
        }
    }
}

@Composable
private fun incidentsSection(state: InventoryOverviewState, component: InventoryComponent) {
    if (state.incidents.isEmpty()) {
        EmptyState("Инциденты", "Инцидентов пока нет")
        return
    }
    SectionCard(title = "Инциденты", subtitle = "Проблемы и замечания комиссии") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.incidents.forEach { incident ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BadgeChip(incident.severity.asUiText(), incident.severity.asTone())
                            BadgeChip(incident.status.asUiText(), ChipTone.INFO)
                        }
                        Text(incident.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(incident.type.name.lowercase(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            incident.inventoryItemId?.let {
                                OutlinedButton(onClick = { component.openItem(it) }) { Text("Карточка") }
                            }
                            incident.sessionId?.let {
                                OutlinedButton(onClick = { component.openSession(it) }) { Text("Инвентаризация") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun catalogsSection(state: InventoryOverviewState, component: InventoryComponent) {
    val organizationId = state.selectedOrganizationId ?: return
    var categoryName by remember { mutableStateOf("") }
    var categoryDescription by remember { mutableStateOf("") }
    var subcategoryName by remember { mutableStateOf("") }
    var subcategoryCategoryId by remember { mutableStateOf<String?>(null) }
    var tagName by remember { mutableStateOf("") }
    var tagColor by remember { mutableStateOf("") }
    var attributeKey by remember { mutableStateOf("") }
    var attributeLabel by remember { mutableStateOf("") }
    var attributeType by remember { mutableStateOf(MeshInventoryAttributeType.TEXT) }
    var attributeRequired by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    var templateCategoryId by remember { mutableStateOf<String?>(null) }
    var fieldKey by remember { mutableStateOf("") }
    var fieldLabel by remember { mutableStateOf("") }
    var fieldType by remember { mutableStateOf(MeshInventoryAttributeType.TEXT) }
    var fieldRequired by remember { mutableStateOf(false) }
    var templateFields by remember { mutableStateOf(listOf<MeshInventoryFieldTemplate>()) }
    var locationName by remember { mutableStateOf("") }
    var locationDescription by remember { mutableStateOf("") }
    var parentLocationId by remember { mutableStateOf<String?>(null) }
    var locationDepartmentId by remember { mutableStateOf<String?>(null) }
    var ownerName by remember { mutableStateOf("") }
    var ownerCode by remember { mutableStateOf("") }
    var ownerDepartmentId by remember { mutableStateOf<String?>(null) }
    var ownerLocationIds by remember { mutableStateOf(setOf<String>()) }
    var ownerSearchQuery by remember { mutableStateOf("") }
    var ownerFilterDepartmentId by remember { mutableStateOf<String?>(null) }
    var ownerFilterLocationId by remember { mutableStateOf<String?>(null) }
    var departmentName by remember { mutableStateOf("") }
    var departmentCode by remember { mutableStateOf("") }
    var parentDepartmentId by remember { mutableStateOf<String?>(null) }
    var departmentLocationIds by remember { mutableStateOf(setOf<String>()) }
    var departmentOwnerIds by remember { mutableStateOf(setOf<String>()) }
    var commissionPeerId by remember { mutableStateOf("") }
    var commissionName by remember { mutableStateOf("") }
    var commissionPosition by remember { mutableStateOf("") }
    var commissionDepartmentId by remember { mutableStateOf<String?>(null) }
    var commissionLocationIds by remember { mutableStateOf(setOf<String>()) }
    var commissionRoleIds by remember { mutableStateOf(setOf<String>()) }
    var costCenterName by remember { mutableStateOf("") }
    var legalHolderName by remember { mutableStateOf("") }
    var supplierName by remember { mutableStateOf("") }
    var fundingSourceName by remember { mutableStateOf("") }
    var ruleType by remember { mutableStateOf(MeshInventoryRuleType.NEXT_INVENTORY_DUE_DAYS) }
    var ruleThreshold by remember { mutableStateOf("") }
    var deadlineTarget by remember { mutableStateOf(MeshInventoryDeadlineTarget.NEXT_INVENTORY) }
    var deadlineDays by remember { mutableStateOf("7") }
    var editingLocation by remember { mutableStateOf<MeshInventoryLocation?>(null) }
    var editingOwnerId by remember { mutableStateOf<String?>(null) }
    var editingDepartmentId by remember { mutableStateOf<String?>(null) }
    var editingCommissionPeerId by remember { mutableStateOf<String?>(null) }
    var locationDashboardId by remember { mutableStateOf<String?>(null) }

    editingLocation?.let { location ->
        LocationInlineEditDialog(
            location = location,
            allLocations = state.locations,
            onDismiss = { editingLocation = null },
            onSave = { name, description, parentLocationId ->
                component.updateLocationInline(
                    locationId = location.locationId,
                    name = name,
                    description = description,
                    parentLocationId = parentLocationId,
                )
                editingLocation = null
            },
        )
    }

    CatalogRegistryTable(
        state = state,
        onEditLocation = { editingLocation = it },
    )

    SectionCard(title = "Категории") {
        if (state.categories.isNotEmpty()) {
            state.categories.forEach { Text(it.name) }
        } else {
            Text("Категорий нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название категории") },
            singleLine = true,
        )
        OutlinedTextField(
            value = categoryDescription,
            onValueChange = { categoryDescription = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Описание") },
        )
        Button(
            onClick = {
                component.createCategory(organizationId, categoryName, categoryDescription)
                categoryName = ""
                categoryDescription = ""
            },
            enabled = state.permissions.contains(MeshInventoryPermission.CATEGORY_MANAGE),
        ) {
            Text("Создать")
        }
    }

    SectionCard(title = "Подкатегории") {
        if (state.subcategories.isNotEmpty()) {
            state.subcategories.forEach { Text(it.name) }
        } else {
            Text("Подкатегорий нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.categories.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories) { category ->
                    FilterChip(
                        selected = subcategoryCategoryId == category.categoryId,
                        onClick = { subcategoryCategoryId = category.categoryId },
                        label = { Text(category.name) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = subcategoryName,
            onValueChange = { subcategoryName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название подкатегории") },
        )
        Button(
            onClick = {
                val categoryId = subcategoryCategoryId ?: return@Button
                component.createSubcategory(organizationId, categoryId, subcategoryName, null)
                subcategoryName = ""
            },
            enabled = subcategoryCategoryId != null && state.permissions.contains(MeshInventoryPermission.CATEGORY_MANAGE),
        ) {
            Text("Создать")
        }
    }

    SectionCard(title = "Теги") {
        if (state.tags.isNotEmpty()) {
            state.tags.forEach { Text(it.name) }
        } else {
            Text("Тегов нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = tagName,
            onValueChange = { tagName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название тега") },
        )
        OutlinedTextField(
            value = tagColor,
            onValueChange = { tagColor = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Цвет (опционально)") },
        )
        Button(
            onClick = {
                component.createTag(organizationId, tagName, tagColor)
                tagName = ""
                tagColor = ""
            },
            enabled = state.permissions.contains(MeshInventoryPermission.TAG_MANAGE),
        ) {
            Text("Создать")
        }
    }

    SectionCard(title = "Атрибуты") {
        if (state.attributeDefinitions.isNotEmpty()) {
            state.attributeDefinitions.forEach { Text(it.label) }
        } else {
            Text("Атрибутов нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = attributeKey,
            onValueChange = { attributeKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ключ") },
        )
        OutlinedTextField(
            value = attributeLabel,
            onValueChange = { attributeLabel = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название") },
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventoryAttributeType.values()) { value ->
                FilterChip(
                    selected = attributeType == value,
                    onClick = { attributeType = value },
                    label = { Text(value.name.lowercase()) },
                )
            }
        }
        FilterChip(
            selected = attributeRequired,
            onClick = { attributeRequired = !attributeRequired },
            label = { Text("Обязательный") },
        )
        Button(
            onClick = {
                component.createAttributeDefinition(
                    organizationId,
                    attributeKey,
                    attributeLabel,
                    attributeType,
                    attributeRequired,
                )
                attributeKey = ""
                attributeLabel = ""
                attributeType = MeshInventoryAttributeType.TEXT
                attributeRequired = false
            },
            enabled = state.permissions.contains(MeshInventoryPermission.ATTRIBUTE_MANAGE),
        ) {
            Text("Создать")
        }
    }

    SectionCard(title = "Шаблоны категорий") {
        if (state.templates.isNotEmpty()) {
            state.templates.forEach { Text(it.name) }
        } else {
            Text("Шаблонов нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.categories.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories) { category ->
                    FilterChip(
                        selected = templateCategoryId == category.categoryId,
                        onClick = { templateCategoryId = category.categoryId },
                        label = { Text(category.name) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = templateName,
            onValueChange = { templateName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название шаблона") },
        )
        OutlinedTextField(
            value = fieldKey,
            onValueChange = { fieldKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ключ поля") },
        )
        OutlinedTextField(
            value = fieldLabel,
            onValueChange = { fieldLabel = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название поля") },
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventoryAttributeType.values()) { value ->
                FilterChip(
                    selected = fieldType == value,
                    onClick = { fieldType = value },
                    label = { Text(value.name.lowercase()) },
                )
            }
        }
        FilterChip(
            selected = fieldRequired,
            onClick = { fieldRequired = !fieldRequired },
            label = { Text("Обязательное поле") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                if (fieldKey.isBlank() || fieldLabel.isBlank()) return@OutlinedButton
                val field = MeshInventoryFieldTemplate(
                    fieldId = "field-${Clock.System.now().toEpochMilliseconds()}-${templateFields.size}",
                    key = fieldKey.trim(),
                    label = fieldLabel.trim(),
                    type = fieldType,
                    required = fieldRequired,
                    order = templateFields.size,
                )
                templateFields = templateFields + field
                fieldKey = ""
                fieldLabel = ""
                fieldType = MeshInventoryAttributeType.TEXT
                fieldRequired = false
            }) {
                Text("Добавить поле")
            }
            if (templateFields.isNotEmpty()) {
                OutlinedButton(onClick = { templateFields = emptyList() }) { Text("Очистить") }
            }
        }
        if (templateFields.isNotEmpty()) {
            Text("Поля: ${templateFields.joinToString { it.label }}", style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = {
                val categoryId = templateCategoryId ?: return@Button
                component.createTemplate(organizationId, categoryId, templateName, templateFields)
                templateName = ""
                templateFields = emptyList()
            },
            enabled = templateCategoryId != null && state.permissions.contains(MeshInventoryPermission.TEMPLATE_MANAGE),
        ) {
            Text("Создать")
        }
    }

    SectionCard(title = "Локации") {
        if (state.locations.isNotEmpty()) {
            state.locations.forEach { Text(it.name) }
        } else {
            Text("Локаций нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.locations.isNotEmpty()) {
            Text("Открыть локацию", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.locations) { location ->
                    FilterChip(
                        selected = locationDashboardId == location.locationId,
                        onClick = {
                            locationDashboardId = if (locationDashboardId == location.locationId) null else location.locationId
                        },
                        label = { Text(location.name) },
                    )
                }
            }
        }
        locationDashboardId?.let { selectedLocationId ->
            val locationItems = filterItemsByStructure(
                items = state.items,
                selectedDepartmentIds = emptySet(),
                selectedLocationIds = setOf(selectedLocationId),
                selectedOwnerIds = emptySet(),
                locations = state.locations,
            )
            val locationItemIds = locationItems.map { it.inventoryItemId }.toSet()
            val locationSessions = state.sessions.filter { session ->
                session.locationIds.any { it in locationAndDescendants(selectedLocationId, state.locations) } ||
                    session.itemIds.any { it in locationItemIds }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        locationPath(selectedLocationId, state.locations) ?: "Локация",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        maxItemsInEachRow = 3,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BadgeChip("Оборудование: ${locationItems.size}", ChipTone.INFO)
                        BadgeChip("Инвентаризации: ${locationSessions.size}", ChipTone.INFO)
                    }
                    Text("Оборудование локации", style = MaterialTheme.typography.labelLarge)
                    if (locationItems.isEmpty()) {
                        Text("За этой локацией пока не закреплено оборудование.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        locationItems.take(8).forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "№ ${item.inventoryNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                OutlinedButton(onClick = { component.openItem(item.inventoryItemId) }) {
                                    Text("Открыть")
                                }
                            }
                        }
                        if (locationItems.size > 8) {
                            Text(
                                "Показаны первые 8 объектов. Полный список доступен в разделе «Оборудование» с фильтром по этой локации.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text("Инвентаризация по локации", style = MaterialTheme.typography.labelLarge)
                    if (locationSessions.isEmpty()) {
                        Text("Инвентаризаций по этой локации пока нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        locationSessions.forEach { session ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(session.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${session.workflowStatus.asUiText()} • ${session.periodStart.toLocalDateTime(TimeZone.currentSystemDefault()).date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                OutlinedButton(onClick = { component.openSession(session.sessionId) }) {
                                    Text("Открыть")
                                }
                            }
                        }
                    }
                }
            }
        }
        if (state.locations.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.locations) { location ->
                    FilterChip(
                        selected = parentLocationId == location.locationId,
                        onClick = { parentLocationId = location.locationId },
                        label = { Text(location.name) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = locationName,
            onValueChange = { locationName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название локации") },
        )
        OutlinedTextField(
            value = locationDescription,
            onValueChange = { locationDescription = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Описание локации") },
        )
        if (state.departments.isNotEmpty()) {
            Text("Связать с подразделением", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.departments) { department ->
                    FilterChip(
                        selected = locationDepartmentId == department.departmentId,
                        onClick = { locationDepartmentId = department.departmentId },
                        label = { Text(department.name) },
                    )
                }
            }
        }
        Button(
            onClick = {
                component.createLocation(
                    organizationId = organizationId,
                    name = locationName,
                    description = locationDescription,
                    parentLocationId = parentLocationId,
                    departmentId = locationDepartmentId,
                )
                locationName = ""
                locationDescription = ""
                locationDepartmentId = null
            },
            enabled = state.permissions.contains(MeshInventoryPermission.LOCATION_MANAGE),
        ) {
            Text("Создать")
        }
    }

    SectionCard(title = "МОЛы") {
        val owners = molOwners(state.owners).filter { owner ->
            val matchesSearch = ownerSearchQuery.isBlank() ||
                owner.name.contains(ownerSearchQuery, ignoreCase = true) ||
                owner.code.orEmpty().contains(ownerSearchQuery, ignoreCase = true)
            val matchesDepartment = ownerFilterDepartmentId == null || owner.departmentId == ownerFilterDepartmentId
            val matchesLocation = ownerFilterLocationId == null || owner.locationIds.any { ownerLocationId ->
                ownerLocationId in locationAndDescendants(ownerFilterLocationId!!, state.locations) ||
                    ownerFilterLocationId in locationAndDescendants(ownerLocationId, state.locations)
            }
            matchesSearch && matchesDepartment && matchesLocation
        }
        OutlinedTextField(
            value = ownerSearchQuery,
            onValueChange = { ownerSearchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск по МОЛам") },
            singleLine = true,
        )
        if (state.departments.isNotEmpty()) {
            Text("Фильтр по подразделению", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.departments) { department ->
                    FilterChip(
                        selected = ownerFilterDepartmentId == department.departmentId,
                        onClick = {
                            ownerFilterDepartmentId = if (ownerFilterDepartmentId == department.departmentId) null else department.departmentId
                        },
                        label = { Text(department.name) },
                    )
                }
            }
        }
        if (state.locations.isNotEmpty()) {
            Text("Фильтр по локации", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.locations) { location ->
                    FilterChip(
                        selected = ownerFilterLocationId == location.locationId,
                        onClick = {
                            ownerFilterLocationId = if (ownerFilterLocationId == location.locationId) null else location.locationId
                        },
                        label = { Text(location.name) },
                    )
                }
            }
        }
        Text(
            "Найдено МОЛов: ${owners.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (owners.isNotEmpty()) {
            if (component.capabilities.prefersWideLayout) {
                val tableWidth = 980.dp
                LazyRow(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Column(
                            modifier = Modifier.width(tableWidth),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RegistryHeaderRow(
                                columns = listOf(
                                    "ФИО / МОЛ" to 240.dp,
                                    "Код" to 120.dp,
                                    "Подразделение" to 200.dp,
                                    "Локации" to 260.dp,
                                    "Действия" to 120.dp,
                                ),
                                modifier = Modifier.width(tableWidth),
                            )
                            owners.forEach { owner ->
                                val departmentName = state.departments.firstOrNull { it.departmentId == owner.departmentId }?.name ?: "—"
                                val locations = owner.locationIds.mapNotNull { locationPath(it, state.locations) }.joinToString().ifBlank { "—" }
                                Surface(
                                    modifier = Modifier.width(tableWidth),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RegistryCell(owner.name, 240.dp, true)
                                        RegistryCell(owner.code ?: "—", 120.dp)
                                        RegistryCell(departmentName, 200.dp)
                                        RegistryCell(locations, 260.dp)
                                        Row(
                                            modifier = Modifier.width(120.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    editingOwnerId = owner.ownerId
                                                    ownerName = owner.name
                                                    ownerCode = owner.code.orEmpty()
                                                    ownerDepartmentId = owner.departmentId
                                                    ownerLocationIds = owner.locationIds
                                                },
                                            ) {
                                                Text("Правка")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                owners.forEach { owner ->
                    val departmentName = state.departments.firstOrNull { it.departmentId == owner.departmentId }?.name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(buildString {
                            append(owner.name)
                            owner.code?.let { append(" ($it)") }
                            departmentName?.let { append(" • $it") }
                        })
                        OutlinedButton(
                            onClick = {
                                editingOwnerId = owner.ownerId
                                ownerName = owner.name
                                ownerCode = owner.code.orEmpty()
                                ownerDepartmentId = owner.departmentId
                                ownerLocationIds = owner.locationIds
                            },
                        ) {
                            Text("Правка")
                        }
                    }
                }
            }
        } else {
            Text("МОЛы по выбранным фильтрам не найдены", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = ownerName,
            onValueChange = { ownerName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ФИО / наименование МОЛа") },
        )
        OutlinedTextField(
            value = ownerCode,
            onValueChange = { ownerCode = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Код / краткое обозначение") },
        )
        if (state.departments.isNotEmpty()) {
            Text("Подразделение МОЛа", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.departments) { department ->
                    FilterChip(
                        selected = ownerDepartmentId == department.departmentId,
                        onClick = { ownerDepartmentId = department.departmentId },
                        label = { Text(department.name) },
                    )
                }
            }
        }
        if (state.locations.isNotEmpty()) {
            Text("Локации МОЛа", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.locations) { location ->
                    FilterChip(
                        selected = location.locationId in ownerLocationIds,
                        onClick = {
                            ownerLocationIds = if (location.locationId in ownerLocationIds) {
                                ownerLocationIds - location.locationId
                            } else {
                                ownerLocationIds + location.locationId
                            }
                        },
                        label = { Text(location.name) },
                    )
                }
            }
        }
        Button(
            onClick = {
                if (editingOwnerId == null) {
                    component.createOwner(
                        organizationId = organizationId,
                        name = ownerName,
                        code = ownerCode,
                        departmentId = ownerDepartmentId,
                        locationIds = ownerLocationIds,
                    )
                } else {
                    component.updateOwner(
                        ownerId = editingOwnerId!!,
                        name = ownerName,
                        code = ownerCode,
                        departmentId = ownerDepartmentId,
                        locationIds = ownerLocationIds,
                        archived = false,
                    )
                }
                editingOwnerId = null
                ownerName = ""
                ownerCode = ""
                ownerDepartmentId = null
                ownerLocationIds = emptySet()
            },
            enabled = state.permissions.contains(MeshInventoryPermission.OWNER_MANAGE),
        ) {
            Text(if (editingOwnerId == null) "Создать" else "Сохранить")
        }
    }

    SectionCard(title = "Подразделения") {
        if (state.departments.isNotEmpty()) {
            state.departments.forEach { department ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(buildString {
                        append(department.name)
                        department.code?.let { append(" ($it)") }
                    })
                    OutlinedButton(
                        onClick = {
                            editingDepartmentId = department.departmentId
                            departmentName = department.name
                            departmentCode = department.code.orEmpty()
                            parentDepartmentId = department.parentDepartmentId
                            departmentLocationIds = department.locationIds
                            departmentOwnerIds = department.ownerIds
                        },
                    ) {
                        Text("Правка")
                    }
                }
            }
        } else {
            Text("Подразделений нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.departments.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.departments) { department ->
                    FilterChip(
                        selected = parentDepartmentId == department.departmentId,
                        onClick = { parentDepartmentId = department.departmentId },
                        label = { Text(department.name) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = departmentName,
            onValueChange = { departmentName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название подразделения") },
        )
        OutlinedTextField(
            value = departmentCode,
            onValueChange = { departmentCode = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Код / краткое обозначение") },
        )
        if (state.locations.isNotEmpty()) {
            Text("Локации подразделения", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.locations) { location ->
                    FilterChip(
                        selected = location.locationId in departmentLocationIds,
                        onClick = {
                            departmentLocationIds = if (location.locationId in departmentLocationIds) {
                                departmentLocationIds - location.locationId
                            } else {
                                departmentLocationIds + location.locationId
                            }
                        },
                        label = { Text(location.name) },
                    )
                }
            }
        }
        if (state.owners.isNotEmpty()) {
            Text("МОЛы подразделения", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(molOwners(state.owners)) { owner ->
                    FilterChip(
                        selected = owner.ownerId in departmentOwnerIds,
                        onClick = {
                            departmentOwnerIds = if (owner.ownerId in departmentOwnerIds) {
                                departmentOwnerIds - owner.ownerId
                            } else {
                                departmentOwnerIds + owner.ownerId
                            }
                        },
                        label = { Text(owner.name) },
                    )
                }
            }
        }
        Button(
            onClick = {
                if (editingDepartmentId == null) {
                    component.createDepartment(
                        organizationId = organizationId,
                        name = departmentName,
                        parentDepartmentId = parentDepartmentId,
                        code = departmentCode,
                        locationIds = departmentLocationIds,
                        ownerIds = departmentOwnerIds,
                    )
                } else {
                    component.updateDepartment(
                        departmentId = editingDepartmentId!!,
                        name = departmentName,
                        parentDepartmentId = parentDepartmentId,
                        code = departmentCode,
                        locationIds = departmentLocationIds,
                        ownerIds = departmentOwnerIds,
                        archived = false,
                    )
                }
                editingDepartmentId = null
                departmentName = ""
                departmentCode = ""
                parentDepartmentId = null
                departmentLocationIds = emptySet()
                departmentOwnerIds = emptySet()
            },
            enabled = state.permissions.contains(MeshInventoryPermission.OWNER_MANAGE),
        ) {
            Text(if (editingDepartmentId == null) "Создать" else "Сохранить")
        }
    }

    SectionCard(title = "Комиссия") {
        val commissionCandidates = remember(state.members, state.peers) {
            buildMap<String, String> {
                state.members.forEach { member ->
                    put(member.peerId, member.displayName.ifBlank { member.peerId.shortId(10) })
                }
                state.peers.forEach { peer ->
                    put(peer.identity.peerId, peer.identity.displayName.ifBlank { peer.identity.peerId.shortId(10) })
                }
            }.entries.sortedBy { it.value.lowercase() }
        }
        val commissionMembers = state.members.filter { it.isCommissionMember }
        if (commissionMembers.isEmpty()) {
            Text("Участников комиссии пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            commissionMembers.forEach { member ->
                val departmentName = state.departments.firstOrNull { it.departmentId == member.departmentId }?.name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(buildString {
                        append(member.displayName.ifBlank { member.peerId.shortId(10) })
                        member.position?.let { append(" • $it") }
                        departmentName?.let { append(" • $it") }
                    })
                    OutlinedButton(
                        onClick = {
                            editingCommissionPeerId = member.peerId
                            commissionPeerId = member.peerId
                            commissionName = member.displayName
                            commissionPosition = member.position.orEmpty()
                            commissionDepartmentId = member.departmentId
                            commissionLocationIds = member.locationIds
                            commissionRoleIds = member.roleIds
                        },
                    ) {
                        Text("Правка")
                    }
                }
            }
        }
        if (commissionCandidates.isNotEmpty()) {
            Text("Участник комиссии", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(commissionCandidates) { candidate ->
                    FilterChip(
                        selected = commissionPeerId == candidate.key,
                        onClick = {
                            commissionPeerId = candidate.key
                            if (commissionName.isBlank()) {
                                commissionName = candidate.value
                            }
                        },
                        label = { Text(candidate.value) },
                    )
                }
            }
        } else {
            StatusBanner("Сначала добавьте участника в организацию или подключите нужный узел.", ChipTone.INFO)
        }
        OutlinedTextField(
            value = commissionName,
            onValueChange = { commissionName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ФИО / имя участника") },
        )
        OutlinedTextField(
            value = commissionPosition,
            onValueChange = { commissionPosition = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Должность") },
        )
        if (state.departments.isNotEmpty()) {
            Text("Подразделение", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.departments) { department ->
                    FilterChip(
                        selected = commissionDepartmentId == department.departmentId,
                        onClick = { commissionDepartmentId = department.departmentId },
                        label = { Text(department.name) },
                    )
                }
            }
        }
        if (state.locations.isNotEmpty()) {
            Text("Локации участника", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.locations) { location ->
                    FilterChip(
                        selected = location.locationId in commissionLocationIds,
                        onClick = {
                            commissionLocationIds = if (location.locationId in commissionLocationIds) {
                                commissionLocationIds - location.locationId
                            } else {
                                commissionLocationIds + location.locationId
                            }
                        },
                        label = { Text(location.name) },
                    )
                }
            }
        }
        if (state.roles.isNotEmpty()) {
            Text("Роли", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.roles) { role ->
                    FilterChip(
                        selected = role.roleId in commissionRoleIds,
                        onClick = {
                            commissionRoleIds = if (role.roleId in commissionRoleIds) {
                                commissionRoleIds - role.roleId
                            } else {
                                commissionRoleIds + role.roleId
                            }
                        },
                        label = { Text(roleDisplayName(role.name)) },
                    )
                }
            }
        }
        Button(
            onClick = {
                if (editingCommissionPeerId == null) {
                    component.addMember(
                        organizationId = organizationId,
                        peerId = commissionPeerId,
                        displayName = commissionName,
                        roleIds = commissionRoleIds,
                        departmentId = commissionDepartmentId,
                        locationIds = commissionLocationIds,
                        position = commissionPosition,
                        isCommissionMember = true,
                    )
                } else {
                    component.updateMember(
                        organizationId = organizationId,
                        peerId = commissionPeerId,
                        displayName = commissionName,
                        roleIds = commissionRoleIds,
                        departmentId = commissionDepartmentId,
                        locationIds = commissionLocationIds,
                        position = commissionPosition,
                        isCommissionMember = true,
                        status = MeshOrganizationMemberStatus.ACTIVE,
                    )
                }
                editingCommissionPeerId = null
                commissionPeerId = ""
                commissionName = ""
                commissionPosition = ""
                commissionDepartmentId = null
                commissionLocationIds = emptySet()
                commissionRoleIds = emptySet()
            },
            enabled = commissionPeerId.isNotBlank() && state.permissions.contains(MeshInventoryPermission.MEMBER_MANAGE),
        ) {
            Text(if (editingCommissionPeerId == null) "Добавить в комиссию" else "Сохранить участника комиссии")
        }
    }

    SectionCard(title = "Центры затрат") {
        if (state.costCenters.isNotEmpty()) {
            state.costCenters.forEach { Text(it.name) }
        } else {
            Text("Центров затрат нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = costCenterName,
            onValueChange = { costCenterName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название центра затрат") },
        )
        Button(
            onClick = {
                component.createCostCenter(organizationId, costCenterName)
                costCenterName = ""
            },
            enabled = state.permissions.contains(MeshInventoryPermission.OWNER_MANAGE),
        ) {
            Text("Создать")
        }
    }

    SectionCard(title = "Балансодержатели") {
        if (state.legalHolders.isNotEmpty()) {
            state.legalHolders.forEach { Text(it.name) }
        } else {
            Text("Балансодержателей нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = legalHolderName,
            onValueChange = { legalHolderName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название балансодержателя") },
        )
        Button(
            onClick = {
                component.createLegalHolder(organizationId, legalHolderName)
                legalHolderName = ""
            },
            enabled = state.permissions.contains(MeshInventoryPermission.OWNER_MANAGE),
        ) {
            Text("Создать")
        }
    }

    SectionCard(title = "Поставщики") {
        if (state.suppliers.isNotEmpty()) {
            state.suppliers.forEach { Text(it.name) }
        } else {
            Text("Поставщиков нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = supplierName,
            onValueChange = { supplierName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название поставщика") },
        )
        Button(
            onClick = {
                component.createSupplier(organizationId, supplierName)
                supplierName = ""
            },
            enabled = state.permissions.contains(MeshInventoryPermission.OWNER_MANAGE),
        ) {
            Text("Создать")
        }
    }

    SectionCard(title = "Источники финансирования") {
        if (state.fundingSources.isNotEmpty()) {
            state.fundingSources.forEach { Text(it.name) }
        } else {
            Text("Источников нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = fundingSourceName,
            onValueChange = { fundingSourceName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название источника") },
        )
        Button(
            onClick = {
                component.createFundingSource(organizationId, fundingSourceName)
                fundingSourceName = ""
            },
            enabled = state.permissions.contains(MeshInventoryPermission.OWNER_MANAGE),
        ) {
            Text("Создать")
        }
    }

    SectionCard(title = "Правила и дедлайны") {
        Text("Пороговые правила", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventoryRuleType.values()) { value ->
                FilterChip(
                    selected = ruleType == value,
                    onClick = { ruleType = value },
                    label = { Text(value.name.lowercase()) },
                )
            }
        }
        OutlinedTextField(
            value = ruleThreshold,
            onValueChange = { ruleThreshold = it.filter { char -> char.isDigit() } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Порог (число)") },
            singleLine = true,
        )
        Button(
            onClick = {
                component.createRuleThreshold(
                    organizationId = organizationId,
                    ruleType = ruleType,
                    thresholdValue = ruleThreshold.toLongOrNull(),
                )
                ruleThreshold = ""
            },
            enabled = state.permissions.contains(MeshInventoryPermission.REMINDER_MANAGE),
        ) {
            Text("Создать правило")
        }
        Text("Дедлайны", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MeshInventoryDeadlineTarget.values()) { value ->
                FilterChip(
                    selected = deadlineTarget == value,
                    onClick = { deadlineTarget = value },
                    label = { Text(value.name.lowercase()) },
                )
            }
        }
        OutlinedTextField(
            value = deadlineDays,
            onValueChange = { deadlineDays = it.filter { char -> char.isDigit() } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Дней до дедлайна") },
            singleLine = true,
        )
        Button(
            onClick = {
                component.createDeadlineRule(
                    organizationId = organizationId,
                    target = deadlineTarget,
                    daysBefore = deadlineDays.toIntOrNull() ?: 0,
                )
                deadlineDays = "7"
            },
            enabled = state.permissions.contains(MeshInventoryPermission.REMINDER_MANAGE),
        ) {
            Text("Создать дедлайн")
        }
    }
}

@Composable
private fun reportsSection(
    state: InventoryOverviewState,
    component: InventoryComponent,
    onFeedback: (String) -> Unit,
) {
    val organizationId = state.selectedOrganizationId
    var selectedSessionId by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(MeshInventoryExportFormat.PDF) }
    var showRequestPanel by remember { mutableStateOf(false) }

    if (organizationId == null) {
        SectionCard(title = "Отчёты", subtitle = "Нет данных для отображения") {
            StatusBanner("Сначала выберите организацию или загрузите демо-данные", ChipTone.WARNING)
            OutlinedButton(onClick = component::seedDemoData) { Text("Загрузить демо-данные") }
        }
        return
    }

    SectionCard(title = "Дашборд отчётов", subtitle = "Экспорт и история выгрузок") {
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BadgeChip("Инвентаризация: ${state.sessions.size}", ChipTone.INFO)
            BadgeChip("Экспортов: ${state.exports.size}", ChipTone.INFO)
            OutlinedButton(onClick = { showRequestPanel = !showRequestPanel }) {
                Text(if (showRequestPanel) "Скрыть запрос" else "Запросить отчёт")
            }
        }
    }

    if (showRequestPanel) {
        SectionCard(title = "Запрос отчёта", subtitle = "Сформировать выгрузку по инвентаризации") {
            if (state.sessions.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.sessions) { session ->
                        FilterChip(
                            selected = session.sessionId == selectedSessionId,
                            onClick = { selectedSessionId = session.sessionId },
                            label = { Text(session.title) },
                        )
                    }
                }
            } else {
                Text("Нет проверок для экспорта", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MeshInventoryExportFormat.values()) { value ->
                    FilterChip(
                        selected = format == value,
                        onClick = { format = value },
                        label = { Text(value.name) },
                    )
                }
            }
            Button(
                onClick = { component.requestExport(organizationId, selectedSessionId, format) },
                enabled = state.busyAction != InventoryBusyAction.REQUEST_EXPORT &&
                    state.permissions.contains(MeshInventoryPermission.EXPORT_REQUEST),
            ) {
                Text("Запросить")
            }
        }
    }

    if (state.exports.isEmpty()) {
        EmptyState("Отчёты", "Экспортов пока нет")
    } else {
        SectionCard(title = "История отчётов", subtitle = "Последние запросы") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.exports.forEach { export ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(export.format.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                export.status.asUiText(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            export.resultDescriptor?.let { descriptor ->
                                InfoRow("Файл", descriptor.fileName)
                                InfoRow("Размер", "${descriptor.sizeBytes / 1024} КБ")
                            } ?: Text(
                                "Файл будет собран локально при скачивании или печати.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (component.capabilities.canOpenFiles) {
                                    OutlinedButton(
                                        onClick = {
                                            component.openExport(export) { result ->
                                                result.fold(
                                                    onSuccess = { onFeedback("Отчёт открыт") },
                                                    onFailure = { error ->
                                                        onFeedback(error.message ?: "Не удалось открыть отчёт")
                                                    },
                                                )
                                            }
                                        },
                                    ) { Text("Открыть") }
                                }
                                OutlinedButton(
                                    onClick = {
                                        component.downloadExport(export) { result ->
                                            result.fold(
                                                onSuccess = { savedPath ->
                                                    onFeedback(
                                                        if (savedPath.isNullOrBlank()) {
                                                            "Сохранение отменено"
                                                        } else {
                                                            "Отчёт сохранён"
                                                        },
                                                    )
                                                },
                                                onFailure = { error ->
                                                    onFeedback(error.message ?: "Файл отчёта пока недоступен")
                                                },
                                            )
                                        }
                                    },
                                    enabled = component.capabilities.canSaveFiles,
                                ) { Text("Скачать") }
                                OutlinedButton(
                                    onClick = {
                                        component.shareExport(export) { result ->
                                            result.fold(
                                                onSuccess = { onFeedback("Отчёт передан в системное меню") },
                                                onFailure = { error ->
                                                    onFeedback(error.message ?: "Не удалось поделиться отчётом")
                                                },
                                            )
                                        }
                                    },
                                    enabled = component.capabilities.canShareFiles,
                                ) { Text("Поделиться") }
                                if (component.capabilities.canPrintFiles && export.format == MeshInventoryExportFormat.PDF) {
                                    OutlinedButton(
                                        onClick = {
                                            component.printExport(export) { result ->
                                                result.fold(
                                                    onSuccess = { onFeedback("Отчёт отправлен на печать") },
                                                    onFailure = { error ->
                                                        onFeedback(error.message ?: "Не удалось распечатать отчёт")
                                                    },
                                                )
                                            }
                                        },
                                    ) { Text("Печать") }
                                }
                            }
                            export.errorMessage?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun roleDisplayName(raw: String): String = when (raw.trim().lowercase()) {
    "organization admin" -> "Администратор организации"
    "reviewer" -> "Аудитор"
    else -> raw
}

private fun permissionRu(permission: MeshInventoryPermission): String = when (permission) {
    MeshInventoryPermission.ORGANIZATION_MANAGE -> "упр. организацией"
    MeshInventoryPermission.MEMBER_MANAGE -> "упр. сотрудниками"
    MeshInventoryPermission.ROLE_MANAGE -> "упр. ролями"
    MeshInventoryPermission.CATEGORY_MANAGE -> "упр. категориями"
    MeshInventoryPermission.LOCATION_MANAGE -> "упр. локациями"
    MeshInventoryPermission.TAG_MANAGE -> "упр. тегами"
    MeshInventoryPermission.ATTRIBUTE_MANAGE -> "упр. атрибутами"
    MeshInventoryPermission.TEMPLATE_MANAGE -> "упр. шаблонами"
    MeshInventoryPermission.OWNER_MANAGE -> "упр. владельцами"
    MeshInventoryPermission.ITEM_CREATE -> "создание объектов"
    MeshInventoryPermission.ITEM_EDIT -> "редактирование объектов"
    MeshInventoryPermission.ITEM_VIEW -> "просмотр объектов"
    MeshInventoryPermission.ITEM_SUBMIT_REVIEW -> "отправка на согласование"
    MeshInventoryPermission.ITEM_CONFIRM -> "подтв. объектов"
    MeshInventoryPermission.ITEM_REJECT -> "отклонение объектов"
    MeshInventoryPermission.ITEM_ARCHIVE -> "архивация объектов"
    MeshInventoryPermission.ITEM_COMMENT -> "комментарии"
    MeshInventoryPermission.ITEM_ATTACH -> "вложения"
    MeshInventoryPermission.ITEM_UPLOAD_PHOTO -> "загрузка фото"
    MeshInventoryPermission.ITEM_INCIDENT -> "создание инцидентов"
    MeshInventoryPermission.SESSION_CREATE -> "создание проверок"
    MeshInventoryPermission.SESSION_UPDATE -> "редактирование проверок"
    MeshInventoryPermission.SESSION_ADD_ITEM -> "добавление объектов"
    MeshInventoryPermission.SESSION_ADD_MEMBER -> "добавление участников"
    MeshInventoryPermission.SESSION_CLOSE -> "закрытие проверок"
    MeshInventoryPermission.INCIDENT_VIEW -> "просмотр инцидентов"
    MeshInventoryPermission.INCIDENT_CREATE -> "создание инцидентов"
    MeshInventoryPermission.INCIDENT_REVIEW -> "рассмотрение инцидентов"
    MeshInventoryPermission.INCIDENT_RESOLVE -> "закрытие инцидентов"
    MeshInventoryPermission.REMINDER_MANAGE -> "упр. напоминаниями"
    MeshInventoryPermission.DASHBOARD_VIEW -> "просмотр дашборда"
    MeshInventoryPermission.SEARCH -> "поиск"
    MeshInventoryPermission.EXPORT_REQUEST -> "запрос экспорта"
    MeshInventoryPermission.EXPORT_MANAGE -> "упр. экспортами"
    MeshInventoryPermission.EXPORT_VIEW -> "просмотр экспортов"
    MeshInventoryPermission.AUDIT_VIEW -> "просмотр аудита"
}
