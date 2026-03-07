package org.expert.link.mesh.application.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.diagnostics.MetricSnapshot

/** Сервис метрик узла в памяти. */
class NodeMetricsService {
    private val mutex = Mutex()
    private val counters = linkedMapOf<String, Long>()
    private val gauges = linkedMapOf<String, Double>()


    suspend fun increment(counter: String, delta: Long = 1) {
        mutex.withLock {
            counters[counter] = (counters[counter] ?: 0L) + delta
        }
    }


    suspend fun gauge(name: String, value: Double) {
        mutex.withLock {
            gauges[name] = value
        }
    }


    suspend fun snapshot(nodePeerId: String): MetricSnapshot = mutex.withLock {
        MetricSnapshot(
            nodePeerId = nodePeerId,
            capturedAt = now(),
            counters = counters.toMap(),
            gauges = gauges.toMap(),
        )
    }
}
