package org.expert.link.mesh.simulator.scenario

import org.expert.link.mesh.contract.api.MeshFileTransferCommand

/**
 * Демо отправки файла, resume и cancel.
 *
 * Контракт:
 * - `MeshNode.sendFile`
 * - `MeshNode.fileTransfers`
 * - `MeshNode.resumeFileTransfer`
 * - `MeshNode.cancelFileTransfer`
 *
 * Модели:
 * - запрос: `MeshFileTransferCommand`
 * - ответы: `MeshFileTransferSession`
 */
class FileTransferDemoScenario(
    private val basePort: Int,
) : DemoScenario {
    override val name: String = "FileTransferDemoScenario"

    override suspend fun run(): DemoScenarioReport {
        val sender = ScenarioSupport.launchNode(name = "file-sender", port = basePort)
        val receiver = ScenarioSupport.launchNode(name = "file-receiver", port = basePort + 1)
        return try {
            ScenarioSupport.connectBidirectional(sender, receiver)
            ScenarioSupport.pair(sender, receiver)

            val conversation = sender.openConversation(receiver.profile.peerId)
            val sourceFile = ScenarioSupport.tempFile("mesh-transfer", "file transfer over public contract demo")
            val outbound = sender.sendFile(
                MeshFileTransferCommand(
                    targetPeerId = receiver.profile.peerId,
                    path = sourceFile.absolutePath,
                    conversationId = conversation.conversationId,
                ),
            )
            ScenarioSupport.settle(1_500)

            val senderTransfers = sender.fileTransfers()
            val receiverTransfers = receiver.fileTransfers()
            val resumed = receiver.resumeFileTransfer(receiverTransfers.first().transferId)

            val cancelFile = ScenarioSupport.largeTempFile("mesh-cancel")
            val cancelTransfer = sender.sendFile(
                MeshFileTransferCommand(
                    targetPeerId = receiver.profile.peerId,
                    path = cancelFile.absolutePath,
                    conversationId = conversation.conversationId,
                ),
            )
            val cancelled = sender.cancelFileTransfer(cancelTransfer.transferId)
            ScenarioSupport.settle(500)

            DemoScenarioReport(
                name = name,
                lines = listOf(
                    "Feature: передача файла, повтор недостающих чанков и отмена.",
                    "Imports: MeshNode, MeshFileTransferCommand.",
                    "Public API: sendFile(command), fileTransfers(), resumeFileTransfer(transferId), cancelFileTransfer(transferId).",
                    "Request model: MeshFileTransferCommand(targetPeerId, path, conversationId).",
                    "Response model: MeshFileTransferSession.",
                    "outbound.transferId=${outbound.transferId}",
                    "sender.transfers=${senderTransfers.map { it.transferId + ":" + it.status }}",
                    "receiver.transfers=${receiverTransfers.map { it.transferId + ":" + it.status }}",
                    "resume.result=${resumed?.transferId}:${resumed?.status}",
                    "cancel.result=${cancelled?.transferId}:${cancelled?.status}",
                    "Mobile usage: resumeFileTransfer() вызывается после восстановления сети, cancelFileTransfer() — по действию пользователя в UI.",
                ),
            )
        } finally {
            ScenarioSupport.stopAll(sender, receiver)
        }
    }
}
