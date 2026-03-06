package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.diagnostics.EventLogEntry
import org.expert.link.mesh.domain.port.external.EventLogRepositoryPort

/** Сервис записи структурированных событий. */
class EventLogService(
    private val eventLogRepositoryPort: EventLogRepositoryPort,
) {
    /**
     * Appends a fully constructed entry to the event log.
     */
    suspend fun append(entry: EventLogEntry): EventLogEntry = eventLogRepositoryPort.append(entry)

    /**
     * Appends a convenience event entry.
     */
    suspend fun log(
        category: EventCategory,
        level: EventLevel,
        message: String,
        peerId: String? = null,
        packetId: String? = null,
        attributes: Map<String, String> = emptyMap(),
    ): EventLogEntry = append(
        EventLogEntry(
            eventId = newId("event"),
            category = category,
            level = level,
            message = message,
            peerId = peerId,
            packetId = packetId,
            attributes = attributes,
            createdAt = now(),
        ),
    )

    /**
     * Returns recent event log entries.
     */
    suspend fun recent(limit: Int = 100): List<EventLogEntry> = eventLogRepositoryPort.listRecent(limit)
}
