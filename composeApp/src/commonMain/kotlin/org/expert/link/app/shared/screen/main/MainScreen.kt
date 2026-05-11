package org.expert.link.app.shared.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.expert.link.app.shared.presentation.HomeState
import org.expert.link.app.shared.presentation.HybridUiState
import org.expert.link.app.shared.presentation.InventoryOverviewState
import org.expert.link.app.shared.presentation.InventorySection
import org.expert.link.app.shared.presentation.NodeSessionState
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.ui.components.BadgeChip
import org.expert.link.app.shared.ui.components.ChipTone
import org.expert.link.app.shared.ui.components.InfoRow
import org.expert.link.app.shared.ui.components.SectionCard
import org.expert.link.app.shared.ui.components.StatusBanner
import org.expert.link.app.shared.ui.components.asUiText
import org.expert.link.app.shared.ui.screens.ChatsScreen
import org.expert.link.app.shared.ui.screens.ContactsScreen
import org.expert.link.app.shared.ui.screens.DiagnosticsScreen
import org.expert.link.app.shared.ui.screens.LoadingScreen
import org.expert.link.app.shared.ui.screens.NearbyScreen
import org.expert.link.app.shared.ui.screens.PairingScreen
import org.expert.link.app.shared.ui.screens.TransfersScreen
import org.expert.link.app.shared.ui.screens.CallScreen
import org.expert.link.app.shared.ui.screens.SettingsScreen
import org.expert.link.app.shared.screen.inventory.InventoryScreen
import org.expert.link.app.shared.screen.profile.ProfileScreen
import org.expert.link.app.shared.ui.components.shortId
import org.expert.link.mesh.contract.model.MeshCentralConnectivityMode

private data class MainDestination(
    val section: MainSection,
    val title: String,
    val icon: @Composable () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(component: MainComponent) {
    val state by component.state.collectAsState()
    val sessionState by component.sessionState.collectAsState()
    val hybridState by component.hybridStore.state.collectAsState()
    val inventoryState by component.inventoryComponent.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(sessionState.status) {
        if (sessionState.status == NodeRuntimeStatus.RUNNING) {
            component.hybridStore.refresh()
        }
    }

    val destinations = remember {
        listOf(
            MainDestination(MainSection.HOME, "Главная") { Icon(Icons.Outlined.Home, contentDescription = null) },
            MainDestination(MainSection.ORGANIZATIONS, "Организации") {
                Icon(Icons.Outlined.Business, contentDescription = null)
            },
            MainDestination(MainSection.EQUIPMENT, "Оборудование") { Icon(Icons.Outlined.Build, contentDescription = null) },
            MainDestination(MainSection.SESSIONS, "Учёт") { Icon(Icons.Outlined.DateRange, contentDescription = null) },
            MainDestination(MainSection.REPORTS, "Отчёты") { Icon(Icons.Outlined.Description, contentDescription = null) },
            MainDestination(MainSection.CHATS, "Чаты") {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null)
            },
            MainDestination(MainSection.TRANSFERS, "Передачи") {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = null)
            },
            MainDestination(MainSection.DIAGNOSTICS, "Диагностика") {
                Icon(Icons.Outlined.BugReport, contentDescription = null)
            },
            MainDestination(MainSection.PROFILE, "Профиль") { Icon(Icons.Outlined.Person, contentDescription = null) },
            MainDestination(MainSection.SETTINGS, "Настройки") { Icon(Icons.Outlined.Settings, contentDescription = null) },
        )
    }
    val mobileDestinations = remember {
        listOf(
            MainDestination(MainSection.HOME, "Главная") { Icon(Icons.Outlined.Home, contentDescription = null) },
            MainDestination(MainSection.EQUIPMENT, "Объекты") { Icon(Icons.Outlined.Build, contentDescription = null) },
            MainDestination(MainSection.SESSIONS, "Учёт") { Icon(Icons.Outlined.DateRange, contentDescription = null) },
            MainDestination(MainSection.CHATS, "Чаты") { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 960.dp
        val selectedDestination = destinations.firstOrNull { it.section == state.selectedSection } ?: destinations.first()
        if (compact) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(selectedDestination.title)
                                Text(
                                    text = inventoryState.selectedOrganizationId?.let { selected ->
                                        inventoryState.organizations.firstOrNull { it.organizationId == selected }?.name
                                    } ?: "Работа без выбранной организации",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        actions = {
                            SyncStatusIndicator(hybridState)
                            IconButton(onClick = { component.selectSection(MainSection.PROFILE) }) {
                                Icon(Icons.Outlined.Person, contentDescription = "Профиль")
                            }
                        },
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        mobileDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = state.selectedSection == destination.section,
                                onClick = { component.selectSection(destination.section) },
                                icon = destination.icon,
                                label = { Text(destination.title) },
                            )
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = component::openScanner,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Сканировать")
                    }
                },
                floatingActionButtonPosition = FabPosition.Center,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                containerColor = Color.Transparent,
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(bottom = 80.dp),
                ) {
                    MainSectionContent(
                        component = component,
                        selectedSection = state.selectedSection,
                        sessionState = sessionState,
                        onShowMessage = ::showMessage,
                        embeddedInShell = true,
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        tonalElevation = 2.dp,
                        shadowElevation = 6.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "EL",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Expert Link",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            NavigationRail(containerColor = Color.Transparent) {
                                destinations.forEach { destination ->
                                    NavigationRailItem(
                                        selected = state.selectedSection == destination.section,
                                        onClick = { component.selectSection(destination.section) },
                                        icon = destination.icon,
                                        label = { Text(destination.title) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        ),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            SyncStatusIndicator(hybridState)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    ) {
                        MainSectionContent(
                            component = component,
                            selectedSection = state.selectedSection,
                            sessionState = sessionState,
                            onShowMessage = ::showMessage,
                            embeddedInShell = false,
                        )
                    }
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SyncStatusIndicator(hybridState: HybridUiState) {
    val connectivityMode = hybridState.hybridState?.connectivityMode
    val (icon, tint, description) = when {
        hybridState.loading || connectivityMode == MeshCentralConnectivityMode.RECONNECTING -> {
            Triple(Icons.Outlined.Sync, MaterialTheme.colorScheme.tertiary, "Синхронизация")
        }

        connectivityMode == MeshCentralConnectivityMode.CENTRAL_AVAILABLE -> {
            Triple(Icons.Outlined.CloudDone, MaterialTheme.colorScheme.primary, "Central доступен")
        }

        connectivityMode == MeshCentralConnectivityMode.CONFLICT_REVIEW_REQUIRED -> {
            Triple(Icons.Outlined.WarningAmber, MaterialTheme.colorScheme.error, "Нужен разбор конфликта")
        }

        connectivityMode == MeshCentralConnectivityMode.CENTRAL_DEGRADED -> {
            Triple(Icons.Outlined.WarningAmber, MaterialTheme.colorScheme.tertiary, "Central отвечает нестабильно")
        }

        else -> Triple(Icons.Outlined.CloudOff, MaterialTheme.colorScheme.onSurfaceVariant, "Оффлайн")
    }

    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
            )
        }
    }
}

@Composable
private fun MainSectionContent(
    component: MainComponent,
    selectedSection: MainSection,
    sessionState: NodeSessionState,
    onShowMessage: (String) -> Unit,
    embeddedInShell: Boolean,
) {
    val inventorySection = selectedSection.toInventorySection()
    when (selectedSection) {
        MainSection.HOME -> MainHomeScreen(
            component = component,
            sessionState = sessionState,
            onNavigate = component::selectSection,
            onSelectOrganization = component.inventoryComponent::selectOrganization,
            showTopBar = !embeddedInShell,
        )
        MainSection.ORGANIZATIONS,
        MainSection.EQUIPMENT,
        MainSection.SESSIONS,
        MainSection.REPORTS,
        -> InventoryScreen(
            component = component.inventoryComponent,
            showBack = false,
            showTopBar = !embeddedInShell,
            showSectionTabs = false,
            showContextPanels = !embeddedInShell,
            forcedSection = inventorySection,
        )
        MainSection.CHATS -> ChatsHubScreen(
            component = component,
            onShowMessage = onShowMessage,
            showTopBar = !embeddedInShell,
        )
        MainSection.TRANSFERS -> TransfersScreen(
            store = component.transfersStore,
            services = component.platformServices,
            onShowMessage = onShowMessage,
        )
        MainSection.DIAGNOSTICS -> DiagnosticsScreen(component.diagnosticsStore)
        MainSection.PROFILE -> ProfileScreen(
            component = component.profileComponent,
            showBack = false,
            showTopBar = !embeddedInShell,
            showTabs = false,
        )
        MainSection.SETTINGS -> SettingsScreen(
            sessionState = sessionState,
            store = component.settingsStore,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MainHomeScreen(
    component: MainComponent,
    sessionState: NodeSessionState,
    onNavigate: (MainSection) -> Unit,
    onSelectOrganization: (String?) -> Unit,
    showTopBar: Boolean = true,
) {
    val inventoryState by component.inventoryComponent.state.collectAsState()
    val hybridState by component.hybridStore.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(sessionState.status) {
        if (sessionState.status == NodeRuntimeStatus.RUNNING) {
            component.inventoryComponent.refreshNow()
            component.hybridStore.refresh()
        }
    }

    if (sessionState.status != NodeRuntimeStatus.RUNNING) {
        LoadingScreen(
            sessionState = sessionState,
            onRetry = { component.inventoryComponent.refreshNow() },
        )
        return
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(title = { Text("Главная") })
            }
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            val wide = maxWidth >= 1080.dp
            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        item {
                            HomeHeroCard(
                                inventoryState = inventoryState,
                                hybridState = hybridState,
                                onNavigate = onNavigate,
                                onSelectOrganization = onSelectOrganization,
                            )
                        }
                        item { InventorySummaryCard(inventoryState) }
                        item {
                            QuickActionsCard(
                                onNavigate = onNavigate,
                                onScanner = component::openScanner,
                                largeCards = false,
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        item { InventoryHighlightsCard(inventoryState, onNavigate) }
                        item {
                            HybridCard(
                                hybridState = hybridState,
                                onSync = { scope.launch { component.hybridStore.syncNow() } },
                                onCentralAccess = { onNavigate(MainSection.ORGANIZATIONS) },
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        HomeHeroCard(
                            inventoryState = inventoryState,
                            hybridState = hybridState,
                            onNavigate = onNavigate,
                            onSelectOrganization = onSelectOrganization,
                        )
                    }
                    item {
                        QuickActionsCard(
                            onNavigate = onNavigate,
                            onScanner = component::openScanner,
                            largeCards = true,
                        )
                    }
                    item { InventoryHighlightsCard(inventoryState, onNavigate) }
                    item { InventorySummaryCard(inventoryState) }
                    item {
                        HybridCard(
                            hybridState = hybridState,
                            onSync = { scope.launch { component.hybridStore.syncNow() } },
                            onCentralAccess = { onNavigate(MainSection.ORGANIZATIONS) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHeroCard(
    inventoryState: InventoryOverviewState,
    hybridState: HybridUiState,
    onNavigate: (MainSection) -> Unit,
    onSelectOrganization: (String?) -> Unit,
) {
    val organization = inventoryState.selectedOrganizationId?.let { selected ->
        inventoryState.organizations.firstOrNull { it.organizationId == selected }
    }
    SectionCard(
        title = "Рабочий центр",
        subtitle = "Оперативная картина по оборудованию, учёту и синхронизации",
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = organization?.name ?: "Выберите организацию",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = if (organization == null) {
                        "Подключите демо-данные или выберите организацию, чтобы открыть рабочие сценарии."
                    } else {
                        "В центре внимания: ${inventoryState.items.size} объектов, ${inventoryState.sessions.size} проверок учёта и ${inventoryState.incidents.size} инцидентов."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                )
            }
        }
        if (inventoryState.organizations.isNotEmpty()) {
            Text("Организация", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(inventoryState.organizations) { candidate ->
                    FilterChip(
                        selected = candidate.organizationId == inventoryState.selectedOrganizationId,
                        onClick = { onSelectOrganization(candidate.organizationId) },
                        label = {
                            Text(
                                text = candidate.name,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BadgeChip("Объектов: ${inventoryState.items.size}", ChipTone.INFO)
            BadgeChip("Учёт: ${inventoryState.sessions.size}", ChipTone.INFO)
            BadgeChip("Инцидентов: ${inventoryState.incidents.size}", ChipTone.WARNING)
            BadgeChip(
                text = when (hybridState.hybridState?.connectivityMode) {
                    MeshCentralConnectivityMode.CENTRAL_AVAILABLE -> "Central online"
                    MeshCentralConnectivityMode.RECONNECTING -> "Идёт синхронизация"
                    MeshCentralConnectivityMode.CONFLICT_REVIEW_REQUIRED -> "Есть конфликт"
                    else -> "Mesh/offline"
                },
                tone = when (hybridState.hybridState?.connectivityMode) {
                    MeshCentralConnectivityMode.CENTRAL_AVAILABLE -> ChipTone.SUCCESS
                    MeshCentralConnectivityMode.CONFLICT_REVIEW_REQUIRED -> ChipTone.ERROR
                    MeshCentralConnectivityMode.RECONNECTING -> ChipTone.WARNING
                    else -> ChipTone.INFO
                },
            )
        }
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(onClick = { onNavigate(MainSection.EQUIPMENT) }, modifier = Modifier.fillMaxWidth()) {
                Text("Открыть оборудование")
            }
            OutlinedButton(onClick = { onNavigate(MainSection.SESSIONS) }, modifier = Modifier.fillMaxWidth()) {
                Text("Открыть учёт")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NodeStatusCard(sessionState: NodeSessionState) {
    SectionCard(title = "Узел") {
        InfoRow(
            "Статус",
            when (sessionState.status) {
                NodeRuntimeStatus.STOPPED -> "Остановлен"
                NodeRuntimeStatus.STARTING -> "Запускается"
                NodeRuntimeStatus.RUNNING -> "Работает"
                NodeRuntimeStatus.ERROR -> "Ошибка"
            },
        )
        sessionState.profile?.let { InfoRow("Имя", it.displayName.ifBlank { "Без имени" }) }
        sessionState.profile?.let { InfoRow("Peer ID", it.peerId.shortId(12)) }
        sessionState.endpoint?.let { InfoRow("Адрес", "${it.host}:${it.port}") }
        InfoRow("Режим", sessionState.transportHint)
        sessionState.errorMessage?.let { StatusBanner(it, ChipTone.ERROR) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(homeState: HomeState) {
    SectionCard(title = "Сводка") {
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BadgeChip("Контакты: ${homeState.peersCount}", ChipTone.INFO)
            BadgeChip("Чаты: ${homeState.conversationsCount}", ChipTone.INFO)
            BadgeChip("Передачи: ${homeState.transfersCount}", ChipTone.INFO)
            BadgeChip("Звонки: ${homeState.callsCount}", ChipTone.INFO)
            homeState.relayStatus?.let {
                BadgeChip(
                    text = if (it.enabled) "Relay готов" else "Relay выключен",
                    tone = if (it.enabled) ChipTone.SUCCESS else ChipTone.WARNING,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HybridCard(
    hybridState: HybridUiState,
    onSync: () -> Unit,
    onCentralAccess: () -> Unit,
) {
    SectionCard(title = "Hybrid режим") {
        val hybrid = hybridState.hybridState
        if (hybrid == null) {
            StatusBanner("Central backend не настроен или узел ещё не обновил статус", ChipTone.WARNING)
            return@SectionCard
        }

        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BadgeChip("Режим: ${hybrid.connectivityMode.asUiText()}", ChipTone.INFO)
            BadgeChip(
                if (hybrid.authenticated) "Central: авторизован" else "Central: offline cache",
                if (hybrid.authenticated) ChipTone.SUCCESS else ChipTone.WARNING,
            )
            BadgeChip("Очередь: ${hybrid.pendingChanges}", ChipTone.INFO)
            BadgeChip(
                "Конфликты: ${hybrid.conflicts}",
                if (hybrid.conflicts > 0) ChipTone.WARNING else ChipTone.SUCCESS,
            )
        }
        hybridState.syncStatus?.let {
            InfoRow("Последний upload seq", it.lastUploadedSequence.toString())
            InfoRow("Последний pull cursor", it.lastPulledCursor.toString())
        }
        hybrid.lastError?.let { StatusBanner(it, ChipTone.ERROR) }
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(onClick = onSync) {
                Text(if (hybridState.loading) "Синхронизация..." else "Синхронизировать")
            }
            OutlinedButton(onClick = onCentralAccess) {
                Text("Central доступ")
            }
        }
    }
}

@Composable
private fun InventorySummaryCard(inventoryState: InventoryOverviewState) {
    val organization = inventoryState.selectedOrganizationId?.let { selected ->
        inventoryState.organizations.firstOrNull { it.organizationId == selected }
    }
    val dashboard = inventoryState.dashboard
    SectionCard(title = "Сводка по оборудованию", subtitle = "Статусы и проверка по активной организации") {
        if (organization == null) {
            StatusBanner("Организация не выбрана", ChipTone.WARNING)
        } else {
            InfoRow("Организация", organization.name)
            InfoRow("Объектов", inventoryState.items.size.toString())
            InfoRow("Проверок", inventoryState.sessions.size.toString())
            InfoRow("На проверке", inventoryState.reviewItems.size.toString())
            InfoRow("Инцидентов", inventoryState.incidents.size.toString())
        }
        if (dashboard != null) {
            InfoRow("Активных проверок", dashboard.sessionsActive.toString())
            InfoRow("Открытых инцидентов", dashboard.incidentsOpen.toString())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionsCard(
    onNavigate: (MainSection) -> Unit,
    onScanner: () -> Unit,
    largeCards: Boolean,
) {
    val actions = remember {
        listOf(
            DashboardActionModel("Оборудование", "Карточки и поиск", Icons.Outlined.Build, true) { onNavigate(MainSection.EQUIPMENT) },
            DashboardActionModel("Учёт", "Комиссия и ход", Icons.Outlined.DateRange, false) { onNavigate(MainSection.SESSIONS) },
            DashboardActionModel("Организация", "Реквизиты и доступ", Icons.Outlined.Business, false) { onNavigate(MainSection.ORGANIZATIONS) },
            DashboardActionModel("Отчёты", "Экспорт и выгрузки", Icons.Outlined.Description, false) { onNavigate(MainSection.REPORTS) },
            DashboardActionModel("Чаты", "Обсуждения и треды", Icons.Outlined.ChatBubbleOutline, false) { onNavigate(MainSection.CHATS) },
            DashboardActionModel("Сканировать", "QR и штрихкод", Icons.Outlined.QrCodeScanner, true) { onScanner() },
            DashboardActionModel("Передачи", "Файлы и статус", Icons.Outlined.SwapHoriz, false) { onNavigate(MainSection.TRANSFERS) },
            DashboardActionModel("Диагностика", "Сеть и события", Icons.Outlined.BugReport, false) { onNavigate(MainSection.DIAGNOSTICS) },
            DashboardActionModel("Настройки", "Узел и режимы", Icons.Outlined.Settings, false) { onNavigate(MainSection.SETTINGS) },
        )
    }
    val cardHeight = if (largeCards) 156.dp else 132.dp
    val rows = (actions.size + 1) / 2
    val gridHeight = (cardHeight * rows) + (12.dp * (rows - 1))
    SectionCard(title = "Быстрые действия", subtitle = "Основные сценарии без лишних уровней меню") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
        ) {
            items(actions) { action ->
                DashboardActionCard(
                    title = action.title,
                    subtitle = action.subtitle,
                    icon = action.icon,
                    primary = action.primary,
                    onClick = action.onClick,
                    largeCards = largeCards,
                )
            }
        }
    }
}

@Composable
private fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    largeCards: Boolean,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (largeCards) 156.dp else 132.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (primary) 8.dp else 3.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = if (largeCards) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Surface(
                modifier = Modifier.size(if (largeCards) 48.dp else 42.dp),
                color = if (primary) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                },
                shape = MaterialTheme.shapes.large,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (primary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = if (largeCards) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 1,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = if (largeCards) TextAlign.Center else TextAlign.Start,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 1,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = if (largeCards) TextAlign.Center else TextAlign.Start,
                )
            }
            Text(
                text = "Открыть",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private data class DashboardActionModel(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val primary: Boolean,
    val onClick: () -> Unit,
)

private enum class ChatsHubTab(val title: String) {
    CHATS("Чаты"),
    CONTACTS("Контакты"),
    NEARBY("Узлы рядом"),
    PAIRING("Сопряжение"),
    CALLS("Звонки"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsHubScreen(
    component: MainComponent,
    onShowMessage: (String) -> Unit,
    showTopBar: Boolean = true,
) {
    var tab by remember { mutableStateOf(ChatsHubTab.CHATS) }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Коммуникации") },
                    actions = {
                        TextButton(onClick = component::openChatList) {
                            Text("Список")
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ChatsHubTab.values()) { value ->
                    FilterChip(
                        selected = tab == value,
                        onClick = { tab = value },
                        label = { Text(value.title) },
                    )
                }
            }

            when (tab) {
                ChatsHubTab.CHATS -> ChatsScreen(
                    store = component.chatsStore,
                    onOpenConversation = { conversationId, peerId ->
                        component.openConversation(conversationId, peerId.ifBlank { null })
                    },
                    onOpenGroup = { chatId ->
                        component.openConversation(chatId, null, "Группа")
                    },
                    onOpenContacts = { tab = ChatsHubTab.CONTACTS },
                )
                ChatsHubTab.CONTACTS -> ContactsScreen(
                    store = component.contactsStore,
                    onOpenChat = component::openChatForPeer,
                    onShowMessage = onShowMessage,
                )
                ChatsHubTab.NEARBY -> NearbyScreen(
                    store = component.nearbyStore,
                    onPairing = { tab = ChatsHubTab.PAIRING },
                    onOpenChat = component::openChatForPeer,
                )
                ChatsHubTab.PAIRING -> PairingScreen(
                    store = component.pairingStore,
                    services = component.platformServices,
                    onShowMessage = onShowMessage,
                )
                ChatsHubTab.CALLS -> CallScreen(component.callsStore)
            }
        }
    }
}

private fun MainSection.toInventorySection(): InventorySection? = when (this) {
    MainSection.ORGANIZATIONS -> InventorySection.ORGANIZATIONS
    MainSection.EQUIPMENT -> InventorySection.ITEMS
    MainSection.SESSIONS -> InventorySection.SESSIONS
    MainSection.REPORTS -> InventorySection.REPORTS
    else -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InventoryHighlightsCard(
    inventoryState: InventoryOverviewState,
    onNavigate: (MainSection) -> Unit,
) {
    SectionCard(title = "Карточки оборудования", subtitle = "Быстрый вход по ключевым блокам") {
        val dashboard = inventoryState.dashboard
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MetricTileCard("Объекты", inventoryState.items.size.toString(), onClick = { onNavigate(MainSection.EQUIPMENT) })
            MetricTileCard("Учёт", inventoryState.sessions.size.toString(), onClick = { onNavigate(MainSection.SESSIONS) })
            MetricTileCard("Инциденты", inventoryState.incidents.size.toString(), onClick = { onNavigate(MainSection.SESSIONS) })
            MetricTileCard("Экспорты", inventoryState.exports.size.toString(), onClick = { onNavigate(MainSection.REPORTS) })
            dashboard?.let {
                MetricTileCard("Активные проверки", it.sessionsActive.toString(), onClick = { onNavigate(MainSection.SESSIONS) })
                MetricTileCard("Открытые", it.incidentsOpen.toString(), onClick = { onNavigate(MainSection.EQUIPMENT) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowScope.MetricTileCard(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .height(126.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
