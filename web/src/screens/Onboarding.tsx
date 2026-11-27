import { useState } from 'preact/hooks';
import { AVATARS } from '../data/models';
import { back, createProfile, data, deleteProfile, go, replaceAll, startSession, updateProfile } from '../data/store';
import { Avatar, Brand, I, Icon, useIsPhone } from '../ui/kit';
import { sfx } from '../ui/sound';

// ---------------------------------------------------------------------------
// Tutorial (4 pasos en web y celular: aquí sí se puede transmitir)
// ---------------------------------------------------------------------------

const steps = [
  { title: 'Trae tus listas', text: 'Agrega las listas que ya tienes: con un enlace, un archivo o el usuario y contraseña que te dio tu proveedor.' },
  { title: 'Un perfil para cada quien', text: 'Cada quien con su avatar, sus listas y sus favoritos. Los peques pueden tener un perfil solo con canales infantiles.' },
  { title: 'Cambia de canal como siempre', text: 'Elige un grupo, pasa por los canales y mira una vista previa. La guía te dice qué sigue.' },
  { title: 'Mándalo a tu tele', text: 'Con Chromecast o AirPlay, lo que ves aquí se ve en grande. Tu celular queda como control.' },
];

function Illustration({ step }: { step: number }) {
  if (step === 0) {
    const lists = [
      ['Casa de mamá', '812 canales', 'var(--menta)'],
      ['Deportes', '120 canales', 'var(--azul)'],
      ['Canales abiertos', '48 canales', '#B79CFF'],
    ];
    return (
      <div class="col" style={{ gap: 12, width: '100%', maxWidth: 380 }}>
        {lists.map(([n, s, c], i) => (
          <div key={n} class="row a-up" style={{ animationDelay: `${0.1 + i * 0.08}s`, marginLeft: i * 22, gap: 14, padding: '14px 16px', borderRadius: 18, background: i === 0 ? 'var(--s3)' : 'var(--s2)' }}>
            <span style={{ width: 44, height: 44, borderRadius: 13, background: c, color: 'var(--noche)', fontWeight: 800, fontSize: 20, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{n[0]}</span>
            <span class="col">
              <strong style={{ fontSize: 17 }}>{n}</strong>
              <span class="muted" style={{ fontSize: 14 }}>{s}</span>
            </span>
          </div>
        ))}
      </div>
    );
  }
  if (step === 1) {
    const people = [
      ['audifonos', 'Fito'],
      ['pelo_largo', 'Mamá'],
      ['chongo', 'Abuela'],
      ['coletas', 'Peques'],
    ];
    return (
      <div class="row" style={{ gap: 14, flexWrap: 'wrap', justifyContent: 'center' }}>
        {people.map(([a, n], i) => (
          <div key={n} class="col a-pop" style={{ animationDelay: `${0.1 + i * 0.07}s`, alignItems: 'center', gap: 8 }}>
            <Avatar k={a} size={84} />
            <strong>{n}</strong>
            {a === 'coletas' && <span style={{ color: 'var(--menta)', fontSize: 13 }}>Solo infantil</span>}
          </div>
        ))}
      </div>
    );
  }
  if (step === 2) {
    return (
      <div class="col" style={{ gap: 12, width: '100%', maxWidth: 400 }}>
        <div class="a-pop" style={{ height: 180, borderRadius: 20, background: 'var(--s3)', position: 'relative', overflow: 'hidden' }}>
          <span class="pill dark" style={{ position: 'absolute', left: 12, top: 12 }}>Vista previa</span>
          <span style={{ position: 'absolute', left: 36, top: 52, width: 170, height: 96, borderRadius: 10, background: '#2B48B5' }} />
          <span style={{ position: 'absolute', right: 56, top: 64, width: 64, height: 64, borderRadius: 32, background: 'var(--noche)' }} />
        </div>
        <div class="row" style={{ gap: 10 }}>
          {[
            ['11', 'Canal Ciudad'],
            ['12', 'Noticias 24'],
            ['13', 'Mundo Hoy'],
          ].map(([n, name], i) => (
            <div key={n} class="col grow a-up" style={{ animationDelay: `${0.15 + i * 0.06}s`, padding: 12, borderRadius: 14, background: i === 1 ? 'var(--s3)' : 'var(--s2)', boxShadow: i === 1 ? '0 0 0 3px #fff' : undefined }}>
              <span class="num" style={{ fontSize: 20 }}>{n}</span>
              <span class="ellipsis" style={{ fontSize: 13, fontWeight: 600 }}>{name}</span>
            </div>
          ))}
        </div>
      </div>
    );
  }
  return (
    <div class="col" style={{ gap: 12, width: '100%', maxWidth: 380 }}>
      {[
        [I.cast, 'Tele de la sala', 'Chromecast'],
        [I.cast, 'Recámara', 'Google TV con Chromecast'],
        [I.airplay, 'Apple TV de la sala', 'AirPlay'],
      ].map(([ic, n, s], i) => (
        <div key={n} class="row a-up" style={{ animationDelay: `${0.1 + i * 0.08}s`, gap: 14, padding: '14px 16px', borderRadius: 16, background: i === 0 ? 'var(--s3)' : 'var(--s2)' }}>
          <Icon d={ic} size={24} />
          <span class="col">
            <strong>{n}</strong>
            <span class="muted" style={{ fontSize: 14 }}>{s}</span>
          </span>
        </div>
      ))}
    </div>
  );
}

export function TutorialScreen() {
  const [step, setStep] = useState(0);
  const phone = useIsPhone();
  const next = () => {
    if (step < steps.length - 1) {
      setStep(step + 1);
      sfx('step');
    } else go({ name: 'createProfile', firstTime: true });
  };
  const s = steps[step];
  const dots = (
    <div class="row" style={{ gap: 8 }} aria-hidden="true">
      {steps.map((_, i) => (
        <span key={i} style={{ width: i === step ? 26 : 10, height: 10, borderRadius: 5, background: i === step ? 'var(--menta)' : 'var(--line3)', transition: 'width .25s' }} />
      ))}
    </div>
  );
  return (
    <div class="onboard">
      <header>
        <Brand size={phone ? 30 : 36} />
        <button class="btn btn-ghost btn-sm" onClick={() => go({ name: 'createProfile', firstTime: true })}>
          Saltar
        </button>
      </header>
      <div key={step} class={phone ? 'col grow' : 'row grow'} style={{ gap: phone ? 28 : 64, alignItems: 'center', justifyContent: 'center', padding: phone ? '24px 0' : '40px 0' }}>
        {phone && (
          <div class="row" style={{ width: '100%', justifyContent: 'center', minHeight: 230 }}>
            <Illustration step={step} />
          </div>
        )}
        <div class="col" style={{ flex: phone ? 'none' : '1', maxWidth: 520, gap: 12, width: '100%' }}>
          <span class="a-up" style={{ color: 'var(--menta)', fontWeight: 700 }}>
            Paso {step + 1} de {steps.length}
          </span>
          <h1 class="h1 a-up d1">{s.title}</h1>
          <p class="lead a-up d2" style={{ margin: 0 }}>
            {s.text}
          </p>
        </div>
        {!phone && (
          <div class="row" style={{ flex: '1', justifyContent: 'center' }}>
            <Illustration step={step} />
          </div>
        )}
      </div>
      <div class="row" style={{ gap: 14, justifyContent: phone ? 'space-between' : 'flex-start' }}>
        {!phone && step > 0 && (
          <button class="btn btn-secondary" onClick={() => setStep(step - 1)}>
            Atrás
          </button>
        )}
        <button class={`btn btn-primary${phone ? ' btn-block' : ''}`} onClick={next} autoFocus>
          {step < steps.length - 1 ? 'Siguiente' : 'Crear mi perfil'}
        </button>
        {!phone && <span style={{ marginLeft: 8 }}>{dots}</span>}
      </div>
      {phone && <div class="row" style={{ justifyContent: 'center', marginTop: 16 }}>{dots}</div>}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Crea tu perfil (también para editar)
// ---------------------------------------------------------------------------

export function CreateProfileScreen({ firstTime, editId }: { firstTime: boolean; editId?: string }) {
  const editing = data.value.profiles.find((p) => p.id === editId);
  const [name, setName] = useState(editing?.name ?? '');
  const [avatar, setAvatar] = useState(editing?.avatar ?? 'audifonos');
  const [kids, setKids] = useState(editing?.kids ?? false);
  const [confirm, setConfirm] = useState(false);
  const phone = useIsPhone();

  const save = (e: Event) => {
    e.preventDefault();
    sfx('success');
    if (editing) {
      updateProfile(editing.id, name, avatar, kids);
      back();
    } else if (firstTime) {
      const id = createProfile(name, avatar, kids);
      startSession(id);
      go({ name: 'addList', firstTime: true });
    } else {
      createProfile(name, avatar, kids);
      back();
    }
  };

  return (
    <form class="onboard" onSubmit={save} style={{ maxWidth: 640 }}>
      <header>
        <Brand size={phone ? 30 : 36} />
        {!firstTime && (
          <button type="button" class="icon-btn" aria-label="Cerrar" onClick={back}>
            <Icon d={I.close} size={20} />
          </button>
        )}
      </header>
      <h1 class="h1 a-up" style={{ marginTop: 28 }}>
        {editing ? 'Edita el perfil' : 'Crea tu perfil'}
      </h1>
      <p class="lead a-up d1">Así tus listas, favoritos y recientes quedan a tu nombre.</p>
      <div class="row a-pop d2" style={{ justifyContent: 'center', margin: '28px 0 8px' }}>
        <span style={{ padding: 8, borderRadius: '50%', boxShadow: '0 0 0 4px var(--menta)' }}>
          <Avatar k={avatar} size={phone ? 120 : 150} />
        </span>
      </div>
      <div class="field a-up d3" style={{ marginTop: 20 }}>
        <label for="nombre">¿Cómo te llamas?</label>
        <input id="nombre" class="input" placeholder="Tu nombre o apodo" maxLength={20} value={name} onInput={(e) => setName((e.target as HTMLInputElement).value)} autoComplete="nickname" />
      </div>
      <div class="field a-up d4" style={{ marginTop: 22 }}>
        <label>Elige tu avatar</label>
        <div class="row" style={{ gap: phone ? 8 : 12, flexWrap: 'wrap' }}>
          {AVATARS.map((a) => (
            <button
              key={a.key}
              type="button"
              aria-label={a.label}
              aria-pressed={a.key === avatar}
              onClick={() => {
                setAvatar(a.key);
                sfx('tick');
              }}
              style={{ borderRadius: '50%', padding: 3, boxShadow: a.key === avatar ? '0 0 0 3px var(--menta)' : 'none' }}
            >
              <Avatar k={a.key} size={phone ? 52 : 64} />
            </button>
          ))}
        </div>
      </div>
      <button
        type="button"
        class="row a-up d5"
        onClick={() => {
          setKids(!kids);
          sfx(kids ? 'off' : 'on');
        }}
        style={{ marginTop: 22, gap: 14, padding: '14px 16px', borderRadius: 16, background: 'var(--s1)', width: '100%' }}
        aria-pressed={kids}
      >
        <Icon d={I.kids} size={22} color="var(--muted2)" />
        <span class="col grow">
          <strong>Perfil infantil</strong>
          <span class="muted" style={{ fontSize: 14 }}>Solo muestra canales del grupo Infantil</span>
        </span>
        <span class={`switch${kids ? ' on' : ''}`} />
      </button>
      <div class="grow" style={{ minHeight: 28 }} />
      <div class="col" style={{ gap: 12 }}>
        <button class="btn btn-primary btn-block" type="submit">
          {editing ? 'Guardar' : 'Continuar'}
        </button>
        {editing && (
          <button
            type="button"
            class="btn btn-ghost btn-block"
            onClick={() => {
              if (!confirm) return setConfirm(true);
              deleteProfile(editing.id);
              replaceAll({ name: data.value.profiles.length ? 'profiles' : 'tutorial' });
            }}
          >
            <Icon d={I.trash} size={18} /> {confirm ? 'Toca otra vez para borrar el perfil' : 'Borrar perfil'}
          </button>
        )}
        {!editing && (
          <p class="hint" style={{ textAlign: 'center', margin: 0 }}>
            Después puedes crear más perfiles para tu familia.
          </p>
        )}
      </div>
    </form>
  );
}

// ---------------------------------------------------------------------------
// ¿Quién va a ver?
// ---------------------------------------------------------------------------

export function ProfilesScreen() {
  const [manage, setManage] = useState(false);
  const phone = useIsPhone();
  const d = data.value;
  const ordered = [...d.profiles].sort((a, b) => a.createdAt - b.createdAt);
  const size = phone ? 96 : 140;
  return (
    <div class="onboard" style={{ alignItems: 'center' }}>
      <header style={{ alignSelf: 'stretch' }}>
        <Brand size={phone ? 30 : 36} />
      </header>
      <div class="col grow" style={{ alignItems: 'center', justifyContent: 'center', gap: phone ? 32 : 48, width: '100%' }}>
        <h1 class="h1 a-up">{manage ? 'Administrar perfiles' : '¿Quién va a ver?'}</h1>
        <div class="row" style={{ gap: phone ? 20 : 36, flexWrap: 'wrap', justifyContent: 'center', maxWidth: 900 }}>
          {ordered.map((p, i) => {
            const lists = d.playlists.filter((l) => l.profileId === p.id).length;
            return (
              <button
                key={p.id}
                class="col a-pop"
                style={{ animationDelay: `${0.06 * i}s`, alignItems: 'center', gap: 10, width: size + 10, textAlign: 'center' }}
                autoFocus={p.id === d.settings.lastProfileId}
                onClick={() => {
                  sfx('ok');
                  if (manage) go({ name: 'createProfile', firstTime: false, editId: p.id });
                  else {
                    startSession(p.id);
                    replaceAll({ name: 'home' });
                  }
                }}
              >
                <span style={{ position: 'relative', borderRadius: '50%', boxShadow: p.id === d.settings.lastProfileId ? '0 0 0 4px var(--noche), 0 0 0 7px #fff' : undefined }}>
                  <Avatar k={p.avatar} size={size} />
                  {manage && (
                    <span style={{ position: 'absolute', inset: 0, borderRadius: '50%', background: 'rgba(10,23,71,.6)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <Icon d={I.edit} size={34} color="#fff" />
                    </span>
                  )}
                </span>
                <strong class="ellipsis" style={{ fontSize: phone ? 17 : 21, maxWidth: '100%' }}>
                  {p.name}
                </strong>
                <span style={{ fontSize: 14, color: p.kids ? 'var(--menta)' : 'var(--muted)', marginTop: -6 }}>{p.kids ? 'Solo infantil' : lists === 1 ? '1 lista' : `${lists} listas`}</span>
              </button>
            );
          })}
          <button class="col a-pop" style={{ alignItems: 'center', gap: 10, width: size + 10 }} onClick={() => go({ name: 'createProfile', firstTime: false })}>
            <span style={{ width: size, height: size, borderRadius: '50%', border: '3px dashed #3550A8', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Icon d={I.plus} size={40} color="var(--num)" />
            </span>
            <span class="muted" style={{ fontSize: phone ? 17 : 21 }}>
              Agregar
            </span>
          </button>
        </div>
        <button class="btn btn-secondary" onClick={() => setManage(!manage)}>
          <Icon d={manage ? I.check : I.edit} size={18} /> {manage ? 'Listo' : 'Administrar perfiles'}
        </button>
      </div>
      <p class="muted" style={{ textAlign: 'center', fontSize: 15 }}>
        Cada perfil guarda sus propias listas, favoritos y lo último que vio.
      </p>
    </div>
  );
}
