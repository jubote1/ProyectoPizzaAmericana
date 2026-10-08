-- ---------------------------------------------------------------------------
-- Rol "Administrador de Tienda": el primero que se crea desde cero (los otros
-- tres nacieron migrando el N/S/P viejo). Arranca con dos pantallas:
--
--   idpantalla 42  Consultar Ubicacion Domiciliario (mapa.jsp)
--   idpantalla 57  Desempeno de Domiciliarios (DesempenoDomiciliario.html)
--
-- Las dos pantallas ya pedian sesion salvo mapa.jsp, que no pedia ninguna (ver
-- el cambio en mapa.jsp de esta misma tanda). Un usuario con SOLO este rol
-- (administrador = 'N') ve estas dos pantallas y nada mas -su menu sale de
-- MenuDinamico.html/GetMenuUsuario, no del Menu.html de Operario-.
--
-- Aditivo e idempotente. Para dar de alta un administrador de tienda:
--   1. Crear el usuario como cualquier otro (administrador = 'N').
--   2. Asignarle este rol desde AsignarRolUsuario.html (o con un INSERT en
--      usuario_rol si se prefiere por SQL: insert into usuario_rol values (idusuario, idrol)).
-- ---------------------------------------------------------------------------

USE pizzaamericana;

INSERT INTO rol (nombre, descripcion, activo)
SELECT 'Administrador de Tienda',
       'Ve el mapa de ubicacion de domiciliarios y el desempeno de domiciliarios de su tienda.', 'S'
WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre = 'Administrador de Tienda');

INSERT INTO rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
  FROM rol r, pantalla p
 WHERE r.nombre = 'Administrador de Tienda' AND p.idpantalla IN (42, 57)
   AND NOT EXISTS (SELECT 1 FROM rol_pantalla rp WHERE rp.idrol = r.idrol AND rp.idpantalla = p.idpantalla);

-- Verificacion
SELECT r.idrol, r.nombre, p.idpantalla, p.nombre AS pantalla, p.url_html
  FROM rol r JOIN rol_pantalla rp ON rp.idrol = r.idrol JOIN pantalla p ON p.idpantalla = rp.idpantalla
 WHERE r.nombre = 'Administrador de Tienda';
