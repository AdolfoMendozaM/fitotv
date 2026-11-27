import type { Programme } from './models';

// Guía de programación (XMLTV). Guardamos de una hora atrás a 30 horas adelante.

function parseTime(raw: string | null): number {
  if (!raw) return 0;
  const m = raw.trim().match(/^(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})\s*([+-]\d{4})?/);
  if (!m) return 0;
  const [, y, mo, d, h, mi, s, tz] = m;
  let t = Date.UTC(+y, +mo - 1, +d, +h, +mi, +s);
  if (tz) {
    const sign = tz[0] === '-' ? -1 : 1;
    const off = (parseInt(tz.slice(1, 3), 10) * 60 + parseInt(tz.slice(3, 5), 10)) * 60000;
    t -= sign * off;
  }
  return t;
}

const decode = (s: string) =>
  s
    .replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, '$1')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;|&apos;/g, "'")
    .replace(/&amp;/g, '&')
    .trim();

export function parseXmltv(xml: string, wanted: Set<string>): Record<string, Programme[]> {
  const now = Date.now();
  const from = now - 3600_000;
  const to = now + 30 * 3600_000;
  const out: Record<string, Programme[]> = {};
  const re = /<programme\b([^>]*)>([\s\S]*?)<\/programme>/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(xml))) {
    const attrs = m[1];
    const ch = (attrs.match(/channel="([^"]*)"/)?.[1] ?? '').toLowerCase();
    if (!ch || !wanted.has(ch)) continue;
    const start = parseTime(attrs.match(/start="([^"]*)"/)?.[1] ?? null);
    const stop = parseTime(attrs.match(/stop="([^"]*)"/)?.[1] ?? null);
    if (!start || stop <= from || start >= to) continue;
    const body = m[2];
    const title = decode(body.match(/<title\b[^>]*>([\s\S]*?)<\/title>/)?.[1] ?? '') || 'Sin título';
    const desc = decode(body.match(/<desc\b[^>]*>([\s\S]*?)<\/desc>/)?.[1] ?? '').slice(0, 400);
    (out[ch] ||= []).push({ start, stop, title, desc });
  }
  for (const k of Object.keys(out)) out[k].sort((a, b) => a.start - b.start);
  return out;
}

export function nowNext(list: Programme[] | undefined, now = Date.now()): [Programme | null, Programme | null] {
  if (!list?.length) return [null, null];
  const i = list.findIndex((p) => p.start <= now && p.stop > now);
  if (i < 0) return [null, list.find((p) => p.start > now) ?? null];
  return [list[i], list[i + 1] ?? null];
}
