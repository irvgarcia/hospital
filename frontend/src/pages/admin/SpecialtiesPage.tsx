import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { specialtiesApi } from '../../api/endpoints'
import type { Specialty } from '../../api/types'
import { ActiveBadge } from '../../components/StatusBadge'
import { Button, Card, ErrorAlert, Field, Input, Loading, Modal, PageHeader, Table, Textarea } from '../../components/ui'
import { formatMoney } from '../../lib/format'

export function SpecialtiesPage() {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<Specialty | 'new' | null>(null)
  const [toggleError, setToggleError] = useState<unknown>(null)
  const query = useQuery({ queryKey: ['specialties', 'all'], queryFn: () => specialtiesApi.list() })
  const toggle = useMutation({
    mutationFn: (s: Specialty) => specialtiesApi.update(s.id, { active: !s.active }),
    onSuccess: () => { setToggleError(null); queryClient.invalidateQueries({ queryKey: ['specialties'] }) },
    onError: setToggleError,
  })
  return (
    <>
      <PageHeader title="Especialidades" actions={<Button onClick={() => setEditing('new')}>Nueva especialidad</Button>} />
      <ErrorAlert error={toggleError} className="mb-4" />
      <Card padded={false}>
        {query.isLoading ? <Loading /> : (
          <Table rows={query.data ?? []} rowKey={(s) => s.id} empty="Sin especialidades"
            columns={[
              { header: 'Especialidad', cell: (s) => <><span className="font-medium">{s.name}</span><div className="text-xs text-slate-500">{s.description}</div></> },
              { header: 'Costo de consulta', cell: (s) => formatMoney(s.cost) },
              { header: 'Doctores activos', cell: (s) => s.doctorCount },
              { header: 'Estado', cell: (s) => <ActiveBadge active={s.active} on="Activa" off="Inactiva" /> },
              {
                header: '', className: 'text-right', cell: (s) => (
                  <div className="flex justify-end gap-1">
                    <Button size="sm" variant="ghost" onClick={() => setEditing(s)}>Editar</Button>
                    <Button size="sm" variant="ghost" onClick={() => toggle.mutate(s)}>{s.active ? 'Desactivar' : 'Activar'}</Button>
                  </div>
                ),
              },
            ]} />
        )}
      </Card>
      {editing && <SpecialtyModal specialty={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
    </>
  )
}

function SpecialtyModal({ specialty, onClose }: { specialty: Specialty | null; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState({ name: specialty?.name ?? '', description: specialty?.description ?? '', cost: String(specialty?.cost ?? '') })
  const save = useMutation({
    mutationFn: () => {
      const body = { ...form, cost: Number(form.cost) }
      return specialty ? specialtiesApi.update(specialty.id, body) : specialtiesApi.create(body)
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['specialties'] }); onClose() },
  })
  return (
    <Modal open title={specialty ? 'Editar especialidad' : 'Nueva especialidad'} onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Volver</Button><Button loading={save.isPending} onClick={() => save.mutate()}>Guardar</Button></>}>
      <div className="space-y-4">
        <Field label="Nombre"><Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
        <Field label="Descripción"><Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
        <Field label="Costo de consulta (MXN)" hint="Solo afecta a las citas nuevas"><Input type="number" min="1" step="0.01" value={form.cost} onChange={(e) => setForm({ ...form, cost: e.target.value })} /></Field>
        <ErrorAlert error={save.error} />
      </div>
    </Modal>
  )
}
