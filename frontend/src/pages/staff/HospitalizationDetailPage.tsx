import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { hospitalizationsApi, suppliesApi } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthContext'
import { ActiveBadge } from '../../components/StatusBadge'
import { Button, Card, ConfirmModal, DescriptionList, ErrorAlert, Field, Input, Loading, PageHeader, Select, Table } from '../../components/ui'
import { formatDate, formatDateTime, formatMoney, UNIT_LABEL } from '../../lib/format'

export function HospitalizationDetailPage() {
  const id = Number(useParams().id)
  const { hasRole } = useAuth()
  const queryClient = useQueryClient()
  const [discharging, setDischarging] = useState(false)
  const hospitalization = useQuery({ queryKey: ['hospitalizations', id], queryFn: () => hospitalizationsApi.get(id) })
  const usages = useQuery({ queryKey: ['hospitalizations', id, 'usages'], queryFn: () => hospitalizationsApi.usages(id) })
  const discharge = useMutation({
    mutationFn: () => hospitalizationsApi.discharge(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['hospitalizations'] }); queryClient.invalidateQueries({ queryKey: ['rooms'] }); setDischarging(false) },
  })
  if (hospitalization.isLoading) return <Loading />
  if (hospitalization.error) return <ErrorAlert error={hospitalization.error} />
  const h = hospitalization.data!
  const canUseSupplies = hasRole('ADMIN', 'DOCTOR') && h.active

  return (
    <>
      <PageHeader title={`Hospitalización de ${h.patientName}`} subtitle={`${h.roomName} · ingreso ${formatDate(h.admissionDate)}`}
        actions={h.active && <Button variant="danger" onClick={() => setDischarging(true)}>Dar de alta</Button>} />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Datos" className="lg:col-span-1">
          <DescriptionList items={[
            { label: 'Paciente', value: <Link className="text-brand-700 hover:underline" to={`/pacientes/${h.patientId}`}>{h.patientName}</Link> },
            { label: 'Doctor responsable', value: h.doctorName },
            { label: 'Sala', value: h.roomName },
            { label: 'Motivo', value: h.reason },
            { label: 'Ingreso', value: formatDate(h.admissionDate) },
            { label: 'Alta', value: formatDate(h.dischargeDate) },
            { label: 'Costo por día', value: formatMoney(h.costPerDay) },
            { label: 'Total', value: h.totalCost != null ? formatMoney(h.totalCost) : 'Se calcula al dar de alta' },
            { label: 'Estado', value: <ActiveBadge active={h.active} on="En curso" off="Alta" /> },
          ]} />
        </Card>
        <Card title="Consumo de insumos" className="lg:col-span-2" padded={false}>
          {canUseSupplies && <div className="border-b border-slate-100 p-4"><RegisterUsageForm hospitalizationId={id} /></div>}
          {usages.isLoading ? <Loading /> : (
            <Table rows={usages.data ?? []} rowKey={(u) => u.id} empty="Sin consumos registrados"
              columns={[
                { header: 'Insumo', cell: (u) => u.supplyName },
                { header: 'Cantidad', cell: (u) => `${u.quantityUsed} ${UNIT_LABEL[u.unit]}` },
                { header: 'Fecha', cell: (u) => formatDateTime(u.usedAt) },
              ]} />
          )}
        </Card>
      </div>
      <ConfirmModal open={discharging} title="Dar de alta" confirmText="Dar de alta" loading={discharge.isPending} error={discharge.error}
        message={`Se registrará el alta con fecha de hoy y el total de la estancia (${formatMoney(h.costPerDay)} por día).`}
        onConfirm={() => discharge.mutate()} onClose={() => { setDischarging(false); discharge.reset() }} />
    </>
  )
}

function RegisterUsageForm({ hospitalizationId }: { hospitalizationId: number }) {
  const queryClient = useQueryClient()
  const [supplyId, setSupplyId] = useState('')
  const [quantity, setQuantity] = useState('1')
  const supplies = useQuery({ queryKey: ['supplies', 'active'], queryFn: () => suppliesApi.list({ active: true, size: 200 }) })
  const selected = supplies.data?.content.find((s) => String(s.id) === supplyId)
  const register = useMutation({
    mutationFn: () => hospitalizationsApi.registerUsage({ hospitalizationId, supplyId: Number(supplyId), quantityUsed: Number(quantity) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['hospitalizations', hospitalizationId, 'usages'] })
      queryClient.invalidateQueries({ queryKey: ['supplies'] })
      setQuantity('1')
    },
  })
  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-end gap-3">
        <Field label="Insumo" className="min-w-56 flex-1">
          <Select value={supplyId} onChange={(e) => setSupplyId(e.target.value)}>
            <option value="">Selecciona…</option>
            {supplies.data?.content.map((s) => <option key={s.id} value={s.id} disabled={s.quantity === 0}>{s.name} ({s.quantity} {UNIT_LABEL[s.unit]})</option>)}
          </Select>
        </Field>
        <Field label="Cantidad" className="w-28"><Input type="number" min="1" max={selected?.quantity} value={quantity} onChange={(e) => setQuantity(e.target.value)} /></Field>
        <Button disabled={!supplyId || Number(quantity) < 1} loading={register.isPending} onClick={() => register.mutate()}>Registrar consumo</Button>
      </div>
      <ErrorAlert error={register.error} />
    </div>
  )
}
