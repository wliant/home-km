import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'

vi.mock('../../components/AppLayout', () => ({
  default: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}))

vi.mock('../../api', () => ({
  trashApi: {
    list: vi.fn(),
    restoreNote: vi.fn(),
    restoreFile: vi.fn(),
    restoreFolder: vi.fn(),
  },
}))

import { trashApi } from '../../api'
import { useAuthStore } from '../../lib/authStore'
import TrashPage from './TrashPage'

const mockTrashApi = vi.mocked(trashApi)

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

describe('TrashPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({
      token: 'tok',
      refreshToken: 'rt',
      user: { id: 1, email: 'a@b.com', displayName: 'A', isAdmin: false, isChild: false, isActive: true, createdAt: '' },
      expiresAt: new Date(Date.now() + 3600_000).toISOString(),
      isAuthenticated: true,
    })
  })

  it('renders trash items grouped by type', async () => {
    mockTrashApi.list.mockResolvedValue({
      notes: [{ id: 1, type: 'note', name: 'My Note', deletedAt: '2026-04-20T00:00:00Z' }],
      files: [{ id: 2, type: 'file', name: 'photo.jpg', deletedAt: '2026-04-21T00:00:00Z' }],
      folders: [],
    })

    render(<TrashPage />, { wrapper: createWrapper() })

    expect(await screen.findByText('My Note')).toBeInTheDocument()
    expect(screen.getByText('photo.jpg')).toBeInTheDocument()
    expect(screen.getByText('Notes')).toBeInTheDocument()
    expect(screen.getByText('Files')).toBeInTheDocument()
  })

  it('shows empty message when trash is empty', async () => {
    mockTrashApi.list.mockResolvedValue({
      notes: [],
      files: [],
      folders: [],
    })

    render(<TrashPage />, { wrapper: createWrapper() })

    expect(await screen.findByText('Trash is empty.')).toBeInTheDocument()
  })

  it('shows 30-day retention notice', async () => {
    mockTrashApi.list.mockResolvedValue({ notes: [], files: [], folders: [] })

    render(<TrashPage />, { wrapper: createWrapper() })

    expect(await screen.findByText(/permanently deleted after 30 days/i)).toBeInTheDocument()
  })

  it('calls restoreNote when Restore button is clicked', async () => {
    mockTrashApi.list.mockResolvedValue({
      notes: [{ id: 5, type: 'note', name: 'Deleted Note', deletedAt: '2026-04-20T00:00:00Z' }],
      files: [],
      folders: [],
    })
    mockTrashApi.restoreNote.mockResolvedValue({} as never)

    render(<TrashPage />, { wrapper: createWrapper() })

    const restoreBtn = await screen.findByRole('button', { name: /restore/i })
    await userEvent.click(restoreBtn)

    expect(mockTrashApi.restoreNote).toHaveBeenCalled()
    expect(mockTrashApi.restoreNote.mock.calls[0][0]).toBe(5)
  })

  it('renders folder items with restore', async () => {
    mockTrashApi.list.mockResolvedValue({
      notes: [],
      files: [],
      folders: [{ id: 3, type: 'folder', name: 'Old Folder', deletedAt: '2026-04-19T00:00:00Z' }],
    })

    render(<TrashPage />, { wrapper: createWrapper() })

    expect(await screen.findByText('Old Folder')).toBeInTheDocument()
    expect(screen.getByText('Folders')).toBeInTheDocument()
  })
})
