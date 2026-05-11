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
        OrganizationRecord::class,
        OrganizationMemberRecord::class,
        RoleRecord::class,
        InventoryCategoryRecord::class,
        InventoryLocationRecord::class,
        InventoryItemRecord::class,
        InventorySessionRecord::class,
        InventorySessionMemberRecord::class,
        InventoryReviewRecord::class,
        InventoryCommentRecord::class,
        InventoryAttachmentRecord::class,
        InventoryEventRecord::class,
        InventoryExportRecord::class,
        InventoryQrCodeRecord::class,
        InventorySubcategoryRecord::class,
        InventoryTagRecord::class,
        InventoryAttributeDefinitionRecord::class,
        InventoryCategoryTemplateRecord::class,
        InventoryOwnerRecord::class,
        InventoryDepartmentRecord::class,
        InventoryCostCenterRecord::class,
        InventoryLegalHolderRecord::class,
        InventorySupplierRecord::class,
        InventoryFundingSourceRecord::class,
        InventoryIncidentRecord::class,
        InventoryAlertRecord::class,
        InventoryReminderRecord::class,
        InventoryRuleThresholdRecord::class,
        InventoryDeadlineRuleRecord::class,
        InventoryChangeLogRecord::class,
        InventoryDashboardSnapshotRecord::class,
        InventoryCodeBindingRecord::class,
        InventoryCodeRecord::class,
        InventoryLabelTemplateRecord::class,
        InventoryLabelRecord::class,
        InventoryPrintTaskRecord::class,
        InventoryScanEventRecord::class,
        InventoryRevisionRecord::class,
        InventoryConflictRecord::class,
        CentralAuthSessionRecord::class,
        CentralOrganizationAccessRecord::class,
        CentralSyncStateRecord::class,
        CentralOrganizationWorkspaceRecord::class,
    ],
    version = 6,
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
    abstract fun inventoryDao(): InventoryDao
}

@Suppress("KotlinNoActualForExpect")
expect object ExpertLinkDatabaseConstructor : RoomDatabaseConstructor<ExpertLinkDatabase> {
    override fun initialize(): ExpertLinkDatabase
}
