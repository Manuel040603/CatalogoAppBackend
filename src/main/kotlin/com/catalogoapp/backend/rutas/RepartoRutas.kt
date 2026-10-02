package com.catalogoapp.backend.rutas

import com.catalogoapp.backend.firebase.FirebaseService
import com.catalogoapp.backend.modelo.ActualizarUbicacionRequest
import com.catalogoapp.backend.modelo.CrearRepartoRequest
import com.catalogoapp.backend.modelo.ErrorResponse
import com.catalogoapp.backend.repositorio.RepartoRepositorio
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.repartoRutas(repositorio: RepartoRepositorio) {
    route("/repartos") {

        post {
            val uid = call.uidAutenticado() ?: return@post
            val datos = call.receive<CrearRepartoRequest>()
            if (datos.pedidoIds.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("El reparto debe tener al menos un pedido"))
                return@post
            }
            val reparto = repositorio.crear(datos.zona, datos.chofer, datos.vehiculo, datos.direccionOrigen, datos.pedidoIds)
            if (reparto == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("No se pudo geolocalizar el origen o los pedidos indicados"))
                return@post
            }
            call.respond(HttpStatusCode.Created, reparto)
        }

        get("/{id}") {
            val uid = call.uidAutenticado() ?: return@get
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Id de reparto invalido"))
                return@get
            }
            val reparto = repositorio.obtenerPorId(id)
            if (reparto == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Reparto no encontrado"))
            } else {
                call.respond(HttpStatusCode.OK, reparto)
            }
        }

        patch("/{id}/ubicacion") {
            val uid = call.uidAutenticado() ?: return@patch
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Id de reparto invalido"))
                return@patch
            }
            val datos = call.receive<ActualizarUbicacionRequest>()
            val actualizado = repositorio.actualizarUbicacion(id, datos.latitud, datos.longitud)
            if (!actualizado) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Reparto no encontrado"))
                return@patch
            }
            call.respond(HttpStatusCode.OK)
        }

        patch("/{id}/paradas/{paradaId}/entregar") {
            val uid = call.uidAutenticado() ?: return@patch
            val id = call.parameters["id"]?.toIntOrNull()
            val paradaId = call.parameters["paradaId"]?.toIntOrNull()
            if (id == null || paradaId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Id de reparto o parada invalido"))
                return@patch
            }
            val parada = repositorio.marcarParadaEntregada(id, paradaId)
            if (parada == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Parada no encontrada"))
                return@patch
            }
            val token = repositorio.obtenerTokenDispositivoDePedido(parada.pedidoId)
            if (token != null) {
                try {
                    FirebaseService.enviarNotificacion(
                        token,
                        "Pedido entregado",
                        "El pedido de ${parada.nombreCliente} fue marcado como entregado"
                    )
                } catch (e: Exception) {
                }
            }
            call.respond(HttpStatusCode.OK, parada)
        }
    }
}
