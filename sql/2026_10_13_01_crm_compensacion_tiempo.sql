-- ---------------------------------------------------------------------------
-- COMPENSACIONES POR TIEMPO (base crm del CENTRAL, 172.19.0.25)
--
-- Aditivo e idempotente: se puede correr dos veces. No usa DELIMITER ni procedimientos: corre tal
-- cual en HeidiSQL. Correrlo ANTES de desplegar el .war.
--
-- QUE ES
--
-- La pantalla "Compensaciones por tiempo" lista los pedidos a domicilio que se demoraron mas de N
-- minutos en un dia, y deja escoger a quienes se les manda una oferta con una disculpa. Esta tabla
-- es el libro de esas compensaciones:
--
--   * Evita compensar dos veces el mismo pedido: la llave unica es (idtienda, idpedidotienda). La
--     pantalla los muestra como "ya compensado" y no los deja volver a escoger.
--   * Guarda la fecha del pedido, que es la que el correo de disculpa pone en palabras ("el dia 12
--     de octubre"). El correo la busca aqui por (idenvio, idpersona): no hay que escribirla en cada
--     envio ni cambiar las tablas de campanas.
--   * Deja constancia de cuantos minutos se demoro, para medir despues si compensar sirvio.
--
-- El envio en si usa lo que ya existe: una campana y una tanda de crm.campana / campana_envio, con
-- la oferta ligada a la tanda y un codigo personal por destinatario.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS crm.compensacion_tiempo (
	idcompensacion  BIGINT       NOT NULL AUTO_INCREMENT,
	idtienda        INT          NOT NULL,
	idpedidotienda  INT          NOT NULL COMMENT 'El numero del pedido en la base de la tienda',
	fecha_pedido    DATE         NOT NULL,
	idpersona       BIGINT       NOT NULL,
	minutos         INT          NOT NULL COMMENT 'Minutos entre que entro el pedido y se entrego',
	prometido       INT          NOT NULL DEFAULT 0 COMMENT 'Minutos que se le prometieron',
	idoferta        INT          NOT NULL,
	idenvio         BIGINT       NOT NULL COMMENT 'La tanda de crm.campana_envio con la que se le escribio',
	usuario         VARCHAR(50)      NULL,
	creado_en       DATETIME     NOT NULL,
	PRIMARY KEY (idcompensacion),
	UNIQUE KEY uk_compensacion_pedido (idtienda, idpedidotienda),
	KEY idx_compensacion_envio (idenvio, idpersona),
	KEY idx_compensacion_fecha (fecha_pedido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci
  COMMENT='Pedidos demorados a los que se les mando una oferta de disculpa';

-- La pantalla en el catalogo (modulo 4 = CRM). El menu que se ve en produccion es el estatico
-- MenuCRM.html; esta fila deja el catalogo completo para cuando el menu dinamico se prenda.
INSERT INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Compensaciones por Tiempo', 4, 'CompensacionesTiempo.html', 56, 'S'
 WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla WHERE url_html = 'CompensacionesTiempo.html');

SELECT 'Listo. crm.compensacion_tiempo y la pantalla en el catalogo.' AS resultado;
