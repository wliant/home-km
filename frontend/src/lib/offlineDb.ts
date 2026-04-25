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

const DB_NAME = 'homekm-offline-queue'
const DB_VERSION = 1
const STORE = 'upload-queue'

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORE)) {
        const store = db.createObjectStore(STORE, { keyPath: 'id', autoIncrement: true })
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
