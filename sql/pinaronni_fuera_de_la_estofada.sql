-- ---------------------------------------------------------------------------
-- LA PINARONNI ESTOFADA NO EXISTE: SACARLA DEL CONTACT CENTER
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
--
-- Contraparte de 2026_10_02_03 en las tiendas. El script
-- pinaronni_donde_este_la_peperoni copio la Pinaronni a donde estuviera la
-- Peperoni y Queso, y la Peperoni si se vende estofada.
--
-- Son dos tablas: la lista de especialidades que ofrece cada estofada, y el
-- valor adicional de esa especialidad en ese producto. Se quitan las dos: si
-- queda el precio sin la lista, la fila no hace dano pero miente, y manana
-- alguien la lee como si la Pinaronni estofada existiera.
--
-- 311 es Pizza MD Estofada, 312 Pizza GD Estofada. Las "Plat" (322 a 325) NO
-- se tocan: esas son las pizzas de plataformas, no estofadas.
-- ---------------------------------------------------------------------------

DELETE FROM pizzaamericana.controla_especialidades
 WHERE Idespecialidad = 40 AND idexcepcion = 0 AND idproducto IN (311,312);

DELETE FROM pizzaamericana.especialidad_excepcion
 WHERE idespecialidad = 40 AND idproducto IN (311,312);

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
       SUM(c.Idespecialidad = 24) AS peperoni, SUM(c.Idespecialidad = 40) AS pinaronni
  FROM pizzaamericana.controla_especialidades c
  LEFT JOIN pizzaamericana.excepcion_precio x ON x.idexcepcion = c.idexcepcion
 GROUP BY c.idexcepcion, donde
HAVING peperoni > 0 OR pinaronni > 0
 ORDER BY c.idexcepcion;
