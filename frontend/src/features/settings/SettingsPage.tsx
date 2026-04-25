import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '../../api/authApi'
import { useAuthStore } from '../../lib/authStore'
import { usePushSubscription } from '../../lib/usePushSubscription'
import AppLayout from '../../components/AppLayout'

const profileSchema = z.object({
  displayName: z.string().min(1).max(100).optional(),
  newPassword: z.string().min(8).regex(/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, 'Need upper, lower, digit').optional().or(z.literal('')),
})
type ProfileForm = z.infer<typeof profileSchema>

export default function SettingsPage() {
  const user = useAuthStore(s => s.user)
  const setAuth = useAuthStore(s => s.setAuth)
  const push = usePushSubscription()

  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: { displayName: user?.displayName ?? '', newPassword: '' },
  })

  const updateMe = useMutation({
    mutationFn: (data: ProfileForm) =>
      authApi.updateMe({
        displayName: data.displayName || undefined,
        newPassword: data.newPassword || undefined,
      }),
    onSuccess: res => {
      const { token, expiresAt } = useAuthStore.getState()
      if (token && expiresAt) setAuth(token, res, expiresAt)
      reset({ displayName: res.displayName, newPassword: '' })
    },
  })

  return (
    <AppLayout>
      <div className="max-w-lg mx-auto">
        <h1 className="text-xl font-bold text-gray-900 mb-6">Settings</h1>

        <section className="rounded-lg border border-gray-200 p-5 mb-6">
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Profile</h2>

          <form onSubmit={handleSubmit(d => updateMe.mutate(d))} className="space-y-4">
            <div>
              <label className="block text-xs text-gray-500 mb-1">Display name</label>
              <input
                {...register('displayName')}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
              {errors.displayName && <p className="mt-1 text-xs text-red-600">{errors.displayName.message}</p>}
            </div>

            <div>
              <label className="block text-xs text-gray-500 mb-1">New password (leave blank to keep current)</label>
              <input
                {...register('newPassword')}
                type="password"
                placeholder="••••••••"
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
              {errors.newPassword && <p className="mt-1 text-xs text-red-600">{errors.newPassword.message}</p>}
            </div>

            <div className="flex items-center gap-3">
              <button
                type="submit"
                disabled={isSubmitting}
                className="px-4 py-2 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700 disabled:opacity-50"
              >
                Save
              </button>
              {updateMe.isSuccess && (
                <span className="text-sm text-green-600">Saved.</span>
              )}
              {updateMe.isError && (
                <span className="text-sm text-red-600">Save failed.</span>
              )}
            </div>
          </form>
        </section>

        {'Notification' in window && (
          <section className="rounded-lg border border-gray-200 p-5 mb-6">
            <h2 className="text-sm font-semibold text-gray-700 mb-3">Push notifications</h2>
            <div className="flex items-center gap-3">
              <button
                onClick={push.subscribed ? push.unsubscribe : push.subscribe}
                disabled={push.loading}
                className={`px-4 py-2 text-sm font-semibold rounded-lg disabled:opacity-50 ${
                  push.subscribed
                    ? 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    : 'bg-primary-600 text-white hover:bg-primary-700'
                }`}
              >
                {push.loading ? 'Working…' : push.subscribed ? 'Disable notifications' : 'Enable notifications'}
              </button>
              {push.subscribed && (
                <span className="text-xs text-green-600">Enabled on this device</span>
              )}
            </div>
          </section>
        )}

        <section className="rounded-lg border border-gray-200 p-5">
          <h2 className="text-sm font-semibold text-gray-700 mb-2">Account info</h2>
          <dl className="text-sm space-y-1">
            <div className="flex gap-2">
              <dt className="text-gray-500 w-28 shrink-0">Email</dt>
              <dd className="text-gray-900">{user?.email}</dd>
            </div>
            <div className="flex gap-2">
              <dt className="text-gray-500 w-28 shrink-0">Role</dt>
              <dd className="text-gray-900">
                {user?.isAdmin ? 'Admin' : user?.isChild ? 'Child account' : 'Member'}
              </dd>
            </div>
          </dl>
        </section>
      </div>
    </AppLayout>
  )
}
