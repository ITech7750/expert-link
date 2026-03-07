package org.expert.link.mesh.simulator.scenario

import org.expert.link.mesh.contract.api.MeshChatCommand

/**
 * Демо topology/host/failover и relay fallback между удалёнными сетями.
 *
 * Контракт:
 * - `MeshNode.observeTopologyState`
 * - `MeshNode.observeHostRole`
 * - `MeshNode.forceTopologyRefresh`
 * - `MeshNode.observeConnectivityStrategy`
 * - `MeshNode.relayModeState`
 * - `MeshNode.sendChat`
 */
class TopologyDemoScenario(
    private val basePort: Int,
) : DemoScenario {
    override val name: String = "TopologyDemoScenario"

    override suspend fun run(): DemoScenarioReport {
        val host = ScenarioSupport.launchNode(name = "topo-host", port = basePort)
        val nodeA = ScenarioSupport.launchNode(name = "topo-a", port = basePort + 1)
        val nodeB = ScenarioSupport.launchNode(name = "topo-b", port = basePort + 2)
        val remoteX = ScenarioSupport.launchNode(name = "topo-remote-x", port = basePort + 10, relayEnabled = true)
        val remoteY = ScenarioSupport.launchNode(name = "topo-remote-y", port = basePort + 11, relayEnabled = true)
        return try {
            ScenarioSupport.connectBidirectional(nodeA, host)
            ScenarioSupport.connectBidirectional(nodeB, host)
            ScenarioSupport.pair(nodeA, host)
            ScenarioSupport.pair(nodeB, host)
            ScenarioSupport.connectBidirectional(nodeA, nodeB)
            ScenarioSupport.pair(nodeA, nodeB)
            ScenarioSupport.settle()

            val hostRoleBeforeA = nodeA.observeHostRole()
            val topologyBeforeA = nodeA.observeTopologyState()

            nodeA.forgetPeerEndpoint(host.profile.peerId)
            nodeB.forgetPeerEndpoint(host.profile.peerId)
            host.stop()
            ScenarioSupport.settle(700)

            val topologyAfterA = nodeA.forceTopologyRefresh()
            val topologyAfterB = nodeB.forceTopologyRefresh()
            val hostRoleAfterA = nodeA.observeHostRole()
            val hostRoleAfterB = nodeB.observeHostRole()

            val localRecoveryMessage = nodeA.sendChat(
                MeshChatCommand(
                    targetPeerId = nodeB.profile.peerId,
                    body = "host failover local message",
                ),
            )
            ScenarioSupport.settle(600)

            val invite = remoteY.createPairingInvite()
            remoteX.pairWithInvite(invite)
            ScenarioSupport.settle(900)
            val strategyRelay = remoteX.observeConnectivityStrategy(remoteY.profile.peerId)
            val relayPlan = remoteX.routingPlan(remoteY.profile.peerId)
            val relayMessage = remoteX.sendChat(
                MeshChatCommand(
                    targetPeerId = remoteY.profile.peerId,
                    body = "remote network relay message",
                ),
            )
            ScenarioSupport.settle(700)
            val topologyRelay = remoteX.observeTopologyState()
            val routeHealthSummary = topologyAfterA.routeHealth.joinToString { "${it.targetPeerId}:${it.state}" }
            val relayModeState = remoteX.relayModeState()

            DemoScenarioReport(
                name = name,
                lines = listOf(
                    "Feature: host mode, failover rebuild и relay/proxy fallback.",
                    "Imports: MeshNode, MeshTopologyState, MeshNetworkRoleState, MeshConnectivityStrategy, MeshRouteHealth, MeshRelayMode.",
                    "Public API: observeTopologyState(), observeHostRole(), forceTopologyRefresh(), observeConnectivityStrategy(peerId), relayModeState(), sendChat().",
                    "Request model: targetPeerId string через MeshChatCommand.",
                    "Response model: MeshTopologyState, MeshNetworkRoleState, MeshConnectivityStrategy, MeshRoutingPlan.",
                    "host.before.role=${hostRoleBeforeA.localRole}",
                    "host.before.currentHost=${hostRoleBeforeA.currentHostPeerId}",
                    "topology.before.mode=${topologyBeforeA.connectivityMode}",
                    "host.after.A.role=${hostRoleAfterA.localRole}",
                    "host.after.A.currentHost=${hostRoleAfterA.currentHostPeerId}",
                    "host.after.B.role=${hostRoleAfterB.localRole}",
                    "host.after.B.currentHost=${hostRoleAfterB.currentHostPeerId}",
                    "failover.A.mode=${topologyAfterA.connectivityMode}",
                    "failover.B.mode=${topologyAfterB.connectivityMode}",
                    "failover.A.routeHealth=$routeHealthSummary",
                    "failover.local.messageId=${localRecoveryMessage.messageId}",
                    "relay.strategy.routeMode=${strategyRelay.routeMode}",
                    "relay.strategy.mode=${strategyRelay.connectivityMode}",
                    "relay.plan.routeMode=${relayPlan.routeMode}",
                    "relay.mode=$relayModeState",
                    "relay.messageId=${relayMessage.messageId}",
                    "relay.topology.mode=${topologyRelay.connectivityMode}",
                    "relay.topology.relayMode=${topologyRelay.relayMode}",
                    "Mobile usage: экран сети вызывает observeTopologyState()/observeHostRole()/inspectRouteHealth() и показывает failover/relay состояние без доступа к внутренним сервисам.",
                ),
            )
        } finally {
            ScenarioSupport.stopAll(nodeA, nodeB, remoteX, remoteY)
            runCatching { host.stop() }
        }
    }
}
