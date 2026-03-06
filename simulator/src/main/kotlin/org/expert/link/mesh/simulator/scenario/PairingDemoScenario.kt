package org.expert.link.mesh.simulator.scenario

/**
 * Демо pairing по invite.
 *
 * Контракт:
 * - `MeshNode.createPairingInvite`
 * - `MeshNode.pairWithInvite`
 * - `MeshNode.peers`
 * - `MeshNode.pairingSessions`
 *
 * Модели:
 * - запрос: `String` invite
 * - ответы: `MeshPairingSession`, `List<MeshPairedPeer>`
 */
class PairingDemoScenario(
    private val basePort: Int,
) : DemoScenario {
    override val name: String = "PairingDemoScenario"

    override suspend fun run(): DemoScenarioReport {
        val alice = ScenarioSupport.launchNode(name = "pair-alice", port = basePort)
        val bob = ScenarioSupport.launchNode(name = "pair-bob", port = basePort + 1)
        return try {
            ScenarioSupport.connectBidirectional(alice, bob)

            val invite: String = bob.createPairingInvite()
            val session = alice.pairWithInvite(invite)
            ScenarioSupport.settle()

            val alicePeers = alice.peers()
            val bobPeers = bob.peers()
            val aliceSessions = alice.pairingSessions()

            DemoScenarioReport(
                name = name,
                lines = listOf(
                    "Feature: pairing доверенных узлов.",
                    "Imports: MeshNode, MeshPeerEndpoint.",
                    "Public API: createPairingInvite(), pairWithInvite(), peers(), pairingSessions().",
                    "Request model: invite string, созданная удалённым узлом.",
                    "Response model: MeshPairingSession и список MeshPairedPeer.",
                    "invite.length=${invite.length}",
                    "pairing.sessionId=${session.sessionId}",
                    "pairing.trustState=${session.trustState}",
                    "alice.peers=${alicePeers.map { it.identity.displayName + ":" + it.trustState }}",
                    "bob.peers=${bobPeers.map { it.identity.displayName + ":" + it.trustState }}",
                    "alice.activePairingSessions=${aliceSessions.size}",
                    "Mobile usage: показать QR/string invite, затем передать строку в pairWithInvite().",
                ),
            )
        } finally {
            ScenarioSupport.stopAll(alice, bob)
        }
    }
}
