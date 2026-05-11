package org.expert.link.app.shared.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Config {
    @Serializable
    data object Main : Config()

    @Serializable
    data object ChatList : Config()

    @Serializable
    data class Chat(
        val conversationId: String,
        val peerId: String? = null,
        val title: String = "Чат",
        val threadRootMessageId: String? = null,
    ) : Config()

    @Serializable
    data object Inventory : Config()

    @Serializable
    data class InventoryItem(
        val itemId: String,
    ) : Config()

    @Serializable
    data class InventorySession(
        val sessionId: String,
    ) : Config()

    @Serializable
    data object InventoryScanner : Config()

    @Serializable
    data object Invite : Config()

    @Serializable
    data object Profile : Config()
}
