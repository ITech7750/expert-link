package org.expert.link.mesh.domain.port.repository

import kotlinx.datetime.Instant
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.identity.PairedPeer
import org.expert.link.mesh.domain.model.identity.PairingSession
import org.expert.link.mesh.domain.model.identity.TrustState
import org.expert.link.mesh.domain.model.security.BlockedPeer

/** Порт хранения локального профиля. */
interface LocalProfileRepositoryPort {
    /** Возвращает локальный профиль или `null`. */
    suspend fun get(): LocalProfile?

    /** Сохраняет локальный профиль. */
    suspend fun save(profile: LocalProfile): LocalProfile
}

/** Порт хранения доверенных узлов и статуса доверия. */
interface PeerRepositoryPort {
    /** Сохраняет peer. */
    suspend fun save(peer: PairedPeer): PairedPeer

    /** Ищет peer по peerId. */
    suspend fun findByPeerId(peerId: String): PairedPeer?

    /** Возвращает список peers. */
    suspend fun list(): List<PairedPeer>

    /** Обновляет trust state peer. */
    suspend fun updateTrustState(peerId: String, trustState: TrustState): PairedPeer?
}

/** Порт хранения активных сессий сопряжения. */
interface PairingSessionRepositoryPort {
    /** Сохраняет pairing-сессию. */
    suspend fun save(session: PairingSession): PairingSession

    /** Ищет сессию по id. */
    suspend fun findBySessionId(sessionId: String): PairingSession?

    /** Ищет активную сессию по invite secret. */
    suspend fun findActiveByInviteSecret(inviteSecret: String): PairingSession?

    /** Помечает сессию как использованную. */
    suspend fun markUsed(sessionId: String): PairingSession?

    /** Возвращает активные сессии. */
    suspend fun listActive(): List<PairingSession>

    /** Удаляет истёкшие сессии. */
    suspend fun removeExpired(now: Instant): Int
}

/** Порт хранения списка блокировок. */
interface BlockListRepositoryPort {
    /** Сохраняет запись block list. */
    suspend fun save(blockedPeer: BlockedPeer): BlockedPeer

    /** Удаляет запись block list. */
    suspend fun remove(peerId: String)

    /** Возвращает запись block list по peerId. */
    suspend fun findByPeerId(peerId: String): BlockedPeer?

    /** Возвращает весь block list. */
    suspend fun list(): List<BlockedPeer>
}
