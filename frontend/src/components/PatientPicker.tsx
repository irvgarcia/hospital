import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { patientsApi } from '../api/endpoints'
import type { Patient } from '../api/types'
import { useDebounced } from '../lib/useDebounced'
import { Input } from './ui'

/** Buscador de pacientes activos por nombre o email. */
export function PatientPicker({ value, onChange }: { value: Patient | null; onChange: (patient: Patient | null) => void }) {
  const [search, setSearch] = useState('')
  const term = useDebounced(search)
  const { data, isFetching } = useQuery({
    queryKey: ['patients', 'picker', term],
    queryFn: () => patientsApi.list({ name: term, status: true, size: 8 }),
    enabled: !value && term.trim().length >= 2,
  })

  if (value) {
    return (
      <div className="flex items-center justify-between rounded-lg border border-brand-200 bg-brand-50 px-3 py-2 text-sm">
        <span><span className="font-medium">{value.fullName}</span> <span className="text-slate-500">· {value.email}</span></span>
        <button type="button" className="text-xs font-medium text-brand-700 hover:underline" onClick={() => onChange(null)}>Cambiar</button>
      </div>
    )
  }
  return (
    <div className="relative">
      <Input placeholder="Escribe nombre o email (mín. 2 letras)" value={search} onChange={(e) => setSearch(e.target.value)} />
      {term.trim().length >= 2 && (
        <div className="absolute z-10 mt-1 w-full overflow-hidden rounded-lg border border-slate-200 bg-white shadow-lg">
          {isFetching && <div className="px-3 py-2 text-sm text-slate-500">Buscando…</div>}
          {!isFetching && data?.content.length === 0 && <div className="px-3 py-2 text-sm text-slate-500">Sin resultados</div>}
          {data?.content.map((patient) => (
            <button type="button" key={patient.id} className="block w-full px-3 py-2 text-left text-sm hover:bg-slate-50"
              onClick={() => { onChange(patient); setSearch('') }}>
              <span className="font-medium">{patient.fullName}</span> <span className="text-slate-500">· {patient.email}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
