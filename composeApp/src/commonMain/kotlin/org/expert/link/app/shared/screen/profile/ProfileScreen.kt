package org.expert.link.app.shared.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackHandler
import org.expert.link.app.shared.screen.components.initials
import org.expert.link.app.shared.ui.screens.ChatsScreen
import org.expert.link.app.shared.ui.screens.ContactsScreen
import org.expert.link.app.shared.ui.screens.DiagnosticsScreen
import org.expert.link.app.shared.ui.screens.HomeScreen
import org.expert.link.app.shared.ui.screens.NearbyScreen
import org.expert.link.app.shared.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(component: ProfileComponent) {
    val state by component.state.collectAsState()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = component::goBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
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

            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                when (state.selectedTab) {
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
                    ProfileTab.PROFILE -> AccountTab(component, state)
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

@Composable
private fun AccountTab(
    component: ProfileComponent,
    state: ProfileState,
) {
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
