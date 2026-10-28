-- ---------------------------------------------------------------------------
-- Usuarios del central para los administradores de tienda
--
-- A cada empleado ACTIVO con cargo "Administrador Punto de Venta" (general.empleado,
-- idtipoempleado = 2) se le crea un usuario del central y se le asigna el rol
-- "Administrador de Tienda", que solo ve las pantallas de ese rol (ver
-- rol_administrador_tienda.sql y reporte_operacion_domiciliarios.sql).
--
-- Usuario:   primera letra del nombre + primer apellido, en minuscula. Ej.: Juan Botero -> jbotero.
-- Clave:     igual al usuario (lo pidio Juan David; cada uno debe cambiarla al entrar).
-- Es un usuario normal del central: administrador = 'N' (la pantalla que ven sale de su ROL),
-- sin CRM, sin contact center, y con idempleado = el id de general.empleado.
--
-- Lo que el nombre no resuelve:
--   * Sara Salazar Holguin  -> 'ssalazar' YA EXISTE (usuario "Sara Salazar", sin roles): se crea 'ssalazarh'
--     (apellido + inicial del segundo apellido, como 'yalvarezi' o 'mnzapataz').
--   * Yesica Labrador       -> 'ylabrador' YA EXISTE (usuario "Yessica Labrador", sin roles): se crea 'yelabrador'
--     (dos letras del nombre, porque no tiene segundo apellido).
--   Los dos usuarios viejos NO se tocan: si son de la misma persona y ya no los usa, se desactivan aparte.
--
-- Aditivo e idempotente: un usuario que ya existe (mismo nombre) no se vuelve a crear ni se le cambia la clave.
-- Los 13 administradores activos al 28 de octubre de 2026. Para sumar uno nuevo, copiar una fila y poner su id de empleado.
-- ---------------------------------------------------------------------------

USE pizzaamericana;

INSERT INTO usuario (nombre, password, nombre_largo, administrador, teletrabajador, plataforma, idempleado, activo, acceso_crm, contact_center)
SELECT t.usuario, t.usuario, e.nombre_largo, 'N', 'N', 'N', e.id, 1, 'N', 0
  FROM (
        SELECT 510 AS idempleado, 'arenteria' AS usuario UNION ALL   -- Ana Marcela Renteria Incel
        SELECT 557, 'amarquez'   UNION ALL                           -- Anthony Alejandro Marquez Bravo
        SELECT 515, 'bdiaz'      UNION ALL                           -- Barbara Gracia Diaz Vargas
        SELECT 474, 'dnarvaez'   UNION ALL                           -- Danilo Andres Narvaez Castillo
        SELECT 356, 'eprimera'   UNION ALL                           -- Eduardo Enrique Primera Ramirez
        SELECT 767, 'eosorio'    UNION ALL                           -- Estefania Osorio Betancur
        SELECT 588, 'mbenites'   UNION ALL                           -- Maria Jose Benites Bello
        SELECT 624, 'mortiz'     UNION ALL                           -- Martha Isabel Ortiz Ruiz
        SELECT 631, 'mmazo'      UNION ALL                           -- Mayra Alejandra Mazo Cardona
        SELECT 658, 'ssalazarh'  UNION ALL                           -- Sara Salazar Holguin
        SELECT 20,  'yelabrador'  UNION ALL                           -- Yesica Labrador
        SELECT 806, 'mhenao'      UNION ALL                           -- Marina Elena Henao Hernandez
        SELECT 807, 'hrojas'                                          -- Hermes Arley Rojas Peralta
       ) t
  JOIN general.empleado e ON e.id = t.idempleado AND e.idtipoempleado = 2
 WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE u.nombre = t.usuario);

INSERT INTO usuario_rol (idusuario, idrol)
SELECT u.id, r.idrol
  FROM usuario u
  JOIN rol r ON r.nombre = 'Administrador de Tienda'
 WHERE u.nombre IN ('arenteria', 'amarquez', 'bdiaz', 'dnarvaez', 'eprimera', 'eosorio', 'mbenites', 'mortiz', 'mmazo',
                    'ssalazarh', 'yelabrador', 'mhenao', 'hrojas')
   AND NOT EXISTS (SELECT 1 FROM usuario_rol ur WHERE ur.idusuario = u.id AND ur.idrol = r.idrol);

-- Verificacion: deben salir 13 filas.
SELECT u.id, u.nombre AS usuario, u.nombre_largo, u.idempleado, u.activo, r.nombre AS rol
  FROM usuario u
  JOIN usuario_rol ur ON ur.idusuario = u.id
  JOIN rol r ON r.idrol = ur.idrol
 WHERE r.nombre = 'Administrador de Tienda'
 ORDER BY u.nombre;
