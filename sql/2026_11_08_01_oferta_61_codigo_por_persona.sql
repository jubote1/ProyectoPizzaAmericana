-- ============================================================================
-- OFERTA 61: PASARLA A CODIGO POR PERSONA Y MOVERLA A LUNES 9 Y MARTES 10
-- 2026-11-08
--
-- POR QUE
--
-- La oferta 61 se creo el sabado 7 como codigo ABIERTO (codigo_general
-- 'TARDE50') porque era la unica forma de mandarla masiva por Brevo: el codigo
-- por persona obligaba al correo directo, uno cada 30 segundos con tope de 50.
--
-- Eso ya no es cierto. El commit 9124f8a hace que el codigo POR PERSONA salga
-- por Brevo, con el codigo en los params del destinatario. Asi que la oferta
-- vuelve a lo que siempre debio ser:
--
--   codigo_promocional = 'S'   cada persona recibe SU codigo, de un solo uso
--   codigo_general     = ''    ya no hay un codigo igual para todos
--   tipo_oferta        = 'C'   no es abierta
--
-- QUE SE GANA con el codigo por persona:
--
--   - Se respeta max_usos_cliente = 1. Un codigo abierto lo usa cualquiera, las
--     veces que quiera, y quien lo reenvie por WhatsApp tambien.
--   - Se respeta la caducidad por persona.
--   - Queda trazado quien redimio: oferta_cliente.idenvio ata el codigo a la
--     campana, y de ahi sale el embudo (cuantos recibieron, cuantos redimieron).
--
-- EL HORARIO 12-18 AHORA SI SE CUMPLE POR LOS DOS CAMINOS. Hasta el commit
-- 9124f8a, un codigo abierto NO validaba controla_hora: la oferta decia 12 a 18
-- y servia a cualquier hora. Por el codigo por persona siempre se valido.
--
-- LAS FECHAS: lunes 9 y martes 10, los dos peores dias de la semana. Medido sin
-- festivos del 12-oct al 6-nov: lunes $15,5M y martes $16,6M, contra viernes
-- $45,0M, sabado $70,2M y domingo $67,8M.
--
-- *** NO SIRVE DE NADA SIN DESPLEGAR EL WAR DEL CENTRAL (commit 9124f8a). ***
-- Sin el, la pantalla de Envio de Publicidad sigue obligando al correo directo
-- cuando hay oferta, y son 50 por tanda.
--
-- Queda HABILITADO = 'N'. Se prende cuando se vaya a mandar la campana:
--     UPDATE pizzaamericana.oferta SET habilitado = 'S' WHERE idoferta = 61;
--
-- Idempotente.
-- ============================================================================

USE pizzaamericana;

UPDATE oferta
   SET nombre_oferta      = 'CUPON 50% LUNES 9 Y MARTES 10 NOV',
       codigo_promocional = 'S',
       codigo_general     = '',
       tipo_oferta        = 'C',
       fecha_desde        = '2026-11-09',
       fecha_hasta        = '2026-11-10',
       dias_caducidad     = 2,
       tipo_caducidad     = 'P',
       controla_hora      = 'S',
       hora_inicio        = 12,
       hora_fin           = 18,
       max_usos_cliente   = 1,
       monto_minimo       = 40000,
       tope_descuento     = 30000,
       habilitado         = 'N',
       mensaje1           = 'Lunes y martes, de 12 del dia a 6 de la tarde, tu pizza con 50% de descuento en Pizza Americana. Usa tu codigo al pedir: llama al 604 4444553 o acercate a un punto de venta. Compra minima $40.000, tope de descuento $30.000.'
 WHERE idoferta = 61;

-- ============================================================================
-- COMPROBACION
-- ============================================================================
SELECT idoferta, nombre_oferta, codigo_promocional AS cod_x_persona, codigo_general,
       tipo_oferta, descuento_fijo_porcentaje AS pct, monto_minimo, tope_descuento,
       controla_hora, hora_inicio, hora_fin, dias_caducidad, max_usos_cliente,
       habilitado, fecha_desde, fecha_hasta
  FROM oferta WHERE idoferta = 61;
