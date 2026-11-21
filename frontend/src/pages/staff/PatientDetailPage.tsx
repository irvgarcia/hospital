import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { download } from '../../api/client'
import { appointmentsApi, hospitalizationsApi, patientsApi, recordsApi } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthContext'
import { FilesPanel } from '../../components/FilesPanel'
import { RecordsList } from '../../components/RecordsList'
import { ActiveBadge, StatusBadge } from '../../components/StatusBadge'
import { Button, Card, ConfirmModal, DescriptionList, ErrorAlert, Loading, Modal, PageHeader, Table } from '../../components/ui'
import { ageFrom, formatDate, formatTime, GENDER_LABEL } from '../../lib/format'
import { ProfileEditor } from '../patient/MyProfilePage'

type Tab = 'datos' | 'historial' | 'citas' | 'hospitalizaciones' | 'archivos'

export function PatientDetailPage() {
  const id = Number(useParams().id)
  const { hasRole } = useAuth()
  const queryClient = useQueryClient()
  const [tab, setTab] = useState<Tab>('datos')
  const [editing, setEditing] = useState(false)
  const [toggling, setToggling] = useState(false)
  const [pdfError, setPdfError] = useState<unknown>(null)
  const patient = useQuery({ queryKey: ['patients', id], queryFn: () => patientsApi.get(id) })
  const toggle = useMutation({
    mutationFn: () => patient.data!.active ? patientsApi.deactivate(id) : patientsApi.activate(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['patients'] }); setToggling(false) },
  })

  if (patient.isLoading) return <Loading />
  if (patient.error) return <ErrorAlert error={patient.error} />
  const p = patient.data!
  const canSeeRecords = hasRole('ADMIN', 'DOCTOR')
  const tabs: { key: Tab; label: string }[] = [
    { key: 'datos', label: 'Datos' },
    ...(canSeeRecords ? [{ key: 'historial' as Tab, label: 'Historial clínico' }] : []),
    ...(hasRole('ADMIN', 'RECEPTIONIST') ? [{ key: 'citas' as Tab, label: 'Citas' }] : []),
    { key: 'hospitalizaciones', label: 'Hospitalizaciones' },
    { key: 'archivos', label: 'Archivos' },
  ]

  return (
    <>
      <PageHeader title={p.fullName} subtitle={`${p.email} · ${ageFrom(p.dateOfBirth)} años · ${p.bloodType ?? 'grupo sin registrar'}`}
        actions={<>
          {canSeeRecords && <Button variant="secondary" onClick={() => download(`/api/v1/patients/${id}/medical-record/pdf`, 'expediente.pdf').catch(setPdfError)}>Expediente PDF</Button>}
          {hasRole('ADMIN', 'RECEPTIONIST') && <Button variant="secondary" onClick={() => setEditing(true)}>Editar</Button>}
          {hasRole('ADMIN') && <Button variant={p.active ? 'danger' : 'primary'} onClick={() => setToggling(true)}>{p.active ? 'Desactivar' : 'Activar'}</Button>}
        </>} />
      <ErrorAlert error={pdfError} className="mb-4" />
      <div className="mb-4 flex gap-1 overflow-x-auto border-b border-slate-200">
        {tabs.map((t) => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`whitespace-nowrap border-b-2 px-4 py-2 text-sm font-medium ${tab === t.key ? 'border-brand-700 text-brand-800' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'datos' && (
        <Card>
          <DescriptionList items={[
            { label: 'Estado', value: <ActiveBadge active={p.active} /> },
            { label: 'Fecha de nacimiento', value: formatDate(p.dateOfBirth) },
            { label: 'Sexo', value: GENDER_LABEL[p.gender] },
            { label: 'Grupo sanguíneo', value: p.bloodType },
            { label: 'Estatura / peso', value: `${p.height} m / ${p.weight} kg` },
            { label: 'Fumador', value: p.smoker ? 'Sí' : 'No' },
            { label: 'Teléfono', value: p.phoneNumber },
            { label: 'Dirección', value: p.address },
            { label: 'Alergias', value: p.allergies },
            { label: 'Enfermedades crónicas', value: p.chronicDiseases },
            { label: 'Medicamentos actuales', value: p.currentMedications },
            { label: 'Contacto de emergencia', value: `${p.emergencyContactName} (${p.emergencyContactRelation}) · ${p.emergencyContact}` },
            { label: 'Registro', value: formatDate(p.registerDate) },
          ]} />
        </Card>
      )}
      {tab === 'historial' && <PatientRecords patientId={id} />}
      {tab === 'citas' && <PatientAppointments patientId={id} />}
      {tab === 'hospitalizaciones' && <PatientHospitalizations patientId={id} />}
      {tab === 'archivos' && <Card><FilesPanel patientId={id} /></Card>}

      <Modal open={editing} wide title="Editar paciente" onClose={() => setEditing(false)}>
        <ProfileEditor patient={p} onSaved={() => setEditing(false)} />
      </Modal>
      <ConfirmModal open={toggling} danger={p.active} title={p.active ? 'Desactivar paciente' : 'Activar paciente'}
        message={p.active ? 'El paciente no podrá iniciar sesión ni agendar citas. Se rechaza si tiene citas pendientes o está hospitalizado.' : 'El paciente podrá volver a iniciar sesión.'}
        confirmText={p.active ? 'Desactivar' : 'Activar'} loading={toggle.isPending} error={toggle.error}
        onConfirm={() => toggle.mutate()} onClose={() => { setToggling(false); toggle.reset() }} />
    </>
  )
}

function PatientRecords({ patientId }: { patientId: number }) {
  const records = useQuery({ queryKey: ['records', 'patient', patientId], queryFn: () => recordsApi.byPatient(patientId, { size: 50 }) })
  return <Card>{records.isLoading ? <Loading /> : <><ErrorAlert error={records.error} /><RecordsList records={records.data?.content ?? []} /></>}</Card>
}

function PatientAppointments({ patientId }: { patientId: number }) {
  const query = useQuery({ queryKey: ['appointments', 'patient', patientId], queryFn: () => appointmentsApi.list({ patientId, size: 50 }) })
  return (
    <Card padded={false}>
      {query.isLoading ? <Loading /> : (
        <Table rows={query.data?.content ?? []} rowKey={(a) => a.id} empty="Sin citas"
          columns={[
            { header: 'Fecha', cell: (a) => `${formatDate(a.date)} ${formatTime(a.startTime)}` },
            { header: 'Doctor', cell: (a) => `${a.doctorName} · ${a.specialty}` },
            { header: 'Motivo', cell: (a) => a.reason ?? '—' },
            { header: 'Estado', cell: (a) => <StatusBadge status={a.status} /> },
          ]} />
      )}
    </Card>
  )
}

function PatientHospitalizations({ patientId }: { patientId: number }) {
  const query = useQuery({ queryKey: ['hospitalizations', 'patient', patientId], queryFn: () => hospitalizationsApi.list({ patientId, size: 50 }) })
  return (
    <Card padded={false}>
      {query.isLoading ? <Loading /> : (
        <Table rows={query.data?.content ?? []} rowKey={(h) => h.id} empty="Sin hospitalizaciones"
          columns={[
            { header: 'Ingreso', cell: (h) => formatDate(h.admissionDate) },
            { header: 'Alta', cell: (h) => formatDate(h.dischargeDate) },
            { header: 'Sala', cell: (h) => h.roomName },
            { header: 'Motivo', cell: (h) => h.reason },
            { header: 'Estado', cell: (h) => <ActiveBadge active={h.active} on="En curso" off="Alta" /> },
          ]} />
      )}
    </Card>
  )
}
