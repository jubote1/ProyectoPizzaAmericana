-- Preguntas forzadas 6 a 10 en producto, en las bases del CENTRAL: pos_central (el modelo nuevo del pedido) y
-- tiendaamericana (la base del proyecto de tienda/bodega que vive en el central).
--
-- Mismo cambio que en las tiendas: idpreguntaforzada6..10 (INT, 0 = sin pregunta, despues de la 5). Idempotente y
-- tolerante: si un esquema o su tabla producto no existe, simplemente no hace nada ahi.
-- Verificacion al final: debe mostrar 10 columnas por cada esquema que exista.

-- ===== pos_central =====
SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'pos_central' AND TABLE_NAME = 'producto' AND COLUMN_NAME = 'idpreguntaforzada6');
SET @sql := IF(@existe = 0 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'pos_central' AND TABLE_NAME = 'producto') = 1,
  'ALTER TABLE pos_central.producto ADD COLUMN idpreguntaforzada6 INT DEFAULT 0 AFTER idpreguntaforzada5',
  'SELECT 1');
PREPARE st FROM @sql;
EXECUTE st;
DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'pos_central' AND TABLE_NAME = 'producto' AND COLUMN_NAME = 'idpreguntaforzada7');
SET @sql := IF(@existe = 0 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'pos_central' AND TABLE_NAME = 'producto') = 1,
  'ALTER TABLE pos_central.producto ADD COLUMN idpreguntaforzada7 INT DEFAULT 0 AFTER idpreguntaforzada6',
  'SELECT 1');
PREPARE st FROM @sql;
EXECUTE st;
DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'pos_central' AND TABLE_NAME = 'producto' AND COLUMN_NAME = 'idpreguntaforzada8');
SET @sql := IF(@existe = 0 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'pos_central' AND TABLE_NAME = 'producto') = 1,
  'ALTER TABLE pos_central.producto ADD COLUMN idpreguntaforzada8 INT DEFAULT 0 AFTER idpreguntaforzada7',
  'SELECT 1');
PREPARE st FROM @sql;
EXECUTE st;
DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'pos_central' AND TABLE_NAME = 'producto' AND COLUMN_NAME = 'idpreguntaforzada9');
SET @sql := IF(@existe = 0 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'pos_central' AND TABLE_NAME = 'producto') = 1,
  'ALTER TABLE pos_central.producto ADD COLUMN idpreguntaforzada9 INT DEFAULT 0 AFTER idpreguntaforzada8',
  'SELECT 1');
PREPARE st FROM @sql;
EXECUTE st;
DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'pos_central' AND TABLE_NAME = 'producto' AND COLUMN_NAME = 'idpreguntaforzada10');
SET @sql := IF(@existe = 0 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'pos_central' AND TABLE_NAME = 'producto') = 1,
  'ALTER TABLE pos_central.producto ADD COLUMN idpreguntaforzada10 INT DEFAULT 0 AFTER idpreguntaforzada9',
  'SELECT 1');
PREPARE st FROM @sql;
EXECUTE st;
DEALLOCATE PREPARE st;

-- ===== tiendaamericana =====
SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'tiendaamericana' AND TABLE_NAME = 'producto' AND COLUMN_NAME = 'idpreguntaforzada6');
SET @sql := IF(@existe = 0 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tiendaamericana' AND TABLE_NAME = 'producto') = 1,
  'ALTER TABLE tiendaamericana.producto ADD COLUMN idpreguntaforzada6 INT DEFAULT 0 AFTER idpreguntaforzada5',
  'SELECT 1');
PREPARE st FROM @sql;
EXECUTE st;
DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'tiendaamericana' AND TABLE_NAME = 'producto' AND COLUMN_NAME = 'idpreguntaforzada7');
SET @sql := IF(@existe = 0 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tiendaamericana' AND TABLE_NAME = 'producto') = 1,
  'ALTER TABLE tiendaamericana.producto ADD COLUMN idpreguntaforzada7 INT DEFAULT 0 AFTER idpreguntaforzada6',
  'SELECT 1');
PREPARE st FROM @sql;
EXECUTE st;
DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'tiendaamericana' AND TABLE_NAME = 'producto' AND COLUMN_NAME = 'idpreguntaforzada8');
SET @sql := IF(@existe = 0 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tiendaamericana' AND TABLE_NAME = 'producto') = 1,
  'ALTER TABLE tiendaamericana.producto ADD COLUMN idpreguntaforzada8 INT DEFAULT 0 AFTER idpreguntaforzada7',
  'SELECT 1');
PREPARE st FROM @sql;
EXECUTE st;
DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'tiendaamericana' AND TABLE_NAME = 'producto' AND COLUMN_NAME = 'idpreguntaforzada9');
SET @sql := IF(@existe = 0 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tiendaamericana' AND TABLE_NAME = 'producto') = 1,
  'ALTER TABLE tiendaamericana.producto ADD COLUMN idpreguntaforzada9 INT DEFAULT 0 AFTER idpreguntaforzada8',
  'SELECT 1');
PREPARE st FROM @sql;
EXECUTE st;
DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'tiendaamericana' AND TABLE_NAME = 'producto' AND COLUMN_NAME = 'idpreguntaforzada10');
SET @sql := IF(@existe = 0 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tiendaamericana' AND TABLE_NAME = 'producto') = 1,
  'ALTER TABLE tiendaamericana.producto ADD COLUMN idpreguntaforzada10 INT DEFAULT 0 AFTER idpreguntaforzada9',
  'SELECT 1');
PREPARE st FROM @sql;
EXECUTE st;
DEALLOCATE PREPARE st;

SELECT TABLE_SCHEMA, COUNT(*) AS columnas_de_preguntas FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA IN ('pos_central', 'tiendaamericana') AND TABLE_NAME = 'producto' AND COLUMN_NAME LIKE 'idpreguntaforzada%'
GROUP BY TABLE_SCHEMA;
