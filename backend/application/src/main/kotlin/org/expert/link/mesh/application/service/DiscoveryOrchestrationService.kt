package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.network.DiscoveryFrame
import org.expert.link.mesh.domain.model.network.DiscoveryMessageType
import org.expert.link.mesh.domain.model.network.EndpointSource
import org.expert.link.mesh.domain.model.network.PeerEndpointCandidate
import org.expert.link.mesh.domain.port.external.CryptoPort
import org.expert.link.mesh.domain.port.repository.EndpointCachePort

/** Результат обработки discovery-кадра. */
data class DiscoveryAction(
    val announceSelf: Boolean = false,
)

/** Сервис проверки и применения discovery-кадров. */
class DiscoveryOrchestrationService(
    private val cryptoPort: CryptoPort,
    private val endpointCachePort: EndpointCachePort,
    private val routingService: RoutingService,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
) {
    /**
     * Processes a discovery frame and returns follow-up actions for the node runtime.
     */
    suspend fun process(localPeerIdentity: PeerIdentity, frame: DiscoveryFrame): DiscoveryAction {
        if (frame.sourcePeerId == localPeerIdentity.peerId) {
            return DiscoveryAction()
        }
        if (frame.expiresAt <= now()) {
            return DiscoveryAction()
        }
        if (cryptoPort.derivePeerId(frame.publicKey) != frame.sourcePeerId) {
            eventLogService.log(
                category = EventCategory.SECURITY,
                level = EventLevel.WARN,
                message = "Discovery frame rejected due to peerId mismatch",
                peerId = frame.sourcePeerId,
            )
            return DiscoveryAction()
        }

        when (frame.type) {
            DiscoveryMessageType.NODE_HELLO,
            DiscoveryMessageType.PEER_ANNOUNCE,
            -> {
                endpointCachePort.put(
                    PeerEndpointCandidate(
                        peerId = frame.sourcePeerId,
                        endpoint = frame.endpoint,
                        source = EndpointSource.DISCOVERY_MULTICAST,
                        discoveredAt = frame.createdAt,
                        capabilities = frame.capabilities,
                    ),
                )
                routingService.learnDirectEndpoint(frame.sourcePeerId, frame.endpoint, frame.capabilities)
                nodeMetricsService.increment("discovery.learned")
                eventLogService.log(
                    category = EventCategory.DISCOVERY,
                    level = EventLevel.INFO,
                    message = "Learned peer endpoint from discovery",
                    peerId = frame.sourcePeerId,
                )
            }
            DiscoveryMessageType.NODE_BYE -> {
                eventLogService.log(
                    category = EventCategory.DISCOVERY,
                    level = EventLevel.INFO,
                    message = "Peer announced shutdown",
                    peerId = frame.sourcePeerId,
                )
            }
            DiscoveryMessageType.PEER_LOOKUP -> {
                return DiscoveryAction(announceSelf = frame.targetPeerId == localPeerIdentity.peerId)
            }
        }
        return DiscoveryAction()
    }
}
