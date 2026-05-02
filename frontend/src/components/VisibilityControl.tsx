import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { itemAccessApi, userRosterApi, type AclEntry, type AclRole, type Visibility } from '../api'
import { useAuthStore } from '../lib/authStore'
import { toast } from '../lib/toastStore'

interface Props {
  itemType: 'note' | 'file' | 'folder'
  itemId: number
  /**
   * Owner's userId — used to suppress redundant ACL entries (the owner always
   * has access). Pass null when unknown; the control still works, just shows
   * the owner in the picker.
   */
  ownerId: number | null
}

const LABELS: Record<Visibility, string> = {
  private: 'Only me',
  household: 'Whole household',
  custom: 'Specific people',
}

const HINTS: Record<Visibility, string> = {
  private: 'No one else in the household sees this.',
  household: 'Every adult member can read; only the owner can edit.',
  custom: 'Pick exactly who has access and what they can do.',
}

/**
 * Visibility + per-item ACL widget for notes/files/folders. Backend wiring is
 * `PUT /api/items/{type}/{id}/visibility`; this component reads the current
 * visibility once on mount, mutates locally, and pushes the change on Save
 * so the user sees a stable preview before committing.
 *
 * Children and non-owners see a read-only summary. The owner and admins see
 * the editable form.
 */
export default function VisibilityControl({ itemType, itemId, ownerId }: Props) {
  const qc = useQueryClient()
  const me = useAuthStore(s => s.user)
  const canEdit = me != null && (me.isAdmin || me.id === ownerId)

  const { data, isLoading } = useQuery({
    queryKey: ['item-visibility', itemType, itemId],
    queryFn: () => itemAccessApi.get(itemType, itemId),
  })
  const { data: roster = [] } = useQuery({
    queryKey: ['user-roster'],
    queryFn: () => userRosterApi.list(),
    staleTime: 60_000,
  })

  const rosterById = useMemo(() => {
    const m = new Map<number, string>()
    for (const r of roster) m.set(r.id, r.displayName)
    return m
  }, [roster])

  const [visibility, setVisibility] = useState<Visibility | null>(null)
  const [acls, setAcls] = useState<AclEntry[] | null>(null)

  // Initialize the local form state from the server payload on first load.
  if (data && visibility == null) setVisibility(data.visibility)
  if (data && acls == null) setAcls(data.acls)

  const save = useMutation({
    mutationFn: () => itemAccessApi.set(itemType, itemId, {
      visibility: visibility ?? 'household',
      acls: visibility === 'custom' ? (acls ?? []) : undefined,
    }),
    onSuccess: result => {
      qc.setQueryData(['item-visibility', itemType, itemId], result)
      toast.success('Visibility updated')
    },
    onError: () => toast.error('Could not update visibility'),
  })

  if (isLoading || !data || visibility == null) return null

  const dirty = visibility !== data.visibility
    || JSON.stringify((acls ?? []).map(a => `${a.userId}:${a.role}`).sort())
       !== JSON.stringify(data.acls.map(a => `${a.userId}:${a.role}`).sort())

  if (!canEdit) {
    return (
      <p className="text-xs text-gray-500 dark:text-gray-400">
        Visibility: <span className="font-medium text-gray-700 dark:text-gray-300">{LABELS[data.visibility]}</span>
        {data.visibility === 'custom' && data.acls.length > 0 && (
          <> — {data.acls.map(a => rosterById.get(a.userId) ?? `user #${a.userId}`).join(', ')}</>
        )}
      </p>
    )
  }

  // Roster minus the owner (always implicit) and anyone already on the ACL
  // list — keeps the "Add" picker tight to candidates the user can actually
  // grant new access to.
  const addCandidates = roster.filter(r =>
    r.id !== ownerId && !(acls ?? []).some(a => a.userId === r.id))

  return (
    <div className="rounded-lg border border-gray-200 dark:border-gray-700 p-3 text-xs space-y-2">
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-gray-500 dark:text-gray-400">Visibility</span>
        <select
          aria-label="Visibility"
          value={visibility}
          onChange={e => setVisibility(e.target.value as Visibility)}
          className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-2 py-1"
        >
          <option value="private">{LABELS.private}</option>
          <option value="household">{LABELS.household}</option>
          <option value="custom">{LABELS.custom}</option>
        </select>
        {dirty && (
          <button
            type="button"
            onClick={() => save.mutate()}
            disabled={save.isPending}
            className="ml-auto px-2 py-1 rounded-lg bg-primary-600 text-white font-medium disabled:opacity-50"
          >
            {save.isPending ? 'Saving…' : 'Save'}
          </button>
        )}
      </div>
      <p className="text-gray-500 dark:text-gray-400">{HINTS[visibility]}</p>
      {visibility === 'custom' && (
        <div className="space-y-1.5">
          {(acls ?? []).map(a => (
            <div key={a.userId} className="flex items-center gap-2">
              <span className="text-gray-700 dark:text-gray-300 flex-1">
                {rosterById.get(a.userId) ?? `User #${a.userId}`}
              </span>
              <select
                aria-label={`Role for ${rosterById.get(a.userId) ?? a.userId}`}
                value={a.role}
                onChange={e => setAcls((acls ?? []).map(x =>
                  x.userId === a.userId ? { ...x, role: e.target.value as AclRole } : x))}
                className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-2 py-1"
              >
                <option value="viewer">Viewer</option>
                <option value="editor">Editor</option>
              </select>
              <button
                type="button"
                onClick={() => setAcls((acls ?? []).filter(x => x.userId !== a.userId))}
                aria-label={`Remove ${rosterById.get(a.userId) ?? a.userId}`}
                className="text-gray-400 hover:text-red-500 px-1"
              >
                ×
              </button>
            </div>
          ))}
          {addCandidates.length > 0 && (
            <select
              aria-label="Add user"
              value=""
              onChange={e => {
                const id = Number(e.target.value)
                if (Number.isFinite(id) && id > 0) {
                  setAcls([...(acls ?? []), { userId: id, role: 'viewer' }])
                }
              }}
              className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-2 py-1 w-full"
            >
              <option value="">+ Add a person…</option>
              {addCandidates.map(c => (
                <option key={c.id} value={c.id}>{c.displayName}{c.isChild ? ' (child)' : ''}</option>
              ))}
            </select>
          )}
          {addCandidates.length === 0 && (acls ?? []).length === 0 && (
            <p className="text-gray-400 dark:text-gray-500">
              No other household members to add. Invite users from Settings → Admin.
            </p>
          )}
        </div>
      )}
    </div>
  )
}
