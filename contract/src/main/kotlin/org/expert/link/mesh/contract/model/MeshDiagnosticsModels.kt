package org.expert.link.mesh.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Категории событий. */
@Serializable
enum class MeshEventCategory {
    DISCOVERY,
    PAIRING,
    MESSAGING,
    FILE_TRANSFER,
    CALL,
    SECURITY,
    ROUTING,
    INVENTORY,
    SYSTEM,
}

/** Уровень события. */
@Serializable
enum class MeshEventLevel {
    TRACE,
    INFO,
    WARN,
    ERROR,
}

/** Публичная запись журнала событий. */
@Serializable
data class MeshEventLogEntry(
    val eventId: String,
    val category: MeshEventCategory,
    val level: MeshEventLevel,
    val message: String,
    val peerId: String? = null,
    val packetId: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val createdAt: Instant,
)

/** Публичный снимок метрик. */
@Serializable
data class MeshMetricSnapshot(
    val nodePeerId: String,
    val capturedAt: Instant,
    val counters: Map<String, Long>,
    val gauges: Map<String, Double>,
)
