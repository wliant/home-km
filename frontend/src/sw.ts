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

  event.waitUntil(
    self.registration.showNotification(title, {
      body,
      icon: '/icon-192.png',
      badge: '/icon-192.png',
      data: { noteId },
    }),
  )
})

self.addEventListener('notificationclick', (event: NotificationEvent) => {
  event.notification.close()
  const noteId: number | undefined = event.notification.data?.noteId
  const url = noteId ? `/notes/${noteId}` : '/'

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
