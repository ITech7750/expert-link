package org.expert.link.mesh.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.expert.link.mesh.domain.model.network.PacketEnvelope

/** Регистрирует HTTP-маршруты транспорта. */
class PacketRouteController(
    private val packetController: PacketController,
) {
    /** Подключает маршруты под `/api/v1`. */
    fun install(route: Route) {
        route.route("/api/v1") {
            post("/packets") {
                val envelope = call.receive<PacketEnvelope>()
                val result = packetController.handleEnvelope(envelope)
                if (result.success) {
                    call.respond(HttpStatusCode.Accepted)
                } else {
                    call.respond(HttpStatusCode.BadGateway, result.errorMessage ?: "Packet rejected")
                }
            }
        }
    }
}
