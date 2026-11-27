import { computed, signal } from '@preact/signals';
import { del, get, set } from 'idb-keyval';
import { nowNext, parseXmltv } from './epg';
import { fetchText, FriendlyError, loadList, webPlayable, xtreamEpg } from './lists';
import {
  emptyData,
  LIST_COLORS,
  newId,
  type AppData,
  type Channel,
  type ContentKind,
  type ListKind,
  type Playlist,
  type Profile,
  type ProfileData,
  type Programme,
  type RecentItem,
} from './models';
import { sfx } from '../ui/sound';

// ---------------------------------------------------------------------------
// Guardado
// ---------------------------------------------------------------------------

const KEY = 'fitotv';

function load(): AppData {
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) return { ...emptyData(), ...JSON.parse(raw) };
  } catch {
    /* datos dañados: empezamos de cero */
  }
  return emptyData();
}

export const data = signal<AppData>(load());

export function update(fn: (d: AppData) => AppData) {
  const next = fn(data.value);
  data.value = next;
  try {
    localStorage.setItem(KEY, JSON.stringify(next));
  } catch {
    /* almacenamiento lleno */
  }
}

// ---------------------------------------------------------------------------
// Navegación (con el botón Atrás del navegador y del celular)
// ---------------------------------------------------------------------------

export type Screen =
  | { name: 'splash' }
  | { name: 'tutorial' }
  | { name: 'createProfile'; firstTime: boolean; editId?: string }
  | { name: 'addList'; firstTime: boolean }
  | { name: 'profiles' }
  | { name: 'home' }
  | { name: 'guide' }
  | { name: 'catalog'; kind: 'MOVIE' | 'SERIES' }
  | { name: 'search' }
  | { name: 'me' };

export const stack = signal<Screen[]>([{ name: 'splash' }]);
export const screen = computed(() => stack.value[stack.value.length - 1]);

export function go(s: Screen) {
  stack.value = [...stack.value, s];
  history.pushState({ fitotv: stack.value.length }, '');
}

export function replaceAll(s: Screen) {
  stack.value = [s];
}

export function openTab(s: Screen) {
  stack.value = s.name === 'home' ? [s] : [{ name: 'home' }, s];
  if (s.name !== 'home') history.pushState({ fitotv: 2 }, '');
}

export function back() {
  if (stack.value.length > 1) history.back();
}

if (typeof window !== 'undefined') {
  window.addEventListener('popstate', () => {
    if (stack.value.length > 1) {
      stack.value = stack.value.slice(0, -1);
      sfx('back');
    }
  });
}

/** Pantalla de carga encima mientras aparece la siguiente (el efecto del play). */
export const revealing = signal(false);

export function beginReveal(next: Screen) {
  replaceAll(next);
  revealing.value = true;
}

// ---------------------------------------------------------------------------
// Sesión
// ---------------------------------------------------------------------------

export interface Toast {
  id: number;
  title: string;
  text: string;
  warn?: boolean;
}

export const toast = signal<Toast | null>(null);
export const showToast = (title: string, text: string, warn = false) => {
  toast.value = { id: Date.now(), title, text, warn };
};

export const profileId = signal<string | null>(null);
export const allChannels = signal<Channel[]>([]);
export const epg = signal<Record<string, Programme[]>>({});
export const selectedGroup = signal<string | null>(null);
export const loading = signal(false);
/** Lo que se está viendo en el reproductor de la página. */
export const current = signal<Channel | null>(null);

export const FAVORITES = '\u0000favoritos';

export const profile = computed<Profile | null>(() => data.value.profiles.find((p) => p.id === profileId.value) ?? null);

export const profileData = computed<ProfileData>(
  () => data.value.profileData[profileId.value ?? ''] ?? { recents: [], favorites: [], lastGroup: {} },
);

export const myLists = computed<Playlist[]>(() =>
  data.value.playlists.filter((l) => l.profileId === profileId.value).sort((a, b) => a.addedAt - b.addedAt),
);

export const activeList = computed<Playlist | null>(() => {
  const p = profile.value;
  if (!p) return null;
  return myLists.value.find((l) => l.id === p.activeListId) ?? myLists.value[0] ?? null;
});

const kidsWords = ['infantil', 'kids', 'nino', 'ninos', 'children', 'cartoon', 'caricatura', 'animad', 'familia'];

export const fold = (s: string) =>
  s
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '');

export const isKidsGroup = (g: string) => kidsWords.some((w) => fold(g).includes(w));

/** Canales visibles para el perfil (el infantil solo ve sus grupos). */
export const channels = computed<Channel[]>(() =>
  profile.value?.kids ? allChannels.value.filter((c) => isKidsGroup(c.group)) : allChannels.value,
);

export const byId = computed(() => new Map(channels.value.map((c) => [c.id, c])));

export interface Group {
  name: string;
  count: number;
}

export const groups = computed<Group[]>(() => {
  const counts = new Map<string, number>();
  for (const c of channels.value) if (c.kind === 'LIVE') counts.set(c.group, (counts.get(c.group) ?? 0) + 1);
  return [...counts].map(([name, count]) => ({ name, count }));
});

export function channelsOf(group: string | null): Channel[] {
  if (group === FAVORITES) return profileData.value.favorites.map((id) => byId.value.get(id)).filter(Boolean) as Channel[];
  return channels.value.filter((c) => c.kind === 'LIVE' && (group == null || c.group === group));
}

export const vod = (kind: ContentKind) => channels.value.filter((c) => c.kind === kind);

export const programmeFor = (c: Channel) => (c.tvgId ? nowNext(epg.value[c.tvgId.toLowerCase()]) : [null, null]);

// ----- Copia guardada en el navegador -----

const chKey = (id: string) => `ch:${id}`;
const epgKey = (id: string) => `epg:${id}`;
const fileKey = (id: string) => `file:${id}`;

async function loadActive() {
  const list = activeList.value;
  if (!list) {
    allChannels.value = [];
    epg.value = {};
    return;
  }
  loading.value = true;
  allChannels.value = ((await get(chKey(list.id))) as Channel[] | undefined) ?? [];
  epg.value = ((await get(epgKey(list.id))) as Record<string, Programme[]> | undefined) ?? {};
  const last = profileData.value.lastGroup[list.id];
  selectedGroup.value = groups.value.find((g) => g.name === last)?.name ?? groups.value[0]?.name ?? null;
  loading.value = false;
}

export function startSession(id: string) {
  profileId.value = id;
  update((d) => ({ ...d, settings: { ...d.settings, lastProfileId: id } }));
  current.value = null;
  void loadActive().then(() => refreshActive(true));
}

function savedWhen(at: number) {
  if (!at) return '';
  const d = new Date(at);
  const hm = d.toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' });
  return new Date().toDateString() === d.toDateString()
    ? ` de hoy a las ${hm}`
    : ` del ${d.toLocaleDateString('es-MX', { day: 'numeric', month: 'long' })}`;
}

export async function refreshActive(quietWhenSame = false) {
  const list = activeList.value;
  if (!list) return;
  const old = allChannels.value;
  try {
    const fileText = list.kind === 'FILE' ? ((await get(fileKey(list.id))) as string | undefined) : undefined;
    const res = await loadList(list, fileText);
    const oldIds = new Set(old.map((c) => c.id));
    const newLive = old.length ? res.channels.filter((c) => c.kind === 'LIVE' && !oldIds.has(c.id)).length : 0;
    await set(chKey(list.id), res.channels);
    if (activeList.value?.id === list.id) allChannels.value = res.channels;
    update((d) => ({
      ...d,
      playlists: d.playlists.map((l) =>
        l.id === list.id
          ? { ...l, updatedAt: Date.now(), channelCount: res.channels.length, epgUrl: l.epgUrl || res.epgUrl, lastError: null }
          : l,
      ),
    }));
    if (!selectedGroup.value || !groups.value.some((g) => g.name === selectedGroup.value)) {
      selectedGroup.value = groups.value[0]?.name ?? null;
    }
    if (newLive > 0) showToast(`${list.name} está al día`, newLive === 1 ? 'Llegó 1 canal nuevo.' : `Llegaron ${newLive} canales nuevos.`);
    else if (!quietWhenSame) showToast(`${list.name} está al día`, 'Sin cambios desde la última vez.');
    void refreshEpg();
  } catch (e) {
    const msg = e instanceof FriendlyError ? e.message : 'No pudimos actualizar la lista.';
    update((d) => ({ ...d, playlists: d.playlists.map((l) => (l.id === list.id ? { ...l, lastError: msg } : l)) }));
    if (old.length) showToast(`No pudimos actualizar ${list.name}`, `Seguimos con la copia guardada${savedWhen(list.updatedAt)}.`, true);
    else showToast(`No pudimos abrir ${list.name}`, msg, true);
  }
}

let epgAt = 0;

async function refreshEpg() {
  const list = activeList.value;
  if (!list) return;
  if (Object.keys(epg.value).length && Date.now() - epgAt < 12 * 3600_000) return;
  const url = list.kind === 'XTREAM' ? xtreamEpg(list) : list.epgUrl;
  if (!url) return;
  const wanted = new Set(allChannels.value.filter((c) => c.kind === 'LIVE' && c.tvgId).map((c) => c.tvgId.toLowerCase()));
  if (!wanted.size) return;
  try {
    const map = parseXmltv(await fetchText(url), wanted);
    await set(epgKey(list.id), map);
    epgAt = Date.now();
    if (activeList.value?.id === list.id) epg.value = map;
  } catch {
    // Muchas guías no se dejan leer desde páginas web: seguimos sin guía.
  }
}

export function selectGroup(name: string) {
  selectedGroup.value = name;
  const list = activeList.value;
  if (list) updateProfileData((pd) => ({ ...pd, lastGroup: { ...pd.lastGroup, [list.id]: name } }));
}

// ----- Perfiles -----

export function createProfile(name: string, avatar: string, kids: boolean): string {
  const id = newId();
  update((d) => ({
    ...d,
    profiles: [...d.profiles, { id, name: name.trim() || 'Yo', avatar, kids, activeListId: null, createdAt: Date.now() }],
  }));
  return id;
}

export function updateProfile(id: string, name: string, avatar: string, kids: boolean) {
  update((d) => ({
    ...d,
    profiles: d.profiles.map((p) => (p.id === id ? { ...p, name: name.trim() || p.name, avatar, kids } : p)),
  }));
}

export function deleteProfile(id: string) {
  const owned = data.value.playlists.filter((l) => l.profileId === id);
  owned.forEach((l) => forgetList(l.id));
  update((d) => {
    const pd = { ...d.profileData };
    delete pd[id];
    return {
      ...d,
      profiles: d.profiles.filter((p) => p.id !== id),
      playlists: d.playlists.filter((l) => l.profileId !== id),
      profileData: pd,
      settings: d.settings.lastProfileId === id ? { ...d.settings, lastProfileId: null } : d.settings,
    };
  });
  if (profileId.value === id) profileId.value = null;
}

export const finishOnboarding = () => update((d) => ({ ...d, settings: { ...d.settings, onboarded: true } }));

export function setSound(on: boolean) {
  update((d) => ({ ...d, settings: { ...d.settings, sound: on } }));
  sfx(on ? 'on' : 'off', true);
}

// ----- Listas -----

export interface NewList {
  name: string;
  kind: ListKind;
  url?: string;
  fileText?: string;
  server?: string;
  user?: string;
  pass?: string;
  epgUrl?: string;
  color?: string;
}

export interface AddResult {
  count: number;
  playable: number;
  live: number;
}

export async function addList(pid: string, input: NewList): Promise<AddResult> {
  const used = data.value.playlists.filter((l) => l.profileId === pid).length;
  const p: Playlist = {
    id: newId(),
    profileId: pid,
    name: input.name.trim() || 'Mi lista',
    kind: input.kind,
    url: (input.url ?? '').trim(),
    server: (input.server ?? '').trim(),
    user: (input.user ?? '').trim(),
    pass: input.pass ?? '',
    epgUrl: (input.epgUrl ?? '').trim(),
    color: input.color ?? LIST_COLORS[used % LIST_COLORS.length],
    addedAt: Date.now(),
    updatedAt: 0,
    channelCount: 0,
  };
  const res = await loadList(p, input.fileText);
  if (input.kind === 'FILE' && input.fileText) await set(fileKey(p.id), input.fileText);
  await set(chKey(p.id), res.channels);
  const stored: Playlist = { ...p, updatedAt: Date.now(), channelCount: res.channels.length, epgUrl: p.epgUrl || res.epgUrl };
  update((d) => ({
    ...d,
    playlists: [...d.playlists, stored],
    profiles: d.profiles.map((x) => (x.id === pid ? { ...x, activeListId: p.id } : x)),
  }));
  if (pid === profileId.value) {
    await loadActive();
    void refreshEpg();
  }
  const live = res.channels.filter((c) => c.kind === 'LIVE');
  return { count: res.channels.length, live: live.length, playable: webPlayable(res.channels.map((c) => c.url)).ok };
}

export function setActiveList(listId: string) {
  const pid = profileId.value;
  if (!pid) return;
  update((d) => ({ ...d, profiles: d.profiles.map((p) => (p.id === pid ? { ...p, activeListId: listId } : p)) }));
  current.value = null;
  void loadActive().then(() => refreshActive(true));
}

function forgetList(listId: string) {
  void del(chKey(listId));
  void del(epgKey(listId));
  void del(fileKey(listId));
}

export function removeList(listId: string) {
  forgetList(listId);
  update((d) => ({
    ...d,
    playlists: d.playlists.filter((l) => l.id !== listId),
    profiles: d.profiles.map((p) => (p.activeListId === listId ? { ...p, activeListId: null } : p)),
  }));
  current.value = null;
  void loadActive();
}

// ----- Recientes y favoritos -----

function updateProfileData(fn: (pd: ProfileData) => ProfileData) {
  const pid = profileId.value;
  if (!pid) return;
  update((d) => ({
    ...d,
    profileData: { ...d.profileData, [pid]: fn(d.profileData[pid] ?? { recents: [], favorites: [], lastGroup: {} }) },
  }));
}

export function recordRecent(c: Channel, positionMs = 0, durationMs = 0) {
  const list = activeList.value;
  if (!list) return;
  const item: RecentItem = { channelId: c.id, listId: list.id, kind: c.kind, at: Date.now(), positionMs, durationMs };
  updateProfileData((pd) => {
    const finished = c.kind !== 'LIVE' && durationMs > 0 && positionMs > durationMs * 0.95;
    const rest = pd.recents.filter((r) => !(r.channelId === c.id && r.listId === list.id));
    return { ...pd, recents: (finished ? rest : [item, ...rest]).slice(0, 60) };
  });
}

export function recents(live: boolean): [Channel, RecentItem][] {
  const list = activeList.value;
  if (!list) return [];
  const out: [Channel, RecentItem][] = [];
  for (const r of profileData.value.recents) {
    if (r.listId !== list.id || (r.kind === 'LIVE') !== live) continue;
    const c = byId.value.get(r.channelId);
    if (c) out.push([c, r]);
    if (out.length >= 12) break;
  }
  return out;
}

export const isFavorite = (id: string) => profileData.value.favorites.includes(id);

export function toggleFavorite(id: string) {
  updateProfileData((pd) => ({
    ...pd,
    favorites: pd.favorites.includes(id) ? pd.favorites.filter((x) => x !== id) : [id, ...pd.favorites],
  }));
}

/** Pone un canal en el reproductor de la página. */
export function play(c: Channel) {
  current.value = c;
  if (c.kind === 'LIVE') recordRecent(c);
}

export function search(q: string): Channel[] {
  const f = fold(q.trim());
  if (f.length < 2) return [];
  const out: Channel[] = [];
  for (const c of channels.value) {
    if (fold(c.name).includes(f)) out.push(c);
    if (out.length >= 80) break;
  }
  return out;
}
