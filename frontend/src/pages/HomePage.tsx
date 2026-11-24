import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { appointmentsApi, dashboardApi, hospitalizationsApi, suppliesApi } from '../api/endpoints'
import type { Appointment } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { CompleteAppointmentButton, MedicalRecordButton } from '../components/AppointmentActions'
import { StatusBadge } from '../components/StatusBadge'
import { Badge, Button, Card, EmptyState, Loading, PageHeader, Stat, Table } from '../components/ui'
import { formatDateLong, formatTime, todayIso, UNIT_LABEL } from '../lib/format'

export function HomePage() {
  const { hasRole } = useAuth()
  if (hasRole('ADMIN', 'RECEPTIONIST')) return <StaffHome />
  if (hasRole('DOCTOR')) return <DoctorHome />
  return <PatientHome />
}

function StaffHome() {
  const { account, hasRole } = useAuth()
  const dashboard = useQuery({ queryKey: ['dashboard'], queryFn: dashboardApi.get })
  const today = useQuery({
    queryKey: ['appointments', 'today'],
    queryFn: () => appointmentsApi.list({ date: todayIso(), size: 50, sort: 'startTime,asc' }),
  })
  const lowStock = useQuery({ queryKey: ['supplies', 'low'], queryFn: suppliesApi.lowStock, enabled: hasRole('ADMIN') })
  const d = dashboard.data

  return (
    <>
      <PageHeader title={`Hola, ${account?.firstName}`} subtitle={formatDateLong(todayIso())}
        actions={<Link to="/citas"><Button>Agendar cita</Button></Link>} />
      {dashboard.isLoading ? <Loading /> : d && (
        <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
          <Stat label="Citas de hoy" value={d.appointmentsToday} hint={`${d.pendingAppointments} pendientes en adelante`} />
          <Stat label="Hospitalizados" value={d.activeHospitalizations} hint={`${d.occupiedBeds} de ${d.totalBeds} camas ocupadas`} />
          <Stat label="Pacientes activos" value={d.activePatients} hint={`${d.activeDoctors} doctores activos`} />
          <Stat label="Insumos con stock bajo" value={d.lowStockSupplies} tone={d.lowStockSupplies > 0 ? 'warn' : 'default'} />
        </div>
      )}
      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Citas de hoy" className="lg:col-span-2" padded={false}>
          {today.isLoading ? <Loading /> : (
            <Table rows={today.data?.content ?? []} rowKey={(a) => a.id} empty="No hay citas para hoy"
              columns={[
                { header: 'Hora', cell: (a) => `${formatTime(a.startTime)}–${formatTime(a.endTime)}` },
                { header: 'Paciente', cell: (a) => <Link className="font-medium text-brand-700 hover:underline" to={`/pacientes/${a.patientId}`}>{a.patientName}</Link> },
                { header: 'Doctor', cell: (a) => <>{a.doctorName}<div className="text-xs text-slate-500">{a.specialty}</div></> },
                { header: 'Estado', cell: (a) => <StatusBadge status={a.status} /> },
              ]} />
          )}
        </Card>
        {hasRole('ADMIN') ? (
          <Card title="Stock bajo" actions={<Link to="/inventario" className="text-sm text-brand-700 hover:underline">Inventario</Link>}>
            {lowStock.data && lowStock.data.length > 0 ? (
              <ul className="space-y-2 text-sm">
                {lowStock.data.map((s) => (
                  <li key={s.id} className="flex items-center justify-between">
                    <span>{s.name}</span>
                    <Badge color={s.quantity === 0 ? 'red' : 'yellow'}>{s.quantity} {UNIT_LABEL[s.unit]} / mín. {s.minimumStock}</Badge>
                  </li>
                ))}
              </ul>
            ) : <EmptyState title="Todo el inventario está sobre el mínimo" />}
          </Card>
        ) : (
          <Card title="Accesos rápidos">
            <div className="flex flex-col gap-2 text-sm">
              <Link className="text-brand-700 hover:underline" to="/pacientes">Registrar o buscar paciente</Link>
              <Link className="text-brand-700 hover:underline" to="/hospitalizaciones">Hospitalizaciones</Link>
              <Link className="text-brand-700 hover:underline" to="/salas">Disponibilidad de salas</Link>
            </div>
          </Card>
        )}
      </div>
    </>
  )
}

function DoctorHome() {
  const { account } = useAuth()
  const today = useQuery({
    queryKey: ['appointments', 'me', 'today'],
    queryFn: () => appointmentsApi.mine({ date: todayIso(), size: 50, sort: 'startTime,asc' }),
  })
  const hospitalized = useQuery({
    queryKey: ['hospitalizations', 'doctor', account?.doctorId],
    queryFn: () => hospitalizationsApi.list({ doctorId: account?.doctorId, status: true, size: 20 }),
    enabled: !!account?.doctorId,
  })
  return (
    <>
      <PageHeader title={`Hola, Dr(a). ${account?.lastName}`} subtitle={formatDateLong(todayIso())}
        actions={<Link to="/agenda"><Button variant="secondary">Ver agenda completa</Button></Link>} />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Consultas de hoy" className="lg:col-span-2" padded={false}>
          {today.isLoading ? <Loading /> : <DoctorAppointmentsTable rows={today.data?.content ?? []} empty="No tienes consultas hoy" />}
        </Card>
        <Card title="Pacientes hospitalizados a tu cargo">
          {hospitalized.data && hospitalized.data.content.length > 0 ? (
            <ul className="space-y-3 text-sm">
              {hospitalized.data.content.map((h) => (
                <li key={h.id}>
                  <Link to={`/hospitalizaciones/${h.id}`} className="font-medium text-brand-700 hover:underline">{h.patientName}</Link>
                  <div className="text-xs text-slate-500">{h.roomName} · {h.reason}</div>
                </li>
              ))}
            </ul>
          ) : <EmptyState title="Sin pacientes hospitalizados" />}
        </Card>
      </div>
    </>
  )
}

export function DoctorAppointmentsTable({ rows, empty }: { rows: Appointment[]; empty: string }) {
  return (
    <Table rows={rows} rowKey={(a) => a.id} empty={empty}
      columns={[
        { header: 'Fecha', cell: (a) => <>{a.date}<div className="text-xs text-slate-500">{formatTime(a.startTime)}–{formatTime(a.endTime)}</div></> },
        { header: 'Paciente', cell: (a) => <><Link className="font-medium text-brand-700 hover:underline" to={`/pacientes/${a.patientId}`}>{a.patientName}</Link>{a.reason && <div className="text-xs text-slate-500">{a.reason}</div>}</> },
        { header: 'Estado', cell: (a) => <StatusBadge status={a.status} /> },
        {
          header: '', className: 'text-right', cell: (a) => (
            <div className="flex justify-end gap-2">
              {a.status === 'PENDING' && <CompleteAppointmentButton appointment={a} />}
              {a.status === 'COMPLETED' && <MedicalRecordButton appointment={a} />}
            </div>
          ),
        },
      ]} />
  )
}

function PatientHome() {
  const { account } = useAuth()
  const next = useQuery({ queryKey: ['appointments', 'next'], queryFn: appointmentsApi.next })
  return (
    <>
      <PageHeader title={`Hola, ${account?.firstName}`} subtitle="Bienvenido a tu portal de paciente" />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Tu próxima cita" className="lg:col-span-2">
          {next.isLoading ? <Loading /> : next.data ? (
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <div className="text-lg font-semibold capitalize">{formatDateLong(next.data.date)}</div>
                <div className="text-sm text-slate-600">{formatTime(next.data.startTime)} – {formatTime(next.data.endTime)} · {next.data.doctorName} ({next.data.specialty})</div>
                {next.data.reason && <div className="mt-1 text-sm text-slate-500">Motivo: {next.data.reason}</div>}
              </div>
              <Link to="/mis-citas"><Button variant="secondary">Ver mis citas</Button></Link>
            </div>
          ) : (
            <EmptyState title="No tienes citas pendientes" action={<Link to="/agendar"><Button>Agendar una cita</Button></Link>} />
          )}
        </Card>
        <Card title="Accesos rápidos">
          <div className="flex flex-col gap-2 text-sm">
            <Link className="text-brand-700 hover:underline" to="/agendar">Agendar una cita</Link>
            <Link className="text-brand-700 hover:underline" to="/mi-historial">Ver mi historial y descargar expediente</Link>
            <Link className="text-brand-700 hover:underline" to="/mis-archivos">Subir estudios o recetas</Link>
            <Link className="text-brand-700 hover:underline" to="/mi-perfil">Actualizar mis datos</Link>
          </div>
        </Card>
      </div>
    </>
  )
}
