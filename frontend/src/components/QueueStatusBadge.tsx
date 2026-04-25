import { useOfflineQueue } from '../lib/useOfflineQueue'

export default function QueueStatusBadge() {
  const { stats, retry } = useOfflineQueue()
  if (stats.pending === 0 && stats.failed === 0) return null

  return (
    <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-amber-50 border border-amber-200 text-xs">
      {stats.pending > 0 && (
        <span className="text-amber-700">
          {stats.pending} upload{stats.pending > 1 ? 's' : ''} pending…
        </span>
      )}
      {stats.failed > 0 && (
        <>
          <span className="text-red-600">
            {stats.failed} failed
          </span>
          <button
            onClick={retry}
            className="ml-1 px-2 py-0.5 rounded bg-red-600 text-white hover:bg-red-700"
          >
            Retry
          </button>
        </>
      )}
    </div>
  )
}
