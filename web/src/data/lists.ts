import { parseM3u, type ParseResult } from './m3u';
import type { Playlist } from './models';

/** Error con un mensaje claro para mostrar tal cual. */
export class FriendlyError extends Error {}

export function normalizeServer(server: string): string {
  let s = server.trim().replace(/\/+$/, '');
  if (!/^https?:\/\//i.test(s)) s = 'http://' + s;
  return s;
}

const enc = encodeURIComponent;

// En la web pedimos los canales en HLS (.m3u8), que es lo que los navegadores reproducen.
export const xtreamM3u = (p: Pick<Playlist, 'server' | 'user' | 'pass'>) =>
  `${normalizeServer(p.server)}/get.php?username=${enc(p.user)}&password=${enc(p.pass)}&type=m3u_plus&output=m3u8`;

export const xtreamEpg = (p: Pick<Playlist, 'server' | 'user' | 'pass'>) =>
  `${normalizeServer(p.server)}/xmltv.php?username=${enc(p.user)}&password=${enc(p.pass)}`;

export const isSecurePage = () => typeof location !== 'undefined' && location.protocol === 'https:';

export const isHttp = (url: string) => /^http:\/\//i.test(url.trim());

function httpMessage(code: number): string {
  if (code === 401 || code === 403) return 'El servidor no dio permiso: revisa el usuario y la contraseña, o si la cuenta venció.';
  if (code === 404) return 'No encontramos la lista en esa dirección.';
  if (code >= 500) return 'El servidor de la lista no está respondiendo. Intenta más tarde.';
  return `El servidor respondió con un error (${code}).`;
}

/** Descarga un texto (lista o guía), descomprimiendo si viene en .gz. */
export async function fetchText(url: string, signal?: AbortSignal): Promise<string> {
  if (isSecurePage() && isHttp(url)) {
    throw new FriendlyError(
      'Esta lista usa una dirección sin cifrar (http://) y los navegadores no dejan abrirla desde una página segura. ' +
        'Prueba la misma dirección con https://, súbela como archivo o ábrela en la app de tele.',
    );
  }
  let res: Response;
  try {
    res = await fetch(url, { signal, cache: 'no-store' });
  } catch (e) {
    if ((e as Error).name === 'AbortError') throw e;
    throw new FriendlyError(
      'El navegador no pudo abrir esta lista. Puede que el proveedor no deje leerla desde páginas web, o que no haya internet. ' +
        'Puedes subirla como archivo o usar la app de tele.',
    );
  }
  if (!res.ok) throw new FriendlyError(httpMessage(res.status));
  const buf = new Uint8Array(await res.arrayBuffer());
  if (buf[0] === 0x1f && buf[1] === 0x8b && typeof DecompressionStream !== 'undefined') {
    const stream = new Blob([buf]).stream().pipeThrough(new DecompressionStream('gzip'));
    return await new Response(stream).text();
  }
  return new TextDecoder('utf-8').decode(buf);
}

/** Lee la lista según cómo se agregó. Para archivos, el texto viene de quien llama. */
export async function loadList(p: Playlist, fileText?: string): Promise<ParseResult> {
  let text: string;
  if (p.kind === 'FILE') {
    if (!fileText) throw new FriendlyError('Vuelve a subir el archivo de la lista para actualizarla.');
    text = fileText;
  } else if (p.kind === 'XTREAM') {
    text = await fetchText(xtreamM3u(p));
  } else {
    text = await fetchText(p.url);
  }
  if (!/#EXT/i.test(text) && !/https?:\/\//i.test(text)) {
    throw new FriendlyError('Eso no parece una lista M3U. Revisa el enlace o el archivo.');
  }
  const result = parseM3u(text);
  if (!result.channels.length) throw new FriendlyError('La lista llegó sin canales.');
  return result;
}

/** Cuántos canales se pueden ver en el navegador (los http no, en una página segura). */
export function webPlayable(urls: string[]): { ok: number; total: number } {
  const total = urls.length;
  if (!isSecurePage()) return { ok: total, total };
  const ok = urls.filter((u) => !isHttp(u)).length;
  return { ok, total };
}

export { type ParseResult };
