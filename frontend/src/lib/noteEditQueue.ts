import axios from 'axios'
import { noteApi } from '../api'
import { toast } from './toastStore'
import {
  enqueueNoteEdit,
  getPendingNoteEdits,
  markNoteEditFailed,
  putNoteCache,
  removeNoteEdit,
} from './offlineDb'
import type { NoteDetail } from '../types'

/**
 * Update a note. On network failure (offline / DNS / 5xx), queue the edit
 * locally and resolve with `{ queued: true }` so the UI can navigate away
 * optimistically. Conflicts (HTTP 409 STALE_VERSION) are surfaced to the
 * caller — those are real merge problems, not transient connectivity.
 */
export async function updateNoteOfflineAware(
  noteId: number,
  payload: Record<string, unknown>,
  expectedVersion: number | null,
): Promise<{ queued: boolean; note?: NoteDetail }> {
  const body = expectedVersion != null ? { ...payload, expectedVersion } : payload
  try {
    const note = await noteApi.update(noteId, body as Parameters<typeof noteApi.update>[1])
    void putNoteCache(noteId, note)
    return { queued: false, note }
  } catch (err) {
    // Network errors (no response) → queue. HTTP-level errors → bubble up
    // so the caller can show a 409 conflict modal or a real error.
    const isNetworkErr = axios.isAxiosError(err) && !err.response
    if (!isNetworkErr) throw err
    await enqueueNoteEdit({ noteId, payload: body, expectedVersion })
    return { queued: true }
  }
}

let installed = false
let flushing = false

/**
 * Drain the pending edit queue. Each successful edit is removed; transient
 * failures stay pending; 409 STALE_VERSION moves to status=failed so the
 * user can resolve manually (a future merge UI lives here).
 */
export async function flushNoteEditQueue(): Promise<void> {
  if (flushing) return
  flushing = true
  try {
    const pending = await getPendingNoteEdits()
    if (pending.length === 0) return
    let okCount = 0
    let conflictCount = 0
    for (const edit of pending) {
      if (edit.id == null) continue
      try {
        const note = await noteApi.update(edit.noteId, edit.payload as Parameters<typeof noteApi.update>[1])
        void putNoteCache(edit.noteId, note)
        await removeNoteEdit(edit.id)
        okCount += 1
      } catch (err) {
        if (axios.isAxiosError(err) && err.response?.status === 409) {
          await markNoteEditFailed(edit.id, 'STALE_VERSION — a newer edit exists on the server')
          conflictCount += 1
        }
        // Other errors leave the row pending for the next flush attempt.
      }
    }
    if (okCount > 0) toast.success(`Synced ${okCount} offline note edit${okCount === 1 ? '' : 's'}`)
    if (conflictCount > 0) toast.error(`${conflictCount} offline edit${conflictCount === 1 ? '' : 's'} conflicted with newer changes — see Trash → Failed sync`)
  } finally {
    flushing = false
  }
}

/**
 * Wire up automatic flush on coming back online. Safe to call multiple
 * times — second invocation is a no-op.
 */
export function installNoteEditFlush(): void {
  if (installed) return
  installed = true
  window.addEventListener('online', () => { void flushNoteEditQueue() })
  // Best-effort: also try once on boot in case we missed the event while
  // the page was loading.
  if (navigator.onLine) void flushNoteEditQueue()
}
