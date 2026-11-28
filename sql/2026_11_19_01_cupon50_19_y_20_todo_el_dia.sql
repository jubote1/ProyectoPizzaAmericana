-- CUPON 50% 25 ANOS - HOY JUEVES 19 Y MANANA VIERNES 20, TODO EL DIA
-- Destino: segmento POR RECUPERAR.
--
-- Sigue siendo la oferta 61, la unica del cupon del 50% (decision del 14-nov:
-- no llenarse de ofertas y medir junto; cada campana se separa por
-- oferta_cliente.idenvio).
--
-- Cambios frente a la corrida del 16-nov:
--   controla_hora 'S' 12-19  ->  'N'  (todo el dia)
--   fechas 16 y 16           ->  19 y 20
--
-- dias_caducidad SE QUEDA EN 1 y eso es lo que da los dos dias: la caducidad
-- se calcula en la emision como hoy + dias_caducidad y es INCLUSIVA hasta el
-- final del dia, asi que un codigo emitido el 19 sirve el 19 y el 20.
-- Poner 2 daria tres dias. Y NO puede ser 0: ofertasEnviables() filtra
-- dias_caducidad > 0 y la oferta desapareceria de Envio de Publicidad.
--
-- Los 2.021 codigos anteriores estan todos vencidos o usados (comprobado: 0
-- vivos), asi que reusar la oferta no revive ninguno.
--
-- Idempotente.

UPDATE pizzaamericana.oferta
   SET habilitado     = 'S',
       fecha_desde    = '2026-11-19',
       fecha_hasta    = '2026-11-20',
       dias_caducidad = 1,
       tipo_caducidad = 'P',
       controla_hora  = 'N',
       hora_inicio    = 0,
       hora_fin       = 0,
       mensaje1       = 'HOY Y MANANA, todo el dia, tu pizza con 50% de descuento en Pizza Americana. Usa tu codigo al pedir: llama al 604 4444553 o acercate a un punto de venta. Compra minima $40.000, tope de descuento $30.000.'
 WHERE idoferta = 61;
