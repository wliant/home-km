import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'

vi.mock('../../api/authApi', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    getMe: vi.fn(),
    updateMe: vi.fn(),
  },
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual as object, useNavigate: () => vi.fn() }
})

import { authApi } from '../../api/authApi'
import LoginPage from './LoginPage'

const mockAuthApi = vi.mocked(authApi)

function wrapper({ children }: { children: React.ReactNode }) {
  return <MemoryRouter>{children}</MemoryRouter>
}

function getEmailInput(container: HTMLElement) {
  return container.querySelector('input[name="email"]') as HTMLInputElement
}

function getPasswordInput(container: HTMLElement) {
  return container.querySelector('input[name="password"]') as HTMLInputElement
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders email, password inputs and submit button', () => {
    const { container } = render(<LoginPage />, { wrapper })
    expect(getEmailInput(container)).toBeInTheDocument()
    expect(getPasswordInput(container)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  it('shows validation error when email is empty', async () => {
    render(<LoginPage />, { wrapper })
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }))
    await waitFor(() =>
      expect(screen.getByText(/email is required/i)).toBeInTheDocument()
    )
  })

  it('shows validation error when password is empty', async () => {
    const { container } = render(<LoginPage />, { wrapper })
    await userEvent.type(getEmailInput(container), 'a@b.com')
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }))
    await waitFor(() =>
      expect(screen.getByText(/password is required/i)).toBeInTheDocument()
    )
  })

  it('calls authApi.login with form values on valid submit', async () => {
    mockAuthApi.login.mockResolvedValue({
      token: 'tok',
      refreshToken: 'rt',
      user: { id: 1, email: 'a@b.com', displayName: 'A', isAdmin: false, isChild: false, isActive: true, createdAt: '' },
      expiresAt: new Date(Date.now() + 86400_000).toISOString(),
    })
    const { container } = render(<LoginPage />, { wrapper })
    await userEvent.type(getEmailInput(container), 'a@b.com')
    await userEvent.type(getPasswordInput(container), 'secret123')
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }))
    await waitFor(() =>
      expect(mockAuthApi.login).toHaveBeenCalledWith(
        expect.objectContaining({ email: 'a@b.com', password: 'secret123' }),
      ),
    )
  })

  it('shows API error message on failed login', async () => {
    mockAuthApi.login.mockRejectedValue({
      response: { data: { code: 'INVALID_CREDENTIALS' } },
    })
    const { container } = render(<LoginPage />, { wrapper })
    await userEvent.type(getEmailInput(container), 'a@b.com')
    await userEvent.type(getPasswordInput(container), 'wrongpass')
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }))
    await waitFor(() =>
      expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument()
    )
  })
})
