package org.expert.link.app.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import org.expert.link.app.desktop.DesktopVideoPanel
import org.expert.link.app.desktop.DesktopVideoTrackRegistry

/** Desktop-отрисовка локального/удалённого видеотрека звонка. */
@Composable
actual fun CallVideoSurface(
    callId: String,
    peerId: String?,
    local: Boolean,
    modifier: Modifier,
) {
    val rendererId = remember(callId, peerId, local) { DesktopVideoTrackRegistry.nextRendererId() }
    val panelRef = remember(rendererId) { mutableStateOf<DesktopVideoPanel?>(null) }

    SwingPanel(
        modifier = modifier,
        factory = {
            DesktopVideoPanel(local = local).also { panel ->
                panelRef.value = panel
                DesktopVideoTrackRegistry.bindRenderer(rendererId, callId, peerId, local, panel)
            }
        },
        update = { panel ->
            panelRef.value = panel
            DesktopVideoTrackRegistry.bindRenderer(rendererId, callId, peerId, local, panel)
        },
    )

    DisposableEffect(rendererId, callId, peerId, local) {
        onDispose {
            DesktopVideoTrackRegistry.unbindRenderer(rendererId)
            panelRef.value = null
        }
    }
}

