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

-- =====================================================================
-- PANTALLA "REPORTE DE PREGUNTAS POR EMPLEADO" (como le ha ido a un empleado en un rango)
-- Mismos permisos que "Resultado Preguntas a Empleados": quien ya ve esa pantalla ve esta.
-- =====================================================================
INSERT INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Reporte Preguntas por Empleado', 3, 'ReporteEmpleadoPreguntas.html', 112, 'S'
WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla WHERE url_html = 'ReporteEmpleadoPreguntas.html');

INSERT INTO pizzaamericana.rol_pantalla (idrol, idpantalla)
SELECT rp.idrol, nueva.idpantalla
FROM pizzaamericana.rol_pantalla rp
JOIN pizzaamericana.pantalla vieja ON vieja.idpantalla = rp.idpantalla AND vieja.url_html = 'ResultadoPreguntasEmpleado.html'
JOIN pizzaamericana.pantalla nueva ON nueva.url_html = 'ReporteEmpleadoPreguntas.html'
WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.rol_pantalla x WHERE x.idrol = rp.idrol AND x.idpantalla = nueva.idpantalla);

INSERT INTO pizzaamericana.pantalla_servlet (idpantalla, patron_url)
SELECT p.idpantalla, '/ConsultarReporteEmpleadoPreguntas'
FROM pizzaamericana.pantalla p
WHERE p.url_html = 'ReporteEmpleadoPreguntas.html'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla_servlet ps WHERE ps.idpantalla = p.idpantalla AND ps.patron_url = '/ConsultarReporteEmpleadoPreguntas');
-- La lista de empleados la alimenta un servlet que la otra pantalla ya registro.
INSERT INTO pizzaamericana.pantalla_servlet (idpantalla, patron_url)
SELECT p.idpantalla, '/ConsultarResumenPreguntasEmpleado'
FROM pizzaamericana.pantalla p
WHERE p.url_html = 'ReporteEmpleadoPreguntas.html'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla_servlet ps WHERE ps.idpantalla = p.idpantalla AND ps.patron_url = '/ConsultarResumenPreguntasEmpleado');

SELECT p.idpantalla, p.nombre, p.url_html FROM pizzaamericana.pantalla p WHERE p.url_html = 'ReporteEmpleadoPreguntas.html';

-- =====================================================================
-- El rol tambien VE las respuestas: se le dan las pantallas de resultados (todos los empleados y uno por uno).
-- Quien crea las preguntas tiene que poder ver como les va.
-- =====================================================================
INSERT INTO pizzaamericana.rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM pizzaamericana.rol r, pizzaamericana.pantalla p
WHERE r.nombre = 'Banco de Preguntas a Empleados'
  AND p.url_html IN ('ResultadoPreguntasEmpleado.html', 'ReporteEmpleadoPreguntas.html')
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.rol_pantalla x WHERE x.idrol = r.idrol AND x.idpantalla = p.idpantalla);

-- =====================================================================
-- QUIEN LO TIENE (decision del 2026-11-13): los lideres de zona (cargo "Supervisor de Operaciones Zona": Dairon
-- Miranda y Vivian Salazar), la direccion (Ana Maria Foronda) y Juan David Botero (arriba). Para sumar o quitar a
-- alguien: "Asignar rol a usuario" > "Banco de Preguntas a Empleados".
-- =====================================================================
INSERT INTO pizzaamericana.usuario_rol (idusuario, idrol)
SELECT u.id, r.idrol
FROM pizzaamericana.usuario u, pizzaamericana.rol r
WHERE u.nombre IN ('ddmirandap', 'vpsalazar', 'amforonda')
  AND r.nombre = 'Banco de Preguntas a Empleados'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.usuario_rol ur WHERE ur.idusuario = u.id AND ur.idrol = r.idrol);

SELECT r.nombre AS rol, u.nombre AS usuario, u.nombre_largo
FROM pizzaamericana.rol r
JOIN pizzaamericana.usuario_rol ur ON ur.idrol = r.idrol
JOIN pizzaamericana.usuario u ON u.id = ur.idusuario
WHERE r.nombre = 'Banco de Preguntas a Empleados' ORDER BY u.nombre_largo;
SELECT p.url_html FROM pizzaamericana.rol_pantalla rp JOIN pizzaamericana.rol r ON r.idrol = rp.idrol
JOIN pizzaamericana.pantalla p ON p.idpantalla = rp.idpantalla WHERE r.nombre = 'Banco de Preguntas a Empleados' ORDER BY 1;
