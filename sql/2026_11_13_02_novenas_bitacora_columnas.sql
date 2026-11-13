-- Bitacora de contacto de Novenas: la tabla real no tenia las columnas que usa el codigo.
--
-- CampanaNovenasDAO inserta/lee idcliente, usuario_asesor, fecha_contacto y estado_contacto (asi esta en
-- 2026_11_09_01_campana_novenas_venta_empresarial.sql), pero la tabla tiendaamericana.campana_novenas_bitacora
-- que existia en el central tenia otras: idusuario, nombre_usuario y estado_gestion. Resultado: cada guardado de la
-- bitacora fallaba con "Unknown column 'idcliente'" (la pantalla decia "No se pudo guardar la bitacora").
--
-- Se adapta la tabla al codigo, SOLO si esta vacia (tenia 0 filas el 2026-11-13) y todavia no tiene las columnas
-- nuevas; y se recrea la vista pizzaamericana.campana_novenas_bitacora, que congelo la lista de columnas vieja.
-- Respaldo de como estaba: sql/respaldos/novenas_bitacora_antes_2026-11-13.sql
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.

SET @vacia := (SELECT COUNT(*) FROM tiendaamericana.campana_novenas_bitacora);
SET @tiene_nueva := (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = 'tiendaamericana' AND TABLE_NAME = 'campana_novenas_bitacora'
                       AND COLUMN_NAME = 'usuario_asesor');
SET @sql := IF(@vacia = 0 AND @tiene_nueva = 0,
  'ALTER TABLE tiendaamericana.campana_novenas_bitacora
     DROP COLUMN idusuario, DROP COLUMN nombre_usuario, DROP COLUMN estado_gestion,
     ADD COLUMN idcliente INT NOT NULL AFTER idlead,
     ADD COLUMN usuario_asesor VARCHAR(50) NOT NULL AFTER idcliente,
     ADD COLUMN fecha_contacto DATETIME NOT NULL AFTER usuario_asesor,
     ADD COLUMN estado_contacto VARCHAR(50) NOT NULL AFTER fecha_contacto,
     ADD INDEX idx_nov_cliente (idcliente), ADD INDEX idx_nov_asesor (usuario_asesor), ADD INDEX idx_nov_fecha_contacto (fecha_contacto)',
  'SELECT ''tabla sin cambios (ya adaptada, o tiene filas)'' AS nota');
PREPARE st FROM @sql;
EXECUTE st;
DEALLOCATE PREPARE st;

CREATE OR REPLACE ALGORITHM=UNDEFINED DEFINER=`root`@`%` SQL SECURITY DEFINER VIEW pizzaamericana.campana_novenas_bitacora AS
SELECT idbitacora, idlead, idcliente, usuario_asesor, fecha_contacto, estado_contacto, fecha_tentativa_evento,
       pizzas_estimadas, observacion, creado_en
FROM tiendaamericana.campana_novenas_bitacora;

SELECT COLUMN_NAME FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'campana_novenas_bitacora' ORDER BY ORDINAL_POSITION;
