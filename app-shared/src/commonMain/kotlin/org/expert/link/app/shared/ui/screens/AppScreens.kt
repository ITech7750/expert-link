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
import org.expert.link.app.shared.presentation.GroupStore
import org.expert.link.app.shared.presentation.HomeStore
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.presentation.NodeSessionState
import org.expert.link.app.shared.presentation.NearbyStore
import org.expert.link.app.shared.presentation.PairingStore
import org.expert.link.app.shared.presentation.ProfileStore
import org.expert.link.app.shared.presentation.SettingsStore
import org.expert.link.app.shared.presentation.ThreadStore
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
    SECURITY("Безопасность"),
    TOPOLOGY("Топология"),
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
                            state.relayMode?.let { BadgeChip("Режим: ${it.asUiText()}", ChipTone.INFO) }
                        }
                        state.error?.let { StatusBanner(it, ChipTone.ERROR) }
                    }
                },
            )
        }
        item {
            state.topology?.let { topology ->
                SectionCard(title = "Состояние сети", subtitle = "Роль узла и устойчивость маршрутов") {
                    InfoRow("Связность", topology.connectivityMode.asUiText())
                    InfoRow("Роль", topology.networkRoleState.localRole.asUiText())
                    InfoRow("Хост", topology.networkRoleState.currentHostPeerId?.shortId(12) ?: "не выбран")
                    InfoRow("Failover", if (topology.networkRoleState.failoverInProgress) "в процессе" else "нет")
                    InfoRow("Нездоровые маршруты", topology.routeHealth.count { it.state != org.expert.link.mesh.contract.model.MeshRouteHealthState.HEALTHY }.toString())
                }
            }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onShowMessage("Откройте камеру и отсканируйте приглашение") },
                                enabled = services.capabilities.canScanQr,
                            ) { Text("Сканировать") }
                            OutlinedButton(onClick = { store.updateInviteInput("") }) { Text("Ввести вручную") }
                        }
                        if (!services.capabilities.canScanQr) {
                            Text("На этой платформе сканер недоступен, используйте ручной ввод.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val routeHealth = remember(state.routeHealth) { state.routeHealth.associateBy { it.targetPeerId } }
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
                            OutlinedButton(onClick = { scope.launch { store.forceTopologyRefresh() } }) { Text("Пересобрать сеть") }
                        }
                        Text("Сначала нажмите «Найти узлы», затем выберите устройство из списка ниже.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        state.message?.let { StatusBanner(it, ChipTone.SUCCESS) }
                        state.error?.let { StatusBanner(it, ChipTone.ERROR) }
                    }
                },
                second = {
                    SectionCard(title = "Поиск по ID", subtitle = "Если знаете ID узла, можно запросить маршрут напрямую") {
                        OutlinedTextField(
                            value = state.queryPeerId,
                            onValueChange = store::updateQueryPeerId,
                            label = { Text("ID узла") },
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
            SectionCard(title = "Топология") {
                state.hostRole?.let { hostRole ->
                    InfoRow("Роль узла", hostRole.localRole.asUiText())
                    InfoRow("Текущий хост", hostRole.currentHostPeerId?.shortId(12) ?: "не выбран")
                    InfoRow("Хост доступен", if (hostRole.hostReachable) "да" else "нет")
                    InfoRow("Failover", if (hostRole.failoverInProgress) "в процессе" else "нет")
                } ?: Text("Снимок сети ещё не получен.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.connectivityStrategy?.let { strategy ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Стратегия", strategy.routeMode.asUiText())
                    InfoRow("Режим сети", strategy.connectivityMode.asUiText())
                    InfoRow("Relay", strategy.relayMode.asUiText())
                }
            }
        }
        item {
            SectionCard(title = "Ручной узел", subtitle = "Используйте, если авто-поиск не нашёл устройство") {
                OutlinedTextField(
                    value = state.manualPeerId,
                    onValueChange = store::updateManualPeerId,
                    label = { Text("ID узла") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.manualHost,
                        onValueChange = store::updateManualHost,
                        label = { Text("Адрес") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.manualPort,
                        onValueChange = store::updateManualPort,
                        label = { Text("Порт") },
                        modifier = Modifier.width(130.dp),
                    )
                }
                OutlinedTextField(
                    value = state.manualPath,
                    onValueChange = store::updateManualPath,
                    label = { Text("Путь") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { store.rememberPeerEndpoint() } }) { Text("Сохранить") }
                    OutlinedButton(onClick = { scope.launch { store.forgetPeerEndpoint() } }) { Text("Удалить") }
                }
            }
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
                                    routeHealth[peer.peerId]?.let { health ->
                                        BadgeChip(
                                            text = "Состояние: ${health.state.asUiText()}",
                                            tone = if (health.state == org.expert.link.mesh.contract.model.MeshRouteHealthState.HEALTHY) ChipTone.SUCCESS else ChipTone.WARNING,
                                        )
                                    }
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
                    Text("Выберите узел из списка или введите ID, чтобы увидеть путь доставки.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onOpenGroup: (chatId: String) -> Unit,
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
            SectionCard(title = "Новый чат", subtitle = "Откройте диалог по ID узла или выберите контакт ниже") {
                OutlinedTextField(
                    value = state.newPeerId,
                    onValueChange = store::updateNewPeerId,
                    label = { Text("ID контакта") },
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
            SectionCard(title = "Новая группа", subtitle = "Создайте групповой чат и добавьте участников") {
                OutlinedTextField(
                    value = state.newGroupTitle,
                    onValueChange = store::updateNewGroupTitle,
                    label = { Text("Название группы") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.newGroupDescription,
                    onValueChange = store::updateNewGroupDescription,
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.newGroupMembersInput,
                    onValueChange = store::updateNewGroupMembersInput,
                    label = { Text("Участники (ID через запятую)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            store.createGroupChat().onSuccess { conversation ->
                                onOpenGroup(conversation.conversationId)
                            }
                        }
                    }) { Text("Создать группу") }
                    OutlinedButton(onClick = onOpenContacts) { Text("Контакты") }
                }
            }
        }
        item {
            SectionCard(title = "Чаты") {
                if (conversations.isEmpty()) {
                    Text("Диалогов пока нет. Начните с контактов или откройте новый чат по ID.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    conversations.forEachIndexed { index, conversation ->
                        val peerId = conversation.participantPeerIds.firstOrNull { it != state.localPeerId }.orEmpty()
                        val peer = peerMap[peerId]
                        val isGroup = conversation.chatType.name == "GROUP"
                        val title = if (isGroup) conversation.title else (peer?.identity?.displayName ?: peerId.shortId(12))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Обновлён ${conversation.updatedAt.asUiTime()}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BadgeChip(
                                        text = if (isGroup) "Группа" else "Личный",
                                        tone = if (isGroup) ChipTone.INFO else ChipTone.SUCCESS,
                                    )
                                    if (conversation.unreadCount > 0) {
                                        BadgeChip("Новых: ${conversation.unreadCount}", ChipTone.WARNING)
                                    }
                                }
                            }
                            if (isGroup) {
                                FilledTonalButton(onClick = { onOpenGroup(conversation.conversationId) }) { Text("Группа") }
                            } else {
                                FilledTonalButton(onClick = { onOpenConversation(conversation.conversationId, peerId) }) { Text("Открыть") }
                            }
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
    onOpenThread: (rootMessageId: String) -> Unit,
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
            Text(
                text = if (state.peerId.isNotBlank()) state.peerId.shortId(14) else "Групповой чат",
                fontWeight = FontWeight.SemiBold,
            )
            val route = state.route
            if (route != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BadgeChip(route.routeMode.asUiText(), route.routeMode.asTone())
                    BadgeChip("Hop: ${route.hops.size}", ChipTone.INFO)
                }
                if (state.peerId.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { onSendFile(state.peerId, state.conversationId) }) { Text("Файл") }
                        OutlinedButton(onClick = { onStartCall(state.peerId) }) { Text("Звонок") }
                        OutlinedButton(onClick = { showRouteDetails = !showRouteDetails }) { Text(if (showRouteDetails) "Скрыть маршрут" else "Маршрут") }
                    }
                }
                if (showRouteDetails) {
                    route.hops.forEach { hop ->
                        InfoRow(hop.peerId.shortId(10), "${hop.endpoint.host}:${hop.endpoint.port}")
                    }
                }
            } else if (state.peerId.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onSendFile(state.peerId, state.conversationId) }) { Text("Файл") }
                    OutlinedButton(onClick = { onStartCall(state.peerId) }) { Text("Звонок") }
                }
            }
            state.error?.let { StatusBanner(it, ChipTone.ERROR) }
        }
        if (state.threadSummaries.isNotEmpty()) {
            SectionCard(title = "Треды") {
                state.threadSummaries.forEach { thread ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Сообщение ${thread.rootMessageId.shortId(8)}", fontWeight = FontWeight.SemiBold)
                            Text("Ответов: ${thread.replyCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { onOpenThread(thread.rootMessageId) }) { Text("Открыть") }
                    }
                }
            }
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { onOpenThread(message.messageId) }) { Text("Тред") }
                                        if (message.threadReplyCount > 0) {
                                            BadgeChip("Ответов: ${message.threadReplyCount}", ChipTone.INFO)
                                        }
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
fun GroupScreen(
    store: GroupStore,
    onOpenChat: (chatId: String) -> Unit,
    onOpenThread: (chatId: String, rootMessageId: String) -> Unit,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    PollingEffect(key = state.chatId, enabled = state.chatId.isNotBlank()) { store.refresh() }
    LazyColumn(
        modifier = Modifier.screenBounds(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionCard(title = "Группа", subtitle = "Участники, события и быстрые действия") {
                val group = state.group
                if (group == null) {
                    Text("Группа недоступна", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(group.title, fontWeight = FontWeight.SemiBold)
                    group.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BadgeChip("Участников: ${group.members.size}", ChipTone.INFO)
                        if (group.archived) BadgeChip("Архив", ChipTone.WARNING)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpenChat(group.chatId) }) { Text("Открыть чат") }
                        OutlinedButton(onClick = { scope.launch { store.refresh() } }) { Text("Обновить") }
                    }
                }
                state.error?.let { StatusBanner(it, ChipTone.ERROR) }
            }
        }
        item {
            SectionCard(title = "Управление группой") {
                OutlinedTextField(
                    value = state.titleDraft,
                    onValueChange = store::updateTitleDraft,
                    label = { Text("Новое название") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { store.rename() } }) { Text("Переименовать") }
                }
                OutlinedTextField(
                    value = state.participantDraft,
                    onValueChange = store::updateParticipantDraft,
                    label = { Text("Добавить участника (ID)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { store.addParticipant() } }) { Text("Добавить") }
                }
            }
        }
        item {
            SectionCard(title = "Участники") {
                val group = state.group
                if (group == null || group.members.isEmpty()) {
                    Text("Список участников пуст.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    group.members.forEach { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.displayName.ifBlank { member.peerId.shortId(10) }, fontWeight = FontWeight.SemiBold)
                                Text(member.role.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (member.role.name != "OWNER") {
                                OutlinedButton(onClick = { scope.launch { store.removeParticipant(member.peerId) } }) { Text("Удалить") }
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = "Системные события") {
                if (state.events.isEmpty()) {
                    Text("Событий пока нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.events.forEach { event ->
                        InfoRow(event.eventType.name, "${event.text} • ${event.createdAt.asUiTime()}")
                    }
                }
            }
        }
        item {
            SectionCard(title = "Последние сообщения") {
                if (state.messages.isEmpty()) {
                    Text("Сообщений пока нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val messages = if (state.messages.size <= 12) {
                        state.messages
                    } else {
                        state.messages.subList(state.messages.size - 12, state.messages.size)
                    }
                    messages.forEach { message ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(message.body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(message.createdAt.asUiTime(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(onClick = { onOpenThread(state.chatId, message.messageId) }) { Text("Тред") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadScreen(store: ThreadStore) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    PollingEffect(key = "${state.chatId}:${state.rootMessageId}", enabled = state.chatId.isNotBlank()) { store.refresh() }
    Column(
        modifier = Modifier
            .screenBounds()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard(title = "Тред", subtitle = "Вложенное обсуждение сообщения") {
            Text("Чат: ${state.chatId.shortId(12)}")
            Text("Root: ${state.rootMessageId.shortId(12)}")
            state.summary?.let {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BadgeChip("Ответов: ${it.replyCount}", ChipTone.INFO)
                    it.lastReplyAt?.let { time -> BadgeChip(time.asUiTime(), ChipTone.INFO) }
                }
            }
            state.error?.let { StatusBanner(it, ChipTone.ERROR) }
        }
        Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.messages.isEmpty()) {
                    item { Text("Пока нет ответов в этом треде.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(state.messages) { message ->
                        Card {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(message.body)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BadgeChip(message.deliveryStatus.asUiText(), message.deliveryStatus.asTone())
                                    Text(message.createdAt.asUiTime(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        SectionCard(title = "Ответ в тред") {
            OutlinedTextField(
                value = state.draft,
                onValueChange = store::updateDraft,
                label = { Text("Введите ответ") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { store.sendReply() } }) { Text("Отправить") }
                OutlinedButton(onClick = { scope.launch { store.refresh() } }) { Text("Обновить") }
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
                    label = { Text("ID получателя") },
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
            SectionCard(title = "Новый звонок", subtitle = "Аудио, видео и групповые звонки через backend-контракт") {
                OutlinedTextField(
                    value = state.targetPeerId,
                    onValueChange = store::updateTargetPeerId,
                    label = { Text("ID контакта") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.groupTargets,
                    onValueChange = store::updateGroupTargets,
                    label = { Text("ID участников (через запятую)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.roomTitle,
                    onValueChange = store::updateRoomTitle,
                    label = { Text("Название комнаты (опционально)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { store.startCall() } }) { Text("Аудио 1:1") }
                    OutlinedButton(onClick = { scope.launch { store.startVideoCall() } }) { Text("Видео 1:1") }
                    OutlinedButton(onClick = { scope.launch { store.startCallLegacy() } }) { Text("Базовый") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { scope.launch { store.startGroupAudioCall() } }) { Text("Группа аудио") }
                    OutlinedButton(onClick = { scope.launch { store.startGroupVideoCall() } }) { Text("Группа видео") }
                }
                state.message?.let { StatusBanner(it, ChipTone.SUCCESS) }
                state.error?.let { StatusBanner(it, ChipTone.ERROR) }
            }
        }
        item {
            SectionCard(title = "Входящие") {
                if (state.incomingCalls.isEmpty()) {
                    Text("Входящих звонков нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.incomingCalls.sortedByDescending { it.updatedAt }.forEach { call ->
                        CallRow(
                            call = call,
                            participants = state.participantsByCall[call.callId].orEmpty(),
                            mediaState = state.mediaByCall[call.callId],
                            mediaStats = state.mediaStatsByCall[call.callId],
                            lastEvent = state.eventsByCall[call.callId].orEmpty().lastOrNull()?.eventType?.name,
                            onAccept = { scope.launch { store.accept(call) } },
                            onReject = { scope.launch { store.reject(call) } },
                            onJoin = { scope.launch { store.join(call) } },
                            onLeave = { scope.launch { store.leave(call) } },
                            onQuality = { scope.launch { store.sendQuality(call) } },
                            onToggleMicrophone = { scope.launch { store.toggleMicrophone(call) } },
                            onToggleCamera = { scope.launch { store.toggleCamera(call) } },
                            onSwitchCamera = { scope.launch { store.switchCamera(call) } },
                            onHangup = { scope.launch { store.hangup(call) } },
                        )
                    }
                }
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
                            participants = state.participantsByCall[call.callId].orEmpty(),
                            mediaState = state.mediaByCall[call.callId],
                            mediaStats = state.mediaStatsByCall[call.callId],
                            lastEvent = state.eventsByCall[call.callId].orEmpty().lastOrNull()?.eventType?.name,
                            onAccept = { scope.launch { store.accept(call) } },
                            onReject = { scope.launch { store.reject(call) } },
                            onJoin = { scope.launch { store.join(call) } },
                            onLeave = { scope.launch { store.leave(call) } },
                            onQuality = { scope.launch { store.sendQuality(call) } },
                            onToggleMicrophone = { scope.launch { store.toggleMicrophone(call) } },
                            onToggleCamera = { scope.launch { store.toggleCamera(call) } },
                            onSwitchCamera = { scope.launch { store.switchCamera(call) } },
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
    participants: List<org.expert.link.mesh.contract.model.MeshCallParticipant>,
    mediaState: org.expert.link.mesh.contract.model.MeshCallMediaState?,
    mediaStats: org.expert.link.mesh.contract.model.MeshMediaStats?,
    lastEvent: String?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onQuality: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onHangup: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(call.callId.shortId(12), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BadgeChip(text = call.status.asUiText(), tone = call.status.asTone())
                BadgeChip(text = if (call.callType.name == "VIDEO") "Видео" else "Аудио", tone = ChipTone.INFO)
                BadgeChip(text = if (call.callScope.name == "GROUP") "Группа" else "1:1", tone = ChipTone.INFO)
                BadgeChip(text = call.updatedAt.asUiTime(), tone = ChipTone.INFO)
            }
            InfoRow("С кем", call.recipientPeerId.shortId(10))
            InfoRow("Участники", participants.size.toString())
            lastEvent?.let { InfoRow("Последнее событие", it) }
            call.qualitySnapshot?.let {
                InfoRow("RTT", "${it.rttMs} мс")
                InfoRow("Потери", "${it.packetLossPercent}%")
            }
            mediaState?.let {
                InfoRow("Микрофон", if (it.localAudioEnabled) "Включён" else "Выключен")
                InfoRow("Камера", if (it.localVideoEnabled) "Включена" else "Выключена")
                InfoRow("Медиа", it.connectionState.name)
            }
            mediaStats?.let {
                InfoRow("RTT медиа", "${it.rttMs} мс")
                InfoRow("Входящий поток", "${it.inboundBitrateKbps} кбит/с")
                InfoRow("Исходящий поток", "${it.outboundBitrateKbps} кбит/с")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (call.status.name in setOf("RINGING", "INVITED", "NEW")) {
                    FilledTonalButton(onClick = onAccept) { Text("Принять") }
                    OutlinedButton(onClick = onReject) { Text("Отклонить") }
                }
                if (call.callScope.name == "GROUP" && call.status.name in setOf("INCOMING", "RINGING", "ACCEPTED", "CONNECTING", "CONNECTED", "ACTIVE")) {
                    OutlinedButton(onClick = onJoin) { Text("Войти") }
                    OutlinedButton(onClick = onLeave) { Text("Выйти") }
                }
                OutlinedButton(onClick = onQuality) { Text("Проверить") }
                OutlinedButton(onClick = onToggleMicrophone) { Text(if (mediaState?.localAudioEnabled == false) "Вкл. микрофон" else "Выкл. микрофон") }
                OutlinedButton(onClick = onToggleCamera) { Text(if (mediaState?.localVideoEnabled == false) "Вкл. камеру" else "Выкл. камеру") }
                OutlinedButton(onClick = onSwitchCamera) { Text("Сменить камеру") }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { scope.launch { store.refresh() } }) { Text("Обновить") }
                    OutlinedButton(onClick = { scope.launch { store.forceTopologyRefresh() } }) { Text("Пересобрать сеть") }
                }
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
                                state.relayMode?.let { InfoRow("Режим relay", it.asUiText()) }
                                state.topology?.let {
                                    InfoRow("Связность", it.connectivityMode.asUiText())
                                    InfoRow("Роль", it.networkRoleState.localRole.asUiText())
                                    InfoRow("Сбой сети", if (it.continuityDegraded) "Есть" else "Нет")
                                }
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
                                        state.routeHealth.firstOrNull { it.targetPeerId == route.targetPeerId }?.let { health ->
                                            Text(
                                                "Состояние: ${health.state.asUiText()} (ошибок: ${health.failureCount})",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
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
                                state.topology?.let { topology ->
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    InfoRow("Хост", topology.networkRoleState.currentHostPeerId?.shortId(12) ?: "не выбран")
                                    InfoRow("Failover", if (topology.networkRoleState.failoverInProgress) "в процессе" else "нет")
                                    InfoRow("Непрерывность", if (topology.continuityDegraded) "деградация" else "стабильно")
                                }
                            }
                        },
                    )
                }
            }
            DiagnosticsSection.SECURITY -> {
                item {
                    SectionCard(title = "Инциденты безопасности") {
                        val incidents = state.events.filter { it.category == MeshEventCategory.SECURITY }
                        if (incidents.isEmpty()) {
                            Text("Инцидентов не зафиксировано.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            incidents.forEachIndexed { index, event ->
                                EventRow(event)
                                if (index < incidents.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
            DiagnosticsSection.TOPOLOGY -> {
                item {
                    SectionCard(title = "Топология и роли") {
                        val topology = state.topology
                        if (topology == null) {
                            Text("Снимок топологии недоступен.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            InfoRow("Роль узла", topology.networkRoleState.localRole.asUiText())
                            InfoRow("Текущий хост", topology.networkRoleState.currentHostPeerId?.shortId(12) ?: "не выбран")
                            InfoRow("Failover", if (topology.networkRoleState.failoverInProgress) "в процессе" else "нет")
                            InfoRow("Связность", topology.connectivityMode.asUiText())
                            InfoRow("Relay режим", topology.relayMode.asUiText())
                            InfoRow("Ожидают ACK", topology.pendingAckCount.toString())
                            InfoRow("В очереди", topology.queuedPacketCount.toString())
                            InfoRow("Передачи", topology.activeFileTransfers.toString())
                            InfoRow("Звонки", topology.activeCallSessions.toString())
                            if (topology.recentEvents.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                topology.recentEvents.take(8).forEach { event ->
                                    InfoRow(event.eventType.name, event.message)
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
                            Text("ID узла")
                            MonospaceValue(profile.peerId)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (services.capabilities.canCopyText) {
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            services.copyText("ID узла", profile.peerId)
                                                .onSuccess { onShowMessage("ID узла скопирован") }
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
                            OutlinedButton(
                                onClick = { onShowMessage("Откройте сканер на втором устройстве") },
                                enabled = services.capabilities.canScanQr,
                            ) { Text("Сканировать") }
                        }
                        if (state.invite.isBlank()) {
                            Text("Создайте приглашение, чтобы поделиться им с другим устройством.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            MonospaceValue(state.invite)
                            if (!services.capabilities.canScanQr) {
                                Text("Если сканер недоступен, передайте строку вручную или через копирование.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
