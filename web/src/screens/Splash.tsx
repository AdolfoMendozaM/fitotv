import { signal } from '@preact/signals';
import type { Ref } from 'preact';
import { useEffect, useRef, useState } from 'preact/hooks';
import { beginReveal, data, revealing, startSession, type Screen } from '../data/store';
import { Logo, Progress } from '../ui/kit';
import { sfx } from '../ui/sound';

/** Triángulo del play del logo, en coordenadas de la pantalla. */
export const revealClip = signal<string | null>(null);
const origin = signal<{ x: number; y: number } | null>(null);

// Puntos del triángulo dentro del logo (proporciones del ícono de FitoTV).
const TRI = [
  [0.422, 0.4065],
  [0.6827, 0.5423],
  [0.422, 0.694],
];

function SplashArt({ fill, textOut, label, logoRef, zoom }: { fill: number; textOut: boolean; label: string; logoRef?: Ref<HTMLDivElement>; zoom?: boolean }) {
  const o = origin.value;
  return (
    <div
      class="splash"
      style={{
        transformOrigin: o ? `${o.x}px ${o.y}px` : '50% 40%',
        transform: zoom ? 'scale(9)' : 'none',
        opacity: zoom ? 0 : 1,
      }}
    >
      <svg class="waves" viewBox="0 0 240 64" aria-hidden="true">
        <path d="M78 60 Q120 22 162 60" fill="none" stroke="var(--menta)" stroke-width="5" stroke-linecap="round" />
        <path d="M52 62 Q120 -4 188 62" fill="none" stroke="var(--menta)" stroke-width="5" stroke-linecap="round" opacity=".7" />
      </svg>
      <div ref={logoRef} class="logo">
        <Logo size={168} />
      </div>
      <div class="col fadeable" style={{ alignItems: 'center', opacity: textOut ? 0 : 1 }}>
        <span class="word">FitoTV</span>
        <span class="bar">
          <Progress value={fill} width={240} />
        </span>
        <span class="label">{label}</span>
      </div>
      <span class="foot fadeable" style={{ opacity: textOut ? 0 : 1 }}>
        Tu tele, tus listas.
      </span>
    </div>
  );
}

export function SplashScreen() {
  const returning = data.value.profiles.length > 0;
  const [fill, setFill] = useState(0);
  const [textOut, setTextOut] = useState(false);
  const logo = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const revealAt = returning ? 1700 : 2400;
    const t: number[] = [];
    t.push(window.setTimeout(() => setFill(1), 60));
    t.push(window.setTimeout(() => sfx('fitotv'), revealAt - 980));
    t.push(window.setTimeout(() => setTextOut(true), revealAt - 400));
    t.push(
      window.setTimeout(() => {
        const r = logo.current?.getBoundingClientRect();
        if (r) {
          const pts = TRI.map(([fx, fy]) => [r.left + fx * r.width, r.top + fy * r.height]);
          const cx = (pts[0][0] + pts[1][0] + pts[2][0]) / 3;
          const cy = (pts[0][1] + pts[1][1] + pts[2][1]) / 3;
          origin.value = { x: cx, y: cy };
          const k = 60;
          const big = pts.map(([x, y]) => [cx + (x - cx) * k, cy + (y - cy) * k]);
          const poly = (p: number[][]) => `polygon(${p.map(([x, y]) => `${x.toFixed(1)}px ${y.toFixed(1)}px`).join(', ')})`;
          revealClip.value = poly(pts);
          requestAnimationFrame(() => requestAnimationFrame(() => (revealClip.value = poly(big))));
        }
        const profiles = data.value.profiles;
        let next: Screen;
        if (!profiles.length) next = { name: 'tutorial' };
        else if (profiles.length === 1) {
          startSession(profiles[0].id);
          next = { name: 'home' };
        } else next = { name: 'profiles' };
        beginReveal(next);
        window.setTimeout(() => {
          revealing.value = false;
          revealClip.value = null;
        }, 1350);
      }, revealAt - 70),
    );
    return () => t.forEach(clearTimeout);
  }, []);

  return <SplashArt fill={fill} textOut={textOut} label={returning ? 'Hola de nuevo' : 'Preparando todo'} logoRef={logo} />;
}

/** La pantalla de carga que se queda abajo y se agranda mientras aparece la siguiente. */
export function RevealLayer() {
  const [zoom, setZoom] = useState(false);
  useEffect(() => {
    const h = requestAnimationFrame(() => requestAnimationFrame(() => setZoom(true)));
    return () => cancelAnimationFrame(h);
  }, []);
  return <SplashArt fill={1} textOut label="" zoom={zoom} />;
}
