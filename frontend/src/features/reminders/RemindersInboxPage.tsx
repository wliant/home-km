import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { meApi, reminderActionApi } from '../../api'
import { toast } from '../../lib/toastStore'
import AppLayout from '../../components/AppLayout'

/**
 * Single-screen view of every reminder the signed-in user is a recipient of
 * or owns. Past-due reminders surface at the top with quick Done / Snooze
 * actions; upcoming reminders list below.
 *
 * Snooze + Done invalidate the inbox query and the unread badge so the UI
 * stays consistent with what the badge reports.
 */
export default function RemindersInboxPage() {
  const qc = useQueryClient()
  const { data = [], isLoading } = useQuery({
    queryKey: ['me', 'reminders'],
    queryFn: () => meApi.myReminders(),
  })

  const done = useMutation({
    mutationFn: (id: number) => reminderActionApi.done(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['me', 'reminders'] })
      qc.invalidateQueries({ queryKey: ['me', 'unread'] })
    },
    onError: () => toast.error('Could not mark done'),
  })
  const snooze = useMutation({
    mutationFn: ({ id, minutes }: { id: number; minutes: number }) => reminderActionApi.snoozeMinutes(id, minutes),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['me', 'reminders'] })
      qc.invalidateQueries({ queryKey: ['me', 'unread'] })
      toast.success('Snoozed')
    },
    onError: () => toast.error('Snooze failed'),
  })

  const fired = data.filter(r => r.fired)
  const upcoming = data.filter(r => !r.fired)

  return (
    <AppLayout>
      <div className="max-w-2xl mx-auto space-y-6">
        <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100">Reminders</h1>

        {isLoading && <p className="text-sm text-gray-400">Loading…</p>}

        {!isLoading && fired.length === 0 && upcoming.length === 0 && (
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Nothing scheduled. Reminders you create on a note appear here.
          </p>
        )}

        {fired.length > 0 && (
          <section>
            <h2 className="text-xs uppercase tracking-wide font-semibold text-amber-700 dark:text-amber-400 mb-2">
              Fired ({fired.length})
            </h2>
            <ul className="space-y-2">
              {fired.map(r => (
                <li
                  key={r.id}
                  className="rounded-lg border border-amber-200 dark:border-amber-700 bg-amber-50 dark:bg-amber-900/20 p-3 flex items-start gap-3"
                >
                  <div className="flex-1 min-w-0">
                    <Link to={`/notes/${r.noteId}`} className="font-medium text-gray-900 dark:text-gray-100 hover:underline">
                      {r.noteTitle}
                    </Link>
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      {new Date(r.remindAt).toLocaleString()}
                      {r.recurrence && <span className="ml-2 italic">recurring</span>}
                    </p>
                  </div>
                  <div className="flex gap-2 shrink-0">
                    <button
                      onClick={() => done.mutate(r.id)}
                      disabled={done.isPending}
                      className="text-xs px-2 py-1 rounded-lg bg-green-600 text-white hover:bg-green-700 disabled:opacity-50"
                    >
                      Done
                    </button>
                    <button
                      onClick={() => snooze.mutate({ id: r.id, minutes: 60 })}
                      disabled={snooze.isPending}
                      className="text-xs px-2 py-1 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
                    >
                      Snooze 1h
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </section>
        )}

        {upcoming.length > 0 && (
          <section>
            <h2 className="text-xs uppercase tracking-wide font-semibold text-gray-500 dark:text-gray-400 mb-2">
              Upcoming ({upcoming.length})
            </h2>
            <ul className="space-y-2">
              {upcoming.map(r => (
                <li
                  key={r.id}
                  className="rounded-lg border border-gray-200 dark:border-gray-700 p-3 flex items-start gap-3"
                >
                  <div className="flex-1 min-w-0">
                    <Link to={`/notes/${r.noteId}`} className="font-medium text-gray-900 dark:text-gray-100 hover:underline">
                      {r.noteTitle}
                    </Link>
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      {new Date(r.remindAt).toLocaleString()}
                      {r.recurrence && <span className="ml-2 italic">recurring</span>}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>
    </AppLayout>
  )
}
