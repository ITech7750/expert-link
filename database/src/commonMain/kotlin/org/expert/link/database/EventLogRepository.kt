package org.expert.link.database

import org.expert.link.mesh.domain.model.diagnostics.EventLogEntry
import org.expert.link.mesh.domain.port.external.EventLogRepositoryPort

internal class RoomEventLogRepository(
    private val eventLogDao: EventLogDao,
) : EventLogRepositoryPort {
    override suspend fun append(entry: EventLogEntry): EventLogEntry {
        eventLogDao.upsertEventLog(
            EventLogRecord(
                eventId = entry.eventId,
                category = entry.category.name,
                createdAt = entry.createdAt.toString(),
                payloadJson = encodePayload(entry),
            ),
        )
        return entry
    }

    override suspend fun listRecent(limit: Int): List<EventLogEntry> =
        eventLogDao.listRecentEventLogs(limit)
            .asReversed()
            .map { decodePayload(it.payloadJson) }
}
