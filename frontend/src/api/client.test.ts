import { describe, it, expect, vi, beforeEach } from 'vitest'
import { apiClient } from './client'
import { useAuthStore } from '../lib/authStore'

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

  it('adds Authorization header when token exists', () => {
    useAuthStore.setState({ token: 'test-jwt-123' })

    // Simulate what the request interceptor does
    const config = { headers: {} as Record<string, string> }
    const interceptor = apiClient.interceptors.request.handlers[0]
    const result = interceptor.fulfilled(config)

    expect(result.headers.Authorization).toBe('Bearer test-jwt-123')
  })

  it('does not add Authorization header when no token', () => {
    const config = { headers: {} as Record<string, string> }
    const interceptor = apiClient.interceptors.request.handlers[0]
    const result = interceptor.fulfilled(config)

    expect(result.headers.Authorization).toBeUndefined()
  })
})

describe('response interceptor', () => {
  beforeEach(() => {
    useAuthStore.setState({
      token: 'old-token',
      refreshToken: null,
      isAuthenticated: true,
      user: { id: 1, email: 'a@b.com', displayName: 'A', isAdmin: false, isChild: false, isActive: true, createdAt: '' },
      expiresAt: new Date(Date.now() + 3600_000).toISOString(),
    })
  })

  it('clears auth and redirects on 401 when no refresh token', async () => {
    const originalHref = window.location.href
    const interceptor = apiClient.interceptors.response.handlers[0]

    const error = {
      config: { _retry: false },
      response: { status: 401, data: { code: 'TOKEN_EXPIRED' } },
    }

    await expect(interceptor.rejected(error)).rejects.toBeDefined()

    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(useAuthStore.getState().token).toBeNull()
  })

  it('passes through non-401 errors', async () => {
    const interceptor = apiClient.interceptors.response.handlers[0]

    const error = {
      config: {},
      response: { status: 500, data: { code: 'INTERNAL_ERROR' } },
    }

    await expect(interceptor.rejected(error)).rejects.toBeDefined()
    // Auth should not be cleared on 500
    expect(useAuthStore.getState().isAuthenticated).toBe(true)
  })
})
