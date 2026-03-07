package org.expert.link.mesh.backend

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.contract.api.MeshChatCommand
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.config.MeshRelayConfig
import org.junit.jupiter.api.Test

class MeshBackendTopologyIntegrationTest {
    @Test
    fun `should rebuild topology after host loss and keep local delivery continuity`() = runBlocking {
        val host = MeshBackend.launch(config("topo-host", 19781))
        val nodeA = MeshBackend.launch(config("topo-a", 19782))
        val nodeB = MeshBackend.launch(config("topo-b", 19783))
        try {
            connectBidirectional(nodeA, host)
            connectBidirectional(nodeB, host)
            connectBidirectional(nodeA, nodeB)
            pair(nodeA, host)
            pair(nodeB, host)
            pair(nodeA, nodeB)
            delay(700)

            val before = nodeA.observeTopologyState()
            nodeA.forgetPeerEndpoint(host.profile.peerId)
            nodeB.forgetPeerEndpoint(host.profile.peerId)
            host.stop()
            delay(700)

            val afterA = nodeA.forceTopologyRefresh()
            val afterB = nodeB.forceTopologyRefresh()
            val message = nodeA.sendChat(
                MeshChatCommand(
                    targetPeerId = nodeB.profile.peerId,
                    body = "continuity message after host loss",
                ),
            )
            delay(700)

            assertThat(before.networkRoleState.currentHostPeerId).isEqualTo(host.profile.peerId)
            assertThat(afterA.networkRoleState.currentHostPeerId).isNotEqualTo(host.profile.peerId)
            assertThat(afterB.networkRoleState.currentHostPeerId).isNotEqualTo(host.profile.peerId)
            assertThat(message.messageId).isNotBlank()
            assertThat(afterA.routeHealth).isNotEmpty()
        } finally {
            runCatching { nodeA.stop() }
            runCatching { nodeB.stop() }
            runCatching { host.stop() }
        }
    }

    @Test
    fun `should select relay proxy strategy for remote networks and activate relay mode`() = runBlocking {
        val nodeX = MeshBackend.launch(config("topo-remote-x", 19881, relay = true))
        val nodeY = MeshBackend.launch(config("topo-remote-y", 19882, relay = true))
        try {
            val invite = nodeY.createPairingInvite()
            nodeX.pairWithInvite(invite)
            delay(900)

            val strategy = nodeX.observeConnectivityStrategy(nodeY.profile.peerId)
            val plan = nodeX.routingPlan(nodeY.profile.peerId)
            nodeX.sendChat(
                MeshChatCommand(
                    targetPeerId = nodeY.profile.peerId,
                    body = "relay topology message",
                ),
            )
            delay(700)
            val topology = nodeX.observeTopologyState()

            assertThat(strategy.routeMode.name).isEqualTo("RENDEZVOUS_RELAY")
            assertThat(strategy.connectivityMode.name).isEqualTo("RELAY_PROXY")
            assertThat(plan.useRelayGateway).isTrue()
            assertThat(topology.relayMode.name).isIn("ACTIVE_FALLBACK", "FORCED")
        } finally {
            runCatching { nodeX.stop() }
            runCatching { nodeY.stop() }
        }
    }

    private suspend fun connectBidirectional(left: MeshNode, right: MeshNode) {
        left.rememberPeerEndpoint(right.profile.peerId, right.endpoint)
        right.rememberPeerEndpoint(left.profile.peerId, left.endpoint)
    }

    private suspend fun pair(left: MeshNode, right: MeshNode) {
        val invite = right.createPairingInvite()
        left.pairWithInvite(invite)
        delay(500)
    }

    private fun config(name: String, port: Int, relay: Boolean = false): MeshNodeConfig = MeshNodeConfig(
        displayName = name,
        bindHost = "127.0.0.1",
        httpPort = port,
        discoveryPort = port + 1_000,
        multicastGroup = "239.60.60.60",
        featureFlags = MeshFeatureFlags(
            discoveryEnabled = false,
            relayEnabled = relay,
            inMemoryTransport = true,
            inMemoryDiscovery = true,
        ),
        relay = MeshRelayConfig(
            enabled = relay,
            forceRelayLookup = relay,
            relayEligible = relay,
        ),
    )
}

