-- =====================================================================
-- DATAMART: BITACORA DE LA REPLICA Y TABLAS DEL ENRUTAMIENTO
-- Base de datos: datamart  (CENTRAL, 172.19.0.25)  -- se corre UNA vez
--
-- Para que sirve
--   1. replica_log: lo que paso con cada tabla de cada tienda en cada corrida de
--      ServicioReplicaPedidos. De ahi sale "ultimo dia bueno" en el correo de la replica.
--      Sin esta tabla la replica funciona igual; solo pierde ese dato.
--   2. pedido_sugerencia, pedido_sugerencia_det y pedido_sugerencia_log: el enrutamiento de cada
--      tienda, para que el desempeno de domiciliarios del central pueda leerlo del datamart cuando
--      la tienda esta apagada. Los ids se repiten de una tienda a otra, por eso la llave siempre
--      lleva idtienda.
--   3. despacho_real.origen, despacho_real.pedido_sugerencia_id y despacho_real_det.origen_pedido:
--      las columnas que la tienda ya tiene y el datamart no. La replica copia por nombre de columna:
--      apenas existan aqui, empiezan a llegar solas.
--
-- Idempotente: se puede correr mas de una vez.
-- =====================================================================

USE datamart;

-- ---------------------------------------------------------------------
-- 1. Bitacora
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS replica_log (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  idtienda    INT NOT NULL,
  tabla       VARCHAR(40) NOT NULL,
  fecha_datos DATE NOT NULL,
  -- OK, YA, CERO, ERROR, NA
  estado      VARCHAR(8) NOT NULL,
  filas       INT NOT NULL DEFAULT 0,
  detalle     VARCHAR(400) NOT NULL DEFAULT '',
  ejecutado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_replica_log_tienda (idtienda, tabla, fecha_datos),
  KEY ix_replica_log_fecha (ejecutado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 2. Enrutamiento
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pedido_sugerencia (
  idtienda           INT NOT NULL,
  id                 INT NOT NULL,
  fecha_jornada      DATE NOT NULL,
  tipo_destino       VARCHAR(10) NOT NULL,
  id_domiciliario    INT NULL,
  turno_llegada      INT NULL,
  estado             VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
  origen             VARCHAR(12) NOT NULL DEFAULT 'MANUAL',
  id_usuario_sugiere INT NULL,
  creado_en          DATETIME NOT NULL,
  resuelto_en        DATETIME NULL,
  PRIMARY KEY (idtienda, id),
  KEY ix_sug_jornada (idtienda, fecha_jornada, estado),
  KEY ix_sug_domiciliario (id_domiciliario, fecha_jornada)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pedido_sugerencia_det (
  idtienda             INT NOT NULL,
  id                   INT NOT NULL,
  pedido_sugerencia_id INT NOT NULL,
  id_pedido            INT NOT NULL,
  orden_sugerido       INT NOT NULL DEFAULT 0,
  estado_det           VARCHAR(12) NOT NULL DEFAULT 'SUGERIDO',
  creado_en            DATETIME NOT NULL,
  PRIMARY KEY (idtienda, id),
  KEY ix_sug_det_cab (idtienda, pedido_sugerencia_id),
  KEY ix_sug_det_pedido (idtienda, id_pedido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pedido_sugerencia_log (
  idtienda             INT NOT NULL,
  id                   INT NOT NULL,
  pedido_sugerencia_id INT NOT NULL,
  id_pedido            INT NULL,
  accion               VARCHAR(20) NOT NULL,
  id_usuario_actua     INT NULL,
  motivo               VARCHAR(200) NULL,
  creado_en            DATETIME NOT NULL,
  PRIMARY KEY (idtienda, id),
  KEY ix_sug_log_cab (idtienda, pedido_sugerencia_id),
  KEY ix_sug_log_fecha (creado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 3. Columnas que faltan en los despachos. MySQL 8 no tiene ADD COLUMN IF NOT EXISTS:
--    se consulta information_schema antes de cada ALTER.
-- ---------------------------------------------------------------------
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE despacho_real ADD COLUMN origen VARCHAR(10) NULL',
  'SELECT ''despacho_real.origen ya existe'''
) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'despacho_real' AND column_name = 'origen');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE despacho_real ADD COLUMN pedido_sugerencia_id INT NULL',
  'SELECT ''despacho_real.pedido_sugerencia_id ya existe'''
) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'despacho_real' AND column_name = 'pedido_sugerencia_id');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE despacho_real_det ADD COLUMN origen_pedido VARCHAR(12) NULL',
  'SELECT ''despacho_real_det.origen_pedido ya existe'''
) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'despacho_real_det' AND column_name = 'origen_pedido');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------------------------------------------------------------------
-- Verificacion: deben salir las tres tablas nuevas y las tres columnas.
-- ---------------------------------------------------------------------
SELECT table_name FROM information_schema.tables
 WHERE table_schema = 'datamart'
   AND table_name IN ('replica_log', 'pedido_sugerencia', 'pedido_sugerencia_det', 'pedido_sugerencia_log');

SELECT table_name, column_name FROM information_schema.columns
 WHERE table_schema = 'datamart'
   AND ((table_name = 'despacho_real' AND column_name IN ('origen', 'pedido_sugerencia_id'))
     OR (table_name = 'despacho_real_det' AND column_name = 'origen_pedido'));

-- Para ver como esta la llave de las tablas de despachos (deberia incluir idtienda):
SHOW CREATE TABLE despacho_real;
SHOW CREATE TABLE despacho_real_det;
SHOW CREATE TABLE pedido;
