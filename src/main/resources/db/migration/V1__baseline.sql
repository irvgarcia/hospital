-- Esquema base (version 1): el que generaba Hibernate con ddl-auto=update antes de la Fase 3.
-- Obtenido con mysqldump --no-data sobre una base nueva creada por Hibernate con las entidades de ese momento.
-- En bases que ya existian (creadas por ddl-auto=update), Flyway no ejecuta este script:
-- spring.flyway.baseline-on-migrate las marca como version 1 y aplica solo de la V2 en adelante.
-- No se debe modificar: cualquier cambio al esquema va en una migracion nueva.

CREATE TABLE `users` (
  `active` bit(1) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpnvtwliis6p05pn6i3ndjrqt2` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_enum` enum('ADMIN','DOCTOR','PATIENT','RECEPTIONIST') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `role_permissions` (
  `permission_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`permission_id`,`role_id`),
  KEY `FKn5fotdgk8d1xvo8nav9uv3muc` (`role_id`),
  CONSTRAINT `FKegdk29eiy7mdtefy5c7eirr6e` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`),
  CONSTRAINT `FKn5fotdgk8d1xvo8nav9uv3muc` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `user_roles` (
  `role_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`user_id`),
  KEY `FKhfh9dx7w3ubf1co1vdev94g3f` (`user_id`),
  CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `auth_tokens` (
  `expired` bit(1) NOT NULL,
  `revoked` bit(1) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `token_type` enum('ACCESS','REFRESH') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3wqaabxbp24ko89s7lkjxch2v` (`token_hash`),
  KEY `FKkhs4tpy3l5krnk87ykkmafeic` (`user_id`),
  CONSTRAINT `FKkhs4tpy3l5krnk87ykkmafeic` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `specialties` (
  `active` bit(1) NOT NULL,
  `cost` decimal(38,2) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKbhb8s9o5hv30lkbidtod9cixc` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `doctors` (
  `active` bit(1) NOT NULL,
  `register_date` date NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `specialty_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `phone_number` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKt1f6cueqyjwx5ghew9ar1exe3` (`user_id`),
  KEY `FKb4ymcpidvwfn4kybv4adfvxcm` (`specialty_id`),
  CONSTRAINT `FKb4ymcpidvwfn4kybv4adfvxcm` FOREIGN KEY (`specialty_id`) REFERENCES `specialties` (`id`),
  CONSTRAINT `FKe9pf5qtxxkdyrwibaevo9frtk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `doctor_schedules` (
  `day_of_week` tinyint NOT NULL,
  `end_time` time(6) NOT NULL,
  `start_time` time(6) NOT NULL,
  `doctor_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  KEY `FKqptts4sun4tpv6elafrnrfeup` (`doctor_id`),
  CONSTRAINT `FKqptts4sun4tpv6elafrnrfeup` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
  CONSTRAINT `doctor_schedules_chk_1` CHECK ((`day_of_week` between 0 and 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `patients` (
  `active` bit(1) NOT NULL,
  `date_of_birth` date NOT NULL,
  `height` double NOT NULL,
  `register_date` date NOT NULL,
  `smoker` bit(1) NOT NULL,
  `weight` double NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `address` varchar(255) NOT NULL,
  `allergies` varchar(255) NOT NULL,
  `blood_type` varchar(255) NOT NULL,
  `chronic_diseases` varchar(255) NOT NULL,
  `current_medications` varchar(255) NOT NULL,
  `emergency_contact` varchar(255) NOT NULL,
  `emergency_contact_name` varchar(255) NOT NULL,
  `emergency_contact_relation` varchar(255) NOT NULL,
  `gender` varchar(255) NOT NULL,
  `phone_number` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9tbsl3fmey0eofbm2xj69v4qs` (`user_id`),
  CONSTRAINT `FKuwca24wcd1tg6pjex8lmc0y7` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `appointments` (
  `date` date NOT NULL,
  `end_time` time(6) NOT NULL,
  `start_time` time(6) NOT NULL,
  `status` tinyint NOT NULL,
  `total` decimal(38,2) NOT NULL,
  `doctor_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patient_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmujeo4tymoo98cmf7uj3vsv76` (`doctor_id`),
  KEY `FK8exap5wmg8kmb1g1rx3by21yt` (`patient_id`),
  CONSTRAINT `FK8exap5wmg8kmb1g1rx3by21yt` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`id`),
  CONSTRAINT `FKmujeo4tymoo98cmf7uj3vsv76` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
  CONSTRAINT `appointments_chk_1` CHECK ((`status` between 0 and 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `rooms` (
  `available` bit(1) NOT NULL,
  `capacity` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1kuqhbfxed2e8t571uo82n545` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `hospitalizations` (
  `active` bit(1) NOT NULL,
  `admission_date` date NOT NULL,
  `discharge_date` date DEFAULT NULL,
  `doctor_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patient_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `reason` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6erp006ktdmhb6b5lkg7hfv7c` (`doctor_id`),
  KEY `FK8qxao4g05q1shpnaw8p35e8u` (`patient_id`),
  KEY `FKhhyx02i8iyqxwv6ovvnrdhky1` (`room_id`),
  CONSTRAINT `FK6erp006ktdmhb6b5lkg7hfv7c` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
  CONSTRAINT `FK8qxao4g05q1shpnaw8p35e8u` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`id`),
  CONSTRAINT `FKhhyx02i8iyqxwv6ovvnrdhky1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `medical_record` (
  `date` date NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patient_id` bigint NOT NULL,
  `diagnostics` varchar(255) NOT NULL,
  `treatment` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1naw9xcc0n0gnmcg5u1cerec5` (`patient_id`),
  CONSTRAINT `FKc0owwx1i1r9xf3h4uxw89rd1l` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `medical_supplies` (
  `active` bit(1) NOT NULL,
  `minimum_stock` int NOT NULL,
  `quantity` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `unity` enum('BOX','MILLIGRAM','MILLILITER') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKa6p9hj2nrreofhwn17geonqao` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `supply_movements` (
  `quantity_changed` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `performed_by` bigint NOT NULL,
  `supply_id` bigint NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `type` enum('IN','OUT') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK2si36i3q4m2aidapc0ykksiss` (`performed_by`),
  KEY `FK5683p7mr7vnsfu1qa5nccaqjc` (`supply_id`),
  CONSTRAINT `FK2si36i3q4m2aidapc0ykksiss` FOREIGN KEY (`performed_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FK5683p7mr7vnsfu1qa5nccaqjc` FOREIGN KEY (`supply_id`) REFERENCES `medical_supplies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `supply_usages` (
  `quantity_used` int NOT NULL,
  `hospitalization_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `supply_id` bigint NOT NULL,
  `used_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKdjy8cm8shmmdls1wuxbk1mvwi` (`hospitalization_id`),
  KEY `FKkm0xmjqbff6r37eediwurp0yy` (`supply_id`),
  CONSTRAINT `FKdjy8cm8shmmdls1wuxbk1mvwi` FOREIGN KEY (`hospitalization_id`) REFERENCES `hospitalizations` (`id`),
  CONSTRAINT `FKkm0xmjqbff6r37eediwurp0yy` FOREIGN KEY (`supply_id`) REFERENCES `medical_supplies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `files` (
  `id` binary(16) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `data` longblob,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- Datos de referencia: los mismos permisos y roles que crea BackendHospitalApplication al arrancar.
-- Se insertan aqui para que existan antes de cualquier migracion de datos (por ejemplo db/dev-data).
INSERT INTO `permissions` (`name`) VALUES ('CREATE'), ('READ'), ('UPDATE'), ('DELETE'), ('REFACTOR');

INSERT INTO `roles` (`role_enum`) VALUES ('ADMIN'), ('DOCTOR'), ('RECEPTIONIST'), ('PATIENT');

INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `roles` r
JOIN `permissions` p ON
     (r.`role_enum` = 'ADMIN')
  OR (r.`role_enum` = 'DOCTOR' AND p.`name` IN ('CREATE', 'READ', 'UPDATE'))
  OR (r.`role_enum` = 'RECEPTIONIST' AND p.`name` IN ('CREATE', 'READ'))
  OR (r.`role_enum` = 'PATIENT' AND p.`name` = 'READ');
