-- ---------------------------------------------------------------------------
-- Reporte de operacion de domiciliarios diaria
--
-- Registra la pantalla ReporteOperacionDomiciliarios.html y la reparte entre los roles:
--
--   * Administrador de Tienda: el rol que ya tiene mapa.jsp y DesempenoDomiciliario.html
--     (ver rol_administrador_tienda.sql). Es el que recibe a los administradores de punto de
--     venta (ver usuarios_administradores_tienda.sql).
--   * Lider Administrativo y Superadministrador: para que quien administra el central lo vea
--     igual que los otros reportes. Si esos roles todavia no existen (no se ha corrido
--     seguridad_menus_reorganizacion.sql) simplemente no se les da nada, y no pasa nada.
--
-- MODULO: "Reporteria" si ya se corrio seguridad_menus_reorganizacion.sql -ahi viven los otros
-- reportes de domiciliarios-; si no, "Monitoreo", donde estaba Desempeno de Domiciliarios.
-- Corra el orden que corra, la pantalla queda en un modulo que existe.
--
-- Lo demas del reporte NO necesita base de datos: lee los despachos de la tienda (o del
-- datamart si el dia ya cerro) y la presencia de general. Sin migraciones.
--
-- Aditivo e idempotente: se puede correr dos veces.
--
-- Si se quiere que el administrador de tienda vea SOLO este reporte, se le quitan
-- las otras dos pantallas del rol con:
--
--   DELETE rp FROM rol_pantalla rp JOIN rol r ON r.idrol = rp.idrol
--    WHERE r.nombre = 'Administrador de Tienda' AND rp.idpantalla IN (42, 57);
-- ---------------------------------------------------------------------------

USE pizzaamericana;
SET NAMES utf8mb4;

-- orden 150: despues del ultimo reporte de la reorganizacion (Costo por Tienda, 140).
INSERT INTO pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Operacion Domiciliarios Diaria',
       COALESCE((SELECT idmodulo FROM menu_modulo WHERE nombre = 'Reportería' LIMIT 1),
                (SELECT idmodulo FROM menu_modulo WHERE nombre = 'Monitoreo' LIMIT 1), 3),
       'ReporteOperacionDomiciliarios.html', 150, 'S'
 WHERE NOT EXISTS (SELECT 1 FROM pantalla WHERE url_html = 'ReporteOperacionDomiciliarios.html');

INSERT IGNORE INTO rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
  FROM rol r, pantalla p
 WHERE r.nombre IN ('Administrador de Tienda', 'Lider Administrativo', 'Superadministrador')
   AND p.url_html = 'ReporteOperacionDomiciliarios.html';

-- Verificacion
SELECT r.nombre AS rol, p.idpantalla, p.nombre AS pantalla, m.nombre AS modulo, p.url_html
  FROM rol r
  JOIN rol_pantalla rp ON rp.idrol = r.idrol
  JOIN pantalla p ON p.idpantalla = rp.idpantalla
  JOIN menu_modulo m ON m.idmodulo = p.idmodulo
 WHERE p.url_html = 'ReporteOperacionDomiciliarios.html';
