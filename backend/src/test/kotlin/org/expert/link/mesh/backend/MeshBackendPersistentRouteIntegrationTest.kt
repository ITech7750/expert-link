package org.expert.link.mesh.backend

import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.expert.link.mesh.contract.api.MeshFileTransferCommand
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.api.MeshStartCallCommand
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.domain.port.repository.PersistentRepositoryBundle
import org.expert.link.mesh.infrastructure.repository.InMemoryBlockListRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCallEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCallParticipantRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCallRoomRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryCallSessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryChatMemberRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryConversationRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryEventLogRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryFileTransferRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryGroupChatRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryGroupEventRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryLocalProfileRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryMessageRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryPairingSessionRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryPeerRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryThreadMessageRepositoryAdapter
import org.expert.link.mesh.infrastructure.repository.InMemoryThreadRepositoryAdapter
import org.junit.jupiter.api.Test

class MeshBackendPersistentRouteIntegrationTest {
    @Test
    fun `should restore peer routes from persisted endpoint hints after restart`() = runBlocking {
        val aliceStore = persistentBundle()
        val bobStore = persistentBundle()
        val alice = MeshBackend.launch(
            configuration = config("persist-route-a", 20181),
            persistentRepositories = aliceStore,
        )
        var bob = MeshBackend.launch(
            configuration = config("persist-route-b", 20182),
            persistentRepositories = bobStore,
        )
        try {
            val invite = alice.createPairingInvite()
            bob.pairWithInvite(invite)
            delay(900)

            bob.stop()
            bob = MeshBackend.launch(
                configuration = config("persist-route-b", 20182),
                persistentRepositories = bobStore,
            )
            delay(250)

            val source = File.createTempFile("persist-route", ".txt").apply {
                writeText("restored-route-file")
                deleteOnExit()
            }
            val transfer = bob.sendFile(
                MeshFileTransferCommand(
                    targetPeerId = alice.profile.peerId,
                    path = source.absolutePath,
                ),
            )

            val inboundTransfer = waitForTransfer(alice, transfer.transferId)
            val outboundCall = bob.startVideoCall(
                MeshStartCallCommand(
                    targetPeerId = alice.profile.peerId,
                    offer = "persist-video-offer",
                ),
            )
            val incomingCall = waitForIncomingCall(alice, outboundCall.callId)

            assertThat(inboundTransfer).isNotNull()
            assertThat(incomingCall).isNotNull()
        } finally {
            runCatching { bob.stop() }
            runCatching { alice.stop() }
        }
    }

    private suspend fun waitForTransfer(node: MeshNode, transferId: String): org.expert.link.mesh.contract.model.MeshFileTransferSession? {
        repeat(30) {
            val transfer = node.fileTransfers().firstOrNull { it.transferId == transferId }
            if (transfer != null) {
                return transfer
            }
            delay(120)
        }
        return null
    }

    private suspend fun waitForIncomingCall(node: MeshNode, callId: String): org.expert.link.mesh.contract.model.MeshCallSession? {
        repeat(30) {
            val incoming = node.observeIncomingCalls().firstOrNull { it.callId == callId }
            if (incoming != null) {
                return incoming
            }
            delay(120)
        }
        return null
    }

    private fun persistentBundle(): PersistentRepositoryBundle = PersistentRepositoryBundle(
        localProfileRepositoryPort = InMemoryLocalProfileRepositoryAdapter(),
        peerRepositoryPort = InMemoryPeerRepositoryAdapter(),
        pairingSessionRepositoryPort = InMemoryPairingSessionRepositoryAdapter(),
        blockListRepositoryPort = InMemoryBlockListRepositoryAdapter(),
        conversationRepositoryPort = InMemoryConversationRepositoryAdapter(),
        messageRepositoryPort = InMemoryMessageRepositoryAdapter(),
        fileTransferRepositoryPort = InMemoryFileTransferRepositoryAdapter(),
        callSessionRepositoryPort = InMemoryCallSessionRepositoryAdapter(),
        callRoomRepositoryPort = InMemoryCallRoomRepositoryAdapter(),
        callParticipantRepositoryPort = InMemoryCallParticipantRepositoryAdapter(),
        callEventRepositoryPort = InMemoryCallEventRepositoryAdapter(),
        groupChatRepositoryPort = InMemoryGroupChatRepositoryAdapter(),
        chatMemberRepositoryPort = InMemoryChatMemberRepositoryAdapter(),
        threadRepositoryPort = InMemoryThreadRepositoryAdapter(),
        threadMessageRepositoryPort = InMemoryThreadMessageRepositoryAdapter(),
        groupEventRepositoryPort = InMemoryGroupEventRepositoryAdapter(),
        eventLogRepositoryPort = InMemoryEventLogRepositoryAdapter(),
        organizationRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationRepositoryAdapter(),
        organizationMemberRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryOrganizationMemberRepositoryAdapter(),
        roleRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryRoleRepositoryAdapter(),
        inventoryCategoryRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCategoryRepositoryAdapter(),
        inventorySubcategoryRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventorySubcategoryRepositoryAdapter(),
        inventoryTagRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryTagRepositoryAdapter(),
        inventoryAttributeDefinitionRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAttributeDefinitionRepositoryAdapter(),
        inventoryCategoryTemplateRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCategoryTemplateRepositoryAdapter(),
        inventoryLocationRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLocationRepositoryAdapter(),
        inventoryOwnerRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryOwnerRepositoryAdapter(),
        inventoryDepartmentRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryDepartmentRepositoryAdapter(),
        inventoryCostCenterRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCostCenterRepositoryAdapter(),
        inventoryLegalHolderRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLegalHolderRepositoryAdapter(),
        inventorySupplierRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventorySupplierRepositoryAdapter(),
        inventoryFundingSourceRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryFundingSourceRepositoryAdapter(),
        inventoryItemRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryItemRepositoryAdapter(),
        inventorySessionRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventorySessionRepositoryAdapter(),
        inventorySessionMemberRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventorySessionMemberRepositoryAdapter(),
        inventoryReviewRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryReviewRepositoryAdapter(),
        inventoryCommentRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCommentRepositoryAdapter(),
        inventoryAttachmentRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAttachmentRepositoryAdapter(),
        inventoryIncidentRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryIncidentRepositoryAdapter(),
        inventoryAlertRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryAlertRepositoryAdapter(),
        inventoryReminderRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryReminderRepositoryAdapter(),
        inventoryThresholdRuleRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryThresholdRuleRepositoryAdapter(),
        inventoryDeadlineRuleRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryDeadlineRuleRepositoryAdapter(),
        inventoryChangeLogRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryChangeLogRepositoryAdapter(),
        inventoryDashboardRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryDashboardRepositoryAdapter(),
        inventoryCodeBindingRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeBindingRepositoryAdapter(),
        inventoryCodeRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryCodeRepositoryAdapter(),
        inventoryLabelTemplateRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLabelTemplateRepositoryAdapter(),
        inventoryLabelRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryLabelRepositoryAdapter(),
        inventoryPrintTaskRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryPrintTaskRepositoryAdapter(),
        inventoryScanEventRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryScanEventRepositoryAdapter(),
        inventoryRevisionRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryRevisionRepositoryAdapter(),
        inventoryConflictRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryConflictRepositoryAdapter(),
        inventoryEventRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryEventRepositoryAdapter(),
        inventoryExportRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryExportRepositoryAdapter(),
        inventoryQrCodeRepositoryPort = org.expert.link.mesh.infrastructure.repository.InMemoryInventoryQrCodeRepositoryAdapter(),
    )

    private fun config(name: String, port: Int): MeshNodeConfig = MeshNodeConfig(
        displayName = name,
        bindHost = "127.0.0.1",
        httpPort = port,
        discoveryPort = port + 1_000,
        multicastGroup = "239.33.33.33",
        featureFlags = MeshFeatureFlags(
            discoveryEnabled = false,
            relayEnabled = false,
            inMemoryTransport = true,
            inMemoryDiscovery = true,
        ),
    )
}
