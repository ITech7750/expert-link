package org.expert.link.mesh.contract.api

import kotlinx.datetime.Instant
import org.expert.link.mesh.contract.model.MeshBlockedPeer
import org.expert.link.mesh.contract.model.MeshCallSession
import org.expert.link.mesh.contract.model.MeshCallSignal
import org.expert.link.mesh.contract.model.MeshCallSignalType
import org.expert.link.mesh.contract.model.MeshCallParticipant
import org.expert.link.mesh.contract.model.MeshCallEvent
import org.expert.link.mesh.contract.model.MeshCallMediaState
import org.expert.link.mesh.contract.model.MeshCentralAttachmentArtifact
import org.expert.link.mesh.contract.model.MeshCentralAuthState
import org.expert.link.mesh.contract.model.MeshCentralConflict
import org.expert.link.mesh.contract.model.MeshCentralExportTask
import org.expert.link.mesh.contract.model.MeshCentralHybridState
import org.expert.link.mesh.contract.model.MeshCentralOrganizationWorkspaceSnapshot
import org.expert.link.mesh.contract.model.MeshCentralOrganizationAccess
import org.expert.link.mesh.contract.model.MeshCentralPendingChange
import org.expert.link.mesh.contract.model.MeshCentralSyncStatus
import org.expert.link.mesh.contract.model.MeshCentralUserProfile
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshChatMember
import org.expert.link.mesh.contract.model.MeshChatSummary
import org.expert.link.mesh.contract.model.MeshConversation
import org.expert.link.mesh.contract.model.MeshEventLogEntry
import org.expert.link.mesh.contract.model.MeshFileDescriptor
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshGroupChat
import org.expert.link.mesh.contract.model.MeshGroupEvent
import org.expert.link.mesh.contract.model.MeshInventoryAttachment
import org.expert.link.mesh.contract.model.MeshInventoryAttachmentPreviewMetadata
import org.expert.link.mesh.contract.model.MeshInventoryAttachmentStatus
import org.expert.link.mesh.contract.model.MeshInventoryAttachmentType
import org.expert.link.mesh.contract.model.MeshInventoryAttributeDefinition
import org.expert.link.mesh.contract.model.MeshInventoryAttributeType
import org.expert.link.mesh.contract.model.MeshInventoryAttributeValue
import org.expert.link.mesh.contract.model.MeshInventoryBarcodeFormat
import org.expert.link.mesh.contract.model.MeshInventoryCategory
import org.expert.link.mesh.contract.model.MeshInventoryCategoryTemplate
import org.expert.link.mesh.contract.model.MeshInventoryChangeLog
import org.expert.link.mesh.contract.model.MeshInventoryComment
import org.expert.link.mesh.contract.model.MeshInventoryCondition
import org.expert.link.mesh.contract.model.MeshInventoryCode
import org.expert.link.mesh.contract.model.MeshInventoryConfirmationStatus
import org.expert.link.mesh.contract.model.MeshInventoryCostCenter
import org.expert.link.mesh.contract.model.MeshInventoryDeadlineRule
import org.expert.link.mesh.contract.model.MeshInventoryDeadlineTarget
import org.expert.link.mesh.contract.model.MeshInventoryDepartment
import org.expert.link.mesh.contract.model.MeshInventoryEvent
import org.expert.link.mesh.contract.model.MeshInventoryExportFormat
import org.expert.link.mesh.contract.model.MeshInventoryExportStatus
import org.expert.link.mesh.contract.model.MeshInventoryExportTask
import org.expert.link.mesh.contract.model.MeshInventoryFieldTemplate
import org.expert.link.mesh.contract.model.MeshInventoryFundingSource
import org.expert.link.mesh.contract.model.MeshInventoryIncident
import org.expert.link.mesh.contract.model.MeshInventoryIncidentSeverity
import org.expert.link.mesh.contract.model.MeshInventoryIncidentStatus
import org.expert.link.mesh.contract.model.MeshInventoryIncidentType
import org.expert.link.mesh.contract.model.MeshInventoryItem
import org.expert.link.mesh.contract.model.MeshInventoryItemType
import org.expert.link.mesh.contract.model.MeshInventoryLabel
import org.expert.link.mesh.contract.model.MeshInventoryLabelFieldKey
import org.expert.link.mesh.contract.model.MeshInventoryLabelTemplate
import org.expert.link.mesh.contract.model.MeshInventoryLabelTemplateType
import org.expert.link.mesh.contract.model.MeshInventoryLocation
import org.expert.link.mesh.contract.model.MeshInventoryLocationType
import org.expert.link.mesh.contract.model.MeshInventoryAcceptanceStatus
import org.expert.link.mesh.contract.model.MeshInventoryOwner
import org.expert.link.mesh.contract.model.MeshInventoryOwnerType
import org.expert.link.mesh.contract.model.MeshInventoryPermission
import org.expert.link.mesh.contract.model.MeshInventoryPresenceStatus
import org.expert.link.mesh.contract.model.MeshInventoryPrintTask
import org.expert.link.mesh.contract.model.MeshInventoryQrCode
import org.expert.link.mesh.contract.model.MeshInventoryReview
import org.expert.link.mesh.contract.model.MeshInventoryReviewStatus
import org.expert.link.mesh.contract.model.MeshInventoryRuleThreshold
import org.expert.link.mesh.contract.model.MeshInventoryRuleType
import org.expert.link.mesh.contract.model.MeshInventorySessionResult
import org.expert.link.mesh.contract.model.MeshInventoryRequiredFieldRule
import org.expert.link.mesh.contract.model.MeshInventorySession
import org.expert.link.mesh.contract.model.MeshInventorySessionMember
import org.expert.link.mesh.contract.model.MeshInventorySessionReviewStatus
import org.expert.link.mesh.contract.model.MeshInventorySessionRole
import org.expert.link.mesh.contract.model.MeshInventorySessionStatus
import org.expert.link.mesh.contract.model.MeshInventorySubcategory
import org.expert.link.mesh.contract.model.MeshInventorySupplier
import org.expert.link.mesh.contract.model.MeshInventoryTag
import org.expert.link.mesh.contract.model.MeshInventoryStatus
import org.expert.link.mesh.contract.model.MeshInventorySyncResult
import org.expert.link.mesh.contract.model.MeshInventorySyncStatus
import org.expert.link.mesh.contract.model.MeshInventoryWorkflowStatus
import org.expert.link.mesh.contract.model.MeshInventoryValidationRule
import org.expert.link.mesh.contract.model.MeshInventoryAlertEvent
import org.expert.link.mesh.contract.model.MeshInventoryReminder
import org.expert.link.mesh.contract.model.MeshInventoryReminderStatus
import org.expert.link.mesh.contract.model.MeshInventoryDashboardSnapshot
import org.expert.link.mesh.contract.model.MeshInventorySearchQuery
import org.expert.link.mesh.contract.model.MeshInventorySearchResult
import org.expert.link.mesh.contract.model.MeshInventoryCodeBinding
import org.expert.link.mesh.contract.model.MeshInventoryCodeType
import org.expert.link.mesh.contract.model.MeshInventoryScanEvent
import org.expert.link.mesh.contract.model.MeshInventoryScanResultStatus
import org.expert.link.mesh.contract.model.MeshInventoryLegalHolder
import org.expert.link.mesh.contract.model.MeshLocalProfile
import org.expert.link.mesh.contract.model.MeshMetricSnapshot
import org.expert.link.mesh.contract.model.MeshMessageReceipt
import org.expert.link.mesh.contract.model.MeshNearbyPeer
import org.expert.link.mesh.contract.model.MeshPairingSession
import org.expert.link.mesh.contract.model.MeshPairedPeer
import org.expert.link.mesh.contract.model.MeshPeerEndpoint
import org.expert.link.mesh.contract.model.MeshRelayStatus
import org.expert.link.mesh.contract.model.MeshRelayMode
import org.expert.link.mesh.contract.model.MeshOrganization
import org.expert.link.mesh.contract.model.MeshOrganizationMember
import org.expert.link.mesh.contract.model.MeshOrganizationMemberStatus
import org.expert.link.mesh.contract.model.MeshRole
import org.expert.link.mesh.contract.model.MeshRouteInfo
import org.expert.link.mesh.contract.model.MeshRouteHealth
import org.expert.link.mesh.contract.model.MeshRoutingPlan
import org.expert.link.mesh.contract.model.MeshConnectivityStrategy
import org.expert.link.mesh.contract.model.MeshNetworkRoleState
import org.expert.link.mesh.contract.model.MeshMediaStats
import org.expert.link.mesh.contract.model.MeshTopologyState
import org.expert.link.mesh.contract.model.MeshThread
import org.expert.link.mesh.contract.model.MeshThreadMessage
import org.expert.link.mesh.contract.model.MeshThreadSummary
import org.expert.link.mesh.contract.model.MeshMessageType

/**
 * Единый публичный контракт встроенного mesh-узла для мобильного приложения.
 *
 * Через этот интерфейс приложение работает с жизненным циклом узла, pairing, чатом, файлами,
 * звонками, поиском узлов и диагностикой. Внутренние слои backend сюда не протекают.
 */
interface MeshNode {
    /** Публичный профиль локального узла. */
    val profile: MeshLocalProfile

    /** Текущий входной endpoint узла. */
    val endpoint: MeshPeerEndpoint

    /** Останавливает узел и освобождает ресурсы. */
    suspend fun stop()

    /** Создаёт короткоживущий invite для pairing. */
    suspend fun createPairingInvite(validitySeconds: Long = 300): String

    /** Принимает invite и запускает pairing. */
    suspend fun pairWithInvite(encodedInvite: String): MeshPairingSession

    /** Возвращает список известных peers. */
    suspend fun peers(): List<MeshPairedPeer>

    /** Возвращает активные pairing-сессии. */
    suspend fun pairingSessions(): List<MeshPairingSession>

    /** Блокирует peer. */
    suspend fun blockPeer(peerId: String, reason: String, expiresAt: Instant? = null): MeshBlockedPeer

    /** Убирает peer из block list. */
    suspend fun unblockPeer(peerId: String)

    /** Возвращает block list. */
    suspend fun blockedPeers(): List<MeshBlockedPeer>

    /** Запускает поиск peer через discovery. */
    suspend fun discoverPeer(peerId: String)

    /** Публикует явное объявление о присутствии узла. */
    suspend fun announcePresence()

    /** Возвращает найденные рядом узлы. */
    suspend fun nearbyPeers(): List<MeshNearbyPeer>

    /** Возвращает известные маршруты. */
    suspend fun routes(): List<MeshRouteInfo>

    /** Строит текущий план доставки до узла. */
    suspend fun routingPlan(peerId: String): MeshRoutingPlan

    /** Возвращает состояние relay/rendezvous режима. */
    fun relayStatus(): MeshRelayStatus

    /** Возвращает текущее состояние relay mode в топологии. */
    suspend fun relayModeState(): MeshRelayMode

    /** Возвращает текущий topology snapshot. */
    suspend fun observeTopologyState(): MeshTopologyState

    /** Возвращает состояние роли локального узла и выбранного хоста. */
    suspend fun observeHostRole(): MeshNetworkRoleState

    /** Возвращает стратегию связности для выбранного peer. */
    suspend fun observeConnectivityStrategy(peerId: String): MeshConnectivityStrategy

    /** Возвращает снимок здоровья всех известных маршрутов. */
    suspend fun inspectRouteHealth(): List<MeshRouteHealth>

    /** Принудительно обновляет topology snapshot. */
    suspend fun forceTopologyRefresh(): MeshTopologyState

    /** Добавляет ручной endpoint hint для peer. */
    suspend fun rememberPeerEndpoint(peerId: String, endpoint: MeshPeerEndpoint)

    /** Удаляет cached route и endpoint для peer. */
    suspend fun forgetPeerEndpoint(peerId: String)

    /** Открывает существующий или создаёт новый диалог. */
    suspend fun openConversation(peerId: String): MeshConversation

    /** Создаёт direct чат с участником. */
    suspend fun createDirectChat(peerId: String): MeshConversation

    /** Создаёт групповой чат. */
    suspend fun createGroupChat(command: MeshCreateGroupChatCommand): MeshConversation

    /** Переименовывает чат. */
    suspend fun renameChat(chatId: String, title: String): MeshConversation?

    /** Добавляет участников в чат. */
    suspend fun addParticipants(chatId: String, participants: List<MeshChatMemberCommand>): MeshConversation?

    /** Удаляет участника из чата. */
    suspend fun removeParticipant(chatId: String, peerId: String): MeshConversation?

    /** Возвращает только групповые чаты в нормализованном виде. */
    suspend fun groupChats(): List<MeshGroupChat>

    /** Возвращает системные события группы. */
    suspend fun groupEvents(chatId: String, limit: Int = 100): List<MeshGroupEvent>

    /** Возвращает сводки чатов для списка. */
    suspend fun chatSummaries(): List<MeshChatSummary>

    /** Возвращает диалоги. */
    suspend fun conversations(): List<MeshConversation>

    /** Возвращает историю сообщений группового чата. */
    suspend fun groupMessages(chatId: String): List<MeshChatMessage>

    /** Возвращает сообщения диалога. */
    suspend fun messages(conversationId: String): List<MeshChatMessage>

    /** Возвращает последние подтверждения доставки. */
    suspend fun messageReceipts(limit: Int = 100): List<MeshMessageReceipt>

    /** Отправляет чат-сообщение доверенному peer. */
    suspend fun sendChat(command: MeshChatCommand): MeshChatMessage

    /** Отправляет сообщение в чат по chatId. */
    suspend fun sendMessage(command: MeshSendMessageCommand): MeshChatMessage

    /** Создаёт тред для root-сообщения. */
    suspend fun createThread(command: MeshCreateThreadCommand): MeshThread

    /** Возвращает метаданные треда по root-сообщению. */
    suspend fun thread(chatId: String, rootMessageId: String): MeshThread?

    /** Возвращает обновления тредов в чате в polling-формате. */
    suspend fun threadUpdates(chatId: String): List<MeshThreadSummary>

    /** Возвращает сообщения треда в специализированной модели. */
    suspend fun threadMessagesDetailed(chatId: String, rootMessageId: String): List<MeshThreadMessage>

    /** Отправляет reply в тред и возвращает специализированную модель. */
    suspend fun sendThreadReply(command: MeshSendThreadMessageCommand): MeshThreadMessage

    /** Возвращает сообщения треда. */
    suspend fun threadMessages(chatId: String, rootMessageId: String): List<MeshChatMessage>

    /** Отправляет сообщение в тред. */
    suspend fun sendThreadMessage(command: MeshSendThreadMessageCommand): MeshChatMessage

    /** Возвращает сводку треда. */
    suspend fun threadSummary(chatId: String, rootMessageId: String): MeshThreadSummary?

    /** Возвращает файловые сессии. */
    suspend fun fileTransfers(): List<MeshFileTransferSession>

    /** Запускает отправку файла. */
    suspend fun sendFile(command: MeshFileTransferCommand): MeshFileTransferSession

    /** Запрашивает повтор недостающих чанков. */
    suspend fun resumeFileTransfer(transferId: String): MeshFileTransferSession?

    /** Отменяет передачу файла. */
    suspend fun cancelFileTransfer(transferId: String): MeshFileTransferSession?

    /** Возвращает call-сессии. */
    suspend fun callSessions(): List<MeshCallSession>

    /** Запускает исходящий 1:1 аудиозвонок. */
    suspend fun startAudioCall(command: MeshStartCallCommand): MeshCallSession

    /** Запускает исходящий 1:1 видеозвонок. */
    suspend fun startVideoCall(command: MeshStartCallCommand): MeshCallSession

    /** Запускает исходящий групповой аудиозвонок. */
    suspend fun startGroupAudioCall(command: MeshStartGroupCallCommand): MeshCallSession

    /** Запускает исходящий групповой видеозвонок. */
    suspend fun startGroupVideoCall(command: MeshStartGroupCallCommand): MeshCallSession

    /** Принимает входящий звонок. */
    suspend fun acceptCall(command: MeshAcceptCallCommand): MeshCallSignal

    /** Отклоняет входящий звонок. */
    suspend fun rejectCall(command: MeshRejectCallCommand): MeshCallSignal

    /** Подключается к групповому звонку. */
    suspend fun joinCall(command: MeshJoinCallCommand): MeshCallSignal

    /** Выходит из группового звонка. */
    suspend fun leaveCall(command: MeshLeaveCallCommand): MeshCallSignal

    /** Завершает звонок для локального узла/комнаты. */
    suspend fun endCall(command: MeshEndCallCommand): MeshCallSession?

    /** Возвращает активные звонки (polling snapshot). */
    suspend fun observeActiveCall(): List<MeshCallSession>

    /** Возвращает входящие звонки (polling snapshot). */
    suspend fun observeIncomingCalls(): List<MeshCallSession>

    /** Возвращает участников звонка. */
    suspend fun observeCallParticipants(callId: String): List<MeshCallParticipant>

    /** Возвращает историю событий звонка. */
    suspend fun observeCallEvents(callId: String, limit: Int = 200): List<MeshCallEvent>

    /** Включает или выключает микрофон в звонке. */
    suspend fun toggleMicrophone(command: MeshToggleMicrophoneCommand): MeshCallMediaState?

    /** Включает или выключает камеру в звонке. */
    suspend fun toggleCamera(command: MeshToggleCameraCommand): MeshCallMediaState?

    /** Переключает активную камеру. */
    suspend fun switchCamera(callId: String): MeshCallMediaState?

    /** Возвращает текущий media-снимок звонка. */
    suspend fun observeMediaState(callId: String): MeshCallMediaState?

    /** Возвращает текущий media-снимок метрик звонка. */
    suspend fun observeMediaStats(callId: String): MeshMediaStats?

    /** Запускает signaling звонка. */
    @Deprecated("Используйте startAudioCall/startVideoCall")
    suspend fun startCall(command: MeshStartCallCommand): MeshCallSession

    /** Отправляет signaling payload звонка. */
    @Deprecated("Используйте acceptCall/rejectCall/joinCall/leaveCall")
    suspend fun sendCallSignal(command: MeshCallSignalCommand): MeshCallSignal

    /** Завершает звонок. */
    @Deprecated("Используйте endCall")
    suspend fun hangupCall(command: MeshHangupCallCommand): MeshCallSession?

    /** Возвращает организации. */
    suspend fun organizations(): List<MeshOrganization>

    /** Создаёт организацию. */
    suspend fun createOrganization(command: MeshCreateOrganizationCommand): MeshOrganization

    /** Обновляет организацию. */
    suspend fun updateOrganization(command: MeshUpdateOrganizationCommand): MeshOrganization?

    /** Возвращает участников организации. */
    suspend fun organizationMembers(organizationId: String): List<MeshOrganizationMember>

    /** Добавляет участника организации. */
    suspend fun addOrganizationMember(command: MeshAddOrganizationMemberCommand): MeshOrganizationMember

    /** Обновляет роли участника организации. */
    suspend fun updateOrganizationMemberRoles(command: MeshUpdateOrganizationMemberRolesCommand): MeshOrganizationMember?

    /** Обновляет профиль участника и его участие в комиссии. */
    suspend fun updateOrganizationMember(command: MeshUpdateOrganizationMemberCommand): MeshOrganizationMember?

    /** Удаляет участника организации. */
    suspend fun removeOrganizationMember(organizationId: String, peerId: String)

    /** Возвращает роли организации. */
    suspend fun roles(organizationId: String): List<MeshRole>

    /** Создаёт роль организации. */
    suspend fun createRole(command: MeshCreateRoleCommand): MeshRole

    /** Обновляет роль организации. */
    suspend fun updateRole(command: MeshUpdateRoleCommand): MeshRole?

    /** Удаляет роль. */
    suspend fun removeRole(roleId: String)

    /** Возвращает категории инвентаря. */
    suspend fun inventoryCategories(organizationId: String): List<MeshInventoryCategory>

    /** Создаёт категорию инвентаря. */
    suspend fun createInventoryCategory(command: MeshCreateInventoryCategoryCommand): MeshInventoryCategory

    /** Обновляет категорию инвентаря. */
    suspend fun updateInventoryCategory(command: MeshUpdateInventoryCategoryCommand): MeshInventoryCategory?

    /** Возвращает подкатегории инвентаря. */
    suspend fun inventorySubcategories(organizationId: String, categoryId: String? = null): List<MeshInventorySubcategory>

    /** Создаёт подкатегорию инвентаря. */
    suspend fun createInventorySubcategory(command: MeshCreateInventorySubcategoryCommand): MeshInventorySubcategory

    /** Обновляет подкатегорию инвентаря. */
    suspend fun updateInventorySubcategory(command: MeshUpdateInventorySubcategoryCommand): MeshInventorySubcategory?

    /** Возвращает теги инвентаря. */
    suspend fun inventoryTags(organizationId: String): List<MeshInventoryTag>

    /** Создаёт тег инвентаря. */
    suspend fun createInventoryTag(command: MeshCreateInventoryTagCommand): MeshInventoryTag

    /** Обновляет тег инвентаря. */
    suspend fun updateInventoryTag(command: MeshUpdateInventoryTagCommand): MeshInventoryTag?

    /** Возвращает определения атрибутов инвентаря. */
    suspend fun inventoryAttributeDefinitions(organizationId: String): List<MeshInventoryAttributeDefinition>

    /** Создаёт определение атрибута. */
    suspend fun createInventoryAttributeDefinition(
        command: MeshCreateInventoryAttributeDefinitionCommand,
    ): MeshInventoryAttributeDefinition

    /** Обновляет определение атрибута. */
    suspend fun updateInventoryAttributeDefinition(
        command: MeshUpdateInventoryAttributeDefinitionCommand,
    ): MeshInventoryAttributeDefinition?

    /** Возвращает шаблоны категорий. */
    suspend fun inventoryCategoryTemplates(
        organizationId: String,
        categoryId: String? = null,
    ): List<MeshInventoryCategoryTemplate>

    /** Создаёт шаблон категории. */
    suspend fun createInventoryCategoryTemplate(
        command: MeshCreateInventoryCategoryTemplateCommand,
    ): MeshInventoryCategoryTemplate

    /** Обновляет шаблон категории. */
    suspend fun updateInventoryCategoryTemplate(
        command: MeshUpdateInventoryCategoryTemplateCommand,
    ): MeshInventoryCategoryTemplate?

    /** Возвращает локации инвентаря. */
    suspend fun inventoryLocations(organizationId: String): List<MeshInventoryLocation>

    /** Создаёт локацию инвентаря. */
    suspend fun createInventoryLocation(command: MeshCreateInventoryLocationCommand): MeshInventoryLocation

    /** Обновляет локацию инвентаря. */
    suspend fun updateInventoryLocation(command: MeshUpdateInventoryLocationCommand): MeshInventoryLocation?

    /** Возвращает владельцев инвентаря. */
    suspend fun inventoryOwners(organizationId: String): List<MeshInventoryOwner>

    /** Создаёт владельца инвентаря. */
    suspend fun createInventoryOwner(command: MeshCreateInventoryOwnerCommand): MeshInventoryOwner

    /** Обновляет владельца инвентаря. */
    suspend fun updateInventoryOwner(command: MeshUpdateInventoryOwnerCommand): MeshInventoryOwner?

    /** Возвращает подразделения организации. */
    suspend fun inventoryDepartments(organizationId: String): List<MeshInventoryDepartment>

    /** Создаёт подразделение. */
    suspend fun createInventoryDepartment(command: MeshCreateInventoryDepartmentCommand): MeshInventoryDepartment

    /** Обновляет подразделение. */
    suspend fun updateInventoryDepartment(command: MeshUpdateInventoryDepartmentCommand): MeshInventoryDepartment?

    /** Возвращает центры затрат. */
    suspend fun inventoryCostCenters(organizationId: String): List<MeshInventoryCostCenter>

    /** Создаёт центр затрат. */
    suspend fun createInventoryCostCenter(command: MeshCreateInventoryCostCenterCommand): MeshInventoryCostCenter

    /** Обновляет центр затрат. */
    suspend fun updateInventoryCostCenter(command: MeshUpdateInventoryCostCenterCommand): MeshInventoryCostCenter?

    /** Возвращает балансодержателей. */
    suspend fun inventoryLegalHolders(organizationId: String): List<MeshInventoryLegalHolder>

    /** Создаёт балансодержателя. */
    suspend fun createInventoryLegalHolder(command: MeshCreateInventoryLegalHolderCommand): MeshInventoryLegalHolder

    /** Обновляет балансодержателя. */
    suspend fun updateInventoryLegalHolder(command: MeshUpdateInventoryLegalHolderCommand): MeshInventoryLegalHolder?

    /** Возвращает поставщиков/контрагентов. */
    suspend fun inventorySuppliers(organizationId: String): List<MeshInventorySupplier>

    /** Создаёт поставщика. */
    suspend fun createInventorySupplier(command: MeshCreateInventorySupplierCommand): MeshInventorySupplier

    /** Обновляет поставщика. */
    suspend fun updateInventorySupplier(command: MeshUpdateInventorySupplierCommand): MeshInventorySupplier?

    /** Возвращает источники финансирования. */
    suspend fun inventoryFundingSources(organizationId: String): List<MeshInventoryFundingSource>

    /** Создаёт источник финансирования. */
    suspend fun createInventoryFundingSource(command: MeshCreateInventoryFundingSourceCommand): MeshInventoryFundingSource

    /** Обновляет источник финансирования. */
    suspend fun updateInventoryFundingSource(command: MeshUpdateInventoryFundingSourceCommand): MeshInventoryFundingSource?

    /** Возвращает инвентарные объекты. */
    suspend fun inventoryItems(organizationId: String, status: MeshInventoryStatus? = null): List<MeshInventoryItem>

    /** Возвращает карточку объекта. */
    suspend fun inventoryItem(itemId: String): MeshInventoryItem?

    /** Создаёт карточку объекта. */
    suspend fun createInventoryItem(command: MeshCreateInventoryItemCommand): MeshInventoryItem

    /** Обновляет карточку объекта. */
    suspend fun updateInventoryItem(command: MeshUpdateInventoryItemCommand): MeshInventoryItem?

    /** Обновляет статус объекта. */
    suspend fun updateInventoryStatus(command: MeshUpdateInventoryStatusCommand): MeshInventoryItem?

    /** Добавляет комментарий к объекту. */
    suspend fun addInventoryComment(command: MeshAddInventoryCommentCommand): MeshInventoryComment

    /** Добавляет вложение/фото к объекту. */
    suspend fun addInventoryAttachment(command: MeshAddInventoryAttachmentCommand): MeshInventoryAttachment

    /** Связывает объект с чат-дискуссией. */
    suspend fun linkInventoryDiscussion(command: MeshLinkInventoryDiscussionCommand): MeshInventoryItem?

    /** Возвращает инциденты по инвентарю. */
    suspend fun inventoryIncidents(
        organizationId: String,
        itemId: String? = null,
        sessionId: String? = null,
    ): List<MeshInventoryIncident>

    /** Регистрирует инцидент. */
    suspend fun reportInventoryIncident(command: MeshReportInventoryIncidentCommand): MeshInventoryIncident

    /** Обновляет статус инцидента. */
    suspend fun updateInventoryIncidentStatus(command: MeshUpdateInventoryIncidentStatusCommand): MeshInventoryIncident?

    /** Возвращает alert события. */
    suspend fun inventoryAlerts(organizationId: String): List<MeshInventoryAlertEvent>

    /** Возвращает напоминания по инвентарю. */
    suspend fun inventoryReminders(organizationId: String, itemId: String? = null): List<MeshInventoryReminder>

    /** Возвращает правила порогов. */
    suspend fun inventoryRuleThresholds(organizationId: String): List<MeshInventoryRuleThreshold>

    /** Создаёт правило порога. */
    suspend fun createInventoryRuleThreshold(command: MeshCreateInventoryRuleThresholdCommand): MeshInventoryRuleThreshold

    /** Обновляет правило порога. */
    suspend fun updateInventoryRuleThreshold(command: MeshUpdateInventoryRuleThresholdCommand): MeshInventoryRuleThreshold?

    /** Возвращает правила дедлайнов. */
    suspend fun inventoryDeadlineRules(organizationId: String): List<MeshInventoryDeadlineRule>

    /** Создаёт правило дедлайна. */
    suspend fun createInventoryDeadlineRule(command: MeshCreateInventoryDeadlineRuleCommand): MeshInventoryDeadlineRule

    /** Обновляет правило дедлайна. */
    suspend fun updateInventoryDeadlineRule(command: MeshUpdateInventoryDeadlineRuleCommand): MeshInventoryDeadlineRule?

    /** Возвращает инвентаризационные сессии. */
    suspend fun inventorySessions(organizationId: String): List<MeshInventorySession>

    /** Возвращает сессию. */
    suspend fun inventorySession(sessionId: String): MeshInventorySession?

    /** Создаёт инвентаризационную сессию. */
    suspend fun createInventorySession(command: MeshCreateInventorySessionCommand): MeshInventorySession

    /** Обновляет инвентаризационную сессию. */
    suspend fun updateInventorySession(command: MeshUpdateInventorySessionCommand): MeshInventorySession?

    /** Добавляет объекты в сессию. */
    suspend fun addItemsToSession(command: MeshAddItemsToSessionCommand): MeshInventorySession?

    /** Добавляет участника сессии. */
    suspend fun addInventorySessionMember(command: MeshAddInventorySessionMemberCommand): MeshInventorySessionMember

    /** Возвращает участников сессии. */
    suspend fun inventorySessionMembers(sessionId: String): List<MeshInventorySessionMember>

    /** Закрывает сессию. */
    suspend fun closeInventorySession(command: MeshCloseInventorySessionCommand): MeshInventorySession?

    /** Создаёт review/approval по объекту. */
    suspend fun submitInventoryReview(command: MeshSubmitInventoryReviewCommand): MeshInventoryReview

    /** Возвращает отзывы по объекту. */
    suspend fun inventoryReviews(itemId: String, sessionId: String? = null): List<MeshInventoryReview>

    /** Возвращает экспортные задачи. */
    suspend fun inventoryExports(organizationId: String): List<MeshInventoryExportTask>

    /** Запрашивает экспорт. */
    suspend fun requestInventoryExport(command: MeshRequestInventoryExportCommand): MeshInventoryExportTask

    /** Обновляет статус экспорта. */
    suspend fun updateInventoryExportStatus(command: MeshUpdateInventoryExportStatusCommand): MeshInventoryExportTask?

    /** Возвращает инвентарные события. */
    suspend fun inventoryEvents(organizationId: String, sinceSequence: Long? = null): List<MeshInventoryEvent>

    /** Возвращает change log инвентаря. */
    suspend fun inventoryChangeLogs(organizationId: String, entityId: String? = null): List<MeshInventoryChangeLog>

    /** Запрашивает dashboard snapshot. */
    suspend fun inventoryDashboardSnapshot(organizationId: String): MeshInventoryDashboardSnapshot

    /** Выполняет поиск по инвентарю. */
    suspend fun searchInventory(command: MeshInventorySearchCommand): MeshInventorySearchResult

    /** Ищет объект по QR-коду. */
    suspend fun findInventoryItemByQr(code: String): MeshInventoryItem?

    /** Ищет объект по штрихкоду. */
    suspend fun findInventoryItemByBarcode(code: String): MeshInventoryItem?

    /** Ищет объект по любому коду. */
    suspend fun findInventoryItemByCode(code: String): MeshInventoryItem?

    /** Возвращает коды объекта. */
    suspend fun inventoryCodes(itemId: String): List<MeshInventoryCode>

    /** Возвращает активный код. */
    suspend fun getInventoryCode(itemId: String, codeType: MeshInventoryCodeType): MeshInventoryCode?

    /** Генерирует код. */
    suspend fun generateInventoryCode(command: MeshGenerateInventoryCodeCommand): MeshInventoryCode

    /** Перевыпускает код. */
    suspend fun regenerateInventoryCode(command: MeshRegenerateInventoryCodeCommand): MeshInventoryCode

    /** Деактивирует код. */
    suspend fun deactivateInventoryCode(command: MeshDeactivateInventoryCodeCommand): MeshInventoryCode?

    /** Возвращает привязки кодов. */
    suspend fun inventoryCodeBindings(itemId: String): List<MeshInventoryCodeBinding>

    /** Создаёт привязку кода. */
    suspend fun createInventoryCodeBinding(command: MeshCreateInventoryCodeBindingCommand): MeshInventoryCodeBinding

    /** Возвращает шаблоны этикеток. */
    suspend fun inventoryLabelTemplates(organizationId: String): List<MeshInventoryLabelTemplate>

    /** Создаёт шаблон этикетки. */
    suspend fun createInventoryLabelTemplate(command: MeshCreateInventoryLabelTemplateCommand): MeshInventoryLabelTemplate

    /** Обновляет шаблон этикетки. */
    suspend fun updateInventoryLabelTemplate(command: MeshUpdateInventoryLabelTemplateCommand): MeshInventoryLabelTemplate?

    /** Выбирает шаблон по умолчанию. */
    suspend fun selectDefaultLabelTemplate(command: MeshSelectDefaultLabelTemplateCommand): MeshInventoryLabelTemplate?

    /** Превью этикетки. */
    suspend fun generateInventoryLabelPreview(request: MeshGenerateInventoryLabelRequest): MeshGenerateInventoryLabelResponse

    /** Генерирует PDF этикетки. */
    suspend fun generateInventoryLabelPdf(request: MeshGenerateInventoryLabelRequest): MeshGenerateInventoryLabelResponse

    /** Печатает этикетку. */
    suspend fun printInventoryLabel(request: MeshPrintInventoryLabelRequest): MeshInventoryPrintTask

    /** Массовая печать этикеток. */
    suspend fun printInventoryLabelsBatch(request: MeshBatchPrintInventoryLabelsRequest): MeshInventoryPrintTask

    /** Возвращает задачи печати. */
    suspend fun inventoryPrintTasks(organizationId: String): List<MeshInventoryPrintTask>

    /** Регистрирует событие сканирования. */
    suspend fun recordInventoryScan(command: MeshRecordInventoryScanCommand): MeshInventoryScanEvent

    /** Разрешает отсканированный код и регистрирует событие. */
    suspend fun resolveScannedCode(request: MeshResolveScannedCodeRequest): MeshResolveScannedCodeResponse

    /** Регистрирует событие сканирования по расширенной модели. */
    suspend fun registerScanEvent(request: MeshRegisterScanEventRequest): MeshInventoryScanEvent

    /** Запрашивает синхронизацию инвентаря с peer. */
    suspend fun syncInventoryWithPeer(command: MeshInventorySyncCommand): MeshInventorySyncResult

    /** Возвращает hybrid-состояние узла относительно central backend. */
    suspend fun hybridState(): MeshCentralHybridState

    /** Выполняет central login и сохраняет auth context локально для offline fallback. */
    suspend fun centralLogin(command: MeshCentralLoginCommand): MeshCentralAuthState

    /** Обновляет access token по refresh token, если central backend доступен. */
    suspend fun centralRefreshAuth(): MeshCentralAuthState?

    /** Завершает central session и очищает локальные токены. */
    suspend fun centralLogout()

    /** Возвращает last-known central auth state. */
    suspend fun centralAuthState(): MeshCentralAuthState?

    /** Возвращает профиль пользователя из central auth context. */
    suspend fun centralUserProfile(): MeshCentralUserProfile?

    /** Возвращает last-known organizations/memberships из organization-access-service cache. */
    suspend fun centralOrganizations(): List<MeshCentralOrganizationAccess>

    /** Возвращает last-known cached snapshot organization-access и relay данных для организации. */
    suspend fun centralOrganizationWorkspace(organizationId: String): MeshCentralOrganizationWorkspaceSnapshot?

    /** Принудительно обновляет organization-access/relay snapshot из central backend и кэширует его локально. */
    suspend fun refreshCentralOrganizationWorkspace(organizationId: String): MeshCentralOrganizationWorkspaceSnapshot?

    /** Выбирает активную организацию для central sync и inventory scope. */
    suspend fun selectActiveCentralOrganization(command: MeshSelectActiveCentralOrganizationCommand): MeshCentralOrganizationAccess?

    /** Возвращает состояние sync для организации. */
    suspend fun centralSyncStatus(organizationId: String): MeshCentralSyncStatus

    /** Возвращает очередь локальных изменений, ожидающих central sync. */
    suspend fun centralPendingChanges(organizationId: String, limit: Int = 100): List<MeshCentralPendingChange>

    /** Возвращает конфликты central sync для организации. */
    suspend fun centralConflicts(organizationId: String): List<MeshCentralConflict>

    /** Пытается разрешить конфликт и синхронизировать canonical snapshot повторно. */
    suspend fun resolveCentralConflict(command: MeshResolveCentralConflictCommand): MeshCentralConflict?

    /** Выполняет push/pull sync с central backend для active или указанной организации. */
    suspend fun syncWithCentral(command: MeshCentralSyncCommand = MeshCentralSyncCommand()): MeshCentralSyncStatus

    /** Синхронизирует вложения с central attachment API. */
    suspend fun syncCentralAttachments(organizationId: String): List<MeshCentralAttachmentArtifact>

    /** Запрашивает central export task. */
    suspend fun requestCentralExport(command: MeshRequestCentralExportCommand): MeshCentralExportTask

    /** Возвращает central export tasks из local cache. */
    suspend fun centralExports(organizationId: String): List<MeshCentralExportTask>

    /** Возвращает последние события. */
    suspend fun recentEvents(limit: Int = 100): List<MeshEventLogEntry>

    /** Возвращает текущий snapshot метрик. */
    suspend fun metrics(): MeshMetricSnapshot
}

/** Команда на отправку чат-сообщения. */
data class MeshChatCommand(
    val targetPeerId: String,
    val body: String,
    val conversationId: String? = null,
)

/** Команда создания группового чата. */
data class MeshCreateGroupChatCommand(
    val title: String,
    val description: String? = null,
    val participantPeerIds: Set<String>,
)

/** Команда участника для изменения состава чата. */
data class MeshChatMemberCommand(
    val peerId: String,
    val displayName: String,
)

/** Команда отправки сообщения в чат. */
data class MeshSendMessageCommand(
    val chatId: String,
    val body: String,
    val messageType: MeshMessageType = MeshMessageType.TEXT,
)

/** Команда создания треда. */
data class MeshCreateThreadCommand(
    val chatId: String,
    val rootMessageId: String,
)

/** Команда отправки сообщения в тред. */
data class MeshSendThreadMessageCommand(
    val chatId: String,
    val rootMessageId: String,
    val body: String,
    val parentMessageId: String? = null,
)

/** Команда на отправку файла.
 *
 * `path` считается платформенным file handle. На JVM это может быть обычный путь,
 * на Android или iOS — URI, sandbox path или иной локальный идентификатор ресурса.
 */
data class MeshFileTransferCommand(
    val targetPeerId: String,
    val path: String,
    val conversationId: String? = null,
)

/** Команда на запуск звонка. */
data class MeshStartCallCommand(
    val targetPeerId: String,
    val offer: String? = null,
    val conversationId: String? = null,
)

/** Команда на запуск группового звонка. */
data class MeshStartGroupCallCommand(
    val targetPeerIds: Set<String>,
    val offer: String? = null,
    val conversationId: String? = null,
    val roomTitle: String? = null,
)

/** Команда на принятие звонка. */
data class MeshAcceptCallCommand(
    val callId: String,
    val recipientPeerId: String,
    val answer: String? = null,
)

/** Команда на отклонение звонка. */
data class MeshRejectCallCommand(
    val callId: String,
    val recipientPeerId: String,
    val reason: String,
)

/** Команда на подключение к звонку. */
data class MeshJoinCallCommand(
    val callId: String,
    val recipientPeerId: String,
    val answer: String? = null,
)

/** Команда на выход из звонка. */
data class MeshLeaveCallCommand(
    val callId: String,
    val recipientPeerId: String,
    val reason: String = "left",
)

/** Команда на завершение звонка. */
data class MeshEndCallCommand(
    val callId: String,
    val reason: String = "ended",
)

/** Команда переключения микрофона. */
data class MeshToggleMicrophoneCommand(
    val callId: String,
    val enabled: Boolean,
)

/** Команда переключения камеры. */
data class MeshToggleCameraCommand(
    val callId: String,
    val enabled: Boolean,
)

/** Команда на отправку сигнала звонка. */
data class MeshCallSignalCommand(
    val callId: String,
    val recipientPeerId: String,
    val signalType: MeshCallSignalType,
    val payload: String,
)

/** Команда на завершение звонка. */
data class MeshHangupCallCommand(
    val callId: String,
    val recipientPeerId: String,
    val reason: String,
)

/** Команда создания организации. */
data class MeshCreateOrganizationCommand(
    val name: String,
    val description: String? = null,
)

/** Команда обновления организации. */
data class MeshUpdateOrganizationCommand(
    val organizationId: String,
    val name: String,
    val description: String? = null,
)

/** Команда добавления участника организации. */
data class MeshAddOrganizationMemberCommand(
    val organizationId: String,
    val peerId: String,
    val displayName: String,
    val roleIds: Set<String>,
    val departmentId: String? = null,
    val locationIds: Set<String> = emptySet(),
    val position: String? = null,
    val isCommissionMember: Boolean = false,
)

/** Команда обновления ролей участника. */
data class MeshUpdateOrganizationMemberRolesCommand(
    val organizationId: String,
    val peerId: String,
    val roleIds: Set<String>,
)

/** Команда обновления профиля участника. */
data class MeshUpdateOrganizationMemberCommand(
    val organizationId: String,
    val peerId: String,
    val displayName: String? = null,
    val roleIds: Set<String>? = null,
    val departmentId: String? = null,
    val locationIds: Set<String>? = null,
    val position: String? = null,
    val isCommissionMember: Boolean? = null,
    val status: MeshOrganizationMemberStatus? = null,
)

/** Команда создания роли. */
data class MeshCreateRoleCommand(
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val permissions: Set<MeshInventoryPermission>,
)

/** Команда обновления роли. */
data class MeshUpdateRoleCommand(
    val roleId: String,
    val name: String,
    val description: String? = null,
    val permissions: Set<MeshInventoryPermission>,
)

/** Команда создания категории. */
data class MeshCreateInventoryCategoryCommand(
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val parentCategoryId: String? = null,
    val templateId: String? = null,
)

/** Команда обновления категории. */
data class MeshUpdateInventoryCategoryCommand(
    val categoryId: String,
    val name: String,
    val description: String? = null,
    val parentCategoryId: String? = null,
    val templateId: String? = null,
)

/** Команда создания подкатегории. */
data class MeshCreateInventorySubcategoryCommand(
    val organizationId: String,
    val categoryId: String,
    val name: String,
    val description: String? = null,
)

/** Команда обновления подкатегории. */
data class MeshUpdateInventorySubcategoryCommand(
    val subcategoryId: String,
    val name: String,
    val description: String? = null,
)

/** Команда создания тега. */
data class MeshCreateInventoryTagCommand(
    val organizationId: String,
    val name: String,
    val color: String? = null,
)

/** Команда обновления тега. */
data class MeshUpdateInventoryTagCommand(
    val tagId: String,
    val name: String,
    val color: String? = null,
)

/** Команда создания определения атрибута. */
data class MeshCreateInventoryAttributeDefinitionCommand(
    val organizationId: String,
    val key: String,
    val label: String,
    val description: String? = null,
    val type: MeshInventoryAttributeType = MeshInventoryAttributeType.TEXT,
    val required: Boolean = false,
    val unit: String? = null,
    val options: List<String> = emptyList(),
    val validationRules: List<MeshInventoryValidationRule> = emptyList(),
)

/** Команда обновления определения атрибута. */
data class MeshUpdateInventoryAttributeDefinitionCommand(
    val attributeId: String,
    val key: String? = null,
    val label: String? = null,
    val description: String? = null,
    val type: MeshInventoryAttributeType? = null,
    val required: Boolean? = null,
    val unit: String? = null,
    val options: List<String>? = null,
    val validationRules: List<MeshInventoryValidationRule>? = null,
)

/** Команда создания шаблона категории. */
data class MeshCreateInventoryCategoryTemplateCommand(
    val organizationId: String,
    val categoryId: String,
    val name: String,
    val description: String? = null,
    val fields: List<MeshInventoryFieldTemplate> = emptyList(),
    val requiredFields: List<MeshInventoryRequiredFieldRule> = emptyList(),
)

/** Команда обновления шаблона категории. */
data class MeshUpdateInventoryCategoryTemplateCommand(
    val templateId: String,
    val name: String? = null,
    val description: String? = null,
    val fields: List<MeshInventoryFieldTemplate>? = null,
    val requiredFields: List<MeshInventoryRequiredFieldRule>? = null,
)

/** Команда создания локации. */
data class MeshCreateInventoryLocationCommand(
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val parentLocationId: String? = null,
    val locationType: MeshInventoryLocationType = MeshInventoryLocationType.OTHER,
    val code: String? = null,
    val path: String? = null,
    val departmentId: String? = null,
    val archived: Boolean = false,
)

/** Команда обновления локации. */
data class MeshUpdateInventoryLocationCommand(
    val locationId: String,
    val name: String,
    val description: String? = null,
    val parentLocationId: String? = null,
    val locationType: MeshInventoryLocationType? = null,
    val code: String? = null,
    val path: String? = null,
    val departmentId: String? = null,
    val archived: Boolean? = null,
)

/** Команда создания владельца. */
data class MeshCreateInventoryOwnerCommand(
    val organizationId: String,
    val name: String,
    val type: MeshInventoryOwnerType = MeshInventoryOwnerType.ORGANIZATION,
    val legalHolderId: String? = null,
    val contactInfo: String? = null,
    val code: String? = null,
    val departmentId: String? = null,
    val locationIds: Set<String> = emptySet(),
    val archived: Boolean = false,
)

/** Команда обновления владельца. */
data class MeshUpdateInventoryOwnerCommand(
    val ownerId: String,
    val name: String? = null,
    val type: MeshInventoryOwnerType? = null,
    val legalHolderId: String? = null,
    val contactInfo: String? = null,
    val code: String? = null,
    val departmentId: String? = null,
    val locationIds: Set<String>? = null,
    val archived: Boolean? = null,
)

/** Команда создания подразделения. */
data class MeshCreateInventoryDepartmentCommand(
    val organizationId: String,
    val name: String,
    val parentDepartmentId: String? = null,
    val code: String? = null,
    val locationIds: Set<String> = emptySet(),
    val ownerIds: Set<String> = emptySet(),
    val archived: Boolean = false,
)

/** Команда обновления подразделения. */
data class MeshUpdateInventoryDepartmentCommand(
    val departmentId: String,
    val name: String? = null,
    val parentDepartmentId: String? = null,
    val code: String? = null,
    val locationIds: Set<String>? = null,
    val ownerIds: Set<String>? = null,
    val archived: Boolean? = null,
)

/** Команда создания центра затрат. */
data class MeshCreateInventoryCostCenterCommand(
    val organizationId: String,
    val code: String,
    val name: String,
    val description: String? = null,
)

/** Команда обновления центра затрат. */
data class MeshUpdateInventoryCostCenterCommand(
    val costCenterId: String,
    val code: String? = null,
    val name: String? = null,
    val description: String? = null,
)

/** Команда создания балансодержателя. */
data class MeshCreateInventoryLegalHolderCommand(
    val organizationId: String,
    val name: String,
    val taxId: String? = null,
    val registrationNumber: String? = null,
    val address: String? = null,
    val bankDetails: String? = null,
)

/** Команда обновления балансодержателя. */
data class MeshUpdateInventoryLegalHolderCommand(
    val legalHolderId: String,
    val name: String? = null,
    val taxId: String? = null,
    val registrationNumber: String? = null,
    val address: String? = null,
    val bankDetails: String? = null,
)

/** Команда создания поставщика. */
data class MeshCreateInventorySupplierCommand(
    val organizationId: String,
    val name: String,
    val contactInfo: String? = null,
    val bankDetails: String? = null,
)

/** Команда обновления поставщика. */
data class MeshUpdateInventorySupplierCommand(
    val supplierId: String,
    val name: String? = null,
    val contactInfo: String? = null,
    val bankDetails: String? = null,
)

/** Команда создания источника финансирования. */
data class MeshCreateInventoryFundingSourceCommand(
    val organizationId: String,
    val name: String,
    val description: String? = null,
)

/** Команда обновления источника финансирования. */
data class MeshUpdateInventoryFundingSourceCommand(
    val fundingSourceId: String,
    val name: String? = null,
    val description: String? = null,
)

/** Команда создания инвентарного объекта. */
data class MeshCreateInventoryItemCommand(
    val organizationId: String,
    val inventoryNumber: String,
    val localNumber: String? = null,
    val qrCode: String? = null,
    val barcode: String? = null,
    val categoryId: String? = null,
    val subcategoryId: String? = null,
    val itemType: MeshInventoryItemType = MeshInventoryItemType.UNKNOWN,
    val title: String,
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val manufacturer: String? = null,
    val purchaseDate: Instant? = null,
    val commissioningDate: Instant? = null,
    val warrantyUntil: Instant? = null,
    val depreciationGroup: String? = null,
    val usefulLifeMonths: Int? = null,
    val condition: MeshInventoryCondition = MeshInventoryCondition.UNKNOWN,
    val locationId: String? = null,
    val responsiblePerson: String? = null,
    val responsibleDepartment: String? = null,
    val responsibleUserId: String? = null,
    val responsibleOwnerIds: Set<String> = emptySet(),
    val ownerOrganizationId: String? = null,
    val ownerId: String? = null,
    val departmentId: String? = null,
    val costCenterId: String? = null,
    val legalHolderId: String? = null,
    val supplierId: String? = null,
    val fundingSourceId: String? = null,
    val lastInventoryAt: Instant? = null,
    val nextInventoryAt: Instant? = null,
    val tagIds: Set<String> = emptySet(),
    val attributes: List<MeshInventoryAttributeValue> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
)

/** Команда обновления инвентарного объекта. */
data class MeshUpdateInventoryItemCommand(
    val inventoryItemId: String,
    val expectedRevision: Long? = null,
    val inventoryNumber: String? = null,
    val localNumber: String? = null,
    val qrCode: String? = null,
    val barcode: String? = null,
    val categoryId: String? = null,
    val subcategoryId: String? = null,
    val itemType: MeshInventoryItemType? = null,
    val title: String? = null,
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val manufacturer: String? = null,
    val purchaseDate: Instant? = null,
    val commissioningDate: Instant? = null,
    val warrantyUntil: Instant? = null,
    val depreciationGroup: String? = null,
    val usefulLifeMonths: Int? = null,
    val condition: MeshInventoryCondition? = null,
    val locationId: String? = null,
    val responsiblePerson: String? = null,
    val responsibleDepartment: String? = null,
    val responsibleUserId: String? = null,
    val responsibleOwnerIds: Set<String>? = null,
    val ownerOrganizationId: String? = null,
    val ownerId: String? = null,
    val departmentId: String? = null,
    val costCenterId: String? = null,
    val legalHolderId: String? = null,
    val supplierId: String? = null,
    val fundingSourceId: String? = null,
    val lastInventoryAt: Instant? = null,
    val nextInventoryAt: Instant? = null,
    val tagIds: Set<String>? = null,
    val attributes: List<MeshInventoryAttributeValue>? = null,
    val syncStatus: MeshInventorySyncStatus? = null,
    val chatId: String? = null,
    val threadRootMessageId: String? = null,
    val metadata: Map<String, String>? = null,
)

/** Команда изменения статуса объекта. */
data class MeshUpdateInventoryStatusCommand(
    val inventoryItemId: String,
    val expectedRevision: Long? = null,
    val status: MeshInventoryStatus,
    val note: String? = null,
)

/** Команда добавления комментария. */
data class MeshAddInventoryCommentCommand(
    val inventoryItemId: String,
    val sessionId: String? = null,
    val body: String,
)

/** Команда добавления вложения/фото. */
data class MeshAddInventoryAttachmentCommand(
    val inventoryItemId: String,
    val sessionId: String? = null,
    val descriptor: MeshFileDescriptor,
    val transferId: String? = null,
    val attachmentType: MeshInventoryAttachmentType = MeshInventoryAttachmentType.DOCUMENT,
    val note: String? = null,
)

/** Команда привязки обсуждения. */
data class MeshLinkInventoryDiscussionCommand(
    val inventoryItemId: String,
    val chatId: String? = null,
    val threadRootMessageId: String? = null,
)

/** Команда регистрации инцидента. */
data class MeshReportInventoryIncidentCommand(
    val organizationId: String,
    val inventoryItemId: String? = null,
    val sessionId: String? = null,
    val locationId: String? = null,
    val type: MeshInventoryIncidentType,
    val severity: MeshInventoryIncidentSeverity = MeshInventoryIncidentSeverity.MEDIUM,
    val title: String,
    val description: String? = null,
    val assigneePeerIds: Set<String> = emptySet(),
    val attachmentIds: List<String> = emptyList(),
    val comment: String? = null,
)

/** Команда обновления инцидента. */
data class MeshUpdateInventoryIncidentStatusCommand(
    val incidentId: String,
    val status: MeshInventoryIncidentStatus,
    val reviewComment: String? = null,
)

/** Команда создания правила порога. */
data class MeshCreateInventoryRuleThresholdCommand(
    val organizationId: String,
    val ruleType: MeshInventoryRuleType,
    val thresholdValue: Long? = null,
    val active: Boolean = true,
    val metadata: Map<String, String> = emptyMap(),
)

/** Команда обновления правила порога. */
data class MeshUpdateInventoryRuleThresholdCommand(
    val ruleId: String,
    val ruleType: MeshInventoryRuleType? = null,
    val thresholdValue: Long? = null,
    val active: Boolean? = null,
    val metadata: Map<String, String>? = null,
)

/** Команда создания правила дедлайна. */
data class MeshCreateInventoryDeadlineRuleCommand(
    val organizationId: String,
    val target: MeshInventoryDeadlineTarget,
    val daysBefore: Int,
    val active: Boolean = true,
)

/** Команда обновления правила дедлайна. */
data class MeshUpdateInventoryDeadlineRuleCommand(
    val ruleId: String,
    val target: MeshInventoryDeadlineTarget? = null,
    val daysBefore: Int? = null,
    val active: Boolean? = null,
)

/** Команда поиска по инвентарю. */
data class MeshInventorySearchCommand(
    val query: MeshInventorySearchQuery,
)

/** Команда привязки кода. */
data class MeshCreateInventoryCodeBindingCommand(
    val organizationId: String,
    val inventoryItemId: String,
    val sessionId: String? = null,
    val codeType: MeshInventoryCodeType = MeshInventoryCodeType.QR,
    val codeValue: String,
)

/** Команда генерации кода. */
data class MeshGenerateInventoryCodeCommand(
    val organizationId: String,
    val inventoryItemId: String,
    val codeType: MeshInventoryCodeType,
    val barcodeFormat: MeshInventoryBarcodeFormat = MeshInventoryBarcodeFormat.CODE_128,
)

/** Команда перевыпуска кода. */
data class MeshRegenerateInventoryCodeCommand(
    val organizationId: String,
    val inventoryItemId: String,
    val codeType: MeshInventoryCodeType,
    val barcodeFormat: MeshInventoryBarcodeFormat = MeshInventoryBarcodeFormat.CODE_128,
)

/** Команда деактивации кода. */
data class MeshDeactivateInventoryCodeCommand(
    val codeId: String,
)

/** Команда регистрации события сканирования. */
data class MeshRecordInventoryScanCommand(
    val organizationId: String,
    val codeType: MeshInventoryCodeType,
    val codeValue: String,
    val inventoryItemId: String? = null,
    val sessionId: String? = null,
    val locationId: String? = null,
    val note: String? = null,
)

/** Команда разрешения отсканированного кода. */
data class MeshResolveScannedCodeRequest(
    val organizationId: String? = null,
    val rawValue: String,
    val sessionId: String? = null,
    val locationId: String? = null,
    val locationHint: String? = null,
    val deviceId: String? = null,
)

/** Ответ на разрешение кода. */
data class MeshResolveScannedCodeResponse(
    val status: MeshInventoryScanResultStatus,
    val item: MeshInventoryItem? = null,
    val code: MeshInventoryCode? = null,
    val codeType: MeshInventoryCodeType? = null,
    val scanEvent: MeshInventoryScanEvent? = null,
)

/** Команда регистрации расширенного события сканирования. */
data class MeshRegisterScanEventRequest(
    val organizationId: String,
    val codeType: MeshInventoryCodeType,
    val codeValue: String,
    val rawValue: String,
    val inventoryItemId: String? = null,
    val sessionId: String? = null,
    val locationId: String? = null,
    val locationHint: String? = null,
    val deviceId: String? = null,
    val resultStatus: MeshInventoryScanResultStatus = MeshInventoryScanResultStatus.RESOLVED,
    val note: String? = null,
)

/** Команда создания шаблона этикетки. */
data class MeshCreateInventoryLabelTemplateCommand(
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val templateType: MeshInventoryLabelTemplateType,
    val fields: List<MeshInventoryLabelFieldKey> = emptyList(),
    val includeBarcode: Boolean = true,
    val includeQr: Boolean = true,
)

/** Команда обновления шаблона этикетки. */
data class MeshUpdateInventoryLabelTemplateCommand(
    val templateId: String,
    val name: String? = null,
    val description: String? = null,
    val fields: List<MeshInventoryLabelFieldKey>? = null,
    val includeBarcode: Boolean? = null,
    val includeQr: Boolean? = null,
)

/** Команда выбора шаблона по умолчанию. */
data class MeshSelectDefaultLabelTemplateCommand(
    val organizationId: String,
    val templateId: String,
)

/** Запрос генерации этикетки. */
data class MeshGenerateInventoryLabelRequest(
    val organizationId: String,
    val inventoryItemId: String,
    val templateId: String? = null,
    val fields: List<MeshInventoryLabelFieldKey>? = null,
    val includeBarcode: Boolean? = null,
    val includeQr: Boolean? = null,
)

/** Ответ генерации этикетки. */
data class MeshGenerateInventoryLabelResponse(
    val label: MeshInventoryLabel,
    val template: MeshInventoryLabelTemplate,
    val pdfDescriptor: MeshFileDescriptor? = null,
    val localPath: String? = null,
)

/** Запрос печати этикетки. */
data class MeshPrintInventoryLabelRequest(
    val organizationId: String,
    val inventoryItemId: String,
    val templateId: String? = null,
    val fields: List<MeshInventoryLabelFieldKey>? = null,
    val includeBarcode: Boolean? = null,
    val includeQr: Boolean? = null,
)

/** Запрос пакетной печати. */
data class MeshBatchPrintInventoryLabelsRequest(
    val organizationId: String,
    val itemIds: List<String>,
    val templateId: String? = null,
    val fields: List<MeshInventoryLabelFieldKey>? = null,
    val includeBarcode: Boolean? = null,
    val includeQr: Boolean? = null,
)

/** Команда создания инвентаризационной сессии. */
data class MeshCreateInventorySessionCommand(
    val organizationId: String,
    val title: String,
    val description: String? = null,
    val periodStart: Instant,
    val periodEnd: Instant? = null,
    val departmentIds: Set<String> = emptySet(),
    val locationIds: Set<String> = emptySet(),
    val ownerIds: Set<String> = emptySet(),
    val workflowStatus: MeshInventoryWorkflowStatus = MeshInventoryWorkflowStatus.CREATED,
    val requiresPhotoForDiscrepancy: Boolean = true,
    val itemIds: Set<String> = emptySet(),
    val memberPeerIds: Set<String> = emptySet(),
)

/** Команда обновления сессии. */
data class MeshUpdateInventorySessionCommand(
    val sessionId: String,
    val expectedRevision: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val periodStart: Instant? = null,
    val periodEnd: Instant? = null,
    val status: MeshInventorySessionStatus? = null,
    val reviewStatus: MeshInventorySessionReviewStatus? = null,
    val workflowStatus: MeshInventoryWorkflowStatus? = null,
    val result: MeshInventorySessionResult? = null,
    val departmentIds: Set<String>? = null,
    val locationIds: Set<String>? = null,
    val ownerIds: Set<String>? = null,
    val requiresPhotoForDiscrepancy: Boolean? = null,
    val completionBlockedReason: String? = null,
    val chatId: String? = null,
    val threadRootMessageId: String? = null,
    val metadata: Map<String, String>? = null,
)

/** Команда добавления объектов в сессию. */
data class MeshAddItemsToSessionCommand(
    val sessionId: String,
    val itemIds: Set<String>,
)

/** Команда добавления участника сессии. */
data class MeshAddInventorySessionMemberCommand(
    val sessionId: String,
    val peerId: String,
    val role: MeshInventorySessionRole,
)

/** Команда закрытия сессии. */
data class MeshCloseInventorySessionCommand(
    val sessionId: String,
    val note: String? = null,
)

/** Команда подтверждения/отклонения объекта. */
data class MeshSubmitInventoryReviewCommand(
    val inventoryItemId: String,
    val sessionId: String? = null,
    val status: MeshInventoryReviewStatus,
    val presenceStatus: MeshInventoryPresenceStatus = MeshInventoryPresenceStatus.UNCHECKED,
    val acceptanceStatus: MeshInventoryAcceptanceStatus = MeshInventoryAcceptanceStatus.UNCHECKED,
    val confirmationStatus: MeshInventoryConfirmationStatus = MeshInventoryConfirmationStatus.UNCHECKED,
    val requiresPhoto: Boolean = false,
    val comment: String? = null,
)

/** Команда запроса экспорта. */
data class MeshRequestInventoryExportCommand(
    val organizationId: String,
    val sessionId: String,
    val format: MeshInventoryExportFormat,
)

/** Команда обновления статуса экспорта. */
data class MeshUpdateInventoryExportStatusCommand(
    val exportTaskId: String,
    val status: MeshInventoryExportStatus,
    val resultDescriptor: MeshFileDescriptor? = null,
    val errorMessage: String? = null,
)

/** Команда синхронизации инвентаря. */
data class MeshInventorySyncCommand(
    val targetPeerId: String,
    val organizationId: String,
    val sinceSequence: Long? = null,
    val timeoutMillis: Long = 4_000,
)

data class MeshCentralLoginCommand(
    val username: String,
    val password: String,
)

data class MeshSelectActiveCentralOrganizationCommand(
    val organizationId: String,
)

data class MeshCentralSyncCommand(
    val organizationId: String? = null,
    val forcePull: Boolean = true,
    val forcePush: Boolean = true,
)

data class MeshResolveCentralConflictCommand(
    val conflictId: String,
    val resolutionNote: String,
    val winningChangeId: String? = null,
)

data class MeshRequestCentralExportCommand(
    val organizationId: String,
    val sessionId: String,
    val format: MeshInventoryExportFormat = MeshInventoryExportFormat.PDF,
)
