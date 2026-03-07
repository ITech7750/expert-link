package org.expert.link.app.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun InlineQrScanner(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCodeScanned: (String) -> Unit,
)
