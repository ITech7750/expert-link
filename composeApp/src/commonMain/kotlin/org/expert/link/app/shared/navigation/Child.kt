package org.expert.link.app.shared.navigation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import org.expert.link.app.shared.di.getComponent
import org.expert.link.app.shared.screen.chat.ChatComponent
import org.expert.link.app.shared.screen.chat.ChatScreen
import org.expert.link.app.shared.screen.chatlist.ChatListComponent
import org.expert.link.app.shared.screen.chatlist.ChatListScreen
import org.expert.link.app.shared.screen.inventory.InventoryComponent
import org.expert.link.app.shared.screen.inventory.InventoryItemComponent
import org.expert.link.app.shared.screen.inventory.InventoryItemScreen
import org.expert.link.app.shared.screen.inventory.InventoryScannerComponent
import org.expert.link.app.shared.screen.inventory.InventoryScannerScreen
import org.expert.link.app.shared.screen.inventory.InventoryScreen
import org.expert.link.app.shared.screen.inventory.InventorySessionComponent
import org.expert.link.app.shared.screen.inventory.InventorySessionScreen
import org.expert.link.app.shared.screen.invite.InviteComponent
import org.expert.link.app.shared.screen.invite.InviteScreen
import org.expert.link.app.shared.screen.main.MainComponent
import org.expert.link.app.shared.screen.main.MainScreen
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

    data class Main(val component: MainComponent) : Child() {
        @Composable
        override fun Content() {
            MainScreen(component)
        }
    }

    data class Chat(val component: ChatComponent) : Child() {
        @Composable
        override fun Content() {
            ChatScreen(component)
        }
    }

    data class Inventory(val component: InventoryComponent) : Child() {
        @Composable
        override fun Content() {
            InventoryScreen(component)
        }
    }

    data class InventoryItem(val component: InventoryItemComponent) : Child() {
        @Composable
        override fun Content() {
            InventoryItemScreen(component)
        }
    }

    data class InventorySession(val component: InventorySessionComponent) : Child() {
        @Composable
        override fun Content() {
            InventorySessionScreen(component)
        }
    }

    data class InventoryScanner(val component: InventoryScannerComponent) : Child() {
        @Composable
        override fun Content() {
            InventoryScannerScreen(component)
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
            onReplaceCurrent: (Config) -> Unit,
            onBack: () -> Unit,
        ): Child = when (config) {
            Config.Main -> Main(
                getComponent(context, onNavigate, onBack),
            )
            Config.ChatList -> ChatList(
                getComponent(context, onNavigate, onBack),
            )

            is Config.Chat -> Chat(
                getComponent(
                    context,
                    onNavigate,
                    onBack,
                    config.conversationId,
                    config.peerId,
                    config.title,
                    config.threadRootMessageId,
                ),
            )

            Config.Inventory -> Inventory(
                getComponent(context, onNavigate, onBack),
            )

            is Config.InventoryItem -> InventoryItem(
                getComponent(context, onNavigate, onBack, config.itemId),
            )

            is Config.InventorySession -> InventorySession(
                getComponent(context, onNavigate, onBack, config.sessionId),
            )

            Config.InventoryScanner -> InventoryScanner(
                getComponent(context, onNavigate, onBack),
            )

            Config.Invite -> Invite(
                getComponent(context, onNavigate, onBack, onReplaceCurrent),
            )

            Config.Profile -> Profile(
                getComponent(context, onNavigate, onBack),
            )
        }
    }
}
