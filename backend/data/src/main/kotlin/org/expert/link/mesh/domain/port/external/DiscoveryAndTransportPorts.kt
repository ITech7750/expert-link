package org.expert.link.mesh.domain.port.external

import kotlinx.coroutines.flow.Flow
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.network.DiscoveryFrame
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.TransportDeliveryResult

/** Порт поиска узлов в локальной сети. */
interface DiscoveryPort {
    /** Поток discovery-кадров. */
    val events: Flow<DiscoveryFrame>

    /** Запускает discovery для локального узла. */
    suspend fun start(localPeerIdentity: PeerIdentity, endpoint: PeerEndpoint)

    /** Останавливает discovery. */
    suspend fun stop()

    /** Рассылает hello-кадр. */
    suspend fun broadcastHello()

    /** Рассылает bye-кадр. */
    suspend fun broadcastBye()

    /** Запускает lookup конкретного peer. */
    suspend fun lookupPeer(targetPeerId: String)

    /** Публикует announce локального peer. */
    suspend fun announcePeer(targetPeerId: String? = null)
}

/** Порт отправки пакета в конкретный endpoint. */
interface PacketTransportPort {
    /** Отправляет пакет в endpoint. */
    suspend fun sendPacket(endpoint: PeerEndpoint, envelope: PacketEnvelope): TransportDeliveryResult
}
