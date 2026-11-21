import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { recordsApi } from '../api/endpoints'
import type { MedicalRecord } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { formatDate } from '../lib/format'
import { Button, EmptyState, ErrorAlert, Field, Modal, Textarea } from './ui'

/** Lista de entradas del historial. El doctor autor puede editar las suyas. */
export function RecordsList({ records }: { records: MedicalRecord[] }) {
  const { account } = useAuth()
  const [editing, setEditing] = useState<MedicalRecord | null>(null)
  if (records.length === 0) return <EmptyState title="Sin entradas en el historial" description="Las entradas se registran al completar una cita." />
  return (
    <>
      <ol className="divide-y divide-slate-100">
        {records.map((record) => (
          <li key={record.id} className="py-4 first:pt-0">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <div className="text-sm font-semibold text-slate-800">{formatDate(record.date)}</div>
              <div className="flex items-center gap-2 text-xs text-slate-500">
                {record.doctorName ? `${record.doctorName}${record.specialty ? ` · ${record.specialty}` : ''}` : 'Registro anterior'}
                {account?.doctorId != null && record.doctorId === account.doctorId && (
                  <Button size="sm" variant="ghost" onClick={() => setEditing(record)}>Editar</Button>
                )}
              </div>
            </div>
            <dl className="mt-2 grid gap-2 text-sm sm:grid-cols-2">
              <div><dt className="text-xs font-medium uppercase text-slate-500">Diagnóstico</dt><dd className="whitespace-pre-line">{record.diagnostics}</dd></div>
              <div><dt className="text-xs font-medium uppercase text-slate-500">Tratamiento</dt><dd className="whitespace-pre-line">{record.treatment}</dd></div>
              {record.notes && <div className="sm:col-span-2"><dt className="text-xs font-medium uppercase text-slate-500">Notas</dt><dd className="whitespace-pre-line">{record.notes}</dd></div>}
            </dl>
          </li>
        ))}
      </ol>
      {editing && <EditRecordModal record={editing} onClose={() => setEditing(null)} />}
    </>
  )
}

function EditRecordModal({ record, onClose }: { record: MedicalRecord; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState({ diagnostics: record.diagnostics, treatment: record.treatment, notes: record.notes ?? '' })
  const save = useMutation({
    mutationFn: () => recordsApi.update(record.id, form),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['records'] }); onClose() },
  })
  return (
    <Modal open title="Editar entrada del historial" onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Volver</Button><Button loading={save.isPending} onClick={() => save.mutate()}>Guardar</Button></>}>
      <div className="space-y-4">
        <Field label="Diagnóstico"><Textarea value={form.diagnostics} onChange={(e) => setForm({ ...form, diagnostics: e.target.value })} /></Field>
        <Field label="Tratamiento"><Textarea value={form.treatment} onChange={(e) => setForm({ ...form, treatment: e.target.value })} /></Field>
        <Field label="Notas"><Textarea value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></Field>
        <ErrorAlert error={save.error} />
      </div>
    </Modal>
  )
}
