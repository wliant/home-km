import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { folderApi } from '../../api'
import { QK } from '../../lib/queryKeys'
import { toast } from '../../lib/toastStore'
import AppLayout from '../../components/AppLayout'

/**
 * Lists folders the user has archived. Archive ≠ delete: items remain
 * searchable and the row keeps its child-safe / ACL state. Restoring
 * brings the folder back into the active tree at its original parent.
 */
export default function ArchivedFoldersPage() {
  const qc = useQueryClient()
  const { data = [], isLoading } = useQuery({
    queryKey: QK.archivedFolders(),
    queryFn: () => folderApi.listArchived(),
  })
  const unarchive = useMutation({
    mutationFn: (id: number) => folderApi.unarchive(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QK.archivedFolders() })
      qc.invalidateQueries({ queryKey: QK.folders() })
      toast.success('Restored')
    },
    onError: () => toast.error('Could not restore'),
  })

  return (
    <AppLayout>
      <div className="max-w-2xl mx-auto space-y-4">
        <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100">Archived folders</h1>
        {isLoading && <p className="text-sm text-gray-400">Loading…</p>}
        {!isLoading && data.length === 0 && (
          <p className="text-sm text-gray-500 dark:text-gray-400">
            No archived folders. Archive a folder from its detail page to hide it from the tree without deleting it.
          </p>
        )}
        <ul className="divide-y divide-gray-200 dark:divide-gray-700 rounded-lg border border-gray-200 dark:border-gray-700">
          {data.map(f => (
            <li key={f.id} className="flex items-center gap-3 px-4 py-2">
              {f.color && (
                <span aria-hidden="true" className="inline-block w-2 h-2 rounded-full shrink-0"
                  style={{ backgroundColor: f.color }} />
              )}
              <span aria-hidden="true">{f.icon || '📁'}</span>
              <Link to={`/folders/${f.id}`} className="flex-1 truncate text-sm text-gray-900 dark:text-gray-100 hover:underline">
                {f.name}
              </Link>
              {f.archivedAt && (
                <span className="text-xs text-gray-500 dark:text-gray-400">
                  archived {new Date(f.archivedAt).toLocaleDateString()}
                </span>
              )}
              <button
                onClick={() => unarchive.mutate(f.id)}
                disabled={unarchive.isPending}
                className="text-xs px-2 py-1 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
              >
                Restore
              </button>
            </li>
          ))}
        </ul>
      </div>
    </AppLayout>
  )
}
