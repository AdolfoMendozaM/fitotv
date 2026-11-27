import type { Channel } from '../data/models';
import { isHttp, isSecurePage } from '../data/lists';

/** Motivo por el que un canal no se puede ver en el navegador, en palabras claras. */
export type PlayProblem = 'http' | 'format' | 'blocked' | 'offline' | 'gone';

export const problemText: Record<PlayProblem, string> = {
  http: 'Este canal usa una conexión sin cifrar (http) y el navegador no lo deja reproducir aquí. Se ve en la app de tele.',
  format: 'Este navegador no puede reproducir el formato de este canal. Se ve en la app de tele.',
  blocked: 'El canal no deja reproducirse desde páginas web. Se ve en la app de tele.',
  offline: 'No pudimos conectarnos con el canal. Revisa tu internet o intenta en un momento.',
  gone: 'El canal no respondió. Puede estar caído o la cuenta venció.',
};

export interface Engine {
  destroy(): void;
}

const path = (url: string) => url.split('?')[0].toLowerCase();
const looksHls = (url: string) => {
  const p = path(url);
  return p.endsWith('.m3u8') || p.endsWith('.m3u') || p.includes('/hls/');
};
const looksTs = (url: string) => {
  const p = path(url);
  // Las listas Xtream mandan canales en vivo sin extensión: son MPEG-TS.
  return p.endsWith('.ts') || (/\/live\/[^/]+\/[^/]+\/\d+$/.test(p));
};

/**
 * Conecta un canal al <video>. Llama a onProblem si no se puede ver.
 * HLS: nativo en Safari/iPhone, hls.js en los demás. MPEG-TS: mpegts.js donde exista.
 */
export async function attach(video: HTMLVideoElement, ch: Channel, onProblem: (p: PlayProblem) => void, startAt = 0): Promise<Engine> {
  const url = ch.url;
  let destroyed = false;
  const cleanups: (() => void)[] = [];
  const engine: Engine = {
    destroy() {
      destroyed = true;
      cleanups.forEach((f) => {
        try {
          f();
        } catch {
          /* ya estaba cerrado */
        }
      });
      video.removeAttribute('src');
      video.load();
    },
  };

  if (isSecurePage() && isHttp(url)) {
    onProblem('http');
    return engine;
  }

  const nativeHls = video.canPlayType('application/vnd.apple.mpegurl') !== '';
  // En iPhone y iPad usamos el reproductor de Safari (así AirPlay funciona directo).
  const apple = /iP(hone|ad|od)/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  const onVideoError = () => {
    if (destroyed) return;
    const code = video.error?.code;
    if (code === MediaError.MEDIA_ERR_NETWORK) onProblem('offline');
    else if (code === MediaError.MEDIA_ERR_DECODE) onProblem('format');
    // Con el reproductor del navegador no se distingue un canal caído de uno que no deja verse en la web.
    else onProblem(looksHls(url) ? 'blocked' : 'format');
  };

  const playNative = () => {
    video.addEventListener('error', onVideoError);
    cleanups.push(() => video.removeEventListener('error', onVideoError));
    video.src = url;
    if (startAt > 0) video.currentTime = startAt / 1000;
    void video.play().catch(() => undefined);
  };

  if (looksHls(url)) {
    if (nativeHls && apple) {
      playNative();
      return engine;
    }
    const { default: Hls } = await import('hls.js');
    if (destroyed) return engine;
    if (!Hls.isSupported()) {
      if (nativeHls) playNative();
      else onProblem('format');
      return engine;
    }
    const hls = new Hls({ lowLatencyMode: false, backBufferLength: 30, startPosition: startAt > 0 ? startAt / 1000 : -1 });
    cleanups.push(() => hls.destroy());
    let recovered = false;
    hls.on(Hls.Events.ERROR, (_e, d) => {
      if (!d.fatal || destroyed) return;
      if (d.type === Hls.ErrorTypes.MEDIA_ERROR && !recovered) {
        recovered = true;
        hls.recoverMediaError();
        return;
      }
      const status = (d.response as { code?: number } | undefined)?.code ?? 0;
      if (d.type === Hls.ErrorTypes.NETWORK_ERROR) onProblem(status === 0 ? 'blocked' : 'gone');
      else onProblem('format');
    });
    hls.loadSource(url);
    hls.attachMedia(video);
    hls.on(Hls.Events.MANIFEST_PARSED, () => void video.play().catch(() => undefined));
    return engine;
  }

  if (looksTs(url)) {
    const { default: mpegts } = await import('mpegts.js');
    if (destroyed) return engine;
    if (!mpegts.isSupported()) {
      onProblem('format');
      return engine;
    }
    const p = mpegts.createPlayer({ type: 'mpegts', isLive: ch.kind === 'LIVE', url }, { enableWorker: false, lazyLoad: false });
    cleanups.push(() => {
      p.pause();
      p.unload();
      p.detachMediaElement();
      p.destroy();
    });
    p.on(mpegts.Events.ERROR, (type: string) => {
      if (destroyed) return;
      onProblem(type === mpegts.ErrorTypes.NETWORK_ERROR ? 'blocked' : 'format');
    });
    p.attachMediaElement(video);
    p.load();
    void Promise.resolve(p.play()).catch(() => undefined);
    return engine;
  }

  playNative();
  return engine;
}

// ----- Transmitir a la tele (AirPlay en Safari, Chromecast en Chrome) -----

type AirPlayVideo = HTMLVideoElement & { webkitShowPlaybackTargetPicker?: () => void };
type RemoteVideo = HTMLVideoElement & { remote?: { prompt(): Promise<void>; state: string } };

export function castOptions(video: HTMLVideoElement | null) {
  const v = video as (AirPlayVideo & RemoteVideo) | null;
  return {
    airplay: !!v?.webkitShowPlaybackTargetPicker || typeof (window as unknown as { WebKitPlaybackTargetAvailabilityEvent?: unknown }).WebKitPlaybackTargetAvailabilityEvent !== 'undefined',
    remote: !!v?.remote,
  };
}

export async function castTo(video: HTMLVideoElement, kind: 'airplay' | 'remote'): Promise<boolean> {
  const v = video as AirPlayVideo & RemoteVideo;
  try {
    if (kind === 'airplay' && v.webkitShowPlaybackTargetPicker) {
      v.webkitShowPlaybackTargetPicker();
      return true;
    }
    if (kind === 'remote' && v.remote) {
      await v.remote.prompt();
      return true;
    }
  } catch {
    /* la persona cerró la ventana o no hay teles cerca */
  }
  return false;
}
