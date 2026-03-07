package org.expert.link.mesh.domain.model.network

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/** Режимы доставки пакетов. */
@Serializable
enum class RouteMode {
    LOCAL_DIRECT,
    RELAY_FLOOD,
    OVERLAY_DIRECT,
    RENDEZVOUS_DIRECT_CANDIDATE,
    RENDEZVOUS_RELAY,
}

/** Типы пакетов. */
@Serializable
enum class PacketType {
    PAIR_REQUEST,
    PAIR_ACCEPT,
    PEER_LOOKUP,
    PEER_ANNOUNCE,
    CHAT_MESSAGE,
    DELIVERY_ACK,
    FILE_OFFER,
    FILE_ACCEPT,
    FILE_CHUNK,
    FILE_ACK,
    FILE_COMPLETE,
    FILE_RESUME_REQUEST,
    CALL_INVITE,
    CALL_SIGNAL,
    CALL_HANGUP,
    SYSTEM_EVENT,
}

/** Типы кадров поиска узлов. */
@Serializable
enum class DiscoveryMessageType {
    NODE_HELLO,
    NODE_BYE,
    PEER_LOOKUP,
    PEER_ANNOUNCE,
}

/** Источник кандидата endpoint. */
@Serializable
enum class EndpointSource {
    DISCOVERY_MULTICAST,
    DISCOVERY_BROADCAST,
    MANUAL_HINT,
    ROUTE_LEARNING,
    RENDEZVOUS,
}

/** Транспортный endpoint узла. */
@Serializable
data class PeerEndpoint(
    val scheme: String = "http",
    val host: String,
    val port: Int,
    val path: String = "/api/v1/packets",
    val announcedPeerId: String? = null,
    val announcedAt: Instant,
    val expiresAt: Instant? = null,
) {
    /** Возвращает абсолютный URL endpoint. */
    fun asUrl(): String = "$scheme://$host:$port$path"
}

/** Кандидат endpoint, найденный через mesh или overlay. */
@Serializable
data class PeerEndpointCandidate(
    val peerId: String,
    val endpoint: PeerEndpoint,
    val source: EndpointSource,
    val discoveredAt: Instant,
    val qualityScore: Int = 100,
    val capabilities: Set<String> = emptySet(),
)

/** Текущий маршрут до узла. */
@Serializable
data class RouteEntry(
    val targetPeerId: String,
    val nextHopPeerId: String,
    val endpoint: PeerEndpoint?,
    val routeMode: RouteMode,
    val hopCount: Int,
    val expiresAt: Instant,
    val learnedAt: Instant,
    val direct: Boolean,
)

/** Обратный путь пакета для ACK. */
@Serializable
data class ReversePathEntry(
    val packetId: String,
    val messageId: String? = null,
    val previousHopPeerId: String,
    val previousHopEndpoint: PeerEndpoint? = null,
    val createdAt: Instant,
    val expiresAt: Instant,
)

/** Данные ACK для ранее полученного пакета. */
@Serializable
data class DeliveryAck(
    val acknowledgedPacketId: String,
    val messageId: String? = null,
    val conversationId: String? = null,
    val status: String = "DELIVERED",
    val receivedAt: Instant,
)

/** Трассировка hop-ов пакета. */
@Serializable
data class HopTrace(
    val packetId: String,
    val hops: List<String>,
    val observedAt: Instant,
)

/** Типы сетевых событий. */
@Serializable
enum class NetworkEventType {
    DISCOVERY,
    ROUTE_UPDATED,
    ROUTE_EXPIRED,
    PACKET_RELAYED,
    ACK_RECEIVED,
    ACK_TIMEOUT,
    SECURITY,
    TRANSPORT,
}

/** Структурированное сетевое событие. */
@Serializable
data class NetworkEvent(
    val eventId: String,
    val type: NetworkEventType,
    val peerId: String? = null,
    val packetId: String? = null,
    val routeMode: RouteMode? = null,
    val detail: String,
    val occurredAt: Instant,
)

/** Discovery-кадр для multicast или broadcast. */
@Serializable
data class DiscoveryFrame(
    val protocolVersion: Int,
    val type: DiscoveryMessageType,
    val sourcePeerId: String,
    val displayName: String,
    val publicKey: String,
    val targetPeerId: String? = null,
    val endpoint: PeerEndpoint,
    val capabilities: Set<String> = emptySet(),
    val createdAt: Instant,
    val expiresAt: Instant,
)

/** Результат доставки пакета транспортом. */
@Serializable
data class TransportDeliveryResult(
    val success: Boolean,
    val statusCode: Int? = null,
    val errorMessage: String? = null,
    val deliveredAt: Instant? = null,
)

/** Состояние пакета, ожидающего подтверждение. */
@Serializable
data class PendingAckRecord(
    val packetId: String,
    val messageId: String,
    val conversationId: String,
    val targetPeerId: String,
    val routeMode: RouteMode,
    val attempt: Int,
    val nextAttemptAt: Instant,
    val envelope: PacketEnvelope,
    val createdAt: Instant,
)

/** Пакет, который передаётся между mesh-узлами.
 *
 * Содержит метаданные маршрутизации, зашифрованный payload и подпись источника.
 * Один и тот же конверт используется для прямой доставки, multihop и будущего relay-сервера.
 *
 *  packetId Идентификатор пакета для dedup и reverse path.
 *  messageId Идентификатор бизнес-сообщения, если он есть.
 *  conversationId Идентификатор диалога, если он есть.
 *  packetType Тип пакета.
 *  sourcePeerId peerId отправителя.
 *  targetPeerId peerId получателя.
 *  previousHopPeerId peerId предыдущего hop.
 *  ttl Оставшееся число hop до удаления пакета.
 *  hopCount Число уже пройденных hop.
 *  createdAt Время создания пакета.
 *  requiresAck Нужен ли ACK.
 *  routeMode Выбранный режим доставки.
 *  payloadNonce Nonce шифрования payload.
 *  encryptedPayload Зашифрованный payload.
 *  metadataSignature Подпись неизменяемых метаданных.
 */
@Serializable
data class PacketEnvelope(
    val packetId: String,
    val messageId: String? = null,
    val conversationId: String? = null,
    val packetType: PacketType,
    val sourcePeerId: String,
    val targetPeerId: String,
    val previousHopPeerId: String? = null,
    val ttl: Int,
    val hopCount: Int,
    val createdAt: Instant,
    val requiresAck: Boolean,
    val routeMode: RouteMode,
    val payloadNonce: String,
    val encryptedPayload: String,
    val metadataSignature: String,
) {
    init {
        require(ttl > 0) { "ttl must be positive" }
        require(hopCount >= 0) { "hopCount must be non-negative" }
    }

    /**
     * Returns `true` when the packet may still be relayed to another hop.
     */
    fun canRelay(): Boolean = ttl > 1

    /**
     * Creates a relay-ready copy with decremented TTL and incremented hop count.
     *
     * @param newPreviousHopPeerId identity of the node that is about to forward the packet.
     * @return a new envelope suitable for the next hop.
     * @throws IllegalStateException when the packet cannot be relayed because TTL would reach zero.
     */
    fun decrementTtl(newPreviousHopPeerId: String): PacketEnvelope {
        check(canRelay()) { "packet $packetId exhausted its ttl and cannot be relayed" }
        return copy(
            ttl = ttl - 1,
            hopCount = hopCount + 1,
            previousHopPeerId = newPreviousHopPeerId,
        )
    }

    /**
     * Produces the canonical string signed by the source peer.
     *
     * The string intentionally contains only immutable metadata and the encrypted payload bytes. Relay nodes may decrement TTL, update the previous hop and even switch the effective route mode during retries or relay fallback, so the signature intentionally excludes mutable delivery metadata and only covers immutable source metadata plus the ciphertext.
     *
     * @return canonical metadata representation suitable for digital signatures.
     */
    fun signaturePayload(): String = listOf(
        packetId,
        messageId.orEmpty(),
        conversationId.orEmpty(),
        packetType.name,
        sourcePeerId,
        targetPeerId,
        createdAt.toString(),
        requiresAck.toString(),
        payloadNonce,
        encryptedPayload,
    ).joinToString(separator = "|")

    override fun toString(): String = buildString {
        append("PacketEnvelope(")
        append(packetType.name)
        append(", packetId=")
        append(packetId)
        append(", source=")
        append(sourcePeerId)
        append(", target=")
        append(targetPeerId)
        append(", ttl=")
        append(ttl)
        append(", hopCount=")
        append(hopCount)
        append(", createdAt=")
        append(createdAt.toLocalDateTime(TimeZone.UTC))
        append(")")
    }
}

/** Роль узла относительно локальной топологии. */
@Serializable
enum class HostRole {
    HOST,
    MEMBER,
    CANDIDATE,
    UNKNOWN,
}

/** Текущий режим сетевой связности узла. */
@Serializable
enum class ConnectivityMode {
    LOCAL_MESH,
    HOST_ROUTED,
    RELAY_PROXY,
    DEGRADED,
}

/** Состояние relay/proxy режима. */
@Serializable
enum class RelayMode {
    DISABLED,
    STANDBY,
    ACTIVE_FALLBACK,
    FORCED,
}

/** Состояние здоровья маршрута. */
@Serializable
enum class RouteHealthState {
    HEALTHY,
    DEGRADED,
    STALE,
    FAILED,
}

/** Кандидат на роль хоста локальной сети. */
@Serializable
data class HostCandidate(
    val peerId: String,
    val endpoint: PeerEndpoint? = null,
    val score: Int,
    val reachable: Boolean,
    val roleHint: HostRole = HostRole.CANDIDATE,
    val lastSeenAt: Instant,
)

/** Выбранная стратегия связности к конкретному peer. */
@Serializable
data class ConnectivityStrategy(
    val targetPeerId: String,
    val routeMode: RouteMode,
    val connectivityMode: ConnectivityMode,
    val relayMode: RelayMode,
    val useRelayGateway: Boolean,
    val reason: String,
    val decidedAt: Instant,
)

/** Снимок здоровья маршрута. */
@Serializable
data class RouteHealth(
    val targetPeerId: String,
    val nextHopPeerId: String? = null,
    val routeMode: RouteMode,
    val state: RouteHealthState,
    val failureCount: Int = 0,
    val lastUpdatedAt: Instant,
    val expiresAt: Instant? = null,
    val detail: String? = null,
)

/** Типы событий перестройки топологии. */
@Serializable
enum class TopologyEventType {
    STARTED,
    HOST_ELECTED,
    HOST_LOST,
    HOST_SWITCHED,
    ROUTE_HEALTH_CHANGED,
    CONNECTIVITY_MODE_CHANGED,
    RELAY_MODE_CHANGED,
    TOPOLOGY_REBUILT,
    CONTINUITY_DEGRADED,
    CONTINUITY_RECOVERED,
}

/** Событие изменения сетевой топологии. */
@Serializable
data class TopologyEvent(
    val eventId: String,
    val eventType: TopologyEventType,
    val message: String,
    val peerId: String? = null,
    val targetPeerId: String? = null,
    val detail: Map<String, String> = emptyMap(),
    val occurredAt: Instant,
)

/** Состояние роли узла и выбранного хоста. */
@Serializable
data class NetworkRoleState(
    val localPeerId: String,
    val localRole: HostRole,
    val currentHostPeerId: String? = null,
    val hostReachable: Boolean,
    val failoverInProgress: Boolean,
    val updatedAt: Instant,
)

/** Полный снимок состояния топологии и связности узла. */
@Serializable
data class NetworkTopologyState(
    val localPeerId: String,
    val connectivityMode: ConnectivityMode,
    val relayMode: RelayMode,
    val networkRoleState: NetworkRoleState,
    val hostCandidates: List<HostCandidate> = emptyList(),
    val routeHealth: List<RouteHealth> = emptyList(),
    val activeStrategies: List<ConnectivityStrategy> = emptyList(),
    val pendingAckCount: Int = 0,
    val queuedPacketCount: Int = 0,
    val activeFileTransfers: Int = 0,
    val activeCallSessions: Int = 0,
    val continuityDegraded: Boolean = false,
    val recentEvents: List<TopologyEvent> = emptyList(),
    val refreshedAt: Instant,
)
