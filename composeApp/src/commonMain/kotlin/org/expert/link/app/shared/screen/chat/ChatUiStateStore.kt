package org.expert.link.app.shared.screen.chat

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class ChatUiStateStore {
    private val dismissedTransfersByConversation = ConcurrentHashMap<String, MutableSet<String>>()

    fun dismissedTransfers(conversationId: String): Set<String> {
        return dismissedTransfersByConversation[conversationId]?.toSet().orEmpty()
    }

    fun dismissTransfer(conversationId: String, transferId: String) {
        val transfers = dismissedTransfersByConversation.getOrPut(conversationId) {
            Collections.newSetFromMap(ConcurrentHashMap())
        }
        transfers += transferId
    }

    fun restoreTransfer(conversationId: String, transferId: String) {
        dismissedTransfersByConversation[conversationId]?.remove(transferId)
    }

    fun retainTransfers(conversationId: String, activeTransferIds: Set<String>): Set<String> {
        val transfers = dismissedTransfersByConversation[conversationId] ?: return emptySet()
        transfers.retainAll(activeTransferIds)
        if (transfers.isEmpty()) {
            dismissedTransfersByConversation.remove(conversationId)
            return emptySet()
        }
        return transfers.toSet()
    }
}
