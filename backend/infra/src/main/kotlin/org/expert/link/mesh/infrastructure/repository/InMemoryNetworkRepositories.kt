package org.expert.link.mesh.infrastructure.repository

import kotlinx.datetime.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.expert.link.mesh.domain.model.network.PeerEndpointCandidate
import org.expert.link.mesh.domain.model.network.ReversePathEntry
import org.expert.link.mesh.domain.model.network.RouteEntry
import org.expert.link.mesh.domain.port.repository.DedupCachePort
import org.expert.link.mesh.domain.port.repository.EndpointCachePort
import org.expert.link.mesh.domain.port.repository.ReversePathRepositoryPort
import org.expert.link.mesh.domain.port.repository.RouteRepositoryPort

/** Кэш endpoint-кандидатов в памяти. */
class InMemoryEndpointCacheAdapter : EndpointCachePort {
    private val mutex = Mutex()
    private val candidates = linkedMapOf<String, MutableMap<String, PeerEndpointCandidate>>()

    override suspend fun put(candidate: PeerEndpointCandidate) {
        mutex.withLock {
            val peerCandidates = candidates.computeIfAbsent(candidate.peerId) { linkedMapOf() }
            peerCandidates[candidate.endpoint.asUrl()] = candidate
        }
    }

    override suspend fun findByPeerId(peerId: String): List<PeerEndpointCandidate> = mutex.withLock {
        candidates[peerId]?.values?.toList().orEmpty()
    }

    override suspend fun listNeighbors(): List<PeerEndpointCandidate> = mutex.withLock {
        candidates.values.flatMap { it.values }
    }

    override suspend fun evictExpired(now: Instant): Int = mutex.withLock {
        var removed = 0
        candidates.values.forEach { peerCandidates ->
            val before = peerCandidates.size
            peerCandidates.entries.removeIf { (_, candidate) -> candidate.endpoint.expiresAt?.let { it <= now } == true }
            removed += before - peerCandidates.size
        }
        candidates.entries.removeIf { it.value.isEmpty() }
        removed
    }

    override suspend fun removePeer(peerId: String) {
        mutex.withLock { candidates.remove(peerId) }
    }
}

/** Репозиторий маршрутов в памяти. */
class InMemoryRouteRepositoryAdapter : RouteRepositoryPort {
    private val mutex = Mutex()
    private val routes = linkedMapOf<String, RouteEntry>()

    override suspend fun save(routeEntry: RouteEntry): RouteEntry = mutex.withLock {
        routes[routeEntry.targetPeerId] = routeEntry
        routeEntry
    }

    override suspend fun findByTargetPeerId(targetPeerId: String): RouteEntry? = mutex.withLock { routes[targetPeerId] }

    override suspend fun list(): List<RouteEntry> = mutex.withLock { routes.values.toList() }

    override suspend fun remove(targetPeerId: String) {
        mutex.withLock { routes.remove(targetPeerId) }
    }

    override suspend fun evictExpired(now: Instant): Int = mutex.withLock {
        val before = routes.size
        routes.entries.removeIf { (_, route) -> route.expiresAt <= now }
        before - routes.size
    }
}

/** Репозиторий обратных путей в памяти. */
class InMemoryReversePathRepositoryAdapter : ReversePathRepositoryPort {
    private val mutex = Mutex()
    private val entries = linkedMapOf<String, ReversePathEntry>()

    override suspend fun save(entry: ReversePathEntry): ReversePathEntry = mutex.withLock {
        entries[entry.packetId] = entry
        entry
    }

    override suspend fun findByPacketId(packetId: String): ReversePathEntry? = mutex.withLock { entries[packetId] }

    override suspend fun remove(packetId: String) {
        mutex.withLock { entries.remove(packetId) }
    }

    override suspend fun evictExpired(now: Instant): Int = mutex.withLock {
        val before = entries.size
        entries.entries.removeIf { (_, entry) -> entry.expiresAt <= now }
        before - entries.size
    }
}

/** Dedup-кэш в памяти. */
class InMemoryDedupCacheAdapter : DedupCachePort {
    private val mutex = Mutex()
    private val entries = linkedMapOf<String, Instant>()

    override suspend fun markSeenIfNew(packetId: String, expiresAt: Instant): Boolean = mutex.withLock {
        if (entries.containsKey(packetId)) {
            return false
        }
        entries[packetId] = expiresAt
        true
    }

    override suspend fun evictExpired(now: Instant): Int = mutex.withLock {
        val before = entries.size
        entries.entries.removeIf { (_, expiry) -> expiry <= now }
        before - entries.size
    }
}
