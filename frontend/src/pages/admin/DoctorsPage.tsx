import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { ApiError } from '../../api/client'
import { doctorsApi, specialtiesApi } from '../../api/endpoints'
import type { DayOfWeek, Doctor, ScheduleSlot } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { ActiveBadge } from '../../components/StatusBadge'
import { Button, Card, ConfirmModal, ErrorAlert, Field, Input, Loading, Modal, PageHeader, Pagination, Select, Table } from '../../components/ui'
import { DAY_LABEL, DAYS, formatMoney, formatTime } from '../../lib/format'
import { useDebounced } from '../../lib/useDebounced'

export function DoctorsPage() {
  const { hasRole } = useAuth()
  const isAdmin = hasRole('ADMIN')
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [specialtyId, setSpecialtyId] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Doctor | 'new' | null>(null)
  const [toggling, setToggling] = useState<Doctor | null>(null)
  const name = useDebounced(search)
  const specialties = useQuery({ queryKey: ['specialties', 'all'], queryFn: () => specialtiesApi.list() })
  const query = useQuery({
    queryKey: ['doctors', 'admin', name, specialtyId, status, page],
    queryFn: () => doctorsApi.list({ name, specialtyId, status, page, size: 15 }),
  })
  const toggle = useMutation({
    mutationFn: (d: Doctor) => d.active ? doctorsApi.deactivate(d.id) : doctorsApi.activate(d.id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['doctors'] }); setToggling(null) },
  })

  return (
    <>
      <PageHeader title="Doctores" actions={isAdmin && <Button onClick={() => setEditing('new')}>Nuevo doctor</Button>} />
      <Card padded={false} title={
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Buscar"><Input placeholder="Nombre o email" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0) }} className="w-56" /></Field>
          <Field label="Especialidad"><Select value={specialtyId} onChange={(e) => { setSpecialtyId(e.target.value); setPage(0) }} className="w-48">
            <option value="">Todas</option>{specialties.data?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </Select></Field>
          <Field label="Estado"><Select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }} className="w-32">
            <option value="">Todos</option><option value="true">Activos</option><option value="false">Inactivos</option>
          </Select></Field>
        </div>}>
        <ErrorAlert error={query.error} className="m-4" />
        {query.isLoading ? <Loading /> : (
          <>
            <Table rows={query.data?.content ?? []} rowKey={(d) => d.id} empty="Sin doctores"
              columns={[
                { header: 'Doctor', cell: (d) => <><span className="font-medium">{d.fullName}</span><div className="text-xs text-slate-500">{d.email} · {d.phoneNumber}</div></> },
                { header: 'Especialidad', cell: (d) => <>{d.specialty}<div className="text-xs text-slate-500">{formatMoney(d.consultationCost)}</div></> },
                { header: 'Horario', cell: (d) => <div className="text-xs text-slate-600">{d.schedules.map((s) => <div key={s.id}>{DAY_LABEL[s.dayOfWeek]} {formatTime(s.startTime)}–{formatTime(s.endTime)}</div>)}</div> },
                { header: 'Estado', cell: (d) => <ActiveBadge active={d.active} /> },
                {
                  header: '', className: 'text-right', cell: (d) => isAdmin && (
                    <div className="flex justify-end gap-1">
                      <Button size="sm" variant="ghost" onClick={() => setEditing(d)}>Editar</Button>
                      <Button size="sm" variant="ghost" className={d.active ? 'text-red-600' : ''} onClick={() => setToggling(d)}>{d.active ? 'Desactivar' : 'Activar'}</Button>
                    </div>
                  ),
                },
              ]} />
            {query.data && <Pagination page={page} totalPages={query.data.page.totalPages} totalElements={query.data.page.totalElements} onChange={setPage} />}
          </>
        )}
      </Card>
      {editing && <DoctorModal doctor={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
      <ConfirmModal open={!!toggling} danger={toggling?.active} title={toggling?.active ? 'Desactivar doctor' : 'Activar doctor'}
        message={toggling?.active ? `${toggling.fullName} no podrá iniciar sesión ni recibir citas. Se rechaza si tiene citas pendientes u hospitalizaciones a su cargo.` : `${toggling?.fullName} podrá volver a iniciar sesión y recibir citas.`}
        confirmText={toggling?.active ? 'Desactivar' : 'Activar'} loading={toggle.isPending} error={toggle.error}
        onConfirm={() => toggling && toggle.mutate(toggling)} onClose={() => { setToggling(null); toggle.reset() }} />
    </>
  )
}

function DoctorModal({ doctor, onClose }: { doctor: Doctor | null; onClose: () => void }) {
  const queryClient = useQueryClient()
  const specialties = useQuery({ queryKey: ['specialties', 'active'], queryFn: () => specialtiesApi.list(true) })
  const [form, setForm] = useState({
    firstName: doctor?.firstName ?? '', lastName: doctor?.lastName ?? '', email: doctor?.email ?? '',
    password: '', phoneNumber: doctor?.phoneNumber ?? '', specialtyId: doctor ? String(doctor.specialtyId) : '',
  })
  const [schedules, setSchedules] = useState<ScheduleSlot[]>(
    doctor?.schedules.map((s) => ({ dayOfWeek: s.dayOfWeek, startTime: formatTime(s.startTime), endTime: formatTime(s.endTime) }))
    ?? ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'].map((d) => ({ dayOfWeek: d as DayOfWeek, startTime: '08:00', endTime: '14:00' })))
  const save = useMutation({
    mutationFn: () => {
      const body: Record<string, unknown> = { ...form, specialtyId: Number(form.specialtyId), schedules }
      if (doctor) {
        if (!form.password) delete body.password
        else { body.newPassword = form.password; delete body.password }
        return doctorsApi.update(doctor.id, body)
      }
      return doctorsApi.create(body)
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['doctors'] }); onClose() },
  })
  const errors = save.error instanceof ApiError ? save.error.fieldErrors() : {}
  const toggleDay = (day: DayOfWeek) => setSchedules((list) => list.some((s) => s.dayOfWeek === day)
    ? list.filter((s) => s.dayOfWeek !== day)
    : [...list, { dayOfWeek: day, startTime: '08:00', endTime: '14:00' }].sort((a, b) => DAYS.indexOf(a.dayOfWeek) - DAYS.indexOf(b.dayOfWeek)))
  const setTime = (day: DayOfWeek, key: 'startTime' | 'endTime', value: string) =>
    setSchedules((list) => list.map((s) => s.dayOfWeek === day ? { ...s, [key]: value } : s))

  return (
    <Modal open wide title={doctor ? `Editar ${doctor.fullName}` : 'Nuevo doctor'} onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Volver</Button><Button loading={save.isPending} onClick={() => save.mutate()}>Guardar</Button></>}>
      <div className="space-y-5">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="Nombre" error={errors.firstName}><Input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} /></Field>
          <Field label="Apellido" error={errors.lastName}><Input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} /></Field>
          <Field label="Email" error={errors.email}><Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
          <Field label={doctor ? 'Nueva contraseña (opcional)' : 'Contraseña'} error={errors.password || errors.newPassword}>
            <Input type="text" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          </Field>
          <Field label="Teléfono (10 dígitos)" error={errors.phoneNumber}><Input value={form.phoneNumber} onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} /></Field>
          <Field label="Especialidad" error={errors.specialtyId}>
            <Select value={form.specialtyId} onChange={(e) => setForm({ ...form, specialtyId: e.target.value })}>
              <option value="">Selecciona…</option>
              {specialties.data?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </Select>
          </Field>
        </div>
        <div>
          <span className="label">Horario semanal (entre 06:00 y 22:00, mínimo 1 hora)</span>
          <div className="space-y-2">
            {DAYS.map((day) => {
              const slot = schedules.find((s) => s.dayOfWeek === day)
              return (
                <div key={day} className="flex flex-wrap items-center gap-3">
                  <label className="flex w-32 items-center gap-2 text-sm">
                    <input type="checkbox" className="h-4 w-4 rounded border-slate-300" checked={!!slot} onChange={() => toggleDay(day)} />
                    {DAY_LABEL[day]}
                  </label>
                  {slot && <>
                    <Input type="time" className="w-32" value={slot.startTime} onChange={(e) => setTime(day, 'startTime', e.target.value)} />
                    <span className="text-sm text-slate-400">a</span>
                    <Input type="time" className="w-32" value={slot.endTime} onChange={(e) => setTime(day, 'endTime', e.target.value)} />
                  </>}
                </div>
              )
            })}
          </div>
        </div>
        <ErrorAlert error={save.error} />
      </div>
    </Modal>
  )
}
