package org.expert.link.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        LocalProfileRecord::class,
        PairedPeerRecord::class,
        PairingSessionRecord::class,
        BlockedPeerRecord::class,
        ConversationRecord::class,
        MessageRecord::class,
        FileTransferRecord::class,
        CallSessionRecord::class,
        CallRoomRecord::class,
        CallParticipantRecord::class,
        CallEventRecord::class,
        GroupChatRecord::class,
        ChatMemberRecord::class,
        ThreadRecord::class,
        ThreadMessageRecord::class,
        GroupEventRecord::class,
        EventLogRecord::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(ExpertLinkDatabaseConstructor::class)
abstract class ExpertLinkDatabase : RoomDatabase() {
    abstract fun identityDao(): IdentityDao
    abstract fun messagingDao(): MessagingDao
    abstract fun groupDao(): GroupDao
    abstract fun transferDao(): TransferDao
    abstract fun callDao(): CallDao
    abstract fun eventLogDao(): EventLogDao
}

@Suppress("KotlinNoActualForExpect")
expect object ExpertLinkDatabaseConstructor : RoomDatabaseConstructor<ExpertLinkDatabase> {
    override fun initialize(): ExpertLinkDatabase
}
