import { api } from './client'
import type {
  Account, Appointment, AvailableSlot, Dashboard, Doctor, FileInfo, Hospitalization, MedicalRecord,
  MedicalSupply, Page, Patient, PatientProfile, Receptionist, Room, Schedule, ScheduleSlot, Specialty,
  SupplyMovement, SupplyUsage, TokenResponse,
} from './types'

type Filters = Record<string, string | number | boolean | null | undefined>
type Body = Record<string, unknown>

export const authApi = {
  login: (email: string, password: string) =>
    api<TokenResponse>('/auth/login', { method: 'POST', body: { email, password }, anonymous: true }),
  register: (body: { email: string; firstName: string; lastName: string; password: string }) =>
    api<TokenResponse>('/auth/register', { method: 'POST', body, anonymous: true }),
}

export const accountApi = {
  get: () => api<Account>('/api/v1/account'),
  changePassword: (currentPassword: string, newPassword: string) =>
    api<void>('/api/v1/account/password', { method: 'PATCH', body: { currentPassword, newPassword } }),
}

export const dashboardApi = {
  get: () => api<Dashboard>('/api/v1/dashboard'),
}

export const patientsApi = {
  list: (query: Filters) => api<Page<Patient>>('/api/v1/patients', { query }),
  get: (id: number) => api<Patient>(`/api/v1/patients/${id}`),
  me: () => api<Patient>('/api/v1/patients/me'),
  createProfile: (body: PatientProfile) => api<Patient>('/api/v1/patients', { method: 'POST', body }),
  register: (body: Body) => api<Patient>('/api/v1/patients/register', { method: 'POST', body }),
  updateMe: (body: Body) => api<Patient>('/api/v1/patients/me', { method: 'PATCH', body }),
  update: (id: number, body: Body) => api<Patient>(`/api/v1/patients/${id}`, { method: 'PATCH', body }),
  deactivate: (id: number) => api<void>(`/api/v1/patients/${id}/deactivate`, { method: 'PATCH' }),
  activate: (id: number) => api<void>(`/api/v1/patients/${id}/activate`, { method: 'PATCH' }),
}

export const doctorsApi = {
  list: (query: Filters) => api<Page<Doctor>>('/api/v1/doctors', { query }),
  get: (id: number) => api<Doctor>(`/api/v1/doctors/${id}`),
  me: () => api<Doctor>('/api/v1/doctors/me'),
  availability: (id: number, date: string) => api<AvailableSlot[]>(`/api/v1/doctors/${id}/availability`, { query: { date } }),
  create: (body: Body) => api<Doctor>('/api/v1/doctors', { method: 'POST', body }),
  update: (id: number, body: Body) => api<Doctor>(`/api/v1/doctors/${id}`, { method: 'PATCH', body }),
  deactivate: (id: number) => api<void>(`/api/v1/doctors/${id}/deactivate`, { method: 'PATCH' }),
  activate: (id: number) => api<void>(`/api/v1/doctors/${id}/activate`, { method: 'PATCH' }),
}

export const schedulesApi = {
  list: (query: Filters) => api<Page<Schedule>>('/api/v1/doctor-schedules', { query }),
  create: (body: ScheduleSlot & { doctorId: number }) => api<Schedule>('/api/v1/doctor-schedules', { method: 'POST', body }),
  update: (id: number, body: { startTime: string; endTime: string }) =>
    api<Schedule>(`/api/v1/doctor-schedules/${id}`, { method: 'PATCH', body }),
  remove: (id: number) => api<void>(`/api/v1/doctor-schedules/${id}`, { method: 'DELETE' }),
}

export const specialtiesApi = {
  list: (active?: boolean) => api<Specialty[]>('/api/v1/specialties', { query: { active } }),
  create: (body: Body) => api<Specialty>('/api/v1/specialties', { method: 'POST', body }),
  update: (id: number, body: Body) => api<Specialty>(`/api/v1/specialties/${id}`, { method: 'PATCH', body }),
}

export const appointmentsApi = {
  list: (query: Filters) => api<Page<Appointment>>('/api/v1/appointments', { query }),
  mine: (query: Filters) => api<Page<Appointment>>('/api/v1/appointments/me', { query }),
  next: () => api<Appointment | undefined>('/api/v1/appointments/me/next'),
  get: (id: number) => api<Appointment>(`/api/v1/appointments/${id}`),
  create: (body: Body) => api<Appointment>('/api/v1/appointments', { method: 'POST', body }),
  reschedule: (id: number, body: { date: string; startTime: string; endTime: string }) =>
    api<Appointment>(`/api/v1/appointments/${id}/reschedule`, { method: 'PATCH', body }),
  complete: (id: number) => api<Appointment>(`/api/v1/appointments/${id}/complete`, { method: 'PATCH' }),
  cancel: (id: number) => api<Appointment>(`/api/v1/appointments/${id}/cancel`, { method: 'PATCH' }),
}

export const recordsApi = {
  byPatient: (patientId: number, query: Filters) => api<Page<MedicalRecord>>(`/api/v1/medical-records/patients/${patientId}`, { query }),
  mine: (query: Filters) => api<Page<MedicalRecord>>('/api/v1/medical-records/me', { query }),
  create: (body: Body) => api<MedicalRecord>('/api/v1/medical-records', { method: 'POST', body }),
  update: (id: number, body: Body) => api<MedicalRecord>(`/api/v1/medical-records/${id}`, { method: 'PATCH', body }),
}

export const hospitalizationsApi = {
  list: (query: Filters) => api<Page<Hospitalization>>('/api/v1/hospitalizations', { query }),
  mine: () => api<Hospitalization[]>('/api/v1/hospitalizations/me'),
  get: (id: number) => api<Hospitalization>(`/api/v1/hospitalizations/${id}`),
  usages: (id: number) => api<SupplyUsage[]>(`/api/v1/hospitalizations/${id}/supply-usages`),
  create: (body: Body) => api<Hospitalization>('/api/v1/hospitalizations', { method: 'POST', body }),
  discharge: (id: number) => api<Hospitalization>(`/api/v1/hospitalizations/${id}/discharge`, { method: 'PATCH' }),
  registerUsage: (body: { hospitalizationId: number; supplyId: number; quantityUsed: number }) =>
    api<SupplyUsage>('/api/v1/supply-usages', { method: 'POST', body }),
}

export const roomsApi = {
  list: (available?: boolean) => api<Room[]>('/api/v1/rooms', { query: { available } }),
  create: (body: Body) => api<Room>('/api/v1/rooms', { method: 'POST', body }),
  update: (id: number, body: Body) => api<Room>(`/api/v1/rooms/${id}`, { method: 'PATCH', body }),
}

export const suppliesApi = {
  list: (query: Filters) => api<Page<MedicalSupply>>('/api/v1/medical-supplies', { query }),
  lowStock: () => api<MedicalSupply[]>('/api/v1/medical-supplies/low-stock'),
  create: (body: Body) => api<MedicalSupply>('/api/v1/medical-supplies', { method: 'POST', body }),
  update: (id: number, body: Body) => api<MedicalSupply>(`/api/v1/medical-supplies/${id}`, { method: 'PATCH', body }),
  movements: (query: Filters) => api<Page<SupplyMovement>>('/api/v1/supply-movements', { query }),
  registerMovement: (body: { supplyId: number; type: string; quantity: number }) =>
    api<SupplyMovement>('/api/v1/supply-movements', { method: 'POST', body }),
}

export const receptionistsApi = {
  list: () => api<Receptionist[]>('/api/v1/receptionists'),
  create: (body: Body) => api<Receptionist>('/api/v1/receptionists', { method: 'POST', body }),
  update: (id: number, body: Body) => api<Receptionist>(`/api/v1/receptionists/${id}`, { method: 'PATCH', body }),
  deactivate: (id: number) => api<void>(`/api/v1/receptionists/${id}/deactivate`, { method: 'PATCH' }),
  activate: (id: number) => api<void>(`/api/v1/receptionists/${id}/activate`, { method: 'PATCH' }),
}

export const filesApi = {
  list: (patientId?: number) => api<FileInfo[]>('/api/v1/files', { query: { patientId } }),
  upload: (file: File, patientId?: number) => {
    const form = new FormData()
    form.append('file', file)
    return api<FileInfo>('/api/v1/files', { method: 'POST', body: form, query: { patientId } })
  },
  remove: (id: string) => api<void>(`/api/v1/files/${id}`, { method: 'DELETE' }),
}
