import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type Theme = 'auto' | 'light' | 'dark'

interface ThemeState {
  theme: Theme
  setTheme: (t: Theme) => void
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      theme: 'auto',
      setTheme: (theme) => set({ theme }),
    }),
    { name: 'homekm-theme' },
  ),
)
