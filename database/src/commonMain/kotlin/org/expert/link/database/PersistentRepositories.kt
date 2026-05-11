package org.expert.link.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.expert.link.mesh.domain.port.repository.PersistentRepositoryBundle

fun buildPersistentRepositoryBundle(
    builder: RoomDatabase.Builder<ExpertLinkDatabase>,
): PersistentRepositoryBundle {
    val database = builder
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
    val inventoryDao = inventoryDao()
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
        organizationRepositoryPort = RoomOrganizationRepository(inventoryDao),
        organizationMemberRepositoryPort = RoomOrganizationMemberRepository(inventoryDao),
        roleRepositoryPort = RoomRoleRepository(inventoryDao),
        inventoryCategoryRepositoryPort = RoomInventoryCategoryRepository(inventoryDao),
        inventorySubcategoryRepositoryPort = RoomInventorySubcategoryRepository(inventoryDao),
        inventoryTagRepositoryPort = RoomInventoryTagRepository(inventoryDao),
        inventoryAttributeDefinitionRepositoryPort = RoomInventoryAttributeDefinitionRepository(inventoryDao),
        inventoryCategoryTemplateRepositoryPort = RoomInventoryCategoryTemplateRepository(inventoryDao),
        inventoryLocationRepositoryPort = RoomInventoryLocationRepository(inventoryDao),
        inventoryOwnerRepositoryPort = RoomInventoryOwnerRepository(inventoryDao),
        inventoryDepartmentRepositoryPort = RoomInventoryDepartmentRepository(inventoryDao),
        inventoryCostCenterRepositoryPort = RoomInventoryCostCenterRepository(inventoryDao),
        inventoryLegalHolderRepositoryPort = RoomInventoryLegalHolderRepository(inventoryDao),
        inventorySupplierRepositoryPort = RoomInventorySupplierRepository(inventoryDao),
        inventoryFundingSourceRepositoryPort = RoomInventoryFundingSourceRepository(inventoryDao),
        inventoryItemRepositoryPort = RoomInventoryItemRepository(inventoryDao),
        inventorySessionRepositoryPort = RoomInventorySessionRepository(inventoryDao),
        inventorySessionMemberRepositoryPort = RoomInventorySessionMemberRepository(inventoryDao),
        inventoryReviewRepositoryPort = RoomInventoryReviewRepository(inventoryDao),
        inventoryCommentRepositoryPort = RoomInventoryCommentRepository(inventoryDao),
        inventoryAttachmentRepositoryPort = RoomInventoryAttachmentRepository(inventoryDao),
        inventoryIncidentRepositoryPort = RoomInventoryIncidentRepository(inventoryDao),
        inventoryAlertRepositoryPort = RoomInventoryAlertRepository(inventoryDao),
        inventoryReminderRepositoryPort = RoomInventoryReminderRepository(inventoryDao),
        inventoryThresholdRuleRepositoryPort = RoomInventoryThresholdRuleRepository(inventoryDao),
        inventoryDeadlineRuleRepositoryPort = RoomInventoryDeadlineRuleRepository(inventoryDao),
        inventoryChangeLogRepositoryPort = RoomInventoryChangeLogRepository(inventoryDao),
        inventoryDashboardRepositoryPort = RoomInventoryDashboardRepository(inventoryDao),
        inventoryCodeBindingRepositoryPort = RoomInventoryCodeBindingRepository(inventoryDao),
        inventoryCodeRepositoryPort = RoomInventoryCodeRepository(inventoryDao),
        inventoryLabelTemplateRepositoryPort = RoomInventoryLabelTemplateRepository(inventoryDao),
        inventoryLabelRepositoryPort = RoomInventoryLabelRepository(inventoryDao),
        inventoryPrintTaskRepositoryPort = RoomInventoryPrintTaskRepository(inventoryDao),
        inventoryScanEventRepositoryPort = RoomInventoryScanEventRepository(inventoryDao),
        inventoryRevisionRepositoryPort = RoomInventoryRevisionRepository(inventoryDao),
        inventoryConflictRepositoryPort = RoomInventoryConflictRepository(inventoryDao),
        inventoryEventRepositoryPort = RoomInventoryEventRepository(inventoryDao),
        inventoryExportRepositoryPort = RoomInventoryExportRepository(inventoryDao),
        inventoryQrCodeRepositoryPort = RoomInventoryQrCodeRepository(inventoryDao),
        centralAuthSessionRepositoryPort = RoomCentralAuthSessionRepository(inventoryDao),
        centralOrganizationAccessRepositoryPort = RoomCentralOrganizationAccessRepository(inventoryDao),
        centralSyncStateRepositoryPort = RoomCentralSyncStateRepository(inventoryDao),
        centralOrganizationWorkspaceRepositoryPort = RoomCentralOrganizationWorkspaceRepository(inventoryDao),
    )
}
