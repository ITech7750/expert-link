package org.expert.link.mesh.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.expert.link.mesh.application.factory.PacketEnvelopeFactory
import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.application.support.plusSeconds
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.identity.PairedPeer
import org.expert.link.mesh.domain.model.identity.PairingInvite
import org.expert.link.mesh.domain.model.identity.PairingSession
import org.expert.link.mesh.domain.model.identity.PairingSessionRole
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.identity.TrustState
import org.expert.link.mesh.domain.model.network.EndpointSource
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.network.PairAccept
import org.expert.link.mesh.domain.model.network.PairRequest
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.PeerEndpointCandidate
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.model.security.SecurityIncident
import org.expert.link.mesh.domain.model.security.SecurityIncidentType
import org.expert.link.mesh.domain.model.security.SecuritySeverity
import org.expert.link.mesh.domain.port.external.CryptoPort
import org.expert.link.mesh.domain.port.repository.EndpointCachePort
import org.expert.link.mesh.domain.port.repository.PairingSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.PeerRepositoryPort

/**
 * Сервис pairing по invite.
 *
 * Создаёт invite, обрабатывает `PAIR_REQUEST` и `PAIR_ACCEPT`,
 * валидирует anti-replay данные и переводит peer в trusted state.
 */
class PairingService(
    private val localProfileService: LocalProfileService,
    private val peerRepositoryPort: PeerRepositoryPort,
    private val pairingSessionRepositoryPort: PairingSessionRepositoryPort,
    private val endpointCachePort: EndpointCachePort,
    private val cryptoPort: CryptoPort,
    private val messageEncryptionService: MessageEncryptionService,
    private val packetEnvelopeFactory: PacketEnvelopeFactory,
    private val packetSignatureService: PacketSignatureService,
    private val deliveryTrackingService: DeliveryTrackingService,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
    private val securityIncidentService: SecurityIncidentService,
    private val localEndpointProvider: suspend () -> PeerEndpoint? = { null },
) {
    private val logger = KotlinLogging.logger {}

    /** Создаёт короткоживущий invite. */
    suspend fun createInvite(validitySeconds: Long = 300): PairingInvite {
        val localProfile = localProfileService.require()
        val invite = PairingInvite(
            protocolVersion = 1,
            peerId = localProfile.peerId,
            displayName = localProfile.displayName,
            publicKey = localProfile.publicKey,
            inviteSecret = cryptoPort.randomSecret(),
            expiresAt = now().plusSeconds(validitySeconds),
            endpointHint = localEndpointProvider(),
            capabilities = localProfile.capabilities,
        )
        pairingSessionRepositoryPort.save(
            PairingSession(
                sessionId = newId("pairing"),
                localPeerId = localProfile.peerId,
                role = PairingSessionRole.INVITER,
                inviteSecret = invite.inviteSecret,
                createdAt = now(),
                expiresAt = invite.expiresAt,
                trustState = TrustState.INVITED,
                endpointHint = invite.endpointHint,
            ),
        )
        eventLogService.log(
            category = EventCategory.PAIRING,
            level = EventLevel.INFO,
            message = "Created pairing invite",
            peerId = localProfile.peerId,
            attributes = mapOf("expiresAt" to invite.expiresAt.toString()),
        )
        return invite
    }

    /** Принимает invite и отправляет pairing request. */
    suspend fun pairWithInvite(encodedInvite: String): PairingSession {
        val invite = PairingInvite.fromEncodedString(encodedInvite)
        require(!invite.isExpired(now())) { "Pairing invite has expired" }

        val localProfile = localProfileService.require()
        invite.endpointHint?.let {
            endpointCachePort.put(
                PeerEndpointCandidate(
                    peerId = invite.peerId,
                    endpoint = it,
                    source = EndpointSource.MANUAL_HINT,
                    discoveredAt = now(),
                    capabilities = invite.capabilities,
                ),
            )
        }

        val requestNonce = cryptoPort.randomSecret(16)
        val session = PairingSession(
            sessionId = newId("pairing"),
            localPeerId = localProfile.peerId,
            remotePeerId = invite.peerId,
            role = PairingSessionRole.JOINER,
            inviteSecret = invite.inviteSecret,
            requestNonce = requestNonce,
            createdAt = now(),
            expiresAt = invite.expiresAt,
            trustState = TrustState.PENDING,
            endpointHint = invite.endpointHint,
            remotePublicKey = invite.publicKey,
            remoteDisplayName = invite.displayName,
        )
        pairingSessionRepositoryPort.save(session)

        val request = PairRequest(
            protocolVersion = invite.protocolVersion,
            requesterPeerId = localProfile.peerId,
            requesterDisplayName = localProfile.displayName,
            requesterPublicKey = localProfile.publicKey,
            inviteSecret = invite.inviteSecret,
            requestNonce = requestNonce,
            expiresAt = invite.expiresAt,
            endpointHint = localEndpointProvider(),
            capabilities = localProfile.capabilities,
        )
        val encrypted = messageEncryptionService.encryptPayload(invite.publicKey, request)
        val unsigned = packetEnvelopeFactory.create(
            packetType = PacketType.PAIR_REQUEST,
            sourcePeerId = localProfile.peerId,
            targetPeerId = invite.peerId,
            encryptedPayload = encrypted,
            routeMode = RouteMode.LOCAL_DIRECT,
            ttl = 5,
            requiresAck = false,
        )
        val signed = packetSignatureService.signEnvelope(localProfile.privateKey, unsigned)
        val result = deliveryTrackingService.send(signed)
        require(result.success) { "Failed to send pairing request: ${result.errorMessage}" }

        eventLogService.log(
            category = EventCategory.PAIRING,
            level = EventLevel.INFO,
            message = "Sent pairing request",
            peerId = invite.peerId,
            packetId = signed.packetId,
        )
        nodeMetricsService.increment("pairing.request.sent")
        return session
    }

    /** Обрабатывает входящий pairing request. */
    suspend fun handlePairRequest(envelope: PacketEnvelope, request: PairRequest): PairedPeer? {
        val localProfile = localProfileService.require()
        if (request.expiresAt <= now()) {
            recordPairingIncident(envelope, request.requesterPeerId, "Expired pairing request", SecurityIncidentType.REPLAY)
            return null
        }
        val session = pairingSessionRepositoryPort.findActiveByInviteSecret(request.inviteSecret)
        if (session == null || session.used || session.isExpired(now())) {
            recordPairingIncident(envelope, request.requesterPeerId, "Unknown or already used invite secret", SecurityIncidentType.REPLAY)
            return null
        }
        if (cryptoPort.derivePeerId(request.requesterPublicKey) != request.requesterPeerId) {
            recordPairingIncident(envelope, request.requesterPeerId, "Requester peerId does not match public key", SecurityIncidentType.TRUST_FAILURE)
            return null
        }
        if (!packetSignatureService.verifyEnvelope(request.requesterPublicKey, envelope)) {
            recordPairingIncident(envelope, request.requesterPeerId, "Pairing request signature mismatch", SecurityIncidentType.SIGNATURE_MISMATCH)
            return null
        }

        val pairedPeer = PairedPeer(
            peerIdentity = PeerIdentity(
                peerId = request.requesterPeerId,
                displayName = request.requesterDisplayName,
                publicKey = request.requesterPublicKey,
                capabilities = request.capabilities,
            ),
            trustState = TrustState.TRUSTED,
            pairedAt = now(),
            endpointHint = request.endpointHint,
        )
        peerRepositoryPort.save(pairedPeer)
        pairingSessionRepositoryPort.save(
            session.copy(
                remotePeerId = request.requesterPeerId,
                requestNonce = request.requestNonce,
                remotePublicKey = request.requesterPublicKey,
                remoteDisplayName = request.requesterDisplayName,
                trustState = TrustState.TRUSTED,
                used = true,
                endpointHint = request.endpointHint ?: session.endpointHint,
            ),
        )
        request.endpointHint?.let {
            endpointCachePort.put(
                PeerEndpointCandidate(
                    peerId = request.requesterPeerId,
                    endpoint = it,
                    source = EndpointSource.MANUAL_HINT,
                    discoveredAt = now(),
                    capabilities = request.capabilities,
                ),
            )
        }

        val accept = PairAccept(
            protocolVersion = request.protocolVersion,
            accepterPeerId = localProfile.peerId,
            accepterDisplayName = localProfile.displayName,
            accepterPublicKey = localProfile.publicKey,
            inviteSecret = request.inviteSecret,
            requestNonce = request.requestNonce,
            acceptNonce = cryptoPort.randomSecret(16),
            expiresAt = now().plusSeconds(300),
            endpointHint = localEndpointProvider(),
            capabilities = localProfile.capabilities,
        )
        val encrypted = messageEncryptionService.encryptPayload(request.requesterPublicKey, accept)
        val unsigned = packetEnvelopeFactory.create(
            packetType = PacketType.PAIR_ACCEPT,
            sourcePeerId = localProfile.peerId,
            targetPeerId = request.requesterPeerId,
            encryptedPayload = encrypted,
            routeMode = RouteMode.LOCAL_DIRECT,
            ttl = 5,
            requiresAck = false,
        )
        val signed = packetSignatureService.signEnvelope(localProfile.privateKey, unsigned)
        deliveryTrackingService.send(signed)

        eventLogService.log(
            category = EventCategory.PAIRING,
            level = EventLevel.INFO,
            message = "Accepted pairing request",
            peerId = request.requesterPeerId,
            packetId = signed.packetId,
        )
        nodeMetricsService.increment("pairing.accept.sent")
        logger.info { "Peer ${request.requesterPeerId} is now trusted" }
        return pairedPeer
    }

    /**
     * Handles an inbound pairing acceptance and promotes the inviter to trusted state.
     */
    suspend fun handlePairAccept(envelope: PacketEnvelope, accept: PairAccept): PairedPeer? {
        val session = pairingSessionRepositoryPort.findActiveByInviteSecret(accept.inviteSecret)
        if (session == null || session.used || session.role != PairingSessionRole.JOINER || session.isExpired(now())) {
            recordPairingIncident(envelope, accept.accepterPeerId, "Unexpected pairing acceptance", SecurityIncidentType.REPLAY)
            return null
        }
        if (session.requestNonce != accept.requestNonce) {
            recordPairingIncident(envelope, accept.accepterPeerId, "Pairing acceptance nonce mismatch", SecurityIncidentType.REPLAY)
            return null
        }
        if (cryptoPort.derivePeerId(accept.accepterPublicKey) != accept.accepterPeerId) {
            recordPairingIncident(envelope, accept.accepterPeerId, "Accepter peerId does not match public key", SecurityIncidentType.TRUST_FAILURE)
            return null
        }
        if (!packetSignatureService.verifyEnvelope(accept.accepterPublicKey, envelope)) {
            recordPairingIncident(envelope, accept.accepterPeerId, "Pairing accept signature mismatch", SecurityIncidentType.SIGNATURE_MISMATCH)
            return null
        }

        val pairedPeer = PairedPeer(
            peerIdentity = PeerIdentity(
                peerId = accept.accepterPeerId,
                displayName = accept.accepterDisplayName,
                publicKey = accept.accepterPublicKey,
                capabilities = accept.capabilities,
            ),
            trustState = TrustState.TRUSTED,
            pairedAt = now(),
            endpointHint = accept.endpointHint,
        )
        peerRepositoryPort.save(pairedPeer)
        pairingSessionRepositoryPort.save(
            session.copy(
                remotePeerId = accept.accepterPeerId,
                remotePublicKey = accept.accepterPublicKey,
                remoteDisplayName = accept.accepterDisplayName,
                trustState = TrustState.TRUSTED,
                used = true,
                endpointHint = accept.endpointHint ?: session.endpointHint,
            ),
        )
        accept.endpointHint?.let {
            endpointCachePort.put(
                PeerEndpointCandidate(
                    peerId = accept.accepterPeerId,
                    endpoint = it,
                    source = EndpointSource.MANUAL_HINT,
                    discoveredAt = now(),
                    capabilities = accept.capabilities,
                ),
            )
        }
        eventLogService.log(
            category = EventCategory.PAIRING,
            level = EventLevel.INFO,
            message = "Pairing completed",
            peerId = accept.accepterPeerId,
            packetId = envelope.packetId,
        )
        nodeMetricsService.increment("pairing.completed")
        return pairedPeer
    }

    private suspend fun recordPairingIncident(
        envelope: PacketEnvelope,
        peerId: String,
        description: String,
        type: SecurityIncidentType,
    ) {
        securityIncidentService.record(
            SecurityIncident(
                incidentId = newId("security"),
                type = type,
                severity = SecuritySeverity.HIGH,
                peerId = peerId,
                packetId = envelope.packetId,
                description = description,
                occurredAt = now(),
            ),
        )
    }
}
