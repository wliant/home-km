import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { adminApi } from '../../api'
import AppLayout from '../../components/AppLayout'

interface AuditEvent {
  id: number
  actorUserId: number | null
  action: string
  targetType: string | null
  targetId: string | null
  ip: string | null
  occurredAt: string
}

interface AuditPageResponse {
  content: AuditEvent[]
  totalPages: number
  totalElements: number
  page: number
  size: number
}

export default function AuditLogPage() {
  const [page, setPage] = useState(0)
  const [actionFilter, setActionFilter] = useState('')

  const { data, isLoading } = useQuery<AuditPageResponse>({
    queryKey: ['admin', 'audit', { page, action: actionFilter || undefined }],
    queryFn: () =>
      adminApi.getAuditLog({
        page,
        size: 25,
        action: actionFilter || undefined,
      }),
  })

  return (
    <AppLayout>
      <div className="max-w-5xl mx-auto">
        <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100 mb-6">Audit Log</h1>

        <div className="flex gap-3 mb-4">
          <select
            value={actionFilter}
            onChange={e => { setActionFilter(e.target.value); setPage(0) }}
            className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm"
          >
            <option value="">All actions</option>
            {['AUTH_LOGIN', 'AUTH_REGISTER', 'AUTH_LOGOUT', 'AUTH_PASSWORD_RESET',
              'NOTE_DELETE', 'FILE_DELETE', 'FOLDER_DELETE'].map(a => (
              <option key={a} value={a}>{a}</option>
            ))}
          </select>
        </div>

        {isLoading && <p className="text-gray-500 dark:text-gray-400 text-sm">Loading…</p>}

        {data && data.content.length === 0 && (
          <p className="text-gray-500 dark:text-gray-400 text-sm">No audit events found.</p>
        )}

        {data && data.content.length > 0 && (
          <div className="rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 dark:bg-gray-800 text-gray-600 dark:text-gray-400 text-left">
                <tr>
                  <th className="px-4 py-3 font-medium">Time</th>
                  <th className="px-4 py-3 font-medium">Action</th>
                  <th className="px-4 py-3 font-medium">Target</th>
                  <th className="px-4 py-3 font-medium">Actor</th>
                  <th className="px-4 py-3 font-medium">IP</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                {data.content.map(event => (
                  <tr key={event.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/50">
                    <td className="px-4 py-3 text-gray-700 dark:text-gray-300 whitespace-nowrap">
                      {new Date(event.occurredAt).toLocaleString()}
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 font-mono">
                        {event.action}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-700 dark:text-gray-300">
                      {event.targetType && (
                        <span>{event.targetType} #{event.targetId}</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-gray-700 dark:text-gray-300">
                      {event.actorUserId ? `User #${event.actorUserId}` : '—'}
                    </td>
                    <td className="px-4 py-3 text-gray-500 dark:text-gray-400 font-mono text-xs">
                      {event.ip ?? '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between mt-4">
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg disabled:opacity-50 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              Previous
            </button>
            <span className="text-sm text-gray-500 dark:text-gray-400">
              Page {page + 1} of {data.totalPages}
            </span>
            <button
              onClick={() => setPage(p => Math.min(data.totalPages - 1, p + 1))}
              disabled={page >= data.totalPages - 1}
              className="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg disabled:opacity-50 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </AppLayout>
  )
}
