package com.catalogoapp.backend.modelo

import kotlinx.serialization.Serializable

@Serializable
data class ItemPedidoDto(
    val productoId: String,
    val nombreProducto: String,
    val cantidad: Int,
    val precioUnitario: Double
)

@Serializable
data class CrearPedidoRequest(
    val nombreCliente: String,
    val telefonoCliente: String,
    val direccion: String = "",
    val items: List<ItemPedidoDto>
)

@Serializable
data class PedidoResponse(
    val id: Int,
    val uidConsultora: String,
    val nombreCliente: String,
    val telefonoCliente: String,
    val direccion: String,
    val total: Double,
    val estado: String,
    val fechaCreacion: String,
    val fechaActualizacion: String,
    val items: List<ItemPedidoDto>
)

@Serializable
data class CambiarEstadoRequest(
    val nuevoEstado: String
)

@Serializable
data class RegistrarDispositivoRequest(
    val tokenFcm: String
)

@Serializable
data class ErrorResponse(
    val error: String
)

@Serializable
data class CrearRepartoRequest(
    val zona: String,
    val chofer: String,
    val vehiculo: String,
    val direccionOrigen: String,
    val pedidoIds: List<Int>
)

@Serializable
data class ParadaRepartoDto(
    val id: Int,
    val pedidoId: Int,
    val nombreCliente: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val orden: Int,
    val entregada: Boolean
)

@Serializable
data class RepartoResponse(
    val id: Int,
    val zona: String,
    val chofer: String,
    val vehiculo: String,
    val estado: String,
    val paradas: List<ParadaRepartoDto>,
    val geometriaRuta: String? = null
)

@Serializable
data class ActualizarUbicacionRequest(
    val latitud: Double,
    val longitud: Double
)
