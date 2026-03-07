package org.expert.link.mesh.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.security.SecurityIncident

/** Сервис фиксации инцидентов безопасности. */
class SecurityIncidentService(
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Records a security incident.
     */
    suspend fun record(incident: SecurityIncident) {
        logger.warn { "Security incident ${incident.type} for peer=${incident.peerId} packet=${incident.packetId}: ${incident.description}" }
        nodeMetricsService.increment("security.incidents")
        eventLogService.log(
            category = EventCategory.SECURITY,
            level = EventLevel.WARN,
            message = incident.description,
            peerId = incident.peerId,
            packetId = incident.packetId,
            attributes = buildMap {
                put("incidentId", incident.incidentId)
                put("type", incident.type.name)
                put("severity", incident.severity.name)
                putAll(incident.attributes)
            },
        )
    }
}
