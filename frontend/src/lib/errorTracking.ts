// Frontend error tracking — opt-in via VITE_SENTRY_DSN at build time + runtime user opt-out.
// We do not import @sentry/react here unless the DSN is set; this keeps the production bundle
// smaller for self-hosters who don't want any third-party JS.

const DSN = import.meta.env.VITE_SENTRY_DSN as string | undefined
const STORAGE_KEY = 'homekm-error-tracking-opted-out'

// Resolved at init() time when Sentry is available; remains undefined when
// the DSN is unset, the user opted out, or the optional dep isn't installed.
// captureException() and the web-vitals listener fall back to console output.
// Typed loosely because the dep is optional — TypeScript can't see it unless
// installed. Errors are caught at runtime via try/catch around dynamic import.
type SentryModule = {
  init: (opts: Record<string, unknown>) => void
  captureException: (e: unknown, ctx?: unknown) => void
  captureMessage: (msg: string, level?: string) => void
  setMeasurement: (name: string, value: number, unit: string) => void
}
let sentry: SentryModule | undefined

export function isEnabled(): boolean {
  if (!DSN) return false
  try {
    return localStorage.getItem(STORAGE_KEY) !== '1'
  } catch {
    return false
  }
}

export function setOptedOut(optedOut: boolean): void {
  try {
    if (optedOut) localStorage.setItem(STORAGE_KEY, '1')
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    /* ignore */
  }
}

export async function init(): Promise<void> {
  if (!isEnabled()) return
  try {
    // @ts-expect-error optional dep — install @sentry/react to enable
    const mod = (await import(/* @vite-ignore */ '@sentry/react')) as unknown as SentryModule
    mod.init({
      dsn: DSN,
      sendDefaultPii: false,
      tracesSampleRate: 0.1,
      release: import.meta.env.VITE_RELEASE_VERSION,
      environment: import.meta.env.MODE,
      ignoreErrors: [
        'ResizeObserver loop limit exceeded',
        'Non-Error promise rejection captured',
      ],
    })
    sentry = mod
    void reportWebVitals(mod)
  } catch {
    // Sentry is an optional dep; if it isn't installed, we silently skip.
  }
}

/**
 * Forward to Sentry when initialized; degrade to console otherwise so a
 * crashing ErrorBoundary still leaves a trace in dev tools.
 */
export function captureException(error: unknown, componentStack?: string): void {
  if (sentry) {
    sentry.captureException(error, componentStack ? { contexts: { react: { componentStack } } } : undefined)
    return
  }
  // eslint-disable-next-line no-console
  console.error('[ErrorBoundary]', error, componentStack)
}

/**
 * Streams Core Web Vitals (CLS / INP / LCP / FCP / TTFB) into Sentry as
 * measurements on the active transaction. Loads web-vitals lazily so
 * non-Sentry builds don't pull it.
 */
async function reportWebVitals(mod: SentryModule): Promise<void> {
  try {
    type Listener = (cb: (m: { value: number; rating?: string }) => void) => void
    type WebVitals = { onCLS: Listener; onINP: Listener; onLCP: Listener; onFCP: Listener; onTTFB: Listener }
    // @ts-expect-error optional dep — install web-vitals to enable
    const wv = (await import(/* @vite-ignore */ 'web-vitals')) as unknown as WebVitals
    const send = (name: string) => (metric: { value: number; rating?: string }) => {
      const unit = name === 'CLS' ? '' : 'millisecond'
      mod.setMeasurement(name, metric.value, unit)
      if (metric.rating === 'poor') {
        mod.captureMessage(`web-vital ${name} poor (${metric.value})`, 'warning')
      }
    }
    wv.onCLS(send('CLS'))
    wv.onINP(send('INP'))
    wv.onLCP(send('LCP'))
    wv.onFCP(send('FCP'))
    wv.onTTFB(send('TTFB'))
  } catch {
    /* web-vitals optional too */
  }
}
