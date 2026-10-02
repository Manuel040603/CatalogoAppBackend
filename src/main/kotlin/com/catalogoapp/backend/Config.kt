package com.catalogoapp.backend

object Config {
    val databaseUrl: String = requireEnv("DATABASE_URL")
    val databaseUser: String = requireEnv("DATABASE_USER")
    val databasePassword: String = requireEnv("DATABASE_PASSWORD")
    val firebaseServiceAccountBase64: String = requireEnv("FIREBASE_SERVICE_ACCOUNT_BASE64")
    val orsApiKey: String = requireEnv("ORS_API_KEY")
    val puerto: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080

    private fun requireEnv(nombre: String): String {
        return System.getenv(nombre) ?: throw IllegalStateException("Falta la variable de entorno $nombre")
    }
}
