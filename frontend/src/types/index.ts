export * from './auth'

export interface FolderResponse {
  id: number
  parentId: number | null
  name: string
  description: string | null
  ownerId: number
  isChildSafe: boolean
  createdAt: string
  updatedAt: string
  archivedAt: string | null
  color: string | null
  icon: string | null
  children: FolderResponse[]
  ancestors: { id: number; name: string }[] | null
}

export interface NoteSummary {
  id: number
  folderId: number | null
  ownerId: number
  title: string
  label: string
  isChildSafe: boolean
  checklistItemCount: number
  checkedItemCount: number
  createdAt: string
  updatedAt: string
  pinnedAt: string | null
}

export interface ChecklistItemResponse {
  id: number
  text: string
  isChecked: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface ReminderResponse {
  id: number
  remindAt: string
  recurrence: string | null
  pushSent: boolean
  recipientUserIds: number[]
  createdAt: string
  updatedAt: string
}

export interface NoteDetail {
  id: number
  folderId: number | null
  ownerId: number
  title: string
  body: string | null
  label: string
  isChildSafe: boolean
  createdAt: string
  updatedAt: string
  pinnedAt: string | null
  checklistItems: ChecklistItemResponse[]
  reminders: ReminderResponse[]
  /** Optimistic-concurrency token. Send back on PUT to detect concurrent edits. */
  version?: number
}

export interface FileResponse {
  id: number
  folderId: number | null
  ownerId: number
  filename: string
  mimeType: string
  sizeBytes: number
  description: string | null
  isChildSafe: boolean
  hasThumbnail: boolean
  downloadUrl: string | null
  thumbnailUrl: string | null
  uploadedAt: string
  updatedAt: string
}

export interface TagResponse {
  id: number
  name: string
  color: string
  createdAt: string
}

export interface SearchResult {
  id: number
  type: 'note' | 'file' | 'folder'
  title: string
  excerpt: string
  folderId: number | null
  isChildSafe: boolean
  updatedAt: string
  /** 0..1 cosine-similarity score; only meaningful when smart=true. */
  score?: number
}

export interface SearchResponse extends PageResponse<SearchResult> {
  suggestion: string | null
}

export interface SavedSearch {
  id: number
  name: string
  query: string
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export const NOTE_LABELS = [
  'recipe', 'todo', 'reminder', 'shopping_list', 'home_items',
  'usage_manual', 'goal', 'aspiration', 'wish_list', 'travel_log', 'custom',
] as const

export type NoteLabel = typeof NOTE_LABELS[number]
