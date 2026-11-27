import { defineConfig } from 'vite';
import preact from '@preact/preset-vite';

// Rutas relativas: la misma web sirve en github.io/fitotv/ y en tv.puntociego.marketing/.
export default defineConfig({
  base: process.env.FITOTV_BASE ?? './',
  plugins: [preact()],
  define: { __APP_VERSION__: JSON.stringify(process.env.FITOTV_VERSION ?? '1.0.0') },
  build: { target: 'es2019', sourcemap: false },
});
