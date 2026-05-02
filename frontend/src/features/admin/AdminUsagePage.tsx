import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { adminApi } from '../../api'
import AppLayout from '../../components/AppLayout'

function formatBytes(b: number): string {
  if (b < 1024) return `${b} B`
  const units = ['KB', 'MB', 'GB', 'TB']
  let v = b / 1024
  let i = 0
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i += 1 }
  return `${v.toFixed(v >= 100 ? 0 : 1)} ${units[i]}`
}

/**
 * Admin-only usage snapshot. Numbers come straight from
 * AdminUsageController so they're always live — no caching beyond the
 * default TanStack staleTime. Tables stop at the top 10 per server.
 */
export default function AdminUsagePage() {
  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'usage'],
    queryFn: () => adminApi.usage(),
  })

  return (
    <AppLayout>
      <div className="max-w-4xl mx-auto space-y-6">
        <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100">Usage</h1>
        {isLoading && <p className="text-sm text-gray-400">Loading…</p>}
        {data && (
          <>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <Tile label="Users" value={data.users} sub={`${data.activeUsers} active (30d)`} />
              <Tile label="Notes" value={data.notes} />
              <Tile label="Files" value={data.files} sub={formatBytes(data.storageBytes)} />
              <Tile label="Folders" value={data.folders} />
              <Tile label="Tags" value={data.tags} />
              <Tile label="Reminders" value={data.reminders} />
              <Tile label="Saved searches" value={data.savedSearches} />
            </div>

            <Section title="Top storage by user">
              {data.topStorageUsers.length === 0 ? (
                <Empty>Nothing uploaded yet.</Empty>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-xs uppercase text-gray-500 dark:text-gray-400">
                      <th className="py-1">User</th>
                      <th className="py-1 text-right">Files</th>
                      <th className="py-1 text-right">Size</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.topStorageUsers.map(u => (
                      <tr key={u.userId} className="border-t border-gray-200 dark:border-gray-700">
                        <td className="py-1.5">{u.displayName}</td>
                        <td className="py-1.5 text-right text-gray-500 dark:text-gray-400">{u.fileCount}</td>
                        <td className="py-1.5 text-right font-mono">{formatBytes(u.bytes)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </Section>

            <div className="grid sm:grid-cols-2 gap-6">
              <Section title="Top tags">
                {data.topTags.length === 0 ? (
                  <Empty>No tags in use.</Empty>
                ) : (
                  <ul className="text-sm space-y-1">
                    {data.topTags.map(t => (
                      <li key={t.tagId} className="flex justify-between">
                        <Link to="/admin/tags" className="text-gray-900 dark:text-gray-100 hover:underline">
                          {t.name}
                        </Link>
                        <span className="text-gray-500 dark:text-gray-400">{t.usageCount}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </Section>
              <Section title="Top folders">
                {data.topFolders.length === 0 ? (
                  <Empty>No folders yet.</Empty>
                ) : (
                  <ul className="text-sm space-y-1">
                    {data.topFolders.map(f => (
                      <li key={f.folderId} className="flex justify-between">
                        <Link to={`/folders/${f.folderId}`} className="text-gray-900 dark:text-gray-100 hover:underline">
                          {f.name}
                        </Link>
                        <span className="text-gray-500 dark:text-gray-400">{f.itemCount}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </Section>
            </div>
          </>
        )}
      </div>
    </AppLayout>
  )
}

function Tile({ label, value, sub }: { label: string; value: number; sub?: string }) {
  return (
    <div className="rounded-xl border border-gray-200 dark:border-gray-700 p-3 bg-white dark:bg-gray-800">
      <p className="text-xs text-gray-500 dark:text-gray-400">{label}</p>
      <p className="text-2xl font-semibold text-gray-900 dark:text-gray-100">{value.toLocaleString()}</p>
      {sub && <p className="text-xs text-gray-400 dark:text-gray-500 mt-0.5">{sub}</p>}
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-xl border border-gray-200 dark:border-gray-700 p-4">
      <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">{title}</h2>
      {children}
    </section>
  )
}

function Empty({ children }: { children: React.ReactNode }) {
  return <p className="text-sm text-gray-500 dark:text-gray-400">{children}</p>
}
