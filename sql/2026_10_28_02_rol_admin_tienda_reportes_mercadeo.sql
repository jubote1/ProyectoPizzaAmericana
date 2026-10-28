-- ---------------------------------------------------------------------------
-- REPORTES DE MERCADEO PARA EL ROL "ADMINISTRADOR DE TIENDA" (base pizzaamericana)
--
-- Aditivo e idempotente: se puede correr dos veces. Corre tal cual en HeidiSQL.
--
-- QUE HACE
--
-- Le ADICIONA al rol 4 (Administrador de Tienda) la pantalla 83 "Reportes de Mercadeo"
-- (ReportesMercadeo.html, modulo 8 Mercadeo). NO quita nada: el rol conserva las tres
-- que ya tenia -Consultar Ubicacion Domiciliario, Desempeno de Domiciliarios y Operacion
-- Domiciliarios Diaria-.
--
-- No hace falta tocar el modulo: no existe tabla rol_modulo, el menu dinamico arma el
-- arbol desde rol_pantalla, asi que el modulo "Mercadeo" le aparece solo al rol en
-- cuanto tenga una pantalla adentro.
--
-- A QUIENES AFECTA
--
-- A los 13 usuarios que hoy tienen el rol 4: arenteria, amarquez, bdiaz, dnarvaez,
-- eprimera, eosorio, mbenites, mortiz, mmazo, ssalazarh, yelabrador, mhenao y hrojas.
--
-- OJO: NO SE VE HASTA QUE SE DESPLIEGUE EL WAR
--
-- Los 13 tienen administrador = 'N', o sea que hoy cargan el Menu.html estatico, y
-- ReportesMercadeo.html NO esta en ninguno de los tres menus estaticos (se llega solo
-- escribiendo la URL). Esta fila solo surte efecto cuando se despliegue el WAR del menu
-- dinamico (commit cc6cfd3 de JuanDavidClaude). Antes de eso, el permiso queda listo
-- pero el administrador de tienda sigue sin ver la opcion.
--
-- PARA DEVOLVERLO
--
--   DELETE FROM pizzaamericana.rol_pantalla WHERE idrol = 4 AND idpantalla = 83;
-- ---------------------------------------------------------------------------

USE pizzaamericana;

INSERT INTO rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM rol r
JOIN pantalla p ON p.url_html = 'ReportesMercadeo.html'
WHERE r.nombre = 'Administrador de Tienda'
  AND NOT EXISTS (
        SELECT 1 FROM rol_pantalla rp
         WHERE rp.idrol = r.idrol AND rp.idpantalla = p.idpantalla);

-- Comprobacion: debe listar las CUATRO pantallas del rol.
SELECT p.idpantalla, p.nombre, p.url_html, m.nombre AS modulo
FROM rol_pantalla rp
JOIN pantalla p ON p.idpantalla = rp.idpantalla
LEFT JOIN menu_modulo m ON m.idmodulo = p.idmodulo
JOIN rol r ON r.idrol = rp.idrol
WHERE r.nombre = 'Administrador de Tienda'
ORDER BY m.nombre, p.nombre;
