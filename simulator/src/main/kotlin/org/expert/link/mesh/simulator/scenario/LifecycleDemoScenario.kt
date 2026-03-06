package org.expert.link.mesh.simulator.scenario

/**
 * Демо жизненного цикла узла.
 *
 * Контракт:
 * - `MeshBackend.launch`
 * - `MeshNode.profile`
 * - `MeshNode.endpoint`
 * - `MeshNode.stop`
 *
 * Модели:
 * - запрос: `MeshNodeConfig`
 * - ответы: `MeshNode`, `MeshLocalProfile`, `MeshPeerEndpoint`
 *
 * Для мобильной команды:
 * тот же вызов делается из app/service или ViewModel без доступа к внутренним пакетам backend.
 */
class LifecycleDemoScenario(
    private val basePort: Int,
) : DemoScenario {
    override val name: String = "LifecycleDemoScenario"

    override suspend fun run(): DemoScenarioReport {
        val node = ScenarioSupport.launchNode(name = "lifecycle-node", port = basePort)
        return try {
            DemoScenarioReport(
                name = name,
                lines = listOf(
                    "Feature: запуск узла и чтение локального профиля.",
                    "Imports: MeshBackend, MeshNode, MeshNodeConfig.",
                    "Public API: MeshBackend.launch(config), node.profile, node.endpoint, node.stop().",
                    "Request model: MeshNodeConfig.",
                    "Response model: MeshNode, MeshLocalProfile, MeshPeerEndpoint.",
                    "profile.peerId=${node.profile.peerId}",
                    "profile.displayName=${node.profile.displayName}",
                    "endpoint=${node.endpoint.scheme}://${node.endpoint.host}:${node.endpoint.port}${node.endpoint.path}",
                    "Mobile usage: сохранить ссылку на MeshNode и работать через неё как через SDK facade.",
                ),
            )
        } finally {
            ScenarioSupport.stopAll(node)
        }
    }
}
