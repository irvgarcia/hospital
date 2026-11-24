import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { appointmentsApi, doctorsApi, specialtiesApi } from '../api/endpoints'
import type { Appointment, AvailableSlot, Patient } from '../api/types'
import { DAY_LABEL, formatMoney, formatTime, todayIso } from '../lib/format'
import { PatientPicker } from './PatientPicker'
import { Button, ErrorAlert, Field, Input, Loading, Select, Textarea } from './ui'

/**
 * Agendar: especialidad -> doctor -> fecha -> horario libre (GET /doctors/{id}/availability).
 * Con forPatient (recepcion/admin) se elige primero el paciente.
 */
export function BookingForm({ forPatient, onBooked }: { forPatient?: boolean; onBooked: (appointment: Appointment) => void }) {
  const queryClient = useQueryClient()
  const [patient, setPatient] = useState<Patient | null>(null)
  const [specialtyId, setSpecialtyId] = useState('')
  const [doctorId, setDoctorId] = useState('')
  const [date, setDate] = useState(todayIso(1))
  const [slot, setSlot] = useState<AvailableSlot | null>(null)
  const [reason, setReason] = useState('')

  const specialties = useQuery({ queryKey: ['specialties', 'active'], queryFn: () => specialtiesApi.list(true) })
  const doctors = useQuery({
    queryKey: ['doctors', 'catalog', specialtyId],
    queryFn: () => doctorsApi.list({ specialtyId, status: true, size: 50 }),
    enabled: !!specialtyId,
  })
  const doctor = doctors.data?.content.find((d) => String(d.id) === doctorId)
  const slots = useQuery({
    queryKey: ['availability', doctorId, date],
    queryFn: () => doctorsApi.availability(Number(doctorId), date),
    enabled: !!doctorId && !!date,
  })

  const book = useMutation({
    mutationFn: () => appointmentsApi.create({
      doctorId: Number(doctorId), date, startTime: slot!.startTime, endTime: slot!.endTime,
      reason: reason || undefined, patientId: patient?.id,
    }),
    onSuccess: (appointment) => {
      queryClient.invalidateQueries({ queryKey: ['appointments'] })
      queryClient.invalidateQueries({ queryKey: ['availability'] })
      onBooked(appointment)
    },
  })

  const ready = !!slot && (!forPatient || !!patient)

  return (
    <div className="space-y-5">
      {forPatient && (
        <Field label="Paciente">
          <PatientPicker value={patient} onChange={setPatient} />
        </Field>
      )}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Field label="Especialidad">
          <Select value={specialtyId} onChange={(e) => { setSpecialtyId(e.target.value); setDoctorId(''); setSlot(null) }}>
            <option value="">Selecciona…</option>
            {specialties.data?.map((s) => <option key={s.id} value={s.id}>{s.name} · {formatMoney(s.cost)}</option>)}
          </Select>
        </Field>
        <Field label="Doctor">
          <Select value={doctorId} disabled={!specialtyId} onChange={(e) => { setDoctorId(e.target.value); setSlot(null) }}>
            <option value="">{specialtyId && doctors.data?.content.length === 0 ? 'Sin doctores disponibles' : 'Selecciona…'}</option>
            {doctors.data?.content.map((d) => <option key={d.id} value={d.id}>{d.fullName}</option>)}
          </Select>
        </Field>
      </div>

      {doctor && (
        <div className="rounded-lg bg-slate-50 px-4 py-3 text-xs text-slate-600">
          <span className="font-medium text-slate-700">Horario: </span>
          {doctor.schedules.length === 0 ? 'sin horario registrado' :
            doctor.schedules.map((s) => `${DAY_LABEL[s.dayOfWeek]} ${formatTime(s.startTime)}–${formatTime(s.endTime)}`).join(' · ')}
        </div>
      )}

      {doctorId && (
        <div className="space-y-3">
          <Field label="Fecha" className="max-w-xs">
            <Input type="date" min={todayIso()} value={date} onChange={(e) => { setDate(e.target.value); setSlot(null) }} />
          </Field>
          <div>
            <span className="label">Horario disponible</span>
            {slots.isLoading ? <Loading text="Buscando horarios…" /> :
              slots.data && slots.data.length > 0 ? (
                <div className="grid grid-cols-3 gap-2 sm:grid-cols-6">
                  {slots.data.map((s) => {
                    const selected = slot?.startTime === s.startTime
                    return (
                      <button type="button" key={s.startTime} onClick={() => setSlot(s)}
                        className={`rounded-lg border px-2 py-2 text-sm font-medium transition ${selected ? 'border-brand-700 bg-brand-700 text-white' : 'border-slate-300 bg-white hover:border-brand-600 hover:text-brand-700'}`}>
                        {formatTime(s.startTime)}
                      </button>
                    )
                  })}
                </div>
              ) : <p className="text-sm text-slate-500">No hay horarios libres ese día. Prueba con otra fecha.</p>}
          </div>
        </div>
      )}

      {slot && (
        <Field label="Motivo de la consulta (opcional)">
          <Textarea maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Describe brevemente el motivo" />
        </Field>
      )}

      <ErrorAlert error={book.error} />
      <div className="flex justify-end">
        <Button disabled={!ready} loading={book.isPending} onClick={() => book.mutate()}>
          {slot ? `Agendar ${formatTime(slot.startTime)}` : 'Agendar cita'}
        </Button>
      </div>
    </div>
  )
}
