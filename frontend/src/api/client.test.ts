import { describe, it, expect, beforeEach } from 'vitest'
import { apiClient } from './client'
import { useAuthStore } from '../lib/authStore'
import type { AxiosHeaders, InternalAxiosRequestConfig } from 'axios'

type Handler = {
  fulfilled: (config: InternalAxiosRequestConfig) => InternalAxiosRequestConfig | Promise<InternalAxiosRequestConfig>
  rejected: (error: unknown) => unknown
}

function reqHandler(): Handler {
  const h = apiClient.interceptors.request.handlers?.[0]
  if (!h) throw new Error('no request interceptor registered')
  return h as unknown as Handler
}

function resHandler(): Handler {
  const h = apiClient.interceptors.response.handlers?.[0]
  if (!h) throw new Error('no response interceptor registered')
  return h as unknown as Handler
}

function emptyConfig(): InternalAxiosRequestConfig {
  return { headers: {} as AxiosHeaders } as InternalAxiosRequestConfig
}

describe('apiClient', () => {
  it('has correct baseURL', () => {
    expect(apiClient.defaults.baseURL).toBe('/api')
  })

  it('has JSON content-type header', () => {
    expect(apiClient.defaults.headers['Content-Type']).toBe('application/json')
  })
})

describe('request interceptor', () => {
  beforeEach(() => {
    useAuthStore.getState().clearAuth()
  })

  it('adds Authorization header when token exists', async () => {
    useAuthStore.setState({ token: 'test-jwt-123' })
    const result = await reqHandler().fulfilled(emptyConfig())
    expect((result as InternalAxiosRequestConfig).headers.Authorization).toBe('Bearer test-jwt-123')
  })

  it('does not add Authorization header when no token', async () => {
    const result = await reqHandler().fulfilled(emptyConfig())
    expect((result as InternalAxiosRequestConfig).headers.Authorization).toBeUndefined()
  })
})

describe('response interceptor', () => {
  beforeEach(() => {
    useAuthStore.setState({
      token: 'old-token',
      refreshToken: null,
      isAuthenticated: true,
      user: { id: 1, email: 'a@b.com', displayName: 'A', isAdmin: false, isChild: false, isActive: true, mfaEnabled: false, createdAt: '' },
      expiresAt: new Date(Date.now() + 3600_000).toISOString(),
    })
  })

  it('clears auth and redirects on 401 when no refresh token', async () => {
    const error = {
      config: { _retry: false },
      response: { status: 401, data: { code: 'TOKEN_EXPIRED' } },
    }

    await expect(resHandler().rejected(error) as Promise<unknown>).rejects.toBeDefined()

    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(useAuthStore.getState().token).toBeNull()
  })

  it('passes through non-401 errors', async () => {
    const error = {
      config: {},
      response: { status: 500, data: { code: 'INTERNAL_ERROR' } },
    }

    await expect(resHandler().rejected(error) as Promise<unknown>).rejects.toBeDefined()
    expect(useAuthStore.getState().isAuthenticated).toBe(true)
  })
})
