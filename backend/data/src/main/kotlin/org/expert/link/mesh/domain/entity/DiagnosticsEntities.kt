package org.expert.link.mesh.domain.entity

import kotlinx.serialization.Serializable

/** Сущность хранения записи журнала событий. */
@Serializable
data class EventLogEntity(
    val eventId: String,
    val category: String,
    val level: String,
    val message: String,
    val peerId: String? = null,
    val packetId: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val createdAt: String,
)
