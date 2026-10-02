package com.catalogoapp.backend.db

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.javatime.datetime

object TablaPedidos : IntIdTable("pedidos") {
    val uidConsultora = varchar("uid_consultora", 128).index()
    val nombreCliente = varchar("nombre_cliente", 150)
    val telefonoCliente = varchar("telefono_cliente", 30)
    val direccion = varchar("direccion", 300).default("")
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

object TablaRepartos : IntIdTable("repartos") {
    val zona = varchar("zona", 150)
    val chofer = varchar("chofer", 150)
    val vehiculo = varchar("vehiculo", 150)
    val direccionOrigen = varchar("direccion_origen", 300)
    val estado = varchar("estado", 20).default("EN_CURSO")
    val geometriaRuta = text("geometria_ruta").nullable()
    val fechaCreacion = datetime("fecha_creacion")
    val latitudActual = double("latitud_actual").nullable()
    val longitudActual = double("longitud_actual").nullable()
}

object TablaParadasReparto : IntIdTable("paradas_reparto") {
    val reparto = reference("reparto_id", TablaRepartos)
    val pedido = reference("pedido_id", TablaPedidos)
    val orden = integer("orden")
    val direccion = varchar("direccion", 300)
    val latitud = double("latitud")
    val longitud = double("longitud")
    val entregada = bool("entregada").default(false)
}
