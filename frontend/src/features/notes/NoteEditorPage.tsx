import { useNavigate, useSearchParams, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
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

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { title: '', folderId: defaultFolderId },
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

  function onSubmit(data: FormData) {
    isEdit ? updateNote.mutate(data) : createNote.mutate(data)
  }

  return (
    <AppLayout>
      <div className="max-w-2xl mx-auto">
        <h1 className="text-xl font-bold text-gray-900 mb-6">{isEdit ? 'Edit note' : 'New note'}</h1>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <input
              {...register('title')}
              placeholder="Title"
              className="w-full text-xl font-semibold rounded-lg border border-gray-300 px-4 py-2 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            {errors.title && <p className="mt-1 text-xs text-red-600">{errors.title.message}</p>}
          </div>

          <div>
            <select {...register('label')} className="rounded-lg border border-gray-300 px-3 py-2 text-sm">
              {NOTE_LABELS.map(l => (
                <option key={l} value={l}>{l.replace('_', ' ')}</option>
              ))}
            </select>
          </div>

          <div>
            <textarea
              {...register('body')}
              rows={12}
              placeholder="Body (Markdown supported)"
              className="w-full rounded-lg border border-gray-300 px-4 py-3 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          <div className="flex items-center gap-4">
            <label className="text-sm text-gray-700">Folder:</label>
            <select {...register('folderId', { valueAsNumber: true })}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm">
              <option value="">Root (no folder)</option>
              {folders.map(f => (
                <option key={f.id} value={f.id}>{f.name}</option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-2">
            <input type="checkbox" {...register('isChildSafe')} id="childSafe"
              className="w-4 h-4 text-primary-600 rounded" />
            <label htmlFor="childSafe" className="text-sm text-gray-700">Child safe</label>
          </div>

          <div className="flex gap-3 pt-2">
            <button type="submit" disabled={isSubmitting}
              className="px-5 py-2 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700 disabled:opacity-50">
              {isEdit ? 'Save' : 'Create'}
            </button>
            <button type="button" onClick={() => navigate(-1)}
              className="px-5 py-2 bg-white border border-gray-300 text-sm rounded-lg hover:bg-gray-50">
              Cancel
            </button>
          </div>
        </form>
      </div>
    </AppLayout>
  )
}
