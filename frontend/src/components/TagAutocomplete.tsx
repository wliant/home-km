import { useState, useRef, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { tagApi } from '../api'
import { QK } from '../lib/queryKeys'
import { toast } from '../lib/toastStore'
import TagChip from './TagChip'
import type { TagResponse } from '../types'

interface TagAutocompleteProps {
  entityType: 'note' | 'file'
  entityId: number
  currentTags: TagResponse[]
  onTagsChange: () => void
  readOnly?: boolean
}

export default function TagAutocomplete({
  entityType, entityId, currentTags, onTagsChange, readOnly = false,
}: TagAutocompleteProps) {
  const [input, setInput] = useState('')
  const [open, setOpen] = useState(false)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const qc = useQueryClient()

  const { data: suggestions = [] } = useQuery({
    queryKey: QK.tags(input),
    queryFn: () => tagApi.list(input || undefined),
    enabled: input.length >= 1 || open,
  })

  const currentTagIds = new Set(currentTags.map(t => t.id))
  const filtered = suggestions.filter(s => !currentTagIds.has(s.id))

  const attachMutation = useMutation({
    mutationFn: (tagId: number) =>
      entityType === 'note'
        ? tagApi.attachToNote(entityId, [tagId])
        : tagApi.attachToFile(entityId, [tagId]),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: entityType === 'note' ? QK.noteTags(entityId) : QK.fileTags(entityId) })
      onTagsChange()
      setInput('')
      setOpen(false)
    },
    onError: () => toast.error('Failed to attach tag'),
  })

  const detachMutation = useMutation({
    mutationFn: (tagId: number) =>
      entityType === 'note'
        ? tagApi.detachFromNote(entityId, tagId)
        : tagApi.detachFromFile(entityId, tagId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: entityType === 'note' ? QK.noteTags(entityId) : QK.fileTags(entityId) })
      onTagsChange()
    },
    onError: () => toast.error('Failed to remove tag'),
  })

  const createAndAttach = useMutation({
    mutationFn: async (name: string) => {
      const tag = await tagApi.create({ name })
      return entityType === 'note'
        ? tagApi.attachToNote(entityId, [tag.id])
        : tagApi.attachToFile(entityId, [tag.id])
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: entityType === 'note' ? QK.noteTags(entityId) : QK.fileTags(entityId) })
      qc.invalidateQueries({ queryKey: QK.tags() })
      onTagsChange()
      setInput('')
      setOpen(false)
    },
    onError: () => toast.error('Failed to create tag'),
  })

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap gap-1.5">
        {currentTags.map(tag => (
          <TagChip
            key={tag.id}
            name={tag.name}
            color={tag.color}
            onRemove={readOnly ? undefined : () => detachMutation.mutate(tag.id)}
          />
        ))}
      </div>

      {!readOnly && (
        <div ref={wrapperRef} className="relative">
          <input
            value={input}
            onChange={e => { setInput(e.target.value); setOpen(true) }}
            onFocus={() => setOpen(true)}
            placeholder="Add tag…"
            className="w-48 rounded-lg border border-gray-300 px-3 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          {open && (
            <ul className="absolute z-20 mt-1 w-56 rounded-lg border border-gray-200 bg-white shadow-lg max-h-48 overflow-y-auto">
              {filtered.map(tag => (
                <li key={tag.id}>
                  <button
                    type="button"
                    onMouseDown={() => attachMutation.mutate(tag.id)}
                    className="w-full text-left px-3 py-2 text-xs hover:bg-gray-50 flex items-center gap-2"
                  >
                    <span
                      className="w-2 h-2 rounded-full shrink-0"
                      style={{ backgroundColor: tag.color }}
                    />
                    {tag.name}
                  </button>
                </li>
              ))}
              {input.trim() && !suggestions.some(s => s.name.toLowerCase() === input.trim().toLowerCase()) && (
                <li>
                  <button
                    type="button"
                    onMouseDown={() => createAndAttach.mutate(input.trim())}
                    className="w-full text-left px-3 py-2 text-xs text-primary-600 hover:bg-primary-50"
                  >
                    + Create "{input.trim()}"
                  </button>
                </li>
              )}
              {filtered.length === 0 && !input.trim() && (
                <li className="px-3 py-2 text-xs text-gray-400">No tags yet</li>
              )}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
