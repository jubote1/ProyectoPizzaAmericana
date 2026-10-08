-- ---------------------------------------------------------------------------
-- LA PINARONNI EN EL CONTACT CENTER, DONDE ESTE LA PEPERONI
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
--
-- En el contact center la Pinaronni (idespecialidad 40) ya existe y ya esta
-- activa, pero le faltan dos cosas que la Peperoni y Queso (24) si tiene.
--
-- 1. EL VALOR EXTRA EN LOS PRODUCTOS QUE NO SON LA PIZZA DE CARTA
--
-- especialidad_excepcion cobra el adicional por (especialidad, producto). La
-- Pinaronni solo lo tiene para las cuatro pizzas de carta -XL, GD, MD, PZ-.
-- Le falta para las seis que la Peperoni si tiene:
--
--   311 Pizza MD Estofada   312 Pizza GD Estofada
--   322 Pizza XL Plat       323 Pizza GD Plat
--   324 Pizza MD Plat       325 Pizza PZ Plat
--
-- Las "Plat" son las de plataformas. Hoy una Pinaronni vendida por ahi NO
-- cobra el adicional: se va en cero. Eso es plata, no es cosmetica.
--
-- 2. LAS LISTAS CERRADAS
--
-- Cuando una promocion tiene controla_especialidades = 'S', las especialidades
-- no salen de la tabla especialidad sino de controla_especialidades, y ahi la
-- Pinaronni no esta en ninguna de las doce listas donde si esta la Peperoni.
-- La que mas pesa hoy es la Pizzeta Promo DIDI, que esta prendida, y las dos
-- estofadas, que se controlan por producto y no por promocion.
--
-- LA REGLA, LA MISMA QUE EN TIENDA
--
-- Donde este la Peperoni y Queso va la Pinaronni, con el mismo valor. No es
-- una lista escrita a mano: se copia, y asi cubre tambien las promociones
-- apagadas, para que el dia que se prendan ya esten bien.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. EL VALOR EXTRA
-- ===========================================================================

INSERT INTO pizzaamericana.especialidad_excepcion (idespecialidad, idproducto, precio)
SELECT 40, x.idproducto, x.precio
  FROM pizzaamericana.especialidad_excepcion x
 WHERE x.idespecialidad = 24
   -- La estofada NO: la Pinaronni estofada no se puede hacer. 311 es Pizza MD
   -- Estofada y 312 Pizza GD Estofada.
   AND x.idproducto NOT IN (311,312)
   AND NOT EXISTS (SELECT 1 FROM pizzaamericana.especialidad_excepcion ya
                    WHERE ya.idespecialidad = 40 AND ya.idproducto = x.idproducto);

-- ===========================================================================
-- 2. LAS LISTAS CERRADAS
-- ===========================================================================

INSERT INTO pizzaamericana.controla_especialidades (idexcepcion, idproducto, Idespecialidad)
SELECT c.idexcepcion, c.idproducto, 40
  FROM pizzaamericana.controla_especialidades c
 WHERE c.Idespecialidad = 24
   -- Lo mismo: fuera las dos estofadas, que se controlan por producto.
   AND NOT (c.idexcepcion = 0 AND c.idproducto IN (311,312))
   AND NOT EXISTS (SELECT 1 FROM pizzaamericana.controla_especialidades ya
                    WHERE ya.idexcepcion = c.idexcepcion AND ya.idproducto = c.idproducto
                      AND ya.Idespecialidad = 40);

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================

SELECT p.idproducto, IFNULL(p.nombre,'(todos)') AS producto,
       MAX(CASE WHEN x.idespecialidad=24 THEN x.precio END) AS peperoni,
       MAX(CASE WHEN x.idespecialidad=40 THEN x.precio END) AS pinaronni
  FROM pizzaamericana.especialidad_excepcion x
  LEFT JOIN pizzaamericana.producto p ON p.idproducto = x.idproducto
 WHERE x.idespecialidad IN (24,40) GROUP BY p.idproducto, p.nombre ORDER BY p.idproducto;

SELECT c.idexcepcion, IFNULL(x.descripcion, CONCAT('producto ', c.idproducto)) AS donde,
       IFNULL(x.habilitado,'S') AS activa,
       SUM(c.Idespecialidad = 24) AS peperoni, SUM(c.Idespecialidad = 40) AS pinaronni
  FROM pizzaamericana.controla_especialidades c
  LEFT JOIN pizzaamericana.excepcion_precio x ON x.idexcepcion = c.idexcepcion
 GROUP BY c.idexcepcion, donde, activa
HAVING peperoni > 0 OR pinaronni > 0
 ORDER BY c.idexcepcion;
