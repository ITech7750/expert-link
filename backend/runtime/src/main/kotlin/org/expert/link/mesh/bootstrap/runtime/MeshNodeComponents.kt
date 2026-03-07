package org.expert.link.mesh.bootstrap.runtime

import org.expert.link.mesh.application.service.BlockListService
import org.expert.link.mesh.application.service.CallSignalingService
import org.expert.link.mesh.application.service.ChatMessagingService
import org.expert.link.mesh.application.service.FileTransferService
import org.expert.link.mesh.application.service.GroupChatService
import org.expert.link.mesh.application.service.RoutingService
import org.expert.link.mesh.application.service.ThreadService
import org.expert.link.mesh.domain.port.repository.ChatMemberRepositoryPort
import org.expert.link.mesh.domain.port.repository.BlockListRepositoryPort
import org.expert.link.mesh.domain.port.repository.CallSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.ConversationRepositoryPort
import org.expert.link.mesh.domain.port.repository.EndpointCachePort
import org.expert.link.mesh.domain.port.repository.FileTransferRepositoryPort
import org.expert.link.mesh.domain.port.repository.GroupChatRepositoryPort
import org.expert.link.mesh.domain.port.repository.GroupEventRepositoryPort
import org.expert.link.mesh.domain.port.repository.MessageRepositoryPort
import org.expert.link.mesh.domain.port.repository.PairingSessionRepositoryPort
import org.expert.link.mesh.domain.port.repository.PeerRepositoryPort
import org.expert.link.mesh.domain.port.repository.RouteRepositoryPort
import org.expert.link.mesh.domain.port.repository.ThreadMessageRepositoryPort
import org.expert.link.mesh.domain.port.repository.ThreadRepositoryPort

/** Внутренний контейнер зависимостей runtime. */
data class MeshNodeComponents(
    val runtime: MeshNodeRuntime,
    val peerRepositoryPort: PeerRepositoryPort,
    val pairingSessionRepositoryPort: PairingSessionRepositoryPort,
    val blockListRepositoryPort: BlockListRepositoryPort,
    val conversationRepositoryPort: ConversationRepositoryPort,
    val messageRepositoryPort: MessageRepositoryPort,
    val fileTransferRepositoryPort: FileTransferRepositoryPort,
    val callSessionRepositoryPort: CallSessionRepositoryPort,
    val endpointCachePort: EndpointCachePort,
    val routeRepositoryPort: RouteRepositoryPort,
    val groupChatRepositoryPort: GroupChatRepositoryPort,
    val chatMemberRepositoryPort: ChatMemberRepositoryPort,
    val threadRepositoryPort: ThreadRepositoryPort,
    val threadMessageRepositoryPort: ThreadMessageRepositoryPort,
    val groupEventRepositoryPort: GroupEventRepositoryPort,
    val blockListService: BlockListService,
    val chatMessagingService: ChatMessagingService,
    val groupChatService: GroupChatService,
    val threadService: ThreadService,
    val fileTransferService: FileTransferService,
    val callSignalingService: CallSignalingService,
    val routingService: RoutingService,
)
