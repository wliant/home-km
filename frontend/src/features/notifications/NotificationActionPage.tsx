import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { reminderActionApi } from '../../api'
import { toast } from '../../lib/toastStore'
import AppLayout from '../../components/AppLayout'

/**
 * Service workers can't sign authenticated requests in this app's setup
 * (Bearer JWT lives in localStorage, unreachable from the SW). When a user
 * taps a push notification action button, the SW navigates here with the
 * reminder + action in the query string and this page fires the API call
 * from the main thread, where the auth interceptor is wired up.
 *
 * After a successful action the user is redirected to the linked note (or
 * home if none) so the round-trip feels like a single tap.
 */
export default function NotificationActionPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const ran = useRef(false)

  useEffect(() => {
    if (ran.current) return
    ran.current = true

    const reminderId = Number(params.get('reminderId'))
    const action = params.get('action')
    const noteId = params.get('noteId')

    if (!reminderId || !action) {
      navigate('/', { replace: true })
      return
    }

    const target = noteId ? `/notes/${noteId}` : '/'

    const promise = action === 'done'
      ? reminderActionApi.done(reminderId)
      : action.startsWith('snooze-')
        ? reminderActionApi.snoozeMinutes(reminderId, parseSnoozeMinutes(action))
        : Promise.reject(new Error('Unknown action'))

    promise
      .then(() => {
        toast.success(action === 'done' ? 'Reminder completed' : 'Reminder snoozed')
        navigate(target, { replace: true })
      })
      .catch(() => {
        setError(`Could not ${action === 'done' ? 'complete' : 'snooze'} reminder.`)
      })
  }, [params, navigate])

  return (
    <AppLayout>
      <div className="text-sm">
        {error
          ? <p className="text-red-600 dark:text-red-400">{error}</p>
          : <p className="text-gray-500 dark:text-gray-400">Processing notification action…</p>}
      </div>
    </AppLayout>
  )
}

function parseSnoozeMinutes(action: string): number {
  // Accept "snooze-1h", "snooze-15m", "snooze-1d" — fall back to one hour.
  const m = action.match(/^snooze-(\d+)([mhd])$/)
  if (!m) return 60
  const n = Number(m[1])
  switch (m[2]) {
    case 'm': return n
    case 'h': return n * 60
    case 'd': return n * 60 * 24
    default: return 60
  }
}
