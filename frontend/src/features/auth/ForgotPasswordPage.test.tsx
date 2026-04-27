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
import ForgotPasswordPage from './ForgotPasswordPage'

const mockApiClient = vi.mocked(apiClient)

function wrapper({ children }: { children: React.ReactNode }) {
  return <MemoryRouter>{children}</MemoryRouter>
}

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders email form', () => {
    render(<ForgotPasswordPage />, { wrapper })

    expect(screen.getByText('Email')).toBeInTheDocument()
    expect(screen.getByRole('textbox')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /send reset link/i })).toBeInTheDocument()
  })

  it('shows success message after submit', async () => {
    mockApiClient.post.mockResolvedValue({ data: {} })

    render(<ForgotPasswordPage />, { wrapper })

    await userEvent.type(screen.getByRole('textbox'), 'user@example.com')
    await userEvent.click(screen.getByRole('button', { name: /send reset link/i }))

    await waitFor(() =>
      expect(screen.getByText(/if an account with that email exists/i)).toBeInTheDocument()
    )
  })
})
