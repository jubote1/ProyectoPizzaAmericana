-- ---------------------------------------------------------------------------
-- NOVEDADES DE BIOMETRIA: QUIEN REPORTA QUEDA IDENTIFICADO
--
-- Se corre en el CENTRAL (172.19.0.25), base GENERAL. Es idempotente.
--
-- Hasta ahora general.empleado_evento_novedad.reportado_por era un texto que la
-- persona escribia a mano en el POS: cualquiera podia poner cualquier nombre. Ahora
-- el POS pide identificarse con la huella (o con la clave rapida si el huellero de
-- la tienda no funciona) al guardar la novedad, y guarda:
--
--   id_reporta         el empleado identificado (general.empleado.id)
--   reporta_biometria  'S' si se identifico con huella, 'N' si fue con clave
--
-- reportado_por sigue existiendo y ahora lleva el nombre real de ese empleado, asi
-- que la pantalla del central que ya lo muestra no cambia. Las novedades anteriores
-- quedan con las dos columnas nuevas en NULL: se sabe que no se identificaron.
--
-- El POS tolera que estas columnas no existan todavia (guarda como antes), pero
-- mientras no se corra este script no queda registro de quien se identifico.
-- ---------------------------------------------------------------------------

USE general;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE empleado_evento_novedad ADD COLUMN id_reporta INT NULL COMMENT ''Empleado identificado que reporta, general.empleado.id''',
  'SELECT ''id_reporta ya existe'' AS nota')
  FROM information_schema.columns
  WHERE table_schema = 'general' AND table_name = 'empleado_evento_novedad' AND column_name = 'id_reporta');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE empleado_evento_novedad ADD COLUMN reporta_biometria CHAR(1) NULL COMMENT ''S = se identifico con huella, N = con clave rapida''',
  'SELECT ''reporta_biometria ya existe'' AS nota')
  FROM information_schema.columns
  WHERE table_schema = 'general' AND table_name = 'empleado_evento_novedad' AND column_name = 'reporta_biometria');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

-- Verificacion
SHOW COLUMNS FROM empleado_evento_novedad;
SELECT COUNT(*) AS novedades, SUM(id_reporta IS NOT NULL) AS con_reporta_identificado
  FROM empleado_evento_novedad;
