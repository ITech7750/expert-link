package org.expert.link.mesh.bootstrap

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.bootstrap.config.FeatureFlags
import org.expert.link.mesh.bootstrap.config.FileTransferSettings
import org.expert.link.mesh.bootstrap.config.NodeConfiguration
import org.expert.link.mesh.bootstrap.config.RelayClientSettings
import org.expert.link.mesh.infrastructure.transport.InMemoryPacketTransportAdapter
import org.expert.link.mesh.domain.model.network.PacketType
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class MeshNodeIntegrationTest {
    @Test
    fun `should complete pairing flow between two nodes`() = runBlocking {
        val bootstrap = MeshNodeBootstrap()
        val nodeA = bootstrap.bootstrap(testConfig("pair-a", 19081))
        val nodeB = bootstrap.bootstrap(testConfig("pair-b", 19082))
        connectBidirectional(nodeA, nodeB)

        val invite = nodeB.lifecycleService.createPairingInvite()
        nodeA.lifecycleService.pairWithInvite(invite)
        delay(500)
        val message = nodeA.lifecycleService.sendChat(nodeB.localProfile.peerId, "hello after pairing")
        delay(500)

        assertThat(message.messageId).isNotBlank()
        assertThat(nodeA.lifecycleService.metricsSnapshot().counters["ack.received"] ?: 0L).isGreaterThanOrEqualTo(1)

        nodeA.lifecycleService.stop()
        nodeB.lifecycleService.stop()
    }

    @Test
    fun `should exchange encrypted chat and receive ack between two nodes`() = runBlocking {
        val bootstrap = MeshNodeBootstrap()
        val nodeA = bootstrap.bootstrap(testConfig("chat-a", 19181))
        val nodeB = bootstrap.bootstrap(testConfig("chat-b", 19182))
        connectBidirectional(nodeA, nodeB)
        pair(nodeA, nodeB)

        nodeA.lifecycleService.sendChat(nodeB.localProfile.peerId, "secure hello")
        delay(500)

        assertThat(nodeA.lifecycleService.metricsSnapshot().counters["ack.received"] ?: 0L).isGreaterThanOrEqualTo(1)
        assertThat(nodeB.lifecycleService.recentEvents().any { it.message.contains("Received chat message") }).isTrue()

        nodeA.lifecycleService.stop()
        nodeB.lifecycleService.stop()
    }

    @Test
    fun `should forward message via intermediate relay node`() = runBlocking {
        val bootstrap = MeshNodeBootstrap()
        val nodeA = bootstrap.bootstrap(testConfig("relay-a", 19281))
        val nodeB = bootstrap.bootstrap(testConfig("relay-b", 19282))
        val nodeC = bootstrap.bootstrap(testConfig("relay-c", 19283))
        nodeA.lifecycleService.rememberPeerEndpoint(nodeB.localProfile.peerId, nodeB.endpoint)
        nodeB.lifecycleService.rememberPeerEndpoint(nodeA.localProfile.peerId, nodeA.endpoint)
        nodeB.lifecycleService.rememberPeerEndpoint(nodeC.localProfile.peerId, nodeC.endpoint)
        nodeC.lifecycleService.rememberPeerEndpoint(nodeB.localProfile.peerId, nodeB.endpoint)

        pair(nodeA, nodeB)
        pair(nodeB, nodeC)
        val inviteFromC = nodeC.lifecycleService.createPairingInvite()
        nodeA.lifecycleService.pairWithInvite(inviteFromC)
        delay(500)
        nodeA.lifecycleService.forgetPeerEndpoint(nodeC.localProfile.peerId)

        nodeA.lifecycleService.sendChat(nodeC.localProfile.peerId, "via relay node")
        delay(750)

        assertThat(nodeB.lifecycleService.metricsSnapshot().counters["relay.forwarded"] ?: 0L).isGreaterThanOrEqualTo(1)
        assertThat(nodeC.lifecycleService.recentEvents().any { it.message.contains("Received chat message") }).isTrue()

        nodeA.lifecycleService.stop()
        nodeB.lifecycleService.stop()
        nodeC.lifecycleService.stop()
    }

    @Test
    fun `should transfer file and resume after missing chunk`() = runBlocking {
        val bootstrap = MeshNodeBootstrap()
        val downloadA = createTempDirectory("mesh-a").toFile()
        val downloadB = createTempDirectory("mesh-b").toFile()
        val nodeA = bootstrap.bootstrap(testConfig("file-a", 19381, downloadDir = downloadA.absolutePath, chunkSizeBytes = 8))
        val nodeB = bootstrap.bootstrap(testConfig("file-b", 19382, downloadDir = downloadB.absolutePath, chunkSizeBytes = 8))
        connectBidirectional(nodeA, nodeB)
        pair(nodeA, nodeB)

        val source = File.createTempFile("mesh-transfer", ".txt")
        source.writeText("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        InMemoryPacketTransportAdapter.dropNext(nodeB.endpoint, PacketType.FILE_CHUNK)
        val session = nodeA.lifecycleService.sendFile(nodeB.localProfile.peerId, source.absolutePath)
        delay(300)
        nodeB.lifecycleService.requestFileResume(session.transferId)
        delay(1_500)

        val downloaded = downloadB.resolve("${session.transferId}-${source.name}")
        assertThat(downloaded.exists()).isTrue()
        assertThat(downloaded.readText()).isEqualTo(source.readText())

        nodeA.lifecycleService.stop()
        nodeB.lifecycleService.stop()
    }

    @Test
    fun `should fallback to rendezvous relay when no direct route exists`() = runBlocking {
        val bootstrap = MeshNodeBootstrap()
        val nodeD = bootstrap.bootstrap(testConfig("relay-d", 19481, relay = true))
        val nodeE = bootstrap.bootstrap(testConfig("relay-e", 19482, relay = true))

        val invite = nodeE.lifecycleService.createPairingInvite()
        nodeD.lifecycleService.pairWithInvite(invite)
        delay(750)
        nodeD.lifecycleService.sendChat(nodeE.localProfile.peerId, "hello over stub relay")
        delay(500)

        assertThat(nodeD.lifecycleService.metricsSnapshot().counters["relay.gateway.sent"] ?: 0L).isGreaterThanOrEqualTo(1)
        assertThat(nodeE.lifecycleService.recentEvents().any { it.message.contains("Received chat message") }).isTrue()

        nodeD.lifecycleService.stop()
        nodeE.lifecycleService.stop()
    }

    private suspend fun pair(left: org.expert.link.mesh.bootstrap.runtime.MeshNodeRuntime, right: org.expert.link.mesh.bootstrap.runtime.MeshNodeRuntime) {
        val invite = right.lifecycleService.createPairingInvite()
        left.lifecycleService.pairWithInvite(invite)
        delay(400)
    }

    private suspend fun connectBidirectional(left: org.expert.link.mesh.bootstrap.runtime.MeshNodeRuntime, right: org.expert.link.mesh.bootstrap.runtime.MeshNodeRuntime) {
        left.lifecycleService.rememberPeerEndpoint(right.localProfile.peerId, right.endpoint)
        right.lifecycleService.rememberPeerEndpoint(left.localProfile.peerId, left.endpoint)
    }

    private fun testConfig(name: String, port: Int, relay: Boolean = false, downloadDir: String = createTempDirectory(name).toString(), chunkSizeBytes: Int = 16): NodeConfiguration {
        return NodeConfiguration(
            displayName = name,
            bindHost = "127.0.0.1",
            httpPort = port,
            discoveryPort = port + 1_000,
            multicastGroup = "239.20.20.20",
            featureFlags = FeatureFlags(
                discoveryEnabled = false,
                relayEnabled = relay,
                inMemoryTransport = true,
                inMemoryDiscovery = true,
            ),
            relayClientSettings = RelayClientSettings(
                enabled = relay,
                forceRelayLookup = relay,
                relayEligible = relay,
            ),
            fileTransferSettings = FileTransferSettings(
                chunkSizeBytes = chunkSizeBytes,
                downloadDirectory = downloadDir,
            ),
        )
    }
}
