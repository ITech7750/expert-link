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
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Collections

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
    private val relayedFrameCache = linkedMapOf<String, Long>()

    override val events: Flow<DiscoveryFrame> = sharedFlow

    override suspend fun start(localPeerIdentity: PeerIdentity, endpoint: PeerEndpoint) {
        this.localPeerIdentity = localPeerIdentity
        this.localEndpoint = endpoint
        multicastSupportPort.prepareForMulticast()
        val multicastSocket = MulticastSocket(null)
        multicastSocket.reuseAddress = true
        multicastSocket.bind(InetSocketAddress(discoveryPort))
        multicastSocket.broadcast = true
        multicastEnabled = multicastSupportPort.isMulticastSupported()
        if (multicastEnabled) {
            val multicastAddress = InetAddress.getByName(multicastGroup)
            val joinedAny = joinMulticastInterfaces(multicastSocket, multicastAddress)
            if (!joinedAny) {
                multicastSocket.joinGroup(multicastAddress)
            }
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
                    relayFrameIfNeeded(multicastSocket, frame, packet.address)
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
        multicastSupportPort.releaseMulticast()
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
        destinationAddresses().forEach { destination ->
            destinationPorts().forEach { targetPort ->
                val packet = DatagramPacket(payload, payload.size, destination, targetPort)
                runCatching { socket?.send(packet) }
                    .onFailure { error ->
                        logger.debug(error) {
                            "Failed to send discovery frame to ${destination.hostAddress}:$targetPort"
                        }
                    }
            }
        }
    }

    private fun joinMulticastInterfaces(socket: MulticastSocket, group: InetAddress): Boolean {
        var joined = false
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { networkInterface ->
                runCatching {
                    networkInterface.isUp && networkInterface.supportsMulticast()
                }.getOrDefault(false)
            }
        interfaces.forEach { networkInterface ->
            runCatching {
                socket.joinGroup(InetSocketAddress(group, discoveryPort), networkInterface)
                joined = true
            }.onFailure { error ->
                logger.debug(error) { "Failed to join multicast on ${networkInterface.displayName}" }
            }
        }
        return joined
    }

    private fun destinationAddresses(): List<InetAddress> {
        val addresses = linkedSetOf<InetAddress>()
        if (multicastEnabled) {
            runCatching { InetAddress.getByName(multicastGroup) }.onSuccess { addresses.add(it) }
        }
        collectInterfaceBroadcasts().forEach { addresses.add(it) }
        if (addresses.isEmpty()) {
            addresses.add(InetAddress.getByName("255.255.255.255"))
        }
        return addresses.toList()
    }

    private fun destinationPorts(): List<Int> {
        val ports = linkedSetOf<Int>()
        for (delta in -2..2) {
            val candidate = discoveryPort + delta
            if (candidate in 1..65_535) {
                ports.add(candidate)
            }
        }
        // Фиксированные fallback-порты для автообнаружения между узлами с разной конфигурацией.
        listOf(19_100, 19_101, 19_102).forEach { ports.add(it) }
        return ports.toList()
    }

    private fun collectInterfaceBroadcasts(): List<InetAddress> {
        return runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces())
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { networkInterface -> networkInterface.interfaceAddresses.asSequence() }
                .mapNotNull { it.broadcast }
                .toList()
        }.getOrDefault(emptyList())
    }

    private fun relayFrameIfNeeded(socket: MulticastSocket, frame: DiscoveryFrame, sourceAddress: InetAddress) {
        if (frame.sourcePeerId == localPeerIdentity.peerId) {
            return
        }
        if (!isBridgeNode()) {
            return
        }
        if (!markRelayed(frame)) {
            return
        }
        val payload = json.encodeToString(DiscoveryFrame.serializer(), frame).toByteArray(Charsets.UTF_8)
        destinationAddresses()
            .filterNot { it.hostAddress == sourceAddress.hostAddress }
            .forEach { destination ->
                destinationPorts().forEach { targetPort ->
                    runCatching {
                        socket.send(DatagramPacket(payload, payload.size, destination, targetPort))
                    }.onFailure { error ->
                        logger.debug(error) {
                            "Failed to relay discovery frame to ${destination.hostAddress}:$targetPort"
                        }
                    }
                }
            }
    }

    private fun isBridgeNode(): Boolean = collectInterfaceBroadcasts().size > 1

    private fun markRelayed(frame: DiscoveryFrame): Boolean {
        val nowMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val frameKey = buildString {
            append(frame.type.name)
            append('|')
            append(frame.sourcePeerId)
            append('|')
            append(frame.targetPeerId.orEmpty())
            append('|')
            append(frame.createdAt.toEpochMilliseconds())
            append('|')
            append(frame.endpoint.host)
            append(':')
            append(frame.endpoint.port)
        }
        synchronized(relayedFrameCache) {
            val iterator = relayedFrameCache.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (nowMs - entry.value > RELAY_FRAME_TTL_MS) {
                    iterator.remove()
                }
            }
            if (relayedFrameCache.containsKey(frameKey)) {
                return false
            }
            relayedFrameCache[frameKey] = nowMs
            return true
        }
    }

    private companion object {
        private const val RELAY_FRAME_TTL_MS = 30_000L
    }
}
