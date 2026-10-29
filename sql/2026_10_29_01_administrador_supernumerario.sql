-- ---------------------------------------------------------------------------
-- QUIEN ES SUPERNUMERARIO ENTRE LOS ADMINISTRADORES DE PUNTO DE VENTA (base general)
--
-- Idempotente: se puede correr dos veces. Corre tal cual en HeidiSQL.
--
-- POR QUE
--
-- Desde ahora el administrador de punto de venta ve en los reportes UNICAMENTE su
-- tienda. Como no existe una columna "la tienda de esta persona" -los administradores
-- se mueven-, el sistema la DEDUCE de las horas de la semana pasada
-- (general.horario_trabajado) y se queda con la tienda donde mas trabajo.
--
-- Eso sirve para quien tiene una tienda. Los supernumerarios no la tienen: rotan a
-- proposito, y a ellos hay que dejarlos ver todas. De ahi la columna nueva.
--
-- POR QUE UNA COLUMNA NUEVA Y NO "rotacion"
--
-- general.empleado.rotacion significa casi lo mismo, pero NO esta libre: el POS ya la
-- usa (capaDAOPOS.UsuarioDAO) para meter a esa persona en la encuesta de evaluacion de
-- empleados de TODAS las tiendas. Marcar ahi a las dos administradoras les cambiaria
-- la encuesta de biometria, que es otro tema y nadie lo pidio. Por eso una columna
-- propia, que no lee nadie mas.
--
-- POR QUE A MANO Y NO ADIVINANDO
--
-- Medido el 2026-10-29 sobre seis semanas (14-sep al 25-oct): los administradores fijos
-- concentran entre el 81% y el 100% de sus horas en una sola tienda, y los dos
-- supernumerarios entre el 33% y el 84%. Las bandas se tocan -Mayra tuvo una semana del
-- 84%-, asi que decidirlo por porcentaje se equivocaria. Por eso la marca es manual.
--
-- A QUIENES MARCA
--
--   20   Yesica Labrador              (usuario yelabrador) - 3 a 5 tiendas por semana
--   631  Mayra Alejandra Mazo Cardona (usuario mmazo)      - 2 a 5 tiendas por semana
--
-- LOS DOS QUE QUEDAN SIN TIENDA
--
-- Marina Elena Henao (806, mhenao) y Hermes Arley Rojas (807, hrojas) entraron el
-- 2026-10-28 y NO tienen ni una hora registrada. Hasta que trabajen una semana el
-- sistema no les puede deducir tienda y los reportes les diran que avisen a Gestion
-- Humana. Si se necesita que vean algo antes, se les pone supernumerario = 1 aqui.
--
-- PARA DEVOLVERLO
--
--   ALTER TABLE general.empleado DROP COLUMN supernumerario;
-- ---------------------------------------------------------------------------

USE general;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'general' AND TABLE_NAME = 'empleado'
                  AND COLUMN_NAME = 'supernumerario');
SET @sql = IF(@existe = 0,
        'ALTER TABLE empleado ADD COLUMN supernumerario TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = rota entre puntos de venta: en los reportes ve todas las tiendas''',
        'SELECT ''la columna supernumerario ya existia''');
PREPARE p FROM @sql; EXECUTE p; DEALLOCATE PREPARE p;

UPDATE empleado SET supernumerario = 1 WHERE id IN (20, 631) AND supernumerario <> 1;

-- Comprobacion: deben salir los dos en 1.
SELECT id, nombre_largo, supernumerario FROM empleado WHERE supernumerario = 1 ORDER BY id;
