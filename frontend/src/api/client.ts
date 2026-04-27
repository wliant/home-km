import axios from 'axios'
import { useAuthStore } from '../lib/authStore'

export const apiClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshPromise: Promise<string> | null = null

apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    const status = error.response?.status
    const code = error.response?.data?.code

    if (status === 401 && (code === 'TOKEN_EXPIRED' || code === 'TOKEN_REVOKED') && !original._retry) {
      original._retry = true

      const { refreshToken } = useAuthStore.getState()
      if (!refreshToken) {
        useAuthStore.getState().clearAuth()
        window.location.href = '/login'
        return Promise.reject(error)
      }

      try {
        // Deduplicate concurrent refresh calls
        if (!refreshPromise) {
          refreshPromise = axios
            .post<{ token: string; refreshToken: string; expiresAt: string }>('/api/auth/refresh', { refreshToken })
            .then((r) => {
              const { token, refreshToken: newRefresh, expiresAt } = r.data
              useAuthStore.getState().setAuth(token, newRefresh, useAuthStore.getState().user!, expiresAt)
              return token
            })
            .finally(() => { refreshPromise = null })
        }

        const newToken = await refreshPromise
        original.headers.Authorization = `Bearer ${newToken}`
        return apiClient(original)
      } catch {
        useAuthStore.getState().clearAuth()
        window.location.href = '/login'
        return Promise.reject(error)
      }
    }

    return Promise.reject(error)
  },
)
