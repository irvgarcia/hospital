import type { AppointmentStatus } from '../api/types'
import { STATUS_LABEL } from '../lib/format'
import { Badge } from './ui'

const COLORS: Record<AppointmentStatus, 'yellow' | 'green' | 'gray'> = { PENDING: 'yellow', COMPLETED: 'green', CANCELLED: 'gray' }

export function StatusBadge({ status }: { status: AppointmentStatus }) {
  return <Badge color={COLORS[status]}>{STATUS_LABEL[status]}</Badge>
}

export function ActiveBadge({ active, on = 'Activo', off = 'Inactivo' }: { active: boolean; on?: string; off?: string }) {
  return <Badge color={active ? 'green' : 'gray'}>{active ? on : off}</Badge>
}
