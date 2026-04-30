import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { authApi } from '../../api/authApi'
import { buildInfoApi, meApi } from '../../api'
import { useAuthStore } from '../../lib/authStore'
import { usePushSubscription } from '../../lib/usePushSubscription'
import { useThemeStore, type Theme } from '../../lib/themeStore'
import { ACCENT_PRESETS } from '../../lib/accentPresets'
import AppLayout from '../../components/AppLayout'

const profileSchema = z.object({
  displayName: z.string().min(1).max(100).optional(),
  newPassword: z.string().min(8).regex(/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, 'Need upper, lower, digit').optional().or(z.literal('')),
})
type ProfileForm = z.infer<typeof profileSchema>

export default function SettingsPage() {
  const user = useAuthStore(s => s.user)
  const setAuth = useAuthStore(s => s.setAuth)
  const refreshToken = useAuthStore(s => s.refreshToken)
  const push = usePushSubscription()
  const theme = useThemeStore(s => s.theme)
  const setTheme = useThemeStore(s => s.setTheme)
  const accent = useThemeStore(s => s.accent)
  const setAccent = useThemeStore(s => s.setAccent)
  const qc = useQueryClient()

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
      const { token, refreshToken, expiresAt } = useAuthStore.getState()
      if (token && refreshToken && expiresAt) setAuth(token, refreshToken, res, expiresAt)
      reset({ displayName: res.displayName, newPassword: '' })
    },
  })

  const sessions = useQuery({
    queryKey: ['auth', 'sessions'],
    queryFn: () => authApi.listSessions(refreshToken),
  })

  const revokeSession = useMutation({
    mutationFn: (id: number) => authApi.revokeSession(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['auth', 'sessions'] }),
  })

  return (
    <AppLayout>
      <div className="max-w-lg mx-auto">
        <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100 mb-6">Settings</h1>

        <section className="rounded-lg border border-gray-200 dark:border-gray-700 p-5 mb-6">
          <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Profile</h2>

          <form onSubmit={handleSubmit(d => updateMe.mutate(d))} className="space-y-4">
            <div>
              <label htmlFor="settings-display-name" className="block text-xs text-gray-500 dark:text-gray-400 mb-1">Display name</label>
              <input
                id="settings-display-name"
                {...register('displayName')}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm"
              />
              {errors.displayName && <p className="mt-1 text-xs text-red-600">{errors.displayName.message}</p>}
            </div>

            <div>
              <label htmlFor="settings-new-password" className="block text-xs text-gray-500 dark:text-gray-400 mb-1">New password (leave blank to keep current)</label>
              <input
                id="settings-new-password"
                {...register('newPassword')}
                type="password"
                placeholder="••••••••"
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm"
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

        <section className="rounded-lg border border-gray-200 dark:border-gray-700 p-5 mb-6">
          <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Active sessions</h2>
          {sessions.isLoading && <p className="text-xs text-gray-500">Loading…</p>}
          {sessions.data && sessions.data.length === 0 && (
            <p className="text-xs text-gray-500">No active sessions.</p>
          )}
          {sessions.data && sessions.data.length > 0 && (
            <ul className="divide-y divide-gray-100 dark:divide-gray-700">
              {sessions.data.map(s => (
                <li key={s.id} className="py-2 flex items-center justify-between">
                  <div className="text-sm">
                    <p className="text-gray-900 dark:text-gray-100">
                      {s.deviceLabel ?? 'Unknown device'}
                      {s.current && <span className="ml-2 text-xs text-primary-600 dark:text-primary-400">(this device)</span>}
                      {s.rememberMe && <span className="ml-2 text-xs text-gray-500">extended</span>}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      {s.userAgent ? s.userAgent.slice(0, 60) : 'No user-agent'} ·{' '}
                      {s.lastSeenAt
                        ? `last seen ${new Date(s.lastSeenAt).toLocaleString()}`
                        : `created ${new Date(s.createdAt).toLocaleString()}`}
                    </p>
                  </div>
                  {!s.current && (
                    <button
                      onClick={() => revokeSession.mutate(s.id)}
                      className="text-xs text-red-600 hover:underline"
                    >
                      Sign out
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>

        {'Notification' in window && (
          <section className="rounded-lg border border-gray-200 dark:border-gray-700 p-5 mb-6">
            <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Push notifications</h2>
            <div className="flex items-center gap-3 mb-4">
              <button
                onClick={push.subscribed ? push.unsubscribe : push.subscribe}
                disabled={push.loading}
                className={`px-4 py-2 text-sm font-semibold rounded-lg disabled:opacity-50 ${
                  push.subscribed
                    ? 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                    : 'bg-primary-600 text-white hover:bg-primary-700'
                }`}
              >
                {push.loading ? 'Working…' : push.subscribed ? 'Disable notifications' : 'Enable notifications'}
              </button>
              {push.subscribed && (
                <span className="text-xs text-green-600">Enabled on this device</span>
              )}
            </div>
            <NotificationPrefs />
          </section>
        )}

        <section className="rounded-lg border border-gray-200 dark:border-gray-700 p-5 mb-6">
          <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Appearance</h2>
          <div className="flex items-center gap-3 mb-4">
            <label htmlFor="settings-theme" className="text-xs text-gray-500 dark:text-gray-400">Theme</label>
            <select
              id="settings-theme"
              value={theme}
              onChange={e => setTheme(e.target.value as Theme)}
              className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm"
            >
              <option value="auto">Auto (system)</option>
              <option value="light">Light</option>
              <option value="dark">Dark</option>
            </select>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-xs text-gray-500 dark:text-gray-400">Accent</span>
            <div role="radiogroup" aria-label="Accent colour" className="flex flex-wrap gap-1.5">
              {ACCENT_PRESETS.map(p => (
                <button
                  key={p.id}
                  type="button"
                  role="radio"
                  aria-checked={accent === p.id}
                  aria-label={p.name}
                  title={p.name}
                  onClick={() => setAccent(p.id)}
                  className={`w-7 h-7 rounded-full border ${
                    accent === p.id
                      ? 'border-gray-900 dark:border-gray-100 ring-2 ring-offset-2 ring-offset-white dark:ring-offset-gray-800 ring-primary-500'
                      : 'border-gray-300 dark:border-gray-600'
                  }`}
                  style={{ backgroundColor: p.swatch }}
                />
              ))}
            </div>
          </div>
        </section>

        <section className="rounded-lg border border-gray-200 dark:border-gray-700 p-5">
          <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">Account info</h2>
          <dl className="text-sm space-y-1">
            <div className="flex gap-2">
              <dt className="text-gray-500 dark:text-gray-400 w-28 shrink-0">Email</dt>
              <dd className="text-gray-900 dark:text-gray-100">{user?.email}</dd>
            </div>
            <div className="flex gap-2">
              <dt className="text-gray-500 dark:text-gray-400 w-28 shrink-0">Role</dt>
              <dd className="text-gray-900 dark:text-gray-100">
                {user?.isAdmin ? 'Admin' : user?.isChild ? 'Child account' : 'Member'}
              </dd>
            </div>
          </dl>
        </section>

        <AboutSection />
        <PrivacySection />
        <DangerZone />
      </div>
    </AppLayout>
  )
}

function PrivacySection() {
  const clearAuth = useAuthStore(s => s.clearAuth)
  const [busy, setBusy] = useState(false)
  const qc = useQueryClient()
  const exports = useQuery({
    queryKey: ['me', 'exports'],
    queryFn: () => meApi.listExports(),
    // Poll while at least one export is still PENDING.
    refetchInterval: data => {
      const list = data as unknown as { state?: { data?: Array<{ status: string }> } }
      const items = list?.state?.data ?? []
      return items.some(e => e.status === 'PENDING') ? 5000 : false
    },
  })
  const requestExport = useMutation({
    mutationFn: () => meApi.requestExport(),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['me', 'exports'] }),
  })

  async function clearLocal() {
    if (!confirm('Sign out and wipe all locally-stored data on this device?')) return
    setBusy(true)
    try {
      // Wipe all storage layers we use. Each .catch(() => {}) handles
      // browsers that don't expose the API.
      try { localStorage.clear() } catch {}
      try { sessionStorage.clear() } catch {}
      if (typeof indexedDB !== 'undefined' && indexedDB.databases) {
        try {
          const dbs = await indexedDB.databases()
          await Promise.all(dbs.map(d => d.name && new Promise<void>(res => {
            const req = indexedDB.deleteDatabase(d.name!)
            req.onsuccess = req.onerror = req.onblocked = () => res()
          })))
        } catch {}
      }
      if ('caches' in window) {
        try {
          const keys = await caches.keys()
          await Promise.all(keys.map(k => caches.delete(k)))
        } catch {}
      }
      if ('serviceWorker' in navigator) {
        try {
          const regs = await navigator.serviceWorker.getRegistrations()
          await Promise.all(regs.map(r => r.unregister()))
        } catch {}
      }
    } finally {
      clearAuth()
      window.location.href = '/login'
    }
  }

  return (
    <section className="rounded-lg border border-gray-200 dark:border-gray-700 p-5">
      <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">Privacy</h2>
      <p className="text-xs text-gray-600 dark:text-gray-400 mb-3">
        On this device the app stores your auth token in <code>localStorage</code>,
        queued uploads + cached notes in <code>IndexedDB</code>, and the offline shell in
        the service-worker cache. See <a className="text-primary-600 dark:text-primary-400 hover:underline" href="/PRIVACY.md">PRIVACY.md</a> for the full list.
      </p>
      <div className="flex flex-wrap gap-2 mb-4">
        <button
          onClick={clearLocal}
          disabled={busy}
          className="px-3 py-1.5 text-xs font-medium text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
        >
          {busy ? 'Clearing…' : 'Clear local data and sign out'}
        </button>
        <button
          onClick={() => requestExport.mutate()}
          disabled={requestExport.isPending}
          className="px-3 py-1.5 text-xs font-medium text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
        >
          {requestExport.isPending ? 'Requesting…' : 'Export my data'}
        </button>
      </div>
      {exports.data && exports.data.length > 0 && (
        <div className="text-xs">
          <h3 className="font-semibold text-gray-700 dark:text-gray-300 mb-1">Recent exports</h3>
          <ul className="space-y-1">
            {exports.data.slice(0, 5).map(ex => (
              <li key={ex.id} className="flex items-center gap-2 text-gray-600 dark:text-gray-400">
                <span className="font-mono">#{ex.id}</span>
                <span>{ex.status}</span>
                <span>{new Date(ex.createdAt).toLocaleString()}</span>
                {ex.status === 'READY' && ex.downloadUrl && (
                  <a
                    href={ex.downloadUrl}
                    className="text-primary-600 dark:text-primary-400 hover:underline"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Download ({ex.sizeBytes ? `${Math.round(ex.sizeBytes / 1024)} KB` : '—'})
                  </a>
                )}
                {ex.status === 'FAILED' && ex.errorMessage && (
                  <span className="text-red-600 dark:text-red-400">{ex.errorMessage}</span>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  )
}

function DangerZone() {
  const clearAuth = useAuthStore(s => s.clearAuth)
  const [open, setOpen] = useState(false)
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!password) return
    setBusy(true)
    setError(null)
    try {
      await authApi.deleteMe(password)
      clearAuth()
      window.location.href = '/login'
    } catch (err) {
      const code = (err as { response?: { data?: { code?: string } } })?.response?.data?.code
      setError(
        code === 'INVALID_PASSWORD' ? 'Wrong password.' :
        code === 'LAST_ADMIN' ? 'You are the only admin — promote another user before deactivating.' :
        'Could not deactivate. Try again.',
      )
      setBusy(false)
    }
  }

  return (
    <section className="rounded-lg border border-red-200 dark:border-red-900/50 p-5 bg-red-50/30 dark:bg-red-900/10">
      <h2 className="text-sm font-semibold text-red-700 dark:text-red-400 mb-2">Danger zone</h2>
      <p className="text-xs text-gray-600 dark:text-gray-400 mb-3">
        Deactivating signs you out everywhere and blocks future sign-ins. Your data is preserved
        so an admin can re-enable the account; ask the household admin to fully delete your data
        if needed.
      </p>
      {!open ? (
        <button
          onClick={() => setOpen(true)}
          className="px-3 py-1.5 text-xs font-medium text-red-700 dark:text-red-300 border border-red-300 dark:border-red-700 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/40"
        >
          Deactivate my account…
        </button>
      ) : (
        <form onSubmit={submit} className="space-y-2">
          <label htmlFor="danger-password" className="block text-xs text-gray-600 dark:text-gray-400">
            Confirm with your password to continue
          </label>
          <input
            id="danger-password"
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            autoComplete="current-password"
            className="w-full max-w-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm"
          />
          {error && <p className="text-xs text-red-600 dark:text-red-400">{error}</p>}
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={busy || !password}
              className="px-3 py-1.5 text-xs font-semibold bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
            >
              {busy ? 'Deactivating…' : 'Yes, deactivate'}
            </button>
            <button
              type="button"
              onClick={() => { setOpen(false); setPassword(''); setError(null) }}
              className="px-3 py-1.5 text-xs text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg"
            >
              Cancel
            </button>
          </div>
        </form>
      )}
    </section>
  )
}

function NotificationPrefs() {
  const qc = useQueryClient()
  const { data: prefs } = useQuery({
    queryKey: ['me', 'notification-prefs'],
    queryFn: () => meApi.getNotificationPrefs(),
  })
  const update = useMutation({
    mutationFn: (next: Record<string, unknown>) => meApi.updateNotificationPrefs(next),
    onSuccess: data => qc.setQueryData(['me', 'notification-prefs'], data),
  })

  if (!prefs) return null
  // Default-on: an absent key means "send".
  const remindersEnabled = prefs.reminders !== false

  return (
    <div className="border-t border-gray-200 dark:border-gray-700 pt-4 space-y-2">
      <h3 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
        What to push
      </h3>
      <label className="flex items-center gap-2 text-sm">
        <input
          type="checkbox"
          checked={remindersEnabled}
          onChange={e => update.mutate({ ...prefs, reminders: e.target.checked })}
          className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
        />
        <span className="text-gray-700 dark:text-gray-300">Reminders</span>
      </label>
      <p className="text-xs text-gray-400 dark:text-gray-500">
        Disabling stops the server from sending push notifications for reminders, but the
        scheduler still records when each reminder fires (visible in the unread badge).
      </p>
    </div>
  )
}

function AboutSection() {
  const { data: info } = useQuery({
    queryKey: ['build-info'],
    queryFn: () => buildInfoApi.get(),
    staleTime: Infinity,
  })

  if (!info?.build && !info?.git) return null

  return (
    <section className="rounded-lg border border-gray-200 dark:border-gray-700 p-5">
      <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">About</h2>
      <dl className="text-sm space-y-1">
        {info.build?.version && (
          <div className="flex gap-2">
            <dt className="text-gray-500 dark:text-gray-400 w-28 shrink-0">Version</dt>
            <dd className="text-gray-900 dark:text-gray-100 font-mono">{info.build.version}</dd>
          </div>
        )}
        {info.git?.commitId && (
          <div className="flex gap-2">
            <dt className="text-gray-500 dark:text-gray-400 w-28 shrink-0">Commit</dt>
            <dd className="text-gray-900 dark:text-gray-100 font-mono">
              {info.git.commitId}
              {info.git.branch && info.git.branch !== 'master' && info.git.branch !== 'main'
                ? ` (${info.git.branch})`
                : ''}
            </dd>
          </div>
        )}
        {info.build?.time && (
          <div className="flex gap-2">
            <dt className="text-gray-500 dark:text-gray-400 w-28 shrink-0">Built</dt>
            <dd className="text-gray-900 dark:text-gray-100">{new Date(info.build.time).toLocaleString()}</dd>
          </div>
        )}
      </dl>
      <p className="text-xs text-gray-400 dark:text-gray-500 mt-3">
        Include these in bug reports.
      </p>
    </section>
  )
}
