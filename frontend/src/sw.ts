/// <reference lib="webworker" />
import { cleanupOutdatedCaches, precacheAndRoute } from 'workbox-precaching'
import { NetworkFirst } from 'workbox-strategies'
import { registerRoute } from 'workbox-routing'

declare const self: ServiceWorkerGlobalScope

cleanupOutdatedCaches()
precacheAndRoute(self.__WB_MANIFEST)

registerRoute(
  ({ url }) => url.pathname.startsWith('/api/'),
  new NetworkFirst({ cacheName: 'api-cache' }),
)

self.addEventListener('push', (event: PushEvent) => {
  const data = event.data?.json() ?? {}
  const title: string = data.title ?? 'Home KM'
  const body: string = data.body ?? 'You have a new reminder'
  const noteId: number | undefined = data.noteId
  const reminderId: number | undefined = data.reminderId

  // Action buttons surface only when we know which reminder fired. Browsers
  // without action support (e.g. iOS Safari) silently ignore the array.
  const actions = reminderId != null
    ? [
      { action: 'done', title: 'Done' },
      { action: 'snooze-1h', title: 'Snooze 1h' },
    ]
    : []

  event.waitUntil(
    self.registration.showNotification(title, {
      body,
      icon: '/icon-192.png',
      badge: '/icon-192.png',
      actions,
      data: { noteId, reminderId },
    } as NotificationOptions),
  )
})

self.addEventListener('notificationclick', (event: NotificationEvent) => {
  event.notification.close()
  const { noteId, reminderId } = (event.notification.data ?? {}) as {
    noteId?: number
    reminderId?: number
  }

  // Action button clicks route through /notifications/action so the in-page
  // client can fire the authenticated POST. Body click (no event.action) just
  // opens the linked note like before.
  const url = event.action && reminderId != null
    ? `/notifications/action?reminderId=${reminderId}&action=${encodeURIComponent(event.action)}` +
        (noteId != null ? `&noteId=${noteId}` : '')
    : noteId != null
      ? `/notes/${noteId}`
      : '/'

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clients => {
      for (const client of clients) {
        if ('focus' in client) {
          client.navigate(url)
          return client.focus()
        }
      }
      return self.clients.openWindow(url)
    }),
  )
})
