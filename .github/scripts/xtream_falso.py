#!/usr/bin/env python3
"""Proveedor Xtream de mentira con muchísimo contenido, para probar FitoTV con listas enormes.

Uso: python3 xtream_falso.py PUERTO [canales] [peliculas] [series]

Contesta player_api.php (cuenta, categorías, canales, películas, series y temporadas),
get.php (la lista M3U equivalente, con un renglón por episodio) y xmltv.php (guía corta).
Todo se genera al vuelo y se manda por partes para no llenar la memoria de la compu.
"""
import base64
import os
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8766
LIVE = int(sys.argv[2]) if len(sys.argv) > 2 else 25000
VOD = int(sys.argv[3]) if len(sys.argv) > 3 else 120000
SERIES = int(sys.argv[4]) if len(sys.argv) > 4 else 15000
SEASONS, EPISODES = 3, 8
# Modo "realista": pocos canales con los nombres disfrazados que mandan algunos proveedores
# ("CINE | *HBO* F@MILY SD"), grupos con emojis y videos de verdad para reproducir.
REAL = os.environ.get("FALSO_MODO") == "realista"
MEDIA = os.environ.get("FALSO_MEDIA", "/tmp/sonido")
REAL_LIVE = [
    ("⭐CINE PREMIUM", ["CINE | *HBO* F@MILY SD", "CINE | *HBÖ* 2 HD", "CINE | *C1NEMAX* HD", "CINE | A&E FHD",
                        "CINE | STAR CHANNEL HD", "CINE | TNT SERIES FHD", "CINE | WARNER FHD", "CINE | *HBO* HD"]),
    ("⭐DƏPORTES PE", ["DEPORTES | ESPN 2 HD", "DEPORTES | FOX SPORTS", "DEPORTES | TUDN"]),
    ("✨ COPA LIBERTADORES 2026 ✨", ["COPA | PARTIDO 1", "COPA | PARTIDO 2"]),
]
REAL_VOD = ["🎬 ACCIÓN", "⭐ TERROR ⭐", "ESTRENOS 2026 🔥", "COMEDIA"]
REAL_SER = ["⭐ SERIES NETFLIX", "ANIME 🔥"]

if REAL:
    LIVE = sum(len(c[1]) for c in REAL_LIVE)
    VOD = 30 * len(REAL_VOD)
    SERIES = 6 * len(REAL_SER)

LIVE_CATS = ["Noticias", "Deportes", "Infantil", "Cine", "Música", "México", "Documentales", "Novelas"] + [f"Grupo {i}" for i in range(1, 293)]
VOD_CATS = [f"Películas {g}" for g in ["Acción", "Comedia", "Terror", "Drama", "Animadas", "Estrenos 2026"]] + [f"Cine {i}" for i in range(1, 195)]
SER_CATS = [f"Series {g}" for g in ["Paramount", "Acción", "Policiales", "Comedia", "Anime", "Novelas"]] + [f"Series {i}" for i in range(1, 95)]
if REAL:
    LIVE_CATS = [c[0] for c in REAL_LIVE]
    VOD_CATS = REAL_VOD
    SER_CATS = REAL_SER


def cats(names):
    return "[" + ",".join(f'{{"category_id":"{i + 1}","category_name":"{n}","parent_id":0}}' for i, n in enumerate(names)) + "]"


class H(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.0"

    def log_message(self, *a):
        pass

    def start(self, ctype="application/json"):
        self.send_response(200)
        self.send_header("Content-Type", ctype)
        self.end_headers()

    def chunks(self, gen):
        buf = []
        size = 0
        for part in gen:
            buf.append(part)
            size += len(part)
            if size > 256 * 1024:
                self.wfile.write("".join(buf).encode())
                buf, size = [], 0
        if buf:
            self.wfile.write("".join(buf).encode())

    def do_GET(self):
        u = urlparse(self.path)
        q = {k: v[0] for k, v in parse_qs(u.query).items()}
        host = self.headers.get("Host", f"10.0.2.2:{PORT}")
        base = f"http://{host}"
        if u.path.endswith("/player_api.php"):
            a = q.get("action")
            if a is None:
                self.start()
                exp = int(time.time()) + 90 * 86400
                self.wfile.write(('{"user_info":{"username":"prueba","auth":1,"status":"Active","exp_date":"%d",'
                                  '"max_connections":"2","active_cons":"0","is_trial":"0","allowed_output_formats":["m3u8","ts"]},"server_info":{"url":"falso","port":"%d"}}' % (exp, PORT)).encode())
            elif a == "get_live_categories":
                self.start(); self.wfile.write(cats(LIVE_CATS).encode())
            elif a == "get_vod_categories":
                self.start(); self.wfile.write(cats(VOD_CATS).encode())
            elif a == "get_series_categories":
                self.start(); self.wfile.write(cats(SER_CATS).encode())
            elif a == "get_live_streams":
                self.start()
                self.chunks(self.live_items())
            elif a == "get_vod_streams":
                self.start()
                self.chunks(self.vod_items())
            elif a == "get_series":
                self.start()
                self.chunks(self.series_items())
            elif a == "get_series_info":
                sid = int(q.get("series_id", "1"))
                self.start()
                eps = []
                for s in range(1, SEASONS + 1):
                    items = ",".join(
                        f'{{"id":"{sid * 100 + s * 10 + e}","episode_num":{e},"title":"Serie {sid} S{s:02d} E{e:02d} - Capítulo {e}",'
                        f'"container_extension":"{"mkv" if REAL else "mp4"}","info":{{"duration":"00:42:00"}}}}'
                        for e in range(1, EPISODES + 1))
                    eps.append(f'"{s}":[{items}]')
                seasons = ",".join(f'{{"season_number":{s},"name":"Season {s}"}}' for s in range(1, SEASONS + 1))
                self.wfile.write(('{"info":{"name":"Serie %d","plot":"Una serie de prueba con %d temporadas.","releaseDate":"2021-03-01"},'
                                  '"seasons":[%s],"episodes":{%s}}' % (sid, SEASONS, seasons, ",".join(eps))).encode())
            elif a == "get_short_epg":
                sid = int(q.get("stream_id", "1"))
                now = int(time.time())
                base_t = now - now % 1800
                items = []
                for k in range(6):
                    t0 = base_t + (k - 1) * 1800
                    title = base64.b64encode(f"Programa {k + 1} del canal {sid}".encode()).decode()
                    desc = base64.b64encode("Guía de prueba".encode()).decode()
                    items.append(f'{{"title":"{title}","description":"{desc}","start_timestamp":"{t0}","stop_timestamp":"{t0 + 1800}"}}')
                self.start()
                self.wfile.write(('{"epg_listings":[' + ",".join(items) + "]}").encode())
            else:
                self.start(); self.wfile.write(b"[]")
        elif u.path.endswith("/get.php") or u.path.endswith(".m3u"):
            self.start("audio/x-mpegurl")
            self.chunks(self.m3u(base))
        elif u.path.endswith("/xmltv.php"):
            self.start("application/xml")
            now = time.gmtime()
            day = time.strftime("%Y%m%d", now)
            parts = ['<?xml version="1.0" encoding="UTF-8"?><tv>']
            for c in range(1, 200):
                for h in range(24):
                    parts.append(f'<programme start="{day}{h:02d}0000 +0000" stop="{day}{h:02d}5900 +0000" channel="canal{c}">'
                                 f'<title>Programa {h} del canal {c}</title><desc>Descripción</desc></programme>')
            parts.append("</tv>")
            self.wfile.write("".join(parts).encode())
        elif u.path.startswith(("/live/", "/movie/", "/series/")) or (REAL and u.path.count("/") == 3):
            self.media(u.path)
        else:
            self.send_response(404)
            self.end_headers()

    def media(self, path):
        """Videos de prueba: los canales tocan un .ts y las películas y episodios un .mkv con dos idiomas y subtítulos."""
        name = "pista.mkv" if path.startswith(("/movie/", "/series/")) else "aac.ts"
        f = os.path.join(MEDIA, name)
        if not os.path.exists(f):
            self.send_response(404)
            self.end_headers()
            return
        size = os.path.getsize(f)
        start, end = 0, size - 1
        rng = self.headers.get("Range")
        if rng and rng.startswith("bytes="):
            a, _, b = rng[6:].partition("-")
            start = int(a) if a else 0
            end = int(b) if b else size - 1
            self.send_response(206)
            self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        else:
            self.send_response(200)
        self.send_header("Content-Type", "video/x-matroska" if name.endswith(".mkv") else "video/mp2t")
        self.send_header("Content-Length", str(end - start + 1))
        self.send_header("Accept-Ranges", "bytes")
        self.end_headers()
        try:
            with open(f, "rb") as fh:
                fh.seek(start)
                left = end - start + 1
                while left > 0:
                    chunk = fh.read(min(65536, left))
                    if not chunk:
                        break
                    self.wfile.write(chunk)
                    left -= len(chunk)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def live_items(self):
        if REAL:
            yield "["
            i = 0
            for ci, (cat, names) in enumerate(REAL_LIVE):
                for n in names:
                    i += 1
                    sep = "," if i > 1 else ""
                    yield (f'{sep}{{"num":{i},"name":"{n}","stream_type":"live","stream_id":{i},"stream_icon":"",'
                           f'"epg_channel_id":"","added":"1700000000","category_id":"{ci + 1}","tv_archive":0}}')
            yield "]"
            return
        yield "["
        for i in range(1, LIVE + 1):
            cat = (i % len(LIVE_CATS)) + 1
            sep = "," if i > 1 else ""
            yield (f'{sep}{{"num":{i},"name":"Canal {i} HD","stream_type":"live","stream_id":{i},"stream_icon":"",'
                   f'"epg_channel_id":"canal{i}","added":"1700000000","category_id":"{cat}","tv_archive":0}}')
        yield "]"

    def vod_items(self):
        yield "["
        for i in range(1, VOD + 1):
            cat = (i % len(VOD_CATS)) + 1
            sep = "," if i > 1 else ""
            yield (f'{sep}{{"num":{i},"name":"Película número {i} ({1990 + i % 36})","stream_type":"movie","stream_id":{100000 + i},'
                   f'"stream_icon":"","rating":"7.1","added":"{1700000000 + i * 60}","category_id":"{cat}","container_extension":"{"mkv" if REAL else "mp4"}"}}')
        yield "]"

    def series_items(self):
        yield "["
        for i in range(1, SERIES + 1):
            cat = (i % len(SER_CATS)) + 1
            sep = "," if i > 1 else ""
            yield (f'{sep}{{"num":{i},"name":"Serie {i}","series_id":{i},"cover":"","plot":"Trama","category_id":"{cat}",'
                   f'"last_modified":"{1700000000 + i * 60}"}}')
        yield "]"

    def m3u(self, base):
        yield "#EXTM3U\n"
        for i in range(1, LIVE + 1):
            g = LIVE_CATS[i % len(LIVE_CATS)]
            yield f'#EXTINF:-1 tvg-id="canal{i}" tvg-name="Canal {i} HD" group-title="{g}",Canal {i} HD\n{base}/u/p/{i}.ts\n'
        for i in range(1, VOD + 1):
            g = VOD_CATS[i % len(VOD_CATS)]
            yield f'#EXTINF:-1 group-title="{g}",Película número {i}\n{base}/movie/u/p/{100000 + i}.mp4\n'
        for i in range(1, SERIES + 1):
            g = SER_CATS[i % len(SER_CATS)]
            for s in range(1, SEASONS + 1):
                for e in range(1, EPISODES + 1):
                    yield f'#EXTINF:-1 group-title="{g}",Serie {i} S{s:02d} E{e:02d}\n{base}/series/u/p/{i * 100 + s * 10 + e}.mp4\n'


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", PORT), H).serve_forever()
