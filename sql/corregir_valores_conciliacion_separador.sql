-- ---------------------------------------------------------------------------
-- LAS SEIS SOLICITUDES QUE QUEDARON MIL VECES MAS PEQUENAS
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana.
--
-- ESTA PENDIENTE DE DECISION. No correr sin que el negocio lo confirme: esto
-- cambia cifras de dinero de solicitudes abiertas.
--
-- QUE PASO
--
-- InsertarSolicitudConciliacion leia el valor con un Double.parseDouble suelto.
-- Al escribir "135.250" -con el punto de los miles, como se escribe aca- Java
-- lo entendio como ciento treinta y cinco con veinticinco y guardo 135,25.
-- Mil veces menos, sin un solo error en pantalla.
--
-- Las seis filas siguen todas en estado PENDIENTE, asi que nadie ha cerrado
-- una diferencia con la cifra mala. Por eso se pueden corregir.
--
--   id     guardado    deberia ser   tienda   fecha
--   706      172,90      172.900       13     2026-01-22
--   1030      76,99       76.990       10     2026-07-19
--   1031      72,90       72.900        2     2026-07-26
--   1032     230,15      230.150        2     2026-07-27
--   1033     135,25      135.250        1     2026-07-28
--   1034     457,50      457.500        9     2026-07-28
--
-- POR QUE POR MIL Y NO OTRA COSA
--
-- Porque el dano es exactamente ese: el punto de los miles se leyo como punto
-- decimal, asi que el numero quedo dividido por mil. No es una estimacion.
--
-- POR QUE VA CON EL VALOR VIEJO EN EL WHERE
--
-- Para que correrlo dos veces no multiplique dos veces. Si alguien ya lo
-- corrigio a mano, la fila no coincide y no se toca.
--
-- El origen del problema ya esta corregido en el codigo: la lectura del valor
-- vive ahora en utilidadesCC.ValorDigitado y rechaza lo que no entiende en vez
-- de guardar un numero cualquiera.
-- ---------------------------------------------------------------------------

START TRANSACTION;

UPDATE pizzaamericana.solicitud_conciliacion SET valor_analizar = 172900
 WHERE idsolicitud = 706  AND valor_analizar = 172.9   AND estado = 'PENDIENTE';

UPDATE pizzaamericana.solicitud_conciliacion SET valor_analizar = 76990
 WHERE idsolicitud = 1030 AND valor_analizar = 76.99   AND estado = 'PENDIENTE';

UPDATE pizzaamericana.solicitud_conciliacion SET valor_analizar = 72900
 WHERE idsolicitud = 1031 AND valor_analizar = 72.9    AND estado = 'PENDIENTE';

UPDATE pizzaamericana.solicitud_conciliacion SET valor_analizar = 230150
 WHERE idsolicitud = 1032 AND valor_analizar = 230.15  AND estado = 'PENDIENTE';

UPDATE pizzaamericana.solicitud_conciliacion SET valor_analizar = 135250
 WHERE idsolicitud = 1033 AND valor_analizar = 135.25  AND estado = 'PENDIENTE';

UPDATE pizzaamericana.solicitud_conciliacion SET valor_analizar = 457500
 WHERE idsolicitud = 1034 AND valor_analizar = 457.5   AND estado = 'PENDIENTE';

COMMIT;

-- Como quedo, y si sigue habiendo alguna sospechosa
SELECT idsolicitud, fecha, idtienda, origen, valor_analizar, estado
  FROM pizzaamericana.solicitud_conciliacion
 WHERE idsolicitud IN (706, 1030, 1031, 1032, 1033, 1034)
 ORDER BY idsolicitud;

SELECT COUNT(*) AS quedan_sospechosas
  FROM pizzaamericana.solicitud_conciliacion
 WHERE valor_analizar > 0 AND valor_analizar < 1000;
