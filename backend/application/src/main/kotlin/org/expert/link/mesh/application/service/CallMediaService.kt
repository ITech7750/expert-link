package org.expert.link.mesh.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.expert.link.mesh.domain.model.call.CallMediaState
import org.expert.link.mesh.domain.model.call.CallMediaStats
import org.expert.link.mesh.domain.model.call.CallScope
import org.expert.link.mesh.domain.model.call.CallSession
import org.expert.link.mesh.domain.model.call.CallSignal
import org.expert.link.mesh.domain.model.call.CallSignalType
import org.expert.link.mesh.domain.model.call.CallType
import org.expert.link.mesh.domain.model.call.IceCandidate
import org.expert.link.mesh.domain.model.call.SdpType
import org.expert.link.mesh.domain.model.call.SessionDescription
import org.expert.link.mesh.domain.model.call.WebRtcSessionConfig
import org.expert.link.mesh.domain.model.network.CallHangup
import org.expert.link.mesh.domain.model.network.CallInvite
import org.expert.link.mesh.domain.model.network.CallSignalPayload
import org.expert.link.mesh.domain.port.external.MediaEnginePort

/**
 * Оркестратор media-подсистемы звонков.
 *
 * Сервис связывает signaling жизненный цикл с WebRTC-сессией:
 * - создаёт и закрывает media-сессии;
 * - прокидывает SDP/ICE между signaling и media-engine;
 * - синхронизирует mute/camera изменения между узлами.
 */
class CallMediaService(
    private val localProfileService: LocalProfileService,
    private val callSignalingService: CallSignalingService,
    private val mediaEnginePort: MediaEnginePort,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
) {
    private val logger = KotlinLogging.logger {}
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val signalJobs = ConcurrentHashMap<String, Job>()
    private val sessionTargets = ConcurrentHashMap<String, Set<String>>()

    /** Возвращает `true`, если платформа предоставляет реальный media-engine. */
    val mediaSupported: Boolean
        get() = mediaEnginePort.isSupported

    /** Запускает исходящий 1:1 звонок и инициирует WebRTC offer/ICE. */
    suspend fun startDirectCall(
        recipientPeerId: String,
        conversationId: String? = null,
        preferredOffer: String? = null,
        callType: CallType,
    ): CallSession {
        val inviteOffer = if (mediaEnginePort.isSupported) "" else preferredOffer.orEmpty()
        val session = callSignalingService.startDirectCall(
            recipientPeerId = recipientPeerId,
            conversationId = conversationId,
            offer = inviteOffer,
            callType = callType,
        )
        sessionTargets[session.callId] = targetsFromSession(session)
        if (mediaEnginePort.isSupported) {
            ensureSession(session)?.let { webRtcSession ->
                runCatching {
                    val offer = webRtcSession.createOffer()
                    sendSessionDescriptionSignal(session, offer, CallSignalType.SDP_OFFER)
                    nodeMetricsService.increment("call.media.offer.sent")
                }.onFailure { error ->
                    logger.warn(error) { "Failed to create/send SDP offer for call ${session.callId}" }
                }
            }
        }
        return session
    }

    /** Запускает исходящий групповой звонок и инициирует WebRTC offer/ICE. */
    suspend fun startGroupCall(
        targetPeerIds: Set<String>,
        conversationId: String? = null,
        preferredOffer: String? = null,
        callType: CallType,
        roomTitle: String? = null,
    ): CallSession {
        val inviteOffer = if (mediaEnginePort.isSupported) "" else preferredOffer.orEmpty()
        val session = callSignalingService.startGroupCall(
            targetPeerIds = targetPeerIds,
            conversationId = conversationId,
            offer = inviteOffer,
            callType = callType,
            roomTitle = roomTitle,
        )
        sessionTargets[session.callId] = targetsFromSession(session)
        if (mediaEnginePort.isSupported) {
            ensureSession(session)?.let { webRtcSession ->
                runCatching {
                    val offer = webRtcSession.createOffer()
                    sendSessionDescriptionSignal(session, offer, CallSignalType.SDP_OFFER)
                    nodeMetricsService.increment("call.media.offer.sent")
                }.onFailure { error ->
                    logger.warn(error) { "Failed to create/send group SDP offer for call ${session.callId}" }
                }
            }
        }
        return session
    }

    /** Обрабатывает входящее приглашение и подготавливает media-сессию. */
    suspend fun handleInvite(payload: CallInvite): CallSession {
        val session = callSignalingService.handleInvite(payload)
        sessionTargets[session.callId] = targetsFromSession(session)
        if (mediaEnginePort.isSupported) {
            ensureSession(session)?.let { webRtcSession ->
                if (payload.offer.isNotBlank()) {
                    runCatching {
                        webRtcSession.setRemoteDescription(decodeSessionDescription(payload.offer, SdpType.OFFER))
                    }.onFailure { error ->
                        logger.warn(error) { "Failed to apply invite offer for call ${payload.callId}" }
                    }
                }
            }
        }
        return session
    }

    /** Принимает звонок и отправляет signaling с SDP answer. */
    suspend fun accept(callId: String, recipientPeerId: String, preferredAnswer: String? = null): CallSignal {
        val answerPayload = resolveAnswerPayload(callId, preferredAnswer)
        return callSignalingService.accept(callId, recipientPeerId, answerPayload)
    }

    /** Отклоняет звонок и освобождает media-сессию. */
    suspend fun reject(callId: String, recipientPeerId: String, reason: String): CallSignal {
        val signal = callSignalingService.reject(callId, recipientPeerId, reason)
        closeMediaSession(callId)
        return signal
    }

    /** Подключается к групповому звонку и отправляет signaling с SDP answer. */
    suspend fun join(callId: String, recipientPeerId: String, preferredAnswer: String? = null): CallSignal {
        val answerPayload = resolveAnswerPayload(callId, preferredAnswer)
        return callSignalingService.join(callId, recipientPeerId, answerPayload)
    }

    /** Выходит из звонка и закрывает media-сессию для локального узла. */
    suspend fun leave(callId: String, recipientPeerId: String, reason: String): CallSignal {
        val signal = callSignalingService.leave(callId, recipientPeerId, reason)
        closeMediaSession(callId)
        return signal
    }

    /** Завершает звонок и закрывает media-сессию. */
    suspend fun end(callId: String, reason: String): CallSession? {
        val ended = callSignalingService.end(callId, reason)
        closeMediaSession(callId)
        return ended
    }

    /** Обрабатывает входящий signaling пакет и применяет его к WebRTC-сессии. */
    suspend fun handleSignal(payload: CallSignalPayload): CallSession? {
        val updated = callSignalingService.handleSignal(payload) ?: return null
        if (!mediaEnginePort.isSupported) {
            return updated
        }
        val signal = payload.signal
        val mediaSession = ensureSession(updated) ?: return updated
        when (signal.signalType) {
            CallSignalType.SDP_OFFER -> {
                runCatching {
                    mediaSession.setRemoteDescription(decodeSessionDescription(signal.payload, SdpType.OFFER))
                }.onFailure { error -> logger.warn(error) { "Failed to apply remote offer for ${signal.callId}" } }
            }
            CallSignalType.SDP_ANSWER -> {
                runCatching {
                    mediaSession.setRemoteDescription(decodeSessionDescription(signal.payload, SdpType.ANSWER))
                }.onFailure { error -> logger.warn(error) { "Failed to apply remote answer for ${signal.callId}" } }
            }
            CallSignalType.ACCEPT,
            CallSignalType.JOIN,
            -> {
                if (signal.payload.isNotBlank()) {
                    runCatching {
                        mediaSession.setRemoteDescription(decodeSessionDescription(signal.payload, SdpType.ANSWER))
                    }.onFailure { error -> logger.warn(error) { "Failed to apply accept/join answer for ${signal.callId}" } }
                }
            }
            CallSignalType.ICE_CANDIDATE -> {
                if (signal.payload.isNotBlank()) {
                    runCatching {
                        mediaSession.addIceCandidate(decodeIceCandidate(signal.payload))
                    }.onFailure { error -> logger.warn(error) { "Failed to apply ICE candidate for ${signal.callId}" } }
                }
            }
            CallSignalType.REJECT,
            CallSignalType.HANGUP,
            -> closeMediaSession(signal.callId)
            CallSignalType.LEAVE -> {
                if (updated.callScope == CallScope.DIRECT) {
                    closeMediaSession(signal.callId)
                }
            }
            else -> Unit
        }
        return updated
    }

    /** Обрабатывает входящий hangup и закрывает media-сессию. */
    suspend fun handleHangup(payload: CallHangup): CallSession? {
        val ended = callSignalingService.handleHangup(payload)
        closeMediaSession(payload.callId)
        return ended
    }

    /** Включает/выключает микрофон и рассылает состояние участникам звонка. */
    suspend fun toggleMicrophone(callId: String, enabled: Boolean): CallMediaState? {
        val session = mediaEnginePort.findSession(callId) ?: return null
        session.setMicrophoneEnabled(enabled)
        notifyPeers(
            callId = callId,
            signalType = CallSignalType.MUTE_CHANGED,
            payload = enabled.toString(),
            muted = !enabled,
            videoEnabled = null,
        )
        return session.currentState()
    }

    /** Включает/выключает камеру и рассылает состояние участникам звонка. */
    suspend fun toggleCamera(callId: String, enabled: Boolean): CallMediaState? {
        val session = mediaEnginePort.findSession(callId) ?: return null
        session.setCameraEnabled(enabled)
        notifyPeers(
            callId = callId,
            signalType = CallSignalType.VIDEO_CHANGED,
            payload = enabled.toString(),
            muted = null,
            videoEnabled = enabled,
        )
        return session.currentState()
    }

    /** Переключает локальную камеру и возвращает новый снимок media-состояния. */
    suspend fun switchCamera(callId: String): CallMediaState? {
        val session = mediaEnginePort.findSession(callId) ?: return null
        session.switchCamera()
        return session.currentState()
    }

    /** Возвращает текущий media-state звонка. */
    suspend fun mediaState(callId: String): CallMediaState? = mediaEnginePort.findSession(callId)?.currentState()

    /** Возвращает текущий media-stats звонка. */
    suspend fun mediaStats(callId: String): CallMediaStats? = mediaEnginePort.findSession(callId)?.currentStats()

    /** Освобождает ресурсы media-сервисов при остановке узла. */
    suspend fun shutdown() {
        signalJobs.values.forEach { it.cancel() }
        signalJobs.clear()
        val callIds = mediaEnginePort.listSessions().map { it.callId }
        callIds.forEach { callId -> mediaEnginePort.closeSession(callId) }
        sessionTargets.clear()
        scope.cancel()
    }

    private suspend fun resolveAnswerPayload(callId: String, preferredAnswer: String?): String {
        val explicit = preferredAnswer?.takeIf { it.isNotBlank() }
        if (explicit != null || !mediaEnginePort.isSupported) {
            return explicit.orEmpty()
        }
        val signaling = callSignalingService.session(callId) ?: return ""
        val session = ensureSession(signaling) ?: return ""
        return runCatching {
            encodeSessionDescription(session.createAnswer())
        }.getOrElse { error ->
            logger.warn(error) { "Failed to create local answer for call $callId" }
            ""
        }
    }

    private suspend fun ensureSession(session: CallSession): org.expert.link.mesh.domain.port.external.WebRtcSessionPort? {
        if (!mediaEnginePort.isSupported) return null
        val opened = mediaEnginePort.openSession(
            WebRtcSessionConfig(
                callId = session.callId,
                localPeerId = localProfileService.require().peerId,
                remotePeerIds = targetsFromSession(session),
                callType = session.callType,
                callScope = session.callScope,
            ),
        )
        startSignalPumpIfNeeded(session, opened)
        return opened
    }

    private suspend fun startSignalPumpIfNeeded(
        session: CallSession,
        webRtcSession: org.expert.link.mesh.domain.port.external.WebRtcSessionPort,
    ) {
        if (signalJobs.containsKey(session.callId)) return
        val job = scope.launch {
            webRtcSession.signalEvents().collect { signalEvent ->
                val payload = when (signalEvent.signalType) {
                    CallSignalType.SDP_OFFER,
                    CallSignalType.SDP_ANSWER,
                    -> signalEvent.description?.let(::encodeSessionDescription)
                    CallSignalType.ICE_CANDIDATE -> signalEvent.iceCandidate?.let(::encodeIceCandidate)
                    else -> null
                } ?: return@collect
                resolveTargets(signalEvent.callId).forEach { targetPeerId ->
                    runCatching {
                        callSignalingService.sendSignal(
                            callId = signalEvent.callId,
                            recipientPeerId = targetPeerId,
                            signalType = signalEvent.signalType,
                            payload = payload,
                        )
                    }.onFailure { error ->
                        logger.warn(error) { "Failed to forward media signal ${signalEvent.signalType} for call ${signalEvent.callId}" }
                    }
                }
                nodeMetricsService.increment("call.media.signal.forwarded")
            }
        }
        signalJobs[session.callId] = job
    }

    private suspend fun sendSessionDescriptionSignal(session: CallSession, description: SessionDescription, signalType: CallSignalType) {
        val payload = encodeSessionDescription(description)
        targetsFromSession(session).forEach { targetPeerId ->
            callSignalingService.sendSignal(
                callId = session.callId,
                recipientPeerId = targetPeerId,
                signalType = signalType,
                payload = payload,
            )
        }
    }

    private suspend fun notifyPeers(
        callId: String,
        signalType: CallSignalType,
        payload: String,
        muted: Boolean?,
        videoEnabled: Boolean?,
    ) {
        val peers = resolveTargets(callId)
        peers.forEach { targetPeerId ->
            runCatching {
                callSignalingService.sendSignal(
                    callId = callId,
                    recipientPeerId = targetPeerId,
                    signalType = signalType,
                    payload = payload,
                    muted = muted,
                    videoEnabled = videoEnabled,
                )
            }.onFailure { error -> logger.warn(error) { "Failed to notify peer $targetPeerId about $signalType" } }
        }
    }

    private suspend fun resolveTargets(callId: String): Set<String> {
        val explicit = sessionTargets[callId]
        if (!explicit.isNullOrEmpty()) {
            return explicit
        }
        val session = callSignalingService.session(callId)
        return if (session == null) emptySet() else targetsFromSession(session)
    }

    private suspend fun closeMediaSession(callId: String) {
        signalJobs.remove(callId)?.cancel()
        sessionTargets.remove(callId)
        runCatching { mediaEnginePort.closeSession(callId) }
            .onFailure { error -> logger.warn(error) { "Failed to close media session $callId" } }
    }

    private suspend fun targetsFromSession(session: CallSession): Set<String> {
        val localPeerId = runCatching { localProfileService.require().peerId }.getOrNull()
        val base = buildSet {
            addAll(session.targetPeerIds)
            add(session.recipientPeerId)
            add(session.initiatorPeerId)
        }
        if (localPeerId == null) {
            return base
        }
        val filtered = base.filterNot { it == localPeerId }.toSet()
        if (filtered.isNotEmpty()) {
            return filtered
        }
        // Фолбек для входящего direct звонка, где targetPeerIds может содержать только локальный peer.
        return if (session.callScope == CallScope.DIRECT) {
            setOf(if (session.initiatorPeerId == localPeerId) session.recipientPeerId else session.initiatorPeerId)
                .filterNot { it == localPeerId }
                .toSet()
        } else {
            emptySet()
        }
    }

    private fun encodeSessionDescription(description: SessionDescription): String {
        return json.encodeToString(SessionDescription.serializer(), description)
    }

    private fun encodeIceCandidate(candidate: IceCandidate): String {
        return json.encodeToString(IceCandidate.serializer(), candidate)
    }

    private fun decodeSessionDescription(payload: String, fallbackType: SdpType): SessionDescription {
        return runCatching {
            json.decodeFromString(SessionDescription.serializer(), payload)
        }.getOrElse {
            SessionDescription(type = fallbackType, sdp = payload)
        }
    }

    private fun decodeIceCandidate(payload: String): IceCandidate {
        return runCatching {
            json.decodeFromString(IceCandidate.serializer(), payload)
        }.getOrElse {
            IceCandidate(sdpMid = null, sdpMLineIndex = 0, candidate = payload)
        }
    }
}
