package org.expert.link.app.shared.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.replaceCurrent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.presentation.NodeSessionController
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RootComponent(
    componentContext: ComponentContext,
) : ComponentContext by componentContext, KoinComponent {
    private val session: NodeSessionController by inject()
    private val componentScope = CoroutineScope(SupervisorJob() + Default).withLifecycle(lifecycle)
    private val navigation = StackNavigation<Config>()

    val childStack = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Main,
        handleBackButton = true,
        childFactory = { config, context ->
            Child.create(
                config = config,
                context = context,
                onNavigate = ::onNavigate,
                onReplaceCurrent = ::onReplaceCurrent,
                onBack = ::onBack,
            )
        },
    )

    init {
        componentScope.launch {
            if (session.state.value.status == NodeRuntimeStatus.STOPPED) {
                session.start()
            }
        }
    }

    fun onNavigate(config: Config) {
        navigation.bringToFront(config)
    }

    fun onReplaceCurrent(config: Config) {
        navigation.replaceCurrent(config)
    }

    fun onBack() {
        navigation.pop()
    }
}
