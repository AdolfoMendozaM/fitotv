import { render } from 'preact';
import { App } from './app';
import './styles.css';

// La dirección oficial ahora es la de tu dominio (sin candado, para que los canales http se vean).
// Si alguien abre la copia vieja de github.io (pestaña, acceso directo o app instalada), lo mandamos allá.
const HOME = import.meta.env.VITE_HOME as string | undefined;
const oldCopy = !!HOME && location.hostname.endsWith('github.io');

if (oldCopy) {
  void navigator.serviceWorker?.getRegistrations().then((rs) => rs.forEach((r) => void r.unregister()));
  location.replace(HOME);
} else {
  render(<App />, document.getElementById('app')!);
}

if (!oldCopy && 'serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    void navigator.serviceWorker.register(`${import.meta.env.BASE_URL}sw.js`).catch(() => undefined);
  });
}
