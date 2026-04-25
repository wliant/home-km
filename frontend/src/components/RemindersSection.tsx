import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { reminderApi } from '../api'
import { QK } from '../lib/queryKeys'
import type { ReminderResponse } from '../types'

const RECURRENCES = ['none', 'daily', 'weekly', 'monthly', 'yearly'] as const

interface RemindersSectionProps {
  noteId: number
  reminders: ReminderResponse[]
  readOnly?: boolean
}

export default function RemindersSection({ noteId, reminders, readOnly = false }: RemindersSectionProps) {
  const qc = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [remindAt, setRemindAt] = useState('')
  const [recurrence, setRecurrence] = useState('none')
  const [editingId, setEditingId] = useState<number | null>(null)

  function invalidate() {
    qc.invalidateQueries({ queryKey: QK.note(noteId) })
  }

  const createReminder = useMutation({
    mutationFn: () => reminderApi.create(noteId, {
      remindAt: new Date(remindAt).toISOString(),
      recurrence: recurrence === 'none' ? undefined : recurrence,
    }),
    onSuccess: () => { invalidate(); setShowForm(false); setRemindAt(''); setRecurrence('none') },
  })

  const updateReminder = useMutation({
    mutationFn: (id: number) => reminderApi.update(noteId, id, {
      remindAt: new Date(remindAt).toISOString(),
      recurrence: recurrence === 'none' ? undefined : recurrence,
    }),
    onSuccess: () => { invalidate(); setEditingId(null); setRemindAt(''); setRecurrence('none') },
  })

  const deleteReminder = useMutation({
    mutationFn: (id: number) => reminderApi.delete(noteId, id),
    onSuccess: invalidate,
  })

  function startEdit(r: ReminderResponse) {
    setEditingId(r.id)
    const dt = new Date(r.remindAt)
    setRemindAt(toLocalDateTimeInput(dt))
    setRecurrence(r.recurrence ?? 'none')
    setShowForm(false)
  }

  function cancelForm() {
    setShowForm(false)
    setEditingId(null)
    setRemindAt('')
    setRecurrence('none')
  }

  return (
    <section>
      <div className="flex items-center justify-between mb-2">
        <h2 className="text-sm font-semibold text-gray-500">Reminders</h2>
        {!readOnly && !showForm && editingId == null && (
          <button
            type="button"
            onClick={() => setShowForm(true)}
            className="text-xs text-primary-600 hover:underline"
          >
            + Add
          </button>
        )}
      </div>

      {reminders.length === 0 && !showForm && (
        <p className="text-xs text-gray-400">No reminders.</p>
      )}

      <ul className="space-y-2 mb-3">
        {reminders.map(r => (
          <li key={r.id} className="rounded-lg border border-gray-200 px-3 py-2">
            {editingId === r.id ? (
              <ReminderForm
                remindAt={remindAt}
                recurrence={recurrence}
                onRemindAtChange={setRemindAt}
                onRecurrenceChange={setRecurrence}
                onSave={() => updateReminder.mutate(r.id)}
                onCancel={cancelForm}
                saving={updateReminder.isPending}
              />
            ) : (
              <div className="flex items-center justify-between gap-2">
                <div>
                  <p className="text-sm text-gray-800">{formatDateTime(r.remindAt)}</p>
                  {r.recurrence && (
                    <p className="text-xs text-gray-500 capitalize">{r.recurrence}</p>
                  )}
                </div>
                {!readOnly && (
                  <div className="flex gap-2 text-xs">
                    <button onClick={() => startEdit(r)} className="text-primary-600 hover:underline">Edit</button>
                    <button
                      onClick={() => { if (confirm('Delete reminder?')) deleteReminder.mutate(r.id) }}
                      className="text-red-500 hover:underline"
                    >
                      Delete
                    </button>
                  </div>
                )}
              </div>
            )}
          </li>
        ))}
      </ul>

      {showForm && (
        <ReminderForm
          remindAt={remindAt}
          recurrence={recurrence}
          onRemindAtChange={setRemindAt}
          onRecurrenceChange={setRecurrence}
          onSave={() => createReminder.mutate()}
          onCancel={cancelForm}
          saving={createReminder.isPending}
        />
      )}
    </section>
  )
}

function ReminderForm({
  remindAt, recurrence, onRemindAtChange, onRecurrenceChange, onSave, onCancel, saving,
}: {
  remindAt: string
  recurrence: string
  onRemindAtChange: (v: string) => void
  onRecurrenceChange: (v: string) => void
  onSave: () => void
  onCancel: () => void
  saving: boolean
}) {
  return (
    <div className="space-y-2 rounded-lg border border-gray-200 p-3">
      <div className="flex gap-2 flex-wrap">
        <input
          type="datetime-local"
          value={remindAt}
          onChange={e => onRemindAtChange(e.target.value)}
          className="rounded border border-gray-300 px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
        <select
          value={recurrence}
          onChange={e => onRecurrenceChange(e.target.value)}
          className="rounded border border-gray-300 px-2 py-1.5 text-sm"
        >
          {RECURRENCES.map(r => (
            <option key={r} value={r}>{r.charAt(0).toUpperCase() + r.slice(1)}</option>
          ))}
        </select>
      </div>
      <div className="flex gap-2">
        <button
          type="button"
          onClick={onSave}
          disabled={!remindAt || saving}
          className="px-3 py-1.5 text-xs bg-primary-600 text-white rounded-lg disabled:opacity-50"
        >
          Save
        </button>
        <button type="button" onClick={onCancel} className="px-3 py-1.5 text-xs border border-gray-300 rounded-lg">
          Cancel
        </button>
      </div>
    </div>
  )
}

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function toLocalDateTimeInput(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
