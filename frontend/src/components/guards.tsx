import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import type { Role } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Loading } from './ui'

/** Exige sesion. Un paciente sin perfil va primero a completarlo. */
export function RequireAuth({ children, allowIncompleteProfile }: { children: ReactNode; allowIncompleteProfile?: boolean }) {
  const { account, loading } = useAuth()
  const location = useLocation()
  if (loading) return <Loading text="Cargando sesión…" />
  if (!account) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (!allowIncompleteProfile && account.roles.includes('PATIENT') && !account.profileCompleted) {
    return <Navigate to="/completar-perfil" replace />
  }
  return <>{children}</>
}

export function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { hasRole } = useAuth()
  if (!hasRole(...roles)) return <Navigate to="/" replace />
  return <>{children}</>
}
