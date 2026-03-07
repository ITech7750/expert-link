package org.expert.link.mesh.backend

import kotlinx.datetime.Clock
import org.expert.link.mesh.bootstrap.MeshNodeBootstrap
import org.expert.link.mesh.bootstrap.runtime.MeshNodeComponents
import org.expert.link.mesh.contract.api.MeshCallSignalCommand
import org.expert.link.mesh.contract.api.MeshChatCommand
import org.expert.link.mesh.contract.api.MeshChatMemberCommand
import org.expert.link.mesh.contract.api.MeshCreateGroupChatCommand
import org.expert.link.mesh.contract.api.MeshCreateThreadCommand
import org.expert.link.mesh.contract.api.MeshFileTransferCommand
import org.expert.link.mesh.contract.api.MeshHangupCallCommand
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.api.MeshSendMessageCommand
import org.expert.link.mesh.contract.api.MeshSendThreadMessageCommand
import org.expert.link.mesh.contract.api.MeshStartCallCommand
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.model.MeshBlockedPeer
import org.expert.link.mesh.contract.model.MeshCallSession
import org.expert.link.mesh.contract.model.MeshCallSignal
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshChatSummary
import org.expert.link.mesh.contract.model.MeshConversation
import org.expert.link.mesh.contract.model.MeshEventLogEntry
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshGroupChat
import org.expert.link.mesh.contract.model.MeshGroupEvent
import org.expert.link.mesh.contract.model.MeshMetricSnapshot
import org.expert.link.mesh.contract.model.MeshMessageReceipt
import org.expert.link.mesh.contract.model.MeshNearbyPeer
import org.expert.link.mesh.contract.model.MeshPairingSession
import org.expert.link.mesh.contract.model.MeshPairedPeer
import org.expert.link.mesh.contract.model.MeshPeerEndpoint
import org.expert.link.mesh.contract.model.MeshLocalProfile
import org.expert.link.mesh.contract.model.MeshRelayStatus
import org.expert.link.mesh.contract.model.MeshRouteInfo
import org.expert.link.mesh.contract.model.MeshRoutingPlan
import org.expert.link.mesh.contract.model.MeshThread
import org.expert.link.mesh.contract.model.MeshThreadMessage
import org.expert.link.mesh.contract.model.MeshThreadSummary
import org.expert.link.mesh.domain.model.security.BlockedPeer
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.backend.internal.toContract
import org.expert.link.mesh.backend.internal.toDomain
import org.expert.link.mesh.backend.internal.toRuntime

/**
 * Публичная точка входа в backend-ядро.
 *
 * Запускает embedded mesh-узел и возвращает объект [MeshNode], через который мобильное
 * приложение работает со всем функционалом ядра.
 */
object MeshBackend {
    /** Запускает узел и возвращает публичный facade. */
    suspend fun launch(configuration: MeshNodeConfig): MeshNode {
        val components = MeshNodeBootstrap().bootstrapComponents(configuration.toRuntime())
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

    override suspend fun startCall(command: MeshStartCallCommand): MeshCallSession {
        return components.runtime.lifecycleService.startCall(command.targetPeerId, command.conversationId, command.offer).toContract()
    }

    override suspend fun sendCallSignal(command: MeshCallSignalCommand): MeshCallSignal {
        return components.callSignalingService.sendSignal(
            callId = command.callId,
            recipientPeerId = command.recipientPeerId,
            signalType = command.signalType.toDomain(),
            payload = command.payload,
        ).toContract()
    }

    override suspend fun hangupCall(command: MeshHangupCallCommand): MeshCallSession? {
        return components.callSignalingService.hangup(command.callId, command.recipientPeerId, command.reason)?.toContract()
    }

    override suspend fun recentEvents(limit: Int): List<MeshEventLogEntry> {
        return components.runtime.lifecycleService.recentEvents(limit).map { it.toContract() }
    }

    override suspend fun metrics(): MeshMetricSnapshot {
        return components.runtime.lifecycleService.metricsSnapshot().toContract()
    }
}
