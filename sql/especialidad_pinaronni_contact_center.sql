-- ---------------------------------------------------------------------------
-- La especialidad Piñaronni entra al contact center
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
--
-- La especialidad ya existe en las tiendas; lo que falta es que el contact
-- center la pueda vender. Eso son dos cosas: la fila en especialidad y la
-- homologacion que traduce el producto del central al de la tienda.
--
-- EL NOMBRE VA COMO LO TIENEN LAS TIENDAS
--
-- En el catalogo de tienda se llama "Piñaronni", con doble N. Se respeta esa
-- escritura para que lo que ve quien toma el pedido y lo que sale en la comanda
-- de la cocina digan lo mismo. Si la buena es "Piñaroni", hay que corregirla en
-- los dos lados a la vez, no solo aqui.
--
-- DE DONDE SALEN LOS IDS
--
-- De America, que es la tienda de referencia:
--
--     617  Piñaronni XL
--     618  Piñaronni GD
--     619  Piñaronni MD
--     620  Piñaronni PZ
--
-- Y SIRVEN PARA LAS DOCE TIENDAS
--
-- El catalogo de producto esta sincronizado entre tiendas: se verifico contra
-- la homologacion de Americana Premium, donde las doce usan exactamente el
-- mismo mapeo 6->597, 7->598, 8->599, 9->600. Asi que los ids de America valen
-- para todas.
--
-- POR ESO LA HOMOLOGACION SE COPIA DE AMERICANA PREMIUM
--
-- En vez de escribir doce veces cuatro filas a mano, se leen las que ya tiene
-- la especialidad 39 y se cambian el idespecialidad y el idproductoext. Eso
-- garantiza que quede en las MISMAS tiendas y con el MISMO mapeo de tamanos:
-- si manana entra una tienda nueva, este script sigue siendo el correcto y no
-- hay que acordarse de agregarla.
--
-- Nota: la especialidad 39 cubre la tienda 6 (Poblado) aunque no tenga base de
-- datos propia. Se deja igual, por consistencia con las demas especialidades.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. LA ESPECIALIDAD
--
-- PNR es la abreviatura: PIN ya la usa Piña y Queso.
-- ===========================================================================

INSERT INTO pizzaamericana.especialidad (nombre, abreviatura, estado)
SELECT 'Piñaronni', 'PNR', 'A' FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.especialidad WHERE nombre = 'Piñaronni');

-- ===========================================================================
-- 2. LA HOMOLOGACION, CALCADA DE AMERICANA PREMIUM
--
-- homologacion_producto no tiene llave unica, asi que la guarda va con
-- NOT EXISTS: correr esto dos veces no duplica filas.
-- ===========================================================================

INSERT INTO pizzaamericana.homologacion_producto
       (idproductoint, idtienda, idespecialidadint, idsabortipoliquidoint, idexcepcion, idproductoext)
SELECT modelo.idproductoint,
       modelo.idtienda,
       nueva.idespecialidad,
       0,
       0,
       CASE modelo.idproductoint
            WHEN 6 THEN 617   -- Extragrande
            WHEN 7 THEN 618   -- Grande
            WHEN 8 THEN 619   -- Mediana
            WHEN 9 THEN 620   -- Pizzeta
       END
  FROM pizzaamericana.homologacion_producto modelo
 CROSS JOIN (SELECT idespecialidad FROM pizzaamericana.especialidad
              WHERE nombre = 'Piñaronni') nueva
 WHERE modelo.idespecialidadint = 39
   AND modelo.idproductoint IN (6, 7, 8, 9)
   AND NOT EXISTS (SELECT 1 FROM pizzaamericana.homologacion_producto ya
                    WHERE ya.idespecialidadint = nueva.idespecialidad
                      AND ya.idtienda = modelo.idtienda
                      AND ya.idproductoint = modelo.idproductoint);

-- ===========================================================================
-- 3. COMO QUEDO
-- ===========================================================================

SELECT idespecialidad, nombre, abreviatura, estado
  FROM pizzaamericana.especialidad WHERE nombre = 'Piñaronni';

SELECT h.idtienda,
       GROUP_CONCAT(CONCAT(h.idproductoint,'->',h.idproductoext) ORDER BY h.idproductoint) AS mapeo
  FROM pizzaamericana.homologacion_producto h
  JOIN pizzaamericana.especialidad e ON e.idespecialidad = h.idespecialidadint
 WHERE e.nombre = 'Piñaronni'
 GROUP BY h.idtienda ORDER BY h.idtienda;
