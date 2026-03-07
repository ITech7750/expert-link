package org.expert.link.app.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.arkivanov.decompose.extensions.compose.stack.Children
import org.expert.link.app.shared.navigation.RootComponent
import org.expert.link.app.shared.ui.theme.AppTheme

@Composable
fun ExpertLinkApp(rootComponent: RootComponent) {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest,
                            androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        ) {
            Children(stack = rootComponent.childStack) { child ->
                child.instance.Content()
            }
        }
    }
}
