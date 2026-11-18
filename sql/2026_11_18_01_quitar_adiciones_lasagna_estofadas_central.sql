-- ============================================================================
-- 2026_11_18_01_quitar_adiciones_lasagna_estofadas_central.sql
--
-- Certificacion #1 del Contact Center: la Lasagna Mixta y las pizzas Estofadas
-- NO llevan adiciones. Se quita la pregunta de adiciones del producto (no el
-- producto, y no las demas preguntas).
--
--   producto 2   Lasagna Mixta        pregunta 9  "Desea Adiciones PZ?"  (queda la 11, gaseosa pequena)
--   producto 469 Pizza MD Estofada    pregunta 8  "Desea Adiciones MD?"  (quedan la 43 y la 10)
--   producto 470 Pizza GD Estofada    pregunta 3  "Desea Adiciones GD?"  (quedan la 44 y la 10)
--
-- Se corre en pos_central (lo que usa TomaPedidosPOS.html). El de las tiendas es
-- 2026_11_18_01_quitar_adiciones_lasagna_estofadas_tienda.sql del repo del POS.
--
-- Idempotente: cada UPDATE solo actua si la columna todavia trae esa pregunta.
-- El motor y el POS arman las preguntas de un producto UNIENDO por valor
-- (pregunta.idpregunta = producto.idpreguntaforzadaN), asi que un 0 en medio no
-- estorba y no hace falta correr las demas posiciones.
--
-- Para DESHACER: ver el respaldo de las tres filas
-- (sql/respaldos/producto_preguntas_<base>_antes_2026-11-18.sql) o volver a poner
--   update pos_central.producto set idpreguntaforzada1 = 9 where idproducto = 2;
--   update pos_central.producto set idpreguntaforzada2 = 8 where idproducto = 469;
--   update pos_central.producto set idpreguntaforzada2 = 3 where idproducto = 470;
-- ============================================================================

UPDATE pos_central.producto SET idpreguntaforzada1 = 0 WHERE idproducto = 2   AND idpreguntaforzada1 = 9;
UPDATE pos_central.producto SET idpreguntaforzada2 = 0 WHERE idproducto = 469 AND idpreguntaforzada2 = 8;
UPDATE pos_central.producto SET idpreguntaforzada2 = 0 WHERE idproducto = 470 AND idpreguntaforzada2 = 3;

-- Comprobacion: ninguna de las tres debe traer ya una pregunta de adiciones (3, 8, 9)
SELECT idproducto, descripcion,
       idpreguntaforzada1 p1, idpreguntaforzada2 p2, idpreguntaforzada3 p3, idpreguntaforzada4 p4, idpreguntaforzada5 p5,
       idpreguntaforzada6 p6, idpreguntaforzada7 p7, idpreguntaforzada8 p8, idpreguntaforzada9 p9, idpreguntaforzada10 p10,
       (idpreguntaforzada1 IN (3,8,9) OR idpreguntaforzada2 IN (3,8,9) OR idpreguntaforzada3 IN (3,8,9) OR idpreguntaforzada4 IN (3,8,9)
        OR idpreguntaforzada5 IN (3,8,9) OR idpreguntaforzada6 IN (3,8,9) OR idpreguntaforzada7 IN (3,8,9) OR idpreguntaforzada8 IN (3,8,9)
        OR idpreguntaforzada9 IN (3,8,9) OR idpreguntaforzada10 IN (3,8,9)) AS aun_con_adiciones
  FROM pos_central.producto
 WHERE idproducto IN (2, 469, 470);
