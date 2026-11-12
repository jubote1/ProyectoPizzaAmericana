-- ============================================================================
-- OFERTA 64: CUPON 50% - SOLO HOY JUEVES 12 DE NOVIEMBRE
-- 2026-11-12
--
-- POR QUE UNA OFERTA NUEVA Y NO MOVERLE LA FECHA A LA 63
--
-- La 63 (`CUPON 50% 25 ANOS - SOLO HOY 11 NOV`) ya tiene 200 codigos emitidos
-- y 2 redimidos. Moverle `fecha_desde` / `fecha_hasta` a hoy haria dos danos:
--
--   1. Los 198 codigos que NO se usaron ayer reviven hoy. Serian 198 personas
--      de ayer + 200 de hoy compitiendo por la misma oferta, sin haberlo
--      decidido.
--   2. Se pierde la medicion. Las redenciones se atan a la oferta por
--      `oferta_cliente.idoferta` y a la campana por `idenvio`; con la misma
--      oferta en dos dias ya no se puede decir cual campana movio que.
--
-- La campana del 11-nov quedo medida: 200 enviados, 2 redimieron (1,0%), 4
-- compraron (2,0%), contra 0,37% del grupo de control. Esa linea base solo
-- sirve si cada campana tiene su propia oferta.
--
-- LA 63 NO SE TOCA. Su `fecha_hasta` es 2026-11-11, o sea que ya esta cerrada
-- sola: los codigos de ayer no validan hoy.
--
-- QUE QUEDA IGUAL QUE AYER (a proposito, para que las dos campanas sean
-- comparables): 50%, minimo $40.000, tope $30.000, 1 uso por cliente, codigo
-- POR PERSONA (`codigo_promocional='S'`, `tipo_oferta='C'`), todo el dia,
-- cualquier canal.
--
-- EL MINIMO DE $40.000 QUEDA PENDIENTE DE DECISION. De los 4 que compraron
-- ayer, 1 no pudo usar el codigo porque su pedido fue de $27.990. Si se decide
-- bajarlo es un solo UPDATE:
--     UPDATE pizzaamericana.oferta SET monto_minimo = 30000 WHERE idoferta = 64;
--
-- `max_emision = 0` (sin tope de emision), igual que ayer. Si se quiere que la
-- campana se detenga sola a los 200:
--     UPDATE pizzaamericana.oferta SET max_emision = 200 WHERE idoferta = 64;
--
-- COMO SE CIERRA, si hay que abortar:
--     UPDATE pizzaamericana.oferta SET habilitado = 'N' WHERE idoferta = 64;
--
-- Idempotente: se puede correr varias veces. No fija el idoferta a mano, lo
-- busca por nombre.
-- ============================================================================

USE pizzaamericana;

-- 1. Crearla solo si no existe ------------------------------------------------
INSERT INTO oferta (nombre_oferta, idexcepcion, codigo_promocional,
                    descuento_fijo_porcentaje, descuento_porcentaje_futuro,
                    descuento_fijo_valor, dias_caducidad, tipo_caducidad,
                    controla_hora, hora_inicio, hora_fin, tipo_oferta,
                    fecha_desde, fecha_hasta, codigo_general, contact, canales,
                    red_parcial, reintegro, habilitado, monto_minimo,
                    tope_descuento, aplica_a, max_usos_cliente, max_emision)
SELECT 'CUPON 50% 25 ANOS - SOLO HOY 12 NOV', 0, 'S',
       50, 0,
       0, 1, 'P',
       'N', 0, 0, 'C',
       '2026-11-12', '2026-11-12', '', 'S', '',
       'N', 'N', 'S', 40000,
       30000, 'T', 1, 0
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM (SELECT idoferta FROM oferta
                                   WHERE nombre_oferta = 'CUPON 50% 25 ANOS - SOLO HOY 12 NOV') x);

-- 2. Dejarla con los valores correctos aunque ya existiera --------------------
UPDATE oferta
   SET codigo_promocional        = 'S',
       codigo_general            = '',
       tipo_oferta               = 'C',
       descuento_fijo_porcentaje = 50,
       monto_minimo              = 40000,
       tope_descuento            = 30000,
       max_usos_cliente          = 1,
       dias_caducidad            = 1,
       tipo_caducidad            = 'P',
       controla_hora             = 'N',
       hora_inicio               = 0,
       hora_fin                  = 0,
       fecha_desde               = '2026-11-12',
       fecha_hasta               = '2026-11-12',
       aplica_a                  = 'T',
       canales                   = '',
       contact                   = 'S',
       habilitado                = 'S',
       mensaje1                  = 'SOLO HOY jueves 12 de noviembre, tu pizza con 50% de descuento en Pizza Americana. Usa tu codigo al pedir: llama al 604 4444553 o acercate a un punto de venta. Compra minima $40.000, tope de descuento $30.000.'
 WHERE nombre_oferta = 'CUPON 50% 25 ANOS - SOLO HOY 12 NOV';

-- 3. La plantilla de Brevo --------------------------------------------------
-- La plantilla 84 se edito en Brevo el 2026-11-12: la fecha estaba escrita en
-- TRES sitios del arte (el sello rojo, el parrafo del saludo y la letra
-- menuda) mas el <title>, que venia del 9-10 nov. Todos dicen ahora
-- "jueves 12 de noviembre". El asunto no lleva fecha, no se toco.
-- Aqui solo se actualiza el nombre con el que la ve Envio de Publicidad.
UPDATE plantilla_brevo
   SET nombre = 'Cupon 50% 25 anos (SOLO HOY 12 nov)'
 WHERE idplantilla = 84;

-- ============================================================================
-- COMPROBACION
-- ============================================================================
SELECT id, idplantilla, nombre, categoria FROM plantilla_brevo WHERE idplantilla = 84;

SELECT idoferta, nombre_oferta, codigo_promocional AS cod_x_persona,
       codigo_general, tipo_oferta, descuento_fijo_porcentaje AS pct,
       monto_minimo, tope_descuento, max_usos_cliente, max_emision,
       dias_caducidad, controla_hora, fecha_desde, fecha_hasta, habilitado,
       canales, aplica_a
  FROM oferta
 WHERE idoferta IN (63, 64)
 ORDER BY idoferta;
