package org.expert.link.app.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.expert.link.app.shared.navigation.AppRoute
import org.expert.link.app.shared.navigation.PrimaryDestination
import org.expert.link.app.shared.navigation.primaryDestination
import org.expert.link.app.shared.navigation.title
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.presentation.ExpertLinkAppStore
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.ui.screens.CallScreen
import org.expert.link.app.shared.ui.screens.ChatScreen
import org.expert.link.app.shared.ui.screens.ChatsScreen
import org.expert.link.app.shared.ui.screens.ContactsScreen
import org.expert.link.app.shared.ui.screens.DiagnosticsScreen
import org.expert.link.app.shared.ui.screens.HomeScreen
import org.expert.link.app.shared.ui.screens.LoadingScreen
import org.expert.link.app.shared.ui.screens.NearbyScreen
import org.expert.link.app.shared.ui.screens.PairingScreen
import org.expert.link.app.shared.ui.screens.ProfileScreen
import org.expert.link.app.shared.ui.screens.SettingsScreen
import org.expert.link.app.shared.ui.screens.TransfersScreen

/** Корневой Compose UI клиентского приложения. */
@Composable
fun ExpertLinkApp(services: AppPlatformServices) {
    val store = remember(services.platformName) { ExpertLinkAppStore(services) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStack by store.navigator.backStack.collectAsState()
    val currentRoute = backStack.last()
    val sessionState by store.session.state.collectAsState()

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch { store.session.stopNode() }
        }
    }

    LaunchedEffect(currentRoute, sessionState.status) {
        if (currentRoute == AppRoute.Loading && sessionState.status == NodeRuntimeStatus.STOPPED) {
            store.session.start()
        }
        if (currentRoute == AppRoute.Loading && sessionState.status == NodeRuntimeStatus.RUNNING) {
            store.navigator.replace(AppRoute.Home)
        }
    }

    LaunchedEffect(sessionState.config) {
        store.settings.syncFromSession()
    }

    LaunchedEffect(sessionState.profile, sessionState.endpoint, sessionState.status) {
        store.profile.refresh()
    }

    ExpertLinkTheme {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = currentRoute.title(),
                    canGoBack = backStack.size > 1,
                    onBack = { store.navigator.pop() },
                )
            },
            bottomBar = {
                if (currentRoute != AppRoute.Loading) {
                    AppBottomBar(
                        selected = currentRoute.primaryDestination(),
                        onSelect = { destination -> store.navigator.switchPrimary(destination) },
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter,
            ) {
                when (val route = currentRoute) {
                    AppRoute.Loading -> LoadingScreen(
                        sessionState = sessionState,
                        onRetry = { scope.launch { store.session.start() } },
                    )
                    AppRoute.Home -> HomeScreen(
                        sessionState = sessionState,
                        store = store.home,
                        onNearby = { store.navigator.push(AppRoute.Nearby) },
                        onPairing = { store.navigator.push(AppRoute.Pairing) },
                        onContacts = { store.navigator.push(AppRoute.Contacts) },
                        onChats = { store.navigator.switchPrimary(PrimaryDestination.CHATS) },
                        onCalls = { store.navigator.push(AppRoute.Call) },
                        onDiagnostics = { store.navigator.push(AppRoute.Diagnostics) },
                        onSettings = { store.navigator.push(AppRoute.Settings) },
                    )
                    AppRoute.Nearby -> NearbyScreen(
                        store = store.nearby,
                        onPairing = { store.navigator.push(AppRoute.Pairing) },
                        onOpenChat = { peerId ->
                            scope.launch {
                                store.chats.openConversation(peerId).onSuccess {
                                    store.navigator.push(AppRoute.Dialog(it.conversationId, peerId))
                                }.onFailure {
                                    showMessage(it.message ?: "Не удалось открыть чат")
                                }
                            }
                        },
                    )
                    AppRoute.Pairing -> PairingScreen(
                        store = store.pairing,
                        services = services,
                        onShowMessage = ::showMessage,
                    )
                    AppRoute.Contacts -> ContactsScreen(
                        store = store.contacts,
                        onOpenChat = { peerId ->
                            scope.launch {
                                store.chats.openConversation(peerId).onSuccess {
                                    store.navigator.push(AppRoute.Dialog(it.conversationId, peerId))
                                }.onFailure {
                                    showMessage(it.message ?: "Не удалось открыть чат")
                                }
                            }
                        },
                        onStartCall = { peerId ->
                            store.calls.prefill(peerId)
                            store.navigator.push(AppRoute.Call)
                        },
                        onSendFile = { peerId ->
                            store.transfers.prefill(peerId)
                            store.navigator.push(AppRoute.Transfers)
                        },
                        onShowMessage = ::showMessage,
                    )
                    AppRoute.Chats -> ChatsScreen(
                        store = store.chats,
                        onOpenConversation = { conversationId, peerId -> store.navigator.push(AppRoute.Dialog(conversationId, peerId)) },
                        onOpenContacts = { store.navigator.push(AppRoute.Contacts) },
                    )
                    is AppRoute.Dialog -> {
                        LaunchedEffect(route.conversationId, route.peerId) {
                            store.chat.bind(route.conversationId, route.peerId)
                        }
                        ChatScreen(
                            store = store.chat,
                            localPeerId = sessionState.profile?.peerId,
                            onStartCall = { peerId ->
                                store.calls.prefill(peerId)
                                store.navigator.push(AppRoute.Call)
                            },
                            onSendFile = { peerId, conversationId ->
                                store.transfers.prefill(peerId, conversationId)
                                store.navigator.push(AppRoute.Transfers)
                            },
                        )
                    }
                    AppRoute.Transfers -> TransfersScreen(
                        store = store.transfers,
                        services = services,
                        onShowMessage = ::showMessage,
                    )
                    AppRoute.Call -> CallScreen(store = store.calls)
                    AppRoute.Diagnostics -> DiagnosticsScreen(store = store.diagnostics)
                    AppRoute.Profile -> ProfileScreen(
                        sessionState = sessionState,
                        store = store.profile,
                        services = services,
                        onPairing = { store.navigator.push(AppRoute.Pairing) },
                        onShowMessage = ::showMessage,
                    )
                    AppRoute.Settings -> SettingsScreen(sessionState = sessionState, store = store.settings)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (canGoBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                }
            }
        },
    )
}

@Composable
private fun AppBottomBar(
    selected: PrimaryDestination?,
    onSelect: (PrimaryDestination) -> Unit,
) {
    NavigationBar {
        PrimaryDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = {
                    when (destination) {
                        PrimaryDestination.HOME -> Icon(Icons.Outlined.Home, contentDescription = destination.title)
                        PrimaryDestination.CHATS -> Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = destination.title)
                        PrimaryDestination.TRANSFERS -> Icon(Icons.Outlined.Folder, contentDescription = destination.title)
                        PrimaryDestination.PROFILE -> Icon(Icons.Outlined.PersonOutline, contentDescription = destination.title)
                    }
                },
                label = { Text(destination.title) },
            )
        }
    }
}
