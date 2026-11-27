// Los mismos datos que guarda la app de tele, para que todo se llame igual.

export type ListKind = 'URL' | 'FILE' | 'XTREAM';
export type ContentKind = 'LIVE' | 'MOVIE' | 'SERIES';

export const AVATARS = [
  { key: 'audifonos', label: 'Avatar con audífonos' },
  { key: 'pelo_largo', label: 'Avatar de pelo largo con aretes' },
  { key: 'gorra', label: 'Avatar con gorra' },
  { key: 'chongo', label: 'Avatar con chongo y lentes' },
  { key: 'coletas', label: 'Avatar infantil con coletas' },
] as const;

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
