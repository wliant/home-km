import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { mentionApi, type MentionInboxItem } from '../../api'
import AppLayout from '../../components/AppLayout'

export default function MentionsPage() {
  const qc = useQueryClient()

  const { data: items = [] } = useQuery<MentionInboxItem[]>({
    queryKey: ['mentions'],
    queryFn: () => mentionApi.list(),
  })

  const markAllRead = useMutation({
    mutationFn: () => mentionApi.markAllRead(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['mentions'] })
      qc.invalidateQueries({ queryKey: ['mentions', 'unread-count'] })
    },
  })

  // Mark everything as read on first view; the bell badge is the indicator.
  useEffect(() => {
    if (items.some(m => !m.readAt)) markAllRead.mutate()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items.length])

  return (
    <AppLayout>
      <div className="max-w-2xl mx-auto p-6 space-y-4">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Mentions</h1>
        {items.length === 0 ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">Nobody has mentioned you yet.</p>
        ) : (
          <ul className="space-y-2">
            {items.map(m => {
              const target = m.itemType === 'note' ? `/notes/${m.itemId}` : `/files/${m.itemId}`
              return (
                <li
                  key={m.id}
                  className={`rounded-md border p-3 ${m.readAt
                    ? 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800'
                    : 'border-primary-300 dark:border-primary-700 bg-primary-50/40 dark:bg-primary-900/20'
                  }`}
                >
                  <div className="text-xs text-gray-500 dark:text-gray-400 mb-1">
                    <span className="font-medium text-gray-700 dark:text-gray-300">{m.authorDisplayName}</span>
                    {' on '}
                    <Link to={target} className="text-primary-600 dark:text-primary-400 hover:underline">
                      {m.itemType === 'note' ? 'a note' : 'a file'}
                    </Link>
                    {' · '}
                    {new Date(m.createdAt).toLocaleString()}
                  </div>
                  <p className="text-sm text-gray-800 dark:text-gray-200 whitespace-pre-wrap">{m.preview}</p>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </AppLayout>
  )
}
