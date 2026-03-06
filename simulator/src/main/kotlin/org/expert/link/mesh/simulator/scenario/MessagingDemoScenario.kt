package org.expert.link.mesh.simulator.scenario

import org.expert.link.mesh.contract.api.MeshChatCommand

/**
 * Демо чата, диалога и ACK.
 *
 * Контракт:
 * - `MeshNode.openConversation`
 * - `MeshNode.sendChat`
 * - `MeshNode.messages`
 * - `MeshNode.messageReceipts`
 *
 * Модели:
 * - запрос: `MeshChatCommand`
 * - ответы: `MeshConversation`, `MeshChatMessage`, `MeshMessageReceipt`
 */
class MessagingDemoScenario(
    private val basePort: Int,
) : DemoScenario {
    override val name: String = "MessagingDemoScenario"

    override suspend fun run(): DemoScenarioReport {
        val alice = ScenarioSupport.launchNode(name = "chat-alice", port = basePort)
        val bob = ScenarioSupport.launchNode(name = "chat-bob", port = basePort + 1)
        return try {
            ScenarioSupport.connectBidirectional(alice, bob)
            ScenarioSupport.pair(alice, bob)

            val conversation = alice.openConversation(bob.profile.peerId)
            val outbound = alice.sendChat(
                MeshChatCommand(
                    targetPeerId = bob.profile.peerId,
                    body = "hello from simulator mobile-style chat",
                    conversationId = conversation.conversationId,
                ),
            )
            ScenarioSupport.settle(700)

            val aliceMessages = alice.messages(conversation.conversationId)
            val receipts = alice.messageReceipts()
            val bobConversation = bob.conversations().first()
            val bobMessages = bob.messages(bobConversation.conversationId)

            DemoScenarioReport(
                name = name,
                lines = listOf(
                    "Feature: открытие диалога, отправка сообщения и чтение ACK.",
                    "Imports: MeshNode, MeshChatCommand.",
                    "Public API: openConversation(peerId), sendChat(command), messages(conversationId), messageReceipts().",
                    "Request model: MeshChatCommand(targetPeerId, body, conversationId).",
                    "Response model: MeshConversation, MeshChatMessage, MeshMessageReceipt.",
                    "conversation.id=${conversation.conversationId}",
                    "outbound.messageId=${outbound.messageId}",
                    "alice.deliveryStates=${aliceMessages.map { it.messageId + ":" + it.deliveryStatus }}",
                    "alice.receipts=${receipts.map { it.messageId + ":" + it.deliveryStatus }}",
                    "bob.messages=${bobMessages.map { it.body }}",
                    "Mobile usage: после sendChat() обновлять UI по messages() и messageReceipts() без прямого доступа к ACK-механизму.",
                ),
            )
        } finally {
            ScenarioSupport.stopAll(alice, bob)
        }
    }
}
