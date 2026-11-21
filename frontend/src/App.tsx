import { Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { RequireAuth, RequireRole } from './components/guards'
import { AccountPage } from './pages/AccountPage'
import { DoctorsPage } from './pages/admin/DoctorsPage'
import { InventoryPage } from './pages/admin/InventoryPage'
import { ReceptionistsPage } from './pages/admin/ReceptionistsPage'
import { RoomsPage } from './pages/admin/RoomsPage'
import { SpecialtiesPage } from './pages/admin/SpecialtiesPage'
import { CompleteProfilePage } from './pages/auth/CompleteProfilePage'
import { LoginPage } from './pages/auth/LoginPage'
import { RegisterPage } from './pages/auth/RegisterPage'
import { AgendaPage } from './pages/doctor/AgendaPage'
import { DoctorProfilePage } from './pages/doctor/DoctorProfilePage'
import { HomePage } from './pages/HomePage'
import { NotFoundPage } from './pages/NotFoundPage'
import { BookAppointmentPage } from './pages/patient/BookAppointmentPage'
import { MyAppointmentsPage } from './pages/patient/MyAppointmentsPage'
import { MyFilesPage } from './pages/patient/MyFilesPage'
import { MyHospitalizationsPage } from './pages/patient/MyHospitalizationsPage'
import { MyProfilePage } from './pages/patient/MyProfilePage'
import { MyRecordsPage } from './pages/patient/MyRecordsPage'
import { AppointmentsPage } from './pages/staff/AppointmentsPage'
import { HospitalizationDetailPage } from './pages/staff/HospitalizationDetailPage'
import { HospitalizationsPage } from './pages/staff/HospitalizationsPage'
import { PatientDetailPage } from './pages/staff/PatientDetailPage'
import { PatientsPage } from './pages/staff/PatientsPage'

const STAFF = ['ADMIN', 'RECEPTIONIST', 'DOCTOR'] as const

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />
      <Route path="/completar-perfil" element={<RequireAuth allowIncompleteProfile><CompleteProfilePage /></RequireAuth>} />

      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route index element={<HomePage />} />
        <Route path="cuenta" element={<AccountPage />} />

        <Route path="agendar" element={<RequireRole roles={['PATIENT']}><BookAppointmentPage /></RequireRole>} />
        <Route path="mis-citas" element={<RequireRole roles={['PATIENT']}><MyAppointmentsPage /></RequireRole>} />
        <Route path="mi-historial" element={<RequireRole roles={['PATIENT']}><MyRecordsPage /></RequireRole>} />
        <Route path="mis-hospitalizaciones" element={<RequireRole roles={['PATIENT']}><MyHospitalizationsPage /></RequireRole>} />
        <Route path="mis-archivos" element={<RequireRole roles={['PATIENT']}><MyFilesPage /></RequireRole>} />
        <Route path="mi-perfil" element={<RequireRole roles={['PATIENT']}><MyProfilePage /></RequireRole>} />

        <Route path="agenda" element={<RequireRole roles={['DOCTOR']}><AgendaPage /></RequireRole>} />
        <Route path="doctor/perfil" element={<RequireRole roles={['DOCTOR']}><DoctorProfilePage /></RequireRole>} />

        <Route path="citas" element={<RequireRole roles={['ADMIN', 'RECEPTIONIST']}><AppointmentsPage /></RequireRole>} />
        <Route path="pacientes" element={<RequireRole roles={[...STAFF]}><PatientsPage /></RequireRole>} />
        <Route path="pacientes/:id" element={<RequireRole roles={[...STAFF]}><PatientDetailPage /></RequireRole>} />
        <Route path="hospitalizaciones" element={<RequireRole roles={[...STAFF]}><HospitalizationsPage /></RequireRole>} />
        <Route path="hospitalizaciones/:id" element={<RequireRole roles={[...STAFF]}><HospitalizationDetailPage /></RequireRole>} />
        <Route path="salas" element={<RequireRole roles={[...STAFF]}><RoomsPage /></RequireRole>} />
        <Route path="doctores" element={<RequireRole roles={['ADMIN', 'RECEPTIONIST']}><DoctorsPage /></RequireRole>} />
        <Route path="especialidades" element={<RequireRole roles={['ADMIN']}><SpecialtiesPage /></RequireRole>} />
        <Route path="recepcionistas" element={<RequireRole roles={['ADMIN']}><ReceptionistsPage /></RequireRole>} />
        <Route path="inventario" element={<RequireRole roles={['ADMIN']}><InventoryPage /></RequireRole>} />

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
