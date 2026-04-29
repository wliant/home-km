import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { DEFAULT_ACCENT_ID } from './accentPresets'

export type Theme = 'auto' | 'light' | 'dark'

interface ThemeState {
  theme: Theme
  accent: string
  setTheme: (t: Theme) => void
  setAccent: (id: string) => void
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      theme: 'auto',
      accent: DEFAULT_ACCENT_ID,
      setTheme: (theme) => set({ theme }),
      setAccent: (accent) => set({ accent }),
    }),
    { name: 'homekm-theme' },
  ),
)
