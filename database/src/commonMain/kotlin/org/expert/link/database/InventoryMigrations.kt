package org.expert.link.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `organization` (
                `organizationId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`organizationId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_organization_updatedAt` ON `organization` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `organization_member` (
                `organizationId` TEXT NOT NULL,
                `peerId` TEXT NOT NULL,
                `roleKey` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`organizationId`, `peerId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_organization_member_peerId` ON `organization_member` (`peerId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_organization_member_updatedAt` ON `organization_member` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `role` (
                `roleId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`roleId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_role_organizationId` ON `role` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_role_name` ON `role` (`name`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_category` (
                `categoryId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`categoryId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_category_organizationId` ON `inventory_category` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_category_updatedAt` ON `inventory_category` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_location` (
                `locationId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`locationId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_location_organizationId` ON `inventory_location` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_location_updatedAt` ON `inventory_location` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_item` (
                `inventoryItemId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `inventoryNumber` TEXT NOT NULL,
                `qrCode` TEXT,
                `barcode` TEXT,
                `status` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `revision` INTEGER NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`inventoryItemId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_item_organizationId` ON `inventory_item` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_item_inventoryNumber` ON `inventory_item` (`inventoryNumber`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_item_qrCode` ON `inventory_item` (`qrCode`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_item_barcode` ON `inventory_item` (`barcode`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_item_status` ON `inventory_item` (`status`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_item_updatedAt` ON `inventory_item` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_session` (
                `sessionId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `periodStart` TEXT NOT NULL,
                `periodEnd` TEXT,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`sessionId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_session_organizationId` ON `inventory_session` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_session_status` ON `inventory_session` (`status`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_session_updatedAt` ON `inventory_session` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_session_member` (
                `sessionId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `peerId` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`sessionId`, `peerId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_session_member_organizationId` ON `inventory_session_member` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_session_member_peerId` ON `inventory_session_member` (`peerId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_session_member_updatedAt` ON `inventory_session_member` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_review` (
                `reviewId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `inventoryItemId` TEXT NOT NULL,
                `sessionId` TEXT,
                `status` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`reviewId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_review_organizationId` ON `inventory_review` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_review_inventoryItemId` ON `inventory_review` (`inventoryItemId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_review_sessionId` ON `inventory_review` (`sessionId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_review_status` ON `inventory_review` (`status`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_review_createdAt` ON `inventory_review` (`createdAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_comment` (
                `commentId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `inventoryItemId` TEXT NOT NULL,
                `sessionId` TEXT,
                `createdAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`commentId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_comment_organizationId` ON `inventory_comment` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_comment_inventoryItemId` ON `inventory_comment` (`inventoryItemId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_comment_sessionId` ON `inventory_comment` (`sessionId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_comment_createdAt` ON `inventory_comment` (`createdAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_attachment` (
                `attachmentId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `inventoryItemId` TEXT NOT NULL,
                `sessionId` TEXT,
                `createdAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`attachmentId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_attachment_organizationId` ON `inventory_attachment` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_attachment_inventoryItemId` ON `inventory_attachment` (`inventoryItemId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_attachment_sessionId` ON `inventory_attachment` (`sessionId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_attachment_createdAt` ON `inventory_attachment` (`createdAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_event` (
                `eventId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `entityId` TEXT NOT NULL,
                `entityType` TEXT NOT NULL,
                `eventType` TEXT NOT NULL,
                `occurredAt` TEXT NOT NULL,
                `sequence` INTEGER NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`eventId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_event_organizationId` ON `inventory_event` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_event_entityId` ON `inventory_event` (`entityId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_event_sequence` ON `inventory_event` (`sequence`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_event_occurredAt` ON `inventory_event` (`occurredAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_export` (
                `exportTaskId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`exportTaskId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_export_organizationId` ON `inventory_export` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_export_sessionId` ON `inventory_export` (`sessionId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_export_status` ON `inventory_export` (`status`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_export_updatedAt` ON `inventory_export` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_qrcode` (
                `codeId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `inventoryItemId` TEXT NOT NULL,
                `qrCode` TEXT NOT NULL,
                `barcode` TEXT,
                `createdAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`codeId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_qrcode_organizationId` ON `inventory_qrcode` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_qrcode_inventoryItemId` ON `inventory_qrcode` (`inventoryItemId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_qrcode_qrCode` ON `inventory_qrcode` (`qrCode`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_qrcode_barcode` ON `inventory_qrcode` (`barcode`)")
    }
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_subcategory` (
                `subcategoryId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `categoryId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`subcategoryId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_subcategory_organizationId` ON `inventory_subcategory` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_subcategory_categoryId` ON `inventory_subcategory` (`categoryId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_subcategory_updatedAt` ON `inventory_subcategory` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_tag` (
                `tagId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`tagId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_tag_organizationId` ON `inventory_tag` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_tag_name` ON `inventory_tag` (`name`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_attribute_definition` (
                `attributeId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `key` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`attributeId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_attribute_definition_organizationId` ON `inventory_attribute_definition` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_attribute_definition_key` ON `inventory_attribute_definition` (`key`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_category_template` (
                `templateId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `categoryId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`templateId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_category_template_organizationId` ON `inventory_category_template` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_category_template_categoryId` ON `inventory_category_template` (`categoryId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_category_template_updatedAt` ON `inventory_category_template` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_owner` (
                `ownerId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`ownerId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_owner_organizationId` ON `inventory_owner` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_owner_name` ON `inventory_owner` (`name`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_owner_updatedAt` ON `inventory_owner` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_department` (
                `departmentId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`departmentId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_department_organizationId` ON `inventory_department` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_department_name` ON `inventory_department` (`name`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_department_updatedAt` ON `inventory_department` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_cost_center` (
                `costCenterId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `code` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`costCenterId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_cost_center_organizationId` ON `inventory_cost_center` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_cost_center_code` ON `inventory_cost_center` (`code`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_cost_center_updatedAt` ON `inventory_cost_center` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_legal_holder` (
                `legalHolderId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`legalHolderId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_legal_holder_organizationId` ON `inventory_legal_holder` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_legal_holder_name` ON `inventory_legal_holder` (`name`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_legal_holder_updatedAt` ON `inventory_legal_holder` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_supplier` (
                `supplierId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`supplierId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_supplier_organizationId` ON `inventory_supplier` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_supplier_name` ON `inventory_supplier` (`name`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_supplier_updatedAt` ON `inventory_supplier` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_funding_source` (
                `fundingSourceId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`fundingSourceId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_funding_source_organizationId` ON `inventory_funding_source` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_funding_source_name` ON `inventory_funding_source` (`name`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_funding_source_updatedAt` ON `inventory_funding_source` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_incident` (
                `incidentId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `inventoryItemId` TEXT,
                `sessionId` TEXT,
                `status` TEXT NOT NULL,
                `reportedAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`incidentId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_incident_organizationId` ON `inventory_incident` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_incident_inventoryItemId` ON `inventory_incident` (`inventoryItemId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_incident_sessionId` ON `inventory_incident` (`sessionId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_incident_status` ON `inventory_incident` (`status`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_incident_reportedAt` ON `inventory_incident` (`reportedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_alert` (
                `alertId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `inventoryItemId` TEXT,
                `sessionId` TEXT,
                `createdAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`alertId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_alert_organizationId` ON `inventory_alert` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_alert_inventoryItemId` ON `inventory_alert` (`inventoryItemId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_alert_sessionId` ON `inventory_alert` (`sessionId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_alert_createdAt` ON `inventory_alert` (`createdAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_reminder` (
                `reminderId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `inventoryItemId` TEXT,
                `sessionId` TEXT,
                `status` TEXT NOT NULL,
                `dueAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`reminderId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_reminder_organizationId` ON `inventory_reminder` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_reminder_inventoryItemId` ON `inventory_reminder` (`inventoryItemId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_reminder_sessionId` ON `inventory_reminder` (`sessionId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_reminder_status` ON `inventory_reminder` (`status`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_reminder_dueAt` ON `inventory_reminder` (`dueAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_rule_threshold` (
                `ruleId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `ruleType` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`ruleId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_rule_threshold_organizationId` ON `inventory_rule_threshold` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_rule_threshold_ruleType` ON `inventory_rule_threshold` (`ruleType`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_rule_threshold_updatedAt` ON `inventory_rule_threshold` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_deadline_rule` (
                `ruleId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `target` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`ruleId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_deadline_rule_organizationId` ON `inventory_deadline_rule` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_deadline_rule_target` ON `inventory_deadline_rule` (`target`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_deadline_rule_updatedAt` ON `inventory_deadline_rule` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_change_log` (
                `changeId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `entityId` TEXT NOT NULL,
                `entityType` TEXT NOT NULL,
                `changedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`changeId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_change_log_organizationId` ON `inventory_change_log` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_change_log_entityId` ON `inventory_change_log` (`entityId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_change_log_entityType` ON `inventory_change_log` (`entityType`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_change_log_changedAt` ON `inventory_change_log` (`changedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_dashboard_snapshot` (
                `snapshotId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `generatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`snapshotId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_dashboard_snapshot_organizationId` ON `inventory_dashboard_snapshot` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_dashboard_snapshot_generatedAt` ON `inventory_dashboard_snapshot` (`generatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_code_binding` (
                `codeId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `inventoryItemId` TEXT NOT NULL,
                `codeValue` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`codeId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_code_binding_organizationId` ON `inventory_code_binding` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_code_binding_inventoryItemId` ON `inventory_code_binding` (`inventoryItemId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_code_binding_codeValue` ON `inventory_code_binding` (`codeValue`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_code_binding_createdAt` ON `inventory_code_binding` (`createdAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_scan_event` (
                `scanEventId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `inventoryItemId` TEXT,
                `scannedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`scanEventId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_scan_event_organizationId` ON `inventory_scan_event` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_scan_event_inventoryItemId` ON `inventory_scan_event` (`inventoryItemId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_scan_event_scannedAt` ON `inventory_scan_event` (`scannedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_revision` (
                `entityId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `entityType` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`entityId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_revision_organizationId` ON `inventory_revision` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_revision_entityType` ON `inventory_revision` (`entityType`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_revision_updatedAt` ON `inventory_revision` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_conflict` (
                `conflictId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `entityId` TEXT NOT NULL,
                `entityType` TEXT NOT NULL,
                `detectedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`conflictId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_conflict_organizationId` ON `inventory_conflict` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_conflict_entityId` ON `inventory_conflict` (`entityId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_conflict_detectedAt` ON `inventory_conflict` (`detectedAt`)")
    }
}

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_code` (
                `inventoryCodeId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `inventoryItemId` TEXT NOT NULL,
                `rawValue` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL,
                `createdAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`inventoryCodeId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_code_organizationId` ON `inventory_code` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_code_inventoryItemId` ON `inventory_code` (`inventoryItemId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_code_rawValue` ON `inventory_code` (`rawValue`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_code_isActive` ON `inventory_code` (`isActive`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_code_createdAt` ON `inventory_code` (`createdAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_label_template` (
                `templateId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `isDefault` INTEGER NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`templateId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_label_template_organizationId` ON `inventory_label_template` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_label_template_name` ON `inventory_label_template` (`name`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_label_template_isDefault` ON `inventory_label_template` (`isDefault`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_label_template_updatedAt` ON `inventory_label_template` (`updatedAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_label` (
                `labelId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `inventoryItemId` TEXT NOT NULL,
                `templateId` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`labelId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_label_organizationId` ON `inventory_label` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_label_inventoryItemId` ON `inventory_label` (`inventoryItemId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_label_templateId` ON `inventory_label` (`templateId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_label_createdAt` ON `inventory_label` (`createdAt`)")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_print_task` (
                `printTaskId` TEXT NOT NULL,
                `organizationId` TEXT NOT NULL,
                `templateId` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`printTaskId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_print_task_organizationId` ON `inventory_print_task` (`organizationId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_print_task_templateId` ON `inventory_print_task` (`templateId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_print_task_status` ON `inventory_print_task` (`status`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_print_task_updatedAt` ON `inventory_print_task` (`updatedAt`)")
    }
}

val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `central_auth_session` (
                `sessionId` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `username` TEXT NOT NULL,
                `displayName` TEXT NOT NULL,
                `accessToken` TEXT NOT NULL,
                `refreshToken` TEXT,
                `accessTokenExpiresAt` TEXT,
                `activeOrganizationId` TEXT,
                `lastAuthenticatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`sessionId`)
            )
            """.trimIndent(),
        )

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `central_organization_access` (
                `organizationId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `active` INTEGER NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`organizationId`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_central_organization_access_updatedAt` ON `central_organization_access` (`updatedAt`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_central_organization_access_active` ON `central_organization_access` (`active`)",
        )

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `central_sync_state` (
                `organizationId` TEXT NOT NULL,
                `connectivityMode` TEXT NOT NULL,
                `runState` TEXT NOT NULL,
                `lastUploadedSequence` INTEGER NOT NULL,
                `lastPulledCursor` INTEGER NOT NULL,
                `pendingChanges` INTEGER NOT NULL,
                `conflicts` INTEGER NOT NULL,
                `lastSyncAt` TEXT,
                `lastError` TEXT,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`organizationId`)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_central_sync_state_lastSyncAt` ON `central_sync_state` (`lastSyncAt`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_central_sync_state_runState` ON `central_sync_state` (`runState`)")
    }
}

val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `central_organization_workspace` (
                `organizationId` TEXT NOT NULL,
                `fetchedAt` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`organizationId`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_central_organization_workspace_fetchedAt` ON `central_organization_workspace` (`fetchedAt`)",
        )
    }
}
