package com.catalogoapp.backend.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object TablaPedidos : IntIdTable("pedidos") {
    val uidConsultora = varchar("uid_consultora", 128).index()
    val nombreCliente = varchar("nombre_cliente", 150)
    val telefonoCliente = varchar("telefono_cliente", 30)
    val total = double("total")
    val estado = varchar("estado", 20).default("PENDIENTE")
    val fechaCreacion = datetime("fecha_creacion")
    val fechaActualizacion = datetime("fecha_actualizacion")
}

object TablaItemsPedido : IntIdTable("items_pedido") {
    val pedido = reference("pedido_id", TablaPedidos)
    val productoId = varchar("producto_id", 128)
    val nombreProducto = varchar("nombre_producto", 200)
    val cantidad = integer("cantidad")
    val precioUnitario = double("precio_unitario")
}

object TablaDispositivos : IntIdTable("dispositivos") {
    val uidConsultora = varchar("uid_consultora", 128).uniqueIndex()
    val tokenFcm = varchar("token_fcm", 300)
    val actualizado = datetime("actualizado")
}
