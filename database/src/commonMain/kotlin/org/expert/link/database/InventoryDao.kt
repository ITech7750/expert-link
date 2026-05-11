package org.expert.link.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrganization(record: OrganizationRecord)

    @Query("SELECT * FROM organization WHERE organizationId = :organizationId")
    suspend fun findOrganization(organizationId: String): OrganizationRecord?

    @Query("SELECT * FROM organization ORDER BY updatedAt DESC")
    suspend fun listOrganizations(): List<OrganizationRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrganizationMember(record: OrganizationMemberRecord)

    @Query("SELECT * FROM organization_member WHERE organizationId = :organizationId")
    suspend fun listOrganizationMembers(organizationId: String): List<OrganizationMemberRecord>

    @Query("SELECT * FROM organization_member WHERE peerId = :peerId")
    suspend fun listOrganizationMembersByPeer(peerId: String): List<OrganizationMemberRecord>

    @Query("DELETE FROM organization_member WHERE organizationId = :organizationId AND peerId = :peerId")
    suspend fun deleteOrganizationMember(organizationId: String, peerId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRole(record: RoleRecord)

    @Query("SELECT * FROM role WHERE roleId = :roleId")
    suspend fun findRole(roleId: String): RoleRecord?

    @Query("SELECT * FROM role WHERE organizationId = :organizationId AND name = :name LIMIT 1")
    suspend fun findRoleByName(organizationId: String, name: String): RoleRecord?

    @Query("SELECT * FROM role WHERE organizationId = :organizationId ORDER BY name ASC")
    suspend fun listRoles(organizationId: String): List<RoleRecord>

    @Query("DELETE FROM role WHERE roleId = :roleId")
    suspend fun deleteRole(roleId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(record: InventoryCategoryRecord)

    @Query("SELECT * FROM inventory_category WHERE categoryId = :categoryId")
    suspend fun findCategory(categoryId: String): InventoryCategoryRecord?

    @Query("SELECT * FROM inventory_category WHERE organizationId = :organizationId ORDER BY name ASC")
    suspend fun listCategories(organizationId: String): List<InventoryCategoryRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(record: InventoryLocationRecord)

    @Query("SELECT * FROM inventory_location WHERE locationId = :locationId")
    suspend fun findLocation(locationId: String): InventoryLocationRecord?

    @Query("SELECT * FROM inventory_location WHERE organizationId = :organizationId ORDER BY name ASC")
    suspend fun listLocations(organizationId: String): List<InventoryLocationRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(record: InventoryItemRecord)

    @Query("SELECT * FROM inventory_item WHERE inventoryItemId = :itemId")
    suspend fun findItem(itemId: String): InventoryItemRecord?

    @Query("SELECT * FROM inventory_item WHERE organizationId = :organizationId AND inventoryNumber = :inventoryNumber LIMIT 1")
    suspend fun findItemByInventoryNumber(organizationId: String, inventoryNumber: String): InventoryItemRecord?

    @Query("SELECT * FROM inventory_item WHERE qrCode = :qrCode LIMIT 1")
    suspend fun findItemByQrCode(qrCode: String): InventoryItemRecord?

    @Query("SELECT * FROM inventory_item WHERE barcode = :barcode LIMIT 1")
    suspend fun findItemByBarcode(barcode: String): InventoryItemRecord?

    @Query("SELECT * FROM inventory_item WHERE organizationId = :organizationId ORDER BY updatedAt DESC")
    suspend fun listItems(organizationId: String): List<InventoryItemRecord>

    @Query("SELECT * FROM inventory_item WHERE organizationId = :organizationId AND status = :status ORDER BY updatedAt DESC")
    suspend fun listItemsByStatus(organizationId: String, status: String): List<InventoryItemRecord>

    @Query("SELECT * FROM inventory_item WHERE organizationId = :organizationId AND updatedAt >= :since ORDER BY updatedAt DESC")
    suspend fun listItemsUpdatedSince(organizationId: String, since: String): List<InventoryItemRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(record: InventorySessionRecord)

    @Query("SELECT * FROM inventory_session WHERE sessionId = :sessionId")
    suspend fun findSession(sessionId: String): InventorySessionRecord?

    @Query("SELECT * FROM inventory_session WHERE organizationId = :organizationId ORDER BY updatedAt DESC")
    suspend fun listSessions(organizationId: String): List<InventorySessionRecord>

    @Query("SELECT * FROM inventory_session WHERE organizationId = :organizationId AND status = :status ORDER BY updatedAt DESC")
    suspend fun listSessionsByStatus(organizationId: String, status: String): List<InventorySessionRecord>

    @Query("SELECT * FROM inventory_session WHERE organizationId = :organizationId AND updatedAt >= :since ORDER BY updatedAt DESC")
    suspend fun listSessionsUpdatedSince(organizationId: String, since: String): List<InventorySessionRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionMember(record: InventorySessionMemberRecord)

    @Query("SELECT * FROM inventory_session_member WHERE sessionId = :sessionId")
    suspend fun listSessionMembers(sessionId: String): List<InventorySessionMemberRecord>

    @Query("SELECT * FROM inventory_session_member WHERE peerId = :peerId")
    suspend fun listSessionMembersByPeer(peerId: String): List<InventorySessionMemberRecord>

    @Query("DELETE FROM inventory_session_member WHERE sessionId = :sessionId AND peerId = :peerId")
    suspend fun deleteSessionMember(sessionId: String, peerId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(record: InventoryReviewRecord)

    @Query("SELECT * FROM inventory_review WHERE inventoryItemId = :itemId ORDER BY createdAt DESC")
    suspend fun listReviewsByItem(itemId: String): List<InventoryReviewRecord>

    @Query("SELECT * FROM inventory_review WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    suspend fun listReviewsBySession(sessionId: String): List<InventoryReviewRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComment(record: InventoryCommentRecord)

    @Query("SELECT * FROM inventory_comment WHERE commentId = :commentId")
    suspend fun findComment(commentId: String): InventoryCommentRecord?

    @Query("SELECT * FROM inventory_comment WHERE inventoryItemId = :itemId ORDER BY createdAt DESC")
    suspend fun listCommentsByItem(itemId: String): List<InventoryCommentRecord>

    @Query("SELECT * FROM inventory_comment WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    suspend fun listCommentsBySession(sessionId: String): List<InventoryCommentRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachment(record: InventoryAttachmentRecord)

    @Query("SELECT * FROM inventory_attachment WHERE attachmentId = :attachmentId")
    suspend fun findAttachment(attachmentId: String): InventoryAttachmentRecord?

    @Query("SELECT * FROM inventory_attachment WHERE inventoryItemId = :itemId ORDER BY createdAt DESC")
    suspend fun listAttachmentsByItem(itemId: String): List<InventoryAttachmentRecord>

    @Query("SELECT * FROM inventory_attachment WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    suspend fun listAttachmentsBySession(sessionId: String): List<InventoryAttachmentRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(record: InventoryEventRecord)

    @Query("SELECT * FROM inventory_event WHERE eventId = :eventId")
    suspend fun findEvent(eventId: String): InventoryEventRecord?

    @Query("SELECT * FROM inventory_event WHERE organizationId = :organizationId ORDER BY sequence ASC")
    suspend fun listEvents(organizationId: String): List<InventoryEventRecord>

    @Query("SELECT * FROM inventory_event WHERE organizationId = :organizationId AND occurredAt >= :since ORDER BY sequence ASC")
    suspend fun listEventsSince(organizationId: String, since: String): List<InventoryEventRecord>

    @Query("SELECT * FROM inventory_event WHERE organizationId = :organizationId AND sequence > :sequence ORDER BY sequence ASC")
    suspend fun listEventsSinceSequence(organizationId: String, sequence: Long): List<InventoryEventRecord>

    @Query("SELECT * FROM inventory_event WHERE entityId = :entityId ORDER BY sequence ASC")
    suspend fun listEventsByEntity(entityId: String): List<InventoryEventRecord>

    @Query("SELECT MAX(sequence) FROM inventory_event WHERE organizationId = :organizationId")
    suspend fun maxEventSequence(organizationId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExport(record: InventoryExportRecord)

    @Query("SELECT * FROM inventory_export WHERE exportTaskId = :exportTaskId")
    suspend fun findExport(exportTaskId: String): InventoryExportRecord?

    @Query("SELECT * FROM inventory_export WHERE organizationId = :organizationId ORDER BY updatedAt DESC")
    suspend fun listExports(organizationId: String): List<InventoryExportRecord>

    @Query("SELECT * FROM inventory_export WHERE sessionId = :sessionId ORDER BY updatedAt DESC")
    suspend fun listExportsBySession(sessionId: String): List<InventoryExportRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQrCode(record: InventoryQrCodeRecord)

    @Query("SELECT * FROM inventory_qrcode WHERE qrCode = :code LIMIT 1")
    suspend fun findQrCodeByCode(code: String): InventoryQrCodeRecord?

    @Query("SELECT * FROM inventory_qrcode WHERE barcode = :barcode LIMIT 1")
    suspend fun findQrCodeByBarcode(barcode: String): InventoryQrCodeRecord?

    @Query("SELECT * FROM inventory_qrcode WHERE inventoryItemId = :itemId")
    suspend fun listQrCodesByItem(itemId: String): List<InventoryQrCodeRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubcategory(record: InventorySubcategoryRecord)

    @Query("SELECT * FROM inventory_subcategory WHERE subcategoryId = :subcategoryId")
    suspend fun findSubcategory(subcategoryId: String): InventorySubcategoryRecord?

    @Query("SELECT * FROM inventory_subcategory WHERE categoryId = :categoryId ORDER BY name ASC")
    suspend fun listSubcategoriesByCategory(categoryId: String): List<InventorySubcategoryRecord>

    @Query("SELECT * FROM inventory_subcategory WHERE organizationId = :organizationId ORDER BY name ASC")
    suspend fun listSubcategoriesByOrganization(organizationId: String): List<InventorySubcategoryRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTag(record: InventoryTagRecord)

    @Query("SELECT * FROM inventory_tag WHERE tagId = :tagId")
    suspend fun findTag(tagId: String): InventoryTagRecord?

    @Query("SELECT * FROM inventory_tag WHERE organizationId = :organizationId ORDER BY name ASC")
    suspend fun listTags(organizationId: String): List<InventoryTagRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttributeDefinition(record: InventoryAttributeDefinitionRecord)

    @Query("SELECT * FROM inventory_attribute_definition WHERE attributeId = :attributeId")
    suspend fun findAttributeDefinition(attributeId: String): InventoryAttributeDefinitionRecord?

    @Query("SELECT * FROM inventory_attribute_definition WHERE organizationId = :organizationId ORDER BY key ASC")
    suspend fun listAttributeDefinitions(organizationId: String): List<InventoryAttributeDefinitionRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryTemplate(record: InventoryCategoryTemplateRecord)

    @Query("SELECT * FROM inventory_category_template WHERE templateId = :templateId")
    suspend fun findCategoryTemplate(templateId: String): InventoryCategoryTemplateRecord?

    @Query("SELECT * FROM inventory_category_template WHERE organizationId = :organizationId ORDER BY updatedAt DESC")
    suspend fun listCategoryTemplatesByOrganization(organizationId: String): List<InventoryCategoryTemplateRecord>

    @Query("SELECT * FROM inventory_category_template WHERE categoryId = :categoryId ORDER BY updatedAt DESC")
    suspend fun listCategoryTemplatesByCategory(categoryId: String): List<InventoryCategoryTemplateRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOwner(record: InventoryOwnerRecord)

    @Query("SELECT * FROM inventory_owner WHERE ownerId = :ownerId")
    suspend fun findOwner(ownerId: String): InventoryOwnerRecord?

    @Query("SELECT * FROM inventory_owner WHERE organizationId = :organizationId ORDER BY name ASC")
    suspend fun listOwners(organizationId: String): List<InventoryOwnerRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDepartment(record: InventoryDepartmentRecord)

    @Query("SELECT * FROM inventory_department WHERE departmentId = :departmentId")
    suspend fun findDepartment(departmentId: String): InventoryDepartmentRecord?

    @Query("SELECT * FROM inventory_department WHERE organizationId = :organizationId ORDER BY name ASC")
    suspend fun listDepartments(organizationId: String): List<InventoryDepartmentRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCostCenter(record: InventoryCostCenterRecord)

    @Query("SELECT * FROM inventory_cost_center WHERE costCenterId = :costCenterId")
    suspend fun findCostCenter(costCenterId: String): InventoryCostCenterRecord?

    @Query("SELECT * FROM inventory_cost_center WHERE organizationId = :organizationId ORDER BY code ASC")
    suspend fun listCostCenters(organizationId: String): List<InventoryCostCenterRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLegalHolder(record: InventoryLegalHolderRecord)

    @Query("SELECT * FROM inventory_legal_holder WHERE legalHolderId = :legalHolderId")
    suspend fun findLegalHolder(legalHolderId: String): InventoryLegalHolderRecord?

    @Query("SELECT * FROM inventory_legal_holder WHERE organizationId = :organizationId ORDER BY name ASC")
    suspend fun listLegalHolders(organizationId: String): List<InventoryLegalHolderRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSupplier(record: InventorySupplierRecord)

    @Query("SELECT * FROM inventory_supplier WHERE supplierId = :supplierId")
    suspend fun findSupplier(supplierId: String): InventorySupplierRecord?

    @Query("SELECT * FROM inventory_supplier WHERE organizationId = :organizationId ORDER BY name ASC")
    suspend fun listSuppliers(organizationId: String): List<InventorySupplierRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFundingSource(record: InventoryFundingSourceRecord)

    @Query("SELECT * FROM inventory_funding_source WHERE fundingSourceId = :fundingSourceId")
    suspend fun findFundingSource(fundingSourceId: String): InventoryFundingSourceRecord?

    @Query("SELECT * FROM inventory_funding_source WHERE organizationId = :organizationId ORDER BY name ASC")
    suspend fun listFundingSources(organizationId: String): List<InventoryFundingSourceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncident(record: InventoryIncidentRecord)

    @Query("SELECT * FROM inventory_incident WHERE incidentId = :incidentId")
    suspend fun findIncident(incidentId: String): InventoryIncidentRecord?

    @Query("SELECT * FROM inventory_incident WHERE organizationId = :organizationId ORDER BY reportedAt DESC")
    suspend fun listIncidents(organizationId: String): List<InventoryIncidentRecord>

    @Query("SELECT * FROM inventory_incident WHERE inventoryItemId = :itemId ORDER BY reportedAt DESC")
    suspend fun listIncidentsByItem(itemId: String): List<InventoryIncidentRecord>

    @Query("SELECT * FROM inventory_incident WHERE sessionId = :sessionId ORDER BY reportedAt DESC")
    suspend fun listIncidentsBySession(sessionId: String): List<InventoryIncidentRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlert(record: InventoryAlertRecord)

    @Query("SELECT * FROM inventory_alert WHERE alertId = :alertId")
    suspend fun findAlert(alertId: String): InventoryAlertRecord?

    @Query("SELECT * FROM inventory_alert WHERE organizationId = :organizationId ORDER BY createdAt DESC")
    suspend fun listAlerts(organizationId: String): List<InventoryAlertRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(record: InventoryReminderRecord)

    @Query("SELECT * FROM inventory_reminder WHERE reminderId = :reminderId")
    suspend fun findReminder(reminderId: String): InventoryReminderRecord?

    @Query("SELECT * FROM inventory_reminder WHERE organizationId = :organizationId ORDER BY dueAt DESC")
    suspend fun listReminders(organizationId: String): List<InventoryReminderRecord>

    @Query("SELECT * FROM inventory_reminder WHERE inventoryItemId = :itemId ORDER BY dueAt DESC")
    suspend fun listRemindersByItem(itemId: String): List<InventoryReminderRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRuleThreshold(record: InventoryRuleThresholdRecord)

    @Query("SELECT * FROM inventory_rule_threshold WHERE ruleId = :ruleId")
    suspend fun findRuleThreshold(ruleId: String): InventoryRuleThresholdRecord?

    @Query("SELECT * FROM inventory_rule_threshold WHERE organizationId = :organizationId ORDER BY updatedAt DESC")
    suspend fun listRuleThresholds(organizationId: String): List<InventoryRuleThresholdRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeadlineRule(record: InventoryDeadlineRuleRecord)

    @Query("SELECT * FROM inventory_deadline_rule WHERE ruleId = :ruleId")
    suspend fun findDeadlineRule(ruleId: String): InventoryDeadlineRuleRecord?

    @Query("SELECT * FROM inventory_deadline_rule WHERE organizationId = :organizationId ORDER BY updatedAt DESC")
    suspend fun listDeadlineRules(organizationId: String): List<InventoryDeadlineRuleRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChangeLog(record: InventoryChangeLogRecord)

    @Query("SELECT * FROM inventory_change_log WHERE organizationId = :organizationId ORDER BY changedAt DESC")
    suspend fun listChangeLogs(organizationId: String): List<InventoryChangeLogRecord>

    @Query("SELECT * FROM inventory_change_log WHERE entityId = :entityId ORDER BY changedAt DESC")
    suspend fun listChangeLogsByEntity(entityId: String): List<InventoryChangeLogRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDashboardSnapshot(record: InventoryDashboardSnapshotRecord)

    @Query("SELECT * FROM inventory_dashboard_snapshot WHERE organizationId = :organizationId ORDER BY generatedAt DESC")
    suspend fun listDashboardSnapshots(organizationId: String): List<InventoryDashboardSnapshotRecord>

    @Query("SELECT * FROM inventory_dashboard_snapshot WHERE organizationId = :organizationId ORDER BY generatedAt DESC LIMIT 1")
    suspend fun latestDashboardSnapshot(organizationId: String): InventoryDashboardSnapshotRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCodeBinding(record: InventoryCodeBindingRecord)

    @Query("SELECT * FROM inventory_code_binding WHERE codeValue = :codeValue LIMIT 1")
    suspend fun findCodeBinding(codeValue: String): InventoryCodeBindingRecord?

    @Query("SELECT * FROM inventory_code_binding WHERE inventoryItemId = :itemId")
    suspend fun listCodeBindingsByItem(itemId: String): List<InventoryCodeBindingRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCode(record: InventoryCodeRecord)

    @Query("SELECT * FROM inventory_code WHERE inventoryCodeId = :codeId")
    suspend fun findCode(codeId: String): InventoryCodeRecord?

    @Query("SELECT * FROM inventory_code WHERE rawValue = :rawValue LIMIT 1")
    suspend fun findCodeByRawValue(rawValue: String): InventoryCodeRecord?

    @Query("SELECT * FROM inventory_code WHERE inventoryItemId = :itemId ORDER BY createdAt DESC")
    suspend fun listCodesByItem(itemId: String): List<InventoryCodeRecord>

    @Query("SELECT * FROM inventory_code WHERE organizationId = :organizationId ORDER BY createdAt DESC")
    suspend fun listCodesByOrganization(organizationId: String): List<InventoryCodeRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLabelTemplate(record: InventoryLabelTemplateRecord)

    @Query("SELECT * FROM inventory_label_template WHERE templateId = :templateId")
    suspend fun findLabelTemplate(templateId: String): InventoryLabelTemplateRecord?

    @Query("SELECT * FROM inventory_label_template WHERE organizationId = :organizationId ORDER BY updatedAt DESC")
    suspend fun listLabelTemplatesByOrganization(organizationId: String): List<InventoryLabelTemplateRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLabel(record: InventoryLabelRecord)

    @Query("SELECT * FROM inventory_label WHERE labelId = :labelId")
    suspend fun findLabel(labelId: String): InventoryLabelRecord?

    @Query("SELECT * FROM inventory_label WHERE inventoryItemId = :itemId ORDER BY createdAt DESC")
    suspend fun listLabelsByItem(itemId: String): List<InventoryLabelRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrintTask(record: InventoryPrintTaskRecord)

    @Query("SELECT * FROM inventory_print_task WHERE printTaskId = :printTaskId")
    suspend fun findPrintTask(printTaskId: String): InventoryPrintTaskRecord?

    @Query("SELECT * FROM inventory_print_task WHERE organizationId = :organizationId ORDER BY updatedAt DESC")
    suspend fun listPrintTasksByOrganization(organizationId: String): List<InventoryPrintTaskRecord>

    @Query("SELECT * FROM inventory_print_task WHERE payloadJson LIKE '%' || :itemId || '%' ORDER BY updatedAt DESC")
    suspend fun listPrintTasksByItem(itemId: String): List<InventoryPrintTaskRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScanEvent(record: InventoryScanEventRecord)

    @Query("SELECT * FROM inventory_scan_event WHERE organizationId = :organizationId ORDER BY scannedAt DESC")
    suspend fun listScanEvents(organizationId: String): List<InventoryScanEventRecord>

    @Query("SELECT * FROM inventory_scan_event WHERE inventoryItemId = :itemId ORDER BY scannedAt DESC")
    suspend fun listScanEventsByItem(itemId: String): List<InventoryScanEventRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRevision(record: InventoryRevisionRecord)

    @Query("SELECT * FROM inventory_revision WHERE entityId = :entityId")
    suspend fun findRevision(entityId: String): InventoryRevisionRecord?

    @Query("SELECT * FROM inventory_revision WHERE organizationId = :organizationId ORDER BY updatedAt DESC")
    suspend fun listRevisions(organizationId: String): List<InventoryRevisionRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConflict(record: InventoryConflictRecord)

    @Query("SELECT * FROM inventory_conflict WHERE conflictId = :conflictId")
    suspend fun findConflict(conflictId: String): InventoryConflictRecord?

    @Query("SELECT * FROM inventory_conflict WHERE organizationId = :organizationId ORDER BY detectedAt DESC")
    suspend fun listConflicts(organizationId: String): List<InventoryConflictRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCentralAuthSession(record: CentralAuthSessionRecord)

    @Query("SELECT * FROM central_auth_session ORDER BY lastAuthenticatedAt DESC LIMIT 1")
    suspend fun findCurrentCentralAuthSession(): CentralAuthSessionRecord?

    @Query("DELETE FROM central_auth_session")
    suspend fun clearCentralAuthSessions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCentralOrganizationAccess(record: CentralOrganizationAccessRecord)

    @Query("SELECT * FROM central_organization_access WHERE organizationId = :organizationId")
    suspend fun findCentralOrganizationAccess(organizationId: String): CentralOrganizationAccessRecord?

    @Query("SELECT * FROM central_organization_access ORDER BY active DESC, updatedAt DESC")
    suspend fun listCentralOrganizationAccess(): List<CentralOrganizationAccessRecord>

    @Query("UPDATE central_organization_access SET active = CASE WHEN organizationId = :organizationId THEN 1 ELSE 0 END")
    suspend fun setActiveCentralOrganization(organizationId: String)

    @Query("SELECT organizationId FROM central_organization_access WHERE active = 1 LIMIT 1")
    suspend fun findActiveCentralOrganizationId(): String?

    @Query("DELETE FROM central_organization_access")
    suspend fun clearCentralOrganizationAccess()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCentralSyncState(record: CentralSyncStateRecord)

    @Query("SELECT * FROM central_sync_state WHERE organizationId = :organizationId")
    suspend fun findCentralSyncState(organizationId: String): CentralSyncStateRecord?

    @Query("SELECT * FROM central_sync_state ORDER BY lastSyncAt DESC")
    suspend fun listCentralSyncStates(): List<CentralSyncStateRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCentralOrganizationWorkspace(record: CentralOrganizationWorkspaceRecord)

    @Query("SELECT * FROM central_organization_workspace WHERE organizationId = :organizationId")
    suspend fun findCentralOrganizationWorkspace(organizationId: String): CentralOrganizationWorkspaceRecord?

    @Query("SELECT * FROM central_organization_workspace ORDER BY fetchedAt DESC")
    suspend fun listCentralOrganizationWorkspaces(): List<CentralOrganizationWorkspaceRecord>

    @Query("DELETE FROM central_organization_workspace")
    suspend fun clearCentralOrganizationWorkspaces()
}
