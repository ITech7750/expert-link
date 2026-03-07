package org.expert.link.mesh.domain.port.repository

import org.expert.link.mesh.domain.model.call.CallEvent
import org.expert.link.mesh.domain.model.call.CallParticipant
import org.expert.link.mesh.domain.model.call.CallState
import org.expert.link.mesh.domain.model.call.CallRoom
import org.expert.link.mesh.domain.model.call.CallSession

/** Порт хранения call-сессий. */
interface CallSessionRepositoryPort {
    /** Сохраняет call-сессию. */
    suspend fun save(session: CallSession): CallSession

    /** Ищет call-сессию по id. */
    suspend fun findByCallId(callId: String): CallSession?

    /** Возвращает call-сессии. */
    suspend fun list(): List<CallSession>

    /** Возвращает call-сессии комнаты. */
    suspend fun listByRoomId(roomId: String): List<CallSession>

    /** Обновляет состояние звонка. */
    suspend fun updateStatus(callId: String, status: CallState): CallSession?
}

/** Порт хранения комнат звонков. */
interface CallRoomRepositoryPort {
    /** Сохраняет комнату звонка. */
    suspend fun save(room: CallRoom): CallRoom

    /** Ищет комнату по id. */
    suspend fun findByRoomId(roomId: String): CallRoom?

    /** Возвращает список комнат. */
    suspend fun list(): List<CallRoom>
}

/** Порт хранения состояний участников звонка. */
interface CallParticipantRepositoryPort {
    /** Перезаписывает список участников звонка. */
    suspend fun replace(callId: String, participants: List<CallParticipant>)

    /** Добавляет или обновляет одного участника звонка. */
    suspend fun upsert(callId: String, participant: CallParticipant)

    /** Возвращает участников звонка. */
    suspend fun listByCallId(callId: String): List<CallParticipant>
}

/** Порт хранения событий звонка. */
interface CallEventRepositoryPort {
    /** Добавляет событие звонка. */
    suspend fun append(event: CallEvent): CallEvent

    /** Возвращает события звонка в порядке времени. */
    suspend fun listByCallId(callId: String, limit: Int = 200): List<CallEvent>
}
