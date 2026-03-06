package org.expert.link.mesh.contract.api

import kotlinx.datetime.Instant
import org.expert.link.mesh.contract.model.MeshBlockedPeer
import org.expert.link.mesh.contract.model.MeshCallSession
import org.expert.link.mesh.contract.model.MeshCallSignal
import org.expert.link.mesh.contract.model.MeshCallSignalType
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshConversation
import org.expert.link.mesh.contract.model.MeshEventLogEntry
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshLocalProfile
import org.expert.link.mesh.contract.model.MeshMetricSnapshot
import org.expert.link.mesh.contract.model.MeshMessageReceipt
import org.expert.link.mesh.contract.model.MeshNearbyPeer
import org.expert.link.mesh.contract.model.MeshPairingSession
import org.expert.link.mesh.contract.model.MeshPairedPeer
import org.expert.link.mesh.contract.model.MeshPeerEndpoint
import org.expert.link.mesh.contract.model.MeshRelayStatus
import org.expert.link.mesh.contract.model.MeshRouteInfo
import org.expert.link.mesh.contract.model.MeshRoutingPlan

/**
 * Единый публичный контракт встроенного mesh-узла для мобильного приложения.
 *
 * Через этот интерфейс приложение работает с жизненным циклом узла, pairing, чатом, файлами,
 * звонками, поиском узлов и диагностикой. Внутренние слои backend сюда не протекают.
 */
interface MeshNode {
    /** Публичный профиль локального узла. */
    val profile: MeshLocalProfile

    /** Текущий входной endpoint узла. */
    val endpoint: MeshPeerEndpoint

    /** Останавливает узел и освобождает ресурсы. */
    suspend fun stop()

    /** Создаёт короткоживущий invite для pairing. */
    suspend fun createPairingInvite(validitySeconds: Long = 300): String

    /** Принимает invite и запускает pairing. */
    suspend fun pairWithInvite(encodedInvite: String): MeshPairingSession

    /** Возвращает список известных peers. */
    suspend fun peers(): List<MeshPairedPeer>

    /** Возвращает активные pairing-сессии. */
    suspend fun pairingSessions(): List<MeshPairingSession>

    /** Блокирует peer. */
    suspend fun blockPeer(peerId: String, reason: String, expiresAt: Instant? = null): MeshBlockedPeer

    /** Убирает peer из block list. */
    suspend fun unblockPeer(peerId: String)

    /** Возвращает block list. */
    suspend fun blockedPeers(): List<MeshBlockedPeer>

    /** Запускает поиск peer через discovery. */
    suspend fun discoverPeer(peerId: String)

    /** Публикует явное объявление о присутствии узла. */
    suspend fun announcePresence()

    /** Возвращает найденные рядом узлы. */
    suspend fun nearbyPeers(): List<MeshNearbyPeer>

    /** Возвращает известные маршруты. */
    suspend fun routes(): List<MeshRouteInfo>

    /** Строит текущий план доставки до узла. */
    suspend fun routingPlan(peerId: String): MeshRoutingPlan

    /** Возвращает состояние relay/rendezvous режима. */
    fun relayStatus(): MeshRelayStatus

    /** Добавляет ручной endpoint hint для peer. */
    suspend fun rememberPeerEndpoint(peerId: String, endpoint: MeshPeerEndpoint)

    /** Удаляет cached route и endpoint для peer. */
    suspend fun forgetPeerEndpoint(peerId: String)

    /** Открывает существующий или создаёт новый диалог. */
    suspend fun openConversation(peerId: String): MeshConversation

    /** Возвращает диалоги. */
    suspend fun conversations(): List<MeshConversation>

    /** Возвращает сообщения диалога. */
    suspend fun messages(conversationId: String): List<MeshChatMessage>

    /** Возвращает последние подтверждения доставки. */
    suspend fun messageReceipts(limit: Int = 100): List<MeshMessageReceipt>

    /** Отправляет чат-сообщение доверенному peer. */
    suspend fun sendChat(command: MeshChatCommand): MeshChatMessage

    /** Возвращает файловые сессии. */
    suspend fun fileTransfers(): List<MeshFileTransferSession>

    /** Запускает отправку файла. */
    suspend fun sendFile(command: MeshFileTransferCommand): MeshFileTransferSession

    /** Запрашивает повтор недостающих чанков. */
    suspend fun resumeFileTransfer(transferId: String): MeshFileTransferSession?

    /** Отменяет передачу файла. */
    suspend fun cancelFileTransfer(transferId: String): MeshFileTransferSession?

    /** Возвращает call-сессии. */
    suspend fun callSessions(): List<MeshCallSession>

    /** Запускает signaling звонка. */
    suspend fun startCall(command: MeshStartCallCommand): MeshCallSession

    /** Отправляет signaling payload звонка. */
    suspend fun sendCallSignal(command: MeshCallSignalCommand): MeshCallSignal

    /** Завершает звонок. */
    suspend fun hangupCall(command: MeshHangupCallCommand): MeshCallSession?

    /** Возвращает последние события. */
    suspend fun recentEvents(limit: Int = 100): List<MeshEventLogEntry>

    /** Возвращает текущий snapshot метрик. */
    suspend fun metrics(): MeshMetricSnapshot
}

/** Команда на отправку чат-сообщения. */
data class MeshChatCommand(
    val targetPeerId: String,
    val body: String,
    val conversationId: String? = null,
)

/** Команда на отправку файла. */
data class MeshFileTransferCommand(
    val targetPeerId: String,
    val path: String,
    val conversationId: String? = null,
)

/** Команда на запуск звонка. */
data class MeshStartCallCommand(
    val targetPeerId: String,
    val offer: String,
    val conversationId: String? = null,
)

/** Команда на отправку сигнала звонка. */
data class MeshCallSignalCommand(
    val callId: String,
    val recipientPeerId: String,
    val signalType: MeshCallSignalType,
    val payload: String,
)

/** Команда на завершение звонка. */
data class MeshHangupCallCommand(
    val callId: String,
    val recipientPeerId: String,
    val reason: String,
)
