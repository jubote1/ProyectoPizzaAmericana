-- ---------------------------------------------------------------------------
-- La Pinaronni entra a Venta Integral
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
--
-- No hace falta tocar codigo: el catalogo de Venta Integral es parametrizable y
-- una categoria nueva es una fila con sus items.
--
-- VA COMO ESPECIALIDAD, NO COMO PRODUCTO
--
-- Igual que Americana Premium, Super, Paisa y Mexicana. Eso importa por las
-- pizzas mitad y mitad: con tipodato ESPECIALIDAD, el contact center usa
-- CONTEO_ESPECIALIDAD_CON_DEDUP, que cuenta media pizza cuando la Pinaronni es
-- uno de los dos sabores y una completa cuando es el unico. Si se metiera como
-- PRODUCTO, una mitad contaria como una entera.
--
-- LOS IDS
--
-- En la tienda la especialidad no existe como tal: existen cuatro productos,
-- uno por tamano. Son los mismos que se homologaron el 2026-09-28:
--
--     617  Pinaronni XL      619  Pinaronni MD
--     618  Pinaronni GD      620  Pinaronni PZ
--
-- En el central si existe como especialidad, y es la 40.
--
-- EL NOMBRE SE LEE DE especialidad, NO SE ESCRIBE AQUI
--
-- Lleva ene, y este archivo se mantiene en ASCII puro para que se pueda correr
-- desde cualquier cliente sin depender de como quede codificado. Tomandolo de
-- la tabla queda, ademas, garantizado que diga exactamente lo mismo que ve
-- quien toma el pedido.
--
-- SUMA_CANTIDAD Y EXCLUYE ANULADOS
--
-- Igual que las otras cuatro especialidades: se cuentan unidades -dos pizzas en
-- una misma linea son dos pizzas- y una linea anulada no es una venta.
--
-- EL ORDEN
--
-- Va en 15, entre Americana Premium (10) y Super (20), para que quede con las
-- demas especialidades y no al final junto a las sodas y los deditos.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. LA CATEGORIA
-- ===========================================================================

INSERT INTO pizzaamericana.venta_integral_categoria
       (nombre, abreviatura, tipodato, medicion_tienda, excluye_anulados_tienda,
        filtro_estacion_tienda, medicion_cc, activo, orden)
SELECT e.nombre, 'PINARONNI', 'ESPECIALIDAD', 'SUMA_CANTIDAD', 'S',
       '%servid%', 'CONTEO_ESPECIALIDAD_CON_DEDUP', 'S', 15
  FROM pizzaamericana.especialidad e
 WHERE e.idespecialidad = 40
   AND NOT EXISTS (SELECT 1 FROM pizzaamericana.venta_integral_categoria ya
                    WHERE ya.abreviatura = 'PINARONNI');

-- ===========================================================================
-- 2. LOS ITEMS
--
-- Cuatro de tienda -un producto por tamano- y uno de contact center -la
-- especialidad-.
-- ===========================================================================

INSERT INTO pizzaamericana.venta_integral_categoria_item (idcategoria, ambito, idvalor, activo)
SELECT c.idcategoria, nuevos.ambito, nuevos.idvalor, 'S'
  FROM (          SELECT 'TIENDA' AS ambito, 617 AS idvalor
        UNION ALL SELECT 'TIENDA', 618
        UNION ALL SELECT 'TIENDA', 619
        UNION ALL SELECT 'TIENDA', 620
        UNION ALL SELECT 'CONTACTCENTER', 40) nuevos
 CROSS JOIN (SELECT idcategoria FROM pizzaamericana.venta_integral_categoria
              WHERE abreviatura = 'PINARONNI') c
 WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.venta_integral_categoria_item ya
                    WHERE ya.idcategoria = c.idcategoria
                      AND ya.ambito = nuevos.ambito
                      AND ya.idvalor = nuevos.idvalor);

-- ===========================================================================
-- 3. EL PARAMETRO DEL REPROCESO, QUE NO EXISTIA
--
-- ServicioSemanalVentaIntegralReproceso lee FECHAREPROCESOVENTAINTEGRAL de
-- general.parametros, y esa fila nunca se creo: el proceso arrancaba, no
-- encontraba fecha utilizable y se salia diciendolo. Nadie lo habia notado
-- porque hasta ahora no habia hecho falta reprocesar.
--
-- OJO: es un parametro PROPIO, distinto de FECHAREPROCESO, que usan los otros
-- reportes. Esta separado a proposito: reprocesar Venta Integral de una semana
-- no tiene por que arrastrar al resto de reportes a esa misma fecha.
--
-- Queda en 2026-09-28 -lunes- para reprocesar la semana del 21 al 27 de
-- septiembre, que es la primera en que se vendio Pinaronni. Se acepta corte
-- domingo o lunes.
-- ===========================================================================

INSERT INTO general.parametros (valorparametro, valornumerico, valortexto, valornumericod)
SELECT 'FECHAREPROCESOVENTAINTEGRAL', 0,
       '2026-09-28', 0 FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM general.parametros
                    WHERE valorparametro = 'FECHAREPROCESOVENTAINTEGRAL');

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================

SELECT valorparametro, valortexto AS fecha_de_corte
  FROM general.parametros WHERE valorparametro = 'FECHAREPROCESOVENTAINTEGRAL';

SELECT idcategoria, nombre, abreviatura, tipodato, medicion_tienda,
       excluye_anulados_tienda, medicion_cc, activo, orden
  FROM pizzaamericana.venta_integral_categoria
 ORDER BY orden;

SELECT c.nombre AS categoria, i.ambito, COUNT(*) AS items,
       GROUP_CONCAT(i.idvalor ORDER BY i.idvalor) AS valores
  FROM pizzaamericana.venta_integral_categoria_item i
  JOIN pizzaamericana.venta_integral_categoria c ON c.idcategoria = i.idcategoria
 WHERE c.abreviatura = 'PINARONNI'
 GROUP BY c.nombre, i.ambito;
