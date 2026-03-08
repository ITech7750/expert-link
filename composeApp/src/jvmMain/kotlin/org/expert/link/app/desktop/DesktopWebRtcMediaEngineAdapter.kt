package org.expert.link.app.desktop

import dev.onvoid.webrtc.CreateSessionDescriptionObserver
import dev.onvoid.webrtc.PeerConnectionFactory
import dev.onvoid.webrtc.PeerConnectionObserver
import dev.onvoid.webrtc.RTCAnswerOptions
import dev.onvoid.webrtc.RTCConfiguration
import dev.onvoid.webrtc.RTCIceCandidate
import dev.onvoid.webrtc.RTCIceConnectionState
import dev.onvoid.webrtc.RTCIceGatheringState
import dev.onvoid.webrtc.RTCIceServer
import dev.onvoid.webrtc.RTCOfferOptions
import dev.onvoid.webrtc.RTCPeerConnection
import dev.onvoid.webrtc.RTCPeerConnectionState
import dev.onvoid.webrtc.RTCSessionDescription
import dev.onvoid.webrtc.RTCSignalingState
import dev.onvoid.webrtc.RTCStatsCollectorCallback
import dev.onvoid.webrtc.RTCStatsType
import dev.onvoid.webrtc.RTCSdpType
import dev.onvoid.webrtc.RTCRtpReceiver
import dev.onvoid.webrtc.RTCRtpTransceiver
import dev.onvoid.webrtc.RTCDataChannel
import dev.onvoid.webrtc.SetSessionDescriptionObserver
import dev.onvoid.webrtc.media.MediaDevices
import dev.onvoid.webrtc.media.MediaStream
import dev.onvoid.webrtc.media.MediaStreamTrack
import dev.onvoid.webrtc.media.audio.AudioOptions
import dev.onvoid.webrtc.media.audio.AudioTrack
import dev.onvoid.webrtc.media.audio.AudioTrackSource
import dev.onvoid.webrtc.media.video.VideoCaptureCapability
import dev.onvoid.webrtc.media.video.VideoDevice
import dev.onvoid.webrtc.media.video.VideoDeviceSource
import dev.onvoid.webrtc.media.video.VideoTrack
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

/**
 * Реальная Desktop интеграция WebRTC media-engine.
 *
 * Сигналинг остаётся в backend, этот адаптер отвечает только за SDP/ICE,
 * локальные треки и состояние медиа-сессии.
 */
class DesktopWebRtcMediaEngineAdapter : MeshMediaEngine {
    private val factory: PeerConnectionFactory = PeerConnectionFactory()
    private val sessions = ConcurrentHashMap<String, DesktopWebRtcSession>()

    override val isSupported: Boolean = true

    override suspend fun openSession(config: MeshMediaSessionConfig): MeshWebRtcSession {
        return sessions.computeIfAbsent(config.callId) {
            DesktopWebRtcSession(factory = factory, config = config)
        }
    }

    override suspend fun findSession(callId: String): MeshWebRtcSession? = sessions[callId]

    override suspend fun listSessions(): List<MeshWebRtcSession> = sessions.values.toList()

    override suspend fun closeSession(callId: String) {
        sessions.remove(callId)?.close()
    }
}

private class DesktopWebRtcSession(
    private val factory: PeerConnectionFactory,
    private val config: MeshMediaSessionConfig,
) : MeshWebRtcSession {
    override val callId: String = config.callId
    override val localPeerId: String = config.localPeerId
    override val remotePeerIds: Set<String> = config.remotePeerIds

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val signalFlow = MutableSharedFlow<MeshWebRtcSignalEvent>(extraBufferCapacity = 64)
    private val stateFlow = MutableStateFlow(initialState())
    private val statsFlow = MutableSharedFlow<MeshMediaStats>(replay = 1, extraBufferCapacity = 32)

    private var audioSource: AudioTrackSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoSource: VideoDeviceSource? = null
    private var videoTrack: VideoTrack? = null
    private var activeVideoDevice: VideoDevice? = null
    private var activeVideoCapability: VideoCaptureCapability? = null
    private val remoteAudioTracks = mutableMapOf<String, AudioTrack>()
    private val remoteVideoTracks = mutableMapOf<String, VideoTrack>()
    private var statsJob: Job? = null

    private var lastStatsAtMs: Long = 0L
    private var lastBytesOut: Long = 0L
    private var lastBytesIn: Long = 0L

    private val peerConnection: RTCPeerConnection = createPeerConnection()

    init {
        trace("session init call=$callId local=$localPeerId remote=$remotePeerIds type=${config.callType}")
        prepareLocalTracks()
        statsJob = scope.launch {
            while (isActive) {
                emitStatsSnapshot()
                delay(2_000)
            }
        }
    }

    override suspend fun createOffer(): MeshSessionDescription {
        return createLocalDescription(isOffer = true).also { description ->
            trace("createOffer success call=$callId sdpLength=${description.sdp.length}")
        }
    }

    override suspend fun createAnswer(): MeshSessionDescription {
        return createLocalDescription(isOffer = false).also { description ->
            trace("createAnswer success call=$callId sdpLength=${description.sdp.length}")
        }
    }

    override suspend fun setRemoteDescription(description: MeshSessionDescription) {
        trace("setRemoteDescription start call=$callId type=${description.type} sdpLength=${description.sdp.length}")
        val remoteDescription = RTCSessionDescription(description.type.toDesktop(), description.sdp)
        suspendCancellableCoroutine<Unit> { continuation ->
            peerConnection.setRemoteDescription(
                remoteDescription,
                object : SetSessionDescriptionObserver {
                    override fun onSuccess() {
                        trace("setRemoteDescription success call=$callId type=${description.type}")
                        continuation.resume(Unit)
                    }

                    override fun onFailure(error: String?) {
                        trace("setRemoteDescription failed call=$callId type=${description.type} error=${error ?: "unknown"}")
                        continuation.resumeWithException(IllegalStateException(error ?: "setRemoteDescription failed"))
                    }
                },
            )
        }
    }

    override suspend fun addIceCandidate(candidate: MeshIceCandidate) {
        trace(
            "addIceCandidate call=$callId sdpMid=${candidate.sdpMid} mLine=${candidate.sdpMLineIndex} " +
                "length=${candidate.candidate.length}",
        )
        peerConnection.addIceCandidate(
            RTCIceCandidate(
                candidate.sdpMid,
                candidate.sdpMLineIndex,
                candidate.candidate,
            ),
        )
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
            current.copy(localVideoEnabled = enabled && videoTrack != null, updatedAt = Clock.System.now())
        }
    }

    override suspend fun isCameraEnabled(): Boolean = stateFlow.value.localVideoEnabled

    override suspend fun switchCamera() {
        val currentSource = videoSource ?: throw UnsupportedOperationException("Видео не инициализировано")
        val devices = runCatching { MediaDevices.getVideoCaptureDevices().orEmpty() }.getOrElse { emptyList() }
        if (devices.size < 2) {
            throw UnsupportedOperationException("Доступна только одна камера")
        }

        val currentIndex = devices.indexOfFirst { it.descriptor == activeVideoDevice?.descriptor }
            .takeIf { it >= 0 }
            ?: 0
        val nextDevice = devices[(currentIndex + 1) % devices.size]
        val nextCapability = selectCapability(nextDevice)

        currentSource.stop()
        currentSource.setVideoCaptureDevice(nextDevice)
        if (nextCapability != null) {
            currentSource.setVideoCaptureCapability(nextCapability)
        }
        currentSource.start()

        activeVideoDevice = nextDevice
        activeVideoCapability = nextCapability
        updateState { current ->
            current.copy(
                cameraFacing = nextDevice.toFacing(),
                updatedAt = Clock.System.now(),
            )
        }
    }

    override suspend fun attachLocalRenderer(rendererId: String) = Unit

    override suspend fun attachRemoteRenderer(peerId: String, rendererId: String) = Unit

    override suspend fun detachRenderer(rendererId: String) = Unit

    override suspend fun currentState(): MeshCallMediaState = stateFlow.value

    override suspend fun currentStats(): MeshMediaStats? = statsFlow.replayCache.lastOrNull()

    override fun signalEvents(): Flow<MeshWebRtcSignalEvent> = signalFlow.asSharedFlow()

    override fun stateUpdates(): Flow<MeshCallMediaState> = stateFlow.asStateFlow()

    override fun statsUpdates(): Flow<MeshMediaStats> = statsFlow.asSharedFlow()

    override suspend fun close() {
        statsJob?.cancel()

        runCatching { videoSource?.stop() }
        remoteAudioTracks.clear()
        remoteVideoTracks.clear()
        DesktopVideoTrackRegistry.clearCall(callId)
        runCatching { videoTrack?.dispose() }
        runCatching { audioTrack?.dispose() }
        runCatching { videoSource?.dispose() }
        runCatching { peerConnection.close() }

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
            cameraFacing = MeshCameraFacing.UNKNOWN,
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

    private fun createPeerConnection(): RTCPeerConnection {
        val configuration = RTCConfiguration().apply {
            iceServers.add(RTCIceServer().apply { urls.add("stun:stun.l.google.com:19302") })
        }
        return requireNotNull(factory.createPeerConnection(configuration, observer())) {
            "Failed to create PeerConnection"
        }
    }

    private fun observer(): PeerConnectionObserver = object : PeerConnectionObserver {
        override fun onSignalingChange(state: RTCSignalingState?) = Unit

        override fun onConnectionChange(state: RTCPeerConnectionState?) {
            val mapped = state.toMeshConnectionState()
            trace("onConnectionChange call=$callId webrtc=$state mapped=$mapped")
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

        override fun onIceConnectionChange(state: RTCIceConnectionState?) {
            val mapped = state.toMeshConnectionState()
            trace("onIceConnectionChange call=$callId webrtc=$state mapped=$mapped")
            updateConnectionState(mapped)
        }

        override fun onStandardizedIceConnectionChange(state: RTCIceConnectionState?) = Unit

        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit

        override fun onIceGatheringChange(state: RTCIceGatheringState?) = Unit

        override fun onIceCandidate(candidate: RTCIceCandidate?) {
            val value = candidate ?: return
            trace(
                "emitLocalIce call=$callId sdpMid=${value.sdpMid} mLine=${value.sdpMLineIndex} " +
                    "length=${value.sdp.length}",
            )
            signalFlow.tryEmit(
                MeshWebRtcSignalEvent(
                    callId = callId,
                    signalType = MeshCallSignalType.ICE_CANDIDATE,
                    iceCandidate = MeshIceCandidate(
                        sdpMid = value.sdpMid,
                        sdpMLineIndex = value.sdpMLineIndex,
                        candidate = value.sdp,
                    ),
                    createdAt = Clock.System.now(),
                ),
            )
        }

        override fun onIceCandidateError(event: dev.onvoid.webrtc.RTCPeerConnectionIceErrorEvent?) = Unit

        override fun onIceCandidatesRemoved(candidates: Array<out RTCIceCandidate>?) = Unit

        override fun onAddStream(stream: MediaStream?) {
            trace("onAddStream call=$callId audio=${stream?.audioTracks?.size ?: 0} video=${stream?.videoTracks?.size ?: 0}")
            handleRemoteStream(stream)
        }

        override fun onRemoveStream(stream: MediaStream?) = Unit

        override fun onDataChannel(dataChannel: RTCDataChannel?) = Unit

        override fun onRenegotiationNeeded() = Unit

        override fun onAddTrack(receiver: RTCRtpReceiver?, mediaStreams: Array<out MediaStream>?) {
            trace("onAddTrack call=$callId kind=${receiver?.track?.kind} mediaStreams=${mediaStreams?.size ?: 0}")
            handleRemoteTrack(receiver?.track)
            mediaStreams.orEmpty().forEach(::handleRemoteStream)
        }

        override fun onRemoveTrack(receiver: RTCRtpReceiver?) = Unit

        override fun onTrack(transceiver: RTCRtpTransceiver?) {
            trace("onTrack call=$callId kind=${transceiver?.receiver?.track?.kind}")
            handleRemoteTrack(transceiver?.receiver?.track)
        }
    }

    private fun handleRemoteStream(stream: MediaStream?) {
        stream ?: return
        trace("handleRemoteStream call=$callId audio=${stream.audioTracks.size} video=${stream.videoTracks.size}")
        stream.audioTracks.orEmpty().forEach(::handleRemoteTrack)
        stream.videoTracks.orEmpty().forEach(::handleRemoteTrack)
    }

    private fun handleRemoteTrack(track: MediaStreamTrack?) {
        track ?: return
        val mappedPeerId = resolveRemotePeerIdForTrack()
        trace("handleRemoteTrack call=$callId kind=${track.kind} mappedPeer=$mappedPeerId remotePeers=$remotePeerIds")
        when (track.kind) {
            MediaStreamTrack.AUDIO_TRACK_KIND -> {
                val audio = track as? AudioTrack
                if (audio != null && mappedPeerId != null) {
                    audio.setEnabled(true)
                    remoteAudioTracks[mappedPeerId] = audio
                    trace("remoteAudioAttached call=$callId peer=$mappedPeerId")
                }
                markRemoteTrack(audio = true, video = false)
            }
            MediaStreamTrack.VIDEO_TRACK_KIND -> {
                val video = track as? VideoTrack
                if (video != null && mappedPeerId != null) {
                    video.setEnabled(true)
                    remoteVideoTracks[mappedPeerId] = video
                    DesktopVideoTrackRegistry.registerRemoteTrack(callId, mappedPeerId, video)
                    trace("remoteVideoAttached call=$callId peer=$mappedPeerId")
                }
                markRemoteTrack(audio = false, video = true)
            }
        }
    }

    private fun prepareLocalTracks() {
        audioSource = factory.createAudioSource(
            AudioOptions().apply {
                echoCancellation = true
                autoGainControl = true
                noiseSuppression = true
                highpassFilter = true
            },
        )
        audioTrack = factory.createAudioTrack("audio-$callId", audioSource)
        audioTrack?.setEnabled(true)
        trace("localAudioTrack ready call=$callId")

        if (config.callType == MeshCallType.VIDEO) {
            runCatching {
                val devices = MediaDevices.getVideoCaptureDevices().orEmpty()
                val selectedDevice = devices.firstOrNull()
                if (selectedDevice != null) {
                    val selectedCapability = selectCapability(selectedDevice)
                    val createdSource = VideoDeviceSource()
                    createdSource.setVideoCaptureDevice(selectedDevice)
                    if (selectedCapability != null) {
                        createdSource.setVideoCaptureCapability(selectedCapability)
                    }
                    createdSource.start()

                    activeVideoDevice = selectedDevice
                    activeVideoCapability = selectedCapability
                    videoSource = createdSource
                    videoTrack = factory.createVideoTrack("video-$callId", createdSource)
                    videoTrack?.setEnabled(true)
                    videoTrack?.let { DesktopVideoTrackRegistry.registerLocalTrack(callId, it) }
                    trace("localVideoTrack ready call=$callId device=${selectedDevice.descriptor}")
                    updateState { current ->
                        current.copy(
                            cameraFacing = selectedDevice.toFacing(),
                            updatedAt = Clock.System.now(),
                        )
                    }
                } else {
                    updateState { current ->
                        current.copy(
                            localVideoEnabled = false,
                            errorMessage = "Камера недоступна",
                            updatedAt = Clock.System.now(),
                        )
                    }
                }
            }.onFailure { error ->
                updateState { current ->
                    current.copy(
                        localVideoEnabled = false,
                        errorMessage = error.message ?: "Ошибка камеры",
                        updatedAt = Clock.System.now(),
                    )
                }
            }
        }

        val streamIds = listOf("stream-$callId")
        audioTrack?.let { peerConnection.addTrack(it, streamIds) }
        videoTrack?.let { peerConnection.addTrack(it, streamIds) }
        configureSenderBitrates()
        trace("localTracks added call=$callId audio=${audioTrack != null} video=${videoTrack != null}")
    }

    private fun resolveRemotePeerIdForTrack(): String? {
        if (remotePeerIds.isEmpty()) {
            return null
        }
        val alreadyMapped = remoteAudioTracks.keys + remoteVideoTracks.keys
        return remotePeerIds.firstOrNull { it !in alreadyMapped } ?: remotePeerIds.first()
    }

    private fun configureSenderBitrates() {
        runCatching {
            peerConnection.senders.forEach { sender ->
                val track = sender.track ?: return@forEach
                val parameters = sender.parameters ?: return@forEach
                parameters.encodings?.forEach { encoding ->
                    if (track.kind == MediaStreamTrack.AUDIO_TRACK_KIND) {
                        encoding.maxBitrate = 96_000
                        encoding.minBitrate = 24_000
                    } else if (track.kind == MediaStreamTrack.VIDEO_TRACK_KIND) {
                        encoding.maxBitrate = 1_200_000
                        encoding.minBitrate = 250_000
                        encoding.maxFramerate = 24.0
                    }
                }
                sender.setParameters(parameters)
            }
        }
    }

    private fun trace(message: String) {
        println("ExpertLinkCall/DesktopRTC $message")
    }

    private suspend fun createLocalDescription(isOffer: Boolean): MeshSessionDescription {
        return suspendCancellableCoroutine { continuation ->
            val createObserver = object : CreateSessionDescriptionObserver {
                override fun onSuccess(description: RTCSessionDescription?) {
                    val created = description
                        ?: return continuation.resumeWithException(IllegalStateException("Empty SessionDescription"))
                    peerConnection.setLocalDescription(
                        created,
                        object : SetSessionDescriptionObserver {
                            override fun onSuccess() {
                                continuation.resume(created.toContract())
                            }

                            override fun onFailure(error: String?) {
                                continuation.resumeWithException(IllegalStateException(error ?: "setLocalDescription failed"))
                            }
                        },
                    )
                }

                override fun onFailure(error: String?) {
                    continuation.resumeWithException(IllegalStateException(error ?: "createDescription failed"))
                }
            }

            if (isOffer) {
                peerConnection.createOffer(RTCOfferOptions(), createObserver)
            } else {
                peerConnection.createAnswer(RTCAnswerOptions(), createObserver)
            }
        }
    }

    private suspend fun emitStatsSnapshot() {
        suspendCancellableCoroutine<Unit> { continuation ->
            peerConnection.getStats(
                RTCStatsCollectorCallback { report ->
                    val nowMs = System.currentTimeMillis()
                    var bytesOut = 0L
                    var bytesIn = 0L
                    var jitter = 0.0
                    var rtt = 0.0
                    var packetsLost = 0L
                    var packetsReceived = 0L

                    report.stats.values.forEach { stat ->
                        when (stat.type) {
                            RTCStatsType.CANDIDATE_PAIR -> {
                                rtt = maxOf(rtt, stat.attributes["currentRoundTripTime"].asDouble())
                            }

                            RTCStatsType.INBOUND_RTP -> {
                                bytesIn += stat.attributes["bytesReceived"].asLong()
                                packetsLost += stat.attributes["packetsLost"].asLong()
                                packetsReceived += stat.attributes["packetsReceived"].asLong()
                                jitter = maxOf(jitter, stat.attributes["jitter"].asDouble())
                            }

                            RTCStatsType.OUTBOUND_RTP -> {
                                bytesOut += stat.attributes["bytesSent"].asLong()
                            }

                            else -> Unit
                        }
                    }

                    val elapsedMs = (nowMs - lastStatsAtMs).coerceAtLeast(1L)
                    val outKbps = if (lastStatsAtMs == 0L) {
                        0
                    } else {
                        (((bytesOut - lastBytesOut).coerceAtLeast(0L) * 8.0) / elapsedMs).toInt()
                    }
                    val inKbps = if (lastStatsAtMs == 0L) {
                        0
                    } else {
                        (((bytesIn - lastBytesIn).coerceAtLeast(0L) * 8.0) / elapsedMs).toInt()
                    }
                    lastStatsAtMs = nowMs
                    lastBytesOut = bytesOut
                    lastBytesIn = bytesIn

                    val lossPercent = if (packetsLost + packetsReceived == 0L) {
                        0.0
                    } else {
                        (packetsLost.toDouble() / (packetsLost + packetsReceived).toDouble()) * 100.0
                    }

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
                },
            )
        }
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

    private inline fun updateState(transform: (MeshCallMediaState) -> MeshCallMediaState) {
        stateFlow.value = transform(stateFlow.value)
    }

    private fun selectCapability(device: VideoDevice): VideoCaptureCapability? {
        val capabilities = runCatching { MediaDevices.getVideoCaptureCapabilities(device).orEmpty() }
            .getOrElse { emptyList() }
        if (capabilities.isEmpty()) {
            return null
        }
        // Для desktop держим сбалансированный профиль, чтобы снизить CPU без заметной потери качества.
        val preferred = capabilities
            .filter { it.width <= 960 && it.height <= 540 && it.frameRate <= 24 }
            .maxWithOrNull(compareBy<VideoCaptureCapability> { it.width * it.height }.thenBy { it.frameRate })
        if (preferred != null) {
            return preferred
        }
        return capabilities.minWithOrNull(compareBy<VideoCaptureCapability> { it.width * it.height }.thenBy { it.frameRate })
    }
}

private fun RTCSessionDescription.toContract(): MeshSessionDescription {
    return MeshSessionDescription(
        type = when (sdpType) {
            RTCSdpType.OFFER -> MeshSdpType.OFFER
            RTCSdpType.ANSWER -> MeshSdpType.ANSWER
            else -> MeshSdpType.OFFER
        },
        sdp = sdp,
    )
}

private fun MeshSdpType.toDesktop(): RTCSdpType = when (this) {
    MeshSdpType.OFFER -> RTCSdpType.OFFER
    MeshSdpType.ANSWER -> RTCSdpType.ANSWER
}

private fun RTCPeerConnectionState?.toMeshConnectionState(): MeshMediaConnectionState = when (this) {
    RTCPeerConnectionState.NEW -> MeshMediaConnectionState.NEW
    RTCPeerConnectionState.CONNECTING -> MeshMediaConnectionState.CONNECTING
    RTCPeerConnectionState.CONNECTED -> MeshMediaConnectionState.CONNECTED
    RTCPeerConnectionState.DISCONNECTED -> MeshMediaConnectionState.DISCONNECTED
    RTCPeerConnectionState.FAILED -> MeshMediaConnectionState.FAILED
    RTCPeerConnectionState.CLOSED -> MeshMediaConnectionState.CLOSED
    null -> MeshMediaConnectionState.NEW
}

private fun RTCIceConnectionState?.toMeshConnectionState(): MeshMediaConnectionState = when (this) {
    RTCIceConnectionState.NEW -> MeshMediaConnectionState.NEW
    RTCIceConnectionState.CHECKING -> MeshMediaConnectionState.CONNECTING
    RTCIceConnectionState.CONNECTED,
    RTCIceConnectionState.COMPLETED,
    -> MeshMediaConnectionState.CONNECTED
    RTCIceConnectionState.DISCONNECTED -> MeshMediaConnectionState.DISCONNECTED
    RTCIceConnectionState.FAILED -> MeshMediaConnectionState.FAILED
    RTCIceConnectionState.CLOSED -> MeshMediaConnectionState.CLOSED
    null -> MeshMediaConnectionState.NEW
}

private fun VideoDevice.toFacing(): MeshCameraFacing {
    val nameValue = name.lowercase()
    return when {
        "front" in nameValue -> MeshCameraFacing.FRONT
        "back" in nameValue || "rear" in nameValue -> MeshCameraFacing.BACK
        else -> MeshCameraFacing.UNKNOWN
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
