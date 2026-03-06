package org.expert.link.mesh.simulator.scenario

/** Общий контракт demo-сценария симулятора. */
interface DemoScenario {
    /** Человекочитаемое имя сценария. */
    val name: String

    /** Выполняет сценарий и возвращает итоговый отчёт. */
    suspend fun run(): DemoScenarioReport
}

/** Печатный результат выполнения demo-сценария. */
data class DemoScenarioReport(
    val name: String,
    val lines: List<String>,
)

/** Печатает сценарии в консоль симулятора. */
object DemoScenarioPrinter {
    /** Выводит сценарий в консоль. */
    fun print(report: DemoScenarioReport) {
        println()
        println("=== ${report.name} ===")
        report.lines.forEach(::println)
    }
}
