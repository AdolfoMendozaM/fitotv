import { useState } from 'preact/hooks';
import { FriendlyError, isHttp, isSecurePage } from '../data/lists';
import { LIST_COLORS, type ListKind } from '../data/models';
import { addList, back, data, finishOnboarding, profileId, replaceAll, type AddResult } from '../data/store';
import { Brand, I, Icon, Spinner, useIsPhone } from '../ui/kit';
import { sfx } from '../ui/sound';

type Tab = 'URL' | 'FILE' | 'XTREAM';

export function AddListScreen({ firstTime }: { firstTime: boolean }) {
  const phone = useIsPhone();
  const [tab, setTab] = useState<Tab>('URL');
  const [url, setUrl] = useState('');
  const [epgUrl, setEpg] = useState('');
  const [name, setName] = useState('');
  const [color, setColor] = useState(LIST_COLORS[data.value.playlists.filter((l) => l.profileId === profileId.value).length % LIST_COLORS.length]);
  const [server, setServer] = useState('');
  const [user, setUser] = useState('');
  const [pass, setPass] = useState('');
  const [file, setFile] = useState<{ name: string; text: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState<AddResult | null>(null);

  const submit = async (e: Event) => {
    e.preventDefault();
    const pid = profileId.value;
    if (!pid) return setError('Primero elige o crea un perfil.');
    let kind: ListKind = tab;
    if (tab === 'URL' && !url.trim()) return setError('Falta el enlace de la lista.');
    if (tab === 'FILE' && !file) return setError('Elige el archivo de tu lista.');
    if (tab === 'XTREAM' && (!server.trim() || !user.trim() || !pass)) return setError('Faltan datos: servidor, usuario y contraseña.');
    setBusy(true);
    setError(null);
    try {
      const res = await addList(pid, {
        name: name || (tab === 'FILE' ? file!.name.replace(/\.(m3u8?|txt)$/i, '') : '') || 'Mi lista',
        kind,
        url,
        fileText: file?.text,
        server,
        user,
        pass,
        epgUrl,
        color,
      });
      sfx('success');
      setDone(res);
    } catch (err) {
      setError(err instanceof FriendlyError ? err.message : 'No pudimos abrir la lista. Revisa los datos e intenta otra vez.');
    } finally {
      setBusy(false);
    }
  };

  const finish = () => {
    if (firstTime) {
      finishOnboarding();
      replaceAll({ name: 'home' });
    } else back();
  };

  if (done) {
    const allOk = done.playable >= done.count;
    return (
      <div class="onboard" style={{ maxWidth: 640, justifyContent: 'center', alignItems: 'center', textAlign: 'center' }}>
        <span class="a-pop" style={{ width: 84, height: 84, borderRadius: 42, background: 'var(--menta)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Icon d={I.check} size={46} color="var(--noche)" stroke={2.6} />
        </span>
        <h1 class="h1 a-up d1" style={{ marginTop: 26 }}>
          ¡Listo!
        </h1>
        <p class="lead a-up d2">
          {(name || 'Tu lista') + ` trae ${done.count} canales y programas.`}
          {!allOk && ` En el navegador se pueden ver ${done.playable}; los demás usan conexiones sin cifrar (http) y se ven en la app de tele.`}
        </p>
        <button class="btn btn-primary a-up d3" style={{ marginTop: 30 }} onClick={finish} autoFocus>
          <Icon d={I.play} size={18} /> Ver mis canales
        </button>
      </div>
    );
  }

  const httpWarn = isSecurePage() && ((tab === 'URL' && isHttp(url)) || (tab === 'XTREAM' && /^http:\/\//i.test(server.trim())) || (tab === 'XTREAM' && server.trim() !== '' && !/^https?:\/\//i.test(server.trim())));

  return (
    <form class="onboard" style={{ maxWidth: 640 }} onSubmit={submit}>
      <header>
        <Brand size={phone ? 30 : 36} />
        {!firstTime && (
          <button type="button" class="icon-btn" aria-label="Cerrar" onClick={back}>
            <Icon d={I.close} size={20} />
          </button>
        )}
      </header>
      <h1 class="h1 a-up" style={{ marginTop: 28 }}>
        {firstTime ? 'Agrega tu primera lista' : 'Agrega una lista'}
      </h1>
      <p class="lead a-up d1">Usa la forma en que te la dio tu proveedor.</p>

      <div class="tabs a-up d2" role="tablist" style={{ marginTop: 24 }}>
        {(
          [
            ['URL', 'Enlace'],
            ['FILE', 'Archivo'],
            ['XTREAM', 'Usuario'],
          ] as [Tab, string][]
        ).map(([k, l]) => (
          <button key={k} type="button" role="tab" aria-selected={tab === k} class={tab === k ? 'on' : ''} onClick={() => setTab(k)}>
            {l}
          </button>
        ))}
      </div>

      <div class="col a-up d3" style={{ gap: 20, marginTop: 22 }}>
        {tab === 'URL' && (
          <div class="field">
            <label for="enlace">Enlace de la lista</label>
            <input id="enlace" class="input" type="url" inputMode="url" placeholder="https://ejemplo.com/lista.m3u" value={url} onInput={(e) => setUrl((e.target as HTMLInputElement).value)} autoComplete="off" />
            <span class="hint">Suele terminar en .m3u o .m3u8.</span>
          </div>
        )}
        {tab === 'FILE' && (
          <div class="field">
            <label for="archivo">Archivo de la lista</label>
            <label for="archivo" class="row" style={{ gap: 12, padding: '16px', borderRadius: 14, border: '2px dashed var(--line3)', cursor: 'pointer' }}>
              <Icon d={I.file} size={24} color="var(--num)" />
              <span class="col grow">
                <strong>{file ? file.name : 'Elegir archivo .m3u'}</strong>
                <span class="hint">{file ? 'Toca para cambiarlo' : 'Desde tu celular o tu compu'}</span>
              </span>
            </label>
            <input
              id="archivo"
              class="sr-only"
              type="file"
              accept=".m3u,.m3u8,.txt,audio/x-mpegurl,application/vnd.apple.mpegurl"
              onChange={async (e) => {
                const f = (e.target as HTMLInputElement).files?.[0];
                if (f) setFile({ name: f.name, text: await f.text() });
              }}
            />
          </div>
        )}
        {tab === 'XTREAM' && (
          <>
            <div class="field">
              <label for="servidor">Servidor</label>
              <input id="servidor" class="input" inputMode="url" placeholder="https://servidor.com:8080" value={server} onInput={(e) => setServer((e.target as HTMLInputElement).value)} autoComplete="off" />
            </div>
            <div class="row" style={{ gap: 12 }}>
              <div class="field grow">
                <label for="usuario">Usuario</label>
                <input id="usuario" class="input" value={user} onInput={(e) => setUser((e.target as HTMLInputElement).value)} autoComplete="off" autoCapitalize="none" />
              </div>
              <div class="field grow">
                <label for="contrasena">Contraseña</label>
                <input id="contrasena" class="input" type="password" value={pass} onInput={(e) => setPass((e.target as HTMLInputElement).value)} autoComplete="off" />
              </div>
            </div>
            <span class="hint" style={{ marginTop: -10 }}>
              Son los datos que te dio tu proveedor (Xtream Codes).
            </span>
          </>
        )}
        {httpWarn && (
          <div class="row" style={{ gap: 12, padding: 14, borderRadius: 14, background: 'rgba(255,197,61,.12)', color: 'var(--ambar)', fontSize: 14, lineHeight: 1.45 }}>
            <Icon d={I.warn} size={20} stroke={2.6} />
            <span>Esta dirección empieza con http://. Los navegadores no dejan abrirla desde una página segura; prueba con https:// o súbela como archivo.</span>
          </div>
        )}
        {tab !== 'XTREAM' && (
          <div class="field">
            <label for="epg">
              Guía de programación <span class="muted" style={{ fontWeight: 500 }}>(opcional)</span>
            </label>
            <input id="epg" class="input" type="url" inputMode="url" placeholder="Enlace EPG o XMLTV" value={epgUrl} onInput={(e) => setEpg((e.target as HTMLInputElement).value)} autoComplete="off" />
            <span class="hint">Si tu lista ya trae guía, la encontramos sola.</span>
          </div>
        )}
        <div class="field">
          <label for="nombre-lista">
            Nombre de la lista <span class="muted" style={{ fontWeight: 500 }}>(opcional)</span>
          </label>
          <input id="nombre-lista" class="input" placeholder="Por ejemplo, Casa de mamá" maxLength={40} value={name} onInput={(e) => setName((e.target as HTMLInputElement).value)} />
        </div>
        <div class="row" style={{ gap: 16, flexWrap: 'wrap' }}>
          <span style={{ fontWeight: 600, fontSize: 15 }}>Color de la lista</span>
          <div class="row" style={{ gap: 10 }}>
            {LIST_COLORS.map((c) => (
              <button
                key={c}
                type="button"
                aria-label={`Color ${c}`}
                aria-pressed={c === color}
                onClick={() => setColor(c)}
                style={{ width: 36, height: 36, borderRadius: 10, background: c, boxShadow: c === color ? '0 0 0 3px var(--noche), 0 0 0 5px #fff' : 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--noche)', fontWeight: 800 }}
              >
                {c === color ? (name.trim()[0] || 'C').toUpperCase() : ''}
              </button>
            ))}
          </div>
        </div>
      </div>

      {error && (
        <div role="alert" class="row" style={{ gap: 12, marginTop: 20, padding: 14, borderRadius: 14, background: 'rgba(255,197,61,.12)', color: 'var(--ambar)', lineHeight: 1.45 }}>
          <Icon d={I.warn} size={20} stroke={2.6} />
          <span>{error}</span>
        </div>
      )}

      <div class="grow" style={{ minHeight: 24 }} />
      <button class="btn btn-primary btn-block" type="submit" disabled={busy} style={{ marginTop: 20 }}>
        {busy ? (
          <>
            <Spinner /> Revisando tu lista…
          </>
        ) : (
          'Agregar lista'
        )}
      </button>
      <p class="hint" style={{ textAlign: 'center', marginTop: 12 }}>
        Tus listas se guardan solo en este aparato. No se mandan a ningún servidor de FitoTV.
      </p>
    </form>
  );
}
