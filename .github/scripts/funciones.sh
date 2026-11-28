#!/usr/bin/env bash
# Prueba de lo nuevo, con un proveedor "realista" (nombres disfrazados, grupos con emojis):
# agregar la lista con el control (usuario y contraseña), nombres limpios, selector de grupos,
# guía pública, recientes, el foco del menú lateral, películas estilo Netflix, idioma y
# subtítulos, buscar con filtro, vigencia de la cuenta y editar la lista.
set -u
OUT=recorrido
mkdir -p "$OUT"
PKG=com.fitotv.app.dev
F="$OUT/funciones.txt"
: > "$F"

FALSO_MODO=realista FALSO_MEDIA=/tmp/pistas nohup python3 .github/scripts/xtream_falso.py 8769 >/dev/null 2>&1 &
sleep 1

shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "captura $1"; }
key() { adb shell input keyevent "$@"; sleep 0.9; }
note() { echo "$*" | tee -a "$F"; }
screen_has() { adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb shell cat /sdcard/u.xml 2>/dev/null | grep -q "$1"; }
wait_text() {
  for i in $(seq 1 "${2:-90}"); do
    if screen_has "$1"; then note "En pantalla: '$1' (${i} s)"; return 0; fi
    sleep 1
  done
  note "NO apareció: '$1'"
  return 1
}
# Todos los textos de la pantalla, para revisar nombres.
texts() { adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb shell cat /sdcard/u.xml 2>/dev/null | grep -o 'text="[^"]*"' | sed 's/text="//;s/"$//' | grep -v '^$' | tr '\n' '|'; echo; }
# Escribe en el campo enfocado: OK abre el teclado, se escribe, y se cierra con Atrás (como en la tele).
type_in() {
  key KEYCODE_DPAD_CENTER
  sleep 1
  adb shell input text "$1"
  sleep 0.6
  key KEYCODE_BACK
  sleep 0.6
}
rail() { # menú lateral: entra con la izquierda y baja N lugares desde el avatar
  for i in 1 2 3 4 5 6 7; do key KEYCODE_DPAD_LEFT; done
  for i in 1 2 3 4 5 6 7 8; do key KEYCODE_DPAD_UP; done
  for i in $(seq 1 "$1"); do key KEYCODE_DPAD_DOWN; done
  key KEYCODE_DPAD_CENTER
}

adb wait-for-device
adb shell pm path "$PKG" 2>/dev/null | grep -q package || adb install -r app/build/outputs/apk/github/debug/app-github-debug.apk
adb shell pm clear "$PKG" >/dev/null
adb logcat -c
adb shell am start -n "$PKG/com.fitotv.MainActivity" >/dev/null
sleep 6
key KEYCODE_DPAD_CENTER; key KEYCODE_DPAD_CENTER; key KEYCODE_DPAD_CENTER
sleep 1.5
key KEYCODE_DPAD_CENTER; sleep 1.5
adb shell input text "Atz"; sleep 0.5
key KEYCODE_ENTER; sleep 1
key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_CENTER
key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_DOWN
key KEYCODE_DPAD_CENTER
sleep 3

# ---------------------------------------------------------------- 1) Usuario y contraseña con el control
shot F01-agregar
key KEYCODE_DPAD_UP; shot F02-pestanas
key KEYCODE_DPAD_RIGHT; shot F03-usuario-y-contrasena
key KEYCODE_DPAD_DOWN
type_in "Premium"
shot F04-nombre-escrito
key KEYCODE_DPAD_DOWN
type_in "http://10.0.2.2:8769"
shot F05-servidor-escrito
key KEYCODE_DPAD_DOWN
type_in "prueba"
key KEYCODE_DPAD_RIGHT
type_in "1234"
shot F06-usuario-y-contrasena-escritos
key KEYCODE_DPAD_DOWN
shot F07-boton-agregar
key KEYCODE_DPAD_CENTER
sleep 2; shot F08-revisando
if wait_text "Ver mis canales" 60; then
  note "Lista agregada con el control: OK"
else
  note "La lista NO se agregó con el control"
  shot F08b-sin-agregar
fi
key KEYCODE_DPAD_CENTER
sleep 5; shot F10-inicio
note "Textos de Inicio: $(texts)"

# ---------------------------------------------------------------- 2) Grupos en el selector de arriba
key KEYCODE_DPAD_UP; key KEYCODE_DPAD_UP; key KEYCODE_DPAD_UP
shot F11-arriba-selectores
key KEYCODE_DPAD_RIGHT; shot F12-selector-grupo
key KEYCODE_DPAD_CENTER; sleep 2; shot F13-grupos
note "Grupos: $(texts)"
key KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_CENTER
sleep 3; shot F14-inicio-otro-grupo

# ---------------------------------------------------------------- 3) Ver un canal y regresar (recientes)
for i in 1 2 3 4 5; do key KEYCODE_DPAD_DOWN; done
key KEYCODE_DPAD_CENTER
sleep 6; shot F15-canal
key KEYCODE_BACK
sleep 3; shot F16-inicio-viste-hace-poco
note "Textos al regresar: $(texts)"

# ---------------------------------------------------------------- 4) El menú lateral solo con la izquierda
for i in $(seq 1 12); do adb shell input keyevent KEYCODE_DPAD_DOWN; sleep 0.3; done
sleep 1; shot F17-abajo-del-todo
for i in $(seq 1 16); do adb shell input keyevent KEYCODE_DPAD_UP; sleep 0.3; done
sleep 1; shot F18-arriba-del-todo
key KEYCODE_DPAD_LEFT; sleep 1; shot F19-izquierda-al-menu
key KEYCODE_DPAD_RIGHT; sleep 1; shot F19b-de-regreso

# ---------------------------------------------------------------- 5) Guía (la pública tarda en bajar la primera vez)
note "Esperando la guía…"
for i in $(seq 1 24); do
  if adb logcat -d | grep -q "FitoEPG.*guía pública"; then break; fi
  sleep 5
done
adb logcat -d | grep "FitoEPG" | tee -a "$F"
rail 2; sleep 5; shot F20-guia
note "Guía: $(texts)"
key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_DOWN; sleep 2; shot F21-guia-abajo

# ---------------------------------------------------------------- 6) Películas estilo Netflix
rail 3; sleep 4; shot F30-peliculas
note "Películas: $(texts)"
key KEYCODE_DPAD_DOWN; sleep 1; shot F31-peliculas-fila
key KEYCODE_DPAD_CENTER
sleep 8; shot F32-pelicula
key KEYCODE_DPAD_DOWN; sleep 2; shot F33-panel-idioma
note "Panel: $(texts)"
key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER     # segundo idioma
sleep 1
for i in 1 2 3; do key KEYCODE_DPAD_DOWN; done; key KEYCODE_DPAD_CENTER   # un subtítulo
sleep 2; shot F34-panel-elegido
key KEYCODE_BACK; sleep 4; shot F35-con-subtitulos
key KEYCODE_BACK; sleep 3
key KEYCODE_DPAD_UP; key KEYCODE_DPAD_UP; sleep 1; shot F36-categorias
key KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_CENTER; sleep 3; shot F37-categoria
rail 4; sleep 4; shot F38-series

# ---------------------------------------------------------------- 7) Buscar con filtro
rail 5; sleep 2
type_in "hbo"
sleep 2; shot F40-buscar-todo
key KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_CENTER; sleep 2; shot F41-buscar-canales
key KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_CENTER; sleep 2; shot F42-buscar-peliculas

# ---------------------------------------------------------------- 8) Ajustes: vigencia y editar la lista
for i in 1 2 3 4 5 6 7 8; do key KEYCODE_DPAD_LEFT; done
for i in 1 2 3 4 5 6 7 8 9 10; do key KEYCODE_DPAD_DOWN; done
key KEYCODE_DPAD_CENTER
sleep 4; shot F50-ajustes
note "Ajustes: $(texts)"
key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3; shot F51-mis-listas
key KEYCODE_DPAD_RIGHT; shot F52-editar-enfocado
key KEYCODE_DPAD_CENTER; sleep 3; shot F53-editar
note "Editar: $(texts)"
for i in 1 2 3 4 5; do key KEYCODE_DPAD_DOWN; done
shot F54-guardar
key KEYCODE_DPAD_CENTER; sleep 6; shot F55-guardado
note "Guardado: $(texts)"

adb logcat -d > "$OUT/logcat-funciones.txt"
grep -E "FitoAudio|FitoEPG" "$OUT/logcat-funciones.txt" | sed -E 's/^.*(FitoAudio|FitoEPG) *: //' | awk '!seen[$0]++' >> "$F"
if grep -q "FATAL EXCEPTION" "$OUT/logcat-funciones.txt"; then
  note "LA APP SE CERRÓ"
  grep -A 30 "FATAL EXCEPTION" "$OUT/logcat-funciones.txt" | head -60 >> "$F"
fi
cat "$F"
pkill -f "xtream_falso.py 8769" || true
exit 0
