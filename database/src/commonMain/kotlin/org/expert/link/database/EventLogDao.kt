package org.expert.link.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EventLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEventLog(record: EventLogRecord)

    @Query("SELECT * FROM event_log ORDER BY createdAt DESC LIMIT :limit")
    suspend fun listRecentEventLogs(limit: Int): List<EventLogRecord>
}
