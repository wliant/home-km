import { useThemeStore, type Theme } from '../lib/themeStore'

const order: Theme[] = ['auto', 'light', 'dark']
const labels: Record<Theme, string> = { auto: '🌗 Auto', light: '☀️ Light', dark: '🌙 Dark' }

export default function ThemeToggle() {
  const theme = useThemeStore(s => s.theme)
  const setTheme = useThemeStore(s => s.setTheme)

  function cycle() {
    const next = order[(order.indexOf(theme) + 1) % order.length]
    setTheme(next)
  }

  return (
    <button
      onClick={cycle}
      className="w-full text-left flex items-center gap-2 px-3 py-1.5 rounded-md text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700"
    >
      {labels[theme]}
    </button>
  )
}
