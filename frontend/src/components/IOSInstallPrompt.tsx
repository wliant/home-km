import { useState } from 'react'

export default function IOSInstallPrompt() {
  const [dismissed, setDismissed] = useState(
    () => sessionStorage.getItem('ios-prompt-dismissed') === '1',
  )

  const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent)
  const isStandalone =
    window.matchMedia('(display-mode: standalone)').matches ||
    (window.navigator as { standalone?: boolean }).standalone === true

  if (!isIOS || isStandalone || dismissed) return null

  function dismiss() {
    sessionStorage.setItem('ios-prompt-dismissed', '1')
    setDismissed(true)
  }

  return (
    <div className="flex items-start gap-3 bg-blue-50 dark:bg-blue-900/30 border-b border-blue-200 dark:border-blue-800 px-4 py-3 text-sm text-blue-900 dark:text-blue-200">
      <span className="text-xl shrink-0">📲</span>
      <p className="flex-1 leading-snug">
        Install this app: tap <strong>Share</strong> then <strong>Add to Home Screen</strong>.
      </p>
      <button
        onClick={dismiss}
        aria-label="Dismiss"
        className="shrink-0 text-blue-500 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 text-base leading-none"
      >
        ✕
      </button>
    </div>
  )
}
