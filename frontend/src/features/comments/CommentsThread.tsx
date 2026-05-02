import { useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { commentApi, groupApi, userRosterApi, type CommentResponse, type GroupResponse, type RosterUser } from '../../api'
import { useAuthStore } from '../../lib/authStore'

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
  // Roster + groups load on first keystroke or picker open so the @-autocomplete
  // can show matches without an extra round-trip per character.
  const [rosterPrefetch, setRosterPrefetch] = useState(false)
  const rosterEnabled = pickerOpen || rosterPrefetch
  const { data: users = [] } = useQuery<RosterUser[]>({
    queryKey: ['user-roster'],
    queryFn: () => userRosterApi.list(),
    enabled: rosterEnabled,
    staleTime: 60_000,
  })
  const { data: groups = [] } = useQuery<GroupResponse[]>({
    queryKey: ['groups'],
    queryFn: () => groupApi.list(),
    enabled: rosterEnabled,
    staleTime: 60_000,
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

  // -- @-mention autocomplete -------------------------------------------------
  const textareaRef = useRef<HTMLTextAreaElement | null>(null)
  // The active query is the substring after the most recent `@` up to the
  // cursor, with no whitespace inside. Null means the cursor isn't inside a
  // mention token right now → no popover.
  const [mentionQuery, setMentionQuery] = useState<{ start: number; text: string } | null>(null)
  const [mentionHighlight, setMentionHighlight] = useState(0)

  type MentionCandidate =
    | { kind: 'user'; id: number; label: string }
    | { kind: 'group'; id: number; label: string }

  const candidates: MentionCandidate[] = useMemo(() => {
    if (!mentionQuery) return []
    const q = mentionQuery.text.toLowerCase()
    const matchedGroups = groups
      .filter(g => g.name.toLowerCase().includes(q))
      .map(g => ({ kind: 'group' as const, id: g.id, label: g.name }))
    const matchedUsers = users
      .filter(u => u.displayName.toLowerCase().includes(q))
      .map(u => ({ kind: 'user' as const, id: u.id, label: u.displayName }))
    return [...matchedGroups, ...matchedUsers].slice(0, 8)
  }, [groups, users, mentionQuery])

  function detectMention(value: string, caret: number) {
    setRosterPrefetch(true)
    // Walk left from the caret looking for an `@`, stopping at whitespace.
    let i = caret - 1
    while (i >= 0) {
      const ch = value[i]
      if (ch === '@') {
        const before = i === 0 ? '' : value[i - 1]
        // Mention only triggers at start of input or after whitespace, so
        // emails like "ada@example.com" don't fire the picker.
        if (before === '' || /\s/.test(before)) {
          setMentionQuery({ start: i, text: value.slice(i + 1, caret) })
          setMentionHighlight(0)
          return
        }
        break
      }
      if (/\s/.test(ch)) break
      i -= 1
    }
    setMentionQuery(null)
  }

  function applyMention(c: MentionCandidate) {
    if (!mentionQuery) return
    const before = body.slice(0, mentionQuery.start)
    const after = body.slice(mentionQuery.start + 1 + mentionQuery.text.length)
    const tag = `@${c.label.replace(/\s+/g, '_')} `
    const next = before + tag + after
    setBody(next)
    if (c.kind === 'user') toggleSet(mentionedUserIds, setMentionedUserIds, c.id)
    else toggleSet(mentionedGroupIds, setMentionedGroupIds, c.id)
    setMentionQuery(null)
    requestAnimationFrame(() => {
      const ta = textareaRef.current
      if (!ta) return
      const pos = before.length + tag.length
      ta.focus()
      ta.setSelectionRange(pos, pos)
    })
  }

  function handleBodyChange(e: React.ChangeEvent<HTMLTextAreaElement>) {
    setBody(e.target.value)
    detectMention(e.target.value, e.target.selectionStart ?? e.target.value.length)
  }

  function handleBodyKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (!mentionQuery || candidates.length === 0) return
    if (e.key === 'ArrowDown') { e.preventDefault(); setMentionHighlight(h => (h + 1) % candidates.length) }
    else if (e.key === 'ArrowUp') { e.preventDefault(); setMentionHighlight(h => (h - 1 + candidates.length) % candidates.length) }
    else if (e.key === 'Enter' || e.key === 'Tab') { e.preventDefault(); applyMention(candidates[mentionHighlight]) }
    else if (e.key === 'Escape') { e.preventDefault(); setMentionQuery(null) }
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
        <div className="relative">
          <textarea
            ref={textareaRef}
            value={body}
            onChange={handleBodyChange}
            onKeyDown={handleBodyKeyDown}
            onBlur={() => { /* defer so click on popover registers first */ setTimeout(() => setMentionQuery(null), 150) }}
            placeholder={editingId ? 'Edit comment…' : 'Add a comment… type @ to mention'}
            rows={3}
            className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
          />
          {mentionQuery && candidates.length > 0 && (
            <ul
              role="listbox"
              aria-label="Mention candidates"
              className="absolute z-10 left-2 top-full mt-1 w-64 max-h-56 overflow-auto rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 shadow-lg text-sm"
            >
              {candidates.map((c, idx) => (
                <li
                  key={`${c.kind}-${c.id}`}
                  role="option"
                  aria-selected={idx === mentionHighlight}
                  // mousedown rather than click — fires before the textarea's
                  // onBlur cancels the popover via setTimeout above.
                  onMouseDown={e => { e.preventDefault(); applyMention(c) }}
                  className={`px-3 py-1.5 cursor-pointer ${
                    idx === mentionHighlight
                      ? 'bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300'
                      : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700'
                  }`}
                >
                  <span className="text-gray-400 mr-1">{c.kind === 'group' ? 'Group' : 'User'}</span>
                  @{c.label}
                </li>
              ))}
            </ul>
          )}
        </div>
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
