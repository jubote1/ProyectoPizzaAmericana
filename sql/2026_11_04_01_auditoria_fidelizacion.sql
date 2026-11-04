-- =====================================================================
-- PANTALLA "AUDITORIA DE FIDELIZACION" Y SU ROL
-- Base de datos: pizzaamericana (la CENTRAL, 172.19.0.25). Idempotente.
--
-- Pantalla de revision de redenciones de puntos: quien redimio, que, cuando y
-- en que tienda, y al dar clic en una persona todo su historial de puntos.
-- Existe para revisar la sospecha de que empleados usen mal los puntos.
--
-- POR QUE UN ROL NUEVO Y NO AMARRARLA A UN USUARIO
--
-- Se pidio que solo la viera Juan David Botero. Amarrar el id 1 en el codigo
-- obliga a tocar y desplegar el war para dar o quitar el acceso. Con un rol la
-- decision queda en datos: se da o se quita desde "Asignar rol a usuario".
--
-- OJO, Y ES IMPORTANTE: general.parametros.SEGURIDADFILTROMODO esta en LOG, o
-- sea que SeguridadFilter audita pero NO bloquea. Esconder la opcion del menu
-- es cosmetico: quien sepa la URL entra igual. Por eso el servlet
-- ConsultarAuditoriaFidelizacion revisa el rol el mismo, leyendolo de la base
-- en cada peticion, y no se confia del filtro ni de los roles que quedaron en
-- la sesion al ingresar.
-- =====================================================================

-- 1. EL ROL -----------------------------------------------------------
INSERT INTO pizzaamericana.rol (nombre, descripcion, activo)
SELECT 'Auditoria de Fidelizacion',
       'Revisa redenciones de puntos y el historial de acumulacion de cada cliente. Pensado para auditar el uso indebido de puntos.',
       'S'
WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.rol WHERE nombre = 'Auditoria de Fidelizacion');

-- 2. LA PANTALLA ------------------------------------------------------
-- Va en Reporteria (idmodulo 7), que es donde se pidio.
INSERT INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Auditoria de Fidelizacion', 7, 'AuditoriaFidelizacion.html', 90, 'S'
WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla WHERE url_html = 'AuditoriaFidelizacion.html');

-- 3. LA PANTALLA PARA EL ROL -----------------------------------------
INSERT INTO pizzaamericana.rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM pizzaamericana.rol r, pizzaamericana.pantalla p
WHERE r.nombre = 'Auditoria de Fidelizacion'
  AND p.url_html = 'AuditoriaFidelizacion.html'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.rol_pantalla rp
                  WHERE rp.idrol = r.idrol AND rp.idpantalla = p.idpantalla);

-- El superadministrador ve todas las pantallas por definicion, pero se deja
-- explicito para que la opcion aparezca en el menu sin depender de esa regla.
INSERT INTO pizzaamericana.rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM pizzaamericana.rol r, pizzaamericana.pantalla p
WHERE r.nombre = 'Superadministrador'
  AND p.url_html = 'AuditoriaFidelizacion.html'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.rol_pantalla rp
                  WHERE rp.idrol = r.idrol AND rp.idpantalla = p.idpantalla);

-- 4. EL SERVLET QUE ALIMENTA LA PANTALLA ------------------------------
-- La columna es patron_url y lleva la ruta con barra inicial, como la de
-- ReporteOperacionRed: '/ConsultarOperacionRed'.
INSERT INTO pizzaamericana.pantalla_servlet (idpantalla, patron_url)
SELECT p.idpantalla, '/ConsultarAuditoriaFidelizacion'
FROM pizzaamericana.pantalla p
WHERE p.url_html = 'AuditoriaFidelizacion.html'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla_servlet ps
                  WHERE ps.idpantalla = p.idpantalla
                    AND ps.patron_url = '/ConsultarAuditoriaFidelizacion');

-- 5. EL ROL PARA JUAN DAVID BOTERO ------------------------------------
INSERT INTO pizzaamericana.usuario_rol (idusuario, idrol)
SELECT u.id, r.idrol
FROM pizzaamericana.usuario u, pizzaamericana.rol r
WHERE u.nombre = 'jdbotero'
  AND r.nombre = 'Auditoria de Fidelizacion'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.usuario_rol ur
                  WHERE ur.idusuario = u.id AND ur.idrol = r.idrol);

-- 6. INDICES ----------------------------------------------------------
-- El detalle por persona filtra fidelizacion_transaccion por correo, y la
-- llave primaria es (correo, idtienda, idpedidotienda): el prefijo sirve, asi
-- que no hace falta indice nuevo ahi. Lo que si se consulta sin indice util es
-- el cruce del correo del cliente contra el de la nomina.
SET @existe := (SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = 'general' AND table_name = 'empleado'
                  AND index_name = 'idx_empleado_email');
SET @sql := IF(@existe = 0,
  'CREATE INDEX idx_empleado_email ON general.empleado (email)',
  'SELECT ''idx_empleado_email ya existe'' AS nota');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- 7. COMPROBACION -----------------------------------------------------
SELECT 'rol'            AS objeto, COUNT(*) AS filas FROM pizzaamericana.rol            WHERE nombre  = 'Auditoria de Fidelizacion'
UNION ALL
SELECT 'pantalla',              COUNT(*) FROM pizzaamericana.pantalla         WHERE url_html = 'AuditoriaFidelizacion.html'
UNION ALL
SELECT 'rol_pantalla',          COUNT(*) FROM pizzaamericana.rol_pantalla rp
                                          JOIN pizzaamericana.pantalla p ON p.idpantalla = rp.idpantalla
                                         WHERE p.url_html = 'AuditoriaFidelizacion.html'
UNION ALL
SELECT 'pantalla_servlet',      COUNT(*) FROM pizzaamericana.pantalla_servlet WHERE patron_url = '/ConsultarAuditoriaFidelizacion'
UNION ALL
SELECT 'usuario_rol jdbotero',  COUNT(*) FROM pizzaamericana.usuario_rol ur
                                          JOIN pizzaamericana.usuario u ON u.id = ur.idusuario
                                          JOIN pizzaamericana.rol r ON r.idrol = ur.idrol
                                         WHERE u.nombre = 'jdbotero' AND r.nombre = 'Auditoria de Fidelizacion';
