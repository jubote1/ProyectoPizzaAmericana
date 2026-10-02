-- ---------------------------------------------------------------------------
-- Los destinatarios del reporte nuevo de promociones
--
-- Se corre en el CENTRAL (172.19.0.25), base general. Idempotente.
-- Requiere promociones_reporte_catalogo.sql.
--
-- El reporte nuevo manda a REPORTEPROMOCIONESDIA, con la misma gente que hoy
-- recibe PROMODIARIA. Son dos parametros distintos a proposito: mientras los
-- dos reportes corren en paralelo, se puede sacar a alguien del nuevo sin
-- tocar el viejo, o al reves.
--
-- Cuando se jubile el viejo, basta con dejar de programar su jar; esta lista
-- ya queda armada.
-- ---------------------------------------------------------------------------

INSERT INTO general.parametros_correo (valorparametro, correo)
SELECT 'REPORTEPROMOCIONESDIA', v.correo
  FROM general.parametros_correo v
 WHERE v.valorparametro = 'PROMODIARIA'
   AND NOT EXISTS (SELECT 1 FROM general.parametros_correo ya
                    WHERE ya.valorparametro = 'REPORTEPROMOCIONESDIA'
                      AND ya.correo = v.correo);

-- ===========================================================================
-- COMO QUEDO
--
-- Las dos listas tienen que tener la misma gente.
-- ===========================================================================

SELECT valorparametro, COUNT(*) AS destinatarios,
       GROUP_CONCAT(correo ORDER BY correo SEPARATOR '  ') AS lista
  FROM general.parametros_correo
 WHERE valorparametro IN ('PROMODIARIA', 'REPORTEPROMOCIONESDIA')
 GROUP BY valorparametro;
