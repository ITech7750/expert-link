package org.expert.link.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessagingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(record: ConversationRecord)

    @Query("SELECT * FROM conversation WHERE conversationId = :conversationId")
    suspend fun findConversation(conversationId: String): ConversationRecord?

    @Query("SELECT * FROM conversation WHERE participantKey = :participantKey LIMIT 1")
    suspend fun findConversationByParticipantKey(participantKey: String): ConversationRecord?

    @Query("SELECT * FROM conversation ORDER BY updatedAt DESC")
    suspend fun listConversations(): List<ConversationRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(record: MessageRecord)

    @Query("SELECT * FROM message WHERE messageId = :messageId")
    suspend fun findMessage(messageId: String): MessageRecord?

    @Query("SELECT * FROM message WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun listMessagesByConversation(conversationId: String): List<MessageRecord>

    @Query(
        """
        SELECT * FROM message
        WHERE conversationId = :conversationId AND threadRootMessageId = :rootMessageId
        ORDER BY createdAt ASC
        """,
    )
    suspend fun listMessagesByThread(conversationId: String, rootMessageId: String): List<MessageRecord>
}
