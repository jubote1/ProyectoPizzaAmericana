-- ==================================================================================
-- Script: sql/2026_11_23_03_enrutador_canales_motor_y_sombra.sql
-- Propósito:
--   1. Crear tabla pos_central.sombra_pedido_canal para auditoría y comparación
--      paralela multicanal (Kuno, App, Bot, Rappi, DiDi, Contact Center).
--   2. Parametrizar interruptor multicanal en general.parametros (por defecto LEGACY).
--   3. Registrar la pantalla 'ConsultaPedidosNuevoMotor.html' en seguridad.pantalla
--      y otorgar permisos para el menú dinámico.
-- Idempotente: seguro para ejecutarse múltiples veces.
-- ==================================================================================

-- 1. Tabla de auditoría comparativa multicanal en pos_central
CREATE TABLE IF NOT EXISTS pos_central.sombra_pedido_canal (
  idsombra bigint NOT NULL AUTO_INCREMENT,
  canal varchar(20) NOT NULL COMMENT 'KUNO, APP, BOT, RAPPI, DIDI, CONTACT',
  idpedido_central int NOT NULL DEFAULT 0,
  idorden_comercio varchar(64) DEFAULT '',
  idtienda int NOT NULL DEFAULT 0,
  fechapedido datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modo_ejecucion varchar(20) NOT NULL DEFAULT 'PARALELO' COMMENT 'PARALELO, NUEVO, LEGACY',
  version_motor varchar(40) NOT NULL DEFAULT 'v1',
  estado_motor varchar(20) NOT NULL DEFAULT 'OK' COMMENT 'OK, ERROR, DIFERENCIA',
  subtotal_viejo double DEFAULT 0,
  total_viejo double DEFAULT 0,
  subtotal_motor double DEFAULT 0,
  total_motor double DEFAULT 0,
  diferencia double DEFAULT 0,
  avisos_traduccion int DEFAULT 0,
  duracion_ms int DEFAULT 0,
  solicitud_json mediumtext,
  resultado_json mediumtext,
  error_mensaje text,
  PRIMARY KEY (idsombra),
  KEY idx_pedido (idpedido_central),
  KEY idx_fecha (fechapedido),
  KEY idx_canal_fecha (canal, fechapedido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

-- 2. Parametros en general.parametros (Interruptores)
-- Modos permitidos:
--   'LEGACY'  : 100% flujo viejito (por defecto)
--   'PARALELO': Flujo viejito síncrono + Nuevo motor asíncrono en sombra (auditoría/comparación)
--   'NUEVO'   : 100% nuevo motor con fallback automático al viejito

INSERT INTO general.parametros (valorparametro, valortexto)
VALUES ('MODO_MOTOR_GLOBAL', 'LEGACY')
ON DUPLICATE KEY UPDATE valortexto = VALUES(valortexto);

INSERT INTO general.parametros (valorparametro, valortexto)
VALUES ('KUNO_NUEVO_MOTOR', 'N')
ON DUPLICATE KEY UPDATE valortexto = VALUES(valortexto);

INSERT INTO general.parametros (valorparametro, valortexto)
VALUES ('MODO_MOTOR_KUNO', 'LEGACY')
ON DUPLICATE KEY UPDATE valortexto = VALUES(valortexto);

INSERT INTO general.parametros (valorparametro, valortexto)
VALUES ('MODO_MOTOR_RAPPI', 'LEGACY')
ON DUPLICATE KEY UPDATE valortexto = VALUES(valortexto);

INSERT INTO general.parametros (valorparametro, valortexto)
VALUES ('MODO_MOTOR_DIDI', 'LEGACY')
ON DUPLICATE KEY UPDATE valortexto = VALUES(valortexto);

INSERT INTO general.parametros (valorparametro, valortexto)
VALUES ('MODO_MOTOR_BOT', 'LEGACY')
ON DUPLICATE KEY UPDATE valortexto = VALUES(valortexto);

-- 3. Registro en pantalla (para el menú dinámico GetMenuUsuario)
-- Ubicación: Submódulo 'Consulta de pedidos' bajo el módulo 'Pedidos'
INSERT INTO pantalla (nombre, url_html, idmodulo, orden, activo)
SELECT 'Consulta Unificada Nuevo Modelo', 'ConsultaPedidosNuevoMotor.html', 
       (SELECT idmodulo FROM menu_modulo WHERE nombre = 'Consulta de pedidos' LIMIT 1),
       35, 'S'
WHERE NOT EXISTS (SELECT 1 FROM pantalla WHERE url_html = 'ConsultaPedidosNuevoMotor.html');

-- Asignar a roles principales (Operario, Lider Administrativo, Superadministrador)
INSERT IGNORE INTO rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM rol r
CROSS JOIN pantalla p
WHERE p.url_html = 'ConsultaPedidosNuevoMotor.html'
  AND r.nombre IN ('Operario', 'Líder Administrativo', 'Superadministrador', 'Administrador');
