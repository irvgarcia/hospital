import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { suppliesApi } from '../../api/endpoints'
import type { MedicalSupply, MovementType, Unit } from '../../api/types'
import { ActiveBadge } from '../../components/StatusBadge'
import { Badge, Button, Card, ErrorAlert, Field, Input, Loading, Modal, PageHeader, Pagination, Select, Table } from '../../components/ui'
import { formatDateTime, MOVEMENT_LABEL, UNIT_LABEL } from '../../lib/format'
import { useDebounced } from '../../lib/useDebounced'

export function InventoryPage() {
  const [tab, setTab] = useState<'supplies' | 'movements'>('supplies')
  return (
    <>
      <PageHeader title="Inventario" subtitle="Insumos médicos, stock mínimo y movimientos" />
      <div className="mb-4 flex gap-1 border-b border-slate-200">
        {([['supplies', 'Insumos'], ['movements', 'Movimientos']] as const).map(([key, label]) => (
          <button key={key} onClick={() => setTab(key)}
            className={`border-b-2 px-4 py-2 text-sm font-medium ${tab === key ? 'border-brand-700 text-brand-800' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>{label}</button>
        ))}
      </div>
      {tab === 'supplies' ? <SuppliesTab /> : <MovementsTab />}
    </>
  )
}

function SuppliesTab() {
  const [search, setSearch] = useState('')
  const [lowStock, setLowStock] = useState('')
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<MedicalSupply | 'new' | null>(null)
  const [moving, setMoving] = useState<MedicalSupply | null>(null)
  const name = useDebounced(search)
  const query = useQuery({
    queryKey: ['supplies', 'list', name, lowStock, page],
    queryFn: () => suppliesApi.list({ name, lowStock, page, size: 15 }),
  })
  return (
    <>
      <Card padded={false} title={
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Buscar"><Input placeholder="Nombre del insumo" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0) }} className="w-56" /></Field>
          <Field label="Stock"><Select value={lowStock} onChange={(e) => { setLowStock(e.target.value); setPage(0) }} className="w-40">
            <option value="">Todos</option><option value="true">Stock bajo</option><option value="false">Stock suficiente</option>
          </Select></Field>
        </div>}
        actions={<Button onClick={() => setEditing('new')}>Nuevo insumo</Button>}>
        <ErrorAlert error={query.error} className="m-4" />
        {query.isLoading ? <Loading /> : (
          <>
            <Table rows={query.data?.content ?? []} rowKey={(s) => s.id} empty="Sin insumos"
              columns={[
                { header: 'Insumo', cell: (s) => <span className="font-medium">{s.name}</span> },
                { header: 'Existencia', cell: (s) => <>{s.quantity} {UNIT_LABEL[s.unit]} {s.lowStock && <Badge color={s.quantity === 0 ? 'red' : 'yellow'}>{s.quantity === 0 ? 'Agotado' : 'Stock bajo'}</Badge>}</> },
                { header: 'Mínimo', cell: (s) => `${s.minimumStock} ${UNIT_LABEL[s.unit]}` },
                { header: 'Estado', cell: (s) => <ActiveBadge active={s.active} /> },
                {
                  header: '', className: 'text-right', cell: (s) => (
                    <div className="flex justify-end gap-1">
                      <Button size="sm" variant="secondary" disabled={!s.active} onClick={() => setMoving(s)}>Entrada / salida</Button>
                      <Button size="sm" variant="ghost" onClick={() => setEditing(s)}>Editar</Button>
                    </div>
                  ),
                },
              ]} />
            {query.data && <Pagination page={page} totalPages={query.data.page.totalPages} totalElements={query.data.page.totalElements} onChange={setPage} />}
          </>
        )}
      </Card>
      {editing && <SupplyModal supply={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
      {moving && <MovementModal supply={moving} onClose={() => setMoving(null)} />}
    </>
  )
}

function SupplyModal({ supply, onClose }: { supply: MedicalSupply | null; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState({ name: supply?.name ?? '', quantity: '0', minimumStock: String(supply?.minimumStock ?? 10), unit: supply?.unit ?? 'BOX' as Unit, active: supply?.active ?? true })
  const save = useMutation({
    mutationFn: () => supply
      ? suppliesApi.update(supply.id, { name: form.name, minimumStock: Number(form.minimumStock), unit: form.unit, active: form.active })
      : suppliesApi.create({ name: form.name, quantity: Number(form.quantity), minimumStock: Number(form.minimumStock), unit: form.unit }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['supplies'] }); queryClient.invalidateQueries({ queryKey: ['dashboard'] }); onClose() },
  })
  return (
    <Modal open title={supply ? 'Editar insumo' : 'Nuevo insumo'} onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Volver</Button><Button loading={save.isPending} onClick={() => save.mutate()}>Guardar</Button></>}>
      <div className="space-y-4">
        <Field label="Nombre"><Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
        <div className="grid grid-cols-3 gap-3">
          {!supply && <Field label="Cantidad inicial"><Input type="number" min="0" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} /></Field>}
          <Field label="Stock mínimo" hint="Umbral de alerta"><Input type="number" min="0" value={form.minimumStock} onChange={(e) => setForm({ ...form, minimumStock: e.target.value })} /></Field>
          <Field label="Unidad"><Select value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value as Unit })}>
            {(Object.keys(UNIT_LABEL) as Unit[]).map((u) => <option key={u} value={u}>{UNIT_LABEL[u]}</option>)}
          </Select></Field>
        </div>
        {supply && (
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" className="h-4 w-4 rounded border-slate-300" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> Activo
          </label>
        )}
        <ErrorAlert error={save.error} />
      </div>
    </Modal>
  )
}

function MovementModal({ supply, onClose }: { supply: MedicalSupply; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [type, setType] = useState<MovementType>('IN')
  const [quantity, setQuantity] = useState('1')
  const save = useMutation({
    mutationFn: () => suppliesApi.registerMovement({ supplyId: supply.id, type, quantity: Number(quantity) }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['supplies'] }); queryClient.invalidateQueries({ queryKey: ['movements'] }); queryClient.invalidateQueries({ queryKey: ['dashboard'] }); onClose() },
  })
  return (
    <Modal open title={`Movimiento · ${supply.name}`} onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Volver</Button><Button loading={save.isPending} onClick={() => save.mutate()}>Registrar</Button></>}>
      <div className="space-y-4">
        <p className="text-sm text-slate-600">Existencia actual: <b>{supply.quantity} {UNIT_LABEL[supply.unit]}</b></p>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Tipo"><Select value={type} onChange={(e) => setType(e.target.value as MovementType)}>
            <option value="IN">Entrada</option><option value="OUT">Salida</option>
          </Select></Field>
          <Field label="Cantidad"><Input type="number" min="1" value={quantity} onChange={(e) => setQuantity(e.target.value)} /></Field>
        </div>
        <ErrorAlert error={save.error} />
      </div>
    </Modal>
  )
}

function MovementsTab() {
  const [type, setType] = useState('')
  const [page, setPage] = useState(0)
  const query = useQuery({ queryKey: ['movements', type, page], queryFn: () => suppliesApi.movements({ type, page, size: 20 }) })
  return (
    <Card padded={false} title={
      <Field label="Tipo"><Select value={type} onChange={(e) => { setType(e.target.value); setPage(0) }} className="w-36">
        <option value="">Todos</option><option value="IN">Entradas</option><option value="OUT">Salidas</option>
      </Select></Field>}>
      {query.isLoading ? <Loading /> : (
        <>
          <Table rows={query.data?.content ?? []} rowKey={(m) => m.id} empty="Sin movimientos"
            columns={[
              { header: 'Fecha', cell: (m) => formatDateTime(m.timestamp) },
              { header: 'Insumo', cell: (m) => m.supplyName },
              { header: 'Tipo', cell: (m) => <Badge color={m.type === 'IN' ? 'green' : 'blue'}>{MOVEMENT_LABEL[m.type]}</Badge> },
              { header: 'Cantidad', cell: (m) => m.quantityChanged },
              { header: 'Registró', cell: (m) => m.performedBy },
            ]} />
          {query.data && <Pagination page={page} totalPages={query.data.page.totalPages} totalElements={query.data.page.totalElements} onChange={setPage} />}
        </>
      )}
    </Card>
  )
}
