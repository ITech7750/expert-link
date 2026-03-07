package org.expert.link.mesh.bootstrap

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.expert.link.mesh.application.service.BlockListService
import org.expert.link.mesh.application.service.CallSignalingService
import org.expert.link.mesh.application.service.ChatMessagingService
import org.expert.link.mesh.application.service.DeduplicationService
import org.expert.link.mesh.application.service.DeliveryTrackingService
import org.expert.link.mesh.application.service.DiscoveryOrchestrationService
import org.expert.link.mesh.application.service.EventLogService
import org.expert.link.mesh.application.service.FileTransferService
import org.expert.link.mesh.application.service.LocalProfileService
import org.expert.link.mesh.application.service.MessageEncryptionService
import org.expert.link.mesh.application.service.NodeMetricsService
import org.expert.link.mesh.application.service.PacketSignatureService
import org.expert.link.mesh.application.service.PairingService
import org.expert.link.mesh.application.service.PeerTrustVerificationService
import org.expert.link.mesh.application.service.RateLimitService
import org.expert.link.mesh.application.service.RelayService
import org.expert.link.mesh.application.service.RetrySchedulerService
import org.expert.link.mesh.application.service.RoutingService
import org.expert.link.mesh.application.service.SecurityIncidentService
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.application.support.plusSeconds
import org.expert.link.mesh.bootstrap.config.NodeConfiguration
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.messaging.OutgoingMessage
import org.expert.link.mesh.domain.model.network.CallInvite
import org.expert.link.mesh.domain.model.network.CallHangup
import org.expert.link.mesh.domain.model.network.CallSignalPayload
import org.expert.link.mesh.domain.model.network.ChatMessagePayload
import org.expert.link.mesh.domain.model.network.DeliveryAckPayload
import org.expert.link.mesh.domain.model.network.EndpointSource
import org.expert.link.mesh.domain.model.network.FileAccept
import org.expert.link.mesh.domain.model.network.FileAck
import org.expert.link.mesh.domain.model.network.FileChunkPayload
import org.expert.link.mesh.domain.model.network.FileComplete
import org.expert.link.mesh.domain.model.network.FileOffer
import org.expert.link.mesh.domain.model.network.FileResumeRequestPayload
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.network.PairAccept
import org.expert.link.mesh.domain.model.network.PairRequest
import org.expert.link.mesh.domain.model.network.PeerAnnounce
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.PeerEndpointCandidate
import org.expert.link.mesh.domain.model.network.PeerLookup
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.model.network.TransportDeliveryResult
import org.expert.link.mesh.domain.model.network.SystemEventPayload
import org.expert.link.mesh.domain.model.security.EncryptedPayload
import org.expert.link.mesh.domain.model.security.RateLimitRule
import org.expert.link.mesh.domain.model.security.RateLimitScope
import org.expert.link.mesh.domain.model.security.SecurityIncident
import org.expert.link.mesh.domain.model.security.SecurityIncidentType
import org.expert.link.mesh.domain.model.security.SecuritySeverity
import org.expert.link.mesh.domain.port.external.DiscoveryPort
import org.expert.link.mesh.domain.port.external.RendezvousRegistryPort
import org.expert.link.mesh.domain.port.repository.EndpointCachePort
import org.expert.link.mesh.domain.port.repository.ReversePathRepositoryPort

/**
 * Координатор runtime одного mesh-узла.
 *
 * Управляет жизненным циклом узла, принимает входящие пакеты и делегирует операции
 * pairing, чата, файлов и звонков соответствующим сервисам.
 */
class NodeLifecycleService(
    private val configuration: NodeConfiguration,
    private val localProfile: LocalProfile,
    private val localEndpoint: PeerEndpoint,
    private val localProfileService: LocalProfileService,
    private val pairingService: PairingService,
    private val chatMessagingService: ChatMessagingService,
    private val fileTransferService: FileTransferService,
    private val callSignalingService: CallSignalingService,
    private val discoveryPort: DiscoveryPort?,
    private val discoveryOrchestrationService: DiscoveryOrchestrationService?,
    private val reversePathRepositoryPort: ReversePathRepositoryPort,
    private val endpointCachePort: EndpointCachePort,
    private val messageEncryptionService: MessageEncryptionService,
    private val packetSignatureService: PacketSignatureService,
    private val peerTrustVerificationService: PeerTrustVerificationService,
    private val blockListService: BlockListService,
    private val rateLimitService: RateLimitService,
    private val deduplicationService: DeduplicationService,
    private val routingService: RoutingService,
    private val relayService: RelayService,
    private val deliveryTrackingService: DeliveryTrackingService,
    private val retrySchedulerService: RetrySchedulerService,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
    private val securityIncidentService: SecurityIncidentService,
    private val rendezvousRegistryPort: RendezvousRegistryPort? = null,
    private val onStart: suspend () -> Unit = {},
    private val onStop: suspend () -> Unit = {},
) {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var discoveryJob: Job? = null
    private var helloJob: Job? = null
    private var relayHeartbeatJob: Job? = null

    /** Запускает фоновые задачи узла. */
    suspend fun start() {
        onStart()
        retrySchedulerService.start(scope)
        if (configuration.featureFlags.discoveryEnabled && discoveryPort != null && discoveryOrchestrationService != null) {
            discoveryPort.start(localProfileService.asPeerIdentity(localProfile), localEndpoint)
            discoveryPort.broadcastHello()
            discoveryJob = scope.launch {
                discoveryPort.events.collectLatest { frame ->
                    val action = discoveryOrchestrationService.process(localProfileService.asPeerIdentity(localProfile), frame)
                    if (action.announceSelf) {
                        discoveryPort.announcePeer(frame.sourcePeerId)
                    }
                }
            }
            helloJob = scope.launch {
                while (true) {
                    discoveryPort.broadcastHello()
                    delay(10_000)
                }
            }
        }
        if (configuration.featureFlags.relayEnabled && rendezvousRegistryPort != null) {
            rendezvousRegistryPort.registerNode(
                org.expert.link.mesh.domain.model.relay.PeerRegistration(
                    peerId = localProfile.peerId,
                    publicKey = localProfile.publicKey,
                    displayName = localProfile.displayName,
                    endpoints = listOf(localEndpoint),
                    lastHeartbeatAt = now(),
                    capabilities = localProfile.capabilities,
                    relayEligible = configuration.relayClientSettings?.relayEligible ?: false,
                ),
            )
            relayHeartbeatJob = scope.launch {
                while (true) {
                    rendezvousRegistryPort.heartbeat(localProfile.peerId)
                    delay(15_000)
                }
            }
        }
        eventLogService.log(
            category = org.expert.link.mesh.domain.model.diagnostics.EventCategory.SYSTEM,
            level = org.expert.link.mesh.domain.model.diagnostics.EventLevel.INFO,
            message = "Node started",
            peerId = localProfile.peerId,
        )
    }

    /** Останавливает узел и освобождает ресурсы. */
    suspend fun stop() {
        relayHeartbeatJob?.cancel()
        helloJob?.cancel()
        discoveryJob?.cancel()
        if (configuration.featureFlags.discoveryEnabled) {
            discoveryPort?.broadcastBye()
            discoveryPort?.stop()
        }
        if (configuration.featureFlags.relayEnabled) {
            rendezvousRegistryPort?.unregisterNode(localProfile.peerId)
        }
        retrySchedulerService.stop()
        onStop()
        scope.cancel()
        eventLogService.log(
            category = org.expert.link.mesh.domain.model.diagnostics.EventCategory.SYSTEM,
            level = org.expert.link.mesh.domain.model.diagnostics.EventLevel.INFO,
            message = "Node stopped",
            peerId = localProfile.peerId,
        )
    }

    /** Проверяет, расшифровывает и маршрутизирует входящий пакет. */
    suspend fun handleIncomingPacket(envelope: PacketEnvelope): TransportDeliveryResult {
        if (blockListService.isBlocked(envelope.sourcePeerId)) {
            return TransportDeliveryResult(success = false, errorMessage = "Source peer is blocked")
        }
        if (!rateLimitService.allow(envelope.sourcePeerId, envelope.packetType, RateLimitRule("default", RateLimitScope.PEER, 100, 10))) {
            securityIncidentService.record(
                SecurityIncident(
                    incidentId = newId("security"),
                    type = SecurityIncidentType.RATE_LIMIT,
                    severity = SecuritySeverity.MEDIUM,
                    peerId = envelope.sourcePeerId,
                    packetId = envelope.packetId,
                    description = "Inbound packet rate limit exceeded",
                    occurredAt = now(),
                ),
            )
            return TransportDeliveryResult(success = false, errorMessage = "Rate limit exceeded")
        }

        learnObservedNetworkState(envelope)

        val isNew = deduplicationService.isNew(envelope.packetId)
        if (!isNew) {
            if (envelope.targetPeerId == localProfile.peerId && envelope.requiresAck && envelope.packetType == PacketType.CHAT_MESSAGE) {
                chatMessagingService.sendDeliveryAck(envelope)
            }
            return TransportDeliveryResult(success = true, deliveredAt = now())
        }

        if (envelope.targetPeerId != localProfile.peerId) {
            val relayed = relayService.relay(localProfile.peerId, envelope)
            return TransportDeliveryResult(success = relayed, deliveredAt = if (relayed) now() else null)
        }

        val encryptedPayload = EncryptedPayload(envelope.payloadNonce, envelope.encryptedPayload)
        return when (envelope.packetType) {
            PacketType.PAIR_REQUEST -> {
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as PairRequest
                pairingService.handlePairRequest(envelope, payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.PAIR_ACCEPT -> {
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as PairAccept
                pairingService.handlePairAccept(envelope, payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.CHAT_MESSAGE -> {
                verifyTrustedEnvelope(envelope) ?: return TransportDeliveryResult(success = false, errorMessage = "Untrusted sender")
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as ChatMessagePayload
                chatMessagingService.handleIncomingMessage(envelope, payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.DELIVERY_ACK -> {
                verifyTrustedEnvelope(envelope) ?: return TransportDeliveryResult(success = false, errorMessage = "Untrusted sender")
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as DeliveryAckPayload
                chatMessagingService.handleDeliveryAck(payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.FILE_OFFER -> {
                verifyTrustedEnvelope(envelope) ?: return TransportDeliveryResult(success = false, errorMessage = "Untrusted sender")
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as FileOffer
                fileTransferService.handleFileOffer(payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.FILE_ACCEPT -> {
                verifyTrustedEnvelope(envelope) ?: return TransportDeliveryResult(success = false, errorMessage = "Untrusted sender")
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as FileAccept
                fileTransferService.handleFileAccept(payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.FILE_CHUNK -> {
                verifyTrustedEnvelope(envelope) ?: return TransportDeliveryResult(success = false, errorMessage = "Untrusted sender")
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as FileChunkPayload
                fileTransferService.handleFileChunk(payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.FILE_ACK -> {
                verifyTrustedEnvelope(envelope) ?: return TransportDeliveryResult(success = false, errorMessage = "Untrusted sender")
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as FileAck
                fileTransferService.handleFileAck(payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.FILE_COMPLETE -> {
                verifyTrustedEnvelope(envelope) ?: return TransportDeliveryResult(success = false, errorMessage = "Untrusted sender")
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as FileComplete
                fileTransferService.handleFileComplete(payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.FILE_RESUME_REQUEST -> {
                verifyTrustedEnvelope(envelope) ?: return TransportDeliveryResult(success = false, errorMessage = "Untrusted sender")
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as FileResumeRequestPayload
                fileTransferService.handleResumeRequest(payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.CALL_INVITE -> {
                verifyTrustedEnvelope(envelope) ?: return TransportDeliveryResult(success = false, errorMessage = "Untrusted sender")
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as CallInvite
                callSignalingService.handleInvite(payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.CALL_SIGNAL -> {
                verifyTrustedEnvelope(envelope) ?: return TransportDeliveryResult(success = false, errorMessage = "Untrusted sender")
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as CallSignalPayload
                callSignalingService.handleSignal(payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.CALL_HANGUP -> {
                verifyTrustedEnvelope(envelope) ?: return TransportDeliveryResult(success = false, errorMessage = "Untrusted sender")
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as CallHangup
                callSignalingService.handleHangup(payload)
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.PEER_LOOKUP -> {
                val payload = messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as PeerLookup
                if (payload.requestedPeerId == localProfile.peerId) {
                    discoveryPort?.announcePeer(payload.requesterPeerId)
                }
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.PEER_ANNOUNCE -> {
                messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as PeerAnnounce
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
            PacketType.SYSTEM_EVENT -> {
                messageEncryptionService.decryptPayload(localProfile.privateKey, envelope.packetType, encryptedPayload) as SystemEventPayload
                TransportDeliveryResult(success = true, deliveredAt = now())
            }
        }
    }

    /**
     * Creates a new pairing invite string.
     */
    suspend fun createPairingInvite(validitySeconds: Long = 300): String = pairingService.createInvite(validitySeconds).toEncodedString()

    /**
     * Consumes a pairing invite string.
     */
    suspend fun pairWithInvite(encodedInvite: String) = pairingService.pairWithInvite(encodedInvite)

    /**
     * Sends a chat message to a trusted peer.
     */
    suspend fun sendChat(targetPeerId: String, body: String, conversationId: String? = null) = chatMessagingService.send(
        OutgoingMessage(targetPeerId = targetPeerId, conversationId = conversationId, body = body, requestedAt = now()),
    )

    /**
     * Starts an active discovery lookup for a specific peer when discovery is enabled.
     */
    suspend fun discoverPeer(peerId: String) {
        discoveryPort?.lookupPeer(peerId)
    }

    /**
     * Publishes an explicit local presence announcement when discovery is enabled.
     */
    suspend fun announcePresence() {
        discoveryPort?.broadcastHello()
    }

    /**
     * Starts an outbound file transfer.
     */
    suspend fun sendFile(targetPeerId: String, path: String, conversationId: String? = null) = fileTransferService.offerFile(targetPeerId, path, conversationId)

    /**
     * Requests retransmission of missing file chunks for an inbound transfer.
     */
    suspend fun requestFileResume(transferId: String) = fileTransferService.requestResume(transferId)

    /**
     * Starts call signaling with a trusted peer.
     */
    suspend fun startCall(targetPeerId: String, conversationId: String? = null, offer: String) = callSignalingService.invite(targetPeerId, conversationId, offer)

    /**
     * Produces a metrics snapshot for the current node.
     */
    suspend fun metricsSnapshot() = nodeMetricsService.snapshot(localProfile.peerId)

    /**
     * Returns recent event log entries.
     */
    suspend fun recentEvents(limit: Int = 100) = eventLogService.recent(limit)

    /**
     * Injects a known neighbor endpoint into the runtime topology.
     */
    suspend fun rememberPeerEndpoint(peerId: String, endpoint: PeerEndpoint) {
        endpointCachePort.put(
            PeerEndpointCandidate(
                peerId = peerId,
                endpoint = endpoint,
                source = EndpointSource.MANUAL_HINT,
                discoveredAt = now(),
            ),
        )
        routingService.learnDirectEndpoint(peerId, endpoint)
    }

    /**
     * Removes cached routing information for a peer.
     */
    suspend fun forgetPeerEndpoint(peerId: String) {
        endpointCachePort.removePeer(peerId)
        routingService.invalidateRoute(peerId)
    }

    private suspend fun learnObservedNetworkState(envelope: PacketEnvelope) {
        val previousHopId = envelope.previousHopPeerId ?: envelope.sourcePeerId
        val previousEndpoint = endpointCachePort.findByPeerId(previousHopId).firstOrNull()?.endpoint
        reversePathRepositoryPort.save(
            org.expert.link.mesh.domain.model.network.ReversePathEntry(
                packetId = envelope.packetId,
                messageId = envelope.messageId,
                previousHopPeerId = previousHopId,
                previousHopEndpoint = previousEndpoint,
                createdAt = now(),
                expiresAt = now().plusSeconds(300),
            ),
        )
        routingService.learnObservedRoute(envelope.sourcePeerId, previousHopId, previousEndpoint, envelope.hopCount)
    }

    private suspend fun verifyTrustedEnvelope(envelope: PacketEnvelope): PeerIdentity? {
        val trustedPeer = peerTrustVerificationService.requireTrusted(envelope.sourcePeerId) ?: run {
            securityIncidentService.record(
                SecurityIncident(
                    incidentId = newId("security"),
                    type = SecurityIncidentType.TRUST_FAILURE,
                    severity = SecuritySeverity.HIGH,
                    peerId = envelope.sourcePeerId,
                    packetId = envelope.packetId,
                    description = "Packet received from untrusted peer",
                    occurredAt = now(),
                ),
            )
            return null
        }
        if (!packetSignatureService.verifyEnvelope(trustedPeer.peerIdentity.publicKey, envelope)) {
            securityIncidentService.record(
                SecurityIncident(
                    incidentId = newId("security"),
                    type = SecurityIncidentType.SIGNATURE_MISMATCH,
                    severity = SecuritySeverity.HIGH,
                    peerId = envelope.sourcePeerId,
                    packetId = envelope.packetId,
                    description = "Packet signature verification failed",
                    occurredAt = now(),
                ),
            )
            return null
        }
        return trustedPeer.peerIdentity
    }
}
