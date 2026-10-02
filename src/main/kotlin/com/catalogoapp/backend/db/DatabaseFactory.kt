package com.catalogoapp.backend.db

import com.catalogoapp.backend.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {

    fun conectar() {
        val configuracion = HikariConfig().apply {
            jdbcUrl = Config.databaseUrl
            username = Config.databaseUser
            password = Config.databasePassword
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5
            minimumIdle = 1
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            keepaliveTime = 60000
            maxLifetime = 1500000
            validate()
        }
        val fuenteDatos = HikariDataSource(configuracion)
        Database.connect(fuenteDatos)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                TablaPedidos,
                TablaItemsPedido,
                TablaDispositivos,
                TablaRepartos,
                TablaParadasReparto
            )
        }
    }
}
