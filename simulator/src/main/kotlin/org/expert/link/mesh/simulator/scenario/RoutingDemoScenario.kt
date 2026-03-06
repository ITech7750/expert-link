package org.expert.link.mesh.simulator.scenario

import org.expert.link.mesh.contract.api.MeshChatCommand

/**
 * Демо multihop и relay readiness.
 *
 * Контракт:
 * - `MeshNode.routingPlan`
 * - `MeshNode.routes`
 * - `MeshNode.sendChat`
 * - `MeshNode.relayStatus`
 *
 * Модели:
 * - запрос: `MeshChatCommand`
 * - ответы: `MeshRoutingPlan`, `List<MeshRouteInfo>`, `MeshRelayStatus`
 */
class RoutingDemoScenario(
    private val basePort: Int,
) : DemoScenario {
    override val name: String = "RoutingDemoScenario"

    override suspend fun run(): DemoScenarioReport {
        val nodeA = ScenarioSupport.launchNode(name = "route-a", port = basePort)
        val nodeB = ScenarioSupport.launchNode(name = "route-b", port = basePort + 1)
        val nodeC = ScenarioSupport.launchNode(name = "route-c", port = basePort + 2)
        val relayD = ScenarioSupport.launchNode(name = "route-d", port = basePort + 10, relayEnabled = true)
        val relayE = ScenarioSupport.launchNode(name = "route-e", port = basePort + 11, relayEnabled = true)
        return try {
            ScenarioSupport.connectBidirectional(nodeA, nodeB)
            ScenarioSupport.connectBidirectional(nodeB, nodeC)
            ScenarioSupport.connectBidirectional(nodeA, nodeC)
            ScenarioSupport.pair(nodeA, nodeB)
            ScenarioSupport.pair(nodeB, nodeC)
            ScenarioSupport.pair(nodeA, nodeC)

            nodeA.forgetPeerEndpoint(nodeC.profile.peerId)
            nodeC.forgetPeerEndpoint(nodeA.profile.peerId)
            ScenarioSupport.settle()

            val multihopConversation = nodeA.openConversation(nodeC.profile.peerId)
            val multihopPlan = nodeA.routingPlan(nodeC.profile.peerId)
            val multihopMessage = nodeA.sendChat(
                MeshChatCommand(
                    targetPeerId = nodeC.profile.peerId,
                    body = "hello over multihop path",
                    conversationId = multihopConversation.conversationId,
                ),
            )
            ScenarioSupport.settle(900)

            val aRoutes = nodeA.routes()
            val cMessages = nodeC.messages(nodeC.conversations().first().conversationId)
            val aReceipts = nodeA.messageReceipts()

            val relayInvite = relayE.createPairingInvite()
            relayD.pairWithInvite(relayInvite)
            ScenarioSupport.settle(700)
            val relayPlan = relayD.routingPlan(relayE.profile.peerId)
            val relayMessage = relayD.sendChat(
                MeshChatCommand(
                    targetPeerId = relayE.profile.peerId,
                    body = "hello over rendezvous relay",
                ),
            )
            ScenarioSupport.settle(700)
            val relayStatus = relayD.relayStatus()
            val relayMessages = relayE.messages(relayE.conversations().first().conversationId)

            DemoScenarioReport(
                name = name,
                lines = listOf(
                    "Feature: выбор маршрута, multihop и readiness для relay/rendezvous.",
                    "Imports: MeshNode, MeshChatCommand, MeshRoutingPlan, MeshRelayStatus.",
                    "Public API: routingPlan(peerId), routes(), sendChat(command), relayStatus().",
                    "Request model: MeshChatCommand(targetPeerId, body, conversationId).",
                    "Response model: MeshRoutingPlan, MeshRouteInfo, MeshChatMessage, MeshRelayStatus.",
                    "multihop.plan.routeMode=${multihopPlan.routeMode}",
                    "multihop.plan.hops=${multihopPlan.hops.map { it.peerId + ":" + it.routeMode }}",
                    "multihop.messageId=${multihopMessage.messageId}",
                    "multihop.ack=${aReceipts.map { it.messageId + ":" + it.deliveryStatus }}",
                    "multihop.routes=${aRoutes.map { it.targetPeerId + ":" + it.routeMode + ":direct=" + it.direct }}",
                    "multihop.inbound=${cMessages.map { it.body }}",
                    "relay.status=$relayStatus",
                    "relay.plan.routeMode=${relayPlan.routeMode}",
                    "relay.plan.useRelayGateway=${relayPlan.useRelayGateway}",
                    "relay.messageId=${relayMessage.messageId}",
                    "relay.inbound=${relayMessages.map { it.body }}",
                    "Mobile usage: перед отправкой можно читать routingPlan() и показывать в UI прямой, multihop или relay маршрут.",
                ),
            )
        } finally {
            ScenarioSupport.stopAll(nodeA, nodeB, nodeC, relayD, relayE)
        }
    }
}
