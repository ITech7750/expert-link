package org.expert.link.mesh.application.service

import kotlinx.serialization.json.Json
import org.expert.link.mesh.domain.model.network.CallHangup
import org.expert.link.mesh.domain.model.network.CallInvite
import org.expert.link.mesh.domain.model.network.CallSignalPayload
import org.expert.link.mesh.domain.model.network.ChatMessagePayload
import org.expert.link.mesh.domain.model.network.DeliveryAckPayload
import org.expert.link.mesh.domain.model.network.FileAccept
import org.expert.link.mesh.domain.model.network.FileAck
import org.expert.link.mesh.domain.model.network.FileChunkPayload
import org.expert.link.mesh.domain.model.network.FileComplete
import org.expert.link.mesh.domain.model.network.FileOffer
import org.expert.link.mesh.domain.model.network.FileResumeRequestPayload
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.PacketPayload
import org.expert.link.mesh.domain.model.network.PacketType
import org.expert.link.mesh.domain.model.network.PairAccept
import org.expert.link.mesh.domain.model.network.PairRequest
import org.expert.link.mesh.domain.model.network.PeerAnnounce
import org.expert.link.mesh.domain.model.network.PeerLookup
import org.expert.link.mesh.domain.model.network.SystemEventPayload

/** Сериализует пакеты и типизированные payload в одном JSON-формате. */
class PacketSerializationService {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /**
     * Serializes a packet envelope into UTF-8 JSON.
     */
    fun serializeEnvelope(envelope: PacketEnvelope): String = json.encodeToString(PacketEnvelope.serializer(), envelope)

    /**
     * Deserializes a packet envelope from UTF-8 JSON.
     */
    fun deserializeEnvelope(serialized: String): PacketEnvelope = json.decodeFromString(PacketEnvelope.serializer(), serialized)

    /**
     * Serializes a typed packet payload into UTF-8 bytes ready for encryption.
     */
    fun serializePayload(payload: PacketPayload): ByteArray = when (payload) {
        is PairRequest -> json.encodeToString(PairRequest.serializer(), payload)
        is PairAccept -> json.encodeToString(PairAccept.serializer(), payload)
        is PeerLookup -> json.encodeToString(PeerLookup.serializer(), payload)
        is PeerAnnounce -> json.encodeToString(PeerAnnounce.serializer(), payload)
        is ChatMessagePayload -> json.encodeToString(ChatMessagePayload.serializer(), payload)
        is DeliveryAckPayload -> json.encodeToString(DeliveryAckPayload.serializer(), payload)
        is FileOffer -> json.encodeToString(FileOffer.serializer(), payload)
        is FileAccept -> json.encodeToString(FileAccept.serializer(), payload)
        is FileChunkPayload -> json.encodeToString(FileChunkPayload.serializer(), payload)
        is FileAck -> json.encodeToString(FileAck.serializer(), payload)
        is FileComplete -> json.encodeToString(FileComplete.serializer(), payload)
        is FileResumeRequestPayload -> json.encodeToString(FileResumeRequestPayload.serializer(), payload)
        is CallInvite -> json.encodeToString(CallInvite.serializer(), payload)
        is CallSignalPayload -> json.encodeToString(CallSignalPayload.serializer(), payload)
        is CallHangup -> json.encodeToString(CallHangup.serializer(), payload)
        is SystemEventPayload -> json.encodeToString(SystemEventPayload.serializer(), payload)
    }.toByteArray(Charsets.UTF_8)

    /**
     * Deserializes a payload according to the packet type stored in the envelope.
     */
    fun deserializePayload(packetType: PacketType, payloadBytes: ByteArray): PacketPayload {
        val text = payloadBytes.toString(Charsets.UTF_8)
        return when (packetType) {
            PacketType.PAIR_REQUEST -> json.decodeFromString(PairRequest.serializer(), text)
            PacketType.PAIR_ACCEPT -> json.decodeFromString(PairAccept.serializer(), text)
            PacketType.PEER_LOOKUP -> json.decodeFromString(PeerLookup.serializer(), text)
            PacketType.PEER_ANNOUNCE -> json.decodeFromString(PeerAnnounce.serializer(), text)
            PacketType.CHAT_MESSAGE -> json.decodeFromString(ChatMessagePayload.serializer(), text)
            PacketType.DELIVERY_ACK -> json.decodeFromString(DeliveryAckPayload.serializer(), text)
            PacketType.FILE_OFFER -> json.decodeFromString(FileOffer.serializer(), text)
            PacketType.FILE_ACCEPT -> json.decodeFromString(FileAccept.serializer(), text)
            PacketType.FILE_CHUNK -> json.decodeFromString(FileChunkPayload.serializer(), text)
            PacketType.FILE_ACK -> json.decodeFromString(FileAck.serializer(), text)
            PacketType.FILE_COMPLETE -> json.decodeFromString(FileComplete.serializer(), text)
            PacketType.FILE_RESUME_REQUEST -> json.decodeFromString(FileResumeRequestPayload.serializer(), text)
            PacketType.CALL_INVITE -> json.decodeFromString(CallInvite.serializer(), text)
            PacketType.CALL_SIGNAL -> json.decodeFromString(CallSignalPayload.serializer(), text)
            PacketType.CALL_HANGUP -> json.decodeFromString(CallHangup.serializer(), text)
            PacketType.SYSTEM_EVENT -> json.decodeFromString(SystemEventPayload.serializer(), text)
        }
    }
}
