-- ---------------------------------------------------------------------------
-- El registro de lo que se le envia a cada persona por Brevo
--
-- Se ejecuta en el CENTRAL (172.19.0.25). Idempotente.
--
-- HOY NO QUEDA NINGUNA EVIDENCIA
--
-- Por Brevo salen correos y WhatsApp todos los dias y no se guarda nada: ni a
-- quien, ni que plantilla, ni cuando, ni si Brevo lo acepto. Si un cliente
-- reclama que le escribimos de mas, o que nunca le llego la promocion, no hay
-- como responder. Y sin esto tampoco se puede medir una campana: comparar a los
-- que recibieron contra los que no exige saber quienes recibieron.
--
-- UNA FILA POR DESTINATARIO, NO POR ENVIO
--
-- El envio es masivo -una llamada con cientos de correos- pero la pregunta que
-- se hace despues es de una persona: "a Fulano que le hemos mandado". Guardar
-- el lote obligaria a abrirlo cada vez. Son unas pocas filas mas y se responde
-- de inmediato en la vista 360.
--
-- SE GUARDA EL DESTINO TAL COMO SE USO
--
-- El correo y el telefono se copian, no se leen despues de crm.persona: un
-- cliente cambia de correo y la evidencia de a donde se le escribio ese dia
-- tiene que quedar fija.
--
-- Y SE RESUELVE LA PERSONA EN EL MOMENTO
--
-- idpersona se busca al insertar y no en la consulta, por dos razones: la
-- consulta de la vista 360 tiene que ser instantanea, y si manana el maestro
-- une dos personas, lo que se envio ese dia siguio yendo a quien fue. Queda en
-- NULL cuando el correo o el celular no corresponden a nadie del maestro, que
-- es informacion util por si sola: son envios a gente que el CRM no conoce.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS crm.envio_brevo (
  idenvio     BIGINT       NOT NULL AUTO_INCREMENT,
  idpersona   BIGINT       NULL,
  -- 'C' correo, 'W' whatsapp. Las mismas letras de plantilla_brevo.categoria,
  -- para no inventar un segundo vocabulario.
  canal       CHAR(1)      NOT NULL,
  destino     VARCHAR(100) NOT NULL,
  nombre      VARCHAR(120) NULL,
  idplantilla INT          NULL,
  asunto      VARCHAR(250) NULL,
  -- 'OK' si Brevo lo acepto, 'ERROR' si no. Aceptado no es lo mismo que
  -- entregado ni que leido: eso Brevo lo sabe, nosotros no.
  resultado   VARCHAR(10)  NOT NULL DEFAULT 'OK',
  detalle     VARCHAR(500) NULL,
  usuario     VARCHAR(50)  NULL,
  enviado_en  DATETIME     NOT NULL,
  PRIMARY KEY (idenvio),
  -- Este es el indice que usa la vista 360: por persona y por fecha.
  KEY idx_envio_persona (idpersona, enviado_en),
  KEY idx_envio_fecha (enviado_en),
  KEY idx_envio_destino (destino)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3
  COMMENT='Un envio de Brevo por destinatario. Es la evidencia de a quien se le escribio.';

-- Como quedo
SELECT TABLE_NAME, TABLE_ROWS FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='crm' AND TABLE_NAME='envio_brevo';
