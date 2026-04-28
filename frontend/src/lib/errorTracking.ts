// Frontend error tracking — opt-in via VITE_SENTRY_DSN at build time + runtime user opt-out.
// We do not import @sentry/react here unless the DSN is set; this keeps the production bundle
// smaller for self-hosters who don't want any third-party JS.

const DSN = import.meta.env.VITE_SENTRY_DSN as string | undefined
const STORAGE_KEY = 'homekm-error-tracking-opted-out'

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
    const Sentry = await import(/* @vite-ignore */ '@sentry/react')
    Sentry.init({
      dsn: DSN,
      tracesSampleRate: 0.1,
      release: import.meta.env.VITE_RELEASE_VERSION,
      environment: import.meta.env.MODE,
    })
  } catch {
    // Sentry is an optional dep; if it isn't installed, we silently skip.
  }
}
