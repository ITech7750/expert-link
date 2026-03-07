package org.expert.link.mesh.domain.port.external

import kotlinx.coroutines.flow.Flow
import org.expert.link.mesh.domain.model.call.CallMediaState
import org.expert.link.mesh.domain.model.call.CallMediaStats
import org.expert.link.mesh.domain.model.call.CameraFacing
import org.expert.link.mesh.domain.model.call.IceCandidate
import org.expert.link.mesh.domain.model.call.SessionDescription
import org.expert.link.mesh.domain.model.call.WebRtcSessionConfig
import org.expert.link.mesh.domain.model.call.WebRtcSignalEvent

/** Порт media-engine (WebRTC) для backend call orchestration. */
interface MediaEnginePort {
    /** Флаг доступности реального media-движка на платформе. */
    val isSupported: Boolean

    /** Открывает или возвращает существующую сессию звонка. */
    suspend fun openSession(config: WebRtcSessionConfig): WebRtcSessionPort

    /** Возвращает сессию звонка по id. */
    suspend fun findSession(callId: String): WebRtcSessionPort?

    /** Возвращает все открытые сессии. */
    suspend fun listSessions(): List<WebRtcSessionPort>

    /** Закрывает сессию звонка. */
    suspend fun closeSession(callId: String)
}

/** Порт управления локальным аудио. */
interface AudioCapturePort {
    /** Включает или выключает микрофон. */
    suspend fun setMicrophoneEnabled(enabled: Boolean)

    /** Возвращает текущее состояние микрофона. */
    suspend fun isMicrophoneEnabled(): Boolean
}

/** Порт управления локальным видео. */
interface VideoCapturePort {
    /** Включает или выключает камеру. */
    suspend fun setCameraEnabled(enabled: Boolean)

    /** Возвращает текущее состояние камеры. */
    suspend fun isCameraEnabled(): Boolean

    /** Переключает активную камеру. */
    suspend fun switchCamera()

    /** Возвращает текущее направление камеры. */
    suspend fun cameraFacing(): CameraFacing
}

/** Порт привязки рендереров локального и удалённых потоков. */
interface MediaRendererPort {
    /** Привязывает локальный рендерер. */
    suspend fun attachLocalRenderer(rendererId: String)

    /** Привязывает рендерер удалённого участника. */
    suspend fun attachRemoteRenderer(peerId: String, rendererId: String)

    /** Отвязывает рендерер. */
    suspend fun detachRenderer(rendererId: String)
}

/** Порт WebRTC-сессии для одного callId. */
interface WebRtcSessionPort : AudioCapturePort, VideoCapturePort, MediaRendererPort {
    /** Идентификатор звонка. */
    val callId: String

    /** Локальный peerId. */
    val localPeerId: String

    /** Удалённые peerId сессии. */
    val remotePeerIds: Set<String>

    /** Создаёт локальный SDP offer. */
    suspend fun createOffer(): SessionDescription

    /** Создаёт локальный SDP answer. */
    suspend fun createAnswer(): SessionDescription

    /** Применяет удалённое SDP-описание. */
    suspend fun setRemoteDescription(description: SessionDescription)

    /** Добавляет удалённый ICE-кандидат. */
    suspend fun addIceCandidate(candidate: IceCandidate)

    /** Возвращает текущий снимок media-состояния. */
    suspend fun currentState(): CallMediaState

    /** Возвращает текущий снимок media-метрик. */
    suspend fun currentStats(): CallMediaStats?

    /** Поток исходящих сигнальных событий (SDP/ICE). */
    fun signalEvents(): Flow<WebRtcSignalEvent>

    /** Поток изменений media-состояния. */
    fun stateUpdates(): Flow<CallMediaState>

    /** Поток изменений media-метрик. */
    fun statsUpdates(): Flow<CallMediaStats>

    /** Закрывает media-сессию. */
    suspend fun close()
}
