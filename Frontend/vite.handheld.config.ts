import { fileURLToPath, URL } from 'node:url'
import { createReadStream, readFileSync } from 'node:fs'
import { realpath, stat } from 'node:fs/promises'
import { extname, isAbsolute, relative, resolve, sep } from 'node:path'

import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'

/** Development-only files from a local disc extraction; never copied into the application bundle. */
export function gameDevAssets(directory = fileURLToPath(new URL('./game-dev', import.meta.url))): Plugin {
  const types: Record<string, string | undefined> = { '.png': 'image/png', '.webp': 'image/webp', '.json': 'application/json' }
  return {
    name: 'digivice-game-dev',
    apply: 'serve',
    configureServer(server) {
      server.middlewares.use(async (req, res, next) => {
        const pathname = (req.url ?? '').split('?')[0]!
        if (pathname !== '/game' && !pathname.startsWith('/game/')) return next()
        try {
          const path = decodeURIComponent(pathname.slice(6))
          if (!['GET', 'HEAD'].includes(req.method ?? '') || /[\\:%\u0000-\u001f\u007f-\u009f]/u.test(path) ||
              path.split('/').some(segment => !segment || segment === '.' || segment === '..')) throw new Error('Invalid asset path')
          const mime = types[extname(path).toLowerCase()]
          if (!mime) throw new Error('Unsupported asset type')
          const root = await realpath(directory)
          const file = await realpath(resolve(root, path))
          const local = relative(root, file)
          if (isAbsolute(local) || local === '..' || local.startsWith(`..${sep}`) || !(await stat(file)).isFile()) throw new Error('Asset outside game-dev')
          res.setHeader('Content-Type', mime)
          if (req.method === 'HEAD') return res.end()
          createReadStream(file).on('error', () => res.destroy()).pipe(res)
        } catch {
          res.statusCode = 404
          res.end()
        }
      })
    }
  }
}

/** The app's version, from the Android build (its only source), for the licences page. */
const appVersion = /versionName\s*=\s*"([^"]+)"/.exec(
  readFileSync(fileURLToPath(new URL('../Android/app/build.gradle.kts', import.meta.url)), 'utf8'))?.[1]
if (!appVersion) throw new Error('versionName not found in Android/app/build.gradle.kts')

// Handheld (Android WebView) entry: events arrive through window.__digivice.
export default defineConfig({
  plugins: [vue(), gameDevAssets()],
  define: {
    __APP_VERSION__: JSON.stringify(appVersion)
  },
  // Relative asset paths so the bundle works from any WebView origin or file location.
  base: './',
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    fs: {
      // The ?mock= simulator reads golden snapshots from Android/core test resources.
      allow: ['..']
    }
  },
  build: {
    // Keep assets auditable as files; game content is loaded separately from /game/.
    assetsInlineLimit: 0,
    outDir: 'dist-handheld',
    emptyOutDir: true,
    rollupOptions: {
      input: fileURLToPath(new URL('./handheld.html', import.meta.url)),
      output: {
        assetFileNames(asset) {
          for (const [source, folder] of [
            ['src/handheld/assets/icons/', 'icons'],
            ['src/handheld/assets/ui/', 'ui']
          ]) {
            if (asset.originalFileNames.length && asset.originalFileNames.every(file =>
              file.replaceAll('\\', '/').includes(`/${source}`) ||
              file.replaceAll('\\', '/').startsWith(source!))) {
              return `${folder}/[name]-[hash][extname]`
            }
          }
          return 'assets/[name]-[hash][extname]'
        }
      }
    }
  }
})
