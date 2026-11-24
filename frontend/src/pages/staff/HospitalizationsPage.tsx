import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { doctorsApi, hospitalizationsApi, roomsApi } from '../../api/endpoints'
import type { Patient } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { PatientPicker } from '../../components/PatientPicker'
import { ActiveBadge } from '../../components/StatusBadge'
import { Button, Card, ErrorAlert, Field, Input, Loading, Modal, PageHeader, Pagination, Select, Table } from '../../components/ui'
import { formatDate, formatMoney } from '../../lib/format'

export function HospitalizationsPage() {
  const navigate = useNavigate()
  const [status, setStatus] = useState('true')
  const [roomId, setRoomId] = useState('')
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(false)
  const rooms = useQuery({ queryKey: ['rooms'], queryFn: () => roomsApi.list() })
  const query = useQuery({
    queryKey: ['hospitalizations', 'list', status, roomId, page],
    queryFn: () => hospitalizationsApi.list({ status, roomId, page, size: 15 }),
  })
  return (
    <>
      <PageHeader title="Hospitalizaciones" actions={<Button onClick={() => setCreating(true)}>Hospitalizar paciente</Button>} />
      <Card padded={false} title={
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Estado"><Select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }} className="w-36">
            <option value="true">En curso</option><option value="false">Con alta</option><option value="">Todas</option>
          </Select></Field>
          <Field label="Sala"><Select value={roomId} onChange={(e) => { setRoomId(e.target.value); setPage(0) }} className="w-48">
            <option value="">Todas</option>{rooms.data?.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
          </Select></Field>
        </div>}>
        <ErrorAlert error={query.error} className="m-4" />
        {query.isLoading ? <Loading /> : (
          <>
            <Table rows={query.data?.content ?? []} rowKey={(h) => h.id} empty="Sin hospitalizaciones" onRowClick={(h) => navigate(`/hospitalizaciones/${h.id}`)}
              columns={[
                { header: 'Paciente', cell: (h) => <span className="font-medium text-brand-700">{h.patientName}</span> },
                { header: 'Sala', cell: (h) => h.roomName },
                { header: 'Doctor', cell: (h) => h.doctorName },
                { header: 'Ingreso', cell: (h) => formatDate(h.admissionDate) },
                { header: 'Motivo', cell: (h) => h.reason },
                { header: 'Costo', cell: (h) => h.totalCost != null ? formatMoney(h.totalCost) : `${formatMoney(h.costPerDay)}/día` },
                { header: 'Estado', cell: (h) => <ActiveBadge active={h.active} on="En curso" off="Alta" /> },
              ]} />
            {query.data && <Pagination page={page} totalPages={query.data.page.totalPages} totalElements={query.data.page.totalElements} onChange={setPage} />}
          </>
        )}
      </Card>
      {creating && <CreateHospitalizationModal onClose={() => setCreating(false)} onCreated={(id) => navigate(`/hospitalizaciones/${id}`)} />}
    </>
  )
}

function CreateHospitalizationModal({ onClose, onCreated }: { onClose: () => void; onCreated: (id: number) => void }) {
  const { account, hasRole } = useAuth()
  const queryClient = useQueryClient()
  const [patient, setPatient] = useState<Patient | null>(null)
  const [form, setForm] = useState({ roomId: '', doctorId: hasRole('DOCTOR') ? String(account?.doctorId ?? '') : '', reason: '', costPerDay: '1500' })
  const rooms = useQuery({ queryKey: ['rooms', 'available'], queryFn: () => roomsApi.list(true) })
  const doctors = useQuery({ queryKey: ['doctors', 'active'], queryFn: () => doctorsApi.list({ status: true, size: 100 }) })
  const create = useMutation({
    mutationFn: () => hospitalizationsApi.create({
      patientId: patient!.id, roomId: Number(form.roomId), doctorId: form.doctorId ? Number(form.doctorId) : undefined,
      reason: form.reason, costPerDay: Number(form.costPerDay),
    }),
    onSuccess: (h) => {
      queryClient.invalidateQueries({ queryKey: ['hospitalizations'] })
      queryClient.invalidateQueries({ queryKey: ['rooms'] })
      onCreated(h.id)
    },
  })
  const ready = patient && form.roomId && form.reason.trim() && Number(form.costPerDay) > 0 && form.doctorId
  return (
    <Modal open title="Hospitalizar paciente" onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Volver</Button><Button disabled={!ready} loading={create.isPending} onClick={() => create.mutate()}>Hospitalizar</Button></>}>
      <div className="space-y-4">
        <Field label="Paciente"><PatientPicker value={patient} onChange={setPatient} /></Field>
        <Field label="Sala (solo con camas libres)">
          <Select value={form.roomId} onChange={(e) => setForm({ ...form, roomId: e.target.value })}>
            <option value="">{rooms.data?.length === 0 ? 'No hay salas con camas libres' : 'Selecciona…'}</option>
            {rooms.data?.map((r) => <option key={r.id} value={r.id}>{r.name} · {r.capacity - r.occupiedBeds} cama(s) libre(s)</option>)}
          </Select>
        </Field>
        <Field label="Doctor responsable">
          <Select value={form.doctorId} onChange={(e) => setForm({ ...form, doctorId: e.target.value })}>
            <option value="">Selecciona…</option>
            {doctors.data?.content.map((d) => <option key={d.id} value={d.id}>{d.fullName} · {d.specialty}</option>)}
          </Select>
        </Field>
        <Field label="Motivo"><Input value={form.reason} maxLength={255} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></Field>
        <Field label="Costo por día (MXN)"><Input type="number" min="1" step="0.01" value={form.costPerDay} onChange={(e) => setForm({ ...form, costPerDay: e.target.value })} /></Field>
        <ErrorAlert error={create.error} />
      </div>
    </Modal>
  )
}
