-- ---------------------------------------------------------------------------
-- RETIRAR LA PROMO "2 LASAGNAS + BEBIDA" -- PARTE DE TIENDA
--
-- Se corre en el MySQL LOCAL DE CADA TIENDA, esquema tiendaamericana.
-- Acompana a 2026_11_07_01_retirar_promo_2_lasagnas.sql (ese va al central).
--
-- Las 11 tiendas y su host (pizzaamericana.tienda.hosbd):
--   1  Manrique         172.19.1.80
--   2  Bello            192.168.2.20
--   3  America          192.168.3.38
--   4  Calasanz         192.168.4.10
--   5  Itagui           192.168.5.3
--   7  La Mota          192.168.7.19
--   8  Envigado         192.168.8.29
--   9  Pilarica         192.168.9.39
--   10 San Antonio      192.168.10.49
--   11 Manrique Piloto  172.19.11.69
--   13 Niquia           172.19.12.19
--
-- Bodega (12) NO lleva este script: no tiene el producto, no vende.
--
-- IDEMPOTENTE: si ya se corrio, borra cero filas.
--
-- Borra por idproducto y no por casilla a proposito. configuracion_menu no
-- tiene clave unica por (multimenu, menu), asi que un producto puede haber
-- quedado en dos casillas de revisiones viejas -ya paso en Itagui y en
-- Pilarica el 2026-11-03-. Por idproducto se van todas.
--
-- El 614 (version DiDi, multimenu 6 Plataformas) se deja: es con lo que la
-- caja teclea un pedido de DiDi cuando no hay internet.
-- ---------------------------------------------------------------------------

DELETE FROM tiendaamericana.configuracion_menu WHERE idproducto = 613;

-- Debe quedar en 0 el 613, y en 1 el 614 (el de Plataformas, que sigue).
SELECT idproducto, COUNT(*) AS botones
  FROM tiendaamericana.configuracion_menu
 WHERE idproducto IN (613, 614)
 GROUP BY idproducto;
