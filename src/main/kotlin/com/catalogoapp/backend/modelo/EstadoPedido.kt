package com.catalogoapp.backend.modelo

enum class EstadoPedido {
    PENDIENTE,
    ENVIADO,
    ENTREGADO;

    companion object {
        fun desdeTexto(texto: String): EstadoPedido? = entries.firstOrNull { it.name == texto.uppercase() }

        fun transicionValida(actual: EstadoPedido, siguiente: EstadoPedido): Boolean {
            return when (actual) {
                PENDIENTE -> siguiente == ENVIADO
                ENVIADO -> siguiente == ENTREGADO
                ENTREGADO -> false
            }
        }
    }
}
