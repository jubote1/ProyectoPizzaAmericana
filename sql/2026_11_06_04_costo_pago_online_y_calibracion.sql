-- ============================================================================
-- COSTO DE PAGOS EN LINEA COMO PARAMETRO, Y CORRECCION DE PORCENTAJECANCELADASRAPPI
-- 2026-11-06
-- ============================================================================

USE general;

-- ----------------------------------------------------------------------------
-- 1. PORCENTAJECOSTOPAGOONLINE (nuevo): 6
--
--    Estaba QUEMADO en ReporteSemanalRappi como
--    PORCENTAJE_COSTO_PAGO_ONLINE = 6. Se saca a parametro SIN cambiarle el
--    valor: mover el numero es una decision de negocio, no de este cambio.
--
--    PERO CONVIENE SABER ESTO ANTES DE DEJARLO EN 6. Reconstruido el reporte
--    financiero de Rappi de octubre de 2026 renglon por renglon, NO HAY NINGUN
--    RENGLON que corresponda a este 6%. Lo mas parecido es el "Gasto bancario"
--    (columna BQ) mas su IVA, que en las cuatro semanas valio $1.907.330 sobre
--    una venta bruta de $122.563.000: el 1,6%.
--
--    Con el 6% el reporte descuenta unos $6,8 millones al mes por algo que
--    cuesta $1,9. Ese error empuja la consignacion estimada hacia ABAJO y se
--    compensa en parte con otros renglones que no se descuentan, que es por lo
--    que el total cuadra al 2%. Si alguna vez se corrigen los otros, hay que
--    bajar este.
-- ----------------------------------------------------------------------------
INSERT INTO parametros (valorparametro, valornumerico, valortexto, valornumericod)
SELECT 'PORCENTAJECOSTOPAGOONLINE', NULL, NULL, 6
WHERE NOT EXISTS (SELECT 1 FROM parametros WHERE valorparametro = 'PORCENTAJECOSTOPAGOONLINE');

-- ----------------------------------------------------------------------------
-- 2. PORCENTAJECANCELADASRAPPI: de 82 a 65
--
--    El 82 estaba mal calibrado: salio de dividir lo que Rappi cobro por las
--    46 cancelaciones ($1.483.658) entre el total_neto de las 34 que nuestra
--    base tiene marcadas. Denominador equivocado.
--
--    Cruzando orden por orden con el id de Rappi (marcacion_pedido.observacion),
--    de las 46 que Rappi cobro hay 32 que si podemos cruzar. Sobre el
--    total_neto de ESAS 32 ($1.654.500), Rappi descuento $1.075.425: el 65,0%.
--
--    Las otras 14 ($408.232) NUNCA entraron al central, asi que el parametro no
--    las puede cubrir y no debe intentar taparlas: eso es un problema de
--    integracion que hay que arreglar, no inflar un porcentaje. Quedan como
--    parte del 2% de error que el reporte no puede predecir.
--
--    Como lee el descuento: Rappi suma la venta completa del pedido cancelado
--    en su "Venta Bruta" y despues descuenta el 65%, o sea que nos paga el 35%
--    restante. No es una perdida total, es un pago parcial.
-- ----------------------------------------------------------------------------
UPDATE parametros SET valornumericod = 65
WHERE valorparametro = 'PORCENTAJECANCELADASRAPPI';

-- ============================================================================
-- COMPROBACION
-- ============================================================================
SELECT valorparametro, valornumericod FROM parametros
WHERE valorparametro IN ('PORCENTAJECOSTOPAGOONLINE','PORCENTAJECANCELADASRAPPI',
                         'PORCENTAJECOMPENSACIONESRAPPI','VALORDOMICILIOCARGO',
                         'PORCENTAJEEFECTIVOCARGO','PORCENTAJEIVACARGO',
                         'PORCENTAJECANCELACIONCARGO')
ORDER BY valorparametro;
