-- =====================================================================
-- PANTALLA "BANCO DE PREGUNTAS A EMPLEADOS" Y SU ROL
-- Base de datos: pizzaamericana (la CENTRAL, 172.19.0.25). Idempotente.
--
-- Administra el banco de preguntas que el huellero del POS le hace al empleado al dar ingreso (tablas de
-- general: pregunta_empleado, pregunta_empleado_tipo, opcion_respuesta, opcion_respuesta_pregunta). Esas tablas YA
-- existen (ver preguntas_empleado_biometria.sql); este script solo registra la pantalla y el rol.
--
-- Un ROL con un miembro (jdbotero); el servlet vuelve a revisar el rol en cada peticion (SeguridadFilter esta en LOG).
-- Para darle acceso a otra persona: "Asignar rol a usuario" > "Banco de Preguntas a Empleados".
-- =====================================================================

INSERT INTO pizzaamericana.rol (nombre, descripcion, activo)
SELECT 'Banco de Preguntas a Empleados',
       'Crea, edita, activa e inactiva las preguntas que el huellero le hace a los empleados al dar ingreso.',
       'S'
WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.rol WHERE nombre = 'Banco de Preguntas a Empleados');

-- Modulo 3 (monitoreo y analisis), junto a "Resultado Preguntas a Empleados" (orden 111)
INSERT INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Banco de Preguntas a Empleados', 3, 'BancoPreguntasEmpleado.html', 110, 'S'
WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla WHERE url_html = 'BancoPreguntasEmpleado.html');

INSERT INTO pizzaamericana.rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM pizzaamericana.rol r, pizzaamericana.pantalla p
WHERE r.nombre = 'Banco de Preguntas a Empleados'
  AND p.url_html = 'BancoPreguntasEmpleado.html'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.rol_pantalla rp WHERE rp.idrol = r.idrol AND rp.idpantalla = p.idpantalla);

INSERT INTO pizzaamericana.pantalla_servlet (idpantalla, patron_url)
SELECT p.idpantalla, '/BancoPreguntasEmpleado'
FROM pizzaamericana.pantalla p
WHERE p.url_html = 'BancoPreguntasEmpleado.html'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla_servlet ps WHERE ps.idpantalla = p.idpantalla AND ps.patron_url = '/BancoPreguntasEmpleado');

INSERT INTO pizzaamericana.usuario_rol (idusuario, idrol)
SELECT u.id, r.idrol
FROM pizzaamericana.usuario u, pizzaamericana.rol r
WHERE u.nombre = 'jdbotero'
  AND r.nombre = 'Banco de Preguntas a Empleados'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.usuario_rol ur WHERE ur.idusuario = u.id AND ur.idrol = r.idrol);

-- Verificacion
SELECT p.idpantalla, p.nombre, p.url_html, (SELECT COUNT(*) FROM pizzaamericana.pantalla_servlet s WHERE s.idpantalla = p.idpantalla) AS servlets
FROM pizzaamericana.pantalla p WHERE p.url_html = 'BancoPreguntasEmpleado.html';
SELECT r.nombre AS rol, u.nombre AS usuario
FROM pizzaamericana.rol r
LEFT JOIN pizzaamericana.usuario_rol ur ON ur.idrol = r.idrol
LEFT JOIN pizzaamericana.usuario u ON u.id = ur.idusuario
WHERE r.nombre = 'Banco de Preguntas a Empleados';
