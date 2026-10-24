-- Bold entra a la comision virtual, y la tarifa del datafono queda en 5%.
--
-- Idempotente: se puede correr varias veces.
--
-- ---------------------------------------------------------------------------
-- 1. BOLD SE LEE DE LA TIENDA, NO DEL LOG DEL WEBHOOK
--
-- Bold es la sexta comision virtual y la unica que no pasa por gasto_semanal:
-- ningun proceso de conciliacion la calcula.
--
-- La primera idea fue sumar pizzaamericana.log_evento_bold, que es lo que el
-- webhook recibio. No sirve: ese log es lo que Bold AVISO, no lo que la tienda
-- registro como pago. Medido en San Antonio, del 1 al 18 de octubre, el log da
-- 7.434.900 y la tienda 7.834.190. Manda la tienda, que es donde se cuadra la
-- caja. (El log ademas trae notificaciones repetidas: 68 pagos de 2.299
-- llegaron dos veces con el mismo monto.)
--
-- Entonces se lee de datamart.pedido_forma_pago, con la forma de pago 7 del
-- catalogo de la TIENDA, que es SONO QR. Esa tabla existia desde hace rato y
-- estaba en CERO filas; la empieza a llenar la replica diaria de Servicios.
-- Mientras no haya corrido, el concepto sale vacio y la pantalla lo dice.
--
-- OJO: el catalogo de formas de pago de la tienda NO es el del central. En la
-- tienda el 7 es SONO QR; en el central el 7 es Rappitendero, inactivo. Lo que
-- se replica y lo que se lee aqui son los ids de la TIENDA.
-- ---------------------------------------------------------------------------

-- Por si quedo de un intento anterior: la fuente ahora es la forma de pago.
DROP TABLE IF EXISTS pizzaamericana.concepto_costo_fuente;

CREATE TABLE IF NOT EXISTS pizzaamericana.concepto_costo_forma_pago (
  idconcepto       INT         NOT NULL,
  -- Del catalogo de la TIENDA, no del central.
  idforma_pago     INT         NOT NULL,
  -- El nombre del parametro de general.parametros que tiene la tarifa, en
  -- porcentaje. Asi la tarifa vive en un solo sitio y se cambia sin recompilar.
  parametro_tarifa VARCHAR(40) NOT NULL,
  PRIMARY KEY (idconcepto, idforma_pago)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bold (SONO QR) entra a COMISION VIRTUAL.
INSERT INTO pizzaamericana.concepto_costo_forma_pago (idconcepto, idforma_pago, parametro_tarifa)
     VALUES (1, 7, 'COMISIONBOLD')
ON DUPLICATE KEY UPDATE parametro_tarifa = VALUES(parametro_tarifa);

-- ---------------------------------------------------------------------------
-- 2. LA TARIFA DE BOLD
--
-- Es la misma que usa ReporteConsignacionBold: 1,5 con el IVA ya incluido. El
-- parametro no existia y el reporte venia corriendo con el valor quemado en el
-- codigo. Se crea para que la tarifa viva en un solo sitio.
-- ---------------------------------------------------------------------------
INSERT INTO general.parametros (valorparametro, valornumericod)
     VALUES ('COMISIONBOLD', 1.5)
ON DUPLICATE KEY UPDATE valorparametro = valorparametro;

-- ---------------------------------------------------------------------------
-- 3. LA COMISION DEL DATAFONO QUEDA EN 5%
--
-- Decidido. Se deja donde ya estaba, gasto_configuracion.porcentaje_gasto del
-- concepto 24, para que no haya dos sitios que puedan decir cosas distintas.
--
-- Queda constancia de lo que no cuadra, que es otro tema y no este:
-- ReporteConsignacionDavivienda y la parte de datafonos de
-- ReporteConsignacionWompi descuentan 1,9% mas IVA -2,261%- quemado en el
-- codigo, para las dos adquirencias. Menos de la mitad. Mientras eso siga asi,
-- el costo de la tienda y la consignacion proyectada no van a cuadrar, y la
-- diferencia no es un error de calculo sino dos tarifas distintas.
-- ---------------------------------------------------------------------------
UPDATE inventarioamericana.gasto_configuracion
   SET porcentaje_gasto = 5
 WHERE idgasto_conf = 24 AND porcentaje_gasto <> 5;

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================
SELECT 'tarifa Bold' AS que, CAST(valornumericod AS CHAR) AS valor
  FROM general.parametros WHERE valorparametro = 'COMISIONBOLD'
 UNION ALL
SELECT 'tarifa datafono', CAST(porcentaje_gasto AS CHAR)
  FROM inventarioamericana.gasto_configuracion WHERE idgasto_conf = 24
 UNION ALL
SELECT 'filas replicadas de forma de pago', CAST(COUNT(*) AS CHAR)
  FROM datamart.pedido_forma_pago;
