package org.expert.link.mesh.simulator.scenario

import org.expert.link.mesh.contract.api.MeshCreateGroupChatCommand
import org.expert.link.mesh.contract.api.MeshCreateThreadCommand
import org.expert.link.mesh.contract.api.MeshChatMemberCommand
import org.expert.link.mesh.contract.api.MeshSendMessageCommand
import org.expert.link.mesh.contract.api.MeshSendThreadMessageCommand

/**
 * Демо группового чата и тредов через публичный контракт.
 *
 * Сценарий показывает, как мобильный клиент вызывает:
 * - создание группы;
 * - добавление участника;
 * - отправку сообщения в группу;
 * - создание треда и отправку reply;
 * - чтение истории группы и треда.
 */
class GroupThreadDemoScenario(
    private val basePort: Int,
) : DemoScenario {
    override val name: String = "GroupThreadDemoScenario"

    override suspend fun run(): DemoScenarioReport {
        val alice = ScenarioSupport.launchNode(name = "group-alice", port = basePort)
        val bob = ScenarioSupport.launchNode(name = "group-bob", port = basePort + 1)
        val carol = ScenarioSupport.launchNode(name = "group-carol", port = basePort + 2)
        return try {
            ScenarioSupport.connectBidirectional(alice, bob)
            ScenarioSupport.connectBidirectional(alice, carol)
            ScenarioSupport.connectBidirectional(bob, carol)
            ScenarioSupport.pair(alice, bob)
            ScenarioSupport.pair(alice, carol)

            val group = alice.createGroupChat(
                MeshCreateGroupChatCommand(
                    title = "Команда",
                    description = "Общий чат",
                    participantPeerIds = setOf(bob.profile.peerId),
                ),
            )
            alice.addParticipants(
                group.conversationId,
                listOf(MeshChatMemberCommand(carol.profile.peerId, "group-carol")),
            )

            val root = alice.sendMessage(
                MeshSendMessageCommand(
                    chatId = group.conversationId,
                    body = "План на сегодня",
                ),
            )
            val thread = alice.createThread(
                MeshCreateThreadCommand(
                    chatId = group.conversationId,
                    rootMessageId = root.messageId,
                ),
            )
            val reply = alice.sendThreadReply(
                MeshSendThreadMessageCommand(
                    chatId = group.conversationId,
                    rootMessageId = root.messageId,
                    body = "Первый ответ в треде",
                ),
            )
            ScenarioSupport.settle(800)

            val chatMessages = alice.groupMessages(group.conversationId)
            val threadMessages = alice.threadMessagesDetailed(group.conversationId, root.messageId)
            val threadSummary = alice.threadSummary(group.conversationId, root.messageId)
            val threadUpdates = alice.threadUpdates(group.conversationId)
            val groupModel = alice.groupChats().first { it.chatId == group.conversationId }
            val groupEvents = alice.groupEvents(group.conversationId, 20)
            val bobInbound = bob.groupMessages(group.conversationId)
            val carolInbound = carol.groupMessages(group.conversationId)
            val bobThreadUpdates = bob.threadUpdates(group.conversationId)
            val carolThreadUpdates = carol.threadUpdates(group.conversationId)
            val bobGroups = bob.groupChats()
            val carolGroups = carol.groupChats()

            DemoScenarioReport(
                name = name,
                lines = listOf(
                    "Feature: групповой чат и треды.",
                    "Imports: MeshCreateGroupChatCommand, MeshChatMemberCommand, MeshCreateThreadCommand, MeshSendMessageCommand, MeshSendThreadMessageCommand.",
                    "Public API: createGroupChat(), addParticipants(), groupMessages(), createThread(), sendThreadReply(), threadUpdates(), threadMessagesDetailed(), groupEvents().",
                    "Request models: MeshCreateGroupChatCommand, MeshChatMemberCommand, MeshCreateThreadCommand, MeshSendMessageCommand, MeshSendThreadMessageCommand.",
                    "Response models: MeshConversation, MeshGroupChat, MeshThread, MeshThreadMessage, MeshGroupEvent.",
                    "group.id=${group.conversationId}",
                    "group.title=${groupModel.title}",
                    "group.members=${groupModel.members.map { it.displayName + ":" + it.role }}",
                    "root.messageId=${root.messageId}",
                    "thread.id=${thread.threadId}",
                    "thread.replyMessageId=${reply.messageId}",
                    "chat.messages=${chatMessages.map { it.body + "|" + it.messageType }}",
                    "thread.messages=${threadMessages.map { it.body }}",
                    "thread.summary=${threadSummary?.rootMessageId}:${threadSummary?.replyCount}",
                    "thread.updates=${threadUpdates.map { it.threadId + ":" + it.replyCount }}",
                    "group.events=${groupEvents.map { it.eventType.name + ":" + it.text }}",
                    "bob.inbound=${bobInbound.map { it.body }}",
                    "carol.inbound=${carolInbound.map { it.body }}",
                    "bob.threadUpdates=${bobThreadUpdates.map { it.rootMessageId + ":" + it.replyCount }}",
                    "carol.threadUpdates=${carolThreadUpdates.map { it.rootMessageId + ":" + it.replyCount }}",
                    "bob.groupChats=${bobGroups.map { it.chatId + ":" + it.title }}",
                    "carol.groupChats=${carolGroups.map { it.chatId + ":" + it.title }}",
                    "Mobile usage: экран группы вызывает createGroupChat()/addParticipants(), чат — groupMessages()/sendMessage(), экран треда — createThread()/sendThreadReply()/threadMessagesDetailed().",
                ),
            )
        } finally {
            ScenarioSupport.stopAll(alice, bob, carol)
        }
    }
}
