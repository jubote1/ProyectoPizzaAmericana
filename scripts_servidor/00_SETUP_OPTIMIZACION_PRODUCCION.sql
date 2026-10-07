-- =================================================================================
-- SCRIPT MAESTRO DE OPTIMIZACIÓN Y ESTRUCTURA PARA PRODUCCIÓN - PIZZA AMERICANA
-- Base de Datos: pizzaamericana
-- Ejecutar este archivo completo en MySQL Workbench o phpMyAdmin de Producción.
-- =================================================================================

USE pizzaamericana;

-- ---------------------------------------------------------------------------------
-- 1. TABLA DE ESTADO EN TIEMPO REAL: domiciliario_ubicacion_actual
-- (1 sola fila por domiciliario. Resuelve consultas instantáneas de flota en vivo)
-- ---------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `domiciliario_ubicacion_actual` (
  `clave_dom` varchar(50) NOT NULL,
  `idtienda` int(11) DEFAULT 0,
  `latitud` double DEFAULT NULL,
  `longitud` double DEFAULT NULL,
  `fecha` datetime DEFAULT NULL,
  `nombre_usuario` varchar(150) DEFAULT NULL,
  `estado` varchar(30) DEFAULT 'EN_TIENDA',
  `bateria` int(11) DEFAULT NULL,
  `velocidad` int(11) DEFAULT 0,
  `pedidos_activos` int(11) DEFAULT 0,
  `pedidos_detalle` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`clave_dom`),
  KEY `idx_domi_actual_fecha` (`fecha`),
  KEY `idx_domi_actual_tienda_fecha` (`idtienda`,`fecha`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Asegurar columnas si la tabla ya existía previamente
SET @dbname = DATABASE();

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'domiciliario_ubicacion_actual' AND COLUMN_NAME = 'estado') > 0,
  'SELECT 1',
  'ALTER TABLE domiciliario_ubicacion_actual ADD COLUMN estado VARCHAR(30) DEFAULT "EN_TIENDA"'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'domiciliario_ubicacion_actual' AND COLUMN_NAME = 'bateria') > 0,
  'SELECT 1',
  'ALTER TABLE domiciliario_ubicacion_actual ADD COLUMN bateria INT(11) DEFAULT NULL'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'domiciliario_ubicacion_actual' AND COLUMN_NAME = 'velocidad') > 0,
  'SELECT 1',
  'ALTER TABLE domiciliario_ubicacion_actual ADD COLUMN velocidad INT(11) DEFAULT 0'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'domiciliario_ubicacion_actual' AND COLUMN_NAME = 'pedidos_activos') > 0,
  'SELECT 1',
  'ALTER TABLE domiciliario_ubicacion_actual ADD COLUMN pedidos_activos INT(11) DEFAULT 0'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'domiciliario_ubicacion_actual' AND COLUMN_NAME = 'pedidos_detalle') > 0,
  'SELECT 1',
  'ALTER TABLE domiciliario_ubicacion_actual ADD COLUMN pedidos_detalle VARCHAR(255) DEFAULT NULL'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ---------------------------------------------------------------------------------
-- 2. TABLA DE PERSISTENCIA DE SESIONES Y DISPOSITIVOS: domiciliario_dispositivo_sesion
-- (Evita duplicidad de inicio de sesión y sobrevive reinicios de PM2 / Servidor)
-- ---------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `domiciliario_dispositivo_sesion` (
  `clave_dom` varchar(50) NOT NULL,
  `device_id` varchar(100) NOT NULL,
  `fcm_token` text DEFAULT NULL,
  `nombre_usuario` varchar(150) DEFAULT NULL,
  `estado` enum('AUTORIZADO','BLOQUEADO') DEFAULT 'AUTORIZADO',
  `ultima_conexion` datetime DEFAULT NULL,
  PRIMARY KEY (`clave_dom`),
  KEY `idx_disp_device` (`device_id`),
  KEY `idx_disp_estado` (`estado`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------------------
-- 3. TABLA DE AUDITORÍA Y ANOMALÍAS DE GPS: eventos_fraude_ubicacion
-- ---------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `eventos_fraude_ubicacion` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `clave_dom` varchar(50) DEFAULT NULL,
  `tipo_anomalia` varchar(50) DEFAULT NULL,
  `latitud` double DEFAULT NULL,
  `longitud` double DEFAULT NULL,
  `distancia` double DEFAULT NULL,
  `tiempo` double DEFAULT NULL,
  `fecha_registro` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_fraude_dom_fecha` (`clave_dom`,`fecha_registro`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------------------------------
-- 4. ÍNDICES COMPUESTOS DE ALTO RENDIMIENTO EN EL HISTÓRICO: ubicacion_domiciliario
-- (Reduce de 8+ segundos a <30ms la consulta de trayectos por repartidor y tienda)
-- ---------------------------------------------------------------------------------

-- Índice para consultar el trayecto del día de un domiciliario: (clave_dom, fecha)
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'ubicacion_domiciliario' AND INDEX_NAME = 'idx_ubi_dom_clave_fecha') > 0,
  'SELECT 1',
  'ALTER TABLE ubicacion_domiciliario ADD INDEX idx_ubi_dom_clave_fecha (clave_dom, fecha)'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice para consultar por tienda y rango de fechas: (idtienda, fecha)
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'ubicacion_domiciliario' AND INDEX_NAME = 'idx_ubi_dom_tienda_fecha') > 0,
  'SELECT 1',
  'ALTER TABLE ubicacion_domiciliario ADD INDEX idx_ubi_dom_tienda_fecha (idtienda, fecha)'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice simple por fecha: (fecha)
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'ubicacion_domiciliario' AND INDEX_NAME = 'idx_ubi_dom_fecha') > 0,
  'SELECT 1',
  'ALTER TABLE ubicacion_domiciliario ADD INDEX idx_ubi_dom_fecha (fecha)'
));
PREPARE stmt FROM @preparedStatement; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT '✅ OPTIMIZACIÓN Y ESTRUCTURA DE PIZZA AMERICANA COMPLETADA SATISFACTORIAMENTE' AS Resultado;
