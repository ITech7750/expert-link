package org.expert.link.mesh.domain.port.repository

import org.expert.link.mesh.domain.model.messaging.ChatMember
import org.expert.link.mesh.domain.model.messaging.ChatThread
import org.expert.link.mesh.domain.model.messaging.GroupChat
import org.expert.link.mesh.domain.model.messaging.GroupChatEvent
import org.expert.link.mesh.domain.model.messaging.ThreadMessage

/** Порт хранения групповых чатов. */
interface GroupChatRepositoryPort {
    /** Сохраняет групповой чат. */
    suspend fun save(groupChat: GroupChat): GroupChat

    /** Ищет групповой чат по chatId. */
    suspend fun findByChatId(chatId: String): GroupChat?

    /** Возвращает все группы. */
    suspend fun list(): List<GroupChat>

    /** Удаляет группу. */
    suspend fun remove(chatId: String)
}

/** Порт хранения участников группового чата. */
interface ChatMemberRepositoryPort {
    /** Перезаписывает весь список участников группы. */
    suspend fun replace(chatId: String, members: List<ChatMember>)

    /** Возвращает участников группы. */
    suspend fun listByChatId(chatId: String): List<ChatMember>

    /** Удаляет участника из группы. */
    suspend fun remove(chatId: String, peerId: String)
}

/** Порт хранения метаданных тредов. */
interface ThreadRepositoryPort {
    /** Сохраняет тред. */
    suspend fun save(thread: ChatThread): ChatThread

    /** Ищет тред по id. */
    suspend fun findByThreadId(threadId: String): ChatThread?

    /** Ищет тред по root-сообщению. */
    suspend fun findByRootMessage(chatId: String, rootMessageId: String): ChatThread?

    /** Возвращает треды чата. */
    suspend fun listByChatId(chatId: String): List<ChatThread>
}

/** Порт хранения сообщений тредов. */
interface ThreadMessageRepositoryPort {
    /** Сохраняет сообщение треда. */
    suspend fun save(threadMessage: ThreadMessage): ThreadMessage

    /** Возвращает сообщения треда по threadId. */
    suspend fun listByThreadId(threadId: String): List<ThreadMessage>

    /** Возвращает сообщения треда по root-сообщению. */
    suspend fun listByRootMessage(chatId: String, rootMessageId: String): List<ThreadMessage>
}

/** Порт хранения системных событий группового чата. */
interface GroupEventRepositoryPort {
    /** Добавляет событие группы. */
    suspend fun append(event: GroupChatEvent): GroupChatEvent

    /** Возвращает последние события группы. */
    suspend fun listByChatId(chatId: String, limit: Int = 100): List<GroupChatEvent>
}

