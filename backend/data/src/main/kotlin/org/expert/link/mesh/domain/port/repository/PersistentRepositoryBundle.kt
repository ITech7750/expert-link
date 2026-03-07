package org.expert.link.mesh.domain.port.repository

import org.expert.link.mesh.domain.port.external.EventLogRepositoryPort

/** Набор persistent repository ports, которые runtime может получить извне. */
data class PersistentRepositoryBundle(
    val localProfileRepositoryPort: LocalProfileRepositoryPort,
    val peerRepositoryPort: PeerRepositoryPort,
    val pairingSessionRepositoryPort: PairingSessionRepositoryPort,
    val blockListRepositoryPort: BlockListRepositoryPort,
    val conversationRepositoryPort: ConversationRepositoryPort,
    val messageRepositoryPort: MessageRepositoryPort,
    val fileTransferRepositoryPort: FileTransferRepositoryPort,
    val callSessionRepositoryPort: CallSessionRepositoryPort,
    val callRoomRepositoryPort: CallRoomRepositoryPort,
    val callParticipantRepositoryPort: CallParticipantRepositoryPort,
    val callEventRepositoryPort: CallEventRepositoryPort,
    val groupChatRepositoryPort: GroupChatRepositoryPort,
    val chatMemberRepositoryPort: ChatMemberRepositoryPort,
    val threadRepositoryPort: ThreadRepositoryPort,
    val threadMessageRepositoryPort: ThreadMessageRepositoryPort,
    val groupEventRepositoryPort: GroupEventRepositoryPort,
    val eventLogRepositoryPort: EventLogRepositoryPort,
)
