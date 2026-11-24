import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import type { Role } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ROLE_LABEL } from '../lib/format'

interface NavItem {
  to: string
  label: string
  roles: Role[]
}

const NAV: NavItem[] = [
  { to: '/', label: 'Inicio', roles: ['ADMIN', 'RECEPTIONIST', 'DOCTOR', 'PATIENT'] },
  // Paciente
  { to: '/agendar', label: 'Agendar cita', roles: ['PATIENT'] },
  { to: '/mis-citas', label: 'Mis citas', roles: ['PATIENT'] },
  { to: '/mi-historial', label: 'Mi historial', roles: ['PATIENT'] },
  { to: '/mis-hospitalizaciones', label: 'Hospitalizaciones', roles: ['PATIENT'] },
  { to: '/mis-archivos', label: 'Mis archivos', roles: ['PATIENT'] },
  { to: '/mi-perfil', label: 'Mi perfil', roles: ['PATIENT'] },
  // Doctor
  { to: '/agenda', label: 'Mi agenda', roles: ['DOCTOR'] },
  // Personal
  { to: '/citas', label: 'Citas', roles: ['ADMIN', 'RECEPTIONIST'] },
  { to: '/pacientes', label: 'Pacientes', roles: ['ADMIN', 'RECEPTIONIST', 'DOCTOR'] },
  { to: '/hospitalizaciones', label: 'Hospitalizaciones', roles: ['ADMIN', 'RECEPTIONIST', 'DOCTOR'] },
  { to: '/doctores', label: 'Doctores', roles: ['ADMIN', 'RECEPTIONIST'] },
  { to: '/salas', label: 'Salas', roles: ['ADMIN', 'RECEPTIONIST', 'DOCTOR'] },
  { to: '/especialidades', label: 'Especialidades', roles: ['ADMIN'] },
  { to: '/recepcionistas', label: 'Recepcionistas', roles: ['ADMIN'] },
  { to: '/inventario', label: 'Inventario', roles: ['ADMIN'] },
  { to: '/doctor/perfil', label: 'Mi perfil', roles: ['DOCTOR'] },
]

export function Layout() {
  const { account, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  if (!account) return null
  const items = NAV.filter((item) => item.roles.some((role) => account.roles.includes(role)))
  const roleLabel = account.roles.map((role) => ROLE_LABEL[role]).join(', ')

  const nav = (
    <nav className="flex flex-col gap-0.5 p-3">
      {items.map((item) => (
        <NavLink key={item.to} to={item.to} end={item.to === '/'} onClick={() => setOpen(false)}
          className={({ isActive }) =>
            `rounded-lg px-3 py-2 text-sm font-medium transition ${isActive ? 'bg-brand-50 text-brand-800' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'}`}>
          {item.label}
        </NavLink>
      ))}
    </nav>
  )

  return (
    <div className="flex min-h-full">
      <aside className="hidden w-60 shrink-0 border-r border-slate-200 bg-white lg:block">
        <Brand />
        {nav}
      </aside>
      {open && (
        <div className="fixed inset-0 z-40 bg-slate-900/30 lg:hidden" onClick={() => setOpen(false)}>
          <aside className="h-full w-64 bg-white shadow-xl" onClick={(e) => e.stopPropagation()}>
            <Brand />
            {nav}
          </aside>
        </div>
      )}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-14 items-center justify-between gap-3 border-b border-slate-200 bg-white/90 px-4 backdrop-blur">
          <button className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 lg:hidden" onClick={() => setOpen(true)} aria-label="Abrir menú">☰</button>
          <div className="flex-1" />
          <NavLink to="/cuenta" className="text-right leading-tight hover:opacity-80">
            <div className="text-sm font-medium text-slate-800">{account.firstName} {account.lastName}</div>
            <div className="text-xs text-slate-500">{roleLabel}</div>
          </NavLink>
          <button
            className="rounded-lg px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-100"
            onClick={async () => { await logout(); navigate('/login', { replace: true }) }}>
            Salir
          </button>
        </header>
        <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 sm:px-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export function Brand() {
  return (
    <div className="flex h-14 items-center gap-2 border-b border-slate-100 px-4">
      <img src="/favicon.svg" alt="" className="h-7 w-7" />
      <span className="text-base font-semibold text-slate-900">Hospital</span>
    </div>
  )
}
