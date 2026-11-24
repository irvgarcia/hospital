import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { patientsApi } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthContext'
import { PatientProfileForm } from '../../components/PatientProfileForm'
import { ActiveBadge } from '../../components/StatusBadge'
import { Button, Card, ErrorAlert, Field, Input, Loading, Modal, PageHeader, Pagination, Select, Table } from '../../components/ui'
import { BLOOD_TYPES, formatDate } from '../../lib/format'
import { useDebounced } from '../../lib/useDebounced'

export function PatientsPage() {
  const { hasRole } = useAuth()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [filters, setFilters] = useState({ status: '', bloodType: '', hospitalized: '' })
  const [page, setPage] = useState(0)
  const [registering, setRegistering] = useState(false)
  const name = useDebounced(search)
  const query = useQuery({
    queryKey: ['patients', 'list', name, filters, page],
    queryFn: () => patientsApi.list({ name, ...filters, page, size: 15 }),
  })
  const set = (key: keyof typeof filters, value: string) => { setFilters({ ...filters, [key]: value }); setPage(0) }

  return (
    <>
      <PageHeader title="Pacientes"
        actions={hasRole('ADMIN', 'RECEPTIONIST') && <Button onClick={() => setRegistering(true)}>Registrar paciente</Button>} />
      <Card padded={false} title={
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Buscar"><Input placeholder="Nombre o email" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0) }} className="w-60" /></Field>
          <Field label="Estado"><Select value={filters.status} onChange={(e) => set('status', e.target.value)} className="w-32">
            <option value="">Todos</option><option value="true">Activos</option><option value="false">Inactivos</option>
          </Select></Field>
          <Field label="Grupo"><Select value={filters.bloodType} onChange={(e) => set('bloodType', e.target.value)} className="w-24">
            <option value="">Todos</option>{BLOOD_TYPES.map((b) => <option key={b} value={b}>{b}</option>)}
          </Select></Field>
          <Field label="Hospitalizado"><Select value={filters.hospitalized} onChange={(e) => set('hospitalized', e.target.value)} className="w-32">
            <option value="">Todos</option><option value="true">Sí</option><option value="false">No</option>
          </Select></Field>
        </div>}>
        <ErrorAlert error={query.error} className="m-4" />
        {query.isLoading ? <Loading /> : (
          <>
            <Table rows={query.data?.content ?? []} rowKey={(p) => p.id} empty="Sin pacientes" onRowClick={(p) => navigate(`/pacientes/${p.id}`)}
              columns={[
                { header: 'Paciente', cell: (p) => <><span className="font-medium text-brand-700">{p.fullName}</span><div className="text-xs text-slate-500">{p.email}</div></> },
                { header: 'Teléfono', cell: (p) => p.phoneNumber },
                { header: 'Nacimiento', cell: (p) => formatDate(p.dateOfBirth) },
                { header: 'Grupo', cell: (p) => p.bloodType ?? '—' },
                { header: 'Registro', cell: (p) => formatDate(p.registerDate) },
                { header: 'Estado', cell: (p) => <ActiveBadge active={p.active} /> },
              ]} />
            {query.data && <Pagination page={page} totalPages={query.data.page.totalPages} totalElements={query.data.page.totalElements} onChange={setPage} />}
          </>
        )}
      </Card>
      {registering && <RegisterPatientModal onClose={() => setRegistering(false)} onCreated={(id) => navigate(`/pacientes/${id}`)} />}
    </>
  )
}

function RegisterPatientModal({ onClose, onCreated }: { onClose: () => void; onCreated: (id: number) => void }) {
  const queryClient = useQueryClient()
  const [account, setAccount] = useState({ firstName: '', lastName: '', email: '', password: '' })
  const register = useMutation({
    mutationFn: (body: Record<string, unknown>) => patientsApi.register(body),
    onSuccess: (patient) => { queryClient.invalidateQueries({ queryKey: ['patients'] }); onCreated(patient.id) },
  })
  const errors = register.error instanceof ApiError ? register.error.fieldErrors() : {}
  return (
    <Modal open wide title="Registrar paciente" onClose={onClose}>
      <PatientProfileForm submitText="Registrar"
        extraFields={
          <fieldset className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <legend className="mb-2 text-sm font-semibold text-slate-800">Cuenta del paciente</legend>
            <Field label="Nombre" error={errors.firstName}><Input required value={account.firstName} onChange={(e) => setAccount({ ...account, firstName: e.target.value })} /></Field>
            <Field label="Apellido" error={errors.lastName}><Input required value={account.lastName} onChange={(e) => setAccount({ ...account, lastName: e.target.value })} /></Field>
            <Field label="Email" error={errors.email}><Input type="email" required value={account.email} onChange={(e) => setAccount({ ...account, email: e.target.value })} /></Field>
            <Field label="Contraseña inicial" error={errors.password} hint="El paciente podrá cambiarla en su cuenta">
              <Input type="text" minLength={8} required value={account.password} onChange={(e) => setAccount({ ...account, password: e.target.value })} />
            </Field>
          </fieldset>
        }
        onSubmit={(profile) => register.mutateAsync({ ...account, profile })} />
    </Modal>
  )
}
