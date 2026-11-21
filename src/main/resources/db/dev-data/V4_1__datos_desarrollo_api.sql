-- Datos de desarrollo ajustados al esquema de la V4 (solo perfil "local").
-- Completa lo que la V4 agrego: costos de hospitalizacion, motivos de consulta, historial ligado a citas
-- y doctores, unidades nuevas y un archivo de ejemplo. Mismas cuentas y contrasena (Hospital-dev-123).

-- ------------------------------------------------------------------ citas: motivo de consulta
UPDATE `appointments` a
JOIN `patients` p ON p.`id` = a.`patient_id`
JOIN `users` u ON u.`id` = p.`user_id`
SET a.`reason` = CASE u.`email`
                     WHEN 'ana.garcia@demo.local'     THEN 'Control de presion arterial'
                     WHEN 'luis.hernandez@demo.local' THEN 'Revision de glucosa y ajuste de tratamiento'
                     WHEN 'sofia.martinez@demo.local' THEN 'Tos persistente por las noches'
                     WHEN 'jorge.ruiz@demo.local'     THEN 'Seguimiento de neumonia'
                 END
WHERE u.`email` LIKE '%@demo.local';

-- ------------------------------------------------------------------ hospitalizaciones: costos
UPDATE `hospitalizations` SET `cost_per_day` = 1500.00 WHERE `cost_per_day` = 0;
UPDATE `hospitalizations`
SET `total_cost` = `cost_per_day` * GREATEST(1, DATEDIFF(`discharge_date`, `admission_date`))
WHERE `active` = 0 AND `discharge_date` IS NOT NULL;

-- ------------------------------------------------------------------ historial clinico
-- La entrada de Ana queda ligada a su cita completada con la Dra. Lopez.
UPDATE `medical_records` mr
JOIN `appointments` a ON a.`patient_id` = mr.`patient_id` AND a.`status` = 'COMPLETED' AND a.`date` = mr.`date`
SET mr.`appointment_id` = a.`id`, mr.`doctor_id` = a.`doctor_id`,
    mr.`notes` = 'Paciente estable; refiere cefaleas ocasionales'
WHERE mr.`appointment_id` IS NULL;

-- Luis: entrada de su cita completada con el Dr. Ramirez. La de Jorge queda sin entrada (para probar la captura).
INSERT INTO `medical_records` (`patient_id`, `doctor_id`, `appointment_id`, `date`, `diagnostics`, `treatment`, `notes`)
SELECT a.`patient_id`, a.`doctor_id`, a.`id`, a.`date`, 'Diabetes tipo 2 con control parcial',
       'Metformina 850 mg cada 12 h; dieta baja en azucares', 'Repetir hemoglobina glucosilada en 3 meses'
FROM `appointments` a
JOIN `patients` p ON p.`id` = a.`patient_id`
JOIN `users` u ON u.`id` = p.`user_id`
WHERE u.`email` = 'luis.hernandez@demo.local' AND a.`status` = 'COMPLETED';

-- ------------------------------------------------------------------ insumos
UPDATE `medical_supplies` SET `unit` = 'UNIT' WHERE `name` = 'Guantes de nitrilo';

-- ------------------------------------------------------------------ archivo de ejemplo (texto plano)
INSERT INTO `files` (`id`, `name`, `type`, `size`, `uploaded_at`, `patient_id`, `uploaded_by`, `data`)
SELECT UUID_TO_BIN(UUID()), 'indicaciones.txt', 'text/plain', LENGTH('Tomar Losartan 50 mg cada 24 horas.'),
       NOW() - INTERVAL 7 DAY, p.`id`, d.`id`, CAST('Tomar Losartan 50 mg cada 24 horas.' AS BINARY)
FROM `patients` p
JOIN `users` u ON u.`id` = p.`user_id` AND u.`email` = 'ana.garcia@demo.local'
JOIN `users` d ON d.`email` = 'dra.lopez@demo.local';
