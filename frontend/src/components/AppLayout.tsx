import { Link, useLocation } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { useAuthStore } from '../lib/authStore'
import { useQuery } from '@tanstack/react-query'
import { folderApi, authApi } from '../api'
import { QK } from '../lib/queryKeys'
import QueueStatusBadge from './QueueStatusBadge'
import IOSInstallPrompt from './IOSInstallPrompt'
import ThemeToggle from './ThemeToggle'
import type { FolderResponse } from '../types'
import { useRealtimeEvents } from '../lib/useRealtimeEvents'
import SkipLink from './SkipLink'
import CommandPalette from './CommandPalette'

function FolderTreeItem({ folder, depth = 0 }: { folder: FolderResponse; depth?: number }) {
  return (
    <li>
      <Link
        to={`/folders/${folder.id}`}
        className="flex items-center gap-1 px-2 py-1 rounded-md text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 truncate"
        style={{ paddingLeft: `${(depth + 1) * 12}px` }}
      >
        <span className="text-gray-400">📁</span>
        {folder.name}
      </Link>
      {folder.children.length > 0 && (
        <ul>
          {folder.children.map(child => (
            <FolderTreeItem key={child.id} folder={child} depth={depth + 1} />
          ))}
        </ul>
      )}
    </li>
  )
}

function Sidebar() {
  const user = useAuthStore(s => s.user)
  const clearAuth = useAuthStore(s => s.clearAuth)
  const refreshToken = useAuthStore(s => s.refreshToken)

  function handleLogout() {
    authApi.logout(refreshToken).catch(() => {})
    clearAuth()
  }
  const { data: folders = [] } = useQuery({ queryKey: QK.folders(), queryFn: folderApi.getTree })

  return (
    <aside className="hidden md:flex flex-col w-60 shrink-0 border-r border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 h-screen sticky top-0 overflow-y-auto">
      <div className="px-4 py-4 border-b border-gray-100 dark:border-gray-700">
        <Link to="/" className="text-lg font-bold text-primary-600 dark:text-primary-400">
          {import.meta.env.VITE_APP_NAME}
        </Link>
        {user?.isChild && (
          <span className="ml-2 text-xs bg-purple-100 dark:bg-purple-900 text-purple-700 dark:text-purple-300 px-2 py-0.5 rounded-full">Kid Mode</span>
        )}
      </div>

      <nav className="flex-1 px-2 py-3 space-y-0.5">
        <Link to="/" className="flex items-center gap-2 px-3 py-2 rounded-md text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
          🏠 Home
        </Link>
        <Link to="/search" className="flex items-center gap-2 px-3 py-2 rounded-md text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
          🔍 Search
        </Link>

        <div className="pt-2 pb-1 px-3 text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-wide">Folders</div>
        <ul className="space-y-0.5">
          {folders.map(folder => (
            <FolderTreeItem key={folder.id} folder={folder} />
          ))}
        </ul>
      </nav>

      <div className="px-3 py-3 border-t border-gray-100 dark:border-gray-700 space-y-1">
        <div className="px-1 pb-1">
          <QueueStatusBadge />
        </div>
        {user?.isAdmin && (
          <>
            <Link to="/admin/users" className="flex items-center gap-2 px-3 py-1.5 rounded-md text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700">
              👤 Admin
            </Link>
            <Link to="/admin/audit" className="flex items-center gap-2 px-3 py-1.5 rounded-md text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700">
              📋 Audit Log
            </Link>
            <Link to="/admin/invitations" className="flex items-center gap-2 px-3 py-1.5 rounded-md text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700">
              ✉️ Invitations
            </Link>
          </>
        )}
        <Link to="/trash" className="flex items-center gap-2 px-3 py-1.5 rounded-md text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700">
          🗑️ Trash
        </Link>
        <Link to="/settings" className="flex items-center gap-2 px-3 py-1.5 rounded-md text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700">
          ⚙️ Settings
        </Link>
        <ThemeToggle />
        <button
          onClick={handleLogout}
          className="w-full text-left flex items-center gap-2 px-3 py-1.5 rounded-md text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950"
        >
          ← Sign out
        </button>
      </div>
    </aside>
  )
}

function BottomTabBar() {
  const location = useLocation()
  const active = (path: string) =>
    location.pathname === path ? 'text-primary-600 dark:text-primary-400' : 'text-gray-500 dark:text-gray-400'

  return (
    <nav className="md:hidden fixed bottom-0 inset-x-0 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 flex z-10">
      {[
        { to: '/', icon: '🏠', label: 'Home' },
        { to: '/search', icon: '🔍', label: 'Search' },
        { to: '/settings', icon: '⚙️', label: 'Settings' },
      ].map(tab => (
        <Link key={tab.to} to={tab.to}
          className={`flex-1 flex flex-col items-center py-2 text-xs ${active(tab.to)}`}>
          <span className="text-lg">{tab.icon}</span>
          {tab.label}
        </Link>
      ))}
    </nav>
  )
}

function OfflineBanner() {
  const [online, setOnline] = useState(navigator.onLine)
  useEffect(() => {
    const on = () => setOnline(true)
    const off = () => setOnline(false)
    window.addEventListener('online', on)
    window.addEventListener('offline', off)
    return () => { window.removeEventListener('online', on); window.removeEventListener('offline', off) }
  }, [])
  if (online) return null
  return (
    <div className="bg-yellow-50 dark:bg-yellow-900/30 border-b border-yellow-200 dark:border-yellow-700 px-4 py-2 text-sm text-yellow-800 dark:text-yellow-200 text-center">
      You are offline. Changes will sync when reconnected.
    </div>
  )
}

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const user = useAuthStore(s => s.user)
  useRealtimeEvents()
  return (
    <div className="flex min-h-screen bg-gray-50 dark:bg-gray-900">
      <SkipLink />
      <CommandPalette />
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0">
        <IOSInstallPrompt />
        <OfflineBanner />
        {user?.isChild && (
          <div role="status" className="bg-purple-50 dark:bg-purple-900/30 border-b border-purple-200 dark:border-purple-700 px-4 py-1.5 text-xs text-purple-800 dark:text-purple-200 text-center font-medium">
            Kid Mode — editing and admin controls are hidden
          </div>
        )}
        <main id="main" tabIndex={-1} className="flex-1 p-4 md:p-6 pb-16 md:pb-6">
          {children}
        </main>
      </div>
      <BottomTabBar />
    </div>
  )
}
