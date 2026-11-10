-- BAJA DE PUBLICIDAD - Giovanni Sanclemente, TIENDA PILARICA (9)
-- Companero de 2026_11_10_01. Este corre contra la base LOCAL de Pilarica
-- (192.168.9.39), no contra el central.
--
-- En la tienda la tabla cliente NO tiene columna envio_publicidad: la unica
-- bandera es politica_datos, y en este sistema esa bandera ES el permiso de
-- publicidad. Lo dice el texto que se le lee al cliente en caja
-- (autorizacion_datos_texto v2): "Pizza Americana le puede enviar promociones
-- y novedades a su celular o correo? Es voluntario, y puede pedirnos que lo
-- saquemos cuando quiera." Asi que ponerla en 'N' es exactamente lo que pidio.
--
-- Idempotente.

UPDATE tiendaamericana.cliente
   SET politica_datos = 'N',
       observacion    = CONCAT(IFNULL(observacion,''),
                        ' | 2026-11-07 solicito baja de publicidad. NO enviar promociones.')
 WHERE idcliente IN (10658, 18813, 28961)
   AND IFNULL(politica_datos,'S') <> 'N';

-- La revocacion queda ademas en el historico con evidencia. Esa tabla no se
-- actualiza nunca: gana la respuesta mas reciente, y es lo que hace posible
-- revocar y poder probarlo despues.
INSERT INTO tiendaamericana.cliente_autorizacion_datos
       (idcliente, idpersona, respuesta, idversion_texto, usuario, idtienda, creado_en)
SELECT 28961, 56375, 'N', 2, 'solicitud escrita 2026-11-07', 9, now()
 WHERE NOT EXISTS (
       SELECT 1 FROM tiendaamericana.cliente_autorizacion_datos
        WHERE idpersona = 56375 AND respuesta = 'N');
