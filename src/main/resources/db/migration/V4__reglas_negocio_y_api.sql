-- Fases 4 a 7: columnas nuevas para las reglas de negocio y la API completa.

-- ------------------------------------------------------------------ citas
-- Motivo de consulta que el paciente escribe al agendar (opcional).
ALTER TABLE `appointments` ADD COLUMN `reason` varchar(500) NULL;

-- ------------------------------------------------------------------ hospitalizaciones (A6)
-- costPerDay se pedia en el DTO pero no se guardaba. Las hospitalizaciones anteriores quedan con costo 0.
-- total_cost se calcula al dar de alta.
ALTER TABLE `hospitalizations`
    ADD COLUMN `cost_per_day` decimal(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN `total_cost` decimal(12,2) NULL;

-- Una sala esta "disponible" mientras tenga camas libres (antes se cerraba con el primer paciente).
UPDATE `rooms` r
SET r.`available` = (SELECT COUNT(*) FROM `hospitalizations` h WHERE h.`room_id` = r.`id` AND h.`active` = 1) < r.`capacity`;

-- ------------------------------------------------------------------ historial clinico (M8)
-- Antes: un solo registro por paciente (indice unico en patient_id). Ahora: una entrada por cita completada,
-- con el doctor que la escribio. Las entradas antiguas quedan sin doctor ni cita.
RENAME TABLE `medical_record` TO `medical_records`;

-- La llave foranea de patient_id necesita un indice: se crea uno normal antes de borrar el unico.
ALTER TABLE `medical_records` ADD KEY `idx_medical_records_patient` (`patient_id`);

-- El nombre del indice unico lo genero Hibernate; se busca en information_schema por si varia entre bases.
SET @uk = (SELECT s.INDEX_NAME
           FROM information_schema.STATISTICS s
           WHERE s.TABLE_SCHEMA = DATABASE() AND s.TABLE_NAME = 'medical_records'
             AND s.COLUMN_NAME = 'patient_id' AND s.NON_UNIQUE = 0
           LIMIT 1);
SET @sql = IF(@uk IS NULL, 'DO 0', CONCAT('ALTER TABLE `medical_records` DROP INDEX `', @uk, '`'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `medical_records`
    MODIFY `diagnostics` varchar(1000) NOT NULL,
    MODIFY `treatment` varchar(1000) NOT NULL,
    ADD COLUMN `notes` varchar(1000) NULL,
    ADD COLUMN `doctor_id` bigint NULL,
    ADD COLUMN `appointment_id` bigint NULL,
    ADD UNIQUE KEY `uk_medical_records_appointment` (`appointment_id`),
    ADD CONSTRAINT `fk_medical_records_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
    ADD CONSTRAINT `fk_medical_records_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointments` (`id`);

-- ------------------------------------------------------------------ insumos (B2: unity -> unit)
ALTER TABLE `medical_supplies`
    CHANGE COLUMN `unity` `unit` enum('BOX','MILLIGRAM','MILLILITER','UNIT') NOT NULL;

-- ------------------------------------------------------------------ pacientes (M1)
-- gender pasa de texto libre a enum. Los valores conocidos se traducen; cualquier otro queda como OTHER.
UPDATE `patients`
SET `gender` = CASE UPPER(TRIM(`gender`))
                   WHEN 'F' THEN 'FEMALE' WHEN 'FEMALE' THEN 'FEMALE' WHEN 'FEMENINO' THEN 'FEMALE' WHEN 'MUJER' THEN 'FEMALE'
                   WHEN 'M' THEN 'MALE' WHEN 'MALE' THEN 'MALE' WHEN 'MASCULINO' THEN 'MALE' WHEN 'HOMBRE' THEN 'MALE'
                   ELSE 'OTHER'
               END;
ALTER TABLE `patients` MODIFY `gender` enum('FEMALE','MALE','OTHER') NOT NULL;

-- blood_type sigue siendo texto ("O+", "AB-"...). Se normaliza; un valor que no es un grupo valido se lee como nulo.
UPDATE `patients` SET `blood_type` = UPPER(REPLACE(`blood_type`, ' ', ''));

-- ------------------------------------------------------------------ archivos (M6)
ALTER TABLE `files`
    ADD COLUMN `size` bigint NOT NULL DEFAULT 0,
    ADD COLUMN `uploaded_at` datetime(6) NULL,
    ADD COLUMN `patient_id` bigint NULL,
    ADD COLUMN `uploaded_by` bigint NULL,
    ADD CONSTRAINT `fk_files_patient` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`id`),
    ADD CONSTRAINT `fk_files_uploaded_by` FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`);

UPDATE `files` SET `size` = COALESCE(LENGTH(`data`), 0);

-- ------------------------------------------------------------------ tokens (B7)
-- Vencimiento del JWT, para que la limpieza programada pueda borrar los tokens viejos.
ALTER TABLE `auth_tokens` ADD COLUMN `expires_at` datetime(6) NULL;
