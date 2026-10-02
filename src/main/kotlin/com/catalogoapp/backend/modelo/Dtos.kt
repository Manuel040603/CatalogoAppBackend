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
    val items: List<ItemPedidoDto>
)

@Serializable
data class PedidoResponse(
    val id: Int,
    val uidConsultora: String,
    val nombreCliente: String,
    val telefonoCliente: String,
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
