-- ---------------------------------------------------------------------------
-- CUANDO EL CENTRAL NO LOGRA SUBIRLE EL ESTADO A LA TIENDA, QUE QUEDE ESCRITO
--
-- Se corre en el CENTRAL (172.19.0.25), esquema pizzaamericana. Idempotente.
--
-- QUE PASABA
--
-- DiDi avisa por webhook que el domiciliario recogio (delivery_status 140) o
-- que entrego (160). El central traduce eso en una llamada HTTP al servicio de
-- la tienda -DarSalidaDomicilioPlataforma y DarEntregaDomicilio- usando
-- tienda.url.
--
-- Esa llamada vivia dentro de un try cuyo catch hacia SOLO un
-- System.out.println. Si la tienda no contestaba: no se reintentaba, no se
-- anotaba en ninguna parte, y el pedido se quedaba sin subir de estado sin que
-- nada avisara. El unico rastro indirecto era que pedido.domiciliario no
-- quedaba en 1.
--
-- Medido del 28 al 30 de octubre de 2026, eso pasaba en 1 o 2 pedidos por
-- tienda por dia, repartido parejo entre America, Itagui, Envigado, Bello,
-- Pilarica, Piloto y Niquia. Poco, pero invisible.
--
-- COMO QUEDA
--
-- Cada cambio de estado que haya que llevarle a una tienda deja fila aqui, con
-- su resultado. Si falla, lo recoge el reintento que corre desde Servicios,
-- igual que ya se hace con los eventos de Bold.
--
-- LA LLAVE ES (orden, estado) Y NO UN CONSECUTIVO
--
-- DiDi reenvia eventos. Sin esa llave, un deliveryStatus repetido crearia una
-- segunda fila y la tienda recibiria dos veces el mismo cambio. Con ella, el
-- reenvio cae sobre la fila que ya existe: si estaba entregada no se vuelve a
-- mandar, y si estaba pendiente se reintenta, que es lo que se quiere.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS pizzaamericana.didi_estado_pendiente (
  idpendiente       BIGINT       NOT NULL AUTO_INCREMENT,
  idordencomercio   BIGINT       NOT NULL COMMENT 'order_id de DiDi',
  delivery_status   INT          NOT NULL COMMENT '140 recogio, 160 entrego',
  idtienda          INT          NOT NULL,
  numposheader      INT          NOT NULL COMMENT 'el pedido en la base de la tienda',
  entregado         TINYINT      NOT NULL DEFAULT 0,
  intentos          INT          NOT NULL DEFAULT 0,
  ultimo_error      VARCHAR(300) NULL,
  fecha_creacion    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_intento     DATETIME     NULL,
  fecha_entrega     DATETIME     NULL,
  PRIMARY KEY (idpendiente),
  UNIQUE KEY uk_orden_estado (idordencomercio, delivery_status),
  KEY idx_pendientes (entregado, intentos, fecha_creacion),
  KEY idx_tienda (idtienda, fecha_creacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Cambios de estado de DiDi que hay que llevarle a la tienda, con su resultado';

-- Como quedo.
SELECT COUNT(*) AS filas FROM pizzaamericana.didi_estado_pendiente;
