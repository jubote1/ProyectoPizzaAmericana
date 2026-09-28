-- ---------------------------------------------------------------------------
-- El recargo de Piñaronni, igual al de las pepperoni
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
-- Requiere especialidad_pinaronni_contact_center.sql.
--
-- QUE ES especialidad_excepcion
--
-- El sobrecosto que se le suma a la pizza por llevar esa especialidad, y va
-- POR TAMANO. No todas lo tienen: Tocineta y Queso, Piña y Queso y Hawaiana
-- Artesanal se venden al precio base.
--
-- LA ESCALA ES LA DE LA FAMILIA PEPPERONI, Y ES LA MISMA EN LAS TRES
--
--     Peperoni y Queso, Pepperoni Chic y Pepperoni Champiñon:
--
--       6  Extragrande   4.000
--       7  Grande        3.500
--       8  Mediana       2.500
--       9  Pizzeta       2.000
--
-- Distinta de Americana Premium, que cobra 2.500 parejo en los cuatro
-- tamanos. Aqui el recargo baja con el tamano.
--
-- SOLO LOS CUATRO TAMANOS, NO LAS PLAT
--
-- Las tres pepperoni tambien cobran recargo en las versiones Plat -productos
-- 322 a 325- porque las tienen homologadas. Piñaronni NO: en el catalogo de
-- tienda existe unicamente en cuatro tamanos (617 a 620), igual que Americana
-- Premium.
--
-- Poner recargo a un producto que no esta homologado seria lo peor de los dos
-- mundos: el sistema cobraria el sobrecosto y despues no encontraria a que
-- producto de tienda mandar el pedido. Si algun dia se quiere Piñaronni Plat,
-- primero hay que crearla en las tiendas, despues homologarla, y de ultimo
-- agregarle el recargo.
-- ---------------------------------------------------------------------------

INSERT INTO pizzaamericana.especialidad_excepcion (idespecialidad, idproducto, precio)
SELECT nueva.idespecialidad, t.idproducto, t.precio
  FROM (SELECT 6 AS idproducto, 4000 AS precio          -- Extragrande
        UNION ALL SELECT 7, 3500                        -- Grande
        UNION ALL SELECT 8, 2500                        -- Mediana
        UNION ALL SELECT 9, 2000) t                     -- Pizzeta
 CROSS JOIN (SELECT idespecialidad FROM pizzaamericana.especialidad
              WHERE nombre = 'Piñaronni') nueva
 WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.especialidad_excepcion ya
                    WHERE ya.idespecialidad = nueva.idespecialidad
                      AND ya.idproducto = t.idproducto);

-- ===========================================================================
-- COMO QUEDO, AL LADO DE LAS PEPPERONI PARA PODER COMPARAR
-- ===========================================================================

SELECT e.idespecialidad, e.nombre, ee.idproducto, p.descripcion AS tamano, ee.precio
  FROM pizzaamericana.especialidad_excepcion ee
  JOIN pizzaamericana.especialidad e ON e.idespecialidad = ee.idespecialidad
  LEFT JOIN pizzaamericana.producto p ON p.idproducto = ee.idproducto
 WHERE e.idespecialidad IN (29, 40) AND ee.idproducto IN (6,7,8,9)
 ORDER BY ee.idproducto, e.idespecialidad;
