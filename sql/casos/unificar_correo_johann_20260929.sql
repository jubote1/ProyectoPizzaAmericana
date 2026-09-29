-- ---------------------------------------------------------------------------
-- UNIFICAR LOS PUNTOS DE UN CLIENTE EN SU CORREO NUEVO
--
-- Cliente: David Flores, celular 3002267310, idpersona 1994, segmento ORO con
-- 81 pedidos. Perdio el acceso a johann448@hotmail.com y pide que todo quede en
-- johandimplus@gmail.com, que ya usa.
--
-- Se corre en el CENTRAL (172.19.0.25). Guarda respaldo y va en UNA transaccion.
--
-- LO QUE SE ENCONTRO AL REVISAR
--
-- Es la MISMA persona en las dos: las cuatro filas de cliente son idpersona
-- 1994, mismo nombre y mismo celular, y una de ellas YA tiene el correo nuevo.
-- No se estan mezclando dos personas distintas.
--
-- Pero el correo no vive en un solo lado. Esta en:
--
--   cliente_fidelizacion          1 fila por cada correo. El correo es LLAVE
--                                 PRIMARIA, asi que no se puede simplemente
--                                 cambiar: hay que fundir y borrar.
--   fidelizacion_transaccion     53 viejas + 2 nuevas. PK (correo, idtienda,
--                                 idpedidotienda); se verifico que no choca
--                                 ninguna.
--   fidelizacion_redencion        3
--   fidelizacion_redencion_det   45
--   codigo_redencion_puntos       4
--   cliente                       3 filas con el viejo (+1 que ya tiene el nuevo)
--   crm.persona / persona_resumen 1 cada una
--
-- POR QUE NO BASTA CON CAMBIAR EL SALDO
--
-- puntos_vigentes NO es un dato suelto: sale de las transacciones. Medido antes
-- de tocar nada:
--
--   viejo   1.603,5 ganados - 1.300 redimidos - 207,5 vencidos = 96      = el saldo
--   nuevo      89,74 ganados -     0          -     0          = 89,74   = el saldo
--
-- O sea que el proceso que acumula los puntos lo recalcula. Si solo se cambiara
-- el saldo y no se movieran las 53 transacciones, la proxima corrida lo
-- devolveria a 89,74 y el cliente perderia sus 96 puntos otra vez, ahora sin
-- que nadie sepa por que.
--
-- Por eso el saldo final no se suma a mano: se DERIVA de las transacciones ya
-- movidas, que es exactamente lo que va a calcular el proceso. Asi no pueden
-- quedar distintos.
--
-- Queda en 96 + 89,74 = 185,74 puntos.
--
-- LA FECHA DE VINCULACION QUE SE CONSERVA ES LA VIEJA
--
-- El cliente esta en el programa desde el 2025-08-22, no desde el 2026-06-27.
-- Quedarse con la fecha nueva le borraria diez meses de antiguedad.
-- ---------------------------------------------------------------------------

SET @viejo = 'johann448@hotmail.com';
SET @nuevo = 'johandimplus@gmail.com';

-- ===========================================================================
-- 1. RESPALDO (va antes de la transaccion: crear tabla hace commit implicito)
-- ===========================================================================

CREATE TABLE IF NOT EXISTS pizzaamericana.respaldo_unificacion_20260929 (
	id             INT AUTO_INCREMENT PRIMARY KEY,
	tabla          VARCHAR(60)  NOT NULL,
	llave          VARCHAR(200)     NULL,
	correo_anterior VARCHAR(50)     NULL,
	dato           VARCHAR(200)     NULL,
	respaldado_en  DATETIME     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci
  COMMENT='Antes de unificar johann448 en johandimplus, 2026-09-29';

INSERT INTO pizzaamericana.respaldo_unificacion_20260929 (tabla, llave, correo_anterior, dato, respaldado_en)
SELECT 'cliente_fidelizacion', correo, correo,
       CONCAT('puntos=', puntos_vigentes, ' vinculacion=', fecha_vinculacion, ' canal=', canal), NOW()
  FROM pizzaamericana.cliente_fidelizacion WHERE correo IN (@viejo, @nuevo);

INSERT INTO pizzaamericana.respaldo_unificacion_20260929 (tabla, llave, correo_anterior, dato, respaldado_en)
SELECT 'fidelizacion_transaccion', CONCAT(idtienda, '/', idpedidotienda), correo,
       CONCAT('puntos=', puntos, ' red=', puntos_redimidos, ' venc=', puntos_vencidos), NOW()
  FROM pizzaamericana.fidelizacion_transaccion WHERE correo = @viejo;

INSERT INTO pizzaamericana.respaldo_unificacion_20260929 (tabla, llave, correo_anterior, dato, respaldado_en)
SELECT 'cliente', idcliente, email, CONCAT('tienda=', idtienda), NOW()
  FROM pizzaamericana.cliente WHERE email = @viejo;

INSERT INTO pizzaamericana.respaldo_unificacion_20260929 (tabla, llave, correo_anterior, dato, respaldado_en)
SELECT 'codigo_redencion_puntos', idcodigo, correo, CONCAT('puntos=', puntos, ' validado=', validado), NOW()
  FROM pizzaamericana.codigo_redencion_puntos WHERE correo = @viejo;

INSERT INTO pizzaamericana.respaldo_unificacion_20260929 (tabla, llave, correo_anterior, dato, respaldado_en)
SELECT 'fidelizacion_redencion', idredencion, correo, NULL, NOW()
  FROM pizzaamericana.fidelizacion_redencion WHERE correo = @viejo;

INSERT INTO pizzaamericana.respaldo_unificacion_20260929 (tabla, llave, correo_anterior, dato, respaldado_en)
VALUES ('crm.persona', '1994', @viejo, 'idpersona 1994', NOW());

-- ===========================================================================
-- 2. LA UNIFICACION, TODO O NADA
-- ===========================================================================

START TRANSACTION;

-- El libro de puntos. Esto es lo que de verdad define el saldo.
UPDATE pizzaamericana.fidelizacion_transaccion SET correo = @nuevo WHERE correo = @viejo;
UPDATE pizzaamericana.fidelizacion_redencion SET correo = @nuevo WHERE correo = @viejo;
UPDATE pizzaamericana.fidelizacion_redencion_detalle SET correo = @nuevo WHERE correo = @viejo;
UPDATE pizzaamericana.codigo_redencion_puntos SET correo = @nuevo WHERE correo = @viejo;

-- La vinculacion al programa: se conserva la fecha y el canal de la mas
-- antigua, que es de donde viene de verdad este cliente.
UPDATE pizzaamericana.cliente_fidelizacion cf
  JOIN (SELECT fecha_vinculacion, canal FROM pizzaamericana.cliente_fidelizacion
         WHERE correo = @viejo) AS v
   SET cf.fecha_vinculacion = v.fecha_vinculacion,
       cf.canal = v.canal,
       cf.activo = 'S'
 WHERE cf.correo = @nuevo;

DELETE FROM pizzaamericana.cliente_fidelizacion WHERE correo = @viejo;

-- El saldo se DERIVA de las transacciones ya movidas, no se suma a mano: asi
-- queda identico a lo que va a calcular el proceso nocturno.
UPDATE pizzaamericana.cliente_fidelizacion cf
   SET cf.puntos_vigentes = (
        SELECT ROUND(IFNULL(SUM(t.puntos - t.puntos_redimidos - t.puntos_vencidos), 0), 2)
          FROM pizzaamericana.fidelizacion_transaccion t WHERE t.correo = cf.correo)
 WHERE cf.correo = @nuevo;

-- Las filas de cliente. Son del mismo idpersona, asi que no se unen personas.
UPDATE pizzaamericana.cliente SET email = @nuevo WHERE email = @viejo;

-- Y el CRM, para que la vista 360 y los envios usen el correo bueno.
UPDATE crm.persona SET email = @nuevo WHERE idpersona = 1994 AND email = @viejo;
UPDATE crm.persona_resumen SET email = @nuevo WHERE idpersona = 1994 AND email = @viejo;

COMMIT;

-- ===========================================================================
-- 3. COMO QUEDO
-- ===========================================================================

SELECT 'saldo del cliente' AS que, correo, puntos_vigentes, fecha_vinculacion, canal
  FROM pizzaamericana.cliente_fidelizacion
 WHERE correo IN ('johann448@hotmail.com', 'johandimplus@gmail.com');

SELECT 'transacciones' AS que, correo, COUNT(*) AS filas,
       ROUND(SUM(puntos - puntos_redimidos - puntos_vencidos), 2) AS saldo_calculado
  FROM pizzaamericana.fidelizacion_transaccion
 WHERE correo IN ('johann448@hotmail.com', 'johandimplus@gmail.com')
 GROUP BY correo;

SELECT 'quedo algo con el correo viejo' AS que,
       (SELECT COUNT(*) FROM pizzaamericana.cliente WHERE email = 'johann448@hotmail.com') AS clientes,
       (SELECT COUNT(*) FROM pizzaamericana.fidelizacion_transaccion WHERE correo = 'johann448@hotmail.com') AS transacciones,
       (SELECT COUNT(*) FROM pizzaamericana.cliente_fidelizacion WHERE correo = 'johann448@hotmail.com') AS fidelizacion,
       (SELECT COUNT(*) FROM crm.persona WHERE email = 'johann448@hotmail.com') AS personas;
