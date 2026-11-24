-- =============================================================================
-- MIGRACIÓN: Registro de Pantalla TomaPedidosPOS y Vistas de Compatibilidad Full
-- Fecha: 2026-11-24
-- Autor: Equipo Antigravity / Juan David
-- Propósito:
--   1. Actualizar las vistas de compatibilidad v_pedido_compatibilidad y 
--      v_detalle_pedido_compatibilidad con el 100% de columnas del esquema legado,
--      permitiendo que reportes y DAOs analíticos lean indistintamente de pos_central.
--   2. Registrar la nueva interfaz TomaPedidosPOS.html en la tabla seguridad.pantalla
--      bajo el módulo 'Toma de pedidos' (orden 15, justo después de Pedidos.html).
--   3. Asignar los permisos de acceso en rol_pantalla a los roles operativos y admin.
-- =============================================================================

USE pizzaamericana;

-- -----------------------------------------------------------------------------
-- 1. Vista de Compatibilidad Cabecera de Pedido (v_pedido_compatibilidad)
--    Mapea pos_central.pedido hacia el esquema legacy pizzaamericana.pedido
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW pizzaamericana.v_pedido_compatibilidad AS
SELECT
  pos.idpedidocontact AS idpedido,
  pos.idtienda,
  pos.total_bruto,
  pos.impuesto,
  pos.total_neto,
  IFNULL(pdesc.descuentopesos, 0.00) AS descuento,
  pos.idcliente,
  pos.idestado AS idestadopedido,
  pos.fechapedido,
  1 AS enviadopixel,
  pos.idpedidotienda AS numposheader,
  pos.fechainsercion AS fechaenviotienda,
  'C' AS origen,
  '' AS stringpixel,
  pos.fechainsercion,
  pos.usuariopedido,
  '' AS usuarioreenvio,
  CAST(pos.tiempopedido AS DOUBLE) AS tiempopedido,
  '' AS idlink,
  NULL AS fechapagovirtual,
  '' AS tipopago,
  NULL AS fechafinalizacion,
  0 AS idordencomercio,
  pos.programado,
  pos.hora_programado,
  pos.idtipopedido,
  'CENTRAL' AS fuentepedido,
  0 AS aceptado_rappi,
  0 AS venta_corporativa,
  IF(pos.idestado = 5, 1, 0) AS cancelado,
  NULL AS fecha_cancelacion,
  0 AS domiciliario,
  NULL AS fecha_domiciliario,
  IF(pos.idestado = 4, 1, 0) AS entregado,
  NULL AS fecha_entregado,
  pos.domicilio_tercerizado,
  pos.empresa_tercerizada,
  NULL AS envio_tienda_en_curso,
  NULL AS retenido_motivo,
  NULL AS retenido_hasta,
  NULL AS retenido_por,
  1 AS enviadopixel_antes,
  pos.observacion
FROM pos_central.pedido pos
LEFT JOIN pos_central.pedido_descuento pdesc ON pdesc.idpedido = pos.idpedidotienda;

-- -----------------------------------------------------------------------------
-- 2. Vista de Compatibilidad Detalle de Pedido (v_detalle_pedido_compatibilidad)
--    Mapea pos_central.detalle_pedido hacia el esquema legacy pizzaamericana.detalle_pedido
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW pizzaamericana.v_detalle_pedido_compatibilidad AS
SELECT
  dp.iddetalle_pedido,
  p.idpedidocontact AS idpedido,
  dp.idproducto,
  dp.cantidad,
  dp.valorunitario AS valorUnitario,
  dp.valortotal AS valorTotal,
  0 AS idespecialidad1,
  0 AS idespecialidad2,
  '' AS adicion,
  '' AS modespecialidad1,
  '' AS modespecialidad2,
  dp.observacion,
  0 AS idsabortipoliquido,
  0 AS idexcepcion,
  dp.iddetalle_pedido_master
FROM pos_central.detalle_pedido dp
JOIN pos_central.pedido p ON p.idpedidotienda = dp.idpedidotienda AND p.idtienda = dp.idtienda;

-- -----------------------------------------------------------------------------
-- 3. Registro de Pantalla en la tabla seguridad.pantalla
--    Ubicación: Módulo 'Toma de pedidos' (idmodulo 13)
-- -----------------------------------------------------------------------------
INSERT INTO pantalla (nombre, url_html, idmodulo, orden, activo)
SELECT 'Nuevo Pedido (Nuevo Motor POS)', 'TomaPedidosPOS.html', 
       (SELECT idmodulo FROM menu_modulo WHERE nombre = 'Toma de pedidos' LIMIT 1),
       15, 'S'
WHERE NOT EXISTS (SELECT 1 FROM pantalla WHERE url_html = 'TomaPedidosPOS.html');

-- Si ya existiera, actualizar orden y nombre
UPDATE pantalla 
SET nombre = 'Nuevo Pedido (Nuevo Motor POS)', 
    idmodulo = (SELECT idmodulo FROM menu_modulo WHERE nombre = 'Toma de pedidos' LIMIT 1),
    orden = 15,
    activo = 'S'
WHERE url_html = 'TomaPedidosPOS.html';

-- -----------------------------------------------------------------------------
-- 4. Asignación de Roles para TomaPedidosPOS.html
--    Roles: Operario, Líder Administrativo, Superadministrador, Administrador, PQRS
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM rol r
CROSS JOIN pantalla p
WHERE p.url_html = 'TomaPedidosPOS.html'
  AND r.nombre IN ('Operario', 'Líder Administrativo', 'Superadministrador', 'Administrador', 'Administrador PQRS/Contact Center');
