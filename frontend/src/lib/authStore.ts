import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserResponse } from '../types/auth'

interface AuthState {
  token: string | null
  user: UserResponse | null
  expiresAt: string | null
  isAuthenticated: boolean
  setAuth: (token: string, user: UserResponse, expiresAt: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      expiresAt: null,
      isAuthenticated: false,
      setAuth: (token, user, expiresAt) =>
        set({ token, user, expiresAt, isAuthenticated: true }),
      clearAuth: () =>
        set({ token: null, user: null, expiresAt: null, isAuthenticated: false }),
    }),
    {
      name: 'homekm-auth',
      onRehydrateStorage: () => (state) => {
        if (state?.expiresAt && new Date(state.expiresAt) <= new Date()) {
          state.clearAuth()
        }
      },
    },
  ),
)
