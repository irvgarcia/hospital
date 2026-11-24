import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { receptionistsApi } from '../../api/endpoints'
import type { Receptionist } from '../../api/types'
import { ActiveBadge } from '../../components/StatusBadge'
import { Button, Card, ConfirmModal, ErrorAlert, Field, Input, Loading, Modal, PageHeader, Table } from '../../components/ui'

export function ReceptionistsPage() {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<Receptionist | 'new' | null>(null)
  const [toggling, setToggling] = useState<Receptionist | null>(null)
  const query = useQuery({ queryKey: ['receptionists'], queryFn: receptionistsApi.list })
  const toggle = useMutation({
    mutationFn: (r: Receptionist) => r.active ? receptionistsApi.deactivate(r.id) : receptionistsApi.activate(r.id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['receptionists'] }); setToggling(null) },
  })
  return (
    <>
      <PageHeader title="Recepcionistas" actions={<Button onClick={() => setEditing('new')}>Nuevo recepcionista</Button>} />
      <Card padded={false}>
        <ErrorAlert error={query.error} className="m-4" />
        {query.isLoading ? <Loading /> : (
          <Table rows={query.data ?? []} rowKey={(r) => r.id} empty="Sin recepcionistas"
            columns={[
              { header: 'Nombre', cell: (r) => <span className="font-medium">{r.firstName} {r.lastName}</span> },
              { header: 'Email', cell: (r) => r.email },
              { header: 'Estado', cell: (r) => <ActiveBadge active={r.active} /> },
              {
                header: '', className: 'text-right', cell: (r) => (
                  <div className="flex justify-end gap-1">
                    <Button size="sm" variant="ghost" onClick={() => setEditing(r)}>Editar</Button>
                    <Button size="sm" variant="ghost" className={r.active ? 'text-red-600' : ''} onClick={() => setToggling(r)}>{r.active ? 'Desactivar' : 'Activar'}</Button>
                  </div>
                ),
              },
            ]} />
        )}
      </Card>
      {editing && <ReceptionistModal receptionist={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
      <ConfirmModal open={!!toggling} danger={toggling?.active} title={toggling?.active ? 'Desactivar recepcionista' : 'Activar recepcionista'}
        message={toggling?.active ? 'Se cerrarán sus sesiones y no podrá iniciar sesión.' : 'Podrá volver a iniciar sesión.'}
        confirmText={toggling?.active ? 'Desactivar' : 'Activar'} loading={toggle.isPending} error={toggle.error}
        onConfirm={() => toggling && toggle.mutate(toggling)} onClose={() => setToggling(null)} />
    </>
  )
}

function ReceptionistModal({ receptionist, onClose }: { receptionist: Receptionist | null; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState({ firstName: receptionist?.firstName ?? '', lastName: receptionist?.lastName ?? '', email: receptionist?.email ?? '', password: '' })
  const save = useMutation({
    mutationFn: () => {
      const body: Record<string, unknown> = { ...form }
      if (receptionist && !form.password) delete body.password
      return receptionist ? receptionistsApi.update(receptionist.id, body) : receptionistsApi.create(body)
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['receptionists'] }); onClose() },
  })
  return (
    <Modal open title={receptionist ? 'Editar recepcionista' : 'Nuevo recepcionista'} onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Volver</Button><Button loading={save.isPending} onClick={() => save.mutate()}>Guardar</Button></>}>
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Field label="Nombre"><Input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} /></Field>
          <Field label="Apellido"><Input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} /></Field>
        </div>
        <Field label="Email"><Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
        <Field label={receptionist ? 'Nueva contraseña (opcional)' : 'Contraseña'} hint="Al menos 8 caracteres">
          <Input type="text" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </Field>
        <ErrorAlert error={save.error} />
      </div>
    </Modal>
  )
}
