package org.expert.link.database

import org.expert.link.mesh.domain.model.messaging.ChatMember
import org.expert.link.mesh.domain.model.messaging.ChatThread
import org.expert.link.mesh.domain.model.messaging.GroupChat
import org.expert.link.mesh.domain.model.messaging.GroupChatEvent
import org.expert.link.mesh.domain.model.messaging.ThreadMessage
import org.expert.link.mesh.domain.port.repository.ChatMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.GroupChatRepositoryPort
import org.expert.link.mesh.domain.port.repository.GroupEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.ThreadMessageRepositoryPort
import org.expert.link.mesh.domain.port.repository.ThreadRepositoryPort

internal class RoomGroupChatRepository(
    private val groupDao: GroupDao,
) : GroupChatRepositoryPort {
    override suspend fun save(groupChat: GroupChat): GroupChat {
        groupDao.upsertGroupChat(
            GroupChatRecord(
                chatId = groupChat.chatId,
                title = groupChat.title,
                updatedAt = groupChat.updatedAt.toString(),
                lastMessageId = groupChat.lastMessageId,
                payloadJson = encodePayload(groupChat),
            ),
        )
        return groupChat
    }

    override suspend fun findByChatId(chatId: String): GroupChat? =
        groupDao.findGroupChat(chatId)?.let { decodePayload(it.payloadJson) }

    override suspend fun list(): List<GroupChat> =
        groupDao.listGroupChats().map { decodePayload(it.payloadJson) }

    override suspend fun remove(chatId: String) {
        groupDao.removeGroupChat(chatId)
    }
}

internal class RoomChatMemberRepository(
    private val groupDao: GroupDao,
) : ChatMemberRepositoryPort {
    override suspend fun replace(chatId: String, members: List<ChatMember>) {
        groupDao.deleteChatMembers(chatId)
        groupDao.upsertChatMembers(
            members.map { member ->
                ChatMemberRecord(
                    chatId = chatId,
                    peerId = member.peerId,
                    joinedAt = member.joinedAt.toString(),
                    payloadJson = encodePayload(member),
                )
            },
        )
    }

    override suspend fun listByChatId(chatId: String): List<ChatMember> =
        groupDao.listChatMembers(chatId).map { decodePayload(it.payloadJson) }

    override suspend fun remove(chatId: String, peerId: String) {
        groupDao.deleteChatMember(chatId, peerId)
    }
}

internal class RoomThreadRepository(
    private val groupDao: GroupDao,
) : ThreadRepositoryPort {
    override suspend fun save(thread: ChatThread): ChatThread {
        groupDao.upsertThread(
            ThreadRecord(
                threadId = thread.threadId,
                chatId = thread.chatId,
                rootMessageId = thread.rootMessageId,
                updatedAt = thread.updatedAt.toString(),
                payloadJson = encodePayload(thread),
            ),
        )
        return thread
    }

    override suspend fun findByThreadId(threadId: String): ChatThread? =
        groupDao.findThread(threadId)?.let { decodePayload(it.payloadJson) }

    override suspend fun findByRootMessage(chatId: String, rootMessageId: String): ChatThread? =
        groupDao.findThreadByRootMessage(chatId, rootMessageId)?.let { decodePayload(it.payloadJson) }

    override suspend fun listByChatId(chatId: String): List<ChatThread> =
        groupDao.listThreadsByChat(chatId).map { decodePayload(it.payloadJson) }
}

internal class RoomThreadMessageRepository(
    private val groupDao: GroupDao,
) : ThreadMessageRepositoryPort {
    override suspend fun save(threadMessage: ThreadMessage): ThreadMessage {
        groupDao.upsertThreadMessage(
            ThreadMessageRecord(
                messageId = threadMessage.messageId,
                threadId = threadMessage.threadId,
                chatId = threadMessage.chatId,
                rootMessageId = threadMessage.rootMessageId,
                createdAt = threadMessage.createdAt.toString(),
                payloadJson = encodePayload(threadMessage),
            ),
        )
        return threadMessage
    }

    override suspend fun listByThreadId(threadId: String): List<ThreadMessage> =
        groupDao.listThreadMessages(threadId).map { decodePayload(it.payloadJson) }

    override suspend fun listByRootMessage(chatId: String, rootMessageId: String): List<ThreadMessage> =
        groupDao.listThreadMessagesByRoot(chatId, rootMessageId).map { decodePayload(it.payloadJson) }
}

internal class RoomGroupEventRepository(
    private val groupDao: GroupDao,
) : GroupEventRepositoryPort {
    override suspend fun append(event: GroupChatEvent): GroupChatEvent {
        groupDao.upsertGroupEvent(
            GroupEventRecord(
                eventId = event.eventId,
                chatId = event.chatId,
                createdAt = event.createdAt.toString(),
                payloadJson = encodePayload(event),
            ),
        )
        return event
    }

    override suspend fun listByChatId(chatId: String, limit: Int): List<GroupChatEvent> =
        groupDao.listGroupEvents(chatId, limit)
            .asReversed()
            .map { decodePayload(it.payloadJson) }
}
