export interface UploadQueueItem {
  id?: number
  clientUploadId: string
  filename: string
  mimeType: string
  folderId?: number
  blob: Blob
  status: 'pending' | 'done' | 'failed'
  errorMessage?: string
  createdAt: number
}

/**
 * Cached note payload. Stored verbatim from the API so render code doesn't
 * need an offline-aware code path — it just calls getNoteCache() when the
 * network read fails. cachedAt powers the "viewing a cached copy from X
 * minutes ago" banner; payload is the NoteDetail JSON.
 */
export interface CachedNote {
  id: number
  cachedAt: number
  payload: unknown
}

/**
 * Queued note edit waiting to be sent. expectedVersion is the version the
 * user was editing against; the server uses it for optimistic-locking and
 * may return 409, in which case we surface a "conflict" toast (v1 behavior).
 */
export interface QueuedNoteEdit {
  id?: number
  noteId: number
  payload: Record<string, unknown>
  expectedVersion: number | null
  status: 'pending' | 'failed'
  attempts: number
  lastError?: string
  enqueuedAt: number
}

const DB_NAME = 'homekm-offline-queue'
// Bump on schema changes — onupgradeneeded creates new stores idempotently.
const DB_VERSION = 3
const STORE = 'upload-queue'
const NOTE_CACHE_STORE = 'notes-cache'
const NOTE_EDIT_QUEUE_STORE = 'note-edit-queue'

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORE)) {
        const store = db.createObjectStore(STORE, { keyPath: 'id', autoIncrement: true })
        store.createIndex('status', 'status', { unique: false })
      }
      // The notes-cache store survives SW cache eviction (browsers purge HTTP
      // caches under storage pressure first). Keyed by noteId so cache lookup
      // is a direct get(), not an index scan.
      if (!db.objectStoreNames.contains(NOTE_CACHE_STORE)) {
        db.createObjectStore(NOTE_CACHE_STORE, { keyPath: 'id' })
      }
      // Queue for note edits captured while offline (or that hit a network
      // error). Drained from the main thread on the `online` event; could
      // be moved to a Background Sync handler later.
      if (!db.objectStoreNames.contains(NOTE_EDIT_QUEUE_STORE)) {
        const store = db.createObjectStore(NOTE_EDIT_QUEUE_STORE, { keyPath: 'id', autoIncrement: true })
        store.createIndex('noteId', 'noteId', { unique: false })
        store.createIndex('status', 'status', { unique: false })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

export async function enqueueUpload(item: Omit<UploadQueueItem, 'id' | 'status' | 'createdAt'>): Promise<number> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    const req = tx.objectStore(STORE).add({ ...item, status: 'pending', createdAt: Date.now() })
    req.onsuccess = () => resolve(req.result as number)
    req.onerror = () => reject(req.error)
  })
}

export async function getPendingUploads(): Promise<UploadQueueItem[]> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readonly')
    const index = tx.objectStore(STORE).index('status')
    const req = index.getAll('pending')
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

export async function getAllUploads(): Promise<UploadQueueItem[]> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readonly')
    const req = tx.objectStore(STORE).getAll()
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

export async function updateUploadStatus(
  id: number,
  status: UploadQueueItem['status'],
  errorMessage?: string,
): Promise<void> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    const store = tx.objectStore(STORE)
    const getReq = store.get(id)
    getReq.onsuccess = () => {
      const item = getReq.result as UploadQueueItem
      if (!item) { resolve(); return }
      item.status = status
      if (errorMessage) item.errorMessage = errorMessage
      store.put(item).onsuccess = () => resolve()
    }
    getReq.onerror = () => reject(getReq.error)
  })
}

export async function removeUpload(id: number): Promise<void> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    const req = tx.objectStore(STORE).delete(id)
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}

export async function putNoteCache(id: number, payload: unknown): Promise<void> {
  try {
    const db = await openDb()
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(NOTE_CACHE_STORE, 'readwrite')
      const req = tx.objectStore(NOTE_CACHE_STORE).put({ id, cachedAt: Date.now(), payload } satisfies CachedNote)
      req.onsuccess = () => resolve()
      req.onerror = () => reject(req.error)
    })
  } catch {
    // IndexedDB can fail in private-browsing modes; cache writes are best-effort.
  }
}

export async function getNoteCache<T = unknown>(id: number): Promise<{ payload: T; cachedAt: number } | null> {
  try {
    const db = await openDb()
    return await new Promise((resolve, reject) => {
      const tx = db.transaction(NOTE_CACHE_STORE, 'readonly')
      const req = tx.objectStore(NOTE_CACHE_STORE).get(id)
      req.onsuccess = () => {
        const row = req.result as CachedNote | undefined
        resolve(row ? { payload: row.payload as T, cachedAt: row.cachedAt } : null)
      }
      req.onerror = () => reject(req.error)
    })
  } catch {
    return null
  }
}

export async function enqueueNoteEdit(
  edit: Omit<QueuedNoteEdit, 'id' | 'status' | 'attempts' | 'enqueuedAt'>,
): Promise<number> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(NOTE_EDIT_QUEUE_STORE, 'readwrite')
    const req = tx.objectStore(NOTE_EDIT_QUEUE_STORE).add({
      ...edit,
      status: 'pending',
      attempts: 0,
      enqueuedAt: Date.now(),
    } satisfies Omit<QueuedNoteEdit, 'id'>)
    req.onsuccess = () => resolve(req.result as number)
    req.onerror = () => reject(req.error)
  })
}

export async function getPendingNoteEdits(): Promise<QueuedNoteEdit[]> {
  try {
    const db = await openDb()
    return await new Promise((resolve, reject) => {
      const tx = db.transaction(NOTE_EDIT_QUEUE_STORE, 'readonly')
      const req = tx.objectStore(NOTE_EDIT_QUEUE_STORE).index('status').getAll('pending')
      req.onsuccess = () => resolve(req.result as QueuedNoteEdit[])
      req.onerror = () => reject(req.error)
    })
  } catch {
    return []
  }
}

export async function removeNoteEdit(id: number): Promise<void> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(NOTE_EDIT_QUEUE_STORE, 'readwrite')
    const req = tx.objectStore(NOTE_EDIT_QUEUE_STORE).delete(id)
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}

export async function markNoteEditFailed(id: number, error: string): Promise<void> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(NOTE_EDIT_QUEUE_STORE, 'readwrite')
    const store = tx.objectStore(NOTE_EDIT_QUEUE_STORE)
    const get = store.get(id)
    get.onsuccess = () => {
      const item = get.result as QueuedNoteEdit | undefined
      if (!item) { resolve(); return }
      item.status = 'failed'
      item.attempts += 1
      item.lastError = error
      const put = store.put(item)
      put.onsuccess = () => resolve()
      put.onerror = () => reject(put.error)
    }
    get.onerror = () => reject(get.error)
  })
}
