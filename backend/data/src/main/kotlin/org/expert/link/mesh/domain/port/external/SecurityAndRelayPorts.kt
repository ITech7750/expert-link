package org.expert.link.mesh.domain.port.external

import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.TransportDeliveryResult
import org.expert.link.mesh.domain.model.relay.PeerLookupResult
import org.expert.link.mesh.domain.model.relay.PeerRegistration
import org.expert.link.mesh.domain.model.security.EncryptedPayload
import org.expert.link.mesh.domain.model.security.KeyMaterial
import org.expert.link.mesh.domain.model.diagnostics.EventLogEntry

/** Криптографический порт. */
interface CryptoPort {
    /** Генерирует новую пару ключей. */
    suspend fun generateKeyMaterial(alias: String): KeyMaterial

    /** Вычисляет peerId по публичному ключу. */
    fun derivePeerId(publicKey: String): String

    /** Шифрует payload для владельца публичного ключа. */
    suspend fun encrypt(targetPublicKey: String, payload: ByteArray): EncryptedPayload

    /** Расшифровывает payload локальным приватным ключом. */
    suspend fun decrypt(privateKey: String, encryptedPayload: EncryptedPayload): ByteArray

    /** Подписывает данные приватным ключом. */
    suspend fun sign(privateKey: String, data: ByteArray): String

    /** Проверяет подпись. */
    suspend fun verify(publicKey: String, data: ByteArray, signature: String): Boolean

    /** Генерирует криптостойкий random secret. */
    fun randomSecret(lengthBytes: Int = 32): String

    /** Считает SHA-256 и возвращает hex-строку. */
    fun sha256Hex(data: ByteArray): String
}

/** Порт внешнего relay-сервиса. */
interface RelayGatewayPort {
    /** Пересылает пакет через внешний relay. */
    suspend fun relayPacket(targetPeerId: String, envelope: PacketEnvelope): TransportDeliveryResult
}

/** Порт внешнего rendezvous-реестра. */
interface RendezvousRegistryPort {
    /** Регистрирует локальный узел. */
    suspend fun registerNode(registration: PeerRegistration)

    /** Отправляет heartbeat. */
    suspend fun heartbeat(peerId: String)

    /** Ищет данные связности удалённого peer. */
    suspend fun findPeer(peerId: String): PeerLookupResult?

    /** Удаляет локальный узел из реестра. */
    suspend fun unregisterNode(peerId: String)
}

/** Порт платформенного сетевого окружения. */
interface NetworkEnvironmentPort {
    /** Возвращает локальные IP-адреса. */
    suspend fun localAddresses(): List<String>
}

/** Порт проверки и подготовки multicast. */
interface MulticastSupportPort {
    /** Возвращает `true`, если multicast доступен. */
    suspend fun isMulticastSupported(): Boolean

    /** Выполняет подготовку перед join multicast. */
    suspend fun prepareForMulticast()
}

/** Порт хранения журнала событий. */
interface EventLogRepositoryPort {
    /** Добавляет запись event log. */
    suspend fun append(entry: EventLogEntry): EventLogEntry

    /** Возвращает последние записи event log. */
    suspend fun listRecent(limit: Int = 100): List<EventLogEntry>
}
