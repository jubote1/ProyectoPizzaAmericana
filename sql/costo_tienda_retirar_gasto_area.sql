-- ---------------------------------------------------------------------------
-- RETIRAR pizzaamericana.gasto_area: ERA UNA TABLA DUPLICADA
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
--
-- QUE PASO
--
-- inventarioamericana.gerencia_gasto_area existe desde el 2026-10-23, con su
-- catalogo gerencia_concepto_gasto -que hasta tiene una columna `linea` que
-- mapea cada concepto a su linea del Excel- y la leen GerenciaCierreDAO y
-- GerenciaGastoDAO: es la tabla del cierre gerencial.
--
-- Un dia despues se creo pizzaamericana.gasto_area para lo mismo, sin saber
-- que aquella ya estaba. Dos tablas para el mismo dato terminan diciendo
-- cosas distintas, asi que esta se retira.
--
-- ANTES DE CORRER ESTO
--
-- Los tres gastos de publicidad ya se pasaron a gerencia_gasto_area con la
-- migracion 2026_10_25_01 de ProyectoInventarioAmericana, y alla suman los
-- mismos 9.587.100. Nada se pierde.
--
-- Tambien se retiran, en el mismo commit, GastoAreaDAO, GastoAreaCtrl,
-- CRUDGastoArea, GastoArea.html, js/gastoArea.js y la entrada del menu: la
-- carga de los gastos de area ya la hace la pantalla de Gerencia.
--
-- LO QUE SE PIERDE, Y COMO SE REEMPLAZA
--
-- gasto_area_mes guardaba el valor de cada mes de un gasto variable. En
-- gerencia_gasto_area eso se expresa con la vigencia: una fila por periodo,
-- con vigencia_desde y vigencia_hasta. Es mas trabajo de digitar, pero tiene
-- la propiedad que importaba -un mes ya repartido entre las tiendas NO cambia
-- porque alguien digite el siguiente- porque cada mes tiene su propia fila.
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS pizzaamericana.gasto_area_mes;
DROP TABLE IF EXISTS pizzaamericana.gasto_area;

-- Como quedo: publicidad tiene que seguir dando 9.587.100 en octubre.
SELECT g.area, COUNT(*) AS conceptos, ROUND(SUM(g.valor_mensual)) AS total_mes
  FROM inventarioamericana.gerencia_gasto_area g
  JOIN inventarioamericana.gerencia_concepto_gasto c
    ON c.idconcepto = g.idconcepto AND c.activo = 'S'
 WHERE g.vigencia_desde <= '2026-10-31'
   AND (g.vigencia_hasta IS NULL OR g.vigencia_hasta >= '2026-10-01')
 GROUP BY g.area;
