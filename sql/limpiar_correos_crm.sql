-- ---------------------------------------------------------------------------
-- LIMPIEZA DE CORREOS EN crm.persona Y crm.persona_resumen
--
-- Se corre en el CENTRAL (172.19.0.25), DESPUES de
-- 2026_09_25_02_limpiar_correos_cliente.sql. Es idempotente y guarda respaldo.
--
-- POR QUE HACE FALTA ESTA SEGUNDA PARTE
--
-- Limpiar pizzaamericana.cliente no alcanza. El envio de publicidad NO lee de
-- cliente: lee de crm.persona_resumen, y el correo de alli sale de
-- crm.persona, no de cliente. La cadena es
--
--   cliente  ->  (cruce de personas)  ->  crm.persona  ->  crm.persona_resumen
--
-- Asi que despues de limpiar cliente, persona_resumen seguia con 10.043
-- correos sucios y el envio habria vuelto a fallar igual.
--
-- LO QUE HAY EN crm.persona (medido el 2026-09-25 sobre 458.158 filas)
--
--     3.162  buenos con espacios sobrando          -> se recortan
--       546  se arreglan quitando espacios adentro -> se quitan
--     9.507  no son correos                        -> se ponen en NULL
--
-- POR QUE NULL AQUI Y VACIO EN cliente
--
-- No es capricho: se respeta lo que cada tabla ya usaba. En cliente no habia
-- una sola fila con NULL y si 48.187 vacias. En persona hay 173.275 NULL
-- contra 35.087 vacias, o sea que "no tenemos correo" aqui se escribe NULL.
-- Inventar una convencion nueva habria dejado tres formas de decir lo mismo.
--
-- persona_resumen se actualiza desde persona con el mismo criterio que usa
-- pr_recalcular_persona_resumen -de alli saca el correo-, para no tener que
-- esperar al proceso nocturno ni recalcular 451.037 filas por una columna.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS crm.respaldo_email_persona_20260925 (
	idpersona      BIGINT       NOT NULL,
	email_anterior VARCHAR(100)     NULL,
	respaldado_en  DATETIME     NOT NULL,
	PRIMARY KEY (idpersona)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci
  COMMENT='Correos de persona antes de la limpieza del 2026-09-25';

DROP PROCEDURE IF EXISTS crm.limpiar_correos_persona_20260925;

DELIMITER //
CREATE PROCEDURE crm.limpiar_correos_persona_20260925()
BEGIN
	DECLARE v_desde INT;
	DECLARE v_hasta INT;
	DECLARE v_tope  INT;
	DECLARE v_paso  INT DEFAULT 50000;

	-- El punto va como [.] y no como barra-punto: si la barra se pierde en el
	-- literal de cadena, el punto pasa a ser "cualquier caracter" y
	-- "lanene91@hotmail@com" se da por bueno.
	SET @re = '^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+([.][A-Za-z0-9-]+)*[.][A-Za-z]{2,}$';

	SELECT MIN(idpersona), MAX(idpersona) INTO v_desde, v_tope FROM crm.persona;

	WHILE v_desde <= v_tope DO
		SET v_hasta = v_desde + v_paso - 1;

		INSERT IGNORE INTO crm.respaldo_email_persona_20260925
			(idpersona, email_anterior, respaldado_en)
		SELECT idpersona, email, NOW()
		  FROM crm.persona
		 WHERE idpersona BETWEEN v_desde AND v_hasta
		   AND email IS NOT NULL AND TRIM(email) <> ''
		   AND NOT (email REGEXP @re);

		-- a) Buenos con espacios sobrando.
		UPDATE crm.persona
		   SET email = TRIM(email)
		 WHERE idpersona BETWEEN v_desde AND v_hasta
		   AND email IS NOT NULL
		   AND NOT (email REGEXP @re)
		   AND TRIM(email) REGEXP @re;

		-- b) Se arreglan quitando los espacios de adentro.
		UPDATE crm.persona
		   SET email = REPLACE(TRIM(email), ' ', '')
		 WHERE idpersona BETWEEN v_desde AND v_hasta
		   AND email IS NOT NULL AND TRIM(email) <> ''
		   AND NOT (email REGEXP @re)
		   AND REPLACE(TRIM(email), ' ', '') REGEXP @re;

		-- c) No es un correo. Se pone NULL, no se adivina.
		UPDATE crm.persona
		   SET email = NULL
		 WHERE idpersona BETWEEN v_desde AND v_hasta
		   AND email IS NOT NULL AND TRIM(email) <> ''
		   AND NOT (email REGEXP @re);

		-- d) Solo espacios: es no tener correo.
		UPDATE crm.persona
		   SET email = NULL
		 WHERE idpersona BETWEEN v_desde AND v_hasta
		   AND email IS NOT NULL AND TRIM(email) = '';

		COMMIT;
		SET v_desde = v_desde + v_paso;
	END WHILE;

	-- Se baja lo corregido al resumen, que es de donde lee el envio. Solo las
	-- personas que cambiaron: recalcular las 451.037 por una columna seria
	-- mover la tabla entera para nada.
	UPDATE crm.persona_resumen r
	  JOIN crm.persona p ON p.idpersona = r.idpersona
	  JOIN crm.respaldo_email_persona_20260925 b ON b.idpersona = r.idpersona
	   SET r.email = p.email;
END //
DELIMITER ;

CALL crm.limpiar_correos_persona_20260925();

DROP PROCEDURE IF EXISTS crm.limpiar_correos_persona_20260925;

SET @re = '^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+([.][A-Za-z0-9-]+)*[.][A-Za-z]{2,}$';

SELECT 'persona' AS tabla,
       SUM(email IS NOT NULL AND TRIM(email) <> '' AND NOT (email REGEXP @re)) AS todavia_sucios,
       SUM(email REGEXP @re) AS sirven
  FROM crm.persona
UNION ALL
SELECT 'persona_resumen',
       SUM(email IS NOT NULL AND TRIM(email) <> '' AND NOT (email REGEXP @re)),
       SUM(email REGEXP @re)
  FROM crm.persona_resumen;
