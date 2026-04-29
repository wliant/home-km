import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import { visualizer } from 'rollup-plugin-visualizer'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const appName = env.VITE_APP_NAME || 'Home KM'
  const themeColor = env.VITE_APP_THEME_COLOR || '#6366f1'
  const analyze = process.env.ANALYZE === '1'

  return {
    plugins: [
      react(),
      analyze && visualizer({
        filename: 'dist/stats.html',
        gzipSize: true,
        brotliSize: true,
        template: 'treemap',
      }),
      VitePWA({
        registerType: 'autoUpdate',
        strategies: 'injectManifest',
        srcDir: 'src',
        filename: 'sw.ts',
        manifest: {
          name: appName,
          short_name: appName,
          theme_color: themeColor,
          background_color: '#ffffff',
          display: 'standalone',
          // Lets desktop installs use the title-bar overlay when supported,
          // falling back to standalone everywhere else.
          display_override: ['window-controls-overlay', 'standalone'],
          start_url: '/',
          icons: [
            { src: '/icon-192.png', sizes: '192x192', type: 'image/png' },
            { src: '/icon-512.png', sizes: '512x512', type: 'image/png' },
            { src: '/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
          ],
          // Receive shared text/links from other apps. /share reads the
          // query params and forwards into the note editor. File-based shares
          // require a SW POST interceptor (deferred — users can still upload
          // via the regular UI).
          share_target: {
            action: '/share',
            method: 'GET',
            params: {
              title: 'title',
              text: 'text',
              url: 'url',
            },
          },
        },
        injectManifest: {
          globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
        },
      }),
    ],
    define: {
      'import.meta.env.VITE_APP_NAME': JSON.stringify(appName),
      'import.meta.env.VITE_APP_THEME_COLOR': JSON.stringify(themeColor),
    },
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
