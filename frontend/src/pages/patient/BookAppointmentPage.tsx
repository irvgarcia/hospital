import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { Appointment } from '../../api/types'
import { BookingForm } from '../../components/BookingForm'
import { Button, Card, PageHeader, SuccessAlert } from '../../components/ui'
import { formatDateLong, formatTime } from '../../lib/format'

export function BookAppointmentPage() {
  const [booked, setBooked] = useState<Appointment | null>(null)
  return (
    <>
      <PageHeader title="Agendar cita" subtitle="Elige especialidad, doctor y un horario libre" />
      <Card>
        {booked ? (
          <div className="space-y-4">
            <SuccessAlert>
              Cita agendada para el <b>{formatDateLong(booked.date)}</b> a las <b>{formatTime(booked.startTime)}</b> con {booked.doctorName}.
            </SuccessAlert>
            <div className="flex gap-2">
              <Link to="/mis-citas"><Button>Ver mis citas</Button></Link>
              <Button variant="secondary" onClick={() => setBooked(null)}>Agendar otra</Button>
            </div>
          </div>
        ) : <BookingForm onBooked={setBooked} />}
      </Card>
    </>
  )
}
