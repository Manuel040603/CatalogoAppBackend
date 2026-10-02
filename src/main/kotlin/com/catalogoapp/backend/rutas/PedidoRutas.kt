package com.catalogoapp.backend.rutas

import com.catalogoapp.backend.firebase.FirebaseService
import com.catalogoapp.backend.modelo.CambiarEstadoRequest
import com.catalogoapp.backend.modelo.CrearPedidoRequest
import com.catalogoapp.backend.modelo.ErrorResponse
import com.catalogoapp.backend.modelo.EstadoPedido
import com.catalogoapp.backend.modelo.RegistrarDispositivoRequest
import com.catalogoapp.backend.repositorio.PedidoRepositorio
import com.google.firebase.auth.FirebaseToken
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

suspend fun ApplicationCall.uidAutenticado(): String? {
    val encabezado = request.headers["Authorization"]
    if (encabezado == null || !encabezado.startsWith("Bearer ")) {
        respond(HttpStatusCode.Unauthorized, ErrorResponse("Falta el token de autenticacion"))
        return null
    }
    val idToken = encabezado.removePrefix("Bearer ").trim()
    val token: FirebaseToken? = FirebaseService.verificarToken(idToken)
    if (token == null) {
        respond(HttpStatusCode.Unauthorized, ErrorResponse("Token invalido o expirado"))
        return null
    }
    return token.uid
}

fun Route.pedidoRutas(repositorio: PedidoRepositorio) {
    route("/pedidos") {

        post {
            val uid = call.uidAutenticado() ?: return@post
            val datos = call.receive<CrearPedidoRequest>()
            if (datos.items.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("El pedido debe tener al menos un producto"))
                return@post
            }
            val pedido = repositorio.crear(uid, datos)
            call.respond(HttpStatusCode.Created, pedido)
        }

        get {
            val uid = call.uidAutenticado() ?: return@get
            val pedidos = repositorio.listarPorConsultora(uid)
            call.respond(HttpStatusCode.OK, pedidos)
        }

        get("/pendientes-antiguos") {
            val uid = call.uidAutenticado() ?: return@get
            val dias = call.request.queryParameters["dias"]?.toLongOrNull() ?: 3L
            val pedidos = repositorio.listarPendientesOlvidadosDe(uid, dias)
            call.respond(HttpStatusCode.OK, pedidos)
        }

        get("/{id}") {
            val uid = call.uidAutenticado() ?: return@get
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Id de pedido invalido"))
                return@get
            }
            val pedido = repositorio.obtenerPorId(id, uid)
            if (pedido == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Pedido no encontrado"))
            } else {
                call.respond(HttpStatusCode.OK, pedido)
            }
        }

        patch("/{id}/estado") {
            val uid = call.uidAutenticado() ?: return@patch
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Id de pedido invalido"))
                return@patch
            }
            val solicitud = call.receive<CambiarEstadoRequest>()
            val nuevoEstado = EstadoPedido.desdeTexto(solicitud.nuevoEstado)
            if (nuevoEstado == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Estado invalido. Usa PENDIENTE, ENVIADO o ENTREGADO"))
                return@patch
            }
            val estadoActualTexto = repositorio.obtenerEstadoActual(id, uid)
            if (estadoActualTexto == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Pedido no encontrado"))
                return@patch
            }
            val estadoActual = EstadoPedido.desdeTexto(estadoActualTexto)
            if (estadoActual == null || !EstadoPedido.transicionValida(estadoActual, nuevoEstado)) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("No se puede pasar de $estadoActualTexto a ${solicitud.nuevoEstado}"))
                return@patch
            }
            val pedidoActualizado = repositorio.actualizarEstado(id, uid, nuevoEstado.name)
            if (pedidoActualizado == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Pedido no encontrado"))
                return@patch
            }
            val tokenDispositivo = repositorio.obtenerTokenDispositivo(uid)
            if (tokenDispositivo != null) {
                try {
                    FirebaseService.enviarNotificacion(
                        tokenDispositivo,
                        "Actualizacion de pedido",
                        "El pedido de ${pedidoActualizado.nombreCliente} ahora esta ${nuevoEstado.name.lowercase()}"
                    )
                } catch (e: Exception) {
                    // El pedido ya se actualizo igual; la notificacion es un beneficio adicional, no bloquea la respuesta
                }
            }
            call.respond(HttpStatusCode.OK, pedidoActualizado)
        }
    }

    route("/dispositivos") {
        post {
            val uid = call.uidAutenticado() ?: return@post
            val solicitud = call.receive<RegistrarDispositivoRequest>()
            repositorio.guardarTokenDispositivo(uid, solicitud.tokenFcm)
            call.respond(HttpStatusCode.OK)
        }
    }
}
