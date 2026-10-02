package com.catalogoapp.backend

import com.catalogoapp.backend.db.DatabaseFactory
import com.catalogoapp.backend.firebase.FirebaseService
import com.catalogoapp.backend.modelo.ErrorResponse
import com.catalogoapp.backend.repositorio.PedidoRepositorio
import com.catalogoapp.backend.repositorio.RepartoRepositorio
import com.catalogoapp.backend.rutas.pedidoRutas
import com.catalogoapp.backend.rutas.repartoRutas
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("Arranque")
    try {
        DatabaseFactory.conectar()
    } catch (e: Throwable) {
        logger.error("Fallo al conectar o migrar la base de datos", e)
        throw e
    }
    try {
        FirebaseService.inicializar()
    } catch (e: Throwable) {
        logger.error("Fallo al inicializar Firebase", e)
        throw e
    }
    try {
        embeddedServer(Netty, port = Config.puerto, module = Application::modulo).start(wait = true)
    } catch (e: Throwable) {
        logger.error("Fallo al iniciar el servidor Netty", e)
        throw e
    }
}

fun Application.modulo() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(CallLogging)
    install(CORS) {
        anyHost()
        allowHeader("Authorization")
        allowHeader("Content-Type")
    }
    install(StatusPages) {
        exception<Throwable> { call, causa ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(causa.message ?: "Error interno"))
        }
    }

    val repositorio = PedidoRepositorio()
    val repositorioReparto = RepartoRepositorio()

    routing {
        get("/salud") {
            call.respond(HttpStatusCode.OK, mapOf("estado" to "ok"))
        }
        pedidoRutas(repositorio)
        repartoRutas(repositorioReparto)
    }
}
