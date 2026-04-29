import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { folderApi, noteApi, fileApi } from '../../api'
import { QK } from '../../lib/queryKeys'
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

  const [showMove, setShowMove] = useState(false)
  const [moveToId, setMoveToId] = useState<string>('')

  const { data: folder } = useQuery({
    queryKey: QK.folder(folderId),
    queryFn: () => folderApi.getById(folderId),
  })
  const { data: allFolders } = useQuery({
    queryKey: QK.folders(),
    queryFn: () => folderApi.getTree(),
    enabled: showMove,
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
    mutationFn: () => folderApi.update(folderId, { name: renameValue, description: renameDesc || undefined }),
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

  function openRename() {
    setRenameValue(folder?.name ?? '')
    setRenameDesc(folder?.description ?? '')
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
      <div className="max-w-3xl mx-auto space-y-6">
        {breadcrumbs.length > 0 && (
          <nav aria-label="Folder breadcrumb" className="text-sm text-gray-500 dark:text-gray-400">
            <ol className="flex flex-wrap items-center gap-1">
              {breadcrumbs.map(crumb => (
                <li key={crumb.id} className="flex items-center gap-1">
                  <Link
                    to={`/folders/${crumb.id}`}
                    className="hover:text-primary-600 dark:hover:text-primary-400 hover:underline"
                  >
                    {crumb.name}
                  </Link>
                  <span aria-hidden="true">/</span>
                </li>
              ))}
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
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Rename folder</h3>
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
                <Link key={child.id} to={`/folders/${child.id}`}
                  className="flex items-center gap-2 p-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:border-primary-300">
                  <span>📁</span>
                  <span className="text-sm truncate">{child.name}</span>
                </Link>
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
              {notesPage?.content.map(note => (
                <Link key={note.id} to={`/notes/${note.id}`}
                  className="block p-4 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:border-primary-300">
                  <div className="flex items-start justify-between">
                    <span className="font-medium text-gray-900 dark:text-gray-100">{note.title}</span>
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
                <a key={file.id} href={file.downloadUrl ?? '#'}
                  target="_blank" rel="noopener noreferrer"
                  className="block p-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:border-primary-300">
                  {file.hasThumbnail && file.thumbnailUrl ? (
                    <img src={file.thumbnailUrl} alt={file.filename}
                      className="w-full h-24 object-cover rounded-lg mb-2" />
                  ) : (
                    <div className="w-full h-24 bg-gray-100 dark:bg-gray-700 rounded-lg flex items-center justify-center text-3xl mb-2">📄</div>
                  )}
                  <p className="text-xs text-gray-700 dark:text-gray-300 truncate">{file.filename}</p>
                </a>
              ))}
            </div>
          )}
        </section>

        <div className="pt-4 border-t border-gray-200 dark:border-gray-700">
          <button
            onClick={() => { if (confirm('Delete this folder?')) deleteFolder.mutate(false) }}
            className="text-sm text-red-500 hover:text-red-700"
          >
            Delete folder
          </button>
        </div>
      </div>
    </AppLayout>
  )
}
