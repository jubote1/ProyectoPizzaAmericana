-- ---------------------------------------------------------------------------
-- Marcas de cliente: pantalla de administracion, "habilitadores" parametricos
-- y conexion con Rappi Cargo.
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente: se puede
-- correr dos veces sin duplicar nada. CORRER ANTES DE DESPLEGAR EL WAR.
--
-- QUE HABIA
--
-- cliente_alerta (telefono, tipo_alerta, mensaje): un texto por telefono que el
-- web y el POS muestran cuando se elige al cliente. Solo se llenaba a mano en la
-- base de datos. NO se toca su forma: las 5 filas que hay siguen funcionando igual.
--
-- QUE SE AGREGA
--
--  1. cliente_marca_tipo : los habilitadores. Cada uno dice si bloquea Rappi Cargo
--                          y que tan grave es. Se crean y se editan desde la pantalla
--                          "Marcas de cliente", no hay que tocar codigo para agregar uno.
--  2. cliente_marca      : que habilitadores tiene prendidos cada telefono.
--  3. cliente_marca_log  : quien marco o desmarco, cuando y que dijo.
--  4. cliente_alerta.usuario_modifica / fecha_modifica: quien toco el texto por ultima vez.
--
-- La marca va por TELEFONO, como cliente_alerta: un "cliente" del sistema es telefono
-- mas direccion y puede tener varias filas, y la persona es la misma.
--
-- Los dos habilitadores de partida:
--   NO_CARGO         : el cliente no desea que lo lleve una plataforma tercerizada.
--                      BLOQUEA Rappi Cargo (el sistema se niega a crear la orden).
--   POSIBLE_FRAUDE   : solo avisa, en grande, al tomar y al enviar el pedido. No bloquea.
-- Que bloquee o no se cambia desde la pantalla, sin codigo.
--
-- NO SE MARCA A NADIE AQUI. Los 5 clientes que ya tienen texto en cliente_alerta
-- (varios por fraude) quedan como estan: marcarlos es una decision de quien administra.
-- Todo el texto de este script va sin tildes a proposito, para que no dependa de
-- la codificacion con la que se corra; los textos se pueden editar desde la pantalla.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS cliente_marca_tipo (
  idtipo        VARCHAR(30)  NOT NULL,
  nombre        VARCHAR(80)  NOT NULL,
  descripcion   VARCHAR(300) NOT NULL DEFAULT '',
  severidad     CHAR(1)      NOT NULL DEFAULT 'I' COMMENT 'N negativa, I informativa, P positiva',
  bloquea_cargo CHAR(1)      NOT NULL DEFAULT 'N' COMMENT 'S: el sistema no deja enviar el pedido por Rappi Cargo',
  activo        CHAR(1)      NOT NULL DEFAULT 'S',
  orden         INT          NOT NULL DEFAULT 10,
  PRIMARY KEY (idtipo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Habilitadores que se le pueden prender a un cliente. Parametricos.';

CREATE TABLE IF NOT EXISTS cliente_marca (
  telefono    VARCHAR(50)  NOT NULL,
  idtipo      VARCHAR(30)  NOT NULL,
  fecha_marca DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  usuario     VARCHAR(100) NOT NULL DEFAULT '',
  PRIMARY KEY (telefono, idtipo),
  KEY ix_cliente_marca_tipo (idtipo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Habilitadores prendidos por telefono. Los administra la pantalla Marcas de cliente.';

CREATE TABLE IF NOT EXISTS cliente_marca_log (
  id       INT           NOT NULL AUTO_INCREMENT,
  fecha    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  usuario  VARCHAR(100)  NOT NULL DEFAULT '',
  telefono VARCHAR(50)   NOT NULL,
  accion   VARCHAR(20)   NOT NULL,
  detalle  VARCHAR(2000) NOT NULL DEFAULT '',
  PRIMARY KEY (id),
  KEY ix_cliente_marca_log_tel (telefono, fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Historial de cambios de marcas y del texto de alerta de un cliente.';

-- Quien toco por ultima vez el texto de cliente_alerta (solo si faltan las columnas).
SET @falta := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'cliente_alerta' AND column_name = 'usuario_modifica');
SET @sql := IF(@falta = 0,
  'ALTER TABLE cliente_alerta ADD COLUMN usuario_modifica VARCHAR(100) NULL, ADD COLUMN fecha_modifica DATETIME NULL',
  'SELECT 1');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

-- Los dos habilitadores de partida (INSERT IGNORE: no pisa lo que ya se haya editado).
INSERT IGNORE INTO cliente_marca_tipo (idtipo, nombre, descripcion, severidad, bloquea_cargo, activo, orden) VALUES
  ('NO_CARGO', 'No desea Rappi Cargo',
   'El cliente no desea que su pedido lo lleve una plataforma tercerizada (Rappi Cargo). El sistema no deja enviarlo por ahi.',
   'I', 'S', 'S', 1),
  ('POSIBLE_FRAUDE', 'Posible fraude',
   'Cliente con antecedentes de posible fraude. Revisar el pedido con cuidado antes de enviarlo.',
   'N', 'N', 'S', 2);

-- La pantalla entra al catalogo de seguridad, junto a la redencion manual de puntos.
-- Hoy se protege exigiendo sesion y rol de administrador en cada llamado del servidor.
INSERT INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Marcas de Cliente', m.idmodulo, 'MarcasCliente.html', 56, 'S'
  FROM pizzaamericana.menu_modulo m
 WHERE m.nombre = 'CRM'
   AND NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla ya WHERE ya.url_html = 'MarcasCliente.html');

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================
SELECT idtipo, nombre, severidad, bloquea_cargo, activo, orden FROM cliente_marca_tipo ORDER BY orden;
SELECT COUNT(*) AS clientes_con_texto_de_alerta FROM cliente_alerta;
SELECT p.idpantalla, p.nombre, p.url_html, p.activo FROM pizzaamericana.pantalla p WHERE p.url_html = 'MarcasCliente.html';
