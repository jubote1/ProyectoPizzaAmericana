-- ============================================================================
-- UNIFICAR EL CUPON DEL 50% EN UNA SOLA OFERTA (la 61)
-- 2026-11-14
--
-- QUE PASABA
--
-- Habia TRES ofertas identicas del 50%, una por cada dia que se mando:
--     61  CUPON 50% 25 ANOS - 8, 9 Y 10 NOV     500 codigos, 3 usados
--     63  CUPON 50% 25 ANOS - SOLO HOY 11 NOV   200 codigos, 2 usados
--     64  CUPON 50% 25 ANOS - SOLO HOY 12 NOV   283 codigos, 3 usados
--
-- Mismo 50%, mismo minimo, mismo tope, mismo uso por cliente. Lo unico que
-- cambiaba era la fecha. Decision de Juan David: una sola oferta que se
-- reusa, y se mide junto.
--
-- POR QUE AHORA SI SE PUEDE REUSAR (y por que el reparo anterior no aplicaba)
--
-- El 12-nov se creo la 64 aparte por miedo a que mover la fecha reviviera los
-- codigos viejos sin usar. Leyendo el codigo, eso NO pasa con codigo por
-- persona:
--
--   PromocionesCtrl.retornarOfertaCodigoPromocional, rama de oferta_cliente,
--   valida (1) fecha_caducidad DEL CODIGO, inclusiva hasta el final del dia,
--   (2) la franja horaria de la oferta y (3) el canal. NO mira fecha_desde /
--   fecha_hasta de la oferta: esas solo gobiernan el codigo ABIERTO, en
--   OfertaDAO.obtenerOfertaCodigoPromocional.
--
-- O sea que a un codigo por persona lo mata su propia caducidad. Los 497 + 198
-- + 280 codigos sin usar caducaron el 10, el 11 y el 13 de noviembre. Reusar
-- la oferta no los revive. Medido antes de tocar nada:
--     select sum(utilizada='N' and anulada='N' and fecha_caducidad >= curdate())
--       from oferta_cliente where idoferta in (61,63,64);   ->  0
--
-- LA MEDICION NO SE PIERDE al compartir oferta: oferta_cliente guarda idenvio,
-- asi que cada campana se sigue separando por envio, y ahora ademas se puede
-- sumar todo el historico del cupon por idoferta = 61.
--
-- LA FRANJA 12 A 18 SI SE CUMPLE. horaDentroDeLaFranja y la rama de
-- oferta_cliente comparan horaActual >= 12 && horaActual < 18, o sea sirve de
-- 12:00 a 17:59.
--
-- *** dias_caducidad NO PUEDE SER 0. ***
-- CodigoPromoDAO.ofertasEnviables() exige "o.dias_caducidad > 0"; con 0 la
-- oferta desaparece de Envio de Publicidad. Queda en 1, asi que el codigo que
-- se emita hoy caduca MANANA. Para que valga de verdad solo hoy, despues del
-- envio hay que bajarle la caducidad a los codigos de hoy:
--
--     UPDATE pizzaamericana.oferta_cliente
--        SET fecha_caducidad = CURDATE()
--      WHERE idenvio = <el envio de hoy>;
--
-- Sin eso el codigo tambien sirve manana domingo entre 12 y 18.
--
-- Idempotente.
-- ============================================================================

USE pizzaamericana;

-- 1. La 61 queda como LA oferta del cupon, configurada para hoy 12 a 18 ------
UPDATE oferta
   SET nombre_oferta      = 'CUPON 50% RECUPERACION - 25 ANOS',
       codigo_promocional = 'S',
       codigo_general     = '',
       tipo_oferta        = 'C',
       descuento_fijo_porcentaje = 50,
       monto_minimo       = 40000,
       tope_descuento     = 30000,
       max_usos_cliente   = 1,
       dias_caducidad     = 1,
       tipo_caducidad     = 'P',
       controla_hora      = 'S',
       hora_inicio        = 12,
       hora_fin           = 18,
       fecha_desde        = '2026-11-14',
       fecha_hasta        = '2026-11-14',
       aplica_a           = 'T',
       canales            = '',
       habilitado         = 'S',
       mensaje1           = 'SOLO HOY, de 12 del dia a 6 de la tarde, tu pizza con 50% de descuento en Pizza Americana. Usa tu codigo al pedir: llama al 604 4444553 o acercate a un punto de venta. Compra minima $40.000, tope de descuento $30.000.'
 WHERE idoferta = 61;

-- 2. Las dos repetidas salen de circulacion -----------------------------------
-- No se borran: guardan el historico de las campanas del 11 y del 12, y sus
-- codigos ya estan caducados. Apagarlas solo las saca de la pantalla de envio
-- (ofertasEnviables exige habilitado = 'S') y de la lista de ofertas activas.
UPDATE oferta SET habilitado = 'N' WHERE idoferta IN (63, 64);

-- 3. La plantilla de Brevo ---------------------------------------------------
-- La 84 se edito en Brevo el 2026-11-14: la fecha vive en TRES sitios del arte
-- (el sello, el parrafo del saludo y la letra menuda) y ahora todos dicen
-- "sabado 14 de noviembre". Se le agrego la franja en cuatro puntos: el sello
-- ("12 M. A 6 P.M."), el saludo, el pie del cupon y la letra menuda, que antes
-- decia "en el horario de atencion de cada tienda" y ahora dice "entre las
-- 12:00 del dia y las 6:00 de la tarde". Aqui solo el nombre que ve el central.
UPDATE plantilla_brevo
   SET nombre = 'Cupon 50% recuperacion (14 nov 12 a 6 pm)'
 WHERE idplantilla = 84;

-- ============================================================================
-- COMPROBACION
-- ============================================================================
SELECT id, idplantilla, nombre, categoria FROM plantilla_brevo WHERE idplantilla = 84;

SELECT idoferta, nombre_oferta, descuento_fijo_porcentaje pct, monto_minimo,
       tope_descuento, controla_hora, hora_inicio, hora_fin, dias_caducidad,
       fecha_desde, fecha_hasta, habilitado
  FROM oferta WHERE idoferta IN (61, 63, 64) ORDER BY idoferta;

-- Ningun codigo viejo puede revivir: tiene que dar 0.
SELECT SUM(utilizada = 'N' AND anulada = 'N' AND fecha_caducidad >= CURDATE()) AS codigos_viejos_vivos
  FROM oferta_cliente WHERE idoferta IN (61, 63, 64);

-- Historico del cupon, para medir junto.
SELECT idoferta, idenvio, COUNT(*) codigos, SUM(utilizada = 'S') redimidos
  FROM oferta_cliente WHERE idoferta IN (61, 63, 64)
 GROUP BY idoferta, idenvio ORDER BY idenvio;
