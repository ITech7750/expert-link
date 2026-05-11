package org.expert.link.mesh.application.service

class InventoryDiscussionService(
    private val groupChatService: GroupChatService,
    private val threadService: ThreadService,
) {
    suspend fun createSessionChat(title: String, description: String?, participantPeerIds: Set<String>): String {
        return groupChatService.createGroupChat(title, description, participantPeerIds).chatId
    }

    suspend fun createItemThread(chatId: String, rootMessageId: String): String {
        return threadService.createThread(chatId, rootMessageId).threadId
    }
}
