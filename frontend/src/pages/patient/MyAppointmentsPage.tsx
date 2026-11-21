import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { appointmentsApi } from '../../api/endpoints'
import { CancelAppointmentButton, RescheduleButton } from '../../components/AppointmentActions'
import { StatusBadge } from '../../components/StatusBadge'
import { Button, Card, ErrorAlert, Loading, PageHeader, Pagination, Select, Table } from '../../components/ui'
import { formatDate, formatMoney, formatTime, todayIso } from '../../lib/format'

export function MyAppointmentsPage() {
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const query = useQuery({
    queryKey: ['appointments', 'me', status, page],
    queryFn: () => appointmentsApi.mine({ status, page, size: 10 }),
  })
  return (
    <>
      <PageHeader title="Mis citas" actions={<Link to="/agendar"><Button>Agendar cita</Button></Link>} />
      <Card padded={false} title="Historial de citas" actions={
        <Select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }} className="w-40">
          <option value="">Todas</option>
          <option value="PENDING">Pendientes</option>
          <option value="COMPLETED">Completadas</option>
          <option value="CANCELLED">Canceladas</option>
        </Select>}>
        <ErrorAlert error={query.error} className="m-4" />
        {query.isLoading ? <Loading /> : (
          <>
            <Table rows={query.data?.content ?? []} rowKey={(a) => a.id} empty="Aún no tienes citas"
              columns={[
                { header: 'Fecha', cell: (a) => <>{formatDate(a.date)}<div className="text-xs text-slate-500">{formatTime(a.startTime)}–{formatTime(a.endTime)}</div></> },
                { header: 'Doctor', cell: (a) => <>{a.doctorName}<div className="text-xs text-slate-500">{a.specialty}</div></> },
                { header: 'Motivo', cell: (a) => a.reason ?? '—' },
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
    </>
  )
}
