package org.expert.link.mesh.bootstrap.runtime

import org.expert.link.mesh.application.service.BlockListService
import org.expert.link.mesh.application.service.CallSignalingService
import org.expert.link.mesh.application.service.CallMediaService
import org.expert.link.mesh.application.service.ChatMessagingService
import org.expert.link.mesh.application.service.CentralAttachmentSyncService
import org.expert.link.mesh.application.service.CentralAuthService
import org.expert.link.mesh.application.service.CentralExportIntegrationService
import org.expert.link.mesh.application.service.CentralOrganizationAccessService
import org.expert.link.mesh.application.service.CentralOrganizationWorkspaceService
import org.expert.link.mesh.application.service.CentralSyncOrchestrationService
import org.expert.link.mesh.application.service.ConflictStateService
import org.expert.link.mesh.application.service.ConnectivityModeService
import org.expert.link.mesh.application.service.InventoryCatalogService
import org.expert.link.mesh.application.service.InventoryChangeLogService
import org.expert.link.mesh.application.service.InventoryCodeService
import org.expert.link.mesh.application.service.InventoryDashboardService
import org.expert.link.mesh.application.service.InventoryDiscussionService
import org.expert.link.mesh.application.service.InventoryExportService
import org.expert.link.mesh.application.service.InventoryIncidentService
import org.expert.link.mesh.application.service.InventoryItemService
import org.expert.link.mesh.application.service.InventoryLabelService
import org.expert.link.mesh.application.service.InventoryOrganizationService
import org.expert.link.mesh.application.service.InventoryOwnershipService
import org.expert.link.mesh.application.service.InventoryQueryService
import org.expert.link.mesh.application.service.InventoryReviewService
import org.expert.link.mesh.application.service.InventorySearchService
import org.expert.link.mesh.application.service.InventorySessionService
import org.expert.link.mesh.application.service.InventorySyncService
import org.expert.link.mesh.application.service.FileTransferService
import org.expert.link.mesh.application.service.GroupChatService
import org.expert.link.mesh.application.service.OnlineOfflineStateService
import org.expert.link.mesh.application.service.SyncQueueService
import org.expert.link.mesh.application.service.ConnectivityStrategyService
import org.expert.link.mesh.application.service.RoutingService
import org.expert.link.mesh.application.service.ThreadService
import org.expert.link.mesh.application.service.TopologyStateService
import org.expert.link.mesh.domain.port.repository.ChatMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.BlockListRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallParticipantRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallRoomRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.ConversationRepositoryPort
import org.expert.link.mesh.domain.port.repository.EndpointCachePort
import org.expert.link.mesh.domain.port.repository.FileTransferRepositoryPort
import org.expert.link.mesh.domain.port.repository.GroupChatRepositoryPort
import org.expert.link.mesh.domain.port.repository.GroupEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.MessageRepositoryPort
import org.expert.link.mesh.domain.port.repository.PairingSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.PeerRepositoryPort
import org.expert.link.mesh.domain.port.repository.RouteRepositoryPort
import org.expert.link.mesh.domain.port.repository.ThreadMessageRepositoryPort
import org.expert.link.mesh.domain.port.repository.ThreadRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryAttachmentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCategoryRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCommentRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCodeRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryExportRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryItemRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLabelRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLabelTemplateRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLocationRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryPrintTaskRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryQrCodeRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryReviewRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySessionMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.OrganizationMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.OrganizationRepositoryPort
import org.expert.link.mesh.domain.port.repository.RoleRepositoryPort

/** Внутренний контейнер зависимостей runtime. */
data class MeshNodeComponents(
    val runtime: MeshNodeRuntime,
    val peerRepositoryPort: PeerRepositoryPort,
    val pairingSessionRepositoryPort: PairingSessionRepositoryPort,
    val blockListRepositoryPort: BlockListRepositoryPort,
    val conversationRepositoryPort: ConversationRepositoryPort,
    val messageRepositoryPort: MessageRepositoryPort,
    val fileTransferRepositoryPort: FileTransferRepositoryPort,
    val callSessionRepositoryPort: CallSessionRepositoryPort,
    val callRoomRepositoryPort: CallRoomRepositoryPort,
    val callParticipantRepositoryPort: CallParticipantRepositoryPort,
    val callEventRepositoryPort: CallEventRepositoryPort,
    val endpointCachePort: EndpointCachePort,
    val routeRepositoryPort: RouteRepositoryPort,
    val groupChatRepositoryPort: GroupChatRepositoryPort,
    val chatMemberRepositoryPort: ChatMemberRepositoryPort,
    val threadRepositoryPort: ThreadRepositoryPort,
    val threadMessageRepositoryPort: ThreadMessageRepositoryPort,
    val groupEventRepositoryPort: GroupEventRepositoryPort,
    val organizationRepositoryPort: OrganizationRepositoryPort,
    val organizationMemberRepositoryPort: OrganizationMemberRepositoryPort,
    val roleRepositoryPort: RoleRepositoryPort,
    val inventoryCategoryRepositoryPort: InventoryCategoryRepositoryPort,
    val inventoryLocationRepositoryPort: InventoryLocationRepositoryPort,
    val inventoryItemRepositoryPort: InventoryItemRepositoryPort,
    val inventorySessionRepositoryPort: InventorySessionRepositoryPort,
    val inventorySessionMemberRepositoryPort: InventorySessionMemberRepositoryPort,
    val inventoryReviewRepositoryPort: InventoryReviewRepositoryPort,
    val inventoryCommentRepositoryPort: InventoryCommentRepositoryPort,
    val inventoryAttachmentRepositoryPort: InventoryAttachmentRepositoryPort,
    val inventoryEventRepositoryPort: InventoryEventRepositoryPort,
    val inventoryExportRepositoryPort: InventoryExportRepositoryPort,
    val inventoryQrCodeRepositoryPort: InventoryQrCodeRepositoryPort,
    val inventoryCodeRepositoryPort: InventoryCodeRepositoryPort,
    val inventoryLabelTemplateRepositoryPort: InventoryLabelTemplateRepositoryPort,
    val inventoryLabelRepositoryPort: InventoryLabelRepositoryPort,
    val inventoryPrintTaskRepositoryPort: InventoryPrintTaskRepositoryPort,
    val blockListService: BlockListService,
    val chatMessagingService: ChatMessagingService,
    val groupChatService: GroupChatService,
    val threadService: ThreadService,
    val fileTransferService: FileTransferService,
    val callSignalingService: CallSignalingService,
    val callMediaService: CallMediaService,
    val routingService: RoutingService,
    val topologyStateService: TopologyStateService,
    val connectivityStrategyService: ConnectivityStrategyService,
    val inventoryOrganizationService: InventoryOrganizationService,
    val inventoryCatalogService: InventoryCatalogService,
    val inventoryOwnershipService: InventoryOwnershipService,
    val inventoryItemService: InventoryItemService,
    val inventorySessionService: InventorySessionService,
    val inventoryReviewService: InventoryReviewService,
    val inventoryIncidentService: InventoryIncidentService,
    val inventoryExportService: InventoryExportService,
    val inventoryQueryService: InventoryQueryService,
    val inventoryChangeLogService: InventoryChangeLogService,
    val inventoryDashboardService: InventoryDashboardService,
    val inventorySearchService: InventorySearchService,
    val inventoryCodeService: InventoryCodeService,
    val inventoryLabelService: InventoryLabelService,
    val inventorySyncService: InventorySyncService,
    val inventoryDiscussionService: InventoryDiscussionService,
    val centralAuthService: CentralAuthService,
    val centralOrganizationAccessService: CentralOrganizationAccessService,
    val centralOrganizationWorkspaceService: CentralOrganizationWorkspaceService,
    val connectivityModeService: ConnectivityModeService,
    val syncQueueService: SyncQueueService,
    val conflictStateService: ConflictStateService,
    val onlineOfflineStateService: OnlineOfflineStateService,
    val centralAttachmentSyncService: CentralAttachmentSyncService,
    val centralExportIntegrationService: CentralExportIntegrationService,
    val centralSyncOrchestrationService: CentralSyncOrchestrationService,
)
