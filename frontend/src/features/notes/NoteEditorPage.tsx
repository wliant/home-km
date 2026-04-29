import { useNavigate, useSearchParams, useParams } from 'react-router-dom'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeSanitize from 'rehype-sanitize'
import { noteApi, folderApi } from '../../api'
import { QK } from '../../lib/queryKeys'
import AppLayout from '../../components/AppLayout'
import { NOTE_LABELS } from '../../types'

const schema = z.object({
  title: z.string().min(1, 'Title is required').max(500),
  body: z.string().max(100_000).optional(),
  label: z.string().optional(),
  folderId: z.number().optional(),
  isChildSafe: z.boolean().optional(),
})

type FormData = z.infer<typeof schema>

export default function NoteEditorPage() {
  const { id } = useParams<{ id?: string }>()
  const isEdit = id != null
  const noteId = Number(id)
  const [searchParams] = useSearchParams()
  const defaultFolderId = searchParams.get('folderId') ? Number(searchParams.get('folderId')) : undefined
  // Pre-fill from PWA share-target redirect or any deep-link.
  const defaultTitle = searchParams.get('title') ?? ''
  const defaultBody = searchParams.get('body') ?? ''
  const qc = useQueryClient()
  const navigate = useNavigate()

  const { data: existing } = useQuery({
    queryKey: QK.note(noteId),
    queryFn: () => noteApi.getById(noteId),
    enabled: isEdit,
  })

  const { data: folders = [] } = useQuery({
    queryKey: QK.folders(),
    queryFn: folderApi.getTree,
  })

  const [tab, setTab] = useState<'edit' | 'preview'>('edit')

  const { register, handleSubmit, control, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { title: defaultTitle, body: defaultBody || undefined, folderId: defaultFolderId },
    values: existing ? {
      title: existing.title,
      body: existing.body ?? '',
      label: existing.label,
      folderId: existing.folderId ?? undefined,
      isChildSafe: existing.isChildSafe,
    } : undefined,
  })

  const createNote = useMutation({
    mutationFn: (data: FormData) => noteApi.create(data),
    onSuccess: note => { qc.invalidateQueries({ queryKey: QK.notes() }); navigate(`/notes/${note.id}`) },
  })

  const updateNote = useMutation({
    mutationFn: (data: FormData) => noteApi.update(noteId, data),
    onSuccess: note => { qc.invalidateQueries({ queryKey: QK.note(noteId) }); navigate(`/notes/${note.id}`) },
  })

  const bodyValue = useWatch({ control, name: 'body', defaultValue: '' })

  function onSubmit(data: FormData) {
    isEdit ? updateNote.mutate(data) : createNote.mutate(data)
  }

  return (
    <AppLayout>
      <div className="max-w-2xl mx-auto">
        <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100 mb-6">{isEdit ? 'Edit note' : 'New note'}</h1>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <input
              {...register('title')}
              placeholder="Title"
              className="w-full text-xl font-semibold rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-4 py-2 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            {errors.title && <p className="mt-1 text-xs text-red-600 dark:text-red-400">{errors.title.message}</p>}
          </div>

          <div>
            <select {...register('label')} className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm">
              {NOTE_LABELS.map(l => (
                <option key={l} value={l}>{l.replace('_', ' ')}</option>
              ))}
            </select>
          </div>

          <div>
            <div className="flex border-b border-gray-300 dark:border-gray-600 mb-2">
              <button type="button" onClick={() => setTab('edit')}
                className={`px-4 py-1.5 text-sm font-medium -mb-px ${tab === 'edit' ? 'border-b-2 border-primary-600 text-primary-600 dark:text-primary-400' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'}`}>
                Edit
              </button>
              <button type="button" onClick={() => setTab('preview')}
                className={`px-4 py-1.5 text-sm font-medium -mb-px ${tab === 'preview' ? 'border-b-2 border-primary-600 text-primary-600 dark:text-primary-400' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'}`}>
                Preview
              </button>
            </div>
            {tab === 'edit' ? (
              <textarea
                {...register('body')}
                rows={12}
                placeholder="Body (Markdown supported)"
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-4 py-3 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            ) : (
              <div className="prose prose-sm dark:prose-invert max-w-none min-h-[12rem] rounded-lg border border-gray-200 dark:border-gray-700 px-4 py-3">
                {bodyValue ? (
                  <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeSanitize]}>{bodyValue}</ReactMarkdown>
                ) : (
                  <p className="text-gray-400 dark:text-gray-500 italic">Nothing to preview</p>
                )}
              </div>
            )}
          </div>

          <div className="flex items-center gap-4">
            <label className="text-sm text-gray-700 dark:text-gray-300">Folder:</label>
            <select {...register('folderId', { valueAsNumber: true })}
              className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 text-sm">
              <option value="">Root (no folder)</option>
              {folders.map(f => (
                <option key={f.id} value={f.id}>{f.name}</option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-2">
            <input type="checkbox" {...register('isChildSafe')} id="childSafe"
              className="w-4 h-4 text-primary-600 rounded" />
            <label htmlFor="childSafe" className="text-sm text-gray-700 dark:text-gray-300">Child safe</label>
          </div>

          <div className="flex gap-3 pt-2">
            <button type="submit" disabled={isSubmitting}
              className="px-5 py-2 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700 disabled:opacity-50">
              {isEdit ? 'Save' : 'Create'}
            </button>
            <button type="button" onClick={() => navigate(-1)}
              className="px-5 py-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 text-sm rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700">
              Cancel
            </button>
          </div>
        </form>
      </div>
    </AppLayout>
  )
}
