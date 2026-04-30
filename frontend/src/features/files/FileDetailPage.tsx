import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fileApi, tagApi } from '../../api'
import { QK } from '../../lib/queryKeys'
import { formatBytes, formatDate } from '../../lib/format'
import { toast } from '../../lib/toastStore'
import AppLayout from '../../components/AppLayout'
import TagAutocomplete from '../../components/TagAutocomplete'
import CommentsThread from '../comments/CommentsThread'
import { useAuthStore } from '../../lib/authStore'

export default function FileDetailPage() {
  const { id } = useParams<{ id: string }>()
  const fileId = Number(id)
  const qc = useQueryClient()
  const navigate = useNavigate()
  const user = useAuthStore(s => s.user)

  const [editingName, setEditingName] = useState(false)
  const [draftName, setDraftName] = useState('')

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

  const renameFile = useMutation({
    mutationFn: (filename: string) => fileApi.update(fileId, { filename }),
    onSuccess: (updated) => {
      qc.setQueryData(QK.file(fileId), updated)
      qc.invalidateQueries({ queryKey: QK.files() })
      setEditingName(false)
    },
    onError: (err: { response?: { data?: { message?: string; errors?: { message: string }[] } } }) => {
      const data = err.response?.data
      const reason = data?.errors?.[0]?.message ?? data?.message
      toast.error(reason ? `Failed to rename file: ${reason}` : 'Failed to rename file')
    },
  })

  if (isLoading) return <AppLayout><div className="text-gray-400 dark:text-gray-500">Loading…</div></AppLayout>
  if (!file) return <AppLayout><div className="text-red-500">File not found</div></AppLayout>

  const startRename = () => {
    setDraftName(file.filename)
    setEditingName(true)
  }

  const submitRename = () => {
    const next = draftName.trim()
    if (!next || next === file.filename) {
      setEditingName(false)
      return
    }
    renameFile.mutate(next)
  }

  return (
    <AppLayout>
      <div className="max-w-2xl mx-auto space-y-6">
        <div className="flex items-start justify-between gap-4">
          {editingName ? (
            <div className="flex-1 flex flex-col sm:flex-row sm:items-center gap-2">
              <input
                autoFocus
                aria-label="Filename"
                value={draftName}
                onChange={e => setDraftName(e.target.value)}
                onKeyDown={e => {
                  if (e.key === 'Enter') { e.preventDefault(); submitRename() }
                  if (e.key === 'Escape') { e.preventDefault(); setEditingName(false) }
                }}
                disabled={renameFile.isPending}
                maxLength={500}
                className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-base font-semibold focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
              <div className="flex gap-2">
                <button
                  onClick={submitRename}
                  disabled={renameFile.isPending || !draftName.trim() || draftName.trim() === file.filename}
                  className="px-3 py-1.5 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
                >
                  Save
                </button>
                <button
                  onClick={() => setEditingName(false)}
                  disabled={renameFile.isPending}
                  className="px-3 py-1.5 text-sm text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  Cancel
                </button>
              </div>
            </div>
          ) : (
            <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100 break-all flex-1">{file.filename}</h1>
          )}
          {!user?.isChild && !editingName && (
            <div className="shrink-0 flex gap-2">
              <button
                onClick={startRename}
                className="px-3 py-1.5 text-sm text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
              >
                Rename
              </button>
              <button
                onClick={() => { if (confirm('Delete this file?')) deleteFile.mutate() }}
                className="px-3 py-1.5 text-sm text-red-600 border border-red-200 dark:border-red-800 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/30"
              >
                Delete
              </button>
            </div>
          )}
        </div>

        {/* Preview */}
        <FilePreview
          mimeType={file.mimeType}
          filename={file.filename}
          downloadUrl={file.downloadUrl}
          thumbnailUrl={file.thumbnailUrl}
        />

        {/* Metadata */}
        <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
          <dt className="text-gray-500 dark:text-gray-400">Type</dt>
          <dd className="text-gray-900 dark:text-gray-100">{file.mimeType}</dd>
          <dt className="text-gray-500 dark:text-gray-400">Size</dt>
          <dd className="text-gray-900 dark:text-gray-100">{formatBytes(file.sizeBytes)}</dd>
          <dt className="text-gray-500 dark:text-gray-400">Uploaded</dt>
          <dd className="text-gray-900 dark:text-gray-100">{formatDate(file.uploadedAt)}</dd>
          {file.description && (
            <>
              <dt className="text-gray-500 dark:text-gray-400">Description</dt>
              <dd className="text-gray-900 dark:text-gray-100">{file.description}</dd>
            </>
          )}
          <dt className="text-gray-500 dark:text-gray-400">Child safe</dt>
          <dd className="text-gray-900 dark:text-gray-100">{file.isChildSafe ? 'Yes' : 'No'}</dd>
        </dl>

        {/* Tags */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 mb-2">Tags</h2>
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

        <CommentsThread itemType="file" itemId={file.id} />
      </div>
    </AppLayout>
  )
}

interface FilePreviewProps {
  mimeType: string
  filename: string
  downloadUrl?: string | null
  thumbnailUrl?: string | null
}

export function FilePreview({ mimeType, filename, downloadUrl, thumbnailUrl }: FilePreviewProps) {
  const placeholder = (
    <div className="flex items-center justify-center h-40 rounded-xl bg-gray-100 dark:bg-gray-700 border border-gray-200 dark:border-gray-700">
      <span className="text-gray-400 dark:text-gray-500 text-sm">{mimeType}</span>
    </div>
  )

  if (mimeType.startsWith('image/')) {
    const src = thumbnailUrl ?? downloadUrl
    if (!src) return placeholder
    return (
      <img
        src={src}
        alt={filename}
        className="max-w-full rounded-xl border border-gray-200 dark:border-gray-700"
      />
    )
  }

  if (!downloadUrl) return placeholder

  if (mimeType === 'application/pdf') {
    return (
      <iframe
        src={`${downloadUrl}#toolbar=0`}
        title={filename}
        className="w-full h-[80vh] rounded-xl border border-gray-200 dark:border-gray-700"
      />
    )
  }

  if (mimeType.startsWith('video/')) {
    return (
      <video
        controls
        preload="metadata"
        className="max-w-full rounded-xl border border-gray-200 dark:border-gray-700"
      >
        <source src={downloadUrl} type={mimeType} />
      </video>
    )
  }

  if (mimeType.startsWith('audio/')) {
    return (
      <audio
        controls
        preload="metadata"
        className="w-full"
        aria-label={filename}
      >
        <source src={downloadUrl} type={mimeType} />
      </audio>
    )
  }

  return placeholder
}
