-- ---------------------------------------------------------------------------
-- LA IDEMPOTENCIA DE BOLD ESTABA EN EL CAMPO EQUIVOCADO
--
-- Se corre en el CENTRAL (172.19.0.25), esquema pizzaamericana. Idempotente.
--
-- QUE PASA
--
-- Bold reintenta la misma notificacion cuando no recibe el 200 a tiempo. El
-- reintento llega entre 910 y 967 segundos despues -promedio 15 minutos 40
-- segundos- y trae:
--
--   el mismo id_notificacion ... 74 de 74 casos
--   el mismo payment_id y el mismo monto
--   un hash_cuerpo DISTINTO .... 0 de 74 coinciden
--
-- Y la llave unica de log_evento_bold es uk_hash_cuerpo. Por eso la guardia de
-- idempotencia nunca atrapa un reintento: Bold cambia algo del cuerpo y el
-- hash sale distinto. id_notificacion, que es el campo que SI se repite, no
-- tenia ni indice.
--
-- Resultado: 74 pagos guardados dos veces, 3.201.110 pesos de mas en el log,
-- entre el 21-sep y el 28-oct. 67 de los 74 fueron el 16 de octubre, entre las
-- 16:00 y las 23:00: esa tarde el central respondia lento y Bold cortaba.
--
-- LO QUE ESTO NO ES
--
-- Esto NO es lo que descuadra la conciliacion diaria. La tabla de la tienda,
-- sonoqr_movimiento, tiene UNIQUE (payment_id, tipo_evento) y rechaza la
-- copia: verificado, cero duplicados en Manrique, America, San Antonio,
-- Niquia y Piloto, con el conteo central igual al de la tienda en las cinco.
-- Lo que descuadra el reporte son cobros reales sin pedido detras, que es otro
-- tema. Esto es basura en el log, y hoy nada suma plata desde aqui.
--
-- NO HACE FALTA TOCAR CODIGO
--
-- LogEventoBoldDAO.insertar ya captura SQLIntegrityConstraintViolationException
-- y devuelve DUPLICADO; BoldWebhook entonces responde 200 sin volver a
-- entregarle a la tienda y lo deja anotado en el log como "(reintento, ya
-- estaba)". Con la llave nueva, eso empieza a pasar tambien con los reintentos.
--
-- POR QUE id_notificacion SIRVE DE LLAVE
--
-- Verificado sobre las 2.814 filas: 0 nulos, 0 vacios y 0 casos del mismo id
-- en tipos de evento distintos.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. RESPALDO ANTES DE BORRAR
--
-- Las filas que se van son copias exactas, pero se guardan igual: es
-- produccion y el costo de conservarlas es cero.
-- ===========================================================================
CREATE TABLE IF NOT EXISTS pizzaamericana.log_evento_bold_duplicado LIKE pizzaamericana.log_evento_bold;

-- La copia a conservar es la de MENOR id: la primera que llego. La que se va
-- es el reintento.
INSERT INTO pizzaamericana.log_evento_bold_duplicado
SELECT l.*
  FROM pizzaamericana.log_evento_bold l
  JOIN (SELECT id_notificacion, MIN(idlog_evento_bold) AS primera
          FROM pizzaamericana.log_evento_bold
         GROUP BY id_notificacion
        HAVING COUNT(*) > 1) d
    ON d.id_notificacion = l.id_notificacion
   AND l.idlog_evento_bold > d.primera
 WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.log_evento_bold_duplicado b
                    WHERE b.idlog_evento_bold = l.idlog_evento_bold);

-- ===========================================================================
-- 2. BORRAR LOS REINTENTOS
-- ===========================================================================
DELETE l
  FROM pizzaamericana.log_evento_bold l
  JOIN (SELECT id_notificacion, MIN(idlog_evento_bold) AS primera
          FROM pizzaamericana.log_evento_bold
         GROUP BY id_notificacion
        HAVING COUNT(*) > 1) d
    ON d.id_notificacion = l.id_notificacion
   AND l.idlog_evento_bold > d.primera;

-- ===========================================================================
-- 3. LA LLAVE QUE DEBIO ESTAR DESDE EL PRINCIPIO
--
-- uk_hash_cuerpo se deja: no estorba y sigue atrapando el caso de un cuerpo
-- identico reenviado tal cual.
-- ===========================================================================
SET @hay := (SELECT COUNT(*) FROM information_schema.statistics
              WHERE table_schema = 'pizzaamericana' AND table_name = 'log_evento_bold'
                AND index_name = 'uk_id_notificacion');
SET @s := IF(@hay = 0,
	'ALTER TABLE pizzaamericana.log_evento_bold
	   ADD UNIQUE KEY uk_id_notificacion (id_notificacion)',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================
SELECT COUNT(*) AS filas,
       COUNT(DISTINCT id_notificacion) AS notificaciones,
       COUNT(DISTINCT payment_id) AS pagos
  FROM pizzaamericana.log_evento_bold;

SELECT COUNT(*) AS reintentos_guardados_aparte FROM pizzaamericana.log_evento_bold_duplicado;
