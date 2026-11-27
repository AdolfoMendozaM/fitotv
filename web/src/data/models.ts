// Los mismos datos que guarda la app de tele, para que todo se llame igual.

export type ListKind = 'URL' | 'FILE' | 'XTREAM';
export type ContentKind = 'LIVE' | 'MOVIE' | 'SERIES';

export const AVATARS = [
  { key: 'nina', label: 'Niña con trenzas y moños' },
  { key: 'nino', label: 'Niño con sudadera roja' },
  { key: 'chava', label: 'Chava con audífonos' },
  { key: 'chavo', label: 'Chavo con gorra' },
  { key: 'mama', label: 'Mamá con trenzas y aretes' },
  { key: 'papa', label: 'Papá con barba y guayabera' },
  { key: 'abuela', label: 'Abuelita con lentes y rebozo' },
  { key: 'abuelo', label: 'Abuelito con sombrero' },
  { key: 'perrito', label: 'Perrito con paliacate' },
  { key: 'gatito', label: 'Gatito con cascabel' },
  { key: 'ajolote', label: 'Ajolote' },
  { key: 'xolo', label: 'Xoloitzcuintle' },
  { key: 'jaguar', label: 'Jaguarcito' },
] as const;

/** Los perfiles hechos con los avatares anteriores pasan al más parecido. */
const LEGACY_AVATARS: Record<string, string> = { audifonos: 'chava', pelo_largo: 'mama', gorra: 'chavo', chongo: 'abuela', coletas: 'nina' };

export function avatarKey(k?: string | null): string {
  if (k && AVATARS.some((a) => a.key === k)) return k;
  return (k && LEGACY_AVATARS[k]) || AVATARS[0].key;
}

export const LIST_COLORS = ['#3DE0BE', '#2350E0', '#B79CFF', '#FFC53D', '#FF6FA8'];

export interface Profile {
  id: string;
  name: string;
  avatar: string;
  kids: boolean;
  activeListId?: string | null;
  createdAt: number;
}

export interface Playlist {
  id: string;
  profileId: string;
  name: string;
  kind: ListKind;
  url: string;
  server: string;
  user: string;
  pass: string;
  epgUrl: string;
  color: string;
  addedAt: number;
  updatedAt: number;
  channelCount: number;
  lastError?: string | null;
}

export interface Channel {
  id: string;
  num: number;
  name: string;
  url: string;
  logo: string;
  group: string;
  tvgId: string;
  kind: ContentKind;
}

export interface RecentItem {
  channelId: string;
  listId: string;
  kind: ContentKind;
  at: number;
  positionMs: number;
  durationMs: number;
}

export interface ProfileData {
  recents: RecentItem[];
  favorites: string[];
  lastGroup: Record<string, string>;
}

export interface Settings {
  sound: boolean;
  lastProfileId?: string | null;
  onboarded: boolean;
}

export interface AppData {
  profiles: Profile[];
  playlists: Playlist[];
  profileData: Record<string, ProfileData>;
  settings: Settings;
}

export interface Programme {
  start: number;
  stop: number;
  title: string;
  desc: string;
}

export const emptyData = (): AppData => ({
  profiles: [],
  playlists: [],
  profileData: {},
  // En la web los sonidos empiezan apagados.
  settings: { sound: false, lastProfileId: null, onboarded: false },
});

export const newId = () => Math.random().toString(36).slice(2, 8) + Date.now().toString(36).slice(-6);
