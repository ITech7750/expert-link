package org.expert.link.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransfer(record: FileTransferRecord)

    @Query("SELECT * FROM file_transfer WHERE transferId = :transferId")
    suspend fun findTransfer(transferId: String): FileTransferRecord?

    @Query("SELECT * FROM file_transfer ORDER BY updatedAt DESC")
    suspend fun listTransfers(): List<FileTransferRecord>
}

@Dao
interface CallDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCallSession(record: CallSessionRecord)

    @Query("SELECT * FROM call_session WHERE callId = :callId")
    suspend fun findCallSession(callId: String): CallSessionRecord?

    @Query("SELECT * FROM call_session ORDER BY updatedAt DESC")
    suspend fun listCallSessions(): List<CallSessionRecord>

    @Query("SELECT * FROM call_session WHERE roomId = :roomId ORDER BY updatedAt DESC")
    suspend fun listCallSessionsByRoom(roomId: String): List<CallSessionRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCallRoom(record: CallRoomRecord)

    @Query("SELECT * FROM call_room WHERE roomId = :roomId")
    suspend fun findCallRoom(roomId: String): CallRoomRecord?

    @Query("SELECT * FROM call_room ORDER BY updatedAt DESC")
    suspend fun listCallRooms(): List<CallRoomRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCallParticipants(records: List<CallParticipantRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCallParticipant(record: CallParticipantRecord)

    @Query("DELETE FROM call_participant WHERE callId = :callId")
    suspend fun deleteCallParticipants(callId: String)

    @Query("SELECT * FROM call_participant WHERE callId = :callId ORDER BY updatedAt ASC")
    suspend fun listCallParticipants(callId: String): List<CallParticipantRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCallEvent(record: CallEventRecord)

    @Query(
        """
        SELECT * FROM call_event
        WHERE callId = :callId
        ORDER BY createdAt DESC
        LIMIT :limit
        """,
    )
    suspend fun listCallEvents(callId: String, limit: Int): List<CallEventRecord>
}
