import type { AppointmentStatus, DayOfWeek, Gender, MovementType, Role, Unit } from '../api/types'

export const ROLE_LABEL: Record<Role, string> = {
  ADMIN: 'Administrador',
  RECEPTIONIST: 'Recepción',
  DOCTOR: 'Doctor',
  PATIENT: 'Paciente',
}

export const STATUS_LABEL: Record<AppointmentStatus, string> = {
  PENDING: 'Pendiente',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
}

export const GENDER_LABEL: Record<Gender, string> = { FEMALE: 'Femenino', MALE: 'Masculino', OTHER: 'Otro' }

export const DAY_LABEL: Record<DayOfWeek, string> = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
}

export const DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

export const UNIT_LABEL: Record<Unit, string> = { BOX: 'Caja', MILLIGRAM: 'mg', MILLILITER: 'ml', UNIT: 'Pieza' }

export const MOVEMENT_LABEL: Record<MovementType, string> = { IN: 'Entrada', OUT: 'Salida' }

export const BLOOD_TYPES = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'] as const

/** "2026-11-21" -> "21 nov 2026" (sin desfase de zona horaria). */
export function formatDate(value?: string | null) {
  if (!value) return '—'
  const [y, m, d] = value.slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('es-MX', { day: '2-digit', month: 'short', year: 'numeric' })
}

export function formatDateLong(value?: string | null) {
  if (!value) return '—'
  const [y, m, d] = value.slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('es-MX', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
}

export function formatDateTime(value?: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleString('es-MX', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

/** "10:30:00" -> "10:30" */
export function formatTime(value?: string | null) {
  return value ? value.slice(0, 5) : '—'
}

export function formatMoney(value?: number | null) {
  if (value === null || value === undefined) return '—'
  return value.toLocaleString('es-MX', { style: 'currency', currency: 'MXN' })
}

export function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

/** Fecha local de hoy en formato ISO (yyyy-mm-dd). */
export function todayIso(offsetDays = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function ageFrom(dateOfBirth: string) {
  const [y, m, d] = dateOfBirth.split('-').map(Number)
  const today = new Date()
  let age = today.getFullYear() - y
  if (today.getMonth() + 1 < m || (today.getMonth() + 1 === m && today.getDate() < d)) age--
  return age
}
