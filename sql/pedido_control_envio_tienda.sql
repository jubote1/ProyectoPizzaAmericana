-- ---------------------------------------------------------------------------
-- El turno de envio a la tienda: una columna para que un pedido no se mande dos veces
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
--
-- QUE PROBLEMA RESUELVE
--
-- El 2026-09-21 en Envigado salieron duplicados los pedidos 192514/192515 y
-- 192516/192517: dos pedidos iguales en cocina, creados con UN SEGUNDO de
-- diferencia. No fue un doble envio de la tienda virtual -el control de
-- pedido_tienda_virtual funciona-; fue que el envio automatico y el reenvio
-- manual de una persona salieron al mismo tiempo.
--
-- Nadie estaba mirando si el pedido YA habia llegado a la tienda. Hay ocho
-- pantallas con boton de reenviar y tres procesos de Servicios que reenvian
-- solos, y ninguno de los once preguntaba.
--
-- POR QUE UNA COLUMNA NUEVA Y NO enviadopixel
--
-- enviadopixel ya significa tres cosas (0 pendiente, 1 enviado, 2 virtual
-- finalizado) y la leen decenas de consultas en tres proyectos. Meterle un
-- cuarto valor para decir "se esta enviando ahora mismo" habria cambiado en
-- silencio el resultado de todas esas consultas.
--
-- Ademas enviadopixel llega tarde: se pone en 1 cuando la TIENDA contesta con
-- su numero, varios segundos despues. Justo en esos segundos es cuando se
-- produce el choque, asi que hacia falta una marca que se ponga ANTES de
-- mandar, no despues.
--
-- POR QUE GUARDA LA HORA Y NO UN SI/NO
--
-- Con un si/no, un envio que se muera a mitad de camino -el navegador se
-- cierra, la tienda no contesta- dejaria el pedido marcado para siempre y nadie
-- podria reenviarlo nunca mas. Guardando la hora, la marca se vence sola al
-- minuto y el pedido vuelve a poder mandarse. Un minuto es mucho mas de lo que
-- tarda un envio bueno y mucho menos de lo que aguanta un cliente esperando.
--
-- ALGORITHM=INSTANT, Y NO ES CAPRICHO
--
-- pedido es la tabla mas caliente del sistema y esto se corre con las once
-- tiendas vendiendo. INSTANT agrega la columna sin reconstruir la tabla ni
-- bloquearla. Si por lo que sea MySQL no pudiera hacerlo asi, es mejor que el
-- script FALLE y se corra en la madrugada, a que se quede media hora con la
-- tabla de pedidos trancada.
-- ---------------------------------------------------------------------------

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'pizzaamericana'
                  AND TABLE_NAME   = 'pedido'
                  AND COLUMN_NAME  = 'envio_tienda_en_curso');

SET @sql = IF(@existe = 0,
  'ALTER TABLE pizzaamericana.pedido
     ADD COLUMN envio_tienda_en_curso DATETIME NULL
     COMMENT ''Hora en que se empezo a mandar el pedido a la tienda. Vence al minuto. La pone EnvioTiendaDAO para que dos envios simultaneos no dupliquen el pedido.''
     , ALGORITHM=INSTANT',
  'SELECT ''La columna ya existe, no se hace nada'' AS resultado');

PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Como quedo
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'pedido'
   AND COLUMN_NAME IN ('enviadopixel', 'numposheader', 'fechaenviotienda', 'envio_tienda_en_curso');
