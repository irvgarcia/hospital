import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { accountApi } from '../api/endpoints'
import { useAuth } from '../auth/AuthContext'
import { Button, Card, DescriptionList, ErrorAlert, Field, Input, PageHeader, SuccessAlert } from '../components/ui'
import { ROLE_LABEL } from '../lib/format'

export function AccountPage() {
  const { account } = useAuth()
  const [form, setForm] = useState({ current: '', next: '', confirm: '' })
  const [mismatch, setMismatch] = useState(false)
  const change = useMutation({
    mutationFn: () => accountApi.changePassword(form.current, form.next),
    onSuccess: () => setForm({ current: '', next: '', confirm: '' }),
  })
  if (!account) return null

  function submit(event: FormEvent) {
    event.preventDefault()
    setMismatch(form.next !== form.confirm)
    if (form.next === form.confirm) change.mutate()
  }

  return (
    <>
      <PageHeader title="Mi cuenta" />
      <div className="grid gap-6 lg:grid-cols-2">
        <Card title="Datos de la cuenta">
          <DescriptionList items={[
            { label: 'Nombre', value: `${account.firstName} ${account.lastName}` },
            { label: 'Email', value: account.email },
            { label: 'Rol', value: account.roles.map((r) => ROLE_LABEL[r]).join(', ') },
          ]} />
        </Card>
        <Card title="Cambiar contraseña">
          <form onSubmit={submit} className="space-y-4">
            <Field label="Contraseña actual"><Input type="password" required value={form.current} onChange={(e) => setForm({ ...form, current: e.target.value })} /></Field>
            <Field label="Contraseña nueva" hint="Al menos 8 caracteres"><Input type="password" minLength={8} required value={form.next} onChange={(e) => setForm({ ...form, next: e.target.value })} /></Field>
            <Field label="Confirmar contraseña nueva" error={mismatch ? 'Las contraseñas no coinciden' : undefined}>
              <Input type="password" required value={form.confirm} onChange={(e) => setForm({ ...form, confirm: e.target.value })} />
            </Field>
            <ErrorAlert error={change.error} />
            {change.isSuccess && <SuccessAlert>Contraseña actualizada.</SuccessAlert>}
            <div className="flex justify-end"><Button type="submit" loading={change.isPending}>Cambiar contraseña</Button></div>
          </form>
        </Card>
      </div>
    </>
  )
}
