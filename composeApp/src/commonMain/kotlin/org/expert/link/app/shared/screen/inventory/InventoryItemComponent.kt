package org.expert.link.app.shared.screen.inventory

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.expert.link.app.shared.navigation.BaseComponent
import org.expert.link.app.shared.navigation.Config
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.presentation.InventoryItemStore
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.presentation.NodeSessionController
import org.expert.link.app.shared.presentation.NodeSessionState
import org.expert.link.mesh.contract.api.MeshCreateGroupChatCommand
import org.expert.link.mesh.contract.api.MeshCreateThreadCommand
import org.expert.link.mesh.contract.api.MeshLinkInventoryDiscussionCommand
import org.expert.link.mesh.contract.api.MeshSendMessageCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventoryItemCommand
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshInventoryAttachmentType
import org.expert.link.mesh.contract.model.MeshInventoryAcceptanceStatus
import org.expert.link.mesh.contract.model.MeshInventoryAttachment
import org.expert.link.mesh.contract.model.MeshInventoryAttributeValue
import org.expert.link.mesh.contract.model.MeshInventoryCodeType
import org.expert.link.mesh.contract.model.MeshInventoryCondition
import org.expert.link.mesh.contract.model.MeshInventoryConfirmationStatus
import org.expert.link.mesh.contract.model.MeshInventoryIncidentSeverity
import org.expert.link.mesh.contract.model.MeshInventoryIncidentStatus
import org.expert.link.mesh.contract.model.MeshInventoryIncidentType
import org.expert.link.mesh.contract.model.MeshInventoryLabelFieldKey
import org.expert.link.mesh.contract.model.MeshInventoryPresenceStatus
import org.expert.link.mesh.contract.model.MeshInventoryReviewStatus
import org.expert.link.mesh.contract.model.MeshInventoryStatus
import org.expert.link.mesh.contract.model.MeshOrganizationMemberStatus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InventoryItemComponent(
    context: ComponentContext,
    onNavigate: (Config) -> Unit,
    onBack: () -> Unit,
    private val itemId: String,
) : BaseComponent(context, onNavigate, onBack), KoinComponent {
    private val session: NodeSessionController by inject()
    private val services: AppPlatformServices by inject()
    private val store = InventoryItemStore(session)

    val state = store.state
    val sessionState: StateFlow<NodeSessionState> = session.state
    val capabilities
        get() = services.capabilities
    fun buildQrCode(text: String) = services.buildQrCode(text)
    fun buildBarcode(text: String) = services.buildBarcode(text)

    init {
        observeSession()
        startPolling()
    }

    private fun observeSession() {
        componentScope.launch {
            session.state.collectLatest { sessionState ->
                store.updateRuntime(sessionState.status, sessionState.profile?.peerId)
                if (sessionState.status == NodeRuntimeStatus.RUNNING) {
                    store.refresh(itemId)
                }
            }
        }
    }

    private fun startPolling() {
        componentScope.launch {
            while (isActive) {
                if (session.state.value.status == NodeRuntimeStatus.RUNNING) {
                    store.refresh(itemId)
                }
                delay(4_000)
            }
        }
    }

    fun goBack() {
        onBack()
    }

    fun clearError() = store.clearError()

    fun clearMessage() = store.clearMessage()

    fun updateStatus(status: MeshInventoryStatus, note: String?) {
        val item = state.value.item ?: return
        componentScope.launch {
            store.updateStatus(item.inventoryItemId, item.revision, status, note)
        }
    }

    fun updateItem(
        inventoryNumber: String?,
        localNumber: String?,
        title: String?,
        description: String?,
        brand: String?,
        model: String?,
        serialNumber: String?,
        manufacturer: String?,
        condition: MeshInventoryCondition?,
        categoryId: String?,
        subcategoryId: String?,
        locationId: String?,
        responsiblePerson: String?,
        responsibleDepartment: String?,
        ownerId: String?,
        departmentId: String?,
        responsibleOwnerIds: Set<String>?,
        costCenterId: String?,
        legalHolderId: String?,
        supplierId: String?,
        fundingSourceId: String?,
        tagIds: Set<String>?,
        attributes: List<MeshInventoryAttributeValue>?,
    ) {
        val item = state.value.item ?: return
        componentScope.launch {
            store.updateItem(
                itemId = item.inventoryItemId,
                expectedRevision = item.revision,
                inventoryNumber = inventoryNumber,
                localNumber = localNumber,
                title = title,
                description = description,
                brand = brand,
                model = model,
                serialNumber = serialNumber,
                manufacturer = manufacturer,
                condition = condition,
                categoryId = categoryId,
                subcategoryId = subcategoryId,
                locationId = locationId,
                responsiblePerson = responsiblePerson,
                responsibleDepartment = responsibleDepartment,
                ownerId = ownerId,
                departmentId = departmentId,
                responsibleOwnerIds = responsibleOwnerIds,
                costCenterId = costCenterId,
                legalHolderId = legalHolderId,
                supplierId = supplierId,
                fundingSourceId = fundingSourceId,
                tagIds = tagIds,
                attributes = attributes,
            )
        }
    }

    fun addComment(text: String) {
        val item = state.value.item ?: return
        componentScope.launch {
            store.addComment(item.inventoryItemId, null, text)
        }
    }

    fun addAttachment(type: MeshInventoryAttachmentType, note: String?) {
        val item = state.value.item ?: return
        componentScope.launch {
            val path = services.pickFile().getOrElse { error ->
                store.reportError(error.message ?: "Не удалось выбрать файл")
                return@launch
            }
            if (path.isNullOrBlank()) return@launch
            val descriptor = services.describeFile(path).getOrElse { error ->
                store.reportError(error.message ?: "Не удалось прочитать файл")
                return@launch
            }
            services.cacheLocalArtifact(path, descriptor.fileName).onFailure { error ->
                store.reportError(error.message ?: "Не удалось сохранить локальную копию файла")
                return@launch
            }
            store.addAttachment(item.inventoryItemId, null, descriptor, type, note)
        }
    }

    fun submitReview(status: MeshInventoryReviewStatus, comment: String?) {
        val item = state.value.item ?: return
        componentScope.launch {
            val isPositive = status == MeshInventoryReviewStatus.APPROVED
            store.submitReview(
                itemId = item.inventoryItemId,
                sessionId = null,
                status = status,
                presenceStatus = if (isPositive) MeshInventoryPresenceStatus.PRESENT else MeshInventoryPresenceStatus.ABSENT,
                acceptanceStatus = if (isPositive) MeshInventoryAcceptanceStatus.ACCEPTED else MeshInventoryAcceptanceStatus.NOT_ACCEPTED,
                confirmationStatus = if (isPositive) MeshInventoryConfirmationStatus.CONFIRMED else MeshInventoryConfirmationStatus.NOT_CONFIRMED,
                requiresPhoto = !isPositive,
                comment = comment,
            )
        }
    }

    fun createCodeBinding(codeType: MeshInventoryCodeType, codeValue: String) {
        val item = state.value.item ?: return
        componentScope.launch {
            store.createCodeBinding(item.organizationId, item.inventoryItemId, codeType, codeValue)
        }
    }

    fun generateCode(codeType: MeshInventoryCodeType, regenerate: Boolean = false) {
        val item = state.value.item ?: return
        componentScope.launch {
            store.generateCode(item.organizationId, item.inventoryItemId, codeType, regenerate)
        }
    }

    fun deactivateCode(codeId: String) {
        val item = state.value.item ?: return
        componentScope.launch {
            store.deactivateCode(item.inventoryItemId, codeId)
        }
    }

    fun generateLabelPreview(
        templateId: String?,
        fields: List<MeshInventoryLabelFieldKey>?,
        includeBarcode: Boolean,
        includeQr: Boolean,
    ) {
        val item = state.value.item ?: return
        componentScope.launch {
            store.generateLabelPreview(
                organizationId = item.organizationId,
                itemId = item.inventoryItemId,
                templateId = templateId,
                fields = fields,
                includeBarcode = includeBarcode,
                includeQr = includeQr,
            )
        }
    }

    fun generateLabelPdf(
        templateId: String?,
        fields: List<MeshInventoryLabelFieldKey>?,
        includeBarcode: Boolean,
        includeQr: Boolean,
    ) {
        val item = state.value.item ?: return
        componentScope.launch {
            store.generateLabelPdf(
                organizationId = item.organizationId,
                itemId = item.inventoryItemId,
                templateId = templateId,
                fields = fields,
                includeBarcode = includeBarcode,
                includeQr = includeQr,
            )
        }
    }

    fun printLabel(
        templateId: String?,
        fields: List<MeshInventoryLabelFieldKey>?,
        includeBarcode: Boolean,
        includeQr: Boolean,
    ) {
        val item = state.value.item ?: return
        componentScope.launch {
            store.printLabel(
                organizationId = item.organizationId,
                itemId = item.inventoryItemId,
                templateId = templateId,
                fields = fields,
                includeBarcode = includeBarcode,
                includeQr = includeQr,
            )
        }
    }

    fun savePdf(path: String, fileName: String) {
        componentScope.launch {
            val result = services.saveFileToDownloads(path, fileName)
            result.onSuccess {
                store.reportMessage("PDF сохранён")
            }.onFailure { error ->
                store.reportError(error.message ?: "Не удалось сохранить PDF")
            }
        }
    }

    fun sharePdf(path: String) {
        componentScope.launch {
            val result = services.shareFile("Маркировка", path)
            result.onSuccess { store.reportMessage("Файл передан в меню отправки") }
                .onFailure { error ->
                    store.reportError(error.message ?: "Не удалось поделиться PDF")
                }
        }
    }

    fun printPdf(path: String) {
        componentScope.launch {
            val result = services.printFile(path)
            result.onSuccess { store.reportMessage("PDF отправлен в печать") }
                .onFailure { error ->
                    store.reportError(error.message ?: "Не удалось отправить PDF на печать")
                }
        }
    }

    fun openAttachment(attachment: MeshInventoryAttachment) {
        componentScope.launch {
            val resolvedPath = services.resolveLocalArtifactPath(attachment.descriptor.fileName)
                .getOrElse { error ->
                    store.reportError(error.message ?: "Не удалось найти вложение")
                    return@launch
                }
            if (resolvedPath.isNullOrBlank()) {
                store.reportError("Локальный файл вложения пока не найден")
                return@launch
            }
            services.openFile(resolvedPath)
                .onSuccess { store.reportMessage("Файл открыт") }
                .onFailure { error ->
                    store.reportError(error.message ?: "Не удалось открыть вложение")
                }
        }
    }

    fun copyText(label: String, text: String) {
        componentScope.launch {
            services.copyText(label, text).onFailure { error ->
                store.reportError(error.message ?: "Не удалось скопировать текст")
            }
        }
    }

    fun shareText(label: String, text: String) {
        componentScope.launch {
            services.shareText(label, text).onFailure { error ->
                store.reportError(error.message ?: "Не удалось поделиться данными")
            }
        }
    }

    fun shareQrImage(text: String) {
        componentScope.launch {
            services.shareQrImage("QR маркировки", text)
                .onSuccess { store.reportMessage("QR отправлен как изображение") }
                .onFailure { error ->
                    store.reportError(error.message ?: "Не удалось отправить QR как изображение")
                }
        }
    }

    fun reportIncident(
        type: MeshInventoryIncidentType,
        severity: MeshInventoryIncidentSeverity,
        title: String,
        description: String?,
    ) {
        val item = state.value.item ?: return
        componentScope.launch {
            store.reportIncident(
                organizationId = item.organizationId,
                itemId = item.inventoryItemId,
                sessionId = null,
                locationId = item.locationId,
                type = type,
                severity = severity,
                title = title,
                description = description,
            )
        }
    }

    fun updateIncidentStatus(incidentId: String, status: MeshInventoryIncidentStatus, comment: String?) {
        val item = state.value.item ?: return
        componentScope.launch {
            store.updateIncidentStatus(item.inventoryItemId, incidentId, status, comment)
        }
    }

    fun openDiscussion() {
        val item = state.value.item ?: return
        val existingChatId = item.chatId
        if (!existingChatId.isNullOrBlank()) {
            onNavigate(
                Config.Chat(
                    conversationId = existingChatId,
                    title = "Обсуждение",
                    threadRootMessageId = item.threadRootMessageId,
                ),
            )
            return
        }
        componentScope.launch {
            val result = session.withNode { node ->
                val members = node.organizationMembers(item.organizationId)
                    .filter { it.status == MeshOrganizationMemberStatus.ACTIVE }
                    .map { it.peerId }
                    .toMutableSet()
                members.add(node.profile.peerId)
                val chat = node.createGroupChat(
                    MeshCreateGroupChatCommand(
                        title = "Инвентарь: ${item.title}",
                        description = "Обсуждение объекта",
                        participantPeerIds = members,
                    ),
                )
                val message: MeshChatMessage = node.sendMessage(
                    MeshSendMessageCommand(
                        chatId = chat.conversationId,
                        body = "Обсуждение объекта: ${item.title}",
                    ),
                )
                val thread = node.createThread(
                    MeshCreateThreadCommand(
                        chatId = chat.conversationId,
                        rootMessageId = message.messageId,
                    ),
                )
                node.linkInventoryDiscussion(
                    MeshLinkInventoryDiscussionCommand(
                        inventoryItemId = item.inventoryItemId,
                        chatId = chat.conversationId,
                        threadRootMessageId = thread.rootMessageId,
                    ),
                )
                chat.conversationId to thread.rootMessageId
            }
            result.fold(
                onSuccess = { (chatId, threadId) ->
                    onNavigate(
                        Config.Chat(
                            conversationId = chatId,
                            title = "Обсуждение",
                            threadRootMessageId = threadId,
                        ),
                    )
                },
                onFailure = { error ->
                    store.reportError(error.message ?: "Не удалось создать обсуждение")
                },
            )
            if (result.isSuccess) {
                store.refresh(item.inventoryItemId)
            }
        }
    }

    fun generateQrCode() {
        val item = state.value.item ?: return
        if (!item.qrCode.isNullOrBlank()) return
        componentScope.launch {
            val code = "inventory:${item.inventoryItemId}"
            val result = session.withNode { node ->
                node.updateInventoryItem(
                    MeshUpdateInventoryItemCommand(
                        inventoryItemId = item.inventoryItemId,
                        expectedRevision = item.revision,
                        qrCode = code,
                    ),
                )
            }
            result.fold(
                onSuccess = { store.refresh(item.inventoryItemId) },
                onFailure = { error ->
                    store.reportError(error.message ?: "Не удалось обновить QR")
                },
            )
        }
    }
}
