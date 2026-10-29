-- ---------------------------------------------------------------------------
-- FIJARLE A MANO LA TIENDA A UN ADMINISTRADOR DE PUNTO DE VENTA (base general)
--
-- Idempotente: se puede correr dos veces. Corre tal cual en HeidiSQL.
--
-- PARA QUE SIRVE
--
-- Normalmente el sistema DEDUCE la tienda del administrador: la que mas horas le sumo
-- la semana pasada en general.horario_trabajado. Eso falla cuando la persona todavia no
-- tiene horas -acaba de entrar, estuvo incapacitada, la trasladaron esta misma semana-.
-- Esta columna es la excepcion: si trae una tienda, MANDA sobre lo que digan las horas.
--
--   general.empleado.idtienda_reportes
--      0            = deducir de las horas (lo normal, el valor por defecto)
--      1..14        = esa tienda y punto, no importa donde haya trabajado
--
-- El orden con que decide el sistema es:
--      1. supernumerario = 1          -> ve TODAS las tiendas
--      2. idtienda_reportes > 0       -> ve esa
--      3. horas de la semana pasada   -> la tienda con mas horas
--      4. horas del ultimo mes        -> la tienda con mas horas
--      5. nada de lo anterior         -> no ve ninguna, y la pantalla se lo explica
--
-- OJO: lo que se fija aqui NO se suelta solo. Si a la persona la trasladan, hay que
-- cambiarlo o volverlo 0 para que el sistema retome la deduccion automatica.
--
-- QUE HACE HOY
--
-- Les fija la tienda a los dos administradores que entraron el 2026-10-28 y no tienen
-- ni una hora registrada, por pedido de Juan David el 2026-10-29:
--
--   807  Hermes Arley Rojas Peralta    (usuario hrojas)  -> 11 Manrique Piloto
--   806  Marina Elena Henao Hernandez  (usuario mhenao)  ->  3 America
--
-- LAS TIENDAS, POR SI HAY QUE CAMBIARLO
--
--   1 Manrique   2 Bello      3 America     4 Calasanz   5 Itagui    6 Poblado
--   7 La Mota    8 Envigado   9 Pilarica   10 San Antonio
--  11 Manrique Piloto        12 Bodega     13 Niquia    14 Tienda Medayoung
--
-- COMO SE HACE A MANO DE AQUI EN ADELANTE
--
--   UPDATE general.empleado SET idtienda_reportes = <idtienda> WHERE id = <idempleado>;
--   UPDATE general.empleado SET idtienda_reportes = 0          WHERE id = <idempleado>;  -- volver a deducir
--
-- El idempleado se saca del usuario con el que entra al central:
--
--   SELECT u.nombre, u.idempleado, e.nombre_largo, e.idtienda_reportes, e.supernumerario
--     FROM pizzaamericana.usuario u JOIN general.empleado e ON e.id = u.idempleado
--    WHERE u.nombre = 'hrojas';
--
-- El cambio se ve cuando la persona vuelve a entrar al central: la tienda se guarda en
-- su sesion al abrirla.
--
-- PARA DEVOLVERLO
--
--   ALTER TABLE general.empleado DROP COLUMN idtienda_reportes;
-- ---------------------------------------------------------------------------

USE general;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'general' AND TABLE_NAME = 'empleado'
                  AND COLUMN_NAME = 'idtienda_reportes');
SET @sql = IF(@existe = 0,
        'ALTER TABLE empleado ADD COLUMN idtienda_reportes INT NOT NULL DEFAULT 0 COMMENT ''Tienda fija para los reportes del central. 0 = deducirla de las horas trabajadas''',
        'SELECT ''la columna idtienda_reportes ya existia''');
PREPARE p FROM @sql; EXECUTE p; DEALLOCATE PREPARE p;

UPDATE empleado SET idtienda_reportes = 11 WHERE id = 807 AND idtienda_reportes <> 11;
UPDATE empleado SET idtienda_reportes =  3 WHERE id = 806 AND idtienda_reportes <>  3;

-- Comprobacion: los dos con su tienda.
SELECT e.id, e.nombre_largo, e.idtienda_reportes, t.nombre AS tienda
FROM empleado e LEFT JOIN pizzaamericana.tienda t ON t.idtienda = e.idtienda_reportes
WHERE e.idtienda_reportes > 0 ORDER BY e.id;
