-- ============================================================================
-- CUPON 50% TARDE DEL SABADO 7 DE NOVIEMBRE DE 2026
-- Campana de prueba a 400 personas del segmento CASI PERDIDO.
--
-- QUE ES
--
-- Un cupon del 50% para usar HOY entre las 12:00 m y las 6:00 p.m. Se emite por
-- el Envio de Publicidad, que le genera un codigo distinto a cada destinatario
-- (mismo camino del envio 183 del 29-oct con la oferta 60).
--
-- LAS DOS GUARDAS DE PLATA, decididas con Juan David el 2026-11-07:
--
--   tope_descuento 30.000  Un 50% sin tope sobre el ticket de esa franja
--                          -$64.783 promedio, el mas alto de la semana- son
--                          unos $32.000 por pedido, y un pedido de grupo de
--                          $200.000 serian $100.000. El tope lo corta ahi.
--   monto_minimo   40.000  Para que el cupon no se gaste en una gaseosa.
--
-- EL HORARIO SI SE RESPETA. capaControladorCC.PromocionesCtrl valida
-- controla_hora con hora_inicio/hora_fin sobre la hora del servidor
-- (horaActual >= hora_inicio && horaActual < hora_fin), en el camino de
-- oferta_cliente, que es el que usa el codigo personal. Por eso el codigo va
-- POR PERSONA (codigo_promocional = 'S') y no abierto: un codigo general se
-- salta esa validacion en la version que hoy esta desplegada.
--
-- *** OJO: HAY QUE APAGARLA HOY A LAS 6 P.M. ***
--
-- dias_caducidad = 1 con tipo_caducidad 'P' pone fecha_caducidad = manana, y la
-- caducidad es INCLUSIVA (vence al final del dia). Como el horario 12-18 se
-- valida todos los dias, el cupon tambien serviria el domingo de 12 a 6, que es
-- el segundo mejor dia de la semana y no necesita descuento. No se puede poner
-- 0 dias: sin fecha de caducidad el correo NO SALE a proposito (guarda de
-- CorreoOferta). Para cerrarla:
--
--     UPDATE pizzaamericana.oferta SET habilitado = 'N' WHERE idoferta = @id;
--
-- Idempotente: si ya existe no la duplica.
-- ============================================================================

USE pizzaamericana;

INSERT INTO oferta (
  nombre_oferta, idexcepcion, codigo_promocional,
  descuento_fijo_porcentaje, descuento_porcentaje_futuro, descuento_fijo_valor,
  mensaje1, dias_caducidad, tipo_caducidad,
  controla_hora, hora_inicio, hora_fin,
  tipo_oferta, fecha_desde, fecha_hasta, codigo_general,
  contact, red_parcial, reintegro, habilitado,
  monto_minimo, tope_descuento, tiendas, aplica_a,
  max_usos_cliente, max_emision)
SELECT
  'CUPON 50% TARDE SABADO 7 NOV', 0, 'S',
  50, 0, 0,
  'Hoy sabado, de 12 del dia a 6 de la tarde, tu pizza con 50% de descuento en Pizza Americana. Usa tu codigo al pedir: llama al 604 4444553 o acercate a un punto de venta. Compra minima $40.000, tope de descuento $30.000. Solo por hoy.',
  1, 'P',
  'S', 12, 18,
  'C', '2026-11-07', '2026-11-07', '',
  'S', 'N', 'N', 'S',
  40000, 30000, NULL, 'T',
  1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM (SELECT idoferta FROM oferta WHERE nombre_oferta = 'CUPON 50% TARDE SABADO 7 NOV') x);

-- ============================================================================
-- COMPROBACION
-- ============================================================================
SELECT idoferta, nombre_oferta, descuento_fijo_porcentaje AS pct,
       monto_minimo, tope_descuento, controla_hora, hora_inicio, hora_fin,
       dias_caducidad, max_usos_cliente, habilitado, fecha_desde, fecha_hasta
FROM oferta WHERE nombre_oferta = 'CUPON 50% TARDE SABADO 7 NOV';
