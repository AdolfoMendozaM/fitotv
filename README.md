# FitoTV

Reproductor para listas M3U, pensado para amigos y familia. Cada quien pone sus propias listas y las ve en la tele, con perfiles, avatares, grupos, vista previa y guía de programación.

FitoTV no incluye canales: solo reproduce las listas que cada persona agrega.

## Versión 1: tele Android

Funciona en **Google TV, Android TV y Fire TV** (Android 5.1 o más nuevo).

Incluye:

- Pantalla de carga con el efecto del play y el sonido de marimba "Fi‑to‑te‑vé".
- Primer uso: tutorial, crea tu perfil y agrega tu primera lista.
- Agregar listas con un enlace, un archivo o usuario y contraseña (Xtream). Se pueden mandar **desde el celular con un QR**, sin escribir con el control: la tele abre una pequeña página en la red de la casa.
- ¿Quién va a ver? con perfiles, avatares y perfil infantil (solo canales del grupo Infantil).
- Inicio con "Continuar viendo contenido de…", "Canales recientes", grupos y canales, con vista previa sin sonido que se queda arriba al bajar.
- Guía de programación (XMLTV), películas, series, búsqueda, favoritos y ajustes.
- Reproductor con cambio de canal (▲▼), salto por número y avance de películas y series.
- Si una lista no responde, sigue con la copia guardada y avisa.

## Cómo se arma

Cada cambio en `main` hace que GitHub arme el instalable solo (ver `.github/workflows/android.yml`). El archivo `FitoTV-1.0.N.apk` aparece en **Releases** y en la pestaña **Actions**.

Para armarlo en tu compu: abre la carpeta en Android Studio y usa *Build → Build APK*, o en una terminal:

```
./gradlew :app:assembleRelease
```

## Cómo se instala

**Fire TV**

1. En el Fire TV: *Configuración → Mi Fire TV → Opciones para desarrolladores* y activa *Instalar apps desconocidas* para la app **Downloader**.
2. Instala **Downloader** desde la tienda de Amazon.
3. En Downloader escribe el enlace del `.apk` y sigue los pasos.

**Google TV / Android TV**

Usa **Downloader** (de la Play Store) igual que en Fire TV, o la app **Send Files to TV** desde tu celular.

Cada versión nueva se instala encima de la anterior y conserva perfiles, listas y recientes.

> El repositorio es privado, así que los enlaces de Releases piden iniciar sesión. Para que la familia pueda descargarlo, sube el `.apk` a un lugar con enlace público (por ejemplo Google Drive o tu propio sitio) o haz público el repositorio.

## Firma del instalable

Mientras no haya una llave propia, el instalable se firma con `app/familia-debug.keystore`, una llave de pruebas que vive en el repositorio para que todas las versiones se puedan instalar encima de la anterior. Antes de publicar en tiendas, crea una llave propia y guárdala en los secretos del repositorio:

- `FITOTV_KEYSTORE_B64` (el archivo .keystore en base64)
- `FITOTV_KEYSTORE_PASSWORD`
- `FITOTV_KEY_ALIAS`
- `FITOTV_KEY_PASSWORD`

Ojo: al cambiar de llave hay que desinstalar la versión anterior una sola vez.

## Estructura

```
app/src/main/java/com/fitotv/
  data/      listas M3U y Xtream, guía XMLTV, perfiles, recientes y copia guardada
  pairing/   servidor local para mandar listas desde el celular (QR)
  player/    reproductor (Media3 / ExoPlayer)
  sound/     sonidos de marimba
  ui/        pantallas en Jetpack Compose, diseñadas a 1280 × 720
```

Siguientes pasos: celular (iPhone y Android), web y Roku.
