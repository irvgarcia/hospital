import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { roomsApi } from '../../api/endpoints'
import type { Room } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Badge, Button, Card, ErrorAlert, Field, Input, Loading, Modal, PageHeader, Table } from '../../components/ui'

export function RoomsPage() {
  const { hasRole } = useAuth()
  const [editing, setEditing] = useState<Room | 'new' | null>(null)
  const query = useQuery({ queryKey: ['rooms'], queryFn: () => roomsApi.list() })
  return (
    <>
      <PageHeader title="Salas" subtitle="Ocupación de camas" actions={hasRole('ADMIN') && <Button onClick={() => setEditing('new')}>Nueva sala</Button>} />
      <Card padded={false}>
        <ErrorAlert error={query.error} className="m-4" />
        {query.isLoading ? <Loading /> : (
          <Table rows={query.data ?? []} rowKey={(r) => r.id} empty="Sin salas"
            columns={[
              { header: 'Sala', cell: (r) => <span className="font-medium">{r.name}</span> },
              {
                header: 'Ocupación', cell: (r) => (
                  <div className="flex items-center gap-3">
                    <div className="h-2 w-32 overflow-hidden rounded-full bg-slate-100">
                      <div className="h-full bg-brand-600" style={{ width: `${Math.min(100, (r.occupiedBeds / r.capacity) * 100)}%` }} />
                    </div>
                    <span className="text-xs text-slate-600">{r.occupiedBeds} / {r.capacity}</span>
                  </div>
                ),
              },
              { header: 'Disponibilidad', cell: (r) => r.available ? <Badge color="green">{r.capacity - r.occupiedBeds} libre(s)</Badge> : <Badge color="red">Llena</Badge> },
              { header: '', className: 'text-right', cell: (r) => hasRole('ADMIN') && <Button size="sm" variant="ghost" onClick={() => setEditing(r)}>Editar</Button> },
            ]} />
        )}
      </Card>
      {editing && <RoomModal room={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
    </>
  )
}

function RoomModal({ room, onClose }: { room: Room | null; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState({ name: room?.name ?? '', capacity: String(room?.capacity ?? 1) })
  const save = useMutation({
    mutationFn: () => {
      const body = { name: form.name, capacity: Number(form.capacity) }
      return room ? roomsApi.update(room.id, body) : roomsApi.create(body)
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['rooms'] }); onClose() },
  })
  return (
    <Modal open title={room ? 'Editar sala' : 'Nueva sala'} onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Volver</Button><Button loading={save.isPending} onClick={() => save.mutate()}>Guardar</Button></>}>
      <div className="space-y-4">
        <Field label="Nombre"><Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
        <Field label="Capacidad (camas)"><Input type="number" min="1" max="50" value={form.capacity} onChange={(e) => setForm({ ...form, capacity: e.target.value })} /></Field>
        <ErrorAlert error={save.error} />
      </div>
    </Modal>
  )
}
