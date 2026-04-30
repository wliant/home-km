import { describe, it, expect, beforeEach } from 'vitest'
import { useAuthStore } from './authStore'

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.getState().clearAuth()
  })

  it('starts unauthenticated', () => {
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(useAuthStore.getState().token).toBeNull()
    expect(useAuthStore.getState().user).toBeNull()
  })

  it('setAuth stores token and marks authenticated', () => {
    const user = { id: 1, email: 'a@b.com', displayName: 'A', isAdmin: false, isChild: false, isActive: true, mfaEnabled: false, createdAt: '' }
    const expiresAt = new Date(Date.now() + 3600_000).toISOString()

    useAuthStore.getState().setAuth('my-token', 'refresh-tok', user, expiresAt)

    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(true)
    expect(state.token).toBe('my-token')
    expect(state.user?.email).toBe('a@b.com')
    expect(state.expiresAt).toBe(expiresAt)
  })

  it('clearAuth resets all fields', () => {
    const user = { id: 1, email: 'a@b.com', displayName: 'A', isAdmin: false, isChild: false, isActive: true, mfaEnabled: false, createdAt: '' }
    useAuthStore.getState().setAuth('tok', 'rt', user, new Date(Date.now() + 3600_000).toISOString())
    useAuthStore.getState().clearAuth()

    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(false)
    expect(state.token).toBeNull()
    expect(state.user).toBeNull()
    expect(state.expiresAt).toBeNull()
  })

  it('setAuth with admin user preserves isAdmin flag', () => {
    const admin = { id: 2, email: 'admin@b.com', displayName: 'Admin', isAdmin: true, isChild: false, isActive: true, mfaEnabled: false, createdAt: '' }
    useAuthStore.getState().setAuth('tok2', 'rt2', admin, new Date(Date.now() + 3600_000).toISOString())

    expect(useAuthStore.getState().user?.isAdmin).toBe(true)
  })
})
