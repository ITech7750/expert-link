package org.expert.link.mesh.contract.api

import kotlinx.coroutines.flow.Flow
import org.expert.link.mesh.contract.model.MeshCallMediaState
import org.expert.link.mesh.contract.model.MeshCallScope
import org.expert.link.mesh.contract.model.MeshCallType
import org.expert.link.mesh.contract.model.MeshIceCandidate
import org.expert.link.mesh.contract.model.MeshMediaStats
import org.expert.link.mesh.contract.model.MeshSessionDescription
import org.expert.link.mesh.contract.model.MeshWebRtcSignalEvent

/** Платформенный media-engine для интеграции real WebRTC в backend-ядро. */
interface MeshMediaEngine {
    /** Флаг доступности реального media-движка на платформе. */
    val isSupported: Boolean

    /** Открывает или возвращает существующую media-сессию звонка. */
    suspend fun openSession(config: MeshMediaSessionConfig): MeshWebRtcSession

    /** Возвращает media-сессию по callId. */
    suspend fun findSession(callId: String): MeshWebRtcSession?

    /** Возвращает все открытые media-сессии. */
    suspend fun listSessions(): List<MeshWebRtcSession>

    /** Закрывает media-сессию по callId. */
    suspend fun closeSession(callId: String)
}

/** Платформенная WebRTC-сессия одного звонка. */
interface MeshWebRtcSession {
    /** Идентификатор звонка. */
    val callId: String

    /** Локальный peerId. */
    val localPeerId: String

    /** Удалённые peerId. */
    val remotePeerIds: Set<String>

    /** Создаёт локальный SDP offer. */
    suspend fun createOffer(): MeshSessionDescription

    /** Создаёт локальный SDP answer. */
    suspend fun createAnswer(): MeshSessionDescription

    /** Применяет удалённое SDP-описание. */
    suspend fun setRemoteDescription(description: MeshSessionDescription)

    /** Добавляет удалённый ICE-кандидат. */
    suspend fun addIceCandidate(candidate: MeshIceCandidate)

    /** Управляет микрофоном. */
    suspend fun setMicrophoneEnabled(enabled: Boolean)

    /** Возвращает состояние микрофона. */
    suspend fun isMicrophoneEnabled(): Boolean

    /** Управляет камерой. */
    suspend fun setCameraEnabled(enabled: Boolean)

    /** Возвращает состояние камеры. */
    suspend fun isCameraEnabled(): Boolean

    /** Переключает камеру. */
    suspend fun switchCamera()

    /** Привязывает локальный рендерер (platform handle). */
    suspend fun attachLocalRenderer(rendererId: String)

    /** Привязывает удалённый рендерер (platform handle). */
    suspend fun attachRemoteRenderer(peerId: String, rendererId: String)

    /** Отвязывает рендерер. */
    suspend fun detachRenderer(rendererId: String)

    /** Возвращает текущий media state. */
    suspend fun currentState(): MeshCallMediaState

    /** Возвращает текущий media stats snapshot. */
    suspend fun currentStats(): MeshMediaStats?

    /** Поток исходящих сигналов SDP/ICE. */
    fun signalEvents(): Flow<MeshWebRtcSignalEvent>

    /** Поток изменений media state. */
    fun stateUpdates(): Flow<MeshCallMediaState>

    /** Поток изменений media stats. */
    fun statsUpdates(): Flow<MeshMediaStats>

    /** Закрывает сессию. */
    suspend fun close()
}

/** Конфигурация media-сессии одного звонка. */
data class MeshMediaSessionConfig(
    val callId: String,
    val localPeerId: String,
    val remotePeerIds: Set<String>,
    val callType: MeshCallType,
    val callScope: MeshCallScope,
)
