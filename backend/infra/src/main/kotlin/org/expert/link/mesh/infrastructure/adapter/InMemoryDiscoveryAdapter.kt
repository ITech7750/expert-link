package org.expert.link.mesh.infrastructure.adapter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.network.DiscoveryFrame
import org.expert.link.mesh.domain.model.network.DiscoveryMessageType
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.port.external.DiscoveryPort
import java.util.concurrent.CopyOnWriteArrayList

/** In-memory discovery-адаптер для тестов и симулятора. */
class InMemoryDiscoveryAdapter : DiscoveryPort {
    private val flow = MutableSharedFlow<DiscoveryFrame>(extraBufferCapacity = 64)
    private lateinit var localPeerIdentity: PeerIdentity
    private lateinit var localEndpoint: PeerEndpoint

    override val events: Flow<DiscoveryFrame> = flow

    override suspend fun start(localPeerIdentity: PeerIdentity, endpoint: PeerEndpoint) {
        this.localPeerIdentity = localPeerIdentity
        this.localEndpoint = endpoint
        instances += this
    }

    override suspend fun stop() {
        instances -= this
    }

    override suspend fun broadcastHello() {
        emit(DiscoveryMessageType.NODE_HELLO, null)
    }

    override suspend fun broadcastBye() {
        emit(DiscoveryMessageType.NODE_BYE, null)
    }

    override suspend fun lookupPeer(targetPeerId: String) {
        emit(DiscoveryMessageType.PEER_LOOKUP, targetPeerId)
    }

    override suspend fun announcePeer(targetPeerId: String?) {
        emit(DiscoveryMessageType.PEER_ANNOUNCE, targetPeerId)
    }

    private suspend fun emit(type: DiscoveryMessageType, targetPeerId: String?) {
        val frame = DiscoveryFrame(
            protocolVersion = 1,
            type = type,
            sourcePeerId = localPeerIdentity.peerId,
            displayName = localPeerIdentity.displayName,
            publicKey = localPeerIdentity.publicKey,
            targetPeerId = targetPeerId,
            endpoint = localEndpoint,
            capabilities = localPeerIdentity.capabilities,
            createdAt = kotlinx.datetime.Clock.System.now(),
            expiresAt = kotlinx.datetime.Instant.fromEpochMilliseconds(kotlinx.datetime.Clock.System.now().toEpochMilliseconds() + 60_000),
        )
        instances.filterNot { it === this }.forEach { it.flow.emit(frame) }
    }

    companion object {
        private val instances = CopyOnWriteArrayList<InMemoryDiscoveryAdapter>()
    }
}
