import { useEffect, useState, useCallback } from 'react'
import { fileApi } from '../api'
import {
  getAllUploads, getPendingUploads, updateUploadStatus, removeUpload,
  type UploadQueueItem,
} from './offlineDb'

export interface QueueStats {
  pending: number
  failed: number
  items: UploadQueueItem[]
}

export function useOfflineQueue(): { stats: QueueStats; retry: () => void } {
  const [stats, setStats] = useState<QueueStats>({ pending: 0, failed: 0, items: [] })

  const refresh = useCallback(async () => {
    const all = await getAllUploads()
    setStats({
      pending: all.filter(i => i.status === 'pending').length,
      failed: all.filter(i => i.status === 'failed').length,
      items: all,
    })
  }, [])

  const processQueue = useCallback(async () => {
    if (!navigator.onLine) return
    const pending = await getPendingUploads()
    for (const item of pending) {
      if (!item.id) continue
      try {
        await fileApi.upload(
          new File([item.blob], item.filename, { type: item.mimeType }),
          item.folderId,
          item.clientUploadId,
        )
        await removeUpload(item.id)
      } catch {
        await updateUploadStatus(item.id, 'failed', 'Upload failed')
      }
    }
    await refresh()
  }, [refresh])

  const retry = useCallback(async () => {
    const all = await getAllUploads()
    for (const item of all.filter(i => i.status === 'failed')) {
      if (item.id) await updateUploadStatus(item.id, 'pending')
    }
    await processQueue()
  }, [processQueue])

  useEffect(() => {
    refresh()
    const interval = setInterval(processQueue, 5000)
    window.addEventListener('online', processQueue)
    return () => {
      clearInterval(interval)
      window.removeEventListener('online', processQueue)
    }
  }, [refresh, processQueue])

  return { stats, retry }
}
