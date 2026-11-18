-- ============================================================================
-- CERRAR LOS CODIGOS DE AYER (envio 244) Y DEJAR EL CUPON LISTO PARA HOY,
-- FESTIVO LUNES 16 DE NOVIEMBRE, DE 12 M. A 7 P.M.
-- 2026-11-16
--
-- 1. LOS CODIGOS DE AYER SIGUEN VIVOS HOY
--
-- El envio 244 salio el domingo 15 con 499 codigos y caducidad 2026-11-16, o
-- sea HOY. Es el mismo efecto de siempre: `PromocionesCtrl` calcula
-- hoy + `dias_caducidad`, y `dias_caducidad` no puede bajar a 0 porque
-- `CodigoPromoDAO.ofertasEnviables()` filtra `> 0` y la oferta desapareceria
-- de la pantalla de envio.
--
-- Sin esto, los 496 sin usar de ayer competirian hoy con los que se emitan en
-- la campana del festivo, y no habria como separar que campana movio cual
-- redencion mirando solo la fecha.
--
-- 2. LA FRANJA DE HOY VA HASTA LAS 7
--
-- `hora_fin = 19` porque la comparacion es `horaActual < hora_fin`: sirve de
-- 12:00:00 a 18:59:59. Quien pida a las 7:00 p.m. en punto ya no alcanza.
-- Es la misma convencion del sabado (12 a 18 = "de 12 a 6").
--
-- La oferta NO cambia de nombre ni de id: es la 61 de siempre, la unica del
-- cupon, para seguir midiendo todo junto.
--
-- Idempotente.
-- ============================================================================

USE pizzaamericana;

-- 1. Cerrar ayer los codigos del envio 244 -----------------------------------
UPDATE oferta_cliente
   SET fecha_caducidad = '2026-11-15'
 WHERE idenvio = 244
   AND idoferta = 61
   AND fecha_caducidad > '2026-11-15';

-- 2. La oferta 61 queda para hoy, festivo, de 12 a 19 ------------------------
UPDATE oferta
   SET controla_hora = 'S',
       hora_inicio   = 12,
       hora_fin      = 19,
       fecha_desde   = '2026-11-16',
       fecha_hasta   = '2026-11-16',
       habilitado    = 'S',
       mensaje1      = 'SOLO HOY, festivo lunes 16 de noviembre, de 12 del dia a 7 de la noche, tu pizza con 50% de descuento en Pizza Americana. Usa tu codigo al pedir: llama al 604 4444553 o acercate a un punto de venta. Compra minima $40.000, tope de descuento $30.000.'
 WHERE idoferta = 61;

-- ============================================================================
-- COMPROBACION
-- ============================================================================
-- Ningun codigo de campanas anteriores puede servir hoy: tiene que dar 0.
SELECT SUM(utilizada = 'N' AND anulada = 'N' AND fecha_caducidad >= CURDATE()) AS codigos_viejos_vivos
  FROM oferta_cliente WHERE idoferta = 61;

SELECT idenvio, DATE(ingreso_oferta) emitido, COUNT(*) codigos,
       MAX(fecha_caducidad) caduca, SUM(utilizada = 'S') redimidos
  FROM oferta_cliente WHERE idoferta = 61
 GROUP BY idenvio, DATE(ingreso_oferta) ORDER BY idenvio;

SELECT idoferta, nombre_oferta, descuento_fijo_porcentaje pct, monto_minimo,
       tope_descuento, controla_hora, hora_inicio, hora_fin, dias_caducidad,
       fecha_desde, fecha_hasta, habilitado
  FROM oferta WHERE idoferta = 61;
