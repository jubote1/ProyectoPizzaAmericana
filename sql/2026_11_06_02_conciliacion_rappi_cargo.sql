-- ============================================================================
-- CONCILIACION DE RAPPI CARGO: parametros y bitacora de la consignacion
-- 2026-11-06
--
-- POR QUE
--
-- El bloque de Rappi Cargo de ReporteSemanalRappi / ReporteSemanalPagoRappi
-- estimaba el costo con una tarifa PLANA de $8.500 por domicilio. Medido del
-- 3 al 30 de octubre de 2026 contra el portal de Cargo (289 ordenes):
--
--   lo que el proceso descontaba .............. $2.519.919
--   lo que Cargo cobro de verdad .............. $2.904.519
--   subestimacion ............................. $  384.600  (13,2%)
--
-- La tarifa plana no ve la variabilidad real: solo el 24% de los pedidos
-- cuesta la tarifa base. El 21% esta entre $10.000 y $12.000, el 11% entre
-- $12.000 y $15.000, y hubo 7 pedidos de mas de $15.000.
--
-- A partir de ahora el reporte toma la tarifa REAL de cada pedido, que ya
-- viene en el webhook de Cargo y se guarda en
-- tercerizado_domicilio_evento.json_evento -> $.estimated_price. No hizo falta
-- tocar el webhook ni crear tabla de tarifas: el dato estaba ahi desde
-- siempre y sirve tambien para el historico.
--
-- Estos parametros quedan para lo que el webhook NO sabe.
--
-- Idempotente: se puede correr varias veces.
-- ============================================================================

USE general;

-- ----------------------------------------------------------------------------
-- 1. VALORDOMICILIOCARGO: de 8.500 a 10.000
--
--    Ya NO es la tarifa de todos los pedidos: ahora es el RESPALDO para el
--    pedido al que no se le encuentre tarifa real en el webhook. Por eso sube
--    al costo real promedio por orden medido en octubre (tarifa con IVA menos
--    la comision de efectivo, sobre las ordenes que el proceso cuenta: $10.009)
--    y no al promedio simple.
-- ----------------------------------------------------------------------------
UPDATE parametros SET valornumericod = 10000
WHERE valorparametro = 'VALORDOMICILIOCARGO';

-- ----------------------------------------------------------------------------
-- 2. PORCENTAJEEFECTIVOCARGO: de 2,8 a 2,85
--
--    El contrato (Formulario de Pedido Acceso Plataforma Cargo, 22-07-2026)
--    dice 2,85% + IVA. El 2,8 era un redondeo nuestro. Comprobado contra el
--    portal en octubre: sobre $7.728.990 recaudados en 154 pedidos, Cargo
--    cobro $220.276, que es 2,850% exacto.
-- ----------------------------------------------------------------------------
UPDATE parametros SET valornumericod = 2.85
WHERE valorparametro = 'PORCENTAJEEFECTIVOCARGO';

-- ----------------------------------------------------------------------------
-- 3. PORCENTAJEIVACARGO (nuevo): 10,72
--
--    OJO: NO ES 19. Cargo no grava toda la tarifa: 41 de las 289 ordenes de
--    octubre salieron con IVA en CERO, y en el resto el IVA va en proporcion a
--    la parte del servicio que si es gravada. El IVA efectivo medido sobre el
--    total del mes fue $281.116 / $2.623.403 = 10,72%.
--
--    Es un promedio, no una formula exacta: la unica manera de saber el IVA
--    exacto de un pedido es el reporte del portal, que llega despues. Conviene
--    recalibrarlo cada cierto tiempo con la conciliacion de los miercoles.
-- ----------------------------------------------------------------------------
INSERT INTO parametros (valorparametro, valornumerico, valortexto, valornumericod)
SELECT 'PORCENTAJEIVACARGO', NULL, NULL, 10.72
WHERE NOT EXISTS (SELECT 1 FROM parametros WHERE valorparametro = 'PORCENTAJEIVACARGO');

-- ----------------------------------------------------------------------------
-- 4. PORCENTAJECANCELACIONCARGO (nuevo): 19
--
--    El proceso NO veia las cancelaciones (la consulta traia
--    "ifnull(cancelado,'0') <> '1'"), pero Rappi SI las cobra: en octubre fueron
--    $47.502 en 11 de 26 cancelaciones. Cobra la tarifa base completa ($8.190)
--    o el 35% de ella ($2.866,50), y no hay forma de saber cual desde el
--    webhook.
--
--    Por eso se usa un porcentaje promedio sobre la tarifa que habria costado
--    el pedido: $47.502 sobre $249.703 de tarifa estimada de las canceladas
--    = 19%. Igual que el anterior, es un promedio para recalibrar.
-- ----------------------------------------------------------------------------
INSERT INTO parametros (valorparametro, valornumerico, valortexto, valornumericod)
SELECT 'PORCENTAJECANCELACIONCARGO', NULL, NULL, 19
WHERE NOT EXISTS (SELECT 1 FROM parametros WHERE valorparametro = 'PORCENTAJECANCELACIONCARGO');

-- ----------------------------------------------------------------------------
-- 5. consignacion_semanal_estimada (nueva)
--
--    La linea "CONSIGNACION APROXIMADA" solo existia dentro del correo semanal.
--    Para saber si lo que Rappi consigno coincidia con lo que el proceso
--    calculo, tocaba ir a buscar el correo; y en gasto_semanal solo queda el
--    concepto de comision, no el total.
--
--    Ahora cada corrida deja aqui su cuenta completa, renglon por renglon, y la
--    conciliacion de los miercoles la compara contra el valor a transferir del
--    portal de Rappi sin depender de ningun correo.
--
--    La llave unica es (proceso, idrazon, fecha_fin): si un reproceso vuelve a
--    correr el mismo periodo, REEMPLAZA la fila en vez de duplicarla.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS consignacion_semanal_estimada (
  idconsignacion        INT          NOT NULL AUTO_INCREMENT,
  proceso               VARCHAR(40)  NOT NULL COMMENT 'SEMANALRAPPI o SEMANALPAGORAPPI',
  idrazon               INT          NOT NULL,
  nombre_razon          VARCHAR(150)     NULL,
  fecha_inicio          DATE         NOT NULL,
  fecha_fin             DATE         NOT NULL,
  recaudo_bruto         DOUBLE       NOT NULL DEFAULT 0,
  comision_rappi        DOUBLE       NOT NULL DEFAULT 0,
  costo_pagos_linea     DOUBLE       NOT NULL DEFAULT 0,
  tarifa_servicio       DOUBLE       NOT NULL DEFAULT 0,
  descuentos_rappi      DOUBLE       NOT NULL DEFAULT 0,
  cargo_pedidos         INT          NOT NULL DEFAULT 0,
  cargo_costo           DOUBLE       NOT NULL DEFAULT 0 COMMENT 'tarifa real + IVA de los entregados',
  cargo_cancelados      INT          NOT NULL DEFAULT 0,
  cargo_costo_cancel    DOUBLE       NOT NULL DEFAULT 0,
  cargo_efectivo        DOUBLE       NOT NULL DEFAULT 0,
  cargo_comision_efect  DOUBLE       NOT NULL DEFAULT 0,
  consignacion_estimada DOUBLE       NOT NULL DEFAULT 0,
  fecha_calculo         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (idconsignacion),
  UNIQUE KEY uk_consig_periodo (proceso, idrazon, fecha_fin),
  KEY ix_consig_fecha (fecha_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Lo que el reporte semanal calculo que Rappi deberia consignar';

-- ============================================================================
-- COMPROBACION
-- ============================================================================
SELECT valorparametro, valornumericod
FROM parametros
WHERE valorparametro IN ('VALORDOMICILIOCARGO','PORCENTAJEEFECTIVOCARGO',
                         'PORCENTAJEIVACARGO','PORCENTAJECANCELACIONCARGO')
ORDER BY valorparametro;

SELECT COUNT(*) AS 'columnas de consignacion_semanal_estimada (deben ser 19)'
FROM information_schema.columns
WHERE table_schema = 'general' AND table_name = 'consignacion_semanal_estimada';
