-- ============================================================================
-- CERRAR HOY LOS CODIGOS DEL ENVIO 243 (cupon 50%, oferta 61)
-- 2026-11-14
--
-- POR QUE
--
-- La campana se anuncio como "SOLO HOY, de 12 del dia a 6 de la tarde", pero
-- los codigos salieron con `fecha_caducidad = 2026-11-15`.
--
-- No es un error del envio: `PromocionesCtrl` calcula la caducidad como
-- hoy + `dias_caducidad`, y `dias_caducidad` no puede bajar a 0 porque
-- `CodigoPromoDAO.ofertasEnviables()` filtra `o.dias_caducidad > 0` y la
-- oferta desapareceria de la pantalla de Envio de Publicidad.
--
-- La franja horaria NO alcanza a cerrarlo: compara hora del dia, no fecha, asi
-- que manana domingo entre 12 y 18 el codigo seguiria sirviendo.
--
-- Bajarle la caducidad a hoy es lo que de verdad lo cierra: la rama de
-- `oferta_cliente` en `PromocionesCtrl.retornarOfertaCodigoPromocional` valida
-- la caducidad de forma INCLUSIVA, hasta el final del dia, asi que con
-- fecha_caducidad = 2026-11-14 el codigo sirve hoy completo y muere a la
-- medianoche.
--
-- Solo toca el envio 243. No se tocan codigos usados ni de otros envios.
--
-- Idempotente.
-- ============================================================================

USE pizzaamericana;

UPDATE oferta_cliente
   SET fecha_caducidad = '2026-11-14'
 WHERE idenvio = 243
   AND idoferta = 61
   AND fecha_caducidad > '2026-11-14';

-- ============================================================================
-- COMPROBACION
-- ============================================================================
SELECT idenvio, COUNT(*) codigos, MIN(fecha_caducidad) cad_min, MAX(fecha_caducidad) cad_max,
       SUM(utilizada = 'S') usados,
       SUM(fecha_caducidad > CURDATE()) AS vivos_manana
  FROM oferta_cliente WHERE idenvio = 243 GROUP BY idenvio;
