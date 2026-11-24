import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { download } from '../api/client'
import { filesApi } from '../api/endpoints'
import { useAuth } from '../auth/AuthContext'
import { formatDateTime, formatSize } from '../lib/format'
import { Button, ConfirmModal, ErrorAlert, Loading, Table } from './ui'

/** Archivos de un paciente (patientId) o del paciente autenticado (sin patientId). */
export function FilesPanel({ patientId }: { patientId?: number }) {
  const { account, hasRole } = useAuth()
  const queryClient = useQueryClient()
  const input = useRef<HTMLInputElement>(null)
  const [toDelete, setToDelete] = useState<string | null>(null)
  const [downloadError, setDownloadError] = useState<unknown>(null)
  const key = ['files', patientId ?? 'me']
  const files = useQuery({ queryKey: key, queryFn: () => filesApi.list(patientId) })
  const upload = useMutation({
    mutationFn: (file: File) => filesApi.upload(file, patientId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['files'] }),
  })
  const remove = useMutation({
    mutationFn: (id: string) => filesApi.remove(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['files'] }); setToDelete(null) },
  })

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-xs text-slate-500">PDF, PNG, JPG, WEBP o TXT de hasta 5 MB.</p>
        <input ref={input} type="file" className="hidden" accept=".pdf,.png,.jpg,.jpeg,.webp,.txt"
          onChange={(e) => { const f = e.target.files?.[0]; if (f) upload.mutate(f); e.target.value = '' }} />
        <Button size="sm" loading={upload.isPending} onClick={() => input.current?.click()}>Subir archivo</Button>
      </div>
      <ErrorAlert error={upload.error || files.error || downloadError} />
      {files.isLoading ? <Loading /> : (
        <Table rows={files.data ?? []} rowKey={(f) => f.id} empty="Sin archivos"
          columns={[
            { header: 'Nombre', cell: (f) => <span className="font-medium">{f.name}</span> },
            { header: 'Tamaño', cell: (f) => formatSize(f.size) },
            { header: 'Subido por', cell: (f) => f.uploadedBy ?? '—' },
            { header: 'Fecha', cell: (f) => formatDateTime(f.uploadedAt) },
            {
              header: '', className: 'text-right', cell: (f) => (
                <div className="flex justify-end gap-1">
                  <Button size="sm" variant="ghost" onClick={() => download(f.url, f.name).catch(setDownloadError)}>Descargar</Button>
                  {(hasRole('ADMIN') || f.uploadedBy === `${account?.firstName} ${account?.lastName}`) && (
                    <Button size="sm" variant="ghost" className="text-red-600" onClick={() => setToDelete(f.id)}>Borrar</Button>
                  )}
                </div>
              ),
            },
          ]} />
      )}
      <ConfirmModal open={!!toDelete} danger title="Borrar archivo" message="El archivo se borrará de forma permanente." confirmText="Borrar"
        loading={remove.isPending} error={remove.error} onConfirm={() => toDelete && remove.mutate(toDelete)} onClose={() => setToDelete(null)} />
    </div>
  )
}
