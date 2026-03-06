package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.diagnostics.MetricSnapshot
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Сервис метрик узла в памяти. */
class NodeMetricsService {
    private val counters = ConcurrentHashMap<String, AtomicLong>()
    private val gauges = ConcurrentHashMap<String, Double>()


    fun increment(counter: String, delta: Long = 1) {
        counters.computeIfAbsent(counter) { AtomicLong(0) }.addAndGet(delta)
    }


    fun gauge(name: String, value: Double) {
        gauges[name] = value
    }


    fun snapshot(nodePeerId: String): MetricSnapshot = MetricSnapshot(
        nodePeerId = nodePeerId,
        capturedAt = now(),
        counters = counters.mapValues { it.value.get() },
        gauges = gauges.toMap(),
    )
}
