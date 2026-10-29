-- ---------------------------------------------------------------------------
-- REPORTE DE OPERACION DE DOMICILIARIOS DE TODA LA RED (base pizzaamericana)
--
-- Idempotente: se puede correr dos veces. Corre tal cual en HeidiSQL.
--
-- QUE HACE
--
-- Crea la pantalla "Operacion Domiciliarios Red" (ReporteOperacionRed.html), en el
-- modulo Reporteria, al lado de la que ya existe por tienda. Muestra UN dia con las
-- once tiendas juntas, una fila por tienda y un Total Red.
--
-- QUIEN LA VE
--
-- Los roles Lider Administrativo (2), Gerencia (5) y Superadministrador (6). Decision
-- de Juan David el 2026-10-29: al rol completo, no a una lista de personas. Hoy eso es:
--
--   Lider Administrativo (11): vpsalazar, ddmirandap, amforonda, mgallo, bsalazar,
--      mbranp, chincapie, jddiaz, dgomez, afloaizam, mvigoya
--   Gerencia / Superadministrador: jdbotero
--
-- El Administrador de Tienda (4) NO la tiene, que es justamente el punto: el ve su
-- tienda y nada mas.
--
-- Para agregar o quitar a alguien no hay que volver aqui: se usa la pantalla
-- "Asignar Rol a Usuario" del central.
--
-- LA FILA DE pantalla_servlet NO ES DECORATIVA
--
-- El servicio ConsultarOperacionRed comprueba el permiso contra pantalla_servlet
-- (PantallaDAO.tienePermisoParaUrl). Sin esa fila NADIE puede consultar el reporte,
-- ni siquiera quien tenga la pantalla: esconder la opcion del menu no impide escribir
-- la URL, asi que la comprobacion se hace en el servidor.
--
-- PARA DEVOLVERLO
--
--   DELETE FROM pizzaamericana.rol_pantalla WHERE idpantalla =
--          (SELECT idpantalla FROM pizzaamericana.pantalla WHERE url_html = 'ReporteOperacionRed.html');
--   DELETE FROM pizzaamericana.pantalla_servlet WHERE patron_url = '/ConsultarOperacionRed';
--   DELETE FROM pizzaamericana.pantalla WHERE url_html = 'ReporteOperacionRed.html';
-- ---------------------------------------------------------------------------

USE pizzaamericana;

-- 1. La pantalla, en Reporteria (modulo 7), justo despues de la de una tienda.
INSERT INTO pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Operacion Domiciliarios Red', 7, 'ReporteOperacionRed.html', 160, 'S'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM pantalla WHERE url_html = 'ReporteOperacionRed.html');

-- 2. El servicio que la respalda. De aqui sale la comprobacion de permiso.
INSERT INTO pantalla_servlet (idpantalla, patron_url)
SELECT p.idpantalla, '/ConsultarOperacionRed'
  FROM pantalla p
 WHERE p.url_html = 'ReporteOperacionRed.html'
   AND NOT EXISTS (SELECT 1 FROM pantalla_servlet ps
                    WHERE ps.idpantalla = p.idpantalla AND ps.patron_url = '/ConsultarOperacionRed');

-- 3. Los roles que la pueden ver.
INSERT INTO rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
  FROM rol r
  JOIN pantalla p ON p.url_html = 'ReporteOperacionRed.html'
 WHERE r.idrol IN (2, 5, 6)
   AND NOT EXISTS (SELECT 1 FROM rol_pantalla rp
                    WHERE rp.idrol = r.idrol AND rp.idpantalla = p.idpantalla);

-- Comprobacion 1: la pantalla y su servicio.
SELECT p.idpantalla, p.nombre, p.url_html, m.nombre AS modulo, ps.patron_url
  FROM pantalla p
  LEFT JOIN menu_modulo m ON m.idmodulo = p.idmodulo
  LEFT JOIN pantalla_servlet ps ON ps.idpantalla = p.idpantalla
 WHERE p.url_html = 'ReporteOperacionRed.html';

-- Comprobacion 2: quien queda viendola. Deben salir 12 usuarios.
SELECT r.nombre AS rol, u.nombre AS usuario, u.nombre_largo
  FROM rol_pantalla rp
  JOIN pantalla p ON p.idpantalla = rp.idpantalla
  JOIN rol r ON r.idrol = rp.idrol
  JOIN usuario_rol ur ON ur.idrol = r.idrol
  JOIN usuario u ON u.id = ur.idusuario AND u.activo = 1
 WHERE p.url_html = 'ReporteOperacionRed.html'
 ORDER BY r.idrol, u.nombre;
