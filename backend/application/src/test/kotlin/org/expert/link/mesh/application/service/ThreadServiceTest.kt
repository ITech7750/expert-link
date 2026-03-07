package org.expert.link.mesh.application.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.messaging.ChatMember
import org.expert.link.mesh.domain.model.messaging.ChatMemberRole
import org.expert.link.mesh.domain.model.messaging.ChatMessage
import org.expert.link.mesh.domain.model.messaging.ChatType
import org.expert.link.mesh.domain.model.messaging.Conversation
import org.expert.link.mesh.domain.model.messaging.MessageDeliveryStatus
import org.expert.link.mesh.domain.model.messaging.MessageType
import org.expert.link.mesh.domain.model.security.CryptoMaterialRef
import org.expert.link.mesh.domain.model.security.CryptoStorageType
import org.expert.link.mesh.infrastructure.repository.InMemoryConversationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryEventLogRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryGroupEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryMessageRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryThreadMessageRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryThreadRepositoryAdapter
import org.junit.jupiter.api.Test

class ThreadServiceTest {
    @Test
    fun `should create thread and update reply count after sending message`() = runTest {
        val localProfileService = mockk<LocalProfileService>()
        coEvery { localProfileService.require() } returns localProfile("alice", "alice-id")
        val chatMessagingService = mockk<ChatMessagingService>()
        val rootMessage = ChatMessage(
            messageId = "message-root",
            conversationId = "chat-1",
            senderPeerId = "bob-id",
            recipientPeerId = "group:chat-1",
            body = "Root message",
            messageType = MessageType.TEXT,
            deliveryStatus = MessageDeliveryStatus.DELIVERED,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            deliveredAt = Instant.parse("2026-01-01T00:00:01Z"),
        )
        val replyMessage = ChatMessage(
            messageId = "message-reply",
            conversationId = "chat-1",
            senderPeerId = "alice-id",
            recipientPeerId = "group:chat-1",
            body = "Reply",
            messageType = MessageType.TEXT,
            threadRootMessageId = "message-root",
            replyToMessageId = "message-root",
            deliveryStatus = MessageDeliveryStatus.ACK_PENDING,
            createdAt = Instant.parse("2026-01-01T00:01:00Z"),
        )
        coEvery { chatMessagingService.message("message-root") } returns rootMessage
        coEvery { chatMessagingService.sendThreadMessage("chat-1", "message-root", "Reply", null) } returns replyMessage
        coEvery { chatMessagingService.threadMessages(any(), any()) } returns emptyList()

        val conversationRepository = InMemoryConversationRepositoryAdapter()
        conversationRepository.save(groupConversation("chat-1"))
        val messageRepository = InMemoryMessageRepositoryAdapter()

        val service = ThreadService(
            localProfileService = localProfileService,
            conversationRepositoryPort = conversationRepository,
            messageRepositoryPort = messageRepository,
            threadRepositoryPort = InMemoryThreadRepositoryAdapter(),
            threadMessageRepositoryPort = InMemoryThreadMessageRepositoryAdapter(),
            groupEventRepositoryPort = InMemoryGroupEventRepositoryAdapter(),
            chatMessagingService = chatMessagingService,
            eventLogService = EventLogService(InMemoryEventLogRepositoryAdapter()),
            nodeMetricsService = NodeMetricsService(),
        )

        val thread = service.createThread("chat-1", "message-root")
        val sent = service.sendThreadMessage("chat-1", "message-root", "Reply")
        val summary = service.threadSummaries("chat-1").first()
        val history = service.threadMessages("chat-1", "message-root")

        assertThat(thread.chatId).isEqualTo("chat-1")
        assertThat(sent.messageId).isEqualTo("message-reply")
        assertThat(summary.replyCount).isEqualTo(1)
        assertThat(history).hasSize(1)
        assertThat(history.first().body).isEqualTo("Reply")
    }

    @Test
    fun `should return fallback thread messages from chat service when thread metadata absent`() = runTest {
        val localProfileService = mockk<LocalProfileService>()
        coEvery { localProfileService.require() } returns localProfile("alice", "alice-id")
        val chatMessagingService = mockk<ChatMessagingService>()
        val inbound = ChatMessage(
            messageId = "message-1",
            conversationId = "chat-2",
            senderPeerId = "bob-id",
            recipientPeerId = "group:chat-2",
            body = "Reply from fallback",
            messageType = MessageType.TEXT,
            threadRootMessageId = "root-2",
            deliveryStatus = MessageDeliveryStatus.DELIVERED,
            createdAt = Instant.parse("2026-01-01T00:02:00Z"),
            deliveredAt = Instant.parse("2026-01-01T00:02:01Z"),
        )
        coEvery { chatMessagingService.threadMessages("chat-2", "root-2") } returns listOf(inbound)
        coEvery { chatMessagingService.threadSummary("chat-2", "root-2") } returns null
        coEvery { chatMessagingService.message(any()) } returns null
        coEvery { chatMessagingService.sendThreadMessage(any(), any(), any(), any()) } returns inbound
        val messageRepository = InMemoryMessageRepositoryAdapter()
        messageRepository.save(inbound)

        val service = ThreadService(
            localProfileService = localProfileService,
            conversationRepositoryPort = InMemoryConversationRepositoryAdapter(),
            messageRepositoryPort = messageRepository,
            threadRepositoryPort = InMemoryThreadRepositoryAdapter(),
            threadMessageRepositoryPort = InMemoryThreadMessageRepositoryAdapter(),
            groupEventRepositoryPort = InMemoryGroupEventRepositoryAdapter(),
            chatMessagingService = chatMessagingService,
            eventLogService = EventLogService(InMemoryEventLogRepositoryAdapter()),
            nodeMetricsService = NodeMetricsService(),
        )

        val messages = service.threadMessages("chat-2", "root-2")
        val summaries = service.threadSummaries("chat-2")

        assertThat(messages).hasSize(1)
        assertThat(messages.first().rootMessageId).isEqualTo("root-2")
        assertThat(messages.first().body).isEqualTo("Reply from fallback")
        assertThat(summaries).hasSize(1)
        assertThat(summaries.first().rootMessageId).isEqualTo("root-2")
    }

    private fun localProfile(displayName: String, peerId: String): LocalProfile = LocalProfile(
        peerId = peerId,
        displayName = displayName,
        publicKey = "public-$peerId",
        privateKey = "private-$peerId",
        keyMaterialRef = CryptoMaterialRef(
            alias = "alias-$peerId",
            storageType = CryptoStorageType.IN_MEMORY,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        ),
        capabilities = setOf("chat"),
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun groupConversation(chatId: String): Conversation = Conversation(
        conversationId = chatId,
        chatType = ChatType.GROUP,
        title = "Group",
        description = null,
        createdByPeerId = "alice-id",
        participantPeerIds = setOf("alice-id", "bob-id"),
        members = listOf(
            ChatMember("alice-id", "Alice", ChatMemberRole.OWNER, Instant.parse("2026-01-01T00:00:00Z")),
            ChatMember("bob-id", "Bob", ChatMemberRole.MEMBER, Instant.parse("2026-01-01T00:00:00Z")),
        ),
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
