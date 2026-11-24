import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { appointmentsApi, doctorsApi } from '../../api/endpoints'
import { CancelAppointmentButton, RescheduleButton } from '../../components/AppointmentActions'
import { BookingForm } from '../../components/BookingForm'
import { StatusBadge } from '../../components/StatusBadge'
import { Button, Card, ErrorAlert, Field, Input, Loading, Modal, PageHeader, Pagination, Select, SuccessAlert, Table } from '../../components/ui'
import { formatDate, formatMoney, formatTime, todayIso } from '../../lib/format'

export function AppointmentsPage() {
  const [filters, setFilters] = useState({ status: '', date: '', doctorId: '' })
  const [page, setPage] = useState(0)
  const [booking, setBooking] = useState(false)
  const [booked, setBooked] = useState<string | null>(null)
  const doctors = useQuery({ queryKey: ['doctors', 'all'], queryFn: () => doctorsApi.list({ size: 100 }) })
  const query = useQuery({
    queryKey: ['appointments', 'staff', filters, page],
    queryFn: () => appointmentsApi.list({ ...filters, page, size: 15 }),
  })
  const set = (key: keyof typeof filters, value: string) => { setFilters({ ...filters, [key]: value }); setPage(0) }

  return (
    <>
      <PageHeader title="Citas" subtitle="Agenda, reprograma o cancela citas de cualquier paciente"
        actions={<Button onClick={() => { setBooking(true); setBooked(null) }}>Nueva cita</Button>} />
      {booked && <div className="mb-4"><SuccessAlert>{booked}</SuccessAlert></div>}
      <Card padded={false} title={
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Estado"><Select value={filters.status} onChange={(e) => set('status', e.target.value)} className="w-36">
            <option value="">Todos</option><option value="PENDING">Pendientes</option><option value="COMPLETED">Completadas</option><option value="CANCELLED">Canceladas</option>
          </Select></Field>
          <Field label="Fecha"><Input type="date" value={filters.date} onChange={(e) => set('date', e.target.value)} /></Field>
          <Field label="Doctor"><Select value={filters.doctorId} onChange={(e) => set('doctorId', e.target.value)} className="w-52">
            <option value="">Todos</option>
            {doctors.data?.content.map((d) => <option key={d.id} value={d.id}>{d.fullName}</option>)}
          </Select></Field>
          <Button variant="ghost" size="sm" onClick={() => set('date', todayIso())}>Hoy</Button>
        </div>}>
        <ErrorAlert error={query.error} className="m-4" />
        {query.isLoading ? <Loading /> : (
          <>
            <Table rows={query.data?.content ?? []} rowKey={(a) => a.id} empty="Sin citas con esos filtros"
              columns={[
                { header: 'Fecha', cell: (a) => <>{formatDate(a.date)}<div className="text-xs text-slate-500">{formatTime(a.startTime)}–{formatTime(a.endTime)}</div></> },
                { header: 'Paciente', cell: (a) => <Link to={`/pacientes/${a.patientId}`} className="font-medium text-brand-700 hover:underline">{a.patientName}</Link> },
                { header: 'Doctor', cell: (a) => <>{a.doctorName}<div className="text-xs text-slate-500">{a.specialty}</div></> },
                { header: 'Costo', cell: (a) => formatMoney(a.total) },
                { header: 'Estado', cell: (a) => <StatusBadge status={a.status} /> },
                {
                  header: '', className: 'text-right', cell: (a) => a.status === 'PENDING' && a.date >= todayIso() ? (
                    <div className="flex justify-end gap-1"><RescheduleButton appointment={a} /><CancelAppointmentButton appointment={a} /></div>
                  ) : null,
                },
              ]} />
            {query.data && <Pagination page={page} totalPages={query.data.page.totalPages} totalElements={query.data.page.totalElements} onChange={setPage} />}
          </>
        )}
      </Card>
      <Modal open={booking} wide title="Nueva cita" onClose={() => setBooking(false)}>
        <BookingForm forPatient onBooked={(a) => {
          setBooking(false)
          setBooked(`Cita agendada: ${a.patientName} con ${a.doctorName} el ${formatDate(a.date)} a las ${formatTime(a.startTime)}.`)
        }} />
      </Modal>
    </>
  )
}
