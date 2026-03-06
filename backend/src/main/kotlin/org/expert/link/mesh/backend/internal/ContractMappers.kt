package org.expert.link.mesh.backend.internal

import org.expert.link.mesh.application.service.RouteHop
import org.expert.link.mesh.application.service.RoutingPlan
import org.expert.link.mesh.bootstrap.config.FeatureFlags
import org.expert.link.mesh.bootstrap.config.FileTransferSettings
import org.expert.link.mesh.bootstrap.config.NodeConfiguration
import org.expert.link.mesh.bootstrap.config.NodeMode
import org.expert.link.mesh.bootstrap.config.RelayClientSettings
import org.expert.link.mesh.bootstrap.config.RetrySettings
import org.expert.link.mesh.bootstrap.config.StaticPeerConfig
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshFileTransferConfig
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.config.MeshNodeMode
import org.expert.link.mesh.contract.config.MeshRelayConfig
import org.expert.link.mesh.contract.config.MeshRetryConfig
import org.expert.link.mesh.contract.config.MeshStaticPeer
import org.expert.link.mesh.contract.model.MeshBlockedPeer
import org.expert.link.mesh.contract.model.MeshCallSession
import org.expert.link.mesh.contract.model.MeshCallSignal
import org.expert.link.mesh.contract.model.MeshCallSignalType
import org.expert.link.mesh.contract.model.MeshCallStatus
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshConversation
import org.expert.link.mesh.contract.model.MeshEventCategory
import org.expert.link.mesh.contract.model.MeshEventLevel
import org.expert.link.mesh.contract.model.MeshEventLogEntry
import org.expert.link.mesh.contract.model.MeshFileDescriptor
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshFileTransferStatus
import org.expert.link.mesh.contract.model.MeshLocalProfile
import org.expert.link.mesh.contract.model.MeshMediaQualitySnapshot
import org.expert.link.mesh.contract.model.MeshMessageDeliveryStatus
import org.expert.link.mesh.contract.model.MeshMessageReceipt
import org.expert.link.mesh.contract.model.MeshPairingRole
import org.expert.link.mesh.contract.model.MeshPairingSession
import org.expert.link.mesh.contract.model.MeshPairedPeer
import org.expert.link.mesh.contract.model.MeshPeerEndpoint
import org.expert.link.mesh.contract.model.MeshPeerIdentity
import org.expert.link.mesh.contract.model.MeshMetricSnapshot
import org.expert.link.mesh.contract.model.MeshNearbyPeer
import org.expert.link.mesh.contract.model.MeshEndpointSource
import org.expert.link.mesh.contract.model.MeshTransferDirection
import org.expert.link.mesh.contract.model.MeshTrustState
import org.expert.link.mesh.contract.model.MeshRouteHop
import org.expert.link.mesh.contract.model.MeshRouteInfo
import org.expert.link.mesh.contract.model.MeshRouteMode
import org.expert.link.mesh.contract.model.MeshRoutingPlan
import org.expert.link.mesh.domain.model.call.CallSession
import org.expert.link.mesh.domain.model.call.CallSignal
import org.expert.link.mesh.domain.model.call.CallSignalType
import org.expert.link.mesh.domain.model.call.MediaQualitySnapshot
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.diagnostics.EventLogEntry
import org.expert.link.mesh.domain.model.diagnostics.MetricSnapshot
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.model.filetransfer.TransferDirection
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.identity.PairedPeer
import org.expert.link.mesh.domain.model.identity.PairingSession
import org.expert.link.mesh.domain.model.identity.PairingSessionRole
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.identity.TrustState
import org.expert.link.mesh.domain.model.messaging.ChatMessage
import org.expert.link.mesh.domain.model.messaging.Conversation
import org.expert.link.mesh.domain.model.messaging.MessageDeliveryStatus
import org.expert.link.mesh.domain.model.messaging.MessageReceipt
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.PeerEndpointCandidate
import org.expert.link.mesh.domain.model.network.RouteEntry
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.model.security.BlockedPeer
import org.expert.link.mesh.domain.model.network.EndpointSource

internal fun MeshNodeConfig.toRuntime(): NodeConfiguration = NodeConfiguration(
    displayName = displayName,
    bindHost = bindHost,
    httpPort = httpPort,
    discoveryPort = discoveryPort,
    multicastGroup = multicastGroup,
    nodeMode = NodeMode.valueOf(nodeMode.name),
    featureFlags = featureFlags.toRuntime(),
    retrySettings = retry.toRuntime(),
    fileTransferSettings = fileTransfer.toRuntime(),
    relayClientSettings = relay?.toRuntime(),
    capabilities = capabilities,
    staticPeers = staticPeers.map(MeshStaticPeer::toRuntime),
)

private fun MeshFeatureFlags.toRuntime(): FeatureFlags = FeatureFlags(
    discoveryEnabled = discoveryEnabled,
    relayEnabled = relayEnabled,
    inMemoryTransport = inMemoryTransport,
    inMemoryDiscovery = inMemoryDiscovery,
)

private fun MeshRetryConfig.toRuntime(): RetrySettings = RetrySettings(
    pollIntervalMillis = pollIntervalMillis,
)

private fun MeshFileTransferConfig.toRuntime(): FileTransferSettings = FileTransferSettings(
    chunkSizeBytes = chunkSizeBytes,
    downloadDirectory = downloadDirectory,
)

private fun MeshRelayConfig.toRuntime(): RelayClientSettings = RelayClientSettings(
    enabled = enabled,
    forceRelayLookup = forceRelayLookup,
    relayEligible = relayEligible,
)

private fun MeshStaticPeer.toRuntime(): StaticPeerConfig = StaticPeerConfig(
    peerId = peerId,
    host = host,
    port = port,
)

internal fun PeerEndpoint.toContract(): MeshPeerEndpoint = MeshPeerEndpoint(
    scheme = scheme,
    host = host,
    port = port,
    path = path,
    announcedPeerId = announcedPeerId,
    announcedAt = announcedAt,
    expiresAt = expiresAt,
)

internal fun MeshPeerEndpoint.toDomain(): PeerEndpoint = PeerEndpoint(
    scheme = scheme,
    host = host,
    port = port,
    path = path,
    announcedPeerId = announcedPeerId,
    announcedAt = announcedAt,
    expiresAt = expiresAt,
)

internal fun LocalProfile.toContract(): MeshLocalProfile = MeshLocalProfile(
    peerId = peerId,
    displayName = displayName,
    publicKey = publicKey,
    capabilities = capabilities,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun PeerIdentity.toContract(): MeshPeerIdentity = MeshPeerIdentity(
    peerId = peerId,
    displayName = displayName,
    publicKey = publicKey,
    capabilities = capabilities,
)

internal fun PairedPeer.toContract(): MeshPairedPeer = MeshPairedPeer(
    identity = peerIdentity.toContract(),
    trustState = MeshTrustState.valueOf(trustState.name),
    pairedAt = pairedAt,
    lastSeenAt = lastSeenAt,
    endpointHint = endpointHint?.toContract(),
    metadata = metadata,
)

internal fun PairingSession.toContract(): MeshPairingSession = MeshPairingSession(
    sessionId = sessionId,
    localPeerId = localPeerId,
    remotePeerId = remotePeerId,
    role = MeshPairingRole.valueOf(role.name),
    createdAt = createdAt,
    expiresAt = expiresAt,
    trustState = MeshTrustState.valueOf(trustState.name),
    used = used,
    endpointHint = endpointHint?.toContract(),
    remotePublicKey = remotePublicKey,
    remoteDisplayName = remoteDisplayName,
)

internal fun BlockedPeer.toContract(): MeshBlockedPeer = MeshBlockedPeer(
    peerId = peerId,
    reason = reason,
    blockedAt = blockedAt,
    expiresAt = expiresAt,
)

internal fun Conversation.toContract(): MeshConversation = MeshConversation(
    conversationId = conversationId,
    participantPeerIds = participantPeerIds,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastMessageId = lastMessageId,
)

internal fun ChatMessage.toContract(): MeshChatMessage = MeshChatMessage(
    messageId = messageId,
    conversationId = conversationId,
    senderPeerId = senderPeerId,
    recipientPeerId = recipientPeerId,
    body = body,
    deliveryStatus = MeshMessageDeliveryStatus.valueOf(deliveryStatus.name),
    createdAt = createdAt,
    deliveredAt = deliveredAt,
    failedAt = failedAt,
)

internal fun MessageReceipt.toContract(): MeshMessageReceipt = MeshMessageReceipt(
    messageId = messageId,
    packetId = packetId,
    conversationId = conversationId,
    receivedAt = receivedAt,
    deliveryStatus = MeshMessageDeliveryStatus.valueOf(deliveryStatus.name),
)

private fun FileDescriptor.toContract(): MeshFileDescriptor = MeshFileDescriptor(
    fileId = fileId,
    fileName = fileName,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    contentType = contentType,
)

internal fun FileTransferSession.toContract(): MeshFileTransferSession = MeshFileTransferSession(
    transferId = transferId,
    conversationId = conversationId,
    descriptor = descriptor.toContract(),
    senderPeerId = senderPeerId,
    recipientPeerId = recipientPeerId,
    direction = MeshTransferDirection.valueOf(direction.name),
    status = MeshFileTransferStatus.valueOf(status.name),
    chunkSizeBytes = chunkSizeBytes,
    totalChunks = totalChunks,
    acknowledgedChunks = acknowledgedChunks,
    receivedChunks = receivedChunks,
    localPath = localPath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun MediaQualitySnapshot.toContract(): MeshMediaQualitySnapshot = MeshMediaQualitySnapshot(
    rttMs = rttMs,
    packetLossPercent = packetLossPercent,
    jitterMs = jitterMs,
    bitrateKbps = bitrateKbps,
    capturedAt = capturedAt,
)

internal fun CallSession.toContract(): MeshCallSession = MeshCallSession(
    callId = callId,
    conversationId = conversationId,
    initiatorPeerId = initiatorPeerId,
    recipientPeerId = recipientPeerId,
    status = MeshCallStatus.valueOf(status.name),
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastSignalAt = lastSignalAt,
    qualitySnapshot = qualitySnapshot?.toContract(),
)

internal fun CallSignal.toContract(): MeshCallSignal = MeshCallSignal(
    callId = callId,
    signalType = MeshCallSignalType.valueOf(signalType.name),
    senderPeerId = senderPeerId,
    recipientPeerId = recipientPeerId,
    payload = payload,
    createdAt = createdAt,
)

internal fun MeshCallSignalType.toDomain(): CallSignalType = CallSignalType.valueOf(name)

internal fun EventLogEntry.toContract(): MeshEventLogEntry = MeshEventLogEntry(
    eventId = eventId,
    category = MeshEventCategory.valueOf(category.name),
    level = MeshEventLevel.valueOf(level.name),
    message = message,
    peerId = peerId,
    packetId = packetId,
    attributes = attributes,
    createdAt = createdAt,
)

internal fun MetricSnapshot.toContract(): MeshMetricSnapshot = MeshMetricSnapshot(
    nodePeerId = nodePeerId,
    capturedAt = capturedAt,
    counters = counters,
    gauges = gauges,
)

private fun EndpointSource.toContract(): MeshEndpointSource = MeshEndpointSource.valueOf(name)

internal fun PeerEndpointCandidate.toContract(): MeshNearbyPeer = MeshNearbyPeer(
    peerId = peerId,
    endpoint = endpoint.toContract(),
    source = source.toContract(),
    discoveredAt = discoveredAt,
    qualityScore = qualityScore,
    capabilities = capabilities,
)

internal fun RouteMode.toContract(): MeshRouteMode = MeshRouteMode.valueOf(name)

internal fun RouteEntry.toContract(): MeshRouteInfo = MeshRouteInfo(
    targetPeerId = targetPeerId,
    nextHopPeerId = nextHopPeerId,
    endpoint = endpoint?.toContract(),
    routeMode = routeMode.toContract(),
    hopCount = hopCount,
    expiresAt = expiresAt,
    learnedAt = learnedAt,
    direct = direct,
)

internal fun RouteHop.toContract(): MeshRouteHop = MeshRouteHop(
    peerId = peerId,
    endpoint = endpoint.toContract(),
    routeMode = routeMode.toContract(),
)

internal fun RoutingPlan.toContract(targetPeerId: String): MeshRoutingPlan = MeshRoutingPlan(
    targetPeerId = targetPeerId,
    routeMode = routeMode.toContract(),
    hops = hops.map(RouteHop::toContract),
    useRelayGateway = useRelayGateway,
)
