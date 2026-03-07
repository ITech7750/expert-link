package org.expert.link.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroupChat(record: GroupChatRecord)

    @Query("SELECT * FROM group_chat WHERE chatId = :chatId")
    suspend fun findGroupChat(chatId: String): GroupChatRecord?

    @Query("SELECT * FROM group_chat ORDER BY updatedAt DESC")
    suspend fun listGroupChats(): List<GroupChatRecord>

    @Query("DELETE FROM group_chat WHERE chatId = :chatId")
    suspend fun removeGroupChat(chatId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChatMembers(records: List<ChatMemberRecord>)

    @Query("DELETE FROM chat_member WHERE chatId = :chatId")
    suspend fun deleteChatMembers(chatId: String)

    @Query("DELETE FROM chat_member WHERE chatId = :chatId AND peerId = :peerId")
    suspend fun deleteChatMember(chatId: String, peerId: String)

    @Query("SELECT * FROM chat_member WHERE chatId = :chatId ORDER BY joinedAt ASC")
    suspend fun listChatMembers(chatId: String): List<ChatMemberRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThread(record: ThreadRecord)

    @Query("SELECT * FROM thread_metadata WHERE threadId = :threadId")
    suspend fun findThread(threadId: String): ThreadRecord?

    @Query(
        """
        SELECT * FROM thread_metadata
        WHERE chatId = :chatId AND rootMessageId = :rootMessageId
        LIMIT 1
        """,
    )
    suspend fun findThreadByRootMessage(chatId: String, rootMessageId: String): ThreadRecord?

    @Query("SELECT * FROM thread_metadata WHERE chatId = :chatId ORDER BY updatedAt DESC")
    suspend fun listThreadsByChat(chatId: String): List<ThreadRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThreadMessage(record: ThreadMessageRecord)

    @Query("SELECT * FROM thread_message WHERE threadId = :threadId ORDER BY createdAt ASC")
    suspend fun listThreadMessages(threadId: String): List<ThreadMessageRecord>

    @Query(
        """
        SELECT * FROM thread_message
        WHERE chatId = :chatId AND rootMessageId = :rootMessageId
        ORDER BY createdAt ASC
        """,
    )
    suspend fun listThreadMessagesByRoot(chatId: String, rootMessageId: String): List<ThreadMessageRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroupEvent(record: GroupEventRecord)

    @Query(
        """
        SELECT * FROM group_event
        WHERE chatId = :chatId
        ORDER BY createdAt DESC
        LIMIT :limit
        """,
    )
    suspend fun listGroupEvents(chatId: String, limit: Int): List<GroupEventRecord>
}
