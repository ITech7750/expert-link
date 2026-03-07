package org.expert.link.app.desktop

import org.expert.link.mesh.contract.api.MeshMediaEngine
import org.expert.link.mesh.contract.api.MeshMediaSessionConfig
import org.expert.link.mesh.contract.api.MeshWebRtcSession

/**
 * Desktop boundary для WebRTC media.
 *
 * В текущей сборке desktop backend использует signaling и call state,
 * а real media-engine подключается отдельным native модулем.
 */
class DesktopWebRtcMediaEngineAdapter : MeshMediaEngine {
    override val isSupported: Boolean = false

    override suspend fun openSession(config: MeshMediaSessionConfig): MeshWebRtcSession {
        throw UnsupportedOperationException("Desktop WebRTC media engine не подключён в текущей сборке")
    }

    override suspend fun findSession(callId: String): MeshWebRtcSession? = null

    override suspend fun listSessions(): List<MeshWebRtcSession> = emptyList()

    override suspend fun closeSession(callId: String) = Unit
}
