export type OwnerFilter = 'all' | 'mine' | 'shared'

interface Props {
  value: OwnerFilter
  onChange: (next: OwnerFilter) => void
}

/**
 * Tri-state segmented control for filtering a list by ownership. "Shared"
 * means anything not owned by the current user — i.e. items shared with
 * me by another household member. Filter is intentionally client-side so
 * the same data fetch covers all three views without re-querying.
 */
export default function OwnerFilterTabs({ value, onChange }: Props) {
  const opts: { id: OwnerFilter; label: string }[] = [
    { id: 'all', label: 'All' },
    { id: 'mine', label: 'Mine' },
    { id: 'shared', label: 'Shared with me' },
  ]
  return (
    <div role="tablist" aria-label="Filter by owner" className="inline-flex rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-0.5 mb-4 text-xs">
      {opts.map(o => (
        <button
          key={o.id}
          type="button"
          role="tab"
          aria-selected={value === o.id}
          onClick={() => onChange(o.id)}
          className={`px-3 py-1 rounded-md transition-colors ${
            value === o.id
              ? 'bg-primary-600 text-white'
              : 'text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100'
          }`}
        >
          {o.label}
        </button>
      ))}
    </div>
  )
}
