-- Deshace lo que corrieron reporte_operacion_domiciliarios.sql y usuarios_administradores_tienda.sql el 2026-10-28.
-- NO se corre solo: solo si se quiere devolver. Borra unicamente lo que esos dos scripts crearon.
USE pizzaamericana;

-- Los 13 usuarios nuevos (ids 131 a 143 el 28-oct) y su rol.
DELETE ur FROM usuario_rol ur JOIN usuario u ON u.id = ur.idusuario
 WHERE u.nombre IN ('arenteria', 'amarquez', 'bdiaz', 'dnarvaez', 'eprimera', 'eosorio', 'mbenites', 'mortiz', 'mmazo',
                    'ssalazarh', 'yelabrador', 'mhenao', 'hrojas');
DELETE FROM usuario
 WHERE nombre IN ('arenteria', 'amarquez', 'bdiaz', 'dnarvaez', 'eprimera', 'eosorio', 'mbenites', 'mortiz', 'mmazo',
                  'ssalazarh', 'yelabrador', 'mhenao', 'hrojas');

-- La pantalla (idpantalla 92) y sus permisos.
DELETE rp FROM rol_pantalla rp JOIN pantalla p ON p.idpantalla = rp.idpantalla
 WHERE p.url_html = 'ReporteOperacionDomiciliarios.html';
DELETE FROM pantalla WHERE url_html = 'ReporteOperacionDomiciliarios.html';
