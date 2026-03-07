package org.expert.link.mesh.domain.port.repository

import kotlinx.datetime.Instant
import org.expert.link.mesh.domain.model.network.PeerEndpointCandidate
import org.expert.link.mesh.domain.model.network.ReversePathEntry
import org.expert.link.mesh.domain.model.network.RouteEntry

/** Порт runtime-кэша endpoint-кандидатов. */
interface EndpointCachePort {
    /** Добавляет или заменяет endpoint-кандидат. */
    suspend fun put(candidate: PeerEndpointCandidate)

    /** Возвращает endpoint-кандидаты peer. */
    suspend fun findByPeerId(peerId: String): List<PeerEndpointCandidate>

    /** Возвращает соседние endpoint-кандидаты. */
    suspend fun listNeighbors(): List<PeerEndpointCandidate>

    /** Удаляет устаревшие endpoints. */
    suspend fun evictExpired(now: Instant): Int

    /** Удаляет все endpoints peer. */
    suspend fun removePeer(peerId: String)
}

/** Порт runtime-хранилища маршрутов. */
interface RouteRepositoryPort {
    /** Сохраняет маршрут. */
    suspend fun save(routeEntry: RouteEntry): RouteEntry

    /** Ищет лучший маршрут до peer. */
    suspend fun findByTargetPeerId(targetPeerId: String): RouteEntry?

    /** Возвращает список маршрутов. */
    suspend fun list(): List<RouteEntry>

    /** Удаляет маршрут по targetPeerId. */
    suspend fun remove(targetPeerId: String)

    /** Удаляет истёкшие маршруты. */
    suspend fun evictExpired(now: Instant): Int
}

/** Порт runtime-хранилища обратного пути для ACK. */
interface ReversePathRepositoryPort {
    /** Сохраняет reverse path. */
    suspend fun save(entry: ReversePathEntry): ReversePathEntry

    /** Ищет reverse path по packetId. */
    suspend fun findByPacketId(packetId: String): ReversePathEntry?

    /** Удаляет reverse path. */
    suspend fun remove(packetId: String)

    /** Удаляет истёкшие reverse path записи. */
    suspend fun evictExpired(now: Instant): Int
}

/** Порт runtime dedup-кэша. */
interface DedupCachePort {
    /** Помечает пакет как увиденный и возвращает `true`, если он новый. */
    suspend fun markSeenIfNew(packetId: String, expiresAt: Instant): Boolean

    /** Удаляет истёкшие dedup-маркеры. */
    suspend fun evictExpired(now: Instant): Int
}
