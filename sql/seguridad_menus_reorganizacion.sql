-- Reorganizacion del menu del central: modulos con submodulos, roles con sus pantallas y un superadministrador.
-- Idempotente: se puede correr dos veces. Base: pizzaamericana del central (172.19.0.25). NO la corre el programa: la corre una persona.
--
-- Que hace:
--  1. Reorganiza los modulos: Pedidos (Toma / Consulta / Correccion), PQRS, Reporteria, Monitoreo, Clientes y CRM,
--     Mercadeo, Personal y Gerencia, Parametrizacion y Seguridad. Mueve cada pantalla a su modulo y agrega las 9 que el
--     menu fijo de administracion mostraba y nunca se habian cargado en la tabla pantalla.
--  2. Le da a los tres roles de siempre (Operario, Lider Administrativo, Administrador PQRS) exactamente las pantallas que
--     hoy muestran Menu.html, MenuAdm.html y MenuPQRS.html, salvo las tres de seguridad (solo superadministrador).
--  3. Crea el rol Superadministrador (todas las pantallas) y se lo asigna a jdbotero.
--  4. A quien hoy no tiene ningun rol le da el que corresponde a su campo administrador (N/S/P), para que nadie pierda su menu.
-- Los roles 4 (Administrador de Tienda) y 5 (Gerencia) no se tocan.
SET NAMES utf8mb4;

-- ---------------------------------------------------------------- 1. modulos
UPDATE menu_modulo SET nombre = 'Pedidos', orden = 10 WHERE idmodulo = 1 AND nombre = 'Funciones';
UPDATE menu_modulo SET nombre = 'Clientes y CRM', orden = 50 WHERE idmodulo = 4 AND nombre = 'CRM';
UPDATE menu_modulo SET nombre = 'Personal y Gerencia', orden = 70 WHERE idmodulo = 5 AND nombre = 'Gerencia';
UPDATE menu_modulo SET nombre = 'Parametrización', orden = 80 WHERE idmodulo = 2 AND nombre IN ('Parametrizacion', 'Parametrización');
UPDATE menu_modulo SET orden = 40 WHERE idmodulo = 3 AND nombre = 'Monitoreo';

INSERT INTO menu_modulo (nombre, orden, idmodulo_padre, activo)
SELECT t.nombre, t.orden, NULL, 'S' FROM (
  SELECT 'PQRS' nombre, 20 orden UNION ALL SELECT 'Reportería', 30 UNION ALL SELECT 'Mercadeo', 60 UNION ALL SELECT 'Seguridad', 90) t
WHERE NOT EXISTS (SELECT 1 FROM menu_modulo m WHERE m.nombre = t.nombre);

INSERT INTO menu_modulo (nombre, orden, idmodulo_padre, activo)
SELECT t.nombre, t.orden, (SELECT idmodulo FROM menu_modulo WHERE nombre = 'Pedidos' LIMIT 1), 'S' FROM (
  SELECT 'Toma de pedidos' nombre, 11 orden UNION ALL SELECT 'Consulta de pedidos', 12 UNION ALL SELECT 'Corrección de pedidos', 13) t
WHERE NOT EXISTS (SELECT 1 FROM menu_modulo m WHERE m.nombre = t.nombre);

-- ---------------------------------------------------------------- 2. pantallas: a que modulo va cada una
DROP TEMPORARY TABLE IF EXISTS tmp_mapa;
CREATE TEMPORARY TABLE tmp_mapa (url_html VARCHAR(150) NOT NULL PRIMARY KEY, modulo VARCHAR(60) NOT NULL, orden INT NOT NULL, nombre_nueva VARCHAR(150) NULL) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
INSERT INTO tmp_mapa (url_html, modulo, orden, nombre_nueva) VALUES
('Pedidos.html', 'Toma de pedidos', 10, NULL),
('PedidosClasico.html', 'Toma de pedidos', 20, NULL),
('RegistroPedidoCumple.html', 'Toma de pedidos', 30, NULL),
('ConsultaPedidos.html', 'Consulta de pedidos', 10, NULL),
('ConsultaPedidosEnCurso.html', 'Consulta de pedidos', 20, NULL),
('ConsultaPedidosNuevaTienda.html', 'Consulta de pedidos', 30, NULL),
('ConsultaPedidosAPP.html', 'Consulta de pedidos', 40, NULL),
('ConsultaPedidosDomicom.html', 'Consulta de pedidos', 50, NULL),
('ConsultaPedidosCRMBOT.html', 'Consulta de pedidos', 60, NULL),
('ConsultaCARGO.html', 'Consulta de pedidos', 70, NULL),
('EstadosPedidosTienda.html', 'Consulta de pedidos', 80, NULL),
('EstadosHistorialPedido.html', 'Consulta de pedidos', 90, NULL),
('ConsultaTiemposPedidos.html', 'Consulta de pedidos', 100, NULL),
('ConsultaDireccionFueraZona.html', 'Consulta de pedidos', 110, NULL),
('ConsultaPedidosMapaEvolutivo.html', 'Consulta de pedidos', 120, NULL),
('mapa.jsp', 'Consulta de pedidos', 130, NULL),
('ConsultarRegistroPedidoCumple.html', 'Consulta de pedidos', 140, NULL),
('ModificarGeneralPedido.html', 'Corrección de pedidos', 10, NULL),
('ValidarCodigo.html', 'Corrección de pedidos', 20, NULL),
('PQRS.html', 'PQRS', 10, NULL),
('ConsultaPQRS.html', 'PQRS', 20, NULL),
('ModificarPQRS.html', 'PQRS', 30, NULL),
('VentaIntegral.html', 'Reportería', 10, NULL),
('VentaIntegralCategorias.html', 'Reportería', 20, NULL),
('PromocionesReporte.html', 'Reportería', 30, NULL),
('ConsultaVentaEmpresarial.html', 'Reportería', 40, NULL),
('ConsultaVentasAsesor.html', 'Reportería', 50, NULL),
('DesempenoDomiciliario.html', 'Reportería', 60, NULL),
('FacturacionElectronica.html', 'Reportería', 70, NULL),
('ConsultaSolicitudFacturaElectronica.html', 'Reportería', 80, NULL),
('ResultadoEncuesta.html', 'Reportería', 90, NULL),
('ResultadoPreguntasEmpleado.html', 'Reportería', 100, NULL),
('ReporteEnrutamiento.html', 'Reportería', 110, 'Reporte Enrutamiento'),
('MapaCalorDomicilios.html', 'Reportería', 120, 'Mapa de Calor de Domicilios'),
('DashboardCargo.html', 'Reportería', 130, 'Dashboard Cargo'),
('PorcentajeTienda.html', 'Reportería', 140, 'Costo por Tienda'),
('MonitoreoPlataformas.html', 'Monitoreo', 10, NULL),
('MonitoreoPagosVirtuales.html', 'Monitoreo', 20, NULL),
('CancelacionesPlataformas.html', 'Monitoreo', 30, NULL),
('ConciliacionQRDATAFONO.html', 'Monitoreo', 40, NULL),
('ConsultarConciliacionQRDATAFONO.html', 'Monitoreo', 50, NULL),
('ConsultarEstadoDatafonos.html', 'Monitoreo', 60, NULL),
('CRM.html', 'Clientes y CRM', 10, NULL),
('Persona360.html', 'Clientes y CRM', 20, NULL),
('CRMClientesFull.html', 'Clientes y CRM', 30, NULL),
('CRMClientes.html', 'Clientes y CRM', 40, NULL),
('ConsultaPuntosCliente.html', 'Clientes y CRM', 50, NULL),
('TableroSegmentos.html', 'Clientes y CRM', 60, NULL),
('SegmentacionPersona.html', 'Clientes y CRM', 70, NULL),
('DefinicionSegmentos.html', 'Clientes y CRM', 80, NULL),
('segmentacionCliente.html', 'Clientes y CRM', 90, NULL),
('RedencionManualPuntos.html', 'Clientes y CRM', 100, NULL),
('RevisionReversaRedencion.html', 'Clientes y CRM', 110, NULL),
('CompensacionesTiempo.html', 'Clientes y CRM', 120, NULL),
('MarcasCliente.html', 'Clientes y CRM', 130, NULL),
('EnvioPublicidad.html', 'Mercadeo', 10, NULL),
('Oferta.html', 'Mercadeo', 20, NULL),
('TableroOfertas.html', 'Mercadeo', 30, 'Tablero de Ofertas y Códigos'),
('CampanaPromo.html', 'Mercadeo', 40, 'Campañas de Promoción'),
('Campana15MinConfig.html', 'Mercadeo', 50, 'Campaña 15 Minutos o Gratis'),
('RevisionIncumplimiento15Min.html', 'Mercadeo', 60, 'Revisión 15 Minutos o Gratis'),
('ReportesMercadeo.html', 'Mercadeo', 70, 'Reportes de Mercadeo'),
('ConsultaEncuestaMercadeo.html', 'Mercadeo', 80, NULL),
('DispersionPremiosRuleta.html', 'Mercadeo', 90, NULL),
('CalendarioSemanas.html', 'Personal y Gerencia', 10, NULL),
('GastosTienda.html', 'Personal y Gerencia', 20, NULL),
('EstructuraGerencia.html', 'Personal y Gerencia', 30, NULL),
('NominaEmpleado.html', 'Personal y Gerencia', 40, NULL),
('NominaCalculo.html', 'Personal y Gerencia', 50, NULL),
('Rentabilidad.html', 'Personal y Gerencia', 60, NULL),
('CierreGerencia.html', 'Personal y Gerencia', 70, NULL),
('NovedadesBiometria.html', 'Personal y Gerencia', 80, NULL),
('Producto.html', 'Parametrización', 10, NULL),
('Especialidad.html', 'Parametrización', 20, NULL),
('ExcepcionPrecio.html', 'Parametrización', 30, NULL),
('SaborTipoLiquido.html', 'Parametrización', 40, NULL),
('TipoLiquido.html', 'Parametrización', 50, NULL),
('EstadoPedido.html', 'Parametrización', 60, NULL),
('Tienda.html', 'Parametrización', 70, NULL),
('TiendaBloqueada.html', 'Parametrización', 80, NULL),
('MarcarProductoNoExistente.html', 'Parametrización', 90, NULL),
('AsignarTiemposPedidos.html', 'Parametrización', 100, NULL),
('Rol.html', 'Seguridad', 10, NULL),
('AsignarPantallasRol.html', 'Seguridad', 20, NULL),
('AsignarRolUsuario.html', 'Seguridad', 30, NULL);

-- las 9 pantallas que el menu fijo mostraba y la tabla pantalla no tenia
INSERT INTO pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT t.nombre_nueva, m.idmodulo, t.url_html, t.orden, 'S'
FROM tmp_mapa t JOIN menu_modulo m ON m.nombre = t.modulo
WHERE t.nombre_nueva IS NOT NULL AND NOT EXISTS (SELECT 1 FROM pantalla p WHERE p.url_html = t.url_html);

-- el resto se mueve a su modulo y toma su orden
UPDATE pantalla p JOIN tmp_mapa t ON t.url_html = p.url_html JOIN menu_modulo m ON m.nombre = t.modulo
SET p.idmodulo = m.idmodulo, p.orden = t.orden;

-- ---------------------------------------------------------------- 3. superadministrador
INSERT INTO rol (nombre, descripcion, activo)
SELECT 'Superadministrador', 'Administra la seguridad (roles, pantallas por rol y rol por usuario) y ve todas las pantallas.', 'S'
WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre = 'Superadministrador');

INSERT IGNORE INTO rol_pantalla (idrol, idpantalla)
SELECT (SELECT idrol FROM rol WHERE nombre = 'Superadministrador'), p.idpantalla FROM pantalla p WHERE p.activo = 'S';

INSERT IGNORE INTO usuario_rol (idusuario, idrol)
SELECT u.id, (SELECT idrol FROM rol WHERE nombre = 'Superadministrador') FROM usuario u WHERE u.nombre = 'jdbotero';

-- ---------------------------------------------------------------- 4. los tres roles de siempre: lo que hoy muestra cada menu fijo
DROP TEMPORARY TABLE IF EXISTS tmp_menu_fijo;
CREATE TEMPORARY TABLE tmp_menu_fijo (menu VARCHAR(10) NOT NULL, url_html VARCHAR(150) NOT NULL) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
INSERT INTO tmp_menu_fijo (menu, url_html) VALUES
('N', 'CRM.html'),
('N', 'CRMClientes.html'),
('N', 'CRMClientesFull.html'),
('N', 'CancelacionesPlataformas.html'),
('N', 'ConsultaPQRS.html'),
('N', 'ConsultaPedidos.html'),
('N', 'ConsultaPedidosAPP.html'),
('N', 'ConsultaPedidosCRMBOT.html'),
('N', 'ConsultaPedidosDomicom.html'),
('N', 'ConsultaPedidosEnCurso.html'),
('N', 'ConsultaPedidosNuevaTienda.html'),
('N', 'ConsultaPuntosCliente.html'),
('N', 'ConsultaSolicitudFacturaElectronica.html'),
('N', 'ConsultaTiemposPedidos.html'),
('N', 'ConsultaVentaEmpresarial.html'),
('N', 'ConsultaVentasAsesor.html'),
('N', 'ConsultarEstadoDatafonos.html'),
('N', 'EstadosHistorialPedido.html'),
('N', 'EstadosPedidosTienda.html'),
('N', 'ModificarGeneralPedido.html'),
('N', 'ModificarPQRS.html'),
('N', 'MonitoreoPagosVirtuales.html'),
('N', 'MonitoreoPlataformas.html'),
('N', 'PQRS.html'),
('N', 'Pedidos.html'),
('N', 'ValidarCodigo.html');
INSERT INTO tmp_menu_fijo (menu, url_html) VALUES
('S', 'AsignarPantallasRol.html'),
('S', 'AsignarRolUsuario.html'),
('S', 'AsignarTiemposPedidos.html'),
('S', 'CRM.html'),
('S', 'CRMClientes.html'),
('S', 'CRMClientesFull.html'),
('S', 'Campana15MinConfig.html'),
('S', 'CampanaPromo.html'),
('S', 'CancelacionesPlataformas.html'),
('S', 'ConciliacionQRDATAFONO.html'),
('S', 'ConsultaCARGO.html'),
('S', 'ConsultaDireccionFueraZona.html'),
('S', 'ConsultaPQRS.html'),
('S', 'ConsultaPedidos.html'),
('S', 'ConsultaPedidosAPP.html'),
('S', 'ConsultaPedidosCRMBOT.html'),
('S', 'ConsultaPedidosDomicom.html'),
('S', 'ConsultaPedidosEnCurso.html'),
('S', 'ConsultaPedidosMapaEvolutivo.html'),
('S', 'ConsultaPedidosNuevaTienda.html'),
('S', 'ConsultaPuntosCliente.html'),
('S', 'ConsultaSolicitudFacturaElectronica.html'),
('S', 'ConsultaTiemposPedidos.html'),
('S', 'ConsultaVentaEmpresarial.html'),
('S', 'ConsultaVentasAsesor.html'),
('S', 'ConsultarConciliacionQRDATAFONO.html'),
('S', 'ConsultarEstadoDatafonos.html'),
('S', 'DashboardCargo.html'),
('S', 'DesempenoDomiciliario.html'),
('S', 'DispersionPremiosRuleta.html'),
('S', 'Especialidad.html'),
('S', 'EstadoPedido.html'),
('S', 'EstadosHistorialPedido.html'),
('S', 'EstadosPedidosTienda.html'),
('S', 'ExcepcionPrecio.html'),
('S', 'MapaCalorDomicilios.html'),
('S', 'MarcarProductoNoExistente.html'),
('S', 'MarcasCliente.html'),
('S', 'ModificarGeneralPedido.html'),
('S', 'ModificarPQRS.html'),
('S', 'MonitoreoPagosVirtuales.html'),
('S', 'MonitoreoPlataformas.html'),
('S', 'NovedadesBiometria.html'),
('S', 'Oferta.html'),
('S', 'PQRS.html'),
('S', 'Pedidos.html'),
('S', 'PorcentajeTienda.html'),
('S', 'Producto.html'),
('S', 'PromocionesReporte.html'),
('S', 'RedencionManualPuntos.html'),
('S', 'ReporteEnrutamiento.html'),
('S', 'ReportesMercadeo.html'),
('S', 'ResultadoEncuesta.html'),
('S', 'ResultadoPreguntasEmpleado.html'),
('S', 'RevisionIncumplimiento15Min.html'),
('S', 'RevisionReversaRedencion.html'),
('S', 'Rol.html'),
('S', 'SaborTipoLiquido.html'),
('S', 'TableroOfertas.html'),
('S', 'Tienda.html'),
('S', 'TiendaBloqueada.html'),
('S', 'TipoLiquido.html'),
('S', 'ValidarCodigo.html'),
('S', 'VentaIntegral.html'),
('S', 'VentaIntegralCategorias.html'),
('S', 'mapa.jsp'),
('S', 'segmentacionCliente.html');
INSERT INTO tmp_menu_fijo (menu, url_html) VALUES
('P', 'CancelacionesPlataformas.html'),
('P', 'ConsultaPQRS.html'),
('P', 'ConsultaPedidos.html'),
('P', 'ConsultaSolicitudFacturaElectronica.html'),
('P', 'ConsultarConciliacionQRDATAFONO.html'),
('P', 'ConsultarRegistroPedidoCumple.html'),
('P', 'EstadosHistorialPedido.html'),
('P', 'PQRS.html'),
('P', 'Pedidos.html'),
('P', 'RegistroPedidoCumple.html'),
('P', 'ValidarCodigo.html');

-- Operario (administrador N), Lider Administrativo (S) y Administrador PQRS (P). Las 3 de seguridad ya no van en el menu del Lider.
INSERT IGNORE INTO rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM tmp_menu_fijo f
JOIN pantalla p ON p.url_html = f.url_html
JOIN rol r ON r.nombre = CASE f.menu WHEN 'N' THEN 'Operario' WHEN 'S' THEN 'Lider Administrativo' ELSE 'Administrador PQRS/Contact Center' END
WHERE NOT (f.menu = 'S' AND f.url_html IN ('Rol.html', 'AsignarPantallasRol.html', 'AsignarRolUsuario.html'));

-- ---------------------------------------------------------------- 5. que nadie pierda su menu: sin rol -> el de su campo administrador
INSERT IGNORE INTO usuario_rol (idusuario, idrol)
SELECT u.id, CASE u.administrador WHEN 'S' THEN (SELECT idrol FROM rol WHERE nombre = 'Lider Administrativo')
                                   WHEN 'P' THEN (SELECT idrol FROM rol WHERE nombre = 'Administrador PQRS/Contact Center')
                                   ELSE (SELECT idrol FROM rol WHERE nombre = 'Operario') END
FROM usuario u WHERE NOT EXISTS (SELECT 1 FROM usuario_rol ur WHERE ur.idusuario = u.id);

-- ---------------------------------------------------------------- 6. el modulo viejo que quede vacio se apaga
UPDATE menu_modulo m SET m.activo = 'N'
WHERE m.idmodulo_padre IS NULL AND NOT EXISTS (SELECT 1 FROM pantalla p WHERE p.idmodulo = m.idmodulo AND p.activo = 'S')
  AND NOT EXISTS (SELECT 1 FROM (SELECT idmodulo, idmodulo_padre FROM menu_modulo) h WHERE h.idmodulo_padre = m.idmodulo);

DROP TEMPORARY TABLE IF EXISTS tmp_mapa;
DROP TEMPORARY TABLE IF EXISTS tmp_menu_fijo;

-- ---------------------------------------------------------------- verificacion
SELECT r.nombre AS rol, COUNT(rp.idpantalla) AS pantallas FROM rol r LEFT JOIN rol_pantalla rp ON rp.idrol = r.idrol GROUP BY r.idrol, r.nombre ORDER BY r.idrol;
SELECT IFNULL(pa.nombre, '(raiz)') AS padre, m.nombre AS modulo, m.orden, m.activo, (SELECT COUNT(*) FROM pantalla p WHERE p.idmodulo = m.idmodulo AND p.activo = 'S') AS pantallas
FROM menu_modulo m LEFT JOIN menu_modulo pa ON pa.idmodulo = m.idmodulo_padre ORDER BY IFNULL(pa.orden, m.orden), m.orden;
SELECT 'pantallas activas en un modulo apagado (debe ser 0)' AS revision, COUNT(*) AS cuantas FROM pantalla p JOIN menu_modulo m ON m.idmodulo = p.idmodulo WHERE p.activo = 'S' AND m.activo <> 'S';
SELECT u.nombre AS usuario, u.administrador, GROUP_CONCAT(r.nombre ORDER BY r.idrol SEPARATOR ', ') AS roles FROM usuario u JOIN usuario_rol ur ON ur.idusuario = u.id JOIN rol r ON r.idrol = ur.idrol WHERE u.nombre = 'jdbotero' GROUP BY u.id;
