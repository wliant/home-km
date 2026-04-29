import { useId, useState } from 'react'
import { COLOR_PALETTE, contrast, contrastLevel, findSwatch } from '../lib/colorPalette'

interface ColorPickerProps {
  value: string
  onChange: (hex: string) => void
  /** Foreground used to compute the contrast warning. Defaults to white. */
  foreground?: string
  ariaLabel?: string
}

/**
 * Curated palette + escape-hatch hex input. Live contrast warning surfaces
 * when a custom hex falls below WCAG AA against the current foreground —
 * tags and folder labels render white text on the chosen colour, so AA is the
 * relevant bar.
 */
export default function ColorPicker({
  value,
  onChange,
  foreground = '#ffffff',
  ariaLabel,
}: ColorPickerProps) {
  const customId = useId()
  const swatch = findSwatch(value)
  const [showCustom, setShowCustom] = useState(swatch == null)

  const ratio = contrast(value || '#000000', foreground)
  const level = contrastLevel(ratio)

  return (
    <div className="space-y-2" role="group" aria-label={ariaLabel ?? 'Colour'}>
      <div className="flex flex-wrap gap-1.5">
        {COLOR_PALETTE.map(s => (
          <button
            key={s.bg}
            type="button"
            onClick={() => { onChange(s.bg); setShowCustom(false) }}
            aria-label={s.name}
            aria-pressed={s.bg === value.toLowerCase()}
            title={s.name}
            className={`w-7 h-7 rounded-full border ${
              s.bg === value.toLowerCase()
                ? 'border-gray-900 dark:border-gray-100 ring-2 ring-offset-2 ring-offset-white dark:ring-offset-gray-800 ring-primary-500'
                : 'border-gray-300 dark:border-gray-600'
            }`}
            style={{ backgroundColor: s.bg }}
          />
        ))}
        <button
          type="button"
          onClick={() => setShowCustom(v => !v)}
          aria-pressed={showCustom}
          className="w-7 h-7 rounded-full border border-gray-300 dark:border-gray-600 bg-gradient-to-br from-rose-400 via-amber-300 to-sky-400 text-xs font-bold text-gray-900"
          title="Custom colour"
        >
          ⋯
        </button>
      </div>

      {showCustom && (
        <div className="flex items-center gap-2">
          <label htmlFor={customId} className="text-xs text-gray-500 dark:text-gray-400">Custom</label>
          <input
            id={customId}
            type="color"
            value={value}
            onChange={e => onChange(e.target.value)}
            className="h-7 w-10 rounded border border-gray-300 dark:border-gray-600 cursor-pointer"
          />
          <span className={`text-xs ${
            level === 'fail'
              ? 'text-red-600 dark:text-red-400'
              : 'text-gray-500 dark:text-gray-400'
          }`}>
            {level === 'fail'
              ? `Low contrast (${ratio.toFixed(1)}:1) — use one of the swatches`
              : `Contrast ${ratio.toFixed(1)}:1 (${level})`}
          </span>
        </div>
      )}
    </div>
  )
}
