package org.expert.link.app.shared.navigation

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.SupervisorJob

abstract class BaseComponent(
    val context: ComponentContext,
    val onNavigate: (Config) -> Unit,
    val onBack: () -> Unit,
    val onReplaceCurrent: (Config) -> Unit = onNavigate,
) : ComponentContext by context {

    protected val componentScope = CoroutineScope(SupervisorJob() + Default).withLifecycle(lifecycle)
}
