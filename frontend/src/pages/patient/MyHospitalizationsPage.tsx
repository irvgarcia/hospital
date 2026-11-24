import { useQuery } from '@tanstack/react-query'
import { hospitalizationsApi } from '../../api/endpoints'
import { ActiveBadge } from '../../components/StatusBadge'
import { Card, ErrorAlert, Loading, PageHeader, Table } from '../../components/ui'
import { formatDate, formatMoney } from '../../lib/format'

export function MyHospitalizationsPage() {
  const query = useQuery({ queryKey: ['hospitalizations', 'me'], queryFn: hospitalizationsApi.mine })
  return (
    <>
      <PageHeader title="Mis hospitalizaciones" />
      <Card padded={false}>
        <ErrorAlert error={query.error} className="m-4" />
        {query.isLoading ? <Loading /> : (
          <Table rows={query.data ?? []} rowKey={(h) => h.id} empty="No tienes hospitalizaciones registradas"
            columns={[
              { header: 'Ingreso', cell: (h) => formatDate(h.admissionDate) },
              { header: 'Alta', cell: (h) => h.dischargeDate ? formatDate(h.dischargeDate) : '—' },
              { header: 'Sala', cell: (h) => h.roomName },
              { header: 'Motivo', cell: (h) => h.reason },
              { header: 'Doctor', cell: (h) => h.doctorName },
              { header: 'Costo', cell: (h) => h.totalCost != null ? formatMoney(h.totalCost) : `${formatMoney(h.costPerDay)} / día` },
              { header: 'Estado', cell: (h) => <ActiveBadge active={h.active} on="En curso" off="Alta" /> },
            ]} />
        )}
      </Card>
    </>
  )
}
