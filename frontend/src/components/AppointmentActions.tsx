import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { appointmentsApi, doctorsApi, recordsApi } from '../api/endpoints'
import type { Appointment, AvailableSlot } from '../api/types'
import { formatDate, formatTime, todayIso } from '../lib/format'
import { Button, ConfirmModal, ErrorAlert, Field, Input, Loading, Modal, SuccessAlert, Textarea } from './ui'

function useInvalidate() {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: ['appointments'] })
    queryClient.invalidateQueries({ queryKey: ['availability'] })
    queryClient.invalidateQueries({ queryKey: ['records'] })
  }
}

export function CancelAppointmentButton({ appointment }: { appointment: Appointment }) {
  const [open, setOpen] = useState(false)
  const invalidate = useInvalidate()
  const cancel = useMutation({
    mutationFn: () => appointmentsApi.cancel(appointment.id),
    onSuccess: () => { invalidate(); setOpen(false) },
  })
  return (
    <>
      <Button size="sm" variant="ghost" className="text-red-600 hover:bg-red-50" onClick={() => setOpen(true)}>Cancelar</Button>
      <ConfirmModal open={open} danger title="Cancelar cita" confirmText="Cancelar cita" loading={cancel.isPending} error={cancel.error}
        message={<>¿Cancelar la cita del {formatDate(appointment.date)} a las {formatTime(appointment.startTime)} con {appointment.doctorName}?</>}
        onConfirm={() => cancel.mutate()} onClose={() => { setOpen(false); cancel.reset() }} />
    </>
  )
}

export function RescheduleButton({ appointment }: { appointment: Appointment }) {
  const [open, setOpen] = useState(false)
  const [date, setDate] = useState(appointment.date >= todayIso() ? appointment.date : todayIso(1))
  const [slot, setSlot] = useState<AvailableSlot | null>(null)
  const invalidate = useInvalidate()
  const slots = useQuery({
    queryKey: ['availability', String(appointment.doctorId), date],
    queryFn: () => doctorsApi.availability(appointment.doctorId, date),
    enabled: open,
  })
  const reschedule = useMutation({
    mutationFn: () => appointmentsApi.reschedule(appointment.id, { date, startTime: slot!.startTime, endTime: slot!.endTime }),
    onSuccess: () => { invalidate(); setOpen(false) },
  })
  return (
    <>
      <Button size="sm" variant="ghost" onClick={() => setOpen(true)}>Reprogramar</Button>
      <Modal open={open} title="Reprogramar cita" onClose={() => setOpen(false)}
        footer={<>
          <Button variant="secondary" onClick={() => setOpen(false)}>Volver</Button>
          <Button disabled={!slot} loading={reschedule.isPending} onClick={() => reschedule.mutate()}>Guardar</Button>
        </>}>
        <div className="space-y-4">
          <p className="text-sm text-slate-600">Cita actual: {formatDate(appointment.date)} {formatTime(appointment.startTime)} con {appointment.doctorName}.</p>
          <Field label="Nueva fecha" className="max-w-xs">
            <Input type="date" min={todayIso()} value={date} onChange={(e) => { setDate(e.target.value); setSlot(null) }} />
          </Field>
          {slots.isLoading ? <Loading /> : slots.data && slots.data.length > 0 ? (
            <div className="grid grid-cols-4 gap-2">
              {slots.data.map((s) => (
                <button type="button" key={s.startTime} onClick={() => setSlot(s)}
                  className={`rounded-lg border px-2 py-1.5 text-sm ${slot?.startTime === s.startTime ? 'border-brand-700 bg-brand-700 text-white' : 'border-slate-300 hover:border-brand-600'}`}>
                  {formatTime(s.startTime)}
                </button>
              ))}
            </div>
          ) : <p className="text-sm text-slate-500">Sin horarios libres ese día.</p>}
          <ErrorAlert error={reschedule.error} />
        </div>
      </Modal>
    </>
  )
}

export function CompleteAppointmentButton({ appointment }: { appointment: Appointment }) {
  const invalidate = useInvalidate()
  const [error, setError] = useState<unknown>(null)
  const complete = useMutation({
    mutationFn: () => appointmentsApi.complete(appointment.id),
    onSuccess: invalidate,
    onError: setError,
  })
  const future = appointment.date > todayIso()
  return (
    <span className="inline-flex flex-col items-start">
      <Button size="sm" disabled={future} title={future ? 'Solo se completan citas de hoy o anteriores' : undefined}
        loading={complete.isPending} onClick={() => complete.mutate()}>Completar</Button>
      {error ? <ErrorAlert error={error} className="mt-1 px-2 py-1 text-xs" /> : null}
    </span>
  )
}

/** El doctor registra la entrada del historial de una cita completada. */
export function MedicalRecordButton({ appointment }: { appointment: Appointment }) {
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState({ diagnostics: '', treatment: '', notes: '' })
  const invalidate = useInvalidate()
  const save = useMutation({
    mutationFn: () => recordsApi.create({ appointmentId: appointment.id, ...form, notes: form.notes || undefined }),
    onSuccess: invalidate,
  })
  return (
    <>
      <Button size="sm" variant="secondary" onClick={() => setOpen(true)}>Registrar historial</Button>
      <Modal open={open} title={`Historial · ${appointment.patientName}`} onClose={() => { setOpen(false); save.reset() }}
        footer={save.isSuccess ? <Button onClick={() => { setOpen(false); save.reset() }}>Cerrar</Button> : <>
          <Button variant="secondary" onClick={() => setOpen(false)}>Volver</Button>
          <Button loading={save.isPending} disabled={!form.diagnostics.trim() || !form.treatment.trim()} onClick={() => save.mutate()}>Guardar</Button>
        </>}>
        {save.isSuccess ? <SuccessAlert>Entrada del historial guardada.</SuccessAlert> : (
          <div className="space-y-4">
            <p className="text-sm text-slate-500">Cita del {formatDate(appointment.date)} {formatTime(appointment.startTime)}{appointment.reason ? ` · Motivo: ${appointment.reason}` : ''}</p>
            <Field label="Diagnóstico"><Textarea maxLength={1000} value={form.diagnostics} onChange={(e) => setForm({ ...form, diagnostics: e.target.value })} /></Field>
            <Field label="Tratamiento"><Textarea maxLength={1000} value={form.treatment} onChange={(e) => setForm({ ...form, treatment: e.target.value })} /></Field>
            <Field label="Notas (opcional)"><Textarea maxLength={1000} value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></Field>
            <ErrorAlert error={save.error} />
          </div>
        )}
      </Modal>
    </>
  )
}
