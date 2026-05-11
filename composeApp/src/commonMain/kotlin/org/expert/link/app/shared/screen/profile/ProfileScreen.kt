package org.expert.link.app.shared.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackHandler
import kotlinx.coroutines.launch
import org.expert.link.app.shared.screen.components.initials
import org.expert.link.app.shared.ui.components.asUiText
import org.expert.link.app.shared.ui.screens.ChatsScreen
import org.expert.link.app.shared.ui.screens.ContactsScreen
import org.expert.link.app.shared.ui.screens.DiagnosticsScreen
import org.expert.link.app.shared.ui.screens.HomeScreen
import org.expert.link.app.shared.ui.screens.NearbyScreen
import org.expert.link.app.shared.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    component: ProfileComponent,
    showBack: Boolean = true,
    showTopBar: Boolean = true,
    showTabs: Boolean = true,
) {
    val state by component.state.collectAsState()
    val hybridState by component.hybridStore.state.collectAsState()
    val sessionState by component.sessionState.collectAsState()
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

    LaunchedEffect(hybridState.error) {
        hybridState.error?.let {
            snackbarHostState.showSnackbar(it)
            component.hybridStore.clearError()
        }
    }

    LaunchedEffect(hybridState.message) {
        hybridState.message?.let {
            snackbarHostState.showSnackbar(it)
            component.hybridStore.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Профиль") },
                    navigationIcon = {
                        if (showBack) {
                            IconButton(onClick = component::goBack) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                            }
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(paddingValues),
        ) {
            if (showTabs) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ProfileTab.entries) { tab ->
                        FilterChip(
                            selected = state.selectedTab == tab,
                            onClick = { component.selectTab(tab) },
                            label = { Text(tab.title) },
                        )
                    }
                }
            }

            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                when (if (showTabs) state.selectedTab else ProfileTab.PROFILE) {
                    ProfileTab.OVERVIEW -> HomeScreen(
                        sessionState = sessionState,
                        store = component.homeStore,
                        onNearby = { component.selectTab(ProfileTab.NETWORK) },
                        onPairing = component::openInvite,
                        onContacts = { component.selectTab(ProfileTab.CONTACTS) },
                        onChats = { component.selectTab(ProfileTab.CHATS) },
                        onDiagnostics = { component.selectTab(ProfileTab.METRICS) },
                        onSettings = { component.selectTab(ProfileTab.SETTINGS) },
                    )
                    ProfileTab.PROFILE -> AccountTab(component, state, hybridState)
                    ProfileTab.CHATS -> ChatsScreen(
                        store = component.chatsStore,
                        onOpenConversation = { conversationId, peerId ->
                            component.openConversation(
                                conversationId = conversationId,
                                peerId = peerId.ifBlank { null },
                            )
                        },
                        onOpenGroup = { chatId -> component.openConversation(chatId, null, "Группа") },
                        onOpenContacts = component::openContactsTab,
                    )
                    ProfileTab.NETWORK -> NearbyScreen(
                        store = component.nearbyStore,
                        onPairing = component::openInvite,
                        onOpenChat = component::openChatForPeer,
                    )
                    ProfileTab.CONTACTS -> ContactsScreen(
                        store = component.contactsStore,
                        onOpenChat = component::openChatForPeer,
                        onShowMessage = component::showMessage,
                    )
                    ProfileTab.METRICS -> DiagnosticsScreen(component.diagnosticsStore)
                    ProfileTab.SETTINGS -> SettingsScreen(
                        sessionState = sessionState,
                        store = component.settingsStore,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountTab(
    component: ProfileComponent,
    state: ProfileState,
    hybridState: org.expert.link.app.shared.presentation.HybridUiState,
) {
    val scope = rememberCoroutineScope()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = initials(state.draftName.ifBlank { state.displayName }),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                        Text(
                            text = state.displayName.ifBlank { "Имя" },
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                        )
                    }

                    OutlinedTextField(
                        value = state.draftName,
                        onValueChange = component::updateName,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Имя") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                    )

                    FlowRow(
                        maxItemsInEachRow = 2,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = component::save,
                            enabled = !state.isSaving && state.draftName.isNotBlank(),
                        ) {
                            Text(if (state.isSaving) "Сохранение..." else "Сохранить")
                        }
                        Button(onClick = component::openInvite) {
                            Text("Контакт")
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Central backend",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = hybridState.hybridState?.connectivityMode?.asUiText() ?: "Не настроен",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = hybridState.baseUrl,
                        onValueChange = component.hybridStore::updateBaseUrl,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Адрес central backend") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                    )

                    OutlinedTextField(
                        value = hybridState.username,
                        onValueChange = component.hybridStore::updateUsername,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Логин") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                    )

                    OutlinedTextField(
                        value = hybridState.password,
                        onValueChange = component.hybridStore::updatePassword,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Пароль") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(hybridState.organizations) { organization ->
                            FilterChip(
                                selected = organization.active,
                                onClick = {
                                    scope.launch {
                                        component.hybridStore.selectOrganization(organization.organizationId)
                                    }
                                },
                                label = { Text(organization.name) },
                            )
                        }
                    }

                    FlowRow(
                        maxItemsInEachRow = 2,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    component.hybridStore.login()
                                }
                            },
                            enabled = !hybridState.loading,
                        ) {
                            Text(if (hybridState.loading) "Подключение..." else "Войти")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    component.hybridStore.syncNow()
                                }
                            },
                            enabled = !hybridState.loading,
                        ) {
                            Text("Синхронизировать")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    component.hybridStore.logout()
                                }
                            },
                            enabled = !hybridState.loading && hybridState.authState?.authenticated == true,
                        ) {
                            Text("Выйти")
                        }
                    }

                    hybridState.authState?.userProfile?.let { profile ->
                        Text(
                            text = "Пользователь: ${profile.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    hybridState.syncStatus?.let { sync ->
                        Text(
                            text = "Очередь: ${sync.pendingChanges}, конфликты: ${sync.conflicts}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    hybridState.workspace?.let { workspace ->
                        workspace.dashboard?.let { dashboard ->
                            Text(
                                text = "Сводка организации: участники ${dashboard.memberships}, роли ${dashboard.roles}, локации ${dashboard.locations}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "Справочники: локации ${workspace.locations.size}, подразделения ${workspace.departments.size}, ЦФО ${workspace.costCenters.size}, счета ${workspace.bankAccounts.size}, контрагенты ${workspace.parties.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (workspace.permissionDefinitions.isNotEmpty()) {
                            Text(
                                text = "Права: ${workspace.permissionDefinitions.size}, relay-узлы: ${workspace.relayNodes.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    hybridState.pendingChanges.takeIf { it.isNotEmpty() }?.let { changes ->
                        Text(
                            text = "Ожидают отправки: ${changes.joinToString(limit = 3) { it.changeType }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    hybridState.conflicts.takeIf { it.isNotEmpty() }?.let { conflicts ->
                        Text(
                            text = "Требуют разбора: ${conflicts.joinToString(limit = 2) { it.aggregateType }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        item {
            ProfileInfoCard(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Router,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                label = "IP / endpoint",
                value = state.endpoint?.let { "${it.host}:${it.port}" } ?: "-",
            )
        }

        item {
            ProfileInfoCard(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                label = "UUID / Peer ID",
                value = state.peerId.ifBlank { "-" },
            )
        }
    }
}

@Composable
private fun ProfileInfoCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.medium),
                color = MaterialTheme.colorScheme.primaryContainer,
                content = {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        icon()
                    }
                },
            )
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
