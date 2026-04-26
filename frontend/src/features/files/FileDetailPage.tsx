import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fileApi, tagApi } from '../../api'
import { QK } from '../../lib/queryKeys'
import { toast } from '../../lib/toastStore'
import AppLayout from '../../components/AppLayout'
import TagAutocomplete from '../../components/TagAutocomplete'
import { useAuthStore } from '../../lib/authStore'

export default function FileDetailPage() {
  const { id } = useParams<{ id: string }>()
  const fileId = Number(id)
  const qc = useQueryClient()
  const navigate = useNavigate()
  const user = useAuthStore(s => s.user)

  const { data: file, isLoading } = useQuery({
    queryKey: QK.file(fileId),
    queryFn: () => fileApi.getById(fileId),
  })

  const { data: fileTags = [] } = useQuery({
    queryKey: QK.fileTags(fileId),
    queryFn: () => tagApi.getForFile(fileId),
  })

  const deleteFile = useMutation({
    mutationFn: () => fileApi.delete(fileId),
    onSuccess: () => navigate(file?.folderId ? `/folders/${file.folderId}` : '/files'),
    onError: () => toast.error('Failed to delete file'),
  })

  if (isLoading) return <AppLayout><div className="text-gray-400">Loading…</div></AppLayout>
  if (!file) return <AppLayout><div className="text-red-500">File not found</div></AppLayout>

  return (
    <AppLayout>
      <div className="max-w-2xl mx-auto space-y-6">
        <div className="flex items-start justify-between gap-4">
          <h1 className="text-xl font-bold text-gray-900 break-all">{file.filename}</h1>
          {!user?.isChild && (
            <button
              onClick={() => { if (confirm('Delete this file?')) deleteFile.mutate() }}
              className="shrink-0 px-3 py-1.5 text-sm text-red-600 border border-red-200 rounded-lg hover:bg-red-50"
            >
              Delete
            </button>
          )}
        </div>

        {/* Preview */}
        {file.thumbnailUrl ? (
          <img
            src={file.thumbnailUrl}
            alt={file.filename}
            className="max-w-full rounded-xl border border-gray-200"
          />
        ) : (
          <div className="flex items-center justify-center h-40 rounded-xl bg-gray-100 border border-gray-200">
            <span className="text-gray-400 text-sm">{file.mimeType}</span>
          </div>
        )}

        {/* Metadata */}
        <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
          <dt className="text-gray-500">Type</dt>
          <dd className="text-gray-900">{file.mimeType}</dd>
          <dt className="text-gray-500">Size</dt>
          <dd className="text-gray-900">{formatSize(file.sizeBytes)}</dd>
          <dt className="text-gray-500">Uploaded</dt>
          <dd className="text-gray-900">{new Date(file.uploadedAt).toLocaleDateString()}</dd>
          {file.description && (
            <>
              <dt className="text-gray-500">Description</dt>
              <dd className="text-gray-900">{file.description}</dd>
            </>
          )}
          <dt className="text-gray-500">Child safe</dt>
          <dd className="text-gray-900">{file.isChildSafe ? 'Yes' : 'No'}</dd>
        </dl>

        {/* Tags */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 mb-2">Tags</h2>
          <TagAutocomplete
            entityType="file"
            entityId={fileId}
            currentTags={fileTags}
            onTagsChange={() => qc.invalidateQueries({ queryKey: QK.fileTags(fileId) })}
            readOnly={user?.isChild}
          />
        </section>

        {/* Download */}
        {file.downloadUrl && (
          <a
            href={file.downloadUrl}
            target="_blank"
            rel="noreferrer"
            className="inline-block px-5 py-2 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700"
          >
            Download
          </a>
        )}
      </div>
    </AppLayout>
  )
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
