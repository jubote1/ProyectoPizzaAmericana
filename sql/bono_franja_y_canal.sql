-- ---------------------------------------------------------------------------
-- BONO DE RECOMPRA: FRANJA HORARIA Y TIPO DE PEDIDO
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
--
-- QUE FALTABA
--
-- La campana del 2026-10-03 es "toda compra EN PUNTO DE VENTA de 12:00 a 5:00
-- pm". El motor solo sabia filtrar por FECHA y contaba TODOS los canales, asi
-- que habria pagado el 40% a quien compro a las 8 de la noche y a quien pidio
-- a domicilio. Con tope de $50.000 por persona y sin minimo, eso no es un
-- detalle.
--
-- hora_desde / hora_hasta: la franja en que la compra cuenta. Nulas = todo el
-- dia, que es el comportamiento de siempre.
--
-- tipos_pedido: lista de idtipopedido separados por coma; vacio = todos. En la
-- base de tienda 1 es domicilio, 2 mostrador, 3 para llevar. "Punto de venta"
-- son el 2 y el 3: los dos se atienden en el mostrador.
--
-- OJO CON LA VALIDACION DE FECHAS
--
-- Hasta ahora no se dejaba emitir el mismo dia en que cierra la ventana,
-- porque las compras de ese dia no habrian alcanzado a entrar. Con franja
-- horaria eso deja de ser cierto: si la ventana cierra a las 5 pm, emitir esa
-- misma noche es correcto. La validacion se afloja solo cuando hay hora_hasta.
-- ---------------------------------------------------------------------------

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'bono_campana'
             AND COLUMN_NAME = 'hora_desde');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.bono_campana ADD COLUMN hora_desde TIME NULL',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'bono_campana'
             AND COLUMN_NAME = 'hora_hasta');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.bono_campana ADD COLUMN hora_hasta TIME NULL',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'bono_campana'
             AND COLUMN_NAME = 'tipos_pedido');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.bono_campana ADD COLUMN tipos_pedido VARCHAR(50) NULL',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SELECT 'Listo. hora_desde, hora_hasta, tipos_pedido.' AS resultado;
