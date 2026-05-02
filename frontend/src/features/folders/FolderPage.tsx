import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  DndContext,
  PointerSensor,
  useDraggable,
  useDroppable,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core'
import { folderApi, noteApi, fileApi, itemMoveApi, type BulkMoveItem } from '../../api'
import { QK } from '../../lib/queryKeys'
import { toast } from '../../lib/toastStore'
import AppLayout from '../../components/AppLayout'
import type { FolderResponse } from '../../types'

export default function FolderPage() {
  const { id } = useParams<{ id: string }>()
  const folderId = Number(id)
  const qc = useQueryClient()
  const navigate = useNavigate()

  const [showNewFolder, setShowNewFolder] = useState(false)
  const [newFolderName, setNewFolderName] = useState('')

  const [showRename, setShowRename] = useState(false)
  const [renameValue, setRenameValue] = useState('')
  const [renameDesc, setRenameDesc] = useState('')
  const [renameColor, setRenameColor] = useState<string | null>(null)
  const [renameIcon, setRenameIcon] = useState<string | null>(null)

  const [showMove, setShowMove] = useState(false)
  const [moveToId, setMoveToId] = useState<string>('')

  // Bulk selection: keys are "{type}-{id}" so the Set can hold heterogeneous
  // items without collisions. Cleared on folder change because the keys are
  // only meaningful on this page's listing.
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [bulkMoveOpen, setBulkMoveOpen] = useState(false)
  const [bulkMoveTo, setBulkMoveTo] = useState<string>('')
  function toggleSelect(key: string) {
    setSelected(prev => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key); else next.add(key)
      return next
    })
  }
  function clearSelection() { setSelected(new Set()) }

  const { data: folder } = useQuery({
    queryKey: QK.folder(folderId),
    queryFn: () => folderApi.getById(folderId),
  })
  const { data: allFolders } = useQuery({
    queryKey: QK.folders(),
    queryFn: () => folderApi.getTree(),
    enabled: showMove || bulkMoveOpen,
  })
  const { data: notesPage } = useQuery({
    queryKey: QK.notes({ folderId }),
    queryFn: () => noteApi.list({ folderId }),
  })
  const { data: filesPage } = useQuery({
    queryKey: QK.files({ folderId }),
    queryFn: () => fileApi.list({ folderId }),
  })

  const createSubfolder = useMutation({
    mutationFn: () => folderApi.create({ name: newFolderName, parentId: folderId }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QK.folders() })
      setShowNewFolder(false)
      setNewFolderName('')
    },
  })

  const renameFolder = useMutation({
    mutationFn: () => folderApi.update(folderId, {
      name: renameValue,
      description: renameDesc || undefined,
      // Empty-string color/icon → null on the server (clears the value).
      color: renameColor === null ? undefined : renameColor || null,
      icon: renameIcon === null ? undefined : renameIcon || null,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QK.folder(folderId) })
      qc.invalidateQueries({ queryKey: QK.folders() })
      setShowRename(false)
    },
  })

  const moveFolder = useMutation({
    mutationFn: () => folderApi.update(folderId, { parentId: moveToId === '' ? null : Number(moveToId) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QK.folders() })
      setShowMove(false)
    },
  })

  const toggleChildSafe = useMutation({
    mutationFn: (safe: boolean) => folderApi.setChildSafe(folderId, safe),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QK.folder(folderId) })
      qc.invalidateQueries({ queryKey: QK.folders() })
    },
  })

  const deleteFolder = useMutation({
    mutationFn: (force: boolean) => folderApi.delete(folderId, force),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QK.folders() }); navigate('/') },
  })

  const archive = useMutation({
    mutationFn: () => folderApi.archive(folderId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QK.folder(folderId) })
      qc.invalidateQueries({ queryKey: QK.folders() })
      toast.success('Archived')
    },
    onError: () => toast.error('Could not archive'),
  })
  const unarchive = useMutation({
    mutationFn: () => folderApi.unarchive(folderId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QK.folder(folderId) })
      qc.invalidateQueries({ queryKey: QK.folders() })
      toast.success('Restored from archive')
    },
    onError: () => toast.error('Could not unarchive'),
  })

  // Drag uses the standard pointer sensor with an 8px activation distance —
  // anything shorter steals click navigation on the linked cards.
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }))

  const moveItems = useMutation({
    mutationFn: ({ items, targetFolderId }: { items: BulkMoveItem[]; targetFolderId: number | null }) =>
      itemMoveApi.move(items, targetFolderId),
    onSuccess: ({ moved }, vars) => {
      qc.invalidateQueries({ queryKey: QK.folders() })
      qc.invalidateQueries({ queryKey: QK.notes({ folderId }) })
      qc.invalidateQueries({ queryKey: QK.files({ folderId }) })
      if (vars.targetFolderId != null) qc.invalidateQueries({ queryKey: QK.folder(vars.targetFolderId) })
      toast.success(`Moved ${moved} item${moved === 1 ? '' : 's'}`)
    },
    onError: (err: { response?: { data?: { code?: string } } }) => {
      const code = err.response?.data?.code
      toast.error(code === 'CYCLE_DETECTED' ? 'Cannot move a folder into itself' : 'Move failed')
    },
  })

  function handleDragEnd(e: DragEndEvent) {
    if (!e.over) return
    const item = e.active.data.current as BulkMoveItem | undefined
    const target = e.over.data.current as { folderId: number | null } | undefined
    if (!item || !target) return
    // No-op when dropped on the same parent folder.
    if (target.folderId === folderId && (item.type !== 'folder')) return
    moveItems.mutate({ items: [item], targetFolderId: target.folderId })
  }

  function selectedAsItems(): BulkMoveItem[] {
    return [...selected].map(key => {
      const [type, id] = key.split('-')
      return { type: type as BulkMoveItem['type'], id: Number(id) }
    })
  }

  const bulkDelete = useMutation({
    // Per-type delete because there's no bulk-delete endpoint; ordering
    // (notes → files → folders) keeps any cycle-detection on folder
    // dependents from firing before their contents are gone.
    mutationFn: async () => {
      const items = selectedAsItems()
      for (const it of items) {
        if (it.type === 'note') await noteApi.delete(it.id)
        else if (it.type === 'file') await fileApi.delete(it.id)
      }
      const folders = items.filter(i => i.type === 'folder')
      for (const f of folders) await folderApi.delete(f.id, false)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QK.folder(folderId) })
      qc.invalidateQueries({ queryKey: QK.folders() })
      qc.invalidateQueries({ queryKey: QK.notes({ folderId }) })
      qc.invalidateQueries({ queryKey: QK.files({ folderId }) })
      toast.success(`Deleted ${selected.size} item${selected.size === 1 ? '' : 's'}`)
      clearSelection()
    },
    onError: () => toast.error('Bulk delete failed'),
  })

  const bulkMove = useMutation({
    mutationFn: () => {
      const target = bulkMoveTo === '' ? null : Number(bulkMoveTo)
      return itemMoveApi.move(selectedAsItems(), target)
    },
    onSuccess: ({ moved }) => {
      qc.invalidateQueries({ queryKey: QK.folder(folderId) })
      qc.invalidateQueries({ queryKey: QK.folders() })
      qc.invalidateQueries({ queryKey: QK.notes({ folderId }) })
      qc.invalidateQueries({ queryKey: QK.files({ folderId }) })
      toast.success(`Moved ${moved} item${moved === 1 ? '' : 's'}`)
      clearSelection()
      setBulkMoveOpen(false)
      setBulkMoveTo('')
    },
    onError: (err: { response?: { data?: { code?: string } } }) => {
      const code = err.response?.data?.code
      toast.error(code === 'CYCLE_DETECTED' ? 'Cannot move a folder into itself' : 'Move failed')
    },
  })

  function openRename() {
    setRenameValue(folder?.name ?? '')
    setRenameDesc(folder?.description ?? '')
    setRenameColor(folder?.color ?? '')
    setRenameIcon(folder?.icon ?? '')
    setShowRename(true)
  }

  function flattenFolders(folders: FolderResponse[], depth = 0): { id: number; label: string }[] {
    return folders.flatMap(f => [
      { id: f.id, label: '  '.repeat(depth) + f.name },
      ...flattenFolders(f.children, depth + 1),
    ])
  }

  if (!folder) return <AppLayout><div className="text-gray-400 dark:text-gray-500">Loading…</div></AppLayout>

  // ancestors include the folder itself as the last entry — drop it for the
  // breadcrumb trail so the trailing item isn't a self-link.
  const breadcrumbs = (folder.ancestors ?? []).slice(0, -1)

  return (
    <AppLayout>
      <DndContext sensors={sensors} onDragEnd={handleDragEnd}>
      <div className="max-w-3xl mx-auto space-y-6">
        {breadcrumbs.length > 0 && (
          <nav aria-label="Folder breadcrumb" className="text-sm text-gray-500 dark:text-gray-400">
            <ol className="flex flex-wrap items-center gap-1">
              {breadcrumbs.map((crumb, idx) => {
                const isImmediateParent = idx === breadcrumbs.length - 1
                return (
                  <li key={crumb.id} className="flex items-center gap-1">
                    {isImmediateParent ? (
                      <BreadcrumbDropTarget folderId={crumb.id}>
                        <Link
                          to={`/folders/${crumb.id}`}
                          className="hover:text-primary-600 dark:hover:text-primary-400 hover:underline"
                        >
                          {crumb.name}
                        </Link>
                      </BreadcrumbDropTarget>
                    ) : (
                      <Link
                        to={`/folders/${crumb.id}`}
                        className="hover:text-primary-600 dark:hover:text-primary-400 hover:underline"
                      >
                        {crumb.name}
                      </Link>
                    )}
                    <span aria-hidden="true">/</span>
                  </li>
                )
              })}
            </ol>
          </nav>
        )}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              {folder.color && (
                <span
                  aria-hidden="true"
                  className="inline-block w-4 h-4 rounded"
                  style={{ backgroundColor: folder.color }}
                />
              )}
              {folder.icon && <span aria-hidden="true">{folder.icon}</span>}
              {folder.name}
            </h1>
            {folder.description && <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">{folder.description}</p>}
            {folder.isChildSafe && (
              <span className="inline-block mt-1 text-xs bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 px-2 py-0.5 rounded-full">
                Child-safe
              </span>
            )}
            {folder.archivedAt && (
              <span className="inline-block mt-1 ml-2 text-xs bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400 px-2 py-0.5 rounded-full">
                Archived
              </span>
            )}
          </div>
          <div className="flex gap-2 flex-wrap justify-end">
            <button
              onClick={() => setShowNewFolder(true)}
              className="px-3 py-1.5 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700"
            >
              + Subfolder
            </button>
            <Link
              to={`/notes/new?folderId=${folderId}`}
              className="px-3 py-1.5 text-sm bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              + Note
            </Link>
            <button
              onClick={openRename}
              className="px-3 py-1.5 text-sm bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              Rename
            </button>
            <button
              onClick={() => setShowMove(true)}
              className="px-3 py-1.5 text-sm bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              Move
            </button>
          </div>
        </div>

        {/* Rename modal */}
        {showRename && (
          <div className="p-4 bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700 rounded-xl space-y-3">
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Edit folder</h3>
            <input
              autoFocus
              value={renameValue}
              onChange={e => setRenameValue(e.target.value)}
              placeholder="Folder name"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <input
              value={renameDesc}
              onChange={e => setRenameDesc(e.target.value)}
              placeholder="Description (optional)"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <FolderColorPicker value={renameColor ?? ''} onChange={setRenameColor} />
            <FolderIconPicker value={renameIcon ?? ''} onChange={setRenameIcon} />
            <div className="flex gap-2">
              <button
                onClick={() => renameFolder.mutate()}
                disabled={!renameValue.trim()}
                className="px-3 py-2 text-sm bg-primary-600 text-white rounded-lg disabled:opacity-50"
              >
                Save
              </button>
              <button
                onClick={() => setShowRename(false)}
                className="px-3 py-2 text-sm bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg"
              >
                Cancel
              </button>
            </div>
          </div>
        )}

        {/* Move modal */}
        {showMove && (
          <div className="p-4 bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700 rounded-xl space-y-3">
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Move to folder</h3>
            <select
              value={moveToId}
              onChange={e => setMoveToId(e.target.value)}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="">— Root (no parent) —</option>
              {allFolders && flattenFolders(allFolders)
                .filter(f => f.id !== folderId)
                .map(f => (
                  <option key={f.id} value={String(f.id)}>{f.label}</option>
                ))}
            </select>
            <div className="flex gap-2">
              <button
                onClick={() => moveFolder.mutate()}
                className="px-3 py-2 text-sm bg-primary-600 text-white rounded-lg"
              >
                Move
              </button>
              <button
                onClick={() => setShowMove(false)}
                className="px-3 py-2 text-sm bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg"
              >
                Cancel
              </button>
            </div>
          </div>
        )}

        {/* Child-safe toggle */}
        <div className="flex items-center gap-3 p-3 bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-800 rounded-xl">
          <div className="flex-1">
            <p className="text-sm font-medium text-blue-900 dark:text-blue-200">Child-safe mode</p>
            <p className="text-xs text-blue-700 dark:text-blue-400 mt-0.5">
              {folder.isChildSafe
                ? 'Enabled — child accounts can see this folder.'
                : 'Disabled — hidden from child accounts. Enabling will cascade to contents.'}
            </p>
          </div>
          <button
            onClick={() => {
              if (!folder.isChildSafe) {
                toggleChildSafe.mutate(true)
              } else if (confirm('Disable child-safe? Child accounts will no longer see this folder and its contents.')) {
                toggleChildSafe.mutate(false)
              }
            }}
            disabled={toggleChildSafe.isPending}
            className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
              folder.isChildSafe
                ? 'bg-green-600 text-white hover:bg-green-700'
                : 'bg-white dark:bg-gray-800 border border-blue-300 dark:border-blue-700 text-blue-700 dark:text-blue-400 hover:bg-blue-100 dark:hover:bg-blue-900/50'
            }`}
          >
            {folder.isChildSafe ? 'Enabled' : 'Enable'}
          </button>
        </div>

        {showNewFolder && (
          <div className="flex gap-2">
            <input
              autoFocus
              value={newFolderName}
              onChange={e => setNewFolderName(e.target.value)}
              placeholder="Folder name"
              className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <button onClick={() => createSubfolder.mutate()}
              className="px-3 py-2 text-sm bg-primary-600 text-white rounded-lg">Create</button>
            <button onClick={() => setShowNewFolder(false)}
              className="px-3 py-2 text-sm bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg">Cancel</button>
          </div>
        )}

        {/* Sub-folders */}
        {folder.children.length > 0 && (
          <section>
            <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">Subfolders</h2>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
              {folder.children.map(child => (
                <SubfolderCard
                  key={child.id}
                  child={child}
                  selected={selected.has(`folder-${child.id}`)}
                  onToggleSelect={() => toggleSelect(`folder-${child.id}`)}
                />
              ))}
            </div>
          </section>
        )}

        {/* Notes */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">Notes</h2>
          {notesPage?.content.length === 0 ? (
            <p className="text-sm text-gray-400 dark:text-gray-500">No notes yet.</p>
          ) : (
            <div className="space-y-2">
              {[...(notesPage?.content ?? [])]
                // Pinned-first stable sort within the folder view; matches
                // NotesListPage which renders Pinned and Others as separate
                // sections.
                .sort((a, b) => Number(!!b.pinnedAt) - Number(!!a.pinnedAt))
                .map(note => (
                  <DraggableNoteRow
                    key={note.id}
                    note={note}
                    selected={selected.has(`note-${note.id}`)}
                    onToggleSelect={() => toggleSelect(`note-${note.id}`)}
                  />
                ))}
            </div>
          )}
        </section>

        {/* Files */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">Files</h2>
          {filesPage?.content.length === 0 ? (
            <p className="text-sm text-gray-400 dark:text-gray-500">No files yet.</p>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {filesPage?.content.map(file => (
                <DraggableFileCard
                  key={file.id}
                  file={file}
                  selected={selected.has(`file-${file.id}`)}
                  onToggleSelect={() => toggleSelect(`file-${file.id}`)}
                />
              ))}
            </div>
          )}
        </section>

        <div className="pt-4 border-t border-gray-200 dark:border-gray-700 flex items-center gap-4">
          {folder.archivedAt ? (
            <button
              onClick={() => unarchive.mutate()}
              disabled={unarchive.isPending}
              className="text-sm text-amber-600 hover:text-amber-700 disabled:opacity-50"
            >
              Unarchive
            </button>
          ) : (
            <button
              onClick={() => { if (confirm('Archive this folder? It will be hidden from the main tree but its contents stay searchable.')) archive.mutate() }}
              disabled={archive.isPending}
              className="text-sm text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 disabled:opacity-50"
            >
              Archive
            </button>
          )}
          <button
            onClick={() => { if (confirm('Delete this folder?')) deleteFolder.mutate(false) }}
            className="text-sm text-red-500 hover:text-red-700 ml-auto"
          >
            Delete folder
          </button>
        </div>
      </div>
      </DndContext>

      {selected.size > 0 && (
        <div
          role="region"
          aria-label="Selection actions"
          className="fixed bottom-4 left-1/2 -translate-x-1/2 z-20 flex items-center gap-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 px-4 py-2 shadow-lg"
        >
          <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
            {selected.size} selected
          </span>
          <button
            type="button"
            onClick={() => setBulkMoveOpen(true)}
            className="text-sm px-3 py-1 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            Move…
          </button>
          <button
            type="button"
            onClick={() => {
              if (confirm(`Delete ${selected.size} selected item${selected.size === 1 ? '' : 's'}? Notes and files go to trash; folders are removed if empty.`)) {
                bulkDelete.mutate()
              }
            }}
            disabled={bulkDelete.isPending}
            className="text-sm px-3 py-1 rounded-lg border border-red-300 dark:border-red-700 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950 disabled:opacity-50"
          >
            Delete
          </button>
          <button
            type="button"
            onClick={clearSelection}
            className="text-xs text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200"
          >
            Clear
          </button>
        </div>
      )}

      {bulkMoveOpen && (
        <div
          role="dialog"
          aria-label="Move selected items"
          className="fixed inset-0 z-30 flex items-center justify-center bg-black/40"
          onClick={() => setBulkMoveOpen(false)}
        >
          <div
            className="w-80 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 space-y-3"
            onClick={e => e.stopPropagation()}
          >
            <h3 className="text-sm font-semibold text-gray-800 dark:text-gray-200">
              Move {selected.size} item{selected.size === 1 ? '' : 's'}
            </h3>
            <select
              value={bulkMoveTo}
              onChange={e => setBulkMoveTo(e.target.value)}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm"
            >
              <option value="">— Root (no parent) —</option>
              {allFolders && flattenFolders(allFolders)
                .filter(f => !selected.has(`folder-${f.id}`))
                .map(f => (
                  <option key={f.id} value={String(f.id)}>{f.label}</option>
                ))}
            </select>
            <div className="flex gap-2">
              <button
                onClick={() => bulkMove.mutate()}
                disabled={bulkMove.isPending}
                className="px-3 py-2 text-sm bg-primary-600 text-white rounded-lg disabled:opacity-50"
              >
                Move
              </button>
              <button
                onClick={() => setBulkMoveOpen(false)}
                className="px-3 py-2 text-sm bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </AppLayout>
  )
}

/**
 * Subfolder card — draggable AND droppable. Drop another item onto it to
 * move the item into this subfolder. Drag the card itself to move the
 * subfolder elsewhere (drop on breadcrumb to bubble up, or onto another
 * subfolder to nest).
 */
function SubfolderCard({ child, selected, onToggleSelect }: {
  child: FolderResponse
  selected: boolean
  onToggleSelect: () => void
}) {
  const draggable = useDraggable({
    id: `folder-${child.id}`,
    data: { type: 'folder', id: child.id } satisfies BulkMoveItem,
  })
  const droppable = useDroppable({
    id: `drop-folder-${child.id}`,
    data: { folderId: child.id },
  })
  const dragging = draggable.isDragging
  const over = droppable.isOver
  return (
    <div
      ref={node => { draggable.setNodeRef(node); droppable.setNodeRef(node) }}
      {...draggable.attributes}
      {...draggable.listeners}
      className={`flex items-center gap-2 p-3 rounded-xl border bg-white dark:bg-gray-800 transition-colors ${
        dragging ? 'opacity-50 cursor-grabbing' : 'cursor-grab'
      } ${selected ? 'ring-2 ring-primary-500' : ''} ${over ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20' : 'border-gray-200 dark:border-gray-700 hover:border-primary-300'}`}
    >
      <input
        type="checkbox"
        checked={selected}
        aria-label={`Select folder ${child.name}`}
        onChange={onToggleSelect}
        // Stop pointer-down so the dnd-kit sensor doesn't claim the click
        // and start a drag instead of toggling the checkbox.
        onPointerDown={e => e.stopPropagation()}
        onClick={e => e.stopPropagation()}
        className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
      />
      {child.color && (
        <span aria-hidden="true" className="inline-block w-2 h-2 rounded-full shrink-0"
          style={{ backgroundColor: child.color }} />
      )}
      <span aria-hidden="true">{child.icon || '📁'}</span>
      <Link
        to={`/folders/${child.id}`}
        // Don't navigate on drag; the link click only fires when no movement
        // crossed the activation distance.
        onClick={e => { if (dragging) e.preventDefault() }}
        className="text-sm truncate flex-1 hover:text-primary-600 dark:hover:text-primary-400"
      >
        {child.name}
      </Link>
    </div>
  )
}

function DraggableNoteRow({ note, selected, onToggleSelect }: {
  note: { id: number; title: string; label: string; checklistItemCount: number; checkedItemCount: number; pinnedAt?: string | null }
  selected: boolean
  onToggleSelect: () => void
}) {
  const draggable = useDraggable({
    id: `note-${note.id}`,
    data: { type: 'note', id: note.id } satisfies BulkMoveItem,
  })
  const dragging = draggable.isDragging
  return (
    <div
      ref={draggable.setNodeRef}
      {...draggable.attributes}
      {...draggable.listeners}
      className={`flex items-start gap-3 p-4 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:border-primary-300 ${
        dragging ? 'opacity-50 cursor-grabbing' : 'cursor-grab'
      } ${selected ? 'ring-2 ring-primary-500' : ''}`}
    >
      <input
        type="checkbox"
        checked={selected}
        aria-label={`Select note ${note.title}`}
        onChange={onToggleSelect}
        onPointerDown={e => e.stopPropagation()}
        onClick={e => e.stopPropagation()}
        className="mt-1 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
      />
      <Link
        to={`/notes/${note.id}`}
        onClick={e => { if (dragging) e.preventDefault() }}
        className="block flex-1 min-w-0"
      >
        <div className="flex items-start justify-between">
          <span className="font-medium text-gray-900 dark:text-gray-100 flex items-center gap-1.5">
            {note.pinnedAt && <span aria-label="Pinned" title="Pinned" className="text-amber-500 text-sm">📌</span>}
            <span className="line-clamp-1">{note.title}</span>
          </span>
          <span className="text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 px-2 py-0.5 rounded-full ml-2 shrink-0">
            {note.label.replace('_', ' ')}
          </span>
        </div>
        {note.checklistItemCount > 0 && (
          <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">
            {note.checkedItemCount}/{note.checklistItemCount} items checked
          </p>
        )}
      </Link>
    </div>
  )
}

function DraggableFileCard({ file, selected, onToggleSelect }: {
  file: { id: number; filename: string; hasThumbnail: boolean; thumbnailUrl: string | null; downloadUrl: string | null }
  selected: boolean
  onToggleSelect: () => void
}) {
  const draggable = useDraggable({
    id: `file-${file.id}`,
    data: { type: 'file', id: file.id } satisfies BulkMoveItem,
  })
  const dragging = draggable.isDragging
  return (
    <div
      ref={draggable.setNodeRef}
      {...draggable.attributes}
      {...draggable.listeners}
      className={`relative rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:border-primary-300 ${
        dragging ? 'opacity-50 cursor-grabbing' : 'cursor-grab'
      } ${selected ? 'ring-2 ring-primary-500' : ''}`}
    >
      <input
        type="checkbox"
        checked={selected}
        aria-label={`Select file ${file.filename}`}
        onChange={onToggleSelect}
        onPointerDown={e => e.stopPropagation()}
        onClick={e => e.stopPropagation()}
        className="absolute top-2 left-2 z-10 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
      />
      <a
        href={file.downloadUrl ?? '#'}
        target="_blank"
        rel="noopener noreferrer"
        onClick={e => { if (dragging) e.preventDefault() }}
        className="block p-3"
      >
        {file.hasThumbnail && file.thumbnailUrl ? (
          <img src={file.thumbnailUrl} alt={file.filename}
            className="w-full h-24 object-cover rounded-lg mb-2" />
        ) : (
          <div className="w-full h-24 bg-gray-100 dark:bg-gray-700 rounded-lg flex items-center justify-center text-3xl mb-2">📄</div>
        )}
        <p className="text-xs text-gray-700 dark:text-gray-300 truncate">{file.filename}</p>
      </a>
    </div>
  )
}

/**
 * Drop target for the immediate-parent breadcrumb. Lets a user drag any
 * item up one level without opening the move modal.
 */
function BreadcrumbDropTarget({ folderId, children }: { folderId: number; children: React.ReactNode }) {
  const droppable = useDroppable({
    id: `drop-breadcrumb-${folderId}`,
    data: { folderId },
  })
  return (
    <span
      ref={droppable.setNodeRef}
      className={`inline-flex rounded px-1 transition-colors ${
        droppable.isOver ? 'bg-primary-100 dark:bg-primary-900/40 ring-2 ring-primary-500' : ''
      }`}
    >
      {children}
    </span>
  )
}

// Curated palette — covers most labelling use-cases without overwhelming the
// modal. Backend stores any 6-digit hex; users can also paste a custom value
// in the "#hex" field below.
const FOLDER_COLORS = [
  '#ef4444', '#f97316', '#f59e0b', '#eab308',
  '#84cc16', '#22c55e', '#10b981', '#14b8a6',
  '#06b6d4', '#3b82f6', '#6366f1', '#8b5cf6',
  '#a855f7', '#d946ef', '#ec4899', '#64748b',
]

function FolderColorPicker({ value, onChange }: { value: string; onChange: (next: string | null) => void }) {
  return (
    <div className="space-y-1.5">
      <span className="text-xs text-gray-500 dark:text-gray-400 block">Color</span>
      <div role="radiogroup" aria-label="Folder color" className="flex flex-wrap gap-1.5 items-center">
        <button
          type="button"
          role="radio"
          aria-checked={!value}
          aria-label="No color"
          title="No color"
          onClick={() => onChange('')}
          className={`w-6 h-6 rounded-full border ${
            !value ? 'border-gray-900 dark:border-gray-100 ring-2 ring-offset-2 ring-offset-gray-50 dark:ring-offset-gray-800 ring-primary-500' : 'border-dashed border-gray-400 dark:border-gray-500'
          } bg-white dark:bg-gray-700`}
        />
        {FOLDER_COLORS.map(c => (
          <button
            key={c}
            type="button"
            role="radio"
            aria-checked={value === c}
            aria-label={`Color ${c}`}
            title={c}
            onClick={() => onChange(c)}
            className={`w-6 h-6 rounded-full border ${
              value === c ? 'border-gray-900 dark:border-gray-100 ring-2 ring-offset-2 ring-offset-gray-50 dark:ring-offset-gray-800 ring-primary-500' : 'border-gray-300 dark:border-gray-600'
            }`}
            style={{ backgroundColor: c }}
          />
        ))}
        <input
          type="text"
          value={value}
          onChange={e => onChange(e.target.value)}
          placeholder="#hex"
          aria-label="Custom hex color"
          className="ml-2 w-20 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-2 py-1 text-xs"
        />
      </div>
    </div>
  )
}

// Emoji icon set — small, opinionated default. Stored verbatim on the folder
// so anything Unicode-renderable works if the user types into the override field.
const FOLDER_ICONS = ['📁', '📂', '📚', '📝', '📋', '🍳', '🛒', '🏠', '✈️', '🎁', '💡', '🎯', '🎨', '🎬', '🎵', '🎮']

function FolderIconPicker({ value, onChange }: { value: string; onChange: (next: string | null) => void }) {
  return (
    <div className="space-y-1.5">
      <span className="text-xs text-gray-500 dark:text-gray-400 block">Icon</span>
      <div role="radiogroup" aria-label="Folder icon" className="flex flex-wrap gap-1.5 items-center">
        <button
          type="button"
          role="radio"
          aria-checked={!value}
          aria-label="No icon"
          title="No icon"
          onClick={() => onChange('')}
          className={`w-7 h-7 rounded text-xs ${
            !value ? 'bg-primary-100 dark:bg-primary-900 text-primary-700 dark:text-primary-300 ring-2 ring-primary-500' : 'bg-gray-100 dark:bg-gray-700 text-gray-500'
          }`}
        >
          ✕
        </button>
        {FOLDER_ICONS.map(i => (
          <button
            key={i}
            type="button"
            role="radio"
            aria-checked={value === i}
            aria-label={`Icon ${i}`}
            title={i}
            onClick={() => onChange(i)}
            className={`w-7 h-7 rounded text-base ${
              value === i ? 'bg-primary-100 dark:bg-primary-900 ring-2 ring-primary-500' : 'bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600'
            }`}
          >
            {i}
          </button>
        ))}
        <input
          type="text"
          value={value}
          onChange={e => onChange(e.target.value)}
          placeholder="emoji"
          aria-label="Custom icon"
          maxLength={4}
          className="ml-2 w-16 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-2 py-1 text-xs text-center"
        />
      </div>
    </div>
  )
}
