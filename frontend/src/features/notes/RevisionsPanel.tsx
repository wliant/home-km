import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { noteApi, userRosterApi } from '../../api'
import { QK } from '../../lib/queryKeys'
import { toast } from '../../lib/toastStore'

interface Props {
  noteId: number
}

/**
 * Collapsed-by-default history panel. Backend caps revision retention to the
 * last 50 entries (REVISION_KEEP in NoteService) so this list is bounded —
 * we render everything without pagination.
 *
 * Restore writes a new revision capturing the current head before flipping
 * fields back, so the action is itself reversible.
 */
export default function RevisionsPanel({ noteId }: Props) {
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)

  const { data: revisions = [], isLoading } = useQuery({
    queryKey: ['note-revisions', noteId],
    queryFn: () => noteApi.listRevisions(noteId),
    enabled: open,
  })
  const { data: roster = [] } = useQuery({
    queryKey: ['user-roster'],
    queryFn: () => userRosterApi.list(),
    enabled: open,
    staleTime: 60_000,
  })

  const restore = useMutation({
    mutationFn: (revisionId: number) => noteApi.restoreRevision(noteId, revisionId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QK.note(noteId) })
      qc.invalidateQueries({ queryKey: ['note-revisions', noteId] })
      toast.success('Restored — current version was snapshotted to history first.')
    },
    onError: () => toast.error('Restore failed'),
  })

  const nameFor = (userId: number) =>
    roster.find(r => r.id === userId)?.displayName ?? `user #${userId}`

  return (
    <details
      open={open}
      onToggle={e => setOpen((e.currentTarget as HTMLDetailsElement).open)}
      className="rounded-lg border border-gray-200 dark:border-gray-700"
    >
      <summary className="cursor-pointer px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 list-none flex items-center justify-between">
        <span>History {revisions.length > 0 && <span className="text-gray-400 dark:text-gray-500">({revisions.length})</span>}</span>
        <span aria-hidden="true" className="text-gray-400">{open ? '▾' : '▸'}</span>
      </summary>
      <div className="px-4 pb-3 space-y-2">
        {isLoading && <p className="text-xs text-gray-400">Loading…</p>}
        {!isLoading && revisions.length === 0 && (
          <p className="text-xs text-gray-400 dark:text-gray-500">No edits recorded yet.</p>
        )}
        <ul className="divide-y divide-gray-200 dark:divide-gray-700">
          {revisions.map(rev => (
            <li key={rev.id} className="py-2 flex items-start gap-3">
              <div className="flex-1 min-w-0">
                <p className="text-sm text-gray-900 dark:text-gray-100 truncate" title={rev.title}>{rev.title}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  {new Date(rev.editedAt).toLocaleString()} · {nameFor(rev.editedBy)}
                </p>
                {rev.body && (
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 line-clamp-2 font-mono">
                    {rev.body.slice(0, 200)}{rev.body.length > 200 ? '…' : ''}
                  </p>
                )}
              </div>
              <button
                type="button"
                onClick={() => {
                  if (confirm('Restore this version? The current text will be saved to history first.')) {
                    restore.mutate(rev.id)
                  }
                }}
                disabled={restore.isPending}
                className="shrink-0 text-xs px-2 py-1 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
              >
                Restore
              </button>
            </li>
          ))}
        </ul>
      </div>
    </details>
  )
}
