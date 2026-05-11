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
import org.expert.link.mesh.application.service.CallMediaService
import org.expert.link.mesh.application.service.CentralAttachmentSyncService
import org.expert.link.mesh.application.service.CentralAuthService
import org.expert.link.mesh.application.service.CentralExportIntegrationService
import org.expert.link.mesh.application.service.CentralOrganizationAccessService
import org.expert.link.mesh.application.service.CentralOrganizationWorkspaceService
import org.expert.link.mesh.application.service.CentralSyncOrchestrationService
import org.expert.link.mesh.application.service.ConflictStateService
import org.expert.link.mesh.application.service.ChatMessagingService
import org.expert.link.mesh.application.service.ConnectivityModeService
import org.expert.link.mesh.application.service.ConnectivityStrategyService
import org.expert.link.mesh.application.service.DeduplicationService
import org.expert.link.mesh.application.service.DeliveryTrackingService
import org.expert.link.mesh.application.service.DiscoveryOrchestrationService
import org.expert.link.mesh.application.service.EventLogService
import org.expert.link.mesh.application.service.FileResumeService
import org.expert.link.mesh.application.service.FileTransferService
import org.expert.link.mesh.application.service.GroupChatService
import org.expert.link.mesh.application.service.InventoryCatalogService
import org.expert.link.mesh.application.service.InventoryChangeLogService
import org.expert.link.mesh.application.service.InventoryCodeService
import org.expert.link.mesh.application.service.InventoryDashboardService
import org.expert.link.mesh.application.service.InventoryDiscussionService
import org.expert.link.mesh.application.service.InventoryEventApplier
import org.expert.link.mesh.application.service.InventoryEventService
import org.expert.link.mesh.application.service.InventoryExportService
import org.expert.link.mesh.application.service.InventoryIncidentService
import org.expert.link.mesh.application.service.InventoryItemService
import org.expert.link.mesh.application.service.InventoryLabelService
import org.expert.link.mesh.application.service.InventoryOrganizationService
import org.expert.link.mesh.application.service.InventoryOwnershipService
import org.expert.link.mesh.application.service.InventoryQueryService
import org.expert.link.mesh.application.service.InventoryRbacService
import org.expert.link.mesh.application.service.InventoryReviewService
import org.expert.link.mesh.application.service.InventorySearchService
import org.expert.link.mesh.application.service.InventorySessionService
import org.expert.link.mesh.application.service.InventorySyncService
import org.expert.link.mesh.application.service.LocalProfileService
import org.expert.link.mesh.application.service.MessageEncryptionService
import org.expert.link.mesh.application.service.NodeMetricsService
import org.expert.link.mesh.application.service.OnlineOfflineStateService
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
import org.expert.link.mesh.application.service.SyncQueueService
import org.expert.link.mesh.application.service.ThreadService
import org.expert.link.mesh.application.service.TopologyStateService
import org.expert.link.mesh.bootstrap.config.NodeConfiguration
import org.expert.link.mesh.controller.PacketController
import org.expert.link.mesh.controller.PacketRouteController
import org.expert.link.mesh.bootstrap.runtime.MeshNodeComponents
import org.expert.link.mesh.bootstrap.runtime.MeshNodeRuntime
import org.expert.link.mesh.domain.model.network.EndpointSource
import org.expert.link.mesh.domain.model.network.PeerEndpointCandidate
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.security.RateLimitRule
import org.expert.link.mesh.domain.model.security.RateLimitScope
import org.expert.link.mesh.domain.port.repository.PersistentRepositoryBundle
import org.expert.link.mesh.domain.port.external.CentralClientBundle
import org.expert.link.mesh.infrastructure.adapter.JvmMulticastSupportAdapter
import org.expert.link.mesh.infrastructure.adapter.JvmNetworkEnvironmentAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryDedupCacheAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryEndpointCacheAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryReversePathRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryRouteRepositoryAdapter
import org.expert.link.mesh.infrastructure.client.RendezvousRelayClient
import org.expert.link.mesh.infrastructure.client.DefaultCentralClientFactory
import org.expert.link.mesh.infrastructure.adapter.BasicCryptoAdapter
import org.expert.link.mesh.infrastructure.adapter.InMemoryDiscoveryAdapter
import org.expert.link.mesh.infrastructure.adapter.UdpDiscoveryAdapter
import org.expert.link.mesh.infrastructure.repository.FileSystemChunkStorageAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryBlockListRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCallEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCallParticipantRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCallRoomRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCallSessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryConversationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryEventLogRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryFileTransferRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryGroupChatRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryGroupEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryLocalProfileRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryMessageRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOutgoingQueueAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryPairingSessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryPeerRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryPendingAckRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryThreadMessageRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryThreadRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryChatMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAttachmentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAlertRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAttributeDefinitionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCategoryRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCategoryTemplateRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryChangeLogRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCommentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeBindingRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryConflictRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCostCenterRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryDeadlineRuleRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryDepartmentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryDashboardRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryExportRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryFundingSourceRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryIncidentRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryItemRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLabelRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLabelTemplateRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLegalHolderRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLocationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryOwnerRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryPrintTaskRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryQrCodeRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryReminderRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryRevisionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryReviewRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryScanEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventorySessionMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventorySessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventorySubcategoryRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventorySupplierRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryTagRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCentralAuthSessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCentralOrganizationAccessRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCentralOrganizationWorkspaceRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCentralSyncStateRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryInventoryThresholdRuleRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryRoleRepositoryAdapter
import org.expert.link.mesh.infrastructure.label.PdfBoxInventoryLabelRendererAdapter
import org.expert.link.mesh.infrastructure.adapter.InMemoryPacketTransportAdapter
import org.expert.link.mesh.infrastructure.adapter.KtorPacketTransportAdapter
import org.expert.link.mesh.infrastructure.adapter.NoopMediaEngineAdapter
import org.expert.link.mesh.domain.port.external.MediaEnginePort
import org.expert.link.mesh.domain.port.external.MulticastSupportPort

/** Сборщик runtime узла.
 *
 * Создаёт адаптеры, сервисы и запущенный экземпляр узла.
 * Это composition root backend-ядра: здесь связываются внутренние слои, а наружу отдаётся готовый runtime.
 */
class MeshNodeBootstrap {
    /**
     * Creates, starts and returns a fully wired node runtime.
     */
    suspend fun bootstrap(
        configuration: NodeConfiguration,
        mediaEnginePort: MediaEnginePort? = null,
        multicastSupportPort: MulticastSupportPort? = null,
        persistentRepositories: PersistentRepositoryBundle? = null,
        centralClients: CentralClientBundle? = null,
    ): MeshNodeRuntime {
        return bootstrapComponents(configuration, mediaEnginePort, multicastSupportPort, persistentRepositories, centralClients).runtime
    }

    /**
     * Creates, starts and returns the complete runtime plus internal services required by the public facade.
     */
    suspend fun bootstrapComponents(
        configuration: NodeConfiguration,
        mediaEnginePort: MediaEnginePort? = null,
        multicastSupportPort: MulticastSupportPort? = null,
        persistentRepositories: PersistentRepositoryBundle? = null,
        centralClients: CentralClientBundle? = null,
    ): MeshNodeComponents {
        val localProfileRepositoryPort = persistentRepositories?.localProfileRepositoryPort ?: InMemoryLocalProfileRepositoryAdapter()
        val peerRepositoryPort = persistentRepositories?.peerRepositoryPort ?: InMemoryPeerRepositoryAdapter()
        val pairingSessionRepositoryPort = persistentRepositories?.pairingSessionRepositoryPort ?: InMemoryPairingSessionRepositoryAdapter()
        val blockListRepositoryPort = persistentRepositories?.blockListRepositoryPort ?: InMemoryBlockListRepositoryAdapter()
        val conversationRepositoryPort = persistentRepositories?.conversationRepositoryPort ?: InMemoryConversationRepositoryAdapter()
        val messageRepositoryPort = persistentRepositories?.messageRepositoryPort ?: InMemoryMessageRepositoryAdapter()
        val outgoingQueuePort = InMemoryOutgoingQueueAdapter()
        val pendingAckRepositoryPort = InMemoryPendingAckRepositoryAdapter()
        val endpointCachePort = InMemoryEndpointCacheAdapter()
        val routeRepositoryPort = InMemoryRouteRepositoryAdapter()
        val reversePathRepositoryPort = InMemoryReversePathRepositoryAdapter()
        val dedupCachePort = InMemoryDedupCacheAdapter()
        val fileTransferRepositoryPort = persistentRepositories?.fileTransferRepositoryPort ?: InMemoryFileTransferRepositoryAdapter()
        val callSessionRepositoryPort = persistentRepositories?.callSessionRepositoryPort ?: InMemoryCallSessionRepositoryAdapter()
        val callRoomRepositoryPort = persistentRepositories?.callRoomRepositoryPort ?: InMemoryCallRoomRepositoryAdapter()
        val callParticipantRepositoryPort = persistentRepositories?.callParticipantRepositoryPort ?: InMemoryCallParticipantRepositoryAdapter()
        val callEventRepositoryPort = persistentRepositories?.callEventRepositoryPort ?: InMemoryCallEventRepositoryAdapter()
        val effectiveMediaEnginePort = mediaEnginePort ?: NoopMediaEngineAdapter()
        val eventLogRepositoryPort = persistentRepositories?.eventLogRepositoryPort ?: InMemoryEventLogRepositoryAdapter()
        val groupChatRepositoryPort = persistentRepositories?.groupChatRepositoryPort ?: InMemoryGroupChatRepositoryAdapter()
        val chatMemberRepositoryPort = persistentRepositories?.chatMemberRepositoryPort ?: InMemoryChatMemberRepositoryAdapter()
        val threadRepositoryPort = persistentRepositories?.threadRepositoryPort ?: InMemoryThreadRepositoryAdapter()
        val threadMessageRepositoryPort = persistentRepositories?.threadMessageRepositoryPort ?: InMemoryThreadMessageRepositoryAdapter()
        val groupEventRepositoryPort = persistentRepositories?.groupEventRepositoryPort ?: InMemoryGroupEventRepositoryAdapter()
        val organizationRepositoryPort = persistentRepositories?.organizationRepositoryPort ?: InMemoryOrganizationRepositoryAdapter()
        val organizationMemberRepositoryPort = persistentRepositories?.organizationMemberRepositoryPort ?: InMemoryOrganizationMemberRepositoryAdapter()
        val roleRepositoryPort = persistentRepositories?.roleRepositoryPort ?: InMemoryRoleRepositoryAdapter()
        val inventoryCategoryRepositoryPort = persistentRepositories?.inventoryCategoryRepositoryPort ?: InMemoryInventoryCategoryRepositoryAdapter()
        val inventorySubcategoryRepositoryPort =
            persistentRepositories?.inventorySubcategoryRepositoryPort ?: InMemoryInventorySubcategoryRepositoryAdapter()
        val inventoryTagRepositoryPort = persistentRepositories?.inventoryTagRepositoryPort ?: InMemoryInventoryTagRepositoryAdapter()
        val inventoryAttributeDefinitionRepositoryPort =
            persistentRepositories?.inventoryAttributeDefinitionRepositoryPort ?: InMemoryInventoryAttributeDefinitionRepositoryAdapter()
        val inventoryCategoryTemplateRepositoryPort =
            persistentRepositories?.inventoryCategoryTemplateRepositoryPort ?: InMemoryInventoryCategoryTemplateRepositoryAdapter()
        val inventoryLocationRepositoryPort = persistentRepositories?.inventoryLocationRepositoryPort ?: InMemoryInventoryLocationRepositoryAdapter()
        val inventoryOwnerRepositoryPort = persistentRepositories?.inventoryOwnerRepositoryPort ?: InMemoryInventoryOwnerRepositoryAdapter()
        val inventoryDepartmentRepositoryPort = persistentRepositories?.inventoryDepartmentRepositoryPort ?: InMemoryInventoryDepartmentRepositoryAdapter()
        val inventoryCostCenterRepositoryPort = persistentRepositories?.inventoryCostCenterRepositoryPort ?: InMemoryInventoryCostCenterRepositoryAdapter()
        val inventoryLegalHolderRepositoryPort =
            persistentRepositories?.inventoryLegalHolderRepositoryPort ?: InMemoryInventoryLegalHolderRepositoryAdapter()
        val inventorySupplierRepositoryPort = persistentRepositories?.inventorySupplierRepositoryPort ?: InMemoryInventorySupplierRepositoryAdapter()
        val inventoryFundingSourceRepositoryPort =
            persistentRepositories?.inventoryFundingSourceRepositoryPort ?: InMemoryInventoryFundingSourceRepositoryAdapter()
        val inventoryItemRepositoryPort = persistentRepositories?.inventoryItemRepositoryPort ?: InMemoryInventoryItemRepositoryAdapter()
        val inventorySessionRepositoryPort = persistentRepositories?.inventorySessionRepositoryPort ?: InMemoryInventorySessionRepositoryAdapter()
        val inventorySessionMemberRepositoryPort =
            persistentRepositories?.inventorySessionMemberRepositoryPort ?: InMemoryInventorySessionMemberRepositoryAdapter()
        val inventoryReviewRepositoryPort = persistentRepositories?.inventoryReviewRepositoryPort ?: InMemoryInventoryReviewRepositoryAdapter()
        val inventoryCommentRepositoryPort = persistentRepositories?.inventoryCommentRepositoryPort ?: InMemoryInventoryCommentRepositoryAdapter()
        val inventoryAttachmentRepositoryPort =
            persistentRepositories?.inventoryAttachmentRepositoryPort ?: InMemoryInventoryAttachmentRepositoryAdapter()
        val inventoryIncidentRepositoryPort = persistentRepositories?.inventoryIncidentRepositoryPort ?: InMemoryInventoryIncidentRepositoryAdapter()
        val inventoryAlertRepositoryPort = persistentRepositories?.inventoryAlertRepositoryPort ?: InMemoryInventoryAlertRepositoryAdapter()
        val inventoryReminderRepositoryPort = persistentRepositories?.inventoryReminderRepositoryPort ?: InMemoryInventoryReminderRepositoryAdapter()
        val inventoryThresholdRuleRepositoryPort =
            persistentRepositories?.inventoryThresholdRuleRepositoryPort ?: InMemoryInventoryThresholdRuleRepositoryAdapter()
        val inventoryDeadlineRuleRepositoryPort =
            persistentRepositories?.inventoryDeadlineRuleRepositoryPort ?: InMemoryInventoryDeadlineRuleRepositoryAdapter()
        val inventoryChangeLogRepositoryPort =
            persistentRepositories?.inventoryChangeLogRepositoryPort ?: InMemoryInventoryChangeLogRepositoryAdapter()
        val inventoryDashboardRepositoryPort =
            persistentRepositories?.inventoryDashboardRepositoryPort ?: InMemoryInventoryDashboardRepositoryAdapter()
        val inventoryCodeBindingRepositoryPort =
            persistentRepositories?.inventoryCodeBindingRepositoryPort ?: InMemoryInventoryCodeBindingRepositoryAdapter()
        val inventoryCodeRepositoryPort =
            persistentRepositories?.inventoryCodeRepositoryPort ?: InMemoryInventoryCodeRepositoryAdapter()
        val inventoryLabelTemplateRepositoryPort =
            persistentRepositories?.inventoryLabelTemplateRepositoryPort ?: InMemoryInventoryLabelTemplateRepositoryAdapter()
        val inventoryLabelRepositoryPort =
            persistentRepositories?.inventoryLabelRepositoryPort ?: InMemoryInventoryLabelRepositoryAdapter()
        val inventoryPrintTaskRepositoryPort =
            persistentRepositories?.inventoryPrintTaskRepositoryPort ?: InMemoryInventoryPrintTaskRepositoryAdapter()
        val inventoryScanEventRepositoryPort =
            persistentRepositories?.inventoryScanEventRepositoryPort ?: InMemoryInventoryScanEventRepositoryAdapter()
        val inventoryRevisionRepositoryPort =
            persistentRepositories?.inventoryRevisionRepositoryPort ?: InMemoryInventoryRevisionRepositoryAdapter()
        val inventoryConflictRepositoryPort =
            persistentRepositories?.inventoryConflictRepositoryPort ?: InMemoryInventoryConflictRepositoryAdapter()
        val inventoryEventRepositoryPort = persistentRepositories?.inventoryEventRepositoryPort ?: InMemoryInventoryEventRepositoryAdapter()
        val inventoryExportRepositoryPort = persistentRepositories?.inventoryExportRepositoryPort ?: InMemoryInventoryExportRepositoryAdapter()
        val inventoryQrCodeRepositoryPort = persistentRepositories?.inventoryQrCodeRepositoryPort ?: InMemoryInventoryQrCodeRepositoryAdapter()
        val centralAuthSessionRepositoryPort =
            persistentRepositories?.centralAuthSessionRepositoryPort ?: InMemoryCentralAuthSessionRepositoryAdapter()
        val centralOrganizationAccessRepositoryPort =
            persistentRepositories?.centralOrganizationAccessRepositoryPort ?: InMemoryCentralOrganizationAccessRepositoryAdapter()
        val centralSyncStateRepositoryPort =
            persistentRepositories?.centralSyncStateRepositoryPort ?: InMemoryCentralSyncStateRepositoryAdapter()
        val centralOrganizationWorkspaceRepositoryPort =
            persistentRepositories?.centralOrganizationWorkspaceRepositoryPort ?: InMemoryCentralOrganizationWorkspaceRepositoryAdapter()
        val effectiveCentralClients = centralClients ?: configuration.centralConfiguration?.let {
            DefaultCentralClientFactory().create(
                configuration = it,
                authSessionRepositoryPort = centralAuthSessionRepositoryPort,
                organizationAccessRepositoryPort = centralOrganizationAccessRepositoryPort,
            )
        }

        val cryptoPort = BasicCryptoAdapter()
        val keyMaterialFactory = KeyMaterialFactory(cryptoPort)
        val localProfileService = LocalProfileService(localProfileRepositoryPort, keyMaterialFactory)
        val localProfile = localProfileService.getOrCreate(configuration.displayName, configuration.capabilities)
        val networkEnvironmentPort = JvmNetworkEnvironmentAdapter()
        val announcedHost = resolveAnnouncedHost(configuration.bindHost, networkEnvironmentPort.localAddresses())
        val localEndpoint = PeerEndpoint(
            scheme = if (configuration.featureFlags.inMemoryTransport) "memory" else "http",
            host = announcedHost,
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
        val topologyStateService = TopologyStateService(
            routeRepositoryPort = routeRepositoryPort,
            endpointCachePort = endpointCachePort,
            pendingAckRepositoryPort = pendingAckRepositoryPort,
            outgoingQueuePort = outgoingQueuePort,
            fileTransferRepositoryPort = fileTransferRepositoryPort,
            callSessionRepositoryPort = callSessionRepositoryPort,
            connectivityStrategyService = liveConnectivityStrategyService,
        )
        val liveRoutingService = RoutingService(
            routeRepositoryPort,
            endpointCachePort,
            liveConnectivityStrategyService,
            eventLogService,
            nodeMetricsService,
            configuration.relayClientSettings?.forceRelayLookup == true,
            topologyStateService,
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
            topologyStateService,
        )
        val relayService = RelayService(
            packetTransportPort,
            relayClient,
            liveRoutingService,
            eventLogService,
            nodeMetricsService,
            topologyStateService,
        )
        val retrySchedulerService = RetrySchedulerService(deliveryTrackingService, configuration.retrySettings.pollIntervalMillis)
        val effectiveMulticastSupportPort = multicastSupportPort ?: JvmMulticastSupportAdapter()
        val discoveryPort = when {
            !configuration.featureFlags.discoveryEnabled -> null
            configuration.featureFlags.inMemoryDiscovery -> InMemoryDiscoveryAdapter()
            else -> UdpDiscoveryAdapter(
                configuration.discoveryPort,
                configuration.multicastGroup,
                effectiveMulticastSupportPort,
            )
        }
        val discoveryOrchestrationService = discoveryPort?.let {
            DiscoveryOrchestrationService(
                cryptoPort,
                endpointCachePort,
                liveRoutingService,
                eventLogService,
                nodeMetricsService,
                topologyStateService,
            )
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
        val pairingService = PairingService(
            localProfileService,
            chatMessagingService,
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
        val groupChatService = GroupChatService(
            localProfileService = localProfileService,
            conversationRepositoryPort = conversationRepositoryPort,
            messageRepositoryPort = messageRepositoryPort,
            groupChatRepositoryPort = groupChatRepositoryPort,
            chatMemberRepositoryPort = chatMemberRepositoryPort,
            groupEventRepositoryPort = groupEventRepositoryPort,
            chatMessagingService = chatMessagingService,
            eventLogService = eventLogService,
            nodeMetricsService = nodeMetricsService,
        )
        val threadService = ThreadService(
            localProfileService = localProfileService,
            conversationRepositoryPort = conversationRepositoryPort,
            messageRepositoryPort = messageRepositoryPort,
            threadRepositoryPort = threadRepositoryPort,
            threadMessageRepositoryPort = threadMessageRepositoryPort,
            groupEventRepositoryPort = groupEventRepositoryPort,
            chatMessagingService = chatMessagingService,
            eventLogService = eventLogService,
            nodeMetricsService = nodeMetricsService,
        )
        val callSignalingService = CallSignalingService(
            localProfileService,
            peerTrustVerificationService,
            messageEncryptionService,
            packetEnvelopeFactory,
            packetSignatureService,
            deliveryTrackingService,
            callSessionRepositoryPort,
            callRoomRepositoryPort,
            callParticipantRepositoryPort,
            callEventRepositoryPort,
            eventLogService,
            nodeMetricsService,
        )
        val callMediaService = CallMediaService(
            localProfileService = localProfileService,
            callSignalingService = callSignalingService,
            mediaEnginePort = effectiveMediaEnginePort,
            eventLogService = eventLogService,
            nodeMetricsService = nodeMetricsService,
        )
        val inventoryRbacService = InventoryRbacService(roleRepositoryPort, organizationMemberRepositoryPort)
        val inventoryEventService = InventoryEventService(inventoryEventRepositoryPort)
        val inventoryEventApplier = InventoryEventApplier(
            localProfileService = localProfileService,
            organizationRepositoryPort = organizationRepositoryPort,
            organizationMemberRepositoryPort = organizationMemberRepositoryPort,
            roleRepositoryPort = roleRepositoryPort,
            categoryRepositoryPort = inventoryCategoryRepositoryPort,
            subcategoryRepositoryPort = inventorySubcategoryRepositoryPort,
            tagRepositoryPort = inventoryTagRepositoryPort,
            attributeDefinitionRepositoryPort = inventoryAttributeDefinitionRepositoryPort,
            categoryTemplateRepositoryPort = inventoryCategoryTemplateRepositoryPort,
            locationRepositoryPort = inventoryLocationRepositoryPort,
            ownerRepositoryPort = inventoryOwnerRepositoryPort,
            departmentRepositoryPort = inventoryDepartmentRepositoryPort,
            costCenterRepositoryPort = inventoryCostCenterRepositoryPort,
            legalHolderRepositoryPort = inventoryLegalHolderRepositoryPort,
            supplierRepositoryPort = inventorySupplierRepositoryPort,
            fundingSourceRepositoryPort = inventoryFundingSourceRepositoryPort,
            itemRepositoryPort = inventoryItemRepositoryPort,
            sessionRepositoryPort = inventorySessionRepositoryPort,
            reviewRepositoryPort = inventoryReviewRepositoryPort,
            commentRepositoryPort = inventoryCommentRepositoryPort,
            attachmentRepositoryPort = inventoryAttachmentRepositoryPort,
            incidentRepositoryPort = inventoryIncidentRepositoryPort,
            alertRepositoryPort = inventoryAlertRepositoryPort,
            reminderRepositoryPort = inventoryReminderRepositoryPort,
            thresholdRuleRepositoryPort = inventoryThresholdRuleRepositoryPort,
            deadlineRuleRepositoryPort = inventoryDeadlineRuleRepositoryPort,
            changeLogRepositoryPort = inventoryChangeLogRepositoryPort,
            dashboardRepositoryPort = inventoryDashboardRepositoryPort,
            codeRepositoryPort = inventoryCodeRepositoryPort,
            codeBindingRepositoryPort = inventoryCodeBindingRepositoryPort,
            labelTemplateRepositoryPort = inventoryLabelTemplateRepositoryPort,
            labelRepositoryPort = inventoryLabelRepositoryPort,
            printTaskRepositoryPort = inventoryPrintTaskRepositoryPort,
            scanEventRepositoryPort = inventoryScanEventRepositoryPort,
            revisionRepositoryPort = inventoryRevisionRepositoryPort,
            conflictRepositoryPort = inventoryConflictRepositoryPort,
            exportRepositoryPort = inventoryExportRepositoryPort,
            qrCodeRepositoryPort = inventoryQrCodeRepositoryPort,
            eventRepositoryPort = inventoryEventRepositoryPort,
            inventoryEventService = inventoryEventService,
        )
        val inventorySyncService = InventorySyncService(
            localProfileService = localProfileService,
            organizationMemberRepositoryPort = organizationMemberRepositoryPort,
            peerTrustVerificationService = peerTrustVerificationService,
            messageEncryptionService = messageEncryptionService,
            packetEnvelopeFactory = packetEnvelopeFactory,
            packetSignatureService = packetSignatureService,
            deliveryTrackingService = deliveryTrackingService,
            inventoryEventRepositoryPort = inventoryEventRepositoryPort,
            inventoryEventApplier = inventoryEventApplier,
            eventLogService = eventLogService,
            nodeMetricsService = nodeMetricsService,
        )
        val inventoryChangeLogService = InventoryChangeLogService(
            changeLogRepositoryPort = inventoryChangeLogRepositoryPort,
            inventoryEventService = inventoryEventService,
            inventorySyncService = inventorySyncService,
        )
        val inventoryOrganizationService = InventoryOrganizationService(
            localProfileService = localProfileService,
            organizationRepositoryPort = organizationRepositoryPort,
            organizationMemberRepositoryPort = organizationMemberRepositoryPort,
            roleRepositoryPort = roleRepositoryPort,
            rbacService = inventoryRbacService,
            inventoryEventService = inventoryEventService,
            inventorySyncService = inventorySyncService,
        )
        val inventoryCatalogService = InventoryCatalogService(
            localProfileService = localProfileService,
            categoryRepositoryPort = inventoryCategoryRepositoryPort,
            subcategoryRepositoryPort = inventorySubcategoryRepositoryPort,
            tagRepositoryPort = inventoryTagRepositoryPort,
            attributeDefinitionRepositoryPort = inventoryAttributeDefinitionRepositoryPort,
            categoryTemplateRepositoryPort = inventoryCategoryTemplateRepositoryPort,
            locationRepositoryPort = inventoryLocationRepositoryPort,
            rbacService = inventoryRbacService,
            inventoryEventService = inventoryEventService,
            inventorySyncService = inventorySyncService,
        )
        val inventoryOwnershipService = InventoryOwnershipService(
            localProfileService = localProfileService,
            ownerRepositoryPort = inventoryOwnerRepositoryPort,
            departmentRepositoryPort = inventoryDepartmentRepositoryPort,
            costCenterRepositoryPort = inventoryCostCenterRepositoryPort,
            legalHolderRepositoryPort = inventoryLegalHolderRepositoryPort,
            supplierRepositoryPort = inventorySupplierRepositoryPort,
            fundingSourceRepositoryPort = inventoryFundingSourceRepositoryPort,
            rbacService = inventoryRbacService,
            inventoryEventService = inventoryEventService,
            inventorySyncService = inventorySyncService,
        )
        val inventoryItemService = InventoryItemService(
            localProfileService = localProfileService,
            itemRepositoryPort = inventoryItemRepositoryPort,
            commentRepositoryPort = inventoryCommentRepositoryPort,
            attachmentRepositoryPort = inventoryAttachmentRepositoryPort,
            qrCodeRepositoryPort = inventoryQrCodeRepositoryPort,
            codeRepositoryPort = inventoryCodeRepositoryPort,
            rbacService = inventoryRbacService,
            inventoryEventService = inventoryEventService,
            inventoryChangeLogService = inventoryChangeLogService,
            inventorySyncService = inventorySyncService,
        )
        val inventorySessionService = InventorySessionService(
            localProfileService = localProfileService,
            sessionRepositoryPort = inventorySessionRepositoryPort,
            sessionMemberRepositoryPort = inventorySessionMemberRepositoryPort,
            itemRepositoryPort = inventoryItemRepositoryPort,
            rbacService = inventoryRbacService,
            inventoryEventService = inventoryEventService,
            inventoryChangeLogService = inventoryChangeLogService,
            inventorySyncService = inventorySyncService,
        )
        val inventoryReviewService = InventoryReviewService(
            localProfileService = localProfileService,
            itemRepositoryPort = inventoryItemRepositoryPort,
            sessionRepositoryPort = inventorySessionRepositoryPort,
            reviewRepositoryPort = inventoryReviewRepositoryPort,
            attachmentRepositoryPort = inventoryAttachmentRepositoryPort,
            incidentRepositoryPort = inventoryIncidentRepositoryPort,
            rbacService = inventoryRbacService,
            inventoryEventService = inventoryEventService,
            inventorySyncService = inventorySyncService,
        )
        val inventoryIncidentService = InventoryIncidentService(
            localProfileService = localProfileService,
            incidentRepositoryPort = inventoryIncidentRepositoryPort,
            alertRepositoryPort = inventoryAlertRepositoryPort,
            reminderRepositoryPort = inventoryReminderRepositoryPort,
            thresholdRuleRepositoryPort = inventoryThresholdRuleRepositoryPort,
            deadlineRuleRepositoryPort = inventoryDeadlineRuleRepositoryPort,
            itemRepositoryPort = inventoryItemRepositoryPort,
            sessionRepositoryPort = inventorySessionRepositoryPort,
            rbacService = inventoryRbacService,
            inventoryEventService = inventoryEventService,
            inventorySyncService = inventorySyncService,
        )
        val inventoryExportService = InventoryExportService(
            localProfileService = localProfileService,
            sessionRepositoryPort = inventorySessionRepositoryPort,
            exportRepositoryPort = inventoryExportRepositoryPort,
            rbacService = inventoryRbacService,
            inventoryEventService = inventoryEventService,
            inventorySyncService = inventorySyncService,
        )
        val inventoryDashboardService = InventoryDashboardService(
            localProfileService = localProfileService,
            itemRepositoryPort = inventoryItemRepositoryPort,
            incidentRepositoryPort = inventoryIncidentRepositoryPort,
            sessionRepositoryPort = inventorySessionRepositoryPort,
            dashboardRepositoryPort = inventoryDashboardRepositoryPort,
            inventoryEventService = inventoryEventService,
            inventorySyncService = inventorySyncService,
        )
        val inventorySearchService = InventorySearchService(
            itemRepositoryPort = inventoryItemRepositoryPort,
            incidentRepositoryPort = inventoryIncidentRepositoryPort,
        )
        val inventoryCodeService = InventoryCodeService(
            localProfileService = localProfileService,
            codeBindingRepositoryPort = inventoryCodeBindingRepositoryPort,
            scanEventRepositoryPort = inventoryScanEventRepositoryPort,
            qrCodeRepositoryPort = inventoryQrCodeRepositoryPort,
            codeRepositoryPort = inventoryCodeRepositoryPort,
            itemRepositoryPort = inventoryItemRepositoryPort,
            inventoryItemService = inventoryItemService,
            rbacService = inventoryRbacService,
            inventoryEventService = inventoryEventService,
            inventorySyncService = inventorySyncService,
        )
        val labelRendererPort = PdfBoxInventoryLabelRendererAdapter(configuration.fileTransferSettings.downloadDirectory)
        val inventoryLabelService = InventoryLabelService(
            localProfileService = localProfileService,
            labelTemplateRepositoryPort = inventoryLabelTemplateRepositoryPort,
            labelRepositoryPort = inventoryLabelRepositoryPort,
            printTaskRepositoryPort = inventoryPrintTaskRepositoryPort,
            codeRepositoryPort = inventoryCodeRepositoryPort,
            itemRepositoryPort = inventoryItemRepositoryPort,
            organizationRepositoryPort = organizationRepositoryPort,
            locationRepositoryPort = inventoryLocationRepositoryPort,
            departmentRepositoryPort = inventoryDepartmentRepositoryPort,
            rbacService = inventoryRbacService,
            inventoryEventService = inventoryEventService,
            inventorySyncService = inventorySyncService,
            rendererPort = labelRendererPort,
        )
        val inventoryQueryService = InventoryQueryService(
            itemRepositoryPort = inventoryItemRepositoryPort,
            qrCodeRepositoryPort = inventoryQrCodeRepositoryPort,
            codeRepositoryPort = inventoryCodeRepositoryPort,
        )
        val inventoryDiscussionService = InventoryDiscussionService(
            groupChatService = groupChatService,
            threadService = threadService,
        )
        val connectivityModeService = ConnectivityModeService(
            configuration = configuration.centralConfiguration,
            syncStateRepositoryPort = centralSyncStateRepositoryPort,
        )
        val syncQueueService = SyncQueueService(
            inventoryEventRepositoryPort = inventoryEventRepositoryPort,
            syncStateRepositoryPort = centralSyncStateRepositoryPort,
        )
        val centralAuthService = CentralAuthService(
            configuration = configuration.centralConfiguration,
            authClientPort = effectiveCentralClients?.authClientPort,
            authSessionRepositoryPort = centralAuthSessionRepositoryPort,
        )
        val centralOrganizationAccessService = CentralOrganizationAccessService(
            localProfileService = localProfileService,
            organizationAccessClientPort = effectiveCentralClients?.organizationAccessClientPort,
            authSessionRepositoryPort = centralAuthSessionRepositoryPort,
            centralOrganizationAccessRepositoryPort = centralOrganizationAccessRepositoryPort,
            organizationRepositoryPort = organizationRepositoryPort,
            organizationMemberRepositoryPort = organizationMemberRepositoryPort,
            roleRepositoryPort = roleRepositoryPort,
        )
        val centralOrganizationWorkspaceService = CentralOrganizationWorkspaceService(
            organizationAccessClientPort = effectiveCentralClients?.organizationAccessClientPort,
            relayClientPort = effectiveCentralClients?.relayClientPort,
            authSessionRepositoryPort = centralAuthSessionRepositoryPort,
            workspaceRepositoryPort = centralOrganizationWorkspaceRepositoryPort,
            locationRepositoryPort = inventoryLocationRepositoryPort,
            departmentRepositoryPort = inventoryDepartmentRepositoryPort,
            costCenterRepositoryPort = inventoryCostCenterRepositoryPort,
        )
        val conflictStateService = ConflictStateService(
            syncClientPort = effectiveCentralClients?.syncClientPort,
            inventoryConflictRepositoryPort = inventoryConflictRepositoryPort,
        )
        val onlineOfflineStateService = OnlineOfflineStateService(
            configuration = configuration.centralConfiguration,
            authSessionRepositoryPort = centralAuthSessionRepositoryPort,
            centralOrganizationAccessRepositoryPort = centralOrganizationAccessRepositoryPort,
            centralSyncStateRepositoryPort = centralSyncStateRepositoryPort,
        )
        val centralAttachmentSyncService = CentralAttachmentSyncService(
            attachmentClientPort = effectiveCentralClients?.attachmentClientPort,
            attachmentRepositoryPort = inventoryAttachmentRepositoryPort,
            fileTransferRepositoryPort = fileTransferRepositoryPort,
            syncQueueService = syncQueueService,
        )
        val centralExportIntegrationService = CentralExportIntegrationService(
            localProfileService = localProfileService,
            exportClientPort = effectiveCentralClients?.exportClientPort,
            exportRepositoryPort = inventoryExportRepositoryPort,
        )
        val centralSyncOrchestrationService = CentralSyncOrchestrationService(
            configuration = configuration.centralConfiguration,
            localProfileService = localProfileService,
            connectivityModeService = connectivityModeService,
            syncQueueService = syncQueueService,
            centralOrganizationAccessService = centralOrganizationAccessService,
            centralOrganizationWorkspaceService = centralOrganizationWorkspaceService,
            inventoryClientPort = effectiveCentralClients?.inventoryClientPort,
            syncClientPort = effectiveCentralClients?.syncClientPort,
            attachmentSyncService = centralAttachmentSyncService,
            inventoryEventApplier = inventoryEventApplier,
            inventoryConflictRepositoryPort = inventoryConflictRepositoryPort,
            organizationRepositoryPort = organizationRepositoryPort,
            organizationMemberRepositoryPort = organizationMemberRepositoryPort,
            roleRepositoryPort = roleRepositoryPort,
            itemRepositoryPort = inventoryItemRepositoryPort,
            sessionRepositoryPort = inventorySessionRepositoryPort,
            centralSyncStateRepositoryPort = centralSyncStateRepositoryPort,
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
            callMediaService = callMediaService,
            inventorySyncService = inventorySyncService,
            discoveryPort = discoveryPort,
            discoveryOrchestrationService = discoveryOrchestrationService,
            reversePathRepositoryPort = reversePathRepositoryPort,
            peerRepositoryPort = peerRepositoryPort,
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
            topologyStateService = topologyStateService,
            onStart = { server?.start(wait = false) },
            onStop = { server?.stop(1_000, 1_000) },
        )
        val packetController = PacketController(lifecycleService)
        val routeHandler = PacketRouteController(packetController)
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
            callRoomRepositoryPort = callRoomRepositoryPort,
            callParticipantRepositoryPort = callParticipantRepositoryPort,
            callEventRepositoryPort = callEventRepositoryPort,
            endpointCachePort = endpointCachePort,
            routeRepositoryPort = routeRepositoryPort,
            groupChatRepositoryPort = groupChatRepositoryPort,
            chatMemberRepositoryPort = chatMemberRepositoryPort,
            threadRepositoryPort = threadRepositoryPort,
            threadMessageRepositoryPort = threadMessageRepositoryPort,
            groupEventRepositoryPort = groupEventRepositoryPort,
            organizationRepositoryPort = organizationRepositoryPort,
            organizationMemberRepositoryPort = organizationMemberRepositoryPort,
            roleRepositoryPort = roleRepositoryPort,
            inventoryCategoryRepositoryPort = inventoryCategoryRepositoryPort,
            inventoryLocationRepositoryPort = inventoryLocationRepositoryPort,
            inventoryItemRepositoryPort = inventoryItemRepositoryPort,
            inventorySessionRepositoryPort = inventorySessionRepositoryPort,
            inventorySessionMemberRepositoryPort = inventorySessionMemberRepositoryPort,
            inventoryReviewRepositoryPort = inventoryReviewRepositoryPort,
            inventoryCommentRepositoryPort = inventoryCommentRepositoryPort,
            inventoryAttachmentRepositoryPort = inventoryAttachmentRepositoryPort,
            inventoryEventRepositoryPort = inventoryEventRepositoryPort,
            inventoryExportRepositoryPort = inventoryExportRepositoryPort,
            inventoryQrCodeRepositoryPort = inventoryQrCodeRepositoryPort,
            inventoryCodeRepositoryPort = inventoryCodeRepositoryPort,
            inventoryLabelTemplateRepositoryPort = inventoryLabelTemplateRepositoryPort,
            inventoryLabelRepositoryPort = inventoryLabelRepositoryPort,
            inventoryPrintTaskRepositoryPort = inventoryPrintTaskRepositoryPort,
            blockListService = blockListService,
            chatMessagingService = chatMessagingService,
            groupChatService = groupChatService,
            threadService = threadService,
            fileTransferService = fileTransferService,
            callSignalingService = callSignalingService,
            callMediaService = callMediaService,
            routingService = liveRoutingService,
            topologyStateService = topologyStateService,
            connectivityStrategyService = liveConnectivityStrategyService,
            inventoryOrganizationService = inventoryOrganizationService,
            inventoryCatalogService = inventoryCatalogService,
            inventoryOwnershipService = inventoryOwnershipService,
            inventoryItemService = inventoryItemService,
            inventorySessionService = inventorySessionService,
            inventoryReviewService = inventoryReviewService,
            inventoryIncidentService = inventoryIncidentService,
            inventoryExportService = inventoryExportService,
            inventoryQueryService = inventoryQueryService,
            inventoryChangeLogService = inventoryChangeLogService,
            inventoryDashboardService = inventoryDashboardService,
            inventorySearchService = inventorySearchService,
            inventoryCodeService = inventoryCodeService,
            inventoryLabelService = inventoryLabelService,
            inventorySyncService = inventorySyncService,
            inventoryDiscussionService = inventoryDiscussionService,
            centralAuthService = centralAuthService,
            centralOrganizationAccessService = centralOrganizationAccessService,
            centralOrganizationWorkspaceService = centralOrganizationWorkspaceService,
            connectivityModeService = connectivityModeService,
            syncQueueService = syncQueueService,
            conflictStateService = conflictStateService,
            onlineOfflineStateService = onlineOfflineStateService,
            centralAttachmentSyncService = centralAttachmentSyncService,
            centralExportIntegrationService = centralExportIntegrationService,
            centralSyncOrchestrationService = centralSyncOrchestrationService,
        )
    }

    private fun createServer(configuration: NodeConfiguration, routeHandler: PacketRouteController): ApplicationEngine {
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

    private fun resolveAnnouncedHost(bindHost: String, localAddresses: List<String>): String {
        if (bindHost !in AUTO_BIND_HOSTS) {
            return bindHost
        }
        return localAddresses
            .asSequence()
            .filterNot(::isLoopbackAddress)
            .sortedByDescending(::addressPriority)
            .firstOrNull()
            ?: bindHost
    }

    private fun isLoopbackAddress(address: String): Boolean {
        return address == "127.0.0.1" ||
            address == "0:0:0:0:0:0:0:1" ||
            address == "::1" ||
            address.startsWith("169.254.") ||
            address.startsWith("198.18.") ||
            address.startsWith("198.19.")
    }

    private fun addressPriority(address: String): Int {
        return when {
            address.startsWith("192.168.") -> 4
            address.startsWith("10.") -> 3
            isPrivate172Address(address) -> 2
            else -> 1
        }
    }

    private fun isPrivate172Address(address: String): Boolean {
        if (!address.startsWith("172.")) {
            return false
        }
        val secondOctet = address.substringAfter("172.").substringBefore('.').toIntOrNull() ?: return false
        return secondOctet in 16..31
    }

    private companion object {
        private val AUTO_BIND_HOSTS = setOf("0.0.0.0", "127.0.0.1", "localhost", "::")
    }
}
