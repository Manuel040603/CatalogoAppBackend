package com.catalogoapp.backend.firebase

import com.catalogoapp.backend.Config
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import java.io.ByteArrayInputStream
import java.util.Base64

object FirebaseService {

    fun inicializar() {
        if (FirebaseApp.getApps().isNotEmpty()) return
        val credencialesJson = Base64.getDecoder().decode(Config.firebaseServiceAccountBase64)
        val credenciales = GoogleCredentials.fromStream(ByteArrayInputStream(credencialesJson))
        val opciones = FirebaseOptions.builder()
            .setCredentials(credenciales)
            .build()
        FirebaseApp.initializeApp(opciones)
    }

    fun verificarToken(idToken: String): FirebaseToken? {
        return try {
            FirebaseAuth.getInstance().verifyIdToken(idToken)
        } catch (e: Exception) {
            null
        }
    }

    fun enviarNotificacion(tokenDispositivo: String, titulo: String, cuerpo: String) {
        val mensaje = Message.builder()
            .setToken(tokenDispositivo)
            .setNotification(
                Notification.builder()
                    .setTitle(titulo)
                    .setBody(cuerpo)
                    .build()
            )
            .build()
        FirebaseMessaging.getInstance().send(mensaje)
    }
}
