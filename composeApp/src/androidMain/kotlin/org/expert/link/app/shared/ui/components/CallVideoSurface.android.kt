package org.expert.link.app.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.expert.link.app.android.AndroidVideoTrackRegistry
import org.webrtc.SurfaceViewRenderer

/** Android-отрисовка локального/удалённого видеотрека звонка. */
@Composable
actual fun CallVideoSurface(
    callId: String,
    peerId: String?,
    local: Boolean,
    modifier: Modifier,
) {
    val rendererId = remember(callId, peerId, local) { AndroidVideoTrackRegistry.nextRendererId() }
    val rendererRef = remember(rendererId) { mutableStateOf<SurfaceViewRenderer?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceViewRenderer(context).also { renderer ->
                AndroidVideoTrackRegistry.prepareRenderer(renderer, local)
                AndroidVideoTrackRegistry.bindRenderer(rendererId, callId, peerId, local, renderer)
                rendererRef.value = renderer
            }
        },
        update = { renderer ->
            rendererRef.value = renderer
            renderer.setMirror(local)
            AndroidVideoTrackRegistry.bindRenderer(rendererId, callId, peerId, local, renderer)
        },
    )

    DisposableEffect(rendererId, callId, peerId, local) {
        onDispose {
            AndroidVideoTrackRegistry.unbindRenderer(rendererId)
            rendererRef.value?.let(AndroidVideoTrackRegistry::releaseRenderer)
            rendererRef.value = null
        }
    }
}
