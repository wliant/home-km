import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'

vi.mock('../../components/AppLayout', () => ({
  default: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}))

vi.mock('../../api', () => ({
  adminApi: {
    getAuditLog: vi.fn(),
  },
}))

import { adminApi } from '../../api'
import AuditLogPage from './AuditLogPage'

const mockAdminApi = vi.mocked(adminApi)

function createWrapper() {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <QueryClientProvider client={qc}>
        <MemoryRouter>{children}</MemoryRouter>
      </QueryClientProvider>
    )
  }
}

describe('AuditLogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders heading and filter', async () => {
    mockAdminApi.getAuditLog.mockResolvedValue({
      content: [],
      totalPages: 0,
      totalElements: 0,
      page: 0,
      size: 25,
    })

    render(<AuditLogPage />, { wrapper: createWrapper() })

    expect(screen.getByText('Audit Log')).toBeInTheDocument()
    expect(screen.getByRole('combobox')).toBeInTheDocument()
  })

  it('shows empty message when no events', async () => {
    mockAdminApi.getAuditLog.mockResolvedValue({
      content: [],
      totalPages: 0,
      totalElements: 0,
      page: 0,
      size: 25,
    })

    render(<AuditLogPage />, { wrapper: createWrapper() })

    expect(await screen.findByText('No audit events found.')).toBeInTheDocument()
  })

  it('renders audit events in table', async () => {
    mockAdminApi.getAuditLog.mockResolvedValue({
      content: [
        { id: 1, actorUserId: 1, action: 'AUTH_LOGIN', targetType: 'user', targetId: '1', ip: '127.0.0.1', occurredAt: '2026-04-27T10:00:00Z' },
        { id: 2, actorUserId: 1, action: 'NOTE_DELETE', targetType: 'note', targetId: '5', ip: '127.0.0.1', occurredAt: '2026-04-27T10:01:00Z' },
      ],
      totalPages: 1,
      totalElements: 2,
      page: 0,
      size: 25,
    })

    render(<AuditLogPage />, { wrapper: createWrapper() })

    expect(await screen.findByText('AUTH_LOGIN')).toBeInTheDocument()
    expect(screen.getByText('NOTE_DELETE')).toBeInTheDocument()
    expect(await screen.findAllByText(/127\.0\.0\.1/)).toHaveLength(2)
  })

  it('renders pagination when multiple pages', async () => {
    mockAdminApi.getAuditLog.mockResolvedValue({
      content: [
        { id: 1, actorUserId: 1, action: 'AUTH_LOGIN', targetType: 'user', targetId: '1', ip: null, occurredAt: '2026-04-27T10:00:00Z' },
      ],
      totalPages: 3,
      totalElements: 75,
      page: 0,
      size: 25,
    })

    render(<AuditLogPage />, { wrapper: createWrapper() })

    expect(await screen.findByText('Page 1 of 3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /previous/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /next/i })).not.toBeDisabled()
  })

  it('filters by action when dropdown changed', async () => {
    mockAdminApi.getAuditLog.mockResolvedValue({
      content: [],
      totalPages: 0,
      totalElements: 0,
      page: 0,
      size: 25,
    })

    render(<AuditLogPage />, { wrapper: createWrapper() })

    await screen.findByText('No audit events found.')

    const select = screen.getByRole('combobox')
    await userEvent.selectOptions(select, 'AUTH_LOGIN')

    await waitFor(() => {
      expect(mockAdminApi.getAuditLog).toHaveBeenCalledWith(
        expect.objectContaining({ action: 'AUTH_LOGIN' })
      )
    })
  })
})
