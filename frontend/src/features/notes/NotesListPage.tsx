import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { noteApi } from '../../api'
import { QK } from '../../lib/queryKeys'
import AppLayout from '../../components/AppLayout'
import type { NoteSummary } from '../../types'

export default function NotesListPage() {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery({
    queryKey: QK.notes(),
    queryFn: () => noteApi.list({ page: 0, size: 50 }),
  })

  const togglePin = useMutation({
    mutationFn: ({ id, pinned }: { id: number; pinned: boolean }) =>
      pinned ? noteApi.unpin(id) : noteApi.pin(id),
    onSuccess: (updated) => {
      qc.setQueryData(QK.note(updated.id), updated)
      qc.invalidateQueries({ queryKey: QK.notes() })
    },
  })

  const [pinned, rest] = useMemo(() => {
    const p: NoteSummary[] = []
    const r: NoteSummary[] = []
    for (const n of data?.content ?? []) (n.pinnedAt ? p : r).push(n)
    return [p, r]
  }, [data?.content])

  const renderRow = (note: NoteSummary) => {
    const isPinned = !!note.pinnedAt
    const isToggling = togglePin.isPending && togglePin.variables?.id === note.id
    return (
      <li
        key={note.id}
        className="flex items-stretch rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
      >
        <button
          type="button"
          aria-label={isPinned ? 'Unpin note' : 'Pin note'}
          aria-pressed={isPinned}
          disabled={isToggling}
          onClick={() => togglePin.mutate({ id: note.id, pinned: isPinned })}
          className="px-3 py-3 text-lg leading-none text-amber-500 hover:text-amber-600 disabled:opacity-50"
        >
          {isPinned ? '⭐' : '☆'}
        </button>
        <Link to={`/notes/${note.id}`} className="flex-1 min-w-0 py-3 pr-4">
          <div className="flex items-start justify-between gap-2">
            <span className="font-medium text-gray-900 dark:text-gray-100 line-clamp-1">{note.title}</span>
            {note.label && (
              <span className="shrink-0 text-xs px-2 py-0.5 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400">
                {note.label.replace('_', ' ')}
              </span>
            )}
          </div>
          {note.checklistItemCount > 0 && (
            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
              {note.checkedItemCount}/{note.checklistItemCount} items
            </p>
          )}
        </Link>
      </li>
    )
  }

  const sectionHeading = (text: string) => (
    <h2 className="text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400 mb-2">{text}</h2>
  )

  return (
    <AppLayout>
      <div className="max-w-3xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100">All Notes</h1>
          <Link
            to="/notes/new"
            className="px-4 py-2 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700"
          >
            + New note
          </Link>
        </div>

        {isLoading && <p className="text-gray-500 dark:text-gray-400 text-sm">Loading…</p>}

        {data?.content.length === 0 && (
          <p className="text-gray-500 dark:text-gray-400 text-sm">No notes yet.</p>
        )}

        {pinned.length > 0 && (
          <section className="mb-6">
            {sectionHeading('Pinned')}
            <ul className="space-y-2">{pinned.map(renderRow)}</ul>
          </section>
        )}

        {rest.length > 0 && (
          <section>
            {pinned.length > 0 && sectionHeading('Others')}
            <ul className="space-y-2">{rest.map(renderRow)}</ul>
          </section>
        )}

        {data && data.totalPages > 1 && (
          <p className="mt-4 text-xs text-gray-400 dark:text-gray-500 text-center">
            Page 1 of {data.totalPages} — use search to narrow results
          </p>
        )}
      </div>
    </AppLayout>
  )
}
