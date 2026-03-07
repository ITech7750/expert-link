package org.expert.link.mesh.infrastructure.adapter

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.network.DiscoveryFrame
import org.expert.link.mesh.domain.model.network.DiscoveryMessageType
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.port.external.DiscoveryPort
import org.expert.link.mesh.domain.port.external.MulticastSupportPort
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketException

/** UDP discovery-адаптер с fallback на broadcast. */
class UdpDiscoveryAdapter(
    private val discoveryPort: Int,
    private val multicastGroup: String,
    private val multicastSupportPort: MulticastSupportPort,
) : DiscoveryPort {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sharedFlow = MutableSharedFlow<DiscoveryFrame>(extraBufferCapacity = 64)
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private var socket: MulticastSocket? = null
    private var receiveJob: Job? = null
    private var multicastEnabled: Boolean = true
    private lateinit var localPeerIdentity: PeerIdentity
    private lateinit var localEndpoint: PeerEndpoint

    override val events: Flow<DiscoveryFrame> = sharedFlow

    override suspend fun start(localPeerIdentity: PeerIdentity, endpoint: PeerEndpoint) {
        this.localPeerIdentity = localPeerIdentity
        this.localEndpoint = endpoint
        multicastSupportPort.prepareForMulticast()
        val multicastSocket = MulticastSocket(discoveryPort)
        multicastSocket.broadcast = true
        multicastEnabled = multicastSupportPort.isMulticastSupported()
        if (multicastEnabled) {
            multicastSocket.joinGroup(InetAddress.getByName(multicastGroup))
        }
        socket = multicastSocket
        receiveJob = scope.launch {
            val buffer = ByteArray(65_535)
            while (true) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    multicastSocket.receive(packet)
                    val frame = json.decodeFromString(DiscoveryFrame.serializer(), packet.data.copyOf(packet.length).toString(Charsets.UTF_8))
                    sharedFlow.emit(frame)
                } catch (_: SocketException) {
                    break
                } catch (error: Exception) {
                    logger.warn(error) { "Failed to decode discovery frame" }
                }
            }
        }
    }

    override suspend fun stop() {
        receiveJob?.cancel()
        socket?.close()
        scope.cancel()
    }

    override suspend fun broadcastHello() {
        sendFrame(DiscoveryMessageType.NODE_HELLO)
    }

    override suspend fun broadcastBye() {
        sendFrame(DiscoveryMessageType.NODE_BYE)
    }

    override suspend fun lookupPeer(targetPeerId: String) {
        sendFrame(DiscoveryMessageType.PEER_LOOKUP, targetPeerId)
    }

    override suspend fun announcePeer(targetPeerId: String?) {
        sendFrame(DiscoveryMessageType.PEER_ANNOUNCE, targetPeerId)
    }

    private fun sendFrame(type: DiscoveryMessageType, targetPeerId: String? = null) {
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
            expiresAt = kotlinx.datetime.Clock.System.now().let { kotlinx.datetime.Instant.fromEpochMilliseconds(it.toEpochMilliseconds() + 60_000) },
        )
        val payload = json.encodeToString(DiscoveryFrame.serializer(), frame).toByteArray(Charsets.UTF_8)
        val destination = if (multicastEnabled) InetAddress.getByName(multicastGroup) else InetAddress.getByName("255.255.255.255")
        val packet = DatagramPacket(payload, payload.size, destination, discoveryPort)
        socket?.send(packet)
    }
}
