package org.expert.link.mesh.application.mapper

import kotlinx.datetime.Instant
import org.expert.link.mesh.domain.entity.EventLogEntity
import org.expert.link.mesh.domain.model.diagnostics.EventLogEntry
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel

/** Маппер записи журнала событий. */
object EventLogEntityMapper {
    /**
     * Converts a domain event log entry into a storage entity.
     */
    fun toEntity(entry: EventLogEntry): EventLogEntity = EventLogEntity(
        eventId = entry.eventId,
        category = entry.category.name,
        level = entry.level.name,
        message = entry.message,
        peerId = entry.peerId,
        packetId = entry.packetId,
        attributes = entry.attributes,
        createdAt = entry.createdAt.toString(),
    )

    /**
     * Converts a storage entity back into a domain event log entry.
     */
    fun fromEntity(entity: EventLogEntity): EventLogEntry = EventLogEntry(
        eventId = entity.eventId,
        category = EventCategory.valueOf(entity.category),
        level = EventLevel.valueOf(entity.level),
        message = entity.message,
        peerId = entity.peerId,
        packetId = entity.packetId,
        attributes = entity.attributes,
        createdAt = Instant.parse(entity.createdAt),
    )
}
