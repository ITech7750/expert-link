package org.expert.link.app.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.presentation.CallsStore
import org.expert.link.app.shared.presentation.ChatStore
import org.expert.link.app.shared.presentation.ChatsStore
import org.expert.link.app.shared.presentation.ContactsStore
import org.expert.link.app.shared.presentation.DiagnosticsStore
import org.expert.link.app.shared.presentation.HomeStore
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.presentation.NodeSessionState
import org.expert.link.app.shared.presentation.NearbyStore
import org.expert.link.app.shared.presentation.PairingStore
import org.expert.link.app.shared.presentation.ProfileStore
import org.expert.link.app.shared.presentation.SettingsStore
import org.expert.link.app.shared.presentation.TransfersStore
import org.expert.link.app.shared.ui.components.BadgeChip
import org.expert.link.app.shared.ui.components.ChipTone
import org.expert.link.app.shared.ui.components.EmptyState
import org.expert.link.app.shared.ui.components.InfoRow
import org.expert.link.app.shared.ui.components.LoadingState
import org.expert.link.app.shared.ui.components.MonospaceValue
import org.expert.link.app.shared.ui.components.PollingEffect
import org.expert.link.app.shared.ui.components.QrCodeCard
import org.expert.link.app.shared.ui.components.ResponsiveColumns
import org.expert.link.app.shared.ui.components.SectionCard
import org.expert.link.app.shared.ui.components.StatusBanner
import org.expert.link.app.shared.ui.components.asTone
import org.expert.link.app.shared.ui.components.asUiText
import org.expert.link.app.shared.ui.components.asUiTime
import org.expert.link.app.shared.ui.components.progress
import org.expert.link.app.shared.ui.components.shortId
import org.expert.link.mesh.contract.config.MeshRelayConfig
import org.expert.link.mesh.contract.model.MeshCallSession
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshConversation
import org.expert.link.mesh.contract.model.MeshEventCategory
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshPairedPeer

private val ScreenWidth = 1120.dp

private fun Modifier.screenBounds(): Modifier = this
    .fillMaxHeight()
    .widthIn(max = ScreenWidth)
    .padding(horizontal = 16.dp)

private enum class TransferFilter(val title: String) {
    ALL("Все"),
    ACTIVE("Активные"),
    DONE("Готово"),
}

private enum class DiagnosticsSection(val title: String) {
    OVERVIEW("Общее"),
    EVENTS("События"),
    METRICS("Метрики"),
    NETWORK("Сеть"),
}

@Composable
fun LoadingScreen(
    sessionState: NodeSessionState,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.screenBounds(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LoadingState(title = "Запуск", text = "Готовим узел и локальное соединение")
        }
        item {
            SectionCard(title = "Состояние") {
                InfoRow("Платформа", sessionState.platformName)
                InfoRow("Режим", sessionState.transportHint)
                InfoRow(
                    "Статус",
                    when (sessionState.status) {
                        NodeRuntimeStatus.STOPPED -> "Остановлен"
                        NodeRuntimeStatus.STARTING -> "Запускается"
                        NodeRuntimeStatus.RUNNING -> "Готов"
                        NodeRuntimeStatus.ERROR -> "Ошибка"
                    },
                )
                sessionState.errorMessage?.let { StatusBanner(it, ChipTone.ERROR) }
                Button(onClick = onRetry) { Text("Повторить") }
            }
        }
    }
}

@Composable
fun HomeScreen(
    sessionState: NodeSessionState,
    store: HomeStore,
    onNearby: () -> Unit,
    onPairing: () -> Unit,
    onContacts: () -> Unit,
    onChats: () -> Unit,
    onCalls: () -> Unit,
    onDiagnostics: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    PollingEffect(key = "home", enabled = sessionState.status == NodeRuntimeStatus.RUNNING) {
        store.refresh()
    }
    LazyColumn(
        modifier = Modifier.screenBounds(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ResponsiveColumns(
                first = {
                    SectionCard(title = "Узел", subtitle = "Главная точка входа в приложение") {
                        InfoRow(
                            "Статус",
                            when (sessionState.status) {
                                NodeRuntimeStatus.STOPPED -> "Остановлен"
                                NodeRuntimeStatus.STARTING -> "Запускается"
                                NodeRuntimeStatus.RUNNING -> "Работает"
                                NodeRuntimeStatus.ERROR -> "Ошибка"
                            },
                        )
                        sessionState.profile?.let { InfoRow("Имя", it.displayName) }
                        sessionState.profile?.let { InfoRow("Короткий ID", it.peerId.shortId(12)) }
                        sessionState.endpoint?.let { InfoRow("Адрес", "${it.host}:${it.port}") }
                        InfoRow("Режим", sessionState.transportHint)
                        sessionState.errorMessage?.let { StatusBanner(it, ChipTone.ERROR) }
                    }
                },
                second = {
                    SectionCard(title = "Сейчас доступно") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BadgeChip("Рядом: ${state.nearbyCount}", ChipTone.INFO)
                            BadgeChip("Контакты: ${state.peersCount}", ChipTone.SUCCESS)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BadgeChip("Чаты: ${state.conversationsCount}", ChipTone.INFO)
                            BadgeChip("Передачи: ${state.transfersCount}", ChipTone.INFO)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BadgeChip("Звонки: ${state.callsCount}", ChipTone.INFO)
                            state.relayStatus?.let {
                                BadgeChip(
                                    text = if (it.enabled) "Relay готов" else "Relay выключен",
                                    tone = if (it.enabled) ChipTone.SUCCESS else ChipTone.WARNING,
                                )
                            }
                        }
                        state.error?.let { StatusBanner(it, ChipTone.ERROR) }
                    }
                },
            )
        }
        item {
            SectionCard(title = "Быстрые действия", subtitle = "Самые частые действия на одном экране") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onNearby) { Text("Найти узлы") }
                    FilledTonalButton(onClick = onPairing) { Text("Сопряжение") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onChats) { Text("Открыть чаты") }
                    FilledTonalButton(onClick = onContacts) { Text("Контакты") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCalls) { Text("Звонок") }
                    OutlinedButton(onClick = onDiagnostics) { Text("Диагностика") }
                    OutlinedButton(onClick = onSettings) { Text("Настройки") }
                }
                OutlinedButton(onClick = { scope.launch { store.refresh() } }) { Text("Обновить") }
            }
        }
        if (state.peersCount == 0 && state.conversationsCount == 0) {
            item {
                SectionCard(title = "Как начать") {
                    Text("1. Найдите узлы рядом или попросите приглашение.")
                    Text("2. Откройте экран сопряжения и подключите контакт.")
                    Text("3. После этого откройте чат, передачу файла или звонок.")
                }
            }
        }
    }
}

@Composable
fun PairingScreen(
    store: PairingStore,
    services: AppPlatformServices,
    onShowMessage: (String) -> Unit,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    val qrCode = remember(state.invite) { state.invite.takeIf { it.isNotBlank() }?.let(services::buildQrCode) }
    PollingEffect(key = "pairing") { store.refresh() }
    LazyColumn(
        modifier = Modifier.screenBounds(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ResponsiveColumns(
                first = {
                    SectionCard(
                        title = "Моё приглашение",
                        subtitle = "Создайте приглашение и передайте его другому пользователю",
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { scope.launch { store.createInvite() } }) { Text("Создать приглашение") }
                            if (state.invite.isNotBlank() && services.capabilities.canCopyText) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        services.copyText("Приглашение", state.invite)
                                            .onSuccess { onShowMessage("Скопировано") }
                                            .onFailure { onShowMessage(it.message ?: "Не удалось скопировать") }
                                    }
                                }) { Text("Копировать") }
                            }
                            if (state.invite.isNotBlank() && services.capabilities.canShareText) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        services.shareText("Приглашение Expert Link", state.invite)
                                            .onSuccess { onShowMessage("Окно отправки открыто") }
                                            .onFailure { onShowMessage(it.message ?: "Не удалось открыть отправку") }
                                    }
                                }) { Text("Поделиться") }
                            }
                        }
                        when {
                            state.loading -> StatusBanner("Готовим новое приглашение", ChipTone.INFO)
                            state.invite.isBlank() -> Text("Нажмите «Создать приглашение», чтобы показать QR или скопировать строку.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> {
                                StatusBanner("Приглашение действует недолго. Лучше использовать его сразу.", ChipTone.INFO)
                                MonospaceValue(state.invite)
                            }
                        }
                        state.message?.let { StatusBanner(it, ChipTone.SUCCESS) }
                        state.error?.let { StatusBanner(it, ChipTone.ERROR) }
                    }
                    if (qrCode != null && services.capabilities.canRenderQr) {
                        QrCodeCard(
                            matrix = qrCode,
                            title = "QR-код",
                            subtitle = "Покажите этот код второму устройству",
                        )
                    }
                },
                second = {
                    SectionCard(
                        title = "Подключиться",
                        subtitle = "Вставьте приглашение другого пользователя или введите его вручную",
                    ) {
                        OutlinedTextField(
                            value = state.importInvite,
                            onValueChange = store::updateInviteInput,
                            label = { Text("Приглашение") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { scope.launch { store.pair() } }) { Text("Подключить") }
                            OutlinedButton(onClick = { scope.launch { store.refresh() } }) { Text("Обновить") }
                        }
                        if (!services.capabilities.canScanQr) {
                            Text("Если QR-сканер недоступен, приглашение можно вставить вручную.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    SectionCard(title = "Что дальше") {
                        Text("После успешного сопряжения контакт появится в списке доверенных.")
                        Text("Из контакта можно сразу открыть чат, передать файл или начать звонок.")
                    }
                },
            )
        }
        item {
            ResponsiveColumns(
                first = {
                    SectionCard(title = "Активные попытки") {
                        if (state.sessions.isEmpty()) {
                            Text("Сейчас нет активных попыток сопряжения.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.sessions.forEach { session ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(session.remoteDisplayName ?: session.remotePeerId?.shortId(12) ?: session.sessionId.shortId(12), fontWeight = FontWeight.SemiBold)
                                        Text(session.expiresAt.asUiTime(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    BadgeChip(session.trustState.asUiText(), session.trustState.let {
                                        if (it.name == "TRUSTED") ChipTone.SUCCESS else ChipTone.INFO
                                    })
                                }
                            }
                        }
                    }
                },
                second = {
                    SectionCard(title = "Доверенные контакты") {
                        if (state.peers.isEmpty()) {
                            Text("После сопряжения контакты появятся здесь.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.peers.forEach { peer ->
                                InfoRow(peer.identity.displayName.ifBlank { peer.identity.peerId.shortId(12) }, peer.trustState.asUiText())
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
fun NearbyScreen(
    store: NearbyStore,
    onPairing: () -> Unit,
    onOpenChat: (String) -> Unit,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    val routes = remember(state.routes) { state.routes.associateBy { it.targetPeerId } }
    PollingEffect(key = "nearby") { store.refresh() }
    LazyColumn(
        modifier = Modifier.screenBounds(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ResponsiveColumns(
                first = {
                    SectionCard(title = "Узлы рядом", subtitle = "Найдите соседние устройства в локальной сети") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { scope.launch { store.announcePresence() } }) { Text("Найти узлы") }
                            OutlinedButton(onClick = { scope.launch { store.refresh() } }) { Text("Обновить") }
                        }
                        Text("Сначала нажмите «Найти узлы», затем выберите устройство из списка ниже.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        state.error?.let { StatusBanner(it, ChipTone.ERROR) }
                    }
                },
                second = {
                    SectionCard(title = "Поиск по ID", subtitle = "Если знаете peerId, можно запросить маршрут напрямую") {
                        OutlinedTextField(
                            value = state.queryPeerId,
                            onValueChange = store::updateQueryPeerId,
                            label = { Text("peerId") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { scope.launch { store.discoverPeer() } }) { Text("Найти по ID") }
                            OutlinedButton(onClick = { store.selectPeer("") }) { Text("Очистить") }
                        }
                    }
                },
            )
        }
        item {
            SectionCard(title = "Список узлов") {
                if (state.nearby.isEmpty()) {
                    Text("Пока ничего не найдено. Проверьте, что второй узел уже запущен и находится в той же сети.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.nearby.forEach { peer ->
                        val knownName = state.trustedPeerNames[peer.peerId]
                        val route = routes[peer.peerId]
                        Card {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(knownName ?: peer.peerId.shortId(14), fontWeight = FontWeight.SemiBold)
                                Text("${peer.endpoint.host}:${peer.endpoint.port}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BadgeChip(
                                        text = if (peer.peerId in state.trustedPeerIds) "Доверен" else "Без пары",
                                        tone = if (peer.peerId in state.trustedPeerIds) ChipTone.SUCCESS else ChipTone.WARNING,
                                    )
                                    BadgeChip(text = peer.source.asUiText(), tone = ChipTone.INFO)
                                    route?.let { BadgeChip(text = it.routeMode.asUiText(), tone = it.routeMode.asTone()) }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (peer.peerId in state.trustedPeerIds) {
                                        FilledTonalButton(onClick = { onOpenChat(peer.peerId) }) { Text("Чат") }
                                    } else {
                                        FilledTonalButton(onClick = onPairing) { Text("Сопряжение") }
                                    }
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            store.selectPeer(peer.peerId)
                                            store.refresh(peer.peerId)
                                        }
                                    }) { Text("Маршрут") }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = "Маршрут") {
                val plan = state.routingPlan
                if (plan == null) {
                    Text("Выберите узел из списка или введите peerId, чтобы увидеть путь доставки.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    BadgeChip(plan.routeMode.asUiText(), plan.routeMode.asTone())
                    if (plan.hops.isEmpty()) {
                        Text("Пока нет подробного списка hop-узлов.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        plan.hops.forEach { hop ->
                            InfoRow(hop.peerId.shortId(10), "${hop.endpoint.host}:${hop.endpoint.port}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsScreen(
    store: ContactsStore,
    onOpenChat: (String) -> Unit,
    onStartCall: (String) -> Unit,
    onSendFile: (String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var pendingBlock by remember { mutableStateOf<MeshPairedPeer?>(null) }
    PollingEffect(key = "contacts") { store.refresh() }
    pendingBlock?.let { peer ->
        AlertDialog(
            onDismissRequest = { pendingBlock = null },
            title = { Text("Заблокировать контакт?") },
            text = { Text("После блокировки сообщения и звонки от этого контакта будут отклоняться.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        store.block(peer.identity.peerId)
                        pendingBlock = null
                        onShowMessage("Контакт заблокирован")
                    }
                }) { Text("Заблокировать") }
            },
            dismissButton = {
                TextButton(onClick = { pendingBlock = null }) { Text("Отмена") }
            },
        )
    }
    LazyColumn(
        modifier = Modifier.screenBounds(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionCard(title = "Доверенные контакты", subtitle = "Отсюда удобнее всего начать чат, звонок или передачу файла") {
                if (state.peers.isEmpty()) {
                    Text("Пока нет доверенных контактов. Сначала выполните сопряжение.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.peers.sortedBy { it.identity.displayName.ifBlank { it.identity.peerId } }.forEach { peer ->
                        ContactRow(
                            peer = peer,
                            onOpenChat = { onOpenChat(peer.identity.peerId) },
                            onStartCall = { onStartCall(peer.identity.peerId) },
                            onSendFile = { onSendFile(peer.identity.peerId) },
                            onBlock = { pendingBlock = peer },
                        )
                    }
                }
            }
        }
        item {
            SectionCard(title = "Заблокированные") {
                if (state.blockedPeers.isEmpty()) {
                    Text("Список блокировок пуст.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.blockedPeers.forEach { blocked ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(blocked.peerId.shortId(12), fontWeight = FontWeight.SemiBold)
                                Text(blocked.reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    store.unblock(blocked.peerId)
                                    onShowMessage("Контакт разблокирован")
                                }
                            }) { Text("Вернуть") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    peer: MeshPairedPeer,
    onOpenChat: () -> Unit,
    onStartCall: () -> Unit,
    onSendFile: () -> Unit,
    onBlock: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(peer.identity.displayName.ifBlank { peer.identity.peerId.shortId(12) }, fontWeight = FontWeight.SemiBold)
            SelectionContainer {
                Text(peer.identity.peerId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BadgeChip(text = peer.trustState.asUiText(), tone = ChipTone.SUCCESS)
                peer.endpointHint?.let { BadgeChip(text = it.host, tone = ChipTone.INFO) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onOpenChat) { Text("Чат") }
                OutlinedButton(onClick = onSendFile) { Text("Файл") }
                OutlinedButton(onClick = onStartCall) { Text("Звонок") }
                OutlinedButton(onClick = onBlock) { Text("Блок") }
            }
        }
    }
}

@Composable
fun ChatsScreen(
    store: ChatsStore,
    onOpenConversation: (conversationId: String, peerId: String) -> Unit,
    onOpenContacts: () -> Unit,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    PollingEffect(key = "chats") { store.refresh() }
    val peerMap = remember(state.peers) { state.peers.associateBy { it.identity.peerId } }
    val conversations = remember(state.conversations) { state.conversations.sortedByDescending { it.updatedAt } }
    LazyColumn(
        modifier = Modifier.screenBounds(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionCard(title = "Новый чат", subtitle = "Откройте диалог по peerId или выберите контакт ниже") {
                OutlinedTextField(
                    value = state.newPeerId,
                    onValueChange = store::updateNewPeerId,
                    label = { Text("peerId контакта") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            store.openConversation().onSuccess {
                                val peerId = it.participantPeerIds.firstOrNull { id -> id != state.localPeerId }.orEmpty()
                                onOpenConversation(it.conversationId, peerId)
                            }
                        }
                    }) { Text("Открыть") }
                    OutlinedButton(onClick = onOpenContacts) { Text("Контакты") }
                }
                if (state.peers.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.peers.take(4).forEach { peer ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    store.prefillPeerId(peer.identity.peerId)
                                },
                                label = { Text(peer.identity.displayName.ifBlank { peer.identity.peerId.shortId(8) }) },
                            )
                        }
                    }
                }
                state.error?.let { StatusBanner(it, ChipTone.ERROR) }
            }
        }
        item {
            SectionCard(title = "Диалоги") {
                if (conversations.isEmpty()) {
                    Text("Диалогов пока нет. Начните с контактов или откройте новый чат по peerId.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    conversations.forEachIndexed { index, conversation ->
                        val peerId = conversation.participantPeerIds.firstOrNull { it != state.localPeerId }.orEmpty()
                        val peer = peerMap[peerId]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(peer?.identity?.displayName ?: peerId.shortId(12), fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Обновлён ${conversation.updatedAt.asUiTime()}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            FilledTonalButton(onClick = { onOpenConversation(conversation.conversationId, peerId) }) { Text("Открыть") }
                        }
                        if (index < conversations.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    store: ChatStore,
    localPeerId: String?,
    onStartCall: (String) -> Unit,
    onSendFile: (String, String) -> Unit,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showRouteDetails by remember { mutableStateOf(false) }
    PollingEffect(key = state.conversationId, enabled = state.conversationId.isNotBlank()) { store.refresh() }
    Column(
        modifier = Modifier
            .screenBounds()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard(title = "Диалог", subtitle = "Сообщения и быстрые действия по контакту") {
            Text(state.peerId.shortId(14), fontWeight = FontWeight.SemiBold)
            state.route?.let { route ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BadgeChip(route.routeMode.asUiText(), route.routeMode.asTone())
                    BadgeChip("Hop: ${route.hops.size}", ChipTone.INFO)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onSendFile(state.peerId, state.conversationId) }) { Text("Файл") }
                    OutlinedButton(onClick = { onStartCall(state.peerId) }) { Text("Звонок") }
                    OutlinedButton(onClick = { showRouteDetails = !showRouteDetails }) { Text(if (showRouteDetails) "Скрыть маршрут" else "Маршрут") }
                }
                if (showRouteDetails) {
                    route.hops.forEach { hop ->
                        InfoRow(hop.peerId.shortId(10), "${hop.endpoint.host}:${hop.endpoint.port}")
                    }
                }
            } ?: Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { onSendFile(state.peerId, state.conversationId) }) { Text("Файл") }
                OutlinedButton(onClick = { onStartCall(state.peerId) }) { Text("Звонок") }
            }
            state.error?.let { StatusBanner(it, ChipTone.ERROR) }
        }
        Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        Text("Сообщений пока нет. Отправьте первое сообщение ниже.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(state.messages) { message ->
                        val own = message.senderPeerId == localPeerId
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (own) Alignment.End else Alignment.Start,
                        ) {
                            Card {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(message.body)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        BadgeChip(message.deliveryStatus.asUiText(), message.deliveryStatus.asTone())
                                        Text(
                                            text = message.createdAt.asUiTime(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            if (message.deliveryStatus.name == "FAILED") {
                                OutlinedButton(onClick = { scope.launch { store.resend(message) } }) { Text("Повторить") }
                            }
                        }
                    }
                }
            }
        }
        SectionCard(title = "Новое сообщение") {
            OutlinedTextField(
                value = state.draft,
                onValueChange = store::updateDraft,
                label = { Text("Введите сообщение") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { scope.launch { store.send() } }) { Text("Отправить") }
                if (state.receipts.isNotEmpty()) {
                    Text(
                        text = "Последнее подтверждение: ${state.receipts.last().deliveryStatus.asUiText()} в ${state.receipts.last().receivedAt.asUiTime()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun TransfersScreen(
    store: TransfersStore,
    services: AppPlatformServices,
    onShowMessage: (String) -> Unit,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf(TransferFilter.ALL) }
    var pendingCancel by remember { mutableStateOf<MeshFileTransferSession?>(null) }
    val filteredTransfers = remember(state.transfers, filter) {
        when (filter) {
            TransferFilter.ALL -> state.transfers.sortedByDescending { it.updatedAt }
            TransferFilter.ACTIVE -> state.transfers.filter { it.status.name in setOf("OFFERED", "ACCEPTED", "IN_PROGRESS", "PAUSED") }.sortedByDescending { it.updatedAt }
            TransferFilter.DONE -> state.transfers.filter { it.status.name in setOf("COMPLETED", "FAILED", "CANCELLED") }.sortedByDescending { it.updatedAt }
        }
    }
    PollingEffect(key = "transfers") { store.refresh() }
    pendingCancel?.let { transfer ->
        AlertDialog(
            onDismissRequest = { pendingCancel = null },
            title = { Text("Отменить передачу?") },
            text = { Text("Передача «${transfer.descriptor.fileName}» будет остановлена.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        store.cancel(transfer.transferId)
                        pendingCancel = null
                        onShowMessage("Передача отменена")
                    }
                }) { Text("Отменить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancel = null }) { Text("Назад") }
            },
        )
    }
    LazyColumn(
        modifier = Modifier.screenBounds(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionCard(title = "Новая передача", subtitle = "Укажите контакт и файл") {
                OutlinedTextField(
                    value = state.targetPeerId,
                    onValueChange = store::updateTargetPeerId,
                    label = { Text("peerId получателя") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.path,
                    onValueChange = store::updatePath,
                    label = { Text("Файл") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("На рабочем столе можно выбрать файл кнопкой ниже.") },
                )
                OutlinedTextField(
                    value = state.conversationId,
                    onValueChange = store::updateConversationId,
                    label = { Text("Диалог, если нужен") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            store.send()
                            store.state.value.message?.let(onShowMessage)
                        }
                    }) { Text("Отправить файл") }
                    if (services.capabilities.canPickFile) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                services.pickFile()
                                    .onSuccess { path ->
                                        path?.let {
                                            store.updatePath(it)
                                            onShowMessage("Файл выбран")
                                        }
                                    }
                                    .onFailure { onShowMessage(it.message ?: "Не удалось выбрать файл") }
                            }
                        }) { Text("Выбрать файл") }
                    }
                }
                state.message?.let { StatusBanner(it, ChipTone.SUCCESS) }
                state.error?.let { StatusBanner(it, ChipTone.ERROR) }
            }
        }
        item {
            SectionCard(title = "Передачи") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransferFilter.entries.forEach { item ->
                        FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item.title) })
                    }
                }
                if (filteredTransfers.isEmpty()) {
                    Text("Передач пока нет. Здесь появится история отправки и получения файлов.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    filteredTransfers.forEach { transfer ->
                        TransferRow(
                            transfer = transfer,
                            onResume = {
                                scope.launch {
                                    store.resume(transfer.transferId)
                                    onShowMessage("Передача обновлена")
                                }
                            },
                            onCancel = { pendingCancel = transfer },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferRow(
    transfer: MeshFileTransferSession,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(transfer.descriptor.fileName, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BadgeChip(text = transfer.status.asUiText(), tone = transfer.status.asTone())
                BadgeChip(text = if (transfer.direction.name == "OUTGOING") "Исходящая" else "Входящая", tone = ChipTone.INFO)
            }
            LinearProgressIndicator(progress = { transfer.progress() }, modifier = Modifier.fillMaxWidth())
            InfoRow("Прогресс", "${(transfer.progress() * 100).toInt()}%")
            InfoRow("Кому", transfer.recipientPeerId.shortId(10))
            InfoRow("Размер", "${transfer.descriptor.sizeBytes} Б")
            transfer.localPath?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (transfer.status.name != "COMPLETED") {
                    OutlinedButton(onClick = onResume) { Text("Продолжить") }
                }
                if (transfer.status.name !in setOf("COMPLETED", "CANCELLED")) {
                    OutlinedButton(onClick = onCancel) { Text("Отменить") }
                }
            }
        }
    }
}

@Composable
fun CallScreen(store: CallsStore) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    PollingEffect(key = "calls") { store.refresh() }
    LazyColumn(
        modifier = Modifier.screenBounds(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionCard(title = "Новый звонок", subtitle = "Сейчас доступен сигнальный режим без полноценной медиа-связи") {
                OutlinedTextField(
                    value = state.targetPeerId,
                    onValueChange = store::updateTargetPeerId,
                    label = { Text("peerId контакта") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { scope.launch { store.startCall() } }) { Text("Позвонить") }
                state.message?.let { StatusBanner(it, ChipTone.SUCCESS) }
                state.error?.let { StatusBanner(it, ChipTone.ERROR) }
            }
        }
        item {
            SectionCard(title = "Текущие звонки") {
                if (state.sessions.isEmpty()) {
                    Text("Активных звонков нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.sessions.sortedByDescending { it.updatedAt }.forEach { call ->
                        CallRow(
                            call = call,
                            onAccept = { scope.launch { store.accept(call) } },
                            onReject = { scope.launch { store.reject(call) } },
                            onQuality = { scope.launch { store.sendQuality(call) } },
                            onHangup = { scope.launch { store.hangup(call) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallRow(
    call: MeshCallSession,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onQuality: () -> Unit,
    onHangup: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(call.callId.shortId(12), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BadgeChip(text = call.status.asUiText(), tone = call.status.asTone())
                BadgeChip(text = call.updatedAt.asUiTime(), tone = ChipTone.INFO)
            }
            InfoRow("С кем", call.recipientPeerId.shortId(10))
            call.qualitySnapshot?.let {
                InfoRow("RTT", "${it.rttMs} мс")
                InfoRow("Потери", "${it.packetLossPercent}%")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (call.status.name in setOf("RINGING", "INVITED", "NEW")) {
                    FilledTonalButton(onClick = onAccept) { Text("Принять") }
                    OutlinedButton(onClick = onReject) { Text("Отклонить") }
                }
                OutlinedButton(onClick = onQuality) { Text("Проверить") }
                OutlinedButton(onClick = onHangup) { Text("Завершить") }
            }
        }
    }
}

@Composable
fun DiagnosticsScreen(store: DiagnosticsStore) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf(DiagnosticsSection.OVERVIEW) }
    PollingEffect(key = "diagnostics") { store.refresh() }
    LazyColumn(
        modifier = Modifier.screenBounds(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionCard(title = "Диагностика", subtitle = "Служебный раздел для проверки сети и маршрутов") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticsSection.entries.forEach { item ->
                        FilterChip(selected = section == item, onClick = { section = item }, label = { Text(item.title) })
                    }
                }
                OutlinedButton(onClick = { scope.launch { store.refresh() } }) { Text("Обновить") }
                state.error?.let { StatusBanner(it, ChipTone.ERROR) }
            }
        }
        when (section) {
            DiagnosticsSection.OVERVIEW -> {
                item {
                    ResponsiveColumns(
                        first = {
                            SectionCard(title = "Сводка") {
                                InfoRow("События", state.events.size.toString())
                                InfoRow("Маршруты", state.routes.size.toString())
                                InfoRow("Узлы рядом", state.nearby.size.toString())
                                state.relayStatus?.let { InfoRow("Relay", if (it.enabled) "Включён" else "Выключен") }
                            }
                        },
                        second = {
                            SectionCard(title = "Главные метрики") {
                                state.metrics?.counters?.entries
                                    ?.sortedByDescending { it.value }
                                    ?.take(6)
                                    ?.forEach { (key, value) -> InfoRow(key, value.toString()) }
                                    ?: Text("Метрики пока пусты", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                    )
                }
            }
            DiagnosticsSection.EVENTS -> {
                item {
                    SectionCard(title = "Последние события") {
                        if (state.events.isEmpty()) {
                            Text("Событий пока нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.events.forEachIndexed { index, event ->
                                EventRow(event)
                                if (index < state.events.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
            DiagnosticsSection.METRICS -> {
                item {
                    SectionCard(title = "Счётчики") {
                        state.metrics?.counters?.entries?.sortedBy { it.key }?.forEach { (key, value) ->
                            InfoRow(key, value.toString())
                        } ?: Text("Метрики пока пусты", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            DiagnosticsSection.NETWORK -> {
                item {
                    ResponsiveColumns(
                        first = {
                            SectionCard(title = "Маршруты") {
                                if (state.routes.isEmpty()) {
                                    Text("Маршрутов пока нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    state.routes.forEach { route ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(route.targetPeerId.shortId(12), fontWeight = FontWeight.SemiBold)
                                                Text("Через ${route.nextHopPeerId.shortId(10)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            BadgeChip(route.routeMode.asUiText(), route.routeMode.asTone())
                                        }
                                    }
                                }
                            }
                        },
                        second = {
                            SectionCard(title = "Узлы рядом") {
                                if (state.nearby.isEmpty()) {
                                    Text("Рядом пока никого нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    state.nearby.forEach { nearby ->
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(nearby.peerId.shortId(12), fontWeight = FontWeight.SemiBold)
                                            Text(nearby.source.asUiText(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: org.expert.link.mesh.contract.model.MeshEventLogEntry) {
    val tone = when (event.category) {
        MeshEventCategory.SECURITY -> ChipTone.ERROR
        MeshEventCategory.ROUTING,
        MeshEventCategory.DISCOVERY,
        -> ChipTone.INFO
        MeshEventCategory.PAIRING,
        MeshEventCategory.MESSAGING,
        MeshEventCategory.FILE_TRANSFER,
        MeshEventCategory.CALL,
        MeshEventCategory.SYSTEM,
        -> ChipTone.SUCCESS
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BadgeChip(event.category.asUiText(), tone)
            Text(event.createdAt.asUiTime(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(event.message, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ProfileScreen(
    sessionState: NodeSessionState,
    store: ProfileStore,
    services: AppPlatformServices,
    onPairing: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    val qrCode = remember(state.invite) { state.invite.takeIf { it.isNotBlank() }?.let(services::buildQrCode) }
    LaunchedEffect(sessionState.profile, sessionState.endpoint) { store.refresh() }
    LazyColumn(
        modifier = Modifier.screenBounds(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ResponsiveColumns(
                first = {
                    SectionCard(title = "Профиль", subtitle = "Локальная учётная запись и адрес узла") {
                        state.profile?.let { profile ->
                            InfoRow("Имя", profile.displayName)
                            Text("peerId")
                            MonospaceValue(profile.peerId)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (services.capabilities.canCopyText) {
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            services.copyText("peerId", profile.peerId)
                                                .onSuccess { onShowMessage("peerId скопирован") }
                                                .onFailure { onShowMessage(it.message ?: "Не удалось скопировать") }
                                        }
                                    }) { Text("Копировать ID") }
                                }
                                FilledTonalButton(onClick = onPairing) { Text("Сопряжение") }
                            }
                            state.endpoint?.let { InfoRow("Адрес", "${it.host}:${it.port}") }
                        } ?: Text("Профиль пока недоступен", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                second = {
                    SectionCard(title = "Приглашение", subtitle = "Создайте приглашение для нового контакта") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { scope.launch { store.createInvite() } }) { Text("Создать") }
                            if (state.invite.isNotBlank() && services.capabilities.canCopyText) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        services.copyText("Приглашение", state.invite)
                                            .onSuccess { onShowMessage("Приглашение скопировано") }
                                            .onFailure { onShowMessage(it.message ?: "Не удалось скопировать") }
                                    }
                                }) { Text("Копировать") }
                            }
                            if (state.invite.isNotBlank() && services.capabilities.canShareText) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        services.shareText("Приглашение Expert Link", state.invite)
                                            .onSuccess { onShowMessage("Окно отправки открыто") }
                                            .onFailure { onShowMessage(it.message ?: "Не удалось открыть отправку") }
                                    }
                                }) { Text("Поделиться") }
                            }
                        }
                        if (state.invite.isBlank()) {
                            Text("Создайте приглашение, чтобы поделиться им с другим устройством.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            MonospaceValue(state.invite)
                        }
                        state.message?.let { StatusBanner(it, ChipTone.SUCCESS) }
                        state.error?.let { StatusBanner(it, ChipTone.ERROR) }
                    }
                    if (qrCode != null && services.capabilities.canRenderQr) {
                        QrCodeCard(
                            matrix = qrCode,
                            title = "QR-код",
                            subtitle = "Покажите его второму устройству",
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun SettingsScreen(
    sessionState: NodeSessionState,
    store: SettingsStore,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(sessionState.config) { store.syncFromSession() }
    val relay = state.config.relay ?: MeshRelayConfig()
    LazyColumn(
        modifier = Modifier.screenBounds(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ResponsiveColumns(
                first = {
                    SectionCard(title = "Имя и сеть", subtitle = "Основные параметры узла") {
                        OutlinedTextField(
                            value = state.config.displayName,
                            onValueChange = { value -> store.updateConfig { it.copy(displayName = value) } },
                            label = { Text("Имя") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = state.config.bindHost,
                            onValueChange = { value -> store.updateConfig { it.copy(bindHost = value) } },
                            label = { Text("Адрес") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        NumberField(label = "Порт связи", value = state.config.httpPort.toString()) { value ->
                            value.toIntOrNull()?.let { port -> store.updateConfig { config -> config.copy(httpPort = port) } }
                        }
                        NumberField(label = "Порт поиска", value = state.config.discoveryPort.toString()) { value ->
                            value.toIntOrNull()?.let { port -> store.updateConfig { config -> config.copy(discoveryPort = port) } }
                        }
                        OutlinedTextField(
                            value = state.config.multicastGroup,
                            onValueChange = { value -> store.updateConfig { it.copy(multicastGroup = value) } },
                            label = { Text("Multicast группа") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                second = {
                    SectionCard(title = "Что включено", subtitle = "Обычные пользовательские функции") {
                        ToggleRow("Поиск узлов", state.config.featureFlags.discoveryEnabled) {
                            store.updateConfig { config -> config.copy(featureFlags = config.featureFlags.copy(discoveryEnabled = it)) }
                        }
                        ToggleRow("Relay", state.config.featureFlags.relayEnabled) {
                            store.updateConfig { config -> config.copy(featureFlags = config.featureFlags.copy(relayEnabled = it)) }
                        }
                        ToggleRow("Искать через relay", relay.forceRelayLookup) {
                            store.updateConfig { config -> config.copy(relay = relay.copy(forceRelayLookup = it)) }
                        }
                        ToggleRow("Узел готов к relay", relay.relayEligible) {
                            store.updateConfig { config -> config.copy(relay = relay.copy(relayEligible = it)) }
                        }
                    }
                },
            )
        }
        item {
            ResponsiveColumns(
                first = {
                    SectionCard(title = "Для проверки", subtitle = "Технические режимы для локального demo") {
                        ToggleRow("In-memory transport", state.config.featureFlags.inMemoryTransport) {
                            store.updateConfig { config -> config.copy(featureFlags = config.featureFlags.copy(inMemoryTransport = it)) }
                        }
                        ToggleRow("In-memory discovery", state.config.featureFlags.inMemoryDiscovery) {
                            store.updateConfig { config -> config.copy(featureFlags = config.featureFlags.copy(inMemoryDiscovery = it)) }
                        }
                    }
                },
                second = {
                    SectionCard(title = "Узел") {
                        InfoRow("Платформа", sessionState.platformName)
                        InfoRow("Режим", sessionState.transportHint)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { scope.launch { store.applyAndRestart() } }) { Text("Применить") }
                            OutlinedButton(onClick = { scope.launch { store.stopNode() } }) { Text("Остановить") }
                        }
                        state.message?.let { StatusBanner(it, ChipTone.SUCCESS) }
                        state.error?.let { StatusBanner(it, ChipTone.ERROR) }
                    }
                },
            )
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
