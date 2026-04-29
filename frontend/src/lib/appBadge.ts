/**
 * Thin wrapper over the Badging API. Supported in Chromium-based browsers
 * (Chrome, Edge, Opera) on desktop + Android, and on iOS PWAs from iOS 16.4+.
 * Calls are no-ops elsewhere.
 *
 * Always pass the absolute count — do not increment. Browsers reset the
 * badge whenever the app focuses anyway.
 */

interface BadgingNavigator {
  setAppBadge?: (count?: number) => Promise<void>
  clearAppBadge?: () => Promise<void>
}

function nav(): BadgingNavigator | null {
  if (typeof navigator === 'undefined') return null
  return navigator as unknown as BadgingNavigator
}

export function setAppBadge(count: number): void {
  const n = nav()
  if (!n?.setAppBadge) return
  // 0 → ask the platform to clear (some browsers display "0" badge otherwise).
  if (count <= 0) {
    n.clearAppBadge?.().catch(() => {})
    return
  }
  n.setAppBadge(count).catch(() => {})
}

export function clearAppBadge(): void {
  nav()?.clearAppBadge?.().catch(() => {})
}
