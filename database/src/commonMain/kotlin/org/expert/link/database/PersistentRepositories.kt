package org.expert.link.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.expert.link.mesh.domain.port.repository.PersistentRepositoryBundle

fun buildPersistentRepositoryBundle(
    builder: RoomDatabase.Builder<ExpertLinkDatabase>,
): PersistentRepositoryBundle {
    val database = builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    return database.asPersistentRepositoryBundle()
}

fun ExpertLinkDatabase.asPersistentRepositoryBundle(): PersistentRepositoryBundle {
    val identityDao = identityDao()
    val messagingDao = messagingDao()
    val groupDao = groupDao()
    val transferDao = transferDao()
    val callDao = callDao()
    val eventLogDao = eventLogDao()
    return PersistentRepositoryBundle(
        localProfileRepositoryPort = RoomLocalProfileRepository(identityDao),
        peerRepositoryPort = RoomPeerRepository(identityDao),
        pairingSessionRepositoryPort = RoomPairingSessionRepository(identityDao),
        blockListRepositoryPort = RoomBlockListRepository(identityDao),
        conversationRepositoryPort = RoomConversationRepository(messagingDao),
        messageRepositoryPort = RoomMessageRepository(messagingDao),
        fileTransferRepositoryPort = RoomFileTransferRepository(transferDao),
        callSessionRepositoryPort = RoomCallSessionRepository(callDao),
        callRoomRepositoryPort = RoomCallRoomRepository(callDao),
        callParticipantRepositoryPort = RoomCallParticipantRepository(callDao),
        callEventRepositoryPort = RoomCallEventRepository(callDao),
        groupChatRepositoryPort = RoomGroupChatRepository(groupDao),
        chatMemberRepositoryPort = RoomChatMemberRepository(groupDao),
        threadRepositoryPort = RoomThreadRepository(groupDao),
        threadMessageRepositoryPort = RoomThreadMessageRepository(groupDao),
        groupEventRepositoryPort = RoomGroupEventRepository(groupDao),
        eventLogRepositoryPort = RoomEventLogRepository(eventLogDao),
    )
}
