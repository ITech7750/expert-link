package org.expert.link.mesh.domain.port.repository

import kotlinx.datetime.Instant
import org.expert.link.mesh.domain.model.messaging.ChatMessage
import org.expert.link.mesh.domain.model.messaging.Conversation
import org.expert.link.mesh.domain.model.messaging.MessageDeliveryStatus
import org.expert.link.mesh.domain.model.network.PendingAckRecord

/** Порт постоянного хранения диалогов. */
interface ConversationRepositoryPort {
    /** Сохраняет диалог. */
    suspend fun save(conversation: Conversation): Conversation

    /** Ищет диалог по id. */
    suspend fun findByConversationId(conversationId: String): Conversation?

    /** Ищет диалог по участникам. */
    suspend fun findByParticipants(participantPeerIds: Set<String>): Conversation?

    /** Возвращает список диалогов. */
    suspend fun list(): List<Conversation>
}

/** Порт постоянного хранения сообщений. */
interface MessageRepositoryPort {
    /** Сохраняет сообщение. */
    suspend fun save(message: ChatMessage): ChatMessage

    /** Ищет сообщение по id. */
    suspend fun findByMessageId(messageId: String): ChatMessage?

    /** Возвращает сообщения диалога. */
    suspend fun listByConversation(conversationId: String): List<ChatMessage>

    /** Обновляет статус доставки сообщения. */
    suspend fun updateStatus(messageId: String, status: MessageDeliveryStatus): ChatMessage?
}

/** Порт runtime-очереди исходящих сообщений. */
interface OutgoingQueuePort {
    /** Добавляет пакет в очередь. */
    suspend fun enqueue(record: PendingAckRecord)

    /** Возвращает следующий элемент очереди или `null`. */
    suspend fun dequeue(): PendingAckRecord?

    /** Возвращает содержимое очереди. */
    suspend fun list(): List<PendingAckRecord>
}

/** Порт runtime-хранилища пакетов, ожидающих ACK. */
interface PendingAckRepositoryPort {
    /** Сохраняет запись ожидания ACK. */
    suspend fun save(record: PendingAckRecord): PendingAckRecord

    /** Ищет запись по packetId. */
    suspend fun findByPacketId(packetId: String): PendingAckRecord?

    /** Возвращает записи с истекшим сроком retry. */
    suspend fun listExpired(now: Instant): List<PendingAckRecord>

    /** Удаляет запись по packetId. */
    suspend fun remove(packetId: String)

    /** Возвращает все ожидаемые ACK. */
    suspend fun list(): List<PendingAckRecord>
}
