package org.expert.link.mesh.infrastructure.adapter

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.network.DiscoveryMessageType
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Проверяет базовый discovery-сценарий на одном UDP-порту. */
class UdpDiscoveryAdapterTest {

    @Test
    fun `two adapters can discover each other on the same discovery port`() = runBlocking {
        val discoveryPort = 19_700
        val multicastGroup = "239.60.60.60"
        val firstAdapter = UdpDiscoveryAdapter(discoveryPort, multicastGroup, JvmMulticastSupportAdapter())
        val secondAdapter = UdpDiscoveryAdapter(discoveryPort, multicastGroup, JvmMulticastSupportAdapter())
        val now = Clock.System.now()

        try {
            firstAdapter.start(
                localPeerIdentity = PeerIdentity(
                    peerId = "peer-a",
                    displayName = "A",
                    publicKey = "pub-a",
                ),
                endpoint = PeerEndpoint(
                    host = "127.0.0.1",
                    port = 18_101,
                    announcedAt = now,
                ),
            )
            secondAdapter.start(
                localPeerIdentity = PeerIdentity(
                    peerId = "peer-b",
                    displayName = "B",
                    publicKey = "pub-b",
                ),
                endpoint = PeerEndpoint(
                    host = "127.0.0.1",
                    port = 18_102,
                    announcedAt = now,
                ),
            )

            firstAdapter.broadcastHello()

            val discoveredFrame = withTimeout(5_000) {
                secondAdapter.events.first { frame ->
                    frame.type == DiscoveryMessageType.NODE_HELLO && frame.sourcePeerId == "peer-a"
                }
            }

            assertEquals("peer-a", discoveredFrame.sourcePeerId)
            assertEquals(18_101, discoveredFrame.endpoint.port)
        } finally {
            firstAdapter.stop()
            secondAdapter.stop()
        }
    }

    @Test
    fun `adapters can discover each other when discovery ports differ within fanout range`() = runBlocking {
        val firstPort = 19_720
        val secondPort = 19_722
        val multicastGroup = "239.60.60.60"
        val firstAdapter = UdpDiscoveryAdapter(firstPort, multicastGroup, JvmMulticastSupportAdapter())
        val secondAdapter = UdpDiscoveryAdapter(secondPort, multicastGroup, JvmMulticastSupportAdapter())
        val now = Clock.System.now()

        try {
            firstAdapter.start(
                localPeerIdentity = PeerIdentity(
                    peerId = "peer-c",
                    displayName = "C",
                    publicKey = "pub-c",
                ),
                endpoint = PeerEndpoint(
                    host = "127.0.0.1",
                    port = 18_201,
                    announcedAt = now,
                ),
            )
            secondAdapter.start(
                localPeerIdentity = PeerIdentity(
                    peerId = "peer-d",
                    displayName = "D",
                    publicKey = "pub-d",
                ),
                endpoint = PeerEndpoint(
                    host = "127.0.0.1",
                    port = 18_202,
                    announcedAt = now,
                ),
            )

            firstAdapter.broadcastHello()

            val discoveredFrame = withTimeout(5_000) {
                secondAdapter.events.first { frame ->
                    frame.type == DiscoveryMessageType.NODE_HELLO && frame.sourcePeerId == "peer-c"
                }
            }

            assertEquals("peer-c", discoveredFrame.sourcePeerId)
            assertEquals(18_201, discoveredFrame.endpoint.port)
        } finally {
            firstAdapter.stop()
            secondAdapter.stop()
        }
    }
}
