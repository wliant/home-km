/**
 * Named accent palettes. Each entry is a Tailwind-shaped 50→900 scale that
 * gets pushed into the {@code --primary-N} CSS custom properties at runtime.
 * Tailwind's {@code primary-N} utilities resolve to those vars, so changing
 * the active preset retints every existing button/link without a rebuild.
 *
 * Adding a preset: pick any Tailwind palette
 * (https://tailwindcss.com/docs/customizing-colors) and paste its scale here.
 * Each shade must be the literal "R G B" triple (Tailwind's
 * {@code <alpha-value>} substitution requires the channel-list form).
 */
export type AccentScale = Readonly<Record<50 | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900, string>>

export interface AccentPreset {
  readonly id: string
  readonly name: string
  readonly swatch: string  // 600 hex, used in the picker
  readonly scale: AccentScale
}

export const ACCENT_PRESETS: readonly AccentPreset[] = [
  {
    id: 'indigo',
    name: 'Indigo',
    swatch: '#4f46e5',
    scale: {
      50: '238 242 255', 100: '224 231 255', 200: '199 210 254', 300: '165 180 252',
      400: '129 140 248', 500: '99 102 241', 600: '79 70 229', 700: '67 56 202',
      800: '55 48 163', 900: '49 46 129',
    },
  },
  {
    id: 'blue',
    name: 'Blue',
    swatch: '#2563eb',
    scale: {
      50: '239 246 255', 100: '219 234 254', 200: '191 219 254', 300: '147 197 253',
      400: '96 165 250', 500: '59 130 246', 600: '37 99 235', 700: '29 78 216',
      800: '30 64 175', 900: '30 58 138',
    },
  },
  {
    id: 'emerald',
    name: 'Emerald',
    swatch: '#059669',
    scale: {
      50: '236 253 245', 100: '209 250 229', 200: '167 243 208', 300: '110 231 183',
      400: '52 211 153', 500: '16 185 129', 600: '5 150 105', 700: '4 120 87',
      800: '6 95 70', 900: '6 78 59',
    },
  },
  {
    id: 'rose',
    name: 'Rose',
    swatch: '#e11d48',
    scale: {
      50: '255 241 242', 100: '255 228 230', 200: '254 205 211', 300: '253 164 175',
      400: '251 113 133', 500: '244 63 94', 600: '225 29 72', 700: '190 18 60',
      800: '159 18 57', 900: '136 19 55',
    },
  },
  {
    id: 'amber',
    name: 'Amber',
    swatch: '#d97706',
    scale: {
      50: '255 251 235', 100: '254 243 199', 200: '253 230 138', 300: '252 211 77',
      400: '251 191 36', 500: '245 158 11', 600: '217 119 6', 700: '180 83 9',
      800: '146 64 14', 900: '120 53 15',
    },
  },
] as const

export const DEFAULT_ACCENT_ID = 'indigo'

export function findAccent(id: string): AccentPreset {
  return ACCENT_PRESETS.find(p => p.id === id) ?? ACCENT_PRESETS[0]
}

/**
 * Push the chosen scale into CSS variables so Tailwind's primary-* utilities
 * pick it up. Idempotent — calling with the same preset is a no-op.
 */
export function applyAccent(id: string): void {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  const preset = findAccent(id)
  for (const [shade, rgb] of Object.entries(preset.scale)) {
    root.style.setProperty(`--primary-${shade}`, rgb)
  }
  root.dataset.accent = preset.id
}
