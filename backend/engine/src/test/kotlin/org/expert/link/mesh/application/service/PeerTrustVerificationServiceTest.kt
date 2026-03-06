package org.expert.link.mesh.application.service

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.domain.model.identity.PairedPeer
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.identity.TrustState
import org.expert.link.mesh.infrastructure.repository.InMemoryPeerRepositoryAdapter
import org.junit.jupiter.api.Test
import kotlinx.datetime.Instant

class PeerTrustVerificationServiceTest {
    @Test
    fun `should return only trusted peers`() = runTest {
        val repository = InMemoryPeerRepositoryAdapter()
        repository.save(
            PairedPeer(
                peerIdentity = PeerIdentity("peer-a", "Alice", "public"),
                trustState = TrustState.TRUSTED,
                pairedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        repository.save(
            PairedPeer(
                peerIdentity = PeerIdentity("peer-b", "Bob", "public"),
                trustState = TrustState.PENDING,
                pairedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        val service = PeerTrustVerificationService(repository)

        assertThat(service.requireTrusted("peer-a")).isNotNull()
        assertThat(service.requireTrusted("peer-b")).isNull()
    }
}
