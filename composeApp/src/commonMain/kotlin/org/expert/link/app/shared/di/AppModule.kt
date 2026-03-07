package org.expert.link.app.shared.di

import com.arkivanov.decompose.ComponentContext
import org.expert.link.app.shared.navigation.BaseComponent
import org.expert.link.app.shared.navigation.Config
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.presentation.NodeSessionController
import org.expert.link.app.shared.screen.chat.ChatComponent
import org.expert.link.app.shared.screen.chatlist.ChatListComponent
import org.expert.link.app.shared.screen.invite.InviteComponent
import org.expert.link.app.shared.screen.profile.ProfileComponent
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin

fun appModule(services: AppPlatformServices) = module {
    single { services }
    single { NodeSessionController(get()) }

    factory { params ->
        ChatListComponent(
            context = params.get(),
            onNavigate = params.get(),
            onBack = params.get(),
        )
    }

    factory { params ->
        ChatComponent(
            context = params[0],
            onNavigate = params[1],
            onBack = params[2],
            conversationId = params[3],
            initialPeerId = params[4],
            initialTitle = params[5],
        )
    }

    factory { params ->
        InviteComponent(
            context = params.get(),
            onNavigate = params.get(),
            onBack = params.get(),
        )
    }

    factory { params ->
        ProfileComponent(
            context = params.get(),
            onNavigate = params.get(),
            onBack = params.get(),
        )
    }
}

fun initAppKoin(services: AppPlatformServices) {
    if (GlobalContext.getOrNull() != null) {
        return
    }
    org.koin.core.context.startKoin {
        modules(appModule(services))
    }
}

inline fun <reified T : BaseComponent> getComponent(vararg parameters: Any?) = getKoin().get<T> {
    parametersOf(*parameters)
}
