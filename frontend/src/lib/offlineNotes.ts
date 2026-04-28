// Lightweight IndexedDB store for offline-readable pinned notes and queued note writes.
// Uses the same homekm-offline-queue DB as offlineDb.ts but adds two object stores.

const DB_NAME = 'homekm-offline-queue'
const DB_VERSION = 2
const NOTES_STORE = 'pinned-notes'
const WRITE_QUEUE = 'note-writes'

export interface PinnedNote {
  id: number
  title: string
  body: string
  cachedAt: number
}

export interface QueuedWrite {
  id?: number
  method: 'POST' | 'PATCH' | 'PUT'
  path: string
  body: string
  idempotencyKey: string
  createdAt: number
  status: 'pending' | 'failed'
  errorMessage?: string
}

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = (event) => {
      const db = req.result
      if (!db.objectStoreNames.contains('upload-queue')) {
        const s = db.createObjectStore('upload-queue', { keyPath: 'id', autoIncrement: true })
        s.createIndex('status', 'status', { unique: false })
      }
      if (event.oldVersion < 2) {
        if (!db.objectStoreNames.contains(NOTES_STORE)) {
          db.createObjectStore(NOTES_STORE, { keyPath: 'id' })
        }
        if (!db.objectStoreNames.contains(WRITE_QUEUE)) {
          const w = db.createObjectStore(WRITE_QUEUE, { keyPath: 'id', autoIncrement: true })
          w.createIndex('status', 'status', { unique: false })
        }
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

export async function pinNoteOffline(note: { id: number; title: string; body: string }): Promise<void> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(NOTES_STORE, 'readwrite')
    tx.objectStore(NOTES_STORE).put({ ...note, cachedAt: Date.now() })
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

export async function unpinNoteOffline(id: number): Promise<void> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(NOTES_STORE, 'readwrite')
    tx.objectStore(NOTES_STORE).delete(id)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

export async function getPinnedNote(id: number): Promise<PinnedNote | null> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(NOTES_STORE, 'readonly')
    const req = tx.objectStore(NOTES_STORE).get(id)
    req.onsuccess = () => resolve((req.result as PinnedNote | undefined) ?? null)
    req.onerror = () => reject(req.error)
  })
}

export async function listPinnedNotes(): Promise<PinnedNote[]> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(NOTES_STORE, 'readonly')
    const req = tx.objectStore(NOTES_STORE).getAll()
    req.onsuccess = () => resolve(req.result as PinnedNote[])
    req.onerror = () => reject(req.error)
  })
}

export async function enqueueWrite(write: Omit<QueuedWrite, 'id' | 'status' | 'createdAt'>): Promise<number> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(WRITE_QUEUE, 'readwrite')
    const req = tx.objectStore(WRITE_QUEUE).add({ ...write, status: 'pending', createdAt: Date.now() })
    req.onsuccess = () => resolve(req.result as number)
    req.onerror = () => reject(req.error)
  })
}

export async function listQueuedWrites(): Promise<QueuedWrite[]> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(WRITE_QUEUE, 'readonly')
    const req = tx.objectStore(WRITE_QUEUE).getAll()
    req.onsuccess = () => resolve(req.result as QueuedWrite[])
    req.onerror = () => reject(req.error)
  })
}

export async function removeQueuedWrite(id: number): Promise<void> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(WRITE_QUEUE, 'readwrite')
    tx.objectStore(WRITE_QUEUE).delete(id)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

export async function markQueuedWriteFailed(id: number, errorMessage: string): Promise<void> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(WRITE_QUEUE, 'readwrite')
    const store = tx.objectStore(WRITE_QUEUE)
    const get = store.get(id)
    get.onsuccess = () => {
      const item = get.result as QueuedWrite | undefined
      if (!item) { resolve(); return }
      item.status = 'failed'
      item.errorMessage = errorMessage
      store.put(item).onsuccess = () => resolve()
    }
    get.onerror = () => reject(get.error)
  })
}

/** Replay any queued writes by sending them via fetch with their saved Idempotency-Key. */
export async function replayQueuedWrites(getAuthToken: () => string | null): Promise<{ ok: number; failed: number }> {
  if (!navigator.onLine) return { ok: 0, failed: 0 }
  const writes = await listQueuedWrites()
  let ok = 0
  let failed = 0
  for (const w of writes) {
    if (!w.id) continue
    if (w.status !== 'pending') continue
    try {
      const res = await fetch(w.path, {
        method: w.method,
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': w.idempotencyKey,
          ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {}),
        },
        body: w.body,
      })
      if (res.ok || res.status === 409) {
        await removeQueuedWrite(w.id)
        ok++
      } else {
        await markQueuedWriteFailed(w.id, `HTTP ${res.status}`)
        failed++
      }
    } catch (err) {
      await markQueuedWriteFailed(w.id, (err as Error).message)
      failed++
    }
  }
  return { ok, failed }
}
