-- ---------------------------------------------------------------------------
-- Los domiciliarios TEMPORALES que estan dentro de cada tienda, en el central.
--
-- Se corre en el CENTRAL (172.19.0.25), base general. Idempotente.
--
-- POR QUE
--
-- El mapa de domiciliarios del central sabe si un domiciliario INTERNO esta en turno porque su
-- ingreso queda en general.empleado_evento (biometria). El temporal no deja nada ahi: su ingreso y
-- su salida viven solo en empleado_temporal_dia de la base LOCAL de cada tienda, asi que el mapa
-- nunca podia saber si ya ingreso ni si ya salio.
--
-- Esta tabla es el reflejo, en el central, de ese registro de cada tienda. Lo escribe el POS por el
-- servicio RegistrarEmpleadoTemporalDia, en dos momentos:
--   * al dar el INGRESO  -> una fila (horasalida vacia)
--   * al dar la SALIDA   -> la misma fila, con horasalida
-- Si la fila trae horasalida, el domiciliario salio.
--
-- LA LLAVE ES (idtienda, idinterno). El idinterno es el autonumerico de la tabla de CADA tienda, y
-- se repite de una tienda a otra: con el solo no alcanza (el mismo problema de los ids repetidos del
-- datamart). El campo id es el cupo "Domiciliario NN" del dia, no la persona, y tambien se repite.
--
-- version: la hora (milisegundos) del POS en que se genero ese estado de la fila. Si llega un envio
-- viejo despues de uno nuevo -un reintento atrasado-, el central lo ignora y no "reabre" a alguien que
-- ya salio.
--
-- anulado = 'S' cuando la tienda borro el registro (se corrigio un ingreso hecho por error).
--
-- NOMBRE: se llama _tienda para no chocar con ninguna otra tabla empleado_temporal_dia (hay una en cada
-- tienda y otra en el datamart, con otras columnas).
-- ---------------------------------------------------------------------------

USE general;

CREATE TABLE IF NOT EXISTS empleado_temporal_dia_tienda (
  idtienda        INT NOT NULL,
  idinterno       INT NOT NULL COMMENT 'Autonumerico de empleado_temporal_dia en la base local de ESA tienda',
  id              INT NOT NULL DEFAULT 0 COMMENT 'El cupo Domiciliario NN del dia (usuario de la tienda), no la persona',
  identificacion  VARCHAR(20) NOT NULL,
  clave_dom       VARCHAR(20) NOT NULL DEFAULT '' COMMENT 'Ultimos 6 digitos de la cedula: es la clave con que la app del domiciliario reporta su ubicacion',
  nombre          VARCHAR(100) NOT NULL DEFAULT '',
  telefono        VARCHAR(20) NOT NULL DEFAULT '',
  empresa         VARCHAR(50) NOT NULL DEFAULT '',
  idempresa       INT NULL,
  fecha_sistema   DATE NOT NULL COMMENT 'La jornada de la tienda, no el dia calendario',
  horaingreso     VARCHAR(10) NULL,
  horasalida      VARCHAR(10) NULL COMMENT 'Vacia mientras sigue dentro',
  observacion     VARCHAR(200) NOT NULL DEFAULT '',
  anulado         CHAR(1) NOT NULL DEFAULT 'N',
  version         BIGINT NOT NULL DEFAULT 0,
  recibido_en     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (idtienda, idinterno),
  KEY ix_etdt_clave (clave_dom, fecha_sistema),
  KEY ix_etdt_ident (identificacion, fecha_sistema),
  KEY ix_etdt_fecha (fecha_sistema, idtienda)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Reflejo en el central del ingreso y la salida de los domiciliarios temporales de cada tienda';

-- Verificacion
SELECT column_name, column_type
  FROM information_schema.columns
 WHERE table_schema = 'general' AND table_name = 'empleado_temporal_dia_tienda'
 ORDER BY ordinal_position;
