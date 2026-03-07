package org.expert.link.app.desktop

import dev.onvoid.webrtc.media.FourCC
import dev.onvoid.webrtc.media.video.VideoBufferConverter
import dev.onvoid.webrtc.media.video.VideoFrame
import dev.onvoid.webrtc.media.video.VideoTrack
import dev.onvoid.webrtc.media.video.VideoTrackSink
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Реестр Desktop видеотреков и привязанных рендереров.
 *
 * Нужен для связывания WebRTC треков из media-адаптера и Compose UI.
 */
internal object DesktopVideoTrackRegistry {
    private data class RendererBinding(
        val panel: DesktopVideoPanel,
        val callId: String,
        val peerId: String?,
        val local: Boolean,
        var attachedTrack: VideoTrack? = null,
    )

    private val lock = Any()
    private val idCounter = AtomicLong(0)
    private val localTracks = mutableMapOf<String, VideoTrack>()
    private val remoteTracks = mutableMapOf<String, MutableMap<String, VideoTrack>>()
    private val bindings = mutableMapOf<String, RendererBinding>()

    fun nextRendererId(): String = "desktop-renderer-${idCounter.incrementAndGet()}"

    fun registerLocalTrack(callId: String, track: VideoTrack) {
        synchronized(lock) {
            localTracks[callId] = track
            refreshBindingsLocked(callId)
        }
    }

    fun registerRemoteTrack(callId: String, peerId: String, track: VideoTrack) {
        synchronized(lock) {
            remoteTracks.getOrPut(callId) { mutableMapOf() }[peerId] = track
            refreshBindingsLocked(callId)
        }
    }

    fun clearCall(callId: String) {
        synchronized(lock) {
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
        panel: DesktopVideoPanel,
    ) {
        synchronized(lock) {
            val current = bindings[rendererId]
            if (current != null && current.panel !== panel) {
                attachTrackLocked(current, null)
            }
            val binding = RendererBinding(
                panel = panel,
                callId = callId,
                peerId = peerId,
                local = local,
            )
            bindings[rendererId] = binding
            attachTrackLocked(binding, selectTrackLocked(callId, peerId, local))
        }
    }

    fun unbindRenderer(rendererId: String) {
        synchronized(lock) {
            val binding = bindings.remove(rendererId) ?: return
            attachTrackLocked(binding, null)
            binding.panel.clearFrame()
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
        binding.attachedTrack?.removeSink(binding.panel)
        binding.attachedTrack = track
        if (track != null) {
            track.addSink(binding.panel)
        }
    }
}

/** Простая Swing-панель для рендеринга WebRTC видеокадров. */
internal class DesktopVideoPanel(
    private val local: Boolean,
) : JPanel(), VideoTrackSink {
    @Volatile
    private var image: BufferedImage? = null

    init {
        background = Color(0x10, 0x12, 0x16)
        isOpaque = true
    }

    override fun onVideoFrame(frame: VideoFrame) {
        runCatching {
            val width = frame.buffer.width
            val height = frame.buffer.height
            if (width <= 0 || height <= 0) {
                return
            }
            val argb = ByteArray(width * height * 4)
            VideoBufferConverter.convertFromI420(frame.buffer, argb, FourCC.ARGB)
            val next = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val pixels = IntArray(width * height)
            var src = 0
            var index = 0
            while (index < pixels.size) {
                val a = argb[src].toInt() and 0xFF
                val r = argb[src + 1].toInt() and 0xFF
                val g = argb[src + 2].toInt() and 0xFF
                val b = argb[src + 3].toInt() and 0xFF
                pixels[index] = (a shl 24) or (r shl 16) or (g shl 8) or b
                src += 4
                index++
            }
            next.setRGB(0, 0, width, height, pixels, 0, width)
            SwingUtilities.invokeLater {
                image = next
                repaint()
            }
        }
    }

    fun clearFrame() {
        image = null
        repaint()
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g2 = graphics as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        val frame = image
        if (frame == null) {
            g2.color = Color(0x33, 0x36, 0x3F)
            g2.fillRect(0, 0, width, height)
            g2.color = Color(0xD0, 0xD7, 0xDE)
            g2.drawString(if (local) "Локальная камера" else "Ожидание видео", 12, 22)
            return
        }
        g2.drawImage(frame, 0, 0, width, height, null)
    }
}

