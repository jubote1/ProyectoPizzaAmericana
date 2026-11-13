-- Parametro DIASAVISOCONCILIACION: hasta cuantos dias atras cuenta el aviso de "solicitudes de conciliacion
-- pendientes" que el POS muestra al abrir y al cerrar el dia (servicio ExisteSolicitudConciliacion).
-- Antes estaba fijo en 7 dias, asi que las pendientes mas viejas nunca se avisaban. Ahora 45.
-- Lo que lleva mas de 7 dias sin resolverse se dice aparte en el mensaje.
--
-- Base GENERAL del central (172.19.0.25). valornumerico = dias. Idempotente: no pisa un valor ya cambiado.
-- Se relee cada minuto, no hace falta reiniciar el central. Para cambiarlo:
--   UPDATE general.parametros SET valornumerico = 30 WHERE valorparametro = 'DIASAVISOCONCILIACION';
INSERT INTO general.parametros (valorparametro, valornumerico)
SELECT 'DIASAVISOCONCILIACION', 45
WHERE NOT EXISTS (SELECT 1 FROM general.parametros WHERE valorparametro = 'DIASAVISOCONCILIACION');

SELECT valorparametro, valornumerico FROM general.parametros WHERE valorparametro = 'DIASAVISOCONCILIACION';
