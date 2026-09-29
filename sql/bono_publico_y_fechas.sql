-- ---------------------------------------------------------------------------
-- BONO DE RECOMPRA: PUBLICO INVITADO Y FECHAS EXPLICITAS
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente y
-- aditivo.
--
-- QUE RESUELVE
--
-- 1. HOY EL BONO SE LO GANA CUALQUIERA QUE COMPRE. No hay forma de decir "solo
--    los que invite". Eso obliga a regalarle a todo el que pase por caja
--    aunque la campana se haya pensado para un publico escogido.
--
-- 2. NO SE PUEDE DECIR CUANDO EMPIEZA A VALER EL BONO. En un codigo personal,
--    motivoDeRechazo solo mira la caducidad del codigo: las fechas desde/hasta
--    de la oferta mandan unicamente en los codigos ABIERTOS. O sea que un bono
--    emitido el miercoles se puede usar el jueves, el viernes y el fin de
--    semana, que es justo cuando no hace falta.
--
--    La solucion no es validar mas al redimir sino EMITIR EL DIA QUE TOCA: si
--    el bono no existe antes del lunes, no hay nada que redimir antes del
--    lunes. Por eso la campana ahora dice que dia emite.
--
-- 3. El mensaje de invitacion tiene que poder decir las fechas -"lo que compres
--    hoy te lo devolvemos en un bono para redimir del 5 al 7, y te llega el
--    lunes"-, y para eso las fechas tienen que estar guardadas, no en la cabeza
--    de quien arma la campana.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. PUBLICO: ABIERTA O POR INVITACION
-- ===========================================================================

-- S = se lo gana cualquiera que compre en la ventana.
-- N = solo las personas a las que se les mando la invitacion.
SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'bono_campana'
             AND COLUMN_NAME = 'abierta');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.bono_campana ADD COLUMN abierta CHAR(1) NOT NULL DEFAULT ''S''',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- La tanda de Envio de Publicidad con la que se invito. De ahi sale QUIENES
-- son el publico: crm.campana_destinatario ya tiene la lista, persona por
-- persona, y ademas dice a quien le llego de verdad y a quien no.
--
-- Se reusa y no se inventa una lista nueva a proposito: dos listas del mismo
-- publico se separan, y el dia que no coincidan nadie sabria cual manda.
SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'bono_campana'
             AND COLUMN_NAME = 'idenvio');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.bono_campana ADD COLUMN idenvio BIGINT NULL',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ===========================================================================
-- 2. FECHAS EXPLICITAS
-- ===========================================================================

-- El dia en que se emite. Antes se emitia "cuando cerrara la ventana", que
-- para una ventana de un solo dia significa el dia siguiente, y eso pone el
-- bono a disposicion del fin de semana.
SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'bono_campana'
             AND COLUMN_NAME = 'fecha_emision');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.bono_campana ADD COLUMN fecha_emision DATE NULL',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- Desde cuando y hasta cuando se puede redimir. redime_desde es informativo
-- -lo que dice el mensaje-; quien de verdad lo hace cumplir es fecha_emision,
-- porque antes de emitir no hay codigo. redime_hasta SI manda: de ahi sale la
-- caducidad del codigo, que es lo unico que el redimir valida.
SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'bono_campana'
             AND COLUMN_NAME = 'redime_desde');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.bono_campana ADD COLUMN redime_desde DATE NULL',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'bono_campana'
             AND COLUMN_NAME = 'redime_hasta');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.bono_campana ADD COLUMN redime_hasta DATE NULL',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SELECT 'Listo. abierta, idenvio, fecha_emision, redime_desde, redime_hasta.' AS resultado;
