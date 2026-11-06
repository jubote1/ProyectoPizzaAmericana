-- ============================================================================
-- DESCUENTOS DE LA PLATAFORMA RAPPI QUE EL REPORTE SEMANAL NO VEIA
-- 2026-11-06
--
-- POR QUE
--
-- Reconstruido renglon por renglon el reporte financiero de Rappi de las cuatro
-- semanas de octubre de 2026, la cuenta da $78.069.820 contra los $78.069.822
-- que Rappi consigno: cuadra al peso. De esos renglones, el reporte semanal no
-- descontaba dos:
--
--   Compensaciones ......................... $  958.290
--   Costo Canceladas ....................... $1.483.658
--                                            ----------
--                                            $2.441.948
--
-- Ninguno de los dos se puede saber con exactitud antes de que llegue el
-- reporte de Rappi, porque son decisiones de ellos. Por eso van como
-- porcentajes, igual que los de Cargo, y se recalibran con la conciliacion de
-- los miercoles.
--
-- Idempotente: se puede correr varias veces.
-- ============================================================================

USE general;

-- ----------------------------------------------------------------------------
-- 1. PORCENTAJECANCELADASRAPPI: 82
--
--    QUE ES. Un pedido de la plataforma de Rappi que no se entrego. Rappi nos
--    cobra parte de el: es la columna "Costo Canceladas" de su reporte. No
--    tiene nada que ver con los domicilios de Rappi Cargo cancelados, que son
--    otro renglon y otro parametro (PORCENTAJECANCELACIONCARGO).
--
--    DE DONDE SALE EL 82. En octubre nuestra base tenia 34 pedidos de Rappi
--    cancelados por $1.799.650 y Rappi cobro $1.483.658, el 82,4%.
--
--    OJO: Rappi cobro 46 cancelaciones y nosotros solo tenemos 34 marcadas. Hay
--    doce que nuestra base no sabe que se cancelaron, asi que este porcentaje
--    tambien esta tapando ese hueco. Si algun dia cuadran los conteos hay que
--    bajarlo.
-- ----------------------------------------------------------------------------
INSERT INTO parametros (valorparametro, valornumerico, valortexto, valornumericod)
SELECT 'PORCENTAJECANCELADASRAPPI', NULL, NULL, 82
WHERE NOT EXISTS (SELECT 1 FROM parametros WHERE valorparametro = 'PORCENTAJECANCELADASRAPPI');

-- ----------------------------------------------------------------------------
-- 2. PORCENTAJECOMPENSACIONESRAPPI: 0.78
--
--    QUE ES. Cuando un cliente se queja -pizza fria, producto faltante,
--    producto cambiado- Rappi le devuelve la plata al cliente y nos la
--    descuenta a nosotros de la consignacion. Son las mismas que salen en el
--    correo semanal de "Quejas de clientes en Rappi".
--
--    ESTE SI ES UN PROMEDIO PURO. No hay forma de saber desde nuestra base
--    cuanto va a compensar Rappi: es una decision de ellos, caso por caso, y
--    nos enteramos cuando llega el reporte. Se estima como un porcentaje de la
--    venta bruta: $958.290 sobre $122.563.000 de octubre = 0,78%.
--
--    Si se quiere apagar la estimacion, poner 0: el reporte deja de restarlo y
--    la consignacion vuelve a salir alta por ese renglon.
-- ----------------------------------------------------------------------------
INSERT INTO parametros (valorparametro, valornumerico, valortexto, valornumericod)
SELECT 'PORCENTAJECOMPENSACIONESRAPPI', NULL, NULL, 0.78
WHERE NOT EXISTS (SELECT 1 FROM parametros WHERE valorparametro = 'PORCENTAJECOMPENSACIONESRAPPI');

-- ----------------------------------------------------------------------------
-- 3. Dos columnas mas en la bitacora de la consignacion
-- ----------------------------------------------------------------------------
SET @hay := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = 'general' AND table_name = 'consignacion_semanal_estimada'
               AND column_name = 'canceladas_plataforma');
SET @sql := IF(@hay = 0,
  'ALTER TABLE consignacion_semanal_estimada
     ADD COLUMN canceladas_plataforma DOUBLE NOT NULL DEFAULT 0 AFTER descuentos_rappi,
     ADD COLUMN compensaciones        DOUBLE NOT NULL DEFAULT 0 AFTER canceladas_plataforma',
  'SELECT ''las columnas ya existian'' AS aviso');
PREPARE p FROM @sql; EXECUTE p; DEALLOCATE PREPARE p;

-- ============================================================================
-- COMPROBACION
-- ============================================================================
SELECT valorparametro, valornumericod
FROM parametros
WHERE valorparametro IN ('PORCENTAJECANCELADASRAPPI','PORCENTAJECOMPENSACIONESRAPPI')
ORDER BY valorparametro;

SELECT column_name FROM information_schema.columns
WHERE table_schema = 'general' AND table_name = 'consignacion_semanal_estimada'
  AND column_name IN ('canceladas_plataforma','compensaciones');
