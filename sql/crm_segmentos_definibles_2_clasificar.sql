-- ---------------------------------------------------------------------------
-- El procedimiento que clasifica a cada persona segun las definiciones
--
-- Se ejecuta en el CENTRAL (172.19.0.25). Idempotente: recrea el procedimiento.
-- Requiere crm_segmentos_definibles_1_tablas.sql.
--
-- SE LE PASA LA TABLA, Y POR ESO SIRVE PARA DOS COSAS
--
-- El recalculo nocturno arma persona_resumen_nueva y solo al final le cambia el
-- nombre, asi que la clasificacion tiene que caer sobre ESA tabla y no sobre la
-- que esta leyendo el CRM.
--
-- Pero pasandole 'persona_resumen' sirve para lo otro, que es lo que de verdad
-- hace usable esto: cambiar una definicion y ver el resultado YA, sin esperar a
-- la noche ni recalcular los 797 mil pedidos. Son segundos contra 75.
--
--     CALL crm.pr_clasificar_segmentos('persona_resumen');
--
-- POR QUE SE RECORRE AL REVES
--
-- Se aplica de la definicion de MENOR prioridad a la de mayor, para que la mas
-- prioritaria escriba de ultima y gane. Es mas simple y mas rapido que
-- preguntar en cada UPDATE si la persona ya quedo clasificada.
--
-- LAS TRES COSAS QUE SE VALIDAN, Y POR QUE
--
-- 1. El campo tiene que estar en crm.segmento_campo. La condicion se arma
--    concatenando ese nombre, asi que sin la lista blanca seria una inyeccion
--    de SQL corriendo como root.
-- 2. El operador tiene que ser uno de los seis. Misma razon.
-- 3. El valor de un campo numerico tiene que ser un numero de verdad.
--
-- Una regla que no pase se IGNORA en silencio, y ahi hay un riesgo real: si se
-- ignoran TODAS las reglas de un segmento, la condicion queda vacia y ese
-- UPDATE cogeria a las 450 mil personas. Por eso, si no queda ninguna regla
-- valida, el segmento se salta completo. Un segmento mal escrito no clasifica a
-- nadie, que es molesto; uno sin condicion clasificaria a todos, que es un
-- desastre silencioso.
--
-- EL QUE NO CUMPLA NINGUNA QUEDA EN 'SIN CLASIFICAR'
--
-- No se deja el valor anterior: seria un segmento viejo disfrazado de actual.
-- Si aparece gente ahi, es que a las definiciones les falta un caso.
-- ---------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS crm.pr_clasificar_segmentos;

DELIMITER $$

CREATE PROCEDURE crm.pr_clasificar_segmentos(IN p_tabla VARCHAR(64))
BEGIN
  DECLARE v_fin      INT DEFAULT 0;
  DECLARE v_id       INT;
  DECLARE v_nombre   VARCHAR(20);
  DECLARE v_cond     TEXT;
  DECLARE v_tabla    VARCHAR(64);

  DECLARE cur CURSOR FOR
    SELECT idsegmento, nombre FROM crm.segmento_definicion
     WHERE activo = 'S' ORDER BY orden DESC, idsegmento DESC;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_fin = 1;

  -- La tabla tambien se concatena, asi que tambien va contra una lista blanca.
  IF p_tabla = 'persona_resumen_nueva' THEN
    SET v_tabla = 'crm.persona_resumen_nueva';
  ELSE
    SET v_tabla = 'crm.persona_resumen';
  END IF;

  SET @sql = CONCAT('UPDATE ', v_tabla, ' SET segmento = ''SIN CLASIFICAR''');
  PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

  OPEN cur;
  bucle: LOOP
    FETCH cur INTO v_id, v_nombre;
    IF v_fin = 1 THEN
      LEAVE bucle;
    END IF;

    SELECT GROUP_CONCAT(
             CONCAT('`', r.campo, '` ', r.operador, ' ',
                    CASE WHEN c.tipo = 'TEXTO'
                         THEN QUOTE(r.valor)
                         ELSE r.valor END)
             ORDER BY r.idregla SEPARATOR ' AND ')
      INTO v_cond
      FROM crm.segmento_regla r
      JOIN crm.segmento_campo c ON c.campo = r.campo
     WHERE r.idsegmento = v_id
       AND r.operador IN ('=', '<>', '>', '<', '>=', '<=')
       AND (c.tipo = 'TEXTO' OR r.valor REGEXP '^-?[0-9]+([.][0-9]+)?$');

    -- Sin condicion NO se toca la tabla. Ver el comentario de arriba: un UPDATE
    -- sin WHERE aqui se llevaria a las 450 mil personas para ese segmento.
    IF v_cond IS NOT NULL AND LENGTH(TRIM(v_cond)) > 0 THEN
      SET @sql = CONCAT('UPDATE ', v_tabla, ' SET segmento = ', QUOTE(v_nombre),
                        ' WHERE ', v_cond);
      PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
  END LOOP;
  CLOSE cur;
END$$

DELIMITER ;

-- Se clasifica ya sobre la tabla viva, para comprobar que da lo mismo que antes.
CALL crm.pr_clasificar_segmentos('persona_resumen');

SELECT segmento, COUNT(*) AS personas, ROUND(SUM(valor)/1000000) AS millones
  FROM crm.persona_resumen GROUP BY segmento ORDER BY personas DESC;
