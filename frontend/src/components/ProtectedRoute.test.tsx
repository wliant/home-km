import { render, screen } from '@testing-library/react'
import { describe, it, expect, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { useAuthStore } from '../lib/authStore'
import ProtectedRoute from './ProtectedRoute'

function renderWithRouter(isAuthenticated: boolean) {
  if (isAuthenticated) {
    useAuthStore.setState({
      token: 'fake-token',
      user: { id: 1, email: 'a@b.com', displayName: 'A', isAdmin: false, isChild: false, isActive: true, mfaEnabled: false, createdAt: '' },
      expiresAt: new Date(Date.now() + 86400_000).toISOString(),
      isAuthenticated: true,
    })
  } else {
    useAuthStore.setState({
      token: null, user: null, expiresAt: null, isAuthenticated: false,
    })
  }

  render(
    <MemoryRouter initialEntries={['/protected']}>
      <Routes>
        <Route path="/login" element={<div>Login Page</div>} />
        <Route
          path="/protected"
          element={
            <ProtectedRoute>
              <div>Protected Content</div>
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    useAuthStore.setState({
      token: null, user: null, expiresAt: null, isAuthenticated: false,
    })
  })

  it('redirects to /login when not authenticated', () => {
    renderWithRouter(false)
    expect(screen.getByText('Login Page')).toBeInTheDocument()
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
  })

  it('renders children when authenticated', () => {
    renderWithRouter(true)
    expect(screen.getByText('Protected Content')).toBeInTheDocument()
    expect(screen.queryByText('Login Page')).not.toBeInTheDocument()
  })
})
