-- ---------------------------------------------------------------------------
-- ENVIO DE PUBLICIDAD CON OFERTA (base crm del CENTRAL)
--
-- Aditivo e idempotente. Correrlo junto con ofertas_control_codigos.sql (esa es de
-- pizzaamericana; esta es de crm) y ANTES de desplegar el .war.
--
-- Una tanda de envio puede llevar una OFERTA: cada destinatario recibe su propio codigo
-- (pizzaamericana.oferta_cliente, ligado por idenvio) y el correo de la oferta.
--
--   campana_envio.idoferta            la oferta que lleva la tanda (NULL = envio sin oferta)
--   campana_destinatario.idofertacliente  el codigo que se le emitio a esa persona
-- ---------------------------------------------------------------------------

USE crm;

DROP PROCEDURE IF EXISTS tmp_agregar_columna_crm;
DELIMITER $$
CREATE PROCEDURE tmp_agregar_columna_crm(IN p_tabla VARCHAR(64), IN p_columna VARCHAR(64), IN p_definicion VARCHAR(400))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                  WHERE table_schema = 'crm' AND table_name = p_tabla AND column_name = p_columna) THEN
    SET @sql = CONCAT('ALTER TABLE crm.', p_tabla, ' ADD COLUMN ', p_columna, ' ', p_definicion);
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
END$$
DELIMITER ;

CALL tmp_agregar_columna_crm('campana_envio', 'idoferta', 'INT NULL COMMENT ''Oferta que lleva la tanda; NULL = sin oferta''');
CALL tmp_agregar_columna_crm('campana_destinatario', 'idofertacliente', 'INT NULL COMMENT ''Codigo emitido a esta persona''');

DROP PROCEDURE tmp_agregar_columna_crm;

SELECT table_name, column_name, column_type
  FROM information_schema.columns
 WHERE table_schema = 'crm'
   AND ((table_name = 'campana_envio' AND column_name = 'idoferta')
     OR (table_name = 'campana_destinatario' AND column_name = 'idofertacliente'));
