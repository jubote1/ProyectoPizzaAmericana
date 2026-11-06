-- Arnes de repeticion / sombra del motor de pedido (etapa 2). Esquema pos_central. Idempotente.
-- sombra_caso: un pedido historico convertido a la forma del contrato (solicitud) con lo que la tienda registro (esperado).
-- sombra_resultado: lo que dio el motor para cada caso en cada corrida, y como se compara con lo esperado.

CREATE TABLE IF NOT EXISTS pos_central.sombra_caso (
  idcaso bigint NOT NULL AUTO_INCREMENT,
  origen varchar(20) NOT NULL COMMENT 'TIENDA_HIST (pedido ya tomado en el POS) o BOT (payload del BOT)',
  idtienda int NOT NULL,
  idpedidotienda int NOT NULL,
  fechapedido datetime DEFAULT NULL,
  solicitud_json mediumtext NOT NULL,
  esperado_json mediumtext NOT NULL,
  subtotal_esperado double NOT NULL DEFAULT 0,
  lineas_esperadas int NOT NULL DEFAULT 0,
  fecha_creacion datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (idcaso),
  UNIQUE KEY uq_caso (origen, idtienda, idpedidotienda),
  KEY idx_fecha (fechapedido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

CREATE TABLE IF NOT EXISTS pos_central.sombra_resultado (
  idresultado bigint NOT NULL AUTO_INCREMENT,
  idcaso bigint NOT NULL,
  version_motor varchar(40) NOT NULL,
  fecha_corrida datetime NOT NULL,
  estado_motor varchar(20) NOT NULL,
  clasificacion varchar(24) NOT NULL COMMENT 'OK, TOTAL_DISTINTO, LINEAS_DISTINTAS, NO_IMPLEMENTADO, RECHAZADO, ERROR',
  subtotal_motor double DEFAULT NULL,
  diferencia double DEFAULT NULL,
  detalle_json mediumtext,
  PRIMARY KEY (idresultado),
  KEY idx_caso (idcaso),
  KEY idx_version (version_motor, clasificacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
