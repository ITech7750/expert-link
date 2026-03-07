package org.expert.link.app.shared.data

import kotlinx.coroutines.flow.Flow
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshConversation

interface ChatRepository {
    fun observeConversations(): Flow<List<MeshConversation>>

    fun observeMessages(conversationId: String): Flow<List<MeshChatMessage>>

    suspend fun sendMessage(conversationId: String, text: String): Result<MeshChatMessage>

    suspend fun loadMoreMessages(conversationId: String, beforeMessageId: String): Result<List<MeshChatMessage>>

    suspend fun markAsRead(conversationId: String, messageIds: List<String>)

    suspend fun createNewChat(peerIds: List<String>, initialMessage: String? = null): Result<MeshConversation>

    suspend fun getConversation(conversationId: String): Flow<MeshConversation?>
}