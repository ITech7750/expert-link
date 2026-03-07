package org.expert.link.app.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Платформенный video-view для звонка.
 *
 * Отрисовывает локальный или удалённый WebRTC видеотрек, если он доступен.
 */
@Composable
expect fun CallVideoSurface(
    callId: String,
    peerId: String?,
    local: Boolean,
    modifier: Modifier = Modifier,
)

