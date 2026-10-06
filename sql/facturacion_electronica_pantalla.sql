-- ---------------------------------------------------------------------------
-- LA PANTALLA DE FACTURACION ELECTRONICA EN EL CATALOGO DE SEGURIDAD
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
--
-- Registra FacturacionElectronica.html (reporte mensual de facturado, notas credito y neto por tienda) en
-- pizzaamericana.pantalla, en el modulo 3 -monitoreo y analisis-, igual que DesempenoDomiciliario.html.
-- Solo agrega la fila: los permisos por rol se asignan aparte, desde "Asignar Pantallas a Rol".
--
-- Requiere antes 2026_10_06_02_datamart_facturacion.sql (en la base datamart).
-- ---------------------------------------------------------------------------

INSERT IGNORE INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
VALUES ('Facturacion Electronica', 3, 'FacturacionElectronica.html', 120, 'S');

SELECT idpantalla, nombre, idmodulo, url_html, orden, activo
  FROM pizzaamericana.pantalla
 WHERE url_html = 'FacturacionElectronica.html';
