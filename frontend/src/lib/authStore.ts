import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserResponse } from '../types/auth'

interface AuthState {
  token: string | null
  refreshToken: string | null
  user: UserResponse | null
  expiresAt: string | null
  isAuthenticated: boolean
  setAuth: (token: string, refreshToken: string, user: UserResponse, expiresAt: string) => void
  setAccessToken: (token: string, expiresAt: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      refreshToken: null,
      user: null,
      expiresAt: null,
      isAuthenticated: false,
      setAuth: (token, refreshToken, user, expiresAt) =>
        set({ token, refreshToken, user, expiresAt, isAuthenticated: true }),
      setAccessToken: (token, expiresAt) =>
        set({ token, expiresAt }),
      clearAuth: () =>
        set({ token: null, refreshToken: null, user: null, expiresAt: null, isAuthenticated: false }),
    }),
    {
      name: 'homekm-auth',
      onRehydrateStorage: () => (state) => {
        if (state?.expiresAt && new Date(state.expiresAt) <= new Date()) {
          // Access token expired — don't clear auth, the interceptor will try to refresh
        }
      },
    },
  ),
)
