# FitoTV

Reproductor para listas M3U, pensado para amigos y familia. Cada quien pone sus propias listas y las ve en la tele, con perfiles, avatares, grupos, vista previa y guía de programación.

FitoTV no incluye canales: solo reproduce las listas que cada persona agrega.

## Tele Android

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

## Web y celular

La versión web vive en `web/` y se publica sola en **https://adolfomendozam.github.io/fitotv/** cada vez que cambia algo en esa carpeta.

- En compu se usa desde el navegador.
- En el celular se instala como app: en iPhone, Safari → *Compartir* → *Agregar a inicio*; en Android, Chrome → *Agregar a la pantalla principal*.
- Las listas, perfiles y recientes se guardan solo en ese aparato.
- Los navegadores no dejan reproducir canales que usan conexiones sin cifrar (http://) ni leer listas de proveedores que lo bloquean. Esos canales se ven en la app de tele. La web avisa cuántos canales de cada lista se pueden ver ahí.

Para probarla en tu compu: `cd web && npm install && npm run dev`.

## Cómo se arma

Cada cambio en `main` hace que GitHub arme el instalable solo (ver `.github/workflows/android.yml`). El archivo `FitoTV-1.0.N.apk` aparece en **Releases** y en la pestaña **Actions**.

Para armarlo en tu compu: abre la carpeta en Android Studio y usa *Build → Build APK*, o en una terminal:

```
./gradlew :app:assembleRelease
```

## Cómo se instala

Este enlace siempre baja la versión más nueva:

```
https://github.com/AdolfoMendozaM/fitotv/releases/latest/download/FitoTV.apk
```

**Fire TV**

1. Instala **Downloader** desde la tienda de Amazon (búscalo con la lupa).
2. Activa las opciones de desarrollador: *Configuración → Mi Fire TV → Acerca de*, y presiona OK unas 7 veces sobre el nombre de tu dispositivo (por ejemplo "Fire TV Stick 4K") hasta que diga que ya eres desarrollador.
3. Regresa un menú y entra a *Opciones para desarrolladores → Instalar apps desconocidas*. Activa **Downloader**.
4. Abre Downloader, escribe el enlace de arriba y elige *Instalar*.

**Google TV / Android TV**

1. Instala **Downloader** desde la Play Store.
2. Activa el modo de desarrollador: *Configuración → Sistema → Acerca de*, y presiona OK unas 7 veces sobre *Compilación del SO de Android TV* hasta que diga que ya eres desarrollador.
3. Ve a *Configuración → Apps → Seguridad y restricciones → Fuentes desconocidas* y activa **Downloader**. (En algunas teles el menú se llama distinto, por ejemplo *Acceso especial de apps → Instalar apps desconocidas*.)
4. Abre Downloader, escribe el enlace de arriba y elige *Instalar*.

Cada versión nueva se instala encima de la anterior y conserva perfiles, listas y recientes. Para actualizar, repite el paso 4.

> Para no escribir el enlace completo con el control, crea un código numérico con el acortador de AFTVnews (aftv.news) usando el enlace de arriba; en Downloader solo se teclea el número. Como el enlace nunca cambia, el mismo código sirve para todas las versiones.

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
