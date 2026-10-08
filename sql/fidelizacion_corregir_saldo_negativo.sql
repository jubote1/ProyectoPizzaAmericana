-- ---------------------------------------------------------------------------
-- Se corrige el unico saldo de puntos que quedo en negativo
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente: la
-- segunda corrida no encuentra saldos negativos y no hace nada.
--
-- ===========================================================================
-- QUE PASO
-- ===========================================================================
--
-- El cliente stefa261991@gmail.com tenia puntos_vigentes = -9,97. La cuenta da
-- exacto:
--
--     Acumulo                                1.054,43
--     Se le vencieron (sep-2025)               - 64,40
--     Redencion 382 del 2026-05-08            - 500,00
--     Redencion 890 del 2026-09-06            - 300,00
--     Reversa de la 890 (pedido cancelado)    + 300,00
--                                            ----------
--     Deberia tener                             490,03
--     Tenia                                      -9,97
--                                            ----------
--     Diferencia                                500,00
--
-- Los 500 puntos de la redencion del 8 de mayo se restaron DOS VECES del
-- acumulado, pero quedaron registrados una sola vez. El detalle lo confirma:
-- en fidelizacion_transaccion hay exactamente 500 marcados como redimidos, no
-- 1.000.
--
-- POR QUE PASO, Y POR QUE NO SE PUEDE REPETIR POR AHI
--
-- Esa redencion tiene la firma del camino viejo: idtienda 0, idpedidotienda 0,
-- sin usuario y sin origen. Era el metodo que hacia cuatro operaciones en
-- cuatro conexiones distintas y en autocommit -marcar el codigo, repartir el
-- debito entre las acumulaciones, restar el saldo y dejar el log-. Si fallaba
-- a mitad y alguien reintentaba, el saldo se restaba dos veces y el log quedaba
-- una.
--
-- Ese camino se reemplazo por FidelizacionRedencionDAO.ejecutarRedencion, que
-- hace todo en una transaccion, el 2026-09-03. La redencion mala es del 8 de
-- mayo, cuatro meses antes.
--
-- LO QUE SIGUE ABIERTO, Y NO SE ARREGLA AQUI
--
-- ClienteFidelizacionDAO.redimirPuntosClienteFidelizacion resta a ciegas -sin
-- mirar si el cliente tiene los puntos- y lo sigue usando el proceso nocturno
-- de VENCIMIENTO de puntos (ServicioReplicaProgramaFidelizacion). Si esa parte
-- se corre dos veces o falla a mitad, puede volver a descuadrar. Es un arreglo
-- aparte.
--
-- ===========================================================================
-- COMO SE CORRIGE
-- ===========================================================================
--
-- El saldo NO se escribe a mano: se recalcula desde el detalle, que es la
-- fuente que si cuadra. Asi el numero no depende de que alguien haya hecho
-- bien la resta, y el script sirve igual si manana aparece otro caso.
--
-- Solo toca saldos NEGATIVOS. Un saldo positivo que no cuadre con su detalle
-- es otra conversacion -puede ser un ajuste legitimo- y no se toca aqui.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. COMO ESTABA, ANTES DE TOCAR NADA
-- ===========================================================================

SELECT c.correo,
       ROUND(c.puntos_vigentes, 2) AS saldo_actual,
       ROUND((SELECT SUM(t.puntos - t.puntos_redimidos)
                FROM pizzaamericana.fidelizacion_transaccion t
               WHERE t.correo = c.correo AND t.vencidos = 'N'), 2) AS segun_el_detalle
  FROM pizzaamericana.cliente_fidelizacion c
 WHERE c.puntos_vigentes < -0.01;

-- ===========================================================================
-- 2. LA CORRECCION
--
-- Se excluyen los -0,00: son ruido de coma flotante, no un descuadre, y
-- recalcularlos no cambia nada.
-- ===========================================================================

UPDATE pizzaamericana.cliente_fidelizacion c
   SET c.puntos_vigentes = IFNULL((SELECT SUM(t.puntos - t.puntos_redimidos)
                                     FROM pizzaamericana.fidelizacion_transaccion t
                                    WHERE t.correo = c.correo AND t.vencidos = 'N'), 0)
 WHERE c.puntos_vigentes < -0.01;

-- ===========================================================================
-- 3. COMO QUEDO
--
-- La primera consulta tiene que volver vacia: ningun saldo por debajo de cero.
-- La segunda muestra como quedo el cliente corregido.
-- ===========================================================================

SELECT correo, ROUND(puntos_vigentes, 2) AS saldo_negativo_que_quedo
  FROM pizzaamericana.cliente_fidelizacion WHERE puntos_vigentes < -0.01;

SELECT c.correo, ROUND(c.puntos_vigentes, 2) AS saldo,
       ROUND((SELECT SUM(t.puntos - t.puntos_redimidos)
                FROM pizzaamericana.fidelizacion_transaccion t
               WHERE t.correo = c.correo AND t.vencidos = 'N'), 2) AS segun_el_detalle
  FROM pizzaamericana.cliente_fidelizacion c
 WHERE c.correo = 'stefa261991@gmail.com';
