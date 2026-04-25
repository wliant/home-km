import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock useOfflineQueue so the badge renders without IndexedDB
vi.mock('../lib/useOfflineQueue', () => ({
  useOfflineQueue: vi.fn(),
}))

import { useOfflineQueue } from '../lib/useOfflineQueue'
import QueueStatusBadge from './QueueStatusBadge'

const mockUseOfflineQueue = vi.mocked(useOfflineQueue)

describe('QueueStatusBadge', () => {
  const retry = vi.fn()

  beforeEach(() => {
    retry.mockClear()
  })

  it('renders nothing when queue is empty', () => {
    mockUseOfflineQueue.mockReturnValue({ stats: { pending: 0, failed: 0, items: [] }, retry })
    const { container } = render(<QueueStatusBadge />)
    expect(container).toBeEmptyDOMElement()
  })

  it('shows pending count', () => {
    mockUseOfflineQueue.mockReturnValue({ stats: { pending: 3, failed: 0, items: [] }, retry })
    render(<QueueStatusBadge />)
    expect(screen.getByText(/3 uploads? pending/)).toBeInTheDocument()
  })

  it('shows failed count and retry button', () => {
    mockUseOfflineQueue.mockReturnValue({ stats: { pending: 0, failed: 2, items: [] }, retry })
    render(<QueueStatusBadge />)
    expect(screen.getByText(/2 failed/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument()
  })

  it('calls retry when retry button is clicked', async () => {
    mockUseOfflineQueue.mockReturnValue({ stats: { pending: 0, failed: 1, items: [] }, retry })
    render(<QueueStatusBadge />)
    await userEvent.click(screen.getByRole('button', { name: /retry/i }))
    expect(retry).toHaveBeenCalledOnce()
  })

  it('uses singular "upload" for exactly one pending', () => {
    mockUseOfflineQueue.mockReturnValue({ stats: { pending: 1, failed: 0, items: [] }, retry })
    render(<QueueStatusBadge />)
    expect(screen.getByText(/1 upload pending/)).toBeInTheDocument()
  })
})
