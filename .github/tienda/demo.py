#!/usr/bin/env python3
"""Lista de ejemplo para las capturas de la tienda.

Canales inventados (sin marcas reales) con logos hechos aquí, una guía de programación
alrededor de la hora actual y algunas películas y series. El video de muestra son
transmisiones de prueba públicas (películas abiertas de Blender y patrones de prueba).

Uso: python3 demo.py <carpeta> <url_base>
"""
import datetime as dt
import os
import random
import sys
import unicodedata
from xml.sax.saxutils import escape

from PIL import Image, ImageDraw, ImageFont

OUT, BASE = sys.argv[1], sys.argv[2].rstrip('/') + '/'
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
FONT_BOLD = os.path.join(ROOT, 'app/src/main/res/font/bricolage_extrabold.ttf')
FONT_SEMI = os.path.join(ROOT, 'app/src/main/res/font/jakarta_bold.ttf')

STREAMS = [
    'https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8',
    'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8',
    'https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8',
    'https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8',
]
VOD = 'https://download.blender.org/durian/trailer/sintel_trailer-480p.mp4'

# (número, nombre, grupo, colores del logo, programas)
CHANNELS = [
    (1, 'Cocina Viva', 'Entretenimiento', ('#FF7A00', '#FFFFFF'), ['Desayunos rápidos', 'Antojitos de la abuela', 'Postres sin horno', 'Cocina de mercado', 'Cena en 20 minutos']),
    (2, 'Rutas y Sabores', 'Entretenimiento', ('#00A3A3', '#FFFFFF'), ['Oaxaca en tres días', 'Mercados del mundo', 'Pueblos con encanto', 'Carretera al mar']),
    (3, 'Cine Clásico', 'Entretenimiento', ('#B42318', '#FFE8A3'), ['Función de matiné', 'El faro del norte', 'Ciclo de comedia', 'Noche de suspenso']),
    (4, 'Estreno Plus', 'Entretenimiento', ('#7A5AF8', '#FFFFFF'), ['La última estación', 'Barrio Alto', 'Detrás de cámaras', 'Maratón de verano']),
    (5, 'Mundo Animal', 'Entretenimiento', ('#3B7A57', '#FFFFFF'), ['Selva en vivo', 'Océanos', 'Grandes migraciones', 'Rescate animal']),
    (6, 'Ciencia Hoy', 'Entretenimiento', ('#0BA5EC', '#0B1B3F'), ['El universo en un minuto', 'Inventos', 'Laboratorio abierto', 'Preguntas al cielo']),
    (7, 'Música 24', 'Entretenimiento', ('#101828', '#3DE0BE'), ['Éxitos de hoy', 'Sesiones acústicas', 'Clásicos en español', 'Conciertos']),
    (8, 'Risas TV', 'Entretenimiento', ('#FFD000', '#0B1B3F'), ['Comedia en vivo', 'Bromas en la calle', 'Stand-up', 'Sketches']),
    (9, 'Hogar y Jardín', 'Entretenimiento', ('#65A30D', '#FFFFFF'), ['Remodela tu sala', 'Huerto en casa', 'Ideas con poco', 'Plantas fáciles']),
    (10, 'Rumbo Viajero', 'Entretenimiento', ('#0E7490', '#FFFFFF'), ['Mochila al hombro', 'Playas escondidas', 'Ciudades de noche', 'Rutas del café']),
    (11, 'Altamar Noticias', 'Noticias', ('#1E5BFF', '#FFFFFF'), ['Noticiero de la mañana', 'Mesa de análisis', 'Clima y tránsito', 'Edición estelar', 'Reportajes', 'Noticias de la noche']),
    (12, 'Canal Ciudad', 'Noticias', ('#FF5A36', '#FFFFFF'), ['Buenos días, ciudad', 'Entrevista abierta', 'Ciudad en vivo', 'Barrios', 'Resumen del día']),
    (13, 'Hoy en 24', 'Noticias', ('#475467', '#FFFFFF'), ['Titulares', 'Economía al día', 'Mundo hoy', 'Cierre informativo']),
    (14, 'Deporte Total', 'Deportes', ('#12B76A', '#0B1B3F'), ['Gol a gol', 'Liga regional: semifinal', 'Vestidor', 'Ciclismo de montaña', 'Resumen deportivo']),
    (15, 'Arena 24', 'Deportes', ('#F5B400', '#0B1B3F'), ['Box: noche de campeones', 'Tenis: cuartos de final', 'Charla técnica', 'Clásicos del estadio']),
    (16, 'Cancha Abierta', 'Deportes', ('#16A34A', '#FFFFFF'), ['Previa del partido', 'Fútbol femenil', 'Goles de la semana', 'Análisis táctico']),
    (17, 'Pista Motor', 'Deportes', ('#DC2626', '#FFFFFF'), ['Rally de montaña', 'Autos clásicos', 'Motos en pista', 'Taller abierto']),
    (18, 'Planeta Kids', 'Infantil', ('#EE46BC', '#FFFFFF'), ['Los exploradores', 'Ciencia divertida', 'Cuentos animados', 'Dibujemos juntos', 'Hora de dormir']),
    (19, 'Cuentos TV', 'Infantil', ('#F97316', '#FFFFFF'), ['Había una vez', 'Fábulas', 'Canciones para cantar', 'Teatro de títeres']),
    (20, 'Mini Genios', 'Infantil', ('#8B5CF6', '#FFFFFF'), ['Números divertidos', 'Experimentos', 'Letras y sonidos', 'Arte con papel']),
]

MOVIES = [
    ('La isla del faro', 'Películas', '#1E3A8A'), ('Camino al norte', 'Películas', '#9A3412'),
    ('Verano en la costa', 'Películas', '#0E7490'), ('El último tren', 'Películas', '#4C1D95'),
    ('Luces de la ciudad', 'Películas', '#B45309'), ('Bajo la lluvia', 'Películas', '#155E75'),
]
SERIES = [
    ('Barrio Alto T1 E1', 'Series de drama', '#7C2D12'), ('Barrio Alto T1 E2', 'Series de drama', '#7C2D12'),
    ('Detectives del sur T1 E1', 'Series policiacas', '#1F2937'), ('La casa azul T2 E5', 'Series de comedia', '#1D4ED8'),
]


def slug(name):
    plain = unicodedata.normalize('NFKD', name).encode('ascii', 'ignore').decode()
    return ''.join(c.lower() if c.isalnum() else '-' for c in plain).strip('-')


def logo(name, colors, path):
    bg, fg = colors
    W, H = 400, 225
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([10, 30, W - 10, H - 30], radius=36, fill=bg)
    words = name.split()
    if len(words) >= 2 and len(name) > 9:
        half = (len(words) + 1) // 2
        text = ' '.join(words[:half]) + '\n' + ' '.join(words[half:])
    else:
        text = name
    size = 76
    while True:
        f = ImageFont.truetype(FONT_BOLD, size)
        bb = d.multiline_textbbox((0, 0), text, font=f, align='center', spacing=0)
        if (bb[2] - bb[0] <= W - 70 and bb[3] - bb[1] <= H - 90) or size <= 30:
            break
        size -= 2
    d.multiline_text(((W - (bb[2] - bb[0])) / 2 - bb[0], (H - (bb[3] - bb[1])) / 2 - bb[1]), text, font=f, fill=fg, align='center', spacing=0)
    im.save(path)


def poster(title, color, path):
    W, H = 640, 360
    base = Image.new('RGB', (W, H), color)
    d = ImageDraw.Draw(base)
    rnd = random.Random(title)
    for _ in range(14):
        x, y, r = rnd.randint(-80, W), rnd.randint(-80, H), rnd.randint(40, 180)
        shade = tuple(min(255, int(int(color[i:i + 2], 16) * rnd.uniform(0.6, 1.6))) for i in (1, 3, 5))
        d.ellipse([x - r, y - r, x + r, y + r], fill=shade)
    grad = Image.new('L', (1, H))
    for yy in range(H):
        grad.putpixel((0, yy), int(230 * max(0, (yy - H * 0.35) / (H * 0.65))))
    shade = Image.new('RGB', (W, H), (10, 23, 71))
    base = Image.composite(shade, base, grad.resize((W, H)))
    d = ImageDraw.Draw(base)
    f = ImageFont.truetype(FONT_BOLD, 58)
    d.text((36, H - 100), title, font=f, fill='white')
    base.save(path)


def main():
    os.makedirs(os.path.join(OUT, 'logos'), exist_ok=True)
    os.makedirs(os.path.join(OUT, 'posters'), exist_ok=True)
    lines = [f'#EXTM3U x-tvg-url="{BASE}guia.xml"']
    for i, (num, name, group, colors, _) in enumerate(CHANNELS):
        s = slug(name)
        logo(name, colors, os.path.join(OUT, 'logos', s + '.png'))
        lines.append(f'#EXTINF:-1 tvg-id="{s}" tvg-chno="{num}" tvg-logo="{BASE}logos/{s}.png" group-title="{group}",{name}')
        lines.append(STREAMS[i % len(STREAMS)])
    for title, group, color in MOVIES + SERIES:
        s = slug(title)
        poster(title, color, os.path.join(OUT, 'posters', s + '.png'))
        lines.append(f'#EXTINF:-1 tvg-logo="{BASE}posters/{s}.png" group-title="{group}",{title}')
        lines.append(VOD + '?v=' + s)
    open(os.path.join(OUT, 'lista.m3u'), 'w', encoding='utf-8').write('\n'.join(lines) + '\n')

    # Guía: de 2 horas antes a 10 horas después de ahora, en bloques de 30 a 90 minutos.
    now = dt.datetime.now(dt.timezone.utc).replace(second=0, microsecond=0)
    start0 = now.replace(minute=0) - dt.timedelta(hours=2)
    fmt = lambda t: t.strftime('%Y%m%d%H%M%S +0000')
    xml = ['<?xml version="1.0" encoding="UTF-8"?>', '<tv generator-info-name="FitoTV demo">']
    for num, name, group, colors, shows in CHANNELS:
        s = slug(name)
        xml.append(f'  <channel id="{s}"><display-name>{escape(name)}</display-name></channel>')
    for num, name, group, colors, shows in CHANNELS:
        s = slug(name)
        rnd = random.Random(name)
        t = start0
        k = rnd.randrange(len(shows))
        while t < now + dt.timedelta(hours=10):
            dur = dt.timedelta(minutes=rnd.choice([30, 30, 60, 60, 90]))
            title = shows[k % len(shows)]
            xml.append(f'  <programme start="{fmt(t)}" stop="{fmt(t + dur)}" channel="{s}">'
                       f'<title lang="es">{escape(title)}</title>'
                       f'<desc lang="es">{escape(title)} en {escape(name)}.</desc></programme>')
            t += dur
            k += 1
    xml.append('</tv>')
    open(os.path.join(OUT, 'guia.xml'), 'w', encoding='utf-8').write('\n'.join(xml) + '\n')
    print(f'lista de ejemplo: {len(CHANNELS)} canales, {len(MOVIES)} películas, {len(SERIES)} episodios')


if __name__ == '__main__':
    main()
