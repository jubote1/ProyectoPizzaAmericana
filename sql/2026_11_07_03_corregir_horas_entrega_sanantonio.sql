-- ---------------------------------------------------------------------------
-- CORREGIR LAS HORAS DE ENTREGA DE JUAN GUILLERMO GOMEZ -- SAN ANTONIO, 6-NOV
--
-- QUE PASO
--
-- A Juan Guillermo Gomez (general.empleado 800, INTERNO, no temporal) se le
-- cayo la app del domiciliario entre las 18:02 y las 20:04 del 2026-11-06.
-- Las cinco entregas de ese lapso quedaron sin marcar en ruta y el POS les
-- puso como hora de entrega la HORA DE REGRESO del despacho, que es el unico
-- dato que tenia.
--
-- Esta en los datos, no es una suposicion: las cinco alertas de
-- pizzaamericana.alerta_entrega_domiciliario salieron el mismo minuto -las
-- 20:07- una cada 7 a 10 segundos, TODAS desde el mismo punto, a 48 metros de
-- la tienda. Las marco de un solo golpe, parado en la tienda, cuando la app
-- volvio. Las cinco quedaron con "Domiciliario se encuentra lejos de la
-- ubicacion del cliente al registrar entrega": esas novedades NO son fraude.
--
-- COMO SE SACO LA HORA REAL
--
-- Del rastro GPS (pizzaamericana.ubicacion_domiciliario, 2.779 puntos de el
-- ese dia, uno cada ~4 a 20 segundos). Criterio UNICO para los cinco, aplicado
-- igual y sin escoger a mano: el PRIMER punto, dentro de la ventana de su
-- despacho, en que llega a 50 metros o menos de la direccion del cliente.
--
--   pedido   insertado   estaba       era       GPS        queda
--   258358   17:40:13    18:26:38     46 min    18:09:56   30 min
--   258361   17:49:40    19:23:44     94 min    18:47:33   58 min  <- pasa a cumplido
--   258368   18:18:06    19:23:43     65 min    19:06:17   48 min  <- pasa a cumplido
--   258385   19:08:04    20:04:36     56 min    19:45:46   38 min
--   258386   19:10:09    20:04:35     54 min    19:33:02   23 min
--
-- Los dos que estaban tarde quedan cumplidos con su hora real, que es lo que
-- la administradora de San Antonio garantizaba. Los otros tres ya estaban en
-- verde, pero tenian la misma hora mala y se corrigen por consistencia: si no,
-- el promedio de la tienda sigue inflado.
--
-- Se corrige SOLO hora_entrega. hora_salida y hora_regreso quedan como estan:
-- esas si son reales.
--
-- Va en los DOS lados: la tienda en vivo y el datamart, que ya habia replicado
-- el dia. Si solo se corrige uno, el reporte da distinto segun si el dia esta
-- cerrado o no.
--
-- Reversa: sql/respaldos/devolver_horas_entrega_sanantonio_2026-11-06.sql
-- ---------------------------------------------------------------------------

-- 1) TIENDA SAN ANTONIO (192.168.10.49, esquema tiendaamericana) -----------
UPDATE tiendaamericana.despacho_real_det SET hora_entrega='2026-11-06 18:09:56' WHERE despacho_real_id=4427 AND id_pedido=258358;
UPDATE tiendaamericana.despacho_real_det SET hora_entrega='2026-11-06 18:47:33' WHERE despacho_real_id=4429 AND id_pedido=258361;
UPDATE tiendaamericana.despacho_real_det SET hora_entrega='2026-11-06 19:06:17' WHERE despacho_real_id=4429 AND id_pedido=258368;
UPDATE tiendaamericana.despacho_real_det SET hora_entrega='2026-11-06 19:45:46' WHERE despacho_real_id=4433 AND id_pedido=258385;
UPDATE tiendaamericana.despacho_real_det SET hora_entrega='2026-11-06 19:33:02' WHERE despacho_real_id=4433 AND id_pedido=258386;

-- 2) DATAMART (172.19.0.25) ------------------------------------------------
UPDATE datamart.despacho_real_det SET hora_entrega='2026-11-06 18:09:56' WHERE idtienda=10 AND despacho_real_id=4427 AND id_pedido=258358;
UPDATE datamart.despacho_real_det SET hora_entrega='2026-11-06 18:47:33' WHERE idtienda=10 AND despacho_real_id=4429 AND id_pedido=258361;
UPDATE datamart.despacho_real_det SET hora_entrega='2026-11-06 19:06:17' WHERE idtienda=10 AND despacho_real_id=4429 AND id_pedido=258368;
UPDATE datamart.despacho_real_det SET hora_entrega='2026-11-06 19:45:46' WHERE idtienda=10 AND despacho_real_id=4433 AND id_pedido=258385;
UPDATE datamart.despacho_real_det SET hora_entrega='2026-11-06 19:33:02' WHERE idtienda=10 AND despacho_real_id=4433 AND id_pedido=258386;
