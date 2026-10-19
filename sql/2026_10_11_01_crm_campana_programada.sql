-- ---------------------------------------------------------------------------
-- ENVIO DE PUBLICIDAD PROGRAMADO PARA UNA FECHA Y HORA FUTURA (base crm del CENTRAL)
--
-- Aditivo e idempotente: se puede correr dos veces. Correrlo ANTES de desplegar el .war.
-- No usa DELIMITER ni procedimientos: corre tal cual en HeidiSQL.
--
-- QUE ES UNA PROGRAMACION
--
-- Lo que la persona dejo listo en la pantalla de Envio de Publicidad -filtros, canal,
-- plantilla o contenido, tope, oferta- guardado para dispararse solo a la hora escogida.
-- Se guardan los PARAMETROS tal cual llegaron de la pantalla, no la lista de destinatarios:
--
--   * El publico se arma en el momento del envio, con los datos de ESE momento. Quien
--     autorizo el correo o compro entre la programacion y la hora queda bien clasificado, y
--     el descanso entre envios (ultima_fecha_publicidad) se aplica contra lo que realmente
--     se haya mandado mientras tanto.
--   * Las mismas validaciones del envio inmediato corren a la hora: la red de "esperados",
--     el tope del correo directo, que no haya otro correo directo en curso, y que la oferta
--     siga vigente. Si algo no cuadra, NO se manda y queda dicho por que.
--
-- ESTADOS
--
--   PROGRAMADA  espera su hora.
--   EJECUTANDO  el servidor la tomo y la esta disparando (una sola instancia la toma).
--   ENVIADA     se disparo; idenvio apunta a la tanda en crm.campana_envio.
--   FALLIDA     llego su hora y no se pudo disparar (resultado dice por que).
--   VENCIDA     el servidor estaba apagado o ocupado y paso la gracia: no se manda tarde.
--   CANCELADA   la cancelo una persona antes de su hora.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS crm.campana_programada (
	idprogramacion  BIGINT        NOT NULL AUTO_INCREMENT,
	idcampana       BIGINT            NULL COMMENT 'Campana escogida; NULL si es nueva y se crea al disparar',
	nombre          VARCHAR(150)      NULL COMMENT 'Nombre de la campana (la escogida o la nueva)',
	canal           CHAR(1)       NOT NULL COMMENT 'C correo Brevo, W WhatsApp Brevo, D correo directo',
	idoferta        INT           NOT NULL DEFAULT 0,
	esperados       INT           NOT NULL DEFAULT 0 COMMENT 'Cuantos conto la pantalla al programar',
	tope            INT           NOT NULL DEFAULT 0,
	resumen_filtros VARCHAR(2000)     NULL,
	parametros      MEDIUMTEXT    NOT NULL COMMENT 'JSON con los parametros de la pantalla',
	ejecutar_en     DATETIME      NOT NULL,
	estado          VARCHAR(12)   NOT NULL DEFAULT 'PROGRAMADA',
	idenvio         BIGINT            NULL COMMENT 'La tanda creada al dispararse',
	resultado       VARCHAR(500)      NULL,
	usuario         VARCHAR(50)       NULL,
	creado_en       DATETIME      NOT NULL,
	actualizado_en  DATETIME          NULL,
	PRIMARY KEY (idprogramacion),
	KEY idx_programada_listas (estado, ejecutar_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci
  COMMENT='Envios de publicidad programados para una fecha y hora futura';

-- Cuanto despues de su hora se sigue disparando una programacion si el servidor estaba apagado
-- o ocupado con otro correo directo. Pasado ese plazo queda VENCIDA y no se manda: una oferta
-- de las 8 de la manana llegando a las 4 de la tarde es peor que no llegar.
-- Va en general.parametros y no en el codigo porque es justo lo que operacion va a querer mover.
INSERT INTO general.parametros (valorparametro, valornumerico, valortexto)
SELECT 'PUBLICIDADPROGRAMADAGRACIAMIN', 120,
       'Minutos de gracia para disparar un envio programado que no pudo salir a su hora'
 WHERE NOT EXISTS (SELECT 1 FROM general.parametros
                    WHERE valorparametro = 'PUBLICIDADPROGRAMADAGRACIAMIN');

SELECT 'Listo. crm.campana_programada y parametro de gracia.' AS resultado;
