-- ---------------------------------------------------------------------------
-- El recalculo del resumen por persona
--
-- Se ejecuta en el CENTRAL (172.19.0.25). Idempotente: recrea el procedimiento.
-- Requiere 2026_09_20_01, que crea las tablas.
--
-- POR QUE PROCEDIMIENTO Y NO CODIGO JAVA
--
-- El calculo vive aqui y el proceso nocturno solo hace CALL. Asi, afinar la
-- formula de un segmento o corregir un conteo es correr este archivo, no volver
-- a armar y desplegar el jar. En esta casa eso importa: el jar del maestro
-- estuvo cuatro dias sin actualizar y nadie lo noto. El central ya tiene un
-- procedimiento -pr_purgar_ubicaciones_antiguas- y dos triggers, asi que no es
-- un patron nuevo.
--
-- UNA SOLA PASADA POR pedido
--
-- La primera version recorria pizzaamericana.pedido DOS veces: una para el
-- total por persona y otra para saber en cuales tiendas compra. Con 797 mil
-- pedidos y un buffer pool de 128 MB -el de fabrica de MySQL- eso se fue a mas
-- de cuatro minutos y medio.
--
-- Aca se recorre UNA vez, agrupando de una por (persona, tienda), que es el
-- detalle mas fino que se necesita. Todo lo demas -el total por persona, la
-- tienda habitual, en cuantas tiendas compra- sale de sumar esa tabla, que ya
-- es chica. Sumar un millon de filas en memoria no se parece a volver a leer
-- la tabla de pedidos.
--
-- COMO SE ARMA, Y POR QUE EN ESE ORDEN
--
-- 1. Pedidos por persona y tienda, de los dos origenes, en una sola tabla.
-- 2. De ahi salen los totales por persona y la tienda habitual.
-- 3. Se arma una tabla NUEVA y al final se cambia de nombre.
--
-- El paso 3 es lo importante: RENAME TABLE es atomico e instantaneo. Si el
-- recalculo se reconstruyera sobre la tabla buena, durante esos minutos el CRM
-- mostraria una tabla a medio llenar -o vacia-, y peor aun, si el proceso falla
-- a la mitad queda asi hasta la noche siguiente. Con el cambio de nombre, la
-- tabla vieja sirve hasta el ultimo segundo y si algo falla antes del RENAME
-- no se toco nada.
--
-- EL ORIGEN SE GUARDA, NO SE MEZCLA
--
-- La columna origen -C del central, T de tienda- permite separar despues los
-- totales. Si manana el reparto de tipos se rompiera -una tienda que empiece a
-- subir mostrador al central-, el doble conteo se ve comparando las dos
-- columnas contra el total, en vez de quedar escondido en una sola suma.
--
-- LOS ALIAS SE RESUELVEN ANTES DE AGRUPAR, Y EN CADENA
--
-- Una persona repetida apunta a la principal por idpersona_principal, pero esa
-- principal puede ser a su vez un alias: hoy hay 6 casos de dos saltos. Por eso
-- lo primero que hace el procedimiento es armar un mapa de persona a persona
-- real, resolviendo la cadena completa con un bucle.
--
-- Un COALESCE de un solo salto -que fue la primera version- dejaba esos pedidos
-- colgados de una fila intermedia que despues queda por fuera de la tabla final.
-- Se perdian sin aviso: 23 pedidos y 1,2 millones que no cuadraban contra
-- pizzaamericana.pedido. La comprobacion que lo encontro vale la pena repetirla
-- cuando se cambie algo aca:
--
--   SELECT SUM(pedidos_central) FROM crm.persona_resumen;
--   SELECT COUNT(*) FROM pizzaamericana.pedido pe
--     JOIN pizzaamericana.cliente c ON c.idcliente = pe.idcliente
--    WHERE pe.fecha_cancelacion IS NULL AND c.idpersona IS NOT NULL;
--
-- Los dos numeros tienen que ser iguales. La tabla final solo lleva personas
-- reales: activa = 'S' y sin principal.
-- ---------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS crm.pr_recalcular_persona_resumen;

DELIMITER $$

CREATE PROCEDURE crm.pr_recalcular_persona_resumen()
BEGIN
  DECLARE v_activo  INT DEFAULT 60;
  DECLARE v_riesgo  INT DEFAULT 120;
  DECLARE v_cambios INT DEFAULT 0;
  DECLARE v_vueltas INT DEFAULT 0;

  -- Los umbrales viven en tabla para que mercadeo los mueva sin tocar codigo.
  SELECT valor INTO v_activo FROM crm.parametro_segmento WHERE nombre = 'DIAS_ACTIVO';
  SELECT valor INTO v_riesgo FROM crm.parametro_segmento WHERE nombre = 'DIAS_RIESGO';

  -- =========================================================================
  -- 0. DE CADA PERSONA A LA PERSONA REAL
  --
  -- Una persona repetida apunta a la principal por idpersona_principal. Pero
  -- ESA principal puede ser a su vez un alias: hoy hay 6 casos de dos saltos.
  -- Resolver un solo salto dejaba los pedidos colgados de una fila intermedia
  -- que despues queda por fuera de la tabla final, y se perdian sin aviso: 23
  -- pedidos y 1,2 millones que no cuadraban contra pizzaamericana.pedido.
  --
  -- Por eso el bucle y no un COALESCE: cada noche se unen personas nuevas y
  -- una cadena de tres puede aparecer cualquier dia. El tope de 10 vueltas es
  -- un seguro contra un ciclo -A apunta a B y B a A-, que no deberia existir
  -- pero dejaria el proceso dando vueltas para siempre.
  -- =========================================================================
  DROP TEMPORARY TABLE IF EXISTS tmp_real;
  CREATE TEMPORARY TABLE tmp_real (
    idpersona BIGINT NOT NULL,
    idreal    BIGINT NOT NULL,
    PRIMARY KEY (idpersona)
  ) ENGINE=InnoDB;

  INSERT INTO tmp_real (idpersona, idreal)
  SELECT idpersona, COALESCE(idpersona_principal, idpersona) FROM crm.persona;

  REPEAT
    UPDATE tmp_real r
      JOIN crm.persona p ON p.idpersona = r.idreal
       SET r.idreal = p.idpersona_principal
     WHERE p.idpersona_principal IS NOT NULL;
    SET v_cambios = ROW_COUNT();
    SET v_vueltas = v_vueltas + 1;
  UNTIL v_cambios = 0 OR v_vueltas >= 10 END REPEAT;

  -- =========================================================================
  -- 1. PEDIDOS POR PERSONA Y TIENDA, DE LOS DOS ORIGENES
  -- =========================================================================
  DROP TEMPORARY TABLE IF EXISTS tmp_pt;
  CREATE TEMPORARY TABLE tmp_pt (
    idpersona BIGINT  NOT NULL,
    idtienda  INT     NOT NULL,
    origen    CHAR(1) NOT NULL,
    pedidos   INT     NOT NULL,
    valor     DOUBLE  NOT NULL,
    primero   DATE    NULL,
    ultimo    DATE    NULL,
    PRIMARY KEY (idpersona, idtienda, origen)
  ) ENGINE=InnoDB;

  -- El central: domicilio y virtual recoger. Esta es la unica lectura pesada.
  INSERT INTO tmp_pt (idpersona, idtienda, origen, pedidos, valor, primero, ultimo)
  SELECT r.idreal, pe.idtienda, 'C',
         COUNT(*), IFNULL(SUM(pe.total_neto), 0),
         MIN(pe.fechapedido), MAX(pe.fechapedido)
    FROM pizzaamericana.cliente c
    JOIN tmp_real r               ON r.idpersona = c.idpersona
    JOIN pizzaamericana.pedido pe ON pe.idcliente = c.idcliente
   WHERE pe.fecha_cancelacion IS NULL
   GROUP BY 1, 2;

  -- Las tiendas: mostrador y para llevar. Ya viene agregado, es instantaneo.
  -- Si stage_pedido_tienda esta vacia esto no falla: el resumen queda con lo
  -- del central, que es lo que habia antes de este paso.
  --
  -- El GROUP BY no sobra aunque la tabla tenga PK (idtienda, idpersona): dos
  -- filas de personas distintas pueden colapsar en una sola si una es alias de
  -- la otra, y sin agrupar el INSERT chocaria contra la llave.
  INSERT INTO tmp_pt (idpersona, idtienda, origen, pedidos, valor, primero, ultimo)
  SELECT r.idreal, s.idtienda, 'T',
         SUM(s.pedidos), SUM(s.valor),
         MIN(s.primer_pedido), MAX(s.ultimo_pedido)
    FROM crm.stage_pedido_tienda s
    JOIN tmp_real r ON r.idpersona = s.idpersona
   GROUP BY 1, 2;

  -- =========================================================================
  -- 2. DE AHI SALE TODO LO DEMAS
  -- =========================================================================

  -- Totales por persona, separados por origen.
  DROP TEMPORARY TABLE IF EXISTS tmp_persona;
  CREATE TEMPORARY TABLE tmp_persona (
    idpersona       BIGINT NOT NULL,
    pedidos_central INT    NOT NULL,
    valor_central   DOUBLE NOT NULL,
    pedidos_tienda  INT    NOT NULL,
    valor_tienda    DOUBLE NOT NULL,
    primero         DATE   NULL,
    ultimo          DATE   NULL,
    PRIMARY KEY (idpersona)
  ) ENGINE=InnoDB;

  INSERT INTO tmp_persona (idpersona, pedidos_central, valor_central,
                           pedidos_tienda, valor_tienda, primero, ultimo)
  SELECT idpersona,
         SUM(CASE WHEN origen = 'C' THEN pedidos ELSE 0 END),
         SUM(CASE WHEN origen = 'C' THEN valor   ELSE 0 END),
         SUM(CASE WHEN origen = 'T' THEN pedidos ELSE 0 END),
         SUM(CASE WHEN origen = 'T' THEN valor   ELSE 0 END),
         MIN(primero), MAX(ultimo)
    FROM tmp_pt
   GROUP BY idpersona;

  -- La habitual es donde mas compra, sumando los dos origenes. El desempate
  -- por idtienda no es capricho: sin el, dos tiendas empatadas darian una
  -- distinta en cada corrida y el dato se veria inestable sin que nada hubiera
  -- cambiado.
  DROP TEMPORARY TABLE IF EXISTS tmp_habitual;
  CREATE TEMPORARY TABLE tmp_habitual (
    idpersona BIGINT NOT NULL,
    idtienda  INT    NOT NULL,
    tiendas   INT    NOT NULL,
    PRIMARY KEY (idpersona)
  ) ENGINE=InnoDB;

  INSERT INTO tmp_habitual (idpersona, idtienda, tiendas)
  SELECT z.idpersona, z.idtienda, z.tiendas
    FROM (
      SELECT idpersona, idtienda,
             COUNT(*)     OVER (PARTITION BY idpersona) AS tiendas,
             ROW_NUMBER() OVER (PARTITION BY idpersona
                                ORDER BY pedidos DESC, idtienda) AS rn
        FROM (
          SELECT idpersona, idtienda, SUM(pedidos) AS pedidos
            FROM tmp_pt GROUP BY idpersona, idtienda
        ) y
    ) z
   WHERE z.rn = 1;

  -- =========================================================================
  -- 3. SE ARMA LA TABLA NUEVA
  -- =========================================================================
  DROP TABLE IF EXISTS crm.persona_resumen_nueva;
  CREATE TABLE crm.persona_resumen_nueva LIKE crm.persona_resumen;

  INSERT INTO crm.persona_resumen_nueva
        (idpersona, nombre, apellido, celular, email, politica_datos,
         pedidos_central, valor_central, pedidos_tienda, valor_tienda,
         pedidos, valor, ticket_promedio,
         primer_pedido, ultimo_pedido, tiendas_distintas, tienda_habitual)
  SELECT p.idpersona, p.nombre, p.apellido, p.celular_norm, p.email, p.politica_datos,
         IFNULL(x.pedidos_central, 0), IFNULL(x.valor_central, 0),
         IFNULL(x.pedidos_tienda, 0),  IFNULL(x.valor_tienda, 0),
         IFNULL(x.pedidos_central, 0) + IFNULL(x.pedidos_tienda, 0),
         IFNULL(x.valor_central, 0)   + IFNULL(x.valor_tienda, 0),
         CASE WHEN IFNULL(x.pedidos_central, 0) + IFNULL(x.pedidos_tienda, 0) = 0 THEN 0
              ELSE (IFNULL(x.valor_central, 0) + IFNULL(x.valor_tienda, 0))
                   / (IFNULL(x.pedidos_central, 0) + IFNULL(x.pedidos_tienda, 0)) END,
         x.primero, x.ultimo,
         IFNULL(h.tiendas, 0), h.idtienda
    FROM crm.persona p
    LEFT JOIN tmp_persona  x ON x.idpersona = p.idpersona
    LEFT JOIN tmp_habitual h ON h.idpersona = p.idpersona
   WHERE p.activa = 'S'
     AND p.idpersona_principal IS NULL;

  -- Recencia, frecuencia y segmento van en una pasada aparte y no en el INSERT
  -- porque necesitan ultimo_pedido ya resuelto, y SQL no deja usar en la misma
  -- lista de columnas un alias que se acaba de calcular.
  UPDATE crm.persona_resumen_nueva
     SET dias_sin_comprar = DATEDIFF(CURDATE(), ultimo_pedido),
         dias_entre_pedidos = CASE WHEN pedidos > 1
              THEN GREATEST(DATEDIFF(ultimo_pedido, primer_pedido) DIV (pedidos - 1), 0)
              ELSE NULL END,
         segmento = CASE
              WHEN pedidos = 0 THEN 'SIN PEDIDOS'
              WHEN DATEDIFF(CURDATE(), ultimo_pedido) <= v_activo AND pedidos = 1 THEN 'NUEVO'
              WHEN DATEDIFF(CURDATE(), ultimo_pedido) <= v_activo THEN 'ACTIVO'
              WHEN DATEDIFF(CURDATE(), ultimo_pedido) <= v_riesgo THEN 'EN RIESGO'
              ELSE 'DORMIDO' END;

  -- =========================================================================
  -- 4. Y SE CAMBIA DE NOMBRE, QUE ES LO UNICO QUE VE EL QUE ESTA LEYENDO
  -- =========================================================================
  DROP TABLE IF EXISTS crm.persona_resumen_vieja;
  RENAME TABLE crm.persona_resumen       TO crm.persona_resumen_vieja,
               crm.persona_resumen_nueva TO crm.persona_resumen;
  DROP TABLE crm.persona_resumen_vieja;

  DROP TEMPORARY TABLE IF EXISTS tmp_real;
  DROP TEMPORARY TABLE IF EXISTS tmp_pt;
  DROP TEMPORARY TABLE IF EXISTS tmp_persona;
  DROP TEMPORARY TABLE IF EXISTS tmp_habitual;
END$$

DELIMITER ;

SELECT ROUTINE_NAME, CREATED, LAST_ALTERED FROM information_schema.ROUTINES
 WHERE ROUTINE_SCHEMA = 'crm';
