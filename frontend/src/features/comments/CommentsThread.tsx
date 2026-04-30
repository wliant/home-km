import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi, commentApi, groupApi, type CommentResponse, type GroupResponse } from '../../api'
import { useAuthStore } from '../../lib/authStore'
import type { UserResponse } from '../../types/auth'

interface Props {
  itemType: 'note' | 'file'
  itemId: number
}

/**
 * Comment thread with explicit ping-list. Mentions are picked from a
 * checkbox list of users and groups rather than parsed out of the body —
 * keeps the contract terse and the resolution unambiguous.
 */
export default function CommentsThread({ itemType, itemId }: Props) {
  const qc = useQueryClient()
  const me = useAuthStore(s => s.user)
  const [body, setBody] = useState('')
  const [pickerOpen, setPickerOpen] = useState(false)
  const [mentionedUserIds, setMentionedUserIds] = useState<Set<number>>(new Set())
  const [mentionedGroupIds, setMentionedGroupIds] = useState<Set<number>>(new Set())
  const [editingId, setEditingId] = useState<number | null>(null)

  const { data: comments = [] } = useQuery<CommentResponse[]>({
    queryKey: ['comments', itemType, itemId],
    queryFn: () => commentApi.list(itemType, itemId),
  })
  const { data: users = [] } = useQuery<UserResponse[]>({
    queryKey: ['admin', 'users'],
    queryFn: () => adminApi.listUsers() as unknown as Promise<UserResponse[]>,
    enabled: pickerOpen,
  })
  const { data: groups = [] } = useQuery<GroupResponse[]>({
    queryKey: ['groups'],
    queryFn: () => groupApi.list(),
    enabled: pickerOpen,
  })

  function reset() {
    setBody('')
    setMentionedUserIds(new Set())
    setMentionedGroupIds(new Set())
    setEditingId(null)
    setPickerOpen(false)
  }

  const create = useMutation({
    mutationFn: () => commentApi.create(itemType, itemId, {
      body: body.trim(),
      mentionedUserIds: [...mentionedUserIds],
      mentionedGroupIds: [...mentionedGroupIds],
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['comments', itemType, itemId] }); reset() },
  })
  const edit = useMutation({
    mutationFn: () => commentApi.update(editingId!, { body: body.trim() }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['comments', itemType, itemId] }); reset() },
  })
  const remove = useMutation({
    mutationFn: (id: number) => commentApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['comments', itemType, itemId] }),
  })

  function startEdit(c: CommentResponse) {
    setEditingId(c.id)
    setBody(c.body)
    setPickerOpen(false)
  }

  function toggleSet(set: Set<number>, setSet: (s: Set<number>) => void, id: number) {
    const next = new Set(set)
    if (next.has(id)) next.delete(id); else next.add(id)
    setSet(next)
  }

  return (
    <section className="mt-6 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
      <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
        Discussion ({comments.length})
      </h2>

      <ul className="space-y-3 mb-4">
        {comments.map(c => (
          <li key={c.id} className="rounded-md border border-gray-200 dark:border-gray-700 p-3 bg-gray-50 dark:bg-gray-900/40">
            <div className="flex items-baseline justify-between mb-1">
              <span className="text-sm font-medium text-gray-900 dark:text-gray-100">{c.authorDisplayName}</span>
              <span className="text-xs text-gray-500 dark:text-gray-400">
                {new Date(c.createdAt).toLocaleString()}
                {c.editedAt && <span className="ml-1 italic">(edited)</span>}
              </span>
            </div>
            <p className="text-sm text-gray-800 dark:text-gray-200 whitespace-pre-wrap">{c.body}</p>
            {c.authorId === me?.id && (
              <div className="mt-2 flex gap-3 text-xs">
                <button onClick={() => startEdit(c)} className="text-primary-600 dark:text-primary-400 hover:underline">Edit</button>
                <button
                  onClick={() => confirm('Delete this comment?') && remove.mutate(c.id)}
                  className="text-red-600 dark:text-red-400 hover:underline"
                >
                  Delete
                </button>
              </div>
            )}
          </li>
        ))}
        {comments.length === 0 && (
          <li className="text-sm text-gray-500 dark:text-gray-400">No comments yet.</li>
        )}
      </ul>

      <div className="space-y-2">
        <textarea
          value={body}
          onChange={e => setBody(e.target.value)}
          placeholder={editingId ? 'Edit comment…' : 'Add a comment…'}
          rows={3}
          className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
        />
        {!editingId && (
          <div>
            <button
              onClick={() => setPickerOpen(o => !o)}
              type="button"
              className="text-xs text-primary-600 dark:text-primary-400 hover:underline"
            >
              {pickerOpen ? 'Hide mention list' : `Notify ${mentionedUserIds.size + mentionedGroupIds.size || 'someone'}`}
            </button>
            {pickerOpen && (
              <div className="mt-2 grid grid-cols-2 gap-1 text-xs">
                {groups.map(g => (
                  <label key={`g-${g.id}`} className="flex items-center gap-2 text-gray-700 dark:text-gray-300">
                    <input
                      type="checkbox"
                      checked={mentionedGroupIds.has(g.id)}
                      onChange={() => toggleSet(mentionedGroupIds, setMentionedGroupIds, g.id)}
                    />
                    <span>@{g.name}{g.isSystem && <span className="text-gray-400"> (built-in)</span>}</span>
                  </label>
                ))}
                {users.map(u => (
                  <label key={`u-${u.id}`} className="flex items-center gap-2 text-gray-700 dark:text-gray-300">
                    <input
                      type="checkbox"
                      checked={mentionedUserIds.has(u.id)}
                      onChange={() => toggleSet(mentionedUserIds, setMentionedUserIds, u.id)}
                    />
                    <span>{u.displayName}</span>
                  </label>
                ))}
              </div>
            )}
          </div>
        )}
        <div className="flex gap-2">
          <button
            onClick={() => editingId ? edit.mutate() : create.mutate()}
            disabled={!body.trim() || create.isPending || edit.isPending}
            className="px-3 py-1.5 text-xs font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-lg disabled:opacity-50"
          >
            {editingId ? 'Save' : 'Post comment'}
          </button>
          {editingId && (
            <button onClick={reset} className="px-3 py-1.5 text-xs text-gray-600 dark:text-gray-400 hover:underline">
              Cancel
            </button>
          )}
        </div>
      </div>
    </section>
  )
}
