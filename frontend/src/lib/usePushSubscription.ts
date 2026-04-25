import { useState, useEffect } from 'react'
import { apiClient } from '../api/client'

export function usePushSubscription() {
  const [subscribed, setSubscribed] = useState(false)
  const [loading, setLoading] = useState(false)

  async function subscribe() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) return
    setLoading(true)
    try {
      const reg = await navigator.serviceWorker.ready
      const { data: vapidKey } = await apiClient.get<string>('/push/vapid-public-key')
      const sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidKey),
      })
      const { endpoint, keys } = sub.toJSON() as {
        endpoint: string
        keys: { p256dh: string; auth: string }
      }
      await apiClient.post('/push/subscribe', {
        endpoint,
        p256dhKey: keys.p256dh,
        authKey: keys.auth,
        userAgent: navigator.userAgent,
      })
      setSubscribed(true)
    } catch (e) {
      console.warn('Push subscription failed:', e)
    } finally {
      setLoading(false)
    }
  }

  async function unsubscribe() {
    if (!('serviceWorker' in navigator)) return
    setLoading(true)
    try {
      const reg = await navigator.serviceWorker.ready
      const sub = await reg.pushManager.getSubscription()
      if (sub) {
        await apiClient.delete('/push/subscribe', { data: { endpoint: sub.endpoint } })
        await sub.unsubscribe()
      }
      setSubscribed(false)
    } catch (e) {
      console.warn('Push unsubscribe failed:', e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!('serviceWorker' in navigator)) return
    navigator.serviceWorker.ready.then(reg =>
      reg.pushManager.getSubscription().then(sub => setSubscribed(!!sub)),
    )
  }, [])

  return { subscribed, loading, subscribe, unsubscribe }
}

function urlBase64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = atob(base64)
  const buf = new ArrayBuffer(rawData.length)
  const view = new Uint8Array(buf)
  for (let i = 0; i < rawData.length; i++) view[i] = rawData.charCodeAt(i)
  return view
}
