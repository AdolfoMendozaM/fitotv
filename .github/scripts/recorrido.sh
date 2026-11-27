#!/usr/bin/env bash
# Recorrido de prueba: instala FitoTV en un emulador de tele, lo maneja con
# teclas del control y guarda capturas de cada pantalla en recorrido/.
set -u
OUT=recorrido
mkdir -p "$OUT"
PKG=com.fitotv.app.dev
APK=app/build/outputs/apk/debug/app-debug.apk
LISTA="${FITOTV_LISTA_PRUEBA:-https://iptv-org.github.io/iptv/countries/mx.m3u}"

shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "captura $1"; }
key() { adb shell input keyevent "$@"; sleep 0.8; }

adb wait-for-device
adb shell settings put global window_animation_scale 1
adb install -r "$APK"
adb logcat -c

adb shell am start -n "$PKG/com.fitotv.MainActivity"
sleep 1.2; shot 01-carga
sleep 1.6; shot 02-carga-revelado
sleep 3; shot 03-tutorial-1
key KEYCODE_DPAD_CENTER; shot 04-tutorial-2
key KEYCODE_DPAD_CENTER; shot 05-tutorial-3
key KEYCODE_DPAD_CENTER; sleep 1.5; shot 06-crear-perfil

# Nombre: OK abre el teclado, escribimos y cerramos con Enter.
key KEYCODE_DPAD_CENTER
sleep 1.5
adb shell input text "Fito"
sleep 0.5
key KEYCODE_ENTER
sleep 1; shot 07-perfil-con-nombre

# Bajar hasta "Continuar" (avatares, perfil infantil, botones).
key KEYCODE_DPAD_DOWN
key KEYCODE_DPAD_RIGHT
key KEYCODE_DPAD_CENTER
key KEYCODE_DPAD_DOWN
key KEYCODE_DPAD_DOWN
shot 08-perfil-foco-continuar
key KEYCODE_DPAD_CENTER
sleep 2.5; shot 09-agrega-lista

# Mandar la lista como lo haría el celular (servidor local de la tele).
adb forward tcp:8090 tcp:8090
curl -s -m 20 http://127.0.0.1:8090/ -o "$OUT/pagina-celular.html" && echo "página del celular OK"
curl -s -m 60 --data-urlencode "tipo=enlace" --data-urlencode "nombre=Casa de prueba" --data-urlencode "enlace=$LISTA" \
  http://127.0.0.1:8090/lista -o "$OUT/respuesta-celular.html" && echo "lista enviada"
sleep 3; shot 10-revisando
sleep 15; shot 11-lista-lista
key KEYCODE_DPAD_CENTER
sleep 4; shot 12-inicio
key KEYCODE_DPAD_DOWN
sleep 2; shot 13-inicio-grupos
key KEYCODE_DPAD_DOWN
sleep 2; shot 14-inicio-canales
key KEYCODE_DPAD_DOWN
key KEYCODE_DPAD_DOWN
sleep 2; shot 15-inicio-abajo

# Ver un canal.
key KEYCODE_DPAD_CENTER
sleep 6; shot 16-reproductor
key KEYCODE_DPAD_UP
sleep 4; shot 17-cambio-canal
key KEYCODE_BACK
sleep 3; shot 18-inicio-recientes

# Menú lateral: Guía, Ajustes.
for i in 1 2 3 4 5 6; do key KEYCODE_DPAD_LEFT; done
for i in 1 2 3 4 5 6 7 8; do key KEYCODE_DPAD_UP; done
key KEYCODE_DPAD_DOWN
key KEYCODE_DPAD_DOWN
shot 19-menu
key KEYCODE_DPAD_CENTER
sleep 4; shot 20-guia
key KEYCODE_BACK
sleep 2

# Cerrar y volver a abrir: debe saludar con el perfil guardado.
adb shell am force-stop "$PKG"
sleep 1
adb shell am start -n "$PKG/com.fitotv.MainActivity"
sleep 1.2; shot 21-carga-regreso
sleep 2; shot 22-hola
sleep 3; shot 23-inicio-regreso

adb logcat -d > "$OUT/logcat.txt"
if grep -E "FATAL EXCEPTION|AndroidRuntime: Process: $PKG" "$OUT/logcat.txt" > "$OUT/errores.txt"; then
  echo "La app se cerró sola. Revisa recorrido/logcat.txt"
  grep -A 30 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -80
else
  echo "Sin cierres inesperados."
fi
exit 0
