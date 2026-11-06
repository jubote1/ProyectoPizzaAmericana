-- ---------------------------------------------------------------------------
-- QUE PLANTILLA SE USO ANOCHE, Y A CUANTOS
--
-- Se corre en el CENTRAL (172.19.0.25), esquema general. Idempotente.
--
-- POR QUE
--
-- El 2026-11-05 salieron las plantillas VIEJAS del correo nocturno de puntos,
-- un dia despues de desplegar el cambio que las saca de la base. Para saber si
-- habia corrido el codigo nuevo o el viejo no habia NADA que mirar: ni una
-- tabla, ni una marca. Hubo que deducirlo por un defecto del codigo viejo -los
-- clientes parados justo en 99, 149, 199, 249 o 299 puntos no recibian correo-
-- y pidiendole a Brevo los numeros de plantilla.
--
-- Eso no se puede repetir. Con esta tabla la pregunta "corrio el nuevo?" se
-- responde con un SELECT:
--
--   SELECT * FROM general.log_correo_puntos ORDER BY idlog DESC LIMIT 10;
--
-- Si las plantillas son 77..83 corrio el nuevo; si son 2, 17..22, sigue el jar
-- viejo en el servidor.
--
-- QUE SE GUARDA Y QUE NO
--
-- Una fila por plantilla y por corrida: cuantos destinatarios le tocaron y como
-- le fue al envio. NO se guardan los correos de los clientes: para responder
-- "que plantilla salio" no hacen falta, y seria poner datos personales en una
-- bitacora tecnica que nadie vigila. Quien necesite el detalle por persona lo
-- tiene en Brevo.
--
-- `proceso` distingue el diario del reproceso: los dos mandan correos y cuando
-- algo sale raro lo primero que uno quiere saber es cual de los dos fue.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS general.log_correo_puntos (
  idlog             BIGINT       NOT NULL AUTO_INCREMENT,
  fecha             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  proceso           VARCHAR(60)  NOT NULL DEFAULT '' COMMENT 'DIARIO o REPROCESO',
  idplantilla_brevo INT          NOT NULL,
  puntos_desde      INT          NOT NULL,
  puntos_hasta      INT          NULL COMMENT 'NULL = sin tope',
  destinatarios     INT          NOT NULL DEFAULT 0,
  resultado         VARCHAR(20)  NOT NULL DEFAULT '' COMMENT 'ENVIADO, SIN DESTINATARIOS o ERROR',
  detalle           VARCHAR(300) NULL COMMENT 'el error, recortado, cuando lo hay',
  PRIMARY KEY (idlog),
  KEY idx_fecha (fecha),
  KEY idx_plantilla (idplantilla_brevo, fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Que plantilla de Brevo uso el correo nocturno de puntos en cada corrida';

-- Como quedo.
SELECT COUNT(*) AS filas FROM general.log_correo_puntos;
