package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.messaging.ChatSummary
import org.expert.link.mesh.domain.model.messaging.ChatType
import org.expert.link.mesh.domain.model.messaging.Conversation
import org.expert.link.mesh.domain.model.messaging.GroupChat
import org.expert.link.mesh.domain.model.messaging.GroupChatEvent
import org.expert.link.mesh.domain.model.messaging.GroupEventType
import org.expert.link.mesh.domain.port.repository.ChatMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.ConversationRepositoryPort
import org.expert.link.mesh.domain.port.repository.GroupChatRepositoryPort
import org.expert.link.mesh.domain.port.repository.GroupEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.MessageRepositoryPort

/**
 * Application-сервис групповых чатов.
 *
 * Инкапсулирует жизненный цикл группы: создание, изменение состава участников,
 * переименование и чтение системных событий.
 */
class GroupChatService(
    private val localProfileService: LocalProfileService,
    private val conversationRepositoryPort: ConversationRepositoryPort,
    private val messageRepositoryPort: MessageRepositoryPort,
    private val groupChatRepositoryPort: GroupChatRepositoryPort,
    private val chatMemberRepositoryPort: ChatMemberRepositoryPort,
    private val groupEventRepositoryPort: GroupEventRepositoryPort,
    private val chatMessagingService: ChatMessagingService,
    private val eventLogService: EventLogService,
    private val nodeMetricsService: NodeMetricsService,
) {
    /** Создаёт новый групповой чат. */
    suspend fun createGroupChat(title: String, description: String?, participantPeerIds: Set<String>): GroupChat {
        val conversation = chatMessagingService.createGroupConversation(title, description, participantPeerIds)
        val groupChat = syncFromConversation(conversation)
        recordGroupEvent(
            chatId = groupChat.chatId,
            type = GroupEventType.GROUP_CREATED,
            text = "Группа создана",
        )
        nodeMetricsService.increment("group.created")
        return groupChat
    }

    /** Переименовывает групповой чат. */
    suspend fun renameGroupChat(chatId: String, title: String): GroupChat? {
        val updatedConversation = chatMessagingService.renameConversation(chatId, title) ?: return null
        val groupChat = syncFromConversation(updatedConversation)
        recordGroupEvent(
            chatId = chatId,
            type = GroupEventType.GROUP_RENAMED,
            text = "Название группы изменено",
            attributes = mapOf("title" to title),
        )
        nodeMetricsService.increment("group.renamed")
        return groupChat
    }

    /** Добавляет участников в группу. */
    suspend fun addParticipants(chatId: String, peers: List<PeerIdentity>): GroupChat? {
        var conversation = conversationRepositoryPort.findByConversationId(chatId) ?: return null
        peers.forEach { peer ->
            conversation = chatMessagingService.addParticipant(chatId, peer) ?: conversation
            recordGroupEvent(
                chatId = chatId,
                type = GroupEventType.MEMBER_ADDED,
                text = "Участник добавлен",
                subjectPeerId = peer.peerId,
                attributes = mapOf("displayName" to peer.displayName),
            )
        }
        nodeMetricsService.increment("group.member_added", peers.size.toLong())
        return syncFromConversation(conversation)
    }

    /** Удаляет участника из группы. */
    suspend fun removeParticipant(chatId: String, peerId: String): GroupChat? {
        val conversation = chatMessagingService.removeParticipant(chatId, peerId) ?: return null
        recordGroupEvent(
            chatId = chatId,
            type = GroupEventType.MEMBER_REMOVED,
            text = "Участник удалён",
            subjectPeerId = peerId,
        )
        nodeMetricsService.increment("group.member_removed")
        return syncFromConversation(conversation)
    }

    /** Возвращает группу по id. */
    suspend fun groupChat(chatId: String): GroupChat? {
        groupChatRepositoryPort.findByChatId(chatId)?.let { return it }
        val conversation = conversationRepositoryPort.findByConversationId(chatId) ?: return null
        if (conversation.chatType != ChatType.GROUP) return null
        return syncFromConversation(conversation)
    }

    /** Возвращает список групповых чатов. */
    suspend fun listGroupChats(): List<GroupChat> {
        val groups = mutableListOf<GroupChat>()
        conversationRepositoryPort.list()
            .filter { it.chatType == ChatType.GROUP }
            .forEach { conversation ->
                groups += syncFromConversation(conversation)
            }
        return groups.sortedByDescending { it.updatedAt }
    }

    /** Возвращает сводки чатов для списка. */
    suspend fun chatSummaries(): List<ChatSummary> {
        val summaries = mutableListOf<ChatSummary>()
        conversationRepositoryPort.list().forEach { conversation ->
            val lastMessage = conversation.lastMessageId?.let { messageId ->
                messageRepositoryPort.findByMessageId(messageId)
            }
            summaries += ChatSummary(
                chatId = conversation.conversationId,
                chatType = conversation.chatType,
                title = conversation.title,
                lastMessageId = conversation.lastMessageId,
                lastMessagePreview = lastMessage?.body?.take(80),
                unreadCount = conversation.unreadCount,
                participantCount = conversation.participantPeerIds.size,
                updatedAt = conversation.updatedAt,
            )
        }
        return summaries.sortedByDescending { it.updatedAt }
    }

    /** Возвращает историю сообщений группы. */
    suspend fun groupMessages(chatId: String) = messageRepositoryPort.listByConversation(chatId)

    /** Возвращает последние системные события группы. */
    suspend fun groupEvents(chatId: String, limit: Int = 100): List<GroupChatEvent> {
        return groupEventRepositoryPort.listByChatId(chatId, limit)
    }

    private suspend fun syncFromConversation(conversation: Conversation): GroupChat {
        conversationRepositoryPort.save(conversation)
        val groupChat = GroupChat(
            chatId = conversation.conversationId,
            title = conversation.title,
            description = conversation.description,
            createdByPeerId = conversation.createdByPeerId,
            members = conversation.members,
            createdAt = conversation.createdAt,
            updatedAt = conversation.updatedAt,
            lastMessageId = conversation.lastMessageId,
            pinned = conversation.pinned,
            archived = conversation.archived,
        )
        groupChatRepositoryPort.save(groupChat)
        chatMemberRepositoryPort.replace(conversation.conversationId, conversation.members)
        return groupChat
    }

    private suspend fun recordGroupEvent(
        chatId: String,
        type: GroupEventType,
        text: String,
        subjectPeerId: String? = null,
        attributes: Map<String, String> = emptyMap(),
    ) {
        val actorPeerId = localProfileService.require().peerId
        groupEventRepositoryPort.append(
            GroupChatEvent(
                eventId = newId("group-event"),
                chatId = chatId,
                eventType = type,
                actorPeerId = actorPeerId,
                subjectPeerId = subjectPeerId,
                text = text,
                attributes = attributes,
                createdAt = now(),
            ),
        )
        eventLogService.log(
            category = EventCategory.MESSAGING,
            level = EventLevel.INFO,
            message = "Group event $type",
            peerId = subjectPeerId,
            attributes = buildMap {
                put("chatId", chatId)
                put("eventType", type.name)
                putAll(attributes)
            },
        )
    }
}
