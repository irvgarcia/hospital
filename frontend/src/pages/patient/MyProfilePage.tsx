import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { patientsApi } from '../../api/endpoints'
import type { Patient } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { PatientProfileForm } from '../../components/PatientProfileForm'
import { Card, ErrorAlert, Field, Input, Loading, PageHeader, SuccessAlert } from '../../components/ui'

export function MyProfilePage() {
  const query = useQuery({ queryKey: ['patients', 'me'], queryFn: patientsApi.me })
  return (
    <>
      <PageHeader title="Mi perfil" subtitle="Mantén actualizados tus datos de contacto y clínicos" />
      <ErrorAlert error={query.error} />
      {query.isLoading ? <Loading /> : query.data && <ProfileEditor patient={query.data} self />}
    </>
  )
}

/** Edicion del perfil (paciente sobre si mismo o recepcion/admin sobre un paciente). */
export function ProfileEditor({ patient, self, onSaved }: { patient: Patient; self?: boolean; onSaved?: () => void }) {
  const queryClient = useQueryClient()
  const { refreshAccount } = useAuth()
  const [names, setNames] = useState({ firstName: patient.firstName, lastName: patient.lastName })
  const [saved, setSaved] = useState(false)
  const save = useMutation({
    mutationFn: (body: Record<string, unknown>) => self ? patientsApi.updateMe(body) : patientsApi.update(patient.id, body),
    onSuccess: async () => {
      setSaved(true)
      await queryClient.invalidateQueries({ queryKey: ['patients'] })
      if (self) await refreshAccount()
      onSaved?.()
    },
  })
  return (
    <Card>
      {saved && <div className="mb-4"><SuccessAlert>Datos guardados.</SuccessAlert></div>}
      <PatientProfileForm lockIdentity initial={patient} submitText="Guardar cambios"
        extraFields={
          <fieldset className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <legend className="mb-2 text-sm font-semibold text-slate-800">Cuenta</legend>
            <Field label="Nombre"><Input required value={names.firstName} onChange={(e) => setNames({ ...names, firstName: e.target.value })} /></Field>
            <Field label="Apellido"><Input required value={names.lastName} onChange={(e) => setNames({ ...names, lastName: e.target.value })} /></Field>
            <Field label="Email"><Input disabled value={patient.email} /></Field>
          </fieldset>
        }
        onSubmit={async (profile) => {
          setSaved(false)
          const { dateOfBirth: _d, gender: _g, bloodType: _b, ...editable } = profile
          void _d; void _g; void _b
          await save.mutateAsync({ ...editable, ...names })
        }} />
    </Card>
  )
}
