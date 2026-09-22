-- ---------------------------------------------------------------------------
-- La pantalla de definicion de segmentos, en el catalogo
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
--
-- ES LO UNICO QUE HACE FALTA EN BASE DE DATOS PARA ESTA PANTALLA
--
-- Las tablas ya estan: crm.segmento_definicion, crm.segmento_regla y
-- crm.segmento_campo se crearon con crm_segmentos_definibles_1_tablas.sql, y el
-- procedimiento crm.pr_clasificar_segmentos con el _2_. Esto solo la registra.
--
-- Y ESTO NO CAMBIA NADA HOY
--
-- El menu que se ve en produccion es el estatico -MenuCRM.html-, no esta tabla.
-- La pantalla aparece porque se agrego el enlace en el HTML. Esta fila deja el
-- catalogo completo para cuando el menu dinamico se prenda y para que el
-- esquema de roles la pueda referenciar.
--
-- QUIEN ENTRA, Y POR QUE IMPORTA MAS QUE EN LAS OTRAS
--
-- Lo decide usuario.acceso_crm, igual que el resto del area. Pero esta pantalla
-- no lee: cambia como quedan clasificadas 450 mil personas y puede disparar una
-- reclasificacion sobre la tabla que el CRM esta leyendo en ese momento. El
-- servicio AdministrarSegmentos valida por su cuenta y ademas exige POST para
-- todo lo que escribe; esconder la opcion del menu no protege nada.
-- ---------------------------------------------------------------------------

INSERT IGNORE INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
VALUES ('Definicion de Segmentos', 4, 'DefinicionSegmentos.html', 56, 'S');

-- Como quedo
SELECT idpantalla, nombre, url_html, orden, activo
  FROM pizzaamericana.pantalla WHERE idmodulo = 4 ORDER BY orden;
