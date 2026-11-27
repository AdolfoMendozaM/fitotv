#!/usr/bin/env bash
# Capturas para la tienda: instala FitoTV en un emulador de tele 1080p, le agrega la
# lista de ejemplo (canales inventados) y guarda capturas 1920x1080 en tienda/.
set -u
OUT=tienda
mkdir -p "$OUT"
PKG=com.fitotv.app.dev
APK=app/build/outputs/apk/amazon/debug/app-amazon-debug.apk

shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "captura $1"; }
key() { adb shell input keyevent "$@"; sleep 0.8; }

# Lista de ejemplo servida desde la compu del emulador (10.0.2.2 dentro del emulador).
mkdir -p /tmp/demo
python3 .github/tienda/demo.py /tmp/demo http://10.0.2.2:8765/
(cd /tmp/demo && nohup python3 -m http.server 8765 >/dev/null 2>&1 &)

adb wait-for-device
adb install -r "$APK"
adb logcat -c
adb shell am start -n "$PKG/com.fitotv.MainActivity"
sleep 6
key KEYCODE_DPAD_CENTER
key KEYCODE_DPAD_CENTER
key KEYCODE_DPAD_CENTER
sleep 1.5

# Perfil
key KEYCODE_DPAD_CENTER
sleep 1.5
adb shell input text "Fito"
sleep 0.5
key KEYCODE_ENTER
sleep 1
key KEYCODE_DPAD_DOWN
key KEYCODE_DPAD_RIGHT
key KEYCODE_DPAD_CENTER
key KEYCODE_DPAD_DOWN
key KEYCODE_DPAD_DOWN
key KEYCODE_DPAD_CENTER
sleep 3; shot 05-agregar-lista-con-el-celular

# La lista llega como si la mandara el celular.
adb forward tcp:8090 tcp:8090
curl -s -m 60 --data-urlencode "tipo=enlace" --data-urlencode "nombre=Casa" \
  --data-urlencode "enlace=http://10.0.2.2:8765/lista.m3u" http://127.0.0.1:8090/lista -o /dev/null && echo "lista enviada"
sleep 12
key KEYCODE_DPAD_CENTER
sleep 14; shot 01-inicio

# Al llegar, el foco está en el primer canal. Uno a la derecha para otra vista previa.
key KEYCODE_DPAD_RIGHT
sleep 10; shot 02-inicio-canales

# Ver un canal y cambiar con ▲ para que salga el cartel del canal.
key KEYCODE_DPAD_CENTER
sleep 2.5; shot 03-reproductor-cartel
sleep 8; shot 03b-reproductor
key KEYCODE_DPAD_UP
sleep 2.5; shot 03c-reproductor-cambio
key KEYCODE_BACK
sleep 6; shot 04-inicio-recientes

# Menú: desde Ajustes (lo más abajo) se sube a Guía y Películas.
menu_desde_ajustes() {
  for i in 1 2 3 4 5 6 7 8; do key KEYCODE_DPAD_LEFT; done
  for i in 1 2 3 4 5 6 7 8 9 10; do key KEYCODE_DPAD_DOWN; done
  for i in $(seq 1 "$1"); do key KEYCODE_DPAD_UP; done
  key KEYCODE_DPAD_CENTER
}
menu_desde_ajustes 4
sleep 10; shot 06-guia
menu_desde_ajustes 3
sleep 6; shot 07-peliculas

# Saludo al volver a abrir
adb shell am force-stop "$PKG"
sleep 1
adb shell am start -n "$PKG/com.fitotv.MainActivity"
sleep 2.2; shot 08-hola
sleep 0.8; shot 08b-hola

adb logcat -d > "$OUT/logcat.txt"
grep -E "FATAL EXCEPTION" "$OUT/logcat.txt" && echo "La app se cerró sola" || echo "Sin cierres inesperados."
pkill -f "http.server 8765" || true
exit 0
