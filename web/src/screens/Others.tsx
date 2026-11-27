import type { ComponentChildren } from 'preact';
import { useState } from 'preact/hooks';
import type { Channel } from '../data/models';
import { AVATARS } from '../data/models';
import {
  activeList,
  back,
  channelsOf,
  current,
  data,
  FAVORITES,
  go,
  myLists,
  play,
  profile,
  profileData,
  programmeFor,
  refreshActive,
  removeList,
  replaceAll,
  search,
  selectedGroup,
  setActiveList,
  setSound,
  updateProfile,
  vod,
  epg,
} from '../data/store';
import { CastSheet, Player } from '../ui/Player';
import { Avatar, ChannelLogo, hhmm, I, Icon, Poster, progressOf, Progress, useIsPhone, useNow, vodFraction } from '../ui/kit';
import { ChannelRows, DeskShell, GroupSheet, PhoneShell } from './Home';
import { sfx } from '../ui/sound';

const isPhone = () => matchMedia('(max-width: 899px)').matches;

function Shell({ tab, children, header }: { tab: string; children: ComponentChildren; header?: boolean }) {
  return useIsPhone() ? (
    <PhoneShell tab={tab} header={header}>
      {children}
    </PhoneShell>
  ) : (
    <DeskShell tab={tab}>{children}</DeskShell>
  );
}

function Title({ children, sub }: { children: string; sub?: string }) {
  return (
    <div class="section-title" style={{ marginTop: isPhone() ? 18 : 0 }}>
      <h2 style={{ fontSize: isPhone() ? 26 : 32 }}>{children}</h2>
      {sub && <span>{sub}</span>}
    </div>
  );
}

/** Reproductor arriba cuando eligen algo en estas pantallas. */
function InlinePlayer({ ch }: { ch: Channel | null }) {
  const [cast, setCast] = useState<HTMLVideoElement | null>(null);
  const phone = useIsPhone();
  if (!ch) return null;
  return (
    <div style={{ maxWidth: phone ? undefined : 900, margin: phone ? '0 -16px 8px' : '0 0 24px' }}>
      <Player channel={ch} withSound phone={phone} onCast={setCast} />
      {cast && <CastSheet video={cast} channel={ch} onClose={() => setCast(null)} />}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Guía
// ---------------------------------------------------------------------------

export function GuideScreen() {
  const now = useNow();
  const phone = useIsPhone();
  const [groupsOpen, setGroupsOpen] = useState(false);
  const [sel, setSel] = useState<Channel | null>(null);
  const list = channelsOf(selectedGroup.value);
  const g = selectedGroup.value === FAVORITES ? 'Favoritos' : selectedGroup.value ?? 'Canales';
  const hasGuide = Object.keys(epg.value).length > 0;
  const pick = (c: Channel) => {
    sfx('ok');
    setSel(c);
    play(c);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };
  return (
    <Shell tab="guide">
      <div class={phone ? 'pad' : ''}>
        <InlinePlayer ch={sel} />
        <Title sub={`${g}, ${list.length} canales`}>Guía</Title>
        {phone && (
          <button class="chip outline" style={{ marginBottom: 10 }} onClick={() => setGroupsOpen(true)}>
            <Icon d={I.grid} size={18} /> Cambiar de grupo
          </button>
        )}
        {!hasGuide && <p class="muted" style={{ lineHeight: 1.5 }}>Esta lista no trae guía de programación, o el proveedor no deja leerla desde páginas web. Puedes agregar un enlace de guía al agregar la lista.</p>}
      </div>
      {phone ? (
        <ChannelRows list={list} playingId={sel?.id} onPick={pick} />
      ) : (
        <div class="col" style={{ gap: 8 }}>
          {list.map((c) => {
            const [p, n] = programmeFor(c);
            return (
              <button key={c.id} class="row" onClick={() => pick(c)} style={{ gap: 16, padding: '12px 16px', borderRadius: 16, background: sel?.id === c.id ? 'var(--s3)' : 'var(--s1)', textAlign: 'left' }}>
                <span class="num" style={{ fontSize: 22, width: 40, textAlign: 'right' }}>
                  {c.num > 0 ? c.num : ''}
                </span>
                <ChannelLogo name={c.name} logo={c.logo} size={40} radius={10} />
                <strong class="ellipsis" style={{ width: 220, fontSize: 16 }}>
                  {c.name}
                </strong>
                <span class="col grow" style={{ gap: 6 }}>
                  <span class="ellipsis" style={{ fontWeight: 600 }}>
                    {p ? p.title : 'Sin información de la guía'}
                  </span>
                  {p && (
                    <span class="row" style={{ gap: 10, fontSize: 13 }}>
                      <span class="muted">
                        {hhmm(p.start)} a {hhmm(p.stop)}
                      </span>
                      <Progress value={progressOf(p, now)} width={120} height={4} />
                    </span>
                  )}
                </span>
                <span class="col" style={{ width: 260, gap: 4 }}>
                  {n && (
                    <>
                      <span class="dim" style={{ fontSize: 13 }}>
                        Después, {hhmm(n.start)}
                      </span>
                      <span class="ellipsis muted2" style={{ fontSize: 14 }}>
                        {n.title}
                      </span>
                    </>
                  )}
                </span>
              </button>
            );
          })}
        </div>
      )}
      {groupsOpen && <GroupSheet onClose={() => setGroupsOpen(false)} />}
    </Shell>
  );
}

// ---------------------------------------------------------------------------
// Películas y series
// ---------------------------------------------------------------------------

export function CatalogScreen({ kind }: { kind: 'MOVIE' | 'SERIES' }) {
  const phone = useIsPhone();
  const all = vod(kind);
  const cats = [...new Set(all.map((c) => c.group))];
  const [cat, setCat] = useState<string | null>(null);
  const [sel, setSel] = useState<Channel | null>(null);
  const shown = cat ? all.filter((c) => c.group === cat) : all;
  const recents = new Map(profileData.value.recents.map((r) => [r.channelId, r]));
  const title = kind === 'MOVIE' ? 'Películas' : 'Series';
  return (
    <Shell tab={kind}>
      <div class={phone ? 'pad' : ''}>
        <InlinePlayer ch={sel} />
        <Title sub={all.length ? `${all.length} en ${activeList.value?.name ?? 'tu lista'}` : undefined}>{title}</Title>
        {!all.length ? (
          <p class="muted" style={{ lineHeight: 1.5, maxWidth: 560 }}>
            Tu lista no trae {title.toLowerCase()}. Algunas listas solo traen canales en vivo; si tu proveedor te dio otra lista con {title.toLowerCase()}, agrégala desde Perfil.
          </p>
        ) : (
          <>
            <div class="chips" style={{ padding: '0 0 16px', margin: 0 }}>
              <button class={`chip${cat == null ? ' on' : ''}`} onClick={() => setCat(null)}>
                Todas
              </button>
              {cats.map((c) => (
                <button key={c} class={`chip${cat === c ? ' on' : ''}`} onClick={() => setCat(c)}>
                  {c}
                </button>
              ))}
            </div>
            <div class="poster-grid" style={phone ? { gridTemplateColumns: 'repeat(auto-fill, minmax(104px, 1fr))', gap: '16px 12px' } : undefined}>
              {shown.slice(0, 400).map((c) => {
                const r = recents.get(c.id);
                return (
                  <button
                    key={c.id}
                    class="poster"
                    onClick={() => {
                      sfx('ok');
                      setSel(c);
                      play(c);
                      window.scrollTo({ top: 0, behavior: 'smooth' });
                    }}
                  >
                    <div class="art" style={{ background: 'var(--s3)' }}>
                      <Poster name={c.name} logo={c.logo} size={phone ? 26 : 34} />
                      {r && (
                        <span style={{ position: 'absolute', left: 0, right: 0, bottom: 0, height: 5, background: 'rgba(10,23,71,.7)' }}>
                          <span style={{ display: 'block', height: 5, width: `${vodFraction(r) * 100}%`, background: 'var(--menta)' }} />
                        </span>
                      )}
                    </div>
                    <span class="clamp2" style={{ fontSize: phone ? 13 : 15, fontWeight: 600, lineHeight: 1.3 }}>
                      {c.name}
                    </span>
                  </button>
                );
              })}
            </div>
          </>
        )}
      </div>
    </Shell>
  );
}

// ---------------------------------------------------------------------------
// Buscar (celular)
// ---------------------------------------------------------------------------

export function SearchScreen() {
  const [q, setQ] = useState('');
  const [sel, setSel] = useState<Channel | null>(null);
  const results = search(q);
  return (
    <PhoneShell tab="" header={false}>
      <header>
        <button class="icon-btn" aria-label="Regresar" onClick={back}>
          <Icon d={I.chevronLeft} size={22} />
        </button>
        <label class="search grow" style={{ flex: 1 }}>
          <Icon d={I.search} size={18} color="var(--muted)" />
          <span class="sr-only">Buscar</span>
          <input autoFocus placeholder="Canal, película o serie" value={q} onInput={(e) => setQ((e.target as HTMLInputElement).value)} />
        </label>
      </header>
      <div class="pad">
        <InlinePlayer ch={sel} />
        {q.trim().length < 2 ? (
          <p class="muted">Escribe al menos dos letras.</p>
        ) : !results.length ? (
          <p class="muted">No encontramos “{q.trim()}” en esta lista.</p>
        ) : null}
      </div>
      <ChannelRows
        list={results}
        playingId={sel?.id}
        onPick={(c) => {
          setSel(c);
          play(c);
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }}
      />
    </PhoneShell>
  );
}

// ---------------------------------------------------------------------------
// Tu perfil y tus listas
// ---------------------------------------------------------------------------

export function MeScreen() {
  const p = profile.value;
  const lists = myLists.value;
  const active = activeList.value;
  const phone = useIsPhone();
  const [confirm, setConfirm] = useState<string | null>(null);
  const [about, setAbout] = useState(false);
  if (!p) return null;
  const body = (
    <div class={phone ? 'pad' : ''} style={{ maxWidth: 720 }}>
      <div class="row" style={{ justifyContent: 'space-between', marginTop: phone ? 14 : 0 }}>
        <h2 class="display" style={{ fontSize: phone ? 26 : 32, margin: 0 }}>
          Tu perfil
        </h2>
        <button class="btn btn-secondary btn-sm" onClick={() => replaceAll({ name: 'profiles' })}>
          Cambiar de perfil
        </button>
      </div>
      <div class="col" style={{ alignItems: 'center', gap: 8, margin: '22px 0' }}>
        <span style={{ padding: 6, borderRadius: '50%', boxShadow: '0 0 0 3px var(--menta)' }}>
          <Avatar k={p.avatar} size={phone ? 96 : 120} />
        </span>
        <strong class="display" style={{ fontSize: 28 }}>
          {p.name}
        </strong>
        <span class="muted" style={{ fontSize: 14 }}>
          Toca un avatar para cambiar el tuyo
        </span>
        <div class="row" style={{ gap: 10, marginTop: 6 }}>
          {AVATARS.map((a) => (
            <button
              key={a.key}
              aria-label={a.label}
              aria-pressed={a.key === p.avatar}
              onClick={() => {
                updateProfile(p.id, p.name, a.key, p.kids);
                sfx('tick');
              }}
              style={{ borderRadius: '50%', padding: 2, boxShadow: a.key === p.avatar ? '0 0 0 3px var(--menta)' : 'none' }}
            >
              <Avatar k={a.key} size={phone ? 46 : 54} />
            </button>
          ))}
        </div>
        <button class="btn btn-ghost btn-sm" onClick={() => go({ name: 'createProfile', firstTime: false, editId: p.id })}>
          <Icon d={I.edit} size={16} /> Editar nombre o borrar perfil
        </button>
      </div>

      <div class="row" style={{ justifyContent: 'space-between', margin: '8px 0 12px' }}>
        <strong style={{ fontSize: 18 }}>Tus listas</strong>
        <span class="muted" style={{ fontSize: 14 }}>
          Toca una para verla
        </span>
      </div>
      <div class="col" style={{ gap: 10 }}>
        {lists.map((l) => (
          <div key={l.id} class="col" style={{ borderRadius: 18, background: l.id === active?.id ? 'var(--s3)' : 'var(--s1)', boxShadow: l.id === active?.id ? 'inset 0 0 0 2px var(--azul)' : undefined }}>
            <button
              class="row"
              style={{ gap: 14, padding: '14px 16px', textAlign: 'left' }}
              onClick={() => {
                sfx('ok');
                if (l.id !== active?.id) setActiveList(l.id);
                replaceAll({ name: 'home' });
              }}
            >
              <span style={{ width: 42, height: 42, borderRadius: 12, background: l.color, color: 'var(--noche)', fontWeight: 800, fontSize: 18, display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 'none' }}>
                {l.name[0]?.toUpperCase()}
              </span>
              <span class="col grow">
                <strong class="ellipsis">{l.name}</strong>
                <span class="ellipsis" style={{ fontSize: 13, color: l.lastError ? 'var(--ambar)' : 'var(--muted)' }}>
                  {l.lastError ? `No se pudo actualizar: ${l.lastError}` : `${l.channelCount} canales`}
                </span>
              </span>
              <span style={{ width: 22, height: 22, borderRadius: 11, border: '2px solid var(--muted)', display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 'none' }}>
                {l.id === active?.id && <span style={{ width: 10, height: 10, borderRadius: 5, background: '#fff' }} />}
              </span>
            </button>
            {l.id === active?.id && (
              <div class="row" style={{ gap: 8, padding: '0 16px 14px' }}>
                <button class="btn btn-secondary btn-sm" onClick={() => void refreshActive(false)}>
                  <Icon d={I.refresh} size={16} /> Actualizar
                </button>
                <button
                  class="btn btn-ghost btn-sm"
                  onClick={() => {
                    if (confirm !== l.id) return setConfirm(l.id);
                    removeList(l.id);
                    setConfirm(null);
                  }}
                >
                  <Icon d={I.trash} size={16} /> {confirm === l.id ? 'Toca otra vez para quitarla' : 'Quitar'}
                </button>
              </div>
            )}
          </div>
        ))}
        <button class="row" onClick={() => go({ name: 'addList', firstTime: false })} style={{ gap: 14, padding: '14px 16px', borderRadius: 18, border: '2px dashed var(--line3)', textAlign: 'left' }}>
          <span style={{ width: 42, height: 42, borderRadius: 12, background: 'var(--s2)', display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 'none' }}>
            <Icon d={I.plus} size={22} color="var(--num)" />
          </span>
          <span class="col">
            <strong>Agregar lista</strong>
            <span class="muted" style={{ fontSize: 13 }}>
              Enlace, archivo o usuario y contraseña
            </span>
          </span>
        </button>
      </div>

      <strong style={{ display: 'block', fontSize: 18, margin: '28px 0 12px' }}>Ajustes</strong>
      <div class="col" style={{ gap: 10 }}>
        <button class="row" onClick={() => setSound(!data.value.settings.sound)} aria-pressed={data.value.settings.sound} style={{ gap: 14, padding: '14px 16px', borderRadius: 16, background: 'var(--s1)', textAlign: 'left' }}>
          <Icon d={I.sound} size={22} color="var(--muted2)" />
          <span class="col grow">
            <strong>Sonidos</strong>
            <span class="muted" style={{ fontSize: 13 }}>
              Los sonidos de marimba al moverte y al abrir FitoTV
            </span>
          </span>
          <span class={`switch${data.value.settings.sound ? ' on' : ''}`} />
        </button>
        <button class="row" onClick={() => setAbout(!about)} style={{ gap: 14, padding: '14px 16px', borderRadius: 16, background: 'var(--s1)', textAlign: 'left' }}>
          <Icon d={I.warn} size={22} color="var(--muted2)" />
          <span class="col grow">
            <strong>Acerca de FitoTV</strong>
            <span class="muted" style={{ fontSize: 13 }}>
              Versión web {__APP_VERSION__}
            </span>
          </span>
        </button>
        {about && (
          <p class="muted" style={{ lineHeight: 1.55, fontSize: 14, margin: '4px 4px 0' }}>
            FitoTV es solo un reproductor: no incluye canales. Reproduce las listas que tú agregas, de proveedores que tengas permiso de usar. Tus listas se guardan solo en este aparato. En el navegador no se pueden ver los canales que usan conexiones sin cifrar (http); esos se ven en la app de tele.
          </p>
        )}
      </div>
      <div style={{ height: 32 }} />
    </div>
  );
  return <Shell tab="me">{body}</Shell>;
}

declare const __APP_VERSION__: string;
