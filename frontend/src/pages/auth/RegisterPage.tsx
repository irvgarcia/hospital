import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { homePathFor, useAuth } from '../../auth/AuthContext'
import { Button, ErrorAlert, Field, Input } from '../../components/ui'
import { AuthShell } from './AuthShell'

export function RegisterPage() {
  const { register, account } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '', confirm: '' })
  const [error, setError] = useState<unknown>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(false)

  if (account) return <Navigate to={homePathFor(account)} replace />

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (form.password !== form.confirm) {
      setFieldErrors({ confirm: 'Las contraseñas no coinciden' })
      return
    }
    setLoading(true)
    setError(null)
    setFieldErrors({})
    try {
      await register({ firstName: form.firstName, lastName: form.lastName, email: form.email, password: form.password })
      navigate('/completar-perfil', { replace: true })
    } catch (e) {
      setError(e)
      if (e instanceof ApiError) setFieldErrors(e.fieldErrors())
    } finally {
      setLoading(false)
    }
  }

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [key]: e.target.value })

  return (
    <AuthShell title="Crear cuenta de paciente" subtitle="Después completarás tu perfil clínico">
      <form onSubmit={submit} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Field label="Nombre" error={fieldErrors.firstName}><Input required value={form.firstName} onChange={set('firstName')} /></Field>
          <Field label="Apellido" error={fieldErrors.lastName}><Input required value={form.lastName} onChange={set('lastName')} /></Field>
        </div>
        <Field label="Email" error={fieldErrors.email}><Input type="email" required value={form.email} onChange={set('email')} /></Field>
        <Field label="Contraseña" error={fieldErrors.password} hint="Al menos 8 caracteres">
          <Input type="password" minLength={8} required value={form.password} onChange={set('password')} />
        </Field>
        <Field label="Confirmar contraseña" error={fieldErrors.confirm}>
          <Input type="password" required value={form.confirm} onChange={set('confirm')} />
        </Field>
        <ErrorAlert error={error} />
        <Button type="submit" className="w-full" loading={loading}>Crear cuenta</Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">
        ¿Ya tienes cuenta? <Link to="/login" className="font-medium text-brand-700 hover:underline">Inicia sesión</Link>
      </p>
    </AuthShell>
  )
}
