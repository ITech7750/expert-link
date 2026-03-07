package org.expert.link.mesh.infrastructure.adapter

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.expert.link.mesh.domain.model.network.PacketEnvelope
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.TransportDeliveryResult
import org.expert.link.mesh.domain.port.external.PacketTransportPort

/** HTTP transport-адаптер на Ktor client. */
class KtorPacketTransportAdapter : PacketTransportPort {
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { encodeDefaults = true; ignoreUnknownKeys = true })
        }
    }

    override suspend fun sendPacket(endpoint: PeerEndpoint, envelope: PacketEnvelope): TransportDeliveryResult {
        return runCatching {
            val response = httpClient.post(endpoint.asUrl()) {
                contentType(ContentType.Application.Json)
                setBody(envelope)
            }
            if (response.status.value in 200..299) {
                TransportDeliveryResult(success = true, statusCode = response.status.value, deliveredAt = Clock.System.now())
            } else {
                TransportDeliveryResult(success = false, statusCode = response.status.value, errorMessage = response.bodyAsText())
            }
        }.getOrElse { error ->
            TransportDeliveryResult(success = false, errorMessage = error.message)
        }
    }

    /**
     * Closes the underlying HTTP client.
     */
    fun close() {
        httpClient.close()
    }
}
