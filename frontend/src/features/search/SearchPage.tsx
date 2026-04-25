import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { searchApi } from '../../api'
import { QK } from '../../lib/queryKeys'
import AppLayout from '../../components/AppLayout'

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [input, setInput] = useState(searchParams.get('q') ?? '')
  const q = searchParams.get('q') ?? ''

  const { data, isLoading } = useQuery({
    queryKey: QK.search({ q }),
    queryFn: () => searchApi.search({ q, page: 0, size: 30 }),
    enabled: q.length >= 2,
  })

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (input.trim()) setSearchParams({ q: input.trim() })
  }

  return (
    <AppLayout>
      <div className="max-w-3xl mx-auto">
        <h1 className="text-xl font-bold text-gray-900 mb-4">Search</h1>

        <form onSubmit={handleSubmit} className="flex gap-2 mb-6">
          <input
            value={input}
            onChange={e => setInput(e.target.value)}
            placeholder="Search notes, files, folders…"
            className="flex-1 rounded-lg border border-gray-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          <button
            type="submit"
            className="px-4 py-2 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700"
          >
            Search
          </button>
        </form>

        {q.length >= 2 && isLoading && (
          <p className="text-gray-500 text-sm">Searching…</p>
        )}

        {q.length >= 2 && !isLoading && data?.content.length === 0 && (
          <p className="text-gray-500 text-sm">No results for "{q}".</p>
        )}

        <ul className="space-y-3">
          {data?.content.map(result => (
            <li key={`${result.type}-${result.id}`}>
              <Link
                to={result.type === 'note' ? `/notes/${result.id}` : result.type === 'folder' ? `/folders/${result.id}` : `/files/${result.id}`}
                className="block rounded-lg border border-gray-200 px-4 py-3 hover:bg-gray-50 transition-colors"
              >
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 capitalize">
                    {result.type}
                  </span>
                  <span className="font-medium text-gray-900 line-clamp-1">{result.title}</span>
                </div>
                {result.excerpt && (
                  <p
                    className="text-sm text-gray-600 line-clamp-2"
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
