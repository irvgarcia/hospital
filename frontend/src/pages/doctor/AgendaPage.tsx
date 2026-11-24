import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { appointmentsApi } from '../../api/endpoints'
import { Card, ErrorAlert, Field, Input, Loading, PageHeader, Pagination, Select } from '../../components/ui'
import { todayIso } from '../../lib/format'
import { DoctorAppointmentsTable } from '../HomePage'

export function AgendaPage() {
  const [status, setStatus] = useState('PENDING')
  const [from, setFrom] = useState(todayIso(-30))
  const [to, setTo] = useState(todayIso(30))
  const [page, setPage] = useState(0)
  const query = useQuery({
    queryKey: ['appointments', 'me', 'agenda', status, from, to, page],
    queryFn: () => appointmentsApi.mine({ status, dateFrom: from, dateTo: to, page, size: 15, sort: 'date,asc' }),
  })
  return (
    <>
      <PageHeader title="Mi agenda" subtitle="Completa tus consultas y registra el historial clínico" />
      <Card padded={false} title={
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Estado"><Select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }} className="w-40">
            <option value="">Todas</option><option value="PENDING">Pendientes</option><option value="COMPLETED">Completadas</option><option value="CANCELLED">Canceladas</option>
          </Select></Field>
          <Field label="Desde"><Input type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0) }} /></Field>
          <Field label="Hasta"><Input type="date" value={to} onChange={(e) => { setTo(e.target.value); setPage(0) }} /></Field>
        </div>}>
        <ErrorAlert error={query.error} className="m-4" />
        {query.isLoading ? <Loading /> : (
          <>
            <DoctorAppointmentsTable rows={query.data?.content ?? []} empty="Sin consultas en ese periodo" />
            {query.data && <Pagination page={page} totalPages={query.data.page.totalPages} totalElements={query.data.page.totalElements} onChange={setPage} />}
          </>
        )}
      </Card>
    </>
  )
}
