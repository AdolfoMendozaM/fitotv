import type { ComponentChildren } from 'preact';
import { useEffect, useRef, useState } from 'preact/hooks';
import type { Channel, RecentItem } from '../data/models';
import {
  activeList,
  channels,
  channelsOf,
  current,
  data,
  FAVORITES,
  go,
  groups,
  isFavorite,
  loading,
  openTab,
  play,
  profile,
  profileData,
  programmeFor,
  recents,
  screen,
  search,
  selectGroup,
  selectedGroup,
  toggleFavorite,
  epg,
} from '../data/store';
import { attach, type Engine } from '../player/engine';
import { CastSheet, Player } from '../ui/Player';
import {
  Avatar,
  Brand,
  ChannelLogo,
  groupIcon,
  hhmm,
  I,
  Icon,
  Logo,
  minutesLeft,
  Poster,
  Progress,
  progressOf,
  remaining,
  Spinner,
  typeLabel,
  useIsPhone,
  useNow,
  visitedAgo,
  vodFraction,
} from '../ui/kit';
import { sfx } from '../ui/sound';

// ---------------------------------------------------------------------------
// Piezas compartidas
// ---------------------------------------------------------------------------

function useWatch() {
  // Lo que se ve: si no han elegido nada, el primer canal de lo reciente o del grupo.
  const [withSound, setWithSound] = useState(false);
  const fallback =
    recents(true)[0]?.[0] ?? channelsOf(selectedGroup.value)[0] ?? null;
  const ch = current.value ?? fallback;
  const choose = (c: Channel) => {
    sfx('ok');
    setWithSound(true);
    play(c);
  };
  return { ch, withSound: withSound && current.value != null, choose };
}

function groupTitle() {
  const g = selectedGroup.value;
  return g === FAVORITES ? 'Favoritos' : g ?? 'Canales';
}

function VodCard({ ch, r, onPick }: { ch: Channel; r: RecentItem; onPick: () => void }) {
  return (
    <button class="vod-card" onClick={onPick}>
      <div class="art" style={{ background: 'var(--s3)' }}>
        <Poster name={ch.name} logo={ch.logo} size={34} />
        <span class="pill dark tag">{typeLabel(ch.kind)}</span>
        <span class="bar">
          <span style={{ width: `${Math.round(vodFraction(r) * 100)}%` }} />
        </span>
      </div>
      <div class="col" style={{ padding: '10px 12px 12px', gap: 3 }}>
        <strong class="ellipsis" style={{ fontSize: 15 }}>
          {ch.name}
        </strong>
        <span class="muted" style={{ fontSize: 13 }}>
          {remaining(r)}
        </span>
      </div>
    </button>
  );
}

function RecentCard({ ch, r, onPick }: { ch: Channel; r: RecentItem; onPick: () => void }) {
  const [now] = programmeFor(ch);
  return (
    <button class="recent-card" onClick={onPick}>
      <div class="row" style={{ gap: 10 }}>
        {ch.num > 0 && (
          <span class="num" style={{ fontSize: 22 }}>
            {ch.num}
          </span>
        )}
        <ChannelLogo name={ch.name} logo={ch.logo} size={30} radius={8} />
        <strong class="ellipsis" style={{ fontSize: 15 }}>
          {ch.name}
        </strong>
      </div>
      <div class="row" style={{ gap: 8 }}>
        <span class="dot" style={{ width: 7, height: 7 }} />
        <span class="ellipsis muted2" style={{ fontSize: 14 }}>
          {now?.title ?? 'En vivo'}
        </span>
      </div>
      <span class="dim" style={{ fontSize: 13 }}>
        {visitedAgo(r.at)}
      </span>
    </button>
  );
}

function Rails({ name, onPick }: { name: string; onPick: (c: Channel) => void }) {
  const vods = recents(false);
  const lives = recents(true);
  return (
    <>
      {vods.length > 0 && (
        <>
          <div class="section-title">
            <h2>Continuar viendo contenido de {name}</h2>
            <span>Lo último que viste</span>
          </div>
          <div class="rail">
            {vods.map(([c, r]) => (
              <VodCard key={c.id} ch={c} r={r} onPick={() => onPick(c)} />
            ))}
          </div>
        </>
      )}
      {lives.length > 0 && (
        <>
          <div class="section-title">
            <h2>Canales recientes</h2>
            <span>Lo que está pasando ahora en los que visitaste</span>
          </div>
          <div class="rail">
            {lives.map(([c, r]) => (
              <RecentCard key={c.id} ch={c} r={r} onPick={() => onPick(c)} />
            ))}
          </div>
        </>
      )}
    </>
  );
}

/** Vista previa sin sonido al dejar el mouse sobre un canal (solo en compu). */
function HoverPreview({ ch }: { ch: Channel }) {
  const v = useRef<HTMLVideoElement>(null);
  const [ok, setOk] = useState(false);
  useEffect(() => {
    const video = v.current;
    if (!video) return;
    video.muted = true;
    let e: Engine | null = null;
    let dead = false;
    const t = setTimeout(() => {
      void attach(video, ch, () => undefined).then((x) => (dead ? x.destroy() : (e = x)));
    }, 700);
    const on = () => setOk(true);
    video.addEventListener('playing', on);
    return () => {
      dead = true;
      clearTimeout(t);
      video.removeEventListener('playing', on);
      e?.destroy();
    };
  }, [ch.id]);
  return (
    <>
      <video ref={v} muted playsInline style={{ opacity: ok ? 1 : 0, transition: 'opacity .3s' }} />
      {ok && <span class="pill dark tag">Vista previa</span>}
    </>
  );
}

function ChannelCard({ ch, playing, onPick }: { ch: Channel; playing: boolean; onPick: () => void }) {
  const [hover, setHover] = useState(false);
  const [now] = programmeFor(ch);
  return (
    <button class={`ch-card${playing ? ' playing' : ''}`} onClick={onPick} onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)} onFocus={() => sfx('tick')}>
      <div class="art">
        <ChannelLogo name={ch.name} logo={ch.logo} size={64} radius={16} />
        {hover && !playing && <HoverPreview ch={ch} />}
        {playing && <span class="pill tag" style={{ background: 'var(--menta)', color: 'var(--noche)' }}>Viendo ahora</span>}
      </div>
      <div class="info">
        <div class="row" style={{ gap: 10 }}>
          {ch.num > 0 && (
            <span class="num" style={{ fontSize: 20 }}>
              {ch.num}
            </span>
          )}
          <strong class="ellipsis" style={{ fontSize: 16 }}>
            {ch.name}
          </strong>
        </div>
        <span class="ellipsis muted" style={{ fontSize: 14 }}>
          {now ? `Ahora: ${now.title}` : ch.group}
        </span>
        {now && <Progress value={progressOf(now)} width="100%" height={4} />}
      </div>
    </button>
  );
}

function InstallTip() {
  const [hidden, setHidden] = useState(() => localStorage.getItem('fitotv-tip') === '1');
  const [prompt, setPrompt] = useState<(Event & { prompt: () => Promise<void> }) | null>(null);
  useEffect(() => {
    const on = (e: Event) => {
      e.preventDefault();
      setPrompt(e as Event & { prompt: () => Promise<void> });
    };
    window.addEventListener('beforeinstallprompt', on);
    return () => window.removeEventListener('beforeinstallprompt', on);
  }, []);
  const standalone = matchMedia('(display-mode: standalone)').matches || (navigator as Navigator & { standalone?: boolean }).standalone;
  if (hidden || standalone) return null;
  const ios = /iphone|ipad|ipod/i.test(navigator.userAgent);
  const close = () => {
    localStorage.setItem('fitotv-tip', '1');
    setHidden(true);
  };
  return (
    <div class="install-tip">
      <Logo size={36} />
      <span class="grow">
        {ios ? (
          <>
            Tenla como app: toca <Icon d={I.share} size={16} /> <strong>Compartir</strong> y luego <strong>Agregar a inicio</strong>.
          </>
        ) : prompt ? (
          'Instala FitoTV en tu celular para abrirla como app.'
        ) : (
          <>
            Tenla como app: en el menú de Chrome elige <strong>Agregar a la pantalla principal</strong>.
          </>
        )}
      </span>
      {prompt && (
        <button class="btn btn-primary btn-sm" onClick={() => void prompt.prompt().then(close)}>
          Instalar
        </button>
      )}
      <button class="icon-btn" style={{ width: 36, height: 36 }} aria-label="Cerrar aviso" onClick={close}>
        <Icon d={I.close} size={16} />
      </button>
    </div>
  );
}

function EmptyList() {
  return (
    <div class="col" style={{ alignItems: 'center', textAlign: 'center', gap: 14, padding: '80px 24px' }}>
      <span style={{ width: 80, height: 80, borderRadius: 40, background: 'var(--s2)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Icon d={I.list} size={36} color="var(--num)" />
      </span>
      <h2 class="display" style={{ fontSize: 30, margin: 0 }}>
        {activeList.value ? (profile.value?.kids ? 'No hay canales infantiles aquí' : 'Esta lista no tiene canales') : 'Agrega tu primera lista'}
      </h2>
      <p class="muted" style={{ margin: 0, maxWidth: 440, lineHeight: 1.5 }}>
        {activeList.value
          ? profile.value?.kids
            ? 'Esta lista no trae un grupo Infantil. Prueba con otra lista.'
            : 'Revisa la lista o elige otra.'
          : 'Con un enlace, un archivo o el usuario y contraseña de tu proveedor.'}
      </p>
      <button class="btn btn-primary" onClick={() => (activeList.value ? go({ name: 'me' }) : go({ name: 'addList', firstTime: false }))}>
        <Icon d={I.plus} size={18} /> {activeList.value ? 'Cambiar de lista' : 'Agregar una lista'}
      </button>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Escritorio
// ---------------------------------------------------------------------------

export function DeskShell({ tab, children }: { tab: string; children: ComponentChildren }) {
  const p = profile.value;
  const list = activeList.value;
  const [q, setQ] = useState('');
  const results = q.trim().length >= 2 ? search(q) : null;
  const pd = profileData.value;
  const nav = [
    ['home', 'Inicio', I.home],
    ['guide', 'Guía', I.guide],
    ['MOVIE', 'Películas', I.film],
    ['SERIES', 'Series', I.tv],
  ];
  const openNav = (k: string) => {
    sfx('ok');
    if (k === 'home') openTab({ name: 'home' });
    else if (k === 'guide') openTab({ name: 'guide' });
    else openTab({ name: 'catalog', kind: k as 'MOVIE' | 'SERIES' });
  };
  const pickGroup = (g: string) => {
    selectGroup(g);
    if (screen.value.name !== 'home' && screen.value.name !== 'guide') openTab({ name: 'home' });
  };
  return (
    <div class="desk">
      <header>
        <Brand size={36} />
        <label class="search">
          <Icon d={I.search} size={18} color="var(--muted)" />
          <span class="sr-only">Buscar</span>
          <input placeholder="Busca un canal, película o serie" value={q} onInput={(e) => setQ((e.target as HTMLInputElement).value)} />
          {q && (
            <button aria-label="Borrar búsqueda" onClick={() => setQ('')}>
              <Icon d={I.close} size={16} color="var(--muted)" />
            </button>
          )}
        </label>
        <div class="grow" />
        {list && (
          <button class="list-chip" onClick={() => go({ name: 'me' })}>
            <span class="letter" style={{ background: list.color }}>
              {list.name[0]?.toUpperCase()}
            </span>
            <strong style={{ fontWeight: 600 }}>{list.name}</strong>
            <span class="muted">{list.channelCount} canales</span>
            <Icon d={I.chevronDown} size={18} color="var(--muted)" />
          </button>
        )}
        <button class="list-chip" onClick={() => go({ name: 'me' })} aria-label="Tu perfil">
          <Avatar k={p?.avatar} size={36} />
          <strong style={{ fontWeight: 600 }}>{p?.name}</strong>
          <Icon d={I.chevronDown} size={18} color="var(--muted)" />
        </button>
      </header>
      <nav class="side" aria-label="Secciones">
        {nav.map(([k, l, ic]) => (
          <button key={k} class={`nav-item${tab === k ? ' on' : ''}`} onClick={() => openNav(k)}>
            <Icon d={ic} size={20} /> {l}
          </button>
        ))}
        <hr />
        <span class="muted" style={{ fontSize: 14, fontWeight: 600, padding: '0 14px 6px' }}>
          Grupos
        </span>
        {pd.favorites.length > 0 && (
          <button class={`group-item${selectedGroup.value === FAVORITES ? ' on' : ''}`} onClick={() => pickGroup(FAVORITES)}>
            <Icon d={I.star} size={18} color="var(--menta)" /> Favoritos <span class="count">{pd.favorites.length}</span>
          </button>
        )}
        {groups.value.map((g) => (
          <button key={g.name} class={`group-item${selectedGroup.value === g.name ? ' on' : ''}`} onClick={() => pickGroup(g.name)}>
            <Icon d={groupIcon(g.name)} size={18} color={selectedGroup.value === g.name ? '#fff' : 'var(--muted2)'} />
            <span class="ellipsis">{g.name}</span>
            <span class="count">{g.count}</span>
          </button>
        ))}
      </nav>
      <main class="main">{results ? <SearchResults results={results} onClose={() => setQ('')} /> : children}</main>
    </div>
  );
}

function SearchResults({ results, onClose }: { results: Channel[]; onClose: () => void }) {
  const [sel, setSel] = useState<Channel | null>(null);
  const [cast, setCast] = useState<HTMLVideoElement | null>(null);
  return (
    <>
      {sel && (
        <div style={{ maxWidth: 900, marginBottom: 24 }}>
          <Player channel={sel} withSound onCast={setCast} />
        </div>
      )}
      <div class="section-title" style={{ marginTop: 0 }}>
        <h2>Resultados</h2>
        <span>{results.length}</span>
        <button class="btn btn-ghost btn-sm" style={{ marginLeft: 'auto' }} onClick={onClose}>
          Cerrar búsqueda
        </button>
      </div>
      {results.length === 0 ? (
        <p class="muted">No encontramos nada con ese nombre en esta lista.</p>
      ) : (
        <div class="grid">
          {results.map((c) => (
            <ChannelCard
              key={c.id}
              ch={c}
              playing={sel?.id === c.id}
              onPick={() => {
                setSel(c);
                play(c);
              }}
            />
          ))}
        </div>
      )}
      {cast && <CastSheet video={cast} channel={sel} onClose={() => setCast(null)} />}
    </>
  );
}

function DeskHome() {
  const { ch, withSound, choose } = useWatch();
  const [cast, setCast] = useState<HTMLVideoElement | null>(null);
  const now = useNow();
  const list = channelsOf(selectedGroup.value);
  const [prog] = ch ? programmeFor(ch) : [null];
  const todays = ch?.tvgId ? (epg.value[ch.tvgId.toLowerCase()] ?? []).filter((p) => p.stop > now - 2 * 3600_000).slice(0, 6) : [];
  const top = useRef<HTMLDivElement>(null);
  const pick = (c: Channel) => {
    choose(c);
    top.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };
  if (!channels.value.length) return loading.value ? <Spinner size={40} /> : <EmptyList />;
  return (
    <>
      <div ref={top} class="watch" style={{ scrollMarginTop: 20 }}>
        <div class="col" style={{ gap: 18, minWidth: 0 }}>
          <Player channel={ch} withSound={withSound} onCast={setCast} />
          {ch && (
            <div class="row" style={{ gap: 16 }}>
              {ch.num > 0 && (
                <span class="num" style={{ fontSize: 44 }}>
                  {ch.num}
                </span>
              )}
              <ChannelLogo name={ch.name} logo={ch.logo} size={40} radius={10} />
              <div class="col grow">
                <strong class="ellipsis" style={{ fontSize: 22 }}>
                  {ch.name}
                </strong>
                <span class="ellipsis muted2" style={{ fontSize: 15 }}>
                  {prog ? `${prog.title}, ${minutesLeft(prog, now)}` : ch.kind === 'LIVE' ? 'En vivo' : typeLabel(ch.kind)}
                </span>
              </div>
              <button class="btn btn-secondary btn-sm" onClick={() => toggleFavorite(ch.id)} aria-pressed={isFavorite(ch.id)}>
                <Icon d={I.star} size={18} color={isFavorite(ch.id) ? 'var(--menta)' : 'var(--muted2)'} fill={isFavorite(ch.id)} stroke={2} />
                {isFavorite(ch.id) ? 'En favoritos' : 'Agregar a favoritos'}
              </button>
            </div>
          )}
        </div>
        <aside class="guide-panel">
          <strong class="display" style={{ fontSize: 22 }}>
            Programación de hoy
          </strong>
          <span class="muted" style={{ fontSize: 14, margin: '2px 0 14px' }}>
            {ch?.name ?? ''}
          </span>
          {todays.length ? (
            todays.map((p) => {
              const live = p.start <= now && p.stop > now;
              return (
                <div key={p.start} class={`slot${live ? ' now' : ''}`}>
                  <span class="t">{hhmm(p.start)}</span>
                  <span class="col grow" style={{ gap: 6 }}>
                    <span class="ellipsis" style={{ fontWeight: live ? 600 : 500 }}>
                      {p.title}
                    </span>
                    {live && <Progress value={progressOf(p, now)} width="100%" height={4} />}
                  </span>
                </div>
              );
            })
          ) : (
            <p class="muted" style={{ fontSize: 14, lineHeight: 1.5 }}>
              Este canal no trae guía en tu lista.
            </p>
          )}
          <div class="grow" />
          <button class="btn btn-secondary btn-block" style={{ marginTop: 16 }} onClick={() => openTab({ name: 'guide' })}>
            Ver la guía completa
          </button>
        </aside>
      </div>
      <Rails name={profile.value?.name ?? ''} onPick={pick} />
      <div class="section-title">
        <h2>{groupTitle()}</h2>
        <span>{list.length === 1 ? '1 canal' : `${list.length} canales`}</span>
      </div>
      <div class="grid">
        {list.map((c) => (
          <ChannelCard key={c.id} ch={c} playing={c.id === ch?.id} onPick={() => pick(c)} />
        ))}
      </div>
      {cast && <CastSheet video={cast} channel={ch} onClose={() => setCast(null)} />}
    </>
  );
}

// ---------------------------------------------------------------------------
// Celular
// ---------------------------------------------------------------------------

export function PhoneShell({ tab, children, header = true }: { tab: string; children: ComponentChildren; header?: boolean }) {
  const p = profile.value;
  const list = activeList.value;
  const tabs = [
    ['home', 'Inicio', I.home],
    ['guide', 'Guía', I.guide],
    ['MOVIE', 'Películas', I.film],
    ['SERIES', 'Series', I.tv],
    ['me', 'Perfil', I.user],
  ];
  const open = (k: string) => {
    sfx('tick');
    if (k === 'home') openTab({ name: 'home' });
    else if (k === 'guide') openTab({ name: 'guide' });
    else if (k === 'me') openTab({ name: 'me' });
    else openTab({ name: 'catalog', kind: k as 'MOVIE' | 'SERIES' });
  };
  return (
    <div class="phone">
      {header && (
        <header>
          <Logo size={34} />
          <button aria-label="Tu perfil" onClick={() => openTab({ name: 'me' })}>
            <Avatar k={p?.avatar} size={36} ring />
          </button>
          {list ? (
            <button class="list-chip grow" style={{ height: 40, minWidth: 0 }} onClick={() => openTab({ name: 'me' })}>
              <span class="letter" style={{ background: list.color, width: 28, height: 28 }}>
                {list.name[0]?.toUpperCase()}
              </span>
              <strong class="ellipsis" style={{ fontWeight: 600 }}>
                {list.name}
              </strong>
              <Icon d={I.chevronDown} size={18} color="var(--muted)" />
            </button>
          ) : (
            <div class="grow" />
          )}
          <button class="icon-btn" aria-label="Buscar" onClick={() => go({ name: 'search' })}>
            <Icon d={I.search} size={20} />
          </button>
        </header>
      )}
      {children}
      <nav class="tabbar" aria-label="Secciones">
        {tabs.map(([k, l, ic]) => (
          <button key={k} class={tab === k ? 'on' : ''} onClick={() => open(k)}>
            <span class="ic">
              <Icon d={ic} size={20} />
            </span>
            {l}
          </button>
        ))}
      </nav>
    </div>
  );
}

export function GroupSheet({ onClose }: { onClose: () => void }) {
  const pd = profileData.value;
  const pick = (g: string) => {
    selectGroup(g);
    onClose();
  };
  return (
    <div class="sheet-back" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div class="sheet" role="dialog" aria-label="Grupos" style={{ maxHeight: '80vh', overflowY: 'auto' }}>
        <div class="grab" />
        <strong class="display" style={{ fontSize: 24 }}>
          Grupos de {activeList.value?.name}
        </strong>
        <div class="col" style={{ gap: 6, marginTop: 14 }}>
          {pd.favorites.length > 0 && (
            <button class={`group-item${selectedGroup.value === FAVORITES ? ' on' : ''}`} onClick={() => pick(FAVORITES)}>
              <Icon d={I.star} size={18} color="var(--menta)" /> Favoritos <span class="count">{pd.favorites.length}</span>
            </button>
          )}
          {groups.value.map((g) => (
            <button key={g.name} class={`group-item${selectedGroup.value === g.name ? ' on' : ''}`} onClick={() => pick(g.name)}>
              <Icon d={groupIcon(g.name)} size={18} />
              <span class="ellipsis">{g.name}</span>
              <span class="count">{g.count}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

export function ChannelRows({ list, playingId, onPick }: { list: Channel[]; playingId?: string; onPick: (c: Channel) => void }) {
  const now = useNow();
  return (
    <div class="col">
      {list.map((c) => {
        const [p] = programmeFor(c);
        return (
          <button key={c.id} class={`ch-row${c.id === playingId ? ' on' : ''}`} onClick={() => onPick(c)}>
            <span class="n">{c.num > 0 ? c.num : ''}</span>
            <ChannelLogo name={c.name} logo={c.logo} size={36} radius={10} />
            <span class="col grow">
              <strong class="ellipsis" style={{ fontSize: 16, color: c.id === playingId ? 'var(--menta)' : undefined }}>
                {c.name}
              </strong>
              <span class="ellipsis muted" style={{ fontSize: 13 }}>
                {p ? `Ahora: ${p.title}` : c.group}
              </span>
            </span>
            {p && (
              <span class="dim" style={{ fontSize: 13, whiteSpace: 'nowrap' }}>
                hasta {hhmm(p.stop)}
              </span>
            )}
          </button>
        );
      })}
      {!list.length && <p class="muted pad">No hay canales en este grupo.</p>}
      <span class="sr-only">{now}</span>
    </div>
  );
}

function PhoneHome() {
  const { ch, withSound, choose } = useWatch();
  const [cast, setCast] = useState<HTMLVideoElement | null>(null);
  const [groupsOpen, setGroupsOpen] = useState(false);
  const now = useNow();
  const list = channelsOf(selectedGroup.value);
  const [prog] = ch ? programmeFor(ch) : [null];
  const pd = profileData.value;
  const pick = (c: Channel) => {
    choose(c);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };
  return (
    <PhoneShell tab="home">
      {!channels.value.length ? (
        loading.value ? (
          <div class="row" style={{ justifyContent: 'center', padding: 60 }}>
            <Spinner size={40} />
          </div>
        ) : (
          <EmptyList />
        )
      ) : (
        <>
          <Player channel={ch} withSound={withSound} phone onCast={setCast} />
          {ch && (
            <div class="row pad" style={{ gap: 12, paddingTop: 14 }}>
              {ch.num > 0 && (
                <span class="num" style={{ fontSize: 32 }}>
                  {ch.num}
                </span>
              )}
              <div class="col grow">
                <strong class="ellipsis" style={{ fontSize: 18 }}>
                  {ch.name}
                </strong>
                <span class="row" style={{ gap: 10 }}>
                  <span class="ellipsis muted2" style={{ fontSize: 14 }}>
                    {prog ? prog.title : ch.kind === 'LIVE' ? 'En vivo' : typeLabel(ch.kind)}
                  </span>
                  {prog && <Progress value={progressOf(prog, now)} width={70} height={4} />}
                  {prog && (
                    <span class="dim" style={{ fontSize: 13, whiteSpace: 'nowrap' }}>
                      {minutesLeft(prog, now).replace('quedan ', '')}
                    </span>
                  )}
                </span>
              </div>
              <button class="icon-btn" aria-label={isFavorite(ch.id) ? 'Quitar de favoritos' : 'Agregar a favoritos'} aria-pressed={isFavorite(ch.id)} onClick={() => toggleFavorite(ch.id)}>
                <Icon d={I.star} size={20} color={isFavorite(ch.id) ? 'var(--menta)' : 'var(--muted2)'} fill={isFavorite(ch.id)} />
              </button>
            </div>
          )}
          <InstallTip />
          <div class="pad">
            <Rails name={profile.value?.name ?? ''} onPick={pick} />
          </div>
          <div class="chips" style={{ marginTop: 8 }}>
            <button class="chip outline" onClick={() => setGroupsOpen(true)}>
              <Icon d={I.grid} size={18} /> Grupos
            </button>
            {pd.favorites.length > 0 && (
              <button class={`chip${selectedGroup.value === FAVORITES ? ' on' : ''}`} onClick={() => selectGroup(FAVORITES)}>
                Favoritos <span class="count">{pd.favorites.length}</span>
              </button>
            )}
            {groups.value.slice(0, 12).map((g) => (
              <button key={g.name} class={`chip${selectedGroup.value === g.name ? ' on' : ''}`} onClick={() => selectGroup(g.name)}>
                {g.name} <span class="count">{g.count}</span>
              </button>
            ))}
          </div>
          <ChannelRows list={list} playingId={ch?.id} onPick={pick} />
        </>
      )}
      {groupsOpen && <GroupSheet onClose={() => setGroupsOpen(false)} />}
      {cast && <CastSheet video={cast} channel={ch} onClose={() => setCast(null)} />}
    </PhoneShell>
  );
}

export function HomeScreen() {
  const phone = useIsPhone();
  // Muestra la guía actualizada aunque la página se quede abierta.
  useNow(60_000);
  void data.value;
  return phone ? (
    <PhoneHome />
  ) : (
    <DeskShell tab="home">
      <DeskHome />
    </DeskShell>
  );
}

