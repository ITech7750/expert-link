package org.expert.link.mesh.domain.model.network

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.expert.link.mesh.domain.model.call.CallSignal
import org.expert.link.mesh.domain.model.filetransfer.FileChunk
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.filetransfer.FileResumeRequest
import org.expert.link.mesh.domain.model.messaging.ChatType
import org.expert.link.mesh.domain.model.messaging.MessageType

/** Базовый тип payload, который кладётся в пакет. */
@Serializable
sealed interface PacketPayload

/** Запрос на pairing по invite. */
@Serializable
data class PairRequest(
    val protocolVersion: Int,
    val requesterPeerId: String,
    val requesterDisplayName: String,
    val requesterPublicKey: String,
    val inviteSecret: String,
    val requestNonce: String,
    val expiresAt: Instant,
    val endpointHint: PeerEndpoint? = null,
    val capabilities: Set<String> = emptySet(),
) : PacketPayload

/** Подтверждение pairing после проверки invite. */
@Serializable
data class PairAccept(
    val protocolVersion: Int,
    val accepterPeerId: String,
    val accepterDisplayName: String,
    val accepterPublicKey: String,
    val inviteSecret: String,
    val requestNonce: String,
    val acceptNonce: String,
    val expiresAt: Instant,
    val endpointHint: PeerEndpoint? = null,
    val capabilities: Set<String> = emptySet(),
) : PacketPayload

/** Поиск узла через транспорт пакетов. */
@Serializable
data class PeerLookup(
    val requestedPeerId: String,
    val requesterPeerId: String,
    val requesterEndpoint: PeerEndpoint? = null,
    val createdAt: Instant,
) : PacketPayload

/** Объявление endpoint удалённого узла. */
@Serializable
data class PeerAnnounce(
    val peerId: String,
    val displayName: String,
    val publicKey: String,
    val endpoint: PeerEndpoint,
    val capabilities: Set<String> = emptySet(),
    val announcedAt: Instant,
) : PacketPayload

/** Payload чат-сообщения. */
@Serializable
data class ChatMessagePayload(
    val messageId: String,
    val conversationId: String,
    val senderPeerId: String,
    val recipientPeerId: String,
    val body: String,
    val chatType: ChatType = ChatType.DIRECT,
    val chatTitle: String? = null,
    val chatDescription: String? = null,
    val participantPeerIds: Set<String> = emptySet(),
    val messageType: MessageType = MessageType.TEXT,
    val threadRootMessageId: String? = null,
    val parentMessageId: String? = null,
    val replyToMessageId: String? = null,
    val sentAt: Instant,
) : PacketPayload

/** Payload подтверждения доставки. */
@Serializable
data class DeliveryAckPayload(
    val ack: DeliveryAck,
) : PacketPayload

/** Предложение принять файл. */
@Serializable
data class FileOffer(
    val transferId: String,
    val descriptor: FileDescriptor,
    val chunkSizeBytes: Int,
    val totalChunks: Int,
    val senderPeerId: String,
    val recipientPeerId: String,
    val createdAt: Instant,
) : PacketPayload

/** Подтверждение приёма файла. */
@Serializable
data class FileAccept(
    val transferId: String,
    val acceptedAt: Instant,
) : PacketPayload

/** Payload с чанком файла. */
@Serializable
data class FileChunkPayload(
    val chunk: FileChunk,
) : PacketPayload

/** Подтверждение приёма чанка. */
@Serializable
data class FileAck(
    val transferId: String,
    val chunkIndex: Int,
    val acknowledgedAt: Instant,
) : PacketPayload

/** Сообщение об успешной сборке файла. */
@Serializable
data class FileComplete(
    val transferId: String,
    val completedAt: Instant,
    val sha256: String,
) : PacketPayload

/** Payload запроса на дозагрузку чанков. */
@Serializable
data class FileResumeRequestPayload(
    val request: FileResumeRequest,
) : PacketPayload

/** Первый пакет звонка с оффером. */
@Serializable
data class CallInvite(
    val callId: String,
    val conversationId: String? = null,
    val senderPeerId: String,
    val recipientPeerId: String,
    val offer: String,
    val createdAt: Instant,
) : PacketPayload

/** Payload сигнального сообщения звонка. */
@Serializable
data class CallSignalPayload(
    val signal: CallSignal,
) : PacketPayload

/** Сообщение о завершении звонка. */
@Serializable
data class CallHangup(
    val callId: String,
    val senderPeerId: String,
    val recipientPeerId: String,
    val reason: String,
    val createdAt: Instant,
) : PacketPayload

/** Системное служебное событие. */
@Serializable
data class SystemEventPayload(
    val eventType: String,
    val message: String,
    val createdAt: Instant,
    val attributes: Map<String, String> = emptyMap(),
) : PacketPayload
