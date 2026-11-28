#!/usr/bin/env bash
# Prueba con un proveedor ENORME (como los que traen cientos de miles de películas y episodios):
# mide cuánto tarda en agregarse, cuánta memoria usa, si se cierra, y toma capturas de
# Inicio, Películas, Series (por temporadas) y del arranque con la copia guardada.
set -u
OUT=recorrido
mkdir -p "$OUT"
PKG=com.fitotv.app.dev
M="$OUT/medidas.txt"
: > "$M"

# Por la API: 25 mil canales, 120 mil películas y 15 mil series (360 mil episodios si fuera M3U).
nohup python3 .github/scripts/xtream_falso.py 8766 25000 120000 15000 >/dev/null 2>&1 &
# Por M3U: 10 mil canales, 50 mil películas y 96 mil episodios sueltos.
nohup python3 .github/scripts/xtream_falso.py 8767 10000 50000 4000 >/dev/null 2>&1 &
sleep 2

shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "captura $1"; }
key() { adb shell input keyevent "$@"; sleep 0.8; }
alive() { [ -n "$(adb shell pidof $PKG | tr -d '\r')" ]; }
mem() { echo "$1: $(adb shell dumpsys meminfo $PKG | grep -E 'TOTAL( PSS)?:|TOTAL  ' | head -1 | tr -s ' ')" | tee -a "$M"; }

onboard() {
  adb shell pm clear "$PKG" >/dev/null
  adb shell am start -n "$PKG/com.fitotv.MainActivity" >/dev/null
  sleep 6
  key KEYCODE_DPAD_CENTER; key KEYCODE_DPAD_CENTER; key KEYCODE_DPAD_CENTER
  sleep 1.5
  key KEYCODE_DPAD_CENTER; sleep 1.5
  adb shell input text "Prueba"; sleep 0.5
  key KEYCODE_ENTER; sleep 1
  key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_CENTER
  key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_DOWN
  key KEYCODE_DPAD_CENTER
  sleep 3
  adb forward tcp:8090 tcp:8090 >/dev/null
}

# Espera a que la lista quede guardada; avisa si la app se cerró.
wait_list() {
  local t0=$(date +%s)
  for i in $(seq 1 360); do
    if adb shell run-as "$PKG" ls files/lists 2>/dev/null | grep -q "channels.bin"; then
      echo "$1: lista lista en $(( $(date +%s) - t0 )) s" | tee -a "$M"
      return 0
    fi
    if ! alive; then
      echo "$1: LA APP SE CERRÓ a los $(( $(date +%s) - t0 )) s" | tee -a "$M"
      return 1
    fi
    sleep 1
  done
  echo "$1: no terminó en 6 minutos" | tee -a "$M"
  return 1
}

# Espera a que la pantalla muestre un texto (Compose lo expone para accesibilidad).
wait_text() {
  local t0=$(date +%s)
  for i in $(seq 1 120); do
    adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1
    if adb shell cat /sdcard/u.xml 2>/dev/null | grep -q "$2"; then
      echo "$1: '$2' en pantalla a los $(( $(date +%s) - t0 )) s" | tee -a "$M"
      return 0
    fi
    sleep 1
  done
  echo "$1: no apareció '$2'" | tee -a "$M"
  return 1
}

rail() { # va al menú lateral y baja N lugares desde el avatar
  for i in 1 2 3 4 5 6 7; do key KEYCODE_DPAD_LEFT; done
  for i in 1 2 3 4 5 6 7 8; do key KEYCODE_DPAD_UP; done
  for i in $(seq 1 "$1"); do key KEYCODE_DPAD_DOWN; done
  key KEYCODE_DPAD_CENTER
}

# ---------------------------------------------------------------- 1) Usuario y contraseña (API)
adb logcat -c
onboard
shot G00-agregar
curl -s -m 30 --data-urlencode "tipo=usuario" --data-urlencode "nombre=Proveedor grande" \
  --data-urlencode "servidor=http://10.0.2.2:8766" --data-urlencode "usuario=prueba" --data-urlencode "contrasena=1234" \
  http://127.0.0.1:8090/lista -o /dev/null && echo "lista enviada (API)"
sleep 2; shot G01-revisando
if wait_list "API"; then
  wait_text "API" "Ver mis canales"
  sleep 1; shot G02-listo
  mem "API, recién agregada"
  key KEYCODE_DPAD_CENTER
  sleep 5; shot G03-inicio
  key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_DOWN
  sleep 2; shot G04-inicio-canales
  rail 4; sleep 4; shot G05-series
  key KEYCODE_DPAD_CENTER; sleep 6; shot G06-serie-temporadas
  key KEYCODE_DPAD_UP; key KEYCODE_DPAD_RIGHT; sleep 2; shot G07-serie-temporada-2
  key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 4; shot G08-episodio
  key KEYCODE_BACK; sleep 2
  key KEYCODE_BACK; sleep 2
  rail 3; sleep 4; shot G09-peliculas
  for i in 1 2 3 4 5 6 7 8 9 10; do adb shell input keyevent KEYCODE_DPAD_DOWN; done
  sleep 2; shot G10-peliculas-abajo
  mem "API, después de recorrer"
  # Cerrar y abrir: debe abrir con la copia guardada, sin bajar todo otra vez.
  adb shell am force-stop "$PKG"; sleep 1
  adb shell am start -n "$PKG/com.fitotv.MainActivity" >/dev/null
  sleep 2; shot G11-arranque-2s
  wait_text "API al volver a abrir" "Ver ahora"
  shot G12-arranque-listo
  mem "API, al volver a abrir"
fi
alive && echo "API: la app sigue abierta" | tee -a "$M"

# ---------------------------------------------------------------- 2) Enlace M3U con episodios sueltos
onboard
curl -s -m 30 --data-urlencode "tipo=enlace" --data-urlencode "nombre=M3U grande" \
  --data-urlencode "enlace=http://10.0.2.2:8767/lista_grande.m3u" \
  http://127.0.0.1:8090/lista -o /dev/null && echo "lista enviada (M3U)"
if wait_list "M3U"; then
  wait_text "M3U" "Ver mis canales"
  sleep 1; shot G20-m3u-listo
  mem "M3U, recién agregada"
  key KEYCODE_DPAD_CENTER
  sleep 5; shot G21-m3u-inicio
  rail 4; sleep 4; shot G22-m3u-series
  key KEYCODE_DPAD_CENTER; sleep 4; shot G23-m3u-temporadas
  key KEYCODE_BACK; sleep 2
  mem "M3U, después de recorrer"
fi
alive && echo "M3U: la app sigue abierta" | tee -a "$M"

adb logcat -d > "$OUT/logcat-grande.txt"
if grep -E "FATAL EXCEPTION|OutOfMemoryError" "$OUT/logcat-grande.txt" > "$OUT/errores-grande.txt"; then
  echo "Hubo cierres: revisa recorrido/logcat-grande.txt" | tee -a "$M"
  grep -A 25 "FATAL EXCEPTION" "$OUT/logcat-grande.txt" | head -60 | tee -a "$M"
fi
grep -E "fitotv|FitoTV" "$OUT/logcat-grande.txt" | grep -iE "skipped .* frames|Davey" | tail -20 >> "$M" || true
grep -E "Choreographer.*Skipped" "$OUT/logcat-grande.txt" | tail -10 >> "$M" || true
cat "$M"
pkill -f xtream_falso.py || true
exit 0
