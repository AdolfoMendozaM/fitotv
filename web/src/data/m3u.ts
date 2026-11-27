import type { Channel, ContentKind } from './models';

// Igual que el lector de la app de tele (M3uParser.kt), con los mismos identificadores.

const attrRe = /([A-Za-z0-9_-]+)\s*=\s*"([^"]*)"/g;
const movieExt = ['.mp4', '.mkv', '.avi', '.mov', '.m4v', '.wmv', '.webm'];

function javaHash(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  return h;
}

function stableId(url: string, name: string): string {
  return (javaHash(url) >>> 0).toString(36) + (javaHash(name) >>> 0).toString(36);
}

function attributes(line: string): Record<string, string> {
  const out: Record<string, string> = {};
  for (const m of line.matchAll(attrRe)) out[m[1].toLowerCase()] = m[2];
  return out;
}

function displayName(line: string): string | null {
  let q = false;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (c === '"') q = !q;
    else if (c === ',' && !q) return line.slice(i + 1).trim() || null;
  }
  return null;
}

function looksLikeUrl(line: string) {
  return /^(https?|rtmp|rtsp|udp|rtp):\/\//i.test(line);
}

export function classify(url: string, group: string): ContentKind {
  const path = url.split('?')[0].toLowerCase();
  const g = group.toLowerCase();
  if (path.includes('/series/')) return 'SERIES';
  if (path.includes('/movie/') || path.includes('/movies/')) return 'MOVIE';
  if (movieExt.some((e) => path.endsWith(e))) return g.includes('serie') ? 'SERIES' : 'MOVIE';
  return 'LIVE';
}

export interface ParseResult {
  channels: Channel[];
  epgUrl: string;
}

export function parseM3u(text: string): ParseResult {
  let epgUrl = '';
  let pendingName: string | null = null;
  let pendingAttrs: Record<string, string> = {};
  let pendingGroup = '';
  const out: Channel[] = [];
  const seen = new Map<string, number>();
  let liveNum = 0;

  const add = (url: string) => {
    const name = (pendingName ?? pendingAttrs['tvg-name'] ?? url.split('/').pop() ?? '').trim() || 'Canal';
    const group = (pendingAttrs['group-title'] ?? pendingGroup).trim() || 'Sin grupo';
    const kind = classify(url, group);
    let num = 0;
    if (kind === 'LIVE') {
      liveNum += 1;
      const chno = parseInt((pendingAttrs['tvg-chno'] ?? '').trim(), 10);
      num = Number.isFinite(chno) ? chno : liveNum;
    }
    const base = stableId(url, name);
    const dup = seen.get(base) ?? 0;
    seen.set(base, dup + 1);
    out.push({
      id: dup === 0 ? base : `${base}-${dup}`,
      num,
      name,
      url,
      logo: (pendingAttrs['tvg-logo'] ?? '').trim(),
      group,
      tvgId: (pendingAttrs['tvg-id'] ?? '').trim(),
      kind,
    });
    pendingName = null;
    pendingAttrs = {};
    pendingGroup = '';
  };

  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim().replace(/^﻿/, '');
    if (!line) continue;
    if (/^#EXTM3U/i.test(line)) {
      const a = attributes(line);
      const tvg = a['x-tvg-url'] ?? a['url-tvg'] ?? a['tvg-url'] ?? '';
      if (tvg.trim()) epgUrl = tvg.split(',')[0].trim();
    } else if (/^#EXTINF/i.test(line)) {
      pendingAttrs = attributes(line);
      pendingName = displayName(line);
    } else if (/^#EXTGRP:/i.test(line)) {
      pendingGroup = line.slice(line.indexOf(':') + 1).trim();
    } else if (line.startsWith('#')) {
      continue;
    } else if (looksLikeUrl(line)) {
      add(line);
    }
  }
  return { channels: out, epgUrl };
}
