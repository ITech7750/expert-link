package org.expert.link.mesh.simulator.scenario

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GroupThreadDemoScenarioTest {
    @Test
    fun `should run group thread scenario through public mesh contract`() = runTest {
        val report = GroupThreadDemoScenario(basePort = 19850).run()

        assertThat(report.name).isEqualTo("GroupThreadDemoScenario")
        assertThat(report.lines.any { it.startsWith("group.id=") }).isTrue()
        assertThat(report.lines.any { it.startsWith("thread.id=") }).isTrue()
        assertThat(report.lines.any { it.startsWith("group.events=") }).isTrue()
        assertThat(report.lines.any { it.startsWith("thread.messages=") }).isTrue()
    }
}

