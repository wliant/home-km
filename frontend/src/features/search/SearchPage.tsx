import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { savedSearchApi, searchApi } from '../../api'
import type { SavedSearch, SearchResult } from '../../types'
import { QK } from '../../lib/queryKeys'
import { toast } from '../../lib/toastStore'
import AppLayout from '../../components/AppLayout'

type TypeFilter = 'all' | SearchResult['type']

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [input, setInput] = useState(searchParams.get('q') ?? '')
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('all')
  const q = searchParams.get('q') ?? ''
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: QK.search({ q }),
    queryFn: () => searchApi.search({ q, page: 0, size: 30 }),
    enabled: q.length >= 2,
  })

  const { data: savedSearches = [] } = useQuery({
    queryKey: QK.savedSearches(),
    queryFn: () => savedSearchApi.list(),
  })

  const createSaved = useMutation({
    mutationFn: savedSearchApi.create,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QK.savedSearches() })
      toast.success('Saved search')
    },
    onError: (err: { response?: { data?: { code?: string } } }) => {
      const code = err.response?.data?.code
      toast.error(code === 'DUPLICATE_NAME' ? 'You already have a saved search with that name.' : 'Could not save search')
    },
  })

  const deleteSaved = useMutation({
    mutationFn: savedSearchApi.delete,
    onSuccess: () => qc.invalidateQueries({ queryKey: QK.savedSearches() }),
  })

  const counts = useMemo(() => {
    const acc = { all: 0, note: 0, file: 0, folder: 0 }
    for (const r of data?.content ?? []) {
      acc.all += 1
      acc[r.type] += 1
    }
    return acc
  }, [data?.content])

  const filteredResults = useMemo(() => {
    if (!data) return []
    if (typeFilter === 'all') return data.content
    return data.content.filter(r => r.type === typeFilter)
  }, [data, typeFilter])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const next = input.trim()
    if (next) {
      setSearchParams({ q: next })
      setTypeFilter('all')
    }
  }

  function applySaved(s: SavedSearch) {
    setInput(s.query)
    setSearchParams({ q: s.query })
    setTypeFilter('all')
  }

  function applySuggestion(suggestion: string) {
    setInput(suggestion)
    setSearchParams({ q: suggestion })
  }

  function saveCurrent() {
    const name = window.prompt('Name this search:', q)
    if (!name) return
    createSaved.mutate({ name: name.trim(), query: q })
  }

  const showSaveButton = q.length >= 2 && !savedSearches.some(s => s.query === q)

  return (
    <AppLayout>
      <div className="max-w-3xl mx-auto">
        <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100 mb-4">Search</h1>

        {savedSearches.length > 0 && (
          <div className="flex flex-wrap gap-2 mb-4">
            {savedSearches.map(s => (
              <span key={s.id} className="inline-flex items-center gap-1 rounded-full border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 pl-3 pr-1 py-1 text-xs">
                <button
                  type="button"
                  onClick={() => applySaved(s)}
                  className="text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-400"
                  title={s.query}
                >
                  {s.name}
                </button>
                <button
                  type="button"
                  onClick={() => deleteSaved.mutate(s.id)}
                  aria-label={`Delete saved search ${s.name}`}
                  className="ml-1 text-gray-400 hover:text-red-500 px-1"
                >
                  ×
                </button>
              </span>
            ))}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex gap-2 mb-4">
          <label htmlFor="search-input" className="sr-only">Search</label>
          <input
            id="search-input"
            value={input}
            onChange={e => setInput(e.target.value)}
            placeholder="Search notes, files, folders…"
            className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          <button
            type="submit"
            className="px-4 py-2 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700"
          >
            Search
          </button>
          {showSaveButton && (
            <button
              type="button"
              onClick={saveCurrent}
              className="px-3 py-2 text-sm font-medium text-primary-600 dark:text-primary-400 border border-primary-200 dark:border-primary-700 rounded-lg hover:bg-primary-50 dark:hover:bg-primary-900/30"
            >
              Save
            </button>
          )}
        </form>

        {q.length >= 2 && data && data.content.length > 0 && (
          <div role="tablist" aria-label="Filter by type" className="flex gap-1 border-b border-gray-200 dark:border-gray-700 mb-4 text-sm">
            {(['all', 'note', 'file', 'folder'] as const).map(t => (
              <button
                key={t}
                role="tab"
                aria-selected={typeFilter === t}
                onClick={() => setTypeFilter(t)}
                className={`px-3 py-1.5 -mb-px border-b-2 ${
                  typeFilter === t
                    ? 'border-primary-600 text-primary-600 dark:text-primary-400 font-medium'
                    : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
                }`}
              >
                {t === 'all' ? 'All' : t[0].toUpperCase() + t.slice(1) + 's'} {counts[t]}
              </button>
            ))}
          </div>
        )}

        {q.length >= 2 && isLoading && (
          <p className="text-gray-500 dark:text-gray-400 text-sm">Searching…</p>
        )}

        {q.length >= 2 && !isLoading && data && data.content.length === 0 && (
          <div className="text-sm">
            <p className="text-gray-500 dark:text-gray-400">No results for "{q}".</p>
            {data.suggestion && (
              <p className="mt-2 text-gray-700 dark:text-gray-300">
                Did you mean{' '}
                <button
                  type="button"
                  onClick={() => applySuggestion(data.suggestion!)}
                  className="text-primary-600 dark:text-primary-400 hover:underline font-medium"
                >
                  {data.suggestion}
                </button>
                ?
              </p>
            )}
          </div>
        )}

        <ul className="space-y-3">
          {filteredResults.map(result => (
            <li key={`${result.type}-${result.id}`}>
              <Link
                to={result.type === 'note' ? `/notes/${result.id}` : result.type === 'folder' ? `/folders/${result.id}` : `/files/${result.id}`}
                className="block rounded-lg border border-gray-200 dark:border-gray-700 px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
              >
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 capitalize">
                    {result.type}
                  </span>
                  <span className="font-medium text-gray-900 dark:text-gray-100 line-clamp-1">{result.title}</span>
                </div>
                {result.excerpt && (
                  <p
                    className="text-sm text-gray-600 dark:text-gray-400 line-clamp-2"
                    dangerouslySetInnerHTML={{ __html: result.excerpt }}
                  />
                )}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </AppLayout>
  )
}
