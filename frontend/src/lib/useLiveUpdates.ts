import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from './authStore'
import { QK } from './queryKeys'
import { consumeEventStream } from './eventStream'

/**
 * Subscribes to the server's SSE event stream while the user is signed in
 * and dispatches cache invalidations so collaborators see each other's
 * edits without a manual refresh. The connection auto-reconnects with
 * exponential backoff via the underlying eventStream consumer.
 *
 * Today the backend publishes:
 *   - ItemUpdated  ({ type: 'note'|'file'|'folder', id })
 *   - ChecklistItemToggled  ({ noteId })
 *   - hello (handshake — ignored)
 *
 * New event types: add a case below; do not introduce per-feature hooks
 * unless they need DOM side effects (toasts, scroll jumps, etc.).
 */
export function useLiveUpdates(): void {
  const qc = useQueryClient()
  const token = useAuthStore(s => s.token)

  useEffect(() => {
    if (!token) return
    const ctrl = new AbortController()
    void consumeEventStream({
      url: '/api/events',
      token,
      signal: ctrl.signal,
      onEvent: (type, data) => {
        const payload = (data ?? {}) as { type?: string; id?: number; noteId?: number }
        switch (type) {
          case 'ItemUpdated': {
            if (payload.type === 'note' && payload.id != null) {
              void qc.invalidateQueries({ queryKey: QK.note(payload.id) })
              void qc.invalidateQueries({ queryKey: QK.notes() })
            } else if (payload.type === 'file' && payload.id != null) {
              void qc.invalidateQueries({ queryKey: QK.file(payload.id) })
              void qc.invalidateQueries({ queryKey: QK.files() })
            } else if (payload.type === 'folder' && payload.id != null) {
              void qc.invalidateQueries({ queryKey: QK.folder(payload.id) })
              void qc.invalidateQueries({ queryKey: QK.folders() })
            }
            break
          }
          case 'ChecklistItemToggled': {
            if (payload.noteId != null) void qc.invalidateQueries({ queryKey: QK.note(payload.noteId) })
            break
          }
          // hello / heartbeat / unknown types: nothing to do.
        }
      },
    })
    return () => ctrl.abort()
  }, [token, qc])
}
