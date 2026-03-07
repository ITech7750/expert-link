package org.expert.link.app.shared.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Config {
    @Serializable
    data object ChatList : Config()

    @Serializable
    data class Chat(
        val conversationId: String,
        val peerId: String? = null,
        val title: String = "Чат",
    ) : Config()

    @Serializable
    data object Invite : Config()

    @Serializable
    data object Profile : Config()
}
