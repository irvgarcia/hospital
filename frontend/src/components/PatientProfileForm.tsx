import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import type { BloodType, Gender, PatientProfile } from '../api/types'
import { BLOOD_TYPES, GENDER_LABEL, todayIso } from '../lib/format'
import { Button, ErrorAlert, Field, Input, Select } from './ui'

const EMPTY: PatientProfile = {
  phoneNumber: '', address: '', dateOfBirth: '', gender: 'FEMALE', allergies: 'Ninguna', bloodType: 'O+',
  chronicDiseases: 'Ninguna', currentMedications: 'Ninguno', emergencyContact: '', emergencyContactName: '',
  emergencyContactRelation: '', height: 1.7, weight: 70, smoker: false,
}

/** Formulario del perfil clinico. Con lockIdentity, sexo/fecha de nacimiento/grupo no se editan (solo al crear). */
export function PatientProfileForm({ initial, submitText, onSubmit, lockIdentity, extraFields }: {
  initial?: Partial<PatientProfile>
  submitText: string
  onSubmit: (profile: PatientProfile) => Promise<unknown>
  lockIdentity?: boolean
  extraFields?: React.ReactNode
}) {
  const [form, setForm] = useState<PatientProfile>({ ...EMPTY, ...initial })
  const [error, setError] = useState<unknown>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  const set = <K extends keyof PatientProfile>(key: K, value: PatientProfile[K]) => setForm((f) => ({ ...f, [key]: value }))
  const err = (key: string) => fieldErrors[key] ?? fieldErrors[`profile.${key}`]

  async function submit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    setFieldErrors({})
    try {
      await onSubmit({ ...form, height: Number(form.height), weight: Number(form.weight) })
    } catch (e) {
      setError(e)
      if (e instanceof ApiError) setFieldErrors(e.fieldErrors())
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-6">
      {extraFields}
      <fieldset className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <legend className="mb-2 text-sm font-semibold text-slate-800">Datos personales</legend>
        <Field label="Fecha de nacimiento" error={err('dateOfBirth')}>
          <Input type="date" required max={todayIso(-1)} value={form.dateOfBirth} disabled={lockIdentity}
            onChange={(e) => set('dateOfBirth', e.target.value)} />
        </Field>
        <Field label="Sexo" error={err('gender')}>
          <Select value={form.gender} disabled={lockIdentity} onChange={(e) => set('gender', e.target.value as Gender)}>
            {(Object.keys(GENDER_LABEL) as Gender[]).map((g) => <option key={g} value={g}>{GENDER_LABEL[g]}</option>)}
          </Select>
        </Field>
        <Field label="Teléfono (10 dígitos)" error={err('phoneNumber')}>
          <Input required inputMode="numeric" pattern="\d{10}" value={form.phoneNumber} onChange={(e) => set('phoneNumber', e.target.value)} />
        </Field>
        <Field label="Dirección" error={err('address')}>
          <Input required value={form.address} onChange={(e) => set('address', e.target.value)} />
        </Field>
      </fieldset>

      <fieldset className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <legend className="mb-2 text-sm font-semibold text-slate-800">Datos clínicos</legend>
        <Field label="Grupo sanguíneo" error={err('bloodType')}>
          <Select value={form.bloodType} disabled={lockIdentity} onChange={(e) => set('bloodType', e.target.value as BloodType)}>
            {BLOOD_TYPES.map((b) => <option key={b} value={b}>{b}</option>)}
          </Select>
        </Field>
        <Field label="Estatura (m)" error={err('height')}>
          <Input type="number" step="0.01" min="0.3" max="2.8" required value={form.height} onChange={(e) => set('height', e.target.value as unknown as number)} />
        </Field>
        <Field label="Peso (kg)" error={err('weight')}>
          <Input type="number" step="0.1" min="1" max="500" required value={form.weight} onChange={(e) => set('weight', e.target.value as unknown as number)} />
        </Field>
        <Field label="Alergias" error={err('allergies')} className="sm:col-span-3">
          <Input required value={form.allergies} onChange={(e) => set('allergies', e.target.value)} />
        </Field>
        <Field label="Enfermedades crónicas" error={err('chronicDiseases')} className="sm:col-span-3">
          <Input required value={form.chronicDiseases} onChange={(e) => set('chronicDiseases', e.target.value)} />
        </Field>
        <Field label="Medicamentos actuales" error={err('currentMedications')} className="sm:col-span-3">
          <Input required value={form.currentMedications} onChange={(e) => set('currentMedications', e.target.value)} />
        </Field>
        <label className="flex items-center gap-2 text-sm text-slate-700 sm:col-span-3">
          <input type="checkbox" className="h-4 w-4 rounded border-slate-300" checked={form.smoker} onChange={(e) => set('smoker', e.target.checked)} />
          Fuma
        </label>
      </fieldset>

      <fieldset className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <legend className="mb-2 text-sm font-semibold text-slate-800">Contacto de emergencia</legend>
        <Field label="Nombre" error={err('emergencyContactName')}>
          <Input required value={form.emergencyContactName} onChange={(e) => set('emergencyContactName', e.target.value)} />
        </Field>
        <Field label="Relación" error={err('emergencyContactRelation')}>
          <Input required value={form.emergencyContactRelation} onChange={(e) => set('emergencyContactRelation', e.target.value)} />
        </Field>
        <Field label="Teléfono (10 dígitos)" error={err('emergencyContact')}>
          <Input required inputMode="numeric" pattern="\d{10}" value={form.emergencyContact} onChange={(e) => set('emergencyContact', e.target.value)} />
        </Field>
      </fieldset>

      <ErrorAlert error={error} />
      <div className="flex justify-end">
        <Button type="submit" loading={saving}>{submitText}</Button>
      </div>
    </form>
  )
}
