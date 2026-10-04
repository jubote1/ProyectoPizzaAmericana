-- ---------------------------------------------------------------------------
-- EL VOLANTE PARA TODOS, EN EL REPORTE DIARIO DE PROMOCIONES
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
--
-- Los tres volantes salieron el 2 de octubre reusando los productos que ya
-- existian -296 XL, 297 GD, 463 MD, antes "Volante 2024"- con precios de
-- 64.990, 55.990 y 45.990, gaseosa 1.5 sin costo y acompanante incluido.
--
-- El reporte diario mide por idproducto contra el detalle_pedido de cada
-- tienda, asi que basta con meterlos al catalogo: no hay nada que programar.
--
-- VAN COMO UNA SOLA PROMOCION, NO COMO TRES
--
-- Igual que el Combo Insuperable, que agrupa sus 480, 481 y 482 en una sola
-- fila. Lo que se quiere saber de un volante recien salido es si se mueve,
-- no cual tamano se mueve mas; y si manana hace falta el detalle por tamano,
-- se parte en tres desde la pantalla de administracion del catalogo sin tocar
-- codigo ni perder lo ya medido.
--
-- plataforma = 'N' porque es venta de tienda y contact center, no de Rappi ni
-- DiDi. Eso es lo que decide en que tabla del correo aparece.
--
-- orden 15 lo deja de segundo, detras del Combo Insuperable, sin tener que
-- renumerar los trece que ya estaban.
-- ---------------------------------------------------------------------------

INSERT INTO pizzaamericana.promocion_reporte (nombre, plataforma, activo, orden)
SELECT 'Volante Para Todos', 'N', 'S', 15 FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.promocion_reporte
                    WHERE nombre = 'Volante Para Todos');

INSERT INTO pizzaamericana.promocion_reporte_item (idpromo, idproducto, activo)
SELECT p.idpromo, nuevos.idproducto, 'S'
  FROM (SELECT 296 AS idproducto UNION ALL SELECT 297 UNION ALL SELECT 463) nuevos
 CROSS JOIN (SELECT idpromo FROM pizzaamericana.promocion_reporte
              WHERE nombre = 'Volante Para Todos') p
 WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.promocion_reporte_item ya
                    WHERE ya.idpromo = p.idpromo AND ya.idproducto = nuevos.idproducto);

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================

SELECT p.idpromo, p.nombre, p.plataforma, p.activo, p.orden,
       GROUP_CONCAT(i.idproducto ORDER BY i.idproducto) AS productos
  FROM pizzaamericana.promocion_reporte p
  LEFT JOIN pizzaamericana.promocion_reporte_item i
         ON i.idpromo = p.idpromo AND i.activo = 'S'
 WHERE p.activo = 'S'
 GROUP BY p.idpromo, p.nombre, p.plataforma, p.activo, p.orden
 ORDER BY p.orden, p.idpromo;
