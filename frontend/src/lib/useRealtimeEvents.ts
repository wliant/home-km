import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from './authStore'

type SsePayload = { type?: string; id?: number; noteId?: number }

/** Subscribes to /api/events SSE stream and invalidates affected query caches. */
export function useRealtimeEvents() {
  const token = useAuthStore((s) => s.token)
  const qc = useQueryClient()

  useEffect(() => {
    if (!token) return
    if (typeof EventSource === 'undefined') return

    const url = `/api/events?access_token=${encodeURIComponent(token)}`
    const es = new EventSource(url, { withCredentials: false })

    function dispatch(name: string, raw: string) {
      let data: SsePayload | null = null
      try { data = JSON.parse(raw) } catch { /* ignore parse errors */ }
      if (name === 'ItemUpdated') {
        if (data?.type === 'note' && data?.id) {
          qc.invalidateQueries({ queryKey: ['note', data.id] })
        }
        qc.invalidateQueries({ queryKey: ['notes'] })
        qc.invalidateQueries({ queryKey: ['files'] })
      } else if (name === 'ChecklistItemToggled' && data?.noteId) {
        qc.invalidateQueries({ queryKey: ['note', data.noteId] })
      }
    }

    const handlers = ['ItemUpdated', 'ChecklistItemToggled', 'CommentAdded', 'ReminderUpdated']
    handlers.forEach(name => {
      es.addEventListener(name, (e) => dispatch(name, (e as MessageEvent).data))
    })

    es.onerror = () => {
      // Browser auto-reconnects with last-event-id; nothing to do.
    }

    return () => es.close()
  }, [token, qc])
}
