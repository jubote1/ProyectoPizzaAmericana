-- ---------------------------------------------------------------------------
-- LIMPIEZA DE CORREOS EN pizzaamericana.cliente
--
-- Se corre en el CENTRAL (172.19.0.25). Es idempotente y guarda respaldo.
--
-- POR QUE
--
-- El 2026-09-25 un envio de 500 correos fallo COMPLETO. Brevo rechaza el lote
-- entero si una sola direccion viene mal, iban de a cien, y catorce malas
-- repartidas en los cinco lotes tumbaron los quinientos.
--
-- El codigo ya no las deja entrar ni salir, pero los datos siguen sucios: se
-- pierden 4.316 clientes cuyo correo es BUENO y solo tiene espacios.
--
-- LO QUE HAY (medido el 2026-09-25 sobre 287.169 filas)
--
--   227.482  perfectos, no se tocan
--    48.187  ya vacios, no se tocan
--     4.316  BUENOS con espacios sobrando         -> se recortan
--       610  se arreglan quitando espacios de adentro -> se quitan
--     6.574  no son correos                       -> se vacian
--
-- Los 6.574 son sobre todo lo que se digito en caja cuando el cliente no quiso
-- darlo: "no tiene" (1.918), "no lo da" (1.188), "no acepta" (877), "null"
-- (130). El resto son correos con acentos que se corrompieron -"mar?a.bedoya"
-- por "maria.bedoya"- y dominios truncados como "@gmail." o "@gmail.c".
--
-- LO QUE NO SE HACE, A PROPOSITO
--
-- Adivinar. "mar?a.bedoya377@hotmail.com" pudo ser maria o mar1a; "lanene91@
-- hotmail@com" pudo ser hotmail.com o hotmail.es. Mandarle correo a una
-- direccion adivinada rebota, y rebotar es justo lo que quema la reputacion
-- del dominio. Se vacian y la tienda los vuelve a pedir.
--
-- NO SE PIERDEN PUNTOS
--
-- Verificado antes de correr: de los 7.184 correos invalidos, CERO tienen
-- filas en codigo_redencion_puntos. Tiene sentido, porque a una direccion
-- invalida nunca le llego un codigo.
--
-- POR LOTES, Y NO DE UN SOLO UPDATE
--
-- Un UPDATE sobre las 287.169 filas filtrando por REGEXP no puede usar indice:
-- recorre la tabla entera y mantiene los candados hasta el commit. El
-- 2026-09-15 algo asi bloqueo esta misma tabla seis minutos y dejo las
-- consultas de pedidos esperando. Aqui se va por rangos de idcliente -que si
-- usan la llave primaria- y se confirma cada lote.
--
-- email_norm NO HAY QUE TOCARLO: es columna VIRTUAL GENERATED y se recalcula
-- sola. De hecho este arreglo se la POBLA a los 4.926 que quedan buenos, que
-- hoy la tienen nula, y con eso entran al cruce de personas del CRM.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. RESPALDO
-- ===========================================================================

CREATE TABLE IF NOT EXISTS pizzaamericana.respaldo_email_20260925 (
	idcliente      INT          NOT NULL,
	email_anterior VARCHAR(50)      NULL,
	respaldado_en  DATETIME     NOT NULL,
	PRIMARY KEY (idcliente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci
  COMMENT='Correos antes de la limpieza del 2026-09-25';

-- ===========================================================================
-- 2. LA LIMPIEZA, POR LOTES
-- ===========================================================================

DROP PROCEDURE IF EXISTS pizzaamericana.limpiar_correos_20260925;

DELIMITER //
CREATE PROCEDURE pizzaamericana.limpiar_correos_20260925()
BEGIN
	DECLARE v_desde  INT;
	DECLARE v_hasta  INT;
	DECLARE v_tope   INT;
	DECLARE v_paso   INT DEFAULT 20000;

	-- El patron se escribe con [.] y no con barra-punto: la barra tiene que
	-- sobrevivir al literal de cadena de MySQL, y si se pierde, el punto pasa a
	-- significar "cualquier caracter" y "lanene91@hotmail@com" se da por bueno.
	SET @re = '^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+([.][A-Za-z0-9-]+)*[.][A-Za-z]{2,}$';

	SELECT MIN(idcliente), MAX(idcliente) INTO v_desde, v_tope
	  FROM pizzaamericana.cliente;

	WHILE v_desde <= v_tope DO
		SET v_hasta = v_desde + v_paso - 1;

		-- Respaldo de lo que este lote va a cambiar. INSERT IGNORE para que
		-- correrlo dos veces no pise el valor original guardado la primera vez.
		INSERT IGNORE INTO pizzaamericana.respaldo_email_20260925
			(idcliente, email_anterior, respaldado_en)
		SELECT idcliente, email, NOW()
		  FROM pizzaamericana.cliente
		 WHERE idcliente BETWEEN v_desde AND v_hasta
		   AND email IS NOT NULL
		   AND TRIM(email) <> ''
		   AND NOT (email REGEXP @re);

		-- a) Los buenos con espacios sobrando. Son la mayoria y el motivo de
		--    todo esto: correos perfectos que no salian por un espacio.
		UPDATE pizzaamericana.cliente
		   SET email = TRIM(email)
		 WHERE idcliente BETWEEN v_desde AND v_hasta
		   AND email IS NOT NULL
		   AND NOT (email REGEXP @re)
		   AND TRIM(email) REGEXP @re;

		-- b) Los que se arreglan quitando los espacios de adentro:
		--    "fabian_londono@hotmail. com", "ppiedad.benitez @ gmail.com".
		UPDATE pizzaamericana.cliente
		   SET email = REPLACE(TRIM(email), ' ', '')
		 WHERE idcliente BETWEEN v_desde AND v_hasta
		   AND email IS NOT NULL
		   AND TRIM(email) <> ''
		   AND NOT (email REGEXP @re)
		   AND REPLACE(TRIM(email), ' ', '') REGEXP @re;

		-- c) Lo que no es un correo. Se vacia, no se adivina.
		UPDATE pizzaamericana.cliente
		   SET email = ''
		 WHERE idcliente BETWEEN v_desde AND v_hasta
		   AND email IS NOT NULL
		   AND TRIM(email) <> ''
		   AND NOT (email REGEXP @re);

		-- d) Los que solo tienen espacios: quedan como vacio de verdad.
		UPDATE pizzaamericana.cliente
		   SET email = ''
		 WHERE idcliente BETWEEN v_desde AND v_hasta
		   AND email IS NOT NULL
		   AND email <> ''
		   AND TRIM(email) = '';

		COMMIT;
		SET v_desde = v_desde + v_paso;
	END WHILE;
END //
DELIMITER ;

CALL pizzaamericana.limpiar_correos_20260925();

DROP PROCEDURE IF EXISTS pizzaamericana.limpiar_correos_20260925;

-- ===========================================================================
-- 3. COMO QUEDO
-- ===========================================================================

SET @re = '^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+([.][A-Za-z0-9-]+)*[.][A-Za-z]{2,}$';

SELECT CASE
         WHEN email IS NULL OR TRIM(email) = '' THEN 'vacio'
         WHEN email REGEXP @re                  THEN 'sirve'
         ELSE 'todavia malo'
       END AS estado,
       COUNT(*) AS cuantos
  FROM pizzaamericana.cliente
 GROUP BY estado ORDER BY estado;

SELECT COUNT(*) AS respaldados FROM pizzaamericana.respaldo_email_20260925;
