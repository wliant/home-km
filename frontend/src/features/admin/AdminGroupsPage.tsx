import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi, groupApi, type GroupResponse } from '../../api'
import AppLayout from '../../components/AppLayout'
import type { UserResponse } from '../../types/auth'
import { QK } from '../../lib/queryKeys'

export default function AdminGroupsPage() {
  const qc = useQueryClient()
  const [error, setError] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [selectedMembers, setSelectedMembers] = useState<Set<number>>(new Set())
  const [editingId, setEditingId] = useState<number | null>(null)

  const { data: groups = [] } = useQuery<GroupResponse[]>({
    queryKey: ['groups'],
    queryFn: () => groupApi.list(),
  })
  const { data: users = [] } = useQuery<UserResponse[]>({
    queryKey: QK.adminUsers(),
    queryFn: () => adminApi.listUsers() as unknown as Promise<UserResponse[]>,
  })

  function reset() {
    setName('')
    setSelectedMembers(new Set())
    setEditingId(null)
    setError(null)
  }

  const create = useMutation({
    mutationFn: () => groupApi.create({ name: name.trim(), memberUserIds: [...selectedMembers] }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['groups'] }); reset() },
    onError: () => setError('Could not create group (name may be taken).'),
  })
  const update = useMutation({
    mutationFn: () => groupApi.update(editingId!, { name: name.trim(), memberUserIds: [...selectedMembers] }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['groups'] }); reset() },
    onError: () => setError('Could not save group.'),
  })
  const remove = useMutation({
    mutationFn: (id: number) => groupApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['groups'] }),
  })

  function startEdit(g: GroupResponse) {
    setEditingId(g.id)
    setName(g.name)
    setSelectedMembers(new Set(g.memberUserIds))
    setError(null)
  }

  function toggleMember(id: number) {
    setSelectedMembers(prev => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id); else next.add(id)
      return next
    })
  }

  return (
    <AppLayout>
      <div className="max-w-3xl mx-auto p-6 space-y-6">
        <header>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Household groups</h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
            Address sets of users at once — for reminders today, more places later.
            Built-in groups are computed from each member's child flag.
          </p>
        </header>

        <section className="rounded-lg border border-gray-200 dark:border-gray-700 p-4">
          <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
            {editingId ? 'Edit group' : 'New group'}
          </h2>
          {error && <p className="text-xs text-red-600 dark:text-red-400 mb-2">{error}</p>}
          <div className="space-y-3">
            <input
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="Group name"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm"
            />
            <div className="grid grid-cols-2 gap-2">
              {users.map(u => (
                <label key={u.id} className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                  <input
                    type="checkbox"
                    checked={selectedMembers.has(u.id)}
                    onChange={() => toggleMember(u.id)}
                  />
                  <span>{u.displayName} <span className="text-gray-400">({u.email})</span></span>
                </label>
              ))}
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => editingId ? update.mutate() : create.mutate()}
                disabled={!name.trim() || create.isPending || update.isPending}
                className="px-3 py-1.5 text-xs font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-lg disabled:opacity-50"
              >
                {editingId ? 'Save changes' : 'Create group'}
              </button>
              {editingId && (
                <button onClick={reset} className="px-3 py-1.5 text-xs text-gray-600 dark:text-gray-400 hover:underline">
                  Cancel
                </button>
              )}
            </div>
          </div>
        </section>

        <section className="rounded-lg border border-gray-200 dark:border-gray-700">
          <ul className="divide-y divide-gray-200 dark:divide-gray-700">
            {groups.map(g => (
              <li key={g.id} className="p-3 flex items-center justify-between">
                <div>
                  <div className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {g.name}
                    {g.isSystem && (
                      <span className="ml-2 text-xs px-2 py-0.5 rounded bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400">
                        Built-in
                      </span>
                    )}
                  </div>
                  <div className="text-xs text-gray-500 dark:text-gray-400">
                    {g.memberUserIds.length} member{g.memberUserIds.length === 1 ? '' : 's'}
                  </div>
                </div>
                {!g.isSystem && (
                  <div className="flex gap-2">
                    <button
                      onClick={() => startEdit(g)}
                      className="text-xs text-primary-600 dark:text-primary-400 hover:underline"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => confirm(`Delete group "${g.name}"?`) && remove.mutate(g.id)}
                      className="text-xs text-red-600 dark:text-red-400 hover:underline"
                    >
                      Delete
                    </button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        </section>
      </div>
    </AppLayout>
  )
}
