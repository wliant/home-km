import { useNavigate, useSearchParams, useParams } from 'react-router-dom'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeSanitize from 'rehype-sanitize'
import { noteApi, folderApi, fileApi } from '../../api'
import { QK } from '../../lib/queryKeys'
import AppLayout from '../../components/AppLayout'
import { NOTE_LABELS } from '../../types'
import { updateNoteOfflineAware } from '../../lib/noteEditQueue'
import { toast } from '../../lib/toastStore'

const schema = z.object({
  title: z.string().min(1, 'Title is required').max(500),
  body: z.string().max(100_000).optional(),
  label: z.string().optional(),
  folderId: z.number().optional(),
  isChildSafe: z.boolean().optional(),
  isTemplate: z.boolean().optional(),
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

  // Templates only matter on a fresh note; skip the round-trip when editing
  // an existing one. Filter on the client-side `templates` flag rather than a
  // server route so children's accounts (which can read templates but can't
  // create from them) just see an empty picker.
  const { data: templates = [] } = useQuery({
    queryKey: ['notes', 'templates'],
    queryFn: () => noteApi.listTemplates(),
    enabled: !isEdit,
    staleTime: 60_000,
  })

  const cloneFromTemplate = useMutation({
    mutationFn: (templateId: number) => noteApi.createFromTemplate(templateId),
    onSuccess: note => {
      qc.invalidateQueries({ queryKey: QK.notes() })
      navigate(`/notes/${note.id}/edit`)
    },
    onError: () => toast.error('Could not clone template'),
  })

  const [tab, setTab] = useState<'edit' | 'preview'>('edit')

  const { register, handleSubmit, control, setValue, getValues, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { title: defaultTitle, body: defaultBody || undefined, folderId: defaultFolderId },
    values: existing ? {
      title: existing.title,
      body: existing.body ?? '',
      label: existing.label,
      folderId: existing.folderId ?? undefined,
      isChildSafe: existing.isChildSafe,
      isTemplate: false,
    } : undefined,
  })

  const createNote = useMutation({
    mutationFn: (data: FormData) => noteApi.create(data),
    onSuccess: note => { qc.invalidateQueries({ queryKey: QK.notes() }); navigate(`/notes/${note.id}`) },
  })

  const updateNote = useMutation({
    mutationFn: (data: FormData) =>
      updateNoteOfflineAware(noteId, data, existing?.version ?? null),
    onSuccess: result => {
      if (result.queued) {
        toast.success('Saved offline — will sync when you reconnect')
        navigate(`/notes/${noteId}`)
        return
      }
      qc.invalidateQueries({ queryKey: QK.note(noteId) })
      navigate(`/notes/${result.note?.id ?? noteId}`)
    },
  })

  const bodyValue = useWatch({ control, name: 'body', defaultValue: '' })

  const textareaRef = useRef<HTMLTextAreaElement | null>(null)
  const bodyRegister = register('body')
  const [uploadingImage, setUploadingImage] = useState(false)

  function onSubmit(data: FormData) {
    isEdit ? updateNote.mutate(data) : createNote.mutate(data)
  }

  function insertAtCursor(snippet: string) {
    const ta = textareaRef.current
    const current = getValues('body') ?? ''
    if (!ta) {
      setValue('body', current + snippet, { shouldDirty: true, shouldValidate: true })
      return
    }
    const start = ta.selectionStart ?? current.length
    const end = ta.selectionEnd ?? current.length
    const next = current.slice(0, start) + snippet + current.slice(end)
    setValue('body', next, { shouldDirty: true, shouldValidate: true })
    // After React re-renders, restore the cursor just after the insertion.
    requestAnimationFrame(() => {
      const pos = start + snippet.length
      ta.focus()
      ta.setSelectionRange(pos, pos)
    })
  }

  async function uploadAndInsertImages(files: File[]) {
    const images = files.filter(f => f.type.startsWith('image/'))
    if (images.length === 0) return false
    setUploadingImage(true)
    try {
      for (const file of images) {
        const clientUploadId = (crypto.randomUUID?.() ?? `${Date.now()}-${Math.random()}`)
        const folderForImage = getValues('folderId') ?? defaultFolderId
        const stored = await fileApi.upload(file, folderForImage, clientUploadId)
        const alt = stored.filename.replace(/[\[\]]/g, '')
        // Pre-formed Markdown image with the presigned download URL. Users can
        // edit the alt text afterwards. The URL expires after the configured
        // PRESIGNED_URL_EXPIRY_MINUTES so on render the markdown component
        // will re-resolve via /api/files/{id}/download-url if the ![]() is
        // routed through that gateway — kept simple here to match how other
        // markdown bodies are stored across the app.
        const url = stored.downloadUrl ?? `/api/files/${stored.id}/download-url`
        insertAtCursor(`\n![${alt}](${url})\n`)
      }
      toast.success(`Inserted ${images.length} image${images.length === 1 ? '' : 's'}`)
      return true
    } catch {
      toast.error('Image upload failed')
      return false
    } finally {
      setUploadingImage(false)
    }
  }

  function handlePaste(e: React.ClipboardEvent<HTMLTextAreaElement>) {
    const items = Array.from(e.clipboardData.files ?? [])
    if (items.length === 0) return
    if (items.some(f => f.type.startsWith('image/'))) {
      e.preventDefault()
      void uploadAndInsertImages(items)
    }
  }

  function handleDrop(e: React.DragEvent<HTMLTextAreaElement>) {
    const items = Array.from(e.dataTransfer.files ?? [])
    if (items.length === 0) return
    if (items.some(f => f.type.startsWith('image/'))) {
      e.preventDefault()
      void uploadAndInsertImages(items)
    }
  }

  return (
    <AppLayout>
      <div className="max-w-2xl mx-auto">
        <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100 mb-6">{isEdit ? 'Edit note' : 'New note'}</h1>

        {!isEdit && templates.length > 0 && (
          <div className="mb-4 flex flex-wrap items-center gap-2 text-xs">
            <span className="text-gray-500 dark:text-gray-400">Start from a template:</span>
            {templates.map(t => (
              <button
                key={t.id}
                type="button"
                onClick={() => cloneFromTemplate.mutate(t.id)}
                disabled={cloneFromTemplate.isPending}
                className="px-2 py-1 rounded-full border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:border-primary-300 hover:bg-primary-50 dark:hover:bg-primary-900/30 disabled:opacity-50"
              >
                {t.title}
              </button>
            ))}
          </div>
        )}

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
            <div className="flex items-center border-b border-gray-300 dark:border-gray-600 mb-2">
              <button type="button" onClick={() => setTab('edit')}
                className={`px-4 py-1.5 text-sm font-medium -mb-px ${tab === 'edit' ? 'border-b-2 border-primary-600 text-primary-600 dark:text-primary-400' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'}`}>
                Edit
              </button>
              <button type="button" onClick={() => setTab('preview')}
                className={`px-4 py-1.5 text-sm font-medium -mb-px ${tab === 'preview' ? 'border-b-2 border-primary-600 text-primary-600 dark:text-primary-400' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'}`}>
                Preview
              </button>
              {tab === 'edit' && (
                <label className="ml-auto inline-flex items-center gap-1 px-3 py-1.5 text-xs text-primary-600 dark:text-primary-400 hover:underline cursor-pointer">
                  <input
                    type="file"
                    accept="image/*"
                    multiple
                    className="hidden"
                    onChange={e => {
                      const files = Array.from(e.target.files ?? [])
                      void uploadAndInsertImages(files)
                      e.target.value = ''
                    }}
                  />
                  {uploadingImage ? 'Uploading…' : '📎 Attach image'}
                </label>
              )}
            </div>
            {tab === 'edit' ? (
              <textarea
                {...bodyRegister}
                ref={el => {
                  bodyRegister.ref(el)
                  textareaRef.current = el
                }}
                onPaste={handlePaste}
                onDrop={handleDrop}
                rows={12}
                placeholder="Body (Markdown supported — drop or paste an image to attach)"
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

          <div className="flex items-center gap-6 flex-wrap">
            <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
              <input type="checkbox" {...register('isChildSafe')} className="w-4 h-4 text-primary-600 rounded" />
              Child safe
            </label>
            <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300" title="Templates appear in the picker on the New Note page.">
              <input type="checkbox" {...register('isTemplate')} className="w-4 h-4 text-primary-600 rounded" />
              Save as template
            </label>
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
