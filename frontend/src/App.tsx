import { Routes, Route, Navigate } from 'react-router-dom'
import { lazy, Suspense, useEffect } from 'react'
import LoginPage from './features/auth/LoginPage'
import RegisterPage from './features/auth/RegisterPage'
import ProtectedRoute from './components/ProtectedRoute'
import AdminRoute from './components/AdminRoute'
import { useAuthStore } from './lib/authStore'
import { useThemeStore } from './lib/themeStore'
import AppLayout from './components/AppLayout'

const FolderPage = lazy(() => import('./features/folders/FolderPage'))
const NoteDetailPage = lazy(() => import('./features/notes/NoteDetailPage'))
const NoteEditorPage = lazy(() => import('./features/notes/NoteEditorPage'))
const NotesListPage = lazy(() => import('./features/notes/NotesListPage'))
const FilesListPage = lazy(() => import('./features/files/FilesListPage'))
const FileDetailPage = lazy(() => import('./features/files/FileDetailPage'))
const SearchPage = lazy(() => import('./features/search/SearchPage'))
const AdminUsersPage = lazy(() => import('./features/admin/AdminUsersPage'))
const AdminTagsPage = lazy(() => import('./features/admin/AdminTagsPage'))
const AuditLogPage = lazy(() => import('./features/admin/AuditLogPage'))
const SettingsPage = lazy(() => import('./features/settings/SettingsPage'))
const TrashPage = lazy(() => import('./features/trash/TrashPage'))
const ForgotPasswordPage = lazy(() => import('./features/auth/ForgotPasswordPage'))
const ResetPasswordPage = lazy(() => import('./features/auth/ResetPasswordPage'))

function HomePage() {
  const user = useAuthStore(s => s.user)
  return (
    <AppLayout>
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold text-primary-600 mb-2">
          {import.meta.env.VITE_APP_NAME}
        </h1>
        <p className="text-gray-600 dark:text-gray-400 mb-6">Welcome{user?.displayName ? `, ${user.displayName}` : ''}.</p>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          {[
            { to: '/notes', label: 'Notes' },
            { to: '/files', label: 'Files' },
            { to: '/search', label: 'Search' },
            { to: '/settings', label: 'Settings' },
          ].map(item => (
            <a
              key={item.to}
              href={item.to}
              className="rounded-xl border border-gray-200 dark:border-gray-700 p-4 text-sm font-medium text-gray-700 dark:text-gray-300 hover:border-primary-300 hover:bg-primary-50 dark:hover:bg-primary-900/30 transition-colors"
            >
              {item.label}
            </a>
          ))}
        </div>
      </div>
    </AppLayout>
  )
}

function Loading() {
  return <AppLayout><div className="text-gray-400 dark:text-gray-500 text-sm">Loading…</div></AppLayout>
}

function Protected({ children }: { children: React.ReactNode }) {
  return <ProtectedRoute>{children}</ProtectedRoute>
}

function useApplyTheme() {
  const theme = useThemeStore(s => s.theme)
  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    function apply() {
      const isDark = theme === 'dark' || (theme === 'auto' && mq.matches)
      document.documentElement.classList.toggle('dark', isDark)
    }
    apply()
    mq.addEventListener('change', apply)
    return () => mq.removeEventListener('change', apply)
  }, [theme])
}

export default function App() {
  useApplyTheme()
  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        <Route path="/" element={<Protected><HomePage /></Protected>} />
        <Route path="/notes" element={<Protected><NotesListPage /></Protected>} />
        <Route path="/notes/new" element={<Protected><NoteEditorPage /></Protected>} />
        <Route path="/notes/:id" element={<Protected><NoteDetailPage /></Protected>} />
        <Route path="/notes/:id/edit" element={<Protected><NoteEditorPage /></Protected>} />
        <Route path="/folders/:id" element={<Protected><FolderPage /></Protected>} />
        <Route path="/files" element={<Protected><FilesListPage /></Protected>} />
        <Route path="/files/:id" element={<Protected><FileDetailPage /></Protected>} />
        <Route path="/search" element={<Protected><SearchPage /></Protected>} />
        <Route path="/admin/users" element={<AdminRoute><AdminUsersPage /></AdminRoute>} />
        <Route path="/admin/tags" element={<AdminRoute><AdminTagsPage /></AdminRoute>} />
        <Route path="/admin/audit" element={<AdminRoute><AuditLogPage /></AdminRoute>} />
        <Route path="/settings" element={<Protected><SettingsPage /></Protected>} />
        <Route path="/trash" element={<Protected><TrashPage /></Protected>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}
