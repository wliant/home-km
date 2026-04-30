export * from './client'
export * from './authApi'

import { apiClient } from './client'
import type {
  FolderResponse, NoteSummary, NoteDetail, FileResponse, TagResponse,
  SearchResponse, SavedSearch, PageResponse, ChecklistItemResponse, ReminderResponse,
} from '../types'

// Folders
export const folderApi = {
  getTree: () => apiClient.get<FolderResponse[]>('/folders').then(r => r.data),
  getById: (id: number) => apiClient.get<FolderResponse>(`/folders/${id}`).then(r => r.data),
  create: (data: { name: string; description?: string; parentId?: number }) =>
    apiClient.post<FolderResponse>('/folders', data).then(r => r.data),
  update: (id: number, data: { name?: string; description?: string; parentId?: number | null }) =>
    apiClient.put<FolderResponse>(`/folders/${id}`, data).then(r => r.data),
  delete: (id: number, force = false) =>
    apiClient.delete(`/folders/${id}?force=${force}`),
  setChildSafe: (id: number, isChildSafe: boolean) =>
    apiClient.put<FolderResponse>(`/folders/${id}/child-safe`, { isChildSafe }).then(r => r.data),
}

// Notes
export const noteApi = {
  list: (params?: { folderId?: number; page?: number; size?: number }) =>
    apiClient.get<PageResponse<NoteSummary>>('/notes', { params }).then(r => r.data),
  getById: (id: number) => apiClient.get<NoteDetail>(`/notes/${id}`).then(r => r.data),
  create: (data: { title: string; body?: string; label?: string; folderId?: number; isChildSafe?: boolean }) =>
    apiClient.post<NoteDetail>('/notes', data).then(r => r.data),
  update: (id: number, data: Partial<{ title: string; body: string; label: string; folderId: number | null; isChildSafe: boolean }>) =>
    apiClient.put<NoteDetail>(`/notes/${id}`, data).then(r => r.data),
  delete: (id: number) => apiClient.delete(`/notes/${id}`),
  pin: (id: number) => apiClient.post<NoteDetail>(`/notes/${id}/pin`).then(r => r.data),
  unpin: (id: number) => apiClient.delete<NoteDetail>(`/notes/${id}/pin`).then(r => r.data),
}

// Checklist items
export const checklistApi = {
  list: (noteId: number) =>
    apiClient.get<ChecklistItemResponse[]>(`/notes/${noteId}/checklist-items`).then(r => r.data),
  add: (noteId: number, data: { text: string; isChecked?: boolean; sortOrder?: number }) =>
    apiClient.post<ChecklistItemResponse>(`/notes/${noteId}/checklist-items`, data).then(r => r.data),
  update: (noteId: number, itemId: number, data: { text?: string; isChecked?: boolean; sortOrder?: number }) =>
    apiClient.put<ChecklistItemResponse>(`/notes/${noteId}/checklist-items/${itemId}`, data).then(r => r.data),
  delete: (noteId: number, itemId: number) =>
    apiClient.delete(`/notes/${noteId}/checklist-items/${itemId}`),
}

// Reminders
export const reminderApi = {
  list: (noteId: number) =>
    apiClient.get<ReminderResponse[]>(`/notes/${noteId}/reminders`).then(r => r.data),
  create: (noteId: number, data: { remindAt: string; recurrence?: string; recipientUserIds?: number[] }) =>
    apiClient.post<ReminderResponse>(`/notes/${noteId}/reminders`, data).then(r => r.data),
  update: (noteId: number, reminderId: number, data: { remindAt: string; recurrence?: string; recipientUserIds?: number[] }) =>
    apiClient.put<ReminderResponse>(`/notes/${noteId}/reminders/${reminderId}`, data).then(r => r.data),
  delete: (noteId: number, reminderId: number) =>
    apiClient.delete(`/notes/${noteId}/reminders/${reminderId}`),
}

// Files
export const fileApi = {
  list: (params?: { folderId?: number; page?: number; size?: number }) =>
    apiClient.get<PageResponse<FileResponse>>('/files', { params }).then(r => r.data),
  getById: (id: number) => apiClient.get<FileResponse>(`/files/${id}`).then(r => r.data),
  upload: (file: File, folderId?: number, clientUploadId?: string, onProgress?: (pct: number) => void) => {
    const form = new FormData()
    form.append('file', file)
    if (folderId != null) form.append('folderId', String(folderId))
    if (clientUploadId) form.append('clientUploadId', clientUploadId)
    return apiClient.post<FileResponse>('/files', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress
        ? (e) => { if (e.total) onProgress(Math.round((e.loaded / e.total) * 100)) }
        : undefined,
    }).then(r => r.data)
  },
  replaceContent: (id: number, file: File, onProgress?: (pct: number) => void) => {
    const form = new FormData()
    form.append('file', file)
    return apiClient.put<FileResponse>(`/files/${id}/content`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress
        ? (e) => { if (e.total) onProgress(Math.round((e.loaded / e.total) * 100)) }
        : undefined,
    }).then(r => r.data)
  },
  update: (id: number, data: { filename?: string; description?: string; folderId?: number; isChildSafe?: boolean }) =>
    apiClient.put<FileResponse>(`/files/${id}`, data).then(r => r.data),
  delete: (id: number) => apiClient.delete(`/files/${id}`),
}

// Tags
export const tagApi = {
  list: (q?: string) =>
    apiClient.get<TagResponse[]>('/tags', { params: q ? { q } : {} }).then(r => r.data),
  create: (data: { name: string; color?: string }) =>
    apiClient.post<TagResponse>('/tags', data).then(r => r.data),
  update: (id: number, data: { name?: string; color?: string }) =>
    apiClient.put<TagResponse>(`/tags/${id}`, data).then(r => r.data),
  delete: (id: number) => apiClient.delete(`/tags/${id}`),
  getForNote: (entityId: number) =>
    apiClient.get<TagResponse[]>(`/tags/notes/${entityId}/tags`).then(r => r.data),
  attachToNote: (entityId: number, tagIds: number[]) =>
    apiClient.post<TagResponse[]>(`/tags/notes/${entityId}/tags`, { tagIds }).then(r => r.data),
  detachFromNote: (entityId: number, tagId: number) =>
    apiClient.delete(`/tags/notes/${entityId}/tags/${tagId}`),
  getForFile: (entityId: number) =>
    apiClient.get<TagResponse[]>(`/tags/files/${entityId}/tags`).then(r => r.data),
  attachToFile: (entityId: number, tagIds: number[]) =>
    apiClient.post<TagResponse[]>(`/tags/files/${entityId}/tags`, { tagIds }).then(r => r.data),
  detachFromFile: (entityId: number, tagId: number) =>
    apiClient.delete(`/tags/files/${entityId}/tags/${tagId}`),
  merge: (sourceId: number, targetId: number) =>
    apiClient.post(`/tags/${sourceId}/merge`, { targetId }),
  bulkUpdate: (body: {
    items: { type: 'note' | 'file' | 'folder'; id: number }[]
    addTagIds?: number[]
    removeTagIds?: number[]
  }) => apiClient.post<{ mutated: number }>('/tags/bulk', body).then(r => r.data),
}

// Search
export const searchApi = {
  search: (params: { q: string; types?: string[]; folderId?: number; tagIds?: number[]; page?: number; size?: number }) =>
    apiClient.get<SearchResponse>('/search', { params }).then(r => r.data),
}

export interface DataExportResponse {
  id: number
  status: 'PENDING' | 'READY' | 'EXPIRED' | 'FAILED'
  sizeBytes: number | null
  createdAt: string
  readyAt: string | null
  expiresAt: string | null
  downloadUrl: string | null
  errorMessage: string | null
}

// Me — ambient state for the current user (badge count, etc.)
export const meApi = {
  unread: () => apiClient.get<{ count: number }>('/me/unread').then(r => r.data),
  getNotificationPrefs: () =>
    apiClient.get<Record<string, unknown>>('/me/notification-prefs').then(r => r.data),
  updateNotificationPrefs: (prefs: Record<string, unknown>) =>
    apiClient.put<Record<string, unknown>>('/me/notification-prefs', prefs).then(r => r.data),
  requestExport: () =>
    apiClient.post<DataExportResponse>('/me/export').then(r => r.data),
  listExports: () =>
    apiClient.get<DataExportResponse[]>('/me/export').then(r => r.data),
  getExport: (id: number) =>
    apiClient.get<DataExportResponse>(`/me/export/${id}`).then(r => r.data),
}

export interface BuildInfo {
  build?: { name?: string; version?: string; time?: string }
  git?: { branch?: string; commitId?: string; commitTime?: string; tags?: string }
}

export const buildInfoApi = {
  get: () => apiClient.get<BuildInfo>('/info').then(r => r.data).catch(() => ({} as BuildInfo)),
}

// Reminder actions surfaced from push notifications.
export const reminderActionApi = {
  done: (id: number) => apiClient.post(`/reminders/${id}/done`),
  snoozeMinutes: (id: number, minutes: number) =>
    apiClient.post(`/reminders/${id}/snooze`, { minutes }),
}

// Saved searches
export const savedSearchApi = {
  list: () => apiClient.get<SavedSearch[]>('/saved-searches').then(r => r.data),
  create: (body: { name: string; query: string }) =>
    apiClient.post<SavedSearch>('/saved-searches', body).then(r => r.data),
  delete: (id: number) => apiClient.delete(`/saved-searches/${id}`),
}

// Trash
export const trashApi = {
  list: () => apiClient.get<{ notes: TrashItem[]; files: TrashItem[]; folders: TrashItem[] }>('/trash').then(r => r.data),
  restoreNote: (id: number) => apiClient.post(`/notes/${id}/restore`),
  restoreFile: (id: number) => apiClient.post(`/files/${id}/restore`),
  restoreFolder: (id: number) => apiClient.post(`/folders/${id}/restore`),
}

export interface TrashItem {
  id: number
  type: string
  name: string
  deletedAt: string
}

// Comments + mentions
export interface CommentResponse {
  id: number
  itemType: 'note' | 'file'
  itemId: number
  authorId: number | null
  authorDisplayName: string | null
  body: string
  mentionedUserIds: number[]
  createdAt: string
  editedAt: string | null
}

export interface MentionInboxItem {
  id: number
  commentId: number
  itemType: 'note' | 'file'
  itemId: number
  authorId: number | null
  authorDisplayName: string | null
  preview: string
  readAt: string | null
  createdAt: string
}

export const commentApi = {
  list: (itemType: 'note' | 'file', itemId: number) =>
    apiClient.get<CommentResponse[]>('/comments', { params: { itemType, itemId } }).then(r => r.data),
  create: (itemType: 'note' | 'file', itemId: number, body: { body: string; mentionedUserIds?: number[]; mentionedGroupIds?: number[] }) =>
    apiClient.post<CommentResponse>('/comments', body, { params: { itemType, itemId } }).then(r => r.data),
  update: (id: number, body: { body: string; mentionedUserIds?: number[]; mentionedGroupIds?: number[] }) =>
    apiClient.put<CommentResponse>(`/comments/${id}`, body).then(r => r.data),
  delete: (id: number) => apiClient.delete(`/comments/${id}`),
}

export const mentionApi = {
  list: () => apiClient.get<MentionInboxItem[]>('/me/mentions').then(r => r.data),
  unreadCount: () => apiClient.get<{ count: number }>('/me/mentions/unread-count').then(r => r.data),
  markRead: (id: number) => apiClient.post(`/me/mentions/${id}/read`),
  markAllRead: () => apiClient.post('/me/mentions/read-all'),
}

// Groups
export interface GroupResponse {
  id: number
  name: string
  kind: 'CUSTOM' | 'SYSTEM_EVERYONE' | 'SYSTEM_ADULTS' | 'SYSTEM_KIDS'
  isSystem: boolean
  memberUserIds: number[]
  createdAt: string
}

export const groupApi = {
  list: () => apiClient.get<GroupResponse[]>('/groups').then(r => r.data),
  getById: (id: number) => apiClient.get<GroupResponse>(`/groups/${id}`).then(r => r.data),
  create: (data: { name: string; memberUserIds: number[] }) =>
    apiClient.post<GroupResponse>('/groups', data).then(r => r.data),
  update: (id: number, data: { name: string; memberUserIds: number[] }) =>
    apiClient.put<GroupResponse>(`/groups/${id}`, data).then(r => r.data),
  delete: (id: number) => apiClient.delete(`/groups/${id}`),
}

// Admin
export const adminApi = {
  listUsers: () => apiClient.get('/admin/users').then(r => r.data),
  createUser: (data: unknown) => apiClient.post('/admin/users', data).then(r => r.data),
  updateUser: (id: number, data: unknown) => apiClient.put(`/admin/users/${id}`, data).then(r => r.data),
  deleteUser: (id: number) => apiClient.delete(`/admin/users/${id}`),
  resetPassword: (id: number, newPassword: string) =>
    apiClient.post(`/admin/users/${id}/reset-password`, { newPassword }),
  revokeUserSessions: (id: number) =>
    apiClient.post(`/admin/users/${id}/sessions/revoke`),
  getAuditLog: (params: { page?: number; size?: number; actorId?: number; action?: string; from?: string; to?: string }) =>
    apiClient.get('/admin/audit', { params }).then(r => r.data),
}
