package org.expert.link.mesh.simulator.scenario

/**
 * Демо discovery и чтения nearby peers.
 *
 * Контракт:
 * - `MeshNode.announcePresence`
 * - `MeshNode.discoverPeer`
 * - `MeshNode.nearbyPeers`
 * - `MeshNode.routes`
 * - `MeshNode.routingPlan`
 *
 * Модели:
 * - запрос: `peerId: String`
 * - ответы: `List<MeshNearbyPeer>`, `List<MeshRouteInfo>`, `MeshRoutingPlan`
 */
class DiscoveryDemoScenario(
    private val basePort: Int,
) : DemoScenario {
    override val name: String = "DiscoveryDemoScenario"

    override suspend fun run(): DemoScenarioReport {
        val alpha = ScenarioSupport.launchNode(name = "discovery-alpha", port = basePort, discoveryEnabled = true)
        val beta = ScenarioSupport.launchNode(name = "discovery-beta", port = basePort + 1, discoveryEnabled = true)
        return try {
            alpha.announcePresence()
            beta.announcePresence()
            alpha.discoverPeer(beta.profile.peerId)
            ScenarioSupport.settle(700)

            val nearby = alpha.nearbyPeers()
            val routes = alpha.routes()
            val plan = alpha.routingPlan(beta.profile.peerId)

            DemoScenarioReport(
                name = name,
                lines = listOf(
                    "Feature: поиск узлов в локальной сети.",
                    "Imports: MeshNode, MeshNearbyPeer, MeshRouteInfo, MeshRoutingPlan.",
                    "Public API: announcePresence(), discoverPeer(peerId), nearbyPeers(), routes(), routingPlan(peerId).",
                    "Request model: peerId как String.",
                    "Response model: nearby peers, routes, routing plan.",
                    "nearby.peerIds=${nearby.map { it.peerId }}",
                    "nearby.sources=${nearby.map { it.source }}",
                    "routes=${routes.map { it.targetPeerId + ":" + it.routeMode }}",
                    "plan.routeMode=${plan.routeMode}",
                    "plan.hops=${plan.hops.map { it.peerId + "@" + it.endpoint.port }}",
                    "Mobile usage: вызвать announcePresence() при старте discovery-экрана и читать nearbyPeers()/routes() для UI списка соседей.",
                ),
            )
        } finally {
            ScenarioSupport.stopAll(alpha, beta)
        }
    }
}
