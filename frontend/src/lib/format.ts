import i18n from '../i18n'

/**
 * Locale-aware formatting helpers. All call sites should route through these
 * instead of {@code Date#toLocaleString()} directly so a single locale change
 * (Settings → Language) immediately retints every rendered date and number,
 * and so currency / unit choices stay consistent across the app.
 *
 * Locale resolution order: i18next active language → browser default. The
 * Intl.* APIs are tolerant of unknown locales (fall back to the closest
 * match), so a misconfigured user does not break formatting.
 */

function locale(): string {
  return i18n.resolvedLanguage || i18n.language || 'en'
}

/**
 * "27 Apr 2026" / "Apr 27, 2026" depending on locale. Pass {@code withTime}
 * for "27 Apr 2026, 14:32".
 */
export function formatDate(input: string | number | Date, withTime = false): string {
  const date = input instanceof Date ? input : new Date(input)
  return new Intl.DateTimeFormat(locale(), {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    ...(withTime ? { hour: '2-digit', minute: '2-digit' } : {}),
  }).format(date)
}

/**
 * "5 minutes ago", "in 2 days". {@code refMs} defaults to now.
 */
export function formatRelative(input: string | number | Date, refMs: number = Date.now()): string {
  const target = (input instanceof Date ? input : new Date(input)).getTime()
  const diffSeconds = Math.round((target - refMs) / 1000)
  const rtf = new Intl.RelativeTimeFormat(locale(), { numeric: 'auto' })

  const ranges: [Intl.RelativeTimeFormatUnit, number][] = [
    ['year', 60 * 60 * 24 * 365],
    ['month', 60 * 60 * 24 * 30],
    ['week', 60 * 60 * 24 * 7],
    ['day', 60 * 60 * 24],
    ['hour', 60 * 60],
    ['minute', 60],
    ['second', 1],
  ]
  for (const [unit, seconds] of ranges) {
    if (Math.abs(diffSeconds) >= seconds || unit === 'second') {
      return rtf.format(Math.round(diffSeconds / seconds), unit)
    }
  }
  return rtf.format(0, 'second')
}

/**
 * "1,234" / "1.234" depending on locale.
 */
export function formatNumber(value: number): string {
  return new Intl.NumberFormat(locale()).format(value)
}

/**
 * Localised file size string. Uses binary units (1 KiB = 1024 B) — matches
 * what users see in macOS Finder and Windows Explorer.
 */
export function formatBytes(bytes: number): string {
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let n = bytes
  while (n >= 1024 && i < units.length - 1) {
    n /= 1024
    i++
  }
  const formatted = i === 0
    ? new Intl.NumberFormat(locale()).format(n)
    : new Intl.NumberFormat(locale(), { maximumFractionDigits: 1 }).format(n)
  return `${formatted} ${units[i]}`
}

/**
 * Currency formatter. Caller passes the currency code; we honour the
 * user's locale for separators and symbol position.
 */
export function formatCurrency(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat(locale(), { style: 'currency', currency }).format(value)
}
