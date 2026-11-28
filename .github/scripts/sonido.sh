#!/usr/bin/env bash
# Prueba de sonido: canales con el sonido en AAC, Dolby (AC-3 y E-AC-3), MP2 y DTS, como los que
# mandan muchos proveedores de México. El emulador no trae decodificador para Dolby, MP2 ni DTS,
# así que si esos canales suenan es gracias al decodificador de FitoTV.
# Los videos los arma el paso "Videos de prueba de sonido" en /tmp/sonido.
set -u
OUT=recorrido
mkdir -p "$OUT"
PKG=com.fitotv.app.dev
S="$OUT/sonido.txt"
: > "$S"
DIR=/tmp/sonido

if ! ls "$DIR"/*.ts >/dev/null 2>&1; then
  echo "No hay videos de prueba de sonido." | tee -a "$S"
  exit 0
fi
{
  echo "#EXTM3U"
  for f in "$DIR"/*.ts; do
    n=$(basename "$f" .ts)
    echo "#EXTINF:-1 tvg-id=\"$n\" group-title=\"Sonido\",Sonido $n"
    echo "http://10.0.2.2:8768/$n.ts"
  done
} > "$DIR/sonido.m3u"
(cd "$DIR" && nohup python3 -m http.server 8768 >/dev/null 2>&1 &)
sleep 1
echo "Canales de prueba: $(ls "$DIR"/*.ts | xargs -n1 basename | tr '\n' ' ')" | tee -a "$S"

shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "captura $1"; }
key() { adb shell input keyevent "$@"; sleep 0.8; }

onboard() {
  adb shell pm clear "$PKG" >/dev/null
  adb shell am start -n "$PKG/com.fitotv.MainActivity" >/dev/null
  sleep 6
  key KEYCODE_DPAD_CENTER; key KEYCODE_DPAD_CENTER; key KEYCODE_DPAD_CENTER
  sleep 1.5
  key KEYCODE_DPAD_CENTER; sleep 1.5
  adb shell input text "Sonido"; sleep 0.5
  key KEYCODE_ENTER; sleep 1
  key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_CENTER
  key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_DOWN
  key KEYCODE_DPAD_CENTER
  sleep 3
  adb forward tcp:8090 tcp:8090 >/dev/null
}

wait_text() {
  for i in $(seq 1 90); do
    adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1
    if adb shell cat /sdcard/u.xml 2>/dev/null | grep -q "$1"; then return 0; fi
    sleep 1
  done
  echo "No apareció '$1'" | tee -a "$S"
  return 1
}

# Ve cada canal unos segundos cambiando con el control (flecha abajo = siguiente canal).
zap_all() {
  local n=$(ls "$DIR"/*.ts | wc -l)
  sleep 9; shot "$1-1"
  for k in $(seq 2 $((n + 1))); do
    key KEYCODE_DPAD_DOWN
    sleep 9; shot "$1-$k"
  done
}

adb wait-for-device
adb shell pm path "$PKG" 2>/dev/null | grep -q package || adb install -r app/build/outputs/apk/github/debug/app-github-debug.apk
adb logcat -c
onboard
curl -s -m 30 --data-urlencode "tipo=enlace" --data-urlencode "nombre=Sonido" \
  --data-urlencode "enlace=http://10.0.2.2:8768/sonido.m3u" \
  http://127.0.0.1:8090/lista -o /dev/null && echo "lista de sonido enviada"
if wait_text "Ver mis canales"; then
  key KEYCODE_DPAD_CENTER
  sleep 5; shot S01-inicio
  # Inicio abre con el foco en el primer canal y la vista previa (con sonido) toca el canal enfocado:
  # recorrerlos con la flecha derecha prueba cada uno.
  n=$(ls "$DIR"/*.ts | wc -l)
  sleep 6; shot S02-previa-1
  for k in $(seq 2 "$n"); do
    key KEYCODE_DPAD_RIGHT
    sleep 8; shot "S02-previa-$k"
  done
  # Y en pantalla completa: abrir el último y cambiar de canal con la flecha abajo.
  key KEYCODE_DPAD_CENTER
  zap_all S03-reproductor
  key KEYCODE_BACK; sleep 2
fi

adb logcat -d > "$OUT/logcat-sonido.txt"
echo "--- Lo que dice FitoTV del sonido de cada canal ---" >> "$S"
grep -E "FitoAudio" "$OUT/logcat-sonido.txt" | sed -E 's/^.*FitoAudio *: //' | awk '!seen[$0]++' >> "$S"
echo "--- Errores del reproductor ---" >> "$S"
grep -E "ExoPlayerImplInternal.*(Playback error|Exception)|FfmpegLibrary|Failed to load" "$OUT/logcat-sonido.txt" | head -20 >> "$S"
if grep -q "FATAL EXCEPTION" "$OUT/logcat-sonido.txt"; then
  echo "LA APP SE CERRÓ" >> "$S"
  grep -A 25 "FATAL EXCEPTION" "$OUT/logcat-sonido.txt" | head -40 >> "$S"
fi
# Resumen por formato: qué decodificador lo tocó y si llegó a sonar.
echo "--- Resumen ---" >> "$S"
for f in "$DIR"/*.ts; do
  n=$(basename "$f" .ts)
  dec=$(grep -E "FitoAudio.*canal=Sonido $n: decodificador" "$OUT/logcat-sonido.txt" | tail -1 | sed -E 's/.*decodificador //')
  if grep -qE "FitoAudio.*canal=Sonido $n: ya suena" "$OUT/logcat-sonido.txt"; then ok="SUENA"; else ok="NO SUENA"; fi
  echo "$n: $ok (decodificador: ${dec:-ninguno})" >> "$S"
done
cat "$S"
pkill -f "http.server 8768" || true
exit 0
