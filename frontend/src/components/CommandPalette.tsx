import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

interface Action {
  id: string
  label: string
  hint?: string
  run: () => void
}

export default function CommandPalette() {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    let chord: string | null = null
    let chordTimer: number | null = null
    function clearChord() {
      chord = null
      if (chordTimer) window.clearTimeout(chordTimer)
      chordTimer = null
    }

    function onKey(e: KeyboardEvent) {
      const target = e.target as HTMLElement | null
      const inEditable =
        target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable)

      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        setOpen(o => !o)
        return
      }
      if (e.key === 'Escape') {
        if (open) setOpen(false)
        clearChord()
        return
      }
      if (e.key === '?' && !inEditable && (e.shiftKey || e.key === '?')) {
        e.preventDefault()
        setOpen(true)
        setQuery('shortcut')
        return
      }
      if (inEditable) return
      if (chord === 'g') {
        if (e.key === 'n') { navigate('/notes'); clearChord(); return }
        if (e.key === 'f') { navigate('/files'); clearChord(); return }
        if (e.key === 's') { navigate('/search'); clearChord(); return }
        clearChord()
      }
      if (e.key === 'g') {
        chord = 'g'
        chordTimer = window.setTimeout(clearChord, 1500)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, navigate])

  if (!open) return null

  const actions: Action[] = [
    { id: 'search', label: 'Search', hint: 'Cmd/Ctrl+K, then type', run: () => navigate('/search') },
    { id: 'notes', label: 'Go to Notes', hint: 'g n', run: () => navigate('/notes') },
    { id: 'files', label: 'Go to Files', hint: 'g f', run: () => navigate('/files') },
    { id: 'settings', label: 'Open Settings', run: () => navigate('/settings') },
    { id: 'shortcut', label: 'Shortcuts: Cmd/Ctrl+K opens this palette; g n / g f / g s for navigation; ? to open this list.', run: () => {} },
  ]
  const filtered = query
    ? actions.filter(a => a.label.toLowerCase().includes(query.toLowerCase()) || a.id.includes(query.toLowerCase()))
    : actions

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Command palette"
      className="fixed inset-0 z-50 flex items-start justify-center bg-black/40 pt-24"
      onClick={() => setOpen(false)}
    >
      <div
        className="w-full max-w-md rounded-lg bg-white dark:bg-gray-800 shadow-xl border border-gray-200 dark:border-gray-700"
        onClick={e => e.stopPropagation()}
      >
        <input
          autoFocus
          placeholder="Type a command…"
          value={query}
          onChange={e => setQuery(e.target.value)}
          className="w-full bg-transparent border-b border-gray-200 dark:border-gray-700 px-4 py-3 text-sm focus:outline-none text-gray-900 dark:text-gray-100"
        />
        <ul className="max-h-60 overflow-y-auto py-1">
          {filtered.map(a => (
            <li key={a.id}>
              <button
                onClick={() => { a.run(); setOpen(false) }}
                className="w-full text-left flex justify-between items-center px-4 py-2 text-sm hover:bg-gray-100 dark:hover:bg-gray-700"
              >
                <span className="text-gray-900 dark:text-gray-100">{a.label}</span>
                {a.hint && <span className="text-xs text-gray-400">{a.hint}</span>}
              </button>
            </li>
          ))}
          {filtered.length === 0 && <li className="px-4 py-2 text-xs text-gray-500">No matches.</li>}
        </ul>
      </div>
    </div>
  )
}
