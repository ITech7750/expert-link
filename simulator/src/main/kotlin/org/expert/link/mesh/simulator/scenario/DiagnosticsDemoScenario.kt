package org.expert.link.mesh.simulator.scenario

import org.expert.link.mesh.contract.api.MeshChatCommand

/**
 * Демо диагностики, event log и метрик.
 *
 * Контракт:
 * - `MeshNode.recentEvents`
 * - `MeshNode.metrics`
 * - `MeshNode.relayStatus`
 * - `MeshNode.routes`
 *
 * Модели:
 * - ответы: `MeshEventLogEntry`, `MeshMetricSnapshot`, `MeshRelayStatus`, `MeshRouteInfo`
 */
class DiagnosticsDemoScenario(
    private val basePort: Int,
) : DemoScenario {
    override val name: String = "DiagnosticsDemoScenario"

    override suspend fun run(): DemoScenarioReport {
        val left = ScenarioSupport.launchNode(name = "diag-left", port = basePort, discoveryEnabled = true, relayEnabled = true)
        val right = ScenarioSupport.launchNode(name = "diag-right", port = basePort + 1, discoveryEnabled = true, relayEnabled = true)
        return try {
            ScenarioSupport.connectBidirectional(left, right)
            ScenarioSupport.pair(left, right)
            left.announcePresence()
            left.discoverPeer(right.profile.peerId)
            val conversation = left.openConversation(right.profile.peerId)
            left.sendChat(
                MeshChatCommand(
                    targetPeerId = right.profile.peerId,
                    body = "diagnostics message",
                    conversationId = conversation.conversationId,
                ),
            )
            ScenarioSupport.settle(900)

            val events = left.recentEvents(limit = 8)
            val metrics = left.metrics()
            val relay = left.relayStatus()
            val routes = left.routes()
            val nearby = left.nearbyPeers()

            DemoScenarioReport(
                name = name,
                lines = listOf(
                    "Feature: чтение event log, метрик и служебных состояний узла.",
                    "Imports: MeshNode, MeshChatCommand.",
                    "Public API: recentEvents(limit), metrics(), relayStatus(), routes(), nearbyPeers().",
                    "Request models: Int limit, peerId String, MeshChatCommand для генерации событий.",
                    "Response models: MeshEventLogEntry, MeshMetricSnapshot, MeshRelayStatus, MeshRouteInfo, MeshNearbyPeer.",
                    "events=${events.map { it.category.name + ":" + it.message }}",
                    "metrics.counters=${metrics.counters}",
                    "relay.status=$relay",
                    "routes=${routes.map { it.targetPeerId + ":" + it.routeMode }}",
                    "nearby=${nearby.map { it.peerId + ":" + it.source }}",
                    "Mobile usage: diagnostics-экран читает recentEvents()/metrics()/routes()/nearbyPeers() без доступа к внутренним репозиториям.",
                ),
            )
        } finally {
            ScenarioSupport.stopAll(left, right)
        }
    }
}
