-- CUPON 50% 25 ANOS - SOLO HOY 15 DE NOVIEMBRE, DE 12 M A 6 PM
--
-- El cupon del 50% vive en UNA SOLA OFERTA, la 61, por decision del 14-nov
-- ("nos estamos llenando de ofertas, la idea es seguir midiendo junto").
-- Cada campana se separa igual por oferta_cliente.idenvio, asi que no se
-- pierde la medicion.
--
-- Los 899 codigos de la 61 ya estan todos vencidos o usados: comprobado con
--   select sum(utilizada='N' and anulada='N' and fecha_caducidad >= curdate())
--     from pizzaamericana.oferta_cliente where idoferta = 61;   -> 0
-- Reusar la oferta NO revive codigos viejos: cada uno caduca por su cuenta.
--
-- Solo se mueven las fechas y se deja habilitada. La franja 12-18 y
-- dias_caducidad=1 ya estaban bien.
-- OJO: dias_caducidad NO puede ser 0, porque ofertasEnviables() filtra
-- dias_caducidad > 0 y la oferta desapareceria de Envio de Publicidad.

UPDATE pizzaamericana.oferta
   SET habilitado    = 'S',
       fecha_desde   = '2026-11-15',
       fecha_hasta   = '2026-11-15',
       controla_hora = 'S',
       hora_inicio   = 12,
       hora_fin      = 18,
       mensaje1      = 'SOLO HOY, de 12 del dia a 6 de la tarde, tu pizza con 50% de descuento en Pizza Americana. Usa tu codigo al pedir: llama al 604 4444553 o acercate a un punto de venta. Compra minima $40.000, tope de descuento $30.000.'
 WHERE idoferta = 61;

-- Se descarta la 65, creada por error hoy antes de releer la decision. No
-- alcanzo a emitir ningun codigo, asi que no deja huerfanos.
DELETE FROM pizzaamericana.oferta
 WHERE idoferta = 65
   AND NOT EXISTS (SELECT 1 FROM (SELECT 1 FROM pizzaamericana.oferta_cliente WHERE idoferta = 65) x);
