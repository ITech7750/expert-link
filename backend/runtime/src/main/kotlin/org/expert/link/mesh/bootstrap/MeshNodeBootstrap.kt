package org.expert.link.mesh.bootstrap

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.expert.link.mesh.application.factory.KeyMaterialFactory
import org.expert.link.mesh.application.factory.PacketEnvelopeFactory
import org.expert.link.mesh.application.service.BlockListService
import org.expert.link.mesh.application.service.CallSignalingService
import org.expert.link.mesh.application.service.ChatMessagingService
import org.expert.link.mesh.application.service.ConnectivityStrategyService
import org.expert.link.mesh.application.service.DeduplicationService
import org.expert.link.mesh.application.service.DeliveryTrackingService
import org.expert.link.mesh.application.service.DiscoveryOrchestrationService
import org.expert.link.mesh.application.service.EventLogService
import org.expert.link.mesh.application.service.FileResumeService
import org.expert.link.mesh.application.service.FileTransferService
import org.expert.link.mesh.application.service.LocalProfileService
import org.expert.link.mesh.application.service.MessageEncryptionService
import org.expert.link.mesh.application.service.NodeMetricsService
import org.expert.link.mesh.application.service.PacketSerializationService
import org.expert.link.mesh.application.service.PacketSignatureService
import org.expert.link.mesh.application.service.PairingService
import org.expert.link.mesh.application.service.PeerTrustVerificationService
import org.expert.link.mesh.application.service.RateLimitService
import org.expert.link.mesh.application.service.RelayService
import org.expert.link.mesh.application.service.RetryPolicyService
import org.expert.link.mesh.application.service.RetrySchedulerService
import org.expert.link.mesh.application.service.RoutingService
import org.expert.link.mesh.application.service.SecurityIncidentService
import org.expert.link.mesh.bootstrap.config.NodeConfiguration
import org.expert.link.mesh.bootstrap.controller.PacketController
import org.expert.link.mesh.bootstrap.controller.PacketRouteHandler
import org.expert.link.mesh.bootstrap.runtime.MeshNodeComponents
import org.expert.link.mesh.bootstrap.runtime.MeshNodeRuntime
import org.expert.link.mesh.domain.model.network.EndpointSource
import org.expert.link.mesh.domain.model.network.PeerEndpointCandidate
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.security.RateLimitRule
import org.expert.link.mesh.domain.model.security.RateLimitScope
import org.expert.link.mesh.infrastructure.adapter.JvmMulticastSupportAdapter
import org.expert.link.mesh.infrastructure.cache.InMemoryDedupCacheAdapter
import org.expert.link.mesh.infrastructure.cache.InMemoryEndpointCacheAdapter
import org.expert.link.mesh.infrastructure.cache.InMemoryReversePathRepositoryAdapter
import org.expert.link.mesh.infrastructure.cache.InMemoryRouteRepositoryAdapter
import org.expert.link.mesh.infrastructure.client.RendezvousRelayClient
import org.expert.link.mesh.infrastructure.crypto.BasicCryptoAdapter
import org.expert.link.mesh.infrastructure.discovery.InMemoryDiscoveryAdapter
import org.expert.link.mesh.infrastructure.discovery.UdpDiscoveryAdapter
import org.expert.link.mesh.infrastructure.repository.FileSystemChunkStorageAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryBlockListRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCallSessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryConversationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryEventLogRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryFileTransferRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryLocalProfileRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryMessageRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOutgoingQueueAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryPairingSessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryPeerRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryPendingAckRepositoryAdapter
import org.expert.link.mesh.infrastructure.transport.InMemoryPacketTransportAdapter
import org.expert.link.mesh.infrastructure.transport.KtorPacketTransportAdapter

/** Сборщик runtime узла.
 *
 * Создаёт адаптеры, сервисы и запущенный экземпляр узла.
 * Это composition root backend-ядра: здесь связываются внутренние слои, а наружу отдаётся готовый runtime.
 */
class MeshNodeBootstrap {
    /**
     * Creates, starts and returns a fully wired node runtime.
     */
    suspend fun bootstrap(configuration: NodeConfiguration): MeshNodeRuntime {
        return bootstrapComponents(configuration).runtime
    }

    /**
     * Creates, starts and returns the complete runtime plus internal services required by the public facade.
     */
    suspend fun bootstrapComponents(configuration: NodeConfiguration): MeshNodeComponents {
        val localProfileRepositoryPort = InMemoryLocalProfileRepositoryAdapter()
        val peerRepositoryPort = InMemoryPeerRepositoryAdapter()
        val pairingSessionRepositoryPort = InMemoryPairingSessionRepositoryAdapter()
        val blockListRepositoryPort = InMemoryBlockListRepositoryAdapter()
        val conversationRepositoryPort = InMemoryConversationRepositoryAdapter()
        val messageRepositoryPort = InMemoryMessageRepositoryAdapter()
        val outgoingQueuePort = InMemoryOutgoingQueueAdapter()
        val pendingAckRepositoryPort = InMemoryPendingAckRepositoryAdapter()
        val endpointCachePort = InMemoryEndpointCacheAdapter()
        val routeRepositoryPort = InMemoryRouteRepositoryAdapter()
        val reversePathRepositoryPort = InMemoryReversePathRepositoryAdapter()
        val dedupCachePort = InMemoryDedupCacheAdapter()
        val fileTransferRepositoryPort = InMemoryFileTransferRepositoryAdapter()
        val callSessionRepositoryPort = InMemoryCallSessionRepositoryAdapter()
        val eventLogRepositoryPort = InMemoryEventLogRepositoryAdapter()

        val cryptoPort = BasicCryptoAdapter()
        val keyMaterialFactory = KeyMaterialFactory(cryptoPort)
        val localProfileService = LocalProfileService(localProfileRepositoryPort, keyMaterialFactory)
        val localProfile = localProfileService.getOrCreate(configuration.displayName, configuration.capabilities)
        val localEndpoint = PeerEndpoint(
            scheme = if (configuration.featureFlags.inMemoryTransport) "memory" else "http",
            host = configuration.bindHost,
            port = configuration.httpPort,
            announcedPeerId = localProfile.peerId,
            announcedAt = kotlinx.datetime.Clock.System.now(),
        )

        val packetSerializationService = PacketSerializationService()
        val messageEncryptionService = MessageEncryptionService(cryptoPort, packetSerializationService)
        val packetEnvelopeFactory = PacketEnvelopeFactory()
        val packetSignatureService = PacketSignatureService(cryptoPort)
        val eventLogService = EventLogService(eventLogRepositoryPort)
        val nodeMetricsService = NodeMetricsService()
        val securityIncidentService = SecurityIncidentService(eventLogService, nodeMetricsService)
        val peerTrustVerificationService = PeerTrustVerificationService(peerRepositoryPort)
        val blockListService = BlockListService(blockListRepositoryPort)
        val retryPolicyService = RetryPolicyService()
        val deduplicationService = DeduplicationService(dedupCachePort)
        val rateLimitService = RateLimitService(RateLimitRule("default", RateLimitScope.PEER, 100, 10))
        val packetTransportPort = if (configuration.featureFlags.inMemoryTransport) {
            InMemoryPacketTransportAdapter()
        } else {
            KtorPacketTransportAdapter()
        }
        val relayClient = if (configuration.relayClientSettings?.enabled == true || configuration.featureFlags.relayEnabled) {
            RendezvousRelayClient(packetTransportPort, configuration.relayClientSettings?.forceRelayLookup == true)
        } else {
            null
        }
        val liveConnectivityStrategyService = ConnectivityStrategyService(
            routeRepositoryPort,
            endpointCachePort,
            relayClient,
            relayClient,
            configuration.relayClientSettings?.forceRelayLookup == true,
        )
        val liveRoutingService = RoutingService(
            routeRepositoryPort,
            endpointCachePort,
            liveConnectivityStrategyService,
            eventLogService,
            nodeMetricsService,
            configuration.relayClientSettings?.forceRelayLookup == true,
        )
        val deliveryTrackingService = DeliveryTrackingService(
            packetTransportPort,
            relayClient,
            liveRoutingService,
            outgoingQueuePort,
            pendingAckRepositoryPort,
            messageRepositoryPort,
            retryPolicyService,
            eventLogService,
            nodeMetricsService,
        )
        val relayService = RelayService(packetTransportPort, relayClient, liveRoutingService, eventLogService, nodeMetricsService)
        val retrySchedulerService = RetrySchedulerService(deliveryTrackingService, configuration.retrySettings.pollIntervalMillis)
        val discoveryPort = when {
            !configuration.featureFlags.discoveryEnabled -> null
            configuration.featureFlags.inMemoryDiscovery -> InMemoryDiscoveryAdapter()
            else -> UdpDiscoveryAdapter(configuration.discoveryPort, configuration.multicastGroup, JvmMulticastSupportAdapter())
        }
        val discoveryOrchestrationService = discoveryPort?.let {
            DiscoveryOrchestrationService(cryptoPort, endpointCachePort, liveRoutingService, eventLogService, nodeMetricsService)
        }

        val fileTransferService = FileTransferService(
            cryptoPort,
            localProfileService,
            peerTrustVerificationService,
            messageEncryptionService,
            packetEnvelopeFactory,
            packetSignatureService,
            deliveryTrackingService,
            fileTransferRepositoryPort,
            FileSystemChunkStorageAdapter(configuration.fileTransferSettings.downloadDirectory),
            FileResumeService(),
            eventLogService,
            nodeMetricsService,
            configuration.fileTransferSettings.downloadDirectory,
            configuration.fileTransferSettings.chunkSizeBytes,
        )
        val pairingService = PairingService(
            localProfileService,
            peerRepositoryPort,
            pairingSessionRepositoryPort,
            endpointCachePort,
            cryptoPort,
            messageEncryptionService,
            packetEnvelopeFactory,
            packetSignatureService,
            deliveryTrackingService,
            eventLogService,
            nodeMetricsService,
            securityIncidentService,
            localEndpointProvider = { localEndpoint },
        )
        val chatMessagingService = ChatMessagingService(
            localProfileService,
            conversationRepositoryPort,
            messageRepositoryPort,
            peerTrustVerificationService,
            blockListService,
            messageEncryptionService,
            packetEnvelopeFactory,
            packetSignatureService,
            deliveryTrackingService,
            eventLogService,
            nodeMetricsService,
        )
        val callSignalingService = CallSignalingService(
            localProfileService,
            peerTrustVerificationService,
            messageEncryptionService,
            packetEnvelopeFactory,
            packetSignatureService,
            deliveryTrackingService,
            callSessionRepositoryPort,
            eventLogService,
            nodeMetricsService,
        )

        var server: ApplicationEngine? = null
        lateinit var lifecycleService: NodeLifecycleService
        lifecycleService = NodeLifecycleService(
            configuration = configuration,
            localProfile = localProfile,
            localEndpoint = localEndpoint,
            localProfileService = localProfileService,
            pairingService = pairingService,
            chatMessagingService = chatMessagingService,
            fileTransferService = fileTransferService,
            callSignalingService = callSignalingService,
            discoveryPort = discoveryPort,
            discoveryOrchestrationService = discoveryOrchestrationService,
            reversePathRepositoryPort = reversePathRepositoryPort,
            endpointCachePort = endpointCachePort,
            messageEncryptionService = messageEncryptionService,
            packetSignatureService = packetSignatureService,
            peerTrustVerificationService = peerTrustVerificationService,
            blockListService = blockListService,
            rateLimitService = rateLimitService,
            deduplicationService = deduplicationService,
            routingService = liveRoutingService,
            relayService = relayService,
            deliveryTrackingService = deliveryTrackingService,
            retrySchedulerService = retrySchedulerService,
            eventLogService = eventLogService,
            nodeMetricsService = nodeMetricsService,
            securityIncidentService = securityIncidentService,
            rendezvousRegistryPort = relayClient,
            onStart = { server?.start(wait = false) },
            onStop = { server?.stop(1_000, 1_000) },
        )
        val packetController = PacketController(lifecycleService)
        val routeHandler = PacketRouteHandler(packetController)
        server = if (configuration.featureFlags.inMemoryTransport) null else createServer(configuration, routeHandler)
        if (configuration.featureFlags.inMemoryTransport) {
            InMemoryPacketTransportAdapter.register(localEndpoint) { envelope -> lifecycleService.handleIncomingPacket(envelope) }
        }

        configuration.staticPeers.forEach {
            endpointCachePort.put(
                PeerEndpointCandidate(
                    peerId = it.peerId,
                    endpoint = PeerEndpoint(host = it.host, port = it.port, announcedAt = kotlinx.datetime.Clock.System.now()),
                    source = EndpointSource.MANUAL_HINT,
                    discoveredAt = kotlinx.datetime.Clock.System.now(),
                ),
            )
        }

        lifecycleService.start()
        val runtime = MeshNodeRuntime(configuration, localProfile, localEndpoint, lifecycleService, server)
        return MeshNodeComponents(
            runtime = runtime,
            peerRepositoryPort = peerRepositoryPort,
            pairingSessionRepositoryPort = pairingSessionRepositoryPort,
            blockListRepositoryPort = blockListRepositoryPort,
            conversationRepositoryPort = conversationRepositoryPort,
            messageRepositoryPort = messageRepositoryPort,
            fileTransferRepositoryPort = fileTransferRepositoryPort,
            callSessionRepositoryPort = callSessionRepositoryPort,
            endpointCachePort = endpointCachePort,
            routeRepositoryPort = routeRepositoryPort,
            blockListService = blockListService,
            chatMessagingService = chatMessagingService,
            fileTransferService = fileTransferService,
            callSignalingService = callSignalingService,
            routingService = liveRoutingService,
        )
    }

    private fun createServer(configuration: NodeConfiguration, routeHandler: PacketRouteHandler): ApplicationEngine {
        return embeddedServer(Netty, host = configuration.bindHost, port = configuration.httpPort) {
            install(CallLogging)
            install(ContentNegotiation) {
                json(Json { encodeDefaults = true; ignoreUnknownKeys = true })
            }
            routing {
                routeHandler.install(this)
            }
        }
    }
}
