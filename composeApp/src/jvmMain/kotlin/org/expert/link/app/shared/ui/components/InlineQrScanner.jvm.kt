package org.expert.link.app.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun InlineQrScanner(
    modifier: Modifier,
    enabled: Boolean,
    onCodeScanned: (String) -> Unit,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (enabled) "Сканер недоступен" else "",
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
