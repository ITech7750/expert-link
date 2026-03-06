package org.expert.link.mesh.bootstrap.runtime

import io.ktor.server.engine.ApplicationEngine
import org.expert.link.mesh.bootstrap.NodeLifecycleService
import org.expert.link.mesh.bootstrap.config.NodeConfiguration
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.network.PeerEndpoint

/** Запущенный экземпляр узла. */
data class MeshNodeRuntime(
    val configuration: NodeConfiguration,
    val localProfile: LocalProfile,
    val endpoint: PeerEndpoint,
    val lifecycleService: NodeLifecycleService,
    val server: ApplicationEngine? = null,
)
