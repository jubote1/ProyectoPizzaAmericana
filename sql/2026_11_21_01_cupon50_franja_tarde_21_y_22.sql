-- CUPON 50% - SABADO 21 Y DOMINGO 22, DE 12 M A 6 PM
-- Objetivo: levantar la venta de la tarde. Segmento EN RIESGO.
--
-- POR QUE UNA OFERTA NUEVA Y NO LA 61
--
-- La 61 tiene HOY 499 codigos vivos (488 sin usar) del rescate del envio 265,
-- y esta en controla_hora='N' porque a esa gente se le prometio por escrito
-- "valido a cualquier hora, hasta el 21/11". Ponerle la franja 12-18 a la 61
-- les romperia la promesa a media jornada, a mitad del dia en que vence.
--
-- Por eso esta campana va en oferta propia. Cuando los codigos de la 61
-- mueran esta noche se puede volver a consolidar todo en ella.
--
-- LA FRANJA SI SE CUMPLE: PromocionesCtrl valida hora >= 12 y hora < 18, o
-- sea de 12:00 a 17:59, en los dos caminos (codigo abierto y por persona).
--
-- dias_caducidad = 1 y NO 2: la caducidad se calcula en la emision como
-- hoy + dias y es INCLUSIVA. Un codigo emitido el sabado 21 vence el domingo
-- 22 y sirve los dos dias. Tampoco puede ser 0: ofertasEnviables() filtra
-- dias_caducidad > 0 y la oferta desapareceria de Envio de Publicidad.
--
-- OJO CON LA TANDA DEL DOMINGO: un codigo emitido el 22 vencera el lunes 23.
-- Despues de ESE envio hay que cerrarlo a mano:
--   UPDATE pizzaamericana.oferta_cliente
--      SET fecha_caducidad = '2026-11-22' WHERE idenvio = <envio del domingo>;
--
-- Idempotente.

INSERT INTO pizzaamericana.oferta
      (nombre_oferta, idexcepcion, codigo_promocional, descuento_fijo_porcentaje,
       descuento_porcentaje_futuro, descuento_fijo_valor, mensaje1, mensaje2,
       dias_caducidad, tipo_caducidad, controla_hora, hora_inicio, hora_fin,
       tipo_oferta, fecha_desde, fecha_hasta, codigo_general, contact, canales,
       red_parcial, reintegro, habilitado, monto_minimo, tope_descuento,
       tiendas, aplica_a, max_usos_cliente, max_emision)
SELECT 'CUPON 50% TARDE - 21 Y 22 NOV (12 A 6)', idexcepcion, codigo_promocional,
       descuento_fijo_porcentaje, descuento_porcentaje_futuro, descuento_fijo_valor,
       'Sabado y domingo, de 12 del dia a 6 de la tarde, tu pizza con 50% de descuento en Pizza Americana. Usa tu codigo al pedir: llama al 604 4444553 o acercate a un punto de venta. Compra minima $40.000, tope de descuento $30.000.',
       mensaje2, 1, 'P', 'S', 12, 18,
       tipo_oferta, '2026-11-21', '2026-11-22', codigo_general, contact, canales,
       red_parcial, reintegro, 'S', monto_minimo, tope_descuento,
       tiendas, aplica_a, max_usos_cliente, max_emision
  FROM pizzaamericana.oferta
 WHERE idoferta = 61
   AND NOT EXISTS (SELECT 1 FROM (SELECT 1 FROM pizzaamericana.oferta
                                   WHERE nombre_oferta = 'CUPON 50% TARDE - 21 Y 22 NOV (12 A 6)') y);
