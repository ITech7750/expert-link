package org.expert.link.app.shared.navigation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import org.expert.link.app.shared.di.getComponent
import org.expert.link.app.shared.screen.chat.ChatComponent
import org.expert.link.app.shared.screen.chat.ChatScreen
import org.expert.link.app.shared.screen.chatlist.ChatListComponent
import org.expert.link.app.shared.screen.chatlist.ChatListScreen
import org.expert.link.app.shared.screen.invite.InviteComponent
import org.expert.link.app.shared.screen.invite.InviteScreen
import org.expert.link.app.shared.screen.profile.ProfileComponent
import org.expert.link.app.shared.screen.profile.ProfileScreen

sealed class Child {
    @Composable
    abstract fun Content()

    data class ChatList(val component: ChatListComponent) : Child() {
        @Composable
        override fun Content() {
            ChatListScreen(component)
        }
    }

    data class Chat(val component: ChatComponent) : Child() {
        @Composable
        override fun Content() {
            ChatScreen(component)
        }
    }

    data class Invite(val component: InviteComponent) : Child() {
        @Composable
        override fun Content() {
            InviteScreen(component)
        }
    }

    data class Profile(val component: ProfileComponent) : Child() {
        @Composable
        override fun Content() {
            ProfileScreen(component)
        }
    }

    companion object {
        fun create(
            config: Config,
            context: ComponentContext,
            onNavigate: (Config) -> Unit,
            onBack: () -> Unit,
        ): Child = when (config) {
            Config.ChatList -> ChatList(
                getComponent(context, onNavigate, onBack),
            )

            is Config.Chat -> Chat(
                getComponent(context, onNavigate, onBack, config.conversationId, config.peerId, config.title),
            )

            Config.Invite -> Invite(
                getComponent(context, onNavigate, onBack),
            )

            Config.Profile -> Profile(
                getComponent(context, onNavigate, onBack),
            )
        }
    }
}
