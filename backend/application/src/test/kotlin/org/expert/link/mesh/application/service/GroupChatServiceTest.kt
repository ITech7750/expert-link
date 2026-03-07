package org.expert.link.mesh.application.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.messaging.ChatMember
import org.expert.link.mesh.domain.model.messaging.ChatMemberRole
import org.expert.link.mesh.domain.model.messaging.ChatType
import org.expert.link.mesh.domain.model.messaging.Conversation
import org.expert.link.mesh.domain.model.security.CryptoMaterialRef
import org.expert.link.mesh.domain.model.security.CryptoStorageType
import org.expert.link.mesh.infrastructure.repository.InMemoryChatMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryConversationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryEventLogRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryGroupChatRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryGroupEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryMessageRepositoryAdapter
import org.junit.jupiter.api.Test

class GroupChatServiceTest {
    @Test
    fun `should create group and record creation event`() = runTest {
        val localProfileService = mockk<LocalProfileService>()
        coEvery { localProfileService.require() } returns localProfile("alice", "alice-id")
        val chatMessagingService = mockk<ChatMessagingService>()
        val createdConversation = groupConversation(
            chatId = "chat-1",
            title = "Команда",
            members = listOf(
                ChatMember("alice-id", "Alice", ChatMemberRole.OWNER, Instant.parse("2026-01-01T00:00:00Z")),
                ChatMember("bob-id", "Bob", ChatMemberRole.MEMBER, Instant.parse("2026-01-01T00:00:00Z")),
            ),
        )
        coEvery {
            chatMessagingService.createGroupConversation("Команда", "Описание", setOf("bob-id"))
        } returns createdConversation

        val service = GroupChatService(
            localProfileService = localProfileService,
            conversationRepositoryPort = InMemoryConversationRepositoryAdapter(),
            messageRepositoryPort = InMemoryMessageRepositoryAdapter(),
            groupChatRepositoryPort = InMemoryGroupChatRepositoryAdapter(),
            chatMemberRepositoryPort = InMemoryChatMemberRepositoryAdapter(),
            groupEventRepositoryPort = InMemoryGroupEventRepositoryAdapter(),
            chatMessagingService = chatMessagingService,
            eventLogService = EventLogService(InMemoryEventLogRepositoryAdapter()),
            nodeMetricsService = NodeMetricsService(),
        )

        val group = service.createGroupChat("Команда", "Описание", setOf("bob-id"))
        val events = service.groupEvents(group.chatId, 10)

        assertThat(group.chatId).isEqualTo("chat-1")
        assertThat(group.members).hasSize(2)
        assertThat(events).hasSize(1)
        assertThat(events.first().eventType.name).isEqualTo("GROUP_CREATED")
    }

    @Test
    fun `should add participant and append member added event`() = runTest {
        val localProfileService = mockk<LocalProfileService>()
        coEvery { localProfileService.require() } returns localProfile("alice", "alice-id")
        val chatMessagingService = mockk<ChatMessagingService>()
        val existingConversation = groupConversation(
            chatId = "chat-2",
            title = "Проект",
            members = listOf(
                ChatMember("alice-id", "Alice", ChatMemberRole.OWNER, Instant.parse("2026-01-01T00:00:00Z")),
                ChatMember("bob-id", "Bob", ChatMemberRole.MEMBER, Instant.parse("2026-01-01T00:00:00Z")),
            ),
        )
        val updatedConversation = existingConversation.copy(
            members = existingConversation.members + ChatMember(
                "carol-id",
                "Carol",
                ChatMemberRole.MEMBER,
                Instant.parse("2026-01-02T00:00:00Z"),
            ),
            participantPeerIds = existingConversation.participantPeerIds + "carol-id",
            updatedAt = Instant.parse("2026-01-02T00:00:00Z"),
        )
        coEvery { chatMessagingService.addParticipant("chat-2", any()) } returns updatedConversation
        coEvery { chatMessagingService.createGroupConversation(any(), any(), any()) } returns existingConversation

        val service = GroupChatService(
            localProfileService = localProfileService,
            conversationRepositoryPort = InMemoryConversationRepositoryAdapter(),
            messageRepositoryPort = InMemoryMessageRepositoryAdapter(),
            groupChatRepositoryPort = InMemoryGroupChatRepositoryAdapter(),
            chatMemberRepositoryPort = InMemoryChatMemberRepositoryAdapter(),
            groupEventRepositoryPort = InMemoryGroupEventRepositoryAdapter(),
            chatMessagingService = chatMessagingService,
            eventLogService = EventLogService(InMemoryEventLogRepositoryAdapter()),
            nodeMetricsService = NodeMetricsService(),
        )
        service.createGroupChat("Проект", null, setOf("bob-id"))

        val group = service.addParticipants(
            "chat-2",
            listOf(org.expert.link.mesh.domain.model.identity.PeerIdentity("carol-id", "Carol", "pk")),
        )
        val events = service.groupEvents("chat-2", 20)

        assertThat(group).isNotNull()
        assertThat(group?.members?.map { it.peerId }).contains("carol-id")
        assertThat(events.any { it.eventType.name == "MEMBER_ADDED" }).isTrue()
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

    private fun groupConversation(chatId: String, title: String, members: List<ChatMember>): Conversation = Conversation(
        conversationId = chatId,
        chatType = ChatType.GROUP,
        title = title,
        description = null,
        createdByPeerId = members.first().peerId,
        participantPeerIds = members.map { it.peerId }.toSet(),
        members = members,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastMessageId = null,
    )
}
