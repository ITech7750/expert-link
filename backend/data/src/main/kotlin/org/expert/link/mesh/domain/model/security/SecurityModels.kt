package org.expert.link.mesh.domain.model.security

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Ссылка на локально управляемый криптографический материал. */
@Serializable
data class CryptoMaterialRef(
    val alias: String,
    val storageType: CryptoStorageType,
    val createdAt: Instant,
)

/** Тип хранилища ключевого материала. */
@Serializable
enum class CryptoStorageType {
    IN_MEMORY,
    FILE_SYSTEM,
    HARDWARE_BACKED,
}

/** Сырой асимметричный ключевой материал локального узла. */
@Serializable
data class KeyMaterial(
    val keyId: String,
    val publicKey: String,
    val privateKey: String,
    val algorithm: String,
    val createdAt: Instant,
)

/** Зашифрованные данные для передачи по сети. */
@Serializable
data class EncryptedPayload(
    val payloadNonce: String,
    val encryptedPayload: String,
)

/** Запись о локально заблокированном узле. */
@Serializable
data class BlockedPeer(
    val peerId: String,
    val reason: String,
    val blockedAt: Instant,
    val expiresAt: Instant? = null,
)

/** Область действия правила ограничения запросов. */
@Serializable
enum class RateLimitScope {
    GLOBAL,
    PEER,
    PACKET_TYPE,
}

/** Правило ограничения запросов. */
@Serializable
data class RateLimitRule(
    val ruleId: String,
    val scope: RateLimitScope,
    val maxRequests: Int,
    val windowSeconds: Int,
    val burstSize: Int = maxRequests,
)

/** Уровень критичности инцидента безопасности. */
@Serializable
enum class SecuritySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

/** Типы инцидентов безопасности. */
@Serializable
enum class SecurityIncidentType {
    SPOOFED_DISCOVERY,
    REPLAY,
    RATE_LIMIT,
    BLOCKED_PEER,
    SIGNATURE_MISMATCH,
    TRUST_FAILURE,
    UNEXPECTED_PACKET,
    ROUTE_POISONING,
}

/** Неизменяемая запись инцидента безопасности. */
@Serializable
data class SecurityIncident(
    val incidentId: String,
    val type: SecurityIncidentType,
    val severity: SecuritySeverity,
    val peerId: String? = null,
    val packetId: String? = null,
    val description: String,
    val occurredAt: Instant,
    val attributes: Map<String, String> = emptyMap(),
)
