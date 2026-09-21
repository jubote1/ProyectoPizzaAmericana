-- ---------------------------------------------------------------------------
-- La pantalla de segmentacion de personas, en el catalogo
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
--
-- ES LO UNICO QUE HACE FALTA EN BASE DE DATOS PARA ESTA PANTALLA
--
-- Los datos ya estan: crm.persona_resumen la llena el proceso nocturno, y las
-- tablas se crearon con crm_persona_resumen_1_tablas.sql. Esta pantalla solo
-- lee. Lo que falta es registrarla en el catalogo de pantallas.
--
-- Y ESTO NO CAMBIA NADA HOY
--
-- El menu que se ve en produccion es el estatico -MenuCRM.html-, no esta tabla:
-- MenuDinamico.html existe pero todavia nadie lo usa. La pantalla aparece en el
-- menu del CRM porque se agrego el enlace en el HTML, no por esta fila. Esto
-- deja el catalogo completo para cuando el menu dinamico se prenda, y para que
-- el esquema de roles y permisos la pueda referenciar.
--
-- QUIEN ENTRA
--
-- Lo decide usuario.acceso_crm, igual que las demas del area. Los dos servicios
-- -ConsultarSegmentacionPersona y DescargarSegmentacionPersona- preguntan por
-- su cuenta antes de contestar: esconder la opcion del menu no protege nada
-- porque la URL se escribe a mano.
-- ---------------------------------------------------------------------------

INSERT IGNORE INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
VALUES ('Segmentacion de Personas', 4, 'SegmentacionPersona.html', 55, 'S');

-- Como quedo
SELECT idpantalla, nombre, url_html, orden, activo
  FROM pizzaamericana.pantalla WHERE idmodulo = 4 ORDER BY orden;
