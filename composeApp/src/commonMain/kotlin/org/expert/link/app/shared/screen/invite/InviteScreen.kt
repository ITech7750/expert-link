package org.expert.link.app.shared.screen.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.expert.link.app.shared.platform.QrCodeMatrix
import org.expert.link.app.shared.ui.components.InlineQrScanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteScreen(component: InviteComponent) {
    val state by component.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val qrCode = remember(state.invite) { component.buildQrCode() }
    val scrollState = rememberScrollState()

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
                title = { Text("Контакт") },
                navigationIcon = {
                    IconButton(onClick = component::goBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                InviteTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { component.selectTab(tab) },
                        text = { Text(if (tab == InviteTab.CREATE) "QR" else "Сканировать") },
                    )
                }
            }

            when (state.selectedTab) {
                InviteTab.CREATE -> {
                    Surface(
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Button(
                                onClick = component::createInvite,
                                enabled = !state.isCreating,
                                modifier = if (component.capabilities.prefersWideLayout) {
                                    Modifier.align(Alignment.CenterHorizontally)
                                } else {
                                    Modifier.fillMaxWidth()
                                },
                            ) {
                                Text(if (state.isCreating) "Создание..." else "Создать QR")
                            }
                            if (state.invite.isNotBlank()) {
                                TextButton(onClick = component::copyInvite) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                                    Text("Копировать", modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (qrCode != null) {
                                InviteQrPreview(matrix = qrCode)
                            } else {
                                Text(
                                    text = "Создайте приглашение",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                InviteTab.SCAN -> {
                    Surface(
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            if (component.capabilities.canScanQr) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 320.dp)
                                        .aspectRatio(1f),
                                ) {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = MaterialTheme.shapes.extraLarge,
                                        tonalElevation = 2.dp,
                                    ) {
                                        InlineQrScanner(
                                            modifier = Modifier.fillMaxSize(),
                                            enabled = state.scanInput.isBlank(),
                                            onCodeScanned = component::applyScannedInvite,
                                        )
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(220.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                        shape = MaterialTheme.shapes.extraLarge,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                        ),
                                    ) {}
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = MaterialTheme.shapes.extraLarge,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Сканер недоступен",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = state.scanInput,
                                onValueChange = component::updateScanInput,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Приглашение") },
                                singleLine = true,
                                maxLines = 1,
                            )

                            Button(
                                onClick = component::pair,
                                enabled = !state.isPairing && state.scanInput.isNotBlank(),
                            ) {
                                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                                Text(
                                    text = if (state.isPairing) "Добавление..." else "Добавить",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }

                            if (state.scanInput.isNotBlank()) {
                                TextButton(onClick = { component.updateScanInput("") }) {
                                    Text("Очистить")
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
private fun InviteQrPreview(
    matrix: QrCodeMatrix,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 320.dp)
            .aspectRatio(1f)
            .background(Color.White, shape = MaterialTheme.shapes.extraLarge)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
        ) {
            val moduleSize = size.minDimension / matrix.size.toFloat()
            for (y in 0 until matrix.size) {
                for (x in 0 until matrix.size) {
                    if (!matrix.isDark(x, y)) continue
                    drawRect(
                        color = Color(0xFF111111),
                        topLeft = Offset(x * moduleSize, y * moduleSize),
                        size = Size(moduleSize, moduleSize),
                    )
                }
            }
        }
    }
}
