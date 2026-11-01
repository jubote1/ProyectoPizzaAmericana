-- ---------------------------------------------------------------------------
-- PARTIDOS DE OCTUBRE 2026 (NACIONAL Y MEDELLIN) -- TODOS SIN CONFIRMAR
--
-- Idempotente. Corre tal cual en HeidiSQL. Necesita 2026_11_01_01 corrido antes.
--
-- DE DONDE SALE Y POR QUE QUEDA SIN CONFIRMAR
--
-- Estas fechas se buscaron en la web (GolCaracol, 365Scores, Colombia.com, Blu Radio,
-- ESPN, FotMob, LiveFutbol, VAVEL). **Las fuentes se contradicen entre si en casi la
-- mitad de los partidos, casi siempre por un dia de diferencia** -zonas horarias- y en
-- algunos tambien en el marcador.
--
-- Un dia de corrimiento no es un detalle: marca como hincha del equipo equivocado a
-- quien compro ese dia. Por eso TODAS entran con confirmado = 'N' y el proceso de
-- marcado NO las usa. Hay que revisarlas una por una contra DIMAYOR o la pagina del
-- club y pasarlas a 'S'.
--
-- COMO CONFIRMARLAS (una linea por fecha que este bien):
--
--   UPDATE pizzaamericana.partido_futbol SET confirmado = 'S'
--    WHERE fecha = '2026-10-20' AND equipo = 'NACIONAL';
--
-- Y si una fecha esta corrida, se corrige antes:
--
--   UPDATE pizzaamericana.partido_futbol SET fecha = '2026-10-18', confirmado = 'S'
--    WHERE fecha = '2026-10-17' AND equipo = 'MEDELLIN';
--
-- OJO CON LO QUE SIGNIFICA UNA FECHA CON LOS DOS EQUIPOS
--
-- No es solo el clasico. Si Nacional y Medellin juegan el mismo dia aunque sea contra
-- rivales distintos, ese dia tampoco sirve para deducir nada, y el proceso lo descarta
-- solo. Pasa el 14, el 17 y el 25 de octubre.
--
-- PARA DEVOLVERLO
--
--   DELETE FROM pizzaamericana.partido_futbol WHERE fecha BETWEEN '2026-10-01' AND '2026-10-31';
-- ---------------------------------------------------------------------------

USE pizzaamericana;

INSERT INTO partido_futbol (fecha, equipo, rival, torneo, confirmado, fuente, usuario)
SELECT * FROM (
  SELECT '2026-10-05' f, 'MEDELLIN' e, 'Santa Fe' r, 'Liga' t, 'N' c, 'ESPN dice 5-oct, FotMob dice 6-oct: VERIFICAR' s, 'claude' u UNION ALL
  SELECT '2026-10-08', 'NACIONAL', 'Junior', 'Liga', 'N', 'Colombia.com; 365Scores da otra fecha: VERIFICAR', 'claude' UNION ALL
  SELECT '2026-10-10', 'NACIONAL', 'Deportivo Pasto', 'Liga', 'N', '365Scores dice 10-oct, Colombia.com dice 22-oct: VERIFICAR', 'claude' UNION ALL
  SELECT '2026-10-11', 'MEDELLIN', 'America de Cali', 'Liga', 'N', 'ESPN/Goal; una sola fuente: VERIFICAR', 'claude' UNION ALL
  SELECT '2026-10-14', 'NACIONAL', 'Independiente Medellin', 'Copa Colombia', 'N', 'VAVEL 3-2; clasico, no sirve para marcar', 'claude' UNION ALL
  SELECT '2026-10-14', 'MEDELLIN', 'Atletico Nacional', 'Copa Colombia', 'N', 'VAVEL 3-2; clasico, no sirve para marcar', 'claude' UNION ALL
  SELECT '2026-10-17', 'NACIONAL', 'Boyaca Chico', 'Liga', 'N', 'Colombia.com y 365Scores coinciden', 'claude' UNION ALL
  SELECT '2026-10-17', 'MEDELLIN', 'Once Caldas', 'Liga', 'N', 'ESPN dice 17-oct, 365Scores dice 18-oct: VERIFICAR', 'claude' UNION ALL
  SELECT '2026-10-20', 'NACIONAL', 'Cucuta Deportivo', 'Liga', 'N', 'Colombia.com y 365Scores coinciden', 'claude' UNION ALL
  SELECT '2026-10-25', 'NACIONAL', 'Independiente Medellin', 'Liga', 'N', 'Clasico, no sirve para marcar', 'claude' UNION ALL
  SELECT '2026-10-25', 'MEDELLIN', 'Atletico Nacional', 'Liga', 'N', 'Clasico, no sirve para marcar', 'claude' UNION ALL
  SELECT '2026-10-28', 'MEDELLIN', 'Internacional de Bogota', 'Liga', 'N', 'VAVEL dice 28-oct, 365Scores y FotMob dicen 29-oct: VERIFICAR', 'claude' UNION ALL
  SELECT '2026-10-29', 'NACIONAL', 'Santa Fe', 'Liga', 'N', 'Blu Radio; partido aplazado de la fecha 5', 'claude'
) nuevos
WHERE NOT EXISTS (SELECT 1 FROM partido_futbol p
                   WHERE p.fecha = nuevos.f AND p.equipo = nuevos.e);

-- Comprobacion: que dias quedarian utiles si se confirmaran tal cual.
SELECT fecha,
       GROUP_CONCAT(equipo ORDER BY equipo) AS equipos,
       CASE WHEN COUNT(DISTINCT equipo) > 1 THEN 'NO SIRVE (juegan los dos)'
            ELSE CONCAT('marcaria hinchas de ', MIN(equipo)) END AS efecto,
       MIN(confirmado) AS confirmado
  FROM partido_futbol
 WHERE fecha BETWEEN '2026-10-01' AND '2026-10-31'
 GROUP BY fecha ORDER BY fecha;
