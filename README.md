# CatalogoApp Backend (HU04 - Pedidos)

API REST en Ktor + Exposed que atiende el registro y seguimiento de pedidos. Pensado para desplegarse gratis, sin tarjeta de credito, en Render + Supabase.

## 1. Crear la base de datos en Supabase (gratis, sin tarjeta)

1. Entra a supabase.com y crea una cuenta gratuita.
2. Crea un nuevo proyecto (elige una contrasena fuerte para la base de datos, la pediras despues).
3. Ve a Project Settings -> Database -> Connection string -> selecciona "Transaction pooler" (puerto 6543, recomendado para apps que abren/cierran conexiones con frecuencia, como en Render free).
4. Copia el host, usuario y contrasena. El DATABASE_URL queda asi:
   `jdbc:postgresql://<host-del-pooler>:6543/postgres?sslmode=require`
5. Importante: Supabase pausa un proyecto free tras 7 dias sin actividad en la base de datos (se reactiva gratis desde el dashboard, pero rompe la app hasta que alguien entre a reactivarlo). Para evitarlo, se puede crear un GitHub Action gratuito que haga una consulta ligera una vez por semana (se puede agregar despues; no es bloqueante para avanzar ahora).

## 2. Generar la cuenta de servicio de Firebase

1. En Firebase Console -> Configuracion del proyecto -> Cuentas de servicio -> Generar nueva clave privada. Descarga el .json.
2. Codificalo en base64 (en una terminal, con el archivo descargado):
   - Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("ruta\al\archivo.json")) | Set-Clipboard`
   - Mac/Linux: `base64 -i archivo.json | pbcopy` (o redirige a un archivo)
3. Ese texto va en la variable de entorno `FIREBASE_SERVICE_ACCOUNT_BASE64`.

## 3. Desplegar en Render (gratis, sin tarjeta)

1. Sube esta carpeta (`CatalogoAppBackend`) a un repositorio de GitHub.
2. Entra a render.com, crea cuenta gratuita (no pide tarjeta para el plan free).
3. New -> Web Service -> conecta el repo -> Render detecta el Dockerfile automaticamente (Environment: Docker).
4. En Environment Variables agrega: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `FIREBASE_SERVICE_ACCOUNT_BASE64`. `PORT` no es necesario, Render lo inyecta solo.
5. Deploy. La primera vez Render compila la imagen Docker (corre `gradle shadowJar` dentro del contenedor), tarda unos minutos.
6. Cuando termine, Render da una URL publica (ej. `https://catalogoapp-backend.onrender.com`). Esa URL es la que va en el Android (Retrofit `BASE_URL`).

Nota: el plan free de Render "duerme" el servicio tras 15 minutos sin trafico, y la primera peticion despues de eso tarda ~1 minuto en responder (cold start). Es normal, no es un error. El limite es 750 horas gratis al mes, mas que suficiente para el curso.

## 4. Probar que funciona

```
curl https://TU-URL-DE-RENDER.onrender.com/salud
```
Debe responder `{"estado":"ok"}`.

## Endpoints

Todos (excepto `/salud`) requieren el header `Authorization: Bearer <idToken de Firebase Auth>` que la app Android ya obtiene al iniciar sesion.

- `POST /pedidos` — crea un pedido. Body: `{"nombreCliente": "...", "telefonoCliente": "...", "items": [{"productoId": "...", "nombreProducto": "...", "cantidad": 2, "precioUnitario": 25.5}]}`
- `GET /pedidos` — lista los pedidos de la consultora autenticada.
- `GET /pedidos/{id}` — detalle de un pedido.
- `GET /pedidos/pendientes-antiguos?dias=3` — pedidos PENDIENTE con mas de `dias` dias de antiguedad (usado por el recordatorio de WorkManager en la app).
- `PATCH /pedidos/{id}/estado` — avanza el estado. Body: `{"nuevoEstado": "ENVIADO"}` (de PENDIENTE solo se puede ir a ENVIADO, y de ENVIADO solo a ENTREGADO). Envia una notificacion push (FCM) al dispositivo registrado de la consultora.
- `POST /dispositivos` — registra/actualiza el token FCM del dispositivo de la consultora. Body: `{"tokenFcm": "..."}`

## Desarrollo local (opcional)

Este proyecto no incluye gradle wrapper (gradlew); si quieres correrlo en tu maquina sin Docker, instala Gradle 8.12+ y Kotlin, y usa el comando `gradle` directamente:

```
export DATABASE_URL=...
export DATABASE_USER=...
export DATABASE_PASSWORD=...
export FIREBASE_SERVICE_ACCOUNT_BASE64=...
gradle run
```

Para el despliegue en Render no necesitas nada de esto: Render construye y corre el `Dockerfile` por ti, usando su propia instalacion de Gradle dentro del contenedor.
