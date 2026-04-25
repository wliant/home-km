import { useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fileApi } from '../../api'
import { QK } from '../../lib/queryKeys'
import AppLayout from '../../components/AppLayout'
import { enqueueUpload } from '../../lib/offlineDb'

export default function FilesListPage() {
  const [searchParams] = useSearchParams()
  const folderId = searchParams.get('folderId') ? Number(searchParams.get('folderId')) : undefined
  const qc = useQueryClient()
  const inputRef = useRef<HTMLInputElement>(null)
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState<{ name: string; pct: number } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [queued, setQueued] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: QK.files({ folderId }),
    queryFn: () => fileApi.list({ folderId, page: 0, size: 40 }),
  })

  const deleteFile = useMutation({
    mutationFn: (id: number) => fileApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: QK.files({ folderId }) }),
  })

  async function handleUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const files = e.target.files
    if (!files?.length) return

    if (!navigator.onLine) {
      // Offline: enqueue all files for later upload
      let count = 0
      for (const file of Array.from(files)) {
        await enqueueUpload({
          clientUploadId: crypto.randomUUID(),
          filename: file.name,
          mimeType: file.type || 'application/octet-stream',
          folderId,
          blob: file,
        })
        count++
      }
      setQueued(count)
      if (inputRef.current) inputRef.current.value = ''
      return
    }

    setUploading(true)
    setError(null)
    setQueued(0)
    try {
      for (const file of Array.from(files)) {
        setUploadProgress({ name: file.name, pct: 0 })
        await fileApi.upload(file, folderId, undefined, (pct) =>
          setUploadProgress({ name: file.name, pct }),
        )
      }
      qc.invalidateQueries({ queryKey: QK.files({ folderId }) })
    } catch {
      setError('Upload failed.')
    } finally {
      setUploading(false)
      setUploadProgress(null)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  return (
    <AppLayout>
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-bold text-gray-900">Files</h1>
          <div>
            <input
              ref={inputRef}
              type="file"
              multiple
              className="hidden"
              id="file-upload"
              onChange={handleUpload}
            />
            <label
              htmlFor="file-upload"
              className="cursor-pointer px-4 py-2 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700"
            >
              {uploading ? 'Uploading…' : 'Upload'}
            </label>
          </div>
        </div>

        {error && <p className="mb-4 text-sm text-red-600">{error}</p>}
        {uploadProgress && (
          <div className="mb-4 space-y-1">
            <p className="text-xs text-gray-600 truncate">Uploading {uploadProgress.name}…</p>
            <div className="w-full bg-gray-200 rounded-full h-1.5">
              <div
                className="bg-primary-600 h-1.5 rounded-full transition-all duration-150"
                style={{ width: `${uploadProgress.pct}%` }}
              />
            </div>
          </div>
        )}
        {queued > 0 && (
          <p className="mb-4 text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
            You are offline. {queued} file{queued !== 1 ? 's' : ''} queued for upload when connection is restored.
          </p>
        )}
        {isLoading && <p className="text-gray-500 text-sm">Loading…</p>}

        {data?.content.length === 0 && !isLoading && (
          <p className="text-gray-500 text-sm">No files yet.</p>
        )}

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {data?.content.map(file => (
            <div
              key={file.id}
              className="group relative rounded-lg border border-gray-200 overflow-hidden cursor-pointer"
            >
              {file.thumbnailUrl ? (
                <img
                  src={file.thumbnailUrl}
                  alt={file.filename}
                  className="w-full h-32 object-cover bg-gray-100"
                />
              ) : (
                <div className="w-full h-32 bg-gray-100 flex items-center justify-center">
                  <span className="text-xs text-gray-400 break-all px-2 text-center">
                    {file.mimeType.split('/')[1] ?? file.mimeType}
                  </span>
                </div>
              )}
              <div className="p-2">
                <p className="text-xs text-gray-700 line-clamp-1 font-medium">{file.filename}</p>
                <p className="text-xs text-gray-400">{(file.sizeBytes / 1024).toFixed(0)} KB</p>
              </div>
              <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                <Link
                  to={`/files/${file.id}`}
                  className="px-2 py-1 bg-white text-xs text-gray-800 rounded hover:bg-gray-100"
                >
                  View
                </Link>
                <a
                  href={file.downloadUrl ?? undefined}
                  target="_blank"
                  rel="noreferrer"
                  className="px-2 py-1 bg-white text-xs text-gray-800 rounded hover:bg-gray-100"
                >
                  Download
                </a>
                <button
                  onClick={() => { if (confirm('Delete file?')) deleteFile.mutate(file.id) }}
                  className="px-2 py-1 bg-red-600 text-xs text-white rounded hover:bg-red-700"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </AppLayout>
  )
}
