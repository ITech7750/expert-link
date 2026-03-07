package org.expert.link.mesh.infrastructure.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.expert.link.mesh.application.mapper.GroupChatEntityMapper
import org.expert.link.mesh.application.mapper.GroupEventEntityMapper
import org.expert.link.mesh.application.mapper.ThreadEntityMapper
import org.expert.link.mesh.application.mapper.ThreadMessageEntityMapper
import org.expert.link.mesh.domain.entity.ChatMemberEntity
import org.expert.link.mesh.domain.model.messaging.ChatMember
import org.expert.link.mesh.domain.model.messaging.ChatMemberRole
import org.expert.link.mesh.domain.model.messaging.ChatThread
import org.expert.link.mesh.domain.model.messaging.GroupChat
import org.expert.link.mesh.domain.model.messaging.GroupChatEvent
import org.expert.link.mesh.domain.model.messaging.ThreadMessage
import org.expert.link.mesh.domain.port.repository.ChatMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.GroupChatRepositoryPort
import org.expert.link.mesh.domain.port.repository.GroupEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.ThreadMessageRepositoryPort
import org.expert.link.mesh.domain.port.repository.ThreadRepositoryPort
import kotlinx.datetime.Instant

/** In-memory хранилище групповых чатов. */
class InMemoryGroupChatRepositoryAdapter : GroupChatRepositoryPort {
    private val mutex = Mutex()
    private val chats = linkedMapOf<String, org.expert.link.mesh.domain.entity.GroupChatEntity>()

    override suspend fun save(groupChat: GroupChat): GroupChat = mutex.withLock {
        val entity = GroupChatEntityMapper.toEntity(groupChat)
        chats[groupChat.chatId] = entity
        GroupChatEntityMapper.fromEntity(entity)
    }

    override suspend fun findByChatId(chatId: String): GroupChat? = mutex.withLock {
        chats[chatId]?.let(GroupChatEntityMapper::fromEntity)
    }

    override suspend fun list(): List<GroupChat> = mutex.withLock {
        chats.values.map(GroupChatEntityMapper::fromEntity)
    }

    override suspend fun remove(chatId: String) {
        mutex.withLock { chats.remove(chatId) }
    }
}

/** In-memory хранилище участников группы. */
class InMemoryChatMemberRepositoryAdapter : ChatMemberRepositoryPort {
    private val mutex = Mutex()
    private val membersByChat = linkedMapOf<String, MutableList<ChatMemberEntity>>()

    override suspend fun replace(chatId: String, members: List<ChatMember>) {
        mutex.withLock {
            membersByChat[chatId] = members.map {
                ChatMemberEntity(
                    chatId = chatId,
                    peerId = it.peerId,
                    displayName = it.displayName,
                    role = it.role.name,
                    joinedAt = it.joinedAt.toString(),
                )
            }.toMutableList()
        }
    }

    override suspend fun listByChatId(chatId: String): List<ChatMember> = mutex.withLock {
        membersByChat[chatId].orEmpty().map {
            ChatMember(
                peerId = it.peerId,
                displayName = it.displayName,
                role = ChatMemberRole.valueOf(it.role),
                joinedAt = Instant.parse(it.joinedAt),
            )
        }
    }

    override suspend fun remove(chatId: String, peerId: String) {
        mutex.withLock {
            val members = membersByChat[chatId]
            if (members != null) {
                members.removeIf { it.peerId == peerId }
            }
        }
    }
}

/** In-memory хранилище метаданных тредов. */
class InMemoryThreadRepositoryAdapter : ThreadRepositoryPort {
    private val mutex = Mutex()
    private val threads = linkedMapOf<String, org.expert.link.mesh.domain.entity.ThreadEntity>()

    override suspend fun save(thread: ChatThread): ChatThread = mutex.withLock {
        val entity = ThreadEntityMapper.toEntity(thread)
        threads[thread.threadId] = entity
        ThreadEntityMapper.fromEntity(entity)
    }

    override suspend fun findByThreadId(threadId: String): ChatThread? = mutex.withLock {
        threads[threadId]?.let(ThreadEntityMapper::fromEntity)
    }

    override suspend fun findByRootMessage(chatId: String, rootMessageId: String): ChatThread? = mutex.withLock {
        threads.values.firstOrNull { it.chatId == chatId && it.rootMessageId == rootMessageId }?.let(ThreadEntityMapper::fromEntity)
    }

    override suspend fun listByChatId(chatId: String): List<ChatThread> = mutex.withLock {
        threads.values.filter { it.chatId == chatId }.map(ThreadEntityMapper::fromEntity)
    }
}

/** In-memory хранилище сообщений тредов. */
class InMemoryThreadMessageRepositoryAdapter : ThreadMessageRepositoryPort {
    private val mutex = Mutex()
    private val messages = linkedMapOf<String, org.expert.link.mesh.domain.entity.ThreadMessageEntity>()

    override suspend fun save(threadMessage: ThreadMessage): ThreadMessage = mutex.withLock {
        val entity = ThreadMessageEntityMapper.toEntity(threadMessage)
        messages[threadMessage.messageId] = entity
        ThreadMessageEntityMapper.fromEntity(entity)
    }

    override suspend fun listByThreadId(threadId: String): List<ThreadMessage> = mutex.withLock {
        messages.values.filter { it.threadId == threadId }.map(ThreadMessageEntityMapper::fromEntity)
    }

    override suspend fun listByRootMessage(chatId: String, rootMessageId: String): List<ThreadMessage> = mutex.withLock {
        messages.values
            .filter { it.chatId == chatId && it.rootMessageId == rootMessageId }
            .map(ThreadMessageEntityMapper::fromEntity)
    }
}

/** In-memory хранилище системных событий группы. */
class InMemoryGroupEventRepositoryAdapter : GroupEventRepositoryPort {
    private val mutex = Mutex()
    private val eventsByChat = linkedMapOf<String, ArrayDeque<org.expert.link.mesh.domain.entity.GroupEventEntity>>()

    override suspend fun append(event: GroupChatEvent): GroupChatEvent = mutex.withLock {
        val entity = GroupEventEntityMapper.toEntity(event)
        val queue = eventsByChat.getOrPut(event.chatId) { ArrayDeque() }
        queue.addLast(entity)
        while (queue.size > 1_000) {
            queue.removeFirst()
        }
        GroupEventEntityMapper.fromEntity(entity)
    }

    override suspend fun listByChatId(chatId: String, limit: Int): List<GroupChatEvent> = mutex.withLock {
        val items = eventsByChat[chatId]?.map(GroupEventEntityMapper::fromEntity).orEmpty()
        if (limit >= items.size) items else items.subList(items.size - limit, items.size)
    }
}
