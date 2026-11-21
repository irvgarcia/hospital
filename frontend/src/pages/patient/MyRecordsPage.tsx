import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { download } from '../../api/client'
import { recordsApi } from '../../api/endpoints'
import { RecordsList } from '../../components/RecordsList'
import { Button, Card, ErrorAlert, Loading, PageHeader, Pagination } from '../../components/ui'

export function MyRecordsPage() {
  const [page, setPage] = useState(0)
  const [downloading, setDownloading] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const records = useQuery({ queryKey: ['records', 'me', page], queryFn: () => recordsApi.mine({ page, size: 10 }) })

  async function downloadPdf() {
    setDownloading(true)
    setError(null)
    try {
      await download('/api/v1/patients/me/medical-record/pdf', 'expediente.pdf')
    } catch (e) {
      setError(e)
    } finally {
      setDownloading(false)
    }
  }

  return (
    <>
      <PageHeader title="Mi historial clínico" subtitle="Diagnósticos y tratamientos de tus consultas"
        actions={<Button variant="secondary" loading={downloading} onClick={downloadPdf}>Descargar expediente (PDF)</Button>} />
      <ErrorAlert error={error || records.error} className="mb-4" />
      <Card padded={false}>
        <div className="p-5">{records.isLoading ? <Loading /> : <RecordsList records={records.data?.content ?? []} />}</div>
        {records.data && <Pagination page={page} totalPages={records.data.page.totalPages} totalElements={records.data.page.totalElements} onChange={setPage} />}
      </Card>
    </>
  )
}
