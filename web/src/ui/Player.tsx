import { useEffect, useRef, useState } from 'preact/hooks';
import type { Channel } from '../data/models';
import { recordRecent, profileData } from '../data/store';
import { attach, castOptions, castTo, problemText, type Engine, type PlayProblem } from '../player/engine';
import { I, Icon, Spinner, typeLabel } from './kit';

interface Props {
  channel: Channel | null;
  /** true si la persona eligió el canal (el navegador deja sonar); false = empieza sin sonido. */
  withSound: boolean;
  phone?: boolean;
  onCast: (video: HTMLVideoElement) => void;
}

const clock = (s: number) => {
  if (!Number.isFinite(s)) return '--:--';
  const t = Math.max(0, Math.floor(s));
  const h = Math.floor(t / 3600);
  const m = Math.floor((t % 3600) / 60);
  const sec = t % 60;
  return h ? `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}` : `${m}:${String(sec).padStart(2, '0')}`;
};

export function Player({ channel, withSound, phone, onCast }: Props) {
  const box = useRef<HTMLDivElement>(null);
  const video = useRef<HTMLVideoElement>(null);
  const engine = useRef<Engine | null>(null);
  const [problem, setProblem] = useState<PlayProblem | null>(null);
  const [waiting, setWaiting] = useState(false);
  const [playing, setPlaying] = useState(false);
  const [muted, setMuted] = useState(true);
  const [volume, setVolume] = useState(1);
  const [idle, setIdle] = useState(false);
  const [time, setTime] = useState({ pos: 0, dur: 0 });
  const isVod = !!channel && channel.kind !== 'LIVE';

  const saveProgress = () => {
    const v = video.current;
    if (!v || !channel || channel.kind === 'LIVE') return;
    const dur = v.duration;
    if (Number.isFinite(dur) && dur > 0 && v.currentTime > 5) recordRecent(channel, v.currentTime * 1000, dur * 1000);
  };

  useEffect(() => {
    const v = video.current;
    if (!v) return;
    setProblem(null);
    setTime({ pos: 0, dur: 0 });
    if (!channel) return;
    const startMuted = !withSound;
    v.muted = startMuted;
    setMuted(startMuted);
    setWaiting(true);
    const resume = channel.kind !== 'LIVE' ? profileData.value.recents.find((r) => r.channelId === channel.id)?.positionMs ?? 0 : 0;
    let cancelled = false;
    void attach(v, channel, (p) => {
      if (!cancelled) {
        setProblem(p);
        setWaiting(false);
      }
    }, resume > 30_000 ? resume - 3000 : 0).then((e) => {
      if (cancelled) e.destroy();
      else engine.current = e;
    });
    const save = setInterval(saveProgress, 15_000);
    return () => {
      cancelled = true;
      clearInterval(save);
      saveProgress();
      engine.current?.destroy();
      engine.current = null;
    };
  }, [channel?.id]);

  useEffect(() => {
    const v = video.current;
    if (!v) return;
    const on = (e: Event) => {
      if (e.type === 'waiting') setWaiting(true);
      if (e.type === 'playing' || e.type === 'canplay') setWaiting(false);
      if (e.type === 'play' || e.type === 'playing') setPlaying(true);
      if (e.type === 'pause') setPlaying(false);
      if (e.type === 'timeupdate' || e.type === 'durationchange') setTime({ pos: v.currentTime, dur: v.duration });
      if (e.type === 'volumechange') setMuted(v.muted);
    };
    const evs = ['waiting', 'playing', 'canplay', 'play', 'pause', 'timeupdate', 'durationchange', 'volumechange'];
    evs.forEach((n) => v.addEventListener(n, on));
    return () => evs.forEach((n) => v.removeEventListener(n, on));
  }, []);

  // Los controles se esconden solos para no tapar.
  useEffect(() => {
    if (!playing) {
      setIdle(false);
      return;
    }
    const h = setTimeout(() => setIdle(true), 3000);
    return () => clearTimeout(h);
  }, [playing, idle]);

  const wake = () => setIdle(false);
  const toggle = () => {
    const v = video.current;
    if (!v) return;
    if (v.paused) void v.play().catch(() => undefined);
    else v.pause();
  };
  const unmute = () => {
    const v = video.current;
    if (!v) return;
    v.muted = false;
    if (v.volume === 0) v.volume = 1;
    void v.play().catch(() => undefined);
  };
  const fullscreen = () => {
    const el = box.current as (HTMLDivElement & { webkitRequestFullscreen?: () => void }) | null;
    const v = video.current as (HTMLVideoElement & { webkitEnterFullscreen?: () => void }) | null;
    if (document.fullscreenElement) void document.exitFullscreen();
    else if (el?.requestFullscreen) void el.requestFullscreen().catch(() => v?.webkitEnterFullscreen?.());
    else v?.webkitEnterFullscreen?.();
  };
  const pip = () => {
    const v = video.current;
    if (!v) return;
    if (document.pictureInPictureElement) void document.exitPictureInPicture();
    else void v.requestPictureInPicture?.().catch(() => undefined);
  };
  const canPip = typeof document !== 'undefined' && 'pictureInPictureEnabled' in document && document.pictureInPictureEnabled;
  const cast = castOptions(video.current);

  return (
    <div ref={box} class={`player${idle ? ' idle' : ''}`} onMouseMove={wake} onTouchStart={wake}>
      <video ref={video} playsInline autoPlay x-webkit-airplay="allow" onClick={phone ? wake : toggle} />
      {!channel && (
        <div class="center">
          <Icon d={I.play} size={40} color="var(--line3)" />
          <span class="muted">Elige un canal para empezar</span>
        </div>
      )}
      {channel && waiting && !problem && (
        <div class="center">
          <Spinner size={44} />
        </div>
      )}
      {problem && (
        <div class="center problem">
          <span style={{ width: 56, height: 56, borderRadius: 28, background: 'var(--ambar)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Icon d={I.warn} size={30} color="var(--noche)" stroke={2.8} />
          </span>
          <strong class="display" style={{ fontSize: phone ? 18 : 22, maxWidth: 520, lineHeight: 1.25 }}>
            {problemText[problem]}
          </strong>
        </div>
      )}
      {channel && !problem && (
        <div class="overlay">
          <div class="top">
            {muted && playing ? (
              <button class="pill dark unmute" onClick={unmute}>
                <Icon d={I.mute} size={16} /> Toca para verlo con sonido
              </button>
            ) : (
              <span class="pill dark">
                {isVod ? typeLabel(channel.kind) : (
                  <>
                    <span class="dot" /> En vivo
                  </>
                )}
              </span>
            )}
            {(cast.airplay || cast.remote) && phone && (
              <button class="icon-btn" aria-label="Transmitir a la tele" style={{ background: 'rgba(10,23,71,.85)' }} onClick={() => video.current && onCast(video.current)}>
                <Icon d={I.cast} size={20} />
              </button>
            )}
          </div>
          <div class="controls">
            <button class="ctl" aria-label={playing ? 'Pausa' : 'Reproducir'} onClick={toggle}>
              <Icon d={playing ? I.pause : I.play} size={20} />
            </button>
            <button class="ctl" aria-label={muted ? 'Activar sonido' : 'Silenciar'} onClick={() => (muted ? unmute() : video.current && (video.current.muted = true))}>
              <Icon d={muted ? I.mute : I.sound} size={22} />
            </button>
            {!phone && (
              <input
                class="vol"
                type="range"
                min={0}
                max={1}
                step={0.05}
                value={muted ? 0 : volume}
                aria-label="Volumen"
                onInput={(e) => {
                  const v = video.current;
                  const val = parseFloat((e.target as HTMLInputElement).value);
                  setVolume(val);
                  if (v) {
                    v.volume = val;
                    v.muted = val === 0;
                  }
                }}
              />
            )}
            {isVod && Number.isFinite(time.dur) && time.dur > 0 ? (
              <div class="row grow" style={{ gap: 10, fontSize: 13 }}>
                <span>{clock(time.pos)}</span>
                <input
                  class="grow"
                  type="range"
                  min={0}
                  max={time.dur}
                  step={1}
                  value={time.pos}
                  aria-label="Avance"
                  style={{ accentColor: 'var(--menta)' }}
                  onInput={(e) => video.current && (video.current.currentTime = parseFloat((e.target as HTMLInputElement).value))}
                />
                <span>{clock(time.dur)}</span>
              </div>
            ) : (
              <div class="grow" />
            )}
            {(cast.airplay || cast.remote) && !phone && (
              <button class="ctl" aria-label="Transmitir a la tele" onClick={() => video.current && onCast(video.current)}>
                <Icon d={I.cast} size={20} />
              </button>
            )}
            {canPip && (
              <button class="ctl" aria-label="Ventana flotante" onClick={pip}>
                <Icon d={I.pip} size={20} />
              </button>
            )}
            <button class="ctl" aria-label="Pantalla completa" onClick={fullscreen}>
              <Icon d={I.full} size={20} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

/** Plan B cuando el canal no se puede mandar directo: duplicar la pantalla. */
function mirrorTip(): string {
  const ua = navigator.userAgent;
  if (/Android/i.test(ua)) return 'Este canal no se puede mandar directo a la tele. Usa “Transmitir pantalla” en el menú rápido de tu celular (desliza desde arriba).';
  if (/iP(hone|ad|od)/.test(ua)) return 'Este canal no se puede mandar directo a la tele. Usa “Duplicar pantalla” en el Centro de control.';
  return 'Este canal no se puede mandar directo a la tele. En Chrome abre el menú ⋮ y elige “Transmitir…” para mandar la pestaña.';
}

/** Hoja para transmitir: AirPlay (Safari) o Chromecast (Chrome). La lista de teles la da el navegador. */
export function CastSheet({ video, channel, onClose }: { video: HTMLVideoElement; channel: Channel | null; onClose: () => void }) {
  const opts = castOptions(video);
  const [note, setNote] = useState('');
  const go = async (k: 'airplay' | 'remote') => {
    const r = await castTo(video, k);
    if (r === 'ok') onClose();
    else if (r === 'none') setNote('No encontramos teles con Chromecast en tu Wi-Fi. Revisa que la tele esté prendida y en la misma red.');
    else if (r === 'unsupported') setNote(mirrorTip());
  };
  return (
    <div class="sheet-back" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div class="sheet" role="dialog" aria-label="Transmitir a una tele">
        <div class="grab" />
        <div class="row" style={{ gap: 12 }}>
          <Icon d={I.cast} size={24} />
          <strong class="display" style={{ fontSize: 24 }}>
            Transmitir a una tele
          </strong>
        </div>
        <p class="muted2" style={{ margin: '10px 0 18px', lineHeight: 1.5 }}>
          {channel ? `${channel.name} se va a ver en la tele que elijas. ` : ''}Tu celular o tu compu quedan como control.
        </p>
        <div class="col" style={{ gap: 10 }}>
          {opts.airplay && (
            <button class="option-row" onClick={() => go('airplay')}>
              <Icon d={I.airplay} size={24} />
              <span class="col grow">
                <strong>AirPlay</strong>
                <span class="muted" style={{ fontSize: 14 }}>Apple TV y teles compatibles</span>
              </span>
              <Icon d={I.chevronRight} size={20} color="var(--muted)" />
            </button>
          )}
          {opts.remote && (
            <button class="option-row" onClick={() => go('remote')}>
              <Icon d={I.cast} size={24} />
              <span class="col grow">
                <strong>Chromecast</strong>
                <span class="muted" style={{ fontSize: 14 }}>Google TV y teles con Chromecast</span>
              </span>
              <Icon d={I.chevronRight} size={20} color="var(--muted)" />
            </button>
          )}
          {!opts.airplay && !opts.remote && (
            <p class="muted" style={{ lineHeight: 1.5 }}>
              Este navegador no puede transmitir. Usa Safari para AirPlay o Chrome para Chromecast.
            </p>
          )}
        </div>
        {note ? (
          <p class="hint" role="status" style={{ margin: '16px 0', color: 'var(--text)' }}>
            {note}
          </p>
        ) : (
          <p class="hint" style={{ margin: '16px 0' }}>
            ¿No ves tu tele? Revisa que esté prendida y conectada al mismo Wi-Fi.
          </p>
        )}
        <button class="btn btn-secondary btn-block" onClick={onClose}>
          Cancelar
        </button>
      </div>
    </div>
  );
}
