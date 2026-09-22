-- ---------------------------------------------------------------------------
-- El tablero de segmentos, en el catalogo
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
--
-- ES LO UNICO QUE HACE FALTA EN BASE DE DATOS PARA ESTA PANTALLA
--
-- El tablero solo lee, y lee del mismo servicio que la pantalla de
-- definiciones: AdministrarSegmentos con accion listar. No tiene tablas
-- propias ni servicio propio.
--
-- Y ESTO NO CAMBIA NADA HOY
--
-- El menu que se ve en produccion es el estatico -MenuCRM.html-, no esta tabla.
-- La pantalla aparece porque se agrego el enlace en el HTML. Esta fila deja el
-- catalogo completo para cuando el menu dinamico se prenda.
--
-- QUIEN ENTRA
--
-- usuario.acceso_crm, igual que el resto del area. Aunque el tablero no
-- escriba, resume datos de 450 mil personas: el servicio valida por su cuenta
-- porque esconder la opcion del menu no protege nada.
-- ---------------------------------------------------------------------------

INSERT IGNORE INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
VALUES ('Tablero de Segmentos', 4, 'TableroSegmentos.html', 54, 'S');

-- Como quedo
SELECT idpantalla, nombre, url_html, orden, activo
  FROM pizzaamericana.pantalla WHERE idmodulo = 4 ORDER BY orden;
