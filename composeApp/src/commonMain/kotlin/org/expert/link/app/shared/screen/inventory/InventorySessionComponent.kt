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
import org.expert.link.app.shared.presentation.InventorySessionReviewDraft
import org.expert.link.app.shared.presentation.InventorySessionStore
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.presentation.NodeSessionController
import org.expert.link.app.shared.presentation.NodeSessionState
import org.expert.link.mesh.contract.api.MeshBatchPrintInventoryLabelsRequest
import org.expert.link.mesh.contract.api.MeshCreateGroupChatCommand
import org.expert.link.mesh.contract.api.MeshCreateThreadCommand
import org.expert.link.mesh.contract.api.MeshSendMessageCommand
import org.expert.link.mesh.contract.api.MeshUpdateInventorySessionCommand
import org.expert.link.mesh.contract.model.MeshInventoryExportFormat
import org.expert.link.mesh.contract.model.MeshInventoryLabelFieldKey
import org.expert.link.mesh.contract.model.MeshInventoryPrintTask
import org.expert.link.mesh.contract.model.MeshInventorySessionRole
import org.expert.link.mesh.contract.model.MeshOrganizationMemberStatus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InventorySessionComponent(
    context: ComponentContext,
    onNavigate: (Config) -> Unit,
    onBack: () -> Unit,
    private val sessionId: String,
) : BaseComponent(context, onNavigate, onBack), KoinComponent {
    private val session: NodeSessionController by inject()
    private val services: AppPlatformServices by inject()
    private val store = InventorySessionStore(session)

    val state = store.state
    val sessionState: StateFlow<NodeSessionState> = session.state
    val capabilities
        get() = services.capabilities

    init {
        observeSession()
        startPolling()
    }

    private fun observeSession() {
        componentScope.launch {
            session.state.collectLatest { sessionState ->
                store.updateRuntime(sessionState.status, sessionState.profile?.peerId)
                if (sessionState.status == NodeRuntimeStatus.RUNNING) {
                    store.refresh(sessionId)
                }
            }
        }
    }

    private fun startPolling() {
        componentScope.launch {
            while (isActive) {
                if (session.state.value.status == NodeRuntimeStatus.RUNNING) {
                    store.refresh(sessionId)
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

    fun openItem(itemId: String) {
        onNavigate(Config.InventoryItem(itemId))
    }

    fun addMember(peerId: String, role: MeshInventorySessionRole) {
        componentScope.launch {
            store.addMember(sessionId, peerId, role)
        }
    }

    fun addItems(itemIds: Set<String>) {
        componentScope.launch {
            store.addItems(sessionId, itemIds)
        }
    }

    fun saveReviews(drafts: List<InventorySessionReviewDraft>) {
        componentScope.launch {
            store.saveReviews(sessionId, drafts)
        }
    }

    fun addAttachment(itemId: String, note: String?) {
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
            store.addAttachment(sessionId, itemId, descriptor, note)
        }
    }

    fun reportCorrectionRequest(title: String, description: String?, assigneePeerIds: Set<String>) {
        val organizationId = state.value.session?.organizationId ?: return
        componentScope.launch {
            store.reportCorrectionRequest(
                sessionId = sessionId,
                organizationId = organizationId,
                title = title,
                description = description,
                assigneePeerIds = assigneePeerIds,
            )
        }
    }

    fun closeSession() {
        componentScope.launch {
            store.closeSession(sessionId)
        }
    }

    fun requestExport(format: MeshInventoryExportFormat) {
        val organizationId = state.value.session?.organizationId ?: return
        componentScope.launch {
            store.requestExport(sessionId, organizationId, format)
        }
    }

    fun printLabelsBatch(
        itemIds: List<String>,
        templateId: String?,
        fields: List<MeshInventoryLabelFieldKey>?,
        includeBarcode: Boolean,
        includeQr: Boolean,
        onResult: (Result<MeshInventoryPrintTask>) -> Unit,
    ) {
        val organizationId = state.value.session?.organizationId ?: return
        componentScope.launch {
            val result = session.withNode { node ->
                node.printInventoryLabelsBatch(
                    MeshBatchPrintInventoryLabelsRequest(
                        organizationId = organizationId,
                        itemIds = itemIds,
                        templateId = templateId,
                        fields = fields,
                        includeBarcode = includeBarcode,
                        includeQr = includeQr,
                    ),
                )
            }
            onResult(result)
            if (result.isSuccess) {
                store.refresh(sessionId)
            }
        }
    }

    fun savePdf(path: String, fileName: String, onResult: (Result<String?>) -> Unit) {
        componentScope.launch {
            onResult(services.saveFileToDownloads(path, fileName))
        }
    }

    fun sharePdf(path: String, onResult: (Result<Unit>) -> Unit) {
        componentScope.launch {
            onResult(services.shareFile("Маркировка", path))
        }
    }

    fun printPdf(path: String, onResult: (Result<Unit>) -> Unit) {
        componentScope.launch {
            onResult(services.printFile(path))
        }
    }

    fun openDiscussion() {
        val current = state.value.session ?: return
        val existingChatId = current.chatId
        if (!existingChatId.isNullOrBlank()) {
            onNavigate(
                Config.Chat(
                    conversationId = existingChatId,
                    title = current.title,
                    threadRootMessageId = current.threadRootMessageId,
                ),
            )
            return
        }
        componentScope.launch {
            val result = session.withNode { node ->
                val members = state.value.members.map { it.peerId }.toMutableSet()
                if (members.isEmpty()) {
                    state.value.organizationMembers
                        .filter { it.status == MeshOrganizationMemberStatus.ACTIVE }
                        .forEach { members.add(it.peerId) }
                }
                members.add(node.profile.peerId)
                val chat = node.createGroupChat(
                    MeshCreateGroupChatCommand(
                        title = "Сессия: ${current.title}",
                        description = "Комиссия и обсуждение",
                        participantPeerIds = members,
                    ),
                )
                val message = node.sendMessage(
                    MeshSendMessageCommand(
                        chatId = chat.conversationId,
                        body = "Обсуждение сессии: ${current.title}",
                    ),
                )
                val thread = node.createThread(
                    MeshCreateThreadCommand(
                        chatId = chat.conversationId,
                        rootMessageId = message.messageId,
                    ),
                )
                node.updateInventorySession(
                    MeshUpdateInventorySessionCommand(
                        sessionId = current.sessionId,
                        expectedRevision = current.revision,
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
                            title = current.title,
                            threadRootMessageId = threadId,
                        ),
                    )
                },
                onFailure = { error ->
                    store.reportError(error.message ?: "Не удалось создать обсуждение")
                },
            )
            if (result.isSuccess) {
                store.refresh(sessionId)
            }
        }
    }
}
