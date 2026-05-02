import { useEffect, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import type { NoteDetail } from '../types'
import { noteApi } from '../api'
import { QK } from './queryKeys'
import { getNoteCache, putNoteCache } from './offlineDb'

export interface OfflineNoteResult {
  note: NoteDetail | undefined
  isLoading: boolean
  isOffline: boolean
  cachedAt: number | null
}

/**
 * Network-first read for a note that survives going offline. The active
 * tab keeps the network call (so edits show up), but if the request fails
 * we hand back the IndexedDB cache so the page still renders. Successful
 * fetches refresh the cache.
 */
export function useOfflineNote(noteId: number): OfflineNoteResult {
  const qc = useQueryClient()
  const [cachedAt, setCachedAt] = useState<number | null>(null)
  const [isOffline, setOffline] = useState(false)

  // Hydrate the React Query cache from IndexedDB before the network call so
  // first paint is instant when the same note has been viewed before.
  useEffect(() => {
    let cancelled = false
    void (async () => {
      const cached = await getNoteCache<NoteDetail>(noteId)
      if (cancelled || !cached) return
      const existing = qc.getQueryData<NoteDetail>(QK.note(noteId))
      if (!existing) qc.setQueryData(QK.note(noteId), cached.payload)
      setCachedAt(cached.cachedAt)
    })()
    return () => { cancelled = true }
  }, [noteId, qc])

  const query = useQuery<NoteDetail>({
    queryKey: QK.note(noteId),
    queryFn: async () => {
      try {
        const data = await noteApi.getById(noteId)
        void putNoteCache(noteId, data)
        setOffline(false)
        setCachedAt(Date.now())
        return data
      } catch (err) {
        const cached = await getNoteCache<NoteDetail>(noteId)
        if (cached) {
          setOffline(true)
          setCachedAt(cached.cachedAt)
          return cached.payload
        }
        throw err
      }
    },
    // Cached payload may be stale; refetch on focus to pick up edits.
    staleTime: 30_000,
  })

  return {
    note: query.data,
    isLoading: query.isLoading,
    isOffline,
    cachedAt,
  }
}
