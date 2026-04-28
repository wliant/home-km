import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { authApi } from '../../api/authApi'
import AppLayout from '../../components/AppLayout'
import type { CreateInvitationRequest, IssuedInvitationResponse } from '../../types/auth'

export default function AdminInvitationsPage() {
  const qc = useQueryClient()
  const [issued, setIssued] = useState<IssuedInvitationResponse | null>(null)
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<'USER' | 'ADMIN'>('USER')

  const list = useQuery({
    queryKey: ['admin', 'invitations'],
    queryFn: authApi.listInvitations,
  })

  const create = useMutation({
    mutationFn: (data: CreateInvitationRequest) => authApi.createInvitation(data),
    onSuccess: (res) => {
      setIssued(res)
      setEmail('')
      qc.invalidateQueries({ queryKey: ['admin', 'invitations'] })
    },
  })

  const revoke = useMutation({
    mutationFn: (id: number) => authApi.revokeInvitation(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'invitations'] }),
  })

  const inviteUrl = issued
    ? `${window.location.origin}/register?invite=${encodeURIComponent(issued.token)}`
    : null

  return (
    <AppLayout>
      <div className="max-w-2xl mx-auto">
        <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100 mb-6">Invitations</h1>

        <section className="rounded-lg border border-gray-200 dark:border-gray-700 p-5 mb-6">
          <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Invite a household member</h2>
          <form
            onSubmit={(e) => {
              e.preventDefault()
              if (!email) return
              create.mutate({ email, role })
            }}
            className="space-y-3"
          >
            <div className="flex gap-2">
              <input
                type="email"
                placeholder="email@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
              />
              <select
                value={role}
                onChange={(e) => setRole(e.target.value as 'USER' | 'ADMIN')}
                className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm"
              >
                <option value="USER">Member</option>
                <option value="ADMIN">Admin</option>
              </select>
              <button
                type="submit"
                disabled={create.isPending}
                className="px-4 py-2 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700 disabled:opacity-50"
              >
                Invite
              </button>
            </div>
            {create.isError && <p className="text-sm text-red-600">Failed to create invitation.</p>}
          </form>

          {issued && inviteUrl && (
            <div className="mt-4 rounded-lg bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-700 p-3 text-sm">
              <p className="font-medium text-green-800 dark:text-green-300">Invitation created.</p>
              <p className="mt-1 text-green-700 dark:text-green-400">
                Share this link (valid until{' '}
                {new Date(issued.invitation.expiresAt).toLocaleString()}):
              </p>
              <div className="mt-2 flex gap-2">
                <input
                  readOnly
                  value={inviteUrl}
                  className="flex-1 rounded border border-green-300 bg-white dark:bg-gray-800 px-2 py-1 text-xs"
                  onFocus={(e) => e.currentTarget.select()}
                />
                <button
                  type="button"
                  onClick={() => navigator.clipboard?.writeText(inviteUrl)}
                  className="px-3 py-1 rounded bg-green-600 text-white text-xs"
                >
                  Copy
                </button>
              </div>
            </div>
          )}
        </section>

        <section className="rounded-lg border border-gray-200 dark:border-gray-700 p-5">
          <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Pending invitations</h2>
          {list.isLoading && <p className="text-sm text-gray-500">Loading…</p>}
          {list.data && list.data.length === 0 && (
            <p className="text-sm text-gray-500 dark:text-gray-400">None.</p>
          )}
          {list.data && list.data.length > 0 && (
            <ul className="divide-y divide-gray-100 dark:divide-gray-700">
              {list.data.map((inv) => (
                <li key={inv.id} className="py-2 flex items-center justify-between">
                  <div className="text-sm">
                    <p className="text-gray-900 dark:text-gray-100">
                      {inv.email}{' '}
                      <span className="text-xs text-gray-500">({inv.role})</span>
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      Created {new Date(inv.createdAt).toLocaleString()} ·{' '}
                      {inv.accepted
                        ? `accepted ${inv.acceptedAt ? new Date(inv.acceptedAt).toLocaleString() : ''}`
                        : inv.expired
                        ? 'expired'
                        : `expires ${new Date(inv.expiresAt).toLocaleString()}`}
                    </p>
                  </div>
                  {!inv.accepted && (
                    <button
                      onClick={() => revoke.mutate(inv.id)}
                      className="text-xs text-red-600 hover:underline"
                    >
                      Revoke
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </AppLayout>
  )
}
