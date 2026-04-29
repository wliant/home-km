import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { meApi } from '../api'
import { useAuthStore } from './authStore'
import { setAppBadge } from './appBadge'

/**
 * Polls /api/me/unread on a slow interval and on window focus. Mirrors the
 * count to the OS app icon badge. Returns the current count for in-app use
 * (e.g. nav indicator).
 */
export function useUnreadBadge() {
  const isAuthed = useAuthStore(s => s.isAuthenticated)

  const { data } = useQuery({
    queryKey: ['me', 'unread'],
    queryFn: meApi.unread,
    enabled: isAuthed,
    refetchInterval: 60_000,
    refetchOnWindowFocus: true,
    staleTime: 30_000,
  })

  useEffect(() => {
    if (!isAuthed) return
    setAppBadge(data?.count ?? 0)
  }, [data?.count, isAuthed])

  return data?.count ?? 0
}
