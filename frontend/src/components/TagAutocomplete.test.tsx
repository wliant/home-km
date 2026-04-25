import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('../api', () => ({
  tagApi: {
    list: vi.fn(),
    create: vi.fn(),
    attachToNote: vi.fn(),
    detachFromNote: vi.fn(),
    attachToFile: vi.fn(),
    detachFromFile: vi.fn(),
  },
}))

import { tagApi } from '../api'
import TagAutocomplete from './TagAutocomplete'
import type { TagResponse } from '../types'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const mockTagApi = vi.mocked(tagApi) as any

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>
}

const TAGS: TagResponse[] = [
  { id: 1, name: 'kitchen', color: '#ff0000', createdAt: '' },
  { id: 2, name: 'garden', color: '#00ff00', createdAt: '' },
]

describe('TagAutocomplete', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockTagApi.list.mockResolvedValue(TAGS)
    mockTagApi.attachToNote.mockResolvedValue([])
    mockTagApi.detachFromNote.mockResolvedValue(undefined)
  })

  it('shows existing tags as chips', () => {
    render(
      <TagAutocomplete entityType="note" entityId={1} currentTags={[TAGS[0]]} onTagsChange={() => {}} />,
      { wrapper },
    )
    expect(screen.getByText('kitchen')).toBeInTheDocument()
  })

  it('does not show input when readOnly', () => {
    render(
      <TagAutocomplete entityType="note" entityId={1} currentTags={[]} onTagsChange={() => {}} readOnly />,
      { wrapper },
    )
    expect(screen.queryByPlaceholderText(/add tag/i)).not.toBeInTheDocument()
  })

  it('shows input when not readOnly', () => {
    render(
      <TagAutocomplete entityType="note" entityId={1} currentTags={[]} onTagsChange={() => {}} />,
      { wrapper },
    )
    expect(screen.getByPlaceholderText(/add tag/i)).toBeInTheDocument()
  })

  it('shows suggestions after typing', async () => {
    render(
      <TagAutocomplete entityType="note" entityId={1} currentTags={[]} onTagsChange={() => {}} />,
      { wrapper },
    )
    await userEvent.type(screen.getByPlaceholderText(/add tag/i), 'kit')
    await waitFor(() => expect(screen.getByText('kitchen')).toBeInTheDocument())
  })

  it('filters out already-attached tags from suggestions', async () => {
    render(
      <TagAutocomplete entityType="note" entityId={1} currentTags={[TAGS[0]]} onTagsChange={() => {}} />,
      { wrapper },
    )
    await userEvent.type(screen.getByPlaceholderText(/add tag/i), 'k')
    // Wait for garden (non-attached) to appear as a suggestion
    await waitFor(() => expect(screen.getByRole('button', { name: /^garden$/i })).toBeInTheDocument())
    // kitchen is already attached → must NOT appear as a suggestion button in the dropdown
    // (the TagChip remove button has text "✕", not "kitchen")
    const dropdownButtons = screen.queryAllByRole('button', { name: /^kitchen$/i })
    expect(dropdownButtons).toHaveLength(0)
  })

  it('shows create option when input has no exact match', async () => {
    render(
      <TagAutocomplete entityType="note" entityId={1} currentTags={[]} onTagsChange={() => {}} />,
      { wrapper },
    )
    await userEvent.type(screen.getByPlaceholderText(/add tag/i), 'newuniqtag')
    await waitFor(() =>
      expect(screen.getByText(/create "newuniqtag"/i)).toBeInTheDocument(),
    )
  })

  it('does not show create option when exact match exists', async () => {
    render(
      <TagAutocomplete entityType="note" entityId={1} currentTags={[]} onTagsChange={() => {}} />,
      { wrapper },
    )
    await userEvent.type(screen.getByPlaceholderText(/add tag/i), 'kitchen')
    await waitFor(() => expect(mockTagApi.list).toHaveBeenCalled())
    expect(screen.queryByText(/create "kitchen"/i)).not.toBeInTheDocument()
  })
})
