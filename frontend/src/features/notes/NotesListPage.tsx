import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { noteApi } from '../../api'
import { QK } from '../../lib/queryKeys'
import AppLayout from '../../components/AppLayout'

export default function NotesListPage() {
  const { data, isLoading } = useQuery({
    queryKey: QK.notes(),
    queryFn: () => noteApi.list({ page: 0, size: 50 }),
  })

  return (
    <AppLayout>
      <div className="max-w-3xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-bold text-gray-900">All Notes</h1>
          <Link
            to="/notes/new"
            className="px-4 py-2 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700"
          >
            + New note
          </Link>
        </div>

        {isLoading && <p className="text-gray-500 text-sm">Loading…</p>}

        {data?.content.length === 0 && (
          <p className="text-gray-500 text-sm">No notes yet.</p>
        )}

        <ul className="space-y-2">
          {data?.content.map(note => (
            <li key={note.id}>
              <Link
                to={`/notes/${note.id}`}
                className="block rounded-lg border border-gray-200 px-4 py-3 hover:bg-gray-50 transition-colors"
              >
                <div className="flex items-start justify-between gap-2">
                  <span className="font-medium text-gray-900 line-clamp-1">{note.title}</span>
                  {note.label && (
                    <span className="shrink-0 text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600">
                      {note.label.replace('_', ' ')}
                    </span>
                  )}
                </div>
                {note.checklistItemCount > 0 && (
                  <p className="mt-1 text-xs text-gray-500">
                    {note.checkedItemCount}/{note.checklistItemCount} items
                  </p>
                )}
              </Link>
            </li>
          ))}
        </ul>

        {data && data.totalPages > 1 && (
          <p className="mt-4 text-xs text-gray-400 text-center">
            Page 1 of {data.totalPages} — use search to narrow results
          </p>
        )}
      </div>
    </AppLayout>
  )
}
