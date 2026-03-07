package org.expert.link.mesh.contract.api

import kotlinx.datetime.Instant
import org.expert.link.mesh.contract.model.MeshBlockedPeer
import org.expert.link.mesh.contract.model.MeshCallSession
import org.expert.link.mesh.contract.model.MeshCallSignal
import org.expert.link.mesh.contract.model.MeshCallSignalType
import org.expert.link.mesh.contract.model.MeshCallParticipant
import org.expert.link.mesh.contract.model.MeshCallEvent
import org.expert.link.mesh.contract.model.MeshCallMediaState
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshChatMember
import org.expert.link.mesh.contract.model.MeshChatSummary
import org.expert.link.mesh.contract.model.MeshConversation
import org.expert.link.mesh.contract.model.MeshEventLogEntry
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshGroupChat
import org.expert.link.mesh.contract.model.MeshGroupEvent
import org.expert.link.mesh.contract.model.MeshLocalProfile
import org.expert.link.mesh.contract.model.MeshMetricSnapshot
import org.expert.link.mesh.contract.model.MeshMessageReceipt
import org.expert.link.mesh.contract.model.MeshNearbyPeer
import org.expert.link.mesh.contract.model.MeshPairingSession
import org.expert.link.mesh.contract.model.MeshPairedPeer
import org.expert.link.mesh.contract.model.MeshPeerEndpoint
import org.expert.link.mesh.contract.model.MeshRelayStatus
import org.expert.link.mesh.contract.model.MeshRelayMode
import org.expert.link.mesh.contract.model.MeshRouteInfo
import org.expert.link.mesh.contract.model.MeshRouteHealth
import org.expert.link.mesh.contract.model.MeshRoutingPlan
import org.expert.link.mesh.contract.model.MeshConnectivityStrategy
import org.expert.link.mesh.contract.model.MeshNetworkRoleState
import org.expert.link.mesh.contract.model.MeshMediaStats
import org.expert.link.mesh.contract.model.MeshTopologyState
import org.expert.link.mesh.contract.model.MeshThread
import org.expert.link.mesh.contract.model.MeshThreadMessage
import org.expert.link.mesh.contract.model.MeshThreadSummary
import org.expert.link.mesh.contract.model.MeshMessageType

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

    /** Возвращает текущее состояние relay mode в топологии. */
    suspend fun relayModeState(): MeshRelayMode

    /** Возвращает текущий topology snapshot. */
    suspend fun observeTopologyState(): MeshTopologyState

    /** Возвращает состояние роли локального узла и выбранного хоста. */
    suspend fun observeHostRole(): MeshNetworkRoleState

    /** Возвращает стратегию связности для выбранного peer. */
    suspend fun observeConnectivityStrategy(peerId: String): MeshConnectivityStrategy

    /** Возвращает снимок здоровья всех известных маршрутов. */
    suspend fun inspectRouteHealth(): List<MeshRouteHealth>

    /** Принудительно обновляет topology snapshot. */
    suspend fun forceTopologyRefresh(): MeshTopologyState

    /** Добавляет ручной endpoint hint для peer. */
    suspend fun rememberPeerEndpoint(peerId: String, endpoint: MeshPeerEndpoint)

    /** Удаляет cached route и endpoint для peer. */
    suspend fun forgetPeerEndpoint(peerId: String)

    /** Открывает существующий или создаёт новый диалог. */
    suspend fun openConversation(peerId: String): MeshConversation

    /** Создаёт direct чат с участником. */
    suspend fun createDirectChat(peerId: String): MeshConversation

    /** Создаёт групповой чат. */
    suspend fun createGroupChat(command: MeshCreateGroupChatCommand): MeshConversation

    /** Переименовывает чат. */
    suspend fun renameChat(chatId: String, title: String): MeshConversation?

    /** Добавляет участников в чат. */
    suspend fun addParticipants(chatId: String, participants: List<MeshChatMemberCommand>): MeshConversation?

    /** Удаляет участника из чата. */
    suspend fun removeParticipant(chatId: String, peerId: String): MeshConversation?

    /** Возвращает только групповые чаты в нормализованном виде. */
    suspend fun groupChats(): List<MeshGroupChat>

    /** Возвращает системные события группы. */
    suspend fun groupEvents(chatId: String, limit: Int = 100): List<MeshGroupEvent>

    /** Возвращает сводки чатов для списка. */
    suspend fun chatSummaries(): List<MeshChatSummary>

    /** Возвращает диалоги. */
    suspend fun conversations(): List<MeshConversation>

    /** Возвращает историю сообщений группового чата. */
    suspend fun groupMessages(chatId: String): List<MeshChatMessage>

    /** Возвращает сообщения диалога. */
    suspend fun messages(conversationId: String): List<MeshChatMessage>

    /** Возвращает последние подтверждения доставки. */
    suspend fun messageReceipts(limit: Int = 100): List<MeshMessageReceipt>

    /** Отправляет чат-сообщение доверенному peer. */
    suspend fun sendChat(command: MeshChatCommand): MeshChatMessage

    /** Отправляет сообщение в чат по chatId. */
    suspend fun sendMessage(command: MeshSendMessageCommand): MeshChatMessage

    /** Создаёт тред для root-сообщения. */
    suspend fun createThread(command: MeshCreateThreadCommand): MeshThread

    /** Возвращает метаданные треда по root-сообщению. */
    suspend fun thread(chatId: String, rootMessageId: String): MeshThread?

    /** Возвращает обновления тредов в чате в polling-формате. */
    suspend fun threadUpdates(chatId: String): List<MeshThreadSummary>

    /** Возвращает сообщения треда в специализированной модели. */
    suspend fun threadMessagesDetailed(chatId: String, rootMessageId: String): List<MeshThreadMessage>

    /** Отправляет reply в тред и возвращает специализированную модель. */
    suspend fun sendThreadReply(command: MeshSendThreadMessageCommand): MeshThreadMessage

    /** Возвращает сообщения треда. */
    suspend fun threadMessages(chatId: String, rootMessageId: String): List<MeshChatMessage>

    /** Отправляет сообщение в тред. */
    suspend fun sendThreadMessage(command: MeshSendThreadMessageCommand): MeshChatMessage

    /** Возвращает сводку треда. */
    suspend fun threadSummary(chatId: String, rootMessageId: String): MeshThreadSummary?

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

    /** Запускает исходящий 1:1 аудиозвонок. */
    suspend fun startAudioCall(command: MeshStartCallCommand): MeshCallSession

    /** Запускает исходящий 1:1 видеозвонок. */
    suspend fun startVideoCall(command: MeshStartCallCommand): MeshCallSession

    /** Запускает исходящий групповой аудиозвонок. */
    suspend fun startGroupAudioCall(command: MeshStartGroupCallCommand): MeshCallSession

    /** Запускает исходящий групповой видеозвонок. */
    suspend fun startGroupVideoCall(command: MeshStartGroupCallCommand): MeshCallSession

    /** Принимает входящий звонок. */
    suspend fun acceptCall(command: MeshAcceptCallCommand): MeshCallSignal

    /** Отклоняет входящий звонок. */
    suspend fun rejectCall(command: MeshRejectCallCommand): MeshCallSignal

    /** Подключается к групповому звонку. */
    suspend fun joinCall(command: MeshJoinCallCommand): MeshCallSignal

    /** Выходит из группового звонка. */
    suspend fun leaveCall(command: MeshLeaveCallCommand): MeshCallSignal

    /** Завершает звонок для локального узла/комнаты. */
    suspend fun endCall(command: MeshEndCallCommand): MeshCallSession?

    /** Возвращает активные звонки (polling snapshot). */
    suspend fun observeActiveCall(): List<MeshCallSession>

    /** Возвращает входящие звонки (polling snapshot). */
    suspend fun observeIncomingCalls(): List<MeshCallSession>

    /** Возвращает участников звонка. */
    suspend fun observeCallParticipants(callId: String): List<MeshCallParticipant>

    /** Возвращает историю событий звонка. */
    suspend fun observeCallEvents(callId: String, limit: Int = 200): List<MeshCallEvent>

    /** Включает или выключает микрофон в звонке. */
    suspend fun toggleMicrophone(command: MeshToggleMicrophoneCommand): MeshCallMediaState?

    /** Включает или выключает камеру в звонке. */
    suspend fun toggleCamera(command: MeshToggleCameraCommand): MeshCallMediaState?

    /** Переключает активную камеру. */
    suspend fun switchCamera(callId: String): MeshCallMediaState?

    /** Возвращает текущий media-снимок звонка. */
    suspend fun observeMediaState(callId: String): MeshCallMediaState?

    /** Возвращает текущий media-снимок метрик звонка. */
    suspend fun observeMediaStats(callId: String): MeshMediaStats?

    /** Запускает signaling звонка. */
    @Deprecated("Используйте startAudioCall/startVideoCall")
    suspend fun startCall(command: MeshStartCallCommand): MeshCallSession

    /** Отправляет signaling payload звонка. */
    @Deprecated("Используйте acceptCall/rejectCall/joinCall/leaveCall")
    suspend fun sendCallSignal(command: MeshCallSignalCommand): MeshCallSignal

    /** Завершает звонок. */
    @Deprecated("Используйте endCall")
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

/** Команда создания группового чата. */
data class MeshCreateGroupChatCommand(
    val title: String,
    val description: String? = null,
    val participantPeerIds: Set<String>,
)

/** Команда участника для изменения состава чата. */
data class MeshChatMemberCommand(
    val peerId: String,
    val displayName: String,
)

/** Команда отправки сообщения в чат. */
data class MeshSendMessageCommand(
    val chatId: String,
    val body: String,
    val messageType: MeshMessageType = MeshMessageType.TEXT,
)

/** Команда создания треда. */
data class MeshCreateThreadCommand(
    val chatId: String,
    val rootMessageId: String,
)

/** Команда отправки сообщения в тред. */
data class MeshSendThreadMessageCommand(
    val chatId: String,
    val rootMessageId: String,
    val body: String,
    val parentMessageId: String? = null,
)

/** Команда на отправку файла.
 *
 * `path` считается платформенным file handle. На JVM это может быть обычный путь,
 * на Android или iOS — URI, sandbox path или иной локальный идентификатор ресурса.
 */
data class MeshFileTransferCommand(
    val targetPeerId: String,
    val path: String,
    val conversationId: String? = null,
)

/** Команда на запуск звонка. */
data class MeshStartCallCommand(
    val targetPeerId: String,
    val offer: String? = null,
    val conversationId: String? = null,
)

/** Команда на запуск группового звонка. */
data class MeshStartGroupCallCommand(
    val targetPeerIds: Set<String>,
    val offer: String? = null,
    val conversationId: String? = null,
    val roomTitle: String? = null,
)

/** Команда на принятие звонка. */
data class MeshAcceptCallCommand(
    val callId: String,
    val recipientPeerId: String,
    val answer: String? = null,
)

/** Команда на отклонение звонка. */
data class MeshRejectCallCommand(
    val callId: String,
    val recipientPeerId: String,
    val reason: String,
)

/** Команда на подключение к звонку. */
data class MeshJoinCallCommand(
    val callId: String,
    val recipientPeerId: String,
    val answer: String? = null,
)

/** Команда на выход из звонка. */
data class MeshLeaveCallCommand(
    val callId: String,
    val recipientPeerId: String,
    val reason: String = "left",
)

/** Команда на завершение звонка. */
data class MeshEndCallCommand(
    val callId: String,
    val reason: String = "ended",
)

/** Команда переключения микрофона. */
data class MeshToggleMicrophoneCommand(
    val callId: String,
    val enabled: Boolean,
)

/** Команда переключения камеры. */
data class MeshToggleCameraCommand(
    val callId: String,
    val enabled: Boolean,
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
