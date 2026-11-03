-- =====================================================================
-- EMPLEADO EXENTO DEL CONTROL DE HORARIO EN EL PUNTO DE VENTA
-- Base de datos: general (la CENTRAL, 172.19.0.25). Idempotente.
--
-- El POS bloquea el ingreso de un empleado que no tiene turno hoy en
-- general.horario_planificado, o que llega mas de 10 minutos antes de la hora.
-- Hay casos legitimos: un domiciliario de una tienda que presta un servicio ese
-- dia en la Bodega, por ejemplo. Esta columna lo marca:
--
--   excepcion_horario_pv = 'S'  -> al marcar INGRESO el POS no revisa horario: no lo
--                                  bloquea ni le muestra avisos de horario.
--   excepcion_horario_pv = 'N'  -> se somete al control como siempre (valor por defecto).
--
-- Mismo estilo que administrador y medio_tiempo: VARCHAR(1) con S/N.
-- Se marca desde la pantalla de empleados del POS (casilla "Exento control de horario")
-- o por SQL:  UPDATE empleado SET excepcion_horario_pv = 'S' WHERE id = <id>;
--
-- El POS lee la columna con tolerancia: si una base general no la tiene todavia, trata
-- a todos como NO exentos y nada se rompe.
-- =====================================================================

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'empleado'
                   AND COLUMN_NAME = 'excepcion_horario_pv');
SET @sql := IF(@existe = 0,
  'ALTER TABLE empleado ADD COLUMN excepcion_horario_pv VARCHAR(1) NOT NULL DEFAULT ''N''',
  'SELECT ''empleado ya tiene excepcion_horario_pv'' AS paso1');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Como quedo: la columna, y cuantos empleados la tienen marcada (al inicio, 0).
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'empleado' AND COLUMN_NAME = 'excepcion_horario_pv';
SELECT SUM(excepcion_horario_pv = 'S') AS marcados, COUNT(*) AS empleados FROM empleado;
