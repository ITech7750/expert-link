package org.expert.link.app.shared.screen.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.expert.link.app.shared.ui.components.ChipTone
import org.expert.link.app.shared.ui.components.InfoRow
import org.expert.link.app.shared.ui.components.InlineQrScanner
import org.expert.link.app.shared.ui.components.SectionCard
import org.expert.link.app.shared.ui.components.StatusBanner
import org.expert.link.app.shared.ui.components.asUiText
import org.expert.link.app.shared.ui.components.asUiTime
import org.expert.link.app.shared.ui.components.shortId
import org.expert.link.mesh.contract.model.MeshInventoryScanResultStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InventoryScannerScreen(component: InventoryScannerComponent) {
    val state by component.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var commentDraft by remember(state.result?.inventoryItemId) { mutableStateOf("") }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            component.clearNotifications()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            component.clearNotifications()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сканировать") },
                navigationIcon = {
                    IconButton(onClick = component::goBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (!component.capabilities.canScanQr) {
                        IconButton(onClick = component::scanViaSystem) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Выбрать изображение с кодом")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        BoxWithConstraints(
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
            val scannerHeight = when {
                maxHeight < 700.dp -> maxHeight * 0.40f
                maxHeight < 900.dp -> maxHeight * 0.48f
                else -> maxHeight * 0.58f
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SectionCard(title = "Камера", subtitle = "Наведите рамку на QR-код или штрихкод") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(scannerHeight)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(28.dp),
                                ),
                        ) {
                            if (component.capabilities.canScanQr) {
                                InlineQrScanner(
                                    modifier = Modifier.fillMaxSize(),
                                    enabled = true,
                                    onCodeScanned = { code ->
                                        component.updateCode(code)
                                        component.lookupQr()
                                    },
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                                ) {
                                    Text("Сканирование камерой недоступно на этой платформе. Выберите изображение с QR или штрихкодом.")
                                    Button(onClick = component::scanViaSystem, modifier = Modifier.fillMaxWidth()) {
                                        Text("Выбрать изображение с кодом")
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.72f)
                                        .height(scannerHeight * 0.42f)
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                            shape = RoundedCornerShape(24.dp),
                                        ),
                                )
                            }
                        }
                    }
                }
                item {
                    SectionCard(title = "Результат сканирования", subtitle = "Поиск и действия по найденному объекту") {
                        OutlinedTextField(
                            value = state.code,
                            onValueChange = component::updateCode,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("QR или штрихкод") },
                            singleLine = true,
                        )
                        Button(onClick = component::lookupQr, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                            Text("Найти объект")
                        }
                        OutlinedButton(onClick = component::lookupBarcode, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                            Text("Искать как штрихкод")
                        }
                        OutlinedButton(onClick = component::clearResult, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                            Text("Очистить")
                        }
                        if (state.isLoading) {
                            StatusBanner("Идёт поиск объекта...", ChipTone.INFO)
                        }
                        val resolution = state.resolution
                        if (resolution == null) {
                            StatusBanner("Наведите камеру на код или введите значение вручную", ChipTone.INFO)
                            return@SectionCard
                        }

                        StatusBanner(
                            text = resolution.status.asUiText(),
                            tone = when (resolution.status) {
                                MeshInventoryScanResultStatus.RESOLVED -> ChipTone.SUCCESS
                                MeshInventoryScanResultStatus.INACTIVE -> ChipTone.WARNING
                                MeshInventoryScanResultStatus.NOT_FOUND,
                                MeshInventoryScanResultStatus.INVALID,
                                MeshInventoryScanResultStatus.ERROR,
                                -> ChipTone.ERROR
                            },
                        )

                        resolution.codeType?.let { InfoRow("Тип кода", it.name) }
                        resolution.code?.let { code ->
                            InfoRow("Код", code.rawValue)
                            InfoRow("Статус кода", if (code.isActive) "Активен" else "Неактивен")
                        }
                        resolution.scanEvent?.let { event ->
                            InfoRow("Сканировал", event.scannedByPeerId.shortId(10))
                            InfoRow("Время", event.scannedAt.asUiTime())
                        }

                        val result = state.result
                        if (result == null) {
                            return@SectionCard
                        }
                        Text(result.title, style = MaterialTheme.typography.titleMedium)
                        Text("№ ${result.inventoryNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        result.locationId?.let { InfoRow("Локация", it.shortId(14)) }
                        result.responsiblePerson?.let { InfoRow("МОЛ", it) }
                        InfoRow("Статус", result.currentStatus.asUiText())
                        result.nextInventoryAt?.let { InfoRow("Следующая инвентаризация", it.asUiTime()) }

                        Button(onClick = { component.openItem(result.inventoryItemId) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Открыть карточку объекта")
                        }
                        OutlinedButton(onClick = component::markChecked, modifier = Modifier.fillMaxWidth()) {
                            Text("Отметить проверку")
                        }
                        OutlinedButton(onClick = component::openDiscussion, modifier = Modifier.fillMaxWidth()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null)
                                Text("Открыть обсуждение")
                            }
                        }
                        OutlinedButton(onClick = component::addPhoto, modifier = Modifier.fillMaxWidth()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                                Text("Добавить фото")
                            }
                        }
                        OutlinedTextField(
                            value = commentDraft,
                            onValueChange = { commentDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Комментарий") },
                            minLines = 2,
                        )
                        Button(
                            onClick = {
                                component.addComment(commentDraft)
                                commentDraft = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Сохранить комментарий")
                        }
                    }
                }
            }
        }
    }
}
