-- ---------------------------------------------------------------------------
-- Se le aplica el detalle a las seis redenciones manuales ya hechas
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente: la
-- segunda corrida no encuentra redenciones sin detalle y no hace nada.
--
-- ===========================================================================
-- QUE SE ESTA CORRIGIENDO
-- ===========================================================================
--
-- La primera version de la redencion manual descontaba el saldo del cliente
-- pero no repartia el debito entre sus acumulaciones de
-- fidelizacion_transaccion, ni escribia fidelizacion_redencion_detalle.
--
-- El saldo de esos clientes esta BIEN. Lo que esta mal es el detalle, que
-- sigue diciendo que esos puntos estan disponibles. De ahi salen dos cosas:
--
--   - el correo de vencimiento le avisaria al cliente que se le vencen puntos
--     que ya no tiene, porque los lee del detalle;
--   - una reversa no sabria de que acumulacion devolverlos.
--
-- Este script NO cambia el saldo de nadie. Solo alinea el detalle con un saldo
-- que ya es correcto.
--
-- El codigo quedo arreglado aparte: la redencion manual ahora usa
-- FidelizacionRedencionDAO.ejecutarRedencion, el mismo camino de las
-- redenciones normales.
--
-- ===========================================================================
-- QUE NO SE TOCA, Y POR QUE
-- ===========================================================================
--
-- Las 877 redenciones anteriores al 2026-09-03 tampoco tienen detalle: el
-- metodo viejo nunca lo escribio. Esas NO se tocan aqui. Son 275.840 puntos de
-- doce meses, el saldo de esos clientes ya refleja lo redimido, y reconstruir
-- a que acumulacion pertenecio cada debito de hace un ano es adivinar. Es una
-- decision aparte, no algo que se arregle de paso.
--
-- Aqui se corrigen las SEIS que produjo la pantalla nueva, que son recientes,
-- acotadas y de las que si se sabe todo.
--
-- ===========================================================================
-- COMO SE REPARTE
-- ===========================================================================
--
-- Igual que lo hace ejecutarRedencion: de la acumulacion mas reciente a la mas
-- antigua, tomando de cada una lo que tenga disponible hasta completar.
--
-- El reparto se calcula con una suma corrida sobre las acumulaciones
-- disponibles. Para cada una, lo que le toca es lo que quede por repartir
-- cuando le llega el turno, topado por lo que esa acumulacion tenga libre.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. EL PLAN, EN UNA TABLA TEMPORAL
--
-- Se materializa primero para poder mirarlo antes de aplicarlo, y para que el
-- UPDATE no tenga que leer de la misma tabla que escribe.
-- ===========================================================================

DROP TEMPORARY TABLE IF EXISTS pizzaamericana.plan_detalle;

CREATE TEMPORARY TABLE pizzaamericana.plan_detalle AS
SELECT p.idredencion, p.correo, p.idtienda, p.idpedidotienda,
       ROUND(p.debito, 6) AS debito
  FROM (
    SELECT r.idredencion, a.correo, a.idtienda, a.idpedidotienda,
           GREATEST(0, LEAST(a.disponible, r.puntos_redimidos - (a.corrido - a.disponible))) AS debito
      FROM (
        SELECT t.correo, t.idtienda, t.idpedidotienda,
               t.puntos - t.puntos_redimidos AS disponible,
               SUM(t.puntos - t.puntos_redimidos) OVER (
                   PARTITION BY t.correo
                   ORDER BY t.fecha_transaccion DESC, t.idtienda, t.idpedidotienda
                   ROWS UNBOUNDED PRECEDING) AS corrido
          FROM pizzaamericana.fidelizacion_transaccion t
         WHERE t.vencidos = 'N'
           AND t.puntos > t.puntos_redimidos
           AND t.correo IN (SELECT correo FROM pizzaamericana.fidelizacion_redencion
                             WHERE origen = 'ADM' AND estado = 'CONFIRMADA')
      ) a
      JOIN pizzaamericana.fidelizacion_redencion r
        ON r.correo = a.correo AND r.origen = 'ADM' AND r.estado = 'CONFIRMADA'
       -- Solo las que todavia no tienen detalle: eso hace el script repetible.
       AND NOT EXISTS (SELECT 1 FROM pizzaamericana.fidelizacion_redencion_detalle d
                        WHERE d.idredencion = r.idredencion)
  ) p
 WHERE p.debito > 0.000001;

-- ===========================================================================
-- 2. ASI VA A QUEDAR REPARTIDO
--
-- La suma por redencion tiene que dar exactamente los puntos redimidos. Si a
-- alguna le falta, es que sus acumulaciones no alcanzan y hay que mirarla a
-- mano antes de seguir.
-- ===========================================================================

SELECT p.idredencion, p.correo,
       COUNT(*) AS acumulaciones_que_se_tocan,
       ROUND(SUM(p.debito), 2) AS se_reparte,
       (SELECT r.puntos_redimidos FROM pizzaamericana.fidelizacion_redencion r
         WHERE r.idredencion = p.idredencion) AS deberia_repartir,
       ROUND(SUM(p.debito) - (SELECT r.puntos_redimidos FROM pizzaamericana.fidelizacion_redencion r
         WHERE r.idredencion = p.idredencion), 2) AS diferencia
  FROM pizzaamericana.plan_detalle p
 GROUP BY p.idredencion, p.correo
 ORDER BY p.idredencion;

-- ===========================================================================
-- 3. SE APLICA
-- ===========================================================================

UPDATE pizzaamericana.fidelizacion_transaccion t
  JOIN pizzaamericana.plan_detalle p
    ON p.correo = t.correo AND p.idtienda = t.idtienda AND p.idpedidotienda = t.idpedidotienda
   SET t.puntos_redimidos = t.puntos_redimidos + p.debito;

INSERT INTO pizzaamericana.fidelizacion_redencion_detalle
       (idredencion, correo, idtienda, idpedidotienda, puntos_debitados)
SELECT p.idredencion, p.correo, p.idtienda, p.idpedidotienda, p.debito
  FROM pizzaamericana.plan_detalle p;

DROP TEMPORARY TABLE IF EXISTS pizzaamericana.plan_detalle;

-- ===========================================================================
-- COMO QUEDO
--
-- La primera consulta tiene que volver vacia: ninguna redencion ADM sin
-- detalle. La segunda muestra que el saldo de cada uno ahora si cuadra con su
-- propio detalle.
-- ===========================================================================

SELECT idredencion, correo, puntos_redimidos
  FROM pizzaamericana.fidelizacion_redencion r
 WHERE r.origen = 'ADM' AND r.estado = 'CONFIRMADA'
   AND NOT EXISTS (SELECT 1 FROM pizzaamericana.fidelizacion_redencion_detalle d
                    WHERE d.idredencion = r.idredencion);

SELECT c.correo, ROUND(c.puntos_vigentes, 2) AS saldo,
       ROUND(IFNULL((SELECT SUM(t.puntos - t.puntos_redimidos)
              FROM pizzaamericana.fidelizacion_transaccion t
             WHERE t.correo = c.correo AND t.vencidos = 'N'), 0), 2) AS segun_el_detalle,
       ROUND(c.puntos_vigentes - IFNULL((SELECT SUM(t.puntos - t.puntos_redimidos)
              FROM pizzaamericana.fidelizacion_transaccion t
             WHERE t.correo = c.correo AND t.vencidos = 'N'), 0), 2) AS diferencia
  FROM pizzaamericana.cliente_fidelizacion c
 WHERE c.correo IN (SELECT correo FROM pizzaamericana.fidelizacion_redencion
                     WHERE origen = 'ADM' AND estado = 'CONFIRMADA')
 ORDER BY c.correo;
