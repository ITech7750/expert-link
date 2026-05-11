package org.expert.link.mesh.application.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.messaging.ChatMember
import org.expert.link.mesh.domain.model.messaging.ChatMemberRole
import org.expert.link.mesh.domain.model.messaging.GroupChat
import org.expert.link.mesh.domain.model.messaging.ChatThread
import org.junit.jupiter.api.Test

class InventoryDiscussionServiceTest {
    @Test
    fun `should create session chat and item thread`() = runTest {
        val groupChatService = mockk<GroupChatService>()
        val threadService = mockk<ThreadService>()
        val inventoryDiscussionService = InventoryDiscussionService(groupChatService, threadService)

        coEvery {
            groupChatService.createGroupChat("Session", null, setOf("peer-1"))
        } returns GroupChat(
            chatId = "chat-1",
            title = "Session",
            description = null,
            createdByPeerId = "peer-1",
            members = listOf(ChatMember("peer-1", "Alice", ChatMemberRole.OWNER, Instant.parse("2026-01-01T00:00:00Z"))),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        coEvery { threadService.createThread("chat-1", "msg-1") } returns ChatThread(
            threadId = "thread-1",
            chatId = "chat-1",
            rootMessageId = "msg-1",
            rootSenderPeerId = "peer-1",
            createdByPeerId = "peer-1",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            replyCount = 0,
            participantPeerIds = setOf("peer-1"),
        )

        val chatId = inventoryDiscussionService.createSessionChat("Session", null, setOf("peer-1"))
        val threadId = inventoryDiscussionService.createItemThread(chatId, "msg-1")

        assertThat(chatId).isEqualTo("chat-1")
        assertThat(threadId).isEqualTo("thread-1")
    }
}
