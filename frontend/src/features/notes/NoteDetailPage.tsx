import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeSanitize from 'rehype-sanitize'
import { noteApi, checklistApi, tagApi } from '../../api'
import { QK } from '../../lib/queryKeys'
import { toast } from '../../lib/toastStore'
import AppLayout from '../../components/AppLayout'
import RemindersSection from '../../components/RemindersSection'
import CommentsThread from '../comments/CommentsThread'
import TagAutocomplete from '../../components/TagAutocomplete'
import { useAuthStore } from '../../lib/authStore'
import { useOfflineNote } from '../../lib/useOfflineNote'
import VisibilityControl from '../../components/VisibilityControl'
import ItemBreadcrumb from '../../components/ItemBreadcrumb'
import RevisionsPanel from './RevisionsPanel'

function formatRelative(ts: number): string {
  const s = Math.max(0, Math.floor((Date.now() - ts) / 1000))
  if (s < 60) return 'just now'
  if (s < 3600) return `${Math.floor(s / 60)} min ago`
  if (s < 86400) return `${Math.floor(s / 3600)} h ago`
  return `${Math.floor(s / 86400)} d ago`
}

export default function NoteDetailPage() {
  const { id } = useParams<{ id: string }>()
  const noteId = Number(id)
  const qc = useQueryClient()
  const navigate = useNavigate()
  const user = useAuthStore(s => s.user)
  const [newItemText, setNewItemText] = useState('')

  const { note, isLoading, isOffline, cachedAt } = useOfflineNote(noteId)

  const { data: noteTags = [] } = useQuery({
    queryKey: QK.noteTags(noteId),
    queryFn: () => tagApi.getForNote(noteId),
  })

  const deleteNote = useMutation({
    mutationFn: () => noteApi.delete(noteId),
    onSuccess: () => navigate(note?.folderId ? `/folders/${note.folderId}` : '/'),
    onError: () => toast.error('Failed to delete note'),
  })

  const togglePin = useMutation({
    mutationFn: () => note?.pinnedAt ? noteApi.unpin(noteId) : noteApi.pin(noteId),
    onSuccess: () => qc.invalidateQueries({ queryKey: QK.note(noteId) }),
    onError: () => toast.error('Failed to update pin'),
  })

  async function downloadExport(format: 'md' | 'pdf') {
    try {
      const blob = await noteApi.export(noteId, format)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${(note?.title ?? 'note').replace(/[^\w.-]+/g, '_')}.${format}`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    } catch {
      toast.error('Export failed')
    }
  }

  const addItem = useMutation({
    mutationFn: (text: string) => checklistApi.add(noteId, { text }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QK.note(noteId) }); setNewItemText('') },
    onError: () => toast.error('Failed to add checklist item'),
  })

  const toggleItem = useMutation({
    mutationFn: ({ itemId, checked }: { itemId: number; checked: boolean }) =>
      checklistApi.update(noteId, itemId, { isChecked: checked }),
    onSuccess: () => qc.invalidateQueries({ queryKey: QK.note(noteId) }),
    onError: () => toast.error('Failed to update checklist item'),
  })

  const deleteItem = useMutation({
    mutationFn: (itemId: number) => checklistApi.delete(noteId, itemId),
    onSuccess: () => qc.invalidateQueries({ queryKey: QK.note(noteId) }),
    onError: () => toast.error('Failed to delete checklist item'),
  })

  if (isLoading) return <AppLayout><div className="text-gray-400 dark:text-gray-500">Loading…</div></AppLayout>
  if (!note) return <AppLayout><div className="text-red-500">Note not found</div></AppLayout>

  const isShoppingList = note.label === 'shopping_list'

  return (
    <AppLayout>
      <div className="max-w-2xl mx-auto space-y-6">
        {isOffline && cachedAt != null && (
          <div className="rounded-lg border border-amber-300 dark:border-amber-700 bg-amber-50 dark:bg-amber-950 px-3 py-2 text-xs text-amber-800 dark:text-amber-200">
            Offline — showing cached copy from {formatRelative(cachedAt)}.
          </div>
        )}

        <ItemBreadcrumb folderId={note.folderId} itemTitle={note.title} />

        {/* Header */}
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1 flex-wrap">
              <span className="text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 px-2 py-0.5 rounded-full">
                {note.label.replace('_', ' ')}
              </span>
              {note.isChildSafe && (
                <span className="text-xs bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300 px-2 py-0.5 rounded-full">Child safe</span>
              )}
            </div>
            <h1 className={`font-bold text-gray-900 dark:text-gray-100 ${isShoppingList ? 'text-2xl' : 'text-xl'}`}>
              {note.title}
            </h1>
          </div>
          {!user?.isChild && (
            <div className="flex gap-2 shrink-0 items-center">
              <button
                onClick={() => togglePin.mutate()}
                disabled={togglePin.isPending}
                aria-pressed={!!note.pinnedAt}
                title={note.pinnedAt ? 'Unpin' : 'Pin to top'}
                className={`px-2 py-1.5 text-sm rounded-lg border transition-colors ${
                  note.pinnedAt
                    ? 'bg-amber-100 dark:bg-amber-900/40 border-amber-300 dark:border-amber-700 text-amber-700 dark:text-amber-300'
                    : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-500 dark:text-gray-400 hover:text-amber-600'
                }`}
              >
                {note.pinnedAt ? '📌 Pinned' : '📌 Pin'}
              </button>
              <ExportMenu onPick={downloadExport} />
              <Link to={`/notes/${noteId}/edit`}
                className="px-3 py-1.5 text-sm bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700">
                Edit
              </Link>
              <button
                onClick={() => { if (confirm('Delete this note?')) deleteNote.mutate() }}
                className="px-3 py-1.5 text-sm text-red-600 dark:text-red-400 border border-red-200 dark:border-red-700 rounded-lg hover:bg-red-50 dark:hover:bg-red-950">
                Delete
              </button>
            </div>
          )}
        </div>

        {/* Tags */}
        <TagAutocomplete
          entityType="note"
          entityId={noteId}
          currentTags={noteTags}
          onTagsChange={() => qc.invalidateQueries({ queryKey: QK.noteTags(noteId) })}
          readOnly={user?.isChild}
        />

        <VisibilityControl itemType="note" itemId={noteId} ownerId={note.ownerId} />

        {/* Body */}
        {note.body && (
          <div className="prose prose-sm dark:prose-invert max-w-none text-gray-700 dark:text-gray-300">
            <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeSanitize]}>{note.body}</ReactMarkdown>
          </div>
        )}

        {/* Checklist */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 mb-2">Checklist</h2>
          {note.checklistItems.length === 0 && (
            <p className="text-xs text-gray-400 dark:text-gray-500 mb-2">No items yet.</p>
          )}
          <ul className="space-y-1">
            {note.checklistItems.map(item => (
              <li key={item.id} className="flex items-center gap-3 group">
                <input
                  type="checkbox"
                  checked={item.isChecked}
                  onChange={e => toggleItem.mutate({ itemId: item.id, checked: e.target.checked })}
                  className="w-4 h-4 rounded text-primary-600"
                />
                <span className={`flex-1 ${isShoppingList ? 'text-lg' : 'text-sm'} ${item.isChecked ? 'line-through text-gray-400 dark:text-gray-500' : 'text-gray-700 dark:text-gray-300'}`}>
                  {item.text}
                </span>
                {!user?.isChild && (
                  <button onClick={() => deleteItem.mutate(item.id)}
                    className="opacity-0 group-hover:opacity-100 text-xs text-red-400 hover:text-red-600">
                    ✕
                  </button>
                )}
              </li>
            ))}
          </ul>
          <div className="flex gap-2 mt-2">
            <input
              value={newItemText}
              onChange={e => setNewItemText(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter' && newItemText.trim()) addItem.mutate(newItemText.trim()) }}
              placeholder="Add item…"
              className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-1.5 text-sm text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <button
              onClick={() => { if (newItemText.trim()) addItem.mutate(newItemText.trim()) }}
              className="px-3 py-1.5 text-sm bg-primary-600 text-white rounded-lg">
              Add
            </button>
          </div>
        </section>

        {/* Reminders */}
        <RemindersSection
          noteId={noteId}
          reminders={note.reminders}
          readOnly={user?.isChild}
        />

        <CommentsThread itemType="note" itemId={noteId} />

        {!user?.isChild && <RevisionsPanel noteId={noteId} />}
      </div>
    </AppLayout>
  )
}

/**
 * Two-button popover for download formats. Implemented as a tiny details/summary
 * native disclosure so we don't drag in a popover library; click-outside is
 * handled by the browser closing the open <details> when focus leaves.
 */
function ExportMenu({ onPick }: { onPick: (fmt: 'md' | 'pdf') => void }) {
  return (
    <details className="relative">
      <summary className="list-none cursor-pointer px-3 py-1.5 text-sm bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700">
        Export ▾
      </summary>
      <div className="absolute right-0 mt-1 w-32 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 shadow-lg z-10 py-1">
        <button
          onClick={() => onPick('md')}
          className="block w-full text-left px-3 py-1.5 text-sm hover:bg-gray-50 dark:hover:bg-gray-700"
        >
          Markdown
        </button>
        <button
          onClick={() => onPick('pdf')}
          className="block w-full text-left px-3 py-1.5 text-sm hover:bg-gray-50 dark:hover:bg-gray-700"
        >
          PDF
        </button>
      </div>
    </details>
  )
}
