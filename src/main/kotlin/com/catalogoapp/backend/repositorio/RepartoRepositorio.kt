package com.catalogoapp.backend.repositorio

import com.catalogoapp.backend.db.TablaDispositivos
import com.catalogoapp.backend.db.TablaParadasReparto
import com.catalogoapp.backend.db.TablaPedidos
import com.catalogoapp.backend.db.TablaRepartos
import com.catalogoapp.backend.modelo.ParadaRepartoDto
import com.catalogoapp.backend.modelo.RepartoResponse
import com.catalogoapp.backend.ors.CoordenadaGeografica
import com.catalogoapp.backend.ors.OrsService
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime

private data class PedidoParaReparto(
    val id: Int,
    val nombreCliente: String,
    val direccion: String,
    val coordenada: CoordenadaGeografica
)

class RepartoRepositorio {

    suspend fun crear(
        zona: String,
        chofer: String,
        vehiculo: String,
        direccionOrigen: String,
        pedidoIds: List<Int>
    ): RepartoResponse? {
        val origen = OrsService.geocodificar(direccionOrigen) ?: return null

        val pedidosGeolocalizados = pedidoIds.mapNotNull { idPedido ->
            val fila = transaction {
                TablaPedidos.selectAll().where { TablaPedidos.id eq idPedido }.singleOrNull()
            } ?: return@mapNotNull null
            val direccionPedido = fila[TablaPedidos.direccion]
            if (direccionPedido.isBlank()) return@mapNotNull null
            val coordenada = OrsService.geocodificar(direccionPedido) ?: return@mapNotNull null
            PedidoParaReparto(idPedido, fila[TablaPedidos.nombreCliente], direccionPedido, coordenada)
        }

        if (pedidosGeolocalizados.isEmpty()) return null

        val resultadoOptimizacion = OrsService.optimizarRuta(
            origen,
            pedidosGeolocalizados.mapIndexed { indice, pedido -> indice to pedido.coordenada }
        )

        val ordenPorIndice = resultadoOptimizacion.ordenParadas.associate { it.idJob to it.orden }

        return transaction {
            val ahora = LocalDateTime.now()
            val idReparto = TablaRepartos.insertAndGetId {
                it[this.zona] = zona
                it[this.chofer] = chofer
                it[this.vehiculo] = vehiculo
                it[this.direccionOrigen] = direccionOrigen
                it[estado] = "EN_CURSO"
                it[geometriaRuta] = resultadoOptimizacion.geometriaRuta
                it[fechaCreacion] = ahora
            }

            pedidosGeolocalizados.forEachIndexed { indice, pedido ->
                val orden = ordenPorIndice[indice] ?: indice
                TablaParadasReparto.insert {
                    it[reparto] = idReparto
                    it[this.pedido] = pedido.id
                    it[this.orden] = orden
                    it[direccion] = pedido.direccion
                    it[latitud] = pedido.coordenada.latitud
                    it[longitud] = pedido.coordenada.longitud
                    it[entregada] = false
                }
            }

            obtenerPorIdInterno(idReparto.value)
        }
    }

    fun obtenerPorId(id: Int): RepartoResponse? = transaction { obtenerPorIdInterno(id) }

    private fun obtenerPorIdInterno(id: Int): RepartoResponse? {
        val filaReparto = TablaRepartos.selectAll().where { TablaRepartos.id eq id }.singleOrNull() ?: return null
        val paradas = obtenerParadas(id)
        return mapearReparto(filaReparto, paradas)
    }

    private fun obtenerParadas(idReparto: Int): List<ParadaRepartoDto> {
        return TablaParadasReparto.selectAll()
            .where { TablaParadasReparto.reparto eq idReparto }
            .orderBy(TablaParadasReparto.orden to SortOrder.ASC)
            .map { fila ->
                val idPedido = fila[TablaParadasReparto.pedido].value
                val nombreCliente = TablaPedidos.selectAll()
                    .where { TablaPedidos.id eq idPedido }
                    .map { it[TablaPedidos.nombreCliente] }
                    .singleOrNull() ?: ""
                ParadaRepartoDto(
                    id = fila[TablaParadasReparto.id].value,
                    pedidoId = idPedido,
                    nombreCliente = nombreCliente,
                    direccion = fila[TablaParadasReparto.direccion],
                    latitud = fila[TablaParadasReparto.latitud],
                    longitud = fila[TablaParadasReparto.longitud],
                    orden = fila[TablaParadasReparto.orden],
                    entregada = fila[TablaParadasReparto.entregada]
                )
            }
    }

    fun actualizarUbicacion(idReparto: Int, latitud: Double, longitud: Double): Boolean = transaction {
        val filasActualizadas = TablaRepartos.update({ TablaRepartos.id eq idReparto }) {
            it[latitudActual] = latitud
            it[longitudActual] = longitud
        }
        filasActualizadas > 0
    }

    fun marcarParadaEntregada(idReparto: Int, idParada: Int): ParadaRepartoDto? = transaction {
        val filasActualizadas = TablaParadasReparto.update({
            (TablaParadasReparto.id eq idParada) and (TablaParadasReparto.reparto eq idReparto)
        }) {
            it[entregada] = true
        }
        if (filasActualizadas == 0) return@transaction null
        val parada = obtenerParadas(idReparto).firstOrNull { it.id == idParada } ?: return@transaction null
        TablaPedidos.update({ TablaPedidos.id eq parada.pedidoId }) {
            it[estado] = "ENTREGADO"
            it[fechaActualizacion] = LocalDateTime.now()
        }
        parada
    }

    fun obtenerTokenDispositivoDePedido(idPedido: Int): String? = transaction {
        val fila = TablaPedidos.selectAll().where { TablaPedidos.id eq idPedido }.singleOrNull() ?: return@transaction null
        val uid = fila[TablaPedidos.uidConsultora]
        TablaDispositivos.selectAll()
            .where { TablaDispositivos.uidConsultora eq uid }
            .map { it[TablaDispositivos.tokenFcm] }
            .singleOrNull()
    }

    private fun mapearReparto(fila: ResultRow, paradas: List<ParadaRepartoDto>): RepartoResponse {
        return RepartoResponse(
            id = fila[TablaRepartos.id].value,
            zona = fila[TablaRepartos.zona],
            chofer = fila[TablaRepartos.chofer],
            vehiculo = fila[TablaRepartos.vehiculo],
            estado = fila[TablaRepartos.estado],
            paradas = paradas,
            geometriaRuta = fila[TablaRepartos.geometriaRuta]
        )
    }
}
