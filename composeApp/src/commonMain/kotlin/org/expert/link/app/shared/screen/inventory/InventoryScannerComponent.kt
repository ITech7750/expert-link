package org.expert.link.app.shared.screen.inventory

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.expert.link.app.shared.navigation.BaseComponent
import org.expert.link.app.shared.navigation.Config
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.presentation.InventoryScannerStore
import org.expert.link.app.shared.presentation.NodeRuntimeStatus
import org.expert.link.app.shared.presentation.NodeSessionController
import org.expert.link.app.shared.presentation.NodeSessionState
import org.expert.link.mesh.contract.api.MeshAddInventoryAttachmentCommand
import org.expert.link.mesh.contract.api.MeshAddInventoryCommentCommand
import org.expert.link.mesh.contract.api.MeshCreateGroupChatCommand
import org.expert.link.mesh.contract.api.MeshCreateThreadCommand
import org.expert.link.mesh.contract.api.MeshLinkInventoryDiscussionCommand
import org.expert.link.mesh.contract.api.MeshSendMessageCommand
import org.expert.link.mesh.contract.model.MeshInventoryAttachmentType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InventoryScannerComponent(
    context: ComponentContext,
    onNavigate: (Config) -> Unit,
    onBack: () -> Unit,
) : BaseComponent(context, onNavigate, onBack), KoinComponent {
    private val session: NodeSessionController by inject()
    private val services: AppPlatformServices by inject()
    private val store = InventoryScannerStore(session)

    val state = store.state
    val sessionState: StateFlow<NodeSessionState> = session.state
    val capabilities
        get() = services.capabilities

    init {
        componentScope.launch {
            session.state.collectLatest { sessionState ->
                store.updateRuntime(sessionState.status)
            }
        }
    }

    fun goBack() {
        onBack()
    }

    fun updateCode(code: String) {
        store.updateCode(code)
    }

    fun clearNotifications() = store.clearNotifications()

    fun clearResult() = store.clearResult()

    fun lookupQr() {
        val code = state.value.code
        componentScope.launch { store.lookupQr(code) }
    }

    fun lookupBarcode() {
        val code = state.value.code
        componentScope.launch { store.lookupBarcode(code) }
    }

    fun scanViaSystem() {
        componentScope.launch {
            if (session.state.value.status != NodeRuntimeStatus.RUNNING) return@launch
            val result = services.scanQr()
            result.onSuccess { code ->
                if (!code.isNullOrBlank()) {
                    store.updateCode(code)
                    store.lookupQr(code)
                }
            }.onFailure {
                store.updateCode("")
                store.reportError("Не удалось считать код")
            }
        }
    }

    fun openItem(itemId: String) {
        onNavigate(Config.InventoryItem(itemId))
    }

    fun openDiscussion() {
        val item = state.value.result ?: return
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
                val members = node.organizationMembers(item.organizationId).map { it.peerId }.toMutableSet()
                members.add(node.profile.peerId)
                val chat = node.createGroupChat(
                    MeshCreateGroupChatCommand(
                        title = "Инвентарь: ${item.title}",
                        description = "Обсуждение объекта после сканирования",
                        participantPeerIds = members,
                    ),
                )
                val message = node.sendMessage(
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
            result.onSuccess { (chatId, threadId) ->
                onNavigate(
                    Config.Chat(
                        conversationId = chatId,
                        title = "Обсуждение",
                        threadRootMessageId = threadId,
                    ),
                )
            }
            result.onFailure { error ->
                store.reportError(error.message ?: "Не удалось открыть обсуждение")
            }
        }
    }

    fun addComment(body: String) {
        val comment = body.trim()
        if (comment.isBlank()) {
            store.reportError("Введите комментарий")
            return
        }
        val item = state.value.result ?: return
        componentScope.launch {
            val result = session.withNode { node ->
                node.addInventoryComment(
                    MeshAddInventoryCommentCommand(
                        inventoryItemId = item.inventoryItemId,
                        body = comment,
                    ),
                )
            }
            result.onSuccess {
                store.reportMessage("Комментарий добавлен")
            }.onFailure { error ->
                store.reportError(error.message ?: "Не удалось добавить комментарий")
            }
        }
    }

    fun addPhoto() {
        val item = state.value.result ?: return
        componentScope.launch {
            val path = services.pickFile().getOrElse { error ->
                store.reportError(error.message ?: "Не удалось выбрать файл")
                return@launch
            } ?: return@launch
            val descriptor = services.describeFile(path).getOrElse { error ->
                store.reportError(error.message ?: "Не удалось прочитать файл")
                return@launch
            }
            services.cacheLocalArtifact(path, descriptor.fileName).onFailure { error ->
                store.reportError(error.message ?: "Не удалось сохранить локальную копию файла")
                return@launch
            }
            val result = session.withNode { node ->
                node.addInventoryAttachment(
                    MeshAddInventoryAttachmentCommand(
                        inventoryItemId = item.inventoryItemId,
                        descriptor = descriptor,
                        attachmentType = MeshInventoryAttachmentType.PHOTO,
                        note = "Фото после сканирования",
                    ),
                )
            }
            result.onSuccess {
                store.reportMessage("Фото добавлено")
            }.onFailure { error ->
                store.reportError(error.message ?: "Не удалось добавить фото")
            }
        }
    }

    fun markChecked() {
        val item = state.value.result ?: return
        componentScope.launch {
            val result = session.withNode { node ->
                node.addInventoryComment(
                    MeshAddInventoryCommentCommand(
                        inventoryItemId = item.inventoryItemId,
                        body = "Инвентаризация по коду выполнена",
                    ),
                )
            }
            result.onSuccess {
                store.reportMessage("Инвентаризация отмечена")
            }.onFailure { error ->
                store.reportError(error.message ?: "Не удалось отметить инвентаризацию")
            }
        }
    }
}
