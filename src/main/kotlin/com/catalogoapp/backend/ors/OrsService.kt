package com.catalogoapp.backend.ors

import com.catalogoapp.backend.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

@Serializable
data class CoordenadaGeografica(val latitud: Double, val longitud: Double)

@Serializable
private data class GeocodeGeometry(val coordinates: List<Double>)

@Serializable
private data class GeocodeFeature(val geometry: GeocodeGeometry)

@Serializable
private data class GeocodeResponse(val features: List<GeocodeFeature> = emptyList())

@Serializable
private data class OptimizationJob(val id: Int, val location: List<Double>)

@Serializable
private data class OptimizationVehicle(
    val id: Int,
    val profile: String = "driving-car",
    val start: List<Double>,
    val end: List<Double>
)

@Serializable
private data class OptimizationOptions(val g: String = "true")

@Serializable
private data class OptimizationRequest(
    val jobs: List<OptimizationJob>,
    val vehicles: List<OptimizationVehicle>,
    val options: OptimizationOptions = OptimizationOptions()
)

@Serializable
private data class OptimizationStep(val type: String, val id: Int? = null)

@Serializable
private data class OptimizationRoute(val steps: List<OptimizationStep> = emptyList(), val geometry: String? = null)

@Serializable
private data class OptimizationResponse(val routes: List<OptimizationRoute> = emptyList())

data class ParadaOptimizada(val idJob: Int, val orden: Int)

data class ResultadoOptimizacion(val ordenParadas: List<ParadaOptimizada>, val geometriaRuta: String?)

object OrsService {

    private const val GEOCODE_URL = "https://api.heigit.org/pelias/v1/search"
    private const val OPTIMIZATION_URL = "https://api.heigit.org/vroom/v0"

    private val cliente = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun geocodificar(direccion: String): CoordenadaGeografica? {
        val textoCodificado = URLEncoder.encode(direccion, "UTF-8")
        val urlCompleta = "$GEOCODE_URL?api_key=${Config.orsApiKey}&text=$textoCodificado&size=1"
        val respuesta: GeocodeResponse = cliente.get(urlCompleta).body()
        val coordenadas = respuesta.features.firstOrNull()?.geometry?.coordinates ?: return null
        if (coordenadas.size < 2) return null
        return CoordenadaGeografica(latitud = coordenadas[1], longitud = coordenadas[0])
    }

    suspend fun optimizarRuta(
        origen: CoordenadaGeografica,
        paradas: List<Pair<Int, CoordenadaGeografica>>
    ): ResultadoOptimizacion {
        val jobs = paradas.map { (id, coordenada) ->
            OptimizationJob(id = id, location = listOf(coordenada.longitud, coordenada.latitud))
        }
        val vehiculo = OptimizationVehicle(
            id = 0,
            start = listOf(origen.longitud, origen.latitud),
            end = listOf(origen.longitud, origen.latitud)
        )
        val cuerpo = OptimizationRequest(jobs = jobs, vehicles = listOf(vehiculo))

        val respuesta: OptimizationResponse = cliente.post(OPTIMIZATION_URL) {
            header("Authorization", Config.orsApiKey)
            contentType(ContentType.Application.Json)
            setBody(cuerpo)
        }.body()

        val ruta = respuesta.routes.firstOrNull() ?: return ResultadoOptimizacion(emptyList(), null)
        val ordenParadas = ruta.steps
            .filter { it.type == "job" && it.id != null }
            .mapIndexed { indice, paso -> ParadaOptimizada(idJob = paso.id!!, orden = indice) }
        return ResultadoOptimizacion(ordenParadas, ruta.geometry)
    }
}
