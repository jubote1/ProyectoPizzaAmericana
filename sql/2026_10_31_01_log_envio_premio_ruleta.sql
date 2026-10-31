-- ---------------------------------------------------------------------------
-- REGISTRO DE LOS CORREOS DE PREMIO DE LA RULETA, Y CUENTA PROPIA PARA ENVIARLOS
--
-- Idempotente: se puede correr dos veces. Corre tal cual en HeidiSQL.
--
-- POR QUE
--
-- En octubre quedaron 31 premios con su codigo generado y SIN aviso al cliente
-- (oferta_cliente.fecha_mensaje en NULL). Los 31 tenian correo y los 31 pasaban la
-- validacion del sistema, asi que no fue por direcciones malas: los envios fallaron.
--
-- Y NO SE PUDO SABER POR QUE. ControladorEnvioCorreo si distingue el motivo
-- -ENVIADO, DIRECCION_INVALIDA, FALLA_TRANSITORIA, SIN_DESTINATARIOS- pero RuletaCtrl
-- usaba la version booleana y lo botaba. Los fallos solo quedaban en memoria y salian
-- como una alerta por correo; no se guardaban en ninguna parte.
--
-- Esta tabla guarda cada intento, con su motivo. La proxima vez no hay que adivinar.
--
-- ADEMAS: CUENTA PROPIA PARA LOS PREMIOS
--
-- RuletaCtrl busca CUENTACORREOPREMIOS y, al no existir, caia a CUENTACORREOWOMPI, que
-- es tecnologia@pizzaamericana.com.co: la MISMA cuenta por la que salen los links de
-- pago y el aviso generico de ofertas. Todo competia por la misma cuota de Gmail.
--
-- Aqui se crea el parametro apuntando POR AHORA a la misma cuenta -no cambia nada
-- todavia-, para que el martes solo haya que cambiarle el valor a la cuenta de mercadeo
-- sin tocar codigo ni desplegar:
--
--   UPDATE general.parametros SET valortexto = 'mercadeo@pizzaamericana.com.co'
--    WHERE valorparametro = 'CUENTACORREOPREMIOS';
--   UPDATE general.parametros SET valortexto = '<la clave de aplicacion>'
--    WHERE valorparametro = 'CLAVECORREOPREMIOS';
--
-- La clave NO se escribe en este archivo: se copia de la que ya esta guardada.
--
-- PARA DEVOLVERLO
--
--   DROP TABLE pizzaamericana.log_envio_premio_ruleta;
--   DELETE FROM general.parametros WHERE valorparametro IN ('CUENTACORREOPREMIOS','CLAVECORREOPREMIOS');
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS pizzaamericana.log_envio_premio_ruleta (
  idlog           INT AUTO_INCREMENT PRIMARY KEY,
  idresultado     INT          NOT NULL COMMENT 'resultado_ruleta.idresultado',
  idofertacliente INT          NOT NULL DEFAULT 0,
  correo          VARCHAR(200) NOT NULL DEFAULT '',
  codigo          VARCHAR(50)  NOT NULL DEFAULT '',
  resultado       VARCHAR(30)  NOT NULL COMMENT 'ENVIADO, DIRECCION_INVALIDA, FALLA_TRANSITORIA, SIN_DESTINATARIOS, SIN_CUENTA, ERROR',
  detalle         VARCHAR(400) NOT NULL DEFAULT '' COMMENT 'de donde venia el correo y que se intento',
  intento         VARCHAR(20)  NOT NULL DEFAULT 'DISPERSION' COMMENT 'DISPERSION o REENVIO',
  usuario         VARCHAR(50)  NOT NULL DEFAULT '',
  fecha           DATETIME     NOT NULL,
  KEY ix_resultado (idresultado),
  KEY ix_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Cada intento de correo de premio de la ruleta, con el motivo cuando no sale';

-- La cuenta propia para los premios, por ahora con los mismos valores de Wompi.
INSERT INTO general.parametros (valorparametro, valortexto)
SELECT 'CUENTACORREOPREMIOS', p.valortexto
  FROM general.parametros p
 WHERE p.valorparametro = 'CUENTACORREOWOMPI'
   AND NOT EXISTS (SELECT 1 FROM general.parametros q WHERE q.valorparametro = 'CUENTACORREOPREMIOS');

INSERT INTO general.parametros (valorparametro, valortexto)
SELECT 'CLAVECORREOPREMIOS', p.valortexto
  FROM general.parametros p
 WHERE p.valorparametro = 'CLAVECORREOWOMPI'
   AND NOT EXISTS (SELECT 1 FROM general.parametros q WHERE q.valorparametro = 'CLAVECORREOPREMIOS');

-- Comprobacion: la cuenta debe quedar igual a la de Wompi, y la clave debe existir.
SELECT valorparametro,
       CASE WHEN valorparametro LIKE 'CLAVE%' THEN CONCAT('(', CHAR_LENGTH(valortexto), ' caracteres)')
            ELSE valortexto END AS valor
  FROM general.parametros
 WHERE valorparametro IN ('CUENTACORREOPREMIOS','CLAVECORREOPREMIOS','CUENTACORREOWOMPI')
 ORDER BY valorparametro;
