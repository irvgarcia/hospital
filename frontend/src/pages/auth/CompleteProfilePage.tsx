import { Navigate, useNavigate } from 'react-router-dom'
import { patientsApi } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthContext'
import { PatientProfileForm } from '../../components/PatientProfileForm'
import { AuthShell } from './AuthShell'

export function CompleteProfilePage() {
  const { account, refreshAccount, logout } = useAuth()
  const navigate = useNavigate()
  if (!account) return <Navigate to="/login" replace />
  if (!account.roles.includes('PATIENT') || account.profileCompleted) return <Navigate to="/" replace />

  return (
    <AuthShell wide title={`Hola, ${account.firstName}`} subtitle="Completa tu perfil clínico para poder agendar citas">
      <PatientProfileForm submitText="Guardar perfil"
        onSubmit={async (profile) => {
          await patientsApi.createProfile(profile)
          await refreshAccount()
          navigate('/', { replace: true })
        }} />
      <button className="mt-4 text-sm text-slate-500 hover:underline" onClick={() => logout()}>Salir</button>
    </AuthShell>
  )
}
