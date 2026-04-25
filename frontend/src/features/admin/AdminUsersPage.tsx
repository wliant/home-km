import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { adminApi } from '../../api'
import { QK } from '../../lib/queryKeys'
import AppLayout from '../../components/AppLayout'
import type { UserResponse } from '../../types/auth'

const createSchema = z.object({
  email: z.string().email(),
  displayName: z.string().min(1).max(100),
  password: z.string().min(8).regex(/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, 'Need upper, lower, digit'),
  isAdmin: z.boolean().optional(),
  isChild: z.boolean().optional(),
})
type CreateForm = z.infer<typeof createSchema>

export default function AdminUsersPage() {
  const qc = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)
  const [apiError, setApiError] = useState<string | null>(null)

  const { data: users = [], isLoading } = useQuery<UserResponse[]>({
    queryKey: QK.adminUsers(),
    queryFn: adminApi.listUsers,
  })

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<CreateForm>({
    resolver: zodResolver(createSchema),
  })

  const createUser = useMutation({
    mutationFn: (data: CreateForm) => adminApi.createUser(data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QK.adminUsers() }); reset(); setShowCreate(false) },
    onError: (e: any) => setApiError(e.response?.data?.message ?? 'Error'),
  })

  const deleteUser = useMutation({
    mutationFn: (id: number) => adminApi.deleteUser(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: QK.adminUsers() }),
  })

  const resetPassword = useMutation({
    mutationFn: ({ id, password }: { id: number; password: string }) =>
      adminApi.resetPassword(id, password),
  })

  return (
    <AppLayout>
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-bold text-gray-900">Users</h1>
          <button
            onClick={() => setShowCreate(s => !s)}
            className="px-4 py-2 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700"
          >
            {showCreate ? 'Cancel' : '+ New user'}
          </button>
        </div>

        {showCreate && (
          <form
            onSubmit={handleSubmit(d => { setApiError(null); createUser.mutate(d) })}
            className="mb-6 rounded-lg border border-gray-200 p-4 space-y-3"
          >
            <h2 className="text-sm font-semibold text-gray-700">Create user</h2>
            {apiError && <p className="text-xs text-red-600">{apiError}</p>}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <input {...register('email')} placeholder="Email" className="w-full rounded border border-gray-300 px-3 py-2 text-sm" />
                {errors.email && <p className="text-xs text-red-600 mt-1">{errors.email.message}</p>}
              </div>
              <div>
                <input {...register('displayName')} placeholder="Display name" className="w-full rounded border border-gray-300 px-3 py-2 text-sm" />
                {errors.displayName && <p className="text-xs text-red-600 mt-1">{errors.displayName.message}</p>}
              </div>
              <div>
                <input {...register('password')} type="password" placeholder="Password" className="w-full rounded border border-gray-300 px-3 py-2 text-sm" />
                {errors.password && <p className="text-xs text-red-600 mt-1">{errors.password.message}</p>}
              </div>
            </div>
            <div className="flex gap-4 text-sm">
              <label className="flex items-center gap-1">
                <input type="checkbox" {...register('isAdmin')} className="w-4 h-4" /> Admin
              </label>
              <label className="flex items-center gap-1">
                <input type="checkbox" {...register('isChild')} className="w-4 h-4" /> Child account
              </label>
            </div>
            <button type="submit" disabled={isSubmitting}
              className="px-4 py-2 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700 disabled:opacity-50">
              Create
            </button>
          </form>
        )}

        {isLoading && <p className="text-gray-500 text-sm">Loading…</p>}

        <div className="rounded-lg border border-gray-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600 text-left">
              <tr>
                <th className="px-4 py-3 font-medium">Email</th>
                <th className="px-4 py-3 font-medium">Name</th>
                <th className="px-4 py-3 font-medium">Role</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {users.map(u => (
                <tr key={u.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-900">{u.email}</td>
                  <td className="px-4 py-3 text-gray-700">{u.displayName}</td>
                  <td className="px-4 py-3">
                    {u.isAdmin && <span className="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700 mr-1">Admin</span>}
                    {u.isChild && <span className="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-700">Child</span>}
                  </td>
                  <td className="px-4 py-3">
                    <span className={`text-xs px-2 py-0.5 rounded-full ${u.isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                      {u.isActive ? 'Active' : 'Disabled'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => {
                        const pw = prompt('New password (min 8 chars, upper+lower+digit):')
                        if (pw) resetPassword.mutate({ id: u.id, password: pw })
                      }}
                      className="text-xs text-primary-600 hover:underline mr-3"
                    >
                      Reset pw
                    </button>
                    <button
                      onClick={() => { if (confirm(`Delete ${u.email}?`)) deleteUser.mutate(u.id) }}
                      className="text-xs text-red-600 hover:underline"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </AppLayout>
  )
}
