-- Sombra en vivo del motor de pedido con el BOT (etapa 2, paquete 2.9). Idempotente.
-- sombra_vivo: por cada confirmacion que hace el BOT se guarda lo que habria calculado el motor nuevo, al lado de la
-- cotizacion del flujo viejo. Mas tarde se concilia con lo que la tienda registro.

CREATE TABLE IF NOT EXISTS pos_central.sombra_vivo (
  idsombra bigint NOT NULL AUTO_INCREMENT,
  fecha datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lead_id bigint NOT NULL,
  idlog_bot int DEFAULT NULL COMMENT 'fila de log_pedido_crmbot de la confirmacion',
  version_motor varchar(40) NOT NULL,
  estado_motor varchar(20) NOT NULL,
  avisos_traduccion int NOT NULL DEFAULT 0,
  subtotal_motor double DEFAULT NULL COMMENT 'productos, sin domicilio',
  subtotal_viejo double DEFAULT NULL COMMENT 'subtotalProductos de la cotizacion del flujo viejo',
  diferencia_vs_viejo double DEFAULT NULL,
  duracion_ms int DEFAULT NULL,
  solicitud_json mediumtext,
  resultado_json mediumtext,
  idpedido int DEFAULT NULL COMMENT 'pedido del central creado despues, si lo hubo',
  subtotal_tienda double DEFAULT NULL COMMENT 'lo que la tienda registro, sin domicilio',
  clasificacion varchar(24) DEFAULT NULL COMMENT 'resultado de comparar el motor con lo que la tienda registro',
  fecha_conciliacion datetime DEFAULT NULL,
  PRIMARY KEY (idsombra),
  KEY idx_lead (lead_id),
  KEY idx_fecha (fecha),
  KEY idx_pendientes (idpedido, fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

-- Interruptor: 'S' enciende la sombra, 'N' la apaga. Nace APAGADO. Se lee como maximo cada 30 segundos.
INSERT IGNORE INTO general.parametros (valorparametro, valornumerico, valortexto, valornumericod)
VALUES ('MOTORSOMBRABOT', 0, 'N', 0);
