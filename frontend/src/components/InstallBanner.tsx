import { useEffect, useState } from 'react'

const DISMISS_KEY = 'install-banner-dismissed-at'
const VISIT_COUNT_KEY = 'install-banner-visit-count'
const DISMISS_COOLDOWN_DAYS = 14
const ENGAGEMENT_VISITS = 3

interface BeforeInstallPromptEvent extends Event {
  readonly platforms: string[]
  prompt(): Promise<void>
  readonly userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

function isStandalone(): boolean {
  return (
    window.matchMedia('(display-mode: standalone)').matches ||
    (window.navigator as { standalone?: boolean }).standalone === true
  )
}

function isIOS(): boolean {
  return /iPad|iPhone|iPod/.test(navigator.userAgent)
}

function isDismissed(): boolean {
  const at = localStorage.getItem(DISMISS_KEY)
  if (!at) return false
  const ageDays = (Date.now() - Number(at)) / (1000 * 60 * 60 * 24)
  return ageDays < DISMISS_COOLDOWN_DAYS
}

function bumpVisitCount(): number {
  const next = Number(localStorage.getItem(VISIT_COUNT_KEY) ?? '0') + 1
  localStorage.setItem(VISIT_COUNT_KEY, String(next))
  return next
}

/**
 * Cross-platform install prompt. On Chromium-based browsers it captures
 * {@code beforeinstallprompt} and renders an Install button that invokes the
 * native dialog. On iOS Safari it falls back to a manual hint pointing at the
 * Share menu. Suppresses for 14 days after dismissal and only appears after
 * the user has visited at least 3 times — first-load nag is the worst kind.
 */
export default function InstallBanner() {
  const [installEvent, setInstallEvent] = useState<BeforeInstallPromptEvent | null>(null)
  const [dismissed, setDismissed] = useState(() => isDismissed())
  const [visits, setVisits] = useState(0)
  const standalone = typeof window !== 'undefined' && isStandalone()

  useEffect(() => {
    setVisits(bumpVisitCount())
  }, [])

  useEffect(() => {
    if (standalone || dismissed) return
    function onBeforeInstall(e: Event) {
      e.preventDefault()
      setInstallEvent(e as BeforeInstallPromptEvent)
    }
    window.addEventListener('beforeinstallprompt', onBeforeInstall)
    return () => window.removeEventListener('beforeinstallprompt', onBeforeInstall)
  }, [standalone, dismissed])

  function dismiss() {
    localStorage.setItem(DISMISS_KEY, String(Date.now()))
    setDismissed(true)
  }

  async function install() {
    if (!installEvent) return
    await installEvent.prompt()
    const choice = await installEvent.userChoice
    if (choice.outcome === 'accepted') {
      setInstallEvent(null)
    } else {
      dismiss()
    }
  }

  if (standalone) return null
  if (dismissed) return null
  if (visits < ENGAGEMENT_VISITS) return null

  // Prefer the native Chromium prompt when available; fall back to iOS hint.
  if (installEvent) {
    return (
      <div className="flex items-center gap-3 bg-blue-50 dark:bg-blue-900/30 border-b border-blue-200 dark:border-blue-800 px-4 py-3 text-sm text-blue-900 dark:text-blue-200">
        <span className="text-xl shrink-0" aria-hidden="true">📲</span>
        <p className="flex-1 leading-snug">Install Home KM for faster access and offline support.</p>
        <button
          onClick={install}
          className="shrink-0 px-3 py-1 rounded-md bg-blue-600 text-white text-xs font-semibold hover:bg-blue-700"
        >
          Install
        </button>
        <button
          onClick={dismiss}
          aria-label="Dismiss install prompt"
          className="shrink-0 text-blue-500 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 text-base leading-none"
        >
          ×
        </button>
      </div>
    )
  }

  if (isIOS()) {
    return (
      <div className="flex items-start gap-3 bg-blue-50 dark:bg-blue-900/30 border-b border-blue-200 dark:border-blue-800 px-4 py-3 text-sm text-blue-900 dark:text-blue-200">
        <span className="text-xl shrink-0" aria-hidden="true">📲</span>
        <p className="flex-1 leading-snug">
          Install this app: tap <strong>Share</strong> then <strong>Add to Home Screen</strong>.
        </p>
        <button
          onClick={dismiss}
          aria-label="Dismiss install prompt"
          className="shrink-0 text-blue-500 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 text-base leading-none"
        >
          ×
        </button>
      </div>
    )
  }

  return null
}
