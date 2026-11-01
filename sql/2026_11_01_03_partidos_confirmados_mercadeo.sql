-- ---------------------------------------------------------------------------
-- CALENDARIO DE PARTIDOS CONFIRMADO POR MERCADEO (mayo a octubre 2026)
--
-- Idempotente. Corre tal cual en HeidiSQL. Necesita 2026_11_01_01 corrido antes.
--
-- DE DONDE SALE
--
-- Del archivo "Dias_Combo_Futbolero_marcado.csv" que lleno Juan David el 2026-11-01:
-- los 70 dias con venta de Combo Futbolero desde mayo, con el equipo que jugo cada uno.
-- Entra con confirmado = 'S' porque lo reviso una persona, a diferencia de lo que yo
-- habia sacado de internet.
--
-- PRIMERO BORRA LO MIO. Las fechas que yo cargue de la web (confirmado = 'N', usuario
-- 'claude') se eliminan: en al menos una -el 17 de octubre- yo tenia a los dos equipos
-- y mercadeo dice que solo jugo Nacional. Entre las dos versiones manda la revisada, y
-- dejarlas conviviendo haria que esa fecha no marcara a nadie.
--
-- UN CLASICO SON DOS FILAS, una por equipo. El proceso descarta solo las fechas que
-- tienen los dos. Los dias marcados NINGUNO no se cargan.
--
-- PARA DEVOLVERLO
--
--   DELETE FROM pizzaamericana.partido_futbol WHERE usuario = 'mercadeo';
-- ---------------------------------------------------------------------------

USE pizzaamericana;

DELETE FROM partido_futbol WHERE usuario = 'claude' AND confirmado = 'N';

INSERT INTO partido_futbol (fecha, equipo, rival, torneo, confirmado, fuente, usuario)
SELECT * FROM (
  SELECT '2026-05-01' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-05-03' f, 'MEDELLIN' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-05-07' f, 'MEDELLIN' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-05-09' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-05-12' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-05-16' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-05-20' f, 'MEDELLIN' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-05-23' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-05-26' f, 'MEDELLIN' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-06-02', 'NACIONAL', 'Independiente Medellin', '', 'S', 'clasico, revisado por mercadeo', 'mercadeo' UNION ALL
  SELECT '2026-06-02', 'MEDELLIN', 'Atletico Nacional', '', 'S', 'clasico, revisado por mercadeo', 'mercadeo' UNION ALL
  SELECT '2026-06-07' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-06-08' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-07-25' f, 'MEDELLIN' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-08-01' f, 'MEDELLIN' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-08-09' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-08-30' f, 'MEDELLIN' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-09-09' f, 'MEDELLIN' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-09-13' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-09-17' f, 'MEDELLIN' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-09-20' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-09-24' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-09-26' f, 'MEDELLIN' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-09-30' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-10-05' f, 'MEDELLIN' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-10-08' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-10-11' f, 'MEDELLIN' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-10-14', 'NACIONAL', 'Independiente Medellin', '', 'S', 'clasico, revisado por mercadeo', 'mercadeo' UNION ALL
  SELECT '2026-10-14', 'MEDELLIN', 'Atletico Nacional', '', 'S', 'clasico, revisado por mercadeo', 'mercadeo' UNION ALL
  SELECT '2026-10-17' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u UNION ALL
  SELECT '2026-10-25', 'NACIONAL', 'Independiente Medellin', '', 'S', 'clasico, revisado por mercadeo', 'mercadeo' UNION ALL
  SELECT '2026-10-25', 'MEDELLIN', 'Atletico Nacional', '', 'S', 'clasico, revisado por mercadeo', 'mercadeo' UNION ALL
  SELECT '2026-10-29' f, 'NACIONAL' e, '' r, '' t, 'S' c, 'revisado por mercadeo' s, 'mercadeo' u
) nuevos
WHERE NOT EXISTS (SELECT 1 FROM partido_futbol p WHERE p.fecha = nuevos.f AND p.equipo = nuevos.e);

-- Comprobacion: cuantos dias quedan de cada clase.
SELECT CASE WHEN equipos > 1 THEN 'NO SIRVE (juegan los dos)' ELSE CONCAT('marca hinchas de ', equipo) END AS efecto,
       COUNT(*) AS dias
  FROM (SELECT fecha, COUNT(DISTINCT equipo) AS equipos, MIN(equipo) AS equipo
          FROM partido_futbol WHERE confirmado = 'S' GROUP BY fecha) z
 GROUP BY efecto ORDER BY dias DESC;
