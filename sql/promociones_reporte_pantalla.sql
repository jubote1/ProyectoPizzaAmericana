-- ---------------------------------------------------------------------------
-- La pantalla del catalogo de promociones entra al catalogo de seguridad
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
-- Requiere promociones_reporte_catalogo.sql.
--
-- La pantalla deja administrar que promociones mide el reporte diario. Va en
-- el modulo Funciones, al lado de Venta Integral, que es el otro catalogo
-- parametrizable del mismo estilo.
--
-- No se le asigna a ningun rol: hoy el menu lo decide usuario.administrador y
-- la pantalla solo exige sesion. Se registra para que el dia que se pase a
-- roles de verdad ya este en la lista y no haya que acordarse de agregarla.
-- ---------------------------------------------------------------------------

INSERT INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Promociones del Reporte Diario', m.idmodulo, 'PromocionesReporte.html', 240, 'S'
  FROM pizzaamericana.menu_modulo m
 WHERE m.nombre = 'Funciones'
   AND NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla ya
                    WHERE ya.url_html = 'PromocionesReporte.html');

SELECT idpantalla, nombre, url_html, orden, activo
  FROM pizzaamericana.pantalla WHERE url_html = 'PromocionesReporte.html';
