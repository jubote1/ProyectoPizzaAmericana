-- ============================================================================
-- OFERTA 61: EL CUPON DEL 50% PASA A VALER TODO EL DIA
-- 2026-11-08
--
-- POR QUE
--
-- Venia con franja de 12 a 18 porque nacio como campana de "llenar la tarde"
-- del sabado. Movida a lunes y martes el objetivo es otro: esos son los dos
-- dias mas flojos de la semana COMPLETOS, no solo por la tarde. Medido sin
-- festivos del 12-oct al 6-nov: lunes $15,5M y martes $16,6M, contra viernes
-- $45,0M, sabado $70,2M y domingo $67,8M.
--
-- Se apaga controla_hora en vez de poner 0 a 24: con 'N' no se evalua la franja
-- y el cupon vale mientras la tienda este abierta, que es justo lo que se
-- quiere. Poner 0 y 24 funcionaria igual pero deja el dato sugiriendo un
-- control que no existe.
--
-- Las horas se dejan en 0 para que nadie lea una franja vieja y crea que sigue
-- vigente.
--
-- Idempotente.
-- ============================================================================

USE pizzaamericana;

UPDATE oferta
   SET controla_hora = 'N',
       hora_inicio   = 0,
       hora_fin      = 0,
       mensaje1      = 'Lunes y martes, todo el dia, tu pizza con 50% de descuento en Pizza Americana. Usa tu codigo al pedir: llama al 604 4444553 o acercate a un punto de venta. Compra minima $40.000, tope de descuento $30.000.'
 WHERE idoferta = 61;

-- ============================================================================
-- COMPROBACION
-- ============================================================================
SELECT idoferta, nombre_oferta, codigo_promocional AS cod_x_persona, codigo_general,
       controla_hora, hora_inicio, hora_fin, descuento_fijo_porcentaje AS pct,
       monto_minimo, tope_descuento, max_usos_cliente, habilitado,
       fecha_desde, fecha_hasta
  FROM oferta WHERE idoferta = 61;
