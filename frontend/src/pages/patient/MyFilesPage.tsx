import { FilesPanel } from '../../components/FilesPanel'
import { Card, PageHeader } from '../../components/ui'

export function MyFilesPage() {
  return (
    <>
      <PageHeader title="Mis archivos" subtitle="Estudios, recetas y documentos de tu expediente" />
      <Card><FilesPanel /></Card>
    </>
  )
}
