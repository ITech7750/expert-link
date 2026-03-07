package org.expert.link.mesh.simulator

import kotlinx.coroutines.runBlocking
import org.expert.link.mesh.simulator.scenario.CallDemoScenario
import org.expert.link.mesh.simulator.scenario.DemoScenario
import org.expert.link.mesh.simulator.scenario.DemoScenarioPrinter
import org.expert.link.mesh.simulator.scenario.DiagnosticsDemoScenario
import org.expert.link.mesh.simulator.scenario.DiscoveryDemoScenario
import org.expert.link.mesh.simulator.scenario.FileTransferDemoScenario
import org.expert.link.mesh.simulator.scenario.GroupThreadDemoScenario
import org.expert.link.mesh.simulator.scenario.LifecycleDemoScenario
import org.expert.link.mesh.simulator.scenario.MessagingDemoScenario
import org.expert.link.mesh.simulator.scenario.PairingDemoScenario
import org.expert.link.mesh.simulator.scenario.RoutingDemoScenario
import org.expert.link.mesh.simulator.scenario.TopologyDemoScenario

/**
 * Запускает simulator как набор mobile-style сценариев.
 *
 * Каждый сценарий использует только `backend` facade и `contract` модели.
 * Это точная демонстрация того, как мобильное приложение будет вызывать backend как библиотеку.
 */
fun main(): Unit = runBlocking {
    val scenarios: List<DemoScenario> = listOf(
        LifecycleDemoScenario(basePort = 18_100),
        PairingDemoScenario(basePort = 18_110),
        DiscoveryDemoScenario(basePort = 18_120),
        MessagingDemoScenario(basePort = 18_130),
        GroupThreadDemoScenario(basePort = 18_135),
        RoutingDemoScenario(basePort = 18_140),
        TopologyDemoScenario(basePort = 18_150),
        FileTransferDemoScenario(basePort = 18_160),
        CallDemoScenario(basePort = 18_170),
        DiagnosticsDemoScenario(basePort = 18_180),
    )

    scenarios.forEach { scenario ->
        DemoScenarioPrinter.print(scenario.run())
    }
}
