-- ---------------------------------------------------------------------------
-- BONO DE RECOMPRA: SE AMARRA A LA CAMPANA, NO A UN ENVIO
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
--
-- QUE ESTABA MAL
--
-- La pantalla pedia "el envio con el que se aviso" y guardaba un idenvio. Pero
-- el motor NUNCA uso ese envio solo: resolvia la campana a partir de el y
-- contaba todas sus tandas. O sea que el campo decia una cosa y el programa
-- hacia otra.
--
-- Eso importa porque una invitacion de verdad son varias tandas -500 y 500, y
-- a segmentos distintos: ORO el martes, FIEL el miercoles-. Quien leia la
-- pantalla creia que solo contaba una, y no se atrevia a mandar mas. O peor:
-- escogia la tanda equivocada y el bono quedaba colgado de otra campana sin
-- que nada lo advirtiera.
--
-- Ahora se guarda la campana, que es lo que el motor siempre uso.
--
-- idenvio NO SE BORRA
--
-- Queda para las tres campanas viejas y como rastro de cual fue la tanda que
-- se escogio en su momento. El motor ya no lo mira.
-- ---------------------------------------------------------------------------

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'bono_campana'
             AND COLUMN_NAME = 'idcampana');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.bono_campana ADD COLUMN idcampana BIGINT NULL
	 COMMENT ''La campana de Envio de Publicidad que invito. Cuentan TODAS sus tandas''',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- Las que ya existen quedan apuntando a la campana de su envio: es exactamente
-- lo que el motor venia calculando, asi que ninguna cambia de comportamiento.
UPDATE pizzaamericana.bono_campana b
  JOIN crm.campana_envio e ON e.idenvio = b.idenvio
   SET b.idcampana = e.idcampana
 WHERE b.idcampana IS NULL
   AND b.idenvio IS NOT NULL;

SELECT idbono, nombre, idenvio, idcampana,
       CASE WHEN abierta = 'S' THEN 'abierta: no necesita campana'
            WHEN idcampana IS NOT NULL THEN 'ok'
            ELSE 'OJO: cerrada y sin campana' END AS revision
  FROM pizzaamericana.bono_campana
 ORDER BY idbono;
