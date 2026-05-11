package org.expert.link.mesh.backend

import kotlinx.datetime.Clock
import org.expert.link.mesh.bootstrap.MeshNodeBootstrap
import org.expert.link.mesh.bootstrap.runtime.MeshNodeComponents
import org.expert.link.mesh.contract.api.MeshCallSignalCommand
import org.expert.link.mesh.contract.api.MeshChatCommand
import org.expert.link.mesh.contract.api.MeshChatMemberCommand
import org.expert.link.mesh.contract.api.MeshCreateGroupChatCommand
import org.expert.link.mesh.contract.api.MeshCreateThreadCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryCategoryCommand
import org.expert.link.mesh.contract.api.MeshCreateInventorySubcategoryCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventorySubcategoryCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryTagCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryTagCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryAttributeDefinitionCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryAttributeDefinitionCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryCategoryTemplateCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryCategoryTemplateCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryItemCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryLocationCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryOwnerCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryOwnerCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryDepartmentCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryDepartmentCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryCostCenterCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryCostCenterCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryLegalHolderCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryLegalHolderCommand
import org.expert.link.mesh.contract.api.MeshCreateInventorySupplierCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventorySupplierCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryFundingSourceCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryFundingSourceCommand
import org.expert.link.mesh.contract.api.MeshCreateInventorySessionCommand
import org.expert.link.mesh.contract.api.MeshCreateOrganizationCommand
import org.expert.link.mesh.contract.api.MeshCreateRoleCommand
import org.expert.link.mesh.contract.api.MeshAddInventoryAttachmentCommand
import org.expert.link.mesh.contract.api.MeshAddInventoryCommentCommand
import org.expert.link.mesh.contract.api.MeshAddItemsToSessionCommand
import org.expert.link.mesh.contract.api.MeshAddInventorySessionMemberCommand
import org.expert.link.mesh.contract.api.MeshAddOrganizationMemberCommand
import org.expert.link.mesh.contract.api.MeshEndCallCommand
import org.expert.link.mesh.contract.api.MeshFileTransferCommand
import org.expert.link.mesh.contract.api.MeshHangupCallCommand
import org.expert.link.mesh.contract.api.MeshMediaEngine
import org.expert.link.mesh.contract.api.MeshMulticastSupport
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.api.MeshAcceptCallCommand
import org.expert.link.mesh.contract.api.MeshJoinCallCommand
import org.expert.link.mesh.contract.api.MeshLeaveCallCommand
import org.expert.link.mesh.contract.api.MeshLinkInventoryDiscussionCommand
import org.expert.link.mesh.contract.api.MeshReportInventoryIncidentCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryIncidentStatusCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryRuleThresholdCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryRuleThresholdCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryDeadlineRuleCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryDeadlineRuleCommand
import org.expert.link.mesh.contract.api.MeshInventorySearchCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryCodeBindingCommand
import org.expert.link.mesh.contract.api.MeshCreateInventoryLabelTemplateCommand
import org.expert.link.mesh.contract.api.MeshRecordInventoryScanCommand
import org.expert.link.mesh.contract.api.MeshDeactivateInventoryCodeCommand
import org.expert.link.mesh.contract.api.MeshGenerateInventoryCodeCommand
import org.expert.link.mesh.contract.api.MeshGenerateInventoryLabelRequest
import org.expert.link.mesh.contract.api.MeshGenerateInventoryLabelResponse
import org.expert.link.mesh.contract.api.MeshPrintInventoryLabelRequest
import org.expert.link.mesh.contract.api.MeshBatchPrintInventoryLabelsRequest
import org.expert.link.mesh.contract.api.MeshCentralLoginCommand
import org.expert.link.mesh.contract.api.MeshCentralSyncCommand
import org.expert.link.mesh.contract.api.MeshRegenerateInventoryCodeCommand
import org.expert.link.mesh.contract.api.MeshRegisterScanEventRequest
import org.expert.link.mesh.contract.api.MeshRejectCallCommand
import org.expert.link.mesh.contract.api.MeshRequestCentralExportCommand
import org.expert.link.mesh.contract.api.MeshResolveScannedCodeRequest
import org.expert.link.mesh.contract.api.MeshResolveScannedCodeResponse
import org.expert.link.mesh.contract.api.MeshResolveCentralConflictCommand
import org.expert.link.mesh.contract.api.MeshSelectActiveCentralOrganizationCommand
import org.expert.link.mesh.contract.api.MeshSendMessageCommand
import org.expert.link.mesh.contract.api.MeshSendThreadMessageCommand
import org.expert.link.mesh.contract.api.MeshStartGroupCallCommand
import org.expert.link.mesh.contract.api.MeshStartCallCommand
import org.expert.link.mesh.contract.api.MeshToggleCameraCommand
import org.expert.link.mesh.contract.api.MeshToggleMicrophoneCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryCategoryCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryItemCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryLabelTemplateCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryLocationCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventorySessionCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryStatusCommand
import org.expert.link.mesh.contract.api.MeshUpdateOrganizationCommand
import org.expert.link.mesh.contract.api.MeshUpdateOrganizationMemberCommand
import org.expert.link.mesh.contract.api.MeshUpdateOrganizationMemberRolesCommand
import org.expert.link.mesh.contract.api.MeshUpdateRoleCommand
import org.expert.link.mesh.contract.api.MeshSelectDefaultLabelTemplateCommand
import org.expert.link.mesh.contract.api.MeshCloseInventorySessionCommand
import org.expert.link.mesh.contract.api.MeshSubmitInventoryReviewCommand
import org.expert.link.mesh.contract.api.MeshRequestInventoryExportCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryExportStatusCommand
import org.expert.link.mesh.contract.api.MeshInventorySyncCommand
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.model.MeshBlockedPeer
import org.expert.link.mesh.contract.model.MeshCallMediaState
import org.expert.link.mesh.contract.model.MeshCallSession
import org.expert.link.mesh.contract.model.MeshCallSignal
import org.expert.link.mesh.contract.model.MeshCallEvent
import org.expert.link.mesh.contract.model.MeshCallParticipant
import org.expert.link.mesh.contract.model.MeshCentralAttachmentArtifact
import org.expert.link.mesh.contract.model.MeshCentralAuthState
import org.expert.link.mesh.contract.model.MeshCentralConflict
import org.expert.link.mesh.contract.model.MeshCentralExportTask
import org.expert.link.mesh.contract.model.MeshCentralHybridState
import org.expert.link.mesh.contract.model.MeshCentralOrganizationAccess
import org.expert.link.mesh.contract.model.MeshCentralOrganizationWorkspaceSnapshot
import org.expert.link.mesh.contract.model.MeshCentralPendingChange
import org.expert.link.mesh.contract.model.MeshCentralSyncStatus
import org.expert.link.mesh.contract.model.MeshCentralUserProfile
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshChatSummary
import org.expert.link.mesh.contract.model.MeshConversation
import org.expert.link.mesh.contract.model.MeshEventLogEntry
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshGroupChat
import org.expert.link.mesh.contract.model.MeshGroupEvent
import org.expert.link.mesh.contract.model.MeshInventoryAttachment
import org.expert.link.mesh.contract.model.MeshInventoryAlertEvent
import org.expert.link.mesh.contract.model.MeshInventoryCategory
import org.expert.link.mesh.contract.model.MeshInventorySubcategory
import org.expert.link.mesh.contract.model.MeshInventoryTag
import org.expert.link.mesh.contract.model.MeshInventoryAttributeDefinition
import org.expert.link.mesh.contract.model.MeshInventoryCategoryTemplate
import org.expert.link.mesh.contract.model.MeshInventoryComment
import org.expert.link.mesh.contract.model.MeshInventoryChangeLog
import org.expert.link.mesh.contract.model.MeshInventoryCode
import org.expert.link.mesh.contract.model.MeshInventoryCodeBinding
import org.expert.link.mesh.contract.model.MeshInventoryCodeType
import org.expert.link.mesh.contract.model.MeshInventoryDashboardSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryDeadlineRule
import org.expert.link.mesh.contract.model.MeshInventoryDepartment
import org.expert.link.mesh.contract.model.MeshInventoryCostCenter
import org.expert.link.mesh.contract.model.MeshInventoryFundingSource
import org.expert.link.mesh.contract.model.MeshInventoryIncident
import org.expert.link.mesh.contract.model.MeshInventoryEvent
import org.expert.link.mesh.contract.model.MeshInventoryExportTask
import org.expert.link.mesh.contract.model.MeshInventoryItem
import org.expert.link.mesh.contract.model.MeshInventoryLabelTemplate
import org.expert.link.mesh.contract.model.MeshInventoryLegalHolder
import org.expert.link.mesh.contract.model.MeshInventoryLocation
import org.expert.link.mesh.contract.model.MeshInventoryOwner
import org.expert.link.mesh.contract.model.MeshInventoryPrintTask
import org.expert.link.mesh.contract.model.MeshInventoryReminder
import org.expert.link.mesh.contract.model.MeshInventoryRuleThreshold
import org.expert.link.mesh.contract.model.MeshInventoryReview
import org.expert.link.mesh.contract.model.MeshInventoryScanResultStatus
import org.expert.link.mesh.contract.model.MeshInventorySession
import org.expert.link.mesh.contract.model.MeshInventorySessionMember
import org.expert.link.mesh.contract.model.MeshInventorySupplier
import org.expert.link.mesh.contract.model.MeshInventorySearchResult
import org.expert.link.mesh.contract.model.MeshInventoryScanEvent
import org.expert.link.mesh.contract.model.MeshInventoryStatus
import org.expert.link.mesh.contract.model.MeshInventorySyncResult
import org.expert.link.mesh.contract.model.MeshOrganization
import org.expert.link.mesh.contract.model.MeshOrganizationMember
import org.expert.link.mesh.contract.model.MeshRole
import org.expert.link.mesh.contract.model.MeshMetricSnapshot
import org.expert.link.mesh.contract.model.MeshMediaStats
import org.expert.link.mesh.contract.model.MeshMessageReceipt
import org.expert.link.mesh.contract.model.MeshNearbyPeer
import org.expert.link.mesh.contract.model.MeshPairingSession
import org.expert.link.mesh.contract.model.MeshPairedPeer
import org.expert.link.mesh.contract.model.MeshPeerEndpoint
import org.expert.link.mesh.contract.model.MeshLocalProfile
import org.expert.link.mesh.contract.model.MeshRelayStatus
import org.expert.link.mesh.contract.model.MeshRelayMode
import org.expert.link.mesh.contract.model.MeshRouteInfo
import org.expert.link.mesh.contract.model.MeshRouteHealth
import org.expert.link.mesh.contract.model.MeshRoutingPlan
import org.expert.link.mesh.contract.model.MeshConnectivityStrategy
import org.expert.link.mesh.contract.model.MeshNetworkRoleState
import org.expert.link.mesh.contract.model.MeshThread
import org.expert.link.mesh.contract.model.MeshThreadMessage
import org.expert.link.mesh.contract.model.MeshThreadSummary
import org.expert.link.mesh.contract.model.MeshTopologyState
import org.expert.link.mesh.domain.model.security.BlockedPeer
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.port.external.CentralClientBundle
import org.expert.link.mesh.domain.port.repository.PersistentRepositoryBundle
import org.expert.link.mesh.backend.internal.toContract
import org.expert.link.mesh.backend.internal.toDomain
import org.expert.link.mesh.backend.internal.toDomainPort
import org.expert.link.mesh.backend.internal.toRuntime
import org.expert.link.mesh.application.service.InventoryLabelGenerationOptions
import org.expert.link.mesh.application.service.InventoryItemUpdate
import org.expert.link.mesh.application.service.InventorySessionUpdate

/**
 * Публичная точка входа в backend-ядро.
 *
 * Запускает embedded mesh-узел и возвращает объект [MeshNode], через который мобильное
 * приложение работает со всем функционалом ядра.
 */
object MeshBackend {
    /** Запускает узел и возвращает публичный facade. */
    suspend fun launch(
        configuration: MeshNodeConfig,
        mediaEngine: MeshMediaEngine? = null,
        multicastSupport: MeshMulticastSupport? = null,
        persistentRepositories: PersistentRepositoryBundle? = null,
        centralClients: CentralClientBundle? = null,
    ): MeshNode {
        val components = MeshNodeBootstrap().bootstrapComponents(
            configuration = configuration.toRuntime(),
            mediaEnginePort = mediaEngine?.toDomainPort(),
            multicastSupportPort = multicastSupport?.toDomainPort(),
            persistentRepositories = persistentRepositories,
            centralClients = centralClients,
        )
        return DefaultMeshNode(components)
    }
}

private class DefaultMeshNode(
    private val components: MeshNodeComponents,
) : MeshNode {
    override val profile: MeshLocalProfile = components.runtime.localProfile.toContract()
    override val endpoint: MeshPeerEndpoint = components.runtime.endpoint.toContract()

    override suspend fun stop() {
        components.runtime.lifecycleService.stop()
    }

    override suspend fun createPairingInvite(validitySeconds: Long): String {
        return components.runtime.lifecycleService.createPairingInvite(validitySeconds)
    }

    override suspend fun pairWithInvite(encodedInvite: String): MeshPairingSession {
        return components.runtime.lifecycleService.pairWithInvite(encodedInvite).toContract()
    }

    override suspend fun peers(): List<MeshPairedPeer> {
        return components.peerRepositoryPort.list().map { it.toContract() }
    }

    override suspend fun pairingSessions(): List<MeshPairingSession> {
        return components.pairingSessionRepositoryPort.listActive().map { it.toContract() }
    }

    override suspend fun blockPeer(peerId: String, reason: String, expiresAt: kotlinx.datetime.Instant?): MeshBlockedPeer {
        return components.blockListService.block(
            BlockedPeer(
                peerId = peerId,
                reason = reason,
                blockedAt = Clock.System.now(),
                expiresAt = expiresAt,
            ),
        ).toContract()
    }

    override suspend fun unblockPeer(peerId: String) {
        components.blockListService.unblock(peerId)
    }

    override suspend fun blockedPeers(): List<MeshBlockedPeer> {
        return components.blockListRepositoryPort.list().map { it.toContract() }
    }

    override suspend fun discoverPeer(peerId: String) {
        components.runtime.lifecycleService.discoverPeer(peerId)
    }

    override suspend fun announcePresence() {
        components.runtime.lifecycleService.announcePresence()
    }

    override suspend fun nearbyPeers(): List<MeshNearbyPeer> {
        return components.endpointCachePort.listNeighbors()
            .sortedByDescending { it.qualityScore }
            .map { it.toContract() }
    }

    override suspend fun routes(): List<MeshRouteInfo> {
        components.routingService.purgeExpired()
        return components.routeRepositoryPort.list()
            .sortedBy { it.targetPeerId }
            .map { it.toContract() }
    }

    override suspend fun routingPlan(peerId: String): MeshRoutingPlan {
        return components.routingService.resolvePlan(peerId).toContract(peerId)
    }

    override fun relayStatus(): MeshRelayStatus {
        val settings = components.runtime.configuration.relayClientSettings
        return MeshRelayStatus(
            enabled = components.runtime.configuration.featureFlags.relayEnabled || settings?.enabled == true,
            forceRelayLookup = settings?.forceRelayLookup == true,
            relayEligible = settings?.relayEligible == true,
        )
    }

    override suspend fun relayModeState(): MeshRelayMode {
        return components.runtime.lifecycleService.relayModeState().toContract()
    }

    override suspend fun observeTopologyState(): MeshTopologyState {
        return components.runtime.lifecycleService.observeTopologyState().toContract()
    }

    override suspend fun observeHostRole(): MeshNetworkRoleState {
        return components.runtime.lifecycleService.observeHostRole().toContract()
    }

    override suspend fun observeConnectivityStrategy(peerId: String): MeshConnectivityStrategy {
        return components.runtime.lifecycleService.observeConnectivityStrategy(peerId).toContract()
    }

    override suspend fun inspectRouteHealth(): List<MeshRouteHealth> {
        return components.runtime.lifecycleService.inspectRouteHealth().map { it.toContract() }
    }

    override suspend fun forceTopologyRefresh(): MeshTopologyState {
        return components.runtime.lifecycleService.forceTopologyRefresh().toContract()
    }

    override suspend fun rememberPeerEndpoint(peerId: String, endpoint: MeshPeerEndpoint) {
        components.runtime.lifecycleService.rememberPeerEndpoint(peerId, endpoint.toDomain())
    }

    override suspend fun forgetPeerEndpoint(peerId: String) {
        components.runtime.lifecycleService.forgetPeerEndpoint(peerId)
    }

    override suspend fun openConversation(peerId: String): MeshConversation {
        return components.chatMessagingService.openConversation(peerId).toContract()
    }

    override suspend fun createDirectChat(peerId: String): MeshConversation {
        return components.chatMessagingService.openConversation(peerId).toContract()
    }

    override suspend fun createGroupChat(command: MeshCreateGroupChatCommand): MeshConversation {
        val group = components.groupChatService
            .createGroupChat(command.title, command.description, command.participantPeerIds)
        return requireNotNull(components.conversationRepositoryPort.findByConversationId(group.chatId)).toContract()
    }

    override suspend fun renameChat(chatId: String, title: String): MeshConversation? {
        components.groupChatService.renameGroupChat(chatId, title) ?: return null
        return components.conversationRepositoryPort.findByConversationId(chatId)?.toContract()
    }

    override suspend fun addParticipants(chatId: String, participants: List<MeshChatMemberCommand>): MeshConversation? {
        val peerIdentities = participants.map { participant ->
            val peer = components.peerRepositoryPort.findByPeerId(participant.peerId)
            peer?.peerIdentity ?: PeerIdentity(
                peerId = participant.peerId,
                displayName = participant.displayName,
                publicKey = "",
            )
        }
        components.groupChatService.addParticipants(chatId, peerIdentities) ?: return null
        return components.conversationRepositoryPort.findByConversationId(chatId)?.toContract()
    }

    override suspend fun removeParticipant(chatId: String, peerId: String): MeshConversation? {
        components.groupChatService.removeParticipant(chatId, peerId) ?: return null
        return components.conversationRepositoryPort.findByConversationId(chatId)?.toContract()
    }

    override suspend fun groupChats(): List<MeshGroupChat> {
        return components.groupChatService.listGroupChats().map { it.toContract() }
    }

    override suspend fun groupEvents(chatId: String, limit: Int): List<MeshGroupEvent> {
        return components.groupChatService.groupEvents(chatId, limit).map { it.toContract() }
    }

    override suspend fun chatSummaries(): List<MeshChatSummary> {
        return components.groupChatService.chatSummaries().map { it.toContract() }
    }

    override suspend fun conversations(): List<MeshConversation> {
        return components.conversationRepositoryPort.list().map { it.toContract() }
    }

    override suspend fun groupMessages(chatId: String): List<MeshChatMessage> {
        return components.groupChatService.groupMessages(chatId).map { it.toContract() }
    }

    override suspend fun messages(conversationId: String): List<MeshChatMessage> {
        return components.messageRepositoryPort.listByConversation(conversationId).map { it.toContract() }
    }

    override suspend fun messageReceipts(limit: Int): List<MeshMessageReceipt> {
        return components.chatMessagingService.recentReceipts(limit).map { it.toContract() }
    }

    override suspend fun sendChat(command: MeshChatCommand): MeshChatMessage {
        return components.runtime.lifecycleService.sendChat(command.targetPeerId, command.body, command.conversationId).toContract()
    }

    override suspend fun sendMessage(command: MeshSendMessageCommand): MeshChatMessage {
        return components.runtime.lifecycleService.sendMessage(command.chatId, command.body).toContract()
    }

    override suspend fun createThread(command: MeshCreateThreadCommand): MeshThread {
        return components.threadService.createThread(command.chatId, command.rootMessageId).toContract()
    }

    override suspend fun thread(chatId: String, rootMessageId: String): MeshThread? {
        return components.threadService.thread(chatId, rootMessageId)?.toContract()
    }

    override suspend fun threadUpdates(chatId: String): List<MeshThreadSummary> {
        return components.threadService.threadSummaries(chatId).map { it.toContract() }
    }

    override suspend fun threadMessagesDetailed(chatId: String, rootMessageId: String): List<MeshThreadMessage> {
        return components.threadService.threadMessages(chatId, rootMessageId).map { it.toContract() }
    }

    override suspend fun sendThreadReply(command: MeshSendThreadMessageCommand): MeshThreadMessage {
        return components.threadService.sendThreadMessage(
            chatId = command.chatId,
            rootMessageId = command.rootMessageId,
            body = command.body,
            parentMessageId = command.parentMessageId,
        ).toContract()
    }

    override suspend fun threadMessages(chatId: String, rootMessageId: String): List<MeshChatMessage> {
        return components.chatMessagingService.threadMessages(chatId, rootMessageId).map { it.toContract() }
    }

    override suspend fun sendThreadMessage(command: MeshSendThreadMessageCommand): MeshChatMessage {
        return components.runtime.lifecycleService.sendThreadMessage(
            conversationId = command.chatId,
            rootMessageId = command.rootMessageId,
            body = command.body,
            parentMessageId = command.parentMessageId,
        ).toContract()
    }

    override suspend fun threadSummary(chatId: String, rootMessageId: String): MeshThreadSummary? {
        return components.chatMessagingService.threadSummary(chatId, rootMessageId)?.toContract()
    }

    override suspend fun fileTransfers(): List<MeshFileTransferSession> {
        return components.fileTransferRepositoryPort.list().map { it.toContract() }
    }

    override suspend fun sendFile(command: MeshFileTransferCommand): MeshFileTransferSession {
        return components.runtime.lifecycleService.sendFile(command.targetPeerId, command.path, command.conversationId).toContract()
    }

    override suspend fun resumeFileTransfer(transferId: String): MeshFileTransferSession? {
        return components.runtime.lifecycleService.requestFileResume(transferId)?.toContract()
    }

    override suspend fun cancelFileTransfer(transferId: String): MeshFileTransferSession? {
        return components.fileTransferService.cancelTransfer(transferId)?.toContract()
    }

    override suspend fun callSessions(): List<MeshCallSession> {
        return components.callSessionRepositoryPort.list().map { it.toContract() }
    }

    override suspend fun startAudioCall(command: MeshStartCallCommand): MeshCallSession {
        return components.runtime.lifecycleService
            .startAudioCall(command.targetPeerId, command.conversationId, command.offer)
            .toContract()
    }

    override suspend fun startVideoCall(command: MeshStartCallCommand): MeshCallSession {
        return components.runtime.lifecycleService
            .startVideoCall(command.targetPeerId, command.conversationId, command.offer)
            .toContract()
    }

    override suspend fun startGroupAudioCall(command: MeshStartGroupCallCommand): MeshCallSession {
        return components.runtime.lifecycleService
            .startGroupAudioCall(command.targetPeerIds, command.conversationId, command.offer, command.roomTitle)
            .toContract()
    }

    override suspend fun startGroupVideoCall(command: MeshStartGroupCallCommand): MeshCallSession {
        return components.runtime.lifecycleService
            .startGroupVideoCall(command.targetPeerIds, command.conversationId, command.offer, command.roomTitle)
            .toContract()
    }

    override suspend fun acceptCall(command: MeshAcceptCallCommand): MeshCallSignal {
        return components.runtime.lifecycleService
            .acceptCall(command.callId, command.recipientPeerId, command.answer)
            .toContract()
    }

    override suspend fun rejectCall(command: MeshRejectCallCommand): MeshCallSignal {
        return components.runtime.lifecycleService
            .rejectCall(command.callId, command.recipientPeerId, command.reason)
            .toContract()
    }

    override suspend fun joinCall(command: MeshJoinCallCommand): MeshCallSignal {
        return components.runtime.lifecycleService
            .joinCall(command.callId, command.recipientPeerId, command.answer)
            .toContract()
    }

    override suspend fun leaveCall(command: MeshLeaveCallCommand): MeshCallSignal {
        return components.runtime.lifecycleService
            .leaveCall(command.callId, command.recipientPeerId, command.reason)
            .toContract()
    }

    override suspend fun endCall(command: MeshEndCallCommand): MeshCallSession? {
        return components.runtime.lifecycleService.endCall(command.callId, command.reason)?.toContract()
    }

    override suspend fun observeActiveCall(): List<MeshCallSession> {
        return components.runtime.lifecycleService.observeActiveCalls().map { it.toContract() }
    }

    override suspend fun observeIncomingCalls(): List<MeshCallSession> {
        return components.runtime.lifecycleService.observeIncomingCalls().map { it.toContract() }
    }

    override suspend fun observeCallParticipants(callId: String): List<MeshCallParticipant> {
        return components.runtime.lifecycleService.observeCallParticipants(callId).map { it.toContract() }
    }

    override suspend fun observeCallEvents(callId: String, limit: Int): List<MeshCallEvent> {
        return components.runtime.lifecycleService.observeCallEvents(callId, limit).map { it.toContract() }
    }

    override suspend fun toggleMicrophone(command: MeshToggleMicrophoneCommand): MeshCallMediaState? {
        return components.runtime.lifecycleService.toggleMicrophone(command.callId, command.enabled)?.toContract()
    }

    override suspend fun toggleCamera(command: MeshToggleCameraCommand): MeshCallMediaState? {
        return components.runtime.lifecycleService.toggleCamera(command.callId, command.enabled)?.toContract()
    }

    override suspend fun switchCamera(callId: String): MeshCallMediaState? {
        return components.runtime.lifecycleService.switchCamera(callId)?.toContract()
    }

    override suspend fun observeMediaState(callId: String): MeshCallMediaState? {
        return components.runtime.lifecycleService.observeMediaState(callId)?.toContract()
    }

    override suspend fun observeMediaStats(callId: String): MeshMediaStats? {
        return components.runtime.lifecycleService.observeMediaStats(callId)?.toContract()
    }

    @Deprecated("Используйте startAudioCall/startVideoCall")
    override suspend fun startCall(command: MeshStartCallCommand): MeshCallSession {
        return startAudioCall(command)
    }

    @Deprecated("Используйте acceptCall/rejectCall/joinCall/leaveCall")
    override suspend fun sendCallSignal(command: MeshCallSignalCommand): MeshCallSignal {
        return components.callSignalingService.sendSignal(
            callId = command.callId,
            recipientPeerId = command.recipientPeerId,
            signalType = command.signalType.toDomain(),
            payload = command.payload,
        ).toContract()
    }

    @Deprecated("Используйте endCall")
    override suspend fun hangupCall(command: MeshHangupCallCommand): MeshCallSession? {
        return components.runtime.lifecycleService.endCall(command.callId, command.reason)?.toContract()
    }

    override suspend fun organizations(): List<MeshOrganization> {
        return components.inventoryOrganizationService.listOrganizations().map { it.toContract() }
    }

    override suspend fun createOrganization(command: MeshCreateOrganizationCommand): MeshOrganization {
        return components.inventoryOrganizationService.createOrganization(command.name, command.description).toContract()
    }

    override suspend fun updateOrganization(command: MeshUpdateOrganizationCommand): MeshOrganization? {
        return components.inventoryOrganizationService
            .updateOrganization(command.organizationId, command.name, command.description)
            ?.toContract()
    }

    override suspend fun organizationMembers(organizationId: String): List<MeshOrganizationMember> {
        return components.inventoryOrganizationService.organizationMembers(organizationId).map { it.toContract() }
    }

    override suspend fun addOrganizationMember(command: MeshAddOrganizationMemberCommand): MeshOrganizationMember {
        return components.inventoryOrganizationService.addMember(
            organizationId = command.organizationId,
            peerId = command.peerId,
            displayName = command.displayName,
            roleIds = command.roleIds,
            departmentId = command.departmentId,
            locationIds = command.locationIds,
            position = command.position,
            isCommissionMember = command.isCommissionMember,
        ).toContract()
    }

    override suspend fun updateOrganizationMemberRoles(command: MeshUpdateOrganizationMemberRolesCommand): MeshOrganizationMember? {
        return components.inventoryOrganizationService
            .updateMemberRoles(command.organizationId, command.peerId, command.roleIds)
            ?.toContract()
    }

    override suspend fun updateOrganizationMember(command: MeshUpdateOrganizationMemberCommand): MeshOrganizationMember? {
        return components.inventoryOrganizationService
            .updateMember(
                organizationId = command.organizationId,
                peerId = command.peerId,
                displayName = command.displayName,
                roleIds = command.roleIds,
                departmentId = command.departmentId,
                locationIds = command.locationIds,
                position = command.position,
                isCommissionMember = command.isCommissionMember,
                status = command.status?.let { org.expert.link.mesh.domain.model.inventory.OrganizationMemberStatus.valueOf(it.name) },
            )
            ?.toContract()
    }

    override suspend fun removeOrganizationMember(organizationId: String, peerId: String) {
        components.inventoryOrganizationService.removeMember(organizationId, peerId)
    }

    override suspend fun roles(organizationId: String): List<MeshRole> {
        return components.inventoryOrganizationService.roles(organizationId).map { it.toContract() }
    }

    override suspend fun createRole(command: MeshCreateRoleCommand): MeshRole {
        return components.inventoryOrganizationService
            .createRole(command.organizationId, command.name, command.description, command.permissions.map { it.toDomain() }.toSet())
            .toContract()
    }

    override suspend fun updateRole(command: MeshUpdateRoleCommand): MeshRole? {
        return components.inventoryOrganizationService
            .updateRole(command.roleId, command.name, command.description, command.permissions.map { it.toDomain() }.toSet())
            ?.toContract()
    }

    override suspend fun removeRole(roleId: String) {
        components.inventoryOrganizationService.removeRole(roleId)
    }

    override suspend fun inventoryCategories(organizationId: String): List<MeshInventoryCategory> {
        return components.inventoryCatalogService.listCategories(organizationId).map { it.toContract() }
    }

    override suspend fun createInventoryCategory(command: MeshCreateInventoryCategoryCommand): MeshInventoryCategory {
        return components.inventoryCatalogService
            .createCategory(
                organizationId = command.organizationId,
                name = command.name,
                description = command.description,
                parentCategoryId = command.parentCategoryId,
                templateId = command.templateId,
            )
            .toContract()
    }

    override suspend fun updateInventoryCategory(command: MeshUpdateInventoryCategoryCommand): MeshInventoryCategory? {
        return components.inventoryCatalogService
            .updateCategory(
                categoryId = command.categoryId,
                name = command.name,
                description = command.description,
                parentCategoryId = command.parentCategoryId,
                templateId = command.templateId,
            )
            ?.toContract()
    }

    override suspend fun inventorySubcategories(organizationId: String, categoryId: String?): List<MeshInventorySubcategory> {
        return components.inventoryCatalogService
            .listSubcategories(organizationId, categoryId)
            .map { it.toContract() }
    }

    override suspend fun createInventorySubcategory(command: MeshCreateInventorySubcategoryCommand): MeshInventorySubcategory {
        return components.inventoryCatalogService
            .createSubcategory(command.organizationId, command.categoryId, command.name, command.description)
            .toContract()
    }

    override suspend fun updateInventorySubcategory(command: MeshUpdateInventorySubcategoryCommand): MeshInventorySubcategory? {
        return components.inventoryCatalogService
            .updateSubcategory(command.subcategoryId, command.name, command.description)
            ?.toContract()
    }

    override suspend fun inventoryTags(organizationId: String): List<MeshInventoryTag> {
        return components.inventoryCatalogService.listTags(organizationId).map { it.toContract() }
    }

    override suspend fun createInventoryTag(command: MeshCreateInventoryTagCommand): MeshInventoryTag {
        return components.inventoryCatalogService
            .createTag(command.organizationId, command.name, command.color)
            .toContract()
    }

    override suspend fun updateInventoryTag(command: MeshUpdateInventoryTagCommand): MeshInventoryTag? {
        return components.inventoryCatalogService
            .updateTag(command.tagId, command.name, command.color)
            ?.toContract()
    }

    override suspend fun inventoryAttributeDefinitions(organizationId: String): List<MeshInventoryAttributeDefinition> {
        return components.inventoryCatalogService.listAttributeDefinitions(organizationId).map { it.toContract() }
    }

    override suspend fun createInventoryAttributeDefinition(
        command: MeshCreateInventoryAttributeDefinitionCommand,
    ): MeshInventoryAttributeDefinition {
        return components.inventoryCatalogService
            .createAttributeDefinition(
                organizationId = command.organizationId,
                key = command.key,
                label = command.label,
                description = command.description,
                type = command.type.toDomain(),
                required = command.required,
                unit = command.unit,
                options = command.options,
                validationRules = command.validationRules.map { it.toDomain() },
            )
            .toContract()
    }

    override suspend fun updateInventoryAttributeDefinition(
        command: MeshUpdateInventoryAttributeDefinitionCommand,
    ): MeshInventoryAttributeDefinition? {
        return components.inventoryCatalogService
            .updateAttributeDefinition(
                attributeId = command.attributeId,
                key = command.key,
                label = command.label,
                description = command.description,
                type = command.type?.toDomain(),
                required = command.required,
                unit = command.unit,
                options = command.options,
                validationRules = command.validationRules?.map { it.toDomain() },
            )
            ?.toContract()
    }

    override suspend fun inventoryCategoryTemplates(
        organizationId: String,
        categoryId: String?,
    ): List<MeshInventoryCategoryTemplate> {
        return components.inventoryCatalogService
            .listCategoryTemplates(organizationId, categoryId)
            .map { it.toContract() }
    }

    override suspend fun createInventoryCategoryTemplate(
        command: MeshCreateInventoryCategoryTemplateCommand,
    ): MeshInventoryCategoryTemplate {
        return components.inventoryCatalogService
            .createCategoryTemplate(
                organizationId = command.organizationId,
                categoryId = command.categoryId,
                name = command.name,
                description = command.description,
                fields = command.fields.map { it.toDomain() },
                requiredFields = command.requiredFields.map { it.toDomain() },
            )
            .toContract()
    }

    override suspend fun updateInventoryCategoryTemplate(
        command: MeshUpdateInventoryCategoryTemplateCommand,
    ): MeshInventoryCategoryTemplate? {
        return components.inventoryCatalogService
            .updateCategoryTemplate(
                templateId = command.templateId,
                name = command.name,
                description = command.description,
                fields = command.fields?.map { it.toDomain() },
                requiredFields = command.requiredFields?.map { it.toDomain() },
            )
            ?.toContract()
    }

    override suspend fun inventoryLocations(organizationId: String): List<MeshInventoryLocation> {
        return components.inventoryCatalogService.listLocations(organizationId).map { it.toContract() }
    }

    override suspend fun createInventoryLocation(command: MeshCreateInventoryLocationCommand): MeshInventoryLocation {
        return components.inventoryCatalogService
            .createLocation(
                organizationId = command.organizationId,
                name = command.name,
                description = command.description,
                parentLocationId = command.parentLocationId,
                locationType = command.locationType.toDomain(),
                code = command.code,
                path = command.path,
                departmentId = command.departmentId,
                archived = command.archived,
            )
            .toContract()
    }

    override suspend fun updateInventoryLocation(command: MeshUpdateInventoryLocationCommand): MeshInventoryLocation? {
        return components.inventoryCatalogService
            .updateLocation(
                locationId = command.locationId,
                name = command.name,
                description = command.description,
                parentLocationId = command.parentLocationId,
                locationType = command.locationType?.toDomain(),
                code = command.code,
                path = command.path,
                departmentId = command.departmentId,
                archived = command.archived,
            )
            ?.toContract()
    }

    override suspend fun inventoryOwners(organizationId: String): List<MeshInventoryOwner> {
        return components.inventoryOwnershipService.listOwners(organizationId).map { it.toContract() }
    }

    override suspend fun createInventoryOwner(command: MeshCreateInventoryOwnerCommand): MeshInventoryOwner {
        return components.inventoryOwnershipService
            .createOwner(
                organizationId = command.organizationId,
                name = command.name,
                type = command.type.toDomain(),
                legalHolderId = command.legalHolderId,
                contactInfo = command.contactInfo,
                code = command.code,
                departmentId = command.departmentId,
                locationIds = command.locationIds,
                archived = command.archived,
            )
            .toContract()
    }

    override suspend fun updateInventoryOwner(command: MeshUpdateInventoryOwnerCommand): MeshInventoryOwner? {
        return components.inventoryOwnershipService
            .updateOwner(
                ownerId = command.ownerId,
                name = command.name,
                type = command.type?.toDomain(),
                legalHolderId = command.legalHolderId,
                contactInfo = command.contactInfo,
                code = command.code,
                departmentId = command.departmentId,
                locationIds = command.locationIds,
                archived = command.archived,
            )
            ?.toContract()
    }

    override suspend fun inventoryDepartments(organizationId: String): List<MeshInventoryDepartment> {
        return components.inventoryOwnershipService.listDepartments(organizationId).map { it.toContract() }
    }

    override suspend fun createInventoryDepartment(command: MeshCreateInventoryDepartmentCommand): MeshInventoryDepartment {
        return components.inventoryOwnershipService
            .createDepartment(
                organizationId = command.organizationId,
                name = command.name,
                parentDepartmentId = command.parentDepartmentId,
                code = command.code,
                locationIds = command.locationIds,
                ownerIds = command.ownerIds,
                archived = command.archived,
            )
            .toContract()
    }

    override suspend fun updateInventoryDepartment(command: MeshUpdateInventoryDepartmentCommand): MeshInventoryDepartment? {
        return components.inventoryOwnershipService
            .updateDepartment(
                departmentId = command.departmentId,
                name = command.name,
                parentDepartmentId = command.parentDepartmentId,
                code = command.code,
                locationIds = command.locationIds,
                ownerIds = command.ownerIds,
                archived = command.archived,
            )
            ?.toContract()
    }

    override suspend fun inventoryCostCenters(organizationId: String): List<MeshInventoryCostCenter> {
        return components.inventoryOwnershipService.listCostCenters(organizationId).map { it.toContract() }
    }

    override suspend fun createInventoryCostCenter(command: MeshCreateInventoryCostCenterCommand): MeshInventoryCostCenter {
        return components.inventoryOwnershipService
            .createCostCenter(
                organizationId = command.organizationId,
                code = command.code,
                name = command.name,
                description = command.description,
            )
            .toContract()
    }

    override suspend fun updateInventoryCostCenter(command: MeshUpdateInventoryCostCenterCommand): MeshInventoryCostCenter? {
        return components.inventoryOwnershipService
            .updateCostCenter(
                costCenterId = command.costCenterId,
                code = command.code,
                name = command.name,
                description = command.description,
            )
            ?.toContract()
    }

    override suspend fun inventoryLegalHolders(organizationId: String): List<MeshInventoryLegalHolder> {
        return components.inventoryOwnershipService.listLegalHolders(organizationId).map { it.toContract() }
    }

    override suspend fun createInventoryLegalHolder(command: MeshCreateInventoryLegalHolderCommand): MeshInventoryLegalHolder {
        return components.inventoryOwnershipService
            .createLegalHolder(
                organizationId = command.organizationId,
                name = command.name,
                taxId = command.taxId,
                registrationNumber = command.registrationNumber,
                address = command.address,
                bankDetails = command.bankDetails,
            )
            .toContract()
    }

    override suspend fun updateInventoryLegalHolder(command: MeshUpdateInventoryLegalHolderCommand): MeshInventoryLegalHolder? {
        return components.inventoryOwnershipService
            .updateLegalHolder(
                legalHolderId = command.legalHolderId,
                name = command.name,
                taxId = command.taxId,
                registrationNumber = command.registrationNumber,
                address = command.address,
                bankDetails = command.bankDetails,
            )
            ?.toContract()
    }

    override suspend fun inventorySuppliers(organizationId: String): List<MeshInventorySupplier> {
        return components.inventoryOwnershipService.listSuppliers(organizationId).map { it.toContract() }
    }

    override suspend fun createInventorySupplier(command: MeshCreateInventorySupplierCommand): MeshInventorySupplier {
        return components.inventoryOwnershipService
            .createSupplier(
                organizationId = command.organizationId,
                name = command.name,
                contactInfo = command.contactInfo,
                bankDetails = command.bankDetails,
            )
            .toContract()
    }

    override suspend fun updateInventorySupplier(command: MeshUpdateInventorySupplierCommand): MeshInventorySupplier? {
        return components.inventoryOwnershipService
            .updateSupplier(
                supplierId = command.supplierId,
                name = command.name,
                contactInfo = command.contactInfo,
                bankDetails = command.bankDetails,
            )
            ?.toContract()
    }

    override suspend fun inventoryFundingSources(organizationId: String): List<MeshInventoryFundingSource> {
        return components.inventoryOwnershipService.listFundingSources(organizationId).map { it.toContract() }
    }

    override suspend fun createInventoryFundingSource(command: MeshCreateInventoryFundingSourceCommand): MeshInventoryFundingSource {
        return components.inventoryOwnershipService
            .createFundingSource(
                organizationId = command.organizationId,
                name = command.name,
                description = command.description,
            )
            .toContract()
    }

    override suspend fun updateInventoryFundingSource(command: MeshUpdateInventoryFundingSourceCommand): MeshInventoryFundingSource? {
        return components.inventoryOwnershipService
            .updateFundingSource(
                fundingSourceId = command.fundingSourceId,
                name = command.name,
                description = command.description,
            )
            ?.toContract()
    }

    override suspend fun inventoryItems(organizationId: String, status: MeshInventoryStatus?): List<MeshInventoryItem> {
        return if (status == null) {
            components.inventoryItemRepositoryPort.listByOrganization(organizationId).map { it.toContract() }
        } else {
            components.inventoryItemRepositoryPort.listByStatus(organizationId, status.toDomain()).map { it.toContract() }
        }
    }

    override suspend fun inventoryItem(itemId: String): MeshInventoryItem? {
        return components.inventoryItemRepositoryPort.findByInventoryItemId(itemId)?.toContract()
    }

    override suspend fun createInventoryItem(command: MeshCreateInventoryItemCommand): MeshInventoryItem {
        return components.inventoryItemService.createItem(
            organizationId = command.organizationId,
            inventoryNumber = command.inventoryNumber,
            localNumber = command.localNumber,
            qrCode = command.qrCode,
            barcode = command.barcode,
            categoryId = command.categoryId,
            subcategoryId = command.subcategoryId,
            itemType = command.itemType.toDomain(),
            title = command.title,
            description = command.description,
            brand = command.brand,
            model = command.model,
            serialNumber = command.serialNumber,
            manufacturer = command.manufacturer,
            purchaseDate = command.purchaseDate,
            commissioningDate = command.commissioningDate,
            warrantyUntil = command.warrantyUntil,
            depreciationGroup = command.depreciationGroup,
            usefulLifeMonths = command.usefulLifeMonths,
            condition = command.condition.toDomain(),
            locationId = command.locationId,
            responsiblePerson = command.responsiblePerson,
            responsibleDepartment = command.responsibleDepartment,
            responsibleUserId = command.responsibleUserId,
            responsibleOwnerIds = command.responsibleOwnerIds,
            ownerOrganizationId = command.ownerOrganizationId,
            ownerId = command.ownerId,
            departmentId = command.departmentId,
            costCenterId = command.costCenterId,
            legalHolderId = command.legalHolderId,
            supplierId = command.supplierId,
            fundingSourceId = command.fundingSourceId,
            lastInventoryAt = command.lastInventoryAt,
            nextInventoryAt = command.nextInventoryAt,
            tagIds = command.tagIds,
            attributes = command.attributes.map { it.toDomain() },
            metadata = command.metadata,
        ).toContract()
    }

    override suspend fun updateInventoryItem(command: MeshUpdateInventoryItemCommand): MeshInventoryItem? {
        return components.inventoryItemService.updateItem(
            itemId = command.inventoryItemId,
            expectedRevision = command.expectedRevision,
            update = InventoryItemUpdate(
                inventoryNumber = command.inventoryNumber,
                localNumber = command.localNumber,
                qrCode = command.qrCode,
                barcode = command.barcode,
                categoryId = command.categoryId,
                subcategoryId = command.subcategoryId,
                itemType = command.itemType?.toDomain(),
                title = command.title,
                description = command.description,
                brand = command.brand,
                model = command.model,
                serialNumber = command.serialNumber,
                manufacturer = command.manufacturer,
                purchaseDate = command.purchaseDate,
                commissioningDate = command.commissioningDate,
                warrantyUntil = command.warrantyUntil,
                depreciationGroup = command.depreciationGroup,
                usefulLifeMonths = command.usefulLifeMonths,
                condition = command.condition?.toDomain(),
                locationId = command.locationId,
                responsiblePerson = command.responsiblePerson,
                responsibleDepartment = command.responsibleDepartment,
                responsibleUserId = command.responsibleUserId,
                responsibleOwnerIds = command.responsibleOwnerIds,
                ownerOrganizationId = command.ownerOrganizationId,
                ownerId = command.ownerId,
                departmentId = command.departmentId,
                costCenterId = command.costCenterId,
                legalHolderId = command.legalHolderId,
                supplierId = command.supplierId,
                fundingSourceId = command.fundingSourceId,
                lastInventoryAt = command.lastInventoryAt,
                nextInventoryAt = command.nextInventoryAt,
                tagIds = command.tagIds,
                attributes = command.attributes?.map { it.toDomain() },
                syncStatus = command.syncStatus?.toDomain(),
                chatId = command.chatId,
                threadRootMessageId = command.threadRootMessageId,
                metadata = command.metadata,
            ),
        )?.toContract()
    }

    override suspend fun updateInventoryStatus(command: MeshUpdateInventoryStatusCommand): MeshInventoryItem? {
        return components.inventoryItemService.updateStatus(
            itemId = command.inventoryItemId,
            expectedRevision = command.expectedRevision,
            status = command.status.toDomain(),
            note = command.note,
        )?.toContract()
    }

    override suspend fun addInventoryComment(command: MeshAddInventoryCommentCommand): MeshInventoryComment {
        return components.inventoryItemService
            .addComment(command.inventoryItemId, command.sessionId, command.body)
            .toContract()
    }

    override suspend fun addInventoryAttachment(command: MeshAddInventoryAttachmentCommand): MeshInventoryAttachment {
        return components.inventoryItemService
            .addAttachment(
                itemId = command.inventoryItemId,
                sessionId = command.sessionId,
                descriptor = command.descriptor.toDomain(),
                transferId = command.transferId,
                attachmentType = command.attachmentType.toDomain(),
                note = command.note,
            )
            .toContract()
    }

    override suspend fun linkInventoryDiscussion(command: MeshLinkInventoryDiscussionCommand): MeshInventoryItem? {
        return components.inventoryItemService
            .linkDiscussion(command.inventoryItemId, command.chatId, command.threadRootMessageId)
            ?.toContract()
    }

    override suspend fun inventoryIncidents(
        organizationId: String,
        itemId: String?,
        sessionId: String?,
    ): List<MeshInventoryIncident> {
        return components.inventoryIncidentService
            .listIncidents(organizationId, itemId, sessionId)
            .map { it.toContract() }
    }

    override suspend fun reportInventoryIncident(command: MeshReportInventoryIncidentCommand): MeshInventoryIncident {
        return components.inventoryIncidentService
            .reportIncident(
                organizationId = command.organizationId,
                inventoryItemId = command.inventoryItemId,
                sessionId = command.sessionId,
                locationId = command.locationId,
                type = command.type.toDomain(),
                severity = command.severity.toDomain(),
                title = command.title,
                description = command.description,
                assigneePeerIds = command.assigneePeerIds,
                attachmentIds = command.attachmentIds,
                comment = command.comment,
            )
            .toContract()
    }

    override suspend fun updateInventoryIncidentStatus(
        command: MeshUpdateInventoryIncidentStatusCommand,
    ): MeshInventoryIncident? {
        return components.inventoryIncidentService
            .updateIncidentStatus(command.incidentId, command.status.toDomain(), command.reviewComment)
            ?.toContract()
    }

    override suspend fun inventoryAlerts(organizationId: String): List<MeshInventoryAlertEvent> {
        return components.inventoryIncidentService.listAlerts(organizationId).map { it.toContract() }
    }

    override suspend fun inventoryReminders(organizationId: String, itemId: String?): List<MeshInventoryReminder> {
        return components.inventoryIncidentService.listReminders(organizationId, itemId).map { it.toContract() }
    }

    override suspend fun inventoryRuleThresholds(organizationId: String): List<MeshInventoryRuleThreshold> {
        return components.inventoryIncidentService.listRuleThresholds(organizationId).map { it.toContract() }
    }

    override suspend fun createInventoryRuleThreshold(command: MeshCreateInventoryRuleThresholdCommand): MeshInventoryRuleThreshold {
        return components.inventoryIncidentService
            .createRuleThreshold(
                organizationId = command.organizationId,
                ruleType = command.ruleType.toDomain(),
                thresholdValue = command.thresholdValue,
                active = command.active,
                metadata = command.metadata,
            )
            .toContract()
    }

    override suspend fun updateInventoryRuleThreshold(
        command: MeshUpdateInventoryRuleThresholdCommand,
    ): MeshInventoryRuleThreshold? {
        return components.inventoryIncidentService
            .updateRuleThreshold(
                ruleId = command.ruleId,
                ruleType = command.ruleType?.toDomain(),
                thresholdValue = command.thresholdValue,
                active = command.active,
                metadata = command.metadata,
            )
            ?.toContract()
    }

    override suspend fun inventoryDeadlineRules(organizationId: String): List<MeshInventoryDeadlineRule> {
        return components.inventoryIncidentService.listDeadlineRules(organizationId).map { it.toContract() }
    }

    override suspend fun createInventoryDeadlineRule(command: MeshCreateInventoryDeadlineRuleCommand): MeshInventoryDeadlineRule {
        return components.inventoryIncidentService
            .createDeadlineRule(
                organizationId = command.organizationId,
                target = command.target.toDomain(),
                daysBefore = command.daysBefore,
                active = command.active,
            )
            .toContract()
    }

    override suspend fun updateInventoryDeadlineRule(
        command: MeshUpdateInventoryDeadlineRuleCommand,
    ): MeshInventoryDeadlineRule? {
        return components.inventoryIncidentService
            .updateDeadlineRule(
                ruleId = command.ruleId,
                target = command.target?.toDomain(),
                daysBefore = command.daysBefore,
                active = command.active,
            )
            ?.toContract()
    }

    override suspend fun inventorySessions(organizationId: String): List<MeshInventorySession> {
        return components.inventorySessionRepositoryPort.listByOrganization(organizationId).map { it.toContract() }
    }

    override suspend fun inventorySession(sessionId: String): MeshInventorySession? {
        return components.inventorySessionRepositoryPort.findBySessionId(sessionId)?.toContract()
    }

    override suspend fun createInventorySession(command: MeshCreateInventorySessionCommand): MeshInventorySession {
        return components.inventorySessionService.createSession(
            organizationId = command.organizationId,
            title = command.title,
            description = command.description,
            periodStart = command.periodStart,
            periodEnd = command.periodEnd,
            departmentIds = command.departmentIds,
            locationIds = command.locationIds,
            ownerIds = command.ownerIds,
            workflowStatus = command.workflowStatus.toDomain(),
            requiresPhotoForDiscrepancy = command.requiresPhotoForDiscrepancy,
            itemIds = command.itemIds,
            memberPeerIds = command.memberPeerIds,
        ).toContract()
    }

    override suspend fun updateInventorySession(command: MeshUpdateInventorySessionCommand): MeshInventorySession? {
        return components.inventorySessionService.updateSession(
            sessionId = command.sessionId,
            expectedRevision = command.expectedRevision,
            update = InventorySessionUpdate(
                title = command.title,
                description = command.description,
                periodStart = command.periodStart,
                periodEnd = command.periodEnd,
                status = command.status?.toDomain(),
                reviewStatus = command.reviewStatus?.toDomain(),
                workflowStatus = command.workflowStatus?.toDomain(),
                result = command.result?.toDomain(),
                departmentIds = command.departmentIds,
                locationIds = command.locationIds,
                ownerIds = command.ownerIds,
                requiresPhotoForDiscrepancy = command.requiresPhotoForDiscrepancy,
                completionBlockedReason = command.completionBlockedReason,
                chatId = command.chatId,
                threadRootMessageId = command.threadRootMessageId,
                metadata = command.metadata,
            ),
        )?.toContract()
    }

    override suspend fun addItemsToSession(command: MeshAddItemsToSessionCommand): MeshInventorySession? {
        return components.inventorySessionService.addItemsToSession(command.sessionId, command.itemIds)?.toContract()
    }

    override suspend fun addInventorySessionMember(command: MeshAddInventorySessionMemberCommand): MeshInventorySessionMember {
        return components.inventorySessionService
            .addMember(command.sessionId, command.peerId, command.role.toDomain())
            .toContract()
    }

    override suspend fun inventorySessionMembers(sessionId: String): List<MeshInventorySessionMember> {
        return components.inventorySessionService.listMembers(sessionId).map { it.toContract() }
    }

    override suspend fun closeInventorySession(command: MeshCloseInventorySessionCommand): MeshInventorySession? {
        return components.inventorySessionService.closeSession(command.sessionId, command.note)?.toContract()
    }

    override suspend fun submitInventoryReview(command: MeshSubmitInventoryReviewCommand): MeshInventoryReview {
        return components.inventoryReviewService
            .submitReview(
                itemId = command.inventoryItemId,
                sessionId = command.sessionId,
                status = command.status.toDomain(),
                presenceStatus = command.presenceStatus.toDomain(),
                acceptanceStatus = command.acceptanceStatus.toDomain(),
                confirmationStatus = command.confirmationStatus.toDomain(),
                requiresPhoto = command.requiresPhoto,
                comment = command.comment,
            )
            .toContract()
    }

    override suspend fun inventoryReviews(itemId: String, sessionId: String?): List<MeshInventoryReview> {
        return components.inventoryReviewService.listReviews(itemId, sessionId).map { it.toContract() }
    }

    override suspend fun inventoryExports(organizationId: String): List<MeshInventoryExportTask> {
        return components.inventoryExportService.listExports(organizationId).map { it.toContract() }
    }

    override suspend fun requestInventoryExport(command: MeshRequestInventoryExportCommand): MeshInventoryExportTask {
        return components.inventoryExportService
            .requestExport(command.organizationId, command.sessionId, command.format.toDomain())
            .toContract()
    }

    override suspend fun updateInventoryExportStatus(command: MeshUpdateInventoryExportStatusCommand): MeshInventoryExportTask? {
        return components.inventoryExportService
            .updateExportStatus(command.exportTaskId, command.status.toDomain(), command.resultDescriptor?.toDomain(), command.errorMessage)
            ?.toContract()
    }

    override suspend fun inventoryEvents(organizationId: String, sinceSequence: Long?): List<MeshInventoryEvent> {
        return if (sinceSequence == null) {
            components.inventoryEventRepositoryPort.listByOrganization(organizationId)
        } else {
            components.inventoryEventRepositoryPort.listByOrganizationSinceSequence(organizationId, sinceSequence)
        }.map { it.toContract() }
    }

    override suspend fun inventoryChangeLogs(organizationId: String, entityId: String?): List<MeshInventoryChangeLog> {
        return if (entityId == null) {
            components.inventoryChangeLogService.listByOrganization(organizationId)
        } else {
            components.inventoryChangeLogService.listByEntity(organizationId, entityId)
        }.map { it.toContract() }
    }

    override suspend fun inventoryDashboardSnapshot(organizationId: String): MeshInventoryDashboardSnapshot {
        return components.inventoryDashboardService.snapshot(organizationId).toContract()
    }

    override suspend fun searchInventory(command: MeshInventorySearchCommand): MeshInventorySearchResult {
        return components.inventorySearchService.search(command.query.toDomain()).toContract()
    }

    override suspend fun findInventoryItemByQr(code: String): MeshInventoryItem? {
        return components.inventoryQueryService.findByQrCode(code)?.toContract()
    }

    override suspend fun findInventoryItemByBarcode(code: String): MeshInventoryItem? {
        return components.inventoryQueryService.findByBarcode(code)?.toContract()
    }

    override suspend fun findInventoryItemByCode(code: String): MeshInventoryItem? {
        return components.inventoryQueryService.findByAnyCode(code)?.toContract()
    }

    override suspend fun inventoryCodes(itemId: String): List<MeshInventoryCode> {
        return components.inventoryCodeService.listCodes(itemId).map { it.toContract() }
    }

    override suspend fun getInventoryCode(itemId: String, codeType: MeshInventoryCodeType): MeshInventoryCode? {
        return components.inventoryCodeService.getActiveCode(itemId, codeType.toDomain())?.toContract()
    }

    override suspend fun generateInventoryCode(command: MeshGenerateInventoryCodeCommand): MeshInventoryCode {
        return components.inventoryCodeService
            .generateCode(
                organizationId = command.organizationId,
                inventoryItemId = command.inventoryItemId,
                codeType = command.codeType.toDomain(),
                barcodeFormat = command.barcodeFormat.toDomain(),
            )
            .toContract()
    }

    override suspend fun regenerateInventoryCode(command: MeshRegenerateInventoryCodeCommand): MeshInventoryCode {
        return components.inventoryCodeService
            .regenerateCode(
                organizationId = command.organizationId,
                inventoryItemId = command.inventoryItemId,
                codeType = command.codeType.toDomain(),
                barcodeFormat = command.barcodeFormat.toDomain(),
            )
            .toContract()
    }

    override suspend fun deactivateInventoryCode(command: MeshDeactivateInventoryCodeCommand): MeshInventoryCode? {
        return components.inventoryCodeService.deactivateCode(command.codeId)?.toContract()
    }

    override suspend fun inventoryCodeBindings(itemId: String): List<MeshInventoryCodeBinding> {
        return components.inventoryCodeService.listBindings(itemId).map { it.toContract() }
    }

    override suspend fun createInventoryCodeBinding(command: MeshCreateInventoryCodeBindingCommand): MeshInventoryCodeBinding {
        return components.inventoryCodeService
            .createBinding(
                organizationId = command.organizationId,
                inventoryItemId = command.inventoryItemId,
                sessionId = command.sessionId,
                codeType = command.codeType.toDomain(),
                codeValue = command.codeValue,
            )
            .toContract()
    }

    override suspend fun inventoryLabelTemplates(organizationId: String): List<MeshInventoryLabelTemplate> {
        return components.inventoryLabelService.listTemplates(organizationId).map { it.toContract() }
    }

    override suspend fun createInventoryLabelTemplate(command: MeshCreateInventoryLabelTemplateCommand): MeshInventoryLabelTemplate {
        return components.inventoryLabelService
            .createTemplate(
                organizationId = command.organizationId,
                name = command.name,
                description = command.description,
                templateType = command.templateType.toDomain(),
                fields = command.fields.map { it.toDomain() },
                includeBarcode = command.includeBarcode,
                includeQr = command.includeQr,
            )
            .toContract()
    }

    override suspend fun updateInventoryLabelTemplate(command: MeshUpdateInventoryLabelTemplateCommand): MeshInventoryLabelTemplate? {
        return components.inventoryLabelService
            .updateTemplate(
                templateId = command.templateId,
                name = command.name,
                description = command.description,
                fields = command.fields?.map { it.toDomain() },
                includeBarcode = command.includeBarcode,
                includeQr = command.includeQr,
            )
            ?.toContract()
    }

    override suspend fun selectDefaultLabelTemplate(command: MeshSelectDefaultLabelTemplateCommand): MeshInventoryLabelTemplate? {
        return components.inventoryLabelService
            .selectDefaultTemplate(command.organizationId, command.templateId)
            ?.toContract()
    }

    override suspend fun generateInventoryLabelPreview(request: MeshGenerateInventoryLabelRequest): MeshGenerateInventoryLabelResponse {
        val result = components.inventoryLabelService.generateLabelPreview(
            organizationId = request.organizationId,
            inventoryItemId = request.inventoryItemId,
            options = request.toGenerationOptions(),
        )
        return MeshGenerateInventoryLabelResponse(
            label = result.label.toContract(),
            template = result.template.toContract(),
        )
    }

    override suspend fun generateInventoryLabelPdf(request: MeshGenerateInventoryLabelRequest): MeshGenerateInventoryLabelResponse {
        val result = components.inventoryLabelService.generateLabelPdf(
            organizationId = request.organizationId,
            inventoryItemId = request.inventoryItemId,
            options = request.toGenerationOptions(),
        )
        return MeshGenerateInventoryLabelResponse(
            label = result.label.toContract(),
            template = result.template.toContract(),
            pdfDescriptor = result.task.resultDescriptor?.toContract(),
            localPath = result.task.localPath,
        )
    }

    override suspend fun printInventoryLabel(request: MeshPrintInventoryLabelRequest): MeshInventoryPrintTask {
        return components.inventoryLabelService
            .printLabel(
                organizationId = request.organizationId,
                inventoryItemId = request.inventoryItemId,
                options = request.toGenerationOptions(),
            )
            .task
            .toContract()
    }

    override suspend fun printInventoryLabelsBatch(request: MeshBatchPrintInventoryLabelsRequest): MeshInventoryPrintTask {
        return components.inventoryLabelService
            .printLabelsBatch(
                organizationId = request.organizationId,
                itemIds = request.itemIds,
                options = request.toGenerationOptions(),
            )
            .task
            .toContract()
    }

    override suspend fun inventoryPrintTasks(organizationId: String): List<MeshInventoryPrintTask> {
        return components.inventoryLabelService.listPrintTasks(organizationId).map { it.toContract() }
    }

    override suspend fun recordInventoryScan(command: MeshRecordInventoryScanCommand): MeshInventoryScanEvent {
        return components.inventoryCodeService
            .recordScan(
                organizationId = command.organizationId,
                codeType = command.codeType.toDomain(),
                codeValue = command.codeValue,
                rawValue = command.codeValue,
                inventoryItemId = command.inventoryItemId,
                sessionId = command.sessionId,
                locationId = command.locationId,
                locationHint = null,
                deviceId = null,
                resultStatus = MeshInventoryScanResultStatus.RESOLVED.toDomain(),
                note = command.note,
            )
            .toContract()
    }

    override suspend fun resolveScannedCode(request: MeshResolveScannedCodeRequest): MeshResolveScannedCodeResponse {
        var resolution = components.inventoryCodeService.resolveScannedCode(request.organizationId, request.rawValue)
        if (resolution.item == null && resolution.code == null) {
            val syncOrganizationId = request.organizationId
                ?: components.centralOrganizationAccessService.activeOrganizationId()
            if (!syncOrganizationId.isNullOrBlank()) {
                runCatching { components.centralSyncOrchestrationService.syncNow(syncOrganizationId) }
                resolution = components.inventoryCodeService.resolveScannedCode(syncOrganizationId, request.rawValue)
            }
        }
        val organizationId = request.organizationId
            ?: resolution.item?.organizationId
            ?: resolution.code?.organizationId
        val resolvedCodeType = resolution.codeType ?: inferCodeType(request.rawValue)
        val scanEvent = if (organizationId != null && resolvedCodeType != null) {
            components.inventoryCodeService.recordScan(
                organizationId = organizationId,
                codeType = resolvedCodeType,
                codeValue = resolution.code?.rawValue ?: request.rawValue.trim(),
                rawValue = request.rawValue,
                inventoryItemId = resolution.item?.inventoryItemId,
                sessionId = request.sessionId,
                locationId = request.locationId ?: resolution.item?.locationId,
                locationHint = request.locationHint,
                deviceId = request.deviceId,
                resultStatus = resolution.status,
                note = if (resolution.status == org.expert.link.mesh.domain.model.inventory.InventoryScanResultStatus.INACTIVE) {
                    "Использован неактивный код"
                } else {
                    null
                },
            ).toContract()
        } else {
            null
        }
        return MeshResolveScannedCodeResponse(
            status = resolution.status.toContract(),
            item = resolution.item?.toContract(),
            code = resolution.code?.toContract(),
            codeType = resolution.codeType?.toContract(),
            scanEvent = scanEvent,
        )
    }

    override suspend fun registerScanEvent(request: MeshRegisterScanEventRequest): MeshInventoryScanEvent {
        return components.inventoryCodeService
            .recordScan(
                organizationId = request.organizationId,
                codeType = request.codeType.toDomain(),
                codeValue = request.codeValue,
                rawValue = request.rawValue,
                inventoryItemId = request.inventoryItemId,
                sessionId = request.sessionId,
                locationId = request.locationId,
                locationHint = request.locationHint,
                deviceId = request.deviceId,
                resultStatus = request.resultStatus.toDomain(),
                note = request.note,
            )
            .toContract()
    }

    override suspend fun syncInventoryWithPeer(command: MeshInventorySyncCommand): MeshInventorySyncResult {
        return components.inventorySyncService
            .requestSync(command.targetPeerId, command.organizationId, command.sinceSequence, command.timeoutMillis)
            .toContract()
    }

    override suspend fun hybridState(): MeshCentralHybridState {
        return components.onlineOfflineStateService.snapshot(meshReady = true).toContract()
    }

    override suspend fun centralLogin(command: MeshCentralLoginCommand): MeshCentralAuthState {
        return components.centralAuthService.login(command.toDomain()).toContract()
    }

    override suspend fun centralRefreshAuth(): MeshCentralAuthState? {
        return components.centralAuthService.refresh()?.toContract()
    }

    override suspend fun centralLogout() {
        components.centralAuthService.logout()
    }

    override suspend fun centralAuthState(): MeshCentralAuthState? {
        return components.centralAuthService.authState()?.toContract()
    }

    override suspend fun centralUserProfile(): MeshCentralUserProfile? {
        return components.centralAuthService.authState()?.userProfile?.toContract()
    }

    override suspend fun centralOrganizations(): List<MeshCentralOrganizationAccess> {
        val cached = components.centralOrganizationAccessService.organizations()
        val organizations = if (cached.isEmpty()) {
            components.centralOrganizationAccessService.refreshFromCentral()
        } else {
            cached
        }
        return organizations.map { it.toContract() }
    }

    override suspend fun centralOrganizationWorkspace(
        organizationId: String,
    ): MeshCentralOrganizationWorkspaceSnapshot? {
        return components.centralOrganizationWorkspaceService.snapshot(organizationId)?.toContract()
    }

    override suspend fun refreshCentralOrganizationWorkspace(
        organizationId: String,
    ): MeshCentralOrganizationWorkspaceSnapshot? {
        return components.centralOrganizationWorkspaceService.refreshFromCentral(organizationId)?.toContract()
    }

    override suspend fun selectActiveCentralOrganization(
        command: MeshSelectActiveCentralOrganizationCommand,
    ): MeshCentralOrganizationAccess? {
        val selected = components.centralOrganizationAccessService
            .selectActiveOrganization(command.organizationId)
        selected?.organizationId?.let { components.centralOrganizationWorkspaceService.refreshFromCentral(it) }
        return selected?.toContract()
    }

    override suspend fun centralSyncStatus(organizationId: String): MeshCentralSyncStatus {
        return components.centralSyncOrchestrationService.syncStatus(organizationId).toContract()
    }

    override suspend fun centralPendingChanges(
        organizationId: String,
        limit: Int,
    ): List<MeshCentralPendingChange> {
        return components.syncQueueService.pendingChanges(organizationId, limit).map { it.toContract() }
    }

    override suspend fun centralConflicts(organizationId: String): List<MeshCentralConflict> {
        return components.conflictStateService.list(organizationId).map { it.toContract() }
    }

    override suspend fun resolveCentralConflict(command: MeshResolveCentralConflictCommand): MeshCentralConflict? {
        val resolved = components.conflictStateService.resolve(command.toDomain())
        resolved?.organizationId?.let { organizationId ->
            runCatching { components.centralSyncOrchestrationService.syncNow(organizationId) }
        }
        return resolved?.toContract()
    }

    override suspend fun syncWithCentral(command: MeshCentralSyncCommand): MeshCentralSyncStatus {
        val organizationId = command.organizationId
            ?: components.centralOrganizationAccessService.activeOrganizationId()
            ?: components.centralOrganizationAccessService.organizations().firstOrNull()?.organizationId
            ?: return components.onlineOfflineStateService.snapshot(meshReady = true).let {
                val hybrid = it.toContract()
                MeshCentralSyncStatus(
                    organizationId = "",
                    connectivityMode = hybrid.connectivityMode,
                    runState = org.expert.link.mesh.contract.model.MeshCentralSyncRunState.PAUSED,
                    pendingChanges = hybrid.pendingChanges,
                    conflicts = hybrid.conflicts,
                    lastSyncAt = hybrid.lastSyncAt,
                    lastError = hybrid.lastError ?: "Активная организация не выбрана",
                )
            }
        return components.centralSyncOrchestrationService
            .syncNow(organizationId, command.forcePull, command.forcePush)
            .toContract()
    }

    override suspend fun syncCentralAttachments(organizationId: String): List<MeshCentralAttachmentArtifact> {
        return components.centralAttachmentSyncService.sync(organizationId).map { it.toContract() }
    }

    override suspend fun requestCentralExport(command: MeshRequestCentralExportCommand): MeshCentralExportTask {
        return components.centralExportIntegrationService
            .requestExport(command.organizationId, command.sessionId, command.format.toDomain())
            .toContract()
    }

    override suspend fun centralExports(organizationId: String): List<MeshCentralExportTask> {
        return components.centralExportIntegrationService.exports(organizationId).map { it.toContract() }
    }

    override suspend fun recentEvents(limit: Int): List<MeshEventLogEntry> {
        return components.runtime.lifecycleService.recentEvents(limit).map { it.toContract() }
    }

    override suspend fun metrics(): MeshMetricSnapshot {
        return components.runtime.lifecycleService.metricsSnapshot().toContract()
    }

    private fun MeshGenerateInventoryLabelRequest.toGenerationOptions(): InventoryLabelGenerationOptions = InventoryLabelGenerationOptions(
        templateId = templateId,
        fields = fields?.map { it.toDomain() },
        includeBarcode = includeBarcode,
        includeQr = includeQr,
    )

    private fun MeshPrintInventoryLabelRequest.toGenerationOptions(): InventoryLabelGenerationOptions = InventoryLabelGenerationOptions(
        templateId = templateId,
        fields = fields?.map { it.toDomain() },
        includeBarcode = includeBarcode,
        includeQr = includeQr,
    )

    private fun MeshBatchPrintInventoryLabelsRequest.toGenerationOptions(): InventoryLabelGenerationOptions = InventoryLabelGenerationOptions(
        templateId = templateId,
        fields = fields?.map { it.toDomain() },
        includeBarcode = includeBarcode,
        includeQr = includeQr,
    )

    private fun inferCodeType(rawValue: String): org.expert.link.mesh.domain.model.inventory.InventoryCodeType = if (
        rawValue.trim().startsWith("inventory:")
    ) {
        org.expert.link.mesh.domain.model.inventory.InventoryCodeType.QR
    } else {
        org.expert.link.mesh.domain.model.inventory.InventoryCodeType.BARCODE
    }
}
