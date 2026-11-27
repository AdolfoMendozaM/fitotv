import { defineConfig } from 'vite';
import preact from '@preact/preset-vite';

// En GitHub Pages la web vive en /fitotv/. Para otro dominio: FITOTV_BASE=/ npm run build
export default defineConfig({
  base: process.env.FITOTV_BASE ?? '/fitotv/',
  plugins: [preact()],
  define: { __APP_VERSION__: JSON.stringify(process.env.FITOTV_VERSION ?? '1.0.0') },
  build: { target: 'es2019', sourcemap: false },
});
