-- ---------------------------------------------------------------------------
-- DEVOLVER LA PROMO "2 LASAGNAS + BEBIDA" SI HAY QUE REVIVIRLA
--
-- Estado EXACTO antes del retiro del 2026-11-07, leido de produccion.
-- Correr solo si se decide volver a vender la promo.
-- ---------------------------------------------------------------------------

-- 1) CENTRAL (172.19.0.25, esquema pizzaamericana)
UPDATE pizzaamericana.producto SET habilitado = 'S' WHERE idproducto = 406;
-- El 407 (version DiDi, $57.000) YA estaba en habilitado='N' antes de este
-- cambio: no se toco y no hay que devolverlo.

-- 2) CATALOGO ESPEJO (172.19.0.25, esquema pos_central)
INSERT INTO pos_central.configuracion_menu (multimenu, menu, fila, columna, idproducto)
SELECT 1, 23, 3, 4, 613 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM pos_central.configuracion_menu
                  WHERE multimenu = 1 AND menu = 23 AND idproducto = 613);

-- 3) LAS 11 TIENDAS (en el MySQL local de cada una, esquema tiendaamericana)
--    Manrique 172.19.1.80, Bello 192.168.2.20, America 192.168.3.38,
--    Calasanz 192.168.4.10, Itagui 192.168.5.3, La Mota 192.168.7.19,
--    Envigado 192.168.8.29, Pilarica 192.168.9.39, San Antonio 192.168.10.49,
--    Manrique Piloto 172.19.11.69, Niquia 172.19.12.19
--
-- INSERT INTO tiendaamericana.configuracion_menu (multimenu, menu, fila, columna, idproducto)
-- SELECT 1, 23, 3, 4, 613 FROM dual
-- WHERE NOT EXISTS (SELECT 1 FROM tiendaamericana.configuracion_menu
--                   WHERE multimenu = 1 AND menu = 23 AND idproducto = 613);

-- NO se borro el producto 613 de tiendaamericana.producto ni el 406 de
-- pizzaamericana.producto: siguen ahi con su precio, solo salieron del menu.
-- Tampoco se toco tiendaamericana.producto_horario del 613 (sabado, domingo y
-- lunes de 12:00 a 17:10 / 22:10 el domingo), asi que al devolver el boton la
-- promo vuelve con su mismo horario.
