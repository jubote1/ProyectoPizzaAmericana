-- ---------------------------------------------------------------------------
-- DEVOLVER LAS HORAS DE ENTREGA DE SAN ANTONIO DEL 2026-11-06
--
-- Estado EXACTO antes de la correccion del 2026-11-07.
-- Correr en la tienda (192.168.10.49, esquema tiendaamericana) Y en el
-- central (172.19.0.25, esquema datamart), cambiando el esquema.
-- ---------------------------------------------------------------------------

UPDATE tiendaamericana.despacho_real_det SET hora_entrega='2026-11-06 18:26:38' WHERE despacho_real_id=4427 AND id_pedido=258358;
UPDATE tiendaamericana.despacho_real_det SET hora_entrega='2026-11-06 19:23:44' WHERE despacho_real_id=4429 AND id_pedido=258361;
UPDATE tiendaamericana.despacho_real_det SET hora_entrega='2026-11-06 19:23:43' WHERE despacho_real_id=4429 AND id_pedido=258368;
UPDATE tiendaamericana.despacho_real_det SET hora_entrega='2026-11-06 20:04:36' WHERE despacho_real_id=4433 AND id_pedido=258385;
UPDATE tiendaamericana.despacho_real_det SET hora_entrega='2026-11-06 20:04:35' WHERE despacho_real_id=4433 AND id_pedido=258386;

UPDATE datamart.despacho_real_det SET hora_entrega='2026-11-06 18:26:38' WHERE idtienda=10 AND despacho_real_id=4427 AND id_pedido=258358;
UPDATE datamart.despacho_real_det SET hora_entrega='2026-11-06 19:23:44' WHERE idtienda=10 AND despacho_real_id=4429 AND id_pedido=258361;
UPDATE datamart.despacho_real_det SET hora_entrega='2026-11-06 19:23:43' WHERE idtienda=10 AND despacho_real_id=4429 AND id_pedido=258368;
UPDATE datamart.despacho_real_det SET hora_entrega='2026-11-06 20:04:36' WHERE idtienda=10 AND despacho_real_id=4433 AND id_pedido=258385;
UPDATE datamart.despacho_real_det SET hora_entrega='2026-11-06 20:04:35' WHERE idtienda=10 AND despacho_real_id=4433 AND id_pedido=258386;
