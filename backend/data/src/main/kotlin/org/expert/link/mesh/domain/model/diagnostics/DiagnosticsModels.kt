package org.expert.link.mesh.domain.model.diagnostics

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Категории событий журнала. */
@Serializable
enum class EventCategory {
    DISCOVERY,
    PAIRING,
    MESSAGING,
    FILE_TRANSFER,
    CALL,
    SECURITY,
    ROUTING,
    SYSTEM,
}

/** Уровни событий. */
@Serializable
enum class EventLevel {
    TRACE,
    INFO,
    WARN,
    ERROR,
}

/** Структурированная запись журнала событий. */
@Serializable
data class EventLogEntry(
    val eventId: String,
    val category: EventCategory,
    val level: EventLevel,
    val message: String,
    val peerId: String? = null,
    val packetId: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val createdAt: Instant,
)

/** Снимок агрегированных метрик узла. */
@Serializable
data class MetricSnapshot(
    val nodePeerId: String,
    val capturedAt: Instant,
    val counters: Map<String, Long>,
    val gauges: Map<String, Double>,
)
