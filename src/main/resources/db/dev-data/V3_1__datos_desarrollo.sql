-- Datos de prueba para desarrollo. SOLO se cargan con el perfil "local"
-- (spring.flyway.locations incluye classpath:db/dev-data unicamente en application-local.properties).
--
-- Version 3.1: se aplica despues de la V3 del esquema y antes de cualquier migracion futura (V4, V5...),
-- asi el orden es el mismo en una base nueva y en una que ya la tenia. Si el esquema cambia mas adelante,
-- los datos se ajustan con otra migracion en esta carpeta con la version siguiente (por ejemplo V6_1__...).
--
-- Flyway la ejecuta una sola vez por base. Para recargarla desde cero:
--   docker compose down -v && docker compose up -d
--
-- Todos los usuarios tienen la contrasena: Hospital-dev-123 (hash BCrypt precalculado, fuerza 10).
--   ADMIN          admin@demo.local
--   RECEPTIONIST   recepcion@demo.local, recepcion2@demo.local
--   DOCTOR         dra.lopez@demo.local (Cardiologia), dr.ramirez@demo.local (Medicina General),
--                  dra.torres@demo.local (Pediatria), dr.mendez@demo.local (Dermatologia, inactivo)
--   PATIENT        ana.garcia@demo.local, luis.hernandez@demo.local, sofia.martinez@demo.local,
--                  jorge.ruiz@demo.local
--
-- Las fechas son relativas al dia en que se ejecuta (citas futuras siempre en el futuro) y caen en lunes a
-- viernes, dentro del horario de cada doctor. Ningun paciente tiene mas de una cita PENDING.

SET @pwd = '$2a$10$8IGGGbcCYu0m9FuDPuXWiuzgwi4hpC4cfWChnjnByU9DkFADBoWIy';
-- WEEKDAY(): lunes = 0. Las variables guardan la fecha como texto: al usarlas se convierten con DATE()
-- (sin eso MySQL falla por mezcla de collations en los UNION).
SET @next_monday = CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY;
SET @prev_monday = CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY;

-- ------------------------------------------------------------------ usuarios
INSERT INTO `users` (`active`, `email`, `first_name`, `last_name`, `password`) VALUES
    (1, 'admin@demo.local',          'Laura',    'Admin',     @pwd),
    (1, 'recepcion@demo.local',      'Carmen',   'Vega',      @pwd),
    (1, 'recepcion2@demo.local',     'Raul',     'Ortega',    @pwd),
    (1, 'dra.lopez@demo.local',      'Maria',    'Lopez',     @pwd),
    (1, 'dr.ramirez@demo.local',     'Pedro',    'Ramirez',   @pwd),
    (1, 'dra.torres@demo.local',     'Elena',    'Torres',    @pwd),
    (0, 'dr.mendez@demo.local',      'Hugo',     'Mendez',    @pwd),
    (1, 'ana.garcia@demo.local',     'Ana',      'Garcia',    @pwd),
    (1, 'luis.hernandez@demo.local', 'Luis',     'Hernandez', @pwd),
    (1, 'sofia.martinez@demo.local', 'Sofia',    'Martinez',  @pwd),
    (1, 'jorge.ruiz@demo.local',     'Jorge',    'Ruiz',      @pwd);

INSERT INTO `user_roles` (`user_id`, `role_id`)
SELECT u.`id`, r.`id`
FROM `users` u
JOIN `roles` r ON r.`role_enum` = CASE
        WHEN u.`email` = 'admin@demo.local'        THEN 'ADMIN'
        WHEN u.`email` LIKE 'recepcion%@demo.local' THEN 'RECEPTIONIST'
        WHEN u.`email` LIKE 'dr%@demo.local'        THEN 'DOCTOR'
        ELSE 'PATIENT'
    END
WHERE u.`email` LIKE '%@demo.local';

-- ------------------------------------------------------------------ especialidades y doctores
INSERT INTO `specialties` (`active`, `cost`, `description`, `name`) VALUES
    (1, 800.00, 'Diagnostico y tratamiento de enfermedades del corazon', 'Cardiologia'),
    (1, 450.00, 'Atencion medica general y preventiva',                  'Medicina General'),
    (1, 600.00, 'Atencion medica de ninos y adolescentes',              'Pediatria'),
    (1, 700.00, 'Enfermedades de la piel, cabello y unas',              'Dermatologia');

INSERT INTO `doctors` (`active`, `register_date`, `phone_number`, `user_id`, `specialty_id`)
SELECT d.`active`, CURDATE() - INTERVAL 1 YEAR, d.`phone`, u.`id`, s.`id`
FROM (SELECT 1 AS `active`, 'dra.lopez@demo.local' AS `email`, '5512345601' AS `phone`, 'Cardiologia' AS `specialty`
      UNION ALL SELECT 1, 'dr.ramirez@demo.local', '5512345602', 'Medicina General'
      UNION ALL SELECT 1, 'dra.torres@demo.local', '5512345603', 'Pediatria'
      UNION ALL SELECT 0, 'dr.mendez@demo.local',  '5512345604', 'Dermatologia') d
JOIN `users` u ON u.`email` = d.`email`
JOIN `specialties` s ON s.`name` = d.`specialty`;

-- Lopez y Ramirez: lunes a viernes 08:00-14:00. Torres: lunes, miercoles y viernes 15:00-20:00.
-- Mendez (inactivo): martes y jueves 09:00-13:00.
INSERT INTO `doctor_schedules` (`doctor_id`, `day_of_week`, `start_time`, `end_time`)
SELECT doc.`id`, h.`day`, h.`start_time`, h.`end_time`
FROM (SELECT 'dra.lopez@demo.local' AS `email`, 'MONDAY' AS `day`, '08:00' AS `start_time`, '14:00' AS `end_time`
      UNION ALL SELECT 'dra.lopez@demo.local',  'TUESDAY',   '08:00', '14:00'
      UNION ALL SELECT 'dra.lopez@demo.local',  'WEDNESDAY', '08:00', '14:00'
      UNION ALL SELECT 'dra.lopez@demo.local',  'THURSDAY',  '08:00', '14:00'
      UNION ALL SELECT 'dra.lopez@demo.local',  'FRIDAY',    '08:00', '14:00'
      UNION ALL SELECT 'dr.ramirez@demo.local', 'MONDAY',    '08:00', '14:00'
      UNION ALL SELECT 'dr.ramirez@demo.local', 'TUESDAY',   '08:00', '14:00'
      UNION ALL SELECT 'dr.ramirez@demo.local', 'WEDNESDAY', '08:00', '14:00'
      UNION ALL SELECT 'dr.ramirez@demo.local', 'THURSDAY',  '08:00', '14:00'
      UNION ALL SELECT 'dr.ramirez@demo.local', 'FRIDAY',    '08:00', '14:00'
      UNION ALL SELECT 'dra.torres@demo.local', 'MONDAY',    '15:00', '20:00'
      UNION ALL SELECT 'dra.torres@demo.local', 'WEDNESDAY', '15:00', '20:00'
      UNION ALL SELECT 'dra.torres@demo.local', 'FRIDAY',    '15:00', '20:00'
      UNION ALL SELECT 'dr.mendez@demo.local',  'TUESDAY',   '09:00', '13:00'
      UNION ALL SELECT 'dr.mendez@demo.local',  'THURSDAY',  '09:00', '13:00') h
JOIN `users` u ON u.`email` = h.`email`
JOIN `doctors` doc ON doc.`user_id` = u.`id`;

-- ------------------------------------------------------------------ pacientes
INSERT INTO `patients` (`active`, `user_id`, `phone_number`, `address`, `date_of_birth`, `gender`, `allergies`,
                        `blood_type`, `chronic_diseases`, `current_medications`, `emergency_contact`,
                        `emergency_contact_name`, `emergency_contact_relation`, `height`, `weight`,
                        `register_date`, `smoker`)
SELECT 1, u.`id`, p.`phone`, p.`address`, p.`birth`, p.`gender`, p.`allergies`, p.`blood`, p.`chronic`, p.`meds`,
       p.`ec_phone`, p.`ec_name`, p.`ec_relation`, p.`height`, p.`weight`, CURDATE() - INTERVAL 6 MONTH, p.`smoker`
FROM (SELECT 'ana.garcia@demo.local' AS `email`, '5598765401' AS `phone`, 'Av. Reforma 120, CDMX' AS `address`,
             DATE '1985-03-14' AS `birth`, 'FEMALE' AS `gender`, 'Penicilina' AS `allergies`, 'O+' AS `blood`,
             'Hipertension' AS `chronic`, 'Losartan 50 mg' AS `meds`, '5598765501' AS `ec_phone`,
             'Roberto Garcia' AS `ec_name`, 'Esposo' AS `ec_relation`, 1.62 AS `height`, 68.5 AS `weight`, 0 AS `smoker`
      UNION ALL SELECT 'luis.hernandez@demo.local', '5598765402', 'Calle Juarez 45, Puebla', DATE '1972-11-02',
             'MALE', 'Ninguna', 'A+', 'Diabetes tipo 2', 'Metformina 850 mg', '5598765502', 'Marta Hernandez', 'Hija',
             1.75, 82.0, 1
      UNION ALL SELECT 'sofia.martinez@demo.local', '5598765403', 'Blvd. Hidalgo 300, Queretaro', DATE '2015-07-21',
             'FEMALE', 'Mariscos', 'B-', 'Asma', 'Salbutamol inhalado', '5598765503', 'Claudia Martinez', 'Madre',
             1.30, 28.0, 0
      UNION ALL SELECT 'jorge.ruiz@demo.local', '5598765404', 'Col. Centro 12, Toluca', DATE '1960-01-30',
             'MALE', 'Ninguna', 'AB+', 'Ninguna', 'Ninguno', '5598765504', 'Patricia Ruiz', 'Esposa',
             1.70, 77.3, 0) p
JOIN `users` u ON u.`email` = p.`email`;

-- ------------------------------------------------------------------ citas (todos los estados)
INSERT INTO `appointments` (`patient_id`, `doctor_id`, `date`, `start_time`, `end_time`, `status`, `total`)
SELECT pat.`id`, doc.`id`, a.`date`, a.`start_time`, a.`end_time`, a.`status`, s.`cost`
FROM (-- pendientes (futuras)
      SELECT 'ana.garcia@demo.local' AS `patient`, 'dra.lopez@demo.local' AS `doctor`, DATE(@next_monday) AS `date`,
             '09:00' AS `start_time`, '09:30' AS `end_time`, 'PENDING' AS `status`
      UNION ALL SELECT 'luis.hernandez@demo.local', 'dr.ramirez@demo.local', DATE(@next_monday) + INTERVAL 1 DAY,
             '10:00', '10:30', 'PENDING'
      UNION ALL SELECT 'sofia.martinez@demo.local', 'dra.torres@demo.local', DATE(@next_monday) + INTERVAL 2 DAY,
             '15:00', '15:30', 'PENDING'
      -- completadas (pasadas)
      UNION ALL SELECT 'ana.garcia@demo.local', 'dra.lopez@demo.local', DATE(@prev_monday), '09:00', '09:30', 'COMPLETED'
      UNION ALL SELECT 'luis.hernandez@demo.local', 'dr.ramirez@demo.local', DATE(@prev_monday) + INTERVAL 1 DAY,
             '11:00', '11:30', 'COMPLETED'
      UNION ALL SELECT 'jorge.ruiz@demo.local', 'dr.ramirez@demo.local', DATE(@prev_monday) + INTERVAL 3 DAY,
             '12:00', '12:30', 'COMPLETED'
      -- canceladas (una pasada y una futura; su horario sigue libre para otra cita)
      UNION ALL SELECT 'sofia.martinez@demo.local', 'dr.ramirez@demo.local', DATE(@prev_monday) + INTERVAL 2 DAY,
             '08:30', '09:00', 'CANCELLED'
      UNION ALL SELECT 'jorge.ruiz@demo.local', 'dra.lopez@demo.local', DATE(@next_monday) + INTERVAL 3 DAY,
             '09:00', '09:30', 'CANCELLED') a
JOIN `users` up ON up.`email` = a.`patient`
JOIN `patients` pat ON pat.`user_id` = up.`id`
JOIN `users` ud ON ud.`email` = a.`doctor`
JOIN `doctors` doc ON doc.`user_id` = ud.`id`
JOIN `specialties` s ON s.`id` = doc.`specialty_id`;

-- ------------------------------------------------------------------ historial clinico
INSERT INTO `medical_record` (`patient_id`, `date`, `diagnostics`, `treatment`)
SELECT pat.`id`, DATE(@prev_monday), 'Hipertension arterial controlada', 'Continuar Losartan 50 mg; control en 3 meses'
FROM `patients` pat JOIN `users` u ON u.`id` = pat.`user_id`
WHERE u.`email` = 'ana.garcia@demo.local';

-- ------------------------------------------------------------------ salas y hospitalizaciones
INSERT INTO `rooms` (`name`, `capacity`, `available`) VALUES
    ('Habitacion 101', 2, 1),
    ('Habitacion 102', 1, 1),
    ('Terapia Intensiva 1', 1, 0);

INSERT INTO `hospitalizations` (`patient_id`, `doctor_id`, `room_id`, `admission_date`, `discharge_date`,
                                `reason`, `active`)
SELECT pat.`id`, doc.`id`, r.`id`, h.`admission`, h.`discharge`, h.`reason`, h.`active`
FROM (SELECT 'jorge.ruiz@demo.local' AS `patient`, 'dr.ramirez@demo.local' AS `doctor`, 'Habitacion 101' AS `room`,
             CURDATE() - INTERVAL 2 DAY AS `admission`, CAST(NULL AS DATE) AS `discharge`,
             'Neumonia adquirida en la comunidad' AS `reason`, 1 AS `active`
      UNION ALL SELECT 'luis.hernandez@demo.local', 'dr.ramirez@demo.local', 'Habitacion 102',
             CURDATE() - INTERVAL 30 DAY, CURDATE() - INTERVAL 25 DAY, 'Descompensacion diabetica', 0) h
JOIN `users` up ON up.`email` = h.`patient`
JOIN `patients` pat ON pat.`user_id` = up.`id`
JOIN `users` ud ON ud.`email` = h.`doctor`
JOIN `doctors` doc ON doc.`user_id` = ud.`id`
JOIN `rooms` r ON r.`name` = h.`room`;

-- ------------------------------------------------------------------ insumos (uno por debajo del minimo)
INSERT INTO `medical_supplies` (`name`, `quantity`, `minimum_stock`, `unity`, `active`) VALUES
    ('Paracetamol 500 mg',        200, 50, 'BOX',        1),
    ('Solucion salina 0.9%',       40, 20, 'MILLILITER', 1),
    ('Ibuprofeno 400 mg',         100, 30, 'MILLIGRAM',  1),
    ('Guantes de nitrilo',         15, 20, 'BOX',        1),
    ('Amoxicilina 500 mg',          0, 10, 'BOX',        0);

-- Entradas iniciales registradas por recepcion (las cantidades de arriba ya las incluyen)
INSERT INTO `supply_movements` (`supply_id`, `type`, `quantity_changed`, `timestamp`, `performed_by`)
SELECT s.`id`, 'IN', s.`quantity` + IF(s.`name` = 'Paracetamol 500 mg', 2, 0), NOW() - INTERVAL 7 DAY, u.`id`
FROM `medical_supplies` s
JOIN `users` u ON u.`email` = 'recepcion@demo.local'
WHERE s.`quantity` > 0;

-- Uso en la hospitalizacion activa, con su movimiento de salida (como lo registra SupplyUsageService)
INSERT INTO `supply_usages` (`hospitalization_id`, `supply_id`, `quantity_used`, `used_at`)
SELECT h.`id`, s.`id`, 2, NOW() - INTERVAL 1 DAY
FROM `hospitalizations` h
JOIN `medical_supplies` s ON s.`name` = 'Paracetamol 500 mg'
WHERE h.`active` = 1;

INSERT INTO `supply_movements` (`supply_id`, `type`, `quantity_changed`, `timestamp`, `performed_by`)
SELECT s.`id`, 'OUT', 2, NOW() - INTERVAL 1 DAY, u.`id`
FROM `medical_supplies` s
JOIN `users` u ON u.`email` = 'dr.ramirez@demo.local'
WHERE s.`name` = 'Paracetamol 500 mg';
