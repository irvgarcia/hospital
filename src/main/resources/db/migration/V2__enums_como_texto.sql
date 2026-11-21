-- A1: appointments.status y doctor_schedules.day_of_week se guardaban como el ordinal del enum (tinyint).
-- Cambiar el orden de las constantes en Java corrompia los datos y filtrar por estado no funcionaba.
-- Ahora se guardan con el nombre (@Enumerated(EnumType.STRING)). Esta migracion convierte los valores existentes.
--
-- Ordinales anteriores:
--   AppointmentStatus:     0 = CANCELLED, 1 = PENDING, 2 = COMPLETED
--   java.time.DayOfWeek:   0 = MONDAY ... 6 = SUNDAY
--
-- Hibernate creaba un CHECK sobre cada columna (status BETWEEN 0 AND 2) que impide borrarla.
-- Su nombre lo pone MySQL y puede variar en bases creadas por ddl-auto=update, por eso se busca en information_schema.

-- ---------------------------------------------------------------- appointments.status
SET @chk = (SELECT cc.CONSTRAINT_NAME
            FROM information_schema.CHECK_CONSTRAINTS cc
            JOIN information_schema.TABLE_CONSTRAINTS tc
              ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
            WHERE tc.TABLE_SCHEMA = DATABASE() AND tc.TABLE_NAME = 'appointments'
              AND cc.CHECK_CLAUSE LIKE '%`status`%'
            LIMIT 1);
SET @sql = IF(@chk IS NULL, 'DO 0', CONCAT('ALTER TABLE `appointments` DROP CHECK `', @chk, '`'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `appointments` ADD COLUMN `status_new` enum('CANCELLED','COMPLETED','PENDING') NULL;

UPDATE `appointments`
SET `status_new` = CASE `status`
                     WHEN 0 THEN 'CANCELLED'
                     WHEN 1 THEN 'PENDING'
                     WHEN 2 THEN 'COMPLETED'
                   END;

ALTER TABLE `appointments` DROP COLUMN `status`;
ALTER TABLE `appointments` CHANGE COLUMN `status_new` `status` enum('CANCELLED','COMPLETED','PENDING') NOT NULL;

-- ---------------------------------------------------------------- doctor_schedules.day_of_week
SET @chk = (SELECT cc.CONSTRAINT_NAME
            FROM information_schema.CHECK_CONSTRAINTS cc
            JOIN information_schema.TABLE_CONSTRAINTS tc
              ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
            WHERE tc.TABLE_SCHEMA = DATABASE() AND tc.TABLE_NAME = 'doctor_schedules'
              AND cc.CHECK_CLAUSE LIKE '%`day_of_week`%'
            LIMIT 1);
SET @sql = IF(@chk IS NULL, 'DO 0', CONCAT('ALTER TABLE `doctor_schedules` DROP CHECK `', @chk, '`'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `doctor_schedules`
    ADD COLUMN `day_of_week_new` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') NULL;

UPDATE `doctor_schedules`
SET `day_of_week_new` = ELT(`day_of_week` + 1,
                            'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY');

ALTER TABLE `doctor_schedules` DROP COLUMN `day_of_week`;
ALTER TABLE `doctor_schedules`
    CHANGE COLUMN `day_of_week_new` `day_of_week`
        enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') NOT NULL;
