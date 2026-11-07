-- ---------------------------------------------------------------------------
-- RETIRAR LA PROMO "2 LASAGNAS + BEBIDA"
--
-- Decision de Juan David el 2026-11-07: el producto no se vende mas.
-- Viene de la medicion del 2026-10-24, que dio cinco meses planos (~454
-- unidades) pese a la campana COMBO LASAGNAS de 2.522 correos.
--
-- IDEMPOTENTE. No borra productos ni historia: el producto sigue en el
-- catalogo con su precio y la venta pasada queda intacta en el datamart.
-- Lo unico que cambia es que deja de ofrecerse.
--
-- DONDE VIVE EL PRODUCTO (medido el 2026-11-07 contra produccion)
--
--   Central  pizzaamericana.producto 406  "2 Lasagnas + Beb(40.990)"  $40.990
--   Espejo   pos_central.producto     613 "PROMO 2 Lasagnas + Beb"    $40.990
--   Tiendas  tiendaamericana.producto 613 (el mismo id en las 11)
--
-- En el POS el producto NO tiene columna "habilitado": la forma de retirarlo
-- es sacarlo de configuracion_menu, que es el boton de la caja. En el central
-- si existe la columna y por eso ahi se deshabilita.
--
-- La casilla era la MISMA en las 11 tiendas y en el espejo: multimenu 1
-- (Pizzas), casilla 23, fila 3 columna 4.
--
-- QUE NO TOCA ESTE SCRIPT, A PROPOSITO
--
--   - pizzaamericana.producto 407 "2 Lasag+ Beb DIDI(57.000)": YA estaba en
--     habilitado='N' desde antes.
--   - El 614 "PROMO 2 Lasag + Beb DIDI" del multimenu 6 (Plataformas), que es
--     con lo que la caja teclea un pedido de DiDi cuando no hay internet.
--     Sacarlo de ahi antes de bajar la promo en DiDi dejaria a la caja sin
--     poder teclear un pedido que todavia puede entrar. Se retira cuando la
--     promo este abajo en la plataforma.
--   - tiendaamericana.producto_horario del 613 (sabado, domingo y lunes de
--     12:00 a 17:10, domingo hasta 22:10): se deja para que devolver el boton
--     reviva la promo con su horario.
--
-- Reversa: sql/respaldos/devolver_promo_2_lasagnas_2026-11-07.sql
-- ---------------------------------------------------------------------------

-- 1) CENTRAL (172.19.0.25) -------------------------------------------------
UPDATE pizzaamericana.producto SET habilitado = 'N' WHERE idproducto = 406;

-- 2) CATALOGO ESPEJO pos_central (172.19.0.25) -----------------------------
DELETE FROM pos_central.configuracion_menu WHERE idproducto = 613;

-- 3) COMPROBACION DEL CENTRAL ----------------------------------------------
SELECT idproducto, nombre, preciogeneral, habilitado
  FROM pizzaamericana.producto WHERE idproducto IN (406, 407);
SELECT COUNT(*) AS botones_613_en_espejo
  FROM pos_central.configuracion_menu WHERE idproducto = 613;
