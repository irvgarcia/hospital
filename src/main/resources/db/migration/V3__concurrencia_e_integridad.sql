-- A8: proteccion contra escrituras concurrentes.

-- 1. Bloqueo optimista en insumos (@Version en MedicalSupply).
--    Dos peticiones que leen el mismo stock y lo descuentan a la vez ya no pisan el cambio de la otra:
--    la segunda falla con OptimisticLockingFailureException (409).
ALTER TABLE `medical_supplies` ADD COLUMN `version` bigint NOT NULL DEFAULT 0;

-- 2. Un doctor no puede tener dos citas activas que empiecen a la misma hora.
--    El servicio ya valida los solapamientos, pero entre esa validacion y el INSERT otra peticion
--    puede crear la misma cita; el indice es la ultima barrera (DataIntegrityViolationException, 409).
--    La ultima parte vale NULL para las citas canceladas, y MySQL no compara los NULL en un indice unico:
--    un horario cancelado se puede volver a reservar (misma regla que en AppointmentService).
--    Si esta sentencia falla con "Duplicate entry", la base ya tiene citas duplicadas: hay que revisarlas
--    (y cancelar o mover las sobrantes) antes de volver a ejecutar la migracion.
ALTER TABLE `appointments`
    ADD UNIQUE KEY `uk_appointments_doctor_slot`
        (`doctor_id`, `date`, `start_time`, ((CASE WHEN `status` <> 'CANCELLED' THEN 1 END)));
