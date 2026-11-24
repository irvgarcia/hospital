// Tipos de la API del backend (/api/v1). Coinciden con los DTOs de Spring.

export type Role = 'ADMIN' | 'RECEPTIONIST' | 'DOCTOR' | 'PATIENT'
export type AppointmentStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED'
export type Gender = 'FEMALE' | 'MALE' | 'OTHER'
export type BloodType = 'A+' | 'A-' | 'B+' | 'B-' | 'AB+' | 'AB-' | 'O+' | 'O-'
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'
export type Unit = 'BOX' | 'MILLIGRAM' | 'MILLILITER' | 'UNIT'
export type MovementType = 'IN' | 'OUT'

export interface Page<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  errors?: { field: string; message: string }[]
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
}

export interface Account {
  id: number
  email: string
  firstName: string
  lastName: string
  roles: Role[]
  patientId: number | null
  doctorId: number | null
  profileCompleted: boolean
}

export interface Schedule {
  id: number
  doctorId: number
  doctorName: string
  dayOfWeek: DayOfWeek
  startTime: string
  endTime: string
}

export interface ScheduleSlot {
  dayOfWeek: DayOfWeek
  startTime: string
  endTime: string
}

export interface Doctor {
  id: number
  userId: number
  firstName: string
  lastName: string
  fullName: string
  email: string
  specialtyId: number
  specialty: string
  consultationCost: number
  phoneNumber: string
  registerDate: string
  active: boolean
  schedules: Schedule[]
}

export interface AvailableSlot {
  startTime: string
  endTime: string
}

export interface Specialty {
  id: number
  name: string
  description: string
  cost: number
  doctorCount: number
  active: boolean
}

export interface PatientProfile {
  phoneNumber: string
  address: string
  dateOfBirth: string
  gender: Gender
  allergies: string
  bloodType: BloodType
  chronicDiseases: string
  currentMedications: string
  emergencyContact: string
  emergencyContactName: string
  emergencyContactRelation: string
  height: number
  weight: number
  smoker: boolean
}

export interface Patient extends PatientProfile {
  id: number
  userId: number
  firstName: string
  lastName: string
  fullName: string
  email: string
  registerDate: string
  active: boolean
}

export interface Appointment {
  id: number
  patientId: number
  patientName: string
  doctorId: number
  doctorName: string
  specialty: string
  date: string
  startTime: string
  endTime: string
  status: AppointmentStatus
  total: number
  reason: string | null
}

export interface MedicalRecord {
  id: number
  patientId: number
  patientName: string
  doctorId: number | null
  doctorName: string | null
  specialty: string | null
  appointmentId: number | null
  date: string
  diagnostics: string
  treatment: string
  notes: string | null
}

export interface Hospitalization {
  id: number
  patientId: number
  patientName: string
  doctorId: number
  doctorName: string
  roomId: number
  roomName: string
  admissionDate: string
  dischargeDate: string | null
  reason: string
  costPerDay: number
  totalCost: number | null
  active: boolean
}

export interface Room {
  id: number
  name: string
  capacity: number
  occupiedBeds: number
  available: boolean
}

export interface MedicalSupply {
  id: number
  name: string
  quantity: number
  minimumStock: number
  unit: Unit
  active: boolean
  lowStock: boolean
}

export interface SupplyMovement {
  id: number
  supplyId: number
  supplyName: string
  type: MovementType
  quantityChanged: number
  timestamp: string
  performedBy: string
}

export interface SupplyUsage {
  id: number
  hospitalizationId: number
  supplyId: number
  supplyName: string
  unit: Unit
  quantityUsed: number
  usedAt: string
}

export interface Receptionist {
  id: number
  email: string
  firstName: string
  lastName: string
  active: boolean
}

export interface FileInfo {
  id: string
  name: string
  type: string
  size: number
  url: string
  patientId: number | null
  patientName: string | null
  uploadedBy: string | null
  uploadedAt: string | null
}

export interface Dashboard {
  activePatients: number
  activeDoctors: number
  appointmentsToday: number
  pendingAppointments: number
  activeHospitalizations: number
  totalBeds: number
  occupiedBeds: number
  lowStockSupplies: number
}
