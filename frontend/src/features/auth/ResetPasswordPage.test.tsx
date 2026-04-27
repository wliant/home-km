import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'

vi.mock('../../api/client', () => ({
  apiClient: {
    post: vi.fn(),
  },
}))

import { apiClient } from '../../api/client'
import ResetPasswordPage from './ResetPasswordPage'

const mockApiClient = vi.mocked(apiClient)

function renderWithRouter(initialEntries: string[]) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <ResetPasswordPage />
    </MemoryRouter>
  )
}

function getPasswordInputs() {
  const inputs = document.querySelectorAll('input[type="password"]')
  return { newPw: inputs[0] as HTMLInputElement, confirmPw: inputs[1] as HTMLInputElement }
}

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows error when token is missing', () => {
    renderWithRouter(['/reset-password'])

    expect(screen.getByText('Invalid or missing reset token.')).toBeInTheDocument()
    expect(screen.getByText(/request a new reset link/i)).toBeInTheDocument()
  })

  it('renders password form when token present', () => {
    renderWithRouter(['/reset-password?token=abc'])

    expect(screen.getByText('Set new password')).toBeInTheDocument()
    expect(screen.getByText('New password')).toBeInTheDocument()
    expect(screen.getByText('Confirm password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /reset password/i })).toBeInTheDocument()
  })

  it('shows validation errors for weak password', async () => {
    renderWithRouter(['/reset-password?token=abc'])

    const { newPw, confirmPw } = getPasswordInputs()
    await userEvent.type(newPw, 'weak')
    await userEvent.type(confirmPw, 'weak')
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }))

    await waitFor(() => {
      expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument()
    })
  })

  it('shows mismatch error when passwords differ', async () => {
    renderWithRouter(['/reset-password?token=abc'])

    const { newPw, confirmPw } = getPasswordInputs()
    await userEvent.type(newPw, 'StrongPass1')
    await userEvent.type(confirmPw, 'Different1')
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }))

    await waitFor(() => {
      expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument()
    })
  })

  it('submits and shows success on valid input', async () => {
    mockApiClient.post.mockResolvedValue({ data: {} })

    renderWithRouter(['/reset-password?token=test-token'])

    const { newPw, confirmPw } = getPasswordInputs()
    await userEvent.type(newPw, 'ValidPass1')
    await userEvent.type(confirmPw, 'ValidPass1')
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }))

    await waitFor(() => {
      expect(screen.getByText(/password updated/i)).toBeInTheDocument()
    })

    expect(mockApiClient.post).toHaveBeenCalledWith('/auth/password-reset/confirm', {
      token: 'test-token',
      newPassword: 'ValidPass1',
    })
  })

  it('shows error message on API failure', async () => {
    mockApiClient.post.mockRejectedValue({
      response: { data: { code: 'RESET_TOKEN_EXPIRED' } },
    })

    renderWithRouter(['/reset-password?token=expired-token'])

    const { newPw, confirmPw } = getPasswordInputs()
    await userEvent.type(newPw, 'ValidPass1')
    await userEvent.type(confirmPw, 'ValidPass1')
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }))

    await waitFor(() => {
      expect(screen.getByText(/expired/i)).toBeInTheDocument()
    })
  })
})
