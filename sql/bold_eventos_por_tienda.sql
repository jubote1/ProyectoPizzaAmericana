-- ---------------------------------------------------------------------------
-- Bold: a que tienda pertenece cada evento y si ya se le entrego a la tienda.
-- Base de datos: pizzaamericana (172.19.0.25). Idempotente.
--
-- Todas las tiendas cobran con UNA cuenta de Bold, pero cada sede tiene su
-- propio usuario y a ese usuario esta atado su datafono. El evento trae ese
-- usuario en data.user_id y data.seller.email; con eso se sabe la tienda.
--
--   bold_sede_tienda   correspondencia usuario de Bold -> tienda (una fila por
--                      usuario/sede). La llena una persona, no el sistema.
--   log_evento_bold    gana seller_email y bold_user_id (los datos del evento
--                      con que se busca la tienda), idtienda (la tienda ya
--                      resuelta) y el estado de la entrega a la tienda:
--                      entregado_tienda, intentos_entrega, fecha_entrega_tienda
--                      y error_entrega.
--
-- Un evento cuya sede no esta en bold_sede_tienda queda con idtienda NULL y sin
-- entregar; en cuanto se agregue la sede, el reintento lo entrega solo.
--
-- Correr ESTO ANTES de desplegar el war del central: el webhook ya escribe las
-- columnas nuevas y, sin ellas, responderia 500 (Bold reintenta despues, asi
-- que no se pierde nada, pero el log quedaria sin registrar mientras tanto).
-- ---------------------------------------------------------------------------

SET @faltan = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'log_evento_bold' AND column_name = 'bold_user_id');

SET @alterar = IF(@faltan = 0, 'ALTER TABLE log_evento_bold ADD COLUMN seller_email varchar(150) DEFAULT NULL AFTER terminal_id, ADD COLUMN bold_user_id varchar(80) DEFAULT NULL AFTER seller_email, ADD COLUMN idtienda int DEFAULT NULL AFTER bold_user_id, ADD COLUMN entregado_tienda tinyint(1) NOT NULL DEFAULT 0, ADD COLUMN intentos_entrega int NOT NULL DEFAULT 0, ADD COLUMN fecha_entrega_tienda datetime DEFAULT NULL, ADD COLUMN error_entrega varchar(200) DEFAULT NULL, ADD KEY idx_bold_user_id (bold_user_id), ADD KEY idx_entrega (entregado_tienda, firma_valida)', 'SELECT 1');

PREPARE ejecutar_alter FROM @alterar;

EXECUTE ejecutar_alter;

DEALLOCATE PREPARE ejecutar_alter;

-- Los eventos que ya estaban guardados: se les saca el usuario de su propio JSON.
-- (CASE y no solo el WHERE: MySQL no garantiza el orden de evaluacion, y un
-- cuerpo que no sea JSON haria fallar todo el UPDATE.)
UPDATE log_evento_bold
   SET seller_email = CASE WHEN JSON_VALID(json_evento) THEN JSON_UNQUOTE(JSON_EXTRACT(json_evento, '$.data.seller.email')) END,
       bold_user_id = CASE WHEN JSON_VALID(json_evento) THEN JSON_UNQUOTE(JSON_EXTRACT(json_evento, '$.data.user_id')) END
 WHERE bold_user_id IS NULL;

CREATE TABLE IF NOT EXISTS bold_sede_tienda (
  idbold_sede int NOT NULL AUTO_INCREMENT,
  idtienda int NOT NULL,
  bold_user_id varchar(80) DEFAULT NULL,
  seller_email varchar(150) DEFAULT NULL,
  descripcion varchar(100) DEFAULT NULL,
  activo tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (idbold_sede),
  UNIQUE KEY uk_bold_user_id (bold_user_id),
  UNIQUE KEY uk_seller_email (seller_email),
  KEY idx_idtienda (idtienda)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='Usuario de Bold de cada sede (y su datafono) -> tienda. Con esto se sabe de que tienda es cada evento.';

-- Manrique, el usuario de la primera prueba (datafono SonoQR 9280).
INSERT INTO bold_sede_tienda (idtienda, bold_user_id, seller_email, descripcion)
SELECT t.idtienda, 'c469f653-0a36-4054-b1e6-14fb8db5f8d4', 'datmanrique@pizzaamericana.com.co', 'SonoQR 9280'
  FROM tienda t
 WHERE t.nombre = 'Manrique'
   AND NOT EXISTS (SELECT 1 FROM bold_sede_tienda b WHERE b.bold_user_id = 'c469f653-0a36-4054-b1e6-14fb8db5f8d4');
