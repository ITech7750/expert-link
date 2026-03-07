package org.expert.link.mesh.domain.model.identity

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.security.CryptoMaterialRef
import org.expert.link.mesh.domain.support.decodeBase64Url
import org.expert.link.mesh.domain.support.encodeBase64Url

/** Состояния доверия. */
@Serializable
enum class TrustState {
    INVITED,
    PENDING,
    TRUSTED,
    BLOCKED,
    REVOKED,
}

/** Публичная идентичность узла. */
@Serializable
data class PeerIdentity(
    val peerId: String,
    val displayName: String,
    val publicKey: String,
    val capabilities: Set<String> = emptySet(),
)

/** Локальный профиль узла с ключевым материалом. */
@Serializable
data class LocalProfile(
    val peerId: String,
    val displayName: String,
    val publicKey: String,
    val privateKey: String,
    val keyMaterialRef: CryptoMaterialRef,
    val capabilities: Set<String> = emptySet(),
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Доверенный или ожидающий узел. */
@Serializable
data class PairedPeer(
    val peerIdentity: PeerIdentity,
    val trustState: TrustState,
    val pairedAt: Instant,
    val lastSeenAt: Instant? = null,
    val endpointHint: PeerEndpoint? = null,
    val metadata: Map<String, String> = emptyMap(),
)

/** Invite для сопряжения через QR или внешний канал. */
@Serializable
data class PairingInvite(
    val protocolVersion: Int,
    val peerId: String,
    val displayName: String,
    val publicKey: String,
    val inviteSecret: String,
    val expiresAt: Instant,
    val endpointHint: PeerEndpoint? = null,
    val capabilities: Set<String> = emptySet(),
) {
    /** Кодирует invite в URL-safe строку. */
    fun toEncodedString(): String {
        val json = Json.encodeToString(PairingInvite.serializer(), this)
        return encodeBase64Url(json.encodeToByteArray())
    }

    /** Возвращает `true`, если invite истёк. */
    fun isExpired(now: Instant = Clock.System.now()): Boolean = expiresAt <= now

    companion object {
        /** Декодирует ранее сериализованный invite. */
        fun fromEncodedString(encoded: String): PairingInvite {
            val decoded = decodeBase64Url(encoded).decodeToString()
            return Json.decodeFromString(PairingInvite.serializer(), decoded)
        }
    }
}

/** Роль узла в сессии сопряжения. */
@Serializable
enum class PairingSessionRole {
    INVITER,
    JOINER,
}

/** Состояние сессии сопряжения и anti-replay данных. */
@Serializable
data class PairingSession(
    val sessionId: String,
    val localPeerId: String,
    val remotePeerId: String? = null,
    val role: PairingSessionRole,
    val inviteSecret: String,
    val requestNonce: String? = null,
    val acceptNonce: String? = null,
    val createdAt: Instant,
    val expiresAt: Instant,
    val trustState: TrustState,
    val used: Boolean = false,
    val endpointHint: PeerEndpoint? = null,
    val remotePublicKey: String? = null,
    val remoteDisplayName: String? = null,
) {
    /** Возвращает `true`, если pairing-сессия истекла. */
    fun isExpired(now: Instant = Clock.System.now()): Boolean = expiresAt <= now
}
