import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { tagApi } from '../../api'
import AppLayout from '../../components/AppLayout'
import ColorPicker from '../../components/ColorPicker'
import type { TagResponse } from '../../types'

function ColorDot({ color }: { color: string }) {
  return (
    <span
      className="inline-block w-4 h-4 rounded-full border border-gray-300 dark:border-gray-600 shrink-0"
      style={{ backgroundColor: color }}
    />
  )
}

export default function AdminTagsPage() {
  const qc = useQueryClient()

  const [newName, setNewName] = useState('')
  const [newColor, setNewColor] = useState('#6366f1')
  const [editId, setEditId] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [editColor, setEditColor] = useState('')
  const [error, setError] = useState<string | null>(null)

  const { data: tags = [], isLoading } = useQuery<TagResponse[]>({
    queryKey: ['tags'],
    queryFn: () => tagApi.list(),
  })

  const createTag = useMutation({
    mutationFn: () => tagApi.create({ name: newName.trim(), color: newColor }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tags'] })
      setNewName('')
      setNewColor('#6366f1')
      setError(null)
    },
    onError: () => setError('Failed to create tag.'),
  })

  const updateTag = useMutation({
    mutationFn: (id: number) => tagApi.update(id, { name: editName.trim(), color: editColor }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tags'] })
      setEditId(null)
      setError(null)
    },
    onError: () => setError('Failed to update tag.'),
  })

  const deleteTag = useMutation({
    mutationFn: (id: number) => tagApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tags'] }),
    onError: () => setError('Failed to delete tag.'),
  })

  const mergeTag = useMutation({
    mutationFn: (vars: { sourceId: number; targetId: number }) =>
      tagApi.merge(vars.sourceId, vars.targetId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tags'] })
      setError(null)
    },
    onError: () => setError('Failed to merge tag.'),
  })

  function handleMerge(source: TagResponse) {
    const candidates = tags.filter(t => t.id !== source.id)
    if (candidates.length === 0) {
      setError('No other tag to merge into.')
      return
    }
    const targetName = window.prompt(
      `Merge "${source.name}" into which tag? (case-insensitive)\n\nOptions:\n${candidates.map(t => `  - ${t.name}`).join('\n')}`,
    )
    if (!targetName) return
    const target = candidates.find(t => t.name.toLowerCase() === targetName.trim().toLowerCase())
    if (!target) {
      setError(`No tag named "${targetName.trim()}".`)
      return
    }
    if (!confirm(`Move all "${source.name}" taggings to "${target.name}" and delete "${source.name}"? This cannot be undone.`)) return
    mergeTag.mutate({ sourceId: source.id, targetId: target.id })
  }

  function startEdit(tag: TagResponse) {
    setEditId(tag.id)
    setEditName(tag.name)
    setEditColor(tag.color)
  }

  return (
    <AppLayout>
      <div className="max-w-2xl mx-auto space-y-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Tag Manager</h1>

        {/* Create */}
        <div className="p-4 bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700 rounded-xl space-y-3">
          <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Create tag</h2>
          <div className="flex gap-2 items-center">
            <input
              value={newName}
              onChange={e => setNewName(e.target.value)}
              placeholder="Tag name"
              className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <ColorPicker
              value={newColor}
              onChange={setNewColor}
              ariaLabel="New tag colour"
            />
            <button
              onClick={() => createTag.mutate()}
              disabled={!newName.trim() || createTag.isPending}
              className="px-3 py-2 text-sm bg-primary-600 text-white rounded-lg disabled:opacity-50 hover:bg-primary-700"
            >
              Create
            </button>
          </div>
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}

        {/* List */}
        {isLoading && <p className="text-sm text-gray-400 dark:text-gray-500">Loading…</p>}
        {tags.length === 0 && !isLoading && (
          <p className="text-sm text-gray-400 dark:text-gray-500">No tags yet.</p>
        )}

        <div className="space-y-2">
          {tags.map(tag => (
            <div key={tag.id} className="flex items-center gap-3 p-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800">
              {editId === tag.id ? (
                <>
                  <input
                    autoFocus
                    value={editName}
                    onChange={e => setEditName(e.target.value)}
                    className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                  <ColorPicker
                    value={editColor}
                    onChange={setEditColor}
                    ariaLabel="Tag colour"
                  />
                  <button
                    onClick={() => updateTag.mutate(tag.id)}
                    disabled={!editName.trim() || updateTag.isPending}
                    className="px-2 py-1 text-xs bg-primary-600 text-white rounded disabled:opacity-50"
                  >
                    Save
                  </button>
                  <button
                    onClick={() => setEditId(null)}
                    className="px-2 py-1 text-xs bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded"
                  >
                    Cancel
                  </button>
                </>
              ) : (
                <>
                  <ColorDot color={tag.color} />
                  <span className="flex-1 text-sm font-medium text-gray-800 dark:text-gray-200">{tag.name}</span>
                  <button
                    onClick={() => startEdit(tag)}
                    className="px-2 py-1 text-xs text-gray-600 dark:text-gray-400 border border-gray-300 dark:border-gray-600 rounded hover:bg-gray-50 dark:hover:bg-gray-700"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleMerge(tag)}
                    disabled={mergeTag.isPending}
                    className="px-2 py-1 text-xs text-gray-600 dark:text-gray-400 border border-gray-300 dark:border-gray-600 rounded hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
                  >
                    Merge…
                  </button>
                  <button
                    onClick={() => {
                      if (confirm(`Delete tag "${tag.name}"? It will be removed from all notes and files.`)) {
                        deleteTag.mutate(tag.id)
                      }
                    }}
                    className="px-2 py-1 text-xs text-red-600 border border-red-200 dark:border-red-800 rounded hover:bg-red-50 dark:hover:bg-red-900/30"
                  >
                    Delete
                  </button>
                </>
              )}
            </div>
          ))}
        </div>
      </div>
    </AppLayout>
  )
}
