package com.catalogoapp.backend.repositorio

import com.catalogoapp.backend.db.TablaDispositivos
import com.catalogoapp.backend.db.TablaItemsPedido
import com.catalogoapp.backend.db.TablaPedidos
import com.catalogoapp.backend.modelo.CrearPedidoRequest
import com.catalogoapp.backend.modelo.ItemPedidoDto
import com.catalogoapp.backend.modelo.PedidoResponse
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PedidoRepositorio {

    private val formato = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun crear(uid: String, datos: CrearPedidoRequest): PedidoResponse = transaction {
        val ahora = LocalDateTime.now()
        val total = datos.items.sumOf { it.cantidad * it.precioUnitario }

        val idPedido = TablaPedidos.insertAndGetId {
            it[uidConsultora] = uid
            it[nombreCliente] = datos.nombreCliente
            it[telefonoCliente] = datos.telefonoCliente
            it[direccion] = datos.direccion
            it[this.total] = total
            it[estado] = "PENDIENTE"
            it[fechaCreacion] = ahora
            it[fechaActualizacion] = ahora
        }

        datos.items.forEach { item ->
            TablaItemsPedido.insert {
                it[pedido] = idPedido
                it[productoId] = item.productoId
                it[nombreProducto] = item.nombreProducto
                it[cantidad] = item.cantidad
                it[precioUnitario] = item.precioUnitario
            }
        }

        obtenerPorId(idPedido.value, uid)!!
    }

    fun listarPorConsultora(uid: String): List<PedidoResponse> = transaction {
        TablaPedidos.selectAll()
            .where { TablaPedidos.uidConsultora eq uid }
            .orderBy(TablaPedidos.fechaCreacion to SortOrder.DESC)
            .map { fila -> mapearPedido(fila, obtenerItems(fila[TablaPedidos.id].value)) }
    }

    fun obtenerPorId(id: Int, uid: String): PedidoResponse? = transaction {
        TablaPedidos.selectAll()
            .where { (TablaPedidos.id eq id) and (TablaPedidos.uidConsultora eq uid) }
            .map { fila -> mapearPedido(fila, obtenerItems(id)) }
            .singleOrNull()
    }

    fun obtenerEstadoActual(id: Int, uid: String): String? = transaction {
        TablaPedidos.selectAll()
            .where { (TablaPedidos.id eq id) and (TablaPedidos.uidConsultora eq uid) }
            .map { it[TablaPedidos.estado] }
            .singleOrNull()
    }

    fun actualizarEstado(id: Int, uid: String, nuevoEstado: String): PedidoResponse? = transaction {
        val filasActualizadas = TablaPedidos.update({ (TablaPedidos.id eq id) and (TablaPedidos.uidConsultora eq uid) }) {
            it[estado] = nuevoEstado
            it[fechaActualizacion] = LocalDateTime.now()
        }
        if (filasActualizadas == 0) null else obtenerPorId(id, uid)
    }

    fun guardarTokenDispositivo(uid: String, token: String) = transaction {
        val existente = TablaDispositivos.selectAll().where { TablaDispositivos.uidConsultora eq uid }.singleOrNull()
        if (existente == null) {
            TablaDispositivos.insert {
                it[uidConsultora] = uid
                it[tokenFcm] = token
                it[actualizado] = LocalDateTime.now()
            }
        } else {
            TablaDispositivos.update({ TablaDispositivos.uidConsultora eq uid }) {
                it[tokenFcm] = token
                it[actualizado] = LocalDateTime.now()
            }
        }
    }

    fun obtenerTokenDispositivo(uid: String): String? = transaction {
        TablaDispositivos.selectAll()
            .where { TablaDispositivos.uidConsultora eq uid }
            .map { it[TablaDispositivos.tokenFcm] }
            .singleOrNull()
    }

    fun listarPendientesOlvidadosDe(uid: String, diasLimite: Long): List<PedidoResponse> = transaction {
        val limite = LocalDateTime.now().minusDays(diasLimite)
        TablaPedidos.selectAll()
            .where { (TablaPedidos.uidConsultora eq uid) and (TablaPedidos.estado eq "PENDIENTE") and (TablaPedidos.fechaCreacion less limite) }
            .map { fila -> mapearPedido(fila, obtenerItems(fila[TablaPedidos.id].value)) }
    }

    private fun obtenerItems(idPedido: Int): List<ItemPedidoDto> {
        return TablaItemsPedido.selectAll()
            .where { TablaItemsPedido.pedido eq idPedido }
            .map {
                ItemPedidoDto(
                    productoId = it[TablaItemsPedido.productoId],
                    nombreProducto = it[TablaItemsPedido.nombreProducto],
                    cantidad = it[TablaItemsPedido.cantidad],
                    precioUnitario = it[TablaItemsPedido.precioUnitario]
                )
            }
    }

    private fun mapearPedido(fila: ResultRow, items: List<ItemPedidoDto>): PedidoResponse {
        return PedidoResponse(
            id = fila[TablaPedidos.id].value,
            uidConsultora = fila[TablaPedidos.uidConsultora],
            nombreCliente = fila[TablaPedidos.nombreCliente],
            telefonoCliente = fila[TablaPedidos.telefonoCliente],
            direccion = fila[TablaPedidos.direccion],
            total = fila[TablaPedidos.total],
            estado = fila[TablaPedidos.estado],
            fechaCreacion = fila[TablaPedidos.fechaCreacion].format(formato),
            fechaActualizacion = fila[TablaPedidos.fechaActualizacion].format(formato),
            items = items
        )
    }
}
