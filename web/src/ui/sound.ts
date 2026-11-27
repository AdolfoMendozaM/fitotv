// Sonidos de marimba de FitoTV. En la web empiezan apagados y se prenden en Perfil.

export type Sfx = 'tick' | 'ok' | 'back' | 'step' | 'on' | 'off' | 'success' | 'fitotv';

let enabled = false;
let ctx: AudioContext | null = null;
const buffers = new Map<Sfx, AudioBuffer>();
let lastTick = 0;

export function setSoundEnabled(on: boolean) {
  enabled = on;
}

const base = import.meta.env.BASE_URL;

async function buffer(name: Sfx): Promise<AudioBuffer | null> {
  if (!ctx) return null;
  const hit = buffers.get(name);
  if (hit) return hit;
  try {
    const res = await fetch(`${base}sfx/sfx_${name}.wav`);
    const buf = await ctx.decodeAudioData(await res.arrayBuffer());
    buffers.set(name, buf);
    return buf;
  } catch {
    return null;
  }
}

/** Los navegadores solo dejan sonar después de un toque o clic. */
export function unlockAudio() {
  if (ctx) {
    if (ctx.state === 'suspended') void ctx.resume();
    return;
  }
  const AC = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!AC) return;
  ctx = new AC();
}

export function sfx(name: Sfx, force = false) {
  if (!enabled && !force) return;
  if (name === 'tick') {
    const now = performance.now();
    if (now - lastTick < 60) return;
    lastTick = now;
  }
  unlockAudio();
  const c = ctx;
  if (!c) return;
  void buffer(name).then((buf) => {
    if (!buf) return;
    const src = c.createBufferSource();
    const gain = c.createGain();
    gain.gain.value = name === 'tick' ? 0.35 : name === 'fitotv' ? 0.9 : 0.6;
    src.buffer = buf;
    src.connect(gain).connect(c.destination);
    src.start();
  });
}
