import { useQuery } from '@tanstack/react-query'
import { doctorsApi } from '../../api/endpoints'
import { ActiveBadge } from '../../components/StatusBadge'
import { Card, DescriptionList, ErrorAlert, Loading, PageHeader, Table } from '../../components/ui'
import { DAY_LABEL, formatDate, formatMoney, formatTime } from '../../lib/format'

export function DoctorProfilePage() {
  const query = useQuery({ queryKey: ['doctors', 'me'], queryFn: doctorsApi.me })
  const d = query.data
  return (
    <>
      <PageHeader title="Mi perfil" subtitle="Los cambios de horario o especialidad los hace un administrador" />
      <ErrorAlert error={query.error} />
      {query.isLoading ? <Loading /> : d && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card title="Datos">
            <DescriptionList items={[
              { label: 'Nombre', value: d.fullName },
              { label: 'Email', value: d.email },
              { label: 'Especialidad', value: d.specialty },
              { label: 'Costo de consulta', value: formatMoney(d.consultationCost) },
              { label: 'Teléfono', value: d.phoneNumber },
              { label: 'Alta', value: formatDate(d.registerDate) },
              { label: 'Estado', value: <ActiveBadge active={d.active} /> },
            ]} />
          </Card>
          <Card title="Horario semanal" padded={false}>
            <Table rows={d.schedules} rowKey={(s) => s.id} empty="Sin horario"
              columns={[
                { header: 'Día', cell: (s) => DAY_LABEL[s.dayOfWeek] },
                { header: 'Horario', cell: (s) => `${formatTime(s.startTime)} – ${formatTime(s.endTime)}` },
              ]} />
          </Card>
        </div>
      )}
    </>
  )
}
