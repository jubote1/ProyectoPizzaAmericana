-- =====================================================================
-- PANTALLA "TABLERO DE PUNTOS" (plan Puntos Pizza) Y SU ROL
-- Base de datos: pizzaamericana (la CENTRAL, 172.19.0.25). Idempotente.
--
-- Tablero con afiliados, puntos asignados / vigentes / redimidos / vencidos, puntos por
-- vencerse, redenciones y consultas. Solo lee; no escribe nada.
--
-- QUIEN LA VE
--   * Rol NUEVO "Tablero de Puntos": ve el tablero completo MENOS las dos consultas de
--     EMPLEADOS (quien redime y quien acumula), que senalan personas de la nomina y son lo que
--     protege la Auditoria de Fidelizacion. Esas dos las revisa el servlet, no solo la pantalla.
--   * "Auditoria de Fidelizacion" y "Superadministrador": ven todo, incluidos los empleados.
--
-- A QUIEN SE LE DA: a Juan David Diaz. Se busca por nombre_largo y SOLO se asigna si hay
-- exactamente UNA coincidencia: dos "Juan David Diaz" (o ninguno) y no se asigna a nadie. La
-- consulta final dice cuantas hubo y quien quedo con el rol; si no fue exactamente uno, se
-- asigna a mano desde "Asignar rol a usuario".
--
-- OJO: general.parametros.SEGURIDADFILTROMODO esta en LOG, o sea que SeguridadFilter audita
-- pero NO bloquea. Esconder la opcion del menu es cosmetico: el servlet
-- ConsultarTableroPuntos revisa el rol el mismo, leyendolo de la base en cada peticion.
--
-- ORDEN DE DESPLIEGUE: primero el WAR, despues este script. Si se corre antes, la opcion del
-- menu aparece y abre una pagina que todavia no existe.
-- =====================================================================

-- 1. EL ROL -----------------------------------------------------------
INSERT INTO pizzaamericana.rol (nombre, descripcion, activo)
SELECT 'Tablero de Puntos',
       'Ve el tablero del plan de puntos (afiliados, puntos asignados, vigentes, por vencer y redenciones). No ve los rankings de empleados.',
       'S'
WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.rol WHERE nombre = 'Tablero de Puntos');

-- 2. LA PANTALLA ------------------------------------------------------
-- Va en "Clientes y CRM" (idmodulo 4), junto al Tablero de Segmentos (orden 60).
INSERT INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Tablero de Puntos', 4, 'TableroPuntos.html', 65, 'S'
WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla WHERE url_html = 'TableroPuntos.html');

-- 3. LA PANTALLA PARA LOS TRES ROLES ----------------------------------
INSERT INTO pizzaamericana.rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM pizzaamericana.rol r, pizzaamericana.pantalla p
WHERE r.nombre IN ('Tablero de Puntos', 'Auditoria de Fidelizacion', 'Superadministrador')
  AND p.url_html = 'TableroPuntos.html'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.rol_pantalla rp
                  WHERE rp.idrol = r.idrol AND rp.idpantalla = p.idpantalla);

-- 4. EL SERVLET QUE ALIMENTA LA PANTALLA ------------------------------
INSERT INTO pizzaamericana.pantalla_servlet (idpantalla, patron_url)
SELECT p.idpantalla, '/ConsultarTableroPuntos'
FROM pizzaamericana.pantalla p
WHERE p.url_html = 'TableroPuntos.html'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla_servlet ps
                  WHERE ps.idpantalla = p.idpantalla
                    AND ps.patron_url = '/ConsultarTableroPuntos');

-- 5. EL ROL PARA JUAN DAVID DIAZ --------------------------------------
SET @coincidencias := (SELECT COUNT(*) FROM pizzaamericana.usuario u
                        WHERE u.nombre_largo LIKE '%JUAN%DAVID%'
                          AND (u.nombre_largo LIKE '%DIAZ%' OR u.nombre_largo LIKE '%DÍAZ%'));

INSERT INTO pizzaamericana.usuario_rol (idusuario, idrol)
SELECT u.id, r.idrol
FROM pizzaamericana.usuario u, pizzaamericana.rol r
WHERE @coincidencias = 1
  AND u.nombre_largo LIKE '%JUAN%DAVID%'
  AND (u.nombre_largo LIKE '%DIAZ%' OR u.nombre_largo LIKE '%DÍAZ%')
  AND r.nombre = 'Tablero de Puntos'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.usuario_rol ur
                  WHERE ur.idusuario = u.id AND ur.idrol = r.idrol);

-- 6. COMPROBACION -----------------------------------------------------
SELECT 'rol'              AS objeto, COUNT(*) AS filas FROM pizzaamericana.rol WHERE nombre = 'Tablero de Puntos'
UNION ALL
SELECT 'pantalla',                   COUNT(*) FROM pizzaamericana.pantalla WHERE url_html = 'TableroPuntos.html'
UNION ALL
SELECT 'rol_pantalla (esperado 3)',  COUNT(*) FROM pizzaamericana.rol_pantalla rp
                                       JOIN pizzaamericana.pantalla p ON p.idpantalla = rp.idpantalla
                                      WHERE p.url_html = 'TableroPuntos.html'
UNION ALL
SELECT 'pantalla_servlet',           COUNT(*) FROM pizzaamericana.pantalla_servlet WHERE patron_url = '/ConsultarTableroPuntos'
UNION ALL
SELECT 'coincidencias Juan David Diaz (esperado 1)', @coincidencias;

-- Quien quedo con el rol de tablero.
SELECT u.id, u.nombre AS usuario, u.nombre_largo, u.activo
FROM pizzaamericana.usuario_rol ur
JOIN pizzaamericana.usuario u ON u.id = ur.idusuario
JOIN pizzaamericana.rol r ON r.idrol = ur.idrol
WHERE r.nombre = 'Tablero de Puntos';
