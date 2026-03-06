package org.expert.link.mesh.simulator.scenario

import kotlinx.coroutines.delay
import org.expert.link.mesh.backend.MeshBackend
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.config.MeshRelayConfig
import java.io.File

/** Вспомогательные функции для mobile-style demo. */
object ScenarioSupport {
    /** Запускает узел только через публичный backend facade. */
    suspend fun launchNode(
        name: String,
        port: Int,
        discoveryEnabled: Boolean = false,
        relayEnabled: Boolean = false,
    ): MeshNode {
        return MeshBackend.launch(
            MeshNodeConfig(
                displayName = name,
                bindHost = "127.0.0.1",
                httpPort = port,
                discoveryPort = port + 1_000,
                multicastGroup = "239.10.10.10",
                featureFlags = MeshFeatureFlags(
                    discoveryEnabled = discoveryEnabled,
                    relayEnabled = relayEnabled,
                    inMemoryTransport = true,
                    inMemoryDiscovery = discoveryEnabled,
                ),
                relay = MeshRelayConfig(
                    enabled = relayEnabled,
                    forceRelayLookup = relayEnabled,
                    relayEligible = relayEnabled,
                ),
            ),
        )
    }

    /** Связывает два узла ручными endpoint hint. */
    suspend fun connectBidirectional(left: MeshNode, right: MeshNode) {
        left.rememberPeerEndpoint(right.profile.peerId, right.endpoint)
        right.rememberPeerEndpoint(left.profile.peerId, left.endpoint)
    }

    /** Выполняет pairing по invite. */
    suspend fun pair(joiner: MeshNode, inviter: MeshNode) {
        val invite = inviter.createPairingInvite()
        joiner.pairWithInvite(invite)
        settle()
    }

    /** Даёт runtime время на асинхронную доставку. */
    suspend fun settle(milliseconds: Long = 500) {
        delay(milliseconds)
    }

    /** Останавливает все переданные узлы. */
    suspend fun stopAll(vararg nodes: MeshNode) {
        nodes.reversed().forEach { node ->
            runCatching { node.stop() }
        }
    }

    /** Создаёт временный файл для file transfer сценариев. */
    fun tempFile(prefix: String, text: String): File {
        val file = File.createTempFile(prefix, ".txt")
        file.writeText(text)
        file.deleteOnExit()
        return file
    }

    /** Создаёт более крупный временный файл для cancel/resume сценариев. */
    fun largeTempFile(prefix: String, repetitions: Int = 16_384): File {
        val file = File.createTempFile(prefix, ".txt")
        buildString(capacity = repetitions * 32) {
            repeat(repetitions) { index ->
                append("mesh-demo-line-")
                append(index)
                append('\n')
            }
        }.also(file::writeText)
        file.deleteOnExit()
        return file
    }
}
