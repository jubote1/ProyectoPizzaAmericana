-- ---------------------------------------------------------------------------
-- LA PANTALLA DE DESEMPENO DE DOMICILIARIOS EN EL CATALOGO DE SEGURIDAD
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana.
--
-- QUE HACE
--
-- Registra DesempenoDomiciliario.html en pizzaamericana.pantalla, que es el
-- catalogo que usa el modulo de roles y pantallas. Solo agrega una fila: no
-- toca permisos de nadie.
--
-- POR QUE EL MODULO 3
--
-- El 3 es el de monitoreo y analisis -ahi estan Monitoreo Plataformas
-- Domicilios, Venta Integral y las conciliaciones-, que es lo que esta
-- pantalla hace. El 1 es la operacion de pedidos y el 2 los maestros.
--
-- LOS PERMISOS SE ASIGNAN APARTE, Y A PROPOSITO
--
-- No se inserta nada en rol_pantalla. Las cuatro pantallas registradas antes
-- que esta -conciliaciones y Venta Integral- tampoco tienen filas ahi: quien
-- ve que pantalla lo decide el negocio desde la opcion "Asignar Pantallas a
-- Rol", no una migracion. Meterla a un rol desde aca seria darle acceso a
-- gente sin que nadie lo haya pedido.
--
-- Mientras el filtro de seguridad siga en modo LOG esto no cambia quien entra;
-- deja el catalogo listo para cuando se prenda.
--
-- ES IDEMPOTENTE: url_html es UNIQUE y el INSERT va con IGNORE.
-- ---------------------------------------------------------------------------

INSERT IGNORE INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
VALUES ('Desempeno de Domiciliarios', 3, 'DesempenoDomiciliario.html', 110, 'S');

SELECT idpantalla, nombre, idmodulo, url_html, orden, activo
  FROM pizzaamericana.pantalla
 WHERE url_html = 'DesempenoDomiciliario.html';
