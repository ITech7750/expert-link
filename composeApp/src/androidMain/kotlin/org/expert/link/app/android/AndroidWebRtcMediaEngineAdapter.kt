package org.expert.link.app.android

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import org.expert.link.mesh.contract.api.MeshMediaEngine
import org.expert.link.mesh.contract.api.MeshMediaSessionConfig
import org.expert.link.mesh.contract.api.MeshWebRtcSession
import org.expert.link.mesh.contract.model.MeshCallMediaState
import org.expert.link.mesh.contract.model.MeshCallScope
import org.expert.link.mesh.contract.model.MeshCallSignalType
import org.expert.link.mesh.contract.model.MeshCallType
import org.expert.link.mesh.contract.model.MeshCameraFacing
import org.expert.link.mesh.contract.model.MeshIceCandidate
import org.expert.link.mesh.contract.model.MeshMediaConnectionState
import org.expert.link.mesh.contract.model.MeshMediaStats
import org.expert.link.mesh.contract.model.MeshPeerMediaState
import org.expert.link.mesh.contract.model.MeshSdpType
import org.expert.link.mesh.contract.model.MeshSessionDescription
import org.expert.link.mesh.contract.model.MeshWebRtcSignalEvent
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * Реальная Android интеграция WebRTC media-engine.
 *
 * Используется backend-ядром как platform media backend для call subsystem.
 */
class AndroidWebRtcMediaEngineAdapter(
    context: Context,
) : MeshMediaEngine {
    private val appContext = context.applicationContext
    private val eglBase: EglBase = EglBase.create()
    private val factory: PeerConnectionFactory
    private val sessions = ConcurrentHashMap<String, AndroidWebRtcSession>()

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions(),
        )
        val audioDeviceModule = JavaAudioDeviceModule.builder(appContext).createAudioDeviceModule()
        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
        audioDeviceModule.release()
    }

    override val isSupported: Boolean = true

    override suspend fun openSession(config: MeshMediaSessionConfig): MeshWebRtcSession {
        return sessions.computeIfAbsent(config.callId) {
            AndroidWebRtcSession(
                context = appContext,
                factory = factory,
                eglBase = eglBase,
                config = config,
            )
        }
    }

    override suspend fun findSession(callId: String): MeshWebRtcSession? = sessions[callId]

    override suspend fun listSessions(): List<MeshWebRtcSession> = sessions.values.toList()

    override suspend fun closeSession(callId: String) {
        sessions.remove(callId)?.close()
    }
}

private class AndroidWebRtcSession(
    private val context: Context,
    private val factory: PeerConnectionFactory,
    private val eglBase: EglBase,
    private val config: MeshMediaSessionConfig,
) : MeshWebRtcSession {
    override val callId: String = config.callId
    override val localPeerId: String = config.localPeerId
    override val remotePeerIds: Set<String> = config.remotePeerIds

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val signalFlow = MutableSharedFlow<MeshWebRtcSignalEvent>(extraBufferCapacity = 64)
    private val stateFlow = MutableStateFlow(initialState())
    private val statsFlow = MutableSharedFlow<MeshMediaStats>(
        replay = 1,
        extraBufferCapacity = 32,
    )

    private var audioSource: AudioSource? = null
    private var videoSource: VideoSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var cameraVideoCapturer: CameraVideoCapturer? = null
    private var surfaceHelper: SurfaceTextureHelper? = null
    private var statsJob: Job? = null

    private var lastStatsAtMs: Long = 0L
    private var lastBytesOut: Long = 0L
    private var lastBytesIn: Long = 0L

    private val peerConnection: PeerConnection = createPeerConnection()

    init {
        prepareLocalTracks()
        statsJob = scope.launch {
            while (isActive) {
                emitStatsSnapshot()
                delay(2_000)
            }
        }
    }

    override suspend fun createOffer(): MeshSessionDescription {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo",
                    if (config.callType == MeshCallType.VIDEO) "true" else "false",
                ),
            )
        }
        return createLocalDescription(isOffer = true, constraints = constraints)
    }

    override suspend fun createAnswer(): MeshSessionDescription {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        return createLocalDescription(isOffer = false, constraints = constraints)
    }

    override suspend fun setRemoteDescription(description: MeshSessionDescription) {
        val remote = SessionDescription(description.type.toWebRtc(), description.sdp)
        suspendCancellableCoroutine<Unit> { continuation ->
            peerConnection.setRemoteDescription(
                object : SdpObserver {
                    override fun onCreateSuccess(sessionDescription: SessionDescription?) = Unit
                    override fun onSetSuccess() {
                        continuation.resume(Unit)
                    }
                    override fun onCreateFailure(message: String?) = Unit
                    override fun onSetFailure(message: String?) {
                        continuation.resumeWithException(IllegalStateException(message ?: "setRemoteDescription failed"))
                    }
                },
                remote,
            )
        }
    }

    override suspend fun addIceCandidate(candidate: MeshIceCandidate) {
        val accepted = peerConnection.addIceCandidate(
            IceCandidate(
                candidate.sdpMid,
                candidate.sdpMLineIndex,
                candidate.candidate,
            ),
        )
        if (!accepted) {
            throw IllegalStateException("ICE candidate rejected")
        }
    }

    override suspend fun setMicrophoneEnabled(enabled: Boolean) {
        audioTrack?.setEnabled(enabled)
        updateState { current ->
            current.copy(localAudioEnabled = enabled, updatedAt = Clock.System.now())
        }
    }

    override suspend fun isMicrophoneEnabled(): Boolean = stateFlow.value.localAudioEnabled

    override suspend fun setCameraEnabled(enabled: Boolean) {
        videoTrack?.setEnabled(enabled)
        updateState { current ->
            current.copy(localVideoEnabled = enabled, updatedAt = Clock.System.now())
        }
    }

    override suspend fun isCameraEnabled(): Boolean = stateFlow.value.localVideoEnabled

    override suspend fun switchCamera() {
        val capturer = cameraVideoCapturer ?: throw UnsupportedOperationException("Camera capturer unavailable")
        suspendCancellableCoroutine<Unit> { continuation ->
            capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                    updateState { current ->
                        current.copy(
                            cameraFacing = if (isFrontCamera) MeshCameraFacing.FRONT else MeshCameraFacing.BACK,
                            updatedAt = Clock.System.now(),
                        )
                    }
                    continuation.resume(Unit)
                }

                override fun onCameraSwitchError(errorDescription: String?) {
                    continuation.resumeWithException(IllegalStateException(errorDescription ?: "switchCamera failed"))
                }
            })
        }
    }

    override suspend fun attachLocalRenderer(rendererId: String) = Unit

    override suspend fun attachRemoteRenderer(peerId: String, rendererId: String) = Unit

    override suspend fun detachRenderer(rendererId: String) = Unit

    override suspend fun currentState(): MeshCallMediaState = stateFlow.value

    override suspend fun currentStats(): MeshMediaStats? {
        val latest = runCatching { statsFlow.replayCache.lastOrNull() }.getOrNull()
        return latest
    }

    override fun signalEvents(): Flow<MeshWebRtcSignalEvent> = signalFlow.asSharedFlow()

    override fun stateUpdates(): Flow<MeshCallMediaState> = stateFlow.asStateFlow()

    override fun statsUpdates(): Flow<MeshMediaStats> = statsFlow.asSharedFlow()

    override suspend fun close() {
        statsJob?.cancel()
        runCatching { (videoCapturer as? CameraVideoCapturer)?.stopCapture() }
        runCatching { videoCapturer?.dispose() }
        runCatching { surfaceHelper?.dispose() }
        runCatching { videoTrack?.dispose() }
        runCatching { audioTrack?.dispose() }
        runCatching { videoSource?.dispose() }
        runCatching { audioSource?.dispose() }
        runCatching { peerConnection.close() }
        runCatching { peerConnection.dispose() }
        updateState { current ->
            current.copy(
                connectionState = MeshMediaConnectionState.CLOSED,
                updatedAt = Clock.System.now(),
            )
        }
        scope.cancel()
    }

    private fun initialState(): MeshCallMediaState {
        val localVideo = config.callType == MeshCallType.VIDEO
        return MeshCallMediaState(
            callId = callId,
            localPeerId = localPeerId,
            localAudioEnabled = true,
            localVideoEnabled = localVideo,
            cameraFacing = MeshCameraFacing.FRONT,
            connectionState = MeshMediaConnectionState.NEW,
            peers = remotePeerIds.map { peerId ->
                MeshPeerMediaState(
                    peerId = peerId,
                    audioEnabled = false,
                    videoEnabled = false,
                    hasAudioTrack = false,
                    hasVideoTrack = false,
                    connectionState = MeshMediaConnectionState.NEW,
                )
            },
            updatedAt = Clock.System.now(),
        )
    }

    private fun createPeerConnection(): PeerConnection {
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()),
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        return requireNotNull(factory.createPeerConnection(rtcConfig, observer())) {
            "Failed to create PeerConnection"
        }
    }

    private fun observer(): PeerConnection.Observer = object : PeerConnection.Observer {
        override fun onSignalingChange(newState: PeerConnection.SignalingState?) = Unit
        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
            val mapped = when (newState) {
                PeerConnection.IceConnectionState.NEW -> MeshMediaConnectionState.NEW
                PeerConnection.IceConnectionState.CHECKING -> MeshMediaConnectionState.CONNECTING
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED,
                -> MeshMediaConnectionState.CONNECTED
                PeerConnection.IceConnectionState.DISCONNECTED -> MeshMediaConnectionState.DISCONNECTED
                PeerConnection.IceConnectionState.FAILED -> MeshMediaConnectionState.FAILED
                PeerConnection.IceConnectionState.CLOSED -> MeshMediaConnectionState.CLOSED
                null -> MeshMediaConnectionState.NEW
            }
            updateConnectionState(mapped)
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit

        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) = Unit

        override fun onIceCandidate(candidate: IceCandidate?) {
            val ice = candidate ?: return
            signalFlow.tryEmit(
                MeshWebRtcSignalEvent(
                    callId = callId,
                    signalType = MeshCallSignalType.ICE_CANDIDATE,
                    iceCandidate = MeshIceCandidate(
                        sdpMid = ice.sdpMid,
                        sdpMLineIndex = ice.sdpMLineIndex,
                        candidate = ice.sdp,
                    ),
                    createdAt = Clock.System.now(),
                ),
            )
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit

        override fun onAddStream(stream: org.webrtc.MediaStream?) = Unit

        override fun onRemoveStream(stream: org.webrtc.MediaStream?) = Unit

        override fun onDataChannel(dataChannel: org.webrtc.DataChannel?) = Unit

        override fun onRenegotiationNeeded() = Unit

        override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, mediaStreams: Array<out org.webrtc.MediaStream>?) = Unit

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
            val mapped = when (newState) {
                PeerConnection.PeerConnectionState.NEW -> MeshMediaConnectionState.NEW
                PeerConnection.PeerConnectionState.CONNECTING -> MeshMediaConnectionState.CONNECTING
                PeerConnection.PeerConnectionState.CONNECTED -> MeshMediaConnectionState.CONNECTED
                PeerConnection.PeerConnectionState.DISCONNECTED -> MeshMediaConnectionState.DISCONNECTED
                PeerConnection.PeerConnectionState.FAILED -> MeshMediaConnectionState.FAILED
                PeerConnection.PeerConnectionState.CLOSED -> MeshMediaConnectionState.CLOSED
                null -> MeshMediaConnectionState.NEW
            }
            updateConnectionState(mapped)
            when (mapped) {
                MeshMediaConnectionState.CONNECTING -> {
                    signalFlow.tryEmit(
                        MeshWebRtcSignalEvent(
                            callId = callId,
                            signalType = MeshCallSignalType.RECONNECTING,
                            createdAt = Clock.System.now(),
                        ),
                    )
                }
                MeshMediaConnectionState.CONNECTED -> {
                    signalFlow.tryEmit(
                        MeshWebRtcSignalEvent(
                            callId = callId,
                            signalType = MeshCallSignalType.RECONNECTED,
                            createdAt = Clock.System.now(),
                        ),
                    )
                }
                else -> Unit
            }
        }

        override fun onTrack(transceiver: RtpTransceiver?) {
            val track = transceiver?.receiver?.track() ?: return
            when (track.kind()) {
                MediaStreamTrack.AUDIO_TRACK_KIND -> markRemoteTrack(audio = true, video = false)
                MediaStreamTrack.VIDEO_TRACK_KIND -> markRemoteTrack(audio = false, video = true)
            }
        }

        override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) = Unit

        override fun onSelectedCandidatePairChanged(event: org.webrtc.CandidatePairChangeEvent?) = Unit
    }

    private fun prepareLocalTracks() {
        audioSource = factory.createAudioSource(MediaConstraints())
        audioTrack = factory.createAudioTrack("audio-$callId", audioSource)
        audioTrack?.setEnabled(true)

        if (config.callType == MeshCallType.VIDEO) {
            val createdCapturer = createVideoCapturer()
            if (createdCapturer != null) {
                videoCapturer = createdCapturer
                cameraVideoCapturer = createdCapturer as? CameraVideoCapturer
                videoSource = factory.createVideoSource(createdCapturer.isScreencast)
                surfaceHelper = SurfaceTextureHelper.create("capture-$callId", eglBase.eglBaseContext)
                createdCapturer.initialize(surfaceHelper, context, videoSource?.capturerObserver)
                runCatching { createdCapturer.startCapture(640, 480, 24) }
                videoTrack = factory.createVideoTrack("video-$callId", videoSource)
                videoTrack?.setEnabled(true)
            } else {
                updateState { current ->
                    current.copy(
                        localVideoEnabled = false,
                        errorMessage = "Камера недоступна",
                        updatedAt = Clock.System.now(),
                    )
                }
            }
        }

        val streamIds = listOf("stream-$callId")
        audioTrack?.let { peerConnection.addTrack(it, streamIds) }
        videoTrack?.let { peerConnection.addTrack(it, streamIds) }
    }

    private suspend fun createLocalDescription(
        isOffer: Boolean,
        constraints: MediaConstraints,
    ): MeshSessionDescription {
        return suspendCancellableCoroutine { continuation ->
            val createObserver = object : SdpObserver {
                override fun onCreateSuccess(description: SessionDescription?) {
                    val created = description
                        ?: return continuation.resumeWithException(IllegalStateException("Empty SessionDescription"))
                    peerConnection.setLocalDescription(
                        object : SdpObserver {
                            override fun onCreateSuccess(sessionDescription: SessionDescription?) = Unit
                            override fun onSetSuccess() {
                                continuation.resume(created.toContract())
                            }
                            override fun onCreateFailure(message: String?) = Unit
                            override fun onSetFailure(message: String?) {
                                continuation.resumeWithException(IllegalStateException(message ?: "setLocalDescription failed"))
                            }
                        },
                        created,
                    )
                }

                override fun onSetSuccess() = Unit

                override fun onCreateFailure(message: String?) {
                    continuation.resumeWithException(IllegalStateException(message ?: "createDescription failed"))
                }

                override fun onSetFailure(message: String?) = Unit
            }
            if (isOffer) {
                peerConnection.createOffer(createObserver, constraints)
            } else {
                peerConnection.createAnswer(createObserver, constraints)
            }
        }
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        val preferred = deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: deviceNames.firstOrNull { enumerator.isBackFacing(it) }
            ?: deviceNames.firstOrNull()
            ?: return null
        updateState { current ->
            current.copy(
                cameraFacing = if (enumerator.isFrontFacing(preferred)) MeshCameraFacing.FRONT else MeshCameraFacing.BACK,
                updatedAt = Clock.System.now(),
            )
        }
        return enumerator.createCapturer(preferred, object : CameraVideoCapturer.CameraEventsHandler {
            override fun onCameraError(errorDescription: String?) {
                updateState { current ->
                    current.copy(
                        errorMessage = errorDescription ?: "Ошибка камеры",
                        updatedAt = Clock.System.now(),
                    )
                }
            }

            override fun onCameraDisconnected() = Unit
            override fun onCameraFreezed(errorDescription: String?) = Unit
            override fun onCameraOpening(cameraName: String?) = Unit
            override fun onFirstFrameAvailable() = Unit
            override fun onCameraClosed() = Unit
        })
    }

    private fun markRemoteTrack(audio: Boolean, video: Boolean) {
        updateState { current ->
            val peers = current.peers.map { peer ->
                peer.copy(
                    hasAudioTrack = peer.hasAudioTrack || audio,
                    hasVideoTrack = peer.hasVideoTrack || video,
                    audioEnabled = peer.audioEnabled || audio,
                    videoEnabled = peer.videoEnabled || video,
                    connectionState = current.connectionState,
                )
            }
            current.copy(peers = peers, updatedAt = Clock.System.now())
        }
    }

    private fun updateConnectionState(connectionState: MeshMediaConnectionState) {
        updateState { current ->
            val peers = current.peers.map { it.copy(connectionState = connectionState) }
            current.copy(
                connectionState = connectionState,
                peers = peers,
                updatedAt = Clock.System.now(),
            )
        }
    }

    private suspend fun emitStatsSnapshot() {
        suspendCancellableCoroutine<Unit> { continuation ->
            peerConnection.getStats { report ->
                val nowMs = System.currentTimeMillis()
                var bytesOut = 0L
                var bytesIn = 0L
                var jitter = 0.0
                var rtt = 0.0
                var packetsLost = 0L
                var packetsReceived = 0L
                report.statsMap.values.forEach { stat ->
                    when (stat.type) {
                        "candidate-pair" -> {
                            rtt = maxOf(rtt, stat.members["currentRoundTripTime"].asDouble())
                        }
                        "inbound-rtp" -> {
                            bytesIn += stat.members["bytesReceived"].asLong()
                            packetsLost += stat.members["packetsLost"].asLong()
                            packetsReceived += stat.members["packetsReceived"].asLong()
                            jitter = maxOf(jitter, stat.members["jitter"].asDouble())
                        }
                        "outbound-rtp" -> {
                            bytesOut += stat.members["bytesSent"].asLong()
                        }
                    }
                }
                val elapsedMs = (nowMs - lastStatsAtMs).coerceAtLeast(1L)
                val outKbps = if (lastStatsAtMs == 0L) 0 else (((bytesOut - lastBytesOut).coerceAtLeast(0L) * 8.0) / elapsedMs).toInt()
                val inKbps = if (lastStatsAtMs == 0L) 0 else (((bytesIn - lastBytesIn).coerceAtLeast(0L) * 8.0) / elapsedMs).toInt()
                lastStatsAtMs = nowMs
                lastBytesOut = bytesOut
                lastBytesIn = bytesIn
                val lossPercent = if (packetsLost + packetsReceived == 0L) 0.0 else (packetsLost.toDouble() / (packetsLost + packetsReceived).toDouble()) * 100.0
                statsFlow.tryEmit(
                    MeshMediaStats(
                        callId = callId,
                        rttMs = (rtt * 1_000).toInt().coerceAtLeast(0),
                        packetLossPercent = lossPercent,
                        jitterMs = (jitter * 1_000).toInt().coerceAtLeast(0),
                        outboundBitrateKbps = outKbps,
                        inboundBitrateKbps = inKbps,
                        capturedAt = Clock.System.now(),
                    ),
                )
                continuation.resume(Unit)
            }
        }
    }

    private inline fun updateState(transform: (MeshCallMediaState) -> MeshCallMediaState) {
        stateFlow.value = transform(stateFlow.value)
    }
}

private fun Any?.asLong(): Long = when (this) {
    is Number -> toLong()
    is String -> toLongOrNull() ?: 0L
    else -> 0L
}

private fun Any?.asDouble(): Double = when (this) {
    is Number -> toDouble()
    is String -> toDoubleOrNull() ?: 0.0
    else -> 0.0
}

private fun MeshSdpType.toWebRtc(): SessionDescription.Type = when (this) {
    MeshSdpType.OFFER -> SessionDescription.Type.OFFER
    MeshSdpType.ANSWER -> SessionDescription.Type.ANSWER
}

private fun SessionDescription.toContract(): MeshSessionDescription = MeshSessionDescription(
    type = when (type) {
        SessionDescription.Type.OFFER -> MeshSdpType.OFFER
        SessionDescription.Type.ANSWER -> MeshSdpType.ANSWER
        else -> MeshSdpType.OFFER
    },
    sdp = description,
)
