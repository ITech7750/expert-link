package org.expert.link.mesh.backend

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.contract.api.MeshCreateThreadCommand
import org.expert.link.mesh.contract.api.MeshCreateGroupChatCommand
import org.expert.link.mesh.contract.api.MeshSendMessageCommand
import org.expert.link.mesh.contract.api.MeshSendThreadMessageCommand
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.junit.jupiter.api.Test

class MeshBackendGroupThreadIntegrationTest {
    @Test
    fun `should create group chat and send thread reply via public contract`() {
        runBlocking {
            val alice = MeshBackend.launch(config("group-test-alice", 19681))
            val bob = MeshBackend.launch(config("group-test-bob", 19682))
            val carol = MeshBackend.launch(config("group-test-carol", 19683))
            try {
                connectBidirectional(alice, bob)
                connectBidirectional(alice, carol)
                connectBidirectional(bob, carol)
                pair(alice, bob)
                pair(alice, carol)

                val group = alice.createGroupChat(
                    MeshCreateGroupChatCommand(
                        title = "Проект",
                        description = "Рабочая группа",
                        participantPeerIds = setOf(bob.profile.peerId, carol.profile.peerId),
                    ),
                )
                val root = alice.sendMessage(MeshSendMessageCommand(chatId = group.conversationId, body = "Старт обсуждения"))
                delay(700)
                val thread = alice.createThread(
                    MeshCreateThreadCommand(
                        chatId = group.conversationId,
                        rootMessageId = root.messageId,
                    ),
                )
                alice.sendThreadReply(
                    MeshSendThreadMessageCommand(
                        chatId = group.conversationId,
                        rootMessageId = root.messageId,
                        body = "Первый комментарий в треде",
                    ),
                )
                delay(700)

                val summary = alice.threadSummary(group.conversationId, root.messageId)
                val threadMessages = alice.threadMessagesDetailed(group.conversationId, root.messageId)
                val threadUpdates = alice.threadUpdates(group.conversationId)
                val groupEvents = alice.groupEvents(group.conversationId, 20)
                val bobMessages = bob.groupMessages(group.conversationId)
                val bobGroups = bob.groupChats()
                val bobThreadUpdates = bob.threadUpdates(group.conversationId)

                assertThat(group.chatType.name).isEqualTo("GROUP")
                assertThat(group.members).hasSize(3)
                assertThat(thread.threadId).isNotBlank()
                assertThat(summary).isNotNull()
                assertThat(summary?.replyCount).isEqualTo(1)
                assertThat(threadMessages).hasSize(1)
                assertThat(threadMessages.first().rootMessageId).isEqualTo(root.messageId)
                assertThat(threadUpdates.any { it.rootMessageId == root.messageId }).isTrue()
                assertThat(groupEvents.any { it.eventType.name == "THREAD_CREATED" }).isTrue()
                assertThat(bobMessages.map { it.body }).contains("Старт обсуждения")
                assertThat(bobGroups.any { it.chatId == group.conversationId }).isTrue()
                assertThat(bobThreadUpdates.any { it.rootMessageId == root.messageId }).isTrue()
            } finally {
                runCatching { alice.stop() }
                runCatching { bob.stop() }
                runCatching { carol.stop() }
            }
        }
    }

    private suspend fun pair(left: org.expert.link.mesh.contract.api.MeshNode, right: org.expert.link.mesh.contract.api.MeshNode) {
        val invite = right.createPairingInvite()
        left.pairWithInvite(invite)
        delay(500)
    }

    private suspend fun connectBidirectional(left: org.expert.link.mesh.contract.api.MeshNode, right: org.expert.link.mesh.contract.api.MeshNode) {
        left.rememberPeerEndpoint(right.profile.peerId, right.endpoint)
        right.rememberPeerEndpoint(left.profile.peerId, left.endpoint)
    }

    private fun config(name: String, port: Int): MeshNodeConfig = MeshNodeConfig(
        displayName = name,
        bindHost = "127.0.0.1",
        httpPort = port,
        discoveryPort = port + 1_000,
        multicastGroup = "239.30.30.30",
        featureFlags = MeshFeatureFlags(
            discoveryEnabled = false,
            relayEnabled = false,
            inMemoryTransport = true,
            inMemoryDiscovery = true,
        ),
    )
}
