-- ---------------------------------------------------------------------------
-- PEDIDO RETENIDO: "existe, pero no lo manden todavia"
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
--
-- PARA QUE
--
-- Dos cosas distintas necesitan lo mismo: poder editar un pedido que aun no ha
-- salido, y poder tomar un pedido para una fecha futura. En los dos casos hace
-- falta un estado que diga "no lo mande", y que lo respete TODO el mundo.
--
-- Hoy ese estado no existe. Lo mas parecido es enviadopixel = 2, pero ese
-- significa "esperando pago virtual" y tiene su propio proceso que lo libera
-- cuando fechapagovirtual deja de ser nula. Reusarlo romperia el pago virtual.
--
-- POR QUE UN 3 EN enviadopixel Y NO UNA COLUMNA NUEVA
--
-- Las cuatro consultas de envio preguntan por un valor EXACTO: las tres de la
-- red de seguridad por enviadopixel = 0 y la de pago virtual por = 2. Un valor
-- 3 les queda invisible sin tocar ninguna de las cuatro.
--
-- Eso importa mas de lo que parece: esas consultas son cadenas de SQL armadas a
-- mano en cuatro archivos distintos de Servicios. Agregar "y no este retenido"
-- en las cuatro es cuatro oportunidades de olvidarlo, y olvidarlo significa un
-- pedido que se va solo a cocina mientras alguien lo edita. Con el 3 no hay
-- nada que recordar.
--
-- LAS OCHO PANTALLAS SI HAY QUE TAPARLAS, PERO EN UN SOLO SITIO
--
-- El boton Reenviar no mira enviadopixel: pide turno. Por eso la condicion de
-- retencion se agrega en capaDAOCC.EnvioTiendaDAO, que es el unico paso por el
-- que pasan las once entradas.
--
-- LAS COLUMNAS
--
-- retenido_motivo  EDICION o PROGRAMADO. Sirve para saber quien lo libera.
-- retenido_hasta   cuando se suelta solo. En EDICION es el seguro contra una
--                  edicion abandonada; en PROGRAMADO es la hora de salida.
-- retenido_por     el usuario que lo retuvo, para el log y para la pantalla.
--
-- retenido_hasta NO es opcional: sin el, cerrar el navegador a mitad de una
-- edicion deja el pedido trancado para siempre y toca sacarlo a mano de la
-- base. Es la misma leccion del turno de EnvioTiendaDAO.
--
-- enviadopixel_antes  el valor que tenia antes de retenerlo.
--
-- Esa ultima hace falta porque al liberar NO se puede devolver a 0 a ciegas: un
-- pedido de pago virtual estaba en 2, y ponerlo en 0 lo mandaria a cocina sin
-- que nadie hubiera pagado.
-- ---------------------------------------------------------------------------

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'pedido'
             AND COLUMN_NAME = 'retenido_motivo');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.pedido ADD COLUMN retenido_motivo VARCHAR(12) NULL
	 COMMENT ''EDICION o PROGRAMADO; nulo = no esta retenido''',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'pedido'
             AND COLUMN_NAME = 'retenido_hasta');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.pedido ADD COLUMN retenido_hasta DATETIME NULL
	 COMMENT ''Cuando se libera solo. En EDICION es el seguro; en PROGRAMADO es la hora de salida''',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'pedido'
             AND COLUMN_NAME = 'retenido_por');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.pedido ADD COLUMN retenido_por VARCHAR(50) NULL
	 COMMENT ''Usuario que lo retuvo''',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'pedido'
             AND COLUMN_NAME = 'enviadopixel_antes');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.pedido ADD COLUMN enviadopixel_antes INT NULL
	 COMMENT ''El enviadopixel que tenia antes de retenerlo; a ese vuelve al liberar''',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- El barrido de retenciones vencidas y la pantalla de programados buscan por
-- aqui. Sin indice, cada corrida recorre la tabla entera de pedidos.
SET @n := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'pedido'
             AND INDEX_NAME = 'idx_pedido_retenido');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.pedido
	 ADD INDEX idx_pedido_retenido (retenido_motivo, retenido_hasta)',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- Comprobacion: ningun pedido deberia quedar retenido al correr esto, y el
-- valor 3 no deberia existir todavia en enviadopixel.
SELECT (SELECT COUNT(*) FROM pizzaamericana.pedido WHERE retenido_motivo IS NOT NULL) AS retenidos,
       (SELECT COUNT(*) FROM pizzaamericana.pedido WHERE enviadopixel = 3)            AS en_estado_3,
       'Listo. pedido.retenido_motivo / retenido_hasta / retenido_por'                AS resultado;
