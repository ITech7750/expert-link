package org.expert.link.app.shared.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Маршруты приложения. */
sealed interface AppRoute {
    data object Loading : AppRoute
    data object Home : AppRoute
    data object Nearby : AppRoute
    data object Pairing : AppRoute
    data object Contacts : AppRoute
    data object Chats : AppRoute
    data class Dialog(val conversationId: String, val peerId: String) : AppRoute
    data object Transfers : AppRoute
    data object Call : AppRoute
    data object Diagnostics : AppRoute
    data object Profile : AppRoute
    data object Settings : AppRoute
}

/** Основные вкладки нижней навигации. */
enum class PrimaryDestination(
    val title: String,
    val route: AppRoute,
) {
    HOME("Главная", AppRoute.Home),
    CHATS("Чаты", AppRoute.Chats),
    TRANSFERS("Передачи", AppRoute.Transfers),
    PROFILE("Профиль", AppRoute.Profile),
}

/** Простой стек навигации без зависимости на platform-specific nav libraries. */
class AppNavigator(
    start: AppRoute = AppRoute.Loading,
) {
    private val _backStack = MutableStateFlow(listOf(start))
    val backStack: StateFlow<List<AppRoute>> = _backStack.asStateFlow()

    val current: AppRoute
        get() = _backStack.value.last()

    fun replace(route: AppRoute) {
        _backStack.value = listOf(route)
    }

    fun push(route: AppRoute) {
        _backStack.update { it + route }
    }

    fun pop() {
        _backStack.update { stack -> if (stack.size > 1) stack.dropLast(1) else stack }
    }

    fun switchPrimary(destination: PrimaryDestination) {
        replace(destination.route)
    }
}

fun AppRoute.title(): String = when (this) {
    AppRoute.Loading -> "Загрузка"
    AppRoute.Home -> "Главная"
    AppRoute.Nearby -> "Узлы рядом"
    AppRoute.Pairing -> "Сопряжение"
    AppRoute.Contacts -> "Контакты"
    AppRoute.Chats -> "Чаты"
    is AppRoute.Dialog -> "Диалог"
    AppRoute.Transfers -> "Передачи"
    AppRoute.Call -> "Звонок"
    AppRoute.Diagnostics -> "Диагностика"
    AppRoute.Profile -> "Профиль"
    AppRoute.Settings -> "Настройки"
}

fun AppRoute.primaryDestination(): PrimaryDestination? = when (this) {
    AppRoute.Home, AppRoute.Nearby, AppRoute.Pairing, AppRoute.Contacts, AppRoute.Diagnostics, AppRoute.Settings -> PrimaryDestination.HOME
    AppRoute.Chats, is AppRoute.Dialog -> PrimaryDestination.CHATS
    AppRoute.Transfers, AppRoute.Call -> PrimaryDestination.TRANSFERS
    AppRoute.Profile -> PrimaryDestination.PROFILE
    AppRoute.Loading -> null
}
