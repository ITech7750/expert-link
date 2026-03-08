package org.expert.link.app.android

import android.util.Log
import java.util.concurrent.atomic.AtomicLong
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * Реестр видеотреков и активных рендереров Android.
 *
 * Позволяет связать WebRTC треки из media-адаптера с UI-поверхностями Compose.
 */
internal object AndroidVideoTrackRegistry {
    private const val TAG = "ExpertLinkCall/TrackRegistry"

    private data class RendererBinding(
        val renderer: SurfaceViewRenderer,
        val callId: String,
        val peerId: String?,
        val local: Boolean,
        var attachedTrack: VideoTrack? = null,
    )

    private val lock = Any()
    private val idCounter = AtomicLong(0)
    private val eglBase: EglBase = EglBase.create()
    private var sharedContext: EglBase.Context? = null
    private val localTracks = mutableMapOf<String, VideoTrack>()
    private val remoteTracks = mutableMapOf<String, MutableMap<String, VideoTrack>>()
    private val bindings = mutableMapOf<String, RendererBinding>()

    fun eglContext(): EglBase.Context = sharedContext ?: eglBase.eglBaseContext

    fun setSharedContext(context: EglBase.Context) {
        synchronized(lock) {
            sharedContext = context
            debug("Shared EGL context updated")
        }
    }

    fun nextRendererId(): String = "android-renderer-${idCounter.incrementAndGet()}"

    fun registerLocalTrack(callId: String, track: VideoTrack) {
        synchronized(lock) {
            localTracks[callId] = track
            debug("registerLocalTrack call=$callId track=${track.id()}")
            refreshBindingsLocked(callId)
        }
    }

    fun registerRemoteTrack(callId: String, peerId: String, track: VideoTrack) {
        synchronized(lock) {
            remoteTracks.getOrPut(callId) { mutableMapOf() }[peerId] = track
            debug("registerRemoteTrack call=$callId peer=$peerId track=${track.id()}")
            refreshBindingsLocked(callId)
        }
    }

    fun clearCall(callId: String) {
        synchronized(lock) {
            debug("clearCall call=$callId")
            localTracks.remove(callId)
            remoteTracks.remove(callId)
            bindings.values
                .filter { it.callId == callId }
                .forEach { binding -> attachTrackLocked(binding, null) }
        }
    }

    fun bindRenderer(
        rendererId: String,
        callId: String,
        peerId: String?,
        local: Boolean,
        renderer: SurfaceViewRenderer,
    ) {
        synchronized(lock) {
            val current = bindings[rendererId]
            if (current != null && current.renderer !== renderer) {
                attachTrackLocked(current, null)
            }
            val binding = RendererBinding(
                renderer = renderer,
                callId = callId,
                peerId = peerId,
                local = local,
            )
            bindings[rendererId] = binding
            debug(
                "bindRenderer renderer=$rendererId call=$callId local=$local peer=${peerId ?: "first"} " +
                    "selected=${trackId(selectTrackLocked(callId, peerId, local))}",
            )
            attachTrackLocked(binding, selectTrackLocked(callId, peerId, local))
        }
    }

    fun unbindRenderer(rendererId: String) {
        synchronized(lock) {
            val binding = bindings.remove(rendererId) ?: return
            debug("unbindRenderer renderer=$rendererId call=${binding.callId} local=${binding.local} peer=${binding.peerId}")
            attachTrackLocked(binding, null)
        }
    }

    private fun refreshBindingsLocked(callId: String) {
        bindings.values
            .filter { it.callId == callId }
            .forEach { binding ->
                attachTrackLocked(binding, selectTrackLocked(binding.callId, binding.peerId, binding.local))
            }
    }

    private fun selectTrackLocked(callId: String, peerId: String?, local: Boolean): VideoTrack? {
        return if (local) {
            localTracks[callId]
        } else {
            val perCall = remoteTracks[callId] ?: return null
            if (peerId != null) {
                perCall[peerId]
            } else {
                perCall.values.firstOrNull()
            }
        }
    }

    private fun attachTrackLocked(binding: RendererBinding, track: VideoTrack?) {
        if (binding.attachedTrack === track) {
            return
        }
        debug(
            "attachTrack call=${binding.callId} local=${binding.local} peer=${binding.peerId ?: "first"} " +
                "from=${trackId(binding.attachedTrack)} to=${trackId(track)}",
        )
        binding.attachedTrack?.removeSink(binding.renderer)
        binding.attachedTrack = track
        if (track != null) {
            track.addSink(binding.renderer)
        } else {
            binding.renderer.clearImage()
        }
    }

    fun prepareRenderer(renderer: SurfaceViewRenderer, local: Boolean) {
        renderer.init(eglContext(), null)
        renderer.setEnableHardwareScaler(true)
        renderer.setMirror(local)
        renderer.setScalingType(
            if (local) {
                RendererCommon.ScalingType.SCALE_ASPECT_FIT
            } else {
                RendererCommon.ScalingType.SCALE_ASPECT_FILL
            },
        )
        renderer.setZOrderMediaOverlay(local)
    }

    fun releaseRenderer(renderer: SurfaceViewRenderer) {
        runCatching { renderer.release() }
    }

    private fun trackId(track: VideoTrack?): String = track?.id() ?: "null"

    private fun debug(message: String) {
        Log.d(TAG, message)
    }
}
