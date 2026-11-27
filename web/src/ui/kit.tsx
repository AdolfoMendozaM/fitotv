import { useEffect, useState } from 'preact/hooks';
import type { JSX } from 'preact';
import type { ContentKind, Programme, RecentItem } from '../data/models';
import { avatarKey } from '../data/models';
import { toast } from '../data/store';

const base = import.meta.env.BASE_URL;
export const asset = (p: string) => `${base}${p}`;

export const I = {
  home: 'M4 11l8-7 8 7v8a1 1 0 0 1-1 1h-4v-6h-6v6H5a1 1 0 0 1-1-1z',
  guide: 'M3 6h18M3 12h18M3 18h18M8 3v18',
  film: 'M4 4h16v16H4zM8 4v16M16 4v16M4 9h4M4 15h4M16 9h4M16 15h4',
  tv: 'M3 8h18v12H3zM8 3l4 4 4-4',
  search: 'M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14zM20 20l-4-4',
  user: 'M12 3a4 4 0 1 0 0 8a4 4 0 1 0 0-8zM4 21a8 8 0 0 1 16 0',
  play: 'M8 5v14l11-7z',
  pause: 'M7 5h4v14H7zM13 5h4v14h-4z',
  star: 'M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z',
  restart: 'M4 12a8 8 0 1 0 2.5-5.8M4 4v5h5',
  chevronDown: 'M6 9l6 6 6-6',
  chevronLeft: 'M15 6l-6 6 6 6',
  chevronRight: 'M9 6l6 6-6 6',
  plus: 'M12 5v14M5 12h14',
  close: 'M6 6l12 12M18 6L6 18',
  mute: 'M4 9v6h4l5 4V5L8 9zM17 9l4 6M21 9l-4 6',
  sound: 'M4 9v6h4l5 4V5L8 9zM16 9a4 4 0 0 1 0 6M18.5 6.5a8 8 0 0 1 0 11',
  grid: 'M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z',
  history: 'M12 7v5l3 2M3.5 12a8.5 8.5 0 1 0 2.5-6M3 4v4h4',
  clock: 'M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM12 7v5l3 2',
  check: 'M5 12.5l4.5 4.5L19 7.5',
  warn: 'M12 7v6M12 17h.01',
  edit: 'M4 20h4L19 9l-4-4L4 16z',
  trash: 'M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13',
  refresh: 'M20 12a8 8 0 1 1-2.5-5.8M20 4v5h-5',
  cast: 'M3 8V6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-6M3 12a8 8 0 0 1 8 8M3 16a4 4 0 0 1 4 4M3 20h.01',
  airplay: 'M5 17H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-1M12 15l5 6H7z',
  pip: 'M3 5h18v14H3zM12 11h7v6h-7z',
  full: 'M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5',
  list: 'M4 6h16M4 12h16M4 18h10',
  news: 'M4 5h13v14H6a2 2 0 0 1-2-2zM17 9h3v8a2 2 0 0 1-2 2M8 9h5M8 13h5',
  ball: 'M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM3.5 12h17M12 3c-3 3-3 15 0 18M12 3c3 3 3 15 0 18',
  kids: 'M12 3a6 6 0 0 1 6 6c0 4-3 7-6 7s-6-3-6-7a6 6 0 0 1 6-6zM12 16v5M10 21h4',
  music: 'M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0a3 3 0 1 1 6 0zM20 16a3 3 0 1 1-6 0a3 3 0 1 1 6 0z',
  globe: 'M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18',
  pin: 'M12 21s-7-6.5-7-11a7 7 0 0 1 14 0c0 4.5-7 11-7 11zM12 7.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z',
  book: 'M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2zM4 5v16',
  phone: 'M7 3h10v18H7zM11 18h2',
  share: 'M12 3v12M7 8l5-5 5 5M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6',
  link: 'M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1',
  file: 'M6 3h8l4 4v14H6zM14 3v4h4',
  key: 'M15 7a4 4 0 1 0-3.9 5L4 19v2h3v-2h2v-2h2l1.1-1.1A4 4 0 0 0 15 7zM16 6h.01',
};

const filled = new Set([I.play, I.pause, I.star]);

export function Icon({ d, size = 22, color = 'currentColor', stroke = 2, fill }: { d: string; size?: number; color?: string; stroke?: number; fill?: boolean }) {
  const isFill = fill ?? filled.has(d);
  return (
    <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill={isFill ? color : 'none'} stroke={isFill ? 'none' : color} stroke-width={stroke} stroke-linecap="round" stroke-linejoin="round" style={{ flex: 'none', display: 'block' }}>
      <path d={d} />
    </svg>
  );
}

export function Avatar({ k, size, ring }: { k?: string | null; size: number; ring?: boolean }) {
  return (
    <img
      src={asset(`img/avatar_${avatarKey(k)}.webp`)}
      alt=""
      width={size}
      height={size}
      style={{ width: size, height: size, borderRadius: '50%', flex: 'none', boxShadow: ring ? `0 0 0 3px var(--menta)` : undefined }}
    />
  );
}

export const Logo = ({ size }: { size: number }) => <img src={asset('img/logo.png')} alt="" width={size} height={size} style={{ width: size, height: size, flex: 'none' }} />;

export const Brand = ({ size = 36 }: { size?: number }) => (
  <div class="brand" style={{ fontSize: size * 0.68 }}>
    <Logo size={size} />
    FitoTV
  </div>
);

const tiles = ['#3A55B8', '#1F3A9E', '#6A4FC2', '#1F7A8C', '#2E6A8F', '#4B3FA8', '#1F5F9E', '#3A6FB8'];
export function tileFor(key: string) {
  let h = 0;
  for (let i = 0; i < key.length; i++) h = (Math.imul(31, h) + key.charCodeAt(i)) | 0;
  return tiles[(h & 0x7fffffff) % tiles.length];
}

const stop = new Set(['de', 'del', 'la', 'el', 'en', 'y', 'los', 'las', 'tv', 'hd', 'fhd', 'sd', '4k', 'canal']);
export function initials(name: string) {
  const words = name.split(/[^\p{L}\p{N}]+/u).filter(Boolean);
  const digits = words.find((w) => /^\d{1,3}$/.test(w));
  if (digits) return digits;
  const main = words.filter((w) => !stop.has(w.toLowerCase()));
  return (main.length ? main : words).slice(0, 2).map((w) => w[0].toUpperCase()).join('') || 'TV';
}

/** Logo del canal o sus iniciales sobre un color. */
export function ChannelLogo({ name, logo, size, radius = 9 }: { name: string; logo?: string; size: number; radius?: number }) {
  const [broken, setBroken] = useState(false);
  return (
    <div style={{ width: size, height: size, borderRadius: radius, background: tileFor(name), flex: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', fontWeight: 700, fontSize: Math.max(9, size * 0.3), letterSpacing: '-0.02em' }}>
      {logo && !broken ? (
        <img src={logo} alt="" loading="lazy" referrerpolicy="no-referrer" onError={() => setBroken(true)} style={{ width: '82%', height: '82%', objectFit: 'contain' }} />
      ) : (
        initials(name)
      )}
    </div>
  );
}

export function Poster({ name, logo, size = 44 }: { name: string; logo?: string; size?: number }) {
  const [broken, setBroken] = useState(false);
  return (
    <>
      <span class="display" style={{ fontSize: size, color: 'rgba(255,255,255,.9)' }}>
        {initials(name)}
      </span>
      {logo && !broken && <img src={logo} alt="" loading="lazy" referrerpolicy="no-referrer" onError={() => setBroken(true)} />}
    </>
  );
}

export const Progress = ({ value, width, height = 6 }: { value: number; width: number | string; height?: number }) => (
  <span class="progress" style={{ width, height }}>
    <span style={{ width: `${Math.round(Math.max(0, Math.min(1, value)) * 100)}%` }} />
  </span>
);

export const Spinner = ({ size = 20 }: { size?: number }) => <span class="spinner" style={{ width: size, height: size }} />;

export function Toast() {
  const t = toast.value;
  const [shown, setShown] = useState<number | null>(null);
  useEffect(() => {
    if (!t) return;
    setShown(t.id);
    const h = setTimeout(() => {
      setShown(null);
      toast.value = null;
    }, 5500);
    return () => clearTimeout(h);
  }, [t?.id]);
  if (!t || shown !== t.id) return null;
  return (
    <div class="toast" role="status">
      <span class="ico" style={{ background: t.warn ? 'var(--ambar)' : 'var(--menta)' }}>
        <Icon d={t.warn ? I.warn : I.check} color="var(--noche)" stroke={2.6} />
      </span>
      <div class="col" style={{ gap: 4 }}>
        <strong style={{ fontSize: 16 }}>{t.title}</strong>
        <span class="muted2" style={{ fontSize: 14, lineHeight: 1.4 }}>
          {t.text}
        </span>
      </div>
    </div>
  );
}

export function useIsPhone() {
  const q = '(max-width: 899px)';
  const [phone, setPhone] = useState(() => typeof matchMedia !== 'undefined' && matchMedia(q).matches);
  useEffect(() => {
    const m = matchMedia(q);
    const on = () => setPhone(m.matches);
    m.addEventListener('change', on);
    return () => m.removeEventListener('change', on);
  }, []);
  return phone;
}

export function useNow(ms = 30_000) {
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const h = setInterval(() => setNow(Date.now()), ms);
    return () => clearInterval(h);
  }, [ms]);
  return now;
}

// ----- Textos de tiempo -----

export const hhmm = (t: number) => new Date(t).toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit', hour12: false });

export function minutesLeft(p: Programme, now = Date.now()) {
  const m = Math.max(0, Math.round((p.stop - now) / 60000));
  return m >= 60 ? `quedan ${Math.floor(m / 60)} h ${m % 60} min` : `quedan ${m} min`;
}

export const progressOf = (p: Programme | null, now = Date.now()) =>
  !p || p.stop <= p.start ? 0 : Math.max(0, Math.min(1, (now - p.start) / (p.stop - p.start)));

export function visitedAgo(at: number, now = Date.now()) {
  const min = Math.floor((now - at) / 60000);
  if (min < 2) return 'Visitado hace un momento';
  if (min < 60) return `Visitado hace ${min} min`;
  if (min < 24 * 60) return `Visitado hace ${Math.floor(min / 60)} h`;
  if (min < 48 * 60) return 'Visitado ayer';
  return `Visitado hace ${Math.floor(min / (24 * 60))} días`;
}

export function remaining(r?: RecentItem | null) {
  if (!r || r.durationMs <= 0) return 'Sin empezar';
  const left = Math.max(1, Math.round((r.durationMs - r.positionMs) / 60000));
  return left >= 60 ? `Quedan ${Math.floor(left / 60)} h ${String(left % 60).padStart(2, '0')} min` : `Quedan ${left} min`;
}

export const vodFraction = (r?: RecentItem | null) => (!r || r.durationMs <= 0 ? 0 : Math.max(0, Math.min(1, r.positionMs / r.durationMs)));

export const typeLabel = (k: ContentKind) => (k === 'MOVIE' ? 'Película' : k === 'SERIES' ? 'Serie' : 'Canal');

export function groupIcon(name: string) {
  const g = name.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  const has = (...w: string[]) => w.some((x) => g.includes(x));
  if (has('notici', 'news', 'informa')) return I.news;
  if (has('deport', 'sport', 'futbol', 'liga', 'box', 'nfl', 'nba')) return I.ball;
  if (has('infantil', 'kids', 'nino', 'cartoon', 'caricatura', 'animad', 'familia')) return I.kids;
  if (has('cine', 'pelic', 'movie', 'film')) return I.film;
  if (has('musica', 'music', 'radio')) return I.music;
  if (has('document', 'cultura', 'educa')) return I.book;
  if (has('mexico', 'local', 'nacional', 'abierta')) return I.pin;
  if (has('usa', 'latino', 'espana', 'internacional', 'world')) return I.globe;
  return I.tv;
}

export type Children = JSX.Element | JSX.Element[] | string | null | false | undefined;
