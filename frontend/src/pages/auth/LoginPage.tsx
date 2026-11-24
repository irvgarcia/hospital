import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { homePathFor, useAuth } from '../../auth/AuthContext'
import { Button, ErrorAlert, Field, Input } from '../../components/ui'
import { AuthShell } from './AuthShell'

export function LoginPage() {
  const { login, account } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [loading, setLoading] = useState(false)

  if (account) return <Navigate to={homePathFor(account)} replace />

  async function submit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const acc = await login(email, password)
      const from = (location.state as { from?: string } | null)?.from
      navigate(acc.roles.includes('PATIENT') && !acc.profileCompleted ? '/completar-perfil' : from ?? '/', { replace: true })
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell title="Iniciar sesión" subtitle="Accede con tu email y contraseña">
      <form onSubmit={submit} className="space-y-4">
        <Field label="Email">
          <Input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </Field>
        <Field label="Contraseña">
          <Input type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        </Field>
        <ErrorAlert error={error} />
        <Button type="submit" className="w-full" loading={loading}>Entrar</Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">
        ¿Eres paciente nuevo? <Link to="/registro" className="font-medium text-brand-700 hover:underline">Crea tu cuenta</Link>
      </p>
    </AuthShell>
  )
}
