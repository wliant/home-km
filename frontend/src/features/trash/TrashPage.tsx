import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { trashApi, type TrashItem } from '../../api'
import { QK } from '../../lib/queryKeys'
import { toast } from '../../lib/toastStore'
import { formatDate } from '../../lib/format'
import AppLayout from '../../components/AppLayout'

export default function TrashPage() {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery({
    queryKey: QK.trash(),
    queryFn: trashApi.list,
  })

  const restoreNote = useMutation({
    mutationFn: trashApi.restoreNote,
    onSuccess: () => { qc.invalidateQueries({ queryKey: QK.trash() }); qc.invalidateQueries({ queryKey: QK.notes() }) },
    onError: () => toast.error('Failed to restore note'),
  })

  const restoreFile = useMutation({
    mutationFn: trashApi.restoreFile,
    onSuccess: () => { qc.invalidateQueries({ queryKey: QK.trash() }); qc.invalidateQueries({ queryKey: QK.files() }) },
    onError: () => toast.error('Failed to restore file'),
  })

  const restoreFolder = useMutation({
    mutationFn: trashApi.restoreFolder,
    onSuccess: () => { qc.invalidateQueries({ queryKey: QK.trash() }); qc.invalidateQueries({ queryKey: QK.folders() }) },
    onError: () => toast.error('Failed to restore folder'),
  })

  const totalItems = (data?.notes.length ?? 0) + (data?.files.length ?? 0) + (data?.folders.length ?? 0)

  function renderSection(title: string, items: TrashItem[], onRestore: (id: number) => void, restoring: boolean) {
    if (items.length === 0) return null
    return (
      <section className="mb-6">
        <h2 className="text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400 mb-2">{title}</h2>
        <ul className="space-y-2">
          {items.map(item => (
            <li key={item.id} className="flex items-center justify-between rounded-lg border border-gray-200 dark:border-gray-700 px-4 py-3">
              <div className="min-w-0">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{item.name}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Deleted {formatDate(item.deletedAt)}
                </p>
              </div>
              <button
                onClick={() => onRestore(item.id)}
                disabled={restoring}
                className="shrink-0 ml-3 px-3 py-1.5 text-xs font-medium text-primary-600 dark:text-primary-400 border border-primary-200 dark:border-primary-700 rounded-lg hover:bg-primary-50 dark:hover:bg-primary-900/30 disabled:opacity-50"
              >
                Restore
              </button>
            </li>
          ))}
        </ul>
      </section>
    )
  }

  return (
    <AppLayout>
      <div className="max-w-3xl mx-auto">
        <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100 mb-1">Trash</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">Items are permanently deleted after 30 days.</p>

        {isLoading && <p className="text-gray-500 dark:text-gray-400 text-sm">Loading…</p>}

        {!isLoading && totalItems === 0 && (
          <p className="text-gray-500 dark:text-gray-400 text-sm">Trash is empty.</p>
        )}

        {data && (
          <>
            {renderSection('Notes', data.notes, id => restoreNote.mutate(id), restoreNote.isPending)}
            {renderSection('Files', data.files, id => restoreFile.mutate(id), restoreFile.isPending)}
            {renderSection('Folders', data.folders, id => restoreFolder.mutate(id), restoreFolder.isPending)}
          </>
        )}
      </div>
    </AppLayout>
  )
}
