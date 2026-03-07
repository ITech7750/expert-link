package org.expert.link.app.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import org.expert.link.app.shared.di.initAppKoin
import org.expert.link.app.shared.navigation.RootComponent
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.ui.ExpertLinkApp

/** Точка входа desktop-клиента. */
fun main(args: Array<String>) = application {
    val services = AppPlatformServices(args.toList())
    initAppKoin(services)
    val lifecycle = LifecycleRegistry()
    val rootComponent = RootComponent(DefaultComponentContext(lifecycle))
    val windowState = rememberWindowState(width = 1280.dp, height = 860.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "Expert Link",
        state = windowState,
    ) {
        ExpertLinkApp(rootComponent)
    }
}
