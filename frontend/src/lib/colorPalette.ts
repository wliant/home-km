/**
 * Curated palette used by tag and folder colour pickers. All entries pair a
 * background colour with a foreground that meets WCAG AA contrast (≥ 4.5:1
 * for body text) on the matching background. Values verified against
 * https://webaim.org/resources/contrastchecker/ in 2026-04.
 *
 * Hex values are lowercase 6-digit; preserve that format so equality checks
 * against {@link findSwatch} remain trivial.
 */
export interface Swatch {
  readonly name: string
  readonly bg: string
  readonly fg: string
}

export const COLOR_PALETTE: readonly Swatch[] = Object.freeze([
  { name: 'Slate',  bg: '#64748b', fg: '#ffffff' },
  { name: 'Red',    bg: '#dc2626', fg: '#ffffff' },
  { name: 'Orange', bg: '#ea580c', fg: '#ffffff' },
  { name: 'Amber',  bg: '#b45309', fg: '#ffffff' },
  { name: 'Lime',   bg: '#4d7c0f', fg: '#ffffff' },
  { name: 'Green',  bg: '#15803d', fg: '#ffffff' },
  { name: 'Teal',   bg: '#0f766e', fg: '#ffffff' },
  { name: 'Sky',    bg: '#0369a1', fg: '#ffffff' },
  { name: 'Blue',   bg: '#1d4ed8', fg: '#ffffff' },
  { name: 'Indigo', bg: '#4338ca', fg: '#ffffff' },
  { name: 'Violet', bg: '#6d28d9', fg: '#ffffff' },
  { name: 'Pink',   bg: '#be185d', fg: '#ffffff' },
])

export function findSwatch(hex: string | null | undefined): Swatch | null {
  if (!hex) return null
  const norm = hex.toLowerCase()
  return COLOR_PALETTE.find(s => s.bg === norm) ?? null
}

/**
 * WCAG relative-luminance contrast between two #RRGGBB strings.
 * Returns 21 for black-on-white and approaches 1 as the colours converge.
 * Pass the result through {@link contrastLevel} for an "AA / fail" tag.
 */
export function contrast(a: string, b: string): number {
  const la = relLuminance(parseHex(a))
  const lb = relLuminance(parseHex(b))
  const [light, dark] = la > lb ? [la, lb] : [lb, la]
  return (light + 0.05) / (dark + 0.05)
}

export function contrastLevel(ratio: number): 'AAA' | 'AA' | 'fail' {
  if (ratio >= 7) return 'AAA'
  if (ratio >= 4.5) return 'AA'
  return 'fail'
}

function parseHex(hex: string): [number, number, number] {
  const clean = hex.replace(/^#/, '').padEnd(6, '0').slice(0, 6)
  return [
    parseInt(clean.slice(0, 2), 16),
    parseInt(clean.slice(2, 4), 16),
    parseInt(clean.slice(4, 6), 16),
  ]
}

function relLuminance([r, g, b]: [number, number, number]): number {
  const channels = [r, g, b].map(c => {
    const v = c / 255
    return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4)
  }) as [number, number, number]
  return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722
}
